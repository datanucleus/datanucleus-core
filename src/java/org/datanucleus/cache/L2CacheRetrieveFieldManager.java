/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.cache;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.cache.CachedPC.CachedId;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * FieldManager responsible for retrieving the values from the provided CachedPC object.
 */
public class L2CacheRetrieveFieldManager extends AbstractFieldManager
{
    /** ObjectProvider of the object we are copying values into. */
    ObjectProvider op;

    /** Execution Context. */
    ExecutionContext ec;

    /** CachedPC that we are taking values from. */
    CachedPC cachedPC;

    List<Integer> fieldsNotLoaded = null;

    public L2CacheRetrieveFieldManager(ObjectProvider op, CachedPC cachedpc)
    {
        this.op = op;
        this.ec = op.getExecutionContext();
        this.cachedPC = cachedpc;
    }

    public int[] getFieldsNotLoaded()
    {
        if (fieldsNotLoaded == null)
        {
            return null;
        }
        int[] flds = new int[fieldsNotLoaded.size()];
        int i=0;
        for (Integer fldNum : fieldsNotLoaded)
        {
            flds[i++] = fldNum;
        }
        return flds;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchBooleanField(int)
     */
    @Override
    public boolean fetchBooleanField(int fieldNumber)
    {
        return (Boolean)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchByteField(int)
     */
    @Override
    public byte fetchByteField(int fieldNumber)
    {
        return (Byte)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchCharField(int)
     */
    @Override
    public char fetchCharField(int fieldNumber)
    {
        return (Character)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchDoubleField(int)
     */
    @Override
    public double fetchDoubleField(int fieldNumber)
    {
        return (Double)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchFloatField(int)
     */
    @Override
    public float fetchFloatField(int fieldNumber)
    {
        return (Float)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchIntField(int)
     */
    @Override
    public int fetchIntField(int fieldNumber)
    {
        return (Integer)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchLongField(int)
     */
    @Override
    public long fetchLongField(int fieldNumber)
    {
        return (Long)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchShortField(int)
     */
    @Override
    public short fetchShortField(int fieldNumber)
    {
        return (Short)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchStringField(int)
     */
    @Override
    public String fetchStringField(int fieldNumber)
    {
        return (String)cachedPC.getFieldValue(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#fetchObjectField(int)
     */
    @Override
    public Object fetchObjectField(int fieldNumber)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        Object value = cachedPC.getFieldValue(fieldNumber);

        RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (relType != RelationType.NONE)
        {
            if (value != null)
            {
                // Field stores a relation and has a value
                if (Collection.class.isAssignableFrom(value.getClass()))
                {
                    // Collection field, with fieldValue being Collection<OID>
                    Collection coll = (Collection)value;
                    try
                    {
                        // Create Collection<Element> of same type as fieldValue
                        Collection fieldColl = coll.getClass().newInstance();
                        Iterator iter = coll.iterator();
                        while (iter.hasNext())
                        {
                            Object cachedId = iter.next();
                            if (cachedId != null)
                            {
                                fieldColl.add(getObjectFromCachedId(cachedId));
                            }
                            else
                            {
                                fieldColl.add(null);
                            }
                        }
                        return op.wrapSCOField(fieldNumber, fieldColl, false, false, true);
                    }
                    catch (Exception e)
                    {
                        // Error creating field value
                        if (fieldsNotLoaded == null)
                        {
                            fieldsNotLoaded = new ArrayList<Integer>();
                        }
                        fieldsNotLoaded.add(fieldNumber);
                        NucleusLogger.CACHE.error("Exception thrown creating value for" +
                            " field " + mmd.getFullFieldName() + 
                            " of type " + value.getClass().getName(), e);
                        return null;
                    }
                }
                else if (Map.class.isAssignableFrom(value.getClass()))
                {
                    // Map field, with fieldValue being Map<OID, OID>
                    Map map = (Map)value;
                    try
                    {
                        // Create Map<Key, Value> of same type as fieldValue
                        Map fieldMap = map.getClass().newInstance();
                        Iterator iter = map.entrySet().iterator();
                        while (iter.hasNext())
                        {
                            Map.Entry entry = (Map.Entry)iter.next();

                            Object mapKey = null;
                            if (mmd.getMap().keyIsPersistent())
                            {
                                mapKey = getObjectFromCachedId(entry.getKey());
                            }
                            else
                            {
                                mapKey = entry.getKey();
                            }

                            Object mapValue = null;
                            Object mapValueId = entry.getValue();
                            if (mapValueId != null)
                            {
                                if (mmd.getMap().valueIsPersistent())
                                {
                                    mapValue = getObjectFromCachedId(entry.getValue());
                                }
                                else
                                {
                                    mapValue = entry.getValue();
                                }
                            }

                            fieldMap.put(mapKey, mapValue);
                        }
                        return op.wrapSCOField(fieldNumber, fieldMap, false, false, true);
                    }
                    catch (Exception e)
                    {
                        // Error creating field value
                        if (fieldsNotLoaded == null)
                        {
                            fieldsNotLoaded = new ArrayList<Integer>();
                        }
                        fieldsNotLoaded.add(fieldNumber);
                        NucleusLogger.CACHE.error("Exception thrown creating value for" +
                            " field " + mmd.getFullFieldName() + 
                            " of type " + value.getClass().getName(), e);
                        return null;
                    }
                }
                else if (value.getClass().isArray())
                {
                    try
                    {
                        Object[] elementOIDs = (Object[])value;
                        Class componentType = mmd.getType().getComponentType();
                        Object fieldArr = Array.newInstance(componentType, elementOIDs.length);
                        boolean persistableElement = ec.getApiAdapter().isPersistable(componentType);
                        for (int i=0;i<elementOIDs.length;i++)
                        {
                            Object element = null;
                            if (elementOIDs[i] == null)
                            {
                            }
                            else if (componentType.isInterface() || persistableElement || componentType == Object.class)
                            {
                                element = getObjectFromCachedId(elementOIDs[i]);
                            }
                            else
                            {
                                element = elementOIDs[i];
                            }
                            Array.set(fieldArr, i, element);
                        }
                        return fieldArr;
                    }
                    catch (NucleusException ne)
                    {
                        // Unable to find element(s) so set field to unloaded
                        if (fieldsNotLoaded == null)
                        {
                            fieldsNotLoaded = new ArrayList<Integer>();
                        }
                        fieldsNotLoaded.add(fieldNumber);
                        NucleusLogger.CACHE.error(
                            "Exception thrown trying to find element of array while getting object with id " + 
                            op.getInternalObjectId() + " from the L2 cache", ne);
                        return null;
                    }
                }
                else
                {
                    if (mmd.isSerialized() ||
                        MetaDataUtils.isMemberEmbedded(mmd, relType,
                            ec.getClassLoaderResolver(), ec.getMetaDataManager()))
                    {
                        if (ec.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED))
                        {
                            if (value instanceof CachedPC)
                            {
                                // Convert the CachedPC back into a managed object loading all cached fields
                                // TODO Perhaps only load fetch plan fields?
                                CachedPC valueCachedPC = (CachedPC)value;
                                AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(
                                    valueCachedPC.getObjectClass(), ec.getClassLoaderResolver());
                                int[] fieldsToLoad = ClassUtils.getFlagsSetTo(valueCachedPC.getLoadedFields(), cmd.getAllMemberPositions(), true);
                                ObjectProvider valueOP = ec.newObjectProviderForEmbedded(cmd, op, mmd.getAbsoluteFieldNumber());
                                valueOP.replaceFields(fieldsToLoad, new L2CacheRetrieveFieldManager(valueOP, valueCachedPC));
                                return valueOP.getObject();
                            }
                        }
                    }

                    // PC field so assume is the identity of the object
                    try
                    {
                        return getObjectFromCachedId(value);
                    }
                    catch (NucleusObjectNotFoundException nonfe)
                    {
                        if (fieldsNotLoaded == null)
                        {
                            fieldsNotLoaded = new ArrayList<Integer>();
                        }
                        fieldsNotLoaded.add(fieldNumber);
                        return null;
                    }
                }
            }
            else
            {
                return null;
            }
        }
        else
        {
            if (value == null)
            {
                return null;
            }
            else if (value instanceof StringBuffer)
            {
                // Use our own copy of this mutable type
                return new StringBuffer(((StringBuffer)value).toString());
            }
            else if (value instanceof StringBuilder)
            {
                // Use our own copy of this mutable type
                return new StringBuilder(((StringBuilder)value).toString());
            }
            else if (value instanceof Date)
            {
                // Use our own copy of this mutable type
                value = ((Date)value).clone();
            }
            else if (value instanceof Calendar)
            {
                // Use our own copy of this mutable type
                value = ((Calendar)value).clone();
            }

            boolean[] mutables = mmd.getAbstractClassMetaData().getSCOMutableMemberFlags();
            if (mutables[fieldNumber])
            {
                return op.wrapSCOField(fieldNumber, value, false, false, true);
            }

            return value;
        }
    }

    private Object getObjectFromCachedId(Object cachedId)
    {
        Object pcId = null;
        String pcClassName = null;
        if (cachedId instanceof CachedId)
        {
            CachedId cId = (CachedId)cachedId;
            pcId = cId.getId();
            pcClassName = cId.getClassName();
        }
        else
        {
            pcId = cachedId;
            pcClassName = IdentityUtils.getClassNameForIdentitySimple(ec.getApiAdapter(), pcId);
        }
        Class pcCls = ec.getClassLoaderResolver().classForName(pcClassName);
        return ec.findObject(pcId, null, pcCls, false, false);
    }
}