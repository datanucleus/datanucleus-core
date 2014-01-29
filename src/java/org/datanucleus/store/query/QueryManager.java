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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.NucleusContext;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.cache.QueryCompilationCache;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.evaluator.memory.ArrayContainsMethodEvaluator;
import org.datanucleus.query.evaluator.memory.ArraySizeMethodEvaluator;
import org.datanucleus.query.evaluator.memory.InvocationEvaluator;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.query.cache.QueryDatastoreCompilationCache;
import org.datanucleus.store.query.cache.QueryResultsCache;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Manages the runtime, metadata and lifecycle of queries.
 * Provides caching of query compilations.
 */
public class QueryManager
{
    /** Localisation of messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        ClassConstants.NUCLEUS_CONTEXT_LOADER);

    protected NucleusContext nucleusCtx;

    protected StoreManager storeMgr;

    /** Cache for generic query compilations. */
    QueryCompilationCache queryCompilationCache = null;

    /** Cache for datastore query compilations. */
    QueryDatastoreCompilationCache queryCompilationCacheDatastore = null;

    /** Cache for query results. */
    QueryResultsCache queryResultsCache = null;

    /** Cache of InvocationEvaluator objects keyed by the method name. */
    Map<String, Map<Object, InvocationEvaluator>> queryMethodEvaluatorMap = new HashMap();

    public QueryManager(NucleusContext nucleusContext, StoreManager storeMgr)
    {
        this.nucleusCtx = nucleusContext;
        this.storeMgr = storeMgr;

        initialiseQueryCaches();
    }

    /**
     * Method to find and initialise the query cache, for caching query compilations.
     */
    protected void initialiseQueryCaches()
    {
        // Instantiate the query compilation cache (generic)
        Configuration conf = nucleusCtx.getConfiguration();
        String cacheType = conf.getStringProperty("datanucleus.cache.queryCompilation.type");
        if (cacheType != null && !cacheType.equalsIgnoreCase("none"))
        {
            String cacheClassName = nucleusCtx.getPluginManager().getAttributeValueForExtension(
                "org.datanucleus.cache_query_compilation", "name", cacheType, "class-name");
            if (cacheClassName == null)
            {
                // Plugin of this name not found
                throw new NucleusUserException(LOCALISER.msg("021500", cacheType)).setFatal();
            }

            try
            {
                // Create an instance of the Query Cache
                queryCompilationCache = (QueryCompilationCache)nucleusCtx.getPluginManager().createExecutableExtension(
                    "org.datanucleus.cache_query_compilation", "name", cacheType, "class-name", 
                    new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[] {nucleusCtx});
                if (NucleusLogger.CACHE.isDebugEnabled())
                {
                    NucleusLogger.CACHE.debug(LOCALISER.msg("021502", cacheClassName));
                }
            }
            catch (Exception e)
            {
                // Class name for this Query cache plugin is not found!
                throw new NucleusUserException(LOCALISER.msg("021501", cacheType, cacheClassName), e).setFatal();
            }
        }

        // Instantiate the query compilation cache (datastore)
        cacheType = conf.getStringProperty("datanucleus.cache.queryCompilationDatastore.type");
        if (cacheType != null && !cacheType.equalsIgnoreCase("none"))
        {
            String cacheClassName = nucleusCtx.getPluginManager().getAttributeValueForExtension(
                "org.datanucleus.cache_query_compilation_store", "name", cacheType, "class-name");
            if (cacheClassName == null)
            {
                // Plugin of this name not found
                throw new NucleusUserException(LOCALISER.msg("021500", cacheType)).setFatal();
            }

            try
            {
                // Create an instance of the Query Cache
                queryCompilationCacheDatastore = 
                    (QueryDatastoreCompilationCache)nucleusCtx.getPluginManager().createExecutableExtension(
                    "org.datanucleus.cache_query_compilation_store", "name", cacheType, "class-name", 
                    new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[] {nucleusCtx});
                if (NucleusLogger.CACHE.isDebugEnabled())
                {
                    NucleusLogger.CACHE.debug(LOCALISER.msg("021502", cacheClassName));
                }
            }
            catch (Exception e)
            {
                // Class name for this Query cache plugin is not found!
                throw new NucleusUserException(LOCALISER.msg("021501", cacheType, cacheClassName), e).setFatal();
            }
        }

        // Instantiate the query results cache
        cacheType = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_TYPE);
        if (cacheType != null && !cacheType.equalsIgnoreCase("none"))
        {
            String cacheClassName = nucleusCtx.getPluginManager().getAttributeValueForExtension(
                "org.datanucleus.cache_query_result", "name", cacheType, "class-name");
            if (cacheClassName == null)
            {
                // Plugin of this name not found
                throw new NucleusUserException(LOCALISER.msg("021500", cacheType)).setFatal();
            }

            try
            {
                // Create an instance of the Query Cache
                queryResultsCache = 
                    (QueryResultsCache)nucleusCtx.getPluginManager().createExecutableExtension(
                    "org.datanucleus.cache_query_result", "name", cacheType, "class-name", 
                    new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[] {nucleusCtx});
                if (NucleusLogger.CACHE.isDebugEnabled())
                {
                    NucleusLogger.CACHE.debug(LOCALISER.msg("021502", cacheClassName));
                }
            }
            catch (Exception e)
            {
                // Class name for this Query cache plugin is not found!
                throw new NucleusUserException(LOCALISER.msg("021501", cacheType, cacheClassName), e).setFatal();
            }
        }
    }

    public void close()
    {
        if (queryCompilationCache != null)
        {
            queryCompilationCache.close();
            queryCompilationCache = null;
        }
        if (queryCompilationCacheDatastore != null)
        {
            queryCompilationCacheDatastore.close();
            queryCompilationCacheDatastore = null;
        }
        if (queryResultsCache != null)
        {
            queryResultsCache.close();
            queryResultsCache = null;
        }

        queryMethodEvaluatorMap.clear();
        queryMethodEvaluatorMap = null;
    }

    /**
     * Method to generate a new query using the passed query as basis.
     * @param language The query language
     * @param ec ExecutionContext
     * @param query The query filter (String) or a previous Query
     * @return The Query
     */
    public Query newQuery(String language, ExecutionContext ec, Object query)
    {
        if (language == null)
        {
            return null;
        }

        String languageImpl = language;

        // Find the query support for this language and this datastore
        try
        {
            if (query == null)
            {
                Class[] argsClass = new Class[] {ClassConstants.STORE_MANAGER, ClassConstants.EXECUTION_CONTEXT};
                Object[] args = new Object[] {storeMgr, ec};
                Query q = (Query) ec.getNucleusContext().getPluginManager().createExecutableExtension(
                    "org.datanucleus.store_query_query", new String[] {"name", "datastore"},
                    new String[] {languageImpl, ec.getStoreManager().getStoreManagerKey()}, 
                    "class-name", argsClass, args);
                if (q == null)
                {
                    // No query support for this language
                    throw new NucleusException(LOCALISER.msg("021034", languageImpl, ec.getStoreManager().getStoreManagerKey()));
                }
                return q;
            }
            else
            {
                Query q = null;
                if (query instanceof String)
                {
                    // Try XXXQuery(ExecutionContext, String);
                    Class[] argsClass = new Class[]{ClassConstants.STORE_MANAGER, ClassConstants.EXECUTION_CONTEXT, String.class};
                    Object[] args = new Object[]{storeMgr, ec, query};
                    q = (Query) ec.getNucleusContext().getPluginManager().createExecutableExtension(
                        "org.datanucleus.store_query_query", new String[] {"name", "datastore"},
                        new String[] {languageImpl, ec.getStoreManager().getStoreManagerKey()}, 
                        "class-name", argsClass, args);
                    if (q == null)
                    {
                        // No query support for this language
                        throw new NucleusException(LOCALISER.msg("021034", languageImpl, ec.getStoreManager().getStoreManagerKey()));
                    }
                }
                else if (query instanceof Query)
                {
                    // Try XXXQuery(StoreManager, ExecutionContext, Query.class);
                    Class[] argsClass = new Class[]{ClassConstants.STORE_MANAGER, ClassConstants.EXECUTION_CONTEXT, query.getClass()};
                    Object[] args = new Object[]{storeMgr, ec, query};
                    q = (Query) ec.getNucleusContext().getPluginManager().createExecutableExtension(
                        "org.datanucleus.store_query_query", new String[] {"name", "datastore"},
                        new String[] {languageImpl, ec.getStoreManager().getStoreManagerKey()}, 
                        "class-name", argsClass, args);
                    if (q == null)
                    {
                        // No query support for this language
                        throw new NucleusException(LOCALISER.msg("021034", languageImpl, ec.getStoreManager().getStoreManagerKey()));
                    }
                }
                else
                {
                    // Try XXXQuery(StoreManager, ExecutionContext, Object);
                    Class[] argsClass = new Class[]{ClassConstants.STORE_MANAGER, ClassConstants.EXECUTION_CONTEXT, Object.class};
                    Object[] args = new Object[]{storeMgr, ec, query};
                    q = (Query) ec.getNucleusContext().getPluginManager().createExecutableExtension(
                        "org.datanucleus.store_query_query", new String[] {"name", "datastore"},
                        new String[] {languageImpl, ec.getStoreManager().getStoreManagerKey()}, 
                        "class-name", argsClass, args);
                    if (q == null)
                    {
                        // No query support for this language
                        throw new NucleusException(LOCALISER.msg("021034", languageImpl, ec.getStoreManager().getStoreManagerKey()));
                    }
                }
                return q;
            }
        }
        catch (InvocationTargetException e)
        {
            Throwable t = e.getTargetException();
            if (t instanceof RuntimeException)
            {
                throw (RuntimeException) t;
            }
            else if (t instanceof Error)
            {
                throw (Error) t;
            }
            else
            {
                throw new NucleusException(t.getMessage(), t).setFatal();
            }
        }
        catch (Exception e)
        {
            throw new NucleusException(e.getMessage(), e).setFatal();
        }
    }

    /**
     * Accessor for the generic compilation cache.
     * @return The cache of generic compilations
     */
    public QueryCompilationCache getQueryCompilationCache()
    {
        return queryCompilationCache;
    }

    /**
     * Method to store the compilation for a query.
     * @param language Language of the query
     * @param query The query string
     * @param compilation The compilation of this query
     */
    public synchronized void addQueryCompilation(String language, String query, QueryCompilation compilation)
    {
        if (queryCompilationCache != null)
        {
            String queryKey = language + ":" + query;
            queryCompilationCache.put(queryKey, compilation);
        }
    }

    /**
     * Accessor for a Query compilation for the specified query and language.
     * @param language Language of the query
     * @param query Query string
     * @return The compilation (if present)
     */
    public synchronized QueryCompilation getQueryCompilationForQuery(String language, String query)
    {
        if (queryCompilationCache != null)
        {
            String queryKey = language + ":" + query;
            QueryCompilation compilation = queryCompilationCache.get(queryKey);
            if (compilation != null)
            {
                if (NucleusLogger.QUERY.isDebugEnabled())
                {
                    NucleusLogger.QUERY.debug(LOCALISER.msg("021079", query, language));
                }
                return compilation;
            }
        }
        return null;
    }

    /**
     * Accessor for the datastore compilation cache.
     * @return The cache of datastore compilations
     */
    public QueryDatastoreCompilationCache getQueryDatastoreCompilationCache()
    {
        return queryCompilationCacheDatastore;
    }

    /**
     * Method to store the datastore-specific compilation for a query.
     * @param datastore The datastore identifier
     * @param language The query language
     * @param query The query (string form)
     * @param compilation The compiled information
     */
    public synchronized void addDatastoreQueryCompilation(String datastore, String language, String query,
            Object compilation)
    {
        if (queryCompilationCacheDatastore != null)
        {
            String queryKey = language + ":" + query;
            queryCompilationCacheDatastore.put(queryKey, compilation);
        }
    }

    /**
     * Method to remove a cached datastore query compilation.
     * @param datastore The datastore
     * @param language The language
     * @param query The query (string form)
     */
    public synchronized void deleteDatastoreQueryCompilation(String datastore, String language, String query)
    {
        if (queryCompilationCacheDatastore != null)
        {
            String queryKey = language + ":" + query;
            queryCompilationCacheDatastore.evict(queryKey);
        }
    }

    /**
     * Accessor for the datastore-specific compilation for a query.
     * @param datastore The datastore identifier
     * @param language The query language
     * @param query The query (string form)
     * @return The compiled information (if available)
     */
    public synchronized Object getDatastoreQueryCompilation(String datastore, String language, String query)
    {
        if (queryCompilationCacheDatastore != null)
        {
            String queryKey = language + ":" + query;
            Object compilation = queryCompilationCacheDatastore.get(queryKey);
            if (compilation != null)
            {
                if (NucleusLogger.QUERY.isDebugEnabled())
                {
                    NucleusLogger.QUERY.debug(LOCALISER.msg("021080", query, language, datastore));
                }
                return compilation;
            }
        }
        return null;
    }

    public QueryResultsCache getQueryResultsCache()
    {
        return queryResultsCache;
    }

    public void evictQueryResultsForType(Class cls)
    {
        if (queryResultsCache != null)
        {
            queryResultsCache.evict(cls);
        }
    }

    /**
     * Method to store the results for a query.
     * @param query The query
     * @param params Map of parameter values keyed by param name
     * @param results The results (List of object identities)
     */
    public synchronized void addDatastoreQueryResult(Query query, Map params, List<Object> results)
    {
        if (queryResultsCache != null)
        {
            String queryKey = QueryUtils.getKeyForQueryResultsCache(query, params);
            queryResultsCache.put(queryKey, results);
            if (NucleusLogger.QUERY.isDebugEnabled())
            {
                NucleusLogger.QUERY.debug(LOCALISER.msg("021081", query, results.size()));
            }
        }
    }

    /**
     * Accessor for the results for a query.
     * @param query The query
     * @param params Map of parameter values keyed by param name
     * @return The results (List of object identities)
     */
    public synchronized List<Object> getDatastoreQueryResult(Query query, Map params)
    {
        if (queryResultsCache != null)
        {
            String queryKey = QueryUtils.getKeyForQueryResultsCache(query, params);
            List<Object> results = queryResultsCache.get(queryKey);
            if (results != null)
            {
                if (NucleusLogger.QUERY.isDebugEnabled())
                {
                    NucleusLogger.QUERY.debug(LOCALISER.msg("021082", query, results.size()));
                }
            }
            return results;
        }
        return null;
    }

    /**
     * Accessor for an evaluator for invocation of the specified method for the supplied type.
     * If it is not a supported method for that type then returns null.
     * @param type The class name
     * @param methodName Name of the method
     * @return Evaluator suitable for this type with this method name
     */
    public InvocationEvaluator getInMemoryEvaluatorForMethod(Class type, String methodName)
    {
        // Hardcode support for Array.size()/Array.length()/Array.contains() since not currently pluggable
        if (type != null && type.isArray())
        {
            // TODO Cache these
            if (methodName.equals("size") || methodName.equals("length"))
            {
                return new ArraySizeMethodEvaluator();
            }
            else if (methodName.equals("contains"))
            {
                return new ArrayContainsMethodEvaluator();
            }
        }

        Map<Object, InvocationEvaluator> evaluatorsForMethod = queryMethodEvaluatorMap.get(methodName);
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
        else
        {
            // Not yet loaded anything for this method
            ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(type != null ? type.getClassLoader() : null);
            PluginManager pluginMgr = nucleusCtx.getPluginManager();
            ConfigurationElement[] elems = pluginMgr.getConfigurationElementsForExtension(
                "org.datanucleus.query_method_evaluators", "method", methodName);
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
                    InvocationEvaluator eval =
                        (InvocationEvaluator)pluginMgr.createExecutableExtension(
                        "org.datanucleus.query_method_evaluators", new String[] {"method", "evaluator"},
                        new String[] {methodName, evalName}, "evaluator", null, null);

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
            queryMethodEvaluatorMap.put(methodName, evaluators);
            return requiredEvaluator;
        }
    }
}