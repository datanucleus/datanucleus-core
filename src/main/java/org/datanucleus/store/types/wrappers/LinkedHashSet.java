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
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.datanucleus.FetchPlanState;
import org.datanucleus.flush.CollectionAddOperation;
import org.datanucleus.flush.CollectionRemoveOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.state.RelationshipManager;
import org.datanucleus.store.types.SCOCollection;
import org.datanucleus.store.types.SCOCollectionIterator;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class LinkedHashSet object.
 * This is the simplified form that intercepts mutators and marks the field as dirty.
 * It also handles cascade-delete triggering for persistable elements.
 */
public class LinkedHashSet<E> extends java.util.LinkedHashSet<E> implements SCOCollection<java.util.LinkedHashSet<E>, E>
{
    protected transient DNStateManager ownerSM;
    protected transient AbstractMemberMetaData ownerMmd;

    /** The internal "delegate". */
    protected java.util.LinkedHashSet<E> delegate;

    /**
     * Constructor, using StateManager of the "owner" and the field name.
     * @param sm The owner StateManager
     * @param mmd Metadata for the member
     */
    public LinkedHashSet(DNStateManager sm, AbstractMemberMetaData mmd)
    {
        this.ownerSM = sm;
        this.ownerMmd = mmd;
    }

    public void initialise(java.util.LinkedHashSet<E> newValue, Object oldValue)
    {
        initialise(newValue);
    }

    public void initialise(java.util.LinkedHashSet c)
    {
        if (c != null)
        {
            delegate = c;
        }
        else
        {
            delegate = new java.util.LinkedHashSet();
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
    public java.util.LinkedHashSet<E> getValue()
    {
        return delegate;
    }

    public void setValue(java.util.LinkedHashSet<E> value)
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
    public java.util.LinkedHashSet detachCopy(FetchPlanState state)
    {
        java.util.LinkedHashSet detached = new java.util.LinkedHashSet();
        SCOUtils.detachCopyForCollection(ownerSM, toArray(), state, detached);
        return detached;
    }

    /**
     * Method to return an attached copy of the passed (detached) value. The returned attached copy
     * is a SCO wrapper. Goes through the existing elements in the store for this owner field and
     * removes ones no longer present, and adds new elements. All elements in the (detached)
     * value are attached.
     * @param value The new (collection) value
     */
    public void attachCopy(java.util.LinkedHashSet value)
    {
        boolean elementsWithoutIdentity = SCOUtils.collectionHasElementsWithoutIdentity(ownerMmd);
        SCOUtils.attachCopyElements(ownerSM, this, value, elementsWithoutIdentity);

/*        // Remove any no-longer-needed elements from this collection
        SCOUtils.attachRemoveDeletedElements(ownerSM.getExecutionContext().getApiAdapter(), this, c, elementsWithoutIdentity);

        // Persist any new elements and form the attached elements collection
        java.util.Collection attachedElements = new java.util.LinkedHashSet(c.size());
        SCOUtils.attachCopyForCollection(ownerSM, c.toArray(), attachedElements, elementsWithoutIdentity);

        // Add any new elements to this collection
        SCOUtils.attachAddNewElements(ownerSM.getExecutionContext().getApiAdapter(), this, attachedElements, elementsWithoutIdentity);*/
    }

    // ------------------ Implementation of LinkedHashSet methods --------------------

    /**
     * Creates and returns a copy of this object.
     * @return The cloned object
     */
    public Object clone()
    {
        return delegate.clone();
    }

    /**
     * Accessor for whether an element is contained in this Set.
     * @param element The element
     * @return Whether it is contained.
     **/
    public boolean contains(Object element)
    {
        return delegate.contains(element);
    }

    /**
     * Accessor for whether a collection is contained in this Set.
     * @param c The collection
     * @return Whether it is contained.
     */
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
     * Accessor for whether the LinkedHashSet is empty.
     * @return Whether it is empty.
     **/
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /**
     * Accessor for an iterator for the Set.
     * @return The iterator
     **/
    public Iterator<E> iterator()
    {
        return new SCOCollectionIterator(this, ownerSM, delegate, null, true);
    }

    /**
     * Accessor for the size of the LinkedHashSet.
     * @return The size.
     **/
    public int size()
    {
        return delegate.size();
    }

    /**
     * Method to return the list as an array.
     * @return The array
     **/
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    /**
     * Method to return the list as an array.
     * @param a The runtime types of the array being defined by this param
     * @return The array
     **/
    public Object[] toArray(Object a[])
    {
        return delegate.toArray(a);
    }
 
    // ------------------------------ Mutator methods --------------------------

    /**
     * Method to add an element to the LinkedHashSet.
     * @param element The new element
     * @return Whether it was added ok.
     **/
    public boolean add(E element)
    {
        boolean success = delegate.add(element);
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            // Relationship management
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
     * Method to add a collection to the LinkedHashSet.
     * @param elements The collection
     * @return Whether it was added ok.
     **/
    public boolean addAll(Collection elements)
    {
        boolean success = delegate.addAll(elements);
        if (ownerSM != null && ownerSM.getExecutionContext().getManageRelations())
        {
            // Relationship management
            for (Object elem : elements)
            {
                ownerSM.getExecutionContext().getRelationshipManager(ownerSM).relationAdd(ownerMmd.getAbsoluteFieldNumber(), elem);
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
     * Method to clear the LinkedHashSet
     */
    public void clear()
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
                // Queue the cascade delete
                Iterator iter = delegate.iterator();
                while (iter.hasNext())
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), iter.next(), true));
                }
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                // Perform the cascade delete
                Iterator iter = delegate.iterator();
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
     * Method to remove an element from the List
     * @param element The Element to remove
     * @return Whether it was removed successfully.
     */
    public boolean remove(Object element)
    {
        return remove(element, true);
    }

    /**
     * Method to remove an element from the List
     * @param element The Element to remove
     * @return Whether it was removed successfully.
     */
    public boolean remove(Object element, boolean allowCascadeDelete)
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
                // Queue the cascade delete
                ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), element, allowCascadeDelete));
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                // Perform the cascade delete
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
     * Method to remove all elements from the collection from the LinkedHashSet.
     * @param elements The collection of elements to remove 
     * @return Whether it was removed ok.
     **/
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
                // Queue the cascade delete
                for (Object elem : elements)
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), elem, true));
                }
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                // Perform the cascade delete
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
     * Method to retain a Collection of elements (and remove all others).
     * @param c The collection to retain
     * @return Whether they were retained successfully.
     */
    public boolean retainAll(java.util.Collection c)
    {
        if (c == null)
        {
            throw new NullPointerException("Input collection was null");
        }
        Collection collToRemove = new java.util.LinkedHashSet();
        for (Object o : delegate)
        {
            if (!c.contains(o))
            {
                collToRemove.add(o);
            }
        }

        boolean success = delegate.retainAll(c);
        if (success)
        {
            makeDirty();
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                // Queue any cascade delete
                for (Object elem : collToRemove)
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, ownerMmd.getAbsoluteFieldNumber(), elem, true));
                }
            }
            else if (SCOUtils.hasDependentElement(ownerMmd))
            {
                // Perform the cascade delete
                for (Object elem : collToRemove)
                {
                    ownerSM.getExecutionContext().deleteObjectInternal(elem);
                }
            }

            if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }
        }
        return success;
    }

    /**
     * The writeReplace method is called when ObjectOutputStream is preparing
     * to write the object to the stream. The ObjectOutputStream checks
     * whether the class defines the writeReplace method. If the method is
     * defined, the writeReplace method is called to allow the object to
     * designate its replacement in the stream. The object returned should be
     * either of the same type as the object passed in or an object that when
     * read and resolved will result in an object of a type that is compatible
     * with all references to the object.
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        return new java.util.LinkedHashSet(delegate);
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
    public Stream stream()
    {
        return delegate.stream();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#parallelStream()
     */
    @Override
    public Stream parallelStream()
    {
        return delegate.parallelStream();
    }

    /* (non-Javadoc)
     * @see java.util.HashSet#spliterator()
     */
    @Override
    public Spliterator spliterator()
    {
        return delegate.spliterator();
    }
}