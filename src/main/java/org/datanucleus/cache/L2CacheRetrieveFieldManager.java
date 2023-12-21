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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistableObjectType;
import org.datanucleus.PropertyNames;
import org.datanucleus.cache.CachedPC.CachedId;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.state.StateManagerImpl;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.containers.ContainerHandler;
import org.datanucleus.store.types.containers.ElementContainerAdapter;
import org.datanucleus.store.types.containers.MapContainerAdapter;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * FieldManager responsible for retrieving the values from the provided CachedPC object.
 */
public class L2CacheRetrieveFieldManager extends AbstractFieldManager
{
    /** StateManager of the object we are copying values into. */
    protected final DNStateManager sm;

    /** Execution Context. */
    protected final ExecutionContext ec;

    /** CachedPC that we are taking values from. */
    protected final CachedPC cachedPC;

    protected List<Integer> fieldsNotLoaded = null;

    public L2CacheRetrieveFieldManager(DNStateManager sm, CachedPC cachedpc)
    {
        this.sm = sm;
        this.ec = sm.getExecutionContext();
        this.cachedPC = cachedpc;
    }

    public int[] getFieldsNotLoaded()
    {
        if (fieldsNotLoaded == null)
        {
            return null;
        }
        return fieldsNotLoaded.stream().mapToInt(i->i).toArray();
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
        Object value = cachedPC.getFieldValue(fieldNumber);
        if (value == null)
        {
            return null;
        }

        AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        return mmd.hasContainer() ? processContainerField(mmd, value) : processField(mmd, value);
    }

    protected Object processContainerField(AbstractMemberMetaData mmd, Object container)
    {
        ContainerHandler containerHandler = sm.getExecutionContext().getTypeManager().getContainerHandler(mmd.getType());
        return mmd.hasMap() ? processMapContainer(mmd, container, containerHandler) : processElementContainer(mmd, container, containerHandler);
    }

    protected Object processMapContainer(AbstractMemberMetaData mmd, Object cachedMapContainer, ContainerHandler<Object, MapContainerAdapter<Object>> containerHandler)
    {
        // Map field, with fieldValue being Map<OID, OID>
        try
        {
            // Create Map<Key, Value> of same type as fieldValue
            MapContainerAdapter<Object> cachedMapContainerAdapter = containerHandler.getAdapter(cachedMapContainer);
            Object newContainer = newContainer(cachedMapContainer, mmd, containerHandler);
            MapContainerAdapter fieldMapContainerAdapter = containerHandler.getAdapter(newContainer);
            boolean keyIsPersistent = mmd.getMap().keyIsPersistent();
            boolean keyIsEmbedded = mmd.getMap().isEmbeddedKey();
            boolean keyIsSerialised = mmd.getMap().isSerializedKey();
            boolean valueIsPersistent = mmd.getMap().valueIsPersistent();
            boolean valueIsEmbedded = mmd.getMap().isEmbeddedValue();
            boolean valueIsSerialised = mmd.getMap().isSerializedValue();
            for (Entry<Object, Object> entry : cachedMapContainerAdapter.entries())
            {
                Object mapKey = null;
                if (keyIsPersistent)
                {
                    if (keyIsEmbedded || keyIsSerialised || mmd.isSerialized())
                    {
                        mapKey = convertCachedPCToPersistable((CachedPC)entry.getKey(), mmd.getAbsoluteFieldNumber(), PersistableObjectType.EMBEDDED_MAP_KEY_PC);
                    }
                    else
                    {
                        mapKey = getObjectFromCachedId(entry.getKey());
                    }
                }
                else
                {
                    mapKey = entry.getKey();
                }

                Object mapValue = null;
                Object mapValueId = entry.getValue();
                if (mapValueId != null)
                {
                    if (valueIsPersistent)
                    {
                        if (valueIsEmbedded || valueIsSerialised || mmd.isSerialized())
                        {
                            mapValue = convertCachedPCToPersistable((CachedPC)entry.getValue(), mmd.getAbsoluteFieldNumber(), PersistableObjectType.EMBEDDED_MAP_VALUE_PC);
                        }
                        else
                        {
                            mapValue = getObjectFromCachedId(entry.getValue());
                        }
                    }
                    else
                    {
                        mapValue = entry.getValue();
                    }
                }

                fieldMapContainerAdapter.put(mapKey, mapValue);
            }

            return SCOUtils.wrapSCOField(sm, mmd.getAbsoluteFieldNumber(), fieldMapContainerAdapter.getContainer(), true);
        }
        catch (Exception e)
        {
            // Error creating field value
            if (fieldsNotLoaded == null)
            {
                fieldsNotLoaded = new ArrayList<Integer>();
            }
            fieldsNotLoaded.add(mmd.getAbsoluteFieldNumber());
            NucleusLogger.CACHE.error("Exception thrown creating value for" + " field " + mmd.getFullFieldName() + " of type " + cachedMapContainer.getClass().getName(), e);
     
            return null;
        }
    }

    protected Object processElementContainer(AbstractMemberMetaData mmd, Object cachedContainer, ContainerHandler<Object, ElementContainerAdapter<Object>> containerHandler)
    {
        try
        {
            // For arrays, rely on the metadata value
            Object newContainer = mmd.hasArray() ? containerHandler.newContainer(mmd) : newContainer(cachedContainer, mmd, containerHandler);
            ElementContainerAdapter<Object> fieldContainerAdapter = containerHandler.getAdapter(newContainer);
            RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());

            ElementContainerAdapter<Object> cachedContainerAdapter = containerHandler.getAdapter(cachedContainer);
            if (relType == RelationType.NONE)
            {
                String elementType = mmd.hasCollection() ? mmd.getCollection().getElementType() : mmd.getArray().getElementType();
                boolean mutableType = ec.getTypeManager().isSecondClassMutableType(elementType);
                
                if (mutableType)
                {
                    // Container<mutable-SCO> - Create the container with a copy of the SCO mutable values
                    for (Object mutableValue : cachedContainerAdapter)
                    {
                        // TODO Need to return the value wrapped?
                        fieldContainerAdapter.add(SCOUtils.copyValue(mutableValue));
                    }
                }
                else
                {
                    // Container<immutable-SCO> - e.g. List<String> Create the container reusing the immutable object values
                    for (Object value : cachedContainerAdapter)
                    {
                        fieldContainerAdapter.add(value);
                    }
                }
            }
            else
            {
                if ((containerHandler.isSerialised(mmd) || containerHandler.isEmbedded(mmd)) &&
                    ec.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED))
                {
                    // Collection/array of embedded elements (stored as nested CachedPC)
                    for (Object cachedObject : cachedContainerAdapter)
                    {
                        CachedPC elementCachedPC = (CachedPC)cachedObject;
                        Object element = null;
                        if (elementCachedPC != null)
                        {
                            // Convert the CachedPC back into a managed object loading all cached fields
                            element = convertCachedPCToPersistable(elementCachedPC, mmd.getAbsoluteFieldNumber(), PersistableObjectType.EMBEDDED_COLLECTION_ELEMENT_PC);
                        }

                        fieldContainerAdapter.add(element);
                    }
                }
                else
                {
                    // Collection/array of embedded elements (stored as "id")
                    for (Object cachedId : cachedContainerAdapter)
                    {
                        Object element = cachedId == null ? null : getObjectFromCachedId(cachedId);  
                        fieldContainerAdapter.add(element);
                    }
                }
            }   
            
            return SCOUtils.wrapSCOField(sm, mmd.getAbsoluteFieldNumber(), fieldContainerAdapter.getContainer(), true);
        }
        catch (Exception e)
        {
            // Error creating field value
            if (fieldsNotLoaded == null)
            {
                fieldsNotLoaded = new ArrayList<Integer>();
            }
            fieldsNotLoaded.add(mmd.getAbsoluteFieldNumber());
            NucleusLogger.CACHE.error("Exception thrown creating value for field " + mmd.getFullFieldName() + " of type " + cachedContainer.getClass().getName(), e);
            return null;
        }
    }

    protected Object processField(AbstractMemberMetaData mmd, Object value)
    {
        RelationType relType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (relType == RelationType.NONE)
        {
            return SCOUtils.wrapSCOField(sm, mmd.getAbsoluteFieldNumber(), SCOUtils.copyValue(value), true);
        }
        
        if (mmd.isSerialized() || MetaDataUtils.isMemberEmbedded(mmd, relType, ec.getClassLoaderResolver(), ec.getMetaDataManager()))
        {
            if (ec.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED))
            {
                if (value instanceof CachedPC)
                {
                    // Convert the CachedPC back into a managed object loading all cached fields
                    return convertCachedPCToPersistable((CachedPC)value, mmd.getAbsoluteFieldNumber(), PersistableObjectType.EMBEDDED_PC);
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
            fieldsNotLoaded.add(mmd.getAbsoluteFieldNumber());
            return null;
        }
    }

    protected Object getObjectFromCachedId(Object cachedId)
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
            pcClassName = IdentityUtils.getTargetClassNameForIdentity(pcId);
        }
        Class pcCls = ec.getClassLoaderResolver().classForName(pcClassName);
        return ec.findObject(pcId, null, pcCls, false, false);
    }
    
    /**
     * Copy container without using the container handler and metadata type info. 
     * Calling newContainer from container handler for interfaces will return the default chosen implementation, but this causes the JDO TCK
     * (TestCollectionCollections) to fail because it expects Collection fields to return the same or at most a List.
     */
    static <T> T newContainer(Object container, AbstractMemberMetaData mmd, ContainerHandler containerHandler)
    {
        try
        {
            return (T)container.getClass().getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            // Fallback for containers that don't have a default constructor
            return (T)containerHandler.newContainer(mmd);
        }
    }

    /**
     * Method to convert a nested (i.e embedded) CachedPC back to the persistable object it represents.
     * @param cachedPC The CachedPC
     * @param memberNumber Member number in the owning object where this is stored
     * @param objectType Type of object that is embedded/serialised
     * @return The (persistable) object
     */
    protected Object convertCachedPCToPersistable(CachedPC cachedPC, int memberNumber, PersistableObjectType objectType)
    {
        AbstractClassMetaData valueCmd = ec.getMetaDataManager().getMetaDataForClass(cachedPC.getObjectClass(), ec.getClassLoaderResolver());
        DNStateManager valueSM = ec.getNucleusContext().getStateManagerFactory().newForEmbedded(ec, valueCmd, sm, memberNumber, objectType);

        // TODO Perhaps only load fetch plan fields?
        int[] fieldsToLoad = ClassUtils.getFlagsSetTo(cachedPC.getLoadedFields(), valueCmd.getAllMemberPositions(), true);
        if (fieldsToLoad != null && fieldsToLoad.length > 0)
        {
            valueSM.replaceFields(fieldsToLoad, constructNew(valueSM, cachedPC));
        }
        return valueSM.getObject();
    }

    protected L2CacheRetrieveFieldManager constructNew(DNStateManager valueSM, CachedPC cachedPC) {
        if (valueSM instanceof StateManagerImpl) {
            return ((StateManagerImpl) valueSM).constructL2CacheRetrieveFieldManager(valueSM, cachedPC);
        }
        return new L2CacheRetrieveFieldManager(valueSM, cachedPC);
    }
}