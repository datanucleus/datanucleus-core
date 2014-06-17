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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.SCO;

/**
 * Field manager that perists all unpersisted PC objects referenced from the source object.
 * If any collection/map fields are not currently using SCO wrappers they will be converted to do so.
 * Effectively provides "persistence-by-reachability" (at insert/update).
 */
public class PersistFieldManager extends AbstractFieldManager
{
    /** ObjectProvider for the owning object. */
    private final ObjectProvider op;

    /** Whether this manager will replace any SCO fields with SCO wrappers. */
    private final boolean replaceSCOsWithWrappers;

    /**
     * Constructor.
     * @param op The ObjectProvider for the object.
     * @param replaceSCOsWithWrappers Whether to swap any SCO field objects for SCO wrappers
     */
    public PersistFieldManager(ObjectProvider op, boolean replaceSCOsWithWrappers)
    {
        this.op = op;
        this.replaceSCOsWithWrappers = replaceSCOsWithWrappers;
    }

    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     * @param ownerFieldNum Field number of owner where this is embedded
     * @param objectType Type of object (see org.datanucleus.ObjectProvider)
     * @return The processed persistable object
     */
    protected Object processPersistable(Object pc, int ownerFieldNum, int objectType)
    {
        // TODO Consider adding more of the functionality in SCOUtils.validateObjectForWriting
        ApiAdapter adapter = op.getExecutionContext().getApiAdapter();
        if (!adapter.isPersistent(pc) || (adapter.isPersistent(pc) && adapter.isDeleted(pc)))
        {
            // Object is TRANSIENT/DETACHED and being persisted, or P_NEW_DELETED and being re-persisted
            if (objectType != ObjectProvider.PC)
            {
                return op.getExecutionContext().persistObjectInternal(pc, op, ownerFieldNum, objectType);
            }
            return op.getExecutionContext().persistObjectInternal(pc, null, -1, objectType);
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
            AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            boolean persistCascade = mmd.isCascadePersist();
            ClassLoaderResolver clr = op.getExecutionContext().getClassLoaderResolver();
            RelationType relationType = mmd.getRelationType(clr);

            if (replaceSCOsWithWrappers)
            {
                // Replace any SCO field that isn't already a wrapper, with its wrapper object
                boolean[] secondClassMutableFieldFlags = op.getClassMetaData().getSCOMutableMemberFlags();
                if (secondClassMutableFieldFlags[fieldNumber] && !(value instanceof SCO))
                {
                    // Replace the field with a SCO wrapper
                    value = op.wrapSCOField(fieldNumber, value, false, true, true);
                }
            }

            if (persistCascade)
            {
                if (RelationType.isRelationSingleValued(relationType))
                {
                    // Process PC fields
                    if (mmd.isEmbedded() || mmd.isSerialized())
                    {
                        processPersistable(value, fieldNumber, ObjectProvider.EMBEDDED_PC);
                    }
                    else
                    {
                        processPersistable(value, -1, ObjectProvider.PC);
                    }
                }
                else if (RelationType.isRelationMultiValued(relationType))
                {
                    ApiAdapter api = op.getExecutionContext().getApiAdapter();
                    if (mmd.hasCollection())
                    {
                        // Process all elements of the Collection that are PC
                        Collection coll = (Collection)value;
                        Iterator iter = coll.iterator();
                        int position = 0;
                        while (iter.hasNext())
                        {
                            Object element = iter.next();
                            if (api.isPersistable(element))
                            {
                                if (mmd.getCollection().isEmbeddedElement() || mmd.getCollection().isSerializedElement())
                                {
                                    processPersistable(element, fieldNumber, ObjectProvider.EMBEDDED_COLLECTION_ELEMENT_PC);
                                }
                                else
                                {
                                    Object newElement = processPersistable(element, -1, ObjectProvider.PC);
                                    ObjectProvider elementSM = op.getExecutionContext().findObjectProvider(newElement);
                                    if (elementSM.getReferencedPC() != null)
                                    {
                                        // Must be attaching this element, so swap element (detached -> attached)
                                        if (coll instanceof List)
                                        {
                                            ((List) coll).set(position, newElement);
                                        }
                                        else
                                        {
                                            coll.remove(element);
                                            coll.add(newElement);
                                        }
                                    }
                                }
                            }
                            position++;
                        }
                    }
                    else if (mmd.hasMap())
                    {
                        // Process all keys, values of the Map that are PC
                        Map map = (Map)value;

                        Iterator<Map.Entry> entryIter = map.entrySet().iterator();
                        while (entryIter.hasNext())
                        {
                            Map.Entry entry = entryIter.next();
                            Object mapKey = entry.getKey();
                            Object mapValue = entry.getValue();
                            Object newMapKey = mapKey;
                            Object newMapValue = mapValue;
                            if (api.isPersistable(mapKey))
                            {
                                // Persist (or attach) the key
                                if (mmd.getMap().isEmbeddedKey() || mmd.getMap().isSerializedKey())
                                {
                                    processPersistable(mapKey, fieldNumber, ObjectProvider.EMBEDDED_MAP_KEY_PC);
                                }
                                else
                                {
                                    newMapKey = processPersistable(mapKey, -1, ObjectProvider.PC);
                                }
                            }
                            if (api.isPersistable(mapValue))
                            {
                                // Persist (or attach) the value
                                if (mmd.getMap().isEmbeddedValue() || mmd.getMap().isSerializedValue())
                                {
                                    processPersistable(mapValue, fieldNumber, ObjectProvider.EMBEDDED_MAP_VALUE_PC);
                                }
                                else
                                {
                                    newMapValue = processPersistable(mapValue, -1, ObjectProvider.PC);
                                }
                            }
                            if (newMapKey != mapKey || newMapValue != mapValue)
                            {
                                // Maybe we have just have attached key or value
                                boolean updateKey = false;
                                boolean updateValue = false;
                                if (newMapKey != mapKey)
                                {
                                    ObjectProvider keySM = op.getExecutionContext().findObjectProvider(newMapKey);
                                    if (keySM.getReferencedPC() != null)
                                    {
                                        // Attaching the key
                                        updateKey = true;
                                    }
                                }
                                if (newMapValue != mapValue)
                                {
                                    ObjectProvider valSM = op.getExecutionContext().findObjectProvider(newMapValue);
                                    if (valSM.getReferencedPC() != null)
                                    {
                                        // Attaching the value
                                        updateValue = true;
                                    }
                                }
                                if (updateKey)
                                {
                                    map.remove(mapKey);
                                    map.put(newMapKey, updateValue ? newMapValue : mapValue);
                                }
                                else if (updateValue)
                                {
                                    map.put(mapKey, newMapValue);
                                }
                            }
                        }
                    }
                    else if (mmd.hasArray())
                    {
                        if (value instanceof Object[])
                        {
                            Object[] array = (Object[]) value;
                            for (int i=0;i<array.length;i++)
                            {
                                Object element = array[i];
                                if (api.isPersistable(element))
                                {
                                    if (mmd.getArray().isEmbeddedElement() || mmd.getArray().isSerializedElement())
                                    {
                                        // TODO This should be ARRAY_ELEMENT_PC but we haven't got that yet
                                        processPersistable(element, fieldNumber, ObjectProvider.EMBEDDED_COLLECTION_ELEMENT_PC);
                                    }
                                    else
                                    {
                                        Object processedElement = processPersistable(element, -1, ObjectProvider.PC);
                                        ObjectProvider elementSM = op.getExecutionContext().findObjectProvider(processedElement);
                                        if (elementSM.getReferencedPC() != null)
                                        {
                                            // Must be attaching this element, so swap element (detached -> attached)
                                            array[i] = processedElement;
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            // primitive array
                        }
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