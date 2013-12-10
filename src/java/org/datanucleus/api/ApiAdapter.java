/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.api;

import java.io.Serializable;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.state.LifeCycleState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.FieldManager;

/**
 * Adapter to allow the core runtime to expose multiple APIs to clients.
 */
public interface ApiAdapter extends Serializable
{
    /**
     * Accessor for the name of the API.
     * @return Name of the API
     */
    String getName();

    /**
     * Accessor for whether this member type is default persistent.
     * @param type Member type
     * @return Whether it is by default persistent
     */
    boolean isMemberDefaultPersistent(Class type);

    // ------------------------------ Object Lifecycle --------------------------------

    /**
     * Whether the provided object is currently managed (has an ExecutionContext).
     * @return Whether it is managed
     */
    boolean isManaged(Object pc);

    /**
     * Method to return the ExecutionContext (if any) associated with the passed object.
     * Supports persistable objects, and PersistenceManager.
     * @param pc The object
     * @return The ExecutionContext
     */
    ExecutionContext getExecutionContext(Object pc);

    /**
     * Returns the LifeCycleState for the state constant.
     * @param stateType the type as integer
     * @return the type as LifeCycleState object
     */
    LifeCycleState getLifeCycleState(int stateType);

    /**
     * Accessor for whether the passed object is persistent.
     * @param obj The object
     * @return Whether it is persistent
     */
    boolean isPersistent(Object obj);

    /**
     * Accessor for whether the passed object is new.
     * @param obj The object
     * @return Whether it is new
     */
    boolean isNew(Object obj);

    /**
     * Accessor for whether the passed object is dirty.
     * @param obj The object
     * @return Whether it is dirty
     */
    boolean isDirty(Object obj);

    /**
     * Accessor for whether the passed object is deleted.
     * @param obj The object
     * @return Whether it is deleted
     */
    boolean isDeleted(Object obj);

    /**
     * Accessor for whether the passed object is detached.
     * @param obj The object
     * @return Whether it is detached
     */
    boolean isDetached(Object obj);

    /**
     * Accessor for whether the passed object is transactional.
     * @param obj The object
     * @return Whether it is transactional
     */
    boolean isTransactional(Object obj);

    /**
     * Method to return if the passed object is persistable using this API.
     * @param obj The object
     * @return Whether it is persistable
     */
    boolean isPersistable(Object obj);

    /**
     * Utility method to check if the specified class is of a type that can be persisted for this API.
     * @param cls The class to check
     * @return Whether the class is persistable using this API.
     */
    boolean isPersistable(Class cls);

    /**
     * Method to return if the passed object is detachable using this API.
     * @param obj The object
     * @return Whether it is detachable
     */
    boolean isDetachable(Object obj);

    /**
     * Accessor for the object state.
     * @param obj Object
     * @return The state ("persistent-clean", "detached-dirty" etc)
     */
    String getObjectState(Object obj);

    /**
     * Method to make the member of the persistable object dirty.
     * @param obj The object
     * @param member Name of the member
     */
    void makeDirty(Object obj, String member);

    // ------------------------------ Object Identity  --------------------------------

    /**
     * Method to return the object identity for the passed persistable object.
     * Returns null if it is not persistable, or has no identity.
     * @param obj The object
     * @return The identity
     */
    Object getIdForObject(Object obj);

    /**
     * Method to return the object version for the passed persistable object.
     * Returns null if it is not persistable, or not versioned.
     * @param obj The object
     * @return The version
     */
    Object getVersionForObject(Object obj);

    /**
     * Utility to check if a primary-key class is valid.
     * Will throw a InvalidPrimaryKeyException if it is invalid, otherwise returning true.
     * @param pkClass The Primary Key class
     * @param cmd AbstractClassMetaData for the persistable class
     * @param clr the ClassLoaderResolver
     * @param noOfPkFields Number of primary key fields
     * @param mmgr MetaData manager
     * @return Whether it is valid
     */
    boolean isValidPrimaryKeyClass(Class pkClass, AbstractClassMetaData cmd, ClassLoaderResolver clr, 
            int noOfPkFields, MetaDataManager mmgr);

    /**
     * Accessor for whether the passed identity is a valid single-field application-identity for this API.
     * @return Whether it is valid
     */
    boolean isSingleFieldIdentity(Object id);

    /**
     * Accessor for whether the passed identity is a valid datastore-identity for this API.
     * @return Whether it is valid
     */
    boolean isDatastoreIdentity(Object id);

    /**
     * Accessor for whether the passed class name is a valid single-field application-identity for this API.
     * @param className Name of the class
     * @return Whether it is valid
     */
    boolean isSingleFieldIdentityClass(String className);

    /**
     * Accessor for the class name to use for identities when there is a single Long/long field.
     * @return Class name of identity class
     */
    String getSingleFieldIdentityClassNameForLong();

    /**
     * Accessor for the class name to use for identities when there is a single Integer/int field.
     * @return Class name of identity class
     */
    String getSingleFieldIdentityClassNameForInt();

    /**
     * Accessor for the class name to use for identities when there is a single Short/short field.
     * @return Class name of identity class
     */
    String getSingleFieldIdentityClassNameForShort();

    /**
     * Accessor for the class name to use for identities when there is a single Byte/byte field.
     * @return Class name of identity class
     */
    String getSingleFieldIdentityClassNameForByte();

    /**
     * Accessor for the class name to use for identities when there is a single Character/char field.
     * @return Class name of identity class
     */
    String getSingleFieldIdentityClassNameForChar();

    /**
     * Accessor for the class name to use for identities when there is a single String field.
     * @return Class name of identity class
     */
    String getSingleFieldIdentityClassNameForString();

    /**
     * Accessor for the class name to use for identities when there is a single Object field.
     * @return Class name of identity class
     */
    String getSingleFieldIdentityClassNameForObject();

    /**
     * Accessor for the target class for the specified single field identity.
     * @param id The identity
     * @return The target class
     */
    Class getTargetClassForSingleFieldIdentity(Object id);

    /**
     * Accessor for the target class name for the specified single field identity.
     * @param id The identity
     * @return The target class name
     */
    String getTargetClassNameForSingleFieldIdentity(Object id);

    /**
     * Accessor for the key object for the specified single field identity.
     * @param id The identity
     * @return The key object
     */
    Object getTargetKeyForSingleFieldIdentity(Object id);

    /**
     * Accessor for the type of the single field application-identity key given the single field identity type.
     * @param idType Single field identity type
     * @return key type
     */
    Class getKeyTypeForSingleFieldIdentityType(Class idType);

    /**
     * Utility to create a new SingleFieldIdentity using reflection when you know the
     * type of the persistable, and also which SingleFieldIdentity, and the value of the key.
     * @param idType Type of SingleFieldIdentity
     * @param pcType Type of the persistable object
     * @param value The value for the identity (the Long, or Int, or ... etc).
     * @return The single field identity
     * @throws NucleusException if invalid input is received
     */
    Object getNewSingleFieldIdentity(Class idType, Class pcType, Object value);

    /**
     * Utility to create a new application-identity when you know the metadata for the target class,
     * and the toString() output of the identity.
     * @param clr ClassLoader resolver
     * @param acmd MetaData for the target class
     * @param value String form of the key
     * @return The new identity object
     * @throws NucleusException if invalid input is received
     */
    Object getNewApplicationIdentityObjectId(ClassLoaderResolver clr, AbstractClassMetaData acmd, String value);

    /**
     * Method to create a new application-identity for the passed object with the supplied MetaData.
     * @param pc The persistable object
     * @param cmd Its metadata
     * @return The new identity object
     */
    Object getNewApplicationIdentityObjectId(Object pc, AbstractClassMetaData cmd);

    /**
     * Method to return a new object identity for the specified class, and key (possibly toString() output).
     * @param cls Persistable class
     * @param key form of the object id
     * @return The object identity
     */
    Object getNewApplicationIdentityObjectId(Class cls, Object key);

    // ------------------------------ Persistence --------------------------------

    /**
     * Whether the API allows (re-)persistence of a deleted object.
     * @return Whether you can call persist on a deleted object
     */
    boolean allowPersistOfDeletedObject();

    /**
     * Whether the API allows deletion of a non-persistent object.
     * @return Whether you can call delete on an object not yet persisted
     */
    boolean allowDeleteOfNonPersistentObject();

    /**
     * Whether the API allows reading a field of a deleted object.
     * @return Whether you can read after deleting
     */
    boolean allowReadFieldOfDeletedObject();

    /**
     * Whether the API requires clearing of the fields of an object when it is deleted.
     * @return Whether to clear loaded fields at delete
     */
    boolean clearLoadedFlagsOnDeleteObject();

    /**
     * Method to return the default setting for cascading persist of a field
     * @return default persist cascade setting.
     */
    boolean getDefaultCascadePersistForField();

    /**
     * Method to return the default setting for cascading update of a field
     * @return default cascade update setting.
     */
    boolean getDefaultCascadeUpdateForField();

    /**
     * Method to return the default setting for cascading delete of a field
     * @return default cascade delete setting.
     */
    boolean getDefaultCascadeDeleteForField();

    // TODO Add defaultCascadeDetachForField but we don't currently use/care about this
    /**
     * Method to return the default setting for cascading refresh of a field
     * @return default cascade refresh setting.
     */
    boolean getDefaultCascadeRefreshForField();

    /**
     * Method to return the default DFG setting for a persistable field.
     * @return DFG default for persistable field
     */
    boolean getDefaultDFGForPersistableField();

    /**
     * Method to return a set of default properties for the factory (PMF, EMF, etc)
     * @return The default properties (if any) for this API
     */
    Map getDefaultFactoryProperties();

    // ------------------------------ ObjectProvider --------------------------------

    /**
     * Convenience method to convert the passed NucleusException into an exception for the API.
     * @param ne The NucleusException
     * @return The Api Exception
     */
    RuntimeException getApiExceptionForNucleusException(NucleusException ne);

    /**
     * Convenience method to return a user exception appropriate for this API when an unexpected
     * exception occurs.
     * @param msg The message
     * @param e The cause (if any)
     * @return The API exception
     */
    RuntimeException getUserExceptionForException(String msg, Exception e);

    /**
     * Convenience method to return a datastore exception appropriate for this API.
     * @param msg The message
     * @param e The cause (if any)
     * @return The exception
     */
    RuntimeException getDataStoreExceptionForException(String msg, Exception e);

    /**
     * Method to return a copy of the provided object, managed by the passed ObjectProvider, copying across
     * the specified fields.
     * @param pc Persistable object
     * @param op The Object Provider
     * @param fieldNumbers Fields to copy
     * @return The copy object
     */
    Object getCopyOfPersistableObject(Object pc, ObjectProvider op, int[] fieldNumbers);

    /**
     * Method to copy field values from the first persistable object across to the second persistable object.
     * Objects are assumed to be the same type.
     * @param pc First persistable object
     * @param fieldNumbers Fields to copy
     * @param pc2 Second persistable object (to have the values copied into it)
     */
    void copyFieldsFromPersistableObject(Object pc, int[] fieldNumbers, Object pc2);

    void copyPkFieldsToPersistableObjectFromId(Object pc, Object id, FieldManager fm);
}