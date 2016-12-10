/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.cache.Caching;

import org.datanucleus.NucleusContext;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.cache.AbstractLevel2Cache;
import org.datanucleus.cache.CachedPC;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.util.NucleusLogger;

/**
 * Simple implementation of a plugin for use of javax.cache (v0.61+) product with DataNucleus. 
 */
public class JavaxCacheLevel2Cache extends AbstractLevel2Cache
{
    private static final long serialVersionUID = 3218890128547271239L;
    /** The cache to use. */
    private final Cache cache;

    /**
     * Constructor.
     * @param nucleusCtx Context
     */
    public JavaxCacheLevel2Cache(NucleusContext nucleusCtx)
    {
        super(nucleusCtx);

        try
        {
            CachingProvider cacheProvider = Caching.getCachingProvider();
            CacheManager cacheMgr = cacheProvider.getCacheManager();
            Cache tmpcache = cacheMgr.getCache(cacheName);
            if (tmpcache == null)
            {
                MutableConfiguration cacheConfig = new MutableConfiguration();
                Configuration conf = nucleusCtx.getConfiguration();
                if (conf.hasProperty(PropertyNames.PROPERTY_CACHE_L2_READ_THROUGH))
                {
                    cacheConfig.setReadThrough(conf.getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_READ_THROUGH));
                }
                if (conf.hasProperty(PropertyNames.PROPERTY_CACHE_L2_WRITE_THROUGH))
                {
                    cacheConfig.setWriteThrough(conf.getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_WRITE_THROUGH));
                }
                if (conf.hasProperty(PropertyNames.PROPERTY_CACHE_L2_STATISTICS_ENABLED))
                {
                    cacheConfig.setStatisticsEnabled(conf.getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_STATISTICS_ENABLED));
                }
                if (conf.hasProperty(PropertyNames.PROPERTY_CACHE_L2_STORE_BY_VALUE))
                {
                    cacheConfig.setStoreByValue(conf.getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_STORE_BY_VALUE));
                }
                if (timeout > 0)
                {
                    // TODO Some way to set the timeout/expiry
                }
                cacheMgr.createCache(cacheName, cacheConfig);
                tmpcache = cacheMgr.getCache(cacheName);
            }
            cache = tmpcache;
        }
        catch (CacheException e)
        {
            throw new NucleusException("Error creating cache", e);
        }
    }

    /**
     * Method to close the cache when no longer needed. Provides a hook to release resources etc.
     */
    public void close()
    {
        if (clearAtClose)
        {
            evictAll();
        }
    }

    /**
     * Accessor for whether the cache contains the specified id.
     * @see org.datanucleus.cache.Level2Cache#containsOid(java.lang.Object)
     */
    public boolean containsOid(Object oid)
    {
        return get(oid) != null;
    }

    /**
     * Accessor for an object in the cache.
     * @see org.datanucleus.cache.Level2Cache#get(java.lang.Object)
     */
    public CachedPC get(Object oid)
    {
        try
        {
            return (CachedPC) cache.get(oid);
        }
        catch (Exception e)
        {
            NucleusLogger.CACHE.info("Object with id " + oid +" not retrieved from cache due to : " + e.getMessage());
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.cache.AbstractLevel2Cache#getAll(java.util.Collection)
     */
    @Override
    public Map<Object, CachedPC> getAll(Collection oids)
    {
        try
        {
            if (oids instanceof Set)
            {
                return cache.getAll((Set)oids);
            }
            return cache.getAll(new HashSet(oids));
        }
        catch (Exception e)
        {
            NucleusLogger.CACHE.info("Objects not retrieved from cache due to : " + e.getMessage());
            return null;
        }
    }

    /**
     * Accessor for the size of the cache.
     * @see org.datanucleus.cache.Level2Cache#getSize()
     */
    public int getSize()
    {
        // TODO Implement this
        throw new UnsupportedOperationException("size() method not supported by this plugin");
    }

    /**
     * Accessor for whether the cache is empty
     * @see org.datanucleus.cache.Level2Cache#isEmpty()
     */
    public boolean isEmpty()
    {
        return getSize() == 0;
    }

    /**
     * Method to add an object to the cache under its id
     * @param oid The identity
     * @param pc The cacheable object
     */
    public synchronized CachedPC put(Object oid, CachedPC pc)
    {
        if (oid == null || pc == null)
        {
            return null;
        }
        else if (maxSize >= 0 && getSize() == maxSize)
        {
            return null;
        }

        try
        {
            cache.put(oid, pc);
        }
        catch (Exception e)
        {
            // Not cached due to some problem. Not serializable?
            NucleusLogger.CACHE.info("Object with id " + oid +" not cached due to : " + e.getMessage());
        }
        return pc;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.cache.AbstractLevel2Cache#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<Object, CachedPC> objs)
    {
        if (objs == null)
        {
            return;
        }

        try
        {
            cache.putAll(objs);
        }
        catch (RuntimeException re)
        {
            // Not cached due to some problem. Not serializable?
            NucleusLogger.CACHE.info("Objects not cached due to : " + re.getMessage());
        }
    }

    /**
     * Evict the parameter instance from the second-level cache.
     * @param oid the object id of the instance to evict.
     */
    public synchronized void evict(Object oid)
    {
        try
        {
            cache.remove(oid);
        }
        catch (RuntimeException re)
        {
            NucleusLogger.CACHE.info("Object with id=" + oid + " not evicted from cache due to : " + re.getMessage());
        }
    }

    /**
     * Evict the parameter instances from the second-level cache.
     * All instances in the PersistenceManager's cache are evicted
     * from the second-level cache.
     */
    public synchronized void evictAll()
    {
        try
        {
            cache.removeAll();
        }
        catch (RuntimeException re)
        {
            NucleusLogger.CACHE.info("Objects not evicted from cache due to : " + re.getMessage());
        }
    }

    /**
     * Evict the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    public synchronized void evictAll(Collection oids)
    {
        if (oids == null)
        {
            return;
        }

        try
        {
            if (oids instanceof Set)
            {
                cache.removeAll((Set)oids);
            }
            else
            {
                cache.removeAll(new HashSet(oids));
            }
        }
        catch (RuntimeException re)
        {
            NucleusLogger.CACHE.info("Objects not evicted from cache due to : " + re.getMessage());
        }
    }

    /**
     * Evict the parameter instances from the second-level cache.
     * @param oids the object ids of the instance to evict.
     */
    public synchronized void evictAll(Object[] oids)
    {
        if (oids == null)
        {
            return;
        }

        try
        {
            Set oidSet = new HashSet(Arrays.asList(oids));
            cache.removeAll(oidSet);
        }
        catch (RuntimeException re)
        {
            NucleusLogger.CACHE.info("Objects not evicted from cache due to : " + re.getMessage());
        }
    }

    /**
     * Evict the parameter instances from the second-level cache.
     * @param pcClass the class of instances to evict
     * @param subclasses if true, evict instances of subclasses also
     */
    public synchronized void evictAll(Class pcClass, boolean subclasses)
    {
        if (!nucleusCtx.getApiAdapter().isPersistable(pcClass))
        {
            return;
        }

        evictAllOfClass(pcClass.getName());
        if (subclasses)
        {
            String[] subclassNames = nucleusCtx.getMetaDataManager().getSubclassesForClass(pcClass.getName(), true);
            if (subclassNames != null)
            {
                for (int i=0;i<subclassNames.length;i++)
                {
                    evictAllOfClass(subclassNames[i]);
                }
            }
        }

    }

    void evictAllOfClass(String className)
    {
        try
        {
            AbstractClassMetaData cmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(className, nucleusCtx.getClassLoaderResolver(null));
            Iterator<Cache.Entry> entryIter = cache.iterator();
            while (entryIter.hasNext())
            {
                Cache.Entry entry = entryIter.next();
                Object key = entry.getKey();
                if (cmd.getIdentityType() == IdentityType.APPLICATION)
                {
                    String targetClassName = IdentityUtils.getTargetClassNameForIdentitySimple(key);
                    if (className.equals(targetClassName))
                    {
                        entryIter.remove();
                    }
                }
                else if (cmd.getIdentityType() == IdentityType.DATASTORE)
                {
                    String targetClassName = IdentityUtils.getTargetClassNameForIdentitySimple(key);
                    if (className.equals(targetClassName))
                    {
                        entryIter.remove();
                    }
                }
            }
        }
        catch (RuntimeException re)
        {
            NucleusLogger.CACHE.info("Objects not evicted from cache due to : " + re.getMessage());
        }
    }
}