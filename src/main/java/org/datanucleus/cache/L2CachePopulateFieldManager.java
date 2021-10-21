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

import static org.datanucleus.cache.L2CacheRetrieveFieldManager.newContainer;

import java.util.Map.Entry;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.cache.CachedPC.CachedId;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOContainer;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.containers.ElementContainerAdapter;
import org.datanucleus.store.types.containers.ElementContainerHandler;
import org.datanucleus.store.types.containers.MapContainerAdapter;
import org.datanucleus.store.types.containers.MapHandler;
import org.datanucleus.store.types.containers.SequenceAdapter;
import org.datanucleus.util.NucleusLogger;

/**
 * FieldManager responsible for populating the provided CachedPC object.
 */
public class L2CachePopulateFieldManager extends AbstractFieldManager
{
    private static final Object[] EMPTY_ARRAY = {};

    /** StateManager of the object we are copying values from. */
    DNStateManager sm;

    ExecutionContext ec;

    /** CachedPC that we are copying values into. */
    CachedPC cachedPC;

    public L2CachePopulateFieldManager(DNStateManager sm, CachedPC cachedpc)
    {
        this.sm = sm;
        this.ec = sm.getExecutionContext();
        this.cachedPC = cachedpc;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.fieldmanager.AbstractFieldManager#storeBooleanField(int, boolean)
     */
    @Override
    public void storeBooleanField(int fieldNumber, boolean value)
    {
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        }
        else if (mmd.hasContainer())
        {
            // 1-N, M-N Container
            processContainer(fieldNumber, value, mmd);
        }
        else
        {
            processField(fieldNumber, value, mmd);
        }
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
    
    private void processContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd)
    {
        Object unwrappedContainer = container;
        if (container instanceof SCOContainer)
        {
            if (!((SCOContainer)container).isLoaded())
            {
                // Contents not loaded so just mark as unloaded
                cachedPC.setLoadedField(fieldNumber, false);
                return;
            }
     
            unwrappedContainer = ((SCO)container).getValue();
        }
         
        TypeManager typeManager = sm.getExecutionContext().getTypeManager();

        if (mmd.hasMap())
        {
            MapHandler mapHandler = typeManager.getContainerHandler(mmd.getType());
            processMapContainer(fieldNumber, unwrappedContainer, mmd, mapHandler);
        }
        else
        {
            ElementContainerHandler elementContainerHandler = typeManager.getContainerHandler(mmd.getType());
            processElementContainer(fieldNumber, unwrappedContainer, mmd, elementContainerHandler);
        }
    }

    private void processMapContainer(int fieldNumber, Object mapContainer, AbstractMemberMetaData mmd, MapHandler<Object> containerHandler)
    {
        try
        {
            if (!ec.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED))
            {
                RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());
                if (relType != RelationType.NONE && (containerHandler.isSerialised(mmd) || containerHandler.isEmbedded(mmd)))
                {
                    // User has requested no caching of embedded/serialised, so ignore this field
                    cachedPC.setLoadedField(fieldNumber, false);
                    return;
                }
            }

            boolean keyIsPersistent = mmd.getMap().keyIsPersistent();
            boolean keyIsEmbedded = mmd.getMap().isEmbeddedKey();
            boolean keyIsSerialised = mmd.getMap().isSerializedKey();
            boolean valueIsPersistent = mmd.getMap().valueIsPersistent();
            boolean valueIsEmbedded = mmd.getMap().isEmbeddedValue();
            boolean valueIsSerialised = mmd.getMap().isSerializedValue();
            Object newContainer = newContainer(mapContainer, mmd, containerHandler);
            MapContainerAdapter<Object> mapToCacheAdapter = containerHandler.getAdapter(newContainer);
            ApiAdapter api = ec.getApiAdapter();
            for (Entry<Object, Object> entry : containerHandler.getAdapter(mapContainer).entries())
            {
                Object mapKey = null;
                if (keyIsPersistent)
                {
                    mapKey = (keyIsEmbedded || keyIsSerialised || mmd.isSerialized()) ? convertPersistableToCachedPC(entry.getKey()) : getCacheableIdForId(api, entry.getKey());
                }
                else
                {
                    mapKey = SCOUtils.copyValue(entry.getKey());
                }

                Object mapValue = null;
                if (valueIsPersistent)
                {
                    mapValue = (valueIsEmbedded || valueIsSerialised || mmd.isSerialized()) ? convertPersistableToCachedPC(entry.getValue()) : getCacheableIdForId(api, entry.getValue());
                }
                else
                {
                    mapValue = SCOUtils.copyValue(entry.getValue());
                }

                mapToCacheAdapter.put(mapKey, mapValue);
            }

            // Put Map<X, Y> in CachedPC where X, Y can be OID or CachedPC if they are persistable objects
            cachedPC.setFieldValue(fieldNumber, mapToCacheAdapter.getContainer());
        }
        catch (Exception e)
        {
            NucleusLogger.CACHE.warn("Unable to create object of type " + mapContainer.getClass().getName() + " for L2 caching", e);

            // Contents not loaded so just mark as unloaded
            cachedPC.setLoadedField(fieldNumber, false);
        }
    }

    private void processElementContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd, ElementContainerHandler<Object, ElementContainerAdapter<Object>> containerHandler)
    {
        RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (relType == RelationType.NONE)
        {
            // Container<Non-PC>
            boolean isContainerMutable = ec.getTypeManager().isSecondClassMutableType(mmd.getType().getName());
            String elementType = containerHandler.getElementTypeName(mmd.getContainer());
            boolean isElementMutable = ec.getTypeManager().isSecondClassMutableType(elementType);

            if (isContainerMutable || isElementMutable)
            {
                ElementContainerAdapter<Object> adapterToCache = containerHandler.getAdapter(newContainer(container, mmd, containerHandler));

                if (isElementMutable)
                {
                    for (Object elementSCO : containerHandler.getAdapter(container))
                    {
                        adapterToCache.add(SCOUtils.copyValue(elementSCO));
                    }
                }
                else
                {
                    for (Object element : containerHandler.getAdapter(container))
                    {
                        adapterToCache.add(element);
                    }
                }
                cachedPC.setFieldValue(fieldNumber, adapterToCache.getContainer());
            }
            else
            {
                // Both container and element are immutable so we can just cache it as it is
                cachedPC.setFieldValue(fieldNumber, container);
            }
        }
        else
        {
            // Container<PC>
            ElementContainerAdapter containerAdapter = containerHandler.getAdapter(container);
            if (containerAdapter instanceof SequenceAdapter && mmd.getOrderMetaData() != null && !mmd.getOrderMetaData().isIndexedList())
            {
                // Ordered list so don't cache since dependent on datastore-retrieve order
                cachedPC.setLoadedField(fieldNumber, false);
                return;
            }

            try
            {
                // For arrays[PC] just use an Object array to store the ids because we can use the same type of the original container. 
                // Later when restoring the values it will be based on the metadata instead of the actual container type, as opposed to how it's done for non-array containers.
                Object newContainer = mmd.hasArray() ? EMPTY_ARRAY : newContainer(container, mmd, containerHandler);
                ElementContainerAdapter containerToCacheAdapter = containerHandler.getAdapter(newContainer);
                ApiAdapter api = ec.getApiAdapter();

                if ((containerHandler.isSerialised(mmd) || containerHandler.isEmbedded(mmd)) &&
                    !ec.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED))
                {
                    // User not caching embedded/serialised
                    cachedPC.setLoadedField(fieldNumber, false);
                    return;
                }

                for (Object element : containerAdapter)
                {
                    if (containerHandler.isSerialised(mmd) || containerHandler.isEmbedded(mmd))
                    {
                        // Store embedded/serialised element as nested collection element
                        containerToCacheAdapter.add(convertPersistableToCachedPC(element));
                    }
                    else
                    {
                        // Store id of element, since cached in its own right
                        containerToCacheAdapter.add(getCacheableIdForId(api, element));
                    }
                }

                cachedPC.setFieldValue(fieldNumber, containerToCacheAdapter.getContainer());
            }
            catch (Exception e)
            {
                NucleusLogger.CACHE.warn("Unable to create object of type " + container.getClass().getName() + " for L2 caching : ", e);

                // Contents not loaded so just mark as unloaded
                cachedPC.setLoadedField(fieldNumber, false);
            }
        }
    }
    
    private void processField(int fieldNumber, Object value, AbstractMemberMetaData mmd)
    {
        RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());
        
        if (relType == RelationType.NONE)
        {
            Object unwrappedValue =  value instanceof SCO ? ((SCO) value).getValue() : value;
            cachedPC.setFieldValue(fieldNumber, SCOUtils.copyValue(unwrappedValue));
            return;
        }

        // 1-1, N-1 persistable field
        if (mmd.isSerialized() || MetaDataUtils.isMemberEmbedded(mmd, relType, ec.getClassLoaderResolver(), ec.getMetaDataManager()))
        {
            if (ec.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED))
            {
                // Put object in cached as (nested) CachedPC
                cachedPC.setFieldValue(fieldNumber, convertPersistableToCachedPC(value));
            }
            else
            {
                // User not caching embedded/serialised
                cachedPC.setLoadedField(fieldNumber, false);
            }
        }
        else
        {
            // Put cacheable form of the id in CachedPC
            cachedPC.setFieldValue(fieldNumber, getCacheableIdForId(ec.getApiAdapter(), value));
        }
    }

    /**
     * Method to convert an embedded/serialised object to a CachedPC object for L2 caching.
     * @param pc The persistable
     * @return The CachedPC that it is stored as
     */
    protected CachedPC convertPersistableToCachedPC(Object pc)
    {
        if (pc == null)
        {
            return null;
        }

        DNStateManager valueSM = ec.findStateManager(pc);
        CachedPC valueCachedPC = new CachedPC(pc.getClass(), valueSM.getLoadedFields(), null, null);

        int[] loadedFields = valueSM.getLoadedFieldNumbers();
        if (loadedFields != null && loadedFields.length > 0)
        {
            // Set the values of any fields that are loaded
            valueSM.provideFields(loadedFields, new L2CachePopulateFieldManager(valueSM, valueCachedPC));
        }

        return valueCachedPC;
    }
}