/**********************************************************************
Copyright (c) 2009 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.ClassNotPersistableException;
import org.datanucleus.exceptions.NoPersistenceInformationException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.flush.Operation;
import org.datanucleus.flush.OperationQueue;
import org.datanucleus.management.ManagerStatistics;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.state.FetchPlanState;
import org.datanucleus.state.LockManager;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.state.RelationshipManager;
import org.datanucleus.store.Extent;
import org.datanucleus.store.FieldValues;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.scostore.Store;
import org.datanucleus.store.types.TypeManager;

/**
 * Context of execution for persistence operations.
 * This equates to the work of a PersistenceManager/EntityManager.
 * An ExecutionContext is responsible for
 * <ul>
 * <li>persist, merge, find and delete of persistable objects from the defined StoreManager</li>
 * <li>have a set of properties defining behaviour over and above the default configuration of the
 * parent NucleusContext</li>
 * <li>provide an interface to querying of the persistable objects in the StoreManager</li>
 * <li>provide a way of managing persistable objects using ObjectProviders</li>
 * <li>have a cache of currently managed objects (the "Level 1" cache), and make use of the
 * cache of the parent NucleusContext when not available in its cache</li>
 * <li>have a single "current" transaction. This transaction can be local, or JTA</li>
 * </ul>
 * <p>
 * An ExecutionContext can be started with a series of options that affect its behaviour thereafter. These
 * are defined by the "OPTION_{YYY}" static Strings.
 * </p>
 */
public interface ExecutionContext
{
    /** Startup option overriding the default (PMF/EMF) username for the connectionURL. */
    public static final String OPTION_USERNAME = "user";
    /** Startup option overriding the default (PMF/EMF) password for the connectionURL. */
    public static final String OPTION_PASSWORD = "password";
    /** Startup option setting whether, when using JTA, to do auto-join of transactions. */
    public static final String OPTION_JTA_AUTOJOIN = "jta_autojoin";

    void initialise(Object owner, Map<String, Object> options);

    /**
     * Method to return the owner object.
     * For JDO this will return the PersistenceManager that owns this object.
     * For JPA this will return a dummy PersistenceManager related to the EntityManager owning the object.
     * @return The owner manager object
     */
    Object getOwner();

    /**
     * Accessor for the current transaction for this execution context.
     * @return The current transaction
     */
    Transaction getTransaction();

    /**
     * Accessor for the Store Manager.
     * @return Store Manager
     */
    StoreManager getStoreManager();

    /**
     * Accessor for the MetaData Manager.
     * @return The MetaData Manager
     */
    MetaDataManager getMetaDataManager();

    /**
     * Accessor for the context in which this execution context is running.
     * @return Returns the context.
     */
    PersistenceNucleusContext getNucleusContext();

    /**
     * Accessor for the API adapter.
     * @return API adapter.
     */
    ApiAdapter getApiAdapter();

    /**
     * Acessor for the current FetchPlan
     * @return FetchPlan
     */
    FetchPlan getFetchPlan();

    /**
     * Accessor for the ClassLoader resolver to use in class loading issues.
     * @return The ClassLoader resolver
     */
    ClassLoaderResolver getClassLoaderResolver();

    /**
     * Accessor for the lock manager for this execution context.
     * @return The lock manager
     */
    LockManager getLockManager();

    /**
     * Accessor for any statistics-gathering object.
     * @return The statistics for this manager
     */
    ManagerStatistics getStatistics();

    /**
     * Method to set properties on the execution context.
     * @param props The properties
     */
    void setProperties(Map props);

    /**
     * Method to set a property on the execution context
     * @param name Name of the property
     * @param value Value to set
     */
    void setProperty(String name, Object value);

    /**
     * Accessor for a property.
     * @param name Name of the property
     * @return The value
     */
    Object getProperty(String name);

    /**
     * Accessor for a boolean property value.
     * @param name Name of the property
     * @return the value
     */
    Boolean getBooleanProperty(String name);

    /**
     * Accessor for an int property value.
     * @param name Name of the property
     * @return the value
     */
    Integer getIntProperty(String name);

    /**
     * Accessor for a String property value.
     * @param name Name of the property
     * @return The value
     */
    String getStringProperty(String name);

    /**
     * Accessor for the defined properties.
     * @return Properties for this execution context
     */
    Map<String, Object> getProperties();

    /**
     * Accessor for the supported property names.
     * @return Set of names
     */
    Set<String> getSupportedProperties();

    /**
     * TODO should we keep this here? this is api/language dependent
     * @return The type manager
     */
    TypeManager getTypeManager();

    /**
     * Method to close the execution context.
     */
    void close();

    /**
     * Accessor for whether this execution context is closed.
     * @return Whether this manager is closed.
     */
    boolean isClosed();

    /**
     * Accessor for whether to ignore the cache.
     * @return Whether to ignore the cache.
     */
    boolean getIgnoreCache();

    /**
     * Accessor for the datastore read timeout in milliseconds.
     * @return Datastore read timeout in milliseconds (if specified)
     */
    Integer getDatastoreReadTimeoutMillis();

    /**
     * Accessor for the datastore write timeout in milliseconds.
     * @return Datastore write timeout in milliseconds (if specified)
     */
    Integer getDatastoreWriteTimeoutMillis();

    /**
     * Method to find the ObjectProvider for the passed persistable object when it is managed by this manager.
     * @param pc The persistable object
     * @return The ObjectProvider
     */
    ObjectProvider findObjectProvider(Object pc);

    /**
     * Method to find the ObjectProvider for the passed persistable object when it is managed by this manager, 
     * and if not yet persistent to persist it.
     * @param pc The persistable object
     * @param persist Whether to persist if not yet persistent
     * @return The ObjectProvider
     */
    ObjectProvider findObjectProvider(Object pc, boolean persist);

    /**
     * Method to find the ObjectProvider for the passed embedded persistable object.
     * Will create one if not already registered, and tie it to the specified owner.
     * @param value The embedded object
     * @param owner The owner ObjectProvider (if known).
     * @param mmd Metadata for the field of the owner
     * @return The ObjectProvider for the embedded object
     */
    ObjectProvider findObjectProviderForEmbedded(Object value, ObjectProvider owner, AbstractMemberMetaData mmd);

    ObjectProvider findObjectProviderOfOwnerForAttachingObject(Object pc);

    /**
     * Method to register the ObjectProvider as being for the passed object.
     * Used during the process of identifying ObjectProvider for persistable object.
     * @param op The ObjectProvider
     * @param pc The object managed by the ObjectProvider
     */
    void hereIsObjectProvider(ObjectProvider op, Object pc);

    /**
     * Constructs an ObjectProvider to manage a hollow instance having the given object ID.
     * This constructor is used for creating new instances of existing persistent objects.
     * @param pcClass the class of the new instance to be created.
     * @param id the JDO identity of the object.
     */
    ObjectProvider newObjectProviderForHollow(Class pcClass, Object id);

    /**
     * Constructs an ObjectProvider to manage a hollow instance having the given object ID.
     * The instance is already supplied.
     * @param id the JDO identity of the object.
     * @param pc The object that is hollow that we are going to manage
     */
    ObjectProvider newObjectProviderForHollowPreConstructed(Object id, Object pc);

    /**
     * Constructs an ObjectProvider to manage a recently populated hollow instance having the
     * given object ID and the given field values. This constructor is used for
     * creating new instances of persistent objects obtained e.g. via a Query or backed by a view.
     * @param pcClass the class of the new instance to be created.
     * @param id the JDO identity of the object.
     * @param fv the initial field values of the object.
     */
    ObjectProvider newObjectProviderForHollowPopulated(Class pcClass, Object id, FieldValues fv);

    /**
     * Constructs an ObjectProvider to manage the specified persistent instance having the given object ID.
     * @param id the JDO identity of the object.
     * @param pc The object that is persistent that we are going to manage
     */
    ObjectProvider newObjectProviderForPersistentClean(Object id, Object pc);

    /**
     * Constructs an ObjectProvider to manage a hollow (or pclean) instance having the given FieldValues.
     * This constructor is used for creating new instances of existing persistent objects using application identity.
     * @param pcClass the class of the new instance to be created.
     * @param fv the initial field values of the object.
     * @deprecated Use newForHollowPopulated instead
     */
    ObjectProvider newObjectProviderForHollowPopulatedAppId(Class pcClass, final FieldValues fv);

    /**
     * Constructs an ObjectProvider to manage a persistable instance that will
     * be EMBEDDED/SERIALISED into another persistable object. The instance will not be
     * assigned an identity in the process since it is a SCO.
     * @param pc The persistable to manage (see copyPc also)
     * @param copyPc Whether the SM should manage a copy of the passed PC or that one
     * @param ownerOP Owner ObjectProvider
     * @param ownerFieldNumber Field number in owner object where this is stored
     */
    ObjectProvider newObjectProviderForEmbedded(Object pc, boolean copyPc, 
            ObjectProvider ownerOP, int ownerFieldNumber);

    /**
     * Constructs an ObjectProvider for an object of the specified type, creating the PC object to hold the values
     * where this object will be EMBEDDED/SERIALISED into another persistable object. The instance will not be
     * assigned an identity in the process since it is a SCO.
     * @param cmd Meta-data for the class that this is an instance of.
     * @param ownerOP Owner ObjectProvider
     * @param ownerFieldNumber Field number in owner object where this is stored
     */
    ObjectProvider newObjectProviderForEmbedded(AbstractClassMetaData cmd, 
            ObjectProvider ownerOP, int ownerFieldNumber);

    /**
     * Constructs an ObjectProvider to manage a transient instance that is 
     * becoming newly persistent.  A new object ID for the
     * instance is obtained from the store manager and the object is inserted
     * in the data store.
     * This constructor is used for assigning state managers to existing
     * instances that are transitioning to a persistent state.
     * @param pc the instance being make persistent.
     * @param fv Any changes to make before inserting
     */
    ObjectProvider newObjectProviderForPersistentNew(Object pc, FieldValues fv);

    /**
     * Constructs an ObjectProvider to manage a Transactional Transient instance.
     * A new object ID for the instance is obtained from the store manager and
     * the object is inserted in the data store.
     * This constructor is used for assigning state managers to Transient
     * instances that are transitioning to a transient clean state.
     * @param pc the instance being make persistent.
     */
    ObjectProvider newObjectProviderForTransactionalTransient(Object pc);

    /**
     * Constructor for creating ObjectProvider instances to manage persistable objects in detached state.
     * @param pc the detached object
     * @param id the JDO identity of the object.
     * @param version the detached version
     */
    ObjectProvider newObjectProviderForDetached(Object pc, Object id, Object version);

    /**
     * Constructor for creating ObjectProvider instances to manage persistable objects that are not persistent yet
     * are about to be deleted. Consequently the initial lifecycle state will be P_NEW, but will soon
     * move to P_NEW_DELETED.
     * @param pc the object being deleted from persistence
     */
    ObjectProvider newObjectProviderForPNewToBeDeleted(Object pc);

    /**
     * Method to add the object managed by the specified ObjectProvider to the cache.
     * @param op The ObjectProvider
     */
    void addObjectProvider(ObjectProvider op);

    /**
     * Method to remove the object managed by the specified ObjectProvider from the cache.
     * @param op The ObjectProvider
     */
    void removeObjectProvider(ObjectProvider op);

    /**
     * Method to evict the passed object.
     * @param pc The object
     */
    void evictObject(Object pc);

    /**
     * Method to evict all objects of the specified type (and optionaly its subclasses).
     * @param cls Type of persistable object
     * @param subclasses Whether to include subclasses
     */
    void evictObjects(Class cls, boolean subclasses);

    /**
     * Method to evict all L1 cache objects
     */
    void evictAllObjects();

    /**
     * Method to retrieve the passed object.
     * @param pc The object
     * @param fgOnly Just retrieve the current fetch group
     */
    void retrieveObject(Object pc, boolean fgOnly);

    /**
     * Method to persist the passed object.
     * @param pc The object
     * @param merging Whether this object (and dependents) is being merged
     * @return The persisted object
     */
    Object persistObject(Object pc, boolean merging);

    /**
     * Method to persist the passed object(s).
     * @param pcs The objects to persist
     * @return The persisted objects
     */
    Object[] persistObjects(Object[] pcs);

    /**
     * Method to persist the passed object (internally).
     * @param pc The object
     * @param preInsertChanges Changes to be made before inserting
     * @param ownerOP ObjectProvider of the owner when embedded
     * @param ownerFieldNum Field number in the owner where this is embedded (or -1 if not embedded)
     * @param objectType Type of object (see org.datanucleus.store.ObjectProvider, e.g ObjectProvider.PC)
     * @return The persisted object
     */
    Object persistObjectInternal(Object pc, FieldValues preInsertChanges, ObjectProvider ownerOP, int ownerFieldNum, 
            int objectType);

    /**
     * Method to persist the passed object (internally).
     * @param pc The object
     * @param ownerOP ObjectProvider of the owner when embedded
     * @param ownerFieldNum Field number in the owner where this is embedded (or -1 if not embedded)
     * @param objectType Type of object (see org.datanucleus.state.ObjectProvider, e.g ObjectProvider.PC)
     * @return The persisted object
     */
    Object persistObjectInternal(Object pc, ObjectProvider ownerOP, int ownerFieldNum, int objectType);

    /**
     * Method to persist the passed object (internally).
     * @param pc The object
     * @param preInsertChanges Changes to be made before inserting
     * @param objectType Type of object (see org.datanucleus.state.ObjectProvider, e.g ObjectProvider.PC)
     * @return The persisted object
     */
    Object persistObjectInternal(Object pc, FieldValues preInsertChanges, int objectType);

    /**
     * Method to make transient the passed object.
     * @param pc The object
     * @param state Object containing the state of the fetchplan processing
     */
    void makeObjectTransient(Object pc, FetchPlanState state);

    /**
     * Method to make the passed object transactional.
     * @param pc The object
     */
    void makeObjectTransactional(Object pc);

    /**
     * Method to make the passed object nontransactional.
     * @param pc The object
     */
    void makeObjectNontransactional(Object pc);

    /**
     * Method to return if the specified object exists in the datastore.
     * @param obj The (persistable) object
     * @return Whether it exists
     */
    boolean exists(Object obj);

    /**
     * Accessor for the currently managed objects for the current transaction.
     * If the transaction is not active this returns null.
     * @return Collection of managed objects enlisted in the current transaction
     */
    Set getManagedObjects();

    /**
     * Accessor for the currently managed objects for the current transaction.
     * If the transaction is not active this returns null.
     * @param classes Classes that we want the objects for
     * @return Collection of managed objects enlisted in the current transaction
     */
    Set getManagedObjects(Class[] classes);

    /**
     * Accessor for the currently managed objects for the current transaction.
     * If the transaction is not active this returns null.
     * @param states States that we want the objects for
     * @return Collection of managed objects enlisted in the current transaction
     */
    Set getManagedObjects(String[] states);

    /**
     * Accessor for the currently managed objects for the current transaction.
     * If the transaction is not active this returns null.
     * @param states States that we want the objects for
     * @param classes Classes that we want the objects for
     * @return Collection of managed objects enlisted in the current transaction
     */
    Set getManagedObjects(String[] states, Class[] classes);

    /**
     * Method to delete an object from the datastore.
     * @param obj The object
     */
    void deleteObject(Object obj);

    /**
     * Method to delete an array of objects from the datastore.
     * @param objs The objects to delete
     */
    void deleteObjects(Object[] objs);

    /**
     * Method to delete the passed object (internally).
     * @param pc The object
     */
    void deleteObjectInternal(Object pc);

    /**
     * Method to detach the passed object.
     * @param pc The object to detach
     * @param state State for the detachment process.
     */
    void detachObject(Object pc, FetchPlanState state);

    /**
     * Method to detach a copy of the passed object using the provided state.
     * @param pc The object
     * @param state State for the detachment process
     * @return The detached copy of the object
     */
    Object detachObjectCopy(Object pc, FetchPlanState state);

    /**
     * Method to detach all managed objects.
     */
    void detachAll();

    /**
     * Method to attach the passed object (and related objects).
     * Throws an exception if another (persistent) object with the same id exists in the L1 cache already.
     * @param op ObjectProvider of the owning object that has this in a field causing its attach
     * @param pc The (detached) object
     * @param sco Whether the object has no identity (embedded or serialised)
     */
    void attachObject(ObjectProvider op, Object pc, boolean sco);

    /**
     * Method to attach a copy of the passed object (and related objects).
     * @param op ObjectProvider of the owning object that has this in a field causing its attach
     * @param pc The object
     * @param sco Whether it has no identity (second-class object)
     * @return The attached copy of the input object
     */
    Object attachObjectCopy(ObjectProvider op, Object pc, boolean sco);

    /**
     * Convenience method to return the attached object for the specified id if one exists.
     * @param id The id
     * @return The attached object
     */
    Object getAttachedObjectForId(Object id);

    /**
     * Method to refresh the passed object.
     * @param pc The object
     */
    void refreshObject(Object pc);

    /**
     * Method to refresh all L1 cache objects
     */
    void refreshAllObjects();

    /**
     * Method to enlist the specified ObjectProvider in the current transaction.
     * @param op The ObjectProvider
     */
    void enlistInTransaction(ObjectProvider op);

    /**
     * Method to return if an object is enlisted in the current transaction.
     * @param id Identity for the object
     * @return Whether it is enlisted in the current transaction
     */
    boolean isEnlistedInTransaction(Object id);

    /**
     * Method to evict the specified ObjectProvider from the current transaction.
     * @param op The ObjectProvider
     */
    void evictFromTransaction(ObjectProvider op);

    /**
     * Mark the specified ObjectProvider as dirty
     * @param op ObjectProvider
     * @param directUpdate Whether the object has had a direct update made on it (if known)
     */
    void markDirty(ObjectProvider op, boolean directUpdate);

    /**
     * Mark the specified ObjectProvider as clean.
     * @param op The ObjectProvider
     */
    void clearDirty(ObjectProvider op);

    /**
     * Method to mark as clean all ObjectProviders of dirty objects.
     */
    void clearDirty();

    /**
     * Whether the datastore operations are delayed until commit.
     * In optimistic transactions this is automatically enabled.
     * @return true if datastore operations are delayed until commit
     */
    boolean isDelayDatastoreOperationsEnabled();

    /**
     * Method to process any outstanding non-transactional updates that are queued.
     * If "datanucleus.nontx.atomic" is false, or currently in a transaction then returns immediately.
     * Otherwise will flush any updates that are outstanding (updates to an object), will perform detachAllOnCommit
     * if enabled (so user always has detached objects), update objects in any L2 cache, and migrates any 
     * objects through lifecycle changes.
     * Is similar in content to "flush"+"preCommit"+"postCommit"
     */
    void processNontransactionalUpdate();

    /**
     * Accessor for an object given the object id.
     * @param id Id of the object.
     * @param validate Whether to validate the object state
     * @param checkInheritance Whether look to the database to determine which
     * class this object is. This parameter is a hint. Set false, if it's
     * already determined the correct pcClass for this pc "object" in a certain
     * level in the hierarchy. Set to true and it will look to the database.
     * @param objectClassName Class name for the object with this id (if known, optional)
     * @return The Object
     */
    Object findObject(Object id, boolean validate, boolean checkInheritance, String objectClassName);

    /**
     * Accessor for an object given the object id. Typically used after a query to apply the retrieved values
     * to an object.
     * @param id Id of the object.
     * @param fv FieldValues to apply to the object (optional)
     * @param pcClass the type which the object is. This type will be used to instantiate the object
     * @param ignoreCache true if the cache is ignored
     * @param checkInheritance Whether to check the inheritance of this object
     * @return the Object
     */
    Object findObject(Object id, FieldValues fv, Class pcClass, boolean ignoreCache, boolean checkInheritance);

    /**
     * Accessor for objects with the specified identities.
     * @param ids Ids of the object(s).
     * @param validate Whether to validate the object state
     * @return The Objects with these ids (same order)
     * @throws NucleusObjectNotFoundException if an object doesn't exist in the datastore
     */
    Object[] findObjects(Object[] ids, boolean validate);

    /**
     * Accessor for the Extent for a class (and optionally its subclasses).
     * @param candidateClass The class
     * @param includeSubclasses Whether to include subclasses
     * @return The Extent
     */
    Extent getExtent(Class candidateClass, boolean includeSubclasses);

    /**
     * Accessor for a new Query.
     * @return The new Query
     */
    Query newQuery();

    /**
     * Method to put a Persistable object associated to the ObjectProvider into the L1 cache.
     * @param op The ObjectProvider
     */
    void putObjectIntoLevel1Cache(ObjectProvider op);

    /**
     * Convenience method to access an object in the cache.
     * Firstly looks in the L1 cache for this PM, and if not found looks in the L2 cache.
     * @param id Id of the object
     * @return Persistable object (with connected ObjectProvider).
     */
    Object getObjectFromCache(Object id);

    /**
     * Convenience method to access objects in the cache.
     * Firstly looks in the L1 cache, and if not found looks in the L2 cache.
     * @param ids Ids of the objects
     * @return Persistable objects (with connected ObjectProvider).
     */
    Object[] getObjectsFromCache(Object[] ids);

    /**
     * Method to remove an object from the L1 cache.
     * @param id The id of the object
     */
    void removeObjectFromLevel1Cache(Object id);

    /**
     * Method to remove an object from the L2 cache.
     * @param id The id of the object
     */
    void removeObjectFromLevel2Cache(Object id);

    /**
     * Method to mark the object with specifed id to have the supplied fields updated in the L2 cache at commit.
     * @param id Id of the object
     * @param fields The fields to update
     */
    void markFieldsForUpdateInLevel2Cache(Object id, boolean[] fields);

    /**
     * Whether an object with the specified identity exists in the cache(s).
     * Used as a check on identity (inheritance-level) validity
     * @param id The identity
     * @return Whether it exists
     */
    boolean hasIdentityInCache(Object id);

    /**
     * This method returns an object id instance corresponding to the pcClass and key arguments.
     * Operates in 2 modes :-
     * <ul>
     * <li>The class uses SingleFieldIdentity and the key is the value of the key field</li>
     * <li>In all other cases the key is the String form of the object id instance</li>
     * </ul>
     * @param pcClass Class of the persistable object to create the identity for
     * @param key Value of the key for SingleFieldIdentity (or the toString value)
     * @return The new object-id instance
     */
    Object newObjectId(Class pcClass, Object key);

    /**
     * This method returns an object id instance corresponding to the class name, and the passed
     * object (when using app identity).
     * @param className Name of the class of the object.
     * @param pc The persistable object. Used for application-identity
     * @return A new object ID.
     */
    Object newObjectId(String className, Object pc);

    /**
     * Convenience method to return the setting for serialize read for the current transaction for
     * the specified class name. Returns the setting for the transaction (if set), otherwise falls back to
     * the setting for the class, otherwise returns false.
     * @param className Name of the class
     * @return Setting for serialize read
     */
    boolean getSerializeReadForClass(String className);

    /**
     * Convenience method to assert if the passed class is not persistable.
     * @param cls The class of which we want to persist objects
     * @throws ClassNotPersistableException When the class is not persistable
     * @throws NoPersistenceInformationException When the class has no available persistence information
     */
    void assertClassPersistable(Class cls);

    /**
     * Utility method to check if the specified class has reachable metadata or annotations.
     * @param cls The class to check
     * @return Whether the class has reachable metadata or annotations
     */
    boolean hasPersistenceInformationForClass(Class cls);

    /**
     * Tests whether this persistable object is being inserted.
     * @param pc the object to verify the status
     * @return true if this instance is inserting.
     */
    boolean isInserting(Object pc);

    /**
     * Accessor for whether the ExecutionContext is flushing changes to the datastore.
     * @return Whether it is currently flushing
     */
    boolean isFlushing();

    /**
     * Accessor for whether this ExecutionContext is currently running detachAllOnCommit.
     * @return Whether running detachAllOnCommit
     */
    boolean isRunningDetachAllOnCommit();

    /**
     * Method callable from external APIs for user-management of flushing.
     * Called by JDO PM.flush, or JPA EM.flush().
     * Performs management of relations, prior to performing internal flush of all dirty/new/deleted
     * instances to the datastore.
     */
    void flush();

    /**
     * Method to flushes all dirty, new, and deleted instances to the datastore.
     * It has no effect if a transaction is not active. 
     * If a datastore transaction is active, this method synchronizes the cache with
     * the datastore and reports any exceptions. 
     * If an optimistic transaction is active, this method obtains a datastore connection
     * and synchronizes the cache with the datastore using this connection.
     * The connection obtained by this method is held until the end of the transaction.
     * @param flushToDatastore Whether to ensure any changes reach the datastore
     *     Otherwise they will be flushed to the datastore manager and leave it to
     *     decide the opportune moment to actually flush them to teh datastore
     * @throws NucleusOptimisticException when optimistic locking error(s) occur
     */
    void flushInternal(boolean flushToDatastore);

    /**
     * Accessor for the operation queue.
     * The queue can be null if 
     * @return The operation queue (typically for collections/maps)
     */
    OperationQueue getOperationQueue();

    /**
     * Method to add an operation to the queue.
     * @param oper The operation to add
     */
    void addOperationToQueue(Operation oper);

    /**
     * Method to flush all queued operations for the specified backing store (if any).
     * @param backingStore The backing store
     * @param op ObjectProvider
     */
    void flushOperationsForBackingStore(Store backingStore, ObjectProvider op);

    /**
     * Accessor for whether this context is multithreaded.
     * @return Whether multithreaded (and hence needing locking)
     */
    boolean getMultithreaded();

    /**
     * Whether managed relations are supported by this execution context.
     * @return Supporting managed relations
     */
    boolean getManageRelations();

    /**
     * Accessor for the RelationshipManager for the provided ObjectProvider.
     * @param op ObjectProvider
     * @return The RelationshipManager
     */
    RelationshipManager getRelationshipManager(ObjectProvider op);

    /**
     * Returns whether this ExecutionContext is currently performing the manage relationships task.
     * @return Whether in the process of managing relations
     */
    boolean isManagingRelations();

    /**
     * Convenience method to inspect the list of objects with outstanding changes to flush.
     * @return ObjectProviders for the objects to be flushed.
     */
    List<ObjectProvider> getObjectsToBeFlushed();

    /**
     * Retrieve the callback handler for this PM
     * @return the callback handler
     */
    CallbackHandler getCallbackHandler();

    /**
     * Method to register a listener for instances of the specified classes.
     * @param listener The listener to sends events to
     * @param classes The classes that it is interested in
     */
    void addListener(Object listener, Class[] classes);

    /**
     * Method to remove a currently registered listener.
     * @param listener The instance lifecycle listener to remove.
     */
    void removeListener(Object listener);

    /**
     * Disconnect the registered LifecycleListener
     */
    public void disconnectLifecycleListener();

    public static interface LifecycleListener
    {
        /**
         * Invoked before closing the ExecutionContext
         * @param ec execution context
         */
        void preClose(ExecutionContext ec);
    }

    /**
     * Accessor for an internal fetch group for the specified class.
     * @param cls The class
     * @param name Name of the group
     * @return The FetchGroup
     */
    FetchGroup getInternalFetchGroup(Class cls, String name);

    /**
     * Method to add an internal fetch group to this ExecutionContext.
     * @param grp The internal fetch group
     */
    void addInternalFetchGroup(FetchGroup grp);

    /**
     * Accessor for the fetch groups for the specified name.
     * @param name Name of the group
     * @return The FetchGroup
     */
    Set getFetchGroupsWithName(String name);

    /**
     * Accessor for the lock object for this ExecutionContext.
     * @return The lock object
     */
    Lock getLock();

    /**
     * Method to generate an instance of an interface, abstract class, or concrete PC class.
     * @param cls The class of the interface or abstract class, or concrete class defined in MetaData
     * @return The instance of this type
     */
    Object newInstance(Class cls);

    /**
     * Accessor for whether the object with this identity is modified in the current transaction.
     * @param id The identity.
     * @return Whether it is modified/new/deleted in this transaction
     */
    boolean isObjectModifiedInTransaction(Object id);

    /**
     * Replace the previous object id for a PC object to a new
     * @param pc The Persistable object
     * @param oldID the old id
     * @param newID the new id
     */
    void replaceObjectId(Object pc, Object oldID, Object newID);

    /**
     * Access a referenced object for this ObjectProvider during the attach/detach process.
     * When attaching and this is the detached object this returns the newly attached object.
     * When attaching and this is the newly attached object this returns the detached object.
     * When detaching and this is the newly detached object this returns the attached object.
     * When detaching and this is the attached object this returns the newly detached object.
     * @param op Object provider
     * @return The referenced object (if any)
     */
    Object getAttachDetachReferencedObject(ObjectProvider op);

    /**
     * Register a referenced object against this ObjectProvider for the attach/detach process.
     * @param op The ObjectProvider
     * @param obj The referenced object (or null to clear out any reference)
     */
    void setAttachDetachReferencedObject(ObjectProvider op, Object obj);

    /**
     * Method to register an embedded relation for the specified memberf of the owner ObjectProvider
     * where the embedded ObjectProvider is stored.
     * @param ownerOP Owner ObjectProvider
     * @param ownerFieldNum Member number that is embedded
     * @param embOP ObjectProvider of the embedded object
     * @return The EmbeddedOwnerRelation
     */
    EmbeddedOwnerRelation registerEmbeddedRelation(ObjectProvider ownerOP, int ownerFieldNum, ObjectProvider embOP);

    /**
     * Method to deregister the specified embedded relation (e.g when the embedded object is disconnected).
     * @param rel The embedded relation
     */
    void deregisterEmbeddedRelation(EmbeddedOwnerRelation rel);

    /**
     * Accessor for the relations for the specified embedded ObjectProvider where it is embedded.
     * @param embOP The ObjectProvider that is embedded
     * @return The List of embedded relations involving this ObjectProvider as embedded
     */
    List<EmbeddedOwnerRelation> getOwnerInformationForEmbedded(ObjectProvider embOP);

    /**
     * Accessor for the relations for the specified embedded ObjectProvider where it is embedded.
     * @param ownerOP The ObjectProvider that owns the embedded
     * @return The List of embedded relations involving this ObjectProvider as owner
     */
    List<EmbeddedOwnerRelation> getEmbeddedInformationForOwner(ObjectProvider ownerOP);

    /**
     * Convenience method to remove the EmbeddedOwnerRelation between the specified ObjectProviders.
     * @param ownerOP Owner ObjectProvider
     * @param ownerFieldNum Field in owner
     * @param embOP Embedded ObjectProvider
     */
    void removeEmbeddedOwnerRelation(ObjectProvider ownerOP, int ownerFieldNum, ObjectProvider embOP);

    public static class EmbeddedOwnerRelation
    {
        protected ObjectProvider ownerOP;
        protected int ownerFieldNum;
        protected ObjectProvider embOP;

        public EmbeddedOwnerRelation(ObjectProvider ownerOP, int ownerFieldNum, ObjectProvider embOP)
        {
            this.ownerOP = ownerOP;
            this.ownerFieldNum = ownerFieldNum;
            this.embOP = embOP;
        }
        public ObjectProvider getOwnerOP() {return ownerOP;}
        public ObjectProvider getEmbeddedOP() {return embOP;}
        public int getOwnerFieldNum() {return ownerFieldNum;}
    }

    void setObjectProviderAssociatedValue(ObjectProvider op,Object key, Object value);
    Object getObjectProviderAssociatedValue(ObjectProvider op,Object key);
    void removeObjectProviderAssociatedValue(ObjectProvider op,Object key);
    boolean containsObjectProviderAssociatedValue(ObjectProvider op, Object key);
}