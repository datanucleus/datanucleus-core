/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.wrappers;

import java.io.ObjectStreamException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.datanucleus.FetchPlanState;
import org.datanucleus.flush.CollectionAddOperation;
import org.datanucleus.flush.CollectionRemoveOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.SCOCollection;
import org.datanucleus.store.types.SCOCollectionIterator;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class PriorityQueue object.
 * This is the simplified form that intercepts mutators and marks the field as dirty.
 * It also handles cascade-delete triggering for persistable elements.
 */
public class PriorityQueue<E> extends java.util.PriorityQueue<E> implements SCOCollection<java.util.PriorityQueue<E>, E>, Cloneable
{
    protected DNStateManager ownerSM;
    protected AbstractMemberMetaData ownerMmd;

    /** The internal "delegate". */
    protected java.util.PriorityQueue<E> delegate;

    /**
     * Constructor. 
     * @param sm StateManager for this set.
     * @param mmd Metadata for the member
     **/
    public PriorityQueue(DNStateManager sm, AbstractMemberMetaData mmd)
    {
        super(1);
        this.ownerSM = sm;
        this.ownerMmd = mmd;
    }

    public void initialise(java.util.PriorityQueue<E> newValue, Object oldValue)
    {
        initialise(newValue);
    }

    public void initialise(java.util.PriorityQueue c)
    {
        if (c != null)
        {
            initialiseDelegate();
            delegate.addAll(c);
        }
        else
        {
            initialiseDelegate();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("023003", this.getClass().getName(), ownerSM.getObjectAsPrintable(), ownerMmd.getName(), "" + size(), 
                SCOUtils.getSCOWrapperOptionsMessage(true, false, false, false)));
        }
    }

    public void initialise()
    {
        initialise(null);
    }

    /**
     * Convenience method to set up the delegate respecting any comparator specified in MetaData.
     */
    protected void initialiseDelegate()
    {
        Comparator comparator = SCOUtils.getComparator(ownerMmd, ownerSM.getExecutionContext().getClassLoaderResolver());
        if (comparator != null)
        {
            this.delegate = new java.util.PriorityQueue(5, comparator);
        }
        else
        {
            this.delegate = new java.util.PriorityQueue();
        }
    }

    // ----------------------- Implementation of SCO methods -------------------

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.PriorityQueue<E> getValue()
    {
        return delegate;
    }

    public void setValue(java.util.PriorityQueue<E> value)
    {
        this.delegate = value;
    }

    /**
     * Method to effect the load of the data in the SCO.
     * Used when the SCO supports lazy-loading to tell it to load all now.
     */
    public void load()
    {
        // Always loaded
    }

    /**
     * Method to return if the SCO has its contents loaded. Returns true.
     * @return Whether it is loaded
     */
    public boolean isLoaded()
    {
        return true;
    }

    /**
     * Method to update an embedded element in this collection.
     * @param element The element
     * @param fieldNumber Number of field in the element
     * @param value New value for this field
     * @param makeDirty Whether to make the SCO field dirty.
     */
    public void updateEmbeddedElement(E element, int fieldNumber, Object value, boolean makeDirty)
    {
        if (makeDirty)
        {
            // Just mark field in embedded owners as dirty
            makeDirty();
        }
    }

    /**
     * Accessor for the field name.
     * @return The field name
     */
    public String getFieldName()
    {
        return ownerMmd.getName();
    }

    /**
     * Accessor for the owner object.
     * @return The owner object
     */
    public Object getOwner()
    {
        return ownerSM != null ? ownerSM.getObject() : null;
    }

    /**
     * Method to unset the owner and field information.
     */
    public void unsetOwner()
    {
        if (ownerSM != null)
        {
            ownerSM = null;
            ownerMmd = null;
        }
    }

    /**
     * Utility to mark the object as dirty
     **/
    public void makeDirty()
    {
        if (ownerSM != null)
        {
            ownerSM.makeDirty(ownerMmd.getAbsoluteFieldNumber());
        }
    }

    /**
     * Method to return a detached copy of the container.
     * Recurses through the elements so that they are likewise detached.
     * @param state State for detachment process
     * @return The detached container
     */
    public java.util.PriorityQueue detachCopy(FetchPlanState state)
    {
        java.util.PriorityQueue detached = new java.util.PriorityQueue();
        SCOUtils.detachCopyForCollection(ownerSM.getExecutionContext(), toArray(), state, detached);
        return detached;
    }

    /**
     * Method to return an attached copy of the passed (detached) value. The returned attached copy
     * is a SCO wrapper. Goes through the existing elements in the store for this owner field and
     * removes ones no longer present, and adds new elements. All elements in the (detached)
     * value are attached.
     * @param value The new (collection) value
     */
    public void attachCopy(java.util.PriorityQueue value)
    {
        boolean elementsWithoutIdentity = SCOUtils.collectionHasElementsWithoutIdentity(ownerMmd);
        SCOUtils.attachCopyElements(ownerSM, this, value, elementsWithoutIdentity);

/*        // Remove any no-longer-needed elements from this collection
        SCOUtils.attachRemoveDeletedElements(ownerOSM.getExecutionContext().getApiAdapter(), this, c, elementsWithoutIdentity);

        // Persist any new elements and form the attached elements collection
        java.util.Collection attachedElements = new java.util.HashSet(c.size());
        SCOUtils.attachCopyForCollection(ownerSM, c.toArray(), attachedElements, elementsWithoutIdentity);

        // Add any new elements to this collection
        SCOUtils.attachAddNewElements(ownerSM.getExecutionContext().getApiAdapter(), this, attachedElements, elementsWithoutIdentity);*/
    }

    // ---------------- Implementation of Queue methods -------------------

    /**
     * Creates and returns a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return A clone of the object
     */
    public Object clone()
    {
//        return (delegate.clone();
        // TODO Implement this since PriorityQueue doesnt have clone method
        return null;
    }

    /**
     * Accessor for the comparator.
     * @return The comparator
     */
    public Comparator comparator()
    {
        return delegate.comparator();
    }

    /**
     * Accessor for whether an element is contained in the Collection.
     * @param element The element
     * @return Whether the element is contained here
     */
    public boolean contains(Object element)
    {
        return delegate.contains(element);
    }

    /**
     * Accessor for whether a collection of elements are contained here.
     * @param c The collection of elements.
     * @return Whether they are contained.
     **/
    public boolean containsAll(java.util.Collection c)
    {
        return delegate.containsAll(c);
    }

    public boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }

    /**
     * Accessor for whether the Collection is empty.
     * @return Whether it is empty.
     */
    public  boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /**
     * Accessor for an iterator for the Collection.
     * @return The iterator
     */
    public Iterator<E> iterator()
    {
        return new SCOCollectionIterator(this, ownerSM, delegate, null, true);
    }

    /**
     * Method to peek at the next element in the Queue.
     * @return The element
     */
    public E peek()
    {
        return delegate.peek();
    }

    /**
     * Accessor for the size of the Collection.
     * @return The size
     */
    public int size()
    {
        return delegate.size();
    }

    /**
     * Method to return the Collection as an array.
     * @return The array
     */
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    /**
     * Method to return the Collection as an array.
     * @param a The array to write the results to
     * @return The array
     */
    public <T> T[] toArray(T a[])
    {
        return delegate.toArray(a);
    }

    /**
     * Method to return the Collection as a String.
     * @return The string form
     **/
    public String toString()
    {
        StringBuilder s = new StringBuilder("[");
        int i=0;
        Iterator iter=iterator();
        while (iter.hasNext())
        {
            if (i > 0)
            {
                s.append(',');
            }
            s.append(iter.next());
            i++;
        }
        s.append("]");

        return s.toString();
    }

    // ----------------------------- Mutator methods ---------------------------

    /**
     * Method to add an element to the Collection.
     * @param element The element to add
     * @return Whether it was added successfully.
     */
    public boolean add(E element)
    {
        boolean success = delegate.add(element);
        if (success)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), element));
            }
            makeDirty();
            if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Method to add a collection of elements.
     * @param elements The collection of elements to add.
     * @return Whether they were added successfully.
     */
    public boolean addAll(java.util.Collection elements)
    {
        boolean success = delegate.addAll(elements);
        if (success)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                for (Object element : elements)
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), element));
                }
            }
            makeDirty();
            if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Method to clear the Collection.
     */
    public void clear()
    {
        if (ownerSM != null && !delegate.isEmpty())
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                java.util.List copy = new java.util.ArrayList(delegate);
                Iterator iter = copy.iterator();
                while (iter.hasNext())
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), iter.next(), true));
                }
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                java.util.List copy = new java.util.ArrayList(delegate);
                Iterator iter = copy.iterator();
                while (iter.hasNext())
                {
                    ownerSM.getExecutionContext().deleteObjectInternal(iter.next());
                }
            }
        }

        delegate.clear();

        makeDirty();
        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to offer an element to the Queue.
     * @param element The element to offer
     * @return Whether it was added successfully.
     */
    public boolean offer(E element)
    {
        return add(element);
    }

    /**
     * Method to poll the next element in the Queue.
     * @return The element (now removed)
     */
    public E poll()
    {
        E obj = delegate.poll();
        makeDirty();
        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return obj;
    }

    /**
     * Method to remove (the first occurrence of) an element from the collection
     * @param element The Element to remove
     * @return Whether it was removed successfully.
     */
    public boolean remove(Object element)
    {
        return remove(element, true);
    }

    /**
     * Method to remove (the first occurrence of) an element from the collection
     * @param element The Element to remove
     * @param allowCascadeDelete Whether to cascade delete
     * @return Whether it was removed successfully.
     */
    public boolean remove(Object element, boolean allowCascadeDelete)
    {
        boolean success = delegate.remove(element);

        if (ownerSM != null && allowCascadeDelete)
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), element, allowCascadeDelete));
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerSM.getExecutionContext().deleteObjectInternal(element);
            }
        }

        if (success)
        {
            makeDirty();
            if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }
        }

        return success;
    }

    /**
     * Method to remove a Collection of elements.
     * @param elements The collection to remove
     * @return Whether they were removed successfully.
     */
    public boolean removeAll(java.util.Collection elements)
    {
        if (elements == null)
        {
            throw new NullPointerException();
        }
        else if (elements.isEmpty())
        {
            return true;
        }

        boolean success = delegate.removeAll(elements);

        if (ownerSM != null)
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                Iterator iter = elements.iterator();
                while (iter.hasNext())
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), iter.next(), true));
                }
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                Iterator iter = elements.iterator();
                while (iter.hasNext())
                {
                    ownerSM.getExecutionContext().deleteObjectInternal(iter.next());
                }
            }
        }

        if (success)
        {
            makeDirty();
            if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }
        }

        return success;
    }

    /**
     * Method to retain a Collection of elements (and remove all others).
     * @param c The collection to retain
     * @return Whether they were retained successfully.
     */
    public boolean retainAll(java.util.Collection c)
    {
        boolean success = delegate.retainAll(c);
        if (success)
        {
            makeDirty();
            if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * The writeReplace method is called when ObjectOutputStream is preparing
     * to write the object to the stream. The ObjectOutputStream checks whether
     * the class defines the writeReplace method. If the method is defined, the
     * writeReplace method is called to allow the object to designate its
     * replacement in the stream. The object returned should be either of the
     * same type as the object passed in or an object that when read and
     * resolved will result in an object of a type that is compatible with all
     * references to the object.
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        return new java.util.PriorityQueue(delegate);
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#forEach(java.util.function.Consumer)
     */
    @Override
    public void forEach(Consumer<? super E> action)
    {
        delegate.forEach(action);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#stream()
     */
    @Override
    public Stream<E> stream()
    {
        return delegate.stream();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#parallelStream()
     */
    @Override
    public Stream<E> parallelStream()
    {
        return delegate.parallelStream();
    }
}