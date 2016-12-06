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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.datanucleus.cache.L2CacheRetrieveFieldManager.copyValue;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.cache.CachedPC.CachedId;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOContainer;
import org.datanucleus.util.NucleusLogger;

/**
 * FieldManager responsible for populating the provided CachedPC object.
 */
public class L2CachePopulateFieldManager extends AbstractFieldManager
{
    /** ObjectProvider of the object we are copying values from. */
    ObjectProvider op;

    ExecutionContext ec;

    /** CachedPC that we are copying values into. */
    CachedPC cachedPC;

    public L2CachePopulateFieldManager(ObjectProvider op, CachedPC cachedpc)
    {
        this.op = op;
        this.ec = op.getExecutionContext();
        this.cachedPC = cachedpc;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeBooleanField(int, boolean)
     */
    @Override
    public void storeBooleanField(int fieldNumber, boolean value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeCharField(int, char)
     */
    @Override
    public void storeCharField(int fieldNumber, char value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeByteField(int, byte)
     */
    @Override
    public void storeByteField(int fieldNumber, byte value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeShortField(int, short)
     */
    @Override
    public void storeShortField(int fieldNumber, short value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeIntField(int, int)
     */
    @Override
    public void storeIntField(int fieldNumber, int value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeLongField(int, long)
     */
    @Override
    public void storeLongField(int fieldNumber, long value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeFloatField(int, float)
     */
    @Override
    public void storeFloatField(int fieldNumber, float value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeDoubleField(int, double)
     */
    @Override
    public void storeDoubleField(int fieldNumber, double value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeStringField(int, java.lang.String)
     */
    @Override
    public void storeStringField(int fieldNumber, String value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true);
        cachedPC.setFieldValue(fieldNumber, value);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeObjectField(int, java.lang.Object)
     */
    @Override
    public void storeObjectField(int fieldNumber, Object value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.TRANSACTIONAL)
        {
            // Cannot cache transactional fields
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        if (!mmd.isCacheable())
        {
            // Field is marked as not cacheable so unset its loaded flag and return null
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        cachedPC.setLoadedField(fieldNumber, true); // Overridden later if necessary

        if (value == null)
        {
            cachedPC.setFieldValue(fieldNumber, null);
            return;
        }

        if (mmd.hasContainer())
        {
            processContainer(fieldNumber, mmd, value);
            return;
        }

        processField(fieldNumber, mmd, value);
    }

    protected void processContainer(int fieldNumber, AbstractMemberMetaData mmd, Object inputValue)
    {
        Object value = inputValue;
        if (inputValue instanceof SCOContainer)
        {
            if (!((SCOContainer)inputValue).isLoaded())
            {
                // Contents not loaded so just mark as unloaded
                cachedPC.setLoadedField(fieldNumber, false);
                return;
            }
     
            // Use the unwrapped value
            value = ((SCO)inputValue).getValue();
        }

        ApiAdapter api = ec.getApiAdapter();
        RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (mmd.hasMap())
        {
            if (RelationType.isRelationMultiValued(relType))
            {
                if (mmd.isSerialized() || mmd.isEmbedded() || mmd.getMap().isSerializedKey() || mmd.getMap().isSerializedValue() || 
                    (mmd.getMap().keyIsPersistent() && mmd.getMap().isEmbeddedKey()) || (mmd.getMap().valueIsPersistent() && mmd.getMap().isEmbeddedValue()))
                {
                    // TODO Support serialised/embedded keys/values
                    cachedPC.setLoadedField(fieldNumber, false);
                    return;
                }

                try
                {
                    Map returnMap = null;
                    if (value.getClass().isInterface())
                    {
                        returnMap = new HashMap();
                    }
                    else
                    {
                        if (value instanceof SCO)
                        {
                            returnMap = (Map)((SCO)value).getValue().getClass().newInstance();
                        }
                        else
                        {
                            returnMap = (Map)value.getClass().newInstance();
                        }
                    }
                    Iterator mapIter = ((Map)value).entrySet().iterator();
                    while (mapIter.hasNext())
                    {
                        Map.Entry entry = (Map.Entry)mapIter.next();
                        Object mapKey = null;
                        Object mapValue = null;
                        if (mmd.getMap().keyIsPersistent())
                        {
                            mapKey = getCacheableIdForId(api, entry.getKey());
                        }
                        else
                        {
                            mapKey = entry.getKey();
                        }
                        if (mmd.getMap().valueIsPersistent())
                        {
                            mapValue = getCacheableIdForId(api, entry.getValue());
                        }
                        else
                        {
                            mapValue = entry.getValue();
                        }
                        returnMap.put(mapKey, mapValue);
                    }

                    // Put Map<X, Y> in CachedPC where X, Y can be OID if they are persistable objects
                    cachedPC.setFieldValue(fieldNumber, returnMap);
                    return;
                }
                catch (Exception e)
                {
                    NucleusLogger.CACHE.warn("Unable to create object of type " + value.getClass().getName() + " for L2 caching : " + e.getMessage());

                    // Contents not loaded so just mark as unloaded
                    cachedPC.setLoadedField(fieldNumber, false);
                    return;
                }
            }

            // Map<Non-PC, Non-PC>
            cachedPC.setFieldValue(fieldNumber, value);
            return;
        }
        else if (mmd.hasCollection())
        {
            // 1-N, M-N Collection
            if (RelationType.isRelationMultiValued(relType))
            {
                if (value instanceof List && mmd.getOrderMetaData() != null && !mmd.getOrderMetaData().isIndexedList())
                {
                    // Ordered list so don't cache since dependent on datastore-retrieve order
                    cachedPC.setLoadedField(fieldNumber, false);
                    return;
                }
                if (mmd.isSerialized() || mmd.isEmbedded() || mmd.getCollection().isSerializedElement() || mmd.getCollection().isEmbeddedElement())
                {
                    // TODO Support serialised/embedded elements
                    cachedPC.setLoadedField(fieldNumber, false);
                    return;
                }

                Collection collValue = (Collection)value;
                Iterator collIter = collValue.iterator();
                Collection returnColl = null;
                try
                {
                    if (value.getClass().isInterface())
                    {
                        if (List.class.isAssignableFrom(value.getClass()) || mmd.getOrderMetaData() != null)
                        {
                            // List based
                            returnColl = new ArrayList();
                        }
                        else
                        {
                            // Set based
                            returnColl = new HashSet();
                        }
                    }
                    else
                    {
                        if (value instanceof SCO)
                        {
                            returnColl = (Collection)((SCO)value).getValue().getClass().newInstance();
                        }
                        else
                        {
                            returnColl = (Collection)value.getClass().newInstance();
                        }
                    }

                    // Recurse through elements, and put ids of elements in return value
                    while (collIter.hasNext())
                    {
                        Object elem = collIter.next();
                        if (elem == null)
                        {
                            returnColl.add(null); // Allow for null elements
                        }
                        else
                        {
                            returnColl.add(getCacheableIdForId(api, elem));
                        }
                    }

                    // Put Collection<OID> in CachedPC
                    cachedPC.setFieldValue(fieldNumber, returnColl);
                    return;
                }
                catch (Exception e)
                {
                    NucleusLogger.CACHE.warn("Unable to create object of type " + value.getClass().getName() + " for L2 caching : " + e.getMessage());

                    // Contents not loaded so just mark as unloaded
                    cachedPC.setLoadedField(fieldNumber, false);
                    return;
                }
            }

            // Collection<Non-PC> so just return it
            cachedPC.setFieldValue(fieldNumber, value);
            return;
        }
        else if (mmd.hasArray())
        {
            // Array, maybe of Persistable objects
            if (MetaDataUtils.getInstance().storesPersistable(mmd, ec))
            {
                if (mmd.isSerialized() || mmd.isEmbedded() || mmd.getArray().isSerializedElement() || mmd.getArray().isEmbeddedElement())
                {
                    // TODO Support serialised/embedded elements
                    cachedPC.setLoadedField(fieldNumber, false);
                    return;
                }

                Object[] returnArr = new Object[Array.getLength(value)];
                for (int i=0;i<Array.getLength(value);i++)
                {
                    Object element = Array.get(value, i);
                    if (element != null)
                    {
                        returnArr[i] = getCacheableIdForId(api, element);
                    }
                    else
                    {
                        returnArr[i] = null;
                    }
                }

                // Store "id[]"
                cachedPC.setFieldValue(fieldNumber, returnArr);
                return;
            }

            // Array element type is not persistable so just return the value
            cachedPC.setFieldValue(fieldNumber, value);
            return;
        }
    }

    protected void processField(int fieldNumber, AbstractMemberMetaData mmd, Object value)
    {
        RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (relType == RelationType.NONE)
        {
            Object unwrappedValue =  value instanceof SCO ? ((SCO) value).getValue() : value;
            cachedPC.setFieldValue(fieldNumber, copyValue(unwrappedValue));
            return;
        }

        // 1-1, N-1 persistable field
        if (mmd.isSerialized() || MetaDataUtils.isMemberEmbedded(mmd, relType, ec.getClassLoaderResolver(), ec.getMetaDataManager()))
        {
            if (ec.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED))
            {
                // Put object in cached as (nested) CachedPC
                ObjectProvider valueOP = ec.findObjectProvider(value);
                int[] loadedFields = valueOP.getLoadedFieldNumbers();
                CachedPC valueCachedPC = new CachedPC(value.getClass(), valueOP.getLoadedFields(), null);
                if (loadedFields != null && loadedFields.length > 0)
                {
                    // Set the values of any fields that are loaded
                    valueOP.provideFields(loadedFields, new L2CachePopulateFieldManager(valueOP, valueCachedPC));
                }

                cachedPC.setFieldValue(fieldNumber, valueCachedPC);
            }
            else
            {
                // TODO Support serialised/embedded PC fields
                cachedPC.setLoadedField(fieldNumber, false);
            }
            return;
        }

        // Put cacheable form of the id in CachedPC
        cachedPC.setFieldValue(fieldNumber, getCacheableIdForId(ec.getApiAdapter(), value));
        return;
    }

    private Object getCacheableIdForId(ApiAdapter api, Object pc)
    {
        if (pc == null)
        {
            return null;
        }
        Object id = api.getIdForObject(pc);
        if (IdentityUtils.isDatastoreIdentity(id) || IdentityUtils.isSingleFieldIdentity(id))
        {
            return id;
        }

        // Doesn't store the class name, so wrap it in a CachedId
        return new CachedId(pc.getClass().getName(), id);
    }
}