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

import java.util.Collection;

import org.datanucleus.NucleusContext;

/**
 * Null implementation of a Level 2 Cache.
 * Does nothing when its methods are invoked.
 */
public class NullLevel2Cache extends AbstractLevel2Cache
{
    public static final String NAME = "none";

    private static final long serialVersionUID = -218917474395656038L;

    public NullLevel2Cache(NucleusContext nucleusCtx)
    {
        super(nucleusCtx);
    }

    /**
     * Method to close the cache when no longer needed. Provides a hook to release resources etc.
     */
    public void close()
    {
    }

    /**
     * Evict the parameter instance from the second-level cache.
     * @param oid the object id of the instance to evict.
     */
    public void evict(Object oid)
    {
    }

    /**
     * Evict the parameter instances from the second-level cache. All instances in the PersistenceManager's
     * cache are evicted from the second-level cache.
     */
    public void evictAll()
    {
    }

    /**
     * Evict the parameter instances from the second-level cache.
     * @param pcClass the class to evict
     * @param subclasses Whether to evict all subclasses of this class also
     */
    public void evictAll(Class pcClass, boolean subclasses)
    {
    }

    /**
     * Evict the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    public void evictAll(Collection oids)
    {
    }

    /**
     * Evict the parameter instances from the second-level cache.
     * @param oids the object ids of the instances to evict
     */
    public void evictAll(Object[] oids)
    {
    }

    /**
     * Accessor for whether an object with the specified id is in the cache
     * @param oid The object id
     * @return Whether it is in the cache
     */
    public boolean containsOid(Object oid)
    {
        return false;
    }

    /**
     * Accessor for an object from the cache
     * @param oid The identity
     * @return The cacheable object
     */
    public CachedPC get(Object oid)
    {
        return null;
    }

    /**
     * @see org.datanucleus.cache.Level2Cache#getSize()
     */
    public int getSize()
    {
        return 0;
    }

    /**
     * @see org.datanucleus.cache.Level2Cache#isEmpty()
     */
    public boolean isEmpty()
    {
        return false;
    }

    /**
     * Method to put an object in the L2 cache
     * @param oid The identity
     * @param pc Cacheable form of the PC
     * @return Previous value stored for this id
     */
    public CachedPC put(Object oid, CachedPC pc)
    {
        return null;
    }
}