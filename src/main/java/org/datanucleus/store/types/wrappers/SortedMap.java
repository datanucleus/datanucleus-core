/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.wrappers;

import java.io.ObjectStreamException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BiConsumer;

import org.datanucleus.FetchPlanState;
import org.datanucleus.flush.MapPutOperation;
import org.datanucleus.flush.MapRemoveOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.SCOMap;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class SortedMap object.
 * This is the simplified form that intercepts mutators and marks the field as dirty.
 * It also handles cascade-delete triggering for persistable elements.
 */
public class SortedMap<K, V> extends AbstractMap<K, V> implements java.util.SortedMap<K, V>, SCOMap<java.util.SortedMap<K, V>, K, V>, Cloneable, java.io.Serializable
{
    protected transient DNStateManager ownerSM;
    protected transient AbstractMemberMetaData ownerMmd;

    /** The internal "delegate". */
    protected java.util.SortedMap<K, V> delegate;

    /**
     * Constructor
     * @param sm the owner StateManager
     * @param mmd Metadata for the member
     */
    public SortedMap(DNStateManager sm, AbstractMemberMetaData mmd)
    {
        this.ownerSM = sm;
        this.ownerMmd = mmd;
    }

    public void initialise(java.util.SortedMap newValue, Object oldValue)
    {
        initialise(newValue);
    }

    public void initialise(java.util.SortedMap m)
    {
        if (m != null)
        {
            initialiseDelegate();
            delegate.putAll(m);
        }
        else
        {
            initialiseDelegate();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("023003", this.getClass().getName(), ownerSM.getObjectAsPrintable(), ownerMmd.getName(), "" + size(), 
                SCOUtils.getSCOWrapperOptionsMessage(true, false, false, false)));
        }
    }

    public void initialise()
    {
        initialise(null);
    }

    /**
     * Convenience method to set up the delegate respecting any comparator specified in MetaData.
     */
    protected void initialiseDelegate()
    {
        Comparator comparator = SCOUtils.getComparator(ownerMmd, ownerSM.getExecutionContext().getClassLoaderResolver());
        if (comparator != null)
        {
            this.delegate = new java.util.TreeMap(comparator);
        }
        else
        {
            this.delegate = new java.util.TreeMap();
        }
    }

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.SortedMap getValue()
    {
        return delegate;
    }

    public void setValue(java.util.SortedMap value)
    {
        this.delegate = value;
    }

    /**
     * Method to effect the load of the data in the SCO.
     * Used when the SCO supports lazy-loading to tell it to load all now.
     */
    public void load()
    {
        // Always loaded
    }

    /**
     * Method to return if the SCO has its contents loaded. Returns true.
     * @return Whether it is loaded
     */
    public boolean isLoaded()
    {
        return true;
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
        if (makeDirty)
        {
            // Just mark field in embedded owners as dirty
            makeDirty();
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
        if (makeDirty)
        {
            // Just mark field in embedded owners as dirty
            makeDirty();
        }
    }

    /**
     * Accessor for the field name that this SortedMap relates to.
     * @return The field name
     **/
    public String getFieldName()
    {
        return ownerMmd.getName();
    }

    /**
     * Accessor for the owner that this SortedMap relates to.
     * @return The owner
     **/
    public Object getOwner()
    {
        return ownerSM != null ? ownerSM.getObject() : null;
    }

    /**
     * Method to unset the owner and field details.
     **/
    public void unsetOwner()
    {
        if (ownerSM != null)
        {
            ownerSM = null;
            ownerMmd = null;
        }
    }

    /**
     * Utility to mark the object as dirty
     **/
    public void makeDirty()
    {
        if (ownerSM != null)
        {
            ownerSM.makeDirty(ownerMmd.getAbsoluteFieldNumber());
        }
    }

    /**
     * Method to return a detached copy of the container.
     * Recurse sthrough the keys/values so that they are likewise detached.
     * @param state State for detachment state
     * @return The detached container
     */
    public java.util.SortedMap detachCopy(FetchPlanState state)
    {
        java.util.SortedMap detached = new java.util.TreeMap();
        SCOUtils.detachCopyForMap(ownerSM, entrySet(), state, detached);
        return detached;
    }

    /**
     * Method to return an attached copy of the passed (detached) value. The returned attached copy
     * is a SCO wrapper. Goes through the existing keys/values in the store for this owner field and
     * removes ones no longer present, and adds new keys/values. All keys/values in the (detached)
     * value are attached.
     * @param value The new (map) value
     */
    public void attachCopy(java.util.SortedMap value)
    {
        // Attach all of the keys/values in the new map
        boolean keysWithoutIdentity = SCOUtils.mapHasKeysWithoutIdentity(ownerMmd);
        boolean valuesWithoutIdentity = SCOUtils.mapHasValuesWithoutIdentity(ownerMmd);

        java.util.Map attachedKeysValues = new java.util.TreeMap();
        SCOUtils.attachCopyForMap(ownerSM, value.entrySet(), attachedKeysValues, keysWithoutIdentity, valuesWithoutIdentity);

        // Update the attached map with the detached elements
        SCOUtils.updateMapWithMapKeysValues(ownerSM.getExecutionContext().getApiAdapter(), this, attachedKeysValues);
    }

    // ------------------ Implementation of SortedMap methods --------------------
 
    /**
     * Creates and returns a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return The cloned object
     */
    public Object clone()
    {
        return ((java.util.TreeMap)delegate).clone();
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
        return delegate.containsKey(key);
    }

    /**
     * Method to return if the map contains this value.
     * @param value The value
     * @return Whether it is contained
     **/
    public boolean containsValue(Object value)
    {
        return delegate.containsValue(value);
    }

    /**
     * Accessor for the set of entries in the Map.
     * @return Set of entries
     **/
    public java.util.Set entrySet()
    {
        return delegate.entrySet();
    }

    /**
     * Method to check the equality of this map, and another.
     * @param o The map to compare against.
     * @return Whether they are equal.
     **/
    public boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    /**
     * Accessor for the first key in the sorted map.
     * @return The first key
     **/
    public K firstKey()
    {
        return delegate.firstKey();
    }
    
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action)
    {
        delegate.forEach(action);
    }

    /**
     * Accessor for the last key in the sorted map.
     * @return The last key
     **/
    public K lastKey()
    {
        return delegate.lastKey();
    }

    /**
     * Method to retrieve the head of the map up to the specified key.
     * @param toKey the key to return up to.
     * @return The map meeting the input
     */
    public java.util.SortedMap headMap(K toKey)
    {
        return delegate.headMap(toKey);
    }

    /**
     * Method to retrieve the subset of the map between the specified keys.
     * @param fromKey The start key
     * @param toKey The end key
     * @return The map meeting the input
     */
    public java.util.SortedMap subMap(K fromKey, K toKey)
    {
        return delegate.subMap(fromKey, toKey);
    }

    /**
     * Method to retrieve the part of the map after the specified key.
     * @param fromKey The start key
     * @return The map meeting the input
     */
    public java.util.SortedMap tailMap(K fromKey)
    {
        return delegate.headMap(fromKey);
    }

    /**
     * Accessor for the value stored against a key.
     * @param key The key
     * @return The value.
     **/
    public V get(Object key)
    {
        return delegate.get(key);
    }

    /**
     * Method to generate a hashcode for this Map.
     * @return The hashcode.
     **/
    public int hashCode()
    {
        return delegate.hashCode();
    }

    /**
     * Method to return if the Map is empty.
     * @return Whether it is empty.
     **/
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /**
     * Accessor for the set of keys in the Map.
     * @return Set of keys.
     **/
    public java.util.Set keySet()
    {
        return delegate.keySet();
    }

    /**
     * Method to return the size of the Map.
     * @return The size
     **/
    public int size()
    {
        return delegate.size();
    }

    /**
     * Accessor for the set of values in the Map.
     * @return Set of values.
     **/
    public Collection values()
    {
        return delegate.values();
    }

    // --------------------------- Mutator methods -----------------------------
 
    /**
     * Method to clear the SortedMap.
     */
    public void clear()
    {
        if (ownerSM != null && !delegate.isEmpty())
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                Iterator<Map.Entry<K, V>> entryIter = delegate.entrySet().iterator();
                while (entryIter.hasNext())
                {
                    Map.Entry entry = entryIter.next();
                    ownerSM.getExecutionContext().addOperationToQueue(new MapRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), entry.getKey(), entry.getValue()));
                }
            }
            else if (SCOUtils.hasDependentKey(ownerMmd) || SCOUtils.hasDependentValue(ownerMmd)) 
            {
                Iterator<Map.Entry<K, V>> entryIter = delegate.entrySet().iterator();
                while (entryIter.hasNext())
                {
                    Map.Entry entry = entryIter.next();
                    if (SCOUtils.hasDependentKey(ownerMmd))
                    {
                        ownerSM.getExecutionContext().deleteObjectInternal(entry.getKey());
                    }
                    if (SCOUtils.hasDependentValue(ownerMmd))
                    {
                        ownerSM.getExecutionContext().deleteObjectInternal(entry.getValue());
                    }
                }
            }
        }

        delegate.clear();
        makeDirty();
        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
    }
 
    /**
     * Method to add a value against a key to the SortedMap.
     * @param key The key
     * @param value The value
     * @return The previous value for the specified key.
     */
    public V put(K key, V value)
    {
        V oldValue = delegate.put(key, value);
        makeDirty();

        if (ownerSM != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), key, value));
            }

            if (!ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return oldValue;
    }

    /**
     * Method to add the specified Map's values under their keys here.
     * @param m The map
     **/
    public void putAll(java.util.Map m)
    {
        delegate.putAll(m);
        makeDirty();

        if (ownerSM != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                Iterator<Map.Entry> entryIter = m.entrySet().iterator();
                while (entryIter.hasNext())
                {
                    Map.Entry entry = entryIter.next();
                    ownerSM.getExecutionContext().addOperationToQueue(new MapPutOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), entry.getKey(), entry.getValue()));
                }
            }

            if (!ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }
        }
    }

    /**
     * Method to remove the value for a key from the SortedMap.
     * @param key The key to remove
     * @return The value that was removed from this key.
     **/
    public V remove(Object key)
    {
        V value = delegate.remove(key);

        if (ownerSM != null)
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new MapRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), key, value));
            }
            else if (SCOUtils.hasDependentKey(ownerMmd) || SCOUtils.hasDependentValue(ownerMmd)) 
            {
                if (SCOUtils.hasDependentKey(ownerMmd))
                {
                    ownerSM.getExecutionContext().deleteObjectInternal(key);
                }
                if (SCOUtils.hasDependentValue(ownerMmd))
                {
                    ownerSM.getExecutionContext().deleteObjectInternal(value);
                }
            }
        }

        makeDirty();
        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }

        return value;
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
        return new java.util.TreeMap(delegate);
    }
}