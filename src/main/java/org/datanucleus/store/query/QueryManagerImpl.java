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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.query.cache.JavaxCacheQueryCompilationCache;
import org.datanucleus.store.query.cache.JavaxCacheQueryDatastoreCompilationCache;
import org.datanucleus.store.query.cache.JavaxCacheQueryResultCache;
import org.datanucleus.store.query.cache.QueryCompilationCache;
import org.datanucleus.store.query.cache.QueryDatastoreCompilationCache;
import org.datanucleus.store.query.cache.QueryResultsCache;
import org.datanucleus.store.query.cache.SoftQueryCompilationCache;
import org.datanucleus.store.query.cache.SoftQueryDatastoreCompilationCache;
import org.datanucleus.store.query.cache.SoftQueryResultsCache;
import org.datanucleus.store.query.cache.StrongQueryCompilationCache;
import org.datanucleus.store.query.cache.StrongQueryDatastoreCompilationCache;
import org.datanucleus.store.query.cache.StrongQueryResultsCache;
import org.datanucleus.store.query.cache.WeakQueryCompilationCache;
import org.datanucleus.store.query.cache.WeakQueryDatastoreCompilationCache;
import org.datanucleus.store.query.cache.WeakQueryResultsCache;
import org.datanucleus.store.query.compiler.QueryCompilation;
import org.datanucleus.store.query.inmemory.InvocationEvaluator;
import org.datanucleus.store.query.inmemory.method.ArrayContainsMethod;
import org.datanucleus.store.query.inmemory.method.ArraySizeMethod;
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

    /** Cache of InvocationEvaluator objects keyed by the "class:methodName", for use by in-memory querying. */
    protected Map<String, InvocationEvaluator> inmemoryQueryMethodEvaluatorByName = new ConcurrentHashMap<>();

    protected Map<String, String> queryMethodAliasByPrefix = null;

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
        if ("soft".equalsIgnoreCase(cacheType))
        {
            queryCompilationCache = new SoftQueryCompilationCache(nucleusContext);
        }
        else if ("weak".equalsIgnoreCase(cacheType))
        {
            queryCompilationCache = new WeakQueryCompilationCache(nucleusCtx);
        }
        else if ("strong".equalsIgnoreCase(cacheType))
        {
            queryCompilationCache = new StrongQueryCompilationCache(nucleusCtx);
        }
        else if ("javax.cache".equalsIgnoreCase(cacheType))
        {
            queryCompilationCache = new JavaxCacheQueryCompilationCache(nucleusCtx);
        }
        else
        {
            // Try via the plugin mechanism
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
        }

        // QueryCompilationCache (datastore compilation)
        cacheType = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYCOMPILEDATASTORE_TYPE);
        if (cacheType == null)
        {
            cacheType = "soft";
        }
        if ("soft".equalsIgnoreCase(cacheType))
        {
            queryCompilationCacheDatastore = new SoftQueryDatastoreCompilationCache(nucleusContext);
        }
        else if ("weak".equalsIgnoreCase(cacheType))
        {
            queryCompilationCacheDatastore = new WeakQueryDatastoreCompilationCache(nucleusCtx);
        }
        else if ("strong".equalsIgnoreCase(cacheType))
        {
            queryCompilationCacheDatastore = new StrongQueryDatastoreCompilationCache(nucleusCtx);
        }
        else if ("javax.cache".equalsIgnoreCase(cacheType))
        {
            queryCompilationCacheDatastore = new JavaxCacheQueryDatastoreCompilationCache(nucleusCtx);
        }
        else
        {
            // Try via the plugin mechanism
            String cacheClassName = nucleusCtx.getPluginManager().getAttributeValueForExtension("org.datanucleus.cache_query_compilation_store", "name", cacheType, "class-name");
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
        }

        // Query results cache
        cacheType = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_TYPE);
        if (cacheType == null)
        {
            cacheType = "soft";
        }
        if ("soft".equalsIgnoreCase(cacheType))
        {
            queryResultsCache = new SoftQueryResultsCache(nucleusContext);
        }
        else if ("weak".equalsIgnoreCase(cacheType))
        {
            queryResultsCache = new WeakQueryResultsCache(nucleusCtx);
        }
        else if ("strong".equalsIgnoreCase(cacheType))
        {
            queryResultsCache = new StrongQueryResultsCache(nucleusCtx);
        }
        else if ("javax.cache".equalsIgnoreCase(cacheType))
        {
            queryResultsCache = new JavaxCacheQueryResultCache(nucleusCtx);
        }
        else
        {
            // Try via the plugin mechanism
            String cacheClassName = nucleusCtx.getPluginManager().getAttributeValueForExtension("org.datanucleus.cache_query_result", "name", cacheType, "class-name");
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

        // JDOQL/JPQL query method alias prefixes (extension)
        queryMethodAliasByPrefix = new HashMap<String, String>();

        // a). built-in aliases for standard JDOQL
        queryMethodAliasByPrefix.put("JDOHelper", "JDOHelper");
        queryMethodAliasByPrefix.put("javax.jdo.JDOHelper", "JDOHelper");
        queryMethodAliasByPrefix.put("Math", "Math");
        queryMethodAliasByPrefix.put("java.lang.Math", "Math");

        // b). use plugin mechanism for extension aliases
        ConfigurationElement[] queryMethodAliases = nucleusCtx.getPluginManager().getConfigurationElementsForExtension("org.datanucleus.query_method_prefix", null, null);
        if (queryMethodAliases != null && queryMethodAliases.length > 0)
        {
            for (int i=0;i<queryMethodAliases.length;i++)
            {
                queryMethodAliasByPrefix.put(queryMethodAliases[i].getAttribute("prefix"), queryMethodAliases[i].getAttribute("alias"));
            }
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

        inmemoryQueryMethodEvaluatorByName.clear();
        inmemoryQueryMethodEvaluatorByName = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.QueryManager#getQueryMethodAliasesByPrefix()
     */
    @Override
    public Map<String, String> getQueryMethodAliasesByPrefix()
    {
        return queryMethodAliasByPrefix;
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
        String lookupName = type != null ? (type.getName() + ":" + methodName) : methodName;

        // Hardcode support for Array.size()/Array.length()/Array.contains() since not currently pluggable
        if (type != null && type.isArray())
        {
            lookupName = "ARRAY:" + methodName;
        }

        InvocationEvaluator eval = inmemoryQueryMethodEvaluatorByName.get(lookupName);
        if (eval != null)
        {
            return eval;
        }

        // Load built-in handler for this class+method
        ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(type != null ? type.getClassLoader() : null);
        if (type == null)
        {
            if ("Math.abs".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.AbsFunction();
            if ("Math.sqrt".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.SqrtFunction();
            if ("Math.acos".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ArcCosineFunction();
            if ("Math.asin".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ArcSineFunction();
            if ("Math.atan".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ArcTangentFunction();
            if ("Math.cos".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CosineFunction();
            if ("Math.sin".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.SineFunction();
            if ("Math.tan".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TangentFunction();
            if ("Math.log".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LogFunction();
            if ("Math.exp".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ExpFunction();
            if ("Math.floor".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.FloorFunction();
            if ("Math.ceil".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CeilFunction();
            if ("Math.toDegrees".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DegreesFunction();
            if ("Math.toRadians".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.RadiansFunction();

            if ("CURRENT_DATE".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CurrentDateFunction();
            if ("CURRENT_TIME".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CurrentTimeFunction();
            if ("CURRENT_TIMESTAMP".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CurrentTimestampFunction();
            if ("LOCAL_DATE".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateFunction();
            if ("LOCAL_TIME".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalTimeFunction();
            if ("LOCAL_DATETIME".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateTimeFunction();
            if ("ABS".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.AbsFunction();
            if ("SQRT".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.SqrtFunction();
            if ("MOD".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ModFunction();
            if ("COALESCE".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CoalesceFunction();
            if ("COS".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CosineFunction();
            if ("SIN".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.SineFunction();
            if ("TAN".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TangentFunction();
            if ("ACOS".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ArcCosineFunction();
            if ("ASIN".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ArcSineFunction();
            if ("ATAN".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ArcTangentFunction();
            if ("CEIL".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CeilFunction();
            if ("CEILING".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CeilFunction();
            if ("FLOOR".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.FloorFunction();
            if ("LOG".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LogFunction();
            if ("EXP".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ExpFunction();
            if ("NULLIF".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.NullIfFunction();
            if ("SIZE".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.SizeFunction();
            if ("UPPER".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.UpperFunction();
            if ("LOWER".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LowerFunction();
            if ("LENGTH".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LengthFunction();
            if ("CONCAT".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ConcatFunction();
            if ("SUBSTRING".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.SubstringFunction();
            if ("LOCATE".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocateFunction();
            if ("TRIM".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TrimFunction();
            if ("TRIM_LEADING".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TrimFunction();
            if ("TRIM_TRAILING".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TrimFunction();
            if ("DEGREES".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DegreesFunction();
            if ("RADIANS".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.RadiansFunction();
            if ("YEAR".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TemporalYearMethod();
            if ("MONTH".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TemporalMonthMethod();
            if ("MONTH_JAVA".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TemporalMonthJavaMethod();
            if ("DAY".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TemporalDayMethod();
            if ("HOUR".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TemporalHourMethod();
            if ("MINUTE".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TemporalMinuteMethod();
            if ("SECOND".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.TemporalSecondMethod();

            if (eval != null)
            {
                inmemoryQueryMethodEvaluatorByName.put(lookupName, eval);
                return eval;
            }
        }
        else
        {
            if (type.isArray())
            {
                if ("size".equals(methodName)) eval = new ArraySizeMethod();
                if ("length".equals(methodName)) eval = new ArraySizeMethod();
                if ("contains".equals(methodName)) eval = new ArrayContainsMethod();
            }
            else if (type.isEnum())
            {
                if ("matches".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.EnumMatchesMethod();
                if ("toString".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.EnumToStringMethod();
                if ("ordinal".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.EnumOrdinalMethod();
            }
            else if ("java.lang.String".equals(type.getName()))
            {
                if ("charAt".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringCharAtMethod();
                if ("concat".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringConcatMethod();
                if ("endsWith".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringEndsWithMethod();
                if ("equals".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringEqualsMethod();
                if ("equalsIgnoreCase".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringEqualsIgnoreCaseMethod();
                if ("indexOf".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringIndexOfMethod();
                if ("length".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringLengthMethod();
                if ("matches".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringMatchesMethod();
                if ("startsWith".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringStartsWithMethod();
                if ("substring".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringSubstringMethod();
                if ("toUpperCase".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringToUpperCaseMethod();
                if ("toLowerCase".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringToLowerCaseMethod();
                if ("trim".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringTrimMethod();
                if ("trimLeft".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringTrimLeftMethod();
                if ("trimRight".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.StringTrimRightMethod();
            }
            else if (java.util.Collection.class.isAssignableFrom(type))
            {
                if ("size".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ContainerSizeMethod();
                if ("isEmpty".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ContainerIsEmptyMethod();
                if ("contains".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.CollectionContainsMethod();
                if ("get".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ListGetMethod();
                if ("indexOf".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ListIndexOfMethod();
            }
            else if (java.util.Map.class.isAssignableFrom(type))
            {
                if ("size".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ContainerSizeMethod();
                if ("isEmpty".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ContainerIsEmptyMethod();
                if ("containsKey".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.MapContainsKeyMethod();
                if ("containsValue".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.MapContainsValueMethod();
                if ("containsEntry".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.MapContainsEntryMethod();
                if ("get".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.MapGetMethod();
            }
            else if (java.util.Optional.class.isAssignableFrom(type))
            {
                if ("isPresent".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.OptionalIsPresentMethod();
                if ("get".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.OptionalGetMethod();
                if ("orElse".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.OptionalOrElseMethod();
            }
            else if (java.util.Date.class.isAssignableFrom(type))
            {
                if ("getTime".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetTimeMethod();
                if ("getDay".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetDayMethod();
                if ("getDate".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetDayMethod();
                if ("getDayOfWeek".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetDayOfWeekMethod();
                if ("getMonth".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetMonthMethod();
                if ("getYear".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetYearMethod();
                if ("getHour".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetHoursMethod();
                if ("getMinute".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetMinutesMethod();
                if ("getSecond".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.DateGetSecondsMethod();
            }
            else if (java.time.LocalDate.class.isAssignableFrom(type))
            {
                if ("getDayOfMonth".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateGetDayOfMonth();
                if ("getDayOfWeek".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateGetDayOfWeek();
                if ("getMonthValue".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateGetMonthValue();
                if ("getYear".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateGetYear();
            }
            else if (java.time.LocalTime.class.isAssignableFrom(type))
            {
                if ("getHour".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalTimeGetHour();
                if ("getMinute".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalTimeGetMinute();
                if ("getSecond".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalTimeGetSecond();
            }
            else if (java.time.LocalDateTime.class.isAssignableFrom(type))
            {
                if ("getDayOfMonth".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateTimeGetDayOfMonth();
                if ("getDayOfWeek".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateTimeGetDayOfWeek();
                if ("getMonthValue".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateTimeGetMonthValue();
                if ("getYear".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateTimeGetYear();
                if ("getHour".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateTimeGetHour();
                if ("getMinute".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateTimeGetMinute();
                if ("getSecond".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.LocalDateTimeGetSecond();
            }
            else if (java.time.MonthDay.class.isAssignableFrom(type))
            {
                if ("getDayOfMonth".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.MonthDayGetDayOfMonth();
                if ("getMonthValue".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.MonthDayGetMonthValue();
            }
            else if (java.time.Period.class.isAssignableFrom(type))
            {
                if ("getDays".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.PeriodGetDays();
                if ("getMonths".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.PeriodGetMonths();
                if ("getYears".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.PeriodGetYears();
            }
            else if (java.time.YearMonth.class.isAssignableFrom(type))
            {
                if ("getYear".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.YearMonthGetYear();
                if ("getMonthValue".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.YearMonthGetMonthValue();
            }

            if (eval == null && java.lang.Object.class.isAssignableFrom(type) && "getClass".equals(methodName)) eval = new org.datanucleus.store.query.inmemory.method.ObjectGetClassMethod();

            if (eval != null)
            {
                inmemoryQueryMethodEvaluatorByName.put(lookupName, eval);
                return eval;
            }
        }

        // Fallback to the plugin mechanism
        PluginManager pluginMgr = nucleusCtx.getPluginManager();
        ConfigurationElement[] elems = pluginMgr.getConfigurationElementsForExtension("org.datanucleus.query_method_evaluators", "method", methodName);
        if (elems == null)
        {
            return null;
        }

        // TODO Lookup with class specified when type != null
        InvocationEvaluator requiredEvaluator = null;
        for (int i=0;i<elems.length;i++)
        {
            try
            {
                String evalName = elems[i].getAttribute("evaluator");
                eval = (InvocationEvaluator)pluginMgr.createExecutableExtension("org.datanucleus.query_method_evaluators", 
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

                    inmemoryQueryMethodEvaluatorByName.put(lookupName, eval);
                }
                else
                {
                    Class elemCls = clr.classForName(elemClsName);
                    if (elemCls.isAssignableFrom(type))
                    {
                        // Evaluator is applicable to the required type
                        requiredEvaluator = eval;
                    }

                    inmemoryQueryMethodEvaluatorByName.put(lookupName, eval);
                }
            }
            catch (Exception e)
            {
                // Impossible to create the evaluator (class doesn't exist?) TODO Log this?
            }
        }

        return requiredEvaluator;
    }
}