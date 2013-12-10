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
import java.util.Map;

/**
 * Interface for any Level 2 Cache used internally.
 * Provides the typical controls required internally and including the JDO2/JPA1 L2 cache methods.
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
    void evict (Object oid);

    /**
     * Evict the parameter instances from the second-level cache.
     * All instances in the PersistenceManager's cache are evicted
     * from the second-level cache.
     */
    void evictAll ();

    /**
     * Evict the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    void evictAll (Object[] oids);

    /**
     * Evict the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    void evictAll (Collection oids);

    /**
     * Evict the parameter instances from the second-level cache.
     * @param pcClass the class of instances to evict
     * @param subclasses if true, evict instances of subclasses also
     */
    void evictAll (Class pcClass, boolean subclasses);

    /**
     * Pin the parameter instance in the second-level cache.
     * @param oid the object id of the instance to pin.
     */
    void pin (Object oid);

    /**
     * Pin the parameter instances in the second-level cache.
     * @param oids the object ids of the instances to pin.
     */
    void pinAll (Collection oids);

    /**
     * Pin the parameter instances in the second-level cache.
     * @param oids the object ids of the instances to pin.
     */
    void pinAll (Object[] oids);

    /**
     * Pin instances in the second-level cache.
     * @param pcClass the class of instances to pin
     * @param subclasses if true, pin instances of subclasses also
     */
    void pinAll (Class pcClass, boolean subclasses);

    /**
     * Unpin the parameter instance from the second-level cache.
     * @param oid the object id of the instance to unpin.
     */
    void unpin(Object oid);

    /**
     * Unpin the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    void unpinAll(Collection oids);

    /**
     * Unpin the parameter instance from the second-level cache.
     * @param oids the object id of the instance to evict.
     */
    void unpinAll(Object[] oids);

    /**
     * Unpin instances from the second-level cache.
     * @param pcClass the class of instances to unpin
     * @param subclasses if true, unpin instances of subclasses also
     */
    void unpinAll(Class pcClass, boolean subclasses);

    /**
     * Accessor for the number of pinned objects in the cache.
     * @return Number of pinned objects
     */
    int getNumberOfPinnedObjects();
    
    /**
     * Accessor for the number of unpinned objects in the cache.
     * @return Number of unpinned objects
     */
    int getNumberOfUnpinnedObjects();

    /**
     * Accessor for the total number of objects in the L2 cache.
     * @return Number of objects
     */
    int getSize();

    /**
     * Accessor for an object from the cache.
     * @param oid The Object ID
     * @return The L2 cacheable object
     */
    CachedPC get(Object oid);

    /**
     * Accessor for a collection of objects from the cache.
     * @param oids The Object IDs
     * @return Map of the objects, keyed by the oids that are found
     */
    Map<Object, CachedPC> getAll(Collection oids);

    /**
     * Method to put an object in the cache.
     * @param oid The Object id for this object
     * @param pc The L2 cacheable persistable object
     * @return The value previously associated with this oid
     */
    CachedPC put(Object oid, CachedPC pc);

    /**
     * Method to put several objects into the cache.
     * @param objs Map of cacheable object keyed by its oid.
     */
    void putAll(Map<Object, CachedPC> objs);

    /**
     * Accessor for whether the cache is empty.
     * @return Whether it is empty.
     */
    boolean isEmpty();

    /**
     * Accessor for whether an object with the specified id is in the cache
     * @param oid The object id
     * @return Whether it is in the cache
     */
    boolean containsOid(Object oid);

    /**
     * Representation of a class whose objects will be pinned when put into the L2 cache.
     */
    class PinnedClass
    {
        Class cls;
        boolean subclasses;
        
        /**
         * Constructor
         * @param cls the class
         * @param subclasses sub classes
         */
        public PinnedClass(Class cls, boolean subclasses)
        {
            this.cls = cls;
            this.subclasses = subclasses;
        }

        public int hashCode()
        {
            return cls.hashCode() ^ (subclasses ? 0 : 1);
        }

        public boolean equals(Object obj)
        {
            if (obj == null)
            {
                return false;
            }
            if (!(obj instanceof PinnedClass))
            {
                return false;
            }
            PinnedClass other = (PinnedClass)obj;
            return other.cls.getName().equals(cls.getName()) && other.subclasses == subclasses;
        }
    }
}