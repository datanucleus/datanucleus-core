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
import java.lang.ref.SoftReference;
import java.util.Map;

/**
 * A <code>java.util.Map</code> implementation with soft values.
 * <P> The values are stored as soft references. If map entry value object 
 * is not actively being used, i.e. no other object has a strong reference 
 * to it, it may become garbage collected at the discretion of the garbage
 * collector (typically if the VM is low on memory). If this happens, the 
 * entry in the <code>SoftValueMap</code> corresponding to the value object
 * will also be removed.
 *
 * @see SoftReference
 */
public class SoftValueMap extends ReferenceValueMap
{
    /**
     * Default Constructor
     **/
    public SoftValueMap()
    {
        super();
    }

    /**
     * Constructor taking the initial capacity.
     * @param initialCapacity The Initial capacity of the collection
     **/
    public SoftValueMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * Constructor taking the initial capacity and load factor.
     * @param initialCapacity The Initial capacity of the collection
     * @param loadFactor The Load Factor of the collection
     **/
    public SoftValueMap(int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor taking a Map for definition.
     * @param m The Map 
     **/
    public SoftValueMap(Map m)
    {
        super(m);
    }

    /**
     * Representation of a soft value reference.
     **/
    private static class SoftValueReference extends SoftReference
       implements ReferenceValueMap.ValueReference
    {
        private final Object key;

        SoftValueReference(Object key, Object value, ReferenceQueue q)
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
        return new SoftValueReference(key, value, queue);
    }
}
