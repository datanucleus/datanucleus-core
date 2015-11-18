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

import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchPlanForClass;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MapMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DetachState;
import org.datanucleus.state.FetchPlanState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.ContainerHandler;
import org.datanucleus.store.types.ElementContainerAdapter;
import org.datanucleus.store.types.MapContainerAdapter;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.TypeManager;

/**
 * FieldManager to handle the detachment of fields with persistable objects.
 */
public class DetachFieldManager extends AbstractFetchDepthFieldManager
{
    /** Whether we should create detached copies, or detach in situ. */
    boolean copy = true;

    /**
     * Constructor for a field manager for detachment.
     * @param op the ObjectProvider of the instance being detached. An instance in Persistent or Transactional state
     * @param secondClassMutableFields The second class mutable fields for the class of this object
     * @param fpClass Fetch Plan for the class of this instance
     * @param state State object to hold any pertinent controls for the detachment process
     * @param copy Whether to create detached COPIES or just detach in-situ
     */
    public DetachFieldManager(ObjectProvider op, boolean[] secondClassMutableFields, FetchPlanForClass fpClass, FetchPlanState state, boolean copy)
    {
        super(op, secondClassMutableFields, fpClass, state);
        this.copy = copy;
    }

    
    /**
     * Utility method to process the passed persistable object creating a copy.
     * @param pc The PC object
     * @return The processed object
     */
    protected Object processPersistableCopy(Object pc)
    {
        ExecutionContext ec = op.getExecutionContext();
        ApiAdapter api = ec.getApiAdapter();

        if (!api.isDetached(pc) && api.isPersistent(pc))
        {
            // Detach a copy and return the copy
            return ec.detachObjectCopy(pc, state);
        }
        return pc;
    }
    
    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     */
    protected void processPersistable(Object pc)
    {
        ExecutionContext ec = op.getExecutionContext();
        ApiAdapter api = ec.getApiAdapter();

        if (!api.isDetached(pc) && api.isPersistent(pc))
        {
            // Persistent object that is not yet detached so detach it
            ec.detachObject(pc, state);
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
        op.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);

        Object detachedValue = null;
        if (value != null)
        {
            AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);

            if (mmd.hasContainer())
            {
                // 1-N, M-N Container
                detachedValue = processContainer(fieldNumber, value, mmd);
            }
            else
            {
                detachedValue = processField(fieldNumber, value, mmd);
            }
        }

        return detachedValue;
    }

    private Object processField(int fieldNumber, Object value, AbstractMemberMetaData mmd)
    {
        RelationType relType = mmd.getRelationType(op.getExecutionContext().getClassLoaderResolver());

        if (relType == RelationType.NONE)
        {
            if (secondClassMutableFields[fieldNumber])
            {
                if (!(value instanceof SCO))
                {
                    // Replace with SCO so we can work with it
                    value = SCOUtils.wrapSCOField(op, fieldNumber, value, true);
                }

                // Other SCO
                if (copy)
                {
                    return ((SCO) value).detachCopy(state);
                }

                SCO sco = (SCO) value;

                if (SCOUtils.detachAsWrapped(op))
                {
                    return sco;
                }
                return SCOUtils.unwrapSCOField(op, fieldNumber, sco);
            }
        }
        else
        {
            // 1-1 PC
            if (copy)
            {
                return processPersistableCopy(value);
            }

            processPersistable(value);
        }

        return value;
    }
        
    private Object processContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd)
    {
        Object detachedContainer;

        TypeManager typeManager = op.getExecutionContext().getTypeManager();
        ContainerHandler containerHandler = typeManager.getContainerHandler(mmd.getType());

        if (mmd.hasMap())
        {
            detachedContainer = processMapContainer(fieldNumber, container, mmd, containerHandler);
        }
        else
        {
            detachedContainer = processElementContainer(fieldNumber, container, mmd, containerHandler);
        }

        if (!mmd.hasArray())
        {
            // Need to unset owner for mutable SCOs

            Object wrappedContainer;
            if (SCOUtils.detachAsWrapped(op))
            {
                // Try to wrap the field, if possible, replacing it since it will be returned as wrapped
                wrappedContainer = SCOUtils.wrapSCOField(op, fieldNumber, detachedContainer, true);

                // Return the wrapped, if mutable, otherwise just the immutable value
                detachedContainer = wrappedContainer;
            }
            else
            {
                // Try to wrap the field, if possible, just to be able to unset the owner, so don't
                // replace it
                wrappedContainer = SCOUtils.wrapSCOField(op, fieldNumber, detachedContainer, false);
                
                // The container can be already an SCO so unwrap it if necessary
                if (detachedContainer instanceof SCO)
                {
                    detachedContainer = SCOUtils.unwrapSCOField(op, fieldNumber, (SCO) detachedContainer);
                }
            }

            // It still can be an immutable collection or map, so must check if has been wrapped
            if (wrappedContainer instanceof SCO)
            {
                SCO sco = (SCO) wrappedContainer;
                sco.unsetOwner();
            }
        }

        return detachedContainer;
    }
        
    private Object processElementContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd,
            ContainerHandler<Object, ElementContainerAdapter<Object>> containerHandler)
    {
        Object detachedContainer;

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

        return detachedContainer;
    }
    
    private Object processMapContainer(int fieldNumber, Object mapContainer, AbstractMemberMetaData mmd,
            ContainerHandler<Object, MapContainerAdapter<Object>> containerHandler)
    {
        Object detachedMapContainer;

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
        op.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);
        ApiAdapter api = op.getExecutionContext().getApiAdapter();

        if (api.isPersistable(value))
        {
            if (copy)
            {
                DetachState.Entry entry = ((DetachState)state).getDetachedCopyEntry(value);
                if (entry != null)
                {
                    // While we are at the end of a branch and this would go beyond the depth limits,
                    // the object here *is* already detached so just return it
                    return entry.getDetachedCopyObject();
                }
            }
            else if (op.getExecutionContext().getApiAdapter().isDetached(value))
            {
                return value;
            }
        }

        // we reached a leaf of the object graph to detach
        throw new EndOfFetchPlanGraphException();
    }
}