/**********************************************************************
Copyright (c) 2015 Renato and others. All rights reserved.
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
package org.datanucleus.store.types.containers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.datanucleus.store.types.ElementContainerAdapter;
import org.datanucleus.store.types.SequenceAdapter;

public class ArrayAdapter<C extends Object> extends ElementContainerAdapter<C>implements SequenceAdapter
{
    List<Object> buffer;

    public ArrayAdapter(C container)
    {
        super(container);
    }

    public Iterator<Object> iterator()
    {
        C container = getContainer();
        return container instanceof Object[] ? new ObjectArrayIterator((Object[]) container) : new ArrayIterator(container);
    }

    public C getContainer()
    {
        if (buffer == null)
        {
            return container;
        }

        Object newArray = Array.newInstance(container.getClass().getComponentType(), buffer.size());
        
        for (int i = 0; i < buffer.size(); i++)
        {
            Object object = buffer.get(i);
            Array.set(newArray, i, object);
        }
        return (C) newArray;
    }

    @Override
    public void clear()
    {
        buffer = new ArrayList();
    }

    @Override
    public void add(Object newElement)
    {
        getBuffer().add(newElement);
    }

    @Override
    public void remove(Object element)
    {
        getBuffer().remove(element);
    }

    private List<Object> getBuffer()
    {
        if (buffer == null)
        {
            buffer = new ArrayList<>();
            for (Object object : this)
            {
                buffer.add(object);
            }
        }

        return buffer;
    }

    @Override
    public void update(Object newElement, int position)
    {
        Array.set(getContainer(), position, newElement);
    }

    /**
     * Based on Apache Collections 3.2 implementation. Specific implementation for Object arrays which will
     * perform better.
     */
    private final class ObjectArrayIterator implements Iterator
    {
        protected Object[] array = null;

        /** The end index to loop to */
        protected int endIndex = 0;

        /** The current iterator index */
        protected int index = 0;

        /**
         * Constructs an ObjectArrayIterator that will iterate over the values in the specified array.
         * @param array the array to iterate over
         * @throws NullPointerException if <code>array</code> is <code>null</code>
         */
        public ObjectArrayIterator(Object[] array)
        {
            this.array = array;
            this.endIndex = array.length;
            this.index = 0;
        }

        // Iterator interface
        // -------------------------------------------------------------------------

        /**
         * Returns true if there are more elements to return from the array.
         * @return true if there is a next element to return
         */
        public boolean hasNext()
        {
            return (this.index < this.endIndex);
        }

        /**
         * Returns the next element in the array.
         * @return the next element in the array
         * @throws NoSuchElementException if all the elements in the array have already been returned
         */
        public Object next()
        {
            if (hasNext() == false)
            {
                throw new NoSuchElementException();
            }
            return this.array[this.index++];
        }

        /**
         * Throws {@link UnsupportedOperationException}.
         * @throws UnsupportedOperationException always
         */
        public void remove()
        {
            throw new UnsupportedOperationException("remove() method is not supported for an ObjectArrayIterator");
        }
    }

    /**
     * Based on Apache Collections 3.2 implementation
     */
    private class ArrayIterator implements Iterator
    {
        /** The array to iterate over */
        protected Object array;

        /** The end index to loop to */
        protected int endIndex = 0;

        /** The current iterator index */
        protected int index = 0;

        /**
         * Constructs an ArrayIterator that will iterate over the values in the specified array.
         * @param array the array to iterate over.
         * @throws IllegalArgumentException if <code>array</code> is not an array.
         * @throws NullPointerException if <code>array</code> is <code>null</code>
         */
        public ArrayIterator(final Object array)
        {
            super();
            setArray(array);
        }

        // Iterator interface
        // -----------------------------------------------------------------------
        /**
         * Returns true if there are more elements to return from the array.
         * @return true if there is a next element to return
         */
        public boolean hasNext()
        {
            return (index < endIndex);
        }

        /**
         * Returns the next element in the array.
         * @return the next element in the array
         * @throws NoSuchElementException if all the elements in the array have already been returned
         */
        public Object next()
        {
            if (hasNext() == false)
            {
                throw new NoSuchElementException();
            }
            return Array.get(array, index++);
        }

        /**
         * Throws {@link UnsupportedOperationException}.
         * @throws UnsupportedOperationException always
         */
        public void remove()
        {
            throw new UnsupportedOperationException("remove() method is not supported");
        }

        /**
         * Sets the array that the ArrayIterator should iterate over.
         * <p>
         * If an array has previously been set (using the single-arg constructor or this method) then that
         * array is discarded in favour of this one. Iteration is restarted at the start of the new array.
         * Although this can be used to reset iteration, the {@link #reset()} method is a more effective
         * choice.
         * @param array the array that the iterator should iterate over.
         * @throws IllegalArgumentException if <code>array</code> is not an array.
         * @throws NullPointerException if <code>array</code> is <code>null</code>
         */
        public void setArray(final Object array)
        {
            // Array.getLength throws IllegalArgumentException if the object is not
            // an array or NullPointerException if the object is null. This call
            // is made before saving the array and resetting the index so that the
            // array iterator remains in a consistent state if the argument is not
            // an array or is null.
            this.endIndex = Array.getLength(array);
            this.array = array;
            this.index = 0;
        }
    }
}
