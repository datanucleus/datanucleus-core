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
package org.datanucleus.store.types.wrappers.backed;

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.flush.ListAddAtOperation;
import org.datanucleus.flush.CollectionAddOperation;
import org.datanucleus.flush.CollectionClearOperation;
import org.datanucleus.flush.ListRemoveAtOperation;
import org.datanucleus.flush.CollectionRemoveOperation;
import org.datanucleus.flush.ListSetOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.BackedSCOStoreManager;
import org.datanucleus.store.scostore.ListStore;
import org.datanucleus.store.scostore.Store;
import org.datanucleus.store.types.SCOListIterator;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class ArrayList object.
 * This class extends ArrayList, using that class to contain the current objects, and the backing ListStore 
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
public class ArrayList extends org.datanucleus.store.types.wrappers.ArrayList implements BackedSCO
{
    protected transient ListStore backingStore;
    protected transient boolean allowNulls = false;
    protected transient boolean useCache=true;
    protected transient boolean isCacheLoaded=false;
    protected transient boolean queued = false;

    /**
     * Constructor, using the ObjectProvider of the "owner" and the field name.
     * @param op The owner ObjectProvider
     * @param mmd Metadata for the member
     */
    public ArrayList(ObjectProvider op, AbstractMemberMetaData mmd)
    {
        super(op, mmd);

        // Set up our delegate
        this.delegate = new java.util.ArrayList();

        ExecutionContext ec = op.getExecutionContext();
        allowNulls = SCOUtils.allowNullsInContainer(allowNulls, mmd);
        queued = ec.isDelayDatastoreOperationsEnabled();
        useCache = SCOUtils.useContainerCache(op, mmd);

        if (!SCOUtils.collectionHasSerialisedElements(mmd) && mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            ClassLoaderResolver clr = ec.getClassLoaderResolver();
            this.backingStore = (ListStore)((BackedSCOStoreManager)op.getStoreManager()).getBackingStoreForField(clr, mmd, java.util.ArrayList.class);
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(SCOUtils.getContainerInfoMessage(op, ownerMmd.getName(), this,
                useCache, queued, allowNulls, SCOUtils.useCachedLazyLoading(op, ownerMmd)));
        }
    }

    /**
     * Method to initialise the SCO from an existing value.
     * @param o The object to set from
     * @param forInsert Whether the object needs inserting in the datastore with this value
     * @param forUpdate Whether to update the SCO in the datastore with this value
     */
    public void initialise(Object o, boolean forInsert, boolean forUpdate)
    {
        Collection c = (Collection) o;
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
                    ObjectProvider objOP = ec.findObjectProvider(pc);
                    if (objOP == null)
                    {
                        objOP = ec.getNucleusContext().getObjectProviderFactory().newForEmbedded(ec, pc, false, ownerOP, ownerMmd.getAbsoluteFieldNumber());
                    }
                }
            }

            if (backingStore != null && useCache && !isCacheLoaded)
            {
                // Mark as loaded so we just use the value
                isCacheLoaded = true;
            }

            if (forInsert)
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023007", 
                        ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + c.size()));
                }
                if (useCache)
                {
                    loadFromStore();
                }
                if (backingStore != null)
                {
                    if (SCOUtils.useQueuedUpdate(queued, ownerOP))
                    {
                        for (Object element : c)
                        {
                            ownerOP.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerOP, backingStore, element));
                        }
                    }
                    else
                    {
                        try
                        {
                            backingStore.addAll(ownerOP, c, (useCache ? delegate.size() : -1));
                        }
                        catch (NucleusDataStoreException dse)
                        {
                            NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("023013", "addAll", ownerMmd.getName(), dse));
                        }
                    }
                }
                makeDirty();
                delegate.addAll(c);
            }
            else if (forUpdate)
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023008", 
                        ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + c.size()));
                }

                // TODO This does clear+addAll : Improve this and work out which elements are added and which deleted
                if (backingStore != null)
                {
                    if (SCOUtils.useQueuedUpdate(queued, ownerOP))
                    {
                        ownerOP.getExecutionContext().addOperationToQueue(new CollectionClearOperation(ownerOP, backingStore));
                    }
                    else
                    {
                        backingStore.clear(ownerOP);
                    }
                }

                if (useCache)
                {
                    loadFromStore();
                }
                if (backingStore != null)
                {
                    if (SCOUtils.useQueuedUpdate(queued, ownerOP))
                    {
                        for (Object element : c)
                        {
                            ownerOP.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerOP, backingStore, element));
                        }
                    }
                    else
                    {
                        try
                        {
                            backingStore.addAll(ownerOP, c, (useCache ? delegate.size() : -1));
                        }
                        catch (NucleusDataStoreException dse)
                        {
                            NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("023013", "addAll", ownerMmd.getName(), dse));
                        }
                    }
                }
                delegate.addAll(c);
                makeDirty();
            }
            else
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023007", 
                        ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + c.size()));
                }
                delegate.clear();
                delegate.addAll(c);
            }
        }
    }

    /**
     * Method to initialise the SCO for use.
     */
    public void initialise()
    {
        if (useCache && !SCOUtils.useCachedLazyLoading(ownerOP, ownerMmd))
        {
            // Load up the collection now if not using lazy loading
            loadFromStore();
        }
    }

    // ----------------------- Implementation of SCO methods -------------------

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public Object getValue()
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
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023006",  
                    ownerOP.getObjectAsPrintable(), ownerMmd.getName()));
            }
            delegate.clear();
            Iterator iter = backingStore.iterator(ownerOP);
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
     * Method to update an embedded element in this list.
     * @param element The element
     * @param fieldNumber Number of field in the element
     * @param value New value for this field
     */
    public void updateEmbeddedElement(Object element, int fieldNumber, Object value)
    {
        if (backingStore != null)
        {
            backingStore.updateEmbeddedElement(ownerOP, element, fieldNumber, value);
        }
    }

    /**
     * Method to unset the owner and backing store information.
     */
    public synchronized void unsetOwner()
    {
        super.unsetOwner();
        if (backingStore != null)
        {
            backingStore = null;
        }
    }

    // ----------------- Implementation of ArrayList methods -------------------
 
    /**
     * Clone operator to return a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return The cloned object
     */
    public Object clone()
    {
        if (useCache)
        {
            loadFromStore();
        }

        return delegate.clone();
    }

    /**
     * Method to return if the list contains this element.
     * @param element The element
     * @return Whether it is contained
     **/
    public boolean contains(Object element)
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
        if (!(o instanceof java.util.List))
        {
            return false;
        }
        java.util.List l = (java.util.List)o;
        if (l.size() != size())
        {
            return false;
        }
        Object[] elements = toArray();
        Object[] otherElements = l.toArray();
        for (int i=0;i<elements.length;i++)
        {
            if (!elements[i].equals(otherElements[i]))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to retrieve an element no.
     * @param index The item to retrieve
     * @return The element at that position.
     **/
    public Object get(int index)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.get(ownerOP, index);
        }

        return delegate.get(index);
    }

    /**
     * Method to the position of an element.
     * @param element The element.
     * @return The position.
     **/
    public int indexOf(Object element)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.indexOf(ownerOP, element);
        }
 
        return delegate.indexOf(element);
    }

    /**
     * Accessor for whether the ArrayList is empty.
     * @return Whether it is empty.
     **/
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * Method to retrieve an iterator for the list.
     * @return The iterator
     **/
    public Iterator iterator()
    {
        // Populate the cache if necessary
        if (useCache)
        {
            loadFromStore();
        }

        return new SCOListIterator(this, ownerOP, delegate, backingStore, useCache, -1);
    }

    /**
     * Method to retrieve a List iterator for the list.
     * @return The iterator
     **/
    public ListIterator listIterator()
    {
        // Populate the cache if necessary
        if (useCache)
        {
            loadFromStore();
        }

        return new SCOListIterator(this, ownerOP, delegate, backingStore, useCache, -1);
    }

    /**
     * Method to retrieve a List iterator for the list from the index.
     * @param index The start point 
     * @return The iterator
     **/
    public ListIterator listIterator(int index)
    {
        // Populate the cache if necessary
        if (useCache)
        {
            loadFromStore();
        }

        return new SCOListIterator(this, ownerOP, delegate, backingStore, useCache, index);
    }

    /**
     * Method to retrieve the last position of the element.
     * @param element The element
     * @return The last position of this element in the List.
     **/
    public int lastIndexOf(Object element)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.lastIndexOf(ownerOP, element);
        }
 
        return delegate.lastIndexOf(element);
    }

    /**
     * Accessor for the size of the ArrayList.
     * @return The size.
     **/
    public int size()
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
     * Accessor for the subList of elements between from and to of the List
     * @param from Start index (inclusive)
     * @param to End index (exclusive) 
     * @return The subList
     **/
    public synchronized java.util.List subList(int from,int to)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.subList(ownerOP,from,to);
        }

        return delegate.subList(from,to);
    }

    /**
     * Method to return the list as an array.
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
     * Method to return the list as an array.
     * @param a The runtime types of the array being defined by this param
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

    // ---------------------------- Mutator methods ----------------------------

    /**
     * Method to add an element to a position in the ArrayList.
     * @param index The position
     * @param element The new element
     */
    public void add(int index, Object element)
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

        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new ListAddAtOperation(ownerOP, backingStore, index, element));
            }
            else
            {
                try
                {
                    backingStore.add(ownerOP, element, index, (useCache ? delegate.size() : -1));
                }
                catch (NucleusDataStoreException dse)
                {
                    throw new IllegalArgumentException(LOCALISER.msg("023013", "addAll", ownerMmd.getName(), dse), dse);
                }
            }
        }

        // Only make it dirty after adding the element(s) to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        delegate.add(index, element);

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to add an element to the ArrayList.
     * @param element The new element
     * @return Whether it was added ok.
     */
    public boolean add(Object element)
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
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerOP, backingStore, element));
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.add(ownerOP, element, (useCache ? delegate.size() : -1));
                }
                catch (NucleusDataStoreException dse)
                {
                    throw new IllegalArgumentException(LOCALISER.msg("023013", "add", ownerMmd.getName(), dse), dse);
                }
            }
        }

        // Only make it dirty after adding the element(s) to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        boolean delegateSuccess = delegate.add(element);

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return (backingStore != null ? backingSuccess : delegateSuccess);
    }

    /**
     * Method to add a Collection to the ArrayList.
     * @param elements The collection
     * @return Whether it was added ok.
     */
    public boolean addAll(Collection elements)
    {
        if (useCache)
        {
            loadFromStore();
        }

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
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
                    backingSuccess = backingStore.addAll(ownerOP, elements, (useCache ? delegate.size() : -1));
                }
                catch (NucleusDataStoreException dse)
                {
                    throw new IllegalArgumentException(LOCALISER.msg("023013", "addAll", ownerMmd.getName(), dse), dse);
                }
            }
        }

        // Only make it dirty after adding the element(s) to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        boolean delegateSuccess = delegate.addAll(elements);

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return (backingStore != null ? backingSuccess : delegateSuccess);
    }

    /**
     * Method to add a Collection to a position in the ArrayList.
     * @param index Position to insert the collection.
     * @param elements The collection
     * @return Whether it was added ok.
     */
    public boolean addAll(int index, Collection elements)
    {
        if (useCache)
        {
            loadFromStore();
        }

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
            {
                int pos = index;
                for (Object element : elements)
                {
                    ownerOP.getExecutionContext().addOperationToQueue(new ListAddAtOperation(ownerOP, backingStore, pos++, element));
                }
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.addAll(ownerOP, elements, index, (useCache ? delegate.size() : -1));
                }
                catch (NucleusDataStoreException dse)
                {
                    throw new IllegalArgumentException(LOCALISER.msg("023013", "addAll", ownerMmd.getName(), dse), dse);
                }
            }
        }

        // Only make it dirty after adding the element(s) to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        boolean delegateSuccess = delegate.addAll(index, elements);

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return (backingStore != null ? backingSuccess : delegateSuccess);
    }

    /**
     * Method to clear the ArrayList.
     */
    public synchronized void clear()
    {
        makeDirty();
        delegate.clear();

        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
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
     * Method to remove an element from the List
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
    public boolean remove(Object element, boolean allowCascadeDelete)
    {
        makeDirty();

        if (useCache)
        {
            loadFromStore();
        }

        int size = (useCache ? delegate.size() : -1);
        boolean contained = delegate.contains(element);
        boolean delegateSuccess = delegate.remove(element);

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
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
                    NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("023013", "remove", ownerMmd.getName(), dse));
                    backingSuccess = false;
                }
            }
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }

        return (backingStore != null ? backingSuccess : delegateSuccess);
    }

    /**
     * Method to remove an element from the ArrayList.
     * @param index The element position.
     * @return The object that was removed
     */
    public synchronized Object remove(int index)
    {
        makeDirty();
 
        if (useCache)
        {
            loadFromStore();
        }

        int size = (useCache ? delegate.size() : -1);
        Object delegateObject = (useCache ? delegate.remove(index) : null);

        Object backingObject = null;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
            {
                backingObject = delegateObject;
                ownerOP.getExecutionContext().addOperationToQueue(new ListRemoveAtOperation(ownerOP, backingStore, index));
            }
            else
            {
                try
                {
                    backingObject = backingStore.remove(ownerOP, index, size);
                }
                catch (NucleusDataStoreException dse)
                {
                    NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("023013", "remove", ownerMmd.getName(), dse));
                    backingObject = null;
                }
            }
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }

        return (backingStore != null ? backingObject : delegateObject);
    }

    /**
     * Method to remove a collection of elements from the List.
     * @param elements Collection of elements to remove
     * @return Whether it was successful.
     */
    public boolean removeAll(Collection elements)
    {
        makeDirty();

        if (useCache)
        {
            loadFromStore();
        }

        int size = (useCache ? delegate.size() : -1);
        Collection contained = null;
        if (backingStore != null && SCOUtils.useQueuedUpdate(queued, ownerOP))
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
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
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
                    NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("023013", "removeAll", ownerMmd.getName(), dse));
                    backingSuccess = false;
                }
            }

            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }

            return backingSuccess;
        }
        else
        {
            if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
            return delegateSuccess;
        }
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
     * Wrapper addition that allows turning off of the dependent-field checks
     * when doing the position setting. This means that we can prevent the deletion of
     * the object that was previously in that position. This particular feature is used
     * when attaching a list field and where some elements have changed positions.
     * @param index The position
     * @param element The new element
     * @return The element previously at that position
     */
    public Object set(int index, Object element, boolean allowDependentField)
    {
        // Reject inappropriate elements
        if (!allowNulls && element == null)
        {
            throw new NullPointerException("Nulls not allowed for collection at field " + ownerMmd.getName() + " but element is null");
        }

        makeDirty();

        if (useCache)
        {
            loadFromStore();
        }

        Object delegateReturn = delegate.set(index, element);
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(queued, ownerOP))
            {
                ownerOP.getExecutionContext().addOperationToQueue(new ListSetOperation(ownerOP, backingStore, index, element, allowDependentField));
            }
            else
            {
                backingStore.set(ownerOP, index, element, allowDependentField);
            }
        }

        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return delegateReturn;
    }

    /**
     * Method to set the element at a position in the ArrayList.
     * @param index The position
     * @param element The new element
     * @return The element previously at that position
     */
    public Object set(int index, Object element)
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
     * 
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        if (useCache)
        {
            loadFromStore();
            return new java.util.ArrayList(delegate);
        }
        else
        {
            // TODO Cater for non-cached collection, load elements in a DB call.
            return new java.util.ArrayList(delegate);
        }
    }
}