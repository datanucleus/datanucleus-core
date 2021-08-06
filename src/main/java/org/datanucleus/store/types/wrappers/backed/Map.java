/**********************************************************************
Copyright (c) 2002 Mike Martin and others. All rights reserved.
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
2003 Erik Bengtson  - removed ununsed imports
2003 Andy Jefferson - added missing methods
2004 Andy Jefferson - rewritten to always have delegate populated
2004 Andy Jefferson - changed to allow caching
2005 Andy Jefferson - allowed for serialised map
    ...
**********************************************************************/
package org.datanucleus.store.types.wrappers.backed;

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.datanucleus.ExecutionContext;
import org.datanucleus.flush.MapClearOperation;
import org.datanucleus.flush.MapPutOperation;
import org.datanucleus.flush.MapRemoveOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.BackedSCOStoreManager;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.scostore.MapStore;
import org.datanucleus.store.types.scostore.Store;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class Map object. 
 * Uses a "delegate" as a local store for the Map. Uses a "backing store" (SetStore) to represent the datastore. 
 * The "delegate" is updated with the "backing store" information at necessary intervals. 
 *
 * <H3>Modes of Operation</H3>
 * The user can operate the map in 2 modes.
 * The <B>cached</B> mode will use an internal cache of the elements (in the "delegate") reading them at the first opportunity and then using the cache thereafter.
 * The <B>non-cached</B> mode will just go direct to the "backing store" each call.
 *
 * <H3>Mutators</H3>
 * When the backing store is present any updates are passed direct to the datastore as well as to the "delegate". 
 * If the "backing store" isn't present the changes are made to the "delegate" only.
 *
 * <H3>Accessors</H3>
 * When any accessor method is invoked, it typically checks whether the map has been loaded from its backing store and does this as necessary. 
 * Some methods (<B>size()</B>, <B>containsKey()</B>) just check if everything is loaded and use the delegate if possible, otherwise going direct to the datastore.
 */
public class Map<K, V> extends org.datanucleus.store.types.wrappers.Map<K, V> implements BackedSCO
{
    protected transient boolean allowNulls = true;
    protected transient MapStore<K, V> backingStore;
    protected transient boolean useCache=true;
    protected transient boolean isCacheLoaded=false;

    /**
     * Constructor, using the ObjectProvider of the "owner" and the field name.
     * @param op The owner ObjectProvider
     * @param mmd Metadata for the member
     */
    public Map(ObjectProvider op, AbstractMemberMetaData mmd)
    {
        super(op, mmd);

        // Set up our "delegate"
        this.delegate = new java.util.HashMap();
        this.allowNulls = SCOUtils.allowNullsInContainer(allowNulls, mmd);
        this.useCache = SCOUtils.useContainerCache(ownerOP, mmd);

        if (!SCOUtils.mapHasSerialisedKeysAndValues(mmd) && mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            this.backingStore = (MapStore)((BackedSCOStoreManager)ownerOP.getStoreManager()).getBackingStoreForField(ownerOP.getExecutionContext().getClassLoaderResolver(), 
                mmd, java.util.Map.class);
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(SCOUtils.getContainerInfoMessage(ownerOP, ownerMmd.getName(), this, useCache, allowNulls, SCOUtils.useCachedLazyLoading(ownerOP, ownerMmd)));
        }
    }

    public void initialise(java.util.Map newValue, Object oldValue)
    {
        if (newValue != null)
        {
            // Check for the case of serialised maps, and assign ObjectProviders to any PC keys/values without
            if (SCOUtils.mapHasSerialisedKeysAndValues(ownerMmd) && (ownerMmd.getMap().keyIsPersistent() || ownerMmd.getMap().valueIsPersistent()))
            {
                ExecutionContext ec = ownerOP.getExecutionContext();
                Iterator iter = newValue.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry entry = (Map.Entry)iter.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (ownerMmd.getMap().keyIsPersistent())
                    {
                        ObjectProvider objSM = ec.findObjectProvider(key);
                        if (objSM == null)
                        {
                            objSM = ec.getNucleusContext().getObjectProviderFactory().newForEmbedded(ec, key, false, ownerOP, ownerMmd.getAbsoluteFieldNumber());
                        }
                    }
                    if (ownerMmd.getMap().valueIsPersistent())
                    {
                        ObjectProvider objSM = ec.findObjectProvider(value);
                        if (objSM == null)
                        {
                            objSM = ec.getNucleusContext().getObjectProviderFactory().newForEmbedded(ec, value, false, ownerOP, ownerMmd.getAbsoluteFieldNumber());
                        }
                    }
                }
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023008", ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + newValue.size()));
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

                SCOUtils.updateMapWithMapKeysValues(ownerOP.getExecutionContext().getApiAdapter(), this, newValue);
            }
            else
            {
                // TODO This is clear+putAll. Improve it to work out what is changed using oldValue
                if (backingStore != null)
                {
                    if (SCOUtils.useQueuedUpdate(ownerOP))
                    {
                        // If not yet flushed to store then no need to add to queue (since will be handled via insert)
                        if (ownerOP.isFlushedToDatastore() || !ownerOP.getLifecycleState().isNew())
                        {
                            ownerOP.getExecutionContext().addOperationToQueue(new MapClearOperation(ownerOP, backingStore));

                            Iterator iter = newValue.entrySet().iterator();
                            while (iter.hasNext())
                            {
                                java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
                                ownerOP.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerOP, backingStore, entry.getKey(), entry.getValue()));
                            }
                        }
                    }
                    else
                    {
                        backingStore.clear(ownerOP);
                        backingStore.putAll(ownerOP, newValue, Collections.emptyMap());
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
     * @param m The object to set from
     */
    public void initialise(java.util.Map m)
    {
        if (m != null)
        {
            // Check for the case of serialised maps, and assign ObjectProviders to any PC keys/values without
            if (SCOUtils.mapHasSerialisedKeysAndValues(ownerMmd) && (ownerMmd.getMap().keyIsPersistent() || ownerMmd.getMap().valueIsPersistent()))
            {
                ExecutionContext ec = ownerOP.getExecutionContext();
                Iterator iter = m.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry entry = (Map.Entry)iter.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (ownerMmd.getMap().keyIsPersistent())
                    {
                        ObjectProvider objSM = ec.findObjectProvider(key);
                        if (objSM == null)
                        {
                            objSM = ec.getNucleusContext().getObjectProviderFactory().newForEmbedded(ec, key, false, ownerOP, ownerMmd.getAbsoluteFieldNumber());
                        }
                    }
                    if (ownerMmd.getMap().valueIsPersistent())
                    {
                        ObjectProvider objSM = ec.findObjectProvider(value);
                        if (objSM == null)
                        {
                            objSM = ec.getNucleusContext().getObjectProviderFactory().newForEmbedded(ec, value, false, ownerOP, ownerMmd.getAbsoluteFieldNumber());
                        }
                    }
                }
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023007", ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + m.size()));
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
        if (useCache && !SCOUtils.useCachedLazyLoading(ownerOP, ownerMmd))
        {
            // Load up the container now if not using lazy loading
            loadFromStore();
        }
    }

    // ------------------------ Implementation of SCO methods ------------------

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.Map getValue()
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
                    ownerOP.getObjectAsPrintable(), ownerMmd.getName()));
            }
            delegate.clear();

            // Populate the delegate with the keys/values from the store
            SCOUtils.populateMapDelegateWithStoreData(delegate, backingStore, ownerOP);

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
    public void updateEmbeddedKey(K key, int fieldNumber, Object newValue, boolean makeDirty)
    {
        if (backingStore != null)
        {
            backingStore.updateEmbeddedKey(ownerOP, key, fieldNumber, newValue);
        }
    }

    /**
     * Method to update an embedded value in this map.
     * @param value The value
     * @param fieldNumber Number of field in the value
     * @param newValue New value for this field
     * @param makeDirty Whether to make the SCO field dirty.
     */
    public void updateEmbeddedValue(V value, int fieldNumber, Object newValue, boolean makeDirty)
    {
        if (backingStore != null)
        {
            backingStore.updateEmbeddedValue(ownerOP, value, fieldNumber, newValue);
        }
    }

    /**
     * Method to unset the owner and field information.
     */
    public void unsetOwner()
    {
        super.unsetOwner();
        if (backingStore != null)
        {
            backingStore = null;
        }
    }

    // -------------------- Implementation of Map Methods ----------------------
 
    /**
     * Creates and returns a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return Clone of the object
     */
    public Object clone()
    {
        if (useCache)
        {
            loadFromStore();
        }

        return ((java.util.HashMap)delegate).clone();
    }

    /**
     * Utility to check if a key is contained in the Map.
     * @param key The key to check
     * @return Whether it is contained
     **/
    public boolean containsKey(Object key)
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.containsKey(key);
        }
        else if (backingStore != null)
        {
            return backingStore.containsKey(ownerOP, key);
        }

        return delegate.containsKey(key);
    }

    /**
     * Utility to check if a value is contained in the Map.
     * @param value The value to check
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
            return backingStore.containsValue(ownerOP, value);
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
            return new Set(ownerOP, ownerMmd, false, backingStore.entrySetStore());
        }

        return delegate.entrySet();
    }

    /**
     * Method to check the equality of this map, and another.
     * @param o The map to compare against.
     * @return Whether they are equal.
     **/
    public boolean equals(Object o)
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

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action)
    {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : (java.util.Set<Map.Entry<K, V>>) entrySet())
        {
            K k;
            V v;
            try
            {
                k = entry.getKey();
                v = entry.getValue();
            }
            catch (IllegalStateException ise)
            {
                // this usually means the entry is no longer in the map.
                throw new ConcurrentModificationException(ise);
            }
            action.accept(k, v);
        }
    }

    /**
     * Accessor for the value stored against a key.
     * @param key The key
     * @return The value.
     **/
    public V get(Object key)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.get(ownerOP, key);
        }

        return delegate.get(key);
    }

    /**
     * Method to generate a hashcode for this Map.
     * @return The hashcode.
     **/
    public int hashCode()
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
    public boolean isEmpty()
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
            return new Set(ownerOP, ownerMmd, false, backingStore.keySetStore());
        }

        return delegate.keySet();
    }

    /**
     * Method to return the size of the Map.
     * @return The size
     **/
    public int size()
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.size();
        }
        else if (backingStore != null)
        {
            return backingStore.entrySetStore().size(ownerOP);
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
            return new org.datanucleus.store.types.wrappers.backed.Collection(ownerOP, ownerMmd, true, backingStore.valueCollectionStore());
        }

        return delegate.values();
    }

    /**
     * Method to return a string form of this Map.
     * @return String form of this Map.
     **/
    public String toString()
    {
        StringBuilder s = new StringBuilder("{");

        Iterator i = entrySet().iterator();
        boolean hasNext = i.hasNext();

        while (hasNext)
        {
            Entry e = (Entry) i.next();
            Object key = e.getKey();
            Object val = e.getValue();
            s.append(key).append('=').append(val);
            hasNext = i.hasNext();

            if (hasNext)
            {
                s.append(',');
            }
        }

        s.append("}");

        return s.toString();
    }

    // -------------------------- Mutator methods ------------------------------
 
    /**
     * Method to clear the Map.
     **/
    public void clear()
    {
        makeDirty();
        delegate.clear();

        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new MapClearOperation(ownerOP, backingStore));
            }
            else
            {
                backingStore.clear(ownerOP);
            }
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to add a value to the Map.
     * @param key The key for the value.
     * @param value The value
     * @return The previous value against this key (if any).
     */
    public V put(K key, V value)
    {
        // Reject inappropriate values
        if (!allowNulls)
        {
            if (value == null)
            {
                throw new NullPointerException("Nulls not allowed for map at field " + ownerMmd.getName() + " but value is null");
            }
            if (key == null)
            {
                throw new NullPointerException("Nulls not allowed for map at field " + ownerMmd.getName() + " but key is null");
            }
        }

        if (useCache)
        {
            // Make sure we have all values loaded (e.g if in optimistic tx and we put new entry)
            loadFromStore();
        }

        makeDirty();

        V oldValue = null;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerOP, backingStore, key, value));
            }
            else
            {
                oldValue = backingStore.put(ownerOP, key, value);
            }
        }
        V delegateOldValue = delegate.put(key, value);
        if (backingStore == null)
        {
            oldValue = delegateOldValue;
        }
        else if (SCOUtils.useQueuedUpdate(ownerOP))
        {
            oldValue = delegateOldValue;
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return oldValue;
    }

    /**
     * Method to add a Map of values to this map.
     * @param m The Map to add
     **/
    public void putAll(java.util.Map m)
    {
        makeDirty();

        if (useCache)
        {
            // Make sure we have all values loaded (e.g if in optimistic tx and we put new entry)
            loadFromStore();
        }

        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                Iterator iter = m.entrySet().iterator();
                while (iter.hasNext())
                {
                    java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
                    ownerOP.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerOP, backingStore, entry.getKey(), entry.getValue()));
                }
            }
            else
            {
                if (useCache)
                {
                    backingStore.putAll(ownerOP, m, Collections.unmodifiableMap(delegate));
                }
                else
                {
                    backingStore.putAll(ownerOP, m);
                }
            }
        }
        delegate.putAll(m);

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to remove a value from the Map.
     * @param key The key for the value.
     * @return The value removed.
     **/
    public V remove(Object key)
    {
        makeDirty();

        if (useCache)
        {
            // Make sure we have all values loaded (e.g if in optimistic tx and we put new entry)
            loadFromStore();
        }

        V removed = null;
        V delegateRemoved = delegate.remove(key);
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new MapRemoveOperation(ownerOP, backingStore, key, delegateRemoved));
                removed = delegateRemoved;
            }
            else
            {
                removed = backingStore.remove(ownerOP, key);
            }
        }
        else
        {
            removed = delegateRemoved;
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return removed;
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
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        if (useCache)
        {
            loadFromStore();
            return new java.util.HashMap(delegate);
        }

        // TODO Cater for non-cached map, load elements in a DB call.
        return new java.util.HashMap(delegate);
    }
}