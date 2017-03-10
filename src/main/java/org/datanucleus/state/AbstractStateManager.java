/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.state;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchPlanForClass;
import org.datanucleus.FetchPlanState;
import org.datanucleus.PropertyNames;
import org.datanucleus.cache.CachedPC;
import org.datanucleus.cache.L2CachePopulateFieldManager;
import org.datanucleus.cache.L2CacheRetrieveFieldManager;
import org.datanucleus.cache.Level2Cache;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.IdentityReference;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.store.fieldmanager.LoadFieldManager;
import org.datanucleus.store.fieldmanager.SingleTypeFieldManager;
import org.datanucleus.store.fieldmanager.AbstractFetchDepthFieldManager.EndOfFetchPlanGraphException;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of an implementation of a bytecode enhancement-based StateManager.
 * This class should have no reference to any JDO class, so it can be reused by a JPAStateManager too (in the future).
 */
public abstract class AbstractStateManager<T> implements ObjectProvider<T>
{
    protected static final SingleTypeFieldManager HOLLOWFIELDMANAGER = new SingleTypeFieldManager();

    /** Flag to signify that we are currently storing the persistable object, so we don't detach it on serialisation. */
    protected static final int FLAG_STORING_PC = 2<<15;
    /** Whether the managed object needs the inheritance level validating before loading fields. */
    protected static final int FLAG_NEED_INHERITANCE_VALIDATION = 2<<14;
    protected static final int FLAG_POSTINSERT_UPDATE = 2<<13;
    protected static final int FLAG_LOADINGFPFIELDS = 2<<12;
    protected static final int FLAG_POSTLOAD_PENDING = 2<<11;
    protected static final int FLAG_CHANGING_STATE = 2<<10;
    /** if the persistable object is new and was flushed to the datastore. */
    protected static final int FLAG_FLUSHED_NEW = 2<<9;
    protected static final int FLAG_BECOMING_DELETED = 2<<8;
    /** Flag whether this SM is updating the ownership of its embedded/serialised field(s). */
    protected static final int FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER = 2<<7;
    /** Flag for {@link #flags} whether we are retrieving detached state from the detached object. */
    protected static final int FLAG_RETRIEVING_DETACHED_STATE = 2<<6;
    /** Flag for {@link #flags} whether we are resetting the detached state. */
    protected static final int FLAG_RESETTING_DETACHED_STATE = 2<<5;
    /** Flag for {@link #flags} whether we are in the process of attaching the object. */
    protected static final int FLAG_ATTACHING = 2<<4;
    /** Flag for {@link #flags} whether we are in the process of detaching the object. */
    protected static final int FLAG_DETACHING = 2<<3;
    /** Flag for {@link #flags} whether we are in the process of making transient the object. */
    protected static final int FLAG_MAKING_TRANSIENT = 2<<2;
    /** Flag for {@link #flags} whether we are in the process of flushing changes to the object. */
    protected static final int FLAG_FLUSHING = 2<<1;
    /** Flag for {@link #flags} whether we are in the process of disconnecting the object. */
    protected static final int FLAG_DISCONNECTING = 2<<0;

    /** The persistable instance managed by this ObjectProvider. */
    protected T myPC;

    /** Bit-packed flags for operational settings (packed into "int" for memory benefit). */
    protected int flags;

    /** Whether to restore values at StateManager. If true, overwrites the restore values at tx level. */
    protected boolean restoreValues = false;

    /** The ExecutionContext for this StateManager */
    protected ExecutionContext myEC;

    /** the metadata for the class. */
    protected AbstractClassMetaData cmd;

    /** The object identity in the JVM. Will be "myID" (if set) or otherwise a temporary id based on this StateManager. */
    protected Object myInternalID;

    /** The object identity in the datastore */
    protected Object myID;

    /** The actual LifeCycleState for the persistable instance */
    protected LifeCycleState myLC;

    /** version field for optimistic transactions */
    protected Object myVersion;

    /** version field for optimistic transactions, after a insert/update but not yet committed. */
    protected Object transactionalVersion;

    /** Flags for state stored with the object. Maps onto JDO PersistenceCapable flags. */
    protected byte persistenceFlags;

    /** Fetch plan for the class of the managed object. */
    protected FetchPlanForClass myFP;

    /**
     * Indicator for whether the persistable instance is dirty.
     * Note that "dirty" in this case is not equated to being in the P_DIRTY state.
     * The P_DIRTY state means that at least one field in the object has been written by the user during 
     * the current transaction, whereas for this parameter, a field is "dirty" if it's been written by the 
     * user but not yet updated in the data store.  The difference is, it's possible for an object's state
     * to be P_DIRTY, yet have no "dirty" fields because flush() has been called at least once during the transaction.
     */
    protected boolean dirty = false;

    /** indicators for which fields are currently dirty in the persistable instance. */
    protected boolean[] dirtyFields;

    /** indicators for which fields are currently loaded in the persistable instance. */
    protected boolean[] loadedFields;

    /** Lock object to synchronise execution when reading/writing fields. */
    protected Lock lock = null;

    /** The current lock mode for this object. */
    protected short lockMode = 0;

    /** Flags of the persistable instance when the instance is enlisted in the transaction. */
    protected byte savedFlags;

    /** Loaded fields of the persistable instance when the instance is enlisted in the transaction. */
    protected boolean[] savedLoadedFields = null;

    /** state for transitions of activities. */
    protected ActivityState activity;

    /** Current FieldManager. */
    protected FieldManager currFM = null;

    /** The type of the managed object (0 = PC, 1 = embedded PC, 2 = embedded element, 3 = embedded key, 4 = embedded value. */
    protected short objectType = 0;

    /**
     * Constructor for object of specified type managed by the provided ExecutionContext.
     * @param ec ExecutionContext
     * @param cmd the metadata for the class.
     */
    public AbstractStateManager(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        connect(ec, cmd);
    }

    /**
     * Method to (re)connect the provider to the specified ExecutionContext and object type.
     */
    public void connect(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        int fieldCount = cmd.getMemberCount();
        this.cmd = cmd;
        this.dirtyFields = new boolean[fieldCount];
        this.loadedFields = new boolean[fieldCount];
        this.dirty = false;
        this.myEC = ec;
        this.myFP = myEC.getFetchPlan().getFetchPlanForClass(cmd);
        this.lock = new ReentrantLock();
        this.lockMode = LockManager.LOCK_MODE_NONE;
        this.savedFlags = 0;
        this.savedLoadedFields = null;
        this.objectType = 0;
        this.activity = ActivityState.NONE;
        this.myVersion = null;
        this.transactionalVersion = null;
        this.persistenceFlags = 0;
    }

    /**
     * Accessor for the ClassMetaData for this object.
     * @return The ClassMetaData.
     */
    public AbstractClassMetaData getClassMetaData()
    {
        return cmd;
    }

    /**
     * Accessor for the ExecutionContext for this object.
     * @return Execution Context
     */
    public ExecutionContext getExecutionContext()
    {
        return myEC;
    }

    public StoreManager getStoreManager()
    {
        return myEC.getNucleusContext().isFederated() ? ((FederatedStoreManager)myEC.getStoreManager()).getStoreManagerForClass(cmd) : myEC.getStoreManager();
    }

    /**
     * Accessor for the LifeCycleState
     * @return the LifeCycleState
     */
    public LifeCycleState getLifecycleState()
    {
        return myLC;
    }

    /**
     * returns the handler for callback events.
     * @return the handler for callback events.
     */
    protected CallbackHandler getCallbackHandler()
    {
        return myEC.getCallbackHandler();
    }

    /**
     * Accessor for the persistable object being managed.
     * @return The persistable object
     */
    public abstract T getObject();

    public String getObjectAsPrintable()
    {
        return StringUtils.toJVMIDString(getObject());
    }

    public String toString()
    {
        return "StateManager[pc=" + StringUtils.toJVMIDString(getObject()) + ", lifecycle=" + myLC + "]";
    }

    /**
     * Accessor for the internal object id of the object we are managing.
     * This will return the "id" if it has been set, otherwise a temporary id based on this StateManager.
     * @return The internal object id
     */
    public Object getInternalObjectId()
    {
        if (myID != null)
        {
            return myID;
        }
        else if (myInternalID == null)
        {
            // Assign a temporary internal "id" based on the object itself until our real identity is assigned
            myInternalID = new IdentityReference(this);
            return myInternalID;
        }
        else
        {
            return myInternalID;
        }
    }

    /**
     * Tests whether this object is being inserted.
     * @return true if this instance is inserting.
     */
    public boolean isInserting()
    {
        return activity == ActivityState.INSERTING;
    }

    /**
     * Accessor for whether the instance is newly persistent yet hasnt yet been flushed to the datastore.
     * @return Whether not yet flushed to the datastore
     */
    public boolean isWaitingToBeFlushedToDatastore()
    {
        // Return true if object is new and not yet flushed to datastore
        return myLC.stateType() == LifeCycleState.P_NEW && !isFlushedNew();
    }

    /**
     * Accessor for whether we are in the process of restoring the values.
     * @return Whether we are restoring values
     */
    public boolean isRestoreValues()
    {
        return restoreValues;
    }

    public void setStoringPC()
    {
        flags |= FLAG_STORING_PC;
    }

    public void unsetStoringPC()
    {
        flags &= ~FLAG_STORING_PC;
    }

    protected boolean isStoringPC()
    {
        return (flags&FLAG_STORING_PC) != 0;
    }

    void setPostLoadPending(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_POSTLOAD_PENDING;
        }
        else
        {
            flags &= ~FLAG_POSTLOAD_PENDING;
        }
    }

    protected boolean isPostLoadPending()
    {
        return (flags&FLAG_POSTLOAD_PENDING) != 0;
    }

    protected boolean isChangingState()
    {
        return (flags&FLAG_CHANGING_STATE) != 0;
    }

    void setResettingDetachedState(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_RESETTING_DETACHED_STATE;
        }
        else
        {
            flags &= ~FLAG_RESETTING_DETACHED_STATE;
        }
    }

    protected boolean isResettingDetachedState()
    {
        return (flags&FLAG_RESETTING_DETACHED_STATE) != 0;
    }

    void setRetrievingDetachedState(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_RETRIEVING_DETACHED_STATE;
        }
        else
        {
            flags &= ~FLAG_RETRIEVING_DETACHED_STATE;
        }
    }

    protected boolean isRetrievingDetachedState()
    {
        return (flags&FLAG_RETRIEVING_DETACHED_STATE) != 0;
    }

    void setDisconnecting(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_DISCONNECTING;
        }
        else
        {
            flags &= ~FLAG_DISCONNECTING;
        }
    }

    protected boolean isDisconnecting()
    {
        return (flags&FLAG_DISCONNECTING) != 0;
    }

    void setMakingTransient(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_MAKING_TRANSIENT;
        }
        else
        {
            flags &= ~FLAG_MAKING_TRANSIENT;
        }
    }

    protected boolean isMakingTransient()
    {
        return (flags&FLAG_MAKING_TRANSIENT) != 0;
    }

    public boolean isDeleting()
    {
        return activity == ActivityState.DELETING;
    }

    void setBecomingDeleted(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_BECOMING_DELETED;
        }
        else
        {
            flags &= ~FLAG_BECOMING_DELETED;
        }
    }

    public boolean becomingDeleted()
    {
        return (flags&FLAG_BECOMING_DELETED)>0;
    }

    public void markForInheritanceValidation()
    {
        flags |= FLAG_NEED_INHERITANCE_VALIDATION;
    }

    void setDetaching(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_DETACHING;
        }
        else
        {
            flags &= ~FLAG_DETACHING;
        }
    }

    public boolean isDetaching()
    {
        return (flags&FLAG_DETACHING) != 0;
    }

    void setAttaching(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_ATTACHING;
        }
        else
        {
            flags &= ~FLAG_ATTACHING;
        }
    }

    public boolean isAttaching()
    {
        return (flags&FLAG_ATTACHING) != 0;
    }

    /**
     * Sets the value for the version column in a transaction not yet committed
     * @param version The version
     */
    public void setTransactionalVersion(Object version)
    {
        this.transactionalVersion = version;
    }

    /**
     * Return the object representing the transactional version of the calling instance.
     * @param pc the calling persistable instance
     * @return the object representing the version of the calling instance
     */    
    public Object getTransactionalVersion(Object pc)
    {
        return this.transactionalVersion;
    }

    /**
     * Sets the value for the version column in the datastore
     * @param version The version
     */
    public void setVersion(Object version)
    {
        this.myVersion = version;
        this.transactionalVersion = version;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.ObjectProvider#setFlushedNew(boolean)
     */
    public void setFlushedNew(boolean flag)
    {
        if (flag)
        {
            flags |= FLAG_FLUSHED_NEW;
        }
        else
        {
            flags &= ~FLAG_FLUSHED_NEW;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.ObjectProvider#isFlushedNew()
     */
    public boolean isFlushedNew()
    {
        return (flags&FLAG_FLUSHED_NEW)!=0;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.ObjectProvider#isFlushedToDatastore()
     */
    public boolean isFlushedToDatastore()
    {
        return !dirty;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.ObjectProvider#setFlushing(boolean)
     */
    public void setFlushing(boolean flushing)
    {
        if (flushing)
        {
            flags |= FLAG_FLUSHING;
        }
        else
        {
            flags &= ~FLAG_FLUSHING;
        }
    }

    protected boolean isFlushing()
    {
        return (flags&FLAG_FLUSHING)!=0;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.ObjectProvider#markAsFlushed()
     */
    public void markAsFlushed()
    {
        clearDirtyFlags();
    }

    /**
     * Method called before a change in state.
     */
    protected void preStateChange()
    {
        flags |= FLAG_CHANGING_STATE;
    }

    /**
     * Method called after a change in state.
     */
    protected abstract void postStateChange();

    /**
     * Method to refresh the object.
     */
    public void refresh()
    {
        preStateChange();
        try
        {
            myLC = myLC.transitionRefresh(this);
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * Method to retrieve the object.
     * @param fgOnly Only load the current fetch group fields
     */
    public void retrieve(boolean fgOnly)
    {
        preStateChange();
        try
        {
            myLC = myLC.transitionRetrieve(this, fgOnly);
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * Makes Transactional Transient instances persistent.
     */
    public void makePersistentTransactionalTransient()
    {
        preStateChange();
        try
        {
            if (myLC.isTransactional && !myLC.isPersistent)
            {
                // make the transient instance persistent in the datastore, if is transactional and !persistent 
                makePersistent();
                myLC = myLC.transitionMakePersistent(this);
            }
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * Method to change the object state to nontransactional.
     */
    public void makeNontransactional()
    {
        preStateChange();
        try
        {
            myLC = myLC.transitionMakeNontransactional(this);
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * Method to change the object state to read-field.
     * @param isLoaded if the field was previously loaded
     */
    protected void transitionReadField(boolean isLoaded)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.getLock().lock();
                lock.lock();
            }

            if (myLC == null)
            {
                return;
            }
            preStateChange();
            try
            {
                myLC = myLC.transitionReadField(this, isLoaded);
            }
            finally
            {
                postStateChange();
            }
        }
        finally
        {
            if (myEC.getMultithreaded())
            {
                lock.unlock();
                myEC.getLock().unlock();
            }
        }
    }

    /**
     * Method to change the object state to write-field.
     */
    protected void transitionWriteField()
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.getLock().lock();
                lock.lock();
            }

            preStateChange();
            try
            {
                myLC = myLC.transitionWriteField(this);
            }
            finally
            {
                postStateChange();
            }
        }
        finally
        {
            if (myEC.getMultithreaded())
            {
                lock.unlock();
                myEC.getLock().unlock();
            }
        }
    }

    /**
     * Method to change the object state to evicted.
     */
    public void evict()
    {
        if (myLC != myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_CLEAN) &&
            myLC != myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_NONTRANS))
        {
            return;
        }

        preStateChange();
        try
        {
            try
            {
                getCallbackHandler().preClear(getObject());

                getCallbackHandler().postClear(getObject());
            }
            finally
            {
                myLC = myLC.transitionEvict(this);
            }
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * Method invoked just before a transaction starts for the ExecutionContext managing us.
     * @param tx The transaction
     */
    public void preBegin(org.datanucleus.Transaction tx)
    {
        preStateChange();
        try
        {
            myLC = myLC.transitionBegin(this, tx);
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * This method is invoked just after a commit is performed in a Transaction
     * involving the persistable object managed by this StateManager
     * @param tx The transaction
     */
    public void postCommit(org.datanucleus.Transaction tx)
    {
        preStateChange();
        try
        {
            myLC = myLC.transitionCommit(this, tx);
            if (transactionalVersion != myVersion)
            {
                myVersion = transactionalVersion;
            }
            this.lockMode = LockManager.LOCK_MODE_NONE;
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * This method is invoked just before a rollback is performed in a Transaction
     * involving the persistable object managed by this StateManager.
     * @param tx The transaction
     */
    public void preRollback(org.datanucleus.Transaction tx)
    {
        preStateChange();
        try
        {
            myEC.clearDirty(this);
            myLC = myLC.transitionRollback(this, tx);
            if (transactionalVersion != myVersion)
            {
                transactionalVersion = myVersion;
            }
            this.lockMode = LockManager.LOCK_MODE_NONE;
        }
        finally
        {
            postStateChange();
        }
    }

    /** Copy of the "loadedFields" just before delete was started to avoid reload during delete */
    boolean[] preDeleteLoadedFields = null;

    /**
     * Method to delete the object from the datastore.
     */
    protected void internalDeletePersistent()
    {
        if (isDeleting())
        {
            throw new NucleusUserException(Localiser.msg("026008"));
        }

        activity = ActivityState.DELETING;
        try
        {
            if (dirty)
            {
                clearDirtyFlags();

                // Clear the PM's knowledge of our being dirty. This calls flush() which does nothing
                myEC.flushInternal(false);
            }

            myEC.getNucleusContext();
            if (!isEmbedded())
            {
                // Nothing to delete if embedded
                getStoreManager().getPersistenceHandler().deleteObject(this);
            }

            preDeleteLoadedFields = null;
        }
        finally
        {
            activity = ActivityState.NONE;
        }
    }

    /**
     * Locate the object in the datastore.
     * @throws NucleusObjectNotFoundException if the object doesnt exist.
     */
    public void locate()
    {
        // Validate the object existence
        getStoreManager().getPersistenceHandler().locateObject(this);
    }

    /**
     * Accessor for the referenced PC object when we are attaching or detaching.
     * When attaching and this is the detached object this returns the newly attached object.
     * When attaching and this is the newly attached object this returns the detached object.
     * When detaching and this is the newly detached object this returns the attached object.
     * When detaching and this is the attached object this returns the newly detached object.
     * @return The referenced object (or null).
     */
    public T getReferencedPC()
    {
        return (T) myEC.getAttachDetachReferencedObject(this);
    }

    /**
     * Called from the StoreManager after StoreManager.update() is called to obtain updated values 
     * from the persistable associated with this StateManager.
     * @param fieldNumbers An array of field numbers to be updated by the Store
     * @param fm The updated values are stored in this object. This object is only valid
     *   for the duration of this call.
     */
    public abstract void provideFields(int fieldNumbers[], FieldManager fm);

    /**
     * Called from the StoreManager to refresh data in the persistable object associated with this StateManager.
     * @param fieldNumbers An array of field numbers to be refreshed by the Store
     * @param fm The updated values are stored in this object. This object is only valid
     *   for the duration of this call.
     */
    public abstract void replaceFields(int fieldNumbers[], FieldManager fm);

    /**
     * Accessor for whether all of the specified field numbers are loaded.
     * @param fieldNumbers The field numbers to check
     * @return Whether the specified fields are all loaded.
     */
    protected boolean areFieldsLoaded(int[] fieldNumbers)
    {
        if (fieldNumbers == null)
        {
            return true;
        }
        for (int i=0; i<fieldNumbers.length; ++i)
        {
            if (!loadedFields[fieldNumbers[i]])
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Method that will unload all fields that are not in the FetchPlan.
     * This is typically for use when the instance is being refreshed.
     */
    public void unloadNonFetchPlanFields()
    {
        int[] fpFieldNumbers = myFP.getMemberNumbers();
        int[] nonfpFieldNumbers = null;
        if (fpFieldNumbers == null || fpFieldNumbers.length == 0)
        {
            nonfpFieldNumbers = cmd.getAllMemberPositions();
        }
        else
        {
            int fieldCount = cmd.getMemberCount();
            if (fieldCount == fpFieldNumbers.length)
            {
                // No fields that arent in FetchPlan
                return;
            }

            nonfpFieldNumbers = new int[fieldCount - fpFieldNumbers.length];
            int currentFPFieldIndex = 0;
            int j = 0;
            for (int i=0;i<fieldCount; i++)
            {
                if (currentFPFieldIndex >= fpFieldNumbers.length)
                {
                    // Past end of FetchPlan fields
                    nonfpFieldNumbers[j++] = i;
                }
                else
                {
                    if (fpFieldNumbers[currentFPFieldIndex] == i)
                    {
                        // FetchPlan field so move to next
                        currentFPFieldIndex++;
                    }
                    else
                    {
                        nonfpFieldNumbers[j++] = i;
                    }
                }
            }
        }

        // Mark all non-FetchPlan fields as unloaded
        for (int i=0;i<nonfpFieldNumbers.length;i++)
        {
            loadedFields[nonfpFieldNumbers[i]] = false;
        }
    }

    /**
     * Convenience method to mark PK fields as loaded (if using app id).
     */
    protected void markPKFieldsAsLoaded()
    {
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            int[] pkPositions = cmd.getPKMemberPositions();
            for (int i=0;i<pkPositions.length;i++)
            {
                loadedFields[pkPositions[i]] = true;
            }
        }
    }

    /**
     * Convenience method to update a Level2 cached version of this object if cacheable
     * and has not been modified during this transaction.
     * @param fieldNumbers Numbers of fields to update in L2 cached object
     */
    protected void updateLevel2CacheForFields(int[] fieldNumbers)
    {
        String updateMode = (String)myEC.getProperty(PropertyNames.PROPERTY_CACHE_L2_UPDATE_MODE);
        if (updateMode != null && updateMode.equalsIgnoreCase("commit-only"))
        {
            return;
        }
        if (fieldNumbers == null || fieldNumbers.length == 0)
        {
            return;
        }

        Level2Cache l2cache = myEC.getNucleusContext().getLevel2Cache();
        if (l2cache != null && myEC.getNucleusContext().isClassCacheable(cmd) && !myEC.isObjectModifiedInTransaction(myID))
        {
            CachedPC<T> cachedPC = l2cache.get(myID);
            if (cachedPC != null)
            {
                // This originally just updated the L2 cache for fields where the L2 cache didn't have a value for that field, like this
                /*
                int[] cacheFieldsToLoad = ClassUtils.getFlagsSetTo(cachedPC.getLoadedFields(), fieldNumbers, false);
                if (cacheFieldsToLoad == null || cacheFieldsToLoad.length == 0)
                {
                    return;
                }
                */
                int[] cacheFieldsToLoad = fieldNumbers;
                CachedPC copyCachedPC = cachedPC.getCopy();
                if (NucleusLogger.CACHE.isDebugEnabled())
                {
                    NucleusLogger.CACHE.debug(Localiser.msg("026033", StringUtils.toJVMIDString(getObject()), myID, StringUtils.intArrayToString(cacheFieldsToLoad)));
                }

                provideFields(cacheFieldsToLoad, new L2CachePopulateFieldManager(this, copyCachedPC));

                // Replace the current L2 cached object with this one
                myEC.getNucleusContext().getLevel2Cache().put(getInternalObjectId(), copyCachedPC);
            }
        }
    }

    /**
     * Convenience method to retrieve field values from an L2 cached object if they are loaded in that object.
     * If the object is not in the L2 cache then just returns, and similarly if the required fields aren't available.
     * @param fieldNumbers Numbers of fields to load from the L2 cache
     * @return The fields that couldn't be loaded
     */
    protected int[] loadFieldsFromLevel2Cache(int[] fieldNumbers)
    {
        // Only continue if there are fields, and not being deleted/flushed etc
        if (fieldNumbers == null || fieldNumbers.length == 0 || myEC.isFlushing() || myLC.isDeleted() || isDeleting() ||
            getExecutionContext().getTransaction().isCommitting())
        {
            return fieldNumbers;
        }
        // TODO Drop this check when we're confident that this doesn't affect some use-cases
        if (!myEC.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_LOADFIELDS, true))
        {
            return fieldNumbers;
        }

        Level2Cache l2cache = myEC.getNucleusContext().getLevel2Cache();
        if (l2cache != null && myEC.getNucleusContext().isClassCacheable(cmd))
        {
            CachedPC<T> cachedPC = l2cache.get(myID);
            if (cachedPC != null)
            {
                int[] cacheFieldsToLoad = ClassUtils.getFlagsSetTo(cachedPC.getLoadedFields(), fieldNumbers, true);
                if (cacheFieldsToLoad != null && cacheFieldsToLoad.length > 0)
                {
                    if (NucleusLogger.CACHE.isDebugEnabled())
                    {
                        NucleusLogger.CACHE.debug(Localiser.msg("026034", StringUtils.toJVMIDString(getObject()), myID,
                            StringUtils.intArrayToString(cacheFieldsToLoad)));
                    }

                    L2CacheRetrieveFieldManager l2RetFM = new L2CacheRetrieveFieldManager(this, cachedPC);
                    this.replaceFields(cacheFieldsToLoad, l2RetFM);
                    int[] fieldsNotLoaded = l2RetFM.getFieldsNotLoaded();
                    if (fieldsNotLoaded != null)
                    {
                        for (int i=0;i<fieldsNotLoaded.length;i++)
                        {
                            loadedFields[fieldsNotLoaded[i]] = false;
                        }
                    }
                }
            }
        }

        return ClassUtils.getFlagsSetTo(loadedFields, fieldNumbers, false);
    }

    /**
     * Method to load all unloaded fields in the FetchPlan.
     * Recurses through the FetchPlan objects and loads fields of sub-objects where needed.
     * Used as a precursor to detaching objects at commit since fields can't be loaded during
     * the postCommit phase when the detach actually happens.
     * @param state The FetchPlan state
     */
    public void loadFieldsInFetchPlan(FetchPlanState state)
    {
        if ((flags&FLAG_LOADINGFPFIELDS)!=0)
        {
            // Already in the process of loading fields in this class so skip
            return;
        }

        flags |= FLAG_LOADINGFPFIELDS;
        try
        {
            // Load unloaded FetchPlan fields of this object
            loadUnloadedFieldsInFetchPlan();

            // Recurse through all fields and do the same
            int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, cmd.getAllMemberPositions(), true);
            if (fieldNumbers != null && fieldNumbers.length > 0)
            {
                // TODO Fix this to just access the fields of the FieldManager yet this actually does a replaceField
                replaceFields(fieldNumbers, new LoadFieldManager(this, cmd.getSCOMutableMemberFlags(), myFP, state));
                updateLevel2CacheForFields(fieldNumbers);
            }
        }
        finally
        {
            flags &= ~FLAG_LOADINGFPFIELDS;
        }
    }

    /**
     * Convenience method to load a field from the datastore.
     * Used in attaching fields and checking their old values (so we don't want any postLoad method being called).
     * TODO Merge this with one of the loadXXXFields methods.
     * @param fieldNumber The field number. If fieldNumber is -1 then this means call loadFieldsFromDatastore(null);
     */
    public void loadFieldFromDatastore(int fieldNumber)
    {
        loadFieldsFromDatastore((fieldNumber >= 0) ? (new int[] {fieldNumber}) : null);
    }

    /**
     * Convenience method to load fields from the datastore.
     * Note that if the fieldNumbers is null/empty we still should call the persistence handler since it may mean
     * that the version field needs loading.
     * @param fieldNumbers The field numbers.
     */
    protected void loadFieldsFromDatastore(int[] fieldNumbers)
    {
        if (myLC.isNew() && myLC.isPersistent() && !isFlushedNew())
        {
            // Not yet flushed new persistent object to datastore so no point in "loading"
            return;
        }

        if ((flags&FLAG_NEED_INHERITANCE_VALIDATION)!=0) // TODO Merge this into fetch object handler
        {
            String className = getStoreManager().getClassNameForObjectID(myID, myEC.getClassLoaderResolver(), myEC);
            if (!getObject().getClass().getName().equals(className))
            {
                myEC.removeObjectFromLevel1Cache(myID);
                myEC.removeObjectFromLevel2Cache(myID);
                throw new NucleusObjectNotFoundException("Object with id " + myID + 
                    " was created without validating of type " + getObject().getClass().getName() +
                    " but is actually of type " + className);
            }
            flags &= ~FLAG_NEED_INHERITANCE_VALIDATION;
        }

        // TODO If the field has "loadFetchGroup" defined, then add it to the fetch plan etc
        getStoreManager().getPersistenceHandler().fetchObject(this, fieldNumbers);
    }

    /**
     * Convenience accessor to return the field numbers for the input loaded and dirty field arrays.
     * @param loadedFields Fields that were detached with the object
     * @param dirtyFields Fields that have been modified while detached
     * @return The field numbers of loaded or dirty fields
     */
    protected int[] getFieldNumbersOfLoadedOrDirtyFields(boolean[] loadedFields, boolean[] dirtyFields)
    {
        // Find the number of fields that are loaded or dirty
        int numFields = 0;
        for (int i=0;i<loadedFields.length;i++)
        {
            if (loadedFields[i] || dirtyFields[i])
            {
                numFields++;
            }
        }

        int[] fieldNumbers = new int[numFields];
        int n=0;
        int[] allFieldNumbers = cmd.getAllMemberPositions();
        for (int i=0;i<loadedFields.length;i++)
        {
            if (loadedFields[i] || dirtyFields[i])
            {
                fieldNumbers[n++] = allFieldNumbers[i];
            }
        }
        return fieldNumbers;
    }

    /**
     * Creates a copy of the {@link #dirtyFields} bitmap.
     * @return a copy of the {@link #dirtyFields} bitmap.
     */
    public boolean[] getDirtyFields()
    {
        boolean[] copy = new boolean[dirtyFields.length];
        System.arraycopy(dirtyFields, 0, copy, 0, dirtyFields.length);
        return copy;
    }

    /**
     * Accessor for the field numbers of all dirty fields.
     * @return Absolute field numbers of the dirty fields in this instance.
     */
    public int[] getDirtyFieldNumbers()
    {
        return ClassUtils.getFlagsSetTo(dirtyFields, true);
    }

    /**
     * Accessor for the fields
     * @return boolean array of loaded state in order of absolute field numbers
     */
    public boolean[] getLoadedFields() 
    {
        return loadedFields.clone();
    }

    /**
     * Accessor for the field numbers of all loaded fields in this managed instance.
     * @return Field numbers of all (currently) loaded fields
     */
    public int[] getLoadedFieldNumbers()
    {
        return ClassUtils.getFlagsSetTo(loadedFields, true);
    }

    /**
     * Returns whether all fields are loaded.
     * @return Returns true if all fields are loaded.
     */
    public boolean getAllFieldsLoaded()
    {
        for (int i = 0;i<loadedFields.length;i++)
        {
            if (!loadedFields[i])
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Convenience accessor for the names of the fields that are dirty.
     * @return Names of the dirty fields
     */
    public String[] getDirtyFieldNames()
    {
        int[] dirtyFieldNumbers = ClassUtils.getFlagsSetTo(dirtyFields, true);
        if (dirtyFieldNumbers != null && dirtyFieldNumbers.length > 0)
        {
            String[] dirtyFieldNames = new String[dirtyFieldNumbers.length];
            for (int i=0;i<dirtyFieldNumbers.length;i++)
            {
                dirtyFieldNames[i] = cmd.getMetaDataForManagedMemberAtAbsolutePosition(dirtyFieldNumbers[i]).getName();
            }
            return dirtyFieldNames;
        }
        return null;
    }

    /**
     * Convenience accessor for the names of the fields that are loaded.
     * @return Names of the loaded fields
     */
    public String[] getLoadedFieldNames()
    {
        int[] loadedFieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, true);
        if (loadedFieldNumbers != null && loadedFieldNumbers.length > 0)
        {
            String[] loadedFieldNames = new String[loadedFieldNumbers.length];
            for (int i=0;i<loadedFieldNumbers.length;i++)
            {
                loadedFieldNames[i] = cmd.getMetaDataForManagedMemberAtAbsolutePosition(loadedFieldNumbers[i]).getName();
            }
            return loadedFieldNames;
        }
        return null;
    }

    /**
     * Accessor for whether a field is currently loaded.
     * Just returns the status, unlike "isLoaded" which also loads it if not.
     * @param fieldNumber The (absolute) field number
     * @return Whether it is loaded
     */
    public boolean isFieldLoaded(int fieldNumber)
    {
        return loadedFields[fieldNumber];
    }

    protected void clearFieldsByNumbers(int[] fieldNumbers)
    {
        replaceFields(fieldNumbers, HOLLOWFIELDMANAGER);
        for (int i=0;i<fieldNumbers.length;i++)
        {
            loadedFields[fieldNumbers[i]] = false;
            dirtyFields[fieldNumbers[i]] = false;
        }
    }

    /**
     * Method to clear all dirty flags on the object.
     */
    protected void clearDirtyFlags()
    {
        dirty = false;
        ClassUtils.clearFlags(dirtyFields);
    }
    
    /**
     * Method to clear all dirty flags on the object.
     * @param fieldNumbers the fields to clear
     */
    protected void clearDirtyFlags(int[] fieldNumbers)
    {
        dirty = false;
        ClassUtils.clearFlags(dirtyFields,fieldNumbers);
    }

    /**
     * Convenience method to unload a field/property.
     * @param fieldName Name of the field/property
     * @throws NucleusUserException if the object managed by this StateManager is embedded
     */
    public void unloadField(String fieldName)
    {
        if (objectType == ObjectProvider.PC)
        {
            // Mark as not loaded
            AbstractMemberMetaData mmd = getClassMetaData().getMetaDataForMember(fieldName);
            loadedFields[mmd.getAbsoluteFieldNumber()] = false;
        }
        else
        {
            // TODO When we have nested embedded objects that can have relations to non-embedded then this needs to change
            throw new NucleusUserException("Cannot unload field/property of embedded object");
        }
    }

    /**
     * Convenience accessor for whether this StateManager manages an embedded/serialised object.
     * @return Whether the managed object is embedded/serialised.
     */
    public boolean isEmbedded()
    {
        return objectType > 0;
    }

    // -------------------------- providedXXXField Methods ----------------------------

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedBooleanField(T pc, int fieldNumber, boolean currentValue)
    {
        currFM.storeBooleanField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedByteField(T pc, int fieldNumber, byte currentValue)
    {
        currFM.storeByteField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedCharField(T pc, int fieldNumber, char currentValue)
    {
        currFM.storeCharField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedDoubleField(T pc, int fieldNumber, double currentValue)
    {
        currFM.storeDoubleField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedFloatField(T pc, int fieldNumber, float currentValue)
    {
        currFM.storeFloatField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedIntField(T pc, int fieldNumber, int currentValue)
    {
        currFM.storeIntField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedLongField(T pc, int fieldNumber, long currentValue)
    {
        currFM.storeLongField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedShortField(T pc, int fieldNumber, short currentValue)
    {
        currFM.storeShortField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedStringField(T pc, int fieldNumber, String currentValue)
    {
        currFM.storeStringField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedObjectField(T pc, int fieldNumber, Object currentValue)
    {
        currFM.storeObjectField(fieldNumber, currentValue);
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a boolean field.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public boolean replacingBooleanField(T pc, int fieldNumber)
    {
        boolean value = currFM.fetchBooleanField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a byte field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public byte replacingByteField(T obj, int fieldNumber)
    {
        byte value = currFM.fetchByteField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a char field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public char replacingCharField(T obj, int fieldNumber)
    {
        char value = currFM.fetchCharField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a double field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public double replacingDoubleField(T obj, int fieldNumber)
    {
        double value = currFM.fetchDoubleField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a float field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public float replacingFloatField(T obj, int fieldNumber)
    {
        float value = currFM.fetchFloatField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a int field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public int replacingIntField(T obj, int fieldNumber)
    {
        int value = currFM.fetchIntField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a long field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public long replacingLongField(T obj, int fieldNumber)
    {
        long value = currFM.fetchLongField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a short field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public short replacingShortField(T obj, int fieldNumber)
    {
        short value = currFM.fetchShortField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a String field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public String replacingStringField(T obj, int fieldNumber)
    {
        String value = currFM.fetchStringField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of an Object field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public Object replacingObjectField(T obj, int fieldNumber)
    {
        try
        {
            Object value = currFM.fetchObjectField(fieldNumber);
            loadedFields[fieldNumber] = true;
            return value;
        }
        catch (EndOfFetchPlanGraphException eodge)
        {
            // Beyond the scope of the fetch-depth when detaching
            return null;
        }
    }

    /**
     * Method to set this StateManager as managing an embedded/serialised object.
     * @param objType The type of object being managed
     */
    public void setPcObjectType(short objType)
    {
        this.objectType = objType;
    }

    public void lock(short lockMode)
    {
        this.lockMode = lockMode;
    }

    public void unlock()
    {
        this.lockMode = LockManager.LOCK_MODE_NONE;
    }

    public short getLockMode()
    {
        return lockMode;
    }

    /**
     * Registers the pc class in the cache
     */
    public void registerTransactional()
    {
        myEC.addObjectProvider(this);
    }

    /**
     * Method to set an associated value stored with this object.
     * This is for a situation such as in ORM where this object can have an "external" foreign-key
     * provided by an owning object (e.g 1-N uni relation and this is the element with no knowledge
     * of the owner, so the associated value is the FK value).
     * @param key Key for the value
     * @param value The associated value
     */
    public void setAssociatedValue(Object key, Object value)
    {
        myEC.setObjectProviderAssociatedValue(this, key, value);
    }

    /**
     * Accessor for an associated value stored with this object.
     * This is for a situation such as in ORM where this object can have an "external" foreign-key
     * provided by an owning object (e.g 1-N uni relation and this is the element with no knowledge
     * of the owner, so the associated value is the FK value).
     * @param key Key for the value
     * @return The associated value
     */
    public Object getAssociatedValue(Object key)
    {
        return myEC.getObjectProviderAssociatedValue(this, key);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.ObjectProvider#removeAssociatedValue(java.lang.Object)
     */
    public void removeAssociatedValue(Object key)
    {
        myEC.removeObjectProviderAssociatedValue(this, key);
    }

    public boolean containsAssociatedValue(Object key)
    {
        return myEC.containsObjectProviderAssociatedValue(this, key);
    }
}