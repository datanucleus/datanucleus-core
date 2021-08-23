/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved. 
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
2003 Andy Jefferson - coding standards
2004 Andy Jefferson - added query methods and comments
    ...
**********************************************************************/
package org.datanucleus.store.types.scostore;

import java.util.Map;

import org.datanucleus.state.ObjectProvider;

/**
 * Interface representation of the backing store for a Map, providing its interface with the datastore.
 * @param <K> Key type for this map
 * @param <V> Value type for this map
 */
public interface MapStore<K, V> extends Store
{
    /**
     * Accessor for whether the keys are embedded.
     * @return Whether we have embedded keys
     */
    boolean keysAreEmbedded();

    /**
     * Accessor for whether the keys are serialised.
     * @return Whether we have serialised keys
     */
    boolean keysAreSerialised();

    /**
     * Accessor for whether the values are embedded.
     * @return Whether we have embedded values
     */
    boolean valuesAreEmbedded();

    /**
     * Accessor for whether the values are serialised.
     * @return Whether we have serialised values
     */
    boolean valuesAreSerialised();

    // -------------------------------- Map Methods ----------------------------
 
    /**
     * Accessor for whether the Map contains this value.
     * @param op ObjectProvider for the owner of the map.
     * @param value The value to check
     * @return Whether it is contained.
     */
    boolean containsValue(ObjectProvider op, Object value);

    /**
     * Accessor for whether the Map contains this key.
     * @param op ObjectProvider for the owner of the map.
     * @param key The key to check
     * @return Whether it is contained.
     */
    boolean containsKey(ObjectProvider op, Object key);

    /**
     * Accessor for a value from the Map.
     * @param op ObjectProvider for the owner of the map. 
     * @param key Key for the value.
     * @return Value for this key.
     */
    V get(ObjectProvider op, Object key);

    /**
     * Method to add a value to the Map against this key.
     * @param op ObjectProvider for the owner of the map. 
     * @param key The key.
     * @param value The value.
     * @return Value that was previously against this key.
     */
    V put(ObjectProvider op, K key, V value);

    /**
     * Method to add a value to the Map against this key, where we know the previous value for the key (if present).
     * Default implementation simply calls the <cite>put(ObjectProvider, Object, Object)</cite> method.
     * Override to provide an efficient implementation for this action.
     * @param op ObjectProvider for the owner of the map. 
     * @param key The key.
     * @param value The value.
     * @param previousValue The previous value
     * @param present Whether the key is present
     */
    default void put(ObjectProvider op, K key, V value, V previousValue, boolean present)
    {
        put(op, key, value);
    }

    /**
     * Method to add a map of values to the Map.
     * @param op ObjectProvider for the owner of the map. 
     * @param m The map to put.
     */ 
    void putAll(ObjectProvider op, Map<? extends K, ? extends V> m);

    /**
     * Method to add a map of values to the Map where we know the existing Map values prior to the putAll call.
     * @param op ObjectProvider for the owner of the map.
     * @param m The map to add.
     * @param previousMap The map prior to the putAll call.
     */
    default void putAll(ObjectProvider<?> op, Map<? extends K, ? extends V> m, Map<K, V> previousMap)
    {
        putAll(op, m);
    }

    /**
     * Method to remove a value from the Map.
     * @param op ObjectProvider for the owner of the map. 
     * @param key Key whose value is to be removed.
     * @return Value that was removed.
     */
    V remove(ObjectProvider op, Object key);

    /**
     * Method to remove a value from the Map where we know the value assigned to this key (to avoid lookups).
     * @param op ObjectProvider for the owner of the map. 
     * @param key Key whose value is to be removed.
     * @param val Value for this key when the value is known (to save the lookup)
     */
    void remove(ObjectProvider op, Object key, Object val);

    /**
     * Method to clear the map.
     * @param op ObjectProvider for the owner of the map. 
     */
    void clear(ObjectProvider op);

    /**
     * Method to update the map to be the supplied map of entries.
     * Default implementation simply does a clear followed by putAll.
     * Override this and provide an efficient implementation for this action.
     * @param op ObjectProvider of the object
     * @param map The map to use
     */
    default void update(ObjectProvider op, Map<K, V> map)
    {
        clear(op);
        putAll(op, map);
    }

    /**
     * Accessor for a backing store representing the key set for the Map.
     * @return Keys for the Map.
     */
    SetStore<K> keySetStore();

    /**
     * Accessor for a backing store representing the values in the Map.
     * @return Values for the Map.
     */
    CollectionStore<V> valueCollectionStore();

    /**
     * Accessor for a backing store representing the entry set for the Map.
     * @return Entry set for the Map.
     */
    SetStore<Map.Entry<K,V>> entrySetStore();

    /**
     * Method to update an embedded key in the map.
     * @param op ObjectProvider for the owner of the map
     * @param key The element
     * @param fieldNumber Field to update in the key
     * @param newValue The new value for the field
     * @return Whether the element was modified
     */
    boolean updateEmbeddedKey(ObjectProvider op, Object key, int fieldNumber, Object newValue);

    /**
     * Method to update an embedded value in the map.
     * @param op ObjectProvider for the owner of the map
     * @param value The element
     * @param fieldNumber Field to update in the value
     * @param newValue The new value for the field
     * @return Whether the element was modified
     */
    boolean updateEmbeddedValue(ObjectProvider op, Object value, int fieldNumber, Object newValue);
}