/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.query.cache;

import java.io.Serializable;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.datanucleus.NucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.NucleusLogger;

/**
 * Query compilation (datastore) cache using javax.cache for implementation.
 */
public class JavaxCacheQueryDatastoreCompilationCache implements QueryDatastoreCompilationCache, Serializable
{
    private static final long serialVersionUID = 835024707422532913L;

    /** The cache to use. */
    private Cache<String, Object> cache;

    public JavaxCacheQueryDatastoreCompilationCache(NucleusContext nucleusCtx)
    {
        org.datanucleus.Configuration conf = nucleusCtx.getConfiguration();
        String cacheName = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYCOMPILEDATASTORE_NAME);
        if (cacheName == null)
        {
            cacheName = "datanucleus-query-compilation-datastore";
            NucleusLogger.CACHE.warn("No 'datanucleus.cache.queryCompilationDatastore.cacheName' specified so using name of '" + cacheName + "'");
        }
        try
        {
            CachingProvider cacheProvider = Caching.getCachingProvider();
            CacheManager cacheMgr = cacheProvider.getCacheManager();
            Cache tmpcache = cacheMgr.getCache(cacheName);
            if (tmpcache == null)
            {
                Configuration cacheConfig = new MutableConfiguration();
                // TODO Allow specification of config of the cache
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

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#close()
     */
    @Override
    public void close()
    {
        cache.removeAll();
        cache = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#evict(java.lang.String)
     */
    @Override
    public void evict(String queryKey)
    {
        cache.remove(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#clear()
     */
    @Override
    public void clear()
    {
        cache.removeAll();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#isEmpty()
     */
    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#size()
     */
    @Override
    public int size()
    {
        throw new UnsupportedOperationException("size() method not supported by this plugin");
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#get(java.lang.String)
     */
    @Override
    public Object get(String queryKey)
    {
        return cache.get(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#put(java.lang.String, java.lang.Object)
     */
    @Override
    public Object put(String queryKey, Object compilation)
    {
        if (queryKey == null || compilation == null)
        {
            return null;
        }

        try
        {
            cache.put(queryKey, compilation);
        }
        catch (RuntimeException re)
        {
            // Not cached for some reason. Not serializable?
            NucleusLogger.CACHE.info("Query results with key '" + queryKey + "' not cached. " + re.getMessage());
        }
        return compilation;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#contains(java.lang.String)
     */
    @Override
    public boolean contains(String queryKey)
    {
        return get(queryKey) != null;
    }
}