/**********************************************************************
Copyright (c) 2003 Erik Bengtson and others. All rights reserved.
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.state.DNStateManager;
import org.datanucleus.util.ConcurrentReferenceHashMap;
import org.datanucleus.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Level 1 Cache using Weak referenced objects in a Map.
 * If the garbage collector clears the reference, the corresponding key is automatically removed from the map.
 *
 * @see java.lang.ref.WeakReference
 */
public class WeakRefCache implements Level1Cache
{
    public static final String NAME = "weak";

    private Map<Object, DNStateManager> weakCache = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.WEAK);
    private Map<CacheUniqueKey, DNStateManager> weakCacheUnique = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.WEAK);

    /**
     * Default constructor (required)
     */
    public WeakRefCache()
    {
    }

    public DNStateManager put(Object id, DNStateManager sm)
    {
        return weakCache.put(id, sm);
    }

    public DNStateManager get(Object id)
    {
        return weakCache.get(id);
    }

    public boolean containsKey(Object id)
    {
        return weakCache.containsKey(id);
    }

    public DNStateManager remove(Object id)
    {
        DNStateManager sm = weakCache.remove(id);
        if (weakCacheUnique.containsValue(sm))
        {
            Iterator<Entry<CacheUniqueKey, DNStateManager>> entrySetIter = weakCacheUnique.entrySet().iterator();
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
        weakCache.clear();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value)
    {
        return weakCache.containsValue(value);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set entrySet()
    {
        return weakCache.entrySet();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty()
    {
        return weakCache.isEmpty();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set keySet()
    {
        return weakCache.keySet();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map t)
    {
        weakCache.putAll(t);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size()
    {
        return weakCache.size();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection values()
    {
        return weakCache.values();
    }

    @Override
    public DNStateManager getUnique(CacheUniqueKey key)
    {
        return weakCacheUnique.get(key);
    }

    @Override
    public Object putUnique(CacheUniqueKey key, DNStateManager sm)
    {
        return weakCacheUnique.put(key, sm);
    }
}