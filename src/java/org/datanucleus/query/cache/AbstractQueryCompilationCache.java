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
package org.datanucleus.query.cache;

import java.util.Map;

import org.datanucleus.query.compiler.QueryCompilation;

/**
 * Abstract representation of a cache of generic query compilations.
 */
public class AbstractQueryCompilationCache
{
    Map<String, QueryCompilation> cache;

    public AbstractQueryCompilationCache()
    {
        super();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.cache.QueryCompilationCache#clear()
     */
    public void clear()
    {
        cache.clear();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.cache.QueryCompilationCache#close()
     */
    public void close()
    {
        cache.clear();
        cache = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.cache.QueryCompilationCache#contains(java.lang.String)
     */
    public boolean contains(String queryKey)
    {
        return cache.containsKey(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.cache.QueryCompilationCache#evict(java.lang.String)
     */
    public void evict(String queryKey)
    {
        cache.remove(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.cache.QueryCompilationCache#get(java.lang.String)
     */
    public QueryCompilation get(String queryKey)
    {
        return cache.get(queryKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.cache.QueryCompilationCache#isEmpty()
     */
    public boolean isEmpty()
    {
        return cache.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.cache.QueryCompilationCache#put(java.lang.String, org.datanucleus.query.compiler.QueryCompilation)
     */
    public QueryCompilation put(String queryKey, QueryCompilation compilation)
    {
        return cache.put(queryKey, compilation);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.cache.QueryCompilationCache#size()
     */
    public int size()
    {
        return cache.size();
    }
}