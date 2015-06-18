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
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

import org.datanucleus.flush.CollectionAddOperation;
import org.datanucleus.flush.CollectionRemoveOperation;
import org.datanucleus.flush.ListAddAtOperation;
import org.datanucleus.flush.ListRemoveAtOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.FetchPlanState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.state.RelationshipManager;
import org.datanucleus.store.types.SCOList;
import org.datanucleus.store.types.SCOListIterator;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class List object.
 * This is the simplified form that intercepts mutators and marks the field as dirty.
 * It also handles cascade-delete triggering for persistable elements.
 */
public class List<E> extends AbstractList<E> implements SCOList<java.util.List<E>, E>, Cloneable, java.io.Serializable
{
    protected ObjectProvider ownerOP;
    protected AbstractMemberMetaData ownerMmd;

    /** The internal "delegate". */
    protected java.util.List<E> delegate;

    /**
     * Constructor, using the ObjectProvider of the "owner" and the field name.
     * @param ownerOP The owner ObjectProvider
     * @param mmd Metadata for the member
     */
    public List(ObjectProvider ownerOP, AbstractMemberMetaData mmd)
    {
        this.ownerOP = ownerOP;
        this.ownerMmd = mmd;
    }

    public void initialise(java.util.List<E> newValue, Object oldValue)
    {
        initialise(newValue);
    }

    public void initialise(java.util.List c)
    {
        if (c != null)
        {
            delegate = new java.util.ArrayList(c); // Make copy of container rather than using same memory
        }
        else
        {
            delegate = new java.util.ArrayList();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("023003", this.getClass().getName(), ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + size(), 
                SCOUtils.getSCOWrapperOptionsMessage(true, false, true, false)));
        }
    }

    public void initialise()
    {
        initialise(null);
    }

    // ----------------------------- Implementation of SCO methods -----------------------------------

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.List<E> getValue()
    {
        return delegate;
    }

    public void setValue(java.util.List<E> value)
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
        return (ownerOP != null ? ownerOP.getObject() : null);
    }

    /**
     * Method to unset the owner and field information.
     */
    public synchronized void unsetOwner()
    {
        if (ownerOP != null)
        {
            ownerOP = null;
            ownerMmd = null;
        }
    }

    /**
     * Utility to mark the object as dirty
     **/
    public void makeDirty()
    {
        if (ownerOP != null)
        {
            ownerOP.makeDirty(ownerMmd.getAbsoluteFieldNumber());
        }
    }

    /**
     * Method to return a detached copy of the container.
     * Recurse sthrough the elements so that they are likewise detached.
     * @param state State for detachment process
     * @return The detached container
     */
    public java.util.List detachCopy(FetchPlanState state)
    {
        java.util.List detached = new java.util.ArrayList();
        SCOUtils.detachCopyForCollection(ownerOP, toArray(), state, detached);
        return detached;
    }

    /**
     * Method to return an attached copy of the passed (detached) value. The returned attached copy
     * is a SCO wrapper. Goes through the existing elements in the store for this owner field and
     * removes ones no longer present, and adds new elements. All elements in the (detached)
     * value are attached.
     * @param value The new (collection) value
     */
    public void attachCopy(java.util.List value)
    {
        // Attach all of the elements in the new list
        boolean elementsWithoutIdentity = SCOUtils.collectionHasElementsWithoutIdentity(ownerMmd);

        java.util.List attachedElements = new java.util.ArrayList(value.size());
        SCOUtils.attachCopyForCollection(ownerOP, value.toArray(), attachedElements, elementsWithoutIdentity);

        // Update the attached list with the detached elements
        SCOUtils.updateListWithListElements(this, attachedElements);
    }

    // ------------------- Implementation of List methods ----------------------
 
    /**
     * Creates and returns a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return The cloned object
     */
    public Object clone()
    {
        return ((java.util.ArrayList)delegate).clone();
    }

    /**
     * Accessor for whether an element is contained in the List.
     * @param element The element
     * @return Whether the element is contained here
     **/
    public synchronized boolean contains(Object element)
    {
        return delegate.contains(element);
    }

    /**
     * Accessor for whether a collection of elements are contained here.
     * @param c The collection of elements.
     * @return Whether they are contained.
     **/
    public synchronized boolean containsAll(java.util.Collection c)
    {
        return delegate.containsAll(c);
    }

    /**
     * Equality operator.
     * @param o The object to compare against.
     * @return Whether this object is the same.
     **/
    public synchronized boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    /**
     * Method to retrieve an element no.
     * @param index The item to retrieve
     * @return The element at that position.
     */
    public E get(int index)
    {
        return delegate.get(index);
    }

    public synchronized int hashCode()
    {
        return delegate.hashCode();
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
     * Accessor for whether the List is empty.
     * @return Whether it is empty.
     **/
    public synchronized boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /**
     * Method to retrieve the last position of the element.
     * @param element The element
     * @return The last position of this element in the List.
     **/
    public int lastIndexOf(Object element)
    {
        return delegate.lastIndexOf(element);
    }

    /**
     * Accessor for an iterator for the List
     * @return The iterator
     **/
    public synchronized Iterator iterator()
    {
        return new SCOListIterator(this, ownerOP, delegate, null, true, -1);
    }

    /**
     * Method to retrieve a List iterator for the list.
     * @return The iterator
     **/
    public ListIterator listIterator()
    {
        return new SCOListIterator(this, ownerOP, delegate, null, true, -1);
    }

    /**
     * Method to retrieve a List iterator for the list from the index.
     * @param index The start point 
     * @return The iterator
     **/
    public ListIterator listIterator(int index)
    {
        return new SCOListIterator(this, ownerOP, delegate, null, true, index);
    }

    /**
     * Accessor for the size of the List
     * @return The size
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
     * Method to return the List as an array.
     * @return The array
     **/
    public synchronized Object[] toArray()
    {
        return delegate.toArray();
    }

    /**
     * Method to return the List as an array.
     * @param a The array to copy to
     * @return The array
     **/
    public synchronized Object[] toArray(Object a[])
    {
        return delegate.toArray(a);
    }

    /**
     * Method to add an element to the List
     * @param element The element to add
     * @return Whether it was added successfully.
     **/
    public synchronized boolean add(E element)
    {
        boolean success = delegate.add(element);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            ownerOP.getExecutionContext().getRelationshipManager(ownerOP).relationAdd(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (success)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), element));
            }
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Method to add an element to the List at a position.
     * @param element The element to add
     * @param index The position
     **/
    public void add(int index, E element)
    {
        delegate.add(index, element);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            ownerOP.getExecutionContext().getRelationshipManager(ownerOP).relationAdd(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (SCOUtils.useQueuedUpdate(ownerOP))
        {
            ownerOP.getExecutionContext().addOperationToQueue(new ListAddAtOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), index, element));
        }
        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to add a Collection to the ArrayList.
     * @param elements The collection
     * @return Whether it was added ok.
     */
    public boolean addAll(Collection elements)
    {
        boolean success = delegate.addAll(elements);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            // Relationship management
            Iterator iter = elements.iterator();
            RelationshipManager relMgr = ownerOP.getExecutionContext().getRelationshipManager(ownerOP);
            while (iter.hasNext())
            {
                relMgr.relationAdd(ownerMmd.getAbsoluteFieldNumber(), iter.next());
            }
        }

        if (success)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                for (Object element : elements)
                {
                    ownerOP.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), element));
                }
            }
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Method to add a collection of elements.
     * @param elements The collection of elements to add.
     * @param index The position to add them
     * @return Whether they were added successfully.
     **/
    public boolean addAll(int index, Collection elements)
    {
        boolean success = delegate.addAll(index, elements);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            // Relationship management
            Iterator iter = elements.iterator();
            RelationshipManager relMgr = ownerOP.getExecutionContext().getRelationshipManager(ownerOP);
            while (iter.hasNext())
            {
                relMgr.relationAdd(ownerMmd.getAbsoluteFieldNumber(), iter.next());
            }
        }

        if (success)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                int pos = index;
                for (Object element : elements)
                {
                    ownerOP.getExecutionContext().addOperationToQueue(new ListAddAtOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), pos++, element));
                }
            }
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * Method to clear the List
     */
    public synchronized void clear()
    {
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            // Relationship management
            Iterator iter = delegate.iterator();
            RelationshipManager relMgr = ownerOP.getExecutionContext().getRelationshipManager(ownerOP);
            while (iter.hasNext())
            {
                relMgr.relationRemove(ownerMmd.getAbsoluteFieldNumber(), iter.next());
            }
        }

        if (ownerOP != null && !delegate.isEmpty())
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                java.util.List copy = new java.util.ArrayList(delegate);
                Iterator iter = copy.iterator();
                while (iter.hasNext())
                {
                    ownerOP.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), iter.next(), true));
                }
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                java.util.List copy = new java.util.ArrayList(delegate);
                Iterator iter = copy.iterator();
                while (iter.hasNext())
                {
                    ownerOP.getExecutionContext().deleteObjectInternal(iter.next());
                }
            }
        }

        delegate.clear();

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to remove an element from the List
     * @param element The Element to remove
     * @return Whether it was removed successfully.
     **/
    public synchronized boolean remove(Object element)
    {
        return remove(element, true);
    }

    /**
     * Method to remove an element from the List
     * @param element The Element to remove
     * @return Whether it was removed successfully.
     **/
    public synchronized boolean remove(Object element, boolean allowCascadeDelete)
    {
        boolean success = delegate.remove(element);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            ownerOP.getExecutionContext().getRelationshipManager(ownerOP).relationRemove(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (ownerOP != null && allowCascadeDelete)
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), element, allowCascadeDelete));
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerOP.getExecutionContext().deleteObjectInternal(element);
            }
        }

        if (success)
        {
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }

        return success;
    }

    /**
     * Method to remove an element from the ArrayList.
     * @param index The element position.
     * @return The object that was removed
     */
    public E remove(int index)
    {
        E element = delegate.remove(index);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            ownerOP.getExecutionContext().getRelationshipManager(ownerOP).relationRemove(ownerMmd.getAbsoluteFieldNumber(), element);
        }

        if (ownerOP != null)
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new ListRemoveAtOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), index, element));
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerOP.getExecutionContext().deleteObjectInternal(element);
            }
        }

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return element;
    }

    /**
     * Method to remove a collection of elements from the List.
     * @param elements Collection of elements to remove
     * @return Whether it was successful.
     */
    public boolean removeAll(Collection elements)
    {
        boolean success = delegate.removeAll(elements);
        if (ownerOP != null && ownerOP.getExecutionContext().getManageRelations())
        {
            // Relationship management
            Iterator iter = elements.iterator();
            RelationshipManager relMgr = ownerOP.getExecutionContext().getRelationshipManager(ownerOP);
            while (iter.hasNext())
            {
                relMgr.relationRemove(ownerMmd.getAbsoluteFieldNumber(), iter.next());
            }
        }

        if (ownerOP != null && elements != null && !elements.isEmpty())
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                Iterator iter = elements.iterator();
                while (iter.hasNext())
                {
                    ownerOP.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), iter.next(), true));
                }
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                Iterator iter = elements.iterator();
                while (iter.hasNext())
                {
                    ownerOP.getExecutionContext().deleteObjectInternal(iter.next());
                }
            }
        }

        if (success)
        {
            makeDirty();
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }

        return success;
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
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * wrapper addition that allows turning off of the dependent-field checks
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
        if (ownerOP != null && allowDependentField && !delegate.contains(prevElement))
        {
            // Cascade delete
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new ListRemoveAtOperation(ownerOP, ownerMmd.getAbsoluteFieldNumber(), index, prevElement));
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                ownerOP.getExecutionContext().deleteObjectInternal(prevElement);
            }
        }

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return prevElement;
    }

    /**
     * Method to set the element at a position in the ArrayList.
     * @param index The position
     * @param element The new element
     * @return The element previously at that position
     */
    public E set(int index, E element)
    {
        return set(index, element, true);
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
        return new java.util.ArrayList(delegate);
    }
}