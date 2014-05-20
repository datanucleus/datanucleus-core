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
2003 Andy Jefferson - commented
2008 Andy Jefferson - fixed put() method return type to give the referred-to object
    ...
**********************************************************************/
package org.datanucleus.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A <code>java.util.Map</code> implementation using reference values.
 * <p>The values are stored in the map as references.  If the garbage collector
 * clears the reference, the corresponding key is automatically removed from the map.
 *
 * @see java.lang.ref.Reference
 */
public abstract class ReferenceValueMap implements Map, Cloneable
{
    private LinkedHashMap map;
    private ReferenceQueue reaped = new ReferenceQueue();

    /**
     * Default Constructor.
     **/
    public ReferenceValueMap()
    {
        map = new LinkedHashMap();
    }

    /**
     * Constructor taking initial capacity.
     * @param initial_capacity Initial Capacity of HashMap
     **/
    public ReferenceValueMap(int initial_capacity)
    {
        map = new LinkedHashMap(initial_capacity);
    }

    /**
     * Constructor taking initial capacity and load factor.
     * @param initial_capacity Initial Capacity of HashMap
     * @param load_factor      Load Factor of HashMap
     **/
    public ReferenceValueMap(int initial_capacity,float load_factor)
    {
        map = new LinkedHashMap(initial_capacity, load_factor);
    }

    /**
     * Constructor taking initial Map.
     * @param m Map to initial with.
     **/
    public ReferenceValueMap(Map m)
    {
        map = new LinkedHashMap();
        putAll(m);
    }

    /**
     * Clone method.
     * @return Clone of this object.
     **/
    public Object clone()
    {
        reap();

        ReferenceValueMap rvm = null;
        
        try
        {
            rvm = (ReferenceValueMap)super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            // Do nothing
        }

        rvm.map = (LinkedHashMap)map.clone(); // to preserve initialCapacity, loadFactor
        rvm.map.clear();
        rvm.reaped = new ReferenceQueue();
        rvm.putAll(entrySet());

        return rvm;
    }

    /**
     * References returned by <code>newValueReference</code> must implement
     * this interface to provide the corresponding map key for the value.
     */
    public interface ValueReference
    {
        /**
         * Returns the key associated with the value referenced by this
         * <code>Reference</code> object.
         * @return The Key
         */
        Object getKey();
    }

    /**
     * Returns a new <code>Reference</code> object to be inserted into the map.
     * Subclasses must implement this method to construct <code>Reference</code>
     * objects of the desired type (e.g. <code>SoftReference</code>, etc.).
     *
     * @param key   The key that will be inserted.
     * @param value The associated value to be referenced.
     * @param queue The <code>ReferenceQueue</code> with which to register the
     *              new <code>Reference</code> object.
     * @return The new ValueReference
     */
    protected abstract ValueReference newValueReference(Object key, Object value, ReferenceQueue queue);

    /**
     * Method to add an object to the Map.
     * @param key Key for object
     * @param value Value of object
     * @return The Object.
     **/ 
    public Object put(Object key, Object value)
    {
        reap();
        return unwrapReference(map.put(key, newValueReference(key, value, reaped)));
    }

    /**
     * Method to add the contents of a Map.
     * @param m Map
     **/ 
    public void putAll(Map m)
    {
        putAll(m.entrySet());
    }

    /**
     * Method to add the contents of a Set.
     * @param entrySet The Set
     **/
    private void putAll(Set entrySet)
    {
        Iterator i = entrySet.iterator();

        while (i.hasNext())
        {
            Map.Entry entry = (Map.Entry)i.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Method to get a value for a key.
     * @param key The Key
     * @return The Value
     **/
    public Object get(Object key)
    {
        reap();
        return unwrapReference(map.get(key));
    }

    /**
     * Method to empty the HashMap.
     **/
    public void clear()
    {
        reap();
        map.clear();
    }

    /**
     * Accessor for the size of the HashMap.
     * @return The size
     **/
    public int size()
    {
        reap();
        return map.size();
    }

    /**
     * Accessor for whether the Map contains the specified Key
     * @param obj The key
     * @return Whether the key exists
     **/
    public boolean containsKey(Object obj)
    {
        reap();
        return map.containsKey(obj);
    }

    /**
     * Accessor for whether the Map contains the specified value.
     * @param obj The value
     * @return Whether the Map contains the value.
     **/
    public boolean containsValue(Object obj)
    {
        reap();

        if (obj != null)
        {
            Iterator i = map.values().iterator();

            while (i.hasNext())
            {
                Reference ref = (Reference)i.next();

                if (obj.equals(ref.get()))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Accessor for whether the Map is empty.
     * @return Whether the Map is empty.
     **/
    public boolean isEmpty()
    {
        reap();
        return map.isEmpty();
    }

    /**
     * Accessor for the Set of keys in the Map.
     * @return The Set of keys
     **/
    public Set keySet()
    {
        reap();
        return map.keySet();
    }

    /**
     * Accessor for the values from the Map.
     * @return The Values.
     **/
    public Collection values()
    {
        reap();

        Collection c = map.values();
        Iterator i = c.iterator();
        ArrayList l = new ArrayList(c.size());

        while (i.hasNext())
        {
            Reference ref = (Reference)i.next();
            Object obj = ref.get();

            if (obj != null)
            {
                l.add(obj);
            }
        }

        return Collections.unmodifiableList(l);
    }

    /**
     * Accessor for the entry set.
     * @return The Set.
     **/
    public Set entrySet()
    {
        reap();

        Set s = map.entrySet();
        Iterator i = s.iterator();
        HashMap m = new HashMap(s.size());

        while (i.hasNext())
        {
            Map.Entry entry = (Map.Entry)i.next();
            Reference ref = (Reference)entry.getValue();
            Object obj = ref.get();

            if (obj != null)
            {
                m.put(entry.getKey(), obj);
            }
        }

        return Collections.unmodifiableSet(m.entrySet());
    }

    /**
     * Method to remove an object for the specified key.
     * @param key The Key
     * @return The Object removed 
     **/
    public Object remove(Object key)
    {
        reap();
        return unwrapReference(map.remove(key));
    }

    /**
     * Hashcode generator for this object.
     * @return The Hashcode
     **/
    public int hashCode()
    {
        reap();
        return map.hashCode();
    }

    /**
     * Equality operator.
     * @param o THe object to compare against.
     * @return Whether it is equal.
     **/
    public boolean equals(Object o)
    {
        reap();
        return map.equals(o);
    }

    /**
     * Utility method to reap objects.
     **/
    public void reap()
    {
        ValueReference ref;

        while ((ref = (ValueReference)reaped.poll()) != null)
        {
            map.remove(ref.getKey());
        }
    }

    private Object unwrapReference(Object obj)
    {
        // We store a Reference, so return the referred-to object
        if (obj == null)
            return null;
        Reference ref = (Reference)obj;
        return ref.get();
    }
}
