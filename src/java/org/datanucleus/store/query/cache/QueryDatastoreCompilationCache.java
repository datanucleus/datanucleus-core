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

/**
 * Cache for query compilations (datastore-specific).
 */
public interface QueryDatastoreCompilationCache
{
    /**
     * Method to close the cache when no longer needed. Provides a hook to release resources etc.
     */
    void close();

    /**
     * Evict the query from the compilation cache.
     * @param queryKey Key for the query to evict.
     */
    void evict(String queryKey);

    /**
     * Method to clear the cache.
     */
    void clear();

    /**
     * Accessor for whether the cache is empty.
     * @return Whether it is empty.
     */
    boolean isEmpty();

    /**
     * Accessor for the total number of compilations in the query cache.
     * @return Number of queries
     */
    int size();

    /**
     * Accessor for a (generic) compilation from the cache.
     * @param queryKey The query key
     * @return The cached query compilation
     */
    Object get(String queryKey);

    /**
     * Method to put an object in the cache.
     * @param queryKey The query key
     * @param compilation The compilation for this datastore
     * @return The cached compilation previously associated with this query (if any)
     */
    Object put(String queryKey, Object compilation);

    /**
     * Accessor for whether the specified query is in the cache
     * @param queryKey The query key
     * @return Whether it is in the cache
     */
    boolean contains(String queryKey);
}