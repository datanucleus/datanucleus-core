/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.ExecutionContext;
import org.datanucleus.cache.CachedPC;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.store.FieldValues;

/**
 * Factory for ObjectProviders. Whenever we have a persistable object that needs to be managed and the values
 * of its fields accessed/updated we associate the object with an ObjectProvider. Should be implemented
 * by whichever type of ObjectProvider we want to instantiate.
 */
public interface ObjectProviderFactory
{
    void close();

    /**
     * Method to be called when an ObjectProvider is disconnected (finished with).
     * This facilitates its reuse within a pool.
     * @param op The ObjectProvider
     */
    void disconnectObjectProvider(ObjectProvider op);

    /**
     * Constructs an ObjectProvider to manage a hollow instance having the given object ID.
     * This constructor is used for creating new instances of existing persistent objects.
     * @param ec the ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param id the identity of the object.
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForHollow(ExecutionContext ec, Class<T> pcClass, Object id);

    /**
     * Constructs an ObjectProvider to manage a hollow instance having the given object ID.
     * The instance is already supplied.
     * @param ec ExecutionContext
     * @param id the identity of the object.
     * @param pc The object that is hollow that we are going to manage
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForHollowPreConstructed(ExecutionContext ec, Object id, T pc);

    /**
     * Constructs an ObjectProvider to manage a recently populated hollow instance having the
     * given object ID and the given field values. This constructor is used for
     * creating new instances of persistent objects obtained e.g. via a Query or backed by a view.
     * @param ec ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param id the identity of the object.
     * @param fv the initial field values of the object.
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForHollow(ExecutionContext ec, Class<T> pcClass, Object id, FieldValues fv);

    /**
     * Constructs an ObjectProvider to manage the specified persistent instance having the given object ID.
     * @param ec the execution context controlling this state manager.
     * @param id the identity of the object.
     * @param pc The object that is persistent that we are going to manage
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForPersistentClean(ExecutionContext ec, Object id, T pc);

    /**
     * Constructs an ObjectProvider to manage a hollow (or pclean) instance having the given FieldValues.
     * This constructor is used for creating new instances of existing persistent objects using application identity.
     * @param ec ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param fv the initial field values of the object.
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     * @deprecated Use newForHollowPopulated instead
     */
    <T> ObjectProvider<T> newForHollowPopulatedAppId(ExecutionContext ec, Class<T> pcClass, final FieldValues fv);

    /**
     * Constructs an ObjectProvider to manage a persistable instance that will
     * be EMBEDDED/SERIALISED into another persistable object. The instance will not be
     * assigned an identity in the process since it is a SCO.
     * @param ec ExecutionContext
     * @param pc The persistable to manage (see copyPc also)
     * @param copyPc Whether the SM should manage a copy of the passed PC or that one
     * @param ownerOP Owner ObjectProvider
     * @param ownerFieldNumber Field number in owner object where this is stored
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForEmbedded(ExecutionContext ec, T pc, boolean copyPc, ObjectProvider ownerOP, int ownerFieldNumber);

    /**
     * Constructs an ObjectProvider for an object of the specified type, creating the PC object to hold the values
     * where this object will be EMBEDDED/SERIALISED into another persistable object. The instance will not be
     * assigned an identity in the process since it is a SCO.
     * @param ec ExecutionContext
     * @param cmd Meta-data for the class that this is an instance of.
     * @param ownerOP Owner ObjectProvider
     * @param ownerFieldNumber Field number in owner object where this is stored
     * @return The ObjectProvider
     */
    ObjectProvider newForEmbedded(ExecutionContext ec, AbstractClassMetaData cmd, ObjectProvider ownerOP, int ownerFieldNumber);

    /**
     * Constructs an ObjectProvider to manage a transient instance that is  becoming newly persistent.
     * A new object ID for the instance is obtained from the store manager and the object is inserted
     * in the data store. This constructor is used for assigning state managers to existing
     * instances that are transitioning to a persistent state.
     * @param ec ExecutionContext
     * @param pc the instance being make persistent.
     * @param fv Any changes to make before inserting
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForPersistentNew(ExecutionContext ec, T pc, FieldValues fv);

    /**
     * Constructs an ObjectProvider to manage a transactional-transient instance.
     * A new object ID for the instance is obtained from the store manager and the object is inserted in 
     * the data store. This constructor is used for assigning state managers to transient
     * instances that are transitioning to a transient clean state.
     * @param ec ExecutionContext
     * @param pc the instance being make persistent.
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForTransactionalTransient(ExecutionContext ec, T pc);

    /**
     * Constructor for creating ObjectProvider instances to manage persistable objects in detached state.
     * @param ec ExecutionContext
     * @param pc the detached object
     * @param id the identity of the object.
     * @param version the detached version
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForDetached(ExecutionContext ec, T pc, Object id, Object version);

    /**
     * Constructor for creating ObjectProvider instances to manage persistable objects that are not persistent yet
     * are about to be deleted. Consequently the initial lifecycle state will be P_NEW, but will soon
     * move to P_NEW_DELETED.
     * @param ec Execution Context
     * @param pc the object being deleted from persistence
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForPNewToBeDeleted(ExecutionContext ec, T pc);

    /**
     * Constructor to create an ObjectProvider for an object taken from the L2 cache with the specified id.
     * Makes a copy of the cached object, assigns a ObjectProvider to it, and copies across the fields that 
     * were loaded when cached.
     * @param ec ExecutionContext
     * @param id Id to assign to the persistable object
     * @param cachedPC CachedPC object from the L2 cache
     * @param <T> Type of the persistable class
     * @return The ObjectProvider
     */
    <T> ObjectProvider<T> newForCachedPC(ExecutionContext ec, Object id, CachedPC<T> cachedPC);
}