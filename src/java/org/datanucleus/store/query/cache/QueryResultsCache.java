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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.datanucleus.store.query.Query;

/**
 * Cache for query results.
 */
public interface QueryResultsCache extends Serializable
{
    /**
     * Method to close the cache when no longer needed. Provides a hook to release resources etc.
     */
    void close();

    /**
     * Method to evict all queries that use the provided class as candidate.
     * This is usually called when an instance of the candidate has been changed in the datastore.
     * @param candidate The candidate
     */
    void evict(Class candidate);

    /**
     * Evict the query from the results cache.
     * @param query The query to evict (evicts all use of this query, with any params)
     */
    void evict(Query query);

    /**
     * Evict the query with the specified params from the results cache.
     * @param query The query to evict
     * @param params The parameters
     */
    void evict(Query query, Map params);

    /**
     * Method to clear the cache.
     */
    void evictAll();

    /**
     * Method to pin the specified query in the cache, preventing garbage collection.
     * @param query The query
     */
    void pin(Query query);

    /**
     * Method to pin the specified query in the cache, preventing garbage collection.
     * @param query The query
     * @param params Its params
     */
    void pin(Query query, Map params);

    /**
     * Method to unpin the specified query from the cache, allowing garbage collection.
     * @param query The query
     */
    void unpin(Query query);

    /**
     * Method to unpin the specified query from the cache, allowing garbage collection.
     * @param query The query
     * @param params Its params
     */
    void unpin(Query query, Map params);

    /**
     * Accessor for whether the cache is empty.
     * @return Whether it is empty.
     */
    boolean isEmpty();

    /**
     * Accessor for the total number of results in the query cache.
     * @return Number of queries
     */
    int size();

    /**
     * Accessor for the results from the cache.
     * @param queryKey The query key
     * @return The cached query result ids
     */
    List<Object> get(String queryKey);

    /**
     * Method to put an object in the cache.
     * @param queryKey The query key
     * @param results The results for this query
     * @return The result ids previously associated with this query (if any)
     */
    List<Object> put(String queryKey, List<Object> results);

    /**
     * Accessor for whether the specified query is in the cache
     * @param queryKey The query key
     * @return Whether it is in the cache
     */
    boolean contains(String queryKey);
}