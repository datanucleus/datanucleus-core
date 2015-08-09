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

import java.util.Collection;
import java.util.Map.Entry;

import org.datanucleus.ExecutionContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.ContainerHandler;
import org.datanucleus.store.types.ElementContainerAdapter;
import org.datanucleus.store.types.MapContainerAdapter;
import org.datanucleus.store.types.TypeManager;

/**
 * Field manager that deletes all "dependent" PC objects referenced from the source object.
 * Effectively provides "delete-dependent".
 */
public class DeleteFieldManager extends AbstractFieldManager
{
    /** ObjectProvider for the owning object. */
    private final ObjectProvider op;

    private boolean manageRelationships = false;

    /**
     * Constructor.
     * @param op The ObjectProvider for the object.
     */
    public DeleteFieldManager(ObjectProvider op)
    {
        this(op, false);
    }

    /**
     * Constructor.
     * @param op The ObjectProvider for the object.
     * @param manageRelationships Whether to make an attempt to manage relationships when bidir fields are affected by this deletion (RDBMS typically doesnt need this)
     */
    public DeleteFieldManager(ObjectProvider op, boolean manageRelationships)
    {
        this.op = op;
        this.manageRelationships = manageRelationships;
    }

    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     */
    protected void processPersistable(Object pc)
    {
        ObjectProvider pcOP = op.getExecutionContext().findObjectProvider(pc);
        if (pcOP != null)
        {
            if (pcOP.isDeleting() || pcOP.becomingDeleted())
            {
                // Already becoming deleted so jump out
                return;
            }
        }

        // Delete it
        op.getExecutionContext().deleteObjectInternal(pc);
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
            AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            ExecutionContext ec = op.getExecutionContext();
            RelationType relationType = mmd.getRelationType(ec.getClassLoaderResolver());

            if (relationType != RelationType.NONE)
            {
                if (mmd.hasContainer())
                {
                    processContainer(fieldNumber, value, mmd, ec, relationType);
                }
                else
                {
                    processSingleValue(value, mmd, ec, relationType);
                }

            }
        }
    }

    private void processSingleValue(Object value, AbstractMemberMetaData mmd, ExecutionContext ec, RelationType relationType)
    {
        // Process PC fields
        if (mmd.isDependent())
        {
            processPersistable(value);
        }
        else if (manageRelationships && RelationType.isBidirectional(relationType) && !mmd.isEmbedded())
        {
            ObjectProvider valueOP = ec.findObjectProvider(value);
            if (valueOP != null && !valueOP.getLifecycleState().isDeleted() && !valueOP.isDeleting())
            {
                AbstractMemberMetaData relMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
                if (relationType == RelationType.ONE_TO_ONE_BI)
                {
                    valueOP.replaceFieldMakeDirty(relMmd.getAbsoluteFieldNumber(), null);
                    valueOP.flush();
                }
                else if (relationType == RelationType.MANY_TO_ONE_BI)
                {
                    // Make sure field at other side is loaded, and remove from any Collection
                    valueOP.loadField(relMmd.getAbsoluteFieldNumber());
                    Object relValue = valueOP.provideField(relMmd.getAbsoluteFieldNumber());
                    if (relValue != null)
                    {
                        if (relValue instanceof Collection)
                        {
                            ((Collection)relValue).remove(op.getObject());
                        }
                    }
                }
            }
        }
    }
    
    private void processContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd, ExecutionContext ec, RelationType relationType)
    {
        TypeManager typeManager = op.getExecutionContext().getTypeManager();
        ContainerHandler containerHandler = typeManager.getContainerHandler(container.getClass());

        if (mmd.hasMap())
        {
            processMapContainer(fieldNumber, container, mmd, containerHandler);
        }
        else
        {
            processElementContainer(fieldNumber, container, mmd, containerHandler, ec, relationType);
        }
    }
    
    private void processMapContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd,
            ContainerHandler<Object, MapContainerAdapter<Object>> containerHandler)
    {
        boolean dependentKey = mmd.getMap().isDependentKey();
        boolean dependentValue = mmd.getMap().isDependentValue();
        
        if (dependentKey && dependentValue)
        {
            ApiAdapter api = op.getExecutionContext().getApiAdapter();
            
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
            ApiAdapter api = op.getExecutionContext().getApiAdapter();
            
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
            ApiAdapter api = op.getExecutionContext().getApiAdapter();
            
            // Process all values of the Map that are PC
            for (Object value : containerHandler.getAdapter(container).values())
            {
                if (api.isPersistable(value))
                {
                    processPersistable(value);
                }
            }
        }
        // Renato: TODO Handle nulling of bidirs - Isn't being done by the relationship manager already?
    }

    private void processElementContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd,
            ContainerHandler<Object, ElementContainerAdapter<Object>> containerHandler, ExecutionContext ec, RelationType relationType)
    {
        if (mmd.isCascadeRemoveOrphans() || (mmd.getCollection() != null && mmd.getCollection().isDependentElement()) || (mmd.getArray() != null && mmd
                .getArray().isDependentElement()))
        {
            ApiAdapter api = op.getExecutionContext().getApiAdapter();

            // Process all elements of the container that are PC
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
            ApiAdapter api = op.getExecutionContext().getApiAdapter();
            for (Object element : containerHandler.getAdapter(container))
            {
                if (api.isPersistable(element))
                {
                    ObjectProvider elementOP = ec.findObjectProvider(element);
                    if (elementOP != null && !elementOP.getLifecycleState().isDeleted() && !elementOP.isDeleting())
                    {
                        AbstractMemberMetaData relMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
                        elementOP.replaceFieldMakeDirty(relMmd.getAbsoluteFieldNumber(), null);
                        elementOP.flush();
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
