/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.datanucleus.NucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.store.query.Query;
import org.datanucleus.util.NucleusLogger;

/**
 * Abstract representation of a query results cache for the query.
 */
public class AbstractQueryResultsCache implements QueryResultsCache
{
    private static final long serialVersionUID = -1071931192920096219L;

    /** Keys to pin, if entering into the cache. */
    Set<String> keysToPin = new HashSet<String>();

    /** Cache of pinned objects. */
    Map<String, List<Object>> pinnedCache = new HashMap<String, List<Object>>();

    /** Cache of unpinned objects. */
    Map<String, List<Object>> cache = null;

    private int maxSize = -1;
    private final NucleusContext nucCtx;

    public AbstractQueryResultsCache(NucleusContext nucleusCtx)
    {
        maxSize = nucleusCtx.getConfiguration().getIntProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_MAXSIZE);
        nucCtx = nucleusCtx;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#close()
     */
    public void close()
    {
        cache.clear();
        cache = null;
        pinnedCache.clear();
        pinnedCache = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#contains(java.lang.String)
     */
    public boolean contains(String queryKey)
    {
        return cache.containsKey(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#evict(java.lang.Class)
     */
    public synchronized void evict(Class candidate)
    {
        AbstractClassMetaData cmd = nucCtx.getMetaDataManager().getMetaDataForClass(candidate, nucCtx.getClassLoaderResolver(candidate.getClassLoader()));
        Iterator<String> iter = cache.keySet().iterator();
        while (iter.hasNext())
        {
            String key = iter.next();
            if (key.matches("JDOQL:.* FROM " + candidate.getName() + ".*"))
            {
                NucleusLogger.GENERAL.info(">> Evicting query results for key="+key);
                iter.remove();
            }
            else if (key.matches("JPQL:.* FROM " + candidate.getName() + ".*"))
            {
                NucleusLogger.GENERAL.info(">> Evicting query results for key="+key);
                iter.remove();
            }
            else if (key.matches("JPQL:.* FROM " + cmd.getEntityName()+".*"))
            {
                NucleusLogger.GENERAL.info(">> Evicting query results for key="+key);
                iter.remove();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#evictAll()
     */
    public synchronized void evictAll()
    {
        cache.clear();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#evict(org.datanucleus.store.query.Query)
     */
    public synchronized void evict(Query query)
    {
        String baseKey = QueryUtils.getKeyForQueryResultsCache(query, null);
        Iterator<String> iter = cache.keySet().iterator();
        while (iter.hasNext())
        {
            String key = iter.next();
            if (key.startsWith(baseKey))
            {
                iter.remove();
            }
        }
        iter = pinnedCache.keySet().iterator();
        while (iter.hasNext())
        {
            String key = iter.next();
            if (key.startsWith(baseKey))
            {
                iter.remove();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#evict(org.datanucleus.store.query.Query, java.util.Map)
     */
    public synchronized void evict(Query query, Map params)
    {
        String key = QueryUtils.getKeyForQueryResultsCache(query, params);
        cache.remove(key);
        pinnedCache.remove(key);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#pin(org.datanucleus.store.query.Query, java.util.Map)
     */
    public void pin(Query query, Map params)
    {
        String key = QueryUtils.getKeyForQueryResultsCache(query, params);
        List<Object> results = cache.get(key);
        if (results != null)
        {
            keysToPin.add(key);
            pinnedCache.put(key, results);
            cache.remove(key);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#pin(org.datanucleus.store.query.Query)
     */
    public void pin(Query query)
    {
        String key = QueryUtils.getKeyForQueryResultsCache(query, null);
        List<Object> results = cache.get(key);
        if (results != null)
        {
            keysToPin.add(key);
            pinnedCache.put(key, results);
            cache.remove(key);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#unpin(org.datanucleus.store.query.Query, java.util.Map)
     */
    public void unpin(Query query, Map params)
    {
        String key = QueryUtils.getKeyForQueryResultsCache(query, params);
        List<Object> results = pinnedCache.get(key);
        if (results != null)
        {
            keysToPin.remove(key);
            cache.put(key, results);
            pinnedCache.remove(key);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#unpin(org.datanucleus.store.query.Query)
     */
    public void unpin(Query query)
    {
        String key = QueryUtils.getKeyForQueryResultsCache(query, null);
        List<Object> results = pinnedCache.get(key);
        if (results != null)
        {
            keysToPin.remove(key);
            cache.put(key, results);
            pinnedCache.remove(key);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#get(java.lang.String)
     */
    public List<Object> get(String queryKey)
    {
        if (pinnedCache.containsKey(queryKey))
        {
            return pinnedCache.get(queryKey);
        }
        return cache.get(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#isEmpty()
     */
    public boolean isEmpty()
    {
        return cache.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#put(java.lang.String, java.util.List)
     */
    public synchronized List<Object> put(String queryKey, List<Object> results)
    {
        if (maxSize >= 0 && size() == maxSize)
        {
            return null;
        }

        if (keysToPin.contains(queryKey))
        {
            return pinnedCache.put(queryKey, results);
        }
        return cache.put(queryKey, results);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryResultsCache#size()
     */
    public int size()
    {
        return cache.size();
    }
}