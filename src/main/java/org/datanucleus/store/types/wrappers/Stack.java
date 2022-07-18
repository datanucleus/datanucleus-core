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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.datanucleus.FetchPlanState;
import org.datanucleus.flush.CollectionAddOperation;
import org.datanucleus.flush.CollectionRemoveOperation;
import org.datanucleus.flush.ListAddAtOperation;
import org.datanucleus.flush.ListRemoveAtOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.state.RelationshipManager;
import org.datanucleus.store.types.SCOList;
import org.datanucleus.store.types.SCOListIterator;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class Stack object.
 * This is the simplified form that intercepts mutators and marks the field as dirty.
 * It also handles cascade-delete triggering for persistable elements.
 */
public class Stack<E> extends java.util.Stack<E> implements SCOList<java.util.Stack<E>, E>
{
    private static final long serialVersionUID = -2356534368275783162L;

    protected transient DNStateManager ownerSM;
    protected transient AbstractMemberMetaData ownerMmd;

    /** The internal "delegate". */
    protected java.util.Stack<E> delegate;

    /**
     * Constructor, using StateManager of the "owner" and the field name.
     * @param sm The owner StateManager
     * @param mmd Metadata for the member
     **/
    public Stack(DNStateManager sm, AbstractMemberMetaData mmd)
    {
        this.ownerSM = sm;
        this.ownerMmd = mmd;
    }

    public void initialise(java.util.Stack<E> newValue, Object oldValue)
    {
        initialise(newValue);
    }

    public void initialise(java.util.Stack c)
    {
        if (c != null)
        {
            delegate = new java.util.Stack(); // Make copy of container rather than using same memory
            delegate.addAll(c);
        }
        else
        {
            delegate = new java.util.Stack();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("023003", this.getClass().getName(), ownerSM.getObjectAsPrintable(), ownerMmd.getName(), "" + size(), 
                SCOUtils.getSCOWrapperOptionsMessage(true, false, true, false)));
        }
    }

    public void initialise()
    {
        initialise(null);
    }

    // ----------------------- Implementation of SCO methods -------------------

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.Stack<E> getValue()
    {
        return delegate;
    }

    public void setValue(java.util.Stack<E> value)
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
     **/
    public String getFieldName()
    {
        return ownerMmd.getName();
    }

    /**
     * Accessor for the owner object.
     * @return The owner object
     **/
    public Object getOwner()
    {
        return ownerSM != null ? ownerSM.getObject() : null;
    }

    /**
     * Method to unset the owner and field information.
     **/
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
     * Recurse sthrough the elements so that they are likewise detached.
     * @param state State of detachment state
     * @return The detached container
     */
    public java.util.Stack detachCopy(FetchPlanState state)
    {
        java.util.Stack detached = new java.util.Stack();
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
    public void attachCopy(java.util.Stack value)
    {
        // Attach all of the elements in the new list
        boolean elementsWithoutIdentity = SCOUtils.collectionHasElementsWithoutIdentity(ownerMmd);

        java.util.List attachedElements = new java.util.ArrayList(value.size());
        SCOUtils.attachCopyForCollection(ownerSM, value.toArray(), attachedElements, elementsWithoutIdentity);

        // Update the attached list with the detached elements
        SCOUtils.updateListWithListElements(this, attachedElements);
    }

    // ------------------- Implementation of Stack methods ---------------------
 
    /**
     * Clone operator to return a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * </p>
     *
     * @return The cloned object
     */
    public synchronized Object clone()
    {
        return delegate.clone();
    }

    /**
     * Method to return if the list contains this element.
     * @param element The element
     * @return Whether it is contained
     **/
    public boolean contains(Object element)
    {
        return delegate.contains(element);
    }

    /**
     * Accessor for whether the Stack is empty.
     * @return Whether it is empty.
     **/
    public boolean empty()
    {
        return delegate.empty();
    }

    public synchronized boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    public synchronized int hashCode()
    {
        return delegate.hashCode();
    }

    /**
     * Method to retrieve an element no.
     * @param index The item to retrieve
     * @return The element at that position.
     */
    public synchronized E get(int index)
    {
        return delegate.get(index);
    }

    /**
     * Method to the position of an element.
     * @param element The element.
     * @return The position.
     **/
    public int indexOf(Object element)
    {
        return delegate.indexOf(element);
    }

    /**
     * Accessor for whether the Stack is empty.
     * @return Whether it is empty.
     **/
    public synchronized boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /**
     * Method to retrieve an iterator for the list.
     * @return The iterator
     **/
    public synchronized Iterator<E> iterator()
    {
        return new SCOListIterator(this, ownerSM, delegate, null, true, -1);
    }

    /**
     * Method to retrieve a List iterator for the list.
     * @return The iterator
     **/
    public synchronized ListIterator<E> listIterator()
    {
        return new SCOListIterator(this, ownerSM, delegate, null, true, -1);
    }

    /**
     * Method to retrieve a List iterator for the list from the index.
     * @param index The start point 
     * @return The iterator
     **/
    public synchronized ListIterator<E> listIterator(int index)
    {
        return new SCOListIterator(this, ownerSM, delegate, null, true, index);
    }

    /**
     * Method to retrieve the last position of the element.
     * @param element The element
     * @return The last position of this element in the List.
     **/
    public synchronized int lastIndexOf(Object element)
    {
        return delegate.lastIndexOf(element);
    }

    /**
     * Method to retrieve the element at the top of the stack.
     * @return The element at the top of the stack
     **/
    public synchronized E peek()
    {
        return delegate.peek();
    }

    /**
     * Accessor for the size of the Stack.
     * @return The size.
     **/
    public synchronized int size()
    {
        return delegate.size();
    }

    /**
     * Accessor for the subList of elements between from and to of the List
     * @param from Start index (inclusive)
     * @param to End index (exclusive) 
     * @return The subList
     **/
    public synchronized java.util.List<E> subList(int from,int to)
    {
        return delegate.subList(from,to);
    }

    /**
     * Method to return the list as an array.
     * @return The array
     **/
    public synchronized Object[] toArray()
    {
        return delegate.toArray();
    }

    /**
     * Method to return the list as an array.
     * @param a The runtime types of the array being defined by this param
     * @return The array
     **/
    public synchronized Object[] toArray(Object a[])
    {
        return delegate.toArray(a);
    }

    /**
     * Method to add an element to a position in the Stack
     *
     * @param index The position
     * @param element The new element
     **/
    public void add(int index, E element)
    {
        delegate.add(index, element);
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            ownerSM.getExecutionContext().getRelationshipManager(ownerSM).relationAdd(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (SCOUtils.useQueuedUpdate(ownerSM))
        {
            ownerSM.getExecutionContext().addOperationToQueue(new ListAddAtOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), index, element));
        }
        makeDirty();
        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to add an element to the Stack
     *
     * @param element The new element
     * @return Whether it was added ok.
     **/
    public synchronized boolean add(E element)
    {
        boolean success = delegate.add(element);
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            ownerSM.getExecutionContext().getRelationshipManager(ownerSM).relationAdd(ownerMmd.getAbsoluteFieldNumber(), element);
        }

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
     * Method to add an element to the Stack
     *
     * @param element The new element
     **/
    public synchronized void addElement(E element)
    {
        add(element);
    }

    /**
     * Method to add a Collection to the Stack
     * @param elements The collection
     * @return Whether it was added ok.
     **/
    public synchronized boolean addAll(Collection elements)
    {
        boolean success = delegate.addAll(elements);
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            // Relationship management
            RelationshipManager relMgr = ownerSM.getExecutionContext().getRelationshipManager(ownerSM);
            for (Object elem : elements)
            {
                relMgr.relationAdd(ownerMmd.getAbsoluteFieldNumber(), elem);
            }
        }

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
     * Method to add a Collection to a position in the Stack
     * @param index Position to insert the collection.
     * @param elements The collection
     * @return Whether it was added ok.
     **/
    public synchronized boolean addAll(int index, Collection elements)
    {
        boolean success = delegate.addAll(index, elements);
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            // Relationship management
            RelationshipManager relMgr = ownerSM.getExecutionContext().getRelationshipManager(ownerSM);
            for (Object elem : elements)
            {
                relMgr.relationAdd(ownerMmd.getAbsoluteFieldNumber(), elem);
            }
        }

        if (success)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                int pos = index;
                for (Object element : elements)
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new ListAddAtOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), pos++, element));
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
     * Method to clear the Stack
     **/
    public synchronized void clear()
    {
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            // Relationship management
            RelationshipManager relMgr = ownerSM.getExecutionContext().getRelationshipManager(ownerSM);
            for (Object elem : delegate)
            {
                relMgr.relationRemove(ownerMmd.getAbsoluteFieldNumber(), elem);
            }
        }

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
     * Method to remove the top element in the stack and return it.
     * @return The top element that was in the Stack (now removed).
     **/
    public synchronized E pop()
    {
        return remove(0);
    }

    /**
     * Method to push an element onto the stack and return it.
     *
     * @param element The element to push onto the stack.
     * @return The element that was pushed onto the Stack
     */
    public E push(E element)
    {
        E obj = delegate.push(element);
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
    public synchronized boolean remove(Object element, boolean allowCascadeDelete)
    {
        boolean success = delegate.remove(element);
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            ownerSM.getExecutionContext().getRelationshipManager(ownerSM).relationRemove(ownerMmd.getAbsoluteFieldNumber(), element);
        }

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
     * Method to remove a Collection of objects from the Stack
     * @param elements The Collection
     * @return Whether the collection of elements were removed
     **/
    public synchronized boolean removeAll(Collection elements)
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
            if (ownerSM.getExecutionContext().getManageRelations())
            {
                // Relationship management
                RelationshipManager relMgr = ownerSM.getExecutionContext().getRelationshipManager(ownerSM);
                for (Object elem : elements)
                {
                    relMgr.relationRemove(ownerMmd.getAbsoluteFieldNumber(), elem);
                }
            }

            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                for (Object elem : elements)
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), elem, true));
                }
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                for (Object elem : elements)
                {
                    ownerSM.getExecutionContext().deleteObjectInternal(elem);
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
     * Method to remove an element from the Stack
     * @param element The element
     * @return Whether the element was removed
     **/
    public synchronized boolean removeElement(Object element)
    {
        return remove(element);
    }

    /**
     * Method to remove an element from the Stack
     * @param index The element position.
     * @return The object that was removed
     **/
    public synchronized E remove(int index)
    {
        E element = delegate.remove(index);
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            ownerSM.getExecutionContext().getRelationshipManager(ownerSM).relationRemove(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (ownerSM != null)
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new ListRemoveAtOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), index, element));
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerSM.getExecutionContext().deleteObjectInternal(element);
            }
        }

        makeDirty();
        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return element;
    }

    /**
     * Method to remove an element from the Stack
     * @param index The element position.
     **/
    public synchronized void removeElementAt(int index)
    {
        remove(index);
    }

    /**
     * Method to remove all elements from the Stack.
     * Same as clear().
     **/
    public synchronized void removeAllElements()
    {
        clear();
    }

    /**
     * Method to retain a Collection of elements (and remove all others).
     * @param c The collection to retain
     * @return Whether they were retained successfully.
     **/
    public synchronized boolean retainAll(java.util.Collection c)
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
     * Wrapper addition that allows turning off of the dependent-field checks
     * when doing the position setting. This means that we can prevent the deletion of
     * the object that was previously in that position. This particular feature is used
     * when attaching a list field and where some elements have changed positions.
     * @param index The position
     * @param element The new element
     * @return The element previously at that position
     */
    public E set(int index, E element, boolean allowDependentField)
    {
        E prevElement = delegate.set(index, element);
        if (ownerSM != null && allowDependentField && !delegate.contains(prevElement))
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new ListRemoveAtOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), index, prevElement));
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerSM.getExecutionContext().deleteObjectInternal(prevElement);
            }
        }

        makeDirty();
        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return prevElement;
    }

    /**
     * Method to set the element at a position in the Stack
     *
     * @param index The position
     * @param element The new element
     * @return The element previously at that position
     **/
    public synchronized E set(int index, E element)
    {
        return set(index, element, !sorting);
    }

    /**
     * Method to set the element at a position in the Stack
     *
     * @param element The new element
     * @param index The position
     **/
    public synchronized void setElementAt(E element,int index)
    {
        set(index, element);
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
     * 
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        java.util.Stack stack = new java.util.Stack();
        stack.addAll(delegate);
        return stack;
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

    /* (non-Javadoc)
     * @see java.util.ArrayList#trimToSize()
     */
    @Override
    public synchronized void trimToSize()
    {
        delegate.trimToSize();
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#ensureCapacity(int)
     */
    @Override
    public synchronized void ensureCapacity(int minCapacity)
    {
        delegate.ensureCapacity(minCapacity);
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#forEach(java.util.function.Consumer)
     */
    @Override
    public synchronized void forEach(Consumer<? super E> action)
    {
        delegate.forEach(action);
    }

    /* (non-Javadoc)
     * @see java.util.Iterable#spliterator()
     */
    @Override
    public Spliterator<E> spliterator()
    {
        return delegate.spliterator();
    }

    protected boolean sorting = false;

    /* (non-Javadoc)
     * @see java.util.List#sort(java.util.Comparator)
     */
    @Override
    public synchronized void sort(Comparator<? super E> comp)
    {
        sorting = true;
        super.sort(comp);
        sorting = false;
    }
}