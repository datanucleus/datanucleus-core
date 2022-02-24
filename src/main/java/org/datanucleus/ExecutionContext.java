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

import org.datanucleus.api.ApiAdapter;
import org.datanucleus.cache.Level1Cache;
import org.datanucleus.enhancement.ExecutionContextReference;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.exceptions.ClassNotPersistableException;
import org.datanucleus.exceptions.NoPersistenceInformationException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.flush.FlushMode;
import org.datanucleus.flush.Operation;
import org.datanucleus.flush.OperationQueue;
import org.datanucleus.management.ManagerStatistics;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.state.LockManager;
import org.datanucleus.state.DNStateManager;
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
 * <li>provide a way of managing persistable objects using StateManagers</li>
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
     * Accessor for the lock manager for objects in this execution context.
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
    void setProperties(Map<String, Object> props);

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
     * Convenience accessor for the type manager for this persistence context (from NucleusContext).
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
     * Method to find StateManager for the passed persistable object when it is managed by this manager.
     * @param pc The persistable object
     * @return StateManager
     */
    DNStateManager findStateManager(Object pc);

    /**
     * Method to find StateManager for the passed persistable object when it is managed by this manager, 
     * and if not yet persistent to persist it and return the assigned StateManager.
     * @param pc The persistable object
     * @param persist Whether to persist if not yet persistent
     * @return StateManager
     */
    DNStateManager findStateManager(Object pc, boolean persist);

    /**
     * Method to find StateManager for the passed embedded persistable object.
     * Will create one if not already registered, and tie it to the specified owner.
     * @param value The embedded object
     * @param owner The owner StateManager (if known).
     * @param mmd Metadata for the field of the owner
     * @param objectType Type of persistable object being stored
     * @return StateManager for the embedded object
     */
    DNStateManager findStateManagerForEmbedded(Object value, DNStateManager owner, AbstractMemberMetaData mmd, PersistableObjectType objectType);

    DNStateManager findStateManagerOfOwnerForAttachingObject(Object pc);

    /**
     * Method to add the object managed by the specified StateManager to the cache.
     * @param sm StateManager
     */
    void addStateManagerToCache(DNStateManager sm);

    /**
     * Method to remove the object managed by the specified StateManager from the cache.
     * @param sm StateManager
     */
    void removeStateManagerFromCache(DNStateManager sm);

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
     * Method to make an object persistent.
     * NOT to be called by internal DataNucleus methods. Only callable by external APIs (JDO/JPA).
     * @param pc The object
     * @param merging Whether this object (and dependents) is being merged
     * @param <T> Type of the persistable object
     * @return The persisted object
     * @throws NucleusUserException if the object is managed by a different manager
     */
    <T> T persistObject(T pc, boolean merging);

    /**
     * Method to persist an array of objects to the datastore.
     * @param objs The objects to persist
     * @return The persisted objects
     * @throws NucleusUserException Thrown if an error occurs during the persist process.
     *     Any exception could have several nested exceptions for each failed object persist
     */
    Object[] persistObjects(Object... pcs);

    /**
     * Method to make an object persistent which should be called from internal calls only.
     * All PM/EM calls should go via persistObject(Object obj).
     * @param obj The object
     * @param preInsertChanges Any changes to make before inserting
     * @param ownerSM StateManager of the owner when embedded
     * @param ownerFieldNum Field number in the owner where this is embedded (or -1 if not embedded)
     * @param objectType Type of object
     * @return The persisted object
     * @throws NucleusUserException if the object is managed by a different manager
     */
    <T> T persistObjectInternal(T pc, FieldValues preInsertChanges, DNStateManager ownerSM, int ownerFieldNum, PersistableObjectType objectType);

    /**
     * Method to persist the passed object (internally).
     * @param pc The object
     * @param ownerSM StateManager of the owner when embedded
     * @param ownerFieldNum Field number in the owner where this is embedded (or -1 if not embedded)
     * @param objectType Type of object
     * @param <T> Type of the persistable object
     * @return The persisted object
     */
    <T> T persistObjectInternal(T pc, DNStateManager ownerSM, int ownerFieldNum, PersistableObjectType objectType);

    /**
     * Method to persist the passed object (internally).
     * @param pc The object
     * @param preInsertChanges Changes to be made before inserting
     * @param objectType Type of object
     * @param <T> Type of the persistable object
     * @return The persisted object
     */
    default <T> T persistObjectInternal(T pc, FieldValues preInsertChanges, PersistableObjectType objectType)
    {
        return persistObjectInternal(pc, preInsertChanges, null, -1, objectType);
    }

    /**
     * Method to migrate an object to transient state.
     * @param obj The object
     * @param state Object containing the state of the fetch plan process (if any)
     * @throws NucleusException When an error occurs in making the object transient
     */
    void makeObjectTransient(Object pc, FetchPlanState state);

    /**
     * Method to make an object transactional.
     * @param obj The object
     * @throws NucleusException Thrown when an error occurs
     */
    void makeObjectTransactional(Object pc);

    /**
     * Method to make the passed object nontransactional.
     * @param pc The object
     * @throws NucleusException Thrown when an error occurs
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
     * @param states States that we want the enlisted objects for
     * @param classes Classes that we want the enlisted objects for
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
     * NOT to be called by internal methods. Only callable by external APIs (JDO/JPA).
     * @param obj The object
     */
    void deleteObject(Object obj);

    /**
     * Method to delete an array of objects from the datastore.
     * @param objs The objects to delete
     * @throws NucleusUserException Thrown if an error occurs during the deletion process. Any exception could have several nested exceptions for each failed object deletion
     */
    void deleteObjects(Object... objs);

    /**
     * Method to delete an object from persistence which should be called from internal calls only.
     * All PM/EM calls should go via deleteObject(Object obj).
     * @param obj Object to delete
     */
    void deleteObjectInternal(Object pc);

    /**
     * Method to detach a persistent object without making a copy. 
     * Note that also all the objects which are refered to from this object are detached.
     * If the object is of class that is not detachable a ClassNotDetachableException will be thrown. 
     * If the object is not persistent a NucleusUserException is thrown.
     * @param state State for the detachment process
     * @param obj The object
     */
    void detachObject(FetchPlanState state, Object pc);

    /**
     * Method to detach the passed object(s).
     * @param state State for the detachment process.
     * @param pcs The object(s) to detach
     */
    void detachObjects(FetchPlanState state, Object... pcs);

    /**
     * Detach a copy of the passed persistent object using the provided detach state.
     * If the object is of class that is not detachable it will be detached as transient.
     * If it is not yet persistent it will be first persisted.
     * @param state State for the detachment process
     * @param pc The object
     * @param <T> Type of the persistable object
     * @return The detached object
     */
    <T> T detachObjectCopy(FetchPlanState state, T pc);

    /**
     * Method to detach all objects in the context.
     * Detaches all objects enlisted as well as all objects in the L1 cache.
     * Of particular use with JPA when doing a clear of the persistence context.
     */
    void detachAll();

    /**
     * Method to attach a persistent detached object.
     * If a different object with the same identity as this object exists in the L1 cache then an exception will be thrown.
     * @param ownerSM StateManager of the owner object that has this in a field that causes this attach
     * @param pc The persistable object
     * @param sco Whether the PC object is stored without an identity (embedded/serialised)
     */
    void attachObject(DNStateManager sm, Object pc, boolean sco);

    /**
     * Method to attach a persistent detached object returning an attached copy of the object.
     * If the object is of class that is not detachable, a ClassNotDetachableException will be thrown.
     * @param ownerSM StateManager of the owner object that has this in a field that causes this attach
     * @param pc The object
     * @param sco Whether it has no identity (second-class object)
     * @param <T> Type of the persistable object
     * @return The attached object
     */
    <T> T attachObjectCopy(DNStateManager sm, T pc, boolean sco);

    /**
     * Convenience method to return the attached object for the specified id if one exists.
     * Returns null if there is no currently enlisted/cached object with the specified id.
     * @param id The id
     * @return The attached object
     */
    Object getAttachedObjectForId(Object id);

    /**
     * Method to do a refresh of an object, updating it from its datastore representation. 
     * Also updates the object in the L1/L2 caches.
     * @param pc The object
     */
    void refreshObject(Object pc);

    /**
     * Method to do a refresh of all objects.
     * @throws NucleusUserException thrown if instances could not be refreshed.
     */
    void refreshAllObjects();

    /**
     * Method to enlist the specified StateManager in the current transaction.
     * @param sm StateManager
     */
    void enlistInTransaction(DNStateManager sm);

    /**
     * Method to evict the specified StateManager from the current transaction.
     * @param sm StateManager
     */
    void evictFromTransaction(DNStateManager sm);

    /**
     * Method to return if an object is enlisted in the current transaction.
     * @param id Identity for the object
     * @return Whether it is enlisted in the current transaction
     */
    boolean isEnlistedInTransaction(Object id);

    /**
     * Mark the specified StateManager as dirty
     * @param sm StateManager
     * @param directUpdate Whether the object has had a direct update made on it (if known)
     */
    void markDirty(DNStateManager sm, boolean directUpdate);

    /**
     * Mark the specified StateManager as clean.
     * @param sm StateManager
     */
    void clearDirty(DNStateManager sm);

    /**
     * Method to mark as clean all StateManagers of dirty objects.
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
     * @return The object for this id.
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
     * Accessor for objects with the specified identities.
     * @param ids Identities of the object(s).
     * @param validate Whether to validate the object state
     * @return The Objects with these ids (same order)
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
     * Accessor for an object given the object id and a set of field values to apply to it.
     * This is intended for use where we have done a query and have the id from the results, and we want to
     * create the object, preferably using the cache, and then apply any field values to it.
     * @param id Id of the object.
     * @param fv Field values for the object (to copy in)
     * @param cls the type which the object is (optional). Used to instantiate the object
     * @param ignoreCache true if it must ignore the cache
     * @param checkInheritance Whether to check the inheritance on the id of the object
     * @return The Object
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
     * Method to put a Persistable object associated to StateManager into the L1 cache.
     * @param sm StateManager
     */
    void putObjectIntoLevel1Cache(DNStateManager sm);

    /**
     * Method to add/update the managed object into the L2 cache as long as it isn't modified in the current transaction.
     * @param sm StateManager for the object
     * @param updateIfPresent Whether to update it in the L2 cache if already present
     */
    void putObjectIntoLevel2Cache(DNStateManager sm, boolean updateIfPresent);

    /**
     * Convenience method to access an object in the cache.
     * Firstly looks in the L1 cache for this ExecutionContext, and if not found looks in the L2 cache.
     * @param id Id of the object
     * @return Persistable object (with connected StateManager).
     */
    Persistable getObjectFromCache(Object id);

    /**
     * Convenience method to access objects in the cache.
     * Firstly looks in the L1 cache, and if not found looks in the L2 cache.
     * @param ids Ids of the objects
     * @return Persistable objects (with connected StateManager).
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
     * Whether the datastore operations are delayed until commit/flush.
     * In optimistic transactions this is automatically enabled.
     * In datastore transactions there is a persistence property to enable it.
     * If we are committing/flushing then will return false since the delay is no longer required.
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
     * @param sm StateManager
     */
    void flushOperationsForBackingStore(Store backingStore, DNStateManager sm);

    /**
     * Convenience method to inspect the list of objects with outstanding changes to flush.
     * @return StateManagers for the objects to be flushed.
     */
    List<DNStateManager> getObjectsToBeFlushed();

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
     * Accessor for the RelationshipManager for the provided StateManager.
     * @param sm StateManager
     * @return The RelationshipManager
     */
    RelationshipManager getRelationshipManager(DNStateManager sm);

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
     * Accessor for the tenant id, for this ExecutionContext.
     * @return The tenant id for this context.
     */
    String getTenantId();

    /**
     * Accessor for the current user, for this ExecutionContext.
     * @return The current user for this context
     */
    String getCurrentUser();

    /**
     * Method to lock this ExecutionContext for threading
     */
    default void threadLock()
    {
        return;
    }

    /**
     * Method to unlock this ExecutionContext for threading
     */
    default void threadUnlock()
    {
        return;
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
     * Access a referenced object for this StateManager during the attach/detach process.
     * When attaching and this is the detached object this returns the newly attached object.
     * When attaching and this is the newly attached object this returns the detached object.
     * When detaching and this is the newly detached object this returns the attached object.
     * When detaching and this is the attached object this returns the newly detached object.
     * @param sm StateManager
     * @return The referenced object (if any)
     */
    Object getAttachDetachReferencedObject(DNStateManager sm);

    /**
     * Register a referenced object against this StateManager for the attach/detach process.
     * @param sm StateManager
     * @param obj The referenced object (or null to clear out any reference)
     */
    void setAttachDetachReferencedObject(DNStateManager sm, Object obj);

    /**
     * Method to register an embedded relation for the specified member of the owner StateManager where the embedded StateManager is stored.
     * @param ownerSM Owner StateManager
     * @param ownerMemberNum Member number that is embedded
     * @param objectType Type of object being persisted
     * @param embSM StateManager of the embedded object
     * @return The EmbeddedOwnerRelation
     */
    EmbeddedOwnerRelation registerEmbeddedRelation(DNStateManager ownerSM, int ownerMemberNum, PersistableObjectType objectType, DNStateManager embSM);

    /**
     * Method to deregister the specified embedded relation (e.g when the embedded object is disconnected).
     * @param rel The embedded relation
     */
    void deregisterEmbeddedRelation(EmbeddedOwnerRelation rel);

    /**
     * Accessor for the relations for the specified embedded StateManager where it is embedded.
     * @param ownerSM StateManager that owns the embedded
     * @return The List of embedded relations involving this StateManager as owner
     */
    List<EmbeddedOwnerRelation> getEmbeddedInformationForOwner(DNStateManager ownerSM);

    /**
     * Accessor for the owner relation for the specified embedded StateManager where it is embedded.
     * @param embSM StateManager that is embedded
     * @return The embedded relation info involving this (embedded) StateManager
     */
    EmbeddedOwnerRelation getOwnerInformationForEmbedded(DNStateManager embSM);

    /**
     * Accessor for the owner StateManager for the provided embedded StateManager.
     * @param embSM StateManager that is embedded
     * @return The owner StateManager that have this object embedded.
     */
    DNStateManager getOwnerForEmbeddedStateManager(DNStateManager embSM);

    /**
     * Convenience method to remove the EmbeddedOwnerRelation between the specified StateManagers.
     * @param ownerSM Owner StateManager
     * @param ownerFieldNum Field in owner
     * @param embSM Embedded StateManager
     */
    void removeEmbeddedOwnerRelation(DNStateManager ownerSM, int ownerFieldNum, DNStateManager embSM);

    public static class EmbeddedOwnerRelation
    {
        protected DNStateManager ownerSM;
        protected int ownerMemberNum;
        protected PersistableObjectType objectType;

        protected DNStateManager embeddedSM;

        public EmbeddedOwnerRelation(DNStateManager sm, int memberNum, PersistableObjectType objectType, DNStateManager embSM)
        {
            this.ownerSM = sm;
            this.ownerMemberNum = memberNum;
            this.objectType = objectType;
            this.embeddedSM = embSM;
        }
        public DNStateManager getOwnerSM() {return ownerSM;}
        public int getOwnerMemberNum() {return ownerMemberNum;}
        public PersistableObjectType getObjectType() {return objectType;}
        public DNStateManager getEmbeddedSM() {return embeddedSM;}
    }

    void setStateManagerAssociatedValue(DNStateManager sm, Object key, Object value);
    Object getStateManagerAssociatedValue(DNStateManager sm, Object key);
    void removeStateManagerAssociatedValue(DNStateManager sm, Object key);
    boolean containsStateManagerAssociatedValue(DNStateManager sm, Object key);

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