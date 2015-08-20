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

import static org.datanucleus.cache.L2CacheRetrieveFieldManager.copyValue;

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
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.datanucleus.store.types.ContainerHandler;
import org.datanucleus.store.types.ElementContainerAdapter;
import org.datanucleus.store.types.MapContainerAdapter;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOContainer;
import org.datanucleus.store.types.SequenceAdapter;
import org.datanucleus.store.types.TypeManager;
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
        
        RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());
        
        if (relType == RelationType.NONE)
        {
            // Container<Non-PC> so just return it
            // TODO Renato Shouldn't we copy it here unless the element SCO type is immutable?
            cachedPC.setFieldValue(fieldNumber, unwrappedContainer);
        } 
        else 
        {
            TypeManager typeManager = op.getExecutionContext().getTypeManager();
            ContainerHandler containerHandler = typeManager.getContainerHandler(mmd.getType());

            if (mmd.hasMap())
            {
                processMapContainer(fieldNumber, unwrappedContainer, mmd, containerHandler);
            }
            else
            {
                processElementContainer(fieldNumber, unwrappedContainer, mmd, containerHandler);
            }
        }
    }

    private void processMapContainer(int fieldNumber, Object mapContainer, AbstractMemberMetaData mmd,
            ContainerHandler<Object, MapContainerAdapter<Object>> containerHandler)
    {
        if (containerHandler.isSerialised(mmd) || containerHandler.isEmbedded(mmd))
        {
            // TODO Support serialised/embedded elements
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        try
        {
            ApiAdapter api = ec.getApiAdapter();
            MapContainerAdapter<Object> mapToCacheAdapter = containerHandler.getAdapter(containerHandler.newContainer(mmd));

            boolean keyIsPersistent = mmd.getMap().keyIsPersistent();
            boolean valueIsPersistent = mmd.getMap().valueIsPersistent();

            for (Entry<Object, Object> entry : containerHandler.getAdapter(mapContainer).entries())
            {
                Object mapKey = keyIsPersistent ? getCacheableIdForId(api, entry.getKey()) : entry.getKey();
                Object mapValue = valueIsPersistent ? getCacheableIdForId(api, entry.getValue()) : entry.getValue();

                mapToCacheAdapter.put(mapKey, mapValue);
            }

            // Put Map<X, Y> in CachedPC where X, Y can be OID if they are persistable objects
            cachedPC.setFieldValue(fieldNumber, mapToCacheAdapter.getContainer());
        }
        catch (Exception e)
        {
            NucleusLogger.CACHE.warn("Unable to create object of type " + mapContainer.getClass().getName() + " for L2 caching : " + e.getMessage());

            // Contents not loaded so just mark as unloaded
            cachedPC.setLoadedField(fieldNumber, false);
        }
    }

    private void processElementContainer(int fieldNumber, Object container, AbstractMemberMetaData mmd,
            ContainerHandler<Object, ElementContainerAdapter<Object>> containerHandler)
    {
        if (containerHandler.isSerialised(mmd) || containerHandler.isEmbedded(mmd))
        {
            // TODO Support serialised/embedded elements
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }
        
        ElementContainerAdapter containerAdapter = containerHandler.getAdapter(container);

        if (containerAdapter instanceof SequenceAdapter && mmd.getOrderMetaData() != null && !mmd.getOrderMetaData().isIndexedList())
        {
            // Ordered list so don't cache since dependent on datastore-retrieve order
            cachedPC.setLoadedField(fieldNumber, false);
            return;
        }

        try
        {
            // TODO Renato: Maybe store always as an array?
            // TODO Renato: if (container.getClass().isInterface())?? isn't container always a concrete class
            // here?

            ElementContainerAdapter containerToCacheAdapter = containerHandler.getAdapter(containerHandler.newContainer(mmd));
            ApiAdapter api = ec.getApiAdapter();
            // Recurse through elements, and put ids of elements in return value
            for (Object element : containerAdapter)
            {
                containerToCacheAdapter.add(getCacheableIdForId(api, element));
            }

            // Put Container<OID> in CachedPC
            cachedPC.setFieldValue(fieldNumber, containerToCacheAdapter.getContainer());
        }
        catch (Exception e)
        {
            NucleusLogger.CACHE.warn("Unable to create object of type " + container.getClass().getName() + " for L2 caching : " + e.getMessage());

            // Contents not loaded so just mark as unloaded
            cachedPC.setLoadedField(fieldNumber, false);
        }
    }
    
    private void processField(int fieldNumber, Object value, AbstractMemberMetaData mmd)
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
                valueOP.provideFields(loadedFields, new L2CachePopulateFieldManager(valueOP, valueCachedPC));

                cachedPC.setFieldValue(fieldNumber, valueCachedPC);
            }
            else
            {
                // TODO Support serialised/embedded PC fields
                cachedPC.setLoadedField(fieldNumber, false);
            }
            return;
        }
        
        ApiAdapter api = ec.getApiAdapter();
        // Put cacheable form of the id in CachedPC
        cachedPC.setFieldValue(fieldNumber, getCacheableIdForId(api, value));
    }
}