/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
***********************************************************************/
package org.datanucleus.store.query;

import java.util.List;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.compiler.QueryCompilationCache;
import org.datanucleus.query.inmemory.InvocationEvaluator;
import org.datanucleus.store.query.cache.QueryDatastoreCompilationCache;
import org.datanucleus.store.query.cache.QueryResultsCache;

/**
 * Interface providing a way of creating instances of queries for datastores, caching of generic and datastore compilations,
 * as well as caching query results.
 */
public interface QueryManager
{
    /**
     * Close the QueryManager, releasing all cached compilations and results.
     */
    void close();

    /**
     * Method to generate a new query using the passed query as basis.
     * @param language The query language
     * @param ec ExecutionContext
     * @param query The query filter (String) or a previous Query
     * @return The Query
     */
    Query newQuery(String language, ExecutionContext ec, Object query);

    /**
     * Accessor for the generic compilation cache.
     * @return The cache of generic compilations
     */
    QueryCompilationCache getQueryCompilationCache();

    /**
     * Method to store the compilation for a query.
     * @param language Language of the query
     * @param query The query string
     * @param compilation The compilation of this query
     */
    void addQueryCompilation(String language, String query, QueryCompilation compilation);

    /**
     * Accessor for a Query compilation for the specified query and language.
     * @param language Language of the query
     * @param query Query string
     * @return The compilation (if present)
     */
    QueryCompilation getQueryCompilationForQuery(String language, String query);

    /**
     * Accessor for the datastore compilation cache.
     * @return The cache of datastore compilations
     */
    QueryDatastoreCompilationCache getQueryDatastoreCompilationCache();

    /**
     * Accessor for the datastore-specific compilation for a query.
     * @param datastore The datastore identifier
     * @param language The query language
     * @param query The query (string form)
     * @return The compiled information (if available)
     */
    Object getDatastoreQueryCompilation(String datastore, String language, String query);

    /**
     * Method to store the datastore-specific compilation for a query.
     * @param datastore The datastore identifier
     * @param language The query language
     * @param query The query (string form)
     * @param compilation The compiled information
     */
    void addDatastoreQueryCompilation(String datastore, String language, String query, Object compilation);

    /**
     * Method to remove a cached datastore query compilation.
     * @param datastore The datastore
     * @param language The language
     * @param query The query (string form)
     */
    void deleteDatastoreQueryCompilation(String datastore, String language, String query);

    /**
     * Accessor for the query results cache.
     * @return Query results cache (if present)
     */
    QueryResultsCache getQueryResultsCache();

    /**
     * Accessor for the results for a query.
     * @param query The query
     * @param params Map of parameter values keyed by param name
     * @return The results (List of object identities)
     */
    List<Object> getQueryResult(Query query, Map params);

    /**
     * Method to evict all query results for the specified candidate type.
     * @param cls Candidate type
     */
    void evictQueryResultsForType(Class cls);

    /**
     * Method to store the results for a query.
     * @param query The query
     * @param params Map of parameter values keyed by param name
     * @param results The results (List of object identities)
     */
    void addQueryResult(Query query, Map params, List<Object> results);

    /**
     * Accessor for an evaluator for invocation of the specified method for the supplied type.
     * If it is not a supported method for that type then returns null.
     * @param type The class name
     * @param methodName Name of the method
     * @return Evaluator suitable for this type with this method name
     */
    InvocationEvaluator getInMemoryEvaluatorForMethod(Class type, String methodName);
}