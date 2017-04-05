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

import org.datanucleus.state.ObjectProvider;
import org.datanucleus.util.SoftValueMap;

/**
 * Level 1 Cache using Soft referenced objects in a Map.
 * <P>
 * If map entry value object is not actively being used, i.e. no other object has a strong reference to it, 
 * it may become garbage collected at the discretion of the garbage collector (typically if the VM is low on memory). 
 * If this happens, the entry in the <code>SoftValueMap</code> corresponding to the value object will also be removed.
 * @see java.lang.ref.SoftReference
 */
public class SoftRefCache implements Level1Cache
{
    private Map<Object, ObjectProvider> softCache = new SoftValueMap();
    private Map<CacheUniqueKey, ObjectProvider> softCacheUnique = new SoftValueMap();

    public SoftRefCache()
    {
    }

    public ObjectProvider put(Object id, ObjectProvider op)
    {
        return softCache.put(id, op);
    }

    public ObjectProvider get(Object id)
    {
        return softCache.get(id);
    }

    public boolean containsKey(Object id)
    {
        return softCache.containsKey(id);
    }

    public ObjectProvider remove(Object id)
    {
        ObjectProvider op = softCache.remove(id);
        if (softCacheUnique.containsValue(op))
        {
            Iterator<Entry<CacheUniqueKey, ObjectProvider>> entrySetIter = softCacheUnique.entrySet().iterator();
            while (entrySetIter.hasNext())
            {
                Entry<CacheUniqueKey, ObjectProvider> entry = entrySetIter.next();
                if (entry.getValue() == op)
                {
                    entrySetIter.remove();
                }
            }
        }
        return op;
    }

    public void clear()
    {
        if (isEmpty())
        {
            return;
        }
        softCache.clear();
        softCacheUnique.clear();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value)
    {
        return softCache.containsValue(value);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set entrySet()
    {
        return softCache.entrySet();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty()
    {
        return softCache.isEmpty();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set keySet()
    {
        return softCache.keySet();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map t)
    {
        softCache.putAll(t);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size()
    {
        return softCache.size();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection values()
    {
        return softCache.values();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.cache.Level1Cache#getUnique(org.datanucleus.cache.CacheUniqueKey)
     */
    @Override
    public ObjectProvider getUnique(CacheUniqueKey key)
    {
        return softCacheUnique.get(key);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.cache.Level1Cache#putUnique(org.datanucleus.cache.CacheUniqueKey, org.datanucleus.state.ObjectProvider)
     */
    @Override
    public Object putUnique(CacheUniqueKey key, ObjectProvider op)
    {
        return softCacheUnique.put(key, op);
    }
}