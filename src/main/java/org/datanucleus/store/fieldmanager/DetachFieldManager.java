/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


Contributors:
2005 Jorg von Frantzius - updates for fetch-depth
2007 Andy Jefferson - rewritten to process all fields detecting persistable objects at runtime
    ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

import java.util.Map.Entry;

import org.datanucleus.DetachState;
import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchPlanForClass;
import org.datanucleus.FetchPlanState;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MapMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.containers.ContainerHandler;
import org.datanucleus.store.types.containers.ElementContainerAdapter;
import org.datanucleus.store.types.containers.MapContainerAdapter;

/**
 * FieldManager to handle the detachment of fields with persistable objects.
 */
public class DetachFieldManager extends AbstractFetchDepthFieldManager
{
    /** Whether we should create detached copies, or detach in situ. */
    boolean copy = true;

    /**
     * Constructor for a field manager for detachment.
     * @param sm StateManager of the instance being detached. An instance in Persistent or Transactional state
     * @param secondClassMutableFields The second class mutable fields for the class of this object
     * @param fpClass Fetch Plan for the class of this instance
     * @param state State object to hold any pertinent controls for the detachment process
     * @param copy Whether to create detached COPIES or just detach in-situ
     */
    public DetachFieldManager(DNStateManager sm, boolean[] secondClassMutableFields, FetchPlanForClass fpClass, FetchPlanState state, boolean copy)
    {
        super(sm, secondClassMutableFields, fpClass, state);
        this.copy = copy;
    }

    /**
     * Utility method to process the passed persistable object creating a copy.
     * @param pc The PC object
     * @return The processed object
     */
    protected Object processPersistableCopy(Object pc)
    {
        ExecutionContext ec = sm.getExecutionContext();
        ApiAdapter api = ec.getApiAdapter();

        if (!api.isDetached(pc) && api.isPersistent(pc))
        {
            // Detach a copy and return the copy
            return ec.detachObjectCopy(state, pc);
        }
        return pc;
    }
    
    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     */
    protected void processPersistable(Object pc)
    {
        ExecutionContext ec = sm.getExecutionContext();
        ApiAdapter api = ec.getApiAdapter();

        if (!api.isDetached(pc) && api.isPersistent(pc))
        {
            // Persistent object that is not yet detached so detach it
            ec.detachObject(state, pc);
        }
    }
    
    /**
     * Method to fetch an object field whether it is collection/map, PC, or whatever for the detachment
     * process.
     * @param fieldNumber Number of the field
     * @return The object
     */
    protected Object internalFetchObjectField(int fieldNumber)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sm.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);

        Object detachedValue = null;
        if (value != null)
        {
            AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);

            if (mmd.hasContainer())
            {
                // 1-N, M-N Container
                detachedValue = processContainer(mmd, value);
            }
            else
            {
                detachedValue = processField(mmd, value);
            }
        }
        return detachedValue;
    }

    private Object processField(AbstractMemberMetaData mmd, Object value)
    {
        RelationType relType = mmd.getRelationType(sm.getExecutionContext().getClassLoaderResolver());
        if (relType == RelationType.NONE)
        {
            if (secondClassMutableFields[mmd.getAbsoluteFieldNumber()])
            {
                if (!(value instanceof SCO))
                {
                    // Replace with SCO so we can work with it
                    value = SCOUtils.wrapSCOField(sm, mmd.getAbsoluteFieldNumber(), value, true);
                }

                // Other SCO
                if (copy)
                {
                    return ((SCO)value).detachCopy(state);
                }

                return SCOUtils.detachAsWrapped(sm.getExecutionContext()) ? value : SCOUtils.unwrapSCOField(sm, mmd.getAbsoluteFieldNumber(), value);
            }
        }
        else
        {
            // 1-1 PC
            if (mmd.isCascadeDetach())
            {
                if (copy)
                {
                    return processPersistableCopy(value);
                }

                processPersistable(value);
            }
        }

        return value;
    }
        
    private Object processContainer(AbstractMemberMetaData mmd, Object container)
    {
        Object detachedContainer;

        TypeManager typeManager = sm.getExecutionContext().getTypeManager();
        ContainerHandler containerHandler = typeManager.getContainerHandler(mmd.getType());

        if (mmd.hasMap())
        {
            detachedContainer = processMapContainer(mmd.getAbsoluteFieldNumber(), container, mmd, containerHandler);
        }
        else
        {
            detachedContainer = processElementContainer(mmd.getAbsoluteFieldNumber(), container, mmd, containerHandler);
        }

        if (!mmd.hasArray())
        {
            // Need to unset owner for mutable SCOs
            Object wrappedContainer;
            if (SCOUtils.detachAsWrapped(sm.getExecutionContext()))
            {
                // Try to wrap the field, if possible, replacing it since it will be returned as wrapped
                wrappedContainer = SCOUtils.wrapSCOField(sm, mmd.getAbsoluteFieldNumber(), detachedContainer, true);

                // Return the wrapped, if mutable, otherwise just the immutable value
                detachedContainer = wrappedContainer;
            }
            else
            {
                // Try to wrap the field, if possible, just to be able to unset the owner, so don't replace it
                wrappedContainer = SCOUtils.wrapSCOField(sm, mmd.getAbsoluteFieldNumber(), detachedContainer, false);
                
                // The container can be already an SCO so unwrap it if necessary
                if (detachedContainer instanceof SCO)
                {
                    detachedContainer = SCOUtils.unwrapSCOField(sm, mmd.getAbsoluteFieldNumber(), detachedContainer);
                }
            }

            // It still can be an immutable collection or map, so must check if has been wrapped
            if (wrappedContainer instanceof SCO)
            {
                ((SCO)wrappedContainer).unsetOwner();
            }
        }

        return detachedContainer;
    }
        
    private Object processElementContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd,
            ContainerHandler<Object, ElementContainerAdapter<Object>> containerHandler)
    {
        Object detachedContainer;

        if (mmd.isCascadeDetach())
        {
            ElementContainerAdapter containerAdapter = containerHandler.getAdapter(container);
            if (copy)
            {
                detachedContainer = containerHandler.newContainer(mmd);
                ElementContainerAdapter<Object> copyAdapter = containerHandler.getAdapter(detachedContainer);
                for (Object element : containerAdapter)
                {
                    copyAdapter.add(processPersistableCopy(element));
                }

                // Get the updated version of the container
                detachedContainer = copyAdapter.getContainer();
            }
            else
            {
                detachedContainer = container;
                for (Object element : containerAdapter)
                {
                    processPersistable(element);
                }
            }
        }
        else
        {
            detachedContainer = container;
        }

        return detachedContainer;
    }

    private Object processMapContainer(int fieldNumber, Object mapContainer, AbstractMemberMetaData mmd,
            ContainerHandler<Object, MapContainerAdapter<Object>> containerHandler)
    {
        Object detachedMapContainer;

        if (mmd.isCascadeDetach())
        {
            MapContainerAdapter<Object> mapAdapter = containerHandler.getAdapter(mapContainer);
            if (copy)
            {
                detachedMapContainer = containerHandler.newContainer(mmd);
                MapMetaData mapMd = mmd.getMap();
                MapContainerAdapter copyAdapter = containerHandler.getAdapter(detachedMapContainer);
                for (Entry<Object, Object> entry : mapAdapter.entries())
                {
                    Object key = entry.getKey();
                    if (mapMd.keyIsPersistent())
                    {
                        key = processPersistableCopy(key);
                    }

                    Object value = entry.getValue();
                    if (mapMd.valueIsPersistent())
                    {
                        value = processPersistableCopy(value);
                    }

                    copyAdapter.put(key, value);
                }

                // Get the updated version of the container
                detachedMapContainer = copyAdapter.getContainer();
            }
            else
            {
                detachedMapContainer = mapContainer;
                MapMetaData mapMd = mmd.getMap();
                for (Entry<Object, Object> entry : mapAdapter.entries())
                {
                    Object key = entry.getKey();
                    if (mapMd.keyIsPersistent())
                    {
                        processPersistable(key);
                    }

                    Object value = entry.getValue();
                    if (mapMd.valueIsPersistent())
                    {
                        processPersistable(value);
                    }
                }
            }
        }
        else
        {
            detachedMapContainer = mapContainer;
        }

        return detachedMapContainer;
    }

    /**
     * Method to throw and EndOfFetchPlanGraphException since we're at the end of a branch in the tree.
     * @param fieldNumber Number of the field
     * @return Object to return
     */
    protected Object endOfGraphOperation(int fieldNumber)
    {
        // check if the object here is PC and is in the detached cache anyway
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sm.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);
        ApiAdapter api = sm.getExecutionContext().getApiAdapter();

        if (api.isPersistable(value))
        {
            if (copy)
            {
                DetachState.Entry entry = ((DetachState)state).getDetachedCopyEntry(value);
                if (entry != null)
                {
                    // While we are at the end of a branch and this would go beyond the depth limits, the object here *is* already detached so just return it
                    return entry.getDetachedCopyObject();
                }
            }
            else if (sm.getExecutionContext().getApiAdapter().isDetached(value))
            {
                return value;
            }
        }

        // we reached a leaf of the object graph to detach
        throw new EndOfFetchPlanGraphException();
    }
}