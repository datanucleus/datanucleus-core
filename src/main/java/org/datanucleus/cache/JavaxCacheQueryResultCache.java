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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.datanucleus.NucleusContext;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.query.cache.AbstractQueryResultsCache;
import org.datanucleus.util.NucleusLogger;

/**
 * Implementation of a query results cache using javax.cache (v0.3+) interface.
 */
public class JavaxCacheQueryResultCache extends AbstractQueryResultsCache
{
    private static final long serialVersionUID = -3967431477335678467L;

    private Cache cache;

    /**
     * Constructor.
     * @param nucleusCtx Context
     */
    public JavaxCacheQueryResultCache(NucleusContext nucleusCtx)
    {
        super(nucleusCtx);

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
     * @see org.datanucleus.store.query.cache.QueryResultsCache#close()
     */
    public void close()
    {
        try
        {
            cache.removeAll();
        }
        catch (Exception re)
        {
            NucleusLogger.CACHE.debug("Objects not evicted from cache due to : " + re.getMessage());
        }
        cache = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#contains(java.lang.String)
     */
    public boolean contains(String queryKey)
    {
        return get(queryKey) != null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#evict(java.lang.Class)
     */
    public void evict(Class candidate)
    {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#evict(org.datanucleus.store.query.Query)
     */
    public void evict(Query query)
    {
        String baseKey = QueryUtils.getKeyForQueryResultsCache(query, null);
        Iterator<Cache.Entry> entryIter = cache.iterator();
        while (entryIter.hasNext())
        {
            Cache.Entry entry = entryIter.next();
            String key = (String)entry.getKey();
            if (key.startsWith(baseKey))
            {
                entryIter.remove();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#evict(org.datanucleus.store.query.Query, java.util.Map)
     */
    public void evict(Query query, Map params)
    {
        String key = QueryUtils.getKeyForQueryResultsCache(query, params);
        cache.remove(key);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#clear()
     */
    public void evictAll()
    {
        try
        {
            cache.removeAll();
        }
        catch (Exception re)
        {
            NucleusLogger.CACHE.debug("Objects not evicted from cache due to : " + re.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#get(java.lang.String)
     */
    public List<Object> get(String queryKey)
    {
        return (List<Object>) cache.get(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#put(java.lang.String, java.util.List)
     */
    public List<Object> put(String queryKey, List<Object> results)
    {
        if (queryKey == null || results == null)
        {
            return null;
        }

        try
        {
            cache.put(queryKey, results);
        }
        catch (RuntimeException re)
        {
            // Not cached for some reason. Not serializable?
            NucleusLogger.CACHE.info("Query results with key '" + queryKey + "' not cached. " + re.getMessage());
        }
        return results;
    }
}