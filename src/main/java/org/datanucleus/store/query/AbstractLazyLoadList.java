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
package org.datanucleus.store.query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.datanucleus.util.Localiser;
import org.datanucleus.util.SoftValueMap;
import org.datanucleus.util.WeakValueMap;

/**
 * Abstract implementation of a lazy loaded list of (persistent) objects.
 * Needs to be extended to implement the <pre>retrieveObjectForIndex()</pre> method to retrieve
 * the object at the specified index from whatever datasource is being used, and to implement
 * the <pre>getSize()</pre> method to return the size of the list. The "datasource" could
 * be results for a query, or a connection to a datastore, or whatever ... just a source of objects.
 * TODO Change Localised message numbers to be generic
 */
public abstract class AbstractLazyLoadList implements List
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Map of object, keyed by the index (0, 1, etc). */
    private Map<Integer, Object> itemsByIndex = null;

    /** Cached size of the list. -1 when not known. */
    protected int size = -1;

    /**
     * Constructor for a lazy load list.
     * @param cacheType Type of caching of objects in the list
     */
    public AbstractLazyLoadList(String cacheType)
    {
        // Process any supported extensions
        if (cacheType != null)
        {
            if (cacheType.equalsIgnoreCase("soft"))
            {
                itemsByIndex = new SoftValueMap();
            }
            else if (cacheType.equalsIgnoreCase("weak"))
            {
                itemsByIndex = new WeakValueMap();
            }
            else if (cacheType.equalsIgnoreCase("strong"))
            {
                itemsByIndex = new HashMap();
            }
            else if (cacheType.equalsIgnoreCase("none"))
            {
                itemsByIndex = null;
            }
            else
            {
                itemsByIndex = new WeakValueMap();
            }
        }
        else
        {
            itemsByIndex = new WeakValueMap();
        }
    }

    /**
     * Accessor to retrieve the object at an index.
     * Is only called if the object is not currently cached.
     * @param index The list index
     * @return The object
     */
    protected abstract Object retrieveObjectForIndex(int index);

    /**
     * Method to return the size of the list.
     * @return The size
     */
    protected abstract int getSize();

    /**
     * Accessor whether the list is open. Always open in this implementation but can be overridden
     * if you want to allow closure.
     * @return Whether it is open.
     */
    protected boolean isOpen()
    {
        // Always open in this implementation
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, Object element)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(Object e)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection c)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection c)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#clear()
     */
    public void clear()
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object o)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection c)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#get(int)
     */
    public Object get(int index)
    {
        // TODO Add check on isOpen() and throw exception accordingly
        if (itemsByIndex != null && itemsByIndex.containsKey(index))
        {
            return itemsByIndex.get(index);
        }
        else
        {
            Object obj = retrieveObjectForIndex(index);
            itemsByIndex.put(index, obj);
            return obj;
        }
    }

    /* (non-Javadoc)
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /* (non-Javadoc)
     * @see java.util.List#iterator()
     */
    public Iterator iterator()
    {
        return listIterator(0);
    }

    /* (non-Javadoc)
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#listIterator()
     */
    public ListIterator listIterator()
    {
        return listIterator(0);
    }

    /* (non-Javadoc)
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int index)
    {
        // TODO Support index
        return new LazyLoadListIterator();
    }

    /* (non-Javadoc)
     * @see java.util.List#remove(int)
     */
    public Object remove(int index)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection c)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection c)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int index, Object element)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.List#size()
     */
    public int size()
    {
        if (size >= 0)
        {
            return size;
        }

        size = getSize();
        return size;
    }

    /* (non-Javadoc)
     * @see java.util.List#subList(int, int)
     */
    public List subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    public Object[] toArray()
    {
        Object[] array = new Object[size()];
        for (int i=0;i<array.length;i++)
        {
            if (itemsByIndex != null && itemsByIndex.containsKey(i))
            {
                array[i] = itemsByIndex.get(i);
            }
            else
            {
                array[i] = retrieveObjectForIndex(i);
            }
        }
        return array;
    }

    public Object[] toArray(Object[] a)
    {
        if (a == null)
        {
            // Throw NPE as per javadoc
            throw new NullPointerException("null argument is illegal!");
        }

        Object[] array = a;
        int ourSize = size();
        if (a.length < ourSize)
        {
            // java.util.List.toArray() : If the input array isn't big enough, then allocate one
            array = new Object[size()];
        }

        for (int i=0;i<ourSize;i++)
        {
            if (itemsByIndex != null && itemsByIndex.containsKey(i))
            {
                array[i] = itemsByIndex.get(i);
            }
            else
            {
                array[i] = retrieveObjectForIndex(i);
            }
        }

        return array;
    }

    /**
     * Iterator for the elements of the List.
     */
    private class LazyLoadListIterator implements ListIterator
    {
        private int iteratorIndex = 0; // The index of the next object

        public boolean hasNext()
        {
            synchronized (AbstractLazyLoadList.this)
            {
                if (!isOpen())
                {
                    // Closed list so return false
                    return false;
                }

                // When we aren't at size()-1 we have at least one more element
                return (iteratorIndex <= (size() - 1));
            }
        }

        public boolean hasPrevious()
        {
            synchronized (AbstractLazyLoadList.this)
            {
                if (!isOpen())
                {
                    // Closed list so return false
                    return false;
                }

                // A List has indices starting at 0 so when we have > 0 we have a previous
                return (iteratorIndex > 0);
            }
        }

        public Object next()
        {
            synchronized (AbstractLazyLoadList.this)
            {
                if (!isOpen())
                {
                    // Closed list so throw NoSuchElementException
                    throw new NoSuchElementException(LOCALISER.msg("052600"));
                }

                if (!hasNext())
                {
                    throw new NoSuchElementException("No next element");
                }

                if (itemsByIndex != null && itemsByIndex.containsKey(iteratorIndex))
                {
                    return itemsByIndex.get(iteratorIndex);
                }
                else
                {
                    Object obj = retrieveObjectForIndex(iteratorIndex);
                    if (itemsByIndex != null)
                    {
                        itemsByIndex.put(iteratorIndex, obj);
                    }
                    iteratorIndex++;
                    return obj;
                }
            }
        }

        public int nextIndex()
        {
            if (hasNext())
            {
                return iteratorIndex;
            }
            return size();
        }

        public Object previous()
        {
            synchronized (AbstractLazyLoadList.this)
            {
                if (!isOpen())
                {
                    // Closed list so throw NoSuchElementException
                    throw new NoSuchElementException(LOCALISER.msg("052600"));
                }

                if (!hasPrevious())
                {
                    throw new NoSuchElementException("No previous element");
                }

                iteratorIndex--;
                if (itemsByIndex != null &&itemsByIndex.containsKey(iteratorIndex))
                {
                    return itemsByIndex.get(iteratorIndex);
                }
                else
                {
                    Object obj = retrieveObjectForIndex(iteratorIndex);
                    if (itemsByIndex != null)
                    {
                        itemsByIndex.put(iteratorIndex, obj);
                    }
                    iteratorIndex++;
                    return obj;
                }
            }
        }

        public int previousIndex()
        {
            if (iteratorIndex == 0)
            {
                return -1;
            }
            return iteratorIndex-1;
        }

        /* (non-Javadoc)
         * @see java.util.ListIterator#add(java.lang.Object)
         */
        public void add(Object e)
        {
            throw new UnsupportedOperationException(LOCALISER.msg("052603"));
        }

        /* (non-Javadoc)
         * @see java.util.ListIterator#remove()
         */
        public void remove()
        {
            throw new UnsupportedOperationException(LOCALISER.msg("052603"));
        }

        /* (non-Javadoc)
         * @see java.util.ListIterator#set(java.lang.Object)
         */
        public void set(Object e)
        {
            throw new UnsupportedOperationException(LOCALISER.msg("052603"));
        }
    }
}