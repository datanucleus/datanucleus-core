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
2011 Andy Jefferson - all javadocs, many methods added during merge with ObjectProvider
    ...
**********************************************************************/
package org.datanucleus.state;

import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchPlan;
import org.datanucleus.FetchPlanState;
import org.datanucleus.cache.CachedPC;
import org.datanucleus.enhancement.StateManager;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.store.FieldValues;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.transaction.Transaction;

/**
 * Provider of field information for a managed object.
 * A JDO StateManager is one possible implementation of an ObjectProvider, using bytecode enhancement in that case.
 * Another possible implementation would use Java reflection to obtain and set field values in the object.
 * 
 * TODO Drop the generics and use Persistable. This will require updates to ExecutionContext to match
 * @param <T> Type of the object being managed
 */
public interface ObjectProvider<T> extends StateManager
{
    /**
     * Key prefix under which the original value of a field is stored in the entity (nondurable objects).
     * This value is set if a field is updated using a setter.
     */
    public static final String ORIGINAL_FIELD_VALUE_KEY_PREFIX = "FIELD_VALUE.ORIGINAL.";

    /** PC **/
    public static short PC = 0;
    /** Embedded (or serialised) PC **/
    public static short EMBEDDED_PC = 1;
    /** Embedded (or serialised) Collection Element PC **/
    public static short EMBEDDED_COLLECTION_ELEMENT_PC = 2;
    /** Embedded (or serialised) Map Key PC **/
    public static short EMBEDDED_MAP_KEY_PC = 3;
    /** Embedded (or serialised) Map Value PC **/
    public static short EMBEDDED_MAP_VALUE_PC = 4;

    /**
     * Method to (re)connect this provider to the specified ExecutionContext and object type.
     * @param ec ExecutionContext to connect to
     * @param cmd Metadata for this class
     */
    void connect(ExecutionContext ec, AbstractClassMetaData cmd);

    /**
     * Disconnect this provider from the ExecutionContext and PC object.
     */
    void disconnect();

    /**
     * Initialises a state manager to manage a hollow instance having the given object ID and the given (optional) field values.
     * This constructor is used for creating new instances of existing persistent objects, and consequently shouldn't be used when 
     * the StoreManager controls the creation of such objects (such as in an ODBMS).
     * @param id the JDO identity of the object.
     * @param fv the initial field values of the object (optional)
     * @param pcClass Class of the object that this will manage the state for
     */
    void initialiseForHollow(Object id, FieldValues fv, Class<T> pcClass);

    /**
     * Initialises a state manager to manage a HOLLOW / P_CLEAN instance having the given FieldValues.
     * This constructor is used for creating new instances of existing persistent objects using application 
     * identity, and consequently shouldn't be used when the StoreManager controls the creation of such objects (such as in an ODBMS).
     * @param fv the initial field values of the object.
     * @param pcClass Class of the object that this will manage the state for
     * @deprecated Remove use of this and use initialiseForHollow
     */
    void initialiseForHollowAppId(FieldValues fv, Class<T> pcClass);

    /**
     * Initialises a state manager to manage the given hollow instance having the given object ID.
     * Unlike the {@link #initialiseForHollow} method, this method does not create a new instance and instead 
     * takes a pre-constructed instance (such as from an ODBMS).
     * @param id the identity of the object.
     * @param pc the object to be managed.
     */
    void initialiseForHollowPreConstructed(Object id, T pc);

    /**
     * Initialises a state manager to manage the passed persistent instance having the given object ID.
     * Used where we have retrieved a PC object from a datastore directly (not field-by-field), for example on an object datastore. 
     * This initialiser will not add ObjectProviders to all related PCs. This must be done by any calling process. 
     * This simply adds StateManager to the specified object and records the id, setting all fields of the object as loaded.
     * @param id the identity of the object.
     * @param pc The object to be managed
     */
    void initialiseForPersistentClean(Object id, T pc);

    /**
     * Initialises a state manager to manage a persistable instance that will be EMBEDDED/SERIALISED into another persistable object. 
     * The instance will not be assigned an identity in the process since it is a SCO.
     * @param pc The persistable to manage (see copyPc also)
     * @param copyPc Whether StateManager should manage a copy of the passed PC or that one
     */
    void initialiseForEmbedded(T pc, boolean copyPc);

    /**
     * Initialises a state manager to manage a transient instance that is becoming newly persistent.
     * A new object ID for the instance is obtained from the store manager and the object is inserted in the data store.
     * <p>This constructor is used for assigning state managers to existing instances that are transitioning to a persistent state.
     * @param pc the instance being make persistent.
     * @param preInsertChanges Any changes to make before inserting
     */
    void initialiseForPersistentNew(T pc, FieldValues preInsertChanges);

    /**
     * Initialises a state manager to manage a Transactional Transient instance.
     * A new object ID for the instance is obtained from the store manager and the object is inserted in the data store.
     * <p>
     * This constructor is used for assigning state managers to Transient instances that are transitioning to a transient clean state.
     * @param pc the instance being make persistent.
     */
    void initialiseForTransactionalTransient(T pc);

    /**
     * Initialises StateManager to manage a persistable object in detached state.
     * @param pc the detach object.
     * @param id the identity of the object.
     * @param version the detached version
     */
    void initialiseForDetached(T pc, Object id, Object version);

    /**
     * Initialises StateManager to manage a persistable object that is not persistent but is about to be deleted.
     * @param pc the object to delete
     */
    void initialiseForPNewToBeDeleted(T pc);

    /**
     * Initialise StateManager, assigning the specified id to the object. 
     * This is used when getting objects out of the L2 Cache, where they have no ObjectProvider assigned, and returning them as associated with a particular ExecutionContext.
     * @param cachedPC The cached PC object
     * @param id Id to assign to the persistable object
     */
    void initialiseForCachedPC(CachedPC cachedPC, Object id);

    /**
     * Accessor for the ClassMetaData for this object.
     * @return The ClassMetaData.
     */
    AbstractClassMetaData getClassMetaData();

    ExecutionContext getExecutionContext();

    StoreManager getStoreManager();

    /**
     * Accessor for the persistable object being managed.
     * @return the persistable object
     */
    T getObject();

    /**
     * Returns a printable form of the managed object.
     * @return The object reference for the persistable object.
     */
    String getObjectAsPrintable();

    /**
     * Accessor for the id of the object being managed.
     * @return The identity of the object
     */
    Object getInternalObjectId();

    Object getExternalObjectId();

    /**
     * Accessor for the LifeCycleState
     * @return the LifeCycleState
     */
    LifeCycleState getLifecycleState();

    /**
     * Method to change the value of the specified field. This WILL NOT make the field dirty.
     * @param fieldNumber (absolute) field number of the field
     * @param value The new value.
     */
    void replaceField(int fieldNumber, Object value);

    /**
     * Method to change the value of the specified field. This WILL make the field dirty.
     * @param fieldNumber (absolute) field number of the field
     * @param value The new value.
     */
    void replaceFieldMakeDirty(int fieldNumber, Object value);

    /**
     * Convenience method to change the value of a field that is assumed loaded.
     * Will mark the object/field as dirty if it isn't previously. Only for use in management of relations.
     * @param fieldNumber Number of field
     * @param newValue The new value
     */
    void replaceFieldValue(int fieldNumber, Object newValue);

    /**
     * Method to update the data in the object with the values from the passed FieldManager.
     * @param fieldNumbers (absolute) field numbers of the fields to update
     * @param fm The FieldManager
     */
    void replaceFields(int fieldNumbers[], FieldManager fm);

    /**
     * Method to update the data in the object with the values from the passed FieldManager.
     * @param fieldNumbers (absolute) field numbers of the fields to update
     * @param fm The FieldManager
     * @param replaceWhenDirty Whether to replace these fields if the field is dirty
     */
    void replaceFields(int fieldNumbers[], FieldManager fm, boolean replaceWhenDirty);

    /**
     * Method to update the data in the object with the values from the passed FieldManager. Only non loaded fields are updated.
     * @param fieldNumbers (absolute) field numbers of the fields to update
     * @param fm The FieldManager
     */
    void replaceNonLoadedFields(int fieldNumbers[], FieldManager fm);

    /**
     * Method to replace all loaded SCO fields with wrappers.
     * If the loaded field already uses a SCO wrapper nothing happens to that field.
     */
    void replaceAllLoadedSCOFieldsWithWrappers();

    /**
     * Method to replace all loaded (wrapped) SCO fields with unwrapped values.
     * If the loaded field doesnt use a SCO wrapper nothing happens to that field.
     */
    void replaceAllLoadedSCOFieldsWithValues();

    /**
     * Method to obtain updated field values from the passed FieldManager.
     * @param fieldNumbers The numbers of the fields
     * @param fm The fieldManager
     */
    void provideFields(int fieldNumbers[], FieldManager fm);

    /**
     * Method to return the current value of the specified field.
     * @param fieldNumber (absolute) field number of the field
     * @return The current value
     */
    Object provideField(int fieldNumber);

    /**
     * Method to set an associated value stored with this object.
     * This is for a situation such as in ORM where this object can have an "external" foreign-key provided by an owning object 
     * (e.g 1-N uni relation and this is the element with no knowledge of the owner, so the associated value is the FK value).
     * @param key Key for the value
     * @param value The associated value
     */
    void setAssociatedValue(Object key, Object value);

    /**
     * Accessor for the value of an external field.
     * This is for a situation such as in ORM where this object can have an "external" foreign-key provided by an owning object 
     * (e.g 1-N uni relation and this is the element with no knowledge of the owner, so the associated value is the FK value).
     * @param key The key for this associated information
     * @return The value stored (if any) against this key
     */
    Object getAssociatedValue(Object key);

    /**
     * Method to remove the associated value with the specified key (if it exists).
     * @param key The key
     */
    void removeAssociatedValue(Object key);

    /**
     * Accessor for the field numbers of all dirty fields.
     * @return Absolute field numbers of the dirty fields in this instance.
     */
    int[] getDirtyFieldNumbers();
    
    /**
     * Accessor for the names of the fields that are dirty.
     * @return Names of the dirty fields
     */
    String[] getDirtyFieldNames();

    /**
     * Creates a copy of the internal dirtyFields array.
     * @return a copy of the internal dirtyFields array.
     */
    boolean[] getDirtyFields();

    /**
     * Marks the given field dirty.
     * @param field The no of field to mark as dirty. 
     */
    void makeDirty(int field);

    /**
     * Convenience accessor for whether this ObjectProvider manages an embedded/serialised object.
     * @return Whether the managed object is embedded/serialised.
     */
    boolean isEmbedded();

    /**
     * Method to update the owner field in an embedded field.
     * @param fieldNumber The field of this object that is embedded
     * @param value The value of the field (embedded)
     */
    void updateOwnerFieldInEmbeddedField(int fieldNumber, Object value);

    /**
     * Method to set this ObjectProvider as managing an embedded/serialised object.
     * @param type The type of object being managed
     */
    void setPcObjectType(short type);

    /**
     * Method to set the storing PC flag.
     */
    void setStoringPC();

    /**
     * Method to unset the storing PC flag.
     */
    void unsetStoringPC();

    /**
     * Accessor for whether all changes have been written to the datastore.
     * @return Whether the datastore has all changes
     */
    boolean isFlushedToDatastore();

    /**
     * Whether this record has been flushed to the datastore in this transaction (i.e called persist() and is in the datastore now). 
     * If user has called persist() on it yet not yet persisted then returns false.
     * @return Whether this is flushed new.
     */
    boolean isFlushedNew();

    void setFlushedNew(boolean flag);

    /**
     * Method to flush all changes to the datastore.
     */
    void flush();

    void setFlushing(boolean flushing);

    /**
     * Method to notify the StateManager that the object has now been flushed to the datastore.
     * This is performed when handling inserts or deletes in a batch external to StateManager.
     */
    void markAsFlushed();

    /**
     * Method to locate that the object exists in the datastore.
     * @throws NucleusObjectNotFoundException if not present
     */
    void locate();

    /**
     * Tests whether this object is new yet waiting to be flushed to the datastore.
     * @return true if this instance is waiting to be flushed
     */
    boolean isWaitingToBeFlushedToDatastore();

    /**
     * Update the activity state.
     * @param state the activity state
     */
    void changeActivityState(ActivityState state);

    /**
     * Tests whether this object is being inserted.
     * @return true if this instance is inserting.
     */
    boolean isInserting();

    /**
     * Tests whether this object is being deleted.
     * @return true if this instance is being deleted.
     */
    boolean isDeleting();

    /**
     * Whether this object is moving to a deleted state.
     * @return Whether the object will be moved into a deleted state during this operation
     */
    boolean becomingDeleted();

    /**
     * Tests whether this object has been deleted.
     * Instances that have been deleted in the current transaction return true. Transient instances return false.
     * @return true if this instance was deleted in the current transaction.
     */
    boolean isDeleted();

    /**
     * Tests whether this object is in the process of being detached.
     * @return true if this instance is being detached.
     */
    boolean isDetaching();

    /**
     * Convenience method to load the passed field values.
     * Loads the fields using any required fetch plan and calls postLoad as appropriate.
     * @param fv Field Values to load (including any fetch plan to use when loading)
     */
    void loadFieldValues(FieldValues fv);

    /**
     * Accessor for the referenced PC object when we are attaching or detaching.
     * When attaching and this is the detached object this returns the newly attached object.
     * When attaching and this is the newly attached object this returns the detached object.
     * When detaching and this is the newly detached object this returns the attached object.
     * When detaching and this is the attached object this returns the newly detached object.
     * @return The referenced object (or null).
     */
    T getReferencedPC();

    /**
     * Convenience method to load the specified field if not loaded.
     * @param fieldNumber Absolute field number
     */
    void loadField(int fieldNumber);

    /**
     * Method to load all unloaded fields in the FetchPlan.
     * Recurses through the FetchPlan objects and loads fields of sub-objects where needed.
     * @param state The FetchPlan state
     */
    void loadFieldsInFetchPlan(FetchPlanState state);

    /**
     * Convenience method to load a field from the datastore.
     * Used in attaching fields and checking their old values (so we don't want any postLoad method being called).
     * TODO Merge this with one of the loadXXXFields methods.
     * @param fieldNumber The field number.
     */
    void loadFieldFromDatastore(int fieldNumber);

    /**
     * Loads (from the database) all unloaded fields that are in the current FetchPlan.
     */
    void loadUnloadedFieldsInFetchPlan();

    /**
     * Loads (from the database) all unloaded fields of the managed class that are in the specified FetchPlan.
     * @param fetchPlan The FetchPlan
     */
    void loadUnloadedFieldsOfClassInFetchPlan(FetchPlan fetchPlan);

    /**
     * Loads (from the database) all unloaded fields that store relations.
     */
    void loadUnloadedRelationFields();

    /**
     * Fetch from the database all fields that are not currently loaded regardless of whether
     * they are in the current fetch group or not. Called by lifecycle transitions.
     */
    void loadUnloadedFields();

    /**
     * Method that will unload all fields that are not in the FetchPlan.
     */
    void unloadNonFetchPlanFields();

    /**
     * Refreshes from the database all fields currently loaded.
     * Called by life-cycle transitions.
     */
    void refreshLoadedFields();

    /**
     * Method to clear all saved fields on the object.
     */
    void clearSavedFields();
    
    /**
     * Refreshes from the database all fields in fetch plan.
     * Called by life-cycle transitions.
     */
    void refreshFieldsInFetchPlan();
    
    /**
     * Method to clear all fields that are not part of the primary key of the object.
     */
    void clearNonPrimaryKeyFields();
    
    /**
     * Method to restore all fields of the object.
     */
    void restoreFields();

    /**
     * Method to save all fields of the object.
     */
    void saveFields();
    
    /**
     * Method to clear all fields of the object.
     */
    void clearFields();
    
    /**
     * Registers the pc class in the cache
     */
    void registerTransactional();

    /**
     * Accessor for the Restore Values flag 
     * @return Whether to restore values
     */
    boolean isRestoreValues();

    /**
     * Method to clear all loaded flags on the object.
     */
    void clearLoadedFlags();

    /**
     * Mark the specified field as not loaded so that it will be reloaded on next access.
     * @param fieldName Name of the field
     */
    void unloadField(String fieldName);

    boolean[] getLoadedFields();

    /**
     * Accessor for the field numbers of all loaded fields.
     * @return Absolute field numbers of the loaded fields in this instance.
     */
    int[] getLoadedFieldNumbers();
    
    /**
     * Accessor for the names of the fields that are loaded.
     * @return Names of the loaded fields
     */
    String[] getLoadedFieldNames();

    boolean isLoaded(int absoluteFieldNumber);

    /**
     * Returns whether all fields are loaded.
     * @return Returns true if all fields are loaded.
     */
    boolean getAllFieldsLoaded();

    /**
     * Accessor for whether a field is currently loaded.
     * Just returns the status, unlike "isLoaded" which also loads it if not.
     * @param fieldNumber The (absolute) field number
     * @return Whether it is loaded
     */
    boolean isFieldLoaded(int fieldNumber);

    /**
     * Marks the given field dirty for issuing an update after the insert.
     * @param pc The Persistable object
     * @param fieldNumber The no of field to mark as dirty. 
     */
    void updateFieldAfterInsert(Object pc, int fieldNumber);

    /**
     * Method to allow the setting of the id of the PC object. 
     * This is used when it is obtained after persisting the object to the datastore. 
     * In the case of RDBMS, this may be via auto-increment, or in the case of ODBMS this may be an accessor for the id after storing.
     * @param id the id received from the datastore. May be an OID, or the key value for an OID, or an application id.
     */
    void setPostStoreNewObjectId(Object id);

    /**
     * Method to swap the managed object for the supplied object.
     * This is of particular use for object datastores where the datastore is responsible for creating the in-memory object and where we 
     * have a temporary object that we want to swap for the datastore generated object. 
     * Makes no change to what fields are loaded/dirty etc, just swaps the managed object.
     * @param pc The persistable object to use
     */
    void replaceManagedPC(T pc);

    /**
     * Sets the value for the version column in a transaction not yet committed
     * @param nextVersion version to use
     */
    void setTransactionalVersion(Object nextVersion);

    /**
     * Return the object representing the transactional version of the managed object.
     * @return the object representing the version of the calling instance
     */
    Object getTransactionalVersion();

    /**
     * Method to set the current version of the managed object.
     * @param version The version
     */
    void setVersion(Object version);

    /**
     * Method to return the current version of the managed object.
     * @return The version
     */
    Object getVersion();

    /**
     * Method to return if the version is loaded.
     * If the class represented is not versioned then returns true.
     * @return Whether it is loaded.
     */
    boolean isVersionLoaded();

    void evictFromTransaction();

    void enlistInTransaction();

    // Behaviour methods

    /**
     * Method to make the managed object transactional.
     */
    void makeTransactional();

    /**
     * Method to make the managed object nontransactional.
     */
    void makeNontransactional();

    /**
     * Method to make the managed object transient.
     * @param state Object containing the state of any fetch plan processing
     */
    void makeTransient(FetchPlanState state);

    /**
     * Make the managed object transient due to reachability (at commit) finding it not needing to be persisted.
     */
    void makeTransientForReachability();

    /**
     * Method to make the managed object persistent.
     */
    void makePersistent();
    
    /**
     * Method to make Transactional Transient instances persistent
     */
    void makePersistentTransactionalTransient();

    /**
     * Method to delete the object from persistence.
     */
    void deletePersistent();

    /**
     * Method to attach to this the detached persistable instance
     * @param detachedPC the detached persistable instance to be attached
     * @param embedded Whether it is embedded
     * @return The attached copy
     */
    T attachCopy(T detachedPC, boolean embedded);

    /**
     * Method to attach the object being managed.
     * @param embedded Whether it is embedded
     */
    void attach(boolean embedded);

    /**
     * Method to attach the provided detached object into the managed instance.
     * @param detachedPC Detached object
     */
    void attach(T detachedPC);

    /**
     * Method to make detached copy of this instance
     * @param state State for the detachment process
     * @return the detached persistable instance
     */
    T detachCopy(FetchPlanState state);

    /**
     * Method to detach the managed object.
     * @param state State for the detachment process
     */
    void detach(FetchPlanState state);

    /**
     * Validates whether the persistence capable instance exists in the datastore.
     * If the instance does not exist in the datastore, this method will fail raising a NucleusObjectNotFoundException.
     */
    void validate();

    /**
     * Mark the state manager as needing to validate the inheritance of the managed object existence before loading fields.
     */
    void markForInheritanceValidation();

    /**
     * Method to change the object state to evicted.
     */
    void evict();

    /**
     * Method to refresh the values of the currently loaded fields in the managed object.
     */
    void refresh();

    /**
     * Method to retrieve the fields for this object.
     * @param fgOnly Whether to retrieve just the current fetch plan fields
     */
    void retrieve(boolean fgOnly);

    // Transaction handling methods

    /**
     * Convenience interceptor to allow operations to be performed before the begin is performed
     * @param tx The transaction
     */
    void preBegin(Transaction tx);

    /**
     * Convenience interceptor to allow operations to be performed after the commit is performed
     * but before returning control to the application.
     * @param tx The transaction
     */
    void postCommit(Transaction tx);

    /**
     * Convenience interceptor to allow operations to be performed before any rollback is performed.
     * @param tx The transaction
     */
    void preRollback(Transaction tx);

    /**
     * Convenience method to reset the detached state in the current object.
     */
    void resetDetachState();

    /**
     * Convenience method to retrieve the detach state from the passed StateManager's managed object
     * @param sm StateManager
     */
    void retrieveDetachState(ObjectProvider sm);

    /**
     * Look to the database to determine which class this object is. 
     * This parameter is a hint. Set false, if it's already determined the correct pcClass for this pc "object" in a certain level in the hierarchy. 
     * Set to true and it will look to the database.
     * TODO This is only called by some outdated code in LDAPUtils; remove it when that is fixed
     * @param fv the initial field values of the object.
     * @deprecated Dont use this, to be removed
     */
    void checkInheritance(FieldValues fv);

    /**
     * Convenience method to mark all fields as "loaded".
     * NOTE: This is a convenience mutator only to be used when you know what you are doing. Currently only used by the XML plugin.
     * @param fieldNumbers The field numbers to mark as loaded
     */
    void markFieldsAsLoaded(int[] fieldNumbers);
}