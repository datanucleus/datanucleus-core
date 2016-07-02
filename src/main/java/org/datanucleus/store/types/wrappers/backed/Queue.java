/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.wrappers.backed;

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.flush.CollectionAddOperation;
import org.datanucleus.flush.CollectionClearOperation;
import org.datanucleus.flush.ListRemoveAtOperation;
import org.datanucleus.flush.CollectionRemoveOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.BackedSCOStoreManager;
import org.datanucleus.store.types.SCOCollectionIterator;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.scostore.ListStore;
import org.datanucleus.store.types.scostore.Store;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class Queue object.
 * This class extends AbstractQueue, using that class to contain the current objects, and the backing CollectionStore 
 * to be the interface to the datastore. A "backing store" is not present for datastores that dont use
 * DatastoreClass, or if the container is serialised or non-persistent.
 * 
 * <H3>Modes of Operation</H3>
 * The user can operate the list in 2 modes.
 * The <B>cached</B> mode will use an internal cache of the elements (in the "delegate") reading them at
 * the first opportunity and then using the cache thereafter.
 * The <B>non-cached</B> mode will just go direct to the "backing store" each call.
 *
 * <H3>Mutators</H3>
 * When the "backing store" is present any updates are passed direct to the datastore as well as to the "delegate".
 * If the "backing store" isn't present the changes are made to the "delegate" only.
 *
 * <H3>Accessors</H3>
 * When any accessor method is invoked, it typically checks whether the container has been loaded from its
 * "backing store" (where present) and does this as necessary. Some methods (<B>size()</B>) just check if 
 * everything is loaded and use the delegate if possible, otherwise going direct to the datastore.
 */
public class Queue<E> extends org.datanucleus.store.types.wrappers.Queue<E> implements BackedSCO
{
    protected transient ListStore<E> backingStore; // Really need a List since the Queue needs ordering
    protected transient boolean allowNulls = false;
    protected transient boolean useCache = true;
    protected transient boolean isCacheLoaded = false;

    /**
     * Constructor. 
     * @param op The ObjectProvider for this set.
     * @param mmd Metadata for the member
     **/
    public Queue(ObjectProvider op, AbstractMemberMetaData mmd)
    {
        super(op, mmd);

        // Set up our delegate, using suitable comparator (DN extension to JDO)
        Comparator comparator = SCOUtils.getComparator(ownerMmd, ownerOP.getExecutionContext().getClassLoaderResolver());
        if (comparator != null)
        {
            this.delegate = new java.util.PriorityQueue(5, comparator);
        }
        else
        {
            this.delegate = new java.util.PriorityQueue();
        }

        ExecutionContext ec = ownerOP.getExecutionContext();
        allowNulls = SCOUtils.allowNullsInContainer(allowNulls, mmd);
        useCache = SCOUtils.useContainerCache(ownerOP, mmd);

        if (!SCOUtils.collectionHasSerialisedElements(mmd) && 
            mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            ClassLoaderResolver clr = ec.getClassLoaderResolver();
            this.backingStore = (ListStore)((BackedSCOStoreManager)ownerOP.getStoreManager()).getBackingStoreForField(clr,mmd,java.util.Queue.class);
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(SCOUtils.getContainerInfoMessage(ownerOP, ownerMmd.getName(), this,
                useCache, allowNulls, SCOUtils.useCachedLazyLoading(ownerOP, ownerMmd)));
        }
    }

    public void initialise(java.util.Queue newValue, Object oldValue)
    {
        if (newValue != null)
        {
            // Check for the case of serialised PC elements, and assign ObjectProviders to the elements without
            if (SCOUtils.collectionHasSerialisedElements(ownerMmd) && ownerMmd.getCollection().elementIsPersistent())
            {
                ExecutionContext ec = ownerOP.getExecutionContext();
                Iterator iter = newValue.iterator();
                while (iter.hasNext())
                {
                    Object pc = iter.next();
                    ObjectProvider objSM = ec.findObjectProvider(pc);
                    if (objSM == null)
                    {
                        objSM = ec.getNucleusContext().getObjectProviderFactory().newForEmbedded(ec, pc, false, ownerOP, ownerMmd.getAbsoluteFieldNumber());
                    }
                }
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023008", ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + newValue.size()));
            }

            // TODO This does clear+addAll : Improve this and work out which elements are added and which deleted
            if (backingStore != null)
            {
                if (SCOUtils.useQueuedUpdate(ownerOP))
                {
                    if (ownerOP.isFlushedToDatastore() || !ownerOP.getLifecycleState().isNew())
                    {
                        ownerOP.getExecutionContext().addOperationToQueue(new CollectionClearOperation(ownerOP, backingStore));

                        for (Object element : newValue)
                        {
                            ownerOP.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerOP, backingStore, element));
                        }
                    }
                }
                else
                {
                    backingStore.clear(ownerOP);

                    try
                    {
                        backingStore.addAll(ownerOP, newValue, useCache ? 0 : -1);
                    }
                    catch (NucleusDataStoreException dse)
                    {
                        NucleusLogger.PERSISTENCE.warn(Localiser.msg("023013", "addAll", ownerMmd.getName(), dse));
                    }
                }
            }
            delegate.addAll(newValue);
            isCacheLoaded = true;
            makeDirty();
        }
    }

    /**
     * Method to initialise the SCO from an existing value.
     * @param c The object to set from
     */
    public void initialise(java.util.Queue c)
    {
        if (c != null)
        {
            // Check for the case of serialised PC elements, and assign ObjectProviders to the elements without
            if (SCOUtils.collectionHasSerialisedElements(ownerMmd) && ownerMmd.getCollection().elementIsPersistent())
            {
                ExecutionContext ec = ownerOP.getExecutionContext();
                Iterator iter = c.iterator();
                while (iter.hasNext())
                {
                    Object pc = iter.next();
                    ObjectProvider objSM = ec.findObjectProvider(pc);
                    if (objSM == null)
                    {
                        objSM = ec.getNucleusContext().getObjectProviderFactory().newForEmbedded(ec, pc, false, ownerOP, ownerMmd.getAbsoluteFieldNumber());
                    }
                }
            }

            if (backingStore != null && useCache && !isCacheLoaded)
            {
                // Mark as loaded
                isCacheLoaded = true;
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023007", ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + c.size()));
            }
            delegate.clear();
            delegate.addAll(c);
        }
    }

    /**
     * Method to initialise the SCO for use.
     */
    public void initialise()
    {
        if (useCache && !SCOUtils.useCachedLazyLoading(ownerOP, ownerMmd))
        {
            // Load up the container now if not using lazy loading
            loadFromStore();
        }
    }

    // ----------------------- Implementation of SCO methods -------------------

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.Queue getValue()
    {
        loadFromStore();
        return super.getValue();
    }

    /**
     * Method to effect the load of the data in the SCO.
     * Used when the SCO supports lazy-loading to tell it to load all now.
     */
    public void load()
    {
        if (useCache)
        {
            loadFromStore();
        }
    }

    /**
     * Method to return if the SCO has its contents loaded.
     * If the SCO doesn't support lazy loading will just return true.
     * @return Whether it is loaded
     */
    public boolean isLoaded()
    {
        return useCache ? isCacheLoaded : false;
    }

    /**
     * Method to load all elements from the "backing store" where appropriate.
     */
    protected void loadFromStore()
    {
        if (backingStore != null && !isCacheLoaded)
        {
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023006", 
                    ownerOP.getObjectAsPrintable(), ownerMmd.getName()));
            }
            delegate.clear();
            Iterator<E> iter=backingStore.iterator(ownerOP);
            while (iter.hasNext())
            {
                delegate.add(iter.next());
            }

            isCacheLoaded = true;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.backed.BackedSCO#getBackingStore()
     */
    public Store getBackingStore()
    {
        return backingStore;
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
        if (backingStore != null)
        {
            backingStore.updateEmbeddedElement(ownerOP, element, fieldNumber, value);
        }
    }

    /**
     * Method to unset the owner and field information.
     */
    public synchronized void unsetOwner()
    {
        super.unsetOwner();
        if (backingStore != null)
        {
            backingStore = null;
        }
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
        if (useCache)
        {
            loadFromStore();
        }

        return super.clone();
    }

    /**
     * Accessor for whether an element is contained in the Collection.
     * @param element The element
     * @return Whether the element is contained here
     **/
    public synchronized boolean contains(Object element)
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.contains(element);
        }
        else if (backingStore != null)
        {
            return backingStore.contains(ownerOP,element);
        }

        return delegate.contains(element);
    }

    /**
     * Accessor for whether a collection of elements are contained here.
     * @param c The collection of elements.
     * @return Whether they are contained.
     **/
    public synchronized boolean containsAll(java.util.Collection c)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            java.util.HashSet h=new java.util.HashSet(c);
            Iterator iter=iterator();
            while (iter.hasNext())
            {
                h.remove(iter.next());
            }

            return h.isEmpty();
        }

        return delegate.containsAll(c);
    }

    /**
     * Equality operator.
     * @param o The object to compare against.
     * @return Whether this object is the same.
     **/
    public synchronized boolean equals(Object o)
    {
        if (useCache)
        {
            loadFromStore();
        }

        if (o == this)
        {
            return true;
        }
        if (!(o instanceof java.util.Queue))
        {
            return false;
        }
        java.util.Queue c = (java.util.Queue)o;

        return c.size() == size() && containsAll(c);
    }

    /**
     * Hashcode operator.
     * @return The Hash code.
     **/
    public synchronized int hashCode()
    {
        if (useCache)
        {
            loadFromStore();
        }
        return delegate.hashCode();
    }

    /**
     * Accessor for whether the Collection is empty.
     * @return Whether it is empty.
     **/
    public synchronized boolean isEmpty()
    {
        return (size() == 0);
    }

    /**
     * Accessor for an iterator for the Collection.
     * @return The iterator
     **/
    public synchronized Iterator<E> iterator()
    {
        // Populate the cache if necessary
        if (useCache)
        {
            loadFromStore();
        }
        return new SCOCollectionIterator(this, ownerOP, delegate, backingStore, useCache);
    }

    /**
     * Method to peek at the next element in the Queue.
     * @return The element
     **/
    public synchronized E peek()
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.get(ownerOP, 0);
        }

        return delegate.peek();
    }

    /**
     * Accessor for the size of the Collection.
     * @return The size
     **/
    public synchronized int size()
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.size();
        }
        else if (backingStore != null)
        {
            return backingStore.size(ownerOP);
        }

        return delegate.size();
    }

    /**
     * Method to return the Collection as an array.
     * @return The array
     **/
    public synchronized Object[] toArray()
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return SCOUtils.toArray(backingStore,ownerOP);
        }  
        return delegate.toArray();
    }

    /**
     * Method to return the Collection as an array.
     * @param a The array to write the results to
     * @return The array
     **/
    public synchronized Object[] toArray(Object a[])
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return SCOUtils.toArray(backingStore,ownerOP,a);
        }  
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
     **/
    public synchronized boolean add(E element)
    {
        // Reject inappropriate elements
        if (!allowNulls && element == null)
        {
            throw new NullPointerException("Nulls not allowed for collection at field " + ownerMmd.getName() + " but element is null");
        }

        if (useCache)
        {
            loadFromStore();
        }

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerOP, backingStore, element));
            }
            else
            {
                try
                {
                    backingStore.add(ownerOP, element, useCache ? delegate.size() : -1);
                }
                catch (NucleusDataStoreException dse)
                {
                    NucleusLogger.PERSISTENCE.warn(Localiser.msg("023013", "add", ownerMmd.getName(), dse));
                    backingSuccess = false;
                }
            }
        }

        // Only make it dirty after adding the field to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        boolean delegateSuccess = delegate.add(element);

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return backingStore != null ? backingSuccess : delegateSuccess;
    }

    /**
     * Method to add a collection of elements.
     * @param elements The collection of elements to add.
     * @return Whether they were added successfully.
     **/
    public synchronized boolean addAll(java.util.Collection elements)
    {
        if (useCache)
        {
            loadFromStore();
        }

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                for (Object element : elements)
                {
                    ownerOP.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerOP, backingStore, element));
                }
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.addAll(ownerOP, elements, useCache ? delegate.size() : -1);
                }
                catch (NucleusDataStoreException dse)
                {
                    NucleusLogger.PERSISTENCE.warn(Localiser.msg("023013", "addAll", ownerMmd.getName(), dse));
                    backingSuccess = false;
                }
            }
        }

        // Only make it dirty after adding the field to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        boolean delegateSuccess = delegate.addAll(elements);

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return backingStore != null ? backingSuccess : delegateSuccess;
    }

    /**
     * Method to clear the Collection.
     **/
    public synchronized void clear()
    {
        makeDirty();
        delegate.clear();

        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new CollectionClearOperation(ownerOP, backingStore));
            }
            else
            {
                backingStore.clear(ownerOP);
            }
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to offer an element to the Queue.
     * @param element The element to offer
     * @return Whether it was added successfully.
     **/
    public synchronized boolean offer(E element)
    {
        return add(element);
    }

    /**
     * Method to poll the next element in the Queue.
     * @return The element (now removed)
     **/
    public synchronized E poll()
    {
        makeDirty();
 
        if (useCache)
        {
            loadFromStore();
        }

        int size = useCache ? delegate.size() : -1;
        E delegateObject = delegate.poll();
        E backingObject = null;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new ListRemoveAtOperation(ownerOP, backingStore, 0));
            }
            else
            {
                try
                {
                    backingObject = backingStore.remove(ownerOP, 0, size);
                }
                catch (NucleusDataStoreException dse)
                {
                    backingObject = null;
                }
            }
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }

        return backingStore != null ? backingObject : delegateObject;
    }

    /**
     * Method to remove an element from the Collection.
     * @param element The Element to remove
     * @return Whether it was removed successfully.
     **/
    public synchronized boolean remove(Object element)
    {
        return remove(element, true);
    }

    /**
     * Method to remove an element from the collection, and observe the flag for whether to allow cascade delete.
     * @param element The element
     * @param allowCascadeDelete Whether to allow cascade delete
     */
    public synchronized boolean remove(Object element, boolean allowCascadeDelete)
    {
        makeDirty();

        if (useCache)
        {
            loadFromStore();
        }

        int size = useCache ? delegate.size() : -1;
        boolean contained = delegate.contains(element);
        boolean delegateSuccess = delegate.remove(element);

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                backingSuccess = contained;
                if (backingSuccess)
                {
                    ownerOP.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerOP, backingStore, element, allowCascadeDelete));
                }
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.remove(ownerOP, element, size, allowCascadeDelete);
                }
                catch (NucleusDataStoreException dse)
                {
                    NucleusLogger.PERSISTENCE.warn(Localiser.msg("023013", "remove", ownerMmd.getName(), dse));
                    backingSuccess = false;
                }
            }
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }

        return backingStore != null ? backingSuccess : delegateSuccess;
    }

    /**
     * Method to remove a Collection of elements.
     * @param elements The collection to remove
     * @return Whether they were removed successfully.
     **/
    public synchronized boolean removeAll(java.util.Collection elements)
    {
        makeDirty();
 
        if (useCache)
        {
            loadFromStore();
        }

        int size = useCache ? delegate.size() : -1;
        Collection contained = null;
        if (backingStore != null && SCOUtils.useQueuedUpdate(ownerOP))
        {
            // Check which are contained before updating the delegate
            contained = new java.util.HashSet();
            for (Object elem : elements)
            {
                if (contains(elem))
                {
                    contained.add(elem);
                }
            }
        }
        boolean delegateSuccess = delegate.removeAll(elements);

        if (backingStore != null)
        {
            boolean backingSuccess = true;
            if (SCOUtils.useQueuedUpdate(ownerOP))
            {
                backingSuccess = false;
                for (Object element : contained)
                {
                    backingSuccess = true;
                    ownerOP.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerOP, backingStore, element, true));
                }
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.removeAll(ownerOP, elements, size);
                }
                catch (NucleusDataStoreException dse)
                {
                    NucleusLogger.PERSISTENCE.warn(Localiser.msg("023013", "removeAll", ownerMmd.getName(), dse));
                    backingSuccess = false;
                }
            }

            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }

            return backingSuccess;
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return delegateSuccess;
    }

    /**
     * Method to retain a Collection of elements (and remove all others).
     * @param c The collection to retain
     * @return Whether they were retained successfully.
     **/
    public synchronized boolean retainAll(java.util.Collection c)
    {
        makeDirty();

        if (useCache)
        {
            loadFromStore();
        }

        boolean modified = false;
        Iterator iter = iterator();
        while (iter.hasNext())
        {
            Object element = iter.next();
            if (!c.contains(element))
            {
                iter.remove();
                modified = true;
            }
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return modified;
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
        if (useCache)
        {
            loadFromStore();
            return new java.util.PriorityQueue(delegate);
        }

        // TODO Cater for non-cached collection, load elements in a DB call.
        return new java.util.PriorityQueue(delegate);
    }

    @Override
    public Spliterator spliterator()
    {
        if (backingStore != null && useCache && !isCacheLoaded)
        {
            loadFromStore();
        }
        // TODO If using backing store yet not caching, then this will fail
        return delegate.spliterator();
    }

    @Override
    public Stream stream()
    {
        if (backingStore != null && useCache && !isCacheLoaded)
        {
            loadFromStore();
        }
        // TODO If using backing store yet not caching, then this will fail
        return delegate.stream();
    }

    @Override
    public Stream parallelStream()
    {
        if (backingStore != null && useCache && !isCacheLoaded)
        {
            loadFromStore();
        }
        // TODO If using backing store yet not caching, then this will fail
        return delegate.parallelStream();
    }
}