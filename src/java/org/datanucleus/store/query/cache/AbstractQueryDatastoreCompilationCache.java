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

import java.util.Map;

/**
 * Abstract representation of a query compilation cache for the query specific to the datastore.
 */
public class AbstractQueryDatastoreCompilationCache implements QueryDatastoreCompilationCache
{
    Map<String, Object> cache;

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#clear()
     */
    public void clear()
    {
        cache.clear();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#close()
     */
    public void close()
    {
        cache.clear();
        cache = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#contains(java.lang.String)
     */
    public boolean contains(String queryKey)
    {
        return cache.containsKey(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#evict(java.lang.String)
     */
    public void evict(String queryKey)
    {
        cache.remove(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#get(java.lang.String)
     */
    public Object get(String queryKey)
    {
        return cache.get(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#isEmpty()
     */
    public boolean isEmpty()
    {
        return cache.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#put(java.lang.String, java.lang.Object)
     */
    public Object put(String queryKey, Object compilation)
    {
        return cache.put(queryKey, compilation);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.cache.QueryDatastoreCompilationCache#size()
     */
    public int size()
    {
        return cache.size();
    }
}