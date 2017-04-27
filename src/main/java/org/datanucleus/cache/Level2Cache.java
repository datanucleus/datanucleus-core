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
    ...
**********************************************************************/
package org.datanucleus.cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Interface for any Level 2 Cache used internally.
 * Provides the typical controls required internally and including the JDO/JPA L2 cache methods.
 * JDO and JPA allow the use of a level 2 (L2) cache, with the cache shared between PersistenceManagers/EntityManagers. 
 * The objects in the level 2 cache don't pertain to any one manager.
 * </p>
 * <p>
 * The L2 cache stores an object of type <i>org.datanucleus.cache.CachedPC</i> and is keyed by the identity of the object. 
 * The <i>CachedPC</i> contains the values of fields of a persistable object, together with the indicators for which fields are loaded.
 * The relation field values do not store actual objects; they store the identities of the related objects. 
 * For example if an object X has a 1-1 relation with another persistable object Y then in the relation field values for X for that field 
 * we store the identity of Y. Similarly if the field is a Collection, then the relation field values will be a  Collection of identities of the related objects. 
 * This provides isolation of each object in the L2 cache (so objects aren't storing references to other objects and so allowing garbage collection etc).
 * </p>
 * <p>
 * Objects are stored in the L2 cache in the following situations
 * </p>
 * <ul>
 * <li>An object is retrieved (from the datastore) within a transaction, and it is stored in the L2 cache if no object with that identity already exists there.</li>
 * <li>At commit() of the transaction any object that has been modified during that transaction will be stored/updated in the L2 cache if its persistable object is
 * still in memory in the PM/EM (could have been garbage collected since flushing)</li>
 * </ul>
 * <p>
 * Each class can be configured to be <i>cacheable</i> or not. 
 * The default for a persistable class is to be cacheable. Configuration is performed via annotations or XML metadata.
 * If a class is not cacheable then objects of that type aren't stored in the L2 cache.
 * </p>
 */
public interface Level2Cache extends Serializable
{
    /**
     * Method to close the cache when no longer needed. Provides a hook to release resources etc.
     */
    void close();

    /**
     * Evict the parameter instance from the second-level cache.
     * @param oid the object id of the instance to evict.
     */
    void evict(Object oid);

    /**
     * Evict the parameter instances from the second-level cache.
     * All instances in the PersistenceManager's cache are evicted
     * from the second-level cache.
     */
    void evictAll();

    /**
     * Evict the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    void evictAll(Object[] oids);

    /**
     * Evict the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    void evictAll(Collection oids);

    /**
     * Evict the parameter instances from the second-level cache.
     * @param pcClass the class of instances to evict
     * @param subclasses if true, evict instances of subclasses also
     */
    void evictAll(Class pcClass, boolean subclasses);

    /**
     * Accessor for the total number of objects in the L2 cache.
     * @return Number of objects
     */
    default int getSize()
    {
        // Some don't support it, so provide a default
        return 0;
    }

    /**
     * Accessor for an object from the cache.
     * @param oid The Object ID
     * @return The L2 cacheable object
     * @param <T> Type of the object represented
     */
    <T> CachedPC<T> get(Object oid);

    /**
     * Accessor for a collection of objects from the cache.
     * @param oids The Object IDs
     * @return Map of the objects, keyed by the oids that are found
     */
    default Map<Object, CachedPC> getAll(Collection oids)
    {
        if (oids == null)
        {
            return null;
        }

        // Just fallback to doing multiple gets. Overridden in the implementation if supported
        Map<Object, CachedPC> objs = new HashMap<>();
        for (Object id : oids)
        {
            CachedPC value = get(id);
            objs.put(id, value);
        }
        return objs;
    }

    /**
     * Method to put an object in the cache.
     * @param oid The Object id for this object
     * @param pc The L2 cacheable persistable object
     * @return The value previously associated with this oid
     * @param <T> Type of the object represented
     */
    <T> CachedPC<T> put(Object oid, CachedPC<T> pc);

    /**
     * Method to put several objects into the cache.
     * @param objs Map of cacheable object keyed by its oid.
     */
    default void putAll(Map<Object, CachedPC> objs)
    {
        if (objs == null)
        {
            return;
        }

        // Just fallback to doing multiple puts. Overridden in the implementation if supported
        Iterator<Map.Entry<Object, CachedPC>> entryIter = objs.entrySet().iterator();
        while (entryIter.hasNext())
        {
            Map.Entry<Object, CachedPC> entry = entryIter.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Accessor for whether the cache is empty.
     * @return Whether it is empty.
     */
    default boolean isEmpty()
    {
        return getSize() == 0;
    }

    /**
     * Accessor for whether an object with the specified id is in the cache
     * @param oid The object id
     * @return Whether it is in the cache
     */
    boolean containsOid(Object oid);

    // ======================================= Caching by UniqueKey constraint ========================================

    /**
     * Method to retrieve the id represented by the specified unique key.
     * @param key Unique key
     * @return The "identity" of the object that this unique key represents
     */
    default CachedPC getUnique(CacheUniqueKey key)
    {
        return null;
    }

    /**
     * Method to store a persistable object for this unique key.
     * @param key The unique key
     * @param pc The representation of the persistable object to cache
     * @return The previous object for this unique key if one was present, otherwise null
     */
    default CachedPC putUnique(CacheUniqueKey key, CachedPC pc)
    {
        return null;
    }

    /**
     * Method to remove any object cached against the provided unique key.
     * @param key Unique key
     */
    default void removeUnique(CacheUniqueKey key)
    {
        return;
    }

    // ======================================= Supported only by caches that allow pinning ========================================

    /**
     * Pin the parameter instance in the second-level cache.
     * @param oid the object id of the instance to pin.
     */
    default void pin (Object oid)
    {
        // Not supported
    }

    /**
     * Pin the parameter instances in the second-level cache.
     * @param oids the object ids of the instances to pin.
     */
    default void pinAll (Collection oids)
    {
        // Not supported
    }

    /**
     * Pin the parameter instances in the second-level cache.
     * @param oids the object ids of the instances to pin.
     */
    default void pinAll (Object[] oids)
    {
        // Not supported
    }

    /**
     * Pin instances in the second-level cache.
     * @param pcClass the class of instances to pin
     * @param subclasses if true, pin instances of subclasses also
     */
    default void pinAll (Class pcClass, boolean subclasses)
    {
        // Not supported
    }

    /**
     * Unpin the parameter instance from the second-level cache.
     * @param oid the object id of the instance to unpin.
     */
    default void unpin(Object oid)
    {
        // Not supported
    }

    /**
     * Unpin the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    default void unpinAll(Collection oids)
    {
        // Not supported
    }

    /**
     * Unpin the parameter instance from the second-level cache.
     * @param oids the object id of the instance to evict.
     */
    default void unpinAll(Object[] oids)
    {
        // Not supported
    }

    /**
     * Unpin instances from the second-level cache.
     * @param pcClass the class of instances to unpin
     * @param subclasses if true, unpin instances of subclasses also
     */
    default void unpinAll(Class pcClass, boolean subclasses)
    {
        // Not supported
    }

    /**
     * Accessor for the number of pinned objects in the cache.
     * @return Number of pinned objects
     */
    default int getNumberOfPinnedObjects()
    {
        // Not supported
        return 0;
    }
    
    /**
     * Accessor for the number of unpinned objects in the cache.
     * @return Number of unpinned objects
     */
    default int getNumberOfUnpinnedObjects()
    {
        // Not supported
        return 0;
    }
}