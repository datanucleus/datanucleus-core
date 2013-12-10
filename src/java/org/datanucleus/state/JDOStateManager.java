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

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOFatalInternalException;
import javax.jdo.JDOFatalUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.spi.Detachable;
import javax.jdo.spi.JDOImplHelper;
import javax.jdo.spi.PersistenceCapable;
import javax.jdo.spi.StateManager;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.ExecutionContext.EmbeddedOwnerRelation;
import org.datanucleus.FetchPlan;
import org.datanucleus.FetchPlanForClass;
import org.datanucleus.NucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.cache.CachedPC;
import org.datanucleus.cache.L2CacheRetrieveFieldManager;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.identity.OID;
import org.datanucleus.identity.OIDFactory;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IdentityStrategy;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.PersistenceFlags;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.store.FieldValues;
import org.datanucleus.store.ObjectReferencingStoreManager;
import org.datanucleus.store.exceptions.NotYetFlushedException;
import org.datanucleus.store.fieldmanager.AttachFieldManager;
import org.datanucleus.store.fieldmanager.DeleteFieldManager;
import org.datanucleus.store.fieldmanager.DetachFieldManager;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.store.fieldmanager.MakeTransientFieldManager;
import org.datanucleus.store.fieldmanager.NullifyRelationFieldManager;
import org.datanucleus.store.fieldmanager.PersistFieldManager;
import org.datanucleus.store.fieldmanager.ReachabilityFieldManager;
import org.datanucleus.store.fieldmanager.SingleValueFieldManager;
import org.datanucleus.store.fieldmanager.UnsetOwnerFieldManager;
import org.datanucleus.store.fieldmanager.AbstractFetchDepthFieldManager.EndOfFetchPlanGraphException;
import org.datanucleus.store.objectvaluegenerator.ObjectValueGenerator;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOCollection;
import org.datanucleus.store.types.SCOContainer;
import org.datanucleus.store.types.SCOMap;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;
import org.datanucleus.util.TypeConversionHelper;

/**
 * Implementation of a JDO StateManager, supporting the bytecode enhancement contract of JDO.
 * Implemented here as one StateManager per Object so adds on functionality particular 
 * to each object. All PersistenceCapable objects will have a StateManager when they 
 * have had communication with the PersistenceManager. They will typically always have
 * an identity also. The exception to that is for embedded/serialised objects.
 * 
 * <H3>Embedded/Serialised Objects</H3>
 * An object that is being embedded/serialised in an owning object will NOT have an identity
 * unless the object is subject to a makePersistent() call also. When an object
 * is embedded/serialised and a field is changed, the field will NOT be marked as dirty (unless
 * it is also an object in its own right with an identity). When a field is changed
 * any owning objects are updated so that they can update their tables accordingly.
 *
 * <H3>Performance and Memory</H3>
 * StateManagers are very performance-critical, because for each PersistentCapable object made persistent,
 * there will be one StateManager instance, adding up to the total memory footprint of that object.
 * In heap profiling analysis, JDOStateManager showed to consume bytes 169 per StateManager by itself
 * and about 500 bytes per StateManager when taking PC-individual child-object (like the OID) referred
 * by the StateManager into account. With small Java objects this can mean a substantial memory overhead and
 * for applications using such small objects can be critical. For this reason the StateManager should always
 * be minimal in memory consumption.
 * 
 * TODO The future aim is to have different types of StateManagers (ObjectProviders) that have particular
 * features. In addition we want to have a JPAStateManager using an internal enhancement contract. This means
 * we'll need various things generalising into a superclass
 */
public class JDOStateManager extends AbstractStateManager implements StateManager, ObjectProvider
{
    /** The PersistenceCapable instance managed by this StateManager */
    protected PersistenceCapable myPC;

    /** Image of the PersistenceCapable instance when the instance is enlisted in the transaction. */
    protected PersistenceCapable savedImage = null;

    private static final JDOImplHelper HELPER;
    static
    {
        HELPER = (JDOImplHelper) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                try
                {
                    return JDOImplHelper.getInstance();
                }
                catch (SecurityException e)
                {
                    throw new JDOFatalUserException(LOCALISER.msg("026000"), e);
                }
            }
        });
    }

    /**
     * Constructor for object of specified type managed by the provided ExecutionContext.
     * @param ec ExecutionContext
     * @param cmd the metadata for the class.
     */
    public JDOStateManager(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        super(ec, cmd);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.AbstractStateManager#connect(org.datanucleus.ExecutionContext, org.datanucleus.metadata.AbstractClassMetaData)
     */
    @Override
    public void connect(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        super.connect(ec, cmd);

        savedImage = null;
        ec.setAttachDetachReferencedObject(this, null);
    }

    /**
     * Disconnect the provider from the ExecutionContext and PC object.
     */
    public void disconnect()
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("026011", StringUtils.toJVMIDString(myPC), this));
        }

        //we are transitioning to TRANSIENT state, so if any postLoad
        //action is pending we do it before. This usually happens when
        //we make transient instances using the fetch plan and some
        //fields were loaded during this action which triggered a jdoPostLoad event
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

        myEC.removeObjectProvider(this);
        persistenceFlags = PersistenceFlags.READ_WRITE_OK;
        myPC.jdoReplaceFlags();

        setDisconnecting(true);
        try
        {
            replaceStateManager(myPC, null);
        }
        finally
        {
            setDisconnecting(false);
        }

        this.preDeleteLoadedFields = null;
        this.myEC.setAttachDetachReferencedObject(this, null);
        this.objectType = 0;

        clearSavedFields();

        NucleusContext nucCtx = myEC.getNucleusContext();
        myPC = null;
        myID = null;
        myInternalID = null;
        myLC = null;
        myEC = null;
        myFP = null;
        myVersion = null;

        persistenceFlags = 0;
        flags = 0;
        restoreValues = false;
        transactionalVersion = null;
        currFM = null;
        dirty = false;

        cmd = null;
        dirtyFields = null;
        loadedFields = null;

        // Remove the object from any pooling
        nucCtx.getObjectProviderFactory().disconnectObjectProvider(this);
    }

    /**
     * Initialises a state manager to manage a hollow instance having the given object ID and the given
     * (optional) field values. This constructor is used for creating new instances of existing persistent
     * objects, and consequently shouldnt be used when the StoreManager controls the creation of such objects
     * (such as in an ODBMS).
     * @param id the JDO identity of the object.
     * @param fv the initial field values of the object (optional)
     * @param pcClass Class of the object that this will manage the state for
     */
    public void initialiseForHollow(Object id, FieldValues fv, Class pcClass)
    {
        myID = id;
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.HOLLOW);
        persistenceFlags = PersistenceFlags.LOAD_REQUIRED;
        if (id instanceof OID || id == null)
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
        }
    }

    /**
     * Initialises a state manager to manage a HOLLOW / P_CLEAN instance having the given FieldValues.
     * This constructor is used for creating new instances of existing persistent objects using application 
     * identity, and consequently shouldnt be used when the StoreManager controls the creation of such objects
     * (such as in an ODBMS).
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
        persistenceFlags = PersistenceFlags.LOAD_REQUIRED;
        myPC = HELPER.newInstance(pcClass, this); // Create new PC
        if (myPC == null)
        {
            if (!HELPER.getRegisteredClasses().contains(pcClass))
            {
                // probably never will get here, as JDOImplHelper.newInstance() internally already throws
                // JDOFatalUserException when class is not registered 
                throw new NucleusUserException(LOCALISER.msg("026018", pcClass.getName())).setFatal();
            }
            else
            {
                // Provide advisory information since we can't create an instance of this class, so maybe they
                // have an error in their data ?
                throw new NucleusUserException(LOCALISER.msg("026019", pcClass.getName())).setFatal();
            }
        }

        loadFieldValues(fv); // as a minimum the PK fields are loaded here

        // Create the ID now that we have the PK fields loaded
        myID = myPC.jdoNewObjectIdInstance();
        if (!cmd.usesSingleFieldIdentityClass())
        {
            myPC.jdoCopyKeyFieldsToObjectId(myID);
        }
    }

    /**
     * Initialises a state manager to manage the given hollow instance having the given object ID.
     * Unlike the {@link #initialiseForHollow} method, this method does not create a new instance and instead 
     * takes a pre-constructed instance (such as from an ODBMS).
     * @param id the identity of the object.
     * @param pc the object to be managed.
     */
    public void initialiseForHollowPreConstructed(Object id, Object pc)
    {
        myID = id;
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.HOLLOW);
        persistenceFlags = PersistenceFlags.LOAD_REQUIRED;
        myPC = (PersistenceCapable)pc;

        replaceStateManager(myPC, this); // Assign this StateManager to the PC
        myPC.jdoReplaceFlags();

        // TODO Add to the cache
    }

    /**
     * Initialises a state manager to manage the passed persistent instance having the given object ID.
     * Used where we have retrieved a PC object from a datastore directly (not field-by-field), for example on
     * an object datastore. This initialiser will not add StateManagers to all related PCs. This must be done by
     * any calling process. This simply adds the StateManager to the specified object and records the id, setting
     * all fields of the object as loaded.
     * @param id the identity of the object.
     * @param pc The object to be managed
     */
    public void initialiseForPersistentClean(Object id, Object pc)
    {
        myID = id;
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_CLEAN);
        persistenceFlags = PersistenceFlags.LOAD_REQUIRED;
        myPC = (PersistenceCapable)pc;

        replaceStateManager(myPC, this); // Assign this StateManager to the PC
        myPC.jdoReplaceFlags();

        // Mark all fields as loaded
        for (int i=0; i<loadedFields.length; ++i)
        {
            loadedFields[i] = true;
        }

        // Add the object to the cache
        myEC.putObjectIntoLevel1Cache(this);
    }

    /**
     * Initialises a state manager to manage a PersistenceCapable instance that will be EMBEDDED/SERIALISED 
     * into another PersistenceCapable object. The instance will not be assigned an identity in the process 
     * since it is a SCO.
     * @param pc The PersistenceCapable to manage (see copyPc also)
     * @param copyPc Whether the SM should manage a copy of the passed PC or that one
     */
    public void initialiseForEmbedded(Object pc, boolean copyPc)
    {
        objectType = ObjectProvider.EMBEDDED_PC; // Default to an embedded PC object
        myID = null; // It is embedded at this point so dont need an ID since we're not persisting it
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_CLEAN);
        persistenceFlags = PersistenceFlags.LOAD_REQUIRED;

        myPC = (PersistenceCapable)pc;
        replaceStateManager(myPC, this); // Set SM for embedded PC to be this
        if (copyPc)
        {
            // Create a new PC with the same field values
            PersistenceCapable pcCopy = myPC.jdoNewInstance(this);
            pcCopy.jdoCopyFields(myPC, cmd.getAllMemberPositions());

            // Swap the managed PC to be the copy and not the input
            replaceStateManager(pcCopy, this);
            myPC = pcCopy;
            disconnectClone((PersistenceCapable)pc);
        }

        // Mark all fields as loaded since we are using the passed PersistenceCapable
        for (int i=0;i<loadedFields.length;i++)
        {
            loadedFields[i] = true;
        }
    }

    /**
     * Initialises a state manager to manage a transient instance that is becoming newly persistent.
     * A new object ID for the instance is obtained from the store manager and the object is inserted
     * in the data store.
     * <p>This constructor is used for assigning state managers to existing
     * instances that are transitioning to a persistent state.
     * @param pc the instance being make persistent.
     * @param preInsertChanges Any changes to make before inserting
     */
    public void initialiseForPersistentNew(Object pc, FieldValues preInsertChanges)
    {
        myPC = (PersistenceCapable)pc;
        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_NEW);
        persistenceFlags = PersistenceFlags.READ_OK;
        for (int i=0; i<loadedFields.length; ++i)
        {
            loadedFields[i] = true;
        }

        replaceStateManager(myPC, this); // Assign this StateManager to the PC
        myPC.jdoReplaceFlags();

        saveFields();

        // Populate all fields that have "value-strategy" and are not datastore populated
        populateStrategyFields();

        if (preInsertChanges != null)
        {
            // Apply any pre-insert field updates
            preInsertChanges.fetchFields(this);
        }

        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            //load key fields from Application Id instance to PC instance

            //if a primary key field is of type PersistenceCapable, it must first be persistent
            int totalNumFields = cmd.getAllMemberPositions().length;
            for (int fieldNumber = 0; fieldNumber < totalNumFields; fieldNumber++)
            {
                AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
                if (fmd.isPrimaryKey())
                {
                    if (myEC.getMetaDataManager().getMetaDataForClass(fmd.getType(), getExecutionContext().getClassLoaderResolver()) != null)
                    {
                        try
                        {
                            if (myEC.getMultithreaded())
                            {
                                myEC.getLock().lock();
                                lock.lock();
                            }

                            FieldManager prevFM = currFM;
                            try
                            {
                                currFM = new SingleValueFieldManager();
                                myPC.jdoProvideField(fieldNumber);
                                PersistenceCapable pkFieldPC = 
                                    (PersistenceCapable) ((SingleValueFieldManager) currFM).fetchObjectField(fieldNumber);
                                if (pkFieldPC == null)
                                {
                                    throw new NucleusUserException(
                                        LOCALISER.msg("026016", fmd.getFullFieldName()));
                                }
                                if (!myEC.getApiAdapter().isPersistent(pkFieldPC))
                                {
                                    // Make sure the PC field is persistent - can cause the insert of our object 
                                    // being managed by this SM via flush() when bidir relation
                                    Object persistedFieldPC = myEC.persistObjectInternal(pkFieldPC, null, null, -1, 
                                        ObjectProvider.PC);
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
                                lock.unlock();
                                myEC.getLock().unlock();
                            }
                        }
                    }
                }
            }
        }

        /* Set the identity
         * This must come after the above block, because in identifying relationships
         * the PK FK associations must be persisted before, otherwise we
         * don't have an id assigned to the PK FK associations
         */        
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
            int[] relationPositions = cmd.getRelationMemberPositions(clr, myEC.getMetaDataManager());
            if (relationPositions != null)
            {
                for (int i=0;i<relationPositions.length;i++)
                {
                    AbstractMemberMetaData mmd = 
                        cmd.getMetaDataForManagedMemberAtAbsolutePosition(relationPositions[i]);
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
    public void initialiseForTransactionalTransient(Object pc)
    {
        myPC = (PersistenceCapable)pc;
        myLC = null;
        persistenceFlags = PersistenceFlags.READ_OK;
        for (int i=0; i<loadedFields.length; ++i)
        {
            loadedFields[i] = true;
        }
        myPC.jdoReplaceFlags();

        // Populate all fields that have "value-strategy" and are not datastore populated
        populateStrategyFields();

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
     * Initialises the StateManager to manage a PersistenceCapable object in detached state.
     * @param pc the detach object.
     * @param id the identity of the object.
     * @param version the detached version
     */
    public void initialiseForDetached(Object pc, Object id, Object version)
    {
        this.myID = id;
        this.myPC = (PersistenceCapable)pc;
        setVersion(version);

        // This lifecycle state is not always correct. It is certainly "detached"
        // but we dont know if it is CLEAN or DIRTY. We need this setting here since all objects
        // have a lifecycle state and other methods e.g isPersistent() depend on it.
        this.myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.DETACHED_CLEAN);

        this.myPC.jdoReplaceFlags();
        replaceStateManager(myPC, this);
    }

    /**
     * Initialises the StateManager to manage a PersistenceCapable object that is not persistent but is
     * about to be deleted.
     * @param pc the object to delete
     */
    public void initialiseForPNewToBeDeleted(Object pc)
    {
        this.myID = null;
        this.myPC = (PersistenceCapable)pc;
        this.myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_NEW);
        for (int i=0; i<loadedFields.length; ++i) // Mark all fields as loaded
        {
            loadedFields[i] = true;
        }
        replaceStateManager(myPC, this);
    }

    /**
     * Initialise the ObjectProvider, assigning the specified id to the object. 
     * This is used when getting objects out of the L2 Cache, where they have no ObjectProvider 
     * assigned, and returning them as associated with a particular ExecutionContext.
     * @param cachedPC The cached PC object
     * @param id Id to assign to the PersistenceCapable object
     */
    public void initialiseForCachedPC(CachedPC cachedPC, Object id)
    {
        // Create a new copy of the input object type, performing the majority of the initialisation
        initialiseForHollow(id, null, cachedPC.getObjectClass());

        myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.P_CLEAN);
        persistenceFlags = PersistenceFlags.READ_OK;

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
     * Accessor for the Persistent Capable object.
     * @return The PersistentCapable object
     */
    public Object getObject()
    {
        return myPC;
    }

    /**
     * Method to save all fields of the object so we can potentially restore them later.
     */
    public void saveFields()
    {
        savedImage = myPC.jdoNewInstance(this);
        savedImage.jdoCopyFields(myPC, cmd.getAllMemberPositions());
        savedFlags = persistenceFlags;
        savedLoadedFields = loadedFields.clone();
    }

    /**
     * Method to clear all saved fields on the object.
     */
    public void clearSavedFields()
    {
        savedImage = null;
        savedFlags = 0;
        savedLoadedFields = null;
    }

    /**
     * Method to restore all fields of the object.
     */
    public void restoreFields()
    {
        if (savedImage != null)
        {
            loadedFields = savedLoadedFields;
            persistenceFlags = savedFlags;
            myPC.jdoReplaceFlags();
            myPC.jdoCopyFields(savedImage, cmd.getAllMemberPositions());

            clearDirtyFlags();
            clearSavedFields();
        }
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

        if (persistenceFlags == PersistenceFlags.LOAD_REQUIRED && areFieldsLoaded(cmd.getDFGMemberPositions()))
        {
            // All DFG fields loaded and object is transactional so it doesnt need to contact us for those fields
            // Note that this is the DFG and NOT the current FetchPlan since in the enhancement of classes
            // all DFG fields are set to check jdoFlags before relaying back to the StateManager
            persistenceFlags = PersistenceFlags.READ_OK;
            myPC.jdoReplaceFlags();
        }
    }

    /**
     * Method to evict the managed object from the current transaction.
     */
    public void evictFromTransaction()
    {
        myEC.evictFromTransaction(this);

        /*
         * A non-transactional object needs to contact us on any field read no
         * matter what fields are loaded.
         */
        persistenceFlags = PersistenceFlags.LOAD_REQUIRED;
        myPC.jdoReplaceFlags();
    }

    /**
     * Utility to update the passed object with the passed StateManager (can be null).
     * @param pc The object to update
     * @param sm The new state manager
     */
    protected void replaceStateManager(final PersistenceCapable pc, final StateManager sm)
    {
        try
        {
            // Calls to pc.jdoReplaceStateManager must be run privileged
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run() 
                {
                    pc.jdoReplaceStateManager(sm);
                    return null;
                }
            });
        }
        catch (SecurityException e)
        {
            throw new JDOFatalUserException(LOCALISER.msg("026000"), e);
        }
    }

    /**
     * Replace the current value of jdoStateManager.
     * <P>This method is called by the PersistenceCapable whenever jdoReplaceStateManager is called and 
     * there is already an owning StateManager. This is a security precaution to ensure that the owning 
     * StateManager is the only source of any change to its reference in the PersistenceCapable.</p>
     *
     * @return the new value for the jdoStateManager
     * @param pc the calling PersistenceCapable instance
     * @param sm the proposed new value for the jdoStateManager
     */
    public StateManager replacingStateManager(PersistenceCapable pc, StateManager sm)
    {
        if (myLC == null)
        {
            throw new JDOFatalInternalException("Null LifeCycleState");
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

            if (myEC == ((JDOStateManager)sm).getExecutionContext())
            {
                NucleusLogger.PERSISTENCE.debug(">> SM.replacingStateManager this=" + this + " sm=" + sm + " with same EC");
                // This is a race condition when makePersistent or
                // makeTransactional is called on the same PC instance for the
                // same PM. It has been already set to this SM - just 
                // disconnect the other one. Return this SM so it won't be
                // replaced.
                ((JDOStateManager)sm).disconnect();
                return this;
            }

            throw myEC.getApiAdapter().getUserExceptionForException(LOCALISER.msg("026003"), null);
        }
        else if (pc == savedImage)
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
     * This happens when we want to get an object for an id and create a Hollow object, and then validate
     * against the datastore. This validation can pull in a new object graph from the datastore (e.g for DB4O)
     * @param pc The PersistenceCapable to use
     */
    public void replaceManagedPC(Object pc)
    {
        if (pc == null)
        {
            return;
        }

        // Swap the StateManager on the objects
        replaceStateManager((PersistenceCapable)pc, this);
        replaceStateManager(myPC, null);

        // Swap our object
        myPC = (PersistenceCapable) pc;

        // Put it in the cache in case the previous object was stored
        myEC.putObjectIntoLevel1Cache(this);
    }

    /**
     * Accessor for the PersistenceManager that owns this instance.
     * @param pc The PersistenceCapable instance
     * @return The PersistenceManager that owns this instance
     */
    public javax.jdo.PersistenceManager getPersistenceManager(PersistenceCapable pc)
    {
        //in identifying relationships, jdoCopyKeyFieldsFromId will call
        //this method, and at this moment, myPC in statemanager is null
        // Currently AbstractPersistenceManager.java putObjectInCache prevents any identifying relation object being put in L2

        //if not identifying relationship, do the default check of disconnectClone:
        //"this.disconnectClone(pc)"
        if (myPC != null && this.disconnectClone(pc))
        {
            return null;
        }
        else if (myEC == null)
        {
            return null;
        }
        else
        {
            myEC.hereIsObjectProvider(this, myPC);
            return (PersistenceManager) myEC.getOwner();
        }
    }

    // -------------------------- Lifecycle Methods ---------------------------

    /**
     * Tests whether this object is dirty.
     *
     * Instances that have been modified, deleted, or newly
     * made persistent in the current transaction return true.
     * <P>Transient nontransactional instances return false (JDO spec).
     * @see PersistenceCapable#jdoMakeDirty(String fieldName)
     * @param pc the calling persistable instance
     * @return true if this instance has been modified in current transaction.
     */
    public boolean isDirty(PersistenceCapable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        else
        {
            return myLC.isDirty();
        }
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
    public boolean isTransactional(PersistenceCapable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        else
        {
            return myLC.isTransactional();
        }
    }

    /**
     * Tests whether this object is persistent.
     * Instances whose state is stored in the data store return true.
     * Transient instances return false.
     * @param pc the calling persistable instance
     * @return true if this instance is persistent.
     */
    public boolean isPersistent(PersistenceCapable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        else
        {
            return myLC.isPersistent();
        }
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
    public boolean isNew(PersistenceCapable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        else
        {
            return myLC.isNew();
        }
    }

    /**
     * Tests whether this object has been deleted.
     * Instances that have been deleted in the current transaction return true.
     * <P>Transient instances return false.
     * @param pc the calling persistable instance
     * @return true if this instance was deleted in the current transaction.
     */
    public boolean isDeleted(PersistenceCapable pc)
    {
        if (disconnectClone(pc))
        {
            return false;
        }
        else
        {
            return myLC.isDeleted();
        }
    }

    // -------------------------- Version handling ----------------------------

    /** 
     * Return the object representing the version of the calling instance.
     * @param pc the calling persistable instance
     * @return the object representing the version of the calling instance
     * @since JDO 2.0
     */    
    public Object getVersion(PersistenceCapable pc)
    {
        if (pc == myPC)
        {
            // JIRA-2993 This used to return myVersion but now we use transactionalVersion
            return transactionalVersion;
        }
        else
        {
            return null;
        }
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
            persistenceFlags = PersistenceFlags.LOAD_REQUIRED;
            myPC.jdoReplaceFlags();

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
            int[] nonpkFields = cmd.getNonPKMemberPositions();
            clearFieldsByNumbers(nonpkFields);
            clearDirtyFlags(nonpkFields);

            if (myEC.getStoreManager() instanceof ObjectReferencingStoreManager)
            {
                // For datastores that manage the object reference
                ((ObjectReferencingStoreManager)myEC.getStoreManager()).notifyObjectIsOutdated(this);
            }

            persistenceFlags = PersistenceFlags.LOAD_REQUIRED;
            myPC.jdoReplaceFlags();

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

        persistenceFlags = PersistenceFlags.LOAD_REQUIRED;
        myPC.jdoReplaceFlags();
        ClassUtils.clearFlags(loadedFields);
    }

    /**
     * The StateManager uses this method to supply the value of jdoFlags to the
     * associated PersistenceCapable instance.
     * @param pc the calling PersistenceCapable instance
     * @return the value of jdoFlags to be stored in the PersistenceCapable instance
     */
    public byte replacingFlags(PersistenceCapable pc)
    {
        // If this is a clone, return READ_WRITE_OK.
        if (pc != myPC)
        {
            return PersistenceFlags.READ_WRITE_OK;
        }
        else
        {
            return persistenceFlags;
        }
    }

    // -------------------------- providedXXXField Methods ----------------------------

    /**
     * This method is called from the associated persistable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedBooleanField(PersistenceCapable pc, int fieldNumber, boolean currentValue)
    {
        currFM.storeBooleanField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedByteField(PersistenceCapable pc, int fieldNumber, byte currentValue)
    {
        currFM.storeByteField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated PersistenceCapable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedCharField(PersistenceCapable pc, int fieldNumber, char currentValue)
    {
        currFM.storeCharField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated PersistenceCapable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedDoubleField(PersistenceCapable pc, int fieldNumber, double currentValue)
    {
        currFM.storeDoubleField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated PersistenceCapable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedFloatField(PersistenceCapable pc, int fieldNumber, float currentValue)
    {
        currFM.storeFloatField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated PersistenceCapable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedIntField(PersistenceCapable pc, int fieldNumber, int currentValue)
    {
        currFM.storeIntField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated PersistenceCapable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedLongField(PersistenceCapable pc, int fieldNumber, long currentValue)
    {
        currFM.storeLongField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated PersistenceCapable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedShortField(PersistenceCapable pc, int fieldNumber, short currentValue)
    {
        currFM.storeShortField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedStringField(PersistenceCapable pc, int fieldNumber, String currentValue)
    {
        currFM.storeStringField(fieldNumber, currentValue);
    }

    /**
     * This method is called from the associated persistable when its
     * PersistenceCapable.jdoProvideFields() method is invoked. Its purpose is
     * to provide the value of the specified field to the StateManager.
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     */
    public void providedObjectField(PersistenceCapable pc, int fieldNumber, Object currentValue)
    {
        currFM.storeObjectField(fieldNumber, currentValue);
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
     * Method to retrieve the value of a field from the PC object.
     * Assumes that it is loaded.
     * @param pc The PC object
     * @param fieldNumber Number of field
     * @return The value of the field
     */
    protected Object provideField(PersistenceCapable pc, int fieldNumber)
    {
        Object obj;
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.getLock().lock();
                lock.lock();
            }

            FieldManager prevFM = currFM;
            currFM = new SingleValueFieldManager();
            try
            {
                pc.jdoProvideField(fieldNumber);
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
                lock.unlock();
                myEC.getLock().unlock();
            }
        }

        return obj;
    }

    /**
     * Called from the StoreManager after StoreManager.update() is called to obtain updated values 
     * from the PersistenceCapable associated with this StateManager.
     * @param fieldNumbers An array of field numbers to be updated by the Store
     * @param fm The updated values are stored in this object. This object is only valid
     *   for the duration of this call.
     */
    public void provideFields(int fieldNumbers[], FieldManager fm)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.getLock().lock();
                lock.lock();
            }

            FieldManager prevFM = currFM;
            currFM = fm;
            try
            {
                // This will respond by calling this.providedXXXFields() with the value of the field
                myPC.jdoProvideFields(fieldNumbers);
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
                lock.unlock();
                myEC.getLock().unlock();
            }
        }
    }

    // -------------------------- replacingXXXField Methods ----------------------------

    /**
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     *
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setBooleanField(PersistenceCapable pc, int fieldNumber, boolean currentValue, boolean newValue)
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
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     *
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setByteField(PersistenceCapable pc, int fieldNumber, byte currentValue, byte newValue)
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
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     *
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setCharField(PersistenceCapable pc, int fieldNumber, char currentValue, char newValue)
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
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     *
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setDoubleField(PersistenceCapable pc, int fieldNumber, double currentValue, double newValue)
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
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     *
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setFloatField(PersistenceCapable pc, int fieldNumber, float currentValue, float newValue)
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
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     *
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setIntField(PersistenceCapable pc, int fieldNumber, int currentValue, int newValue)
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
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     *
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setLongField(PersistenceCapable pc, int fieldNumber, long currentValue, long newValue)
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
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setShortField(PersistenceCapable pc, int fieldNumber, short currentValue, short newValue)
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
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setStringField(PersistenceCapable pc, int fieldNumber, String currentValue, String newValue)
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

            if (!loadedFields[fieldNumber] || 
                !(currentValue == null ? (newValue == null) : (currentValue.equals(newValue))))
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE)
                {
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
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
     * This method is called by the associated PersistenceCapable when the
     * corresponding mutator method (setXXX()) is called on the PersistenceCapable.
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @param currentValue the current value of the field
     * @param newValue the new value for the field
     */
    public void setObjectField(PersistenceCapable pc, int fieldNumber, Object currentValue, Object newValue)
    {
        if (currentValue != null && currentValue != newValue && currentValue instanceof PersistenceCapable)
        {
            // Where the object is embedded, remove the owner from its old value since it is no longer managed by this StateManager
            JDOStateManager currentSM = (JDOStateManager)myEC.findObjectProvider(currentValue);
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

                if (relationType != RelationType.NONE && newValue == null && 
                    (mmd.isDependent() || mmd.isCascadeRemoveOrphans()))
                {
                    // Field being nulled and is dependent so load the existing value so it can be deleted
                    loadField(fieldNumber);
                    loadedOldValue = true;
                    oldValue = provideField(fieldNumber);
                }
                // TODO When field has relation consider loading it always for managed relations
            }

            // Check equality of old and new values
            boolean equal = false;
            if (oldValue == null && newValue == null)
            {
                equal = true;
            }
            else if (oldValue != null && newValue != null)
            {
                if (oldValue instanceof PersistenceCapable)
                {
                    // PC object field so compare object equality
                    // See JDO2 [5.4] "The JDO implementation must not use the application's hashCode and equals methods 
                    // from the persistence-capable classes except as needed to implement the Collections Framework" 
                    if (oldValue == newValue)
                    {
                        equal = true;
                    }
                }
                else
                {
                    // Non-PC object field so compare using equals()
                    if (oldValue.equals(newValue))
                    {
                        equal = true;
                    }
                }
            }

            // Update the field
            boolean needsSCOUpdating = false;
            if (!loadedFields[fieldNumber] || !equal || mmd.hasArray())
            {
                if (cmd.getIdentityType() == IdentityType.NONDURABLE && relationType == RelationType.NONE)
                {
                    String key = ObjectProvider.ORIGINAL_FIELD_VALUE_KEY_PREFIX + fieldNumber;
                    if (!containsAssociatedValue(key))
                    {
                        setAssociatedValue(key, oldValue);
                    }
                }

                // Either field isn't loaded, or has changed, or is an array.
                // We include arrays here since we have no way of knowing if the array element has changed
                // except if the user sets the array field. See JDO2 [6.3] that the application should
                // replace the value with its current value.
                if (oldValue instanceof SCO)
                {
                    if (oldValue instanceof SCOContainer)
                    {
                        // Make sure container values are loaded
                        ((SCOContainer)oldValue).load();
                    }
                    ((SCO) oldValue).unsetOwner();
                }
                if (newValue instanceof SCO)
                {
                    SCO sco = (SCO) newValue;
                    Object owner = sco.getOwner();
                    if (owner != null)
                    {
                        throw myEC.getApiAdapter().getUserExceptionForException(LOCALISER.msg("026007", sco.getFieldName(), owner), null);
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

            if (!equal && RelationType.isBidirectional(relationType)&& myEC.getManageRelations())
            {
                // Managed Relationships - add the field to be managed so we can analyse its value at flush
                myEC.getRelationshipManager(this).relationChange(fieldNumber, oldValue, newValue);
            }

            if (needsSCOUpdating)
            {
                // Wrap with SCO so we can detect future updates
                newValue = wrapSCOField(fieldNumber, newValue, false, true, true);
            }

            if (oldValue != null && newValue == null && oldValue instanceof PersistenceCapable)
            {
                if (mmd.isDependent() || mmd.isCascadeRemoveOrphans())
                {
                    if (myEC.getApiAdapter().isPersistent(oldValue))
                    {
                        // PC field being nulled, so delete previous PC value
                        NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("026026", oldValue, mmd.getFullFieldName()));
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
    protected void updateField(PersistenceCapable pc, int fieldNumber, Object value)
    {
        boolean wasDirty = dirty;

        /*
         * If we're writing a field in the process of inserting it must be due 
         * to jdoPreStore().  We haven't actually done the INSERT yet so we 
         * don't want to mark anything as dirty, which would make us want to do 
         * an UPDATE later. 
         */
        if (activity != ActivityState.INSERTING && activity != ActivityState.INSERTING_CALLBACKS)
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
        if (/*!myLC.isDirty && */activity == ActivityState.NONE && !isFlushing() && 
            !(myLC.isTransactional() && !myLC.isPersistent()))
        {
            // Not during flush, and not transactional-transient, and not inserting - so mark as dirty
            myEC.markDirty(this, true);
        }
    }

    /**
     * This method is invoked by the PersistenceCapable object's
     * jdoReplaceField() method to refresh the value of a boolean field.
     * @param pc the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public boolean replacingBooleanField(PersistenceCapable pc, int fieldNumber)
    {
        boolean value = currFM.fetchBooleanField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the PersistenceCapable object's
     * jdoReplaceField() method to refresh the value of a byte field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public byte replacingByteField(PersistenceCapable obj, int fieldNumber)
    {
        byte value = currFM.fetchByteField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the PersistenceCapable object's
     * jdoReplaceField() method to refresh the value of a char field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public char replacingCharField(PersistenceCapable obj, int fieldNumber)
    {
        char value = currFM.fetchCharField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the PersistenceCapable object's
     * jdoReplaceField() method to refresh the value of a double field.
     * @param obj the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public double replacingDoubleField(PersistenceCapable obj, int fieldNumber)
    {
        double value = currFM.fetchDoubleField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the PersistenceCapable object's
     * jdoReplaceField() method to refresh the value of a float field.
     * @param obj the calling PersistenceCapable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public float replacingFloatField(PersistenceCapable obj, int fieldNumber)
    {
        float value = currFM.fetchFloatField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's
     * jdoReplaceField() method to refresh the value of a int field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public int replacingIntField(PersistenceCapable obj, int fieldNumber)
    {
        int value = currFM.fetchIntField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's
     * jdoReplaceField() method to refresh the value of a long field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public long replacingLongField(PersistenceCapable obj, int fieldNumber)
    {
        long value = currFM.fetchLongField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's
     * jdoReplaceField() method to refresh the value of a short field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public short replacingShortField(PersistenceCapable obj, int fieldNumber)
    {
        short value = currFM.fetchShortField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's
     * jdoReplaceField() method to refresh the value of a String field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public String replacingStringField(PersistenceCapable obj, int fieldNumber)
    {
        String value = currFM.fetchStringField(fieldNumber);
        loadedFields[fieldNumber] = true;
        return value;
    }

    /**
     * This method is invoked by the persistable object's
     * jdoReplaceField() method to refresh the value of an Object field.
     * @param obj the calling persistable instance
     * @param fieldNumber the field number
     * @return the new value for the field
     */
    public Object replacingObjectField(PersistenceCapable obj, int fieldNumber)
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
     * Method to change the value of a field in the PC object.
     * @param pc The PC object
     * @param fieldNumber Number of field
     * @param value The new value of the field
     */
    protected void replaceField(PersistenceCapable pc, int fieldNumber, Object value)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.getLock().lock();
                lock.lock();
            }

            // Update the field in our PC object
            FieldManager prevFM = currFM;
            currFM = new SingleValueFieldManager();

            try
            {
                currFM.storeObjectField(fieldNumber, value);
                pc.jdoReplaceField(fieldNumber);
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
                lock.unlock();
                myEC.getLock().unlock();
            }
        }
    }

    /**
     * Method to disconnect any cloned persistence capable objects from their StateManager.
     * @param pc The PersistenceCapable object
     * @return Whether the object was disconnected.
     */
    protected boolean disconnectClone(PersistenceCapable pc)
    {
        if (isDetaching())
        {
            return false;
        }
        if (pc != myPC)
        {
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("026001", StringUtils.toJVMIDString(pc), this));
            }

            // Reset jdoFlags in the clone to PersistenceFlags.READ_WRITE_OK 
            // and clear its state manager.
            pc.jdoReplaceFlags();
            replaceStateManager(pc, null);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Convenience method to retrieve the detach state from the passed ObjectProvider's object.
     * @param op ObjectProvider
     */
    public void retrieveDetachState(ObjectProvider op)
    {
        if (op.getObject() instanceof Detachable)
        {
            ((AbstractStateManager)op).setRetrievingDetachedState(true);
            ((Detachable)op.getObject()).jdoReplaceDetachedState();
            ((AbstractStateManager)op).setRetrievingDetachedState(false);
        }
    }

    /**
     * Convenience method to reset the detached state in the current object.
     */
    public void resetDetachState()
    {
        if (getObject() instanceof Detachable)
        {
            setResettingDetachedState(true);
            try
            {
                ((Detachable)getObject()).jdoReplaceDetachedState();
            }
            finally
            {
                setResettingDetachedState(false);
            }
        }
    }

    /**
     * Method to update the "detached state" in the detached object to obtain the "detached state" 
     * from the detached object, or to reset it (to null).
     * @param pc The PersistenceCapable beind updated
     * @param currentState The current state values
     * @return The detached state to assign to the object
     */
    public Object[] replacingDetachedState(Detachable pc, Object[] currentState)
    {
        if (isResettingDetachedState())
        {
            return null;
        }
        else if (isRetrievingDetachedState())
        {
            // Retrieving the detached state from the detached object
            // Don't need the id or version since they can't change
            BitSet jdoLoadedFields = (BitSet)currentState[2];
            for (int i = 0; i < this.loadedFields.length; i++)
            {
                this.loadedFields[i] = jdoLoadedFields.get(i);
            }

            BitSet jdoModifiedFields = (BitSet)currentState[3];
            for (int i = 0; i < dirtyFields.length; i++)
            {
                dirtyFields[i] = jdoModifiedFields.get(i);
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

    // -------------------------- getXXXField Methods ----------------------------
    // Note that isLoaded() will always load the field if not loaded so these methods are never called

    public boolean getBooleanField(PersistenceCapable pc, int fieldNumber, boolean currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public byte getByteField(PersistenceCapable pc, int fieldNumber, byte currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public char getCharField(PersistenceCapable pc, int fieldNumber, char currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public double getDoubleField(PersistenceCapable pc, int fieldNumber, double currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public float getFloatField(PersistenceCapable pc, int fieldNumber, float currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public int getIntField(PersistenceCapable pc, int fieldNumber, int currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public long getLongField(PersistenceCapable pc, int fieldNumber, long currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public short getShortField(PersistenceCapable pc, int fieldNumber, short currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public String getStringField(PersistenceCapable pc, int fieldNumber, String currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
    }
    public Object getObjectField(PersistenceCapable pc, int fieldNumber, Object currentValue)
    {
        throw new NucleusException(LOCALISER.msg("026006"));
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
        String className = myEC.getStoreManager().getClassNameForObjectID(myID, clr, myEC);
        if (className == null)
        {
            // className is null when id class exists, and object has been validated and doesn't exist.
            throw new NucleusObjectNotFoundException(LOCALISER.msg("026013", 
                IdentityUtils.getIdentityAsString(myEC.getApiAdapter(), myID)), myID);
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
                NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("026014",
                    IdentityUtils.getIdentityAsString(myEC.getApiAdapter(), myID)));
                throw new NucleusUserException(LOCALISER.msg("026014",
                    IdentityUtils.getIdentityAsString(myEC.getApiAdapter(), myID)), e);
            }
            if (cmd == null)
            {
                throw new NucleusUserException(LOCALISER.msg("026012", pcClass)).setFatal();
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
                throw new NucleusUserException(LOCALISER.msg("026018", cmd.getFullClassName())).setFatal();
            }

            // Note that this will mean the fields are loaded twice (loaded earlier in this method)
            // and also that postLoad will be called twice
            loadFieldValues(fv);

            // Create the id for the new PC
            myID = myPC.jdoNewObjectIdInstance();
            if (!cmd.usesSingleFieldIdentityClass())
            {
                myPC.jdoCopyKeyFieldsToObjectId(myID);
            }
        }
    }

    /**
     * Convenience method to populate all fields in the PC object that have "value-strategy" specified
     * and that aren't datastore attributed. This applies not just to PK fields (where it is most
     * useful to use value-strategy) but also to any other field. Fields are populated only if they are null
     * This is called once on a PC object, when makePersistent is called.
     */
    private void populateStrategyFields()
    {
        int totalFieldCount = cmd.getNoOfInheritedManagedMembers() + cmd.getNoOfManagedMembers();

        for (int fieldNumber=0; fieldNumber<totalFieldCount; fieldNumber++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            IdentityStrategy strategy = mmd.getValueStrategy();

            // Check for the strategy, and if it is a datastore attributed strategy
            if (strategy != null && !myEC.getStoreManager().isStrategyDatastoreAttributed(cmd, fieldNumber))
            {
                // Assign the strategy value where required.
                // Default JDO2 behaviour is to always provide a strategy value when it is marked as using a strategy
                boolean applyStrategy = true;
                if (!mmd.getType().isPrimitive() &&
                    mmd.hasExtension("strategy-when-notnull") &&
                    mmd.getValueForExtension("strategy-when-notnull").equalsIgnoreCase("false") &&
                    this.provideField(fieldNumber) != null)
                {
                    // extension to only provide a value-strategy value where the field is null at persistence.
                    applyStrategy = false;
                }

                if (applyStrategy)
                {
                    // Apply a strategy value for this field
                    Object obj = myEC.getStoreManager().getStrategyValue(myEC, cmd, fieldNumber);
                    this.replaceField(fieldNumber, obj);
                }
            }
            else if (mmd.hasExtension("object-value-generator"))
            {
                // Field has object value-generator so generate value based on this object
                String valGenName = mmd.getValueForExtension("object-value-generator");
                ObjectValueGenerator valGen = getObjectValueGenerator(myEC, valGenName);
                Object value = valGen.generate(myEC, myPC, mmd.getExtensions());
                this.replaceField(myPC, fieldNumber, value, true);
            }
        }
    }

    /**
     * Convenience method to load the passed field values.
     * Loads the fields using any required fetch plan and calls jdoPostLoad() as appropriate.
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
     * Utility to set the identity for the PersistenceCapable object.
     * Creates the identity instance if the required PK field(s) are all already set (by the user, or by
     * a value-strategy). If the identity is set in the datastore (sequence, autoassign, etc) then this
     * will not set the identity.
     * @param afterPreStore Whether preStore has (just) been invoked
     */
    private void setIdentity(boolean afterPreStore)
    {
        if (cmd.isEmbeddedOnly())
        {
            // Embedded objects don't have an "identity"
            return;
        }

        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            if (cmd.getIdentityMetaData() == null || !myEC.getStoreManager().isStrategyDatastoreAttributed(cmd, -1))
            {
                // Assumed to be set
                myID = myEC.newObjectId(cmd.getFullClassName(), myPC);
            }
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            boolean idSetInDatastore = false;
            int totalFieldCount = cmd.getNoOfInheritedManagedMembers() + cmd.getNoOfManagedMembers();
            for (int fieldNumber=0; fieldNumber<totalFieldCount; fieldNumber++)
            {
                AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
                if (fmd.isPrimaryKey())
                {
                    if (myEC.getStoreManager().isStrategyDatastoreAttributed(cmd, fieldNumber))
                    {
                        idSetInDatastore = true;
                        break;
                    }
                    else if (cmd.usesSingleFieldIdentityClass())
                    {
                        if (this.provideField(fieldNumber) == null)
                        {
                            // PK field has not had its value set (user/value-strategy)
                            // and must be set for single-field identity
                            if (afterPreStore)
                            {
                                // Not set even after preStore, so user error
                                throw new NucleusUserException(LOCALISER.msg("026017", cmd.getFullClassName(), 
                                    fmd.getName())).setFatal();
                            }
                            else
                            {
                                // Log that the value is not yet set for this field, maybe set later in preStore?
                                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("026017", cmd.getFullClassName(),
                                    fmd.getName()));
                                return;
                            }
                        }
                    }
                }
            }

            if (!idSetInDatastore)
            {
                // Not generating the identity in the datastore so set it now
                myID = myEC.newObjectId(cmd.getFullClassName(), myPC);
            }
        }

        if (myInternalID != myID && myID != null && myEC.getApiAdapter().getIdForObject(myPC) != null)
        {
            // Update the id with the PM if it is changing
            myEC.replaceObjectId(myPC, myInternalID, myID);

            this.myInternalID = myID;
        }
    }

    /**
     * Convenience method to update our object with the field values from the passed object.
     * Objects need to be of the same type, and the other object should not have a StateManager.
     * TODO Only used by XML plugin so strip this out into a convenience method.
     * @param obj The object that we should copy fields from
     * @param fieldNumbers Numbers of fields to copy
     */
    public void copyFieldsFromObject(Object obj, int[] fieldNumbers)
    {
        if (obj == null)
        {
            return;
        }
        if (!obj.getClass().getName().equals(myPC.getClass().getName()))
        {
            return;
        }
        if (!(obj instanceof PersistenceCapable))
        {
            throw new NucleusUserException("Must be PersistenceCapable");
        }
        PersistenceCapable pc = (PersistenceCapable)obj;

        // Assign the new object to this StateManager temporarily so that we can copy its fields
        replaceStateManager(pc, this);
        myPC.jdoCopyFields(pc, fieldNumbers);

        // Remove the StateManager from the other object
        replaceStateManager(pc, null);

        // Set the loaded flags now that we have copied
        for (int i=0;i<fieldNumbers.length;i++)
        {
            loadedFields[fieldNumbers[i]] = true;
        }
    }

    /**
     * Marks the given field dirty.
     * @param fieldNumber The no of field to mark as dirty. 
     */
    public void makeDirty(int fieldNumber)
    {
        if (activity != ActivityState.DELETING)
        {
            // Mark dirty unless in the process of being deleted
            boolean wasDirty = preWriteField(fieldNumber);
            postWriteField(wasDirty);

            List<EmbeddedOwnerRelation> embeddedOwners = myEC.getOwnerInformationForEmbedded(this);
            if (embeddedOwners != null)
            {
                // Notify any owners that embed this object that it has just changed
                for (EmbeddedOwnerRelation owner : embeddedOwners)
                {
                    AbstractStateManager ownerOP = (AbstractStateManager) owner.getOwnerOP();

                    if (ownerOP == null || ownerOP.getClassMetaData() == null)
                    {
                        // for some reason these are null... raised when running JPA TCK
                        continue;
                    }

                    if ((ownerOP.flags&FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER)==0)
                    {
                        ownerOP.makeDirty(owner.getOwnerFieldNum());
                    }
                }
            }
        }
    }

    /**
     * Mark the associated PersistenceCapable field dirty.
     * @param pc the calling PersistenceCapable instance
     * @param fieldName the name of the field
     */
    public void makeDirty(PersistenceCapable pc, String fieldName)
    {
        if (!disconnectClone(pc))
        {
            int fieldNumber = cmd.getAbsolutePositionOfMember(fieldName);
            if (fieldNumber == -1)
            {
                throw myEC.getApiAdapter().getUserExceptionForException(
                    LOCALISER.msg("026002", fieldName, cmd.getFullClassName()), null);
            }

            makeDirty(fieldNumber);
        }
    }

    // -------------------------- Accessor Methods -----------------------------

    /**
     * Return the object representing the JDO identity of the calling instance.
     * According to the JDO specification, if the JDO identity is being changed in the current transaction, 
     * this method returns the JDO identify as of the beginning of the transaction.
     * @param pc the calling PersistenceCapable instance
     * @return the object representing the JDO identity of the calling instance
     */
    public Object getObjectId(PersistenceCapable pc)
    {
        if (disconnectClone(pc))
        {
            return null;
        }
        else
        {
            try
            {
                return getExternalObjectId(pc);
            }
            catch (NucleusException ne)
            {
                // This can be called from user-facing methods (e.g JDOHelper.getObjectId) so wrap any exception with API variant
                throw myEC.getApiAdapter().getApiExceptionForNucleusException(ne);
            }
        }
    }

    /**
     * Return the object representing the JDO identity of the calling instance.  
     * If the JDO identity is being changed in the current transaction, this method returns the 
     * current identity as changed in the transaction. In this implementation we don't allow
     * change of identity so this is always the same as the result of getObjectId(PersistenceCapable).
     *
     * @param pc the calling PersistenceCapable instance
     * @return the object representing the JDO identity of the calling instance
     */
    public Object getTransactionalObjectId(PersistenceCapable pc)
    {
        return getObjectId(pc);
    }

    /**
     * If the id is obtained after inserting the object into the database, set
     * new a new id for persistent classes (for example, increment).
     * @param id the id received from the datastore
     */
    public void setPostStoreNewObjectId(Object id)
    {
        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            if (id instanceof OID)
            {
                // Provided an OID direct
                myID = id;
            }
            else
            {
                // OID "key" value provided
                myID = OIDFactory.getInstance(myEC.getNucleusContext(), cmd.getFullClassName(), id);
            }
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            try
            {
                myID = null;

                int fieldCount = cmd.getMemberCount();
                for (int fieldNumber = 0; fieldNumber < fieldCount; fieldNumber++)
                {
                    AbstractMemberMetaData fmd=cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
                    if (fmd.isPrimaryKey() && myEC.getStoreManager().isStrategyDatastoreAttributed(cmd, fieldNumber))
                    {
                        //replace the value of the id, but before convert the value to the field type if needed
                        replaceField(myPC, fieldNumber, TypeConversionHelper.convertTo(id, fmd.getType()), false);
                    }
                }
            }
            catch (Exception e)
            {
                NucleusLogger.PERSISTENCE.error(e);
            }
            finally
            {
                myID = myEC.getApiAdapter().getNewApplicationIdentityObjectId(getObject(), cmd);
            }
        }

        if (myInternalID != myID && myID != null)
        {
            // Update the id with the ExecutionContext if it is changing
            myEC.replaceObjectId(myPC, myInternalID, myID);

            myInternalID = myID;
        }
    }

    /**
     * Return an object id that the user can use.
     * @param obj the PersistenceCapable object
     * @return the object id
     */
    protected Object getExternalObjectId(Object obj)
    {
        List<EmbeddedOwnerRelation> embeddedOwners = myEC.getOwnerInformationForEmbedded(this);
        if (embeddedOwners != null)
        {
            // Embedded object has no id
            return myID;
        }

        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            if (!isFlushing())
            {
                // Flush any datastore changes so that myID is set by the time we return
                if (!isFlushedNew() &&
                    activity != ActivityState.INSERTING && activity != ActivityState.INSERTING_CALLBACKS &&
                    myLC.stateType() == LifeCycleState.P_NEW)
                {
                    if (myEC.getStoreManager().isStrategyDatastoreAttributed(cmd, -1))
                    {
                        flush();
                    }
                }
            }
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            // Note that we always create a new application identity since it is mutable and we can't allow
            // the user to change it. The only drawback of this is that we *must* have the relevant fields
            // set when this method is called, so that the identity can be generated.
            if (!isFlushing())
            {
                // Flush any datastore changes so that we have all necessary fields populated
                // only if the datastore generates the field numbers
                if (!isFlushedNew() &&
                    activity != ActivityState.INSERTING && activity != ActivityState.INSERTING_CALLBACKS &&
                    myLC.stateType() == LifeCycleState.P_NEW)
                {
                    int[] pkFieldNumbers = cmd.getPKMemberPositions();
                    for (int i = 0; i < pkFieldNumbers.length; i++)
                    {
                        if (myEC.getStoreManager().isStrategyDatastoreAttributed(cmd, pkFieldNumbers[i]))
                        {
                            flush();
                            break;
                        }
                    }
                }
            }

            if (cmd.usesSingleFieldIdentityClass())
            {
                //SingleFieldIdentity classes are immutable.
                //Note, the instances of SingleFieldIdentity can be changed by the user using reflection,
                //but this is not allowed by the JDO spec
                return myID;
            }
            return myEC.getApiAdapter().getNewApplicationIdentityObjectId(myPC, cmd);
        }

        return myID;
    }

    /**
     * Return an object identity that can be used by the user for the managed object.
     * @return the object id
     */
    public Object getExternalObjectId()
    {
        return getExternalObjectId(myPC);
    }

    // --------------------------- Load Field Methods --------------------------

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

    /**
     * Fetch from the database all fields that are not currently loaded regardless of whether
     * they are in the current fetch group or not. Called by lifecycle transitions.
     */
    public void loadUnloadedFields()
    {
        int[] fieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, cmd.getAllMemberPositions(), false);
        if (fieldNumbers == null || fieldNumbers.length == 0)
        {
            // All loaded so return
            return;
        }

        if (preDeleteLoadedFields != null &&
            ((myLC.isDeleted() && myEC.isFlushing()) || activity == ActivityState.DELETING))
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

            // Make sure all SCO lazy-loaded fields have been loaded
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
            if (callPostLoad)
            {
                postLoad();
            }
        }
    }

    /**
     * Fetchs from the database all fields that are not currently loaded and that are in the current
     * fetch group. Called by lifecycle transitions.
     */
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
            if (callPostLoad)
            {
                postLoad();
            }
        }
    }

    /**
     * Fetchs from the database all fields in current fetch plan that are not currently loaded as well as
     * the version. Called by lifecycle transitions.
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
            if (callPostLoad && fieldNumbers.length > 0)
            {
                postLoad();
            }
        }
    }

    /**
     * Fetchs from the database all fields in the actual fetch plan.
     * Called by life-cycle transitions.
     */
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
            if (callPostLoad)
            {
                postLoad();
            }
        }
    }

    /**
     * Refreshes from the database all fields in fetch plan.
     * Called by life-cycle transitions when the object undergoes a "transitionRefresh".
     */
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

            if (cmd.hasRelations(myEC.getClassLoaderResolver(), myEC.getMetaDataManager()))
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
                            if (value instanceof Collection)
                            {
                                // Refresh any PC elements in the collection
                                // TODO This should replace the SCO wrapper with a new one, or reload the wrapper
                                SCOUtils.refreshFetchPlanFieldsForCollection(this, ((Collection)value).toArray());
                            }
                            else if (value instanceof Map)
                            {
                                // Refresh any PC keys/values in the map
                                // TODO This should replace the SCO wrapper with a new one, or reload the wrapper
                                SCOUtils.refreshFetchPlanFieldsForMap(this, ((Map)value).entrySet());
                            }
                            else if (value instanceof PersistenceCapable)
                            {
                                // Refresh any PC fields
                                myEC.refreshObject(value);
                            }
                        }
                    }
                }
            }

            if (callPostLoad)
            {
                postLoad();
            }

            getCallbackHandler().postRefresh(myPC);
        }
    }
    
    /**
     * Refreshes from the database all fields currently loaded.
     * Called by life-cycle transitions when making transactional or reading fields.
     */
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
            if (callPostLoad)
            {
                postLoad();
            }
        }
    }

    /**
     * Returns the loaded setting for the field of the managed object.
     * Refer to the javadoc of isLoaded(PersistenceCapable, int);
     * @param fieldNumber the absolute field number
     * @return always returns true (this implementation)
     */
    public boolean isLoaded(int fieldNumber)
    {
        return isLoaded(myPC, fieldNumber);
    }

    /**
     * Return true if the field is cached in the calling instance.
     * In this implementation, isLoaded() will always return true. 
     * If the field is not loaded, it will be loaded as a side effect of the 
     * call to this method. If it is in the default fetch group,
     * the default fetch group, including this field, will be loaded.
     *
     * @param pc the calling PersistenceCapable instance
     * @param fieldNumber the absolute field number
     * @return always returns true (this implementation)
     */
    public boolean isLoaded(PersistenceCapable pc, int fieldNumber)
    {
        try
        {
            if (disconnectClone(pc))
            {
                return true;
            }
            else
            {
                boolean checkRead = true;
                boolean beingDeleted = false;
                if ((myLC.isDeleted() && myEC.isFlushing()) || activity == ActivityState.DELETING)
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
                    if (objectType != ObjectProvider.PC)
                    {
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
                        // Load rest of FetchPlan if this is part of it (and not in the process of deletion)
                        loadUnloadedFieldsInFetchPlan();
                    }
                    else
                    {
                        // Just load this field
                        loadSpecifiedFields(new int[] {fieldNumber});
                    }
                }

                return true;
            }
        }
        catch (NucleusException ne)
        {
            NucleusLogger.PERSISTENCE.warn("Exception thrown by StateManager.isLoaded", ne);

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
    protected void replaceField(PersistenceCapable pc, int fieldNumber, Object value, boolean makeDirty)
    {
        List<EmbeddedOwnerRelation> embeddedOwners = myEC.getOwnerInformationForEmbedded(this);
        if (embeddedOwners != null)
        {
            // Notify any owners that embed this object that it has just changed
            // We do this before we actually change the object so we can compare with the old value
            Iterator<EmbeddedOwnerRelation> ownerRelIter = embeddedOwners.iterator();
            while (ownerRelIter.hasNext())
            {
                EmbeddedOwnerRelation ownerRel = ownerRelIter.next();
                AbstractStateManager ownerOP = (AbstractStateManager) ownerRel.getOwnerOP();

                if (ownerOP == null || ownerOP.getClassMetaData() == null)
                {
                    //for some reason these are null... raised when running JPA TCK
                    continue;
                }

                AbstractMemberMetaData ownerMmd = ownerOP.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(ownerRel.getOwnerFieldNum());
                if (ownerMmd.getCollection() != null)
                {
                    // PC Object embedded in collection
                    Object ownerField = ownerOP.provideField(ownerRel.getOwnerFieldNum());
                    if (ownerField instanceof SCOCollection)
                    {
                        ((SCOCollection)ownerField).updateEmbeddedElement(myPC, fieldNumber, value);
                    }
                }
                else if (ownerMmd.getMap() != null)
                {
                    // PC Object embedded in map
                    Object ownerField = ownerOP.provideField(ownerRel.getOwnerFieldNum());
                    if (ownerField instanceof SCOMap)
                    {
                        if (objectType == ObjectProvider.EMBEDDED_MAP_KEY_PC)
                        {
                            ((SCOMap)ownerField).updateEmbeddedKey(myPC, fieldNumber, value);
                        }
                        if (objectType == ObjectProvider.EMBEDDED_MAP_VALUE_PC)
                        {
                            ((SCOMap)ownerField).updateEmbeddedValue(myPC, fieldNumber, value);
                        }
                    }
                }
                else
                {
                    // PC Object embedded in PC object
                    if ((ownerOP.flags&FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER)==0)
                    {
                        // Update the owner when one of our fields have changed, EXCEPT when they have just
                        // notified us of our owner field!
                        if (makeDirty)
                        {
                            ownerOP.replaceFieldMakeDirty(ownerRel.getOwnerFieldNum(), pc);
                        }
                        else
                        {
                            ownerOP.replaceField(ownerRel.getOwnerFieldNum(), pc);
                        }
                    }
                }
            }
        }

        // Update the field in our PC object
        // TODO Why don't we mark as dirty if non-tx ? Maybe need P_NONTRANS_DIRTY
        if (embeddedOwners == null && makeDirty && !myLC.isDeleted() && myEC.getTransaction().isActive())
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

    /**
     * Called from the StoreManager to refresh data in the PersistenceCapable
     * object associated with this StateManager.
     * @param fieldNumbers An array of field numbers to be refreshed by the Store
     * @param fm The updated values are stored in this object. This object is only valid
     *   for the duration of this call.
     * @param replaceWhenDirty Whether to replace the fields when they are dirty here
     */
    public void replaceFields(int fieldNumbers[], FieldManager fm, boolean replaceWhenDirty)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.getLock().lock();
                lock.lock();
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
                    myPC.jdoReplaceFields(fieldsToReplace);
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
                lock.unlock();
                myEC.getLock().unlock();
            }
        }
    }

    /**
     * Called from the StoreManager to refresh data in the PersistenceCapable
     * object associated with this StateManager.
     * @param fieldNumbers An array of field numbers to be refreshed by the Store
     * @param fm The updated values are stored in this object. This object is only valid
     *   for the duration of this call.
     */
    public void replaceFields(int fieldNumbers[], FieldManager fm)
    {
        replaceFields(fieldNumbers, fm, true);
    }

    /**
     * Called from the StoreManager to refresh data in the PersistenceCapable
     * object associated with this StateManager. Only not loaded fields are refreshed
     *
     * @param fieldNumbers An array of field numbers to be refreshed by the Store
     * @param fm The updated values are stored in this object. This object is only valid
     *   for the duration of this call.
     */
    public void replaceNonLoadedFields(int fieldNumbers[], FieldManager fm)
    {
        try
        {
            if (myEC.getMultithreaded())
            {
                myEC.getLock().lock();
                lock.lock();
            }

            FieldManager prevFM = currFM;
            currFM = fm;

            boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
            try
            {
                int[] fieldsToReplace = ClassUtils.getFlagsSetTo(loadedFields, fieldNumbers, false);
                if (fieldsToReplace != null && fieldsToReplace.length > 0)
                {
                    myPC.jdoReplaceFields(fieldsToReplace);
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
                lock.unlock();
                myEC.getLock().unlock();
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
                    wrapSCOField(i, value, false, false, true);
                }
            }
        }
    }

    /**
     * Method to replace all loaded SCO fields that have wrappers with their value.
     * If the loaded field doesnt have a SCO wrapper nothing happens to that field.
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
                    unwrapSCOField(i, value, true);
                }
            }
        }
    }

    /**
     * Method to unwrap a SCO field (if it is wrapped currently).
     * If the field is not a SCO field will just return the value.
     * If "replaceFieldIfChanged" is set, we replace the value in the object with the unwrapped value.
     * @param fieldNumber The field number
     * @param value The value for the field
     * @param replaceFieldIfChanged Whether to replace the field value in the object if unwrapping the value
     * @return The unwrapped field value
     */
    public Object unwrapSCOField(int fieldNumber, Object value, boolean replaceFieldIfChanged)
    {
        if (value == null)
        {
            return value;
        }
        if (cmd.getSCOMutableMemberFlags()[fieldNumber] && value instanceof SCO)
        {
            SCO sco = (SCO)value;

            // Not a SCO wrapper, or is a SCO wrapper but not owned by this object
            Object unwrappedValue = sco.getValue();
            if (replaceFieldIfChanged)
            {
                AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("026030",
                        StringUtils.toJVMIDString(myPC),
                        IdentityUtils.getIdentityAsString(myEC.getApiAdapter(), myID), fmd.getName()));
                }
                replaceField(myPC, fieldNumber, unwrappedValue, false);
            }
            return unwrappedValue;
        }
        return value;
    }

    /**
     * Method to create a new SCO wrapper for the specified field.
     * If the field is not a SCO field will just return the value.
     * @param fieldNumber The field number
     * @param value The value to initialise the wrapper with (if any)
     * @param forInsert Whether the creation of any wrapper should insert this value into the datastore
     * @param forUpdate Whether the creation of any wrapper should update the datastore with this value
     * @param replaceFieldIfChanged Whether to replace the field in the object if wrapping the value
     * @return The wrapper (or original value if not wrappable)
     */
    public Object wrapSCOField(int fieldNumber, Object value, 
            boolean forInsert, boolean forUpdate, boolean replaceFieldIfChanged)
    {
        if (value == null)
        {
            // We don't wrap null objects currently
            return value;
        }

        if (value instanceof PersistenceCapable)
        {
            // Special case of SCO that we should split into a separate method for clarity, nothing to do with wrapping
            AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            if (fmd.getEmbeddedMetaData() != null && fmd.getEmbeddedMetaData().getOwnerMember() != null)
            {
                // Embedded field, so assign the embedded/serialised object "owner-field" if specified
                AbstractStateManager subSM = (AbstractStateManager)myEC.findObjectProvider(value);
                int ownerAbsFieldNum = subSM.getClassMetaData().getAbsolutePositionOfMember(fmd.getEmbeddedMetaData().getOwnerMember());
                if (ownerAbsFieldNum >= 0)
                {
                    flags |= FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER;
                    subSM.replaceFieldMakeDirty(ownerAbsFieldNum, myPC);
                    flags &= ~FLAG_UPDATING_EMBEDDING_FIELDS_WITH_OWNER;
                }
            }
        }

        if (cmd.getSCOMutableMemberFlags()[fieldNumber])
        {
            if (!(value instanceof SCO) || myPC != ((SCO)value).getOwner())
            {
                // Not a SCO wrapper, or is a SCO wrapper but not owned by this object
                AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
                if (replaceFieldIfChanged)
                {
                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("026029", 
                            StringUtils.toJVMIDString(myPC), 
                            myEC != null ? IdentityUtils.getIdentityAsString(myEC.getApiAdapter(), myID) : myID, fmd.getName()));
                    }
                }
                return SCOUtils.newSCOInstance(this, fmd, fmd.getType(), 
                    value.getClass(), value, forInsert, forUpdate, replaceFieldIfChanged);
            }
        }

        return value;
    }

    // ------------------------- Lifecycle Methods -----------------------------

    /**
     * Method to mark an object for reachability.
     * Provides the basis for "persistence-by-reachability", but run at commit time only.
     * The reachability algorithm is also run at makePersistent, but directly via InsertRequest.
     * @param reachables List of object ids currently logged as reachable
     */
    public void runReachability(Set reachables)
    {
        if (reachables == null)
        {
            return;
        }
        if (!reachables.contains(getInternalObjectId()))
        {
            // Make sure all changes are persisted
            flush();

            if (isDeleted(myPC))
            {
                // This object is deleted so nothing further will be reachable
                return;
            }

            // This object was enlisted so make sure all of its fields are loaded before continuing
            if (myEC.isEnlistedInTransaction(getInternalObjectId()))
            {
                loadUnloadedFields();
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("007000", 
                    StringUtils.toJVMIDString(myPC), getInternalObjectId(), myLC));
            }
            // Add this object id since not yet reached
            reachables.add(getInternalObjectId());

            // Go through all (loaded FetchPlan) fields for reachability using ReachabilityFieldManager
            int[] loadedFieldNumbers = ClassUtils.getFlagsSetTo(loadedFields, cmd.getAllMemberPositions(), true);
            if (loadedFieldNumbers != null && loadedFieldNumbers.length > 0)
            {
                provideFields(loadedFieldNumbers, new ReachabilityFieldManager(this, reachables));
            }
        }
    }

    /**
     * Method to make the object persistent.
     */
    public void makePersistent()
    {
        if (myLC.isDeleted() && !myEC.getNucleusContext().getApiAdapter().allowPersistOfDeletedObject())
        {
            // API doesnt allow repersist of deleted objects
            return;
        }
        if (activity != ActivityState.NONE)
        {
            // Already making persistent
            return;
        }

        if (dirty && !myLC.isDeleted() && myLC.isTransactional() && myEC.isDelayDatastoreOperationsEnabled())
        {
            // Already provisionally persistent, but delaying til commit so just re-run reachability
            // to bring in any new objects that are now reachable
            if (cmd.hasRelations(myEC.getClassLoaderResolver(), myEC.getMetaDataManager()))
            {
                provideFields(cmd.getAllMemberPositions(), new PersistFieldManager(this, false));
            }
            return;
        }

        getCallbackHandler().prePersist(myPC);
        // TODO Call prePersist for any embedded field objects

        if (isFlushedNew())
        {
            // With CompoundIdentity bidir relations when the SM is created for this object ("initialiseForPersistentNew") the persist
            // of the PK PC fields can cause the flush of this object, and so it is already persisted by the time we ge here
            registerTransactional();
            return;
        }

        if (cmd.isEmbeddedOnly())
        {
            // Cant persist an object of this type since can only be embedded
            return;
        }

        // If this is an embedded/serialised object becoming persistent in its own right, assign an identity.
        if (myID == null)
        {
            setIdentity(false);
        }

        dirty = true;

        if (myEC.isDelayDatastoreOperationsEnabled())
        {
            // Delaying datastore flush til later
            myEC.markDirty(this, false);
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("026028", StringUtils.toJVMIDString(myPC)));
            }
            registerTransactional();

            if (myLC.isTransactional() && myLC.isDeleted())
            {
                // Re-persist of a previously deleted object
                myLC = myLC.transitionMakePersistent(this);
            }

            if (cmd.hasRelations(myEC.getClassLoaderResolver(), myEC.getMetaDataManager()))
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
        activity = ActivityState.INSERTING;
        boolean[] tmpDirtyFields = dirtyFields.clone();
        try
        {
            getCallbackHandler().preStore(myPC); // This comes after setting the INSERTING flag so we know we are inserting it now
            if (myID == null)
            {
                setIdentity(true); // Just in case user is setting it in preStore
            }

            //in InstanceLifecycleEvents this object could get dirty if a field is changed in preStore or
            //postCreate, we clear dirty flags to make sure this object will not be flushed again
            clearDirtyFlags();

            myEC.getStoreManager().getPersistenceHandler().insertObject(this);
            setFlushedNew(true);

            getCallbackHandler().postStore(myPC);

            if (!isEmbedded())
            {
                // Update the object in the cache(s) - has version set etc now
                myEC.putObjectIntoLevel1Cache(this);
            }
        }
        catch (NotYetFlushedException ex)
        {
            // can happen on cyclic relationships with RDBMS
            // if not yet flushed error, we rollback dirty fields, so we can retry inserting
            dirtyFields = tmpDirtyFields;
            myEC.markDirty(this, false);
            dirty = true;
            //we throw exception, so the owning relationship will mark it's foreign key to update later
            throw ex;
        }
        finally
        {
            activity = ActivityState.NONE;
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
                final JDOStateManager thisSM = this;
                myLC = myEC.getNucleusContext().getApiAdapter().getLifeCycleState(LifeCycleState.T_CLEAN);

                try
                {
                    if (myLC.isPersistent())
                    {
                        myEC.addObjectProvider(this);
                    }

                    // Everything OK so far. Now we can set SM reference in PC 
                    // It can be done only after myLC is set to deligate validation
                    // to the LC and objectId verified for uniqueness
                    replaceStateManager(myPC, thisSM);
                }
                catch (SecurityException e)
                {
                    throw new NucleusUserException(e.getMessage());
                }
                catch (NucleusException ne)
                {
                    if (myEC.findObjectProvider(myEC.getObjectFromCache(myID)) == this)
                    {
                        myEC.removeObjectProvider(this);
                    }
                    throw ne;
                }

                this.restoreValues = true;
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
        if (isMakingTransient())
        {
            return; // In the process of becoming transient
        }

        try
        {
            setMakingTransient(true);
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
            setMakingTransient(false);
        }
    }

    /**
     * Method to detach this object.
     * If the object is detachable then it will be migrated to DETACHED state, otherwise will migrate
     * to TRANSIENT. Used by "DetachAllOnCommit"/"DetachAllOnRollback"
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
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("010009", StringUtils.toJVMIDString(myPC), 
                    "" + state.getCurrentFetchDepth()));
            }

            // Call any "pre-detach" listeners
            getCallbackHandler().preDetach(myPC);
        }

        try
        {
            setDetaching(true);

            String detachedState = myEC.getNucleusContext().getPersistenceConfiguration().getStringProperty(PropertyNames.PROPERTY_DETACH_DETACHED_STATE);
            if (detachedState.equalsIgnoreCase("all"))
            {
                loadUnloadedFields();
            }
            else if (detachedState.equalsIgnoreCase("loaded"))
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
                        PersistenceCapable dummyPC = myPC.jdoNewInstance(this);
                        myPC.jdoCopyFields(dummyPC, unloadedFields);
                        replaceStateManager(dummyPC, null);
                    }
                }
            }

            // Detach all (loaded) fields in the FetchPlan
            FieldManager detachFieldManager = new DetachFieldManager(this, cmd.getSCOMutableMemberFlags(), 
                myFP, state, false);
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
                            org.datanucleus.state.JDOStateManager valueSM = (JDOStateManager) myEC.findObjectProvider(value);
                            if (!api.isDetached(value) && !(valueSM != null && valueSM.isDetaching()))
                            {
                                // Field value is not detached or being detached so unload it
                                String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(i).getName();
                                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                                {
                                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("026032", 
                                        StringUtils.toJVMIDString(myPC), 
                                        IdentityUtils.getIdentityAsString(myEC.getApiAdapter(), myID), fieldName));
                                }
                                unloadField(fieldName);
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
                myPC.jdoReplaceFlags();
                ((Detachable)myPC).jdoReplaceDetachedState();

                // Call any "post-detach" listeners
                getCallbackHandler().postDetach(myPC, myPC); // there is no copy, so give the same object

                PersistenceCapable toCheckPC = myPC;
                Object toCheckID = myID;
                disconnect();

                if (!toCheckPC.jdoIsDetached())
                {
                    // Sanity check on the objects detached state
                    throw new NucleusUserException(LOCALISER.msg("026025", toCheckPC.getClass().getName(), toCheckID));
                }
            }
            else
            {
                // Warn the user since they selected detachAllOnCommit
                NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("026031", myPC.getClass().getName(), 
                    IdentityUtils.getIdentityAsString(myEC.getApiAdapter(), myID)));

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
     * If the object is detachable then the copy will be migrated to DETACHED state, otherwise will migrate
     * the copy to TRANSIENT. Used by "ExecutionContext.detachObjectCopy()".
     * @param state State for the detachment process
     * @return the detached PersistenceCapable instance
     */
    public Object detachCopy(FetchPlanState state)
    {
        if (myLC.isDeleted())
        {
            throw new NucleusUserException(
                LOCALISER.msg("026023", myPC.getClass().getName(), myID));
        }
        if (myEC.getApiAdapter().isDetached(myPC))
        {
            throw new NucleusUserException(
                LOCALISER.msg("026024", myPC.getClass().getName(), myID));
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

        PersistenceCapable detachedPC;
        if (existingDetached == null)
        {
            // No existing detached copy - create new one
            detachedPC = myPC.jdoNewInstance(this);
            detachState.setDetachedCopyEntry(myPC, detachedPC);
        }
        else
        {
            // Found one - if it's sufficient for current FetchPlanState, return it immediately
            detachedPC = (PersistenceCapable) existingDetached.getDetachedCopyObject();
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
                    NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("010010", StringUtils.toJVMIDString(myPC), 
                        "" + state.getCurrentFetchDepth(), StringUtils.toJVMIDString(detachedPC),
                        StringUtils.intArrayToString(detachFieldNums),
                        StringUtils.intArrayToString(fieldsToLoad)));
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
                JDOStateManager smDetachedPC = new JDOStateManager(myEC, cmd);
                smDetachedPC.initialiseForDetached(detachedPC, getExternalObjectId(myPC), getVersion(myPC));
                myEC.setAttachDetachReferencedObject(smDetachedPC, myPC);

                // If detached copy already existed, take note of fields previously loaded
                if (existingDetached != null)
                {
                    smDetachedPC.retrieveDetachState(smDetachedPC);
                }

                smDetachedPC.replaceFields(detachFieldNums, new DetachFieldManager(this, 
                    cmd.getSCOMutableMemberFlags(), myFP, state, true));

                myEC.setAttachDetachReferencedObject(smDetachedPC, null);
                if (detachable)
                {
                    // Update the object with its detached state - not to be confused with the "state" object above
                    detachedPC.jdoReplaceFlags();
                    ((Detachable)detachedPC).jdoReplaceDetachedState();
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
                throw new NucleusUserException(LOCALISER.msg("026025", detachedPC.getClass().getName(), myID));
            }

            if (detachable)
            {
                // Call any "post-detach" listeners
                getCallbackHandler().postDetach(myPC, detachedPC);
            }
        }
        return detachedPC;
    }

    /**
     * Return an array of field numbers that must be included in the detached object
     * @return the field numbers array for detaching
     */
    private int[] getFieldsNumbersToDetach()
    {
        String detachedState = myEC.getNucleusContext().getPersistenceConfiguration().getStringProperty(
            PropertyNames.PROPERTY_DETACH_DETACHED_STATE);
        if (detachedState.equalsIgnoreCase("all"))
        {
            return cmd.getAllMemberPositions();
        }
        else if (detachedState.equalsIgnoreCase("loaded"))
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
            else
            {
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
        }
        else
        {
            if ((myEC.getFetchPlan().getDetachmentOptions() & FetchPlan.DETACH_LOAD_FIELDS) == 0)
            {
                // Return loaded fields that are in the FetchPlan
                return ClassUtils.getFlagsSetTo(loadedFields, myFP.getMemberNumbers(), true);
            }
            else
            {
                // Return FetchPlan fields
                return myFP.getMemberNumbers();
            }
        }
    }

    /**
     * Accessor for the referenced PC object when we are attaching or detaching.
     * When attaching and this is the detached object this returns the newly attached object.
     * When attaching and this is the newly attached object this returns the detached object.
     * When detaching and this is the newly detached object this returns the attached object.
     * When detaching and this is the attached object this returns the newly detached object.
     * @return The referenced object (or null).
     */
    public Object getReferencedPC()
    {
        return myEC.getAttachDetachReferencedObject(this);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.StateManager#attach(java.lang.Object)
     */
    public void attach(Object trans)
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
            JDOStateManager detachedSM = new JDOStateManager(myEC, cmd);
            detachedSM.initialiseForDetached(trans, myID, null);

            // Make sure the attached object is in the cache
            myEC.putObjectIntoLevel1Cache(this);

            int[] nonPKFieldNumbers = cmd.getNonPKMemberPositions();
            if (nonPKFieldNumbers != null && nonPKFieldNumbers.length > 0)
            {
                // Attach the (non-PK) fields from the transient
                NucleusLogger.GENERAL.debug("Attaching id=" + getInternalObjectId() +
                    " fields=" + StringUtils.intArrayToString(nonPKFieldNumbers));
                detachedSM.provideFields(nonPKFieldNumbers,
                    new AttachFieldManager(this, cmd.getSCOMutableMemberFlags(), cmd.getNonPKMemberFlags(),
                        true, true, false));
            }

            // Disconnect the transient object
            replaceStateManager((PersistenceCapable) trans, null);

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
                if (!myEC.getNucleusContext().getPersistenceConfiguration().getBooleanProperty(PropertyNames.PROPERTY_ATTACH_SAME_DATASTORE))
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
                NucleusLogger.GENERAL.debug("Attaching id=" + getInternalObjectId() +
                    " fields=" + StringUtils.intArrayToString(attachFieldNumbers));
                provideFields(attachFieldNumbers,
                    new AttachFieldManager(this, cmd.getSCOMutableMemberFlags(), dirtyFields,
                        persistent, true, false));
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
     * @param obj the detached persistable instance to be attached
     * @param embedded Whether the object is stored embedded/serialised in another object
     * @return The attached copy
     */
    public Object attachCopy(Object obj, boolean embedded)
    {
        if (isAttaching())
        {
            return myPC;
        }

        setAttaching(true);
        PersistenceCapable detachedPC = (PersistenceCapable)obj;
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
                if (!myEC.getNucleusContext().getPersistenceConfiguration().getBooleanProperty(PropertyNames.PROPERTY_ATTACH_SAME_DATASTORE))
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
                (myLC == myEC.getApiAdapter().getLifeCycleState(LifeCycleState.HOLLOW) ||
                 myLC == myEC.getApiAdapter().getLifeCycleState(LifeCycleState.P_NONTRANS)))
            {
                // Pessimistic txns and in HOLLOW/P_NONTRANS, so move to P_CLEAN
                // TODO Move this into the lifecycle state classes as a "transitionAttach"
                myLC = myLC.transitionMakeTransactional(this, persistent);
            }

            JDOStateManager smDetachedPC = null;
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
                smDetachedPC = new JDOStateManager(myEC, cmd);
                smDetachedPC.initialiseForDetached(detachedPC, getExternalObjectId(detachedPC), null);

                // Cross-reference the attached and detached objects for the attach process
                myEC.setAttachDetachReferencedObject(smDetachedPC, myPC);
                myEC.setAttachDetachReferencedObject(this, detachedPC);

                // Retrieve the updated values from the detached object
                retrieveDetachState(smDetachedPC);
            }
            else
            {
                // Attaching object that was detached from another datastore, so perform as replicate

                // Copy field values from detached to attached so we know what value will need inserting
                replaceStateManager(detachedPC, this);
                myPC.jdoCopyFields(detachedPC, cmd.getAllMemberPositions());
                replaceStateManager(detachedPC, null);

                // Add a state manager to the detached PC so that we can retrieve its detached state
                smDetachedPC = new JDOStateManager(myEC, cmd);
                smDetachedPC.initialiseForDetached(detachedPC, getExternalObjectId(detachedPC), null);

                // Cross-reference the attached and detached objects for the attach process
                myEC.setAttachDetachReferencedObject(smDetachedPC, myPC);
                myEC.setAttachDetachReferencedObject(this, detachedPC);

                // Retrieve the updated values from the detached object
                retrieveDetachState(smDetachedPC);

                // Object is not yet persisted so make it persistent
                // Make sure all field values in the attach object are ready for inserts (but dont trigger any cascade attaches)
                internalAttachCopy(smDetachedPC, smDetachedPC.loadedFields, smDetachedPC.dirtyFields, persistent, 
                    smDetachedPC.myVersion, false);

                makePersistent();
            }

            // Go through all related fields and attach them (including relationships)
            internalAttachCopy(smDetachedPC, smDetachedPC.loadedFields, smDetachedPC.dirtyFields, persistent, 
                smDetachedPC.myVersion, true);

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
            NucleusLogger.PERSISTENCE.debug("Unexpected exception thrown in attach", ne);
            throw ne;
        }
        finally
        {
            setAttaching(false);
        }
        return myPC;
    }

    /**
     * Attach the fields for this object using the provided detached object.
     * This will attach all loaded plus all dirty fields.
     * @param detachedOP ObjectProvider for the detached object.
     * @param loadedFields Fields that were detached with the object
     * @param dirtyFields Fields that have been modified while detached
     * @param persistent whether the object is already persistent
     * @param version the version
     * @param cascade Whether to cascade the attach to related fields
     */
    private void internalAttachCopy(ObjectProvider detachedOP,
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
            NucleusLogger.GENERAL.debug("Attaching id=" + getInternalObjectId() +
                " fields=" + StringUtils.intArrayToString(attachFieldNumbers));
            detachedOP.provideFields(attachFieldNumbers,
                new AttachFieldManager(this, cmd.getSCOMutableMemberFlags(), dirtyFields, persistent, cascade, true));
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
                    loadUnloadedFields();
                }
                setBecomingDeleted(true);

                // Run reachability for relations
                if (cmd.hasRelations(myEC.getClassLoaderResolver(), myEC.getMetaDataManager()))
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
                    for (int i=0;i<preDeleteLoadedFields.length;i++)
                    {
                        preDeleteLoadedFields[i] = loadedFields[i];
                    }

                    myLC = myLC.transitionDeletePersistent(this);
                }
                finally
                {
                    setBecomingDeleted(false);
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
                    for (int i=0;i<preDeleteLoadedFields.length;i++)
                    {
                        preDeleteLoadedFields[i] = loadedFields[i];
                    }

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

    /**
     * Nullify fields with reference to PersistenceCapable or SCO instances 
     */
    public void nullifyFields()
    {
        if (!myLC.isDeleted() && !myEC.getApiAdapter().isDetached(myPC))
        {
            // Update any relationships for fields of this object that aren't dependent
            replaceFields(cmd.getNonPKMemberPositions(), new NullifyRelationFieldManager(this));
            flush();
        }
    }

    /**
     * Validates whether the persistence capable instance exists in the datastore.
     * If the instance doesn't exist in the datastore, this method will fail raising a 
     * NucleusObjectNotFoundException. If the object is transactional then does nothing.
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
                transitionReadField(false);
                // Some fetch plan fields, or the version are not loaded so try to load them, and this by itself 
                // validates the existence. Loads the fields in the current FetchPlan (JDO2 spec 12.6.5)
                fieldNumbers = myFP.getMemberNumbers();
                if (fieldNumbers != null || versionNeedsLoading)
                {
                    boolean callPostLoad = myFP.isToCallPostLoadFetchPlan(this.loadedFields);
                    setTransactionalVersion(null); // Make sure we get the latest version
                    loadFieldsFromDatastore(fieldNumbers);
                    if (callPostLoad)
                    {
                        postLoad();
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
        /*
         * If we're writing a field in the process of inserting it must be due 
         * to jdoPreStore().  We haven't actually done the INSERT yet so we 
         * don't want to mark anything as dirty, which would make us want to do 
         * an UPDATE later. 
         */
        if (activity != ActivityState.INSERTING && activity != ActivityState.INSERTING_CALLBACKS)
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

        if (activity == ActivityState.NONE && !isFlushing() && !(myLC.isTransactional() && !myLC.isPersistent()))
        {
            if (isDetaching() && getReferencedPC() == null)
            {
                // detachAllOnCommit caused a field to be dirty so ignore it
                return;
            }
            else
            {
                // Not during flush, and not transactional-transient, and not inserting - so mark as dirty
                myEC.markDirty(this, true);
            }
        }
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

    /**
     * Called whenever the default fetch group fields have all been loaded.
     * Updates jdoFlags and calls jdoPostLoad() as appropriate.
     * <p>
     * If it's called in the midst of a life-cycle transition both actions will
     * be deferred until the transition is complete.
     * <em>This deferral is important</em>. Without it, we could enter user
     * code (jdoPostLoad()) while still making a state transition, and that way
     * lies madness.
     * <p>
     * As an example, consider a jdoPostLoad() that calls other enhanced methods
     * that read fields (jdoPostLoad() itself is not enhanced). A P_NONTRANS
     * object accessed within a transaction would produce the following infinite
     * loop:
     * <p>
     * <blockquote>
     * 
     * <pre>
     *  isLoaded()
     *  transitionReadField()
     *  refreshLoadedFields()
     *  jdoPostLoad()
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
            /*
             * A transactional object whose DFG fields are loaded does not need to contact us
             * in order to read those fields, so we can safely set READ_OK.
             * A non-transactional object needs to notify us on all field reads
             * so that we can decide whether or not any transition should occur,
             * so we leave the flags at LOAD_REQUIRED.
             */
            if (persistenceFlags == PersistenceFlags.LOAD_REQUIRED && myLC.isTransactional())
            {
                persistenceFlags = PersistenceFlags.READ_OK;
                myPC.jdoReplaceFlags();
            }

            getCallbackHandler().postLoad(myPC);
        }
    }

    /**
     * Guarantee that the serializable transactional and persistent fields are loaded into the instance. 
     * This method is called by the generated jdoPreSerialize method prior to serialization of the instance.
     * @param pc the calling PersistenceCapable instance
     */
    public void preSerialize(PersistenceCapable pc)
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
                // JDO2 spec "For Detachable classes, the jdoPreSerialize method must also initialize the jdoDetachedState
                // instance so that the detached state is serialized along with the instance."
                ((Detachable)pc).jdoReplaceDetachedState();
            }
        }
    }

    /**
     * Flushes any outstanding changes to the object to the datastore. 
     * This will process :-
     * <ul>
     * <li>Any objects that have been marked as provisionally persistent yet haven't been flushed to the 
     * datastore.</li>
     * <li>Any objects that have been marked as provisionally deleted yet haven't been flushed to the 
     * datastore.</li>
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
            if (activity == ActivityState.INSERTING || activity == ActivityState.INSERTING_CALLBACKS)
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
                        if (!isEmbedded() && dirtyFieldNumbers == null)
                        {
                            throw new NucleusException(LOCALISER.msg("026010")).setFatal();
                        }
                        if (myEC.getNucleusContext().isClassCacheable(getClassMetaData()))
                        {
                            myEC.markFieldsForUpdateInLevel2Cache(getInternalObjectId(), dirtyFields);
                        }
                        myEC.getStoreManager().getPersistenceHandler().updateObject(this, dirtyFieldNumbers);

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
     * Registers the pc class in the cache
     */
    public void registerTransactional()
    {
        myEC.addObjectProvider(this);
    }

    // ------------------------------ Helper Methods ---------------------------

    /**
     * Method to dump a persistable object to the specified PrintWriter.
     * @param pc The persistable object
     * @param out The PrintWriter
     */
    private static void dumpPC(Object pc, PrintWriter out)
    {
        out.println(StringUtils.toJVMIDString(pc));

        if (pc == null)
        {
            return;
        }

        out.print("jdoStateManager = " + peekField(pc, "jdoStateManager"));
        out.print("jdoFlags = ");
        Object flagsObj = peekField(pc, "jdoFlags");
        if (flagsObj instanceof Byte)
        {
            out.println(PersistenceFlags.persistenceFlagsToString(((Byte) flagsObj).byteValue()));
        }
        else
        {
            out.println(flagsObj);
        }

        Class c = pc.getClass();
        do
        {
            String[] fieldNames = HELPER.getFieldNames(c);
            for (int i = 0; i < fieldNames.length; ++i)
            {
                out.print(fieldNames[i]);
                out.print(" = ");
                out.println(peekField(pc, fieldNames[i]));
            }
            c = c.getSuperclass();
        }
        while (c != null && PersistenceCapable.class.isAssignableFrom(c));
    }

    /**
     * Utility to dump the contents of the StateManager.
     * @param out PrintWriter to dump to
     */
    public void dump(PrintWriter out)
    {
        out.println("myEC = " + myEC);
        out.println("myID = " + myID);
        out.println("myLC = " + myLC);
        out.println("cmd = " + cmd);
        out.println("fieldCount = " + cmd.getMemberCount());
        out.println("dirty = " + dirty);
        out.println("flushing = " + isFlushing());
        out.println("changingState = " + isChangingState());
        out.println("postLoadPending = " + isPostLoadPending());
        out.println("disconnecting = " + isDisconnecting());
        out.println("dirtyFields = " + StringUtils.booleanArrayToString(dirtyFields));
        out.println("getSecondClassMutableFields() = " + StringUtils.booleanArrayToString(cmd.getSCOMutableMemberFlags()));
        out.println("getAllFieldNumbers() = " + StringUtils.intArrayToString(cmd.getAllMemberPositions()));
        out.println("secondClassMutableFieldNumbers = " + StringUtils.intArrayToString(cmd.getSCOMutableMemberPositions()));

        out.println();
        out.println("persistenceFlags = " + PersistenceFlags.persistenceFlagsToString(persistenceFlags));
        out.println("loadedFields = " + StringUtils.booleanArrayToString(loadedFields));
        out.print("myPC = ");
        dumpPC(myPC, out);

        out.println();
        out.println("savedFlags = " + PersistenceFlags.persistenceFlagsToString(savedFlags));
        out.println("savedLoadedFields = " + StringUtils.booleanArrayToString(savedLoadedFields));

        out.print("savedImage = ");
        dumpPC(savedImage, out);
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
            /*
             * This doesn't work due to security problems but you get the idea.
             * I'm trying to get field values directly without going through
             * the provideField machinery.
             */
            Object value = obj.getClass().getDeclaredField(fieldName).get(obj);
            if (value instanceof PersistenceCapable)
            {
                return StringUtils.toJVMIDString(value);
            }
            else
            {
                return value;
            }
        }
        catch (Exception e)
        {
            return e.toString();
        }
    }

    /**
     * Accessor for the owning ObjectProviders for the managed object when stored embedded.
     * Should really only have a single owner but users could, in principle, assign it to multiple.
     * TODO Move this to ExecutionContext - need to update some store plugins
     * @return ObjectProviders owning this embedded object.
     */
    public ObjectProvider[] getEmbeddedOwners()
    {
        List<EmbeddedOwnerRelation> ownerRels = myEC.getOwnerInformationForEmbedded(this);
        if (ownerRels == null)
        {
            return null;
        }
        ObjectProvider[] owners = new ObjectProvider[ownerRels.size()];
        int i = 0;
        for (EmbeddedOwnerRelation rel : ownerRels)
        {
            owners[i++] = rel.getOwnerOP();
        }
        return owners;
    }

    public void changeActivityState(ActivityState state)
    {
        // Does nothing in this implementation; refer to ReferentialJDOStateManager
    }

    public void updateFieldAfterInsert(Object pc, int fieldNumber)
    {
        // Does nothing in this implementation; refer to ReferentialJDOStateManager
    }
}