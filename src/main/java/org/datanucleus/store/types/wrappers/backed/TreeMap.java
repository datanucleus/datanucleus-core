/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
2004 Andy Jefferson - added caching capability
    ...
**********************************************************************/
package org.datanucleus.store.types.wrappers.backed;

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.flush.MapClearOperation;
import org.datanucleus.flush.MapPutOperation;
import org.datanucleus.flush.MapRemoveOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.BackedSCOStoreManager;
import org.datanucleus.store.scostore.MapStore;
import org.datanucleus.store.scostore.Store;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class TreeMap object. Backed by a MapStore object.
 */
public class TreeMap extends org.datanucleus.store.types.wrappers.TreeMap implements BackedSCO
{
    protected transient MapStore backingStore;
    protected transient boolean allowNulls = false;
    protected transient boolean useCache = true;
    protected transient boolean isCacheLoaded = false;
    protected transient boolean queued = false;

    /**
     * Constructor
     * @param op the owner ObjectProvider
     * @param mmd Metadata for the member
     */
    public TreeMap(ObjectProvider op, AbstractMemberMetaData mmd)
    {
        super(op, mmd);

        ExecutionContext ec = ownerOP.getExecutionContext();
        allowNulls = SCOUtils.allowNullsInContainer(allowNulls, mmd);
        queued = ec.isDelayDatastoreOperationsEnabled();
        useCache = SCOUtils.useContainerCache(ownerOP, mmd);

        if (!SCOUtils.mapHasSerialisedKeysAndValues(mmd) && mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            ClassLoaderResolver clr = ec.getClassLoaderResolver();
            this.backingStore = (MapStore)((BackedSCOStoreManager)ownerOP.getStoreManager()).getBackingStoreForField(clr, mmd, java.util.TreeMap.class);
        }

        // Set up our delegate, using a comparator
        Comparator comparator = SCOUtils.getComparator(mmd, ec.getClassLoaderResolver());
        if (comparator != null)
        {
            this.delegate = new java.util.TreeMap(comparator);
        }
        else
        {
            this.delegate = new java.util.TreeMap();
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(SCOUtils.getContainerInfoMessage(op, ownerMmd.getName(), this,
                useCache, queued, allowNulls, SCOUtils.useCachedLazyLoading(op, ownerMmd)));
        }
    }

    /**
     * Method to initialise the SCO from an existing value.
     * @param o Object to set value using.
     * @param forInsert Whether the object needs inserting in the datastore with this value
     * @param forUpdate Whether to update the datastore with this value
     */
    public void initialise(Object o, boolean forInsert, boolean forUpdate)
    {
        java.util.Map m = (java.util.Map)o;
        if (m != null)
        {
            // Check for the case of serialised maps, and assign ObjectProviders to any PC keys/values without
            if (SCOUtils.mapHasSerialisedKeysAndValues(ownerMmd) &&
                (ownerMmd.getMap().keyIsPersistent() || ownerMmd.getMap().valueIsPersistent()))
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

            if (backingStore != null && useCache && !isCacheLoaded)
            {
                // Mark as loaded
                isCacheLoaded = true;
            }

            if (forInsert)
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023007", 
                        ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + m.size()));
                }

                if (useCache)
                {
                    // Make sure we have all values loaded (e.g if in optimistic tx and we put new entry)
                    loadFromStore();
                }
                if (backingStore != null)
                {
                    if (SCOUtils.useQueuedUpdate(queued, ownerOP))
                    {
                        Iterator iter = m.entrySet().iterator();
                        while (iter.hasNext())
                        {
                            Map.Entry entry = (Map.Entry)iter.next();
                            ownerOP.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerOP, backingStore, entry.getKey(), entry.getValue()));
                        }
                    }
                    else
                    {
                        backingStore.putAll(ownerOP, m);
                    }
                }
                delegate.putAll(m);
                makeDirty();
            }
            else if (forUpdate)
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023008", 
                        ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + m.size()));
                }

                // TODO This is clear+putAll. Improve it to work out what is changed
                delegate.clear();
                if (backingStore != null)
                {
                    if (SCOUtils.useQueuedUpdate(queued, ownerOP))
                    {
                        ownerOP.getExecutionContext().addOperationToQueue(new MapClearOperation(ownerOP, backingStore));
                    }
                    else
                    {
                        backingStore.clear(ownerOP);
                    }
                }
                if (useCache)
                {
                    // Make sure we have all values loaded (e.g if in optimistic tx and we put new entry)
                    loadFromStore();
                }

                if (backingStore != null)
                {
                    if (SCOUtils.useQueuedUpdate(queued, ownerOP))
                    {
                        Iterator iter = m.entrySet().iterator();
                        while (iter.hasNext())
                        {
                            Map.Entry entry = (Map.Entry)iter.next();
                            ownerOP.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerOP, backingStore, entry.getKey(), entry.getValue()));
                        }
                    }
                    else
                    {
                        backingStore.putAll(ownerOP, m);
                    }
                }
                delegate.putAll(m);
                makeDirty();
            }
            else
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023007", 
                        ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + m.size()));
                }
                delegate.clear();
                delegate.putAll(m);
            }
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

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public Object getValue()
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
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023006", 
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
     */
    public void updateEmbeddedKey(Object key, int fieldNumber, Object newValue)
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
     */
    public void updateEmbeddedValue(Object value, int fieldNumber, Object newValue)
    {
        if (backingStore != null)
        {
            backingStore.updateEmbeddedValue(ownerOP, value, fieldNumber, newValue);
        }
    }

    /**
     * Method to unset the owner and field details.
     */
    public synchronized void unsetOwner()
    {
        super.unsetOwner();
        if (backingStore != null)
        {
            backingStore = null;
        }
    }

    // ------------------ Implementation of TreeMap methods --------------------
 
    /**
     * Creates and returns a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return The cloned object
     */
    public Object clone()
    {
        if (useCache)
        {
            loadFromStore();
        }

        return delegate.clone();
    }

    /**
     * Accessor for the comparator.
     * @return The comparator
     */
    public Comparator comparator()
    {
        return delegate.comparator();
    }

    /**
     * Method to return if the map contains this key
     * @param key The key
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
     * Accessor for the first key in the sorted map.
     * @return The first key
     **/
    public Object firstKey()
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.firstKey();
        }
        else if (useCache)
        {
            loadFromStore();
        }
        else
        {
            // Use Iterator to get element
            java.util.Set keys = keySet();
            Iterator keysIter = keys.iterator();
            return keysIter.next();
        }

        return delegate.firstKey();
    }

    /**
     * Accessor for the last key in the sorted map.
     * @return The last key
     **/
    public Object lastKey()
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.lastKey();
        }
        else if (useCache)
        {
            loadFromStore();
        }
        else
        {
            // Use Iterator to get element
            java.util.Set keys = keySet();
            Iterator keysIter = keys.iterator();
            Object last = null;
            while (keysIter.hasNext())
            {
                last = keysIter.next();
            }
            return last;
        }

        return delegate.lastKey();
    }

    /**
     * Method to retrieve the head of the map up to the specified key.
     * @param toKey the key to return up to.
     * @return The map meeting the input
     */
    public SortedMap headMap(Object toKey)
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.headMap(toKey);
        }
        else if (useCache)
        {
            loadFromStore();
        }
        else
        {
            // TODO Provide a datastore method to do this
            throw new NucleusUserException("Don't currently support TreeMap.headMap() when not using cached containers");
        }

        return delegate.headMap(toKey);
    }

    /**
     * Method to retrieve the subset of the map between the specified keys.
     * @param fromKey The start key
     * @param toKey The end key
     * @return The map meeting the input
     */
    public SortedMap subMap(Object fromKey, Object toKey)
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.subMap(fromKey, toKey);
        }
        else if (useCache)
        {
            loadFromStore();
        }
        else
        {
            // TODO Provide a datastore method to do this
            throw new NucleusUserException("Don't currently support TreeMap.subMap() when not using cached container");
        }

        return delegate.subMap(fromKey, toKey);
    }

    /**
     * Method to retrieve the part of the map after the specified key.
     * @param fromKey The start key
     * @return The map meeting the input
     */
    public SortedMap tailMap(Object fromKey)
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.headMap(fromKey);
        }
        else if (useCache)
        {
            loadFromStore();
        }
        else
        {
            // TODO Provide a datastore method to do this
            throw new NucleusUserException("Don't currently support TreeMap.tailMap() when not using cached containers");
        }

        return delegate.headMap(fromKey);
    }

    /**
     * Accessor for the value stored against a key.
     * @param key The key
     * @return The value.
     **/
    public Object get(Object key)
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

    // --------------------------- Mutator methods -----------------------------
 
    /**
     * Method to clear the TreeMap.
     **/
    public void clear()
    {
        makeDirty();
        delegate.clear();

        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
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
     * Method to add a value against a key to the TreeMap.
     * @param key The key
     * @param value The value
     * @return The previous value for the specified key.
     */
    public Object put(Object key,Object value)
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

        Object oldValue = null;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerOP, backingStore, key, value));
            }
            else
            {
                oldValue = backingStore.put(ownerOP, key, value);
            }
        }
        Object delegateOldValue = delegate.put(key, value);
        if (backingStore == null)
        {
            oldValue = delegateOldValue;
        }
        else if (SCOUtils.useQueuedUpdate(queued, ownerOP))
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
     * Method to add the specified Map's values under their keys here.
     * @param m The map
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
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
            {
                Iterator iter = m.entrySet().iterator();
                while (iter.hasNext())
                {
                    Map.Entry entry = (Map.Entry)iter.next();
                    ownerOP.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerOP, backingStore, entry.getKey(), entry.getValue()));
                }
            }
            else
            {
                backingStore.putAll(ownerOP, m);
            }
        }
        delegate.putAll(m);

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to remove the value for a key from the TreeMap.
     * @param key The key to remove
     * @return The value that was removed from this key.
     **/
    public Object remove(Object key)
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
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
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
     * 
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        if (useCache)
        {
            loadFromStore();
            return new java.util.TreeMap(delegate);
        }
        else
        {
            // TODO Cater for non-cached map, load elements in a DB call.
            return new java.util.TreeMap(delegate);
        }
    }
}