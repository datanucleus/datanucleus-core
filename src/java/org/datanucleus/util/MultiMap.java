/**********************************************************************
Copyright (c) 2003 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/** 
 * An implementation of a <code>MultiMap</code>, which is basically a Map
 * with multiple values for a key. This will be removed when SUN see sense and
 * include it in the JDK java.util package as standard.
 */
public class MultiMap extends HashMap
{
    private transient Collection values=null;
    
    /**
     * Constructor.
     */
    public MultiMap()
    {
        super();
    }

    /**
     * Constructor.
     * 
     * @param initialCapacity  the initial capacity
     */
    public MultiMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * Constructor.
     * @param initialCapacity initial capacity
     * @param loadFactor      load factor for the Map.
     */
    public MultiMap(int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor.
     * @param map  The initial Map.
     */
    public MultiMap(MultiMap map)
    {
        super();
        if (map != null)
        {
            Iterator it = map.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry) it.next();
                super.put(entry.getKey(), new ArrayList((List)entry.getValue()));
            }
        }
    }

    /**
     * Check if the map contains the passed value.
     *
     * @param value  the value to search for
     * @return true if the list contains the value
     */
    public boolean containsValue(Object value)
    {
        Set pairs = super.entrySet();

        if (pairs == null)
        {
            return false;
        }
        Iterator pairsIterator = pairs.iterator();
        while (pairsIterator.hasNext())
        {
            Map.Entry keyValuePair = (Map.Entry) pairsIterator.next();
            Collection coll = (Collection) keyValuePair.getValue();
            if (coll.contains(value))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a key, and its value, to the map.
     * 
     * @param key   the key to set
     * @param value the value to set the key to
     * @return the value added when successful, or null if an error
     */
    public Object put(Object key,Object value)
    {
        Collection c=(Collection)super.get(key);
        if (c == null)
        {
            c = createCollection(null);
            super.put(key, c);
        }
        boolean results = c.add(value);

        return (results ? value : null);
    }

    /**
     * Removes a specific value from map.
     * The item is removed from the collection mapped to the specified key.
     * 
     * @param key  the key to remove from
     * @param item  the value to remove
     * @return the value removed (which was passed in)
     */
    public Object remove(Object key, Object item)
    {
        Collection valuesForKey=(Collection)super.get(key);
        if (valuesForKey == null)
        {
            return null;
        }
        valuesForKey.remove(item);

        // remove the list if it is now empty
        // (saves space, and allows equals to work)
        if (valuesForKey.isEmpty())
        {
            remove(key);
        }
        return item;
    }

    /**
     * Clear the map.
     */
    public void clear()
    {
        // Clear the mappings
        Set pairs=super.entrySet();
        Iterator pairsIterator = pairs.iterator();
        while (pairsIterator.hasNext())
        {
            Map.Entry keyValuePair=(Map.Entry) pairsIterator.next();
            Collection coll=(Collection)keyValuePair.getValue();
            coll.clear();
        }
        super.clear();
    }

    /** 
     * Accessor for the values in the Map.
     * @return all of the values in the map
     */
    public Collection values()
    {
        Collection vs = values;
        return (vs != null ? vs : (values = new ValueElement()));
    }

    /**
     * Method to clone the Map. Performs a shallow copy of the entry set.
     * @return the cloned map
     */
    public Object clone()
    {
        MultiMap obj = (MultiMap) super.clone();

        // Clone the entry set.
        for (Iterator it = entrySet().iterator(); it.hasNext();)
        {
            Map.Entry entry = (Map.Entry) it.next();
            Collection coll = (Collection) entry.getValue();
            Collection newColl = createCollection(coll);
            entry.setValue(newColl);
        }
        return obj;
    }

    /** 
     * Creates a new instance of the map value Collection container.
     * @param c the collection to copy
     * @return new collection
     */
    protected Collection createCollection(Collection c)
    {
        if (c == null)
        {
            return new ArrayList();
        }
        else
        {
            return new ArrayList(c);
        }
    }

    /**
     * Representation of the values.
     */
    private class ValueElement extends AbstractCollection
    {
        public Iterator iterator()
        {
            return new ValueElementIter();
        }

        public int size()
        {
            int i=0;
            Iterator iter = iterator();
            while (iter.hasNext())
            {
                iter.next();
                i++;
            }
            return i;
        }

        public void clear()
        {
            MultiMap.this.clear();
        }
    }

    /**
     * Iterator for the values.
     */
    private class ValueElementIter implements Iterator
    {
        private Iterator backing;
        private Iterator temp;

        private ValueElementIter()
        {
            backing = MultiMap.super.values().iterator();
        }

        private boolean searchNextIterator()
        {
            while (temp == null || temp.hasNext() == false)
            {
                if (backing.hasNext() == false)
                {
                    return false;
                }
                temp = ((Collection) backing.next()).iterator();
            }
            return true;
        }

        public boolean hasNext()
        {
            return searchNextIterator();
        }

        public Object next()
        {
            if (searchNextIterator() == false)
            {
                throw new NoSuchElementException();
            }
            return temp.next();
        }

        public void remove()
        {
            if (temp == null)
            {
                throw new IllegalStateException();
            }
            temp.remove();
        }
    }
}