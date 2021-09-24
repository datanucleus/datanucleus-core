/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types;

import org.datanucleus.FetchPlanState;

/**
 * Representation of a wrapper/proxy for a mutable SCO type (e.g Date, Collection, Map).
 * An implementation of this interface must have a constructor with the arguments 
 * <i>{@link org.datanucleus.state.DNStateManager} ownerOP</i>, <i>AbstractMemberMetaData mmd</i>.
 * The constructor must be capable of taking nulls for these arguments to create a non-managed wrapper
 * which effectively just acts like an unwrapped object.
 * @param <T> The type of the field/property
 */
public interface SCO<T>
{
    /**
     * Method to initialise the SCO for use with the provided initial value.
     * This is used, for example, when retrieving the field from the datastore and setting it in the persistable object.
     * @param value the object from which to copy the value.
     */
    void initialise(T value);

    /**
     * Method to initialise the SCO for use, and allowing the SCO to be loaded from the datastore (when we have a backing store).
     * This can be utilised to perform any eager loading of information from the datastore.
     */
    void initialise();

    /**
     * Method to initialise the SCO for use, where replacing an old value with a new value such as when calling a setter field 
     * passing in a new value. Note that oldValue is marked as Object since for cases where the member type is Collection the
     * newValue could be, for example, ArrayList, and the oldValue of type Collection (representing null).
     * @param newValue New value (to wrap)
     * @param oldValue Old value (to use in deciding what needs deleting etc)
     */
    void initialise(T newValue, Object oldValue);

    /**
     * Accessor for the field name.
     * @return field name, or null if no longer associated with an object 
     */
    String getFieldName();

    /**
     * Accessor for the owner object of the SCO instance. Is typically a persistable object.
     * @return owner object, or null if no longer associated with an object
     */
    Object getOwner();

    /**
     * Nullifies references to the owner Object and field. Thereafter the SCO is no longer associated 
     * with the owner and thus should not issue any request to the datastore.
     */
    void unsetOwner();

    /**
     * Method to return the value of the unwrapped type.
     * @return The value that is wrapped by this object.
     */
    T getValue();

    /**
     * Mutable second class objects are required to provide a public clone() method so that copying
     * of persistable objects can take place. This mustn't throw a {@link CloneNotSupportedException}.
     * @return A clone of the object
     */
    Object clone();

    /**
     * Method to return a detached copy of this object.
     * Detaches all components of this object that are also persistable.
     * @param state State of the detachment process
     * @return The detached copy
     */
    T detachCopy(FetchPlanState state);

    /**
     * Method to return an attached copy of this object.
     * Attaches all components of this object that are also persistable.
     * @param value The object value from the detached instance
     */
    void attachCopy(T value);
}