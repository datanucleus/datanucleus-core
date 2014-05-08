/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved.
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
2002 Kelly Grizzle (TJDO)
2003 Andy Jefferson - coding standards
    ...
**********************************************************************/
package org.datanucleus.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * A <code>java.util.Map</code> implementation using weak reference values.
 * <p>The values are stored in the map as weak references.  If the garbage
 * collector clears the reference, the corresponding key is automatically removed from the map.
 * @see WeakReference
 */
public class WeakValueMap extends ReferenceValueMap
{
    /**
     * Default Constructor
     **/
    public WeakValueMap()
    {
        super();
    }

    /**
     * Constructor taking the initial capacity.
     * @param initialCapacity The Initial capacity of the collection
     **/
    public WeakValueMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * Constructor taking the initial capacity and load factor.
     * @param initialCapacity The Initial capacity of the collection
     * @param loadFactor The Load Factor of the collection
     **/
    public WeakValueMap(int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor taking a Map for definition.
     * @param m The Map 
     **/
    public WeakValueMap(Map m)
    {
        super(m);
    }

    /**
     * Inner class to represent a weak reference - one that can be garbage
     * collected.
     **/
    private static class WeakValueReference extends WeakReference implements ReferenceValueMap.ValueReference
    {
        private final Object key;

        WeakValueReference(Object key, Object value, ReferenceQueue q)
        {
            super(value, q);
            this.key = key;
        }

        public Object getKey()
        {
            return key;
        }
    }

    protected ReferenceValueMap.ValueReference newValueReference(Object key, Object value, ReferenceQueue queue)
    {
        return new WeakValueReference(key, value, queue);
    }
}
