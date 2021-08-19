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
import org.datanucleus.cache.Level1Cache;
import org.datanucleus.enhancement.ExecutionContextReference;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.exceptions.ClassNotPersistableException;
import org.datanucleus.exceptions.NoPersistenceInformationException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.flush.FlushMode;
import org.datanucleus.flush.Operation;
import org.datanucleus.flush.OperationQueue;
import org.datanucleus.management.ManagerStatistics;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.state.LockManager;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.state.RelationshipManager;
import org.datanucleus.store.FieldValues;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.query.Extent;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.scostore.Store;
import org.datanucleus.transaction.Transaction;

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
public interface ExecutionContext extends ExecutionContextReference
{
    /** Startup option overriding the default (PMF/EMF) username for the connectionURL. */
    public static final String OPTION_USERNAME = "user";
    /** Startup option overriding the default (PMF/EMF) password for the connectionURL. */
    public static final String OPTION_PASSWORD = "password";
    /** Startup option setting whether, when using JTA, to do auto-join of transactions. */
    public static final String OPTION_JTA_AUTOJOIN = "jta_autojoin";

    void initialise(Object owner, Map<String, Object> options);

    Level1Cache getLevel1Cache();

    /**
     * Accessor for the current transaction for this execution context.
     * @return The current transaction
     */
    Transaction getTransaction();

    /**
     * Accessor for the Store Manager.
     * @return Store Manager
     */
    default StoreManager getStoreManager()
    {
        return getNucleusContext().getStoreManager();
    }

    /**
     * Accessor for the MetaData Manager.
     * @return The MetaData Manager
     */
    default MetaDataManager getMetaDataManager()
    {
        return getNucleusContext().getMetaDataManager();
    }

    /**
     * Accessor for the context in which this execution context is running.
     * @return Returns the context.
     */
    PersistenceNucleusContext getNucleusContext();

    /**
     * Accessor for the API adapter.
     * @return API adapter.
     */
    default ApiAdapter getApiAdapter()
    {
        return getNucleusContext().getApiAdapter();
    }

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
    default TypeManager getTypeManager()
    {
        return getNucleusContext().getTypeManager();
    }

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
     * Method to find the ObjectProvider for the passed persistable object when it is managed by this manager.
     * @param pc The persistable object
     * @return The ObjectProvider
     */
    ObjectProvider findObjectProvider(Object pc);

    /**
     * Method to find the ObjectProvider for the passed persistable object when it is managed by this manager, 
     * and if not yet persistent to persist it and return the assigned ObjectProvider.
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
     * Method to add the object managed by the specified ObjectProvider to the cache.
     * @param op The ObjectProvider
     */
    void addObjectProviderToCache(ObjectProvider op);

    /**
     * Method to remove the object managed by the specified ObjectProvider from the cache.
     * @param op The ObjectProvider
     */
    void removeObjectProviderFromCache(ObjectProvider op);

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
     * Method to retrieve the (fields of the) passed object(s).
     * @param useFetchPlan Whether to retrieve the current fetch plan
     * @param pcs The objects
     */
    void retrieveObjects(boolean useFetchPlan, Object... pcs);

    /**
     * Method to persist the passed object.
     * @param pc The object
     * @param merging Whether this object (and dependents) is being merged
     * @param <T> Type of the persistable object
     * @return The persisted object
     */
    <T> T persistObject(T pc, boolean merging);

    /**
     * Method to persist the passed object(s).
     * @param pcs The objects to persist
     * @return The persisted objects
     */
    Object[] persistObjects(Object... pcs);

    /**
     * Method to persist the passed object (internally).
     * @param pc The object
     * @param preInsertChanges Changes to be made before inserting
     * @param ownerOP ObjectProvider of the owner when embedded
     * @param ownerFieldNum Field number in the owner where this is embedded (or -1 if not embedded)
     * @param objectType Type of object (see org.datanucleus.store.ObjectProvider, e.g ObjectProvider.PC)
     * @param <T> Type of the persistable object
     * @return The persisted object
     */
    <T> T persistObjectInternal(T pc, FieldValues preInsertChanges, ObjectProvider ownerOP, int ownerFieldNum, int objectType);

    /**
     * Method to persist the passed object (internally).
     * @param pc The object
     * @param ownerOP ObjectProvider of the owner when embedded
     * @param ownerFieldNum Field number in the owner where this is embedded (or -1 if not embedded)
     * @param objectType Type of object (see org.datanucleus.state.ObjectProvider, e.g ObjectProvider.PC)
     * @param <T> Type of the persistable object
     * @return The persisted object
     */
    <T> T persistObjectInternal(T pc, ObjectProvider ownerOP, int ownerFieldNum, int objectType);

    /**
     * Method to persist the passed object (internally).
     * @param pc The object
     * @param preInsertChanges Changes to be made before inserting
     * @param objectType Type of object (see org.datanucleus.state.ObjectProvider, e.g ObjectProvider.PC)
     * @param <T> Type of the persistable object
     * @return The persisted object
     */
    default <T> T persistObjectInternal(T pc, FieldValues preInsertChanges, int objectType)
    {
        return persistObjectInternal(pc, preInsertChanges, null, -1, objectType);
    }

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
    void deleteObjects(Object... objs);

    /**
     * Method to delete the passed object (internally).
     * @param pc The object
     */
    void deleteObjectInternal(Object pc);

    /**
     * Method to detach the passed object.
     * @param state State for the detachment process.
     * @param pc The object to detach
     */
    void detachObject(FetchPlanState state, Object pc);

    /**
     * Method to detach the passed object(s).
     * @param state State for the detachment process.
     * @param pcs The object(s) to detach
     */
    void detachObjects(FetchPlanState state, Object... pcs);

    /**
     * Method to detach a copy of the passed object using the provided state.
     * @param state State for the detachment process
     * @param pc The object
     * @param <T> Type of the persistable object
     * @return The detached copy of the object
     */
    <T> T detachObjectCopy(FetchPlanState state, T pc);

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
     * @param <T> Type of the persistable object
     * @return The attached copy of the input object
     */
    <T> T attachObjectCopy(ObjectProvider op, T pc, boolean sco);

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
     * Method to process any outstanding non-transactional updates that are queued.
     * If "datanucleus.nontx.atomic" is false, or currently in a transaction then returns immediately.
     * Otherwise will flush any updates that are outstanding (updates to an object), will perform detachAllOnCommit
     * if enabled (so user always has detached objects), update objects in any L2 cache, and migrates any 
     * objects through lifecycle changes.
     * Is similar in content to "flush"+"preCommit"+"postCommit"
     */
    void processNontransactionalUpdate();

    /**
     * Accessor for an object of the specified type with the provided id "key".
     * With datastore id or single-field id the "key" is the key of the id, and with composite ids the "key" is the toString() of the id.
     * @param cls Class of the persistable
     * @param key Value of the key field for SingleFieldIdentity, or the string value of the key otherwise
     * @return The object meeting this requirement
     * @param <T> Type of the persistable
     */
    <T> T findObject(Class<T> cls, Object key);

    /**
     * Accessor for objects of the specified type, with the provided id "key"s.
     * With datastore id or single-field id the "key" is the key of the id, and with composite ids the "key" is the toString() of the id.
     * @param cls Class of the persistable
     * @param keys Values of the key field for SingleFieldIdentity, or the string value of the keys otherwise
     * @return The objects meeting this requirement
     * @param <T> Type of the persistable
     */
    <T> List<T> findObjects(Class<T> cls, List keys);

    /**
     * Accessor for an object of the specified type with the provided values for a unique key.
     * Alternative would be to have an intermediate class and do this
     * <pre>
     * ec.findObjectByUnique(cls).for("field1", val1).for("field2", val2).find();
     * </pre>
     * @param cls Class of the persistable
     * @param fieldNames Name(s) of the field(s) forming the unique key
     * @param fieldValues Value(s) of the field(s) forming the unique key
     * @return The object meeting this requirement
     * @param <T> Type of the persistable
     */
    <T> T findObjectByUnique(Class<T> cls, String[] fieldNames, Object[] fieldValues);

    /**
     * Shortcut to calling "findObject(id, validate, validate, null)".
     * Note: This is used by the bytecode enhancement contract in <pre>dnCopyKeyFieldsFromObjectId</pre>
     * @param id The id of the object
     * @param validate Whether to validate the id
     * @return The object
     */
    Persistable findObject(Object id, boolean validate);

    /**
     * Accessor for persistable objects with the specified identities.
     * @param ids Ids of the object(s).
     * @param validate Whether to validate the object state
     * @return The persistable objects with these ids (same order)
     * @throws NucleusObjectNotFoundException if an object doesn't exist in the datastore
     */
    Persistable[] findObjectsById(Object[] ids, boolean validate);

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
    Persistable findObject(Object id, boolean validate, boolean checkInheritance, String objectClassName);

    /**
     * Accessor for an object given the object id. Typically used after a query to apply the retrieved values to an object.
     * @param id Id of the object.
     * @param fv FieldValues to apply to the object (optional)
     * @param pcClass the type which the object is. This type will be used to instantiate the object
     * @param ignoreCache true if the cache is ignored
     * @param checkInheritance Whether to check the inheritance of this object
     * @return the Object
     */
    Persistable findObject(Object id, FieldValues fv, Class pcClass, boolean ignoreCache, boolean checkInheritance);

    /**
     * Accessor for the Extent for a class (and optionally its subclasses).
     * @param candidateClass The class
     * @param includeSubclasses Whether to include subclasses
     * @param <T> Type of the persistable object
     * @return The Extent
     */
    <T> Extent<T> getExtent(Class<T> candidateClass, boolean includeSubclasses);

    /**
     * Method to put a Persistable object associated to the ObjectProvider into the L1 cache.
     * @param op The ObjectProvider
     */
    void putObjectIntoLevel1Cache(ObjectProvider op);

    /**
     * Convenience method to access an object in the cache.
     * Firstly looks in the L1 cache for this ExecutionContext, and if not found looks in the L2 cache.
     * @param id Id of the object
     * @return Persistable object (with connected ObjectProvider).
     */
    Persistable getObjectFromCache(Object id);

    /**
     * Convenience method to access objects in the cache.
     * Firstly looks in the L1 cache, and if not found looks in the L2 cache.
     * @param ids Ids of the objects
     * @return Persistable objects (with connected ObjectProvider).
     */
    Persistable[] getObjectsFromCache(Object[] ids);

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
     * Accessor for the flush mode. Whether to auto-commit, or whether to delay flushing.
     * @return The flush mode.
     */
    FlushMode getFlushMode();

    /**
     * Whether the datastore operations are delayed until commit.
     * In optimistic transactions this is automatically enabled.
     * @return true if datastore operations are delayed until commit
     */
    boolean isDelayDatastoreOperationsEnabled();

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
     * The queue can be null if there are no operations queued (txn not active, not optimistic, no ops arrived yet).
     * @return The operation queue (typically for collections/maps)
     */
    OperationQueue getOperationQueue();

    /**
     * Accessor for whether the operation queue is currently active.
     * Will return false if not delaying flush, or not in a transaction, or flushing.
     * @return Whether the operation queue is active for adding operations
     */
    boolean operationQueueIsActive();

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
     * Convenience method to inspect the list of objects with outstanding changes to flush.
     * @return ObjectProviders for the objects to be flushed.
     */
    List<ObjectProvider> getObjectsToBeFlushed();

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
     * Retrieve the callback handler for this ExecutionContext.
     * @return the callback handler
     */
    CallbackHandler getCallbackHandler();

    /**
     * Interface to be implemented by a listener for the closure of the ExecutionContext.
     */
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
    Set<FetchGroup> getFetchGroupsWithName(String name);

    /**
     * Accessor for the lock object for this ExecutionContext.
     * @return The lock object
     */
    default Lock getLock()
    {
        // No locking with default ExecutionContext (single-threaded).
        return null;
    }

    /**
     * Method to generate an instance of an interface, abstract class, or concrete PC class.
     * @param cls The class of the interface or abstract class, or concrete class defined in MetaData
     * @param <T> Type of the persistable object
     * @return The instance of this type
     */
    <T> T newInstance(Class<T> cls);

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
    void replaceObjectId(Persistable pc, Object oldID, Object newID);

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
     * @param ownerOP The ObjectProvider that owns the embedded
     * @return The List of embedded relations involving this ObjectProvider as owner
     */
    List<EmbeddedOwnerRelation> getEmbeddedInformationForOwner(ObjectProvider ownerOP);

    /**
     * Accessor for the relations for the specified embedded ObjectProvider where it is embedded.
     * @param embOP The ObjectProvider that is embedded
     * @return The List of embedded relations involving this ObjectProvider as embedded
     */
    List<EmbeddedOwnerRelation> getOwnerInformationForEmbedded(ObjectProvider embOP);

    /**
     * Accessor for the owner objects for the provided embedded object provider.
     * @param embOP The ObjectProvider that is embedded
     * @return The owner object(s) that have this object embedded.
     */
    ObjectProvider[] getOwnersForEmbeddedObjectProvider(ObjectProvider embOP);

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

    void setObjectProviderAssociatedValue(ObjectProvider op, Object key, Object value);
    Object getObjectProviderAssociatedValue(ObjectProvider op, Object key);
    void removeObjectProviderAssociatedValue(ObjectProvider op, Object key);
    boolean containsObjectProviderAssociatedValue(ObjectProvider op, Object key);

    /**
     * Register a listener to be called when this ExecutionContext is closing.
     * @param listener The listener
     */
    void registerExecutionContextListener(ExecutionContextListener listener);

    /**
     * Deregister a listener from calling when this ExecutionContext is closing.
     * @param listener The listener
     */
    void deregisterExecutionContextListener(ExecutionContextListener listener);

    /** Close the callback handler, and disconnect any registered instance listeners. Used by JCA. */
    void closeCallbackHandler();
}