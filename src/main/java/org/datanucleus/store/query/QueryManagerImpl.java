/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved.
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
2008 Andy Jefferson - check on datastore when getting query support
2008 Andy Jefferson - query cache
     ...
***********************************************************************/
package org.datanucleus.store.query;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.inmemory.InvocationEvaluator;
import org.datanucleus.query.inmemory.method.ArrayContainsMethod;
import org.datanucleus.query.inmemory.method.ArraySizeMethod;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.query.cache.QueryCompilationCache;
import org.datanucleus.store.query.cache.QueryDatastoreCompilationCache;
import org.datanucleus.store.query.cache.QueryResultsCache;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Manages the creation, compilation and results of queries.
 * Provides caching of query compilations (generic and datastore-specific) and results.
 */
public class QueryManagerImpl implements QueryManager
{
    protected NucleusContext nucleusCtx;

    protected StoreManager storeMgr;

    /** Cache for generic query compilations. */
    protected final QueryCompilationCache queryCompilationCache;

    /** Cache for datastore query compilations. */
    protected final QueryDatastoreCompilationCache queryCompilationCacheDatastore;

    /** Cache for query results. */
    protected final QueryResultsCache queryResultsCache;

    /** Cache of InvocationEvaluator objects keyed by the method name, for use by in-memory querying. */
    protected Map<String, Map<Object, InvocationEvaluator>> inmemoryQueryMethodEvaluatorMap = new ConcurrentHashMap<String, Map<Object,InvocationEvaluator>>();

    public QueryManagerImpl(NucleusContext nucleusContext, StoreManager storeMgr)
    {
        this.nucleusCtx = nucleusContext;
        this.storeMgr = storeMgr;

        Configuration conf = nucleusCtx.getConfiguration();

        // QueryCompilationCache (generic compilation)
        String cacheType = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYCOMPILE_TYPE);
        if (cacheType == null)
        {
            cacheType = "soft";
        }
        String cacheClassName = nucleusCtx.getPluginManager().getAttributeValueForExtension("org.datanucleus.cache_query_compilation", "name", cacheType, "class-name");
        if (cacheClassName == null)
        {
            // Plugin of this name not found
            throw new NucleusUserException(Localiser.msg("021500", cacheType)).setFatal();
        }
        try
        {
            // Create an instance of the Query Cache
            queryCompilationCache = (QueryCompilationCache)nucleusCtx.getPluginManager().createExecutableExtension("org.datanucleus.cache_query_compilation",
                "name", cacheType, "class-name", new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[] {nucleusCtx});
            if (NucleusLogger.CACHE.isDebugEnabled())
            {
                NucleusLogger.CACHE.debug(Localiser.msg("021502", cacheClassName));
            }
        }
        catch (Exception e)
        {
            // Class name for this Query cache plugin is not found!
            throw new NucleusUserException(Localiser.msg("021501", cacheType, cacheClassName), e).setFatal();
        }

        // QueryCompilationCache (datastore compilation)
        cacheType = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYCOMPILEDATASTORE_TYPE);
        if (cacheType == null)
        {
            cacheType = "soft";
        }
        cacheClassName = nucleusCtx.getPluginManager().getAttributeValueForExtension("org.datanucleus.cache_query_compilation_store", "name", cacheType, "class-name");
        if (cacheClassName == null)
        {
            // Plugin of this name not found
            throw new NucleusUserException(Localiser.msg("021500", cacheType)).setFatal();
        }
        try
        {
            // Create an instance of the Query Cache
            queryCompilationCacheDatastore = (QueryDatastoreCompilationCache)nucleusCtx.getPluginManager().createExecutableExtension("org.datanucleus.cache_query_compilation_store", 
                "name", cacheType, "class-name", new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[] {nucleusCtx});
            if (NucleusLogger.CACHE.isDebugEnabled())
            {
                NucleusLogger.CACHE.debug(Localiser.msg("021502", cacheClassName));
            }
        }
        catch (Exception e)
        {
            // Class name for this Query cache plugin is not found!
            throw new NucleusUserException(Localiser.msg("021501", cacheType, cacheClassName), e).setFatal();
        }

        // Query results cache
        cacheType = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_TYPE);
        if (cacheType == null)
        {
            cacheType = "soft";
        }
        cacheClassName = nucleusCtx.getPluginManager().getAttributeValueForExtension("org.datanucleus.cache_query_result", "name", cacheType, "class-name");
        if (cacheClassName == null)
        {
            // Plugin of this name not found
            throw new NucleusUserException(Localiser.msg("021500", cacheType)).setFatal();
        }
        try
        {
            // Create an instance of the Query Cache
            queryResultsCache = (QueryResultsCache)nucleusCtx.getPluginManager().createExecutableExtension("org.datanucleus.cache_query_result", 
                "name", cacheType, "class-name", new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[] {nucleusCtx});
            if (NucleusLogger.CACHE.isDebugEnabled())
            {
                NucleusLogger.CACHE.debug(Localiser.msg("021502", cacheClassName));
            }
        }
        catch (Exception e)
        {
            // Class name for this Query cache plugin is not found!
            throw new NucleusUserException(Localiser.msg("021501", cacheType, cacheClassName), e).setFatal();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#close()
     */
    @Override
    public void close()
    {
        if (queryCompilationCache != null)
        {
            queryCompilationCache.close();
        }
        if (queryCompilationCacheDatastore != null)
        {
            queryCompilationCacheDatastore.close();
        }
        if (queryResultsCache != null)
        {
            queryResultsCache.close();
        }

        inmemoryQueryMethodEvaluatorMap.clear();
        inmemoryQueryMethodEvaluatorMap = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#getQueryCompilationCache()
     */
    @Override
    public QueryCompilationCache getQueryCompilationCache()
    {
        return queryCompilationCache;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#addQueryCompilation(java.lang.String, java.lang.String, org.datanucleus.query.compiler.QueryCompilation)
     */
    @Override
    public void addQueryCompilation(String language, String query, QueryCompilation compilation)
    {
        if (queryCompilationCache != null)
        {
            queryCompilationCache.put(language + ":" + query, compilation);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#deleteQueryCompilation(java.lang.String, java.lang.String)
     */
    @Override
    public void removeQueryCompilation(String language, String query)
    {
        if (queryCompilationCache != null)
        {
            queryCompilationCache.evict(language + ":" + query);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#getQueryCompilationForQuery(java.lang.String, java.lang.String)
     */
    @Override
    public QueryCompilation getQueryCompilationForQuery(String language, String query)
    {
        if (queryCompilationCache != null)
        {
            String queryKey = language + ":" + query;
            QueryCompilation compilation = queryCompilationCache.get(queryKey);
            if (compilation != null)
            {
                if (NucleusLogger.QUERY.isDebugEnabled())
                {
                    NucleusLogger.QUERY.debug(Localiser.msg("021079", query, language));
                }
                return compilation;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#getQueryDatastoreCompilationCache()
     */
    @Override
    public QueryDatastoreCompilationCache getQueryDatastoreCompilationCache()
    {
        return queryCompilationCacheDatastore;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#addDatastoreQueryCompilation(java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     */
    @Override
    public void addDatastoreQueryCompilation(String datastore, String language, String query, Object compilation)
    {
        if (queryCompilationCacheDatastore != null)
        {
            String queryKey = language + ":" + query;
            queryCompilationCacheDatastore.put(queryKey, compilation);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#deleteDatastoreQueryCompilation(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void removeDatastoreQueryCompilation(String datastore, String language, String query)
    {
        if (queryCompilationCacheDatastore != null)
        {
            String queryKey = language + ":" + query;
            queryCompilationCacheDatastore.evict(queryKey);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#getDatastoreQueryCompilation(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Object getDatastoreQueryCompilation(String datastore, String language, String query)
    {
        if (queryCompilationCacheDatastore != null)
        {
            String queryKey = language + ":" + query;
            Object compilation = queryCompilationCacheDatastore.get(queryKey);
            if (compilation != null)
            {
                if (NucleusLogger.QUERY.isDebugEnabled())
                {
                    NucleusLogger.QUERY.debug(Localiser.msg("021080", query, language, datastore));
                }
                return compilation;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#getQueryResultsCache()
     */
    @Override
    public QueryResultsCache getQueryResultsCache()
    {
        return queryResultsCache;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#evictQueryResultsForType(java.lang.Class)
     */
    @Override
    public void evictQueryResultsForType(Class cls)
    {
        if (queryResultsCache != null)
        {
            queryResultsCache.evict(cls);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#addDatastoreQueryResult(org.datanucleus.store.query.Query, java.util.Map, java.util.List)
     */
    @Override
    public void addQueryResult(Query query, Map params, List<Object> results)
    {
        if (queryResultsCache != null)
        {
            String queryKey = QueryUtils.getKeyForQueryResultsCache(query, params);
            queryResultsCache.put(queryKey, results);
            if (NucleusLogger.QUERY.isDebugEnabled())
            {
                NucleusLogger.QUERY.debug(Localiser.msg("021081", query, results.size()));
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#getDatastoreQueryResult(org.datanucleus.store.query.Query, java.util.Map)
     */
    @Override
    public List<Object> getQueryResult(Query query, Map params)
    {
        if (queryResultsCache != null)
        {
            String queryKey = QueryUtils.getKeyForQueryResultsCache(query, params);
            List<Object> results = queryResultsCache.get(queryKey);
            if (results != null)
            {
                if (NucleusLogger.QUERY.isDebugEnabled())
                {
                    NucleusLogger.QUERY.debug(Localiser.msg("021082", query, results.size()));
                }
            }
            return results;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#getInMemoryEvaluatorForMethod(java.lang.Class, java.lang.String)
     */
    @Override
    public InvocationEvaluator getInMemoryEvaluatorForMethod(Class type, String methodName)
    {
        // Hardcode support for Array.size()/Array.length()/Array.contains() since not currently pluggable
        if (type != null && type.isArray())
        {
            // TODO Cache these
            if (methodName.equals("size") || methodName.equals("length"))
            {
                return new ArraySizeMethod();
            }
            else if (methodName.equals("contains"))
            {
                return new ArrayContainsMethod();
            }
        }

        Map<Object, InvocationEvaluator> evaluatorsForMethod = inmemoryQueryMethodEvaluatorMap.get(methodName);
        if (evaluatorsForMethod != null)
        {
            Iterator evaluatorClsIter = evaluatorsForMethod.entrySet().iterator();
            while (evaluatorClsIter.hasNext())
            {
                Map.Entry<Object, InvocationEvaluator> entry = (Entry<Object, InvocationEvaluator>) evaluatorClsIter.next();
                Object clsKey = entry.getKey();
                if (clsKey instanceof Class && ((Class)clsKey).isAssignableFrom(type))
                {
                    return entry.getValue();
                }
                else if (clsKey instanceof String && ((String)clsKey).equals("STATIC") && type == null)
                {
                    // Can only be one static method so just return it
                    return entry.getValue();
                }
            }
            return null;
        }

        // Not yet loaded anything for this method
        ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(type != null ? type.getClassLoader() : null);
        PluginManager pluginMgr = nucleusCtx.getPluginManager();
        ConfigurationElement[] elems = pluginMgr.getConfigurationElementsForExtension("org.datanucleus.query_method_evaluators", "method", methodName);
        Map<Object, InvocationEvaluator> evaluators = new HashMap();
        InvocationEvaluator requiredEvaluator = null;
        if (elems == null)
        {
            return null;
        }

        for (int i=0;i<elems.length;i++)
        {
            try
            {
                String evalName = elems[i].getAttribute("evaluator");
                InvocationEvaluator eval = (InvocationEvaluator)pluginMgr.createExecutableExtension("org.datanucleus.query_method_evaluators", 
                    new String[] {"method", "evaluator"}, new String[] {methodName, evalName}, "evaluator", null, null);

                String elemClsName = elems[i].getAttribute("class");
                if (elemClsName != null && StringUtils.isWhitespace(elemClsName))
                {
                    elemClsName = null;
                }
                if (elemClsName == null)
                {
                    // Static method call
                    if (type == null)
                    {
                        // Evaluator is applicable to the required type
                        requiredEvaluator = eval;
                    }
                    evaluators.put("STATIC", eval);
                }
                else
                {
                    Class elemCls = clr.classForName(elemClsName);
                    if (elemCls.isAssignableFrom(type))
                    {
                        // Evaluator is applicable to the required type
                        requiredEvaluator = eval;
                    }
                    evaluators.put(elemCls, eval);
                }
            }
            catch (Exception e)
            {
                // Impossible to create the evaluator (class doesn't exist?) TODO Log this?
            }
        }

        // Store evaluators for this method name
        inmemoryQueryMethodEvaluatorMap.put(methodName, evaluators);
        return requiredEvaluator;
    }
}