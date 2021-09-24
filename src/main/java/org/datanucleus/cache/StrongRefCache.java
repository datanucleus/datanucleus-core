/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.state.DNStateManager;

/**
 * Implementation of a Level 1 cache keeping strong references to the objects.
 * This means that objects are not garbage collected, and have to be removed directly by calls to remove(). 
 * This differs from the WeakRefCache/SoftRefCache which do not guarantee to retain objects.
 */
public class StrongRefCache implements Level1Cache
{
    public static final String NAME = "strong";

    private Map<Object, DNStateManager> cache = new HashMap<>();
    private Map<CacheUniqueKey, DNStateManager> cacheUnique = new HashMap<>();

    public StrongRefCache()
    {
    }

    public DNStateManager put(Object id, DNStateManager sm)
    {
        return cache.put(id, sm);
    }

    public DNStateManager get(Object id)
    {
        return cache.get(id);
    }

    public boolean containsKey(Object id)
    {
        return cache.containsKey(id);
    }

    public DNStateManager remove(Object id)
    {
        DNStateManager sm = cache.remove(id);
        if (cacheUnique.containsValue(sm))
        {
            Iterator<Entry<CacheUniqueKey, DNStateManager>> entrySetIter = cacheUnique.entrySet().iterator();
            while (entrySetIter.hasNext())
            {
                Entry<CacheUniqueKey, DNStateManager> entry = entrySetIter.next();
                if (entry.getValue() == sm)
                {
                    entrySetIter.remove();
                }
            }
        }
        return sm;
    }

    public void clear()
    {
        cache.clear();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value)
    {
        return cache.containsValue(value);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set entrySet()
    {
        return cache.entrySet();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty()
    {
        return cache.isEmpty();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set keySet()
    {
        return cache.keySet();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map t)
    {
        cache.putAll(t);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size()
    {
        return cache.size();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection values()
    {
        return cache.values();
    }

    @Override
    public DNStateManager getUnique(CacheUniqueKey key)
    {
        return cacheUnique.get(key);
    }

    @Override
    public Object putUnique(CacheUniqueKey key, DNStateManager sm)
    {
        return cacheUnique.put(key, sm);
    }
}