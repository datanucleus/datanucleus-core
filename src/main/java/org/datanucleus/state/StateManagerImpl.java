/**********************************************************************
Copyright (c) 2002 Kelly Grizzle and others. All rights reserved.
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
2003 Erik Bengtson - removed exist() operation
2003 Andy Jefferson - added localiser
2003 Erik Bengtson - added new constructor for App ID
2003 Erik Bengtson - fixed loadDefaultFetchGroup to call jdoPostLoad
2003 Erik Bengtson - fixed evict to call jdoPreClear
2004 Andy Jefferson - converted to use Logger
2004 Andy Jefferson - reordered methods to put in categories, split String utilities across to StringUtils.
2004 Andy Jefferson - added Lifecycle Listener callbacks
2004 Andy Jefferson - removed JDK 1.4 methods so that we support 1.3 also
2005 Martin Taal - Contrib of detach() method for "detachOnClose" functionality.
2007 Xuan Baldauf - Contrib of initialiseForHollowPreConstructed()
2007 Xuan Baldauf - Contrib of internalXXX() methods for fields
2007 Xuan Baldauf - remove the fields "jdoLoadedFields" and "jdoModifiedFields".  
2007 Xuan Baldauf - remove the fields "retrievingDetachedState" and "resettingDetachedState".
2007 Xuan Baldauf - remove the field "updatingEmbeddedFieldsWithOwner"
2008 Andy Jefferson - removed all deps on org.datanucleus.store.mapped
    ...
 **********************************************************************/
package org.datanucleus.state;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.BitSet;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.DetachState;
import org.datanucleus.ExecutionContext;
import org.datanucleus.ExecutionContext.EmbeddedOwnerRelation;
import org.datanucleus.FetchPlan;
import org.datanucleus.FetchPlanForClass;
import org.datanucleus.FetchPlanState;
import org.datanucleus.PersistableObjectType;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.cache.CachedPC;
import org.datanucleus.cache.L2CachePopulateFieldManager;
import org.datanucleus.cache.L2CacheRetrieveFieldManager;
import org.datanucleus.cache.Level2Cache;
import org.datanucleus.enhancement.Detachable;
import org.datanucleus.enhancement.ExecutionContextReference;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.enhancement.StateManager;
import org.datanucleus.enhancer.EnhancementHelper;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NotYetFlushedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.flush.DeleteOperation;
import org.datanucleus.flush.PersistOperation;
import org.datanucleus.flush.UpdateMemberOperation;
import org.datanucleus.identity.IdentityReference;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.metadata.VersionStrategy;
import org.datanucleus.store.FieldValues;
import org.datanucleus.store.ObjectReferencingStoreManager;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.store.fieldmanager.AbstractFetchDepthFieldManager.EndOfFetchPlanGraphException;
import org.datanucleus.store.fieldmanager.AttachFieldManager;
import org.datanucleus.store.fieldmanager.DeleteFieldManager;
import org.datanucleus.store.fieldmanager.DetachFieldManager;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.store.fieldmanager.LoadFieldManager;
import org.datanucleus.store.fieldmanager.MakeTransientFieldManager;
import org.datanucleus.store.fieldmanager.PersistFieldManager;
import org.datanucleus.store.fieldmanager.SingleTypeFieldManager;
import org.datanucleus.store.fieldmanager.SingleValueFieldManager;
import org.datanucleus.store.fieldmanager.UnsetOwnerFieldManager;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOCollection;
import org.datanucleus.store.types.SCOContainer;
import org.datanucleus.store.types.SCOMap;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.containers.ContainerHandler;
import org.datanucleus.store.types.converters.TypeConversionHelper;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Implementation of a StateManager, supporting the bytecode enhancement contract of DataNucleus.
 * Implemented here as one StateManager per Object so adds on functionality particular to each object. 
 * All Persistable objects will have a StateManager when they have had communication with the ExecutionContext. 
 * They will typically always have an identity also. The exception to that is for embedded/serialised objects.
 * 
 * <H3>Embedded/Serialised Objects</H3>
 * An object that is being embedded/serialised in an owning object will NOT have an identity unless the object is subject to a makePersistent() call also. 
 * When an object is embedded/serialised and a field is changed, the field will NOT be marked as dirty (unless it is also an object in its own right with an identity). 
 * When a field is changed any owning objects are updated so that they can update their tables accordingly.
 *
 * <H3>Performance and Memory</H3>
 * StateManagers are very performance-critical, because for each Persistable object made persistent,
 * there will be one StateManager instance, adding up to the total memory footprint of that object.
 * In heap profiling analysis (cerca 2008), StateManagerImpl showed to consume bytes 169 per StateManager by itself
 * and about 500 bytes per StateManager when taking PC-individual child-object (like the id) referred by the StateManager into account. 
 * With small Java objects this can mean a substantial memory overhead and for applications using such small objects can be critical. 
 * For this reason the StateManager should always be minimal in memory consumption.
 * Any fields that are only present for some cases should, in general, either be offloaded to the ExecutionContext, or to a separate object if multiple fields.
 * The fields <I>loadedFields</I> and <I>dirtyFields</I> could, arguably, be made BitSet but it isn't clear of the benefit in the typical use-case
 * of smaller array sizes (number of fields in a class), as per https://www.baeldung.com/java-boolean-array-bitset-performance
 * 
 * <H3>Commit/Rollback</H3>
 * When the managed object is changed it is saved as <I>savedPC</I> and its state as <I>savedPersistenceFlags</I> and <I>savedLoadedFields</I>.
 * These fields allow it to be rolled-back to an earlier state. Refer to the <I>saveFields</I> and <I>restoreFields</I> methods.
 */
public class StateManagerImpl implements DNStateManager<Persistable>
{
    protected static final SingleTypeFieldManager HOLLOWFIELDMANAGER = new SingleTypeFieldManager();

    /** Whether we are in the process of INSERTING the object to persistence. */
    protected static final int FLAG_INSERTING = 2<<21;
    /** Whether we are in the process of INSERTING the object from persistence, running callbacks. */
    protected static final int FLAG_INSERTING_CALLBACKS = 2<<20;
    /** Whether we are in the process of DELETING the object from persistence. */
    protected static final int FLAG_DELETING = 2<<19;
    /** Whether we are managing an embedded object. */
    protected static final int FLAG_EMBEDDED = 2<<18;
    /** Whether we are currently validating the object in the datastore. */
    protected static final int FLAG_VALIDATING = 2<<17;
    /** Whether to restore values at StateManager. If true, overwrites the restore values at tx level. */
    protected static final int FLAG_RESTORE_VALUES = 2<<16;
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

    /** The persistable instance managed by this StateManager. */
    protected Persistable myPC;

    /** Bit-packed flags for operational settings (packed into "int" for memory benefit). */
    protected int flags;

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

    /** Optimistic version, when starting any transaction. */
    protected Object myVersion;

    /** Optimistic version, after insert/update but not yet committed (i.e incremented). */
    protected Object transactionalVersion;

    /** Flags for state stored with the object. Maps onto org.datanucleus.enhancement.Persistable "dnFlags". */
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

    /** Current FieldManager. */
    protected FieldManager currFM = null;

    /** Saved state, for use during any rollback for reinstating the object. */
    protected SavedState savedState = null;

    private static final EnhancementHelper HELPER;
    static
    {
        HELPER = (EnhancementHelper) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                try
                {
                    return EnhancementHelper.getInstance();
                }
                catch (SecurityException e)
                {
                    throw new NucleusUserException(Localiser.msg("026000"), e).setFatal();
                }
            }
        });
    }

    /**
     * Constructor for object of specified type managed by the provided ExecutionContext.
     * @param ec ExecutionContext
     * @param cmd the metadata for the class.
     */
    public StateManagerImpl(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        connect(ec, cmd);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.AbstractStateManager#connect(org.datanucleus.ExecutionContext, org.datanucleus.metadata.AbstractClassMetaData)
     */
    @Override
    public void connect(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        int fieldCount = cmd.getMemberCount();
        this.cmd = cmd;
        this.myEC = ec;

        dirtyFields = new boolean[fieldCount];
        loadedFields = new boolean[fieldCount];
        dirty = false;
        myFP = myEC.getFetchPlan().getFetchPlanForClass(cmd);
        myVersion = null;
        transactionalVersion = null;
        persistenceFlags = 0;

        ec.setAttachDetachReferencedObject(this, null);
    }

    /**
     * Disconnect from the ExecutionContext and persistable object.
     */
    public void disconnect()
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("026011", IdentityUtils.getPersistableIdentityForId(getInternalObjectId()), this));
        }

        // Transitioning to TRANSIENT state, so if any postLoad action is pending we do it before. 
        // This usually happens when we make transient instances using the fetch plan and some
        // fields were loaded during this action which triggered a dnPostLoad event
        if (isPostLoadPending())
        {
            flags &= ~FLAG_CHANGING_STATE; //hack to make sure postLoad does not return without processing
            setPostLoadPending(false);
            postLoad();
        }

        // Call unsetOwner() on all loaded SCO fields.
        int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, cmd.getSCOMutableMemberPositions(), true);
        if (fieldNumbers != null && fieldNumbers.length > 0)
        {
            provideFields(fieldNumbers, new UnsetOwnerFieldManager());
        }

        // Disconnect any embedded related StateManagers, since we own them
        List<EmbeddedOwnerRelation> subSMRelations = myEC.getEmbeddedInformationForOwner(this);
        if (subSMRelations != null && !subSMRelations.isEmpty())
        {
            for (EmbeddedOwnerRelation embRel : subSMRelations)
            {
                DNStateManager embSM = embRel.getEmbeddedSM();
                if (embSM.isConnected())
                {
                    embSM.disconnect();
                }
            }
        }

        myEC.removeStateManagerFromCache(this);

        persistenceFlags = Persistable.READ_WRITE_OK;
        myPC.dnReplaceFlags();

        flags |= FLAG_DISCONNECTING;
        try
        {
            replaceStateManager(myPC, null);
        }
        finally
        {
            flags &= ~FLAG_DISCONNECTING;
        }

        savedState = null;
        preDeleteLoadedFields = null;
        myPC = null;
        myID = null;
        myInternalID = null;
        myLC = null;
        myEC = null;
        myFP = null;
        myVersion = null;
        persistenceFlags = 0;
        flags = 0;
        transactionalVersion = null;
        currFM = null;
        dirty = false;
        cmd = null;
        dirtyFields = null;
        loadedFields = null;

        // TODO Remove the object from any pooling (when we enable it) via nucCtx.getStateManagerFactory().disconnectStateManager(this);
    }

    @Override
    public boolean isConnected()
    {
        return myPC != null;
    }

    /**
     * Initialises a state manager to manage a hollow instance having the given object ID and the given (optional) field values. 
     * This constructor is used for creating new instances of existing persistent objects, and consequently shouldn't be used 
     * when the StoreManager controls the creation of such objects (such as in an ODBMS).
     * @param id the JDO identity of the object.
     * @param fv the initial field values of the object (optional)
     * @param pcClass Class of the object that this will manage the state for
     */
    public void initialiseForHollow(Object id, FieldValues fv, Class pcClass)
    {
        myID = id;
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.HOLLOW);
        persistenceFlags = Persistable.LOAD_REQUIRED;
        if (IdentityUtils.isDatastoreIdentity(id) || id == null)
        {
            // Create new PC
            myPC = HELPER.newInstance(pcClass, this);
        }
        else
        {
            // Create new PC, and copy the key class to fields
            myPC = HELPER.newInstance(pcClass, this, myID);
            markPKFieldsAsLoaded();
        }

        // Put in L1 cache just in case referred to by other objects in the FieldValues
        // e.g when we retrieve objects with circular references in the same result set from a query
        myEC.putObjectIntoLevel1Cache(this);

        if (fv != null)
        {
            loadFieldValues(fv);
            // TODO If this object has unique key(s) then they will likely be loaded from the fieldValues, so could put in L1 cache here
        }
    }

    /**
     * Initialises a state manager to manage the given hollow instance having the given object ID.
     * Unlike the {@link #initialiseForHollow} method, this method does not create a new instance and instead 
     * takes a pre-constructed instance (such as from an ODBMS).
     * @param id the identity of the object.
     * @param pc the object to be managed.
     */
    public void initialiseForHollowPreConstructed(Object id, Persistable pc)
    {
        myID = id;
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.HOLLOW);
        persistenceFlags = Persistable.LOAD_REQUIRED;
        myPC = pc;

        replaceStateManager(myPC, this); // Assign this StateManager to the PC
        myPC.dnReplaceFlags();

        // TODO Add to the cache
    }

    /**
     * Initialises a state manager to manage the passed persistent instance having the given object ID.
     * Used where we have retrieved a PC object from a datastore directly (not field-by-field), for example on an object datastore. 
     * This initialiser will not add StateManagers to all related PCs. This must be done by any calling process. 
     * This simply adds the StateManager to the specified object and records the id, setting all fields of the object as loaded.
     * @param id the identity of the object.
     * @param pc The object to be managed
     */
    public void initialiseForPersistentClean(Object id, Persistable pc)
    {
        myID = id;
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_CLEAN);
        persistenceFlags = Persistable.LOAD_REQUIRED;
        myPC = pc;

        replaceStateManager(myPC, this); // Assign this StateManager to the PC
        myPC.dnReplaceFlags();

        // Mark all fields as loaded
        for (int i=0; i<loadedFields.length; ++i)
        {
            loadedFields[i] = true;
        }

        // Add the object to the cache
        myEC.putObjectIntoLevel1Cache(this);
    }

    /**
     * Initialises a state manager to manage a provided Persistable instance that will be EMBEDDED/SERIALISED into another Persistable object. 
     * The instance will not be assigned an identity in the process since it is a SCO.
     * @param pc The Persistable to manage (see copyPc also)
     * @param copyPc Whether the SM should manage a copy of the passed PC or that one
     */
    public void initialiseForEmbedded(Persistable pc, boolean copyPc)
    {
        myID = null; // Embedded, so don;t need an ID since we're not persisting it
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_CLEAN);
        persistenceFlags = Persistable.LOAD_REQUIRED;
        flags |= FLAG_EMBEDDED;

        myPC = pc;
        replaceStateManager(myPC, this); // Set SM for embedded PC to be this
        if (copyPc)
        {
            // Create a new PC with the same field values
            Persistable pcCopy = myPC.dnNewInstance(this);
            pcCopy.dnCopyFields(myPC, cmd.getAllMemberPositions());

            // Swap the managed PC to be the copy and not the input
            replaceStateManager(pcCopy, this);
            myPC = pcCopy;

            // Reset dnFlags in the input object to Persistable.READ_WRITE_OK and clear its state manager.
            pc.dnReplaceFlags();
            replaceStateManager(pc, null);
        }

        // Mark all fields as loaded since we are using the passed Persistable
        for (int i=0;i<loadedFields.length;i++)
        {
            loadedFields[i] = true;
        }
    }

    /**
     * Initialises a state manager to manage an embedded instance of the specified type.
     * This constructor is used for creating new instances of existing (embedded) persistent objects, and consequently shouldn't be used 
     * when the StoreManager controls the creation of such objects (such as in an ODBMS).
     * TODO Consider passing in a FieldValues and set the fields
     * @param pcClass Class of the (embedded) object that this will manage the state for
     */
    public void initialiseForEmbedded(Class<Persistable> pcClass)
    {
        myID = null; // Embedded, so don't need an ID since we're not persisting it
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_CLEAN); // TODO Should be HOLLOW?
        persistenceFlags = Persistable.LOAD_REQUIRED;
        flags |= FLAG_EMBEDDED;

        // Create new (embedded) PC
        myPC = HELPER.newInstance(pcClass, this);

        // Mark all fields as loaded for now, to be populated later
        for (int i=0;i<loadedFields.length;i++)
        {
            loadedFields[i] = true;
        }
    }

    /**
     * Initialises a state manager to manage a transient instance that is becoming newly persistent.
     * A new object ID for the instance is obtained from the store manager and the object is inserted in the data store.
     * <p>
     * This constructor is used for assigning state managers to existing instances that are transitioning to a persistent state.
     * @param pc the instance being make persistent.
     * @param preInsertChanges Any changes to make before inserting
     */
    public void initialiseForPersistentNew(Persistable pc, FieldValues preInsertChanges)
    {
        myPC = pc;
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_NEW);
        persistenceFlags = Persistable.READ_OK;
        for (int i=0; i<loadedFields.length; ++i)
        {
            loadedFields[i] = true;
        }

        replaceStateManager(myPC, this); // Assign this StateManager to the PC
        myPC.dnReplaceFlags();

        saveFields();

        // Populate all fields that have "value-strategy" and are not datastore populated
        populateValueGenerationMembers();

        if (preInsertChanges != null)
        {
            // Apply any pre-insert field updates
            preInsertChanges.fetchFields(this);
        }

        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            // Make sure any PK fields that are Persistable are persisted first, so we have an id to assign our identity
            int[] pkFieldNumbers = cmd.getPKMemberPositions();
            for (int i=0;i<pkFieldNumbers.length;i++)
            {
                int fieldNumber = pkFieldNumbers[i];
                AbstractMemberMetaData pkMmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
                if (myEC.getMetaDataManager().getMetaDataForClass(pkMmd.getType(), getExecutionContext().getClassLoaderResolver()) != null)
                {
                    try
                    {
                        if (myEC.getMultithreaded())
                        {
                            myEC.threadLock();
                        }

                        FieldManager prevFM = currFM;
                        try
                        {
                            currFM = new SingleValueFieldManager();
                            myPC.dnProvideField(fieldNumber);
                            Persistable pkFieldPC = (Persistable) ((SingleValueFieldManager) currFM).fetchObjectField(fieldNumber);
                            if (pkFieldPC == null)
                            {
                                throw new NucleusUserException(Localiser.msg("026016", pkMmd.getFullFieldName()));
                            }
                            if (!myEC.getApiAdapter().isPersistent(pkFieldPC))
                            {
                                // Make sure the PC field is persistent - can cause the insert of our object being managed by this SM via flush() when bidir relation
                                Object persistedFieldPC = null;
                                if (pkMmd.isEmbedded())
                                {
                                    persistedFieldPC = myEC.persistObjectInternal(pkFieldPC, null, PersistableObjectType.EMBEDDED_PC, this, fieldNumber);
                                }
                                else
                                {
                                    persistedFieldPC = myEC.persistObjectInternal(pkFieldPC, null, PersistableObjectType.PC);
                                }
                                replaceField(myPC, fieldNumber, persistedFieldPC, false);
                            }
                        }
                        finally
                        {
                            currFM = prevFM;
                        }
                    }
                    finally
                    {
                        if (myEC.getMultithreaded())
                        {
                            myEC.threadUnlock();
                        }
                    }
                }
            }
        }

        // Set the identity of this object
        setIdentity(false);

        if (myEC.getTransaction().isActive())
        {
            myEC.enlistInTransaction(this);
        }

        // Now in PERSISTENT_NEW so call any callbacks/listeners
        getCallbackHandler().postCreate(myPC);

        if (myEC.getManageRelations())
        {
            // Managed Relations : register non-null bidir fields for later processing
            ClassLoaderResolver clr = myEC.getClassLoaderResolver();
            int[] relationPositions = cmd.getRelationMemberPositions(clr);
            if (relationPositions != null)
            {
                for (int i=0;i<relationPositions.length;i++)
                {
                    AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(relationPositions[i]);
                    if (RelationType.isBidirectional(mmd.getRelationType(clr)))
                    {
                        Object value = provideField(relationPositions[i]);
                        if (value != null)
                        {
                            // Store the field with value of null so it gets checked
                            myEC.getRelationshipManager(this).relationChange(relationPositions[i], null, value);
                        }
                    }
                }
            }
        }
    }

    /**
     * Initialises a state manager to manage a Transactional Transient instance.
     * A new object ID for the instance is obtained from the store manager and the object is inserted in the data store.
     * <p>
     * This constructor is used for assigning state managers to Transient
     * instances that are transitioning to a transient clean state.
     * @param pc the instance being make persistent.
     */
    public void initialiseForTransactionalTransient(Persistable pc)
    {
        myPC = pc;
        myLC = null;
        persistenceFlags = Persistable.READ_OK;
        for (int i=0; i<loadedFields.length; ++i)
        {
            loadedFields[i] = true;
        }
        myPC.dnReplaceFlags();

        // Populate all fields that have "value-strategy" and are not datastore populated
        populateValueGenerationMembers();

        // Set the identity
        setIdentity(false);

        // for non transactional read, tx might be not active
        // TODO add verification if is non transactional read = true
        if (myEC.getTransaction().isActive())
        {
            myEC.enlistInTransaction(this);
        }
    }

    /**
     * Initialises the StateManager to manage a Persistable object in detached state.
     * @param pc the detach object.
     * @param id the identity of the object.
     * @param version the detached version
     */
    public void initialiseForDetached(Persistable pc, Object id, Object version)
    {
        this.myID = id;
        this.myPC = pc;
        setVersion(version);

        // This lifecycle state is not always correct. It is certainly "detached"
        // but we dont know if it is CLEAN or DIRTY. We need this setting here since all objects
        // have a lifecycle state and other methods e.g isPersistent() depend on it.
        this.myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.DETACHED_CLEAN);

        this.myPC.dnReplaceFlags();
        replaceStateManager(myPC, this);
    }

    /**
     * Initialises the StateManager to manage a Persistable object that is not persistent but is about to be deleted.
     * @param pc the object to delete
     */
    public void initialiseForPNewToBeDeleted(Persistable pc)
    {
        this.myID = null;
        this.myPC = pc;
        this.myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_NEW);
        for (int i=0; i<loadedFields.length; ++i) // Mark all fields as loaded
        {
            loadedFields[i] = true;
        }
        replaceStateManager(myPC, this);
    }

    /**
     * Initialise StateManager, assigning the specified id to the object. 
     * This is used when getting objects out of the L2 Cache, where they have no StateManager 
     * assigned, and returning them as associated with a particular ExecutionContext.
     * @param cachedPC The cached PC object
     * @param id Id to assign to the Persistable object
     */
    public void initialiseForCachedPC(CachedPC cachedPC, Object id)
    {
        // Create a new copy of the input object type, performing the majority of the initialisation
        initialiseForHollow(id, null, cachedPC.getObjectClass());

        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_CLEAN);
        persistenceFlags = Persistable.READ_OK;

        int[] fieldsToLoad = ClassUtils.getFlagsSetTo(cachedPC.getLoadedFields(), myFP.getMemberNumbers(), true);
        if (fieldsToLoad != null)
        {
            // Put this object in L1 cache for easy referencing
            myEC.putObjectIntoLevel1Cache(this);

            L2CacheRetrieveFieldManager l2RetFM = new L2CacheRetrieveFieldManager(this, cachedPC);
            this.replaceFields(fieldsToLoad, l2RetFM);
            for (int i=0;i<fieldsToLoad.length;i++)
            {
                loadedFields[fieldsToLoad[i]] = true;
            }

            int[] fieldsNotLoaded = l2RetFM.getFieldsNotLoaded();
            if (fieldsNotLoaded != null)
            {
                for (int i=0;i<fieldsNotLoaded.length;i++)
                {
                    loadedFields[fieldsNotLoaded[i]] = false;
                }
            }
        }

        if (cachedPC.getVersion() != null)
        {
            // Make sure we start from the same version as was cached
            setVersion(cachedPC.getVersion());
        }

        // Make sure any SCO fields are wrapped
        replaceAllLoadedSCOFieldsWithWrappers();

        if (myEC.getTransaction().isActive())
        {
            myEC.enlistInTransaction(this);
        }

        if (areFieldsLoaded(myFP.getMemberNumbers()))
        {
            // Should we call postLoad when getting the object out of the L2 cache ? Seems incorrect IMHO
            postLoad();
        }
    }

    /**
     * Convenience method to populate all members in the PC object that need their value generating (according to metadata) and that aren't datastore-attributed. 
     * This applies not just to PK members (main use-case) but also to any other member (DN extension). 
     * Members can be populated only if they are null, dependent on metadata. 
     * This method is called once on a PC object, when makePersistent is called.
     */
    private void populateValueGenerationMembers()
    {
        int[] valueGenMemberPositions = cmd.getValueGenerationMemberPositions();
        if (valueGenMemberPositions != null)
        {
            for (int i=0;i<valueGenMemberPositions.length;i++)
            {
                int memberPos = valueGenMemberPositions[i];
                AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(memberPos);

                // Check for the strategy (not really required now TODO Remove check), and if it is a datastore attributed strategy
                if (mmd.getValueStrategy() != null && !getStoreManager().isValueGenerationStrategyDatastoreAttributed(cmd, memberPos))
                {
                    // Assign the strategy value where required.
                    // Default JDO/JPA behaviour is to always provide a strategy value when it is marked as using a strategy
                    boolean applyStrategy = true;
                    if (!mmd.getType().isPrimitive() &&
                        mmd.hasExtension(MetaData.EXTENSION_MEMBER_STRATEGY_WHEN_NOTNULL) &&
                        mmd.getValueForExtension(MetaData.EXTENSION_MEMBER_STRATEGY_WHEN_NOTNULL).equalsIgnoreCase("false") &&
                        this.provideField(memberPos) != null)
                    {
                        // extension to only provide a value-strategy value where the field is null at persistence.
                        applyStrategy = false;
                    }

                    if (applyStrategy)
                    {
                        // Apply a strategy value for this field
                        Object obj = getStoreManager().getValueGenerationStrategyValue(myEC, cmd, mmd);
                        this.replaceField(memberPos, obj);
                    }
                }
            }
        }
    }

    @Override
    public AbstractClassMetaData getClassMetaData()
    {
        return cmd;
    }

    @Override
    public ExecutionContext getExecutionContext()
    {
        return myEC;
    }

    @Override
    public ExecutionContextReference getExecutionContextReference()
    {
        return myEC;
    }

    @Override
    public StoreManager getStoreManager()
    {
        return myEC.getNucleusContext().isFederated() ? ((FederatedStoreManager)myEC.getStoreManager()).getStoreManagerForClass(cmd) : myEC.getStoreManager();
    }

    @Override
    public FetchPlanForClass getFetchPlanForClass()
    {
        return myFP;
    }

    public LifeCycleState getLifecycleState()
    {
        return myLC;
    }

    protected CallbackHandler getCallbackHandler()
    {
        return myEC.getCallbackHandler();
    }

    public Persistable getObject()
    {
        return myPC;
    }

    public String getObjectAsPrintable()
    {
        return StringUtils.toJVMIDString(myPC);
    }

    public String toString()
    {
        return "StateManager[pc=" + StringUtils.toJVMIDString(myPC) + ", lifecycle=" + myLC + (isEmbedded() ? ", EMBEDDED" : "") + "]";
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
        return (flags&FLAG_RESTORE_VALUES) != 0;
    }

    protected boolean isChangingState()
    {
        return (flags&FLAG_CHANGING_STATE) != 0;
    }

    public boolean isInserting()
    {
        return (flags&FLAG_INSERTING) != 0;
    }

    @Override
    public void setInserting()
    {
        flags |= FLAG_INSERTING;
        flags &= ~FLAG_INSERTING_CALLBACKS;
    }

    @Override
    public void setInsertingCallbacks()
    {
        flags &= ~FLAG_INSERTING;
        flags |= FLAG_INSERTING_CALLBACKS;
    }

    public boolean isDeleting()
    {
        return (flags&FLAG_DELETING) != 0;
    }

    public void markForInheritanceValidation()
    {
        flags |= FLAG_NEED_INHERITANCE_VALIDATION;
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
     * @see org.datanucleus.store.DNStateManager#setFlushedNew(boolean)
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
     * @see org.datanucleus.store.DNStateManager#isFlushedNew()
     */
    public boolean isFlushedNew()
    {
        return (flags&FLAG_FLUSHED_NEW)!=0;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.DNStateManager#isFlushedToDatastore()
     */
    public boolean isFlushedToDatastore()
    {
        return !dirty;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.DNStateManager#setFlushing(boolean)
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
     * @see org.datanucleus.store.DNStateManager#markAsFlushed()
     */
    public void markAsFlushed()
    {
        clearDirtyFlags();
    }

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
        if (myLC == null)
        {
            return;
        }

        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadLock();
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
                myEC.threadUnlock();
            }
        }
    }

    /**
     * Method to change the object state to write-field.
     */
    protected void transitionWriteField()
    {
        if (isEmbedded())
        {
            // Means that embedded objects don't change to DIRTY state, the owner handles state
            return;
        }
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadLock();
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
                myEC.threadUnlock();
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
    public void preBegin(org.datanucleus.transaction.Transaction tx)
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
     * This method is invoked just after a commit is performed in a Transaction involving the persistable object managed by this StateManager
     * @param tx The transaction
     */
    public void postCommit(org.datanucleus.transaction.Transaction tx)
    {
        preStateChange();
        try
        {
            myLC = myLC.transitionCommit(this, tx);
            if (transactionalVersion != myVersion)
            {
                myVersion = transactionalVersion;
            }
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * This method is invoked just before a rollback is performed in a Transaction involving the persistable object managed by this StateManager.
     * @param tx The transaction
     */
    public void preRollback(org.datanucleus.transaction.Transaction tx)
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

        flags |= FLAG_DELETING;
        try
        {
            if (dirty)
            {
                clearDirtyFlags();

                // Clear the PM's knowledge of our being dirty. This calls flush() which does nothing
                myEC.flushInternal(false);
            }

            if (!isEmbedded())
            {
                // Nothing to delete if embedded
                getStoreManager().getPersistenceHandler().deleteObject(this);
            }

            preDeleteLoadedFields = null;
        }
        finally
        {
            flags &= ~FLAG_DELETING;
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
    public Persistable getReferencedPC()
    {
        return (Persistable) myEC.getAttachDetachReferencedObject(this);
    }

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

    @Override
    public void unloadField(int fieldNumber)
    {
        if (isEmbedded())
        {
            // TODO When we have nested embedded objects that can have relations to non-embedded then this needs to change
            throw new NucleusUserException("Cannot unload field/property of embedded object");
        }

        // Mark as not loaded
        loadedFields[fieldNumber] = false;
    }

    @Override
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

    public void markFieldsAsLoaded(int[] fieldNumbers)
    {
        for (int i=0;i<fieldNumbers.length;i++)
        {
            loadedFields[fieldNumbers[i]] = true;
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
        String updateMode = myEC.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_UPDATE_MODE);
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
            CachedPC cachedPC = l2cache.get(myID);
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
                    NucleusLogger.CACHE.debug(Localiser.msg("026033", IdentityUtils.getPersistableIdentityForId(myID), StringUtils.intArrayToString(cacheFieldsToLoad)));
                }

                provideFields(cacheFieldsToLoad, new L2CachePopulateFieldManager(this, copyCachedPC));

                // Replace the current L2 cached object with this one
                myEC.getNucleusContext().getLevel2Cache().put(getInternalObjectId(), copyCachedPC);
            }
            else
            {
                // Add to L2 cache since not present
                myEC.putObjectIntoLevel2Cache(this, false);
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
        if (fieldNumbers == null || fieldNumbers.length == 0 || myEC.isFlushing() || myLC.isDeleted() || isDeleting() || myEC.getTransaction().isCommitting())
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
            CachedPC cachedPC = l2cache.get(myID);
            if (cachedPC != null)
            {
                int[] cacheFieldsToLoad = ClassUtils.getFlagsSetTo(cachedPC.getLoadedFields(), fieldNumbers, true);
                if (cacheFieldsToLoad != null && cacheFieldsToLoad.length > 0)
                {
                    if (NucleusLogger.CACHE.isDebugEnabled())
                    {
                        NucleusLogger.CACHE.debug(Localiser.msg("026034", IdentityUtils.getPersistableIdentityForId(myID), StringUtils.intArrayToString(cacheFieldsToLoad)));
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
        loadFieldsFromDatastore((fieldNumber >= 0) ? (new int[] {fieldNumber}) : new int[] {});
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
                throw new NucleusObjectNotFoundException("Object with id " + IdentityUtils.getPersistableIdentityForId(myID) + 
                    " was created without validating of type " + getObject().getClass().getName() + " but is actually of type " + className);
            }
            flags &= ~FLAG_NEED_INHERITANCE_VALIDATION;
        }

        // Add on version field if not currently set and using version field (surrogate will be added automatically by the query if required)
        int[] fieldNumbersToFetch = fieldNumbers;
        if (cmd.isVersioned())
        {
            VersionMetaData vermd = cmd.getVersionMetaDataForClass();
            if (vermd != null && vermd.getMemberName() != null)
            {
                int verFieldNum = cmd.getMetaDataForMember(vermd.getMemberName()).getAbsoluteFieldNumber();
                boolean versionPresent = false;
                for (int i=0;i<fieldNumbers.length;i++)
                {
                    if (fieldNumbers[i] == verFieldNum)
                    {
                        versionPresent = true;
                        break;
                    }
                }
                if (!versionPresent)
                {
                    // Add version field to fields to be fetched
                    int[] tmpFieldNumbers = new int[fieldNumbers.length + 1];
                    for (int i=0;i<fieldNumbers.length;i++)
                    {
                        tmpFieldNumbers[i] = fieldNumbers[i];
                    }
                    tmpFieldNumbers[fieldNumbers.length] = verFieldNum;
                    fieldNumbersToFetch = tmpFieldNumbers;
                }
            }
        }

        // TODO If the field has "loadFetchGroup" defined, then add it to the fetch plan etc
        getStoreManager().getPersistenceHandler().fetchObject(this, fieldNumbersToFetch);
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

    @Override
    public void storeFieldValue(int fieldNumber, Object value)
    {
        if (!loadedFields[fieldNumber])
        {
            setAssociatedValue(DNStateManager.MEMBER_VALUE_STORED_PREFIX + fieldNumber, value);
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("026037", cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber).getName(), this));
            }
        }
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
        ClassUtils.clearFlags(dirtyFields, fieldNumbers);
    }

    /**
     * Convenience accessor for whether this StateManager manages an embedded/serialised object.
     * @return Whether the managed object is embedded/serialised.
     */
    public boolean isEmbedded()
    {
        return (flags&FLAG_EMBEDDED) != 0;
    }

    // -------------------------- providedXXXField Methods ----------------------------

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedBooleanField(Persistable ignored, int fieldNumber, boolean currentValue)
    {
        currFM.storeBooleanField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedByteField(Persistable ignored, int fieldNumber, byte currentValue)
    {
        currFM.storeByteField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedCharField(Persistable ignored, int fieldNumber, char currentValue)
    {
        currFM.storeCharField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedDoubleField(Persistable ignored, int fieldNumber, double currentValue)
    {
        currFM.storeDoubleField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedFloatField(Persistable ignored, int fieldNumber, float currentValue)
    {
        currFM.storeFloatField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedIntField(Persistable ignored, int fieldNumber, int currentValue)
    {
        currFM.storeIntField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedLongField(Persistable ignored, int fieldNumber, long currentValue)
    {
        currFM.storeLongField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedShortField(Persistable ignored, int fieldNumber, short currentValue)
    {
        currFM.storeShortField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedStringField(Persistable ignored, int fieldNumber, String currentValue)
    {
        currFM.storeStringField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its dnProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedObjectField(Persistable ignored, int fieldNumber, Object currentValue)
    {
        currFM.storeObjectField(fieldNumber, currentValue);
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a boolean field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public boolean replacingBooleanField(Persistable ignored, int fieldNumber)
    {
        boolean value = currFM.fetchBooleanField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a byte field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public byte replacingByteField(Persistable ignored, int fieldNumber)
    {
        byte value = currFM.fetchByteField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a char field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public char replacingCharField(Persistable ignored, int fieldNumber)
    {
        char value = currFM.fetchCharField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a double field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public double replacingDoubleField(Persistable ignored, int fieldNumber)
    {
        double value = currFM.fetchDoubleField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a float field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public float replacingFloatField(Persistable ignored, int fieldNumber)
    {
        float value = currFM.fetchFloatField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a int field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public int replacingIntField(Persistable ignored, int fieldNumber)
    {
        int value = currFM.fetchIntField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a long field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public long replacingLongField(Persistable ignored, int fieldNumber)
    {
        long value = currFM.fetchLongField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a short field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public short replacingShortField(Persistable ignored, int fieldNumber)
    {
        short value = currFM.fetchShortField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of a String field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public String replacingStringField(Persistable ignored, int fieldNumber)
    {
        String value = currFM.fetchStringField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's dnReplaceField() method to refresh the value of an Object field.
     * @param ignored the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public Object replacingObjectField(Persistable ignored, int fieldNumber)
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
     * Registers the pc class in the cache
     */
    public void registerTransactional()
    {
        myEC.addStateManagerToCache(this);
    }

    @Override
    public void setAssociatedValue(Object key, Object value)
    {
        myEC.setStateManagerAssociatedValue(this, key, value);
    }

    @Override
    public Object getAssociatedValue(Object key)
    {
        return myEC.getStateManagerAssociatedValue(this, key);
    }

    @Override
    public void removeAssociatedValue(Object key)
    {
        myEC.removeStateManagerAssociatedValue(this, key);
    }

    @Override
    public boolean containsAssociatedValue(Object key)
    {
        return myEC.containsStateManagerAssociatedValue(this, key);
    }

    /**
     * Method to enlist the managed object in the current transaction.
     */
    public void enlistInTransaction()
    {
        if (!myEC.getTransaction().isActive())
        {
            return;
        }
        myEC.enlistInTransaction(this);

        if (persistenceFlags == Persistable.LOAD_REQUIRED && areFieldsLoaded(cmd.getDFGMemberPositions()))
        {
            // All DFG fields loaded and object is transactional so it doesnt need to contact us for those fields
            // Note that this is the DFG and NOT the current FetchPlan since in the enhancement of classes
            // all DFG fields are set to check dnFlags before relaying back to the StateManager
            persistenceFlags = Persistable.READ_OK;
            myPC.dnReplaceFlags();
        }
    }

    /**
     * Method to evict the managed object from the current transaction.
     */
    public void evictFromTransaction()
    {
        myEC.evictFromTransaction(this);

        // A non-transactional object needs to contact us on any field read no matter what fields are loaded.
        persistenceFlags = Persistable.LOAD_REQUIRED;
        myPC.dnReplaceFlags();
    }

    /**
     * Utility to update the passed object with the passed StateManager (can be null).
     * @param pc The object to update
     * @param sm The new state manager
     */
    protected void replaceStateManager(final Persistable pc, final StateManager sm)
    {
        try
        {
            // Calls to pc.dnReplaceStateManager must be run privileged
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run() 
                {
                    pc.dnReplaceStateManager(sm);
                    return null;
                }
            });
        }
        catch (SecurityException e)
        {
            throw new NucleusUserException(Localiser.msg("026000"), e).setFatal();
        }
    }

    /**
     * Replace the current value of StateManager in the Persistable object.
     * <p>
     * This method is called by the Persistable whenever dnReplaceStateManager is called and 
     * there is already an owning StateManager. This is a security precaution to ensure that the owning 
     * StateManager is the only source of any change to its reference in the Persistable.
     * </p>
     * @param pc the calling Persistable instance
     * @param sm the proposed new value for the StateManager
     * @return the new value for the StateManager
     */
    public StateManager replacingStateManager(Persistable pc, StateManager sm)
    {
        if (myLC == null)
        {
            throw new NucleusException("Null LifeCycleState").setFatal();
        }
        else if (myLC.stateType() == LifeCycleState.DETACHED_CLEAN)
        {
            return sm;
        }
        else if (pc == myPC)
        {
            //TODO check if we are really in transition to a transient instance
            if (sm == null)
            {
                return null;
            }
            if (sm == this)
            {
                return this;
            }

            if (myEC == ((DNStateManager)sm).getExecutionContext())
            {
                NucleusLogger.PERSISTENCE.debug("StateManagerImpl.replacingStateManager this=" + this + " sm=" + sm + " with same EC");
                // This is a race condition when makePersistent or makeTransactional is called on the same PC instance for the
                // same PM. It has been already set to this SM - just disconnect the other one. Return this SM so it won't be replaced.
                ((DNStateManager)sm).disconnect();
                return this;
            }

            throw myEC.getApiAdapter().getUserExceptionForException(Localiser.msg("026003"), null);
        }
        else if (pc == (savedState != null ? savedState.getPC() : null))
        {
            return null;
        }
        else
        {
            return sm;
        }
    }

    /**
     * Method that replaces the PC managed by this StateManager to be the supplied object.
     * This is used when we want to get an object for an id and create a Hollow object, and then validate against the datastore. 
     * This validation can pull in a new object graph from the datastore (e.g for an ODBMS).
     * @param pc The persistable to use
     */
    public void replaceManagedPC(Persistable pc)
    {
        if (pc == null)
        {
            return;
        }

        // Swap the StateManager on the objects
        replaceStateManager(pc, this);
        replaceStateManager(myPC, null);

        // Swap our object
        myPC = pc;

        // Put it in the cache in case the previous object was stored
        myEC.putObjectIntoLevel1Cache(this);
    }

    // -------------------------- Lifecycle Methods ---------------------------

    /**
     * Tests whether this object is dirty.
     * Instances that have been modified, deleted, or newly made persistent in the current transaction return true.
     * Transient nontransactional instances return false (JDO spec).
     * @see Persistable#dnMakeDirty(String fieldName)
     * @param pc the calling persistable instance
     * @return true if this instance has been modified in current transaction.
     */
    public boolean isDirty(Persistable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        return myLC.isDirty();
    }

    /**
     * Tests whether this object is transactional.
     *
     * Instances that respect transaction boundaries return true.  These
     * instances include transient instances made transactional as a result of
     * being the target of a makeTransactional method call; newly made
     * persistent or deleted persistent instances; persistent instances read
     * in data store transactions; and persistent instances modified in
     * optimistic transactions.
     * <P>
     * Transient nontransactional instances return false.
     *
     * @param pc the calling persistable instance
     * @return true if this instance is transactional.
     */
    public boolean isTransactional(Persistable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        return myLC.isTransactional();
    }

    /**
     * Tests whether this object is persistent.
     * Instances whose state is stored in the data store return true.
     * Transient instances return false.
     * @param pc the calling persistable instance
     * @return true if this instance is persistent.
     */
    public boolean isPersistent(Persistable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        return myLC.isPersistent();
    }

    /**
     * Tests whether this object has been newly made persistent.
     * Instances that have been made persistent in the current transaction
     * return true.
     * <P>
     * Transient instances return false.
     * @param pc the calling persistable instance
     * @return true if this instance was made persistent
     * in the current transaction.
     */
    public boolean isNew(Persistable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        return myLC.isNew();
    }

    public boolean isDeleted()
    {
        return isDeleted(myPC);
    }

    /**
     * Tests whether this object has been deleted.
     * Instances that have been deleted in the current transaction return true.
     * <P>Transient instances return false.
     * @param pc the calling persistable instance
     * @return true if this instance was deleted in the current transaction.
     */
    public boolean isDeleted(Persistable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        return myLC.isDeleted();
    }

    // -------------------------- Version handling ----------------------------

    /** 
     * Return the object representing the version of the calling instance.
     * @param pc the calling persistable instance
     * @return the object representing the version of the calling instance
     */
    public Object getVersion(Persistable pc)
    {
        if (pc == myPC)
        {
            if (transactionalVersion == null && cmd.isVersioned())
            {
                // If the object is versioned and no version is loaded (e.g obtained via findObject without loading fields) and in a state where we need it then pull in the version
                VersionMetaData vermd = cmd.getVersionMetaDataForClass();
                if (vermd != null && vermd.getStrategy() != VersionStrategy.NONE)
                {
                    if (myLC.stateType() == LifeCycleState.P_CLEAN || myLC.stateType() == LifeCycleState.HOLLOW) // Add other states?
                    {
                        if (vermd.getMemberName() != null)
                        {
                            loadFieldFromDatastore(cmd.getMetaDataForMember(vermd.getMemberName()).getAbsoluteFieldNumber());
                        }
                        else
                        {
                            loadFieldsFromDatastore(new int[] {});
                        }
                    }
                }
            }

            return transactionalVersion;
        }
        return null;
    }

    /**
     * Method to return if the version is loaded.
     * If the class represented is not versioned then returns true
     * @return Whether it is loaded.
     */
    public boolean isVersionLoaded()
    {
        if (cmd.isVersioned())
        {
            return transactionalVersion != null;
        }
        // No version required, so return true
        return true;
    }

    /**
     * Method to return the current version of the managed object.
     * @return The version
     */
    public Object getVersion()
    {
        return getVersion(myPC);
    }

    /**
     * Return the transactional version of the managed object.
     * @return Version of the managed instance at this point in the transaction
     */
    public Object getTransactionalVersion()
    {
        return getTransactionalVersion(myPC);
    }

    // -------------------------- Field Handling Methods ------------------------------

    /**
     * Method to clear all fields of the object.
     */
    public void clearFields()
    {
        try
        {
            getCallbackHandler().preClear(myPC);
        }
        finally
        {
            clearFieldsByNumbers(cmd.getAllMemberPositions());
            clearDirtyFlags();

            if (myEC.getStoreManager() instanceof ObjectReferencingStoreManager)
            {
                // For datastores that manage the object reference
                ((ObjectReferencingStoreManager)myEC.getStoreManager()).notifyObjectIsOutdated(this);
            }
            persistenceFlags = Persistable.LOAD_REQUIRED;
            myPC.dnReplaceFlags();

            getCallbackHandler().postClear(myPC);
        }
    }

    /**
     * Method to clear all fields that are not part of the primary key of the object.
     */
    public void clearNonPrimaryKeyFields()
    {
        try
        {
            getCallbackHandler().preClear(myPC);
        }
        finally
        {
            int[] nonPKMemberPosns = cmd.getNonPKMemberPositions();

            // Unset owner of any SCO wrapper so if the user holds on to a wrapper it doesn't affect the datastore
            int[] nonPkScoFields = ClassUtils.getFlagsSetTo(cmd.getSCOMutableMemberFlags(), ClassUtils.getFlagsSetTo(loadedFields, nonPKMemberPosns, true), true);
            if (nonPkScoFields != null)
            {
                provideFields(nonPkScoFields, new UnsetOwnerFieldManager());
            }

            clearFieldsByNumbers(nonPKMemberPosns);
            clearDirtyFlags(nonPKMemberPosns);

            if (myEC.getStoreManager() instanceof ObjectReferencingStoreManager)
            {
                // For datastores that manage the object reference
                ((ObjectReferencingStoreManager)myEC.getStoreManager()).notifyObjectIsOutdated(this);
            }

            persistenceFlags = Persistable.LOAD_REQUIRED;
            myPC.dnReplaceFlags();

            getCallbackHandler().postClear(myPC);
        }
    }

    /**
     * Method to clear all loaded flags on the object.
     * Note that the contract of this method implies, especially for object database backends, that the memory form
     * of the object is outdated.
     * Thus, for features like implicit saving of dirty object subgraphs should be switched off for this PC, even if the 
     * object actually looks like being dirty (because it is being changed to null values).
     */
    public void clearLoadedFlags()
    {
        if (myEC.getStoreManager() instanceof ObjectReferencingStoreManager)
        {
            // For datastores that manage the object reference
            ((ObjectReferencingStoreManager)myEC.getStoreManager()).notifyObjectIsOutdated(this);
        }

        persistenceFlags = Persistable.LOAD_REQUIRED;
        myPC.dnReplaceFlags();
        ClassUtils.clearFlags(loadedFields);
    }

    /**
     * The StateManager uses this method to supply the value of dnFlags to the associated persistable instance.
     * @param pc the calling Persistable instance
     * @return the value of dnFlags to be stored in the Persistable instance
     */
    public byte replacingFlags(Persistable pc)
    {
        // If this is a clone, return READ_WRITE_OK.
        if (pc != myPC)
        {
            return Persistable.READ_WRITE_OK;
        }
        return persistenceFlags;
    }

    /**
     * Method to return the current value of a particular field.
     * @param fieldNumber Number of field
     * @return The value of the field
     */
    public Object provideField(int fieldNumber)
    {
        return provideField(myPC, fieldNumber);
    }

    /**
     * Method to retrieve the value of a field from the PC object. Assumes that it is loaded.
     * @param pc The PC object
     * @param fieldNumber Number of field
     * @return The value of the field
     */
    protected Object provideField(Persistable pc, int fieldNumber)
    {
        Object obj;
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadLock();
            }

            FieldManager prevFM = currFM;
            currFM = new SingleValueFieldManager();
            try
            {
                pc.dnProvideField(fieldNumber);
                obj = currFM.fetchObjectField(fieldNumber);
            }
            finally
            {
                currFM = prevFM;
            }
        }
        finally
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadUnlock();
            }
        }

        return obj;
    }

    /**
     * Called from the StoreManager after StoreManager.update() is called to obtain updated values from the Persistable associated with this StateManager.
     * @param fieldNumbers An array of field numbers to be updated by the Store
     * @param fm The updated values are stored in this object. This object is only valid for the duration of this call.
     */
    public void provideFields(int fieldNumbers[], FieldManager fm)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadLock();
            }

            FieldManager prevFM = currFM;
            currFM = fm;
            try
            {
                // This will respond by calling this.providedXXXFields() with the value of the field
                myPC.dnProvideFields(fieldNumbers);
            }
            finally
            {
                currFM = prevFM;
            }
        }
        finally
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadUnlock();
            }
        }
    }

    // -------------------------- replacingXXXField Methods ----------------------------

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setBooleanField(Persistable pc, int fieldNumber, boolean currentValue, boolean newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, newValue ? Boolean.TRUE : Boolean.FALSE, true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || currentValue != newValue)
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, newValue ? Boolean.TRUE : Boolean.FALSE);

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, newValue ? Boolean.TRUE : Boolean.FALSE, true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setByteField(Persistable pc, int fieldNumber, byte currentValue, byte newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, Byte.valueOf(newValue), true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || currentValue != newValue)
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, Byte.valueOf(newValue));

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, Byte.valueOf(newValue), true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setCharField(Persistable pc, int fieldNumber, char currentValue, char newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, Character.valueOf(newValue), true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || currentValue != newValue)
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, Character.valueOf(newValue));

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, Character.valueOf(newValue), true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setDoubleField(Persistable pc, int fieldNumber, double currentValue, double newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, Double.valueOf(newValue), true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || currentValue != newValue)
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, Double.valueOf(newValue));

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, Double.valueOf(newValue), true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setFloatField(Persistable pc, int fieldNumber, float currentValue, float newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, Float.valueOf(newValue), true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || currentValue != newValue)
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, Float.valueOf(newValue));

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, Float.valueOf(newValue), true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setIntField(Persistable pc, int fieldNumber, int currentValue, int newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, Integer.valueOf(newValue), true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || currentValue != newValue)
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, Integer.valueOf(newValue));

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, Integer.valueOf(newValue), true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setLongField(Persistable pc, int fieldNumber, long currentValue, long newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, Long.valueOf(newValue), true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || currentValue != newValue)
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, Long.valueOf(newValue));

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, Long.valueOf(newValue), true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setShortField(Persistable pc, int fieldNumber, short currentValue, short newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, Short.valueOf(newValue), true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || currentValue != newValue)
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, Short.valueOf(newValue));

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, Short.valueOf(newValue), true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setStringField(Persistable pc, int fieldNumber, String currentValue, String newValue)
    {
        if (pc != myPC)
        {
            replaceField(pc, fieldNumber, newValue, true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            if (!loadedFields[fieldNumber] || !(currentValue == null ? (newValue == null) : currentValue.equals(newValue)))
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, currentValue);
                    }
                }

                updateField(pc, fieldNumber, newValue);

                if (!myEC.getTransaction().isActive())
                {
                    myEC.processNontransactionalUpdate();
                }
            }
        }
        else
        {
            replaceField(pc, fieldNumber, newValue, true);
        }
    }

    /**
     * This method is called by the associated Persistable when the corresponding mutator method (setXXX()) is called on the Persistable.
     * @param pc the calling Persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setObjectField(Persistable pc, int fieldNumber, Object currentValue, Object newValue)
    {
        if (currentValue != null && currentValue != newValue && currentValue instanceof Persistable)
        {
            // Where the object is embedded, remove the owner from its old value since it is no longer managed by this StateManager
            DNStateManager currentSM = myEC.findStateManager(currentValue);
            if (currentSM != null && currentSM.isEmbedded())
            {
                myEC.removeEmbeddedOwnerRelation(this, fieldNumber, currentSM);
            }
        }

        if (pc != myPC)
        {
            // Clone
            replaceField(pc, fieldNumber, newValue, true);
            disconnectClone(pc);
        }
        else if (myLC != null)
        {
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                // Not got version but should have
                loadUnloadedFieldsInFetchPlanAndVersion();
            }

            boolean loadedOldValue = false;
            Object oldValue = currentValue;
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            ClassLoaderResolver clr = myEC.getClassLoaderResolver();
            RelationType relationType = mmd.getRelationType(clr);

            // Remove this object from L2 cache since now dirty to avoid potential problems
            myEC.removeObjectFromLevel2Cache(myID);

            if (!loadedFields[fieldNumber] && currentValue == null)
            {
                // Updating value of a field that isnt currently loaded
                if (myEC.getManageRelations() &&
                    (relationType == RelationType.ONE_TO_ONE_BI || relationType == RelationType.MANY_TO_ONE_BI))
                {
                    // Managed relation field, so load old value
                    loadField(fieldNumber);
                    loadedOldValue = true;
                    oldValue = provideField(fieldNumber);
                }
                else if (relationType != RelationType.NONE && newValue == null && (mmd.isDependent() || mmd.isCascadeRemoveOrphans()))
                {
                    // Field being nulled and is dependent so load the existing value so it can be deleted
                    loadField(fieldNumber);
                    loadedOldValue = true;
                    oldValue = provideField(fieldNumber);
                }
                else if (cmd.getSCOContainerMemberFlags()[fieldNumber] && !mmd.isSerialized())
                {
                    // Situation such as SCO field being replaced by "new HashMap(...)" so old value not loaded yet
                    loadField(fieldNumber);
                    oldValue = provideField(fieldNumber);
                }
            }

            // Check equality of old and new values
            boolean equal = false;
            boolean equalButContainerRefChanged = false;
            if (oldValue == null && newValue == null)
            {
                equal = true;
            }
            else if (oldValue != null && newValue != null)
            {
                if (RelationType.isRelationSingleValued(relationType))
                {
                    // Persistable field so compare object equality
                    // See JDO2 [5.4] "The JDO implementation must not use the application's hashCode and equals methods 
                    // from the persistence-capable classes except as needed to implement the Collections Framework" 
                    if (oldValue == newValue)
                    {
                        equal = true;
                    }
                }
                else
                {
                    // Not 1-1/N-1 relation so compare using equals()
                    if (oldValue.equals(newValue))
                    {
                        equal = true;
                        if (oldValue instanceof SCOContainer && ((SCOContainer)oldValue).getValue() != newValue && !(newValue instanceof SCO))
                        {
                            // Field value is container and equal (i.e same elements/keys/values) BUT different container reference so need to update the delegate in SCO wrappers
                            equalButContainerRefChanged = true;
                        }
                    }
                }
            }

            // Update the field
            boolean needsSCOUpdating = false;
            if (!loadedFields[fieldNumber] || !equal || equalButContainerRefChanged || mmd.hasArray())
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE && relationType == RelationType.NONE)
                {
                    String key = DNStateManager.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, oldValue);
                    }
                }

                // Either field isn't loaded, or has changed, or is an array.
                // We include arrays here since we have no way of knowing if the array element has changed except if the user sets the array field. 
                // See JDO2 spec [6.3] that the application should replace the value with its current value.
                if (oldValue instanceof SCO)
                {
                    if (oldValue instanceof SCOContainer)
                    {
                        // Make sure container values are loaded
                        ((SCOContainer)oldValue).load();
                    }
                    if (!equalButContainerRefChanged)
                    {
                        ((SCO)oldValue).unsetOwner();
                    }
                }
                if (newValue instanceof SCO)
                {
                    SCO sco = (SCO) newValue;
                    Object owner = sco.getOwner();
                    if (owner != null)
                    {
                        throw myEC.getApiAdapter().getUserExceptionForException(Localiser.msg("026007", sco.getFieldName(), owner), null);
                    }
                }

                updateField(pc, fieldNumber, newValue);

                if (cmd.getSCOMutableMemberFlags()[fieldNumber] && !(newValue instanceof SCO))
                {
                    // Need to wrap this field change
                    needsSCOUpdating = true;
                }
            }
            else if (loadedOldValue)
            {
                // We've updated the value with the old value (when retrieving it above), so put the new value back again
                updateField(pc, fieldNumber, newValue);
            }

            if (!equal)
            {
                if (RelationType.isBidirectional(relationType)&& myEC.getManageRelations())
                {
                    // Managed Relationships - add the field to be managed so we can analyse its value at flush
                    myEC.getRelationshipManager(this).relationChange(fieldNumber, oldValue, newValue);
                }
                if (myEC.operationQueueIsActive())
                {
                    myEC.addOperationToQueue(new UpdateMemberOperation(this, fieldNumber, newValue, oldValue));
                }
            }
            else if (equalButContainerRefChanged)
            {
                // Previous value was a SCO wrapper and this new value is equal to the old wrappers delegate so swap the delegate for the original wrapper to this new value
                ((SCOContainer)oldValue).setValue(newValue);
                newValue = oldValue; // Point to the old wrapper
                needsSCOUpdating = false;
                replaceField(fieldNumber, oldValue);
            }

            if (needsSCOUpdating)
            {
                // Wrap with SCO so we can detect future updates
                newValue = SCOUtils.wrapAndReplaceSCOField(this, fieldNumber, newValue, oldValue, true);
            }

            if (oldValue != null && newValue == null)
            {
                if (RelationType.isRelationSingleValued(relationType) && (mmd.isDependent() || mmd.isCascadeRemoveOrphans()))
                {
                    // Persistable field being nulled, so delete previous persistable value if in a position to be deleted
                    if (myEC.getApiAdapter().isPersistent(oldValue))
                    {
                        // TODO Queue this when using optimistic txns, so the old value could be assigned somewhere else
                        NucleusLogger.PERSISTENCE.debug(Localiser.msg("026026", oldValue, mmd.getFullFieldName()));
                        myEC.deleteObjectInternal(oldValue);
                    }
                }
            }

            if (!myEC.getTransaction().isActive())
            {
                myEC.processNontransactionalUpdate();
            }
        }
        else
        {
            replaceField(pc, fieldNumber, newValue, true);
        }
    }

    /**
     * Convenience method to perform the update of a field value when a setter is invoked.
     * Called by setXXXField methods.
     * @param pc The PC object
     * @param fieldNumber The field number
     * @param value The new value
     */
    protected void updateField(Persistable pc, int fieldNumber, Object value)
    {
        boolean wasDirty = dirty;

        // If we're writing a field in the process of inserting it must be due to dnPreStore().
        // We haven't actually done the INSERT yet so we don't want to mark anything as dirty, which would make us want to do an UPDATE later.
        if ((flags&FLAG_INSERTING) == 0 && (flags&FLAG_INSERTING_CALLBACKS) == 0)
        {
            if (!wasDirty) // (only do it for first dirty event).
            {
                // Call any lifecycle listeners waiting for this event
                getCallbackHandler().preDirty(myPC);
            }

            // Update lifecycle state as required
            transitionWriteField();

            dirty = true;
            dirtyFields[fieldNumber] = true;
            loadedFields[fieldNumber] = true;
        }

        replaceField(pc, fieldNumber, value, true);

        if (dirty && !wasDirty) // (only do it for first dirty event).
        {
            // Call any lifecycle listeners waiting for this event
            getCallbackHandler().postDirty(myPC);
        }

        // TODO replaceField typically does a markDirty above, so need to catch those cases and avoid multiple calls to it
        if ((flags&FLAG_INSERTING) == 0 && (flags&FLAG_INSERTING_CALLBACKS) == 0 && !isFlushing() && !(myLC.isTransactional() && !myLC.isPersistent()))
        {
            // Not during flush, and not transactional-transient, and not inserting - so mark as dirty
            myEC.markDirty(this, true);
        }
    }

    /**
     * Method to change the value of a field in the PC object.
     * @param pc The PC object
     * @param fieldNumber Number of field
     * @param value The new value of the field
     */
    protected void replaceField(Persistable pc, int fieldNumber, Object value)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadLock();
            }

            // Update the field in our PC object
            FieldManager prevFM = currFM;
            currFM = new SingleValueFieldManager();

            try
            {
                currFM.storeObjectField(fieldNumber, value);
                pc.dnReplaceField(fieldNumber);
            }
            finally
            {
                currFM = prevFM;
            }

            if (containsAssociatedValue(MEMBER_VALUE_STORED_PREFIX + fieldNumber))
            {
                removeAssociatedValue(MEMBER_VALUE_STORED_PREFIX + fieldNumber);
            }
        }
        finally
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadUnlock();
            }
        }
    }

    /**
     * Method to disconnect any cloned persistence capable objects from their StateManager.
     * @param pc The Persistable object
     * @return Whether the object was disconnected.
     */
    protected boolean disconnectClone(Persistable pc)
    {
        if (isDetaching())
        {
            return false;
        }
        if (pc != myPC)
        {
            NucleusLogger.PERSISTENCE.warn("StateManager.disconnectClone : Please let developers know how you got this - myPC=" + StringUtils.toJVMIDString(myPC) + " other=" + StringUtils.toJVMIDString(pc), new Exception());
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("026001", StringUtils.toJVMIDString(pc), this));
            }

            // Reset dnFlags in the clone to Persistable.READ_WRITE_OK and clear its state manager.
            pc.dnReplaceFlags();
            replaceStateManager(pc, null);
            return true;
        }

        return false;
    }

    /**
     * Convenience method to retrieve the detach state from the passed StateManager's object.
     * @param sm StateManager
     */
    public void retrieveDetachState(DNStateManager sm)
    {
        if (sm.getObject() instanceof Detachable)
        {
            ((StateManagerImpl)sm).flags |= FLAG_RETRIEVING_DETACHED_STATE;

            ((Detachable)sm.getObject()).dnReplaceDetachedState();

            ((StateManagerImpl)sm).flags &= ~FLAG_RETRIEVING_DETACHED_STATE;
        }
    }

    /**
     * Convenience method to reset the detached state in the current object.
     */
    public void resetDetachState()
    {
        if (getObject() instanceof Detachable)
        {
            flags |= FLAG_RESETTING_DETACHED_STATE;
            try
            {
                ((Detachable)getObject()).dnReplaceDetachedState();
            }
            finally
            {
                flags &= ~FLAG_RESETTING_DETACHED_STATE;
            }
        }
    }

    /**
     * Method to update the "detached state" in the detached object to obtain the "detached state" from the detached object, or to reset it (to null).
     * @param pc The Persistable being updated
     * @param currentState The current state values
     * @return The detached state to assign to the object
     */
    public Object[] replacingDetachedState(Detachable pc, Object[] currentState)
    {
        if ((flags&FLAG_RESETTING_DETACHED_STATE) != 0)
        {
            return null;
        }
        else if ((flags&FLAG_RETRIEVING_DETACHED_STATE) != 0)
        {
            // Retrieving the detached state from the detached object
            // Don't need the id or version since they can't change
            BitSet theLoadedFields = (BitSet)currentState[2];
            for (int i = 0; i < this.loadedFields.length; i++)
            {
                this.loadedFields[i] = theLoadedFields.get(i);
            }

            BitSet theModifiedFields = (BitSet)currentState[3];
            for (int i = 0; i < dirtyFields.length; i++)
            {
                dirtyFields[i] = theModifiedFields.get(i);
            }
            setVersion(currentState[1]);
            return currentState;
        }
        else
        {
            // Updating the detached state in the detached object with our state
            Object[] state = new Object[4];
            state[0] = myID;
            state[1] = getVersion(myPC);

            // Loaded fields
            BitSet loadedState = new BitSet();
            for (int i = 0; i < loadedFields.length; i++)
            {
                if (loadedFields[i])
                {
                    loadedState.set(i);
                }
                else
                {
                    loadedState.clear(i);
                }
            }
            state[2] = loadedState;

            // Modified fields
            BitSet modifiedState = new BitSet();
            for (int i = 0; i < dirtyFields.length; i++)
            {
                if (dirtyFields[i])
                {
                    modifiedState.set(i);
                }
                else
                {
                    modifiedState.clear(i);
                }
            }
            state[3] = modifiedState;

            return state;
        }
    }

    /**
     * Marks the given field dirty.
     * @param fieldNumber The no of field to mark as dirty. 
     */
    public void makeDirty(int fieldNumber)
    {
        if ((flags & FLAG_DELETING) == 0/* && !isDeleted() */)
        {
            // Mark dirty unless in the process of being deleted
            boolean wasDirty = preWriteField(fieldNumber);
            postWriteField(wasDirty);

            if (isEmbedded())
            {
                EmbeddedOwnerRelation embeddedRel = myEC.getOwnerInformationForEmbedded(this);
                if (embeddedRel != null)
                {
                    // Notify owner that this embedded object has just changed
                    StateManagerImpl ownerSM = (StateManagerImpl)embeddedRel.getOwnerSM();

                    if ((ownerSM.flags&FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER)==0)
                    {
                        ownerSM.makeDirty(embeddedRel.getOwnerMemberNum());
                    }
                }
            }
        }
    }

    /**
     * Mark the associated persistable field dirty.
     * @param pc the calling persistable instance
     * @param fieldName the name of the field
     */
    public void makeDirty(Persistable pc, String fieldName)
    {
        if (!disconnectClone(pc))
        {
            int fieldNumber = cmd.getAbsolutePositionOfMember(fieldName);
            if (fieldNumber == -1)
            {
                throw myEC.getApiAdapter().getUserExceptionForException(Localiser.msg("026002", fieldName, cmd.getFullClassName()), null);
            }

            makeDirty(fieldNumber);
        }
    }

    // -------------------------- Object Identity Methods -----------------------------

    /**
     * Accessor for the internal object id of the object we are managing.
     * This will return the "id" if it has been set, otherwise a temporary id (IdentityReference).
     * @return The internal object id
     */
    @Override
    public Object getInternalObjectId()
    {
        if (myID != null)
        {
            return myID;
        }

        if (myInternalID == null)
        {
            // Assign a temporary internal "id" based on the object itself until our real identity is assigned
            myInternalID = new IdentityReference(myPC);
        }
        return myInternalID;
    }

    /**
     * Return the object representing the persistent identity of the calling instance.
     * According to the JDO specification, if the persistent identity is being changed in the current transaction, 
     * this method returns the persistent identify as of the beginning of the transaction.
     * In DataNucleus we don't allow change of identity so this is always the same as the result of getExternalObjectId(Persistable).
     * @param pc the calling Persistable instance
     * @return the object representing the persistent identity of the calling instance
     */
    @Override
    public Object getObjectId(Persistable pc)
    {
        if (disconnectClone(pc))
        {
            return null;
        }

        try
        {
            return getExternalObjectId();
        }
        catch (NucleusException ne)
        {
            // This method can be called from user-facing methods (e.g JDOHelper.getObjectId) so wrap any exception with API variant
            throw myEC.getApiAdapter().getApiExceptionForNucleusException(ne);
        }
    }

    /**
     * Return the object representing the persistent identity of the calling instance.  
     * If the persistent identity is being changed in the current transaction, this method returns the current identity as changed in the transaction. 
     * In DataNucleus we don't allow change of identity so this is always the same as the result of getObjectId(Persistable).
     * @param pc the calling Persistable instance
     * @return the object representing the persistent identity of the calling instance
     */
    @Override
    public Object getTransactionalObjectId(Persistable pc)
    {
        return getObjectId(pc);
    }

    @Override
    public Object getExternalObjectId()
    {
        if (myID != null)
        {
            // Id already set and we don't support changing id, so return it
            return myID;
        }
        else if (isEmbedded())
        {
            // Embedded object has no id
            return null;
        }

        // Ensure that the id is generated
        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            if (!isFlushing())
            {
                // Flush any datastore changes so that myID is set by the time we return
                if (!isFlushedNew() && (flags&FLAG_INSERTING) == 0 && (flags&FLAG_INSERTING_CALLBACKS) == 0 && myLC.stateType() == LifeCycleState.P_NEW)
                {
                    if (getStoreManager().isValueGenerationStrategyDatastoreAttributed(cmd, -1))
                    {
                        flush();
                    }
                }
            }
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            // Note that we always create a new application identity since it is mutable and we can't allow the user to change it. 
            // The only drawback of this is that we *must* have the relevant fields set when this method is called, so that the identity can be generated.
            if (!isFlushing())
            {
                // Flush any datastore changes so that we have all necessary fields populated only if the datastore generates the field numbers
                if (!isFlushedNew() && (flags&FLAG_INSERTING) == 0 && (flags&FLAG_INSERTING_CALLBACKS) == 0 && myLC.stateType() == LifeCycleState.P_NEW)
                {
                    int[] pkMemberNumbers = cmd.getPKMemberPositions();
                    for (int i = 0; i < pkMemberNumbers.length; i++)
                    {
                        if (getStoreManager().isValueGenerationStrategyDatastoreAttributed(cmd, pkMemberNumbers[i]))
                        {
                            flush();
                            break;
                        }
                    }
                }
            }
        }

        return myID;
    }

    /**
     * Utility to set the identity for the Persistable object.
     * Creates the identity instance if the required PK field(s) are all already set (by the user, or by a value-strategy). 
     * If the identity is set in the datastore (sequence, autoassign, etc) then this will not set the identity.
     * @param afterPreStore Whether preStore has (just) been invoked
     */
    private void setIdentity(boolean afterPreStore)
    {
        if (isEmbedded())
        {
            // Embedded objects don't have an "identity"
            return;
        }

        boolean idSet = false;
        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            if (cmd.getDatastoreIdentityMetaData() == null || !getStoreManager().isValueGenerationStrategyDatastoreAttributed(cmd, -1))
            {
                // Assumed to be set
                myID = myEC.newObjectId(cmd.getFullClassName(), myPC);
                idSet = true;
            }
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            boolean idSetInDatastore = false;
            int[] pkMemberNumbers = cmd.getPKMemberPositions();
            for (int i=0;i<pkMemberNumbers.length;i++)
            {
                int pkMemberNumber = pkMemberNumbers[i];
                AbstractMemberMetaData pkMmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkMemberNumber);

                if (getStoreManager().isValueGenerationStrategyDatastoreAttributed(cmd, pkMemberNumber))
                {
                    idSetInDatastore = true;
                    break;
                }

                if (pkMemberNumbers.length == 1 && afterPreStore)
                {
                    // Only 1 PK field and after preStore callback, so check that it is set
                    try
                    {
                        if (this.provideField(pkMemberNumber) == null)
                        {
                            // Cannot have sole PK field as null
                            throw new NucleusUserException(Localiser.msg("026017", cmd.getFullClassName(), pkMmd.getName())).setFatal();
                        }
                    }
                    catch (Exception e)
                    {
                        // StateManager maybe not yet connected to the object
                        return;
                    }
                }
            }

            if (!idSetInDatastore)
            {
                // Not generating the identity in the datastore so set it now
                myID = myEC.newObjectId(cmd.getFullClassName(), myPC);
                idSet = true;
            }
        }

        if (myInternalID != myID && myID != null && (idSet || myEC.getApiAdapter().getIdForObject(myPC) != null))
        {
            // Update the id with the ExecutionContext if it is changing
            myEC.replaceObjectId(myPC, myInternalID, myID);

            myInternalID = myID;
        }
    }

    /**
     * If the id is obtained after inserting the object into the database, set new a new id for persistent classes (for example, increment).
     * @param id the id received from the datastore
     */
    public void setPostStoreNewObjectId(Object id)
    {
        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            if (IdentityUtils.isDatastoreIdentity(id))
            {
                // Provided the id direct
                myID = id;
            }
            else
            {
                // id "key" value provided
                myID = myEC.getNucleusContext().getIdentityManager().getDatastoreId(cmd.getFullClassName(), id);
            }
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            try
            {
                myID = null;

                int[] pkMemberNumbers = cmd.getPKMemberPositions();
                for (int i=0;i<pkMemberNumbers.length;i++)
                {
                    AbstractMemberMetaData pkMmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkMemberNumbers[i]);
                    if (pkMmd.isPrimaryKey() && getStoreManager().isValueGenerationStrategyDatastoreAttributed(cmd, pkMemberNumbers[i]))
                    {
                        //replace the value of the id, but before convert the value to the field type if needed
                        replaceField(myPC, pkMemberNumbers[i], TypeConversionHelper.convertTo(id, pkMmd.getType()), false);
                    }
                }
            }
            catch (Exception e)
            {
                NucleusLogger.PERSISTENCE.error(e);
            }
            finally
            {
                myID = myEC.getNucleusContext().getIdentityManager().getApplicationId(getObject(), cmd);
            }
        }

        if (myInternalID != myID && myID != null)
        {
            // Update the id with the ExecutionContext if it is changing
            myEC.replaceObjectId(myPC, myInternalID, myID);

            myInternalID = myID;
        }
    }

    // --------------------------- Load Field Methods --------------------------

    /**
     * Convenience method to load the passed field values.
     * Loads the fields using any required fetch plan and calls dnPostLoad() as appropriate.
     * @param fv Field Values to load (including any fetch plan to use when loading)
     */
    public void loadFieldValues(FieldValues fv)
    {
        // Fetch the required fields using any defined fetch plan
        FetchPlanForClass origFetchPlan = myFP;
        FetchPlan loadFetchPlan = fv.getFetchPlanForLoading();
        if (loadFetchPlan != null)
        {
            myFP = loadFetchPlan.getFetchPlanForClass(cmd);
        }

        boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
        if (loadedFields.length == 0)
        {
            // Class has no fields so since we are loading from scratch just call postLoad
            callPostLoad = true;
        }

        fv.fetchFields(this);
        if (callPostLoad && areFieldsLoaded(myFP.getMemberNumbers()))
        {
            postLoad();
        }

        // Reinstate the original (PM) fetch plan
        myFP = origFetchPlan;
    }

    /**
     * Fetch the specified fields from the database.
     * @param fieldNumbers the numbers of the field(s) to fetch.
     */
    protected void loadSpecifiedFields(int[] fieldNumbers)
    {
        if (myEC.getApiAdapter().isDetached(myPC))
        {
            // Nothing to do since we're detached
            return;
        }

        // Try from the L2 cache first
        int[] unloadedFieldNumbers = loadFieldsFromLevel2Cache(fieldNumbers);
        if (unloadedFieldNumbers != null)
        {
            if (!isEmbedded()) // Embedded should always retrieve all in one go, so likely to be unnecessary
            {
                loadFieldsFromDatastore(unloadedFieldNumbers);
                updateLevel2CacheForFields(unloadedFieldNumbers);
            }
        }
    }

    /**
     * Convenience method to load the specified field if not loaded.
     * @param fieldNumber Absolute field number
     */
    public void loadField(int fieldNumber)
    {
        if (loadedFields[fieldNumber])
        {
            // Already loaded
            return;
        }
        loadSpecifiedFields(new int[]{fieldNumber});
    }

    @Override
    public boolean loadStoredField(int fieldNumber)
    {
        String assocValueKey = MEMBER_VALUE_STORED_PREFIX + fieldNumber;
        boolean hasStored = containsAssociatedValue(assocValueKey);
        if (hasStored)
        {
            // Instantiate object using the stored "id" value, and remove associated value
            Object memberValue = getAssociatedValue(assocValueKey);
            Object member = myEC.findObject(memberValue, false);
            DNStateManager memberSM = (member != null) ? myEC.findStateManager(member) : null;
            if (memberSM != null && !memberSM.isDeleted()) // Only apply the stored value if found and not (being) deleted
            {
                AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
                if (mmd.getType().isAssignableFrom(member.getClass()))
                {
                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        NucleusLogger.PERSISTENCE.debug(Localiser.msg("026038", mmd.getName(), this));
                    }
                    replaceField(myPC, fieldNumber, member);
                }
            }
            removeAssociatedValue(assocValueKey);
            updateLevel2CacheForFields(new int[] {fieldNumber});
        }
        return hasStored;
    }

    @Override
    public void loadUnloadedRelationFields()
    {
        int[] fieldsConsidered = cmd.getRelationMemberPositions(myEC.getClassLoaderResolver());
        int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, fieldsConsidered, false);
        if (fieldNumbers == null || fieldNumbers.length == 0)
        {
            // All loaded so return
            return;
        }

        if (preDeleteLoadedFields != null && ((myLC.isDeleted() && myEC.isFlushing()) || (flags&FLAG_DELETING) != 0))
        {
            // During deletion process so we know what is really loaded so only load if necessary
            fieldNumbers = ClassUtils.getFlagsSetTo(preDeleteLoadedFields, fieldNumbers, false);
        }

        if (fieldNumbers != null && fieldNumbers.length > 0)
        {
            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
            int[] unloadedFieldNumbers = loadFieldsFromLevel2Cache(fieldNumbers);
            if (unloadedFieldNumbers != null)
            {
                loadFieldsFromDatastore(unloadedFieldNumbers);
            }

            int[] secondClassMutableFieldNumbers = cmd.getSCOMutableMemberPositions();
            for (int i=0;i<secondClassMutableFieldNumbers.length;i++)
            {
                // Make sure all SCO lazy-loaded relation fields have contents loaded
                AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(secondClassMutableFieldNumbers[i]);
                if (mmd.getRelationType(myEC.getClassLoaderResolver()) != RelationType.NONE)
                {
                    SingleValueFieldManager sfv = new SingleValueFieldManager();
                    provideFields(new int[]{secondClassMutableFieldNumbers[i]}, sfv);
                    Object value = sfv.fetchObjectField(i);
                    if (value instanceof SCOContainer)
                    {
                        ((SCOContainer)value).load();
                    }
                }
            }

            updateLevel2CacheForFields(fieldNumbers);
            if (callPostLoad && areFieldsLoaded(myFP.getMemberNumbers())) // If a FK is in the STORED cache then wont be marked as loaded yet
            {
                postLoad();
            }
        }
    }

    @Override
    public void loadUnloadedFields()
    {
        int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, cmd.getAllMemberPositions(), false);
        if (fieldNumbers == null || fieldNumbers.length == 0)
        {
            // All loaded so return
            return;
        }

        if (preDeleteLoadedFields != null && ((myLC.isDeleted() && myEC.isFlushing()) || (flags&FLAG_DELETING) != 0))
        {
            // During deletion process so we know what is really loaded so only load if necessary
            fieldNumbers = ClassUtils.getFlagsSetTo(preDeleteLoadedFields, fieldNumbers, false);
        }

        if (fieldNumbers != null && fieldNumbers.length > 0)
        {
            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
            int[] unloadedFieldNumbers = loadFieldsFromLevel2Cache(fieldNumbers);
            if (unloadedFieldNumbers != null)
            {
                loadFieldsFromDatastore(unloadedFieldNumbers);
            }

            // Make sure all SCO lazy-loaded fields have contents loaded
            int[] secondClassMutableFieldNumbers = cmd.getSCOMutableMemberPositions();
            for (int i=0;i<secondClassMutableFieldNumbers.length;i++)
            {
                SingleValueFieldManager sfv = new SingleValueFieldManager();
                provideFields(new int[]{secondClassMutableFieldNumbers[i]}, sfv);
                Object value = sfv.fetchObjectField(i);
                if (value instanceof SCOContainer)
                {
                    ((SCOContainer)value).load();
                }
            }

            updateLevel2CacheForFields(fieldNumbers);
            if (callPostLoad && areFieldsLoaded(myFP.getMemberNumbers())) // If a FK is in the STORED cache then wont be marked as loaded yet
            {
                postLoad();
            }
        }
    }

    @Override
    public void loadUnloadedFieldsInFetchPlan()
    {
        int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, myFP.getMemberNumbers(), false);
        if (fieldNumbers != null && fieldNumbers.length > 0)
        {
            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
            int[] unloadedFieldNumbers = loadFieldsFromLevel2Cache(fieldNumbers);
            if (unloadedFieldNumbers != null)
            {
                loadFieldsFromDatastore(unloadedFieldNumbers);
                updateLevel2CacheForFields(unloadedFieldNumbers);
            }
            if (callPostLoad && areFieldsLoaded(myFP.getMemberNumbers())) // If a FK is in the STORED cache then wont be marked as loaded yet
            {
                postLoad();
            }
        }
    }

    /**
     * Fetch from the database all fields in current fetch plan that are not currently loaded as well as the version. Called by lifecycle transitions.
     */
    protected void loadUnloadedFieldsInFetchPlanAndVersion()
    {
        if (!cmd.isVersioned())
        {
            loadUnloadedFieldsInFetchPlan();
        }
        else
        {
            int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, myFP.getMemberNumbers(), false);
            if (fieldNumbers == null)
            {
                fieldNumbers = new int[0];
            }

            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
            int[] unloadedFieldNumbers = loadFieldsFromLevel2Cache(fieldNumbers);
            if (unloadedFieldNumbers != null)
            {
                loadFieldsFromDatastore(unloadedFieldNumbers);
                updateLevel2CacheForFields(unloadedFieldNumbers);
            }
            if (callPostLoad && fieldNumbers.length > 0 && areFieldsLoaded(myFP.getMemberNumbers())) // If a FK is in the STORED cache then wont be marked as loaded yet
            {
                postLoad();
            }
        }
    }

    @Override
    public void loadUnloadedFieldsOfClassInFetchPlan(FetchPlan fetchPlan)
    {
        FetchPlanForClass fpc = fetchPlan.getFetchPlanForClass(this.cmd);
        int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, fpc.getMemberNumbers(), false);
        if (fieldNumbers != null && fieldNumbers.length > 0)
        {
            boolean callPostLoad = fpc.isToCallPostLoadFetchPlan(this.loadedFields);
            int[] unloadedFieldNumbers = loadFieldsFromLevel2Cache(fieldNumbers);
            if (unloadedFieldNumbers != null)
            {
                loadFieldsFromDatastore(unloadedFieldNumbers);
                updateLevel2CacheForFields(unloadedFieldNumbers);
            }
            if (callPostLoad && areFieldsLoaded(myFP.getMemberNumbers())) // If a FK is in the STORED cache then wont be marked as loaded yet
            {
                postLoad();
            }
        }
    }

    @Override
    public void refreshFieldsInFetchPlan()
    {
        int[] fieldNumbers = myFP.getMemberNumbers();
        if (fieldNumbers != null && fieldNumbers.length > 0)
        {
            clearDirtyFlags(fieldNumbers);
            ClassUtils.clearFlags(loadedFields, fieldNumbers);
            markPKFieldsAsLoaded(); // Can't refresh PK fields!

            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);

            // Refresh the fetch plan fields in this object
            setTransactionalVersion(null); // Make sure that the version is reset upon fetch
            loadFieldsFromDatastore(fieldNumbers);

            if (cmd.hasRelations(myEC.getClassLoaderResolver()))
            {
                // Check for cascade refreshes to related objects
                for (int i=0;i<fieldNumbers.length;i++)
                {
                    AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumbers[i]);
                    RelationType relationType = fmd.getRelationType(myEC.getClassLoaderResolver());
                    if (relationType != RelationType.NONE && fmd.isCascadeRefresh())
                    {
                        // Need to refresh the related field object(s)
                        Object value = provideField(fieldNumbers[i]);
                        if (value != null)
                        {
                            if (fmd.hasContainer())
                            {
                                // TODO This should replace the SCO wrapper with a new one, or reload the wrapper
                                ApiAdapter api = getExecutionContext().getApiAdapter();
                                ContainerHandler containerHandler = myEC.getTypeManager().getContainerHandler(fmd.getType());
                                for (Object object : containerHandler.getAdapter(value))
                                {
                                    if (api.isPersistable(object))
                                    {
                                        getExecutionContext().refreshObject(object);
                                    }
                                }
                            }
                            else if (value instanceof Persistable)
                            {
                                // Refresh any PC fields
                                myEC.refreshObject(value);
                            }
                        }
                    }
                }
            }

            updateLevel2CacheForFields(fieldNumbers);

            if (callPostLoad)
            {
                postLoad();
            }

            getCallbackHandler().postRefresh(myPC);
        }
    }

    @Override
    public void refreshLoadedFields()
    {
        int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, myFP.getMemberNumbers(), true);

        if (fieldNumbers != null && fieldNumbers.length > 0)
        {
            clearDirtyFlags();
            ClassUtils.clearFlags(loadedFields);
            markPKFieldsAsLoaded(); // Can't refresh PK fields!

            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
            loadFieldsFromDatastore(fieldNumbers);
            updateLevel2CacheForFields(fieldNumbers);

            if (callPostLoad)
            {
                postLoad();
            }
        }
    }

    @Override
    public boolean isLoaded(int fieldNumber)
    {
        return isLoaded(myPC, fieldNumber);
    }

    /**
     * Return true if the field is cached in the calling instance; in this implementation we always return true.
     * If the field is not loaded, it will be loaded as a side effect of the call to this method. 
     * If it is in the default fetch group, the default fetch group, including this field, will be loaded.
     * @param pc the calling Persistable instance
     * @param fieldNumber the absolute field number
     * @return always returns true (this implementation)
     */
    public boolean isLoaded(Persistable pc, int fieldNumber)
    {
        try
        {
            if (disconnectClone(pc))
            {
                return true;
            }

            boolean checkRead = true;
            boolean beingDeleted = false;

            if ((myLC.isDeleted() && myEC.isFlushing()) || (flags&FLAG_DELETING) != 0)
            {
                // Bypass "read-field" check when deleting, or when marked for deletion and flushing
                checkRead = false;
                beingDeleted = true;
            }
            if (checkRead)
            {
                transitionReadField(loadedFields[fieldNumber]);
            }

            if (!loadedFields[fieldNumber])
            {
                // Field not loaded, so load it
                if (isEmbedded())
                {
                    // TODO When we have nested embedded objects that can have relations to non-embedded then this needs to change
                    // Embedded object so we assume that all was loaded before (when it was read)
                    return true;
                }

                if (beingDeleted && preDeleteLoadedFields != null && preDeleteLoadedFields[fieldNumber])
                {
                    // Field was loaded prior to starting delete so just return true
                    return true;
                }
                else if (!beingDeleted && myFP.hasMember(fieldNumber))
                {
                    if (!loadStoredField(fieldNumber))
                    {
                        // Load rest of FetchPlan if this is part of it (and not in the process of deletion)
                        loadUnloadedFieldsInFetchPlan();
                        if (!loadedFields[fieldNumber])
                        {
                            // Case where the field is marked for storing, and was stored during the loadSpecifiedFields call, so load it now
                            loadStoredField(fieldNumber);
                        }
                    }
                }
                else
                {
                    if (!loadStoredField(fieldNumber))
                    {
                        // Just load this field
                        loadSpecifiedFields(new int[] {fieldNumber});
                        if (!loadedFields[fieldNumber])
                        {
                            // Case where the field is marked for storing, and was stored during the loadSpecifiedFields call, so load it now
                            loadStoredField(fieldNumber);
                        }
                    }
                }
            }

            return true;
        }
        catch (NucleusException ne)
        {
            NucleusLogger.PERSISTENCE.warn("Exception thrown by StateManager.isLoaded for field=" + fieldNumber + " of " + this + " : " + StringUtils.getMessageFromRootCauseOfThrowable(ne));

            // Convert into an exception suitable for the current API since this is called from a user update of a field
            throw myEC.getApiAdapter().getApiExceptionForNucleusException(ne);
        }
    }

    /**
     * Convenience method to change the value of a field that is assumed loaded.
     * Will mark the object/field as dirty if it isn't previously. If the object is deleted then does nothing.
     * Doesn't cater for embedded fields.
     * *** Only for use in management of relations. ***
     * @param fieldNumber Number of field
     * @param newValue The new value
     */
    public void replaceFieldValue(int fieldNumber, Object newValue)
    {
        if (myLC.isDeleted())
        {
            // Object is deleted so do nothing
            return;
        }

        boolean currentWasDirty = preWriteField(fieldNumber);
        replaceField(myPC, fieldNumber, newValue, true);
        postWriteField(currentWasDirty);
    }

    /**
     * Method to change the value of a particular field and not mark it dirty.
     * @param fieldNumber Number of field
     * @param value New value
     */
    public void replaceField(int fieldNumber, Object value)
    {
        replaceField(myPC, fieldNumber, value, false);
    }

    /**
     * Method to change the value of a particular field and mark it dirty.
     * @param fieldNumber Number of field
     * @param value New value
     */
    public void replaceFieldMakeDirty(int fieldNumber, Object value)
    {
        replaceField(myPC, fieldNumber, value, true);
    }

    /**
     * Method to change the value of a field in the PC object.
     * Adds on handling for embedded fields to the superclass handler.
     * @param pc The PC object
     * @param fieldNumber Number of field
     * @param value The new value of the field
     * @param makeDirty Whether to make the field dirty while replacing its value (in embedded owners)
     */
    protected void replaceField(Persistable pc, int fieldNumber, Object value, boolean makeDirty)
    {
        if (isEmbedded())
        {
            // Inform the owner of this change in the embedded object if necessary
            EmbeddedOwnerRelation ownerRel = myEC.getOwnerInformationForEmbedded(this);
            if (ownerRel != null)
            {
                // Notify the owner of this embedded object that it has just changed
                // We do this before we actually change the object so we can compare with the old value
                StateManagerImpl ownerSM = (StateManagerImpl) ownerRel.getOwnerSM();
                AbstractMemberMetaData ownerMmd = ownerSM.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(ownerRel.getOwnerMemberNum());
                if (ownerMmd.getCollection() != null)
                {
                    // PC Object embedded in collection element
                    Object ownerField = ownerSM.provideField(ownerRel.getOwnerMemberNum());
                    if (ownerField instanceof SCOCollection)
                    {
                        ((SCOCollection)ownerField).updateEmbeddedElement(myPC, fieldNumber, value, makeDirty);
                    }
                }
                else if (ownerMmd.getMap() != null)
                {
                    // PC Object embedded in map key / value
                    Object ownerField = ownerSM.provideField(ownerRel.getOwnerMemberNum());
                    if (ownerField instanceof SCOMap)
                    {
                        if (ownerRel.getObjectType() == PersistableObjectType.EMBEDDED_MAP_KEY_PC)
                        {
                            ((SCOMap)ownerField).updateEmbeddedKey(myPC, fieldNumber, value, makeDirty);
                        }
                        if (ownerRel.getObjectType() == PersistableObjectType.EMBEDDED_MAP_VALUE_PC)
                        {
                            ((SCOMap)ownerField).updateEmbeddedValue(myPC, fieldNumber, value, makeDirty);
                        }
                    }
                }
                else
                {
                    // PC Object embedded in PC object
                }

                if ((ownerSM.flags&FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER)==0)
                {
                    // Update the owner when one of our fields have changed, EXCEPT when they have just notified us of our owner field!
                    if (makeDirty)
                    {
                        ownerSM.makeDirty(ownerRel.getOwnerMemberNum());
                    }
                }
            }

            // Replace the field value in this embedded object
            replaceField(pc, fieldNumber, value);
        }
        else
        {
            // Non-embedded TODO Why don't we mark as dirty if non-tx ? Maybe need P_NONTRANS_DIRTY
            if (makeDirty && !myLC.isDeleted() && myEC.getTransaction().isActive())
            {
                // Mark dirty (if not being deleted)
                boolean wasDirty = preWriteField(fieldNumber);
                replaceField(pc, fieldNumber, value);
                postWriteField(wasDirty);
            }
            else
            {
                replaceField(pc, fieldNumber, value);
            }
        }
    }

    /**
     * Called from the StoreManager to refresh data in the Persistable object associated with this StateManager.
     * Typically called as a result of a query, so as to populate the query results into the associated persistable object(s).
     * @param fieldNumbers Field numbers to be refreshed from the store data
     * @param fm The updated values are stored in this object. This object is only valid for the duration of this call.
     * @param replaceWhenDirty Whether to replace the fields when they are dirty here
     */
    public void replaceFields(int fieldNumbers[], FieldManager fm, boolean replaceWhenDirty)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadLock();
            }

            FieldManager prevFM = currFM;
            currFM = fm;

            try
            {
                int[] fieldsToReplace = fieldNumbers;
                if (!replaceWhenDirty)
                {
                    int numberToReplace = fieldNumbers.length;
                    for (int i=0;i<fieldNumbers.length;i++)
                    {
                        if (dirtyFields[fieldNumbers[i]])
                        {
                            numberToReplace--;
                        }
                    }
                    if (numberToReplace > 0 && numberToReplace != fieldNumbers.length)
                    {
                        fieldsToReplace = new int[numberToReplace];
                        int n = 0;
                        for (int i=0;i<fieldNumbers.length;i++)
                        {
                            if (!dirtyFields[fieldNumbers[i]])
                            {
                                fieldsToReplace[n++] = fieldNumbers[i];
                            }
                        }
                    }
                    else if (numberToReplace == 0)
                    {
                        fieldsToReplace = null;
                    }
                }

                if (fieldsToReplace != null)
                {
                    myPC.dnReplaceFields(fieldsToReplace);
                }
            }
            finally
            {
                currFM = prevFM;
            }
        }
        finally
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadUnlock();
            }
        }
    }

    /**
     * Called from the StoreManager to refresh data in the Persistable object associated with this StateManager.
     * @param fieldNumbers An array of field numbers to be refreshed by the Store
     * @param fm The updated values are stored in this object. This object is only valid for the duration of this call.
     */
    public void replaceFields(int fieldNumbers[], FieldManager fm)
    {
        replaceFields(fieldNumbers, fm, true);
    }

    /**
     * Called from the StoreManager to refresh data in the Persistable object associated with this StateManager. 
     * Only fields that are not currently loaded are refreshed
     * @param fieldNumbers An array of field numbers to be refreshed by the Store
     * @param fm The updated values are stored in this object. This object is only valid for the duration of this call.
     */
    public void replaceNonLoadedFields(int fieldNumbers[], FieldManager fm)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadLock();
            }

            FieldManager prevFM = currFM;
            currFM = fm;

            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
            try
            {
                int[] fieldsToReplace = ClassUtils.getFlagsSetTo(loadedFields, fieldNumbers, false);
                if (fieldsToReplace != null && fieldsToReplace.length > 0)
                {
                    myPC.dnReplaceFields(fieldsToReplace);
                }
            }
            finally
            {
                currFM = prevFM;
            }
            if (callPostLoad && areFieldsLoaded(myFP.getMemberNumbers()))
            {
                // The fetch plan is now loaded so fire off any necessary post load
                postLoad();
            }
        }
        finally
        {
            if (myEC.getMultithreaded())
            {
                myEC.threadUnlock();
            }
        }
    }

    /**
     * Method to replace all loaded SCO fields with wrappers.
     * If the loaded field already uses a SCO wrapper nothing happens to that field.
     */
    public void replaceAllLoadedSCOFieldsWithWrappers()
    {
        boolean[] scoMutableFieldFlags = cmd.getSCOMutableMemberFlags();
        for (int i=0;i<scoMutableFieldFlags.length;i++)
        {
            if (scoMutableFieldFlags[i] && loadedFields[i])
            {
                Object value = provideField(i);
                if (!(value instanceof SCO))
                {
                    SCOUtils.wrapSCOField(this, i, value, true);
                }
            }
        }
    }

    /**
     * Method to replace all loaded SCO fields that have wrappers with their value.
     * If the loaded field doesn't have a SCO wrapper nothing happens to that field.
     */
    public void replaceAllLoadedSCOFieldsWithValues()
    {
        boolean[] scoMutableFieldFlags = cmd.getSCOMutableMemberFlags();
        for (int i=0;i<scoMutableFieldFlags.length;i++)
        {
            if (scoMutableFieldFlags[i] && loadedFields[i])
            {
                Object value = provideField(i);
                if (value instanceof SCO)
                {
                    SCOUtils.unwrapSCOField(this, i, (SCO) value);
                }
            }
        }
    }

    /**
     * Method to update the "owner-field" in an embedded object with the owner object.
     * TODO Likely this should be moved into a replaceField method, or maybe Managed Relationships.
     * @param fieldNumber The field number
     * @param value The value to initialise the wrapper with (if any)
     */
    public void updateOwnerFieldInEmbeddedField(int fieldNumber, Object value)
    {
        if (value != null)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            RelationType relationType = mmd.getRelationType(myEC.getClassLoaderResolver());
            if (RelationType.isRelationSingleValued(relationType) && mmd.getEmbeddedMetaData() != null && mmd.getEmbeddedMetaData().getOwnerMember() != null)
            {
                // Embedded field, so assign the embedded/serialised object "owner-field" if specified
                DNStateManager subSM = myEC.findStateManager(value);
                int ownerAbsFieldNum = subSM.getClassMetaData().getAbsolutePositionOfMember(mmd.getEmbeddedMetaData().getOwnerMember());
                if (ownerAbsFieldNum >= 0)
                {
                    flags |= FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER;
                    subSM.replaceFieldMakeDirty(ownerAbsFieldNum, myPC);
                    flags &= ~FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER;
                }
            }
        }
    }

    // ------------------------- Lifecycle Methods -----------------------------

    /**
     * Method to make the object persistent.
     */
    public void makePersistent()
    {
        if (myLC.isDeleted() && !myEC.getNucleusContext().getApiAdapter().allowPersistOfDeletedObject())
        {
            // API doesn't allow re-persist of deleted objects
            return;
        }

        if ((flags&FLAG_INSERTING) != 0 || (flags&FLAG_INSERTING_CALLBACKS) != 0)
        {
            // Already making persistent
            return;
        }

        if (myEC.operationQueueIsActive())
        {
            myEC.addOperationToQueue(new PersistOperation(this));
        }
        if (dirty && !myLC.isDeleted() && myLC.isTransactional() && myEC.isDelayDatastoreOperationsEnabled())
        {
            // Already provisionally persistent, but delaying til commit so just re-run reachability to bring in any new objects that are now reachable
            if (cmd.hasRelations(myEC.getClassLoaderResolver()))
            {
                provideFields(cmd.getAllMemberPositions(), new PersistFieldManager(this, false));
            }
            return;
        }

        getCallbackHandler().prePersist(myPC);

        if (isFlushedNew())
        {
            // With CompoundIdentity bidir relations when the SM is created for this object ("initialiseForPersistentNew") the persist
            // of the PK PC fields can cause the flush of this object, and so it is already persisted by the time we get here
            registerTransactional();
            return;
        }

        if (isEmbedded())
        {
            // Object is embedded so return
            return;
        }

        if (myID == null)
        {
            // Assign identity when not yet assigned (is this possible?)
            setIdentity(false);
        }

        dirty = true;

        if (myEC.isDelayDatastoreOperationsEnabled())
        {
            // Delaying datastore flush til later
            myEC.markDirty(this, false);
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("026028", StringUtils.toJVMIDString(myPC)));
            }
            registerTransactional();

            if (myLC.isTransactional() && myLC.isDeleted())
            {
                // Re-persist of a previously deleted object
                myLC = myLC.transitionMakePersistent(this);
            }

            if (cmd.hasRelations(myEC.getClassLoaderResolver()))
            {
                // Run reachability on all fields of this PC - JDO2 [12.6.7]
                provideFields(cmd.getAllMemberPositions(), new PersistFieldManager(this, false));
            }
        }
        else
        {
            // Persist the object and all reachables
            internalMakePersistent();
            registerTransactional();
        }
    }

    /**
     * Method to persist the object to the datastore.
     */
    private void internalMakePersistent()
    {
        flags |= FLAG_INSERTING;
        boolean[] tmpDirtyFields = dirtyFields.clone();
        try
        {
            getCallbackHandler().preStore(myPC); // This comes after setting the INSERTING flag so we know we are inserting it now
            if (myID == null)
            {
                setIdentity(true); // Just in case user is setting it in preStore
            }

            // in InstanceLifecycleEvents this object could get dirty if a field is changed in preStore/postCreate; clear dirty flags to make sure this object will not be flushed again
            clearDirtyFlags();

            getStoreManager().getPersistenceHandler().insertObject(this);
            setFlushedNew(true);

            getCallbackHandler().postStore(myPC);
        }
        catch (NotYetFlushedException ex)
        {
            // can happen on cyclic relationships with RDBMS; if not yet flushed error, we rollback dirty fields, so we can retry inserting
            dirtyFields = tmpDirtyFields;
            myEC.markDirty(this, false);
            dirty = true;
            throw ex; // throw exception, so the owning relationship will mark its FK to update later
        }
        finally
        {
            flags &= ~FLAG_INSERTING;
            flags &= ~FLAG_INSERTING_CALLBACKS;
        }
    }

    /**
     * Method to change the object state to transactional.
     */
    public void makeTransactional()
    {
        preStateChange();
        try
        {
            if (myLC == null)
            {
                // Initialise the StateManager in T_CLEAN state
                final DNStateManager thisSM = this;
                myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.T_CLEAN);

                try
                {
                    // Set SM reference in PC - can be done only after myLC is set to delegate validation to the LC and objectId verified for uniqueness
                    replaceStateManager(myPC, thisSM);
                }
                catch (SecurityException e)
                {
                    throw new NucleusUserException(e.getMessage());
                }
                catch (NucleusException ne)
                {
                    if (myEC.findStateManager(myEC.getObjectFromCache(myID)) == this)
                    {
                        myEC.removeStateManagerFromCache(this);
                    }
                    throw ne;
                }

                flags |= FLAG_RESTORE_VALUES;
            }
            else
            {
                myLC = myLC.transitionMakeTransactional(this, true);
            }
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * Method to change the object state to transient.
     * @param state Object containing the state of any fetchplan processing
     */
    public void makeTransient(FetchPlanState state)
    {
        if ((flags&FLAG_MAKING_TRANSIENT) != 0)
        {
            return; // In the process of becoming transient
        }

        try
        {
            flags |= FLAG_MAKING_TRANSIENT;

            if (state == null)
            {
                // No FetchPlan in use so just unset the owner of all loaded SCO fields
                int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, cmd.getSCOMutableMemberPositions(), true);
                if (fieldNumbers != null && fieldNumbers.length > 0)
                {
                    provideFields(fieldNumbers, new UnsetOwnerFieldManager());
                }
            }
            else
            {
                // Make all loaded SCO fields transient appropriate to this fetch plan
                loadUnloadedFieldsInFetchPlan();
                int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, cmd.getAllMemberPositions(), true);
                if (fieldNumbers != null && fieldNumbers.length > 0)
                {
                    // TODO Fix this to just access the fields of the FieldManager yet this actually does a replaceField
                    replaceFields(fieldNumbers, new MakeTransientFieldManager(this, cmd.getSCOMutableMemberFlags(), myFP, state));
                }
            }

            preStateChange();
            try
            {
                myLC = myLC.transitionMakeTransient(this, state != null, myEC.isRunningDetachAllOnCommit());
            }
            finally
            {
                postStateChange();
            }
        }
        finally
        {
            flags &= ~FLAG_MAKING_TRANSIENT;
        }
    }

    /**
     * Make the managed object transient as a result of persistence-by-reachability when run at commit time.
     * The object was brought into persistence by reachability but found to not be needed at commit time.
     * Here we delete it from persistence (since it will have been persisted/flushed to the datastore), and
     * then we migrate the lifecycle to transient (which disconnects this StateManager).
     */
    public void makeTransientForReachability()
    {
        // Call any lifecycle listeners waiting for this event.
        getCallbackHandler().preDelete(myPC);

        // Delete the object from the datastore (includes reachability)
        internalDeletePersistent();

        // Call any lifecycle listeners waiting for this event.
        getCallbackHandler().postDelete(myPC);

        // Update lifecycle state to TRANSIENT
        dirty = true;
        preStateChange();
        try
        {
            myLC = myLC.transitionMakeTransient(this, false, true);
        }
        finally
        {
            postStateChange();
        }
    }

    /**
     * Method to detach this object.
     * If the object is detachable then it will be migrated to DETACHED state, otherwise will migrate to TRANSIENT. Used by "DetachAllOnCommit"/"DetachAllOnRollback"
     * @param state State for the detachment process
     */
    public void detach(FetchPlanState state)
    {
        if (myEC == null)
        {
            return;
        }

        ApiAdapter api = myEC.getApiAdapter();
        if (myLC.isDeleted() || api.isDetached(myPC) || isDetaching())
        {
            // Already deleted, detached or being detached
            return;
        }

        // Check if detachable ... if so then we detach a copy, otherwise we return a transient copy
        boolean detachable = api.isDetachable(myPC);
        if (detachable)
        {
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("010009", StringUtils.toJVMIDString(myPC), "" + state.getCurrentFetchDepth()));
            }

            // Call any "pre-detach" listeners
            getCallbackHandler().preDetach(myPC);
        }

        try
        {
            setDetaching(true);

            String detachedState = myEC.getNucleusContext().getConfiguration().getStringProperty(PropertyNames.PROPERTY_DETACH_DETACHED_STATE).toLowerCase();
            if (detachedState.equals("all"))
            {
                loadUnloadedFields();
            }
            else if (detachedState.equals("loaded"))
            {
                // Do nothing since just using currently loaded fields
            }
            else
            {
                // Using fetch-groups, so honour detachmentOptions for loading/unloading
                if ((myEC.getFetchPlan().getDetachmentOptions() & FetchPlan.DETACH_LOAD_FIELDS) != 0)
                {
                    // Load any unloaded fetch-plan fields
                    loadUnloadedFieldsInFetchPlan();
                }
                if ((myEC.getFetchPlan().getDetachmentOptions() & FetchPlan.DETACH_UNLOAD_FIELDS) != 0)
                {
                    // Unload any loaded fetch-plan fields that aren't in the current fetch plan
                    unloadNonFetchPlanFields();

                    // Remove the values from the detached object - not required by the spec
                    int[] unloadedFields = ClassUtils.getFlagsSetTo(loadedFields, cmd.getAllMemberPositions(), false);
                    if (unloadedFields != null && unloadedFields.length > 0)
                    {
                        Persistable dummyPC = myPC.dnNewInstance(this);
                        myPC.dnCopyFields(dummyPC, unloadedFields);
                        replaceStateManager(dummyPC, null);
                    }
                }
            }

            // Detach all (loaded) fields in the FetchPlan
            FieldManager detachFieldManager = new DetachFieldManager(this, cmd.getSCOMutableMemberFlags(), myFP, state, false);
            for (int i = 0; i < loadedFields.length; i++)
            {
                if (loadedFields[i])
                {
                    try
                    {
                        // Just fetch the field since we are usually called in postCommit() so dont want to update it
                        detachFieldManager.fetchObjectField(i);
                    }
                    catch (EndOfFetchPlanGraphException eofpge)
                    {
                        Object value = provideField(i);
                        if (api.isPersistable(value))
                        {
                            // PC field beyond end of graph
                            DNStateManager valueSM = myEC.findStateManager(value);
                            if (!api.isDetached(value) && !(valueSM != null && valueSM.isDetaching()))
                            {
                                // Field value is not detached or being detached so unload it
                                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                                {
                                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("026032", IdentityUtils.getPersistableIdentityForId(myID),
                                        cmd.getMetaDataForManagedMemberAtAbsolutePosition(i).getName()));
                                }
                                unloadField(i);
                            }
                        }
                        // TODO What if we have collection/map that includes some objects that are not detached?
                        // Currently we just leave as persistent etc but should we????
                        // The problem is that with 1-N bidir fields we could unload the field incorrectly
                    }
                }
            }

            if (detachable)
            {
                // Migrate the lifecycle state to DETACHED_CLEAN
                myLC = myLC.transitionDetach(this);

                // Update the object with its detached state
                myPC.dnReplaceFlags();
                ((Detachable)myPC).dnReplaceDetachedState();

                // Call any "post-detach" listeners
                getCallbackHandler().postDetach(myPC, myPC); // there is no copy, so give the same object

                Persistable toCheckPC = myPC;
                Object toCheckID = myID;
                disconnect();

                if (!toCheckPC.dnIsDetached())
                {
                    // Sanity check on the objects detached state
                    throw new NucleusUserException(Localiser.msg("026025", toCheckPC.getClass().getName(), toCheckID));
                }
            }
            else
            {
                // Warn the user since they selected detachAllOnCommit
                NucleusLogger.PERSISTENCE.warn(Localiser.msg("026031", IdentityUtils.getPersistableIdentityForId(myID)));

                // Make the object transient
                makeTransient(null);
            }
        }
        finally
        {
            setDetaching(false);
        }
    }

    /**
     * Method to make detached copy of this instance
     * If the object is detachable then the copy will be migrated to DETACHED state, otherwise will migrate the copy to TRANSIENT. 
     * Used by "ExecutionContext.detachObjectCopy()".
     * @param state State for the detachment process
     * @return the detached Persistable instance
     */
    public Persistable detachCopy(FetchPlanState state)
    {
        if (myLC.isDeleted())
        {
            throw new NucleusUserException(Localiser.msg("026023", myPC.getClass().getName(), myID));
        }
        if (myEC.getApiAdapter().isDetached(myPC))
        {
            throw new NucleusUserException(Localiser.msg("026024", myPC.getClass().getName(), myID));
        }

        if (dirty)
        {
            myEC.flushInternal(false);
        }
        if (isDetaching())
        {
            // Object in the process of detaching (recursive) so return the object which will be the detached object
            return getReferencedPC();
        }

        // Look for an existing detached copy
        DetachState detachState = (DetachState) state;
        DetachState.Entry existingDetached = detachState.getDetachedCopyEntry(myPC);

        Persistable detachedPC;
        if (existingDetached == null)
        {
            // No existing detached copy - create new one
            detachedPC = myPC.dnNewInstance(this);
            detachState.setDetachedCopyEntry(myPC, detachedPC);
        }
        else
        {
            // Found one - if it's sufficient for current FetchPlanState, return it immediately
            detachedPC = (Persistable) existingDetached.getDetachedCopyObject();
            if (existingDetached.checkCurrentState())
            {
                return detachedPC;
            }

            // Need to process the detached copy using current FetchPlanState
        }

        myEC.setAttachDetachReferencedObject(this, detachedPC);

        // Check if detachable ... if so then we detach a copy, otherwise we return a transient copy
        boolean detachable = myEC.getApiAdapter().isDetachable(myPC);

        // make sure a detaching PC is not read by another thread while we are detaching
        Object referencedPC = getReferencedPC();
        synchronized (referencedPC)
        {
            int[] detachFieldNums = getFieldsNumbersToDetach();
            if (detachable)
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    int[] fieldsToLoad = null;
                    if ((myEC.getFetchPlan().getDetachmentOptions() & FetchPlan.DETACH_LOAD_FIELDS) != 0)
                    {
                        fieldsToLoad = ClassUtils.getFlagsSetTo(loadedFields, myFP.getMemberNumbers(), false);
                    }
                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("010010", StringUtils.toJVMIDString(myPC), 
                        "" + state.getCurrentFetchDepth(), StringUtils.toJVMIDString(detachedPC),
                        StringUtils.intArrayToString(detachFieldNums), StringUtils.intArrayToString(fieldsToLoad)));
                }

                // Call any "pre-detach" listeners
                getCallbackHandler().preDetach(myPC);
            }

            try
            {
                setDetaching(true);

                // Handle any field loading/unloading before the detach
                if ((myEC.getFetchPlan().getDetachmentOptions() & FetchPlan.DETACH_LOAD_FIELDS) != 0)
                {
                    // Load any unloaded fetch-plan fields
                    loadUnloadedFieldsInFetchPlan();
                }

                if (myLC == myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.HOLLOW) ||
                    myLC == myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_NONTRANS))
                {
                    // Migrate any HOLLOW/P_NONTRANS to P_CLEAN etc
                    myLC = myLC.transitionReadField(this, true);
                }

                // Create a SM for our copy object
                DNStateManager smDetachedPC = new StateManagerImpl(myEC, cmd);
                smDetachedPC.initialiseForDetached(detachedPC, getExternalObjectId(), getVersion(myPC));
                myEC.setAttachDetachReferencedObject(smDetachedPC, myPC);

                // If detached copy already existed, take note of fields previously loaded
                if (existingDetached != null)
                {
                    smDetachedPC.retrieveDetachState(smDetachedPC);
                }

                smDetachedPC.replaceFields(detachFieldNums, new DetachFieldManager(this, cmd.getSCOMutableMemberFlags(), myFP, state, true));

                myEC.setAttachDetachReferencedObject(smDetachedPC, null);
                if (detachable)
                {
                    // Update the object with its detached state - not to be confused with the "state" object above
                    detachedPC.dnReplaceFlags();
                    ((Detachable)detachedPC).dnReplaceDetachedState();
                }
                else
                {
                    smDetachedPC.makeTransient(null);
                }

                // Remove its StateManager since now detached or transient
                replaceStateManager(detachedPC, null);
            }
            catch (Exception e)
            {
                // What could possibly be wrong here ? Log it and let the user provide a testcase, yeah right
                NucleusLogger.PERSISTENCE.warn("DETACH ERROR : Error thrown while detaching " +
                    StringUtils.toJVMIDString(myPC) + " (id=" + myID + "). Provide a testcase that demonstrates this", e);
            }
            finally
            {
                setDetaching(false);
                referencedPC = null;
            }

            if (detachable && !myEC.getApiAdapter().isDetached(detachedPC))
            {
                // Sanity check on the objects detached state
                throw new NucleusUserException(Localiser.msg("026025", detachedPC.getClass().getName(), myID));
            }

            if (detachable)
            {
                // Call any "post-detach" listeners
                getCallbackHandler().postDetach(myPC, detachedPC);
            }
        }
        return detachedPC;
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

    /**
     * Return an array of field numbers that must be included in the detached object
     * @return the field numbers array for detaching
     */
    private int[] getFieldsNumbersToDetach()
    {
        String detachedState = myEC.getNucleusContext().getConfiguration().getStringProperty(PropertyNames.PROPERTY_DETACH_DETACHED_STATE).toLowerCase();
        if (detachedState.equals("all"))
        {
            return cmd.getAllMemberPositions();
        }
        else if (detachedState.equals("loaded"))
        {
            return getLoadedFieldNumbers();
        }
        else if ((myEC.getFetchPlan().getDetachmentOptions() & FetchPlan.DETACH_UNLOAD_FIELDS) == 0)
        {
            if ((myEC.getFetchPlan().getDetachmentOptions() & FetchPlan.DETACH_LOAD_FIELDS) == 0)
            {
                // Return loaded fields
                return getLoadedFieldNumbers();
            }

            // Return all loaded plus any unloaded FP fields
            int[] fieldsToDetach = myFP.getMemberNumbers();
            int[] allFieldNumbers = cmd.getAllMemberPositions();
            int[] loadedFieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, allFieldNumbers, true);
            if (loadedFieldNumbers != null && loadedFieldNumbers.length > 0)
            {
                boolean[] flds = new boolean[allFieldNumbers.length];
                for (int i=0;i<fieldsToDetach.length;i++)
                {
                    flds[fieldsToDetach[i]] = true;
                }
                for (int i=0;i<loadedFieldNumbers.length;i++)
                {
                    flds[loadedFieldNumbers[i]] = true;
                }
                fieldsToDetach = ClassUtils.getFlagsSetTo(flds, true);
            }
            return fieldsToDetach;
        }
        else
        {
            if ((myEC.getFetchPlan().getDetachmentOptions() & FetchPlan.DETACH_LOAD_FIELDS) == 0)
            {
                // Return loaded fields that are in the FetchPlan
                return ClassUtils.getFlagsSetTo(loadedFields, myFP.getMemberNumbers(), true);
            }

            // Return FetchPlan fields
            return myFP.getMemberNumbers();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.StateManager#attach(java.lang.Object)
     */
    public void attach(Persistable detachedPC)
    {
        if (isAttaching())
        {
            return;
        }

        setAttaching(true);
        try
        {
            // Call any "pre-attach" listeners
            getCallbackHandler().preAttach(myPC);

            // Connect the transient object to a StateManager so we can get its values
            DNStateManager detachedSM = new StateManagerImpl(myEC, cmd);
            detachedSM.initialiseForDetached(detachedPC, myID, null);

            // Make sure the attached object is in the cache
            myEC.putObjectIntoLevel1Cache(this);

            int[] nonPKFieldNumbers = cmd.getNonPKMemberPositions();
            if (nonPKFieldNumbers != null && nonPKFieldNumbers.length > 0)
            {
                // Attach the (non-PK) fields from the transient
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("026035", IdentityUtils.getPersistableIdentityForId(getInternalObjectId()), StringUtils.intArrayToString(nonPKFieldNumbers)));
                }
                detachedSM.provideFields(nonPKFieldNumbers, new AttachFieldManager(this, cmd.getSCOMutableMemberFlags(), cmd.getNonPKMemberFlags(), true, true, false));
            }

            // Disconnect the transient object
            replaceStateManager(detachedPC, null);

            // Call any "post-attach" listeners
            getCallbackHandler().postAttach(myPC, myPC);
        }
        finally
        {
            setAttaching(false);
        }
    }

    /**
     * Method to attach the object managed by this StateManager.
     * @param embedded Whether it is embedded
     */
    public void attach(boolean embedded)
    {
        if (isAttaching())
        {
            return;
        }

        setAttaching(true);
        try
        {
            // Check if the object is already persisted
            boolean persistent = false;
            if (embedded)
            {
                persistent = true;
            }
            else
            {
                if (!myEC.getBooleanProperty(PropertyNames.PROPERTY_ATTACH_SAME_DATASTORE))
                {
                    // We cant assume that this object was detached from this datastore so we check it
                    try
                    {
                        locate();
                        persistent = true;
                    }
                    catch (NucleusObjectNotFoundException onfe)
                    {
                        // Not currently present!
                    }
                }
                else
                {
                    // Assumed detached from this datastore
                    persistent = true;
                }
            }

            // Call any "pre-attach" listeners
            getCallbackHandler().preAttach(myPC);

            // Retrieve the updated values from the detached object
            replaceStateManager(myPC, this);
            retrieveDetachState(this);

            if (!persistent)
            {
                // Persist the object into this datastore first
                makePersistent();
            }

            // Migrate the lifecycle state to persistent
            myLC = myLC.transitionAttach(this);

            // Make sure the attached object goes in the cache
            // [would not get cached when not changed if we didnt do this here]
            myEC.putObjectIntoLevel1Cache(this);

            int[] attachFieldNumbers = getFieldNumbersOfLoadedOrDirtyFields(loadedFields, dirtyFields);
            if (attachFieldNumbers != null)
            {
                // Only update the fields that were detached, and only update them if there are any to update
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("026035", IdentityUtils.getPersistableIdentityForId(getInternalObjectId()), StringUtils.intArrayToString(attachFieldNumbers)));
                }
                provideFields(attachFieldNumbers, new AttachFieldManager(this, cmd.getSCOMutableMemberFlags(), dirtyFields, persistent, true, false));
            }

            // Call any "post-attach" listeners
            getCallbackHandler().postAttach(myPC, myPC);
        }
        finally
        {
            setAttaching(false);
        }
    }

    /**
     * Method to attach a copy of the detached persistable instance and return the (attached) copy.
     * @param detachedPC the detached persistable instance to be attached
     * @param embedded Whether the object is stored embedded/serialised in another object
     * @return The attached copy
     */
    public Persistable attachCopy(Persistable detachedPC, boolean embedded)
    {
        if (isAttaching())
        {
            return myPC;
        }

        setAttaching(true);
        try
        {
            // Check if the object is already persisted
            boolean persistent = false;
            if (embedded)
            {
                persistent = true;
            }
            else
            {
                if (!myEC.getBooleanProperty(PropertyNames.PROPERTY_ATTACH_SAME_DATASTORE))
                {
                    // We cant assume that this object was detached from this datastore so we check it
                    try
                    {
                        locate();
                        persistent = true;
                    }
                    catch (NucleusObjectNotFoundException onfe)
                    {
                        // Not currently present!
                    }
                }
                else
                {
                    // Assumed detached from this datastore
                    persistent = true;
                }
            }

            // Call any "pre-attach" listeners
            getCallbackHandler().preAttach(detachedPC);

            if (myEC.getApiAdapter().isDeleted(detachedPC))
            {
                // The detached object has been deleted
                myLC = myLC.transitionDeletePersistent(this);
            }

            if (!myEC.getTransaction().getOptimistic() &&
                (myLC == myEC.getApiAdapter().getLifeCycleState(LifeCycleState.HOLLOW) || myLC == myEC.getApiAdapter().getLifeCycleState(LifeCycleState.P_NONTRANS)))
            {
                // Pessimistic txns and in HOLLOW/P_NONTRANS, so move to P_CLEAN
                // TODO Move this into the lifecycle state classes as a "transitionAttach"
                myLC = myLC.transitionMakeTransactional(this, persistent);
            }

            StateManagerImpl smDetachedPC = null;
            if (persistent)
            {
                // Attaching object that was detached from this datastore, so perform as update

                // Make sure that all non-container SCO fields are loaded so we can make valid dirty checks
                // for whether these fields have been updated whilst detached. The detached object doesnt know if the contents
                // have been changed.
                int[] noncontainerFieldNumbers = cmd.getSCONonContainerMemberPositions();
                int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, noncontainerFieldNumbers, false);
                if (fieldNumbers != null && fieldNumbers.length > 0)
                {
                    int[] unloadedFieldNumbers = loadFieldsFromLevel2Cache(fieldNumbers);
                    if (unloadedFieldNumbers != null)
                    {
                        loadFieldsFromDatastore(unloadedFieldNumbers);
                        updateLevel2CacheForFields(unloadedFieldNumbers);
                    }
                    // We currently don't call postLoad here since this is only called as part of attaching an object
                    // and consequently we just read to get the current (attached) values. 
                    // Could add a flag on input to allow postLoad
                }

                // Add a state manager to the detached PC so that we can retrieve its detached state
                smDetachedPC = new StateManagerImpl(myEC, cmd);
                smDetachedPC.initialiseForDetached(detachedPC, getExternalObjectId(), null);

                // Cross-reference the attached and detached objects for the attach process
                myEC.setAttachDetachReferencedObject(smDetachedPC, myPC);
                myEC.setAttachDetachReferencedObject(this, detachedPC);

                // Retrieve the updated values from the detached object
                retrieveDetachState(smDetachedPC);
            }
            else
            {
                // Attaching object that was detached from another datastore, so perform as replicate
                // Reset lifecycle to P_NEW since not persistent yet in this datastore
                myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_NEW);

                // Copy field values from detached to attached so we know what value will need inserting
                replaceStateManager(detachedPC, this);
                myPC.dnCopyFields(detachedPC, cmd.getAllMemberPositions());
                replaceStateManager(detachedPC, null);

                // Add a state manager to the detached PC so that we can retrieve its detached state
                smDetachedPC = new StateManagerImpl(myEC, cmd);
                smDetachedPC.initialiseForDetached(detachedPC, getExternalObjectId(), null);

                // Cross-reference the attached and detached objects for the attach process
                myEC.setAttachDetachReferencedObject(smDetachedPC, myPC);
                myEC.setAttachDetachReferencedObject(this, detachedPC);

                // Retrieve the updated values from the detached object
                retrieveDetachState(smDetachedPC);

                // Object is not yet persisted so make it persistent
                // Make sure all field values in the attach object are ready for inserts (but dont trigger any cascade attaches)
                internalAttachCopy(smDetachedPC, smDetachedPC.getLoadedFields(), smDetachedPC.getDirtyFields(), persistent, smDetachedPC.myVersion, false);

                makePersistent();
            }

            // Go through all related fields and attach them (including relationships)
            internalAttachCopy(smDetachedPC, smDetachedPC.getLoadedFields(), smDetachedPC.getDirtyFields(), persistent, smDetachedPC.myVersion, true);

            // Remove the state manager from the detached PC
            replaceStateManager(detachedPC, null);

            // Remove the cross-referencing now we have finished the attach process
            myEC.setAttachDetachReferencedObject(smDetachedPC, null);
            myEC.setAttachDetachReferencedObject(this, null);

            // Call any "post-attach" listeners
            getCallbackHandler().postAttach(myPC,detachedPC);
        }
        catch (NucleusException ne)
        {
            // Log any errors in the attach
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("026036", IdentityUtils.getPersistableIdentityForId(getInternalObjectId()), ne.getMessage()), ne);
            }
            throw ne;
        }
        finally
        {
            setAttaching(false);
        }
        return myPC;
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
     * Attach the fields for this object using the provided detached object.
     * This will attach all loaded plus all dirty fields.
     * @param detachedSM StateManager for the detached object.
     * @param loadedFields Fields that were detached with the object
     * @param dirtyFields Fields that have been modified while detached
     * @param persistent whether the object is already persistent
     * @param version the version
     * @param cascade Whether to cascade the attach to related fields
     */
    private void internalAttachCopy(DNStateManager detachedSM,
                                   boolean[] loadedFields,
                                   boolean[] dirtyFields,
                                   boolean persistent,
                                   Object version,
                                   boolean cascade)
    {
        // Need to take all loaded fields plus all modified fields
        // (maybe some werent detached but have been modified) and attach them
        int[] attachFieldNumbers = getFieldNumbersOfLoadedOrDirtyFields(loadedFields, dirtyFields);
        setVersion(version);
        if (attachFieldNumbers != null)
        {
            // Attach all dirty fields, and load other loaded fields
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("026035", IdentityUtils.getPersistableIdentityForId(getInternalObjectId()), StringUtils.intArrayToString(attachFieldNumbers)));
            }
            detachedSM.provideFields(attachFieldNumbers, new AttachFieldManager(this, cmd.getSCOMutableMemberFlags(), dirtyFields, persistent, cascade, true));
        }
    }

    /**
     * Method to delete the object from persistence.
     */
    public void deletePersistent()
    {
        if (!myLC.isDeleted())
        {
            if (myEC.isDelayDatastoreOperationsEnabled())
            {
                // Optimistic transactions, with all updates delayed til flush/commit
                if (myEC.operationQueueIsActive())
                {
                    myEC.addOperationToQueue(new DeleteOperation(this));
                }

                // Call any lifecycle listeners waiting for this event
                getCallbackHandler().preDelete(myPC);

                // Delay deletion until flush/commit so run reachability now to tag all reachable instances as necessary
                myEC.markDirty(this, false);

                // Reachability
                if (myLC.stateType() == LifeCycleState.P_CLEAN || 
                    myLC.stateType() == LifeCycleState.P_DIRTY || 
                    myLC.stateType() == LifeCycleState.HOLLOW ||
                    myLC.stateType() == LifeCycleState.P_NONTRANS ||
                    myLC.stateType() == LifeCycleState.P_NONTRANS_DIRTY)
                {
                    // Make sure all fields are loaded so we can perform reachability
                    loadUnloadedRelationFields();
                }

                flags |= FLAG_BECOMING_DELETED;

                // Run reachability for relations
                if (cmd.hasRelations(myEC.getClassLoaderResolver()))
                {
                    provideFields(cmd.getAllMemberPositions(), new DeleteFieldManager(this));
                }

                // Update lifecycle state (after running reachability since it will unload all fields)
                dirty = true;
                preStateChange();
                try
                {
                    // Keep "loadedFields" settings til after delete is complete to save reloading
                    preDeleteLoadedFields = new boolean[loadedFields.length];
                    System.arraycopy(loadedFields, 0, preDeleteLoadedFields, 0, loadedFields.length);

                    myLC = myLC.transitionDeletePersistent(this);
                }
                finally
                {
                    flags &= ~FLAG_BECOMING_DELETED;
                    postStateChange();
                }
            }
            else
            {
                // Datastore transactions, with all updates processed now

                // Call any lifecycle listeners waiting for this event.
                getCallbackHandler().preDelete(myPC);

                // Update lifecycle state
                dirty = true;
                preStateChange();
                try
                {
                    // Keep "loadedFields" settings til after delete is complete to save reloading
                    preDeleteLoadedFields = new boolean[loadedFields.length];
                    System.arraycopy(loadedFields, 0, preDeleteLoadedFields, 0, loadedFields.length);

                    myLC = myLC.transitionDeletePersistent(this);
                }
                finally
                {
                    postStateChange();
                }

                // TODO If this is an embedded object (cascaded from the owner) need to make sure we cascade as required

                // Delete the object from the datastore (includes reachability)
                internalDeletePersistent();

                // Call any lifecycle listeners waiting for this event.
                getCallbackHandler().postDelete(myPC);
            }
        }
    }

    public boolean becomingDeleted()
    {
        return (flags&FLAG_BECOMING_DELETED)>0;
    }

    /**
     * Validates whether the persistable instance exists in the datastore.
     * If the instance doesn't exist in the datastore, this method will fail raising a NucleusObjectNotFoundException. 
     * If the object is transactional then does nothing.
     * If the object has unloaded (non-SCO, non-PK) fetch plan fields then fetches them.
     * Else it checks the existence of the object in the datastore.
     */
    public void validate()
    {
        if (!myLC.isTransactional())
        {
            // Find all FetchPlan fields that are not PK, not SCO and still not loaded
            int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, myFP.getMemberNumbers(), false);
            if (fieldNumbers != null && fieldNumbers.length > 0)
            {
                fieldNumbers = ClassUtils.getFlagsSetTo(cmd.getNonPKMemberFlags(), fieldNumbers, true);
            }
            if (fieldNumbers != null && fieldNumbers.length > 0)
            {
                fieldNumbers = ClassUtils.getFlagsSetTo(cmd.getSCOMutableMemberFlags(), fieldNumbers, false);
            }

            boolean versionNeedsLoading = false;
            if (cmd.isVersioned() && transactionalVersion == null)
            {
                versionNeedsLoading = true;
            }
            if ((fieldNumbers != null && fieldNumbers.length > 0) || versionNeedsLoading)
            {
                if ((flags&FLAG_VALIDATING) == 0)
                {
                    try
                    {
                        // It is possible to get recursive validation when using things like ODF, Cassandra etc and having a bidir relation, and nontransactional.
                        flags |= FLAG_VALIDATING;

                        transitionReadField(false);
                        // Some fetch plan fields, or the version are not loaded so try to load them, and this by itself 
                        // validates the existence. Loads the fields in the current FetchPlan (JDO2 spec 12.6.5)
                        fieldNumbers = myFP.getMemberNumbers();
                        if (fieldNumbers != null || versionNeedsLoading)
                        {
                            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
                            setTransactionalVersion(null); // Make sure we get the latest version
                            loadFieldsFromDatastore(fieldNumbers);
                            if (callPostLoad && areFieldsLoaded(fieldNumbers)) // If a FK is in the STORED cache then wont be marked as loaded yet
                            {
                                postLoad();
                            }
                        }
                    }
                    finally
                    {
                        flags &= ~FLAG_VALIDATING;
                    }
                }
            }
            else
            {
                // Validate the object existence
                locate();
                transitionReadField(false);
            }
        }
    }

    // --------------------------- Process Methods -----------------------------

    /**
     * Method called before a write of the specified field.
     * @param fieldNumber The field to write
     * @return true if the field was already dirty before
     */
    protected boolean preWriteField(int fieldNumber)
    {
        boolean wasDirty = dirty;

        // If we're writing a field in the process of inserting it must be due to dnPreStore().
        // We haven't actually done the INSERT yet so we don't want to mark anything as dirty, which would make us want to do an UPDATE later. 
        if ((flags&FLAG_INSERTING) == 0 && (flags&FLAG_INSERTING_CALLBACKS) == 0)
        {
            if (!wasDirty) // (only do it for first dirty event).
            {
                // Call any lifecycle listeners waiting for this event
                getCallbackHandler().preDirty(myPC);
            }

            // Update lifecycle state as required
            transitionWriteField();

            dirty = true;
            dirtyFields[fieldNumber] = true;
            loadedFields[fieldNumber] = true;
        }
        return wasDirty;
    }

    /**
     * Method called after the write of a field.
     * @param wasDirty whether before writing this field the pc was dirty
     */
    protected void postWriteField(boolean wasDirty)
    {
        if (dirty && !wasDirty) // (only do it for first dirty event).
        {
            // Call any lifecycle listeners waiting for this event
            getCallbackHandler().postDirty(myPC);
        }

        if ((flags&FLAG_INSERTING) == 0 && (flags&FLAG_INSERTING_CALLBACKS) == 0 && !isFlushing() && !(myLC.isTransactional() && !myLC.isPersistent()))
        {
            if (isDetaching() && getReferencedPC() == null)
            {
                // detachAllOnCommit caused a field to be dirty so ignore it
                return;
            }

            // Not during flush, and not transactional-transient, and not inserting - so mark as dirty
            myEC.markDirty(this, true);
        }
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
    protected void postStateChange()
    {
        flags &= ~FLAG_CHANGING_STATE;
        if (isPostLoadPending() && areFieldsLoaded(myFP.getMemberNumbers()))
        {
            // Only call postLoad when all FetchPlan fields are loaded
            setPostLoadPending(false);
            postLoad();
        }
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

    /**
     * Called whenever the default fetch group fields have all been loaded.
     * Updates dnFlags and calls dnPostLoad() as appropriate.
     * <p>
     * If it's called in the midst of a life-cycle transition both actions will
     * be deferred until the transition is complete.
     * <em>This deferral is important</em>. Without it, we could enter user
     * code (dnPostLoad()) while still making a state transition, and that way
     * lies madness.
     * <p>
     * As an example, consider a dnPostLoad() that calls other enhanced methods
     * that read fields (dnPostLoad() itself is not enhanced). A P_NONTRANS
     * object accessed within a transaction would produce the following infinite
     * loop:
     * <p>
     * <blockquote>
     * 
     * <pre>
     *  isLoaded()
     *  transitionReadField()
     *  refreshLoadedFields()
     *  dnPostLoad()
     *  isLoaded()
     *  ...
     * </pre>
     * 
     * </blockquote>
     * <p>
     * because the transition from P_NONTRANS to P_CLEAN can never be completed.
     */
    private void postLoad()
    {
        if (isChangingState())
        {
            setPostLoadPending(true);
        }
        else
        {
            // A tx object whose DFG fields are loaded doesn't need to contact us in order to read those fields, so we can safely set READ_OK.
            // A non-tx object needs to notify us on all field reads so that we can decide whether or not any transition should occur, so we leave the flags at LOAD_REQUIRED.
            if (persistenceFlags == Persistable.LOAD_REQUIRED && myLC.isTransactional())
            {
                persistenceFlags = Persistable.READ_OK;
                myPC.dnReplaceFlags();
            }

            getCallbackHandler().postLoad(myPC);
        }
    }

    /**
     * Guarantee that the serializable transactional and persistent fields are loaded into the instance. 
     * This method is called by the generated dnPreSerialize method prior to serialization of the instance.
     * @param pc the calling Persistable instance
     */
    public void preSerialize(Persistable pc)
    {
        if (disconnectClone(pc))
        {
            return;
        }

        // Retrieve all fields prior to serialisation
        retrieve(false);

        myLC = myLC.transitionSerialize(this);

        if (!isStoringPC() && pc instanceof Detachable)
        {
            if (!myLC.isDeleted() && myLC.isPersistent())
            {
                if (myLC.isDirty())
                {
                    flush();
                }

                // Normal PC Detachable object being serialised so load up the detached state into the instance
                // JDO spec "For Detachable classes, the dnPreSerialize method must also initialize the dnDetachedState
                // instance so that the detached state is serialized along with the instance."
                ((Detachable)pc).dnReplaceDetachedState();
            }
        }
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

    /**
     * Flushes any outstanding changes to the object to the datastore. 
     * This will process :-
     * <ul>
     * <li>Any objects that have been marked as provisionally persistent yet haven't been flushed to the datastore.</li>
     * <li>Any objects that have been marked as provisionally deleted yet haven't been flushed to the datastore.</li>
     * <li>Any fields that have been updated.</li>
     * </ul>
     */
    public void flush()
    {
        if (dirty)
        {
            if (isFlushing())
            {
                // In the case of persisting a new object using autoincrement id within an optimistic
                // transaction, flush() will initially be called at the point of recognising that the
                // id is generated in the datastore, and will then be called again at the point of doing
                // the InsertRequest for the object itself. Just return since we are flushing right now
                return;
            }
            if ((flags&FLAG_INSERTING) != 0 || (flags&FLAG_INSERTING_CALLBACKS) != 0)
            {
                return;
            }

            setFlushing(true);
            try
            {
                if (myLC.stateType() == LifeCycleState.P_NEW && !isFlushedNew())
                {
                    // Newly persisted object but not yet flushed to datastore (e.g optimistic transactions)
                    if (!isEmbedded())
                    {
                        // internalMakePersistent does preStore, postStore
                        internalMakePersistent();
                    }
                    else
                    {
                        getCallbackHandler().preStore(myPC);
                        if (myID == null)
                        {
                            setIdentity(true); // Just in case user is setting it in preStore
                        }

                        getCallbackHandler().postStore(myPC);
                    }
                    dirty = false;
                }
                else if (myLC.stateType() == LifeCycleState.P_DELETED)
                {
                    // Object marked as deleted but not yet deleted from datastore
                    getCallbackHandler().preDelete(myPC);
                    if (!isEmbedded())
                    {
                        internalDeletePersistent();
                    }
                    getCallbackHandler().postDelete(myPC);
                }
                else if (myLC.stateType() == LifeCycleState.P_NEW_DELETED)
                {
                    // Newly persisted object marked as deleted but not yet deleted from datastore
                    if (isFlushedNew())
                    {
                        // Only delete it if it was actually persisted into the datastore
                        getCallbackHandler().preDelete(myPC);
                        if (!isEmbedded())
                        {
                            internalDeletePersistent();
                        }
                        setFlushedNew(false); // No longer newly persisted flushed object since has been deleted
                        getCallbackHandler().postDelete(myPC);
                    }
                    else
                    {
                        // Was never persisted to the datastore so nothing to do
                        dirty = false;
                    }
                }
                else
                {
                    // Updated object with changes to flush to datastore
                    if (!isDeleting())
                    {
                        getCallbackHandler().preStore(myPC);
                        if (myID == null)
                        {
                            setIdentity(true); // Just in case user is setting it in preStore
                        }
                    }

                    if (!isEmbedded())
                    {
                        int[] dirtyFieldNumbers = ClassUtils.getFlagsSetTo(dirtyFields, true);
                        if (dirtyFieldNumbers == null)
                        {
                            // StateManager is dirty but no fields. What happened?
                            throw new NucleusException(Localiser.msg("026010")).setFatal();
                        }

                        if (myEC.getNucleusContext().isClassCacheable(getClassMetaData()))
                        {
                            myEC.markFieldsForUpdateInLevel2Cache(getInternalObjectId(), dirtyFields);
                        }
                        getStoreManager().getPersistenceHandler().updateObject(this, dirtyFieldNumbers);

                        // Update the object in the cache(s)
                        myEC.putObjectIntoLevel1Cache(this);
                    }

                    clearDirtyFlags();

                    getCallbackHandler().postStore(myPC);
                }
            }
            finally
            {
                setFlushing(false);
            }
        }
    }

    /**
     * Method to save all fields of the object, for use in any rollback.
     */
    @Override
    public void saveFields()
    {
        if (savedState == null)
        {
            // Create SavedState, and copy PC fields
            savedState = new SavedState(myPC.dnNewInstance(this), loadedFields.clone(), persistenceFlags);
            savedState.getPC().dnCopyFields(myPC, cmd.getAllMemberPositions());
        }
        else
        {
            // Update SavedState with current PC fields, flags etc
            savedState.getPC().dnCopyFields(myPC, cmd.getAllMemberPositions());
            savedState.setPersistenceFlags(persistenceFlags);
            savedState.setLoadedFields(loadedFields.clone());
        }
    }

    /**
     * Method to restore all fields of the object.
     */
    @Override
    public void restoreFields()
    {
        if (savedState != null)
        {
            // Restore PC fields, flags etc from SavedState
            loadedFields = savedState.getLoadedFields();

            persistenceFlags = savedState.getPersistenceFlags();
            myPC.dnReplaceFlags();

            myPC.dnCopyFields(savedState.getPC(), cmd.getAllMemberPositions());

            savedState = null;

            clearDirtyFlags();
        }
    }

    /**
     * Method to clear all saved fields on the object.
     */
    @Override
    public void clearSavedFields()
    {
        savedState = null;
    }

    // ------------------------------ Helper Methods ---------------------------

    /**
     * Method to convert the persistable object into String form.
     * @param pc The persistable object
     * @param cmd Metadata for the class
     * @return The string containing the description of the Persistable
     */
    private static String convertPCToString(Object pc, AbstractClassMetaData cmd)
    {
        StringBuilder str = new StringBuilder();
        str.append(StringUtils.toJVMIDString(pc));

        if (pc == null)
        {
            return str.toString();
        }

        str.append(" [");
        str.append("dnStateManager=").append(peekField(pc, "dnStateManager"));

        Object flagsObj = peekField(pc, "dnFlags");
        if (flagsObj instanceof Byte)
        {
            switch (((Byte)flagsObj).byteValue())
            {
                case Persistable.LOAD_REQUIRED:
                    str.append(", dnFlags=LOAD_REQUIRED");
                    break;
                case Persistable.READ_OK:
                    str.append(", dnFlags=READ_OK");
                    break;
                case Persistable.READ_WRITE_OK:
                    str.append(", dnFlags=READ_WRITE_OK");
                    break;
                default:
                    str.append(", dnFlags=???");
                    break;
            }
        }
        else
        {
            str.append(", dnFlags=").append(flagsObj);
        }

        Class c = pc.getClass();
        do
        {
            AbstractMemberMetaData[] mmds = cmd.getManagedMembers();
            for (AbstractMemberMetaData mmd : mmds)
            {
                str.append(", ").append(mmd.getName());
                str.append("=");
                str.append(peekField(pc, mmd.getName()));
            }

            c = c.getSuperclass();
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        while (c != null && Persistable.class.isAssignableFrom(c));
        
        str.append("]");
        return str.toString();
    }

    /**
     * Utility to dump the contents of the StateManager to the provided log.
     * @param log Logger to use
     */
    public void log(NucleusLogger log)
    {
        log.debug("StateManager[" + StringUtils.toJVMIDString(this) + "]");
        log.debug("    myEC=" + myEC);
        log.debug("    myID=" + myID);
        log.debug("    myLC=" + myLC);
        log.debug("    cmd=" + cmd);
        log.debug("    fieldCount=" + cmd.getMemberCount());
        log.debug("    dirty=" + dirty);
        log.debug("    flushing=" + isFlushing());
        log.debug("    changingState=" + isChangingState());
        log.debug("    postLoadPending=" + isPostLoadPending());
        log.debug("    disconnecting=" + ((flags&FLAG_DISCONNECTING) != 0));
        log.debug("    loadedFields=" + StringUtils.booleanArrayToString(loadedFields));
        log.debug("    dirtyFields=" + StringUtils.booleanArrayToString(dirtyFields));
        log.debug("    getSecondClassMutableFields()=" + StringUtils.booleanArrayToString(cmd.getSCOMutableMemberFlags()));
        log.debug("    getAllFieldNumbers()=" + StringUtils.intArrayToString(cmd.getAllMemberPositions()));
        log.debug("    secondClassMutableFieldNumbers=" + StringUtils.intArrayToString(cmd.getSCOMutableMemberPositions()));

        switch (persistenceFlags)
        {
            case Persistable.LOAD_REQUIRED:
                log.debug("    persistenceFlags=LOAD_REQUIRED");
                break;
            case Persistable.READ_OK:
                log.debug("    persistenceFlags=READ_OK");
                break;
            case Persistable.READ_WRITE_OK:
                log.debug("    persistenceFlags=READ_WRITE_OK");
                break;
            default:
                log.debug("    persistenceFlags=???");
                break;
        }
        log.debug("    myPC=" + convertPCToString(myPC, cmd));

        if (savedState != null)
        {
            switch (savedState.getPersistenceFlags())
            {
                case Persistable.LOAD_REQUIRED:
                    log.debug("    savedState.persistableFlags=LOAD_REQUIRED");
                    break;
                case Persistable.READ_OK:
                    log.debug("    savedState.persistableFlags=READ_OK");
                    break;
                case Persistable.READ_WRITE_OK:
                    log.debug("    savedState.persistableFlags=READ_WRITE_OK");
                    break;
                default:
                    log.debug("    savedState.persistableFlags=???");
                    break;
            }
            log.debug("    savedState.loadedFields=" + StringUtils.booleanArrayToString(savedState.getLoadedFields()));
            log.debug("    savedState.PC= " + convertPCToString(savedState.getPC(), cmd));
        }
    }

    /**
     * Utility to take a peek at a field in the persistable object.
     * @param obj The persistable object
     * @param fieldName The field to peek at
     * @return The value of the field.
     */
    protected static Object peekField(Object obj, String fieldName)
    {
        try
        {
            // This doesn't work due to security problems but you get the idea.
            // I'm trying to get field values directly without going through the provideField machinery.
            Object value = obj.getClass().getDeclaredField(fieldName).get(obj);
            if (value instanceof Persistable)
            {
                return StringUtils.toJVMIDString(value);
            }
            return value;
        }
        catch (Exception e)
        {
            return e.toString();
        }
    }

    public void updateFieldAfterInsert(Object pc, int fieldNumber)
    {
        // Does nothing in this implementation; refer to ReferentialJDOStateManager
    }

    // ============================================= DEPRECATED METHODS ==============================================

    /**
     * Initialises a state manager to manage a HOLLOW / P_CLEAN instance having the given FieldValues.
     * This constructor is used for creating new instances of existing persistent objects using application identity,
     * and consequently shouldn't be used when the StoreManager controls the creation of such objects (such as in an ODBMS).
     * @param fv the initial field values of the object.
     * @param pcClass Class of the object that this will manage the state for
     * @deprecated Remove use of this and use initialiseForHollow
     */
    public void initialiseForHollowAppId(FieldValues fv, Class pcClass)
    {
        if (cmd.getIdentityType() != IdentityType.APPLICATION)
        {
            throw new NucleusUserException("This constructor is only for objects using application identity.").setFatal();
        }

        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.HOLLOW);
        persistenceFlags = Persistable.LOAD_REQUIRED;
        myPC = HELPER.newInstance(pcClass, this); // Create new PC
        if (myPC == null)
        {
            if (!HELPER.getRegisteredClasses().contains(pcClass))
            {
                // probably never will get here, as EnhancementHelper.newInstance() internally already throws JDOFatalUserException when class is not registered 
                throw new NucleusUserException(Localiser.msg("026018", pcClass.getName())).setFatal();
            }

            // Provide advisory information since we can't create an instance of this class, so maybe they have an error in their data ?
            throw new NucleusUserException(Localiser.msg("026019", pcClass.getName())).setFatal();
        }

        loadFieldValues(fv); // as a minimum the PK fields are loaded here

        // Create the ID now that we have the PK fields loaded
        myID = myEC.getNucleusContext().getIdentityManager().getApplicationId(myPC, cmd);
    }

    /**
     * Look to the database to determine which class this object is. This parameter is a hint. Set false, if it's
     * already determined the correct pcClass for this pc "object" in a certain
     * level in the hierarchy. Set to true and it will look to the database.
     * TODO This is only called by some outdated code in LDAPUtils; remove it when that is fixed
     * @param fv the initial field values of the object.
     * @deprecated Dont use this, to be removed
     */
    public void checkInheritance(FieldValues fv)
    {
        // Inheritance case, check the level of the instance
        ClassLoaderResolver clr = myEC.getClassLoaderResolver();
        String className = getStoreManager().getClassNameForObjectID(myID, clr, myEC);
        if (className == null)
        {
            // className is null when id class exists, and object has been validated and doesn't exist.
            throw new NucleusObjectNotFoundException(Localiser.msg("026013", IdentityUtils.getPersistableIdentityForId(myID)), myID);
        }
        else if (!cmd.getFullClassName().equals(className))
        {
            Class pcClass;
            try
            {
                //load the class and make sure the class is initialized
                pcClass = clr.classForName(className, myID.getClass().getClassLoader(), true);
                cmd = myEC.getMetaDataManager().getMetaDataForClass(pcClass, clr);
            }
            catch (ClassNotResolvedException e)
            {
                NucleusLogger.PERSISTENCE.warn(Localiser.msg("026014", IdentityUtils.getPersistableIdentityForId(myID)));
                throw new NucleusUserException(Localiser.msg("026014", IdentityUtils.getPersistableIdentityForId(myID)), e);
            }
            if (cmd == null)
            {
                throw new NucleusUserException(Localiser.msg("026012", pcClass)).setFatal();
            }
            if (cmd.getIdentityType() != IdentityType.APPLICATION)
            {
                throw new NucleusUserException("This method should only be used for objects using application identity.").setFatal();
            }
            myFP = myEC.getFetchPlan().getFetchPlanForClass(cmd);

            int fieldCount = cmd.getMemberCount();
            dirtyFields = new boolean[fieldCount];
            loadedFields = new boolean[fieldCount];

            // Create new PC at right inheritance level
            myPC = HELPER.newInstance(pcClass, this);
            if (myPC == null)
            {
                throw new NucleusUserException(Localiser.msg("026018", cmd.getFullClassName())).setFatal();
            }

            // Note that this will mean the fields are loaded twice (loaded earlier in this method)
            // and also that postLoad will be called twice
            loadFieldValues(fv);

            // Create the id for the new PC
            myID = myEC.getNucleusContext().getIdentityManager().getApplicationId(myPC, cmd);
        }
    }
}