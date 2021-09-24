/**********************************************************************
Copyright (c) 2005 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.store.types.wrappers.backed;

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.flush.MapClearOperation;
import org.datanucleus.flush.MapPutOperation;
import org.datanucleus.flush.MapRemoveOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.BackedSCOStoreManager;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.scostore.MapStore;
import org.datanucleus.store.types.scostore.Store;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class Properties object. Backed by a MapStore object.
 * The key and value types of this class is {@link java.lang.String}.
 */
public class Properties extends org.datanucleus.store.types.wrappers.Properties implements BackedSCO
{
    protected transient MapStore backingStore;
    protected transient boolean allowNulls = false;
    protected transient boolean useCache = true;
    protected transient boolean isCacheLoaded = false;

    /**
     * Constructor
     * @param sm the owner StateManager
     * @param mmd Metadata for the member
     */
    public Properties(DNStateManager sm, AbstractMemberMetaData mmd)
    {
        super(sm, mmd);

        // Set up our "delegate"
        this.delegate = new java.util.Properties();
        this.useCache = SCOUtils.useContainerCache(ownerSM, mmd);

        if (!SCOUtils.mapHasSerialisedKeysAndValues(mmd) && mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            this.backingStore = (MapStore)((BackedSCOStoreManager)ownerSM.getStoreManager()).getBackingStoreForField(ownerSM.getExecutionContext().getClassLoaderResolver(), 
                mmd, java.util.Map.class);
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(SCOUtils.getContainerInfoMessage(ownerSM, ownerMmd.getName(), this, useCache, allowNulls, SCOUtils.useCachedLazyLoading(ownerSM, ownerMmd)));
        }
    }

    public void initialise(java.util.Properties newValue, Object oldValue)
    {
        if (newValue != null)
        {
            // Check for the case of serialised maps, and assign StateManagers to any PC keys/values without
            if (SCOUtils.mapHasSerialisedKeysAndValues(ownerMmd) &&
                (ownerMmd.getMap().keyIsPersistent() || ownerMmd.getMap().valueIsPersistent()))
            {
                ExecutionContext ec = ownerSM.getExecutionContext();
                Iterator iter = newValue.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry entry = (Map.Entry)iter.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (ownerMmd.getMap().keyIsPersistent())
                    {
                        DNStateManager objSM = ec.findStateManager(key);
                        if (objSM == null)
                        {
                            objSM = ec.getNucleusContext().getStateManagerFactory().newForEmbedded(ec, key, false, ownerSM, ownerMmd.getAbsoluteFieldNumber());
                        }
                    }
                    if (ownerMmd.getMap().valueIsPersistent())
                    {
                        DNStateManager objSM = ec.findStateManager(value);
                        if (objSM == null)
                        {
                            objSM = ec.getNucleusContext().getStateManagerFactory().newForEmbedded(ec, value, false, ownerSM, ownerMmd.getAbsoluteFieldNumber());
                        }
                    }
                }
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023008", ownerSM.getObjectAsPrintable(), ownerMmd.getName(), "" + newValue.size()));
            }

            if (useCache)
            {
                // Load up old values into delegate as starting point
                java.util.Map oldMap = (java.util.Map)oldValue;
                if (oldMap != null)
                {
                    delegate.putAll(oldMap);
                }
                isCacheLoaded = true;

                SCOUtils.updateMapWithMapKeysValues(ownerSM.getExecutionContext().getApiAdapter(), this, newValue);
            }
            else
            {
                // TODO This is clear+putAll. Improve it to work out what is changed using oldValue
                if (backingStore != null)
                {
                    if (SCOUtils.useQueuedUpdate(ownerSM))
                    {
                        // If not yet flushed to store then no need to add to queue (since will be handled via insert)
                        if (ownerSM.isFlushedToDatastore() || !ownerSM.getLifecycleState().isNew())
                        {
                            ownerSM.getExecutionContext().addOperationToQueue(new MapClearOperation(ownerSM, backingStore));

                            Iterator iter = newValue.entrySet().iterator();
                            while (iter.hasNext())
                            {
                                java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
                                ownerSM.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerSM, backingStore, entry.getKey(), entry.getValue()));
                            }
                        }
                    }
                    else
                    {
                        backingStore.clear(ownerSM);
                        backingStore.putAll(ownerSM, newValue, Collections.emptyMap());
                    }
                }
                delegate.putAll(newValue);
                isCacheLoaded = true;
                makeDirty();
            }
        }
    }

    /**
     * Method to initialise the SCO from an existing value.
     * @param m Object to set value using.
     */
    public void initialise(java.util.Properties m)
    {
        if (m != null)
        {
            // Check for the case of serialised maps, and assign StateManagers to any PC keys/values without
            if (SCOUtils.mapHasSerialisedKeysAndValues(ownerMmd) && (ownerMmd.getMap().keyIsPersistent() || ownerMmd.getMap().valueIsPersistent()))
            {
                ExecutionContext ec = ownerSM.getExecutionContext();
                Iterator iter = m.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry entry = (Map.Entry)iter.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (ownerMmd.getMap().keyIsPersistent())
                    {
                        DNStateManager objSM = ec.findStateManager(key);
                        if (objSM == null)
                        {
                            objSM = ec.getNucleusContext().getStateManagerFactory().newForEmbedded(ec, key, false, ownerSM, ownerMmd.getAbsoluteFieldNumber());
                        }
                    }
                    if (ownerMmd.getMap().valueIsPersistent())
                    {
                        DNStateManager objSM = ec.findStateManager(value);
                        if (objSM == null)
                        {
                            objSM = ec.getNucleusContext().getStateManagerFactory().newForEmbedded(ec, value, false, ownerSM, ownerMmd.getAbsoluteFieldNumber());
                        }
                    }
                }
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023007", ownerSM.getObjectAsPrintable(), ownerMmd.getName(), "" + m.size()));
            }

            delegate.putAll(m);
            isCacheLoaded = true;
        }
    }

    /**
     * Method to initialise the SCO for use.
     */
    public void initialise()
    {
        if (useCache && !SCOUtils.useCachedLazyLoading(ownerSM, ownerMmd))
        {
            // Load up the container now if not using lazy loading
            loadFromStore();
        }
    }

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.Properties getValue()
    {
        loadFromStore();
        return super.getValue();
    }

    /**
     * Method to effect the load of the data in the SCO.
     * Used when the SCO supports lazy-loading to tell it to load all now.
     */
    public void load()
    {
        if (useCache)
        {
            loadFromStore();
        }
    }

    /**
     * Method to return if the SCO has its contents loaded.
     * If the SCO doesn't support lazy loading will just return true.
     * @return Whether it is loaded
     */
    public boolean isLoaded()
    {
        return useCache ? isCacheLoaded : false;
    }

    /**
     * Method to load all elements from the "backing store" where appropriate.
     */
    protected void loadFromStore()
    {
        if (backingStore != null && !isCacheLoaded)
        {
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023006", 
                    ownerSM.getObjectAsPrintable(), ownerMmd.getName()));
            }
            delegate.clear();

            // Populate the delegate with the keys/values from the store
            SCOUtils.populateMapDelegateWithStoreData(delegate, backingStore, ownerSM);

            isCacheLoaded = true;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.backed.BackedSCO#getBackingStore()
     */
    public Store getBackingStore()
    {
        return backingStore;
    }

    /**
     * Method to update an embedded key in this map.
     * @param key The key
     * @param fieldNumber Number of field in the key
     * @param newValue New value for this field
     * @param makeDirty Whether to make the SCO field dirty.
     */
    public void updateEmbeddedKey(Object key, int fieldNumber, Object newValue, boolean makeDirty)
    {
        if (backingStore != null)
        {
            backingStore.updateEmbeddedKey(ownerSM, key, fieldNumber, newValue);
        }
    }

    /**
     * Method to update an embedded value in this map.
     * @param value The value
     * @param fieldNumber Number of field in the value
     * @param newValue New value for this field
     * @param makeDirty Whether to make the SCO field dirty.
     */
    public void updateEmbeddedValue(Object value, int fieldNumber, Object newValue, boolean makeDirty)
    {
        if (backingStore != null)
        {
            backingStore.updateEmbeddedValue(ownerSM, value, fieldNumber, newValue);
        }
    }

    /**
     * Method to unset the owner and field details.
     **/
    public void unsetOwner()
    {
        super.unsetOwner();
        if (backingStore != null)
        {
            backingStore = null;
        }
    }

    // ------------------ Implementation of Hashtable methods ------------------
    
    /**
     * Creates and returns a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return The cloned object
     */
    public synchronized Object clone()
    {
        if (useCache)
        {
            loadFromStore();
        }

        return delegate.clone();
    }
    
    /**
     * Method to return if the map contains this key
     * @param key The key
     * @return Whether it is contained
     **/
    public synchronized boolean containsKey(Object key)
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.containsKey(key);
        }
        else if (backingStore != null)
        {
            return backingStore.containsKey(ownerSM, key);
        }
        
        return delegate.containsKey(key);
    }
    
    /**
     * Method to return if the map contains this value.
     * @param value The value
     * @return Whether it is contained
     **/
    public boolean containsValue(Object value)
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.containsValue(value);
        }
        else if (backingStore != null)
        {
            return backingStore.containsValue(ownerSM, value);
        }
        
        return delegate.containsValue(value);
    }
    
    /**
     * Accessor for the set of entries in the Map.
     * @return Set of entries
     **/
    public java.util.Set entrySet()
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return new Set(ownerSM, ownerMmd, false, backingStore.entrySetStore());
        }
        
        return delegate.entrySet();
    }
    
    /**
     * Method to check the equality of this map, and another.
     * @param o The map to compare against.
     * @return Whether they are equal.
     **/
    public synchronized boolean equals(Object o)
    {
        if (useCache)
        {
            loadFromStore();
        }
        
        if (o == this)
        {
            return true;
        }
        if (!(o instanceof java.util.Map))
        {
            return false;
        }
        java.util.Map m = (java.util.Map)o;
        
        return entrySet().equals(m.entrySet());
    }

    /**
     * Accessor for the value stored against a key.
     * @param key The key
     * @return The value.
     */
    public synchronized Object get(Object key)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.get(ownerSM, key);
        }
        
        return delegate.get(key);
    }

    /**
     * Accessor for the string value stored against a string key.
     * @param key The key
     * @return The value.
     */
    public synchronized String getProperty(String key)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            Object val = backingStore.get(ownerSM, key);
            String strVal = (val instanceof String) ? (String)val : null;
            return ((strVal == null) && (defaults != null)) ? defaults.getProperty(key) : strVal;
        }

        return delegate.getProperty(key);
    }
    
    /**
     * Method to generate a hashcode for this Map.
     * @return The hashcode.
     **/
    public synchronized int hashCode()
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            int h = 0;
            Iterator i = entrySet().iterator();
            while (i.hasNext())
            {
                h += i.next().hashCode();
            }
            
            return h;
        }
        return delegate.hashCode();
    }
    
    /**
     * Method to return if the Map is empty.
     * @return Whether it is empty.
     **/
    public synchronized boolean isEmpty()
    {
        return size() == 0;
    }
    
    /**
     * Accessor for the set of keys in the Map.
     * @return Set of keys.
     **/
    public java.util.Set keySet()
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return new Set(ownerSM, ownerMmd, false, backingStore.keySetStore());
        }
        
        return delegate.keySet();
    }
    
    /**
     * Method to return the size of the Map.
     * @return The size
     **/
    public synchronized int size()
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.size();
        }
        else if (backingStore != null)
        {
            return backingStore.entrySetStore().size(ownerSM);
        }
        
        return delegate.size();
    }
    
    /**
     * Accessor for the set of values in the Map.
     * @return Set of values.
     **/
    public Collection values()
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return new org.datanucleus.store.types.wrappers.backed.Collection(ownerSM, ownerMmd, true, backingStore.valueCollectionStore());
        }
        
        return delegate.values();
    }
    
    // -------------------------------- Mutator methods ------------------------
    
    /**
     * Method to clear the Hashtable
     **/
    public synchronized void clear()
    {
        makeDirty();
        delegate.clear();
        
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new MapClearOperation(ownerSM, backingStore));
            }
            else
            {
                backingStore.clear(ownerSM);
            }
        }

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
    }
    
    /**
     * Method to add a value against a key to the Hashtable
     * @param key The key
     * @param value The value
     * @return The previous value for the specified key.
     **/
    public synchronized Object put(Object key,Object value)
    {
        // Reject inappropriate elements
        if (!allowNulls)
        {
            if (key == null)
            {
                throw new NullPointerException("Nulls not allowed for map at field " + ownerMmd.getName() + " but key is null");
            }
            if (value == null)
            {
                throw new NullPointerException("Nulls not allowed for map at field " + ownerMmd.getName() + " but value is null");
            }
        }

        if (useCache)
        {
            // Make sure we have all values loaded (e.g if in optimistic tx and we put new entry)
            loadFromStore();
        }

        makeDirty();

        Object oldValue = null;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerSM, backingStore, key, value));
            }
            else if (useCache)
            {
                oldValue = delegate.get(key);
                backingStore.put(ownerSM, key, value, oldValue, delegate.containsKey(key));
            }
            else
            {
                oldValue = backingStore.put(ownerSM, key, value);
            }
        }
        Object delegateOldValue = delegate.put(key, value);
        if (backingStore == null)
        {
            oldValue = delegateOldValue;
        }
        else if (SCOUtils.useQueuedUpdate(ownerSM))
        {
            oldValue = delegateOldValue;
        }

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return oldValue;
    }
    
    /**
     * Method to add the specified Map's values under their keys here.
     * @param m The map
     **/
    public synchronized void putAll(java.util.Map m)
    {
        makeDirty();

        if (useCache)
        {
            // Make sure we have all values loaded (e.g if in optimistic tx and we put new entry)
            loadFromStore();
        }

        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                Iterator iter = m.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry entry = (Map.Entry)iter.next();
                    ownerSM.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerSM, backingStore, entry.getKey(), entry.getValue()));
                }
            }
            else
            {
                if (useCache)
                {
                    backingStore.putAll(ownerSM, m, Collections.unmodifiableMap(delegate));
                }
                else
                {
                    backingStore.putAll(ownerSM, m);
                }
            }
        }
        delegate.putAll(m);

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
    }
    
    /**
     * Method to remove the value for a key from the Hashtable
     * @param key The key to remove
     * @return The value that was removed from this key.
     **/
    public synchronized Object remove(Object key)
    {
        makeDirty();

        if (useCache)
        {
            // Make sure we have all values loaded (e.g if in optimistic tx and we put new entry)
            loadFromStore();
        }

        Object removed = null;
        Object delegateRemoved = delegate.remove(key);
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new MapRemoveOperation(ownerSM, backingStore, key, delegateRemoved));
                removed = delegateRemoved;
            }
            else if (useCache)
            {
                backingStore.remove(ownerSM, key, delegateRemoved);
                removed = delegateRemoved;
            }
            else
            {
                removed = backingStore.remove(ownerSM, key);
            }
        }
        else
        {
            removed = delegateRemoved;
        }

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return removed;
    }

    /**
     * Method to add a string value against a string key to the Hashtable
     * @param key The key
     * @param value The value
     * @return The previous value for the specified key.
     */
    public synchronized Object setProperty(String key, String value)
    {
        return put(key, value);
    }

    /**
     * The writeReplace method is called when ObjectOutputStream is preparing
     * to write the object to the stream. The ObjectOutputStream checks whether
     * the class defines the writeReplace method. If the method is defined, the
     * writeReplace method is called to allow the object to designate its
     * replacement in the stream. The object returned should be either of the
     * same type as the object passed in or an object that when read and 
     * resolved will result in an object of a type that is compatible with all
     * references to the object.
     * 
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        if (useCache)
        {
            loadFromStore();
            return new java.util.Hashtable(delegate);
        }

        // TODO Cater for non-cached map, load elements in a DB call.
        return new java.util.Hashtable(delegate);
    }
}