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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistableObjectType;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.flush.ListAddAtOperation;
import org.datanucleus.flush.CollectionAddOperation;
import org.datanucleus.flush.CollectionClearOperation;
import org.datanucleus.flush.ListRemoveAtOperation;
import org.datanucleus.flush.CollectionRemoveOperation;
import org.datanucleus.flush.ListSetOperation;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.BackedSCOStoreManager;
import org.datanucleus.store.types.SCOListIterator;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.scostore.ListStore;
import org.datanucleus.store.types.scostore.Store;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class Vector object.
 * This class extends Vector, using that class to contain the current objects, and the backing ListStore 
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
public class Vector<E> extends org.datanucleus.store.types.wrappers.Vector<E> implements BackedSCO
{
    protected transient ListStore<E> backingStore;
    protected transient boolean allowNulls = false;
    protected transient boolean useCache = true;
    protected transient boolean isCacheLoaded = false;

    /**
     * Constructor, using StateManager of the "owner" and the field name.
     * @param sm The owner StateManager
     * @param mmd Metadata for the member
     */
    public Vector(DNStateManager sm, AbstractMemberMetaData mmd)
    {
        super(sm, mmd);

        // Set up our delegate
        this.delegate = new java.util.Vector();

        allowNulls = SCOUtils.allowNullsInContainer(allowNulls, mmd);
        useCache = SCOUtils.useContainerCache(ownerSM, mmd);

        if (!SCOUtils.collectionHasSerialisedElements(mmd) && mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            ClassLoaderResolver clr = ownerSM.getExecutionContext().getClassLoaderResolver();
            this.backingStore = (ListStore)((BackedSCOStoreManager)ownerSM.getStoreManager()).getBackingStoreForField(clr, mmd, java.util.Vector.class);
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(SCOUtils.getContainerInfoMessage(ownerSM, ownerMmd.getName(), this,
                useCache, allowNulls, SCOUtils.useCachedLazyLoading(ownerSM, ownerMmd)));
        }
    }

    public void initialise(java.util.Vector<E> newValue, Object oldValue)
    {
        if (newValue != null)
        {
            // Check for the case of serialised PC elements, and assign StateManagers to the elements without
            if (SCOUtils.collectionHasSerialisedElements(ownerMmd) && ownerMmd.getCollection().elementIsPersistent())
            {
                ExecutionContext ec = ownerSM.getExecutionContext();
                Iterator iter = newValue.iterator();
                while (iter.hasNext())
                {
                    Object pc = iter.next();
                    DNStateManager objSM = ec.findStateManager(pc);
                    if (objSM == null)
                    {
                        objSM = ec.getNucleusContext().getStateManagerFactory().newForEmbedded(ec, pc, false, ownerSM, ownerMmd.getAbsoluteFieldNumber(),
                            PersistableObjectType.EMBEDDED_COLLECTION_ELEMENT_PC);
                    }
                }
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023008", ownerSM.getObjectAsPrintable(), ownerMmd.getName(), "" + newValue.size()));
            }

            // TODO This does clear+addAll : Improve this and work out which elements are added and which deleted
            if (backingStore != null)
            {
                if (SCOUtils.useQueuedUpdate(ownerSM))
                {
                    if (ownerSM.isFlushedToDatastore() || !ownerSM.getLifecycleState().isNew())
                    {
                        ownerSM.getExecutionContext().addOperationToQueue(new CollectionClearOperation(ownerSM, backingStore));

                        for (Object element : newValue)
                        {
                            ownerSM.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerSM, backingStore, element));
                        }
                    }
                }
                else
                {
                    backingStore.clear(ownerSM);

                    try
                    {
                        backingStore.addAll(ownerSM, newValue, useCache ? 0 : -1);
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
    public void initialise(java.util.Vector<E> c)
    {
        if (c != null)
        {
            // Check for the case of serialised PC elements, and assign StateManagers to the elements without
            if (SCOUtils.collectionHasSerialisedElements(ownerMmd) && ownerMmd.getCollection().elementIsPersistent())
            {
                ExecutionContext ec = ownerSM.getExecutionContext();
                Iterator iter = c.iterator();
                while (iter.hasNext())
                {
                    Object pc = iter.next();
                    DNStateManager objSM = ec.findStateManager(pc);
                    if (objSM == null)
                    {
                        objSM = ec.getNucleusContext().getStateManagerFactory().newForEmbedded(ec, pc, false, ownerSM, ownerMmd.getAbsoluteFieldNumber(),
                            PersistableObjectType.EMBEDDED_COLLECTION_ELEMENT_PC);
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
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023007", ownerSM.getObjectAsPrintable(), ownerMmd.getName(), "" + c.size()));
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
        if (useCache && !SCOUtils.useCachedLazyLoading(ownerSM, ownerMmd))
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
    public java.util.Vector<E> getValue()
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
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("023006", ownerSM.getObjectAsPrintable(), ownerMmd.getName()));
            }
            delegate.clear();

            ExecutionContext ec = ownerSM.getExecutionContext();
            RelationType relType = ownerMmd.getRelationType(ec.getClassLoaderResolver());
            int relatedMemberNum = -1;
            if (RelationType.isBidirectional(relType) && relType == RelationType.ONE_TO_MANY_BI)
            {
                AbstractMemberMetaData[] relMmds = ownerMmd.getRelatedMemberMetaData(ec.getClassLoaderResolver());
                relatedMemberNum = (relMmds != null && relMmds.length > 0) ? relMmds[0].getAbsoluteFieldNumber() : -1;
            }

            Iterator<E> iter = backingStore.iterator(ownerSM);
            while (iter.hasNext())
            {
                E element = iter.next();
                if (relatedMemberNum >= 0)
                {
                    DNStateManager elemSM = ec.findStateManager(element);
                    if (!elemSM.isFieldLoaded(relatedMemberNum))
                    {
                        // Store the "id" value in case the container owner member is ever accessed
                        elemSM.storeFieldValue(relatedMemberNum, ownerSM.getExternalObjectId());
                    }
                }
                delegate.add(element);
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
            backingStore.updateEmbeddedElement(ownerSM, element, fieldNumber, value);
        }
    }

    /**
     * Method to unset the owner and field information.
     */
    public void unsetOwner()
    {
        super.unsetOwner();
        if (backingStore != null)
        {
            backingStore = null;
        }
    }

    /**
     * Clone operator to return a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return The cloned object
     */
    public synchronized Object clone()
    {
        if (useCache)
        {
            loadFromStore();
        }

        return delegate.clone();
    }

    @Override
    public boolean contains(Object element)
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.contains(element);
        }
        else if (backingStore != null)
        {
            return backingStore.contains(ownerSM,element);
        }

        return delegate.contains(element);
    }

    @Override
    public synchronized boolean containsAll(java.util.Collection c)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            java.util.HashSet h = new java.util.HashSet(c);
            Iterator iter=iterator();
            while (iter.hasNext())
            {
                h.remove(iter.next());
            }

            return h.isEmpty();
        }

        return delegate.containsAll(c);
    }

    @Override
    public synchronized E elementAt(int index)
    {
        return get(index);
    }

    @Override
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

    @Override
    public Enumeration elements()
    {
        if (useCache)
        {
            loadFromStore();
            return delegate.elements();
        }

        final Iterator iter = new SCOListIterator(this, ownerSM, delegate, backingStore, useCache, -1);
        return new Enumeration() {
            public boolean hasMoreElements()
            {
                return iter.hasNext();
            }
            public Object nextElement()
            {
                return iter.next();
            }
        };
    }

    @Override
    public synchronized E firstElement()
    {
        return get(0);
    }

    @Override
    public synchronized void forEach(Consumer<? super E> action)
    {
        Objects.requireNonNull(action);
        for (E t : this)
        { // uses iterator() implicitly
            action.accept(t);
        }
    }

    @Override
    public synchronized E get(int index)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.get(ownerSM, index);
        }

        return delegate.get(index);
    }

    @Override
    public synchronized int hashCode()
    {
        if (useCache)
        {
            loadFromStore();
        }
        return delegate.hashCode();
    }

    @Override
    public int indexOf(Object element)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.indexOf(ownerSM, element);
        }
 
        return delegate.indexOf(element);
    }

    @Override
    public synchronized int indexOf(Object element, int startIndex)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            // TODO Currently ignores the "startIndex" but should use it
            return backingStore.indexOf(ownerSM, element);
        }
 
        return delegate.indexOf(element, startIndex);
    }

    @Override
    public synchronized boolean isEmpty()
    {
        return size() == 0;
    }

    @Override
    public synchronized Iterator<E> iterator()
    {
        // Populate the cache if necessary
        if (useCache)
        {
            loadFromStore();
        }

        return new SCOListIterator(this, ownerSM, delegate, backingStore, useCache, -1);
    }

    @Override
    public synchronized E lastElement()
    {
        return get(size() - 1);
    }

    @Override
    public synchronized int lastIndexOf(Object element)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.lastIndexOf(ownerSM, element);
        }
 
        return delegate.lastIndexOf(element);
    }

    @Override
    public synchronized int lastIndexOf(Object element, int startIndex)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            // TODO This doesnt start at the "startIndex"
            return backingStore.lastIndexOf(ownerSM, element);
        }
 
        return delegate.lastIndexOf(element, startIndex);
    }

    @Override
    public synchronized ListIterator<E> listIterator()
    {
        // Populate the cache if necessary
        if (useCache)
        {
            loadFromStore();
        }

        return new SCOListIterator(this, ownerSM, delegate, backingStore, useCache, -1);
    }

    @Override
    public synchronized ListIterator<E> listIterator(int index)
    {
        // Populate the cache if necessary
        if (useCache)
        {
            loadFromStore();
        }

        return new SCOListIterator(this, ownerSM, delegate, backingStore, useCache, index);
    }

    @Override
    public synchronized int size()
    {
        if (useCache && isCacheLoaded)
        {
            // If the "delegate" is already loaded, use it
            return delegate.size();
        }
        else if (backingStore != null)
        {
            return backingStore.size(ownerSM);
        }

        return delegate.size();
    }

    @Override
    public synchronized java.util.List<E> subList(int from,int to)
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return backingStore.subList(ownerSM,from,to);
        }

        return delegate.subList(from,to);
    }

    @Override
    public synchronized Object[] toArray()
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return SCOUtils.toArray(backingStore, ownerSM);
        }  
        return delegate.toArray();
    }

    @Override
    public synchronized <T> T[] toArray(T a[])
    {
        if (useCache)
        {
            loadFromStore();
        }
        else if (backingStore != null)
        {
            return SCOUtils.toArray(backingStore, ownerSM, a);
        }  
        return delegate.toArray(a);
    }

    @Override
    public void add(int index, E element)
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
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new ListAddAtOperation(ownerSM, backingStore, index, element));
            }
            else
            {
                try
                {
                    backingStore.add(ownerSM, element, index, useCache ? delegate.size() : -1);
                }
                catch (NucleusDataStoreException dse)
                {
                    throw new IllegalArgumentException(Localiser.msg("023013", "add", ownerMmd.getName(), dse), dse);
                }
            }
        }

        // Only make it dirty after adding the element(s) to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        delegate.add(index, element);

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
    }

    @Override
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
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerSM, backingStore, element));
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.add(ownerSM,element, useCache ? delegate.size() : -1);
                }
                catch (NucleusDataStoreException dse)
                {
                    throw new IllegalArgumentException(Localiser.msg("023013", "add", ownerMmd.getName(), dse), dse);
                }
            }
        }

        // Only make it dirty after adding the element(s) to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        boolean delegateSuccess = delegate.add(element);

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return backingStore != null ? backingSuccess : delegateSuccess;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> elements)
    {
        if (useCache)
        {
            loadFromStore();
        }

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                for (Object element : elements)
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionAddOperation(ownerSM, backingStore, element));
                }
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.addAll(ownerSM, elements, useCache ? delegate.size() : -1);
                }
                catch (NucleusDataStoreException dse)
                {
                    throw new IllegalArgumentException(Localiser.msg("023013", "addAll", ownerMmd.getName(), dse), dse);
                }
            }
        }

        // Only make it dirty after adding the element(s) to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        boolean delegateSuccess = delegate.addAll(elements);

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return backingStore != null ? backingSuccess : delegateSuccess;
    }

    @Override
    public synchronized boolean addAll(int index, Collection elements)
    {
        if (useCache)
        {
            loadFromStore();
        }

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                int pos = index;
                for (Object element : elements)
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new ListAddAtOperation(ownerSM, backingStore, pos++, element));
                }
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.addAll(ownerSM, elements, index, useCache ? delegate.size() : -1);
                }
                catch (NucleusDataStoreException dse)
                {
                    throw new IllegalArgumentException(Localiser.msg("023013", "addAll", ownerMmd.getName(), dse), dse);
                }
            }
        }

        // Only make it dirty after adding the element(s) to the datastore so we give it time
        // to be inserted - otherwise jdoPreStore on this object would have been called before completing the addition
        makeDirty();

        boolean delegateSuccess = delegate.addAll(index, elements);

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return backingStore != null ? backingSuccess : delegateSuccess;
    }

    @Override
    public synchronized void addElement(E element)
    {
        // This is a historical wrapper to the Collection method
        add(element);
    }

    @Override
    public synchronized void clear()
    {
        makeDirty();
        delegate.clear();

        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new CollectionClearOperation(ownerSM, backingStore));
            }
            else
            {
                backingStore.clear(ownerSM);
            }
        }

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
    }

    @Override
    public synchronized boolean remove(Object element)
    {
        return remove(element, true);
    }

    @Override
    public synchronized boolean remove(Object element, boolean allowCascadeDelete)
    {
        makeDirty();

        if (useCache)
        {
            loadFromStore();
        }

        int size = useCache ? delegate.size() : -1;
        boolean contained = delegate.contains(element);
        int indexOfElement = -1;
        if (useCache)
        {
            if (contained)
            {
                indexOfElement = delegate.indexOf(element);
            }
            else
            {
                // Element not present in the delegate so nothing to do
                return false;
            }
        }
        boolean delegateSuccess = delegate.remove(element);

        boolean backingSuccess = true;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                backingSuccess = contained;
                if (backingSuccess)
                {
                    ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, backingStore, element, allowCascadeDelete));
                }
            }
            else
            {
                if (indexOfElement >= 0)
                {
                    // We know the index of the first instance so use that
                    Object removedElement = backingStore.remove(ownerSM, indexOfElement, size);
                    if (removedElement != null)
                    {
                        backingSuccess = true;
                    }
                }
                else
                {
                    try
                    {
                        backingSuccess = backingStore.remove(ownerSM, element, size, allowCascadeDelete);
                    }
                    catch (NucleusDataStoreException dse)
                    {
                        NucleusLogger.PERSISTENCE.warn(Localiser.msg("023013", "remove", ownerMmd.getName(), dse));
                        backingSuccess = false;
                    }
                }
            }
        }

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }

        return backingStore != null ? backingSuccess : delegateSuccess;
    }

    @Override
    public synchronized E remove(int index)
    {
        makeDirty();
 
        if (useCache)
        {
            loadFromStore();
        }

        int size = useCache ? delegate.size() : -1;
        if (useCache && (index < 0 || index>= size))
        {
            throw new IndexOutOfBoundsException(index);
        }
        E delegateObject = useCache ? delegate.remove(index) : null;
        E backingObject = null;
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                backingObject = delegateObject;
                ownerSM.getExecutionContext().addOperationToQueue(new ListRemoveAtOperation(ownerSM, backingStore, index));
            }
            else
            {
                try
                {
                    backingObject = backingStore.remove(ownerSM, index, size);
                }
                catch (NucleusDataStoreException dse)
                {
                    NucleusLogger.PERSISTENCE.warn(Localiser.msg("023013", "remove", ownerMmd.getName(), dse));
                    backingObject = null;
                }
            }
        }

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }

        return backingStore != null ? backingObject : delegateObject;
    }

    @Override
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

        makeDirty();

        if (useCache)
        {
            loadFromStore();
        }

        int[] elementIndexes = useCache ? ClassUtils.getIndexesOfCollectionInList(delegate, elements) : null;
        int size = useCache ? delegate.size() : -1;

        if (backingStore != null && ownerSM != null)
        {
            boolean backingSuccess = true;
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                // Check which are contained before updating the delegate
                Collection contained = new java.util.HashSet();
                for (Object elem : elements)
                {
                    if (contains(elem))
                    {
                        contained.add(elem);
                    }
                }
                if (!contained.isEmpty())
                {
                    backingSuccess = false;
                    for (Object element : contained)
                    {
                        backingSuccess = true;
                        ownerSM.getExecutionContext().addOperationToQueue(new CollectionRemoveOperation(ownerSM, backingStore, element, true));
                    }
                }
            }
            else
            {
                try
                {
                    backingSuccess = backingStore.removeAll(ownerSM, elements, size, elementIndexes);
                }
                catch (NucleusDataStoreException dse)
                {
                    NucleusLogger.PERSISTENCE.warn(Localiser.msg("023013", "removeAll", ownerMmd.getName(), dse));
                    backingSuccess = false;
                }
            }

            delegate.removeAll(elements);

            if (!ownerSM.getExecutionContext().getTransaction().isActive())
            {
                ownerSM.getExecutionContext().processNontransactionalUpdate();
            }

            return backingSuccess;
        }

        boolean delegateSuccess = delegate.removeAll(elements);

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return delegateSuccess;
    }

    @Override
    public synchronized boolean removeElement(Object element)
    {
        // This is a historical wrapper to the Collection method
        return remove(element);
    }

    @Override
    public synchronized void removeElementAt(int index)
    {
        // This is a historical wrapper to the Collection method
        remove(index);
    }

    @Override
    public synchronized void removeAllElements()
    {
        clear();
    }

    @Override
    public synchronized boolean retainAll(java.util.Collection c)
    {
        makeDirty();

        if (useCache)
        {
            loadFromStore();
        }
        
        boolean modified = false;
        Iterator iter=iterator();
        while (iter.hasNext())
        {
            Object element = iter.next();
            if (!c.contains(element))
            {
                iter.remove();
                modified = true;
            }
        }

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return modified;
    }

    @Override
    public E set(int index, E element, boolean allowDependentField)
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

        E delegateReturn = delegate.set(index, element);
        if (backingStore != null)
        {
            if (SCOUtils.useQueuedUpdate(ownerSM))
            {
                ownerSM.getExecutionContext().addOperationToQueue(new ListSetOperation(ownerSM, backingStore, index, element, allowDependentField));
            }
            else
            {
                backingStore.set(ownerSM, index, element, allowDependentField);
            }
        }

        if (ownerSM != null && !ownerSM.getExecutionContext().getTransaction().isActive())
        {
            ownerSM.getExecutionContext().processNontransactionalUpdate();
        }
        return delegateReturn;
    }

    @Override
    public synchronized E set(int index, E element)
    {
        return set(index, element, !sorting);
    }

    @Override
    public synchronized void setElementAt(E element, int index)
    {
        // This is a historical wrapper to the Collection method
        set(index,element);
    }

    @Override
    protected Object writeReplace() throws ObjectStreamException
    {
        if (useCache)
        {
            loadFromStore();
            return new java.util.Vector(delegate);
        }

        // TODO Cater for non-cached collection, load elements in a DB call.
        return new java.util.Vector(delegate);
    }

    @Override
    public Spliterator<E> spliterator()
    {
        if (backingStore != null && useCache && !isCacheLoaded)
        {
            loadFromStore();
        }
        // TODO If using backing store yet not caching, then this will fail
        return delegate.spliterator();
    }

    @Override
    public Stream<E> stream()
    {
        if (backingStore != null && useCache && !isCacheLoaded)
        {
            loadFromStore();
        }
        // TODO If using backing store yet not caching, then this will fail
        return delegate.stream();
    }

    @Override
    public Stream<E> parallelStream()
    {
        if (backingStore != null && useCache && !isCacheLoaded)
        {
            loadFromStore();
        }
        // TODO If using backing store yet not caching, then this will fail
        return delegate.parallelStream();
    }
}