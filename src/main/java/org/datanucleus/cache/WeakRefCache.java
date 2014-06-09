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
import java.util.Map;
import java.util.Set;

import org.datanucleus.state.ObjectProvider;
import org.datanucleus.util.WeakValueMap;

/**
 * Level 1 Cache using Weak referenced objects in a Map.
 * <p>If the garbage collector clears the reference, the corresponding key is
 * automatically removed from the map.
 *
 * @see java.lang.ref.WeakReference
 */
public class WeakRefCache implements Level1Cache
{
    private Map<Object, ObjectProvider> weakCache = new WeakValueMap();

    /**
     * Default constructor (required)
     */
    public WeakRefCache()
    {
    }

    public ObjectProvider put(Object key, ObjectProvider value)
    {
        return weakCache.put(key, value);
    }

    public ObjectProvider get(Object key)
    {
        return weakCache.get(key);
    }

    public boolean containsKey(Object key)
    {
        return weakCache.containsKey(key);
    }

    public ObjectProvider remove(Object key)
    {
        return weakCache.remove(key);
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
}