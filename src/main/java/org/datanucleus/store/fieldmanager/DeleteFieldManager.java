/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
2011 Andy Jefferson - add null if bidir and not dependent functionality
    ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

import java.util.Map.Entry;

import org.datanucleus.ExecutionContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.containers.ContainerHandler;
import org.datanucleus.store.types.containers.ElementContainerAdapter;
import org.datanucleus.store.types.containers.ElementContainerHandler;
import org.datanucleus.store.types.containers.MapContainerAdapter;

/**
 * Field manager that deletes all "dependent" PC objects referenced from the source object.
 * Effectively provides "delete-dependent".
 */
public class DeleteFieldManager extends AbstractFieldManager
{
    /** StateManager for the owning object. */
    private final DNStateManager sm;

    private boolean manageRelationships = false;

    /**
     * Constructor.
     * @param sm StateManager for the object.
     */
    public DeleteFieldManager(DNStateManager sm)
    {
        this(sm, false);
    }

    /**
     * Constructor.
     * @param sm StateManager for the object.
     * @param manageRelationships Whether to make an attempt to manage relationships when bidir fields are affected by this deletion (RDBMS typically doesnt need this)
     */
    public DeleteFieldManager(DNStateManager sm, boolean manageRelationships)
    {
        this.sm = sm;
        this.manageRelationships = manageRelationships;
    }

    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     */
    protected void processPersistable(Object pc)
    {
        DNStateManager pcSM = sm.getExecutionContext().findStateManager(pc);
        if (pcSM != null)
        {
            if (pcSM.isDeleting() || pcSM.becomingDeleted())
            {
                // Already becoming deleted so jump out
                return;
            }
        }

        // Delete it
        sm.getExecutionContext().deleteObjectInternal(pc);
    }

    /**
     * Method to store an object field.
     * @param fieldNumber Number of the field (absolute)
     * @param value Value of the field
     */
    public void storeObjectField(int fieldNumber, Object value)
    {
        if (value != null)
        {
            AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            ExecutionContext ec = sm.getExecutionContext();
            RelationType relationType = mmd.getRelationType(ec.getClassLoaderResolver());

            if (relationType != RelationType.NONE)
            {
                if (mmd.hasContainer())
                {
                    processContainer(mmd, value, ec, relationType);
                }
                else
                {
                    processSingleValue(mmd, value, ec, relationType);
                }
            }
        }
    }

    private void processSingleValue(AbstractMemberMetaData mmd, Object value, ExecutionContext ec, RelationType relationType)
    {
        // Process PC fields
        if (mmd.isDependent())
        {
            processPersistable(value);
        }
        else if (manageRelationships && RelationType.isBidirectional(relationType) && !mmd.isEmbedded())
        {
            DNStateManager valueSM = ec.findStateManager(value);
            if (valueSM != null && !valueSM.getLifecycleState().isDeleted() && !valueSM.isDeleting())
            {
                AbstractMemberMetaData relMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
                if (relationType == RelationType.ONE_TO_ONE_BI)
                {
                    valueSM.replaceFieldMakeDirty(relMmd.getAbsoluteFieldNumber(), null);
                    valueSM.flush();
                }
                else if (relationType == RelationType.MANY_TO_ONE_BI)
                {
                    // Make sure field at other side is loaded, and remove from any Collection
                    valueSM.loadField(relMmd.getAbsoluteFieldNumber());
                    Object relValue = valueSM.provideField(relMmd.getAbsoluteFieldNumber());
                    if (relValue != null)
                    {
                        ContainerHandler containerHandler = ec.getTypeManager().getContainerHandler(relMmd.getType());
                        if (containerHandler instanceof ElementContainerHandler)
                        {
                            ElementContainerAdapter adapter = (ElementContainerAdapter) containerHandler.getAdapter(relValue);
                            adapter.remove(sm.getObject());
                        }
                    }
                }
            }
        }
    }
    
    private void processContainer(AbstractMemberMetaData mmd, Object container, ExecutionContext ec, RelationType relationType)
    {
        TypeManager typeManager = sm.getExecutionContext().getTypeManager();

        if (mmd.hasMap())
        {
            processMapContainer(mmd, container, typeManager.getContainerHandler(mmd.getType()));
        }
        else
        {
            processElementContainer(mmd, container, typeManager.getContainerHandler(mmd.getType()), ec, relationType);
        }
    }
    
    private void processMapContainer(AbstractMemberMetaData mmd, Object container, ContainerHandler<Object, MapContainerAdapter<Object>> containerHandler)
    {
        boolean dependentKey = mmd.getMap().isDependentKey();
        boolean dependentValue = mmd.getMap().isDependentValue();
        
        if (dependentKey && dependentValue)
        {
            ApiAdapter api = sm.getExecutionContext().getApiAdapter();
            
            // Process all keys and values of the Map that are PC
            for (Entry<Object, Object> entry : containerHandler.getAdapter(container).entries())
            {
                Object key = entry.getKey();
                if (api.isPersistable(key))
                {
                    processPersistable(key);
                }
                Object value = entry.getValue();
                if (api.isPersistable(value))
                {
                    processPersistable(key);
                }
            }
        }
        else if (dependentKey)
        {
            ApiAdapter api = sm.getExecutionContext().getApiAdapter();
            
            // Process all keys of the Map that are PC
            for (Object key : containerHandler.getAdapter(container).keys())
            {
                if (api.isPersistable(key))
                {
                    processPersistable(key);
                }
            }
        }
        else if (dependentValue)
        {
            ApiAdapter api = sm.getExecutionContext().getApiAdapter();
            
            // Process all values of the Map that are PC
            for (Object value : containerHandler.getAdapter(container).values())
            {
                if (api.isPersistable(value))
                {
                    processPersistable(value);
                }
            }
        }
        // TODO Handle nulling of bidirs - Isn't being done by the relationship manager already?
    }

    private void processElementContainer(AbstractMemberMetaData mmd, Object container, 
            ContainerHandler<Object, ElementContainerAdapter<Object>> containerHandler, ExecutionContext ec, RelationType relationType)
    {
        if (mmd.isCascadeRemoveOrphans() || (mmd.getCollection() != null && mmd.getCollection().isDependentElement()) || 
            (mmd.getArray() != null && mmd.getArray().isDependentElement()))
        {
            // Process all elements of the container that are PC
            ApiAdapter api = sm.getExecutionContext().getApiAdapter();
            for (Object element : containerHandler.getAdapter(container))
            {
                if (api.isPersistable(element))
                {
                    processPersistable(element);
                }
            }
        }
        else if (manageRelationships && relationType == RelationType.ONE_TO_MANY_BI && !mmd.isEmbedded() && !mmd.getCollection().isEmbeddedElement())
        {
            ApiAdapter api = sm.getExecutionContext().getApiAdapter();
            for (Object element : containerHandler.getAdapter(container))
            {
                if (api.isPersistable(element))
                {
                    DNStateManager elementSM = ec.findStateManager(element);
                    if (elementSM != null && !elementSM.getLifecycleState().isDeleted() && !elementSM.isDeleting())
                    {
                        AbstractMemberMetaData relMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
                        elementSM.replaceFieldMakeDirty(relMmd.getAbsoluteFieldNumber(), null);
                        elementSM.flush();
                    }
                }
            }
        }
    }

    public void storeBooleanField(int fieldNumber, boolean value)
    {
        // Do nothing
    }

    public void storeByteField(int fieldNumber, byte value)
    {
        // Do nothing
    }

    public void storeCharField(int fieldNumber, char value)
    {
        // Do nothing
    }

    public void storeDoubleField(int fieldNumber, double value)
    {
        // Do nothing
    }

    public void storeFloatField(int fieldNumber, float value)
    {
        // Do nothing
    }

    public void storeIntField(int fieldNumber, int value)
    {
        // Do nothing
    }

    public void storeLongField(int fieldNumber, long value)
    {
        // Do nothing
    }

    public void storeShortField(int fieldNumber, short value)
    {
        // Do nothing
    }

    public void storeStringField(int fieldNumber, String value)
    {
        // Do nothing
    }
}
