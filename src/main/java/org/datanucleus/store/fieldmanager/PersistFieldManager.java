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
    ...
 **********************************************************************/
package org.datanucleus.store.fieldmanager;

import java.util.Map.Entry;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.containers.ContainerHandler;
import org.datanucleus.store.types.containers.ElementContainerAdapter;
import org.datanucleus.store.types.containers.ElementContainerHandler;
import org.datanucleus.store.types.containers.MapContainerAdapter;
import org.datanucleus.store.types.containers.SequenceAdapter;

/**
 * Field manager that persists all unpersisted PC objects referenced from the source object. 
 * If any collection/map fields are not currently using SCO wrappers they will be converted to do so. 
 * Effectively provides "persistence-by-reachability" (at insert/update).
 */
public class PersistFieldManager extends AbstractFieldManager
{
    /** StateManager for the owning object. */
    private final DNStateManager sm;

    /** Whether this manager will replace any SCO fields with SCO wrappers. */
    private final boolean replaceSCOsWithWrappers;

    /**
     * Constructor.
     * @param sm StateManager for the object.
     * @param replaceSCOsWithWrappers Whether to swap any SCO field objects for SCO wrappers
     */
    public PersistFieldManager(DNStateManager sm, boolean replaceSCOsWithWrappers)
    {
        this.sm = sm;
        this.replaceSCOsWithWrappers = replaceSCOsWithWrappers;
    }

    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     * @param ownerFieldNum Field number of owner where this is embedded
     * @param objectType Type of object (see org.datanucleus.state.DNStateManager)
     * @return The processed persistable object
     */
    protected Object processPersistable(Object pc, int ownerFieldNum, int objectType)
    {
        // TODO Consider adding more of the functionality in SCOUtils.validateObjectForWriting
        ApiAdapter adapter = sm.getExecutionContext().getApiAdapter();
        if (!adapter.isPersistent(pc) || (adapter.isPersistent(pc) && adapter.isDeleted(pc)))
        {
            // Object is TRANSIENT/DETACHED and being persisted, or P_NEW_DELETED and being re-persisted
            if (objectType != DNStateManager.PC)
            {
                return sm.getExecutionContext().persistObjectInternal(pc, sm, ownerFieldNum, objectType);
            }
            return sm.getExecutionContext().persistObjectInternal(pc, null, -1, objectType);
        }
        return pc;
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

            if (replaceSCOsWithWrappers)
            {
                // Replace any SCO field that isn't already a wrapper, with its wrapper object
                boolean[] secondClassMutableFieldFlags = sm.getClassMetaData().getSCOMutableMemberFlags();
                if (secondClassMutableFieldFlags[fieldNumber] && !(value instanceof SCO))
                {
                    // Replace the field with a SCO wrapper
                    value = SCOUtils.wrapSCOField(sm, fieldNumber, value, true);
                }
            }

            if (mmd.isCascadePersist())
            {
                ClassLoaderResolver clr = sm.getExecutionContext().getClassLoaderResolver();
                RelationType relationType = mmd.getRelationType(clr);

                if (relationType != RelationType.NONE)
                {
                    if (mmd.hasContainer())
                    {
                        processContainer(value, mmd);
                    }
                    else
                    {
                        // Process PC fields
                        if (mmd.isEmbedded() || mmd.isSerialized())
                        {
                            processPersistable(value, fieldNumber, DNStateManager.EMBEDDED_PC);
                        }
                        else
                        {
                            processPersistable(value, -1, DNStateManager.PC);
                        }
                    }
                }
            }
        }
    }

    private void  processContainer(Object container, AbstractMemberMetaData mmd)
    {
        if (mmd.hasMap())
        {
            processMapContainer(mmd, container);
        }
        else
        {
            processElementContainer(mmd, container);
        }
    }

    private void processMapContainer(AbstractMemberMetaData mmd, Object container)
    {
    	TypeManager typeManager = sm.getExecutionContext().getTypeManager();
    	ContainerHandler<Object, MapContainerAdapter<Object>> containerHandler = typeManager.getContainerHandler(mmd.getType());
        ApiAdapter api = sm.getExecutionContext().getApiAdapter();

        // Process all keys, values of the Map that are PC
        MapContainerAdapter<Object> mapAdapter = containerHandler.getAdapter(container);
        for (Entry<Object, Object> entry : mapAdapter.entries())
        {
            Object mapKey = entry.getKey();
            Object mapValue = entry.getValue();
            Object newMapKey = mapKey;
            Object newMapValue = mapValue;

            if (api.isPersistable(mapKey))
            {
                // Persist (or attach) the key
                int mapKeyObjectType = mmd.getMap().isEmbeddedKey() || mmd.getMap().isSerializedKey() ? DNStateManager.EMBEDDED_MAP_KEY_PC : DNStateManager.PC;
                newMapKey = processPersistable(mapKey, mmd.getAbsoluteFieldNumber(), mapKeyObjectType);
            }

            if (api.isPersistable(mapValue))
            {
                // Persist (or attach) the value
                int mapValueObjectType = mmd.getMap().isEmbeddedValue() || mmd.getMap().isSerializedValue() ? DNStateManager.EMBEDDED_MAP_VALUE_PC : DNStateManager.PC;
                newMapValue = processPersistable(mapValue, mmd.getAbsoluteFieldNumber(), mapValueObjectType);
            }

            if (newMapKey != mapKey || newMapValue != mapValue)
            {
                // Maybe we have just have attached key or value
                boolean updateKey = false;
                boolean updateValue = false;
                if (newMapKey != mapKey)
                {
                    DNStateManager keySM = sm.getExecutionContext().findStateManager(newMapKey);
                    if (keySM.getReferencedPC() != null)
                    {
                        // Attaching the key
                        updateKey = true;
                    }
                }
                if (newMapValue != mapValue)
                {
                    DNStateManager valSM = sm.getExecutionContext().findStateManager(newMapValue);
                    if (valSM.getReferencedPC() != null)
                    {
                        // Attaching the value
                        updateValue = true;
                    }
                }
                
                if (updateKey)
                {
                    mapAdapter.remove(mapKey);
                    mapAdapter.put(newMapKey, updateValue ? newMapValue : mapValue);
                }
                else if (updateValue)
                {
                    mapAdapter.put(mapKey, newMapValue);
                }
            }
        }
    }

    private void processElementContainer(AbstractMemberMetaData mmd, Object container)
    {
    	TypeManager typeManager = sm.getExecutionContext().getTypeManager();
    	ElementContainerHandler<Object, ElementContainerAdapter<Object>> elementContainerHandler = typeManager.getContainerHandler(mmd.getType());

        // Process all elements of the container that are PC
        ElementContainerAdapter containerAdapter = elementContainerHandler.getAdapter(container);

        ApiAdapter api = sm.getExecutionContext().getApiAdapter();
        int objectType = elementContainerHandler.getObjectType(mmd);
        if (objectType == DNStateManager.PC)
        {
            int elementPosition = 0;
            for (Object element : containerAdapter)
            {
                if (api.isPersistable(element))
                {
                    Object newElement = processPersistable(element, -1, objectType);
                    DNStateManager elementSM = sm.getExecutionContext().findStateManager(newElement);
                    if (elementSM.getReferencedPC() != null)
                    {
                        // Must be attaching this element, so swap element (detached -> attached)
                        if (containerAdapter instanceof SequenceAdapter) 
                        {
                            ((SequenceAdapter) containerAdapter).update(newElement, elementPosition); 
                        }
                        else
                        {
                            containerAdapter.remove(elementSM);
                            containerAdapter.add(newElement);
                        }
                    }
                }
                elementPosition++;
            }
        }
        else
        {
            // Embedded/Serialized
            for (Object element : containerAdapter)
            {
                if (api.isPersistable(element))
                {
                    processPersistable(element, mmd.getAbsoluteFieldNumber(), objectType);
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