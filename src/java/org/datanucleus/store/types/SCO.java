/**********************************************************************
Copyright (c) 2003 Mike Martin and others. All rights reserved.
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
2003 Andy Jefferson - commented
2007 Andy Jefferson - added detachCopy, attachCopy, initialise, getValue
    ...
**********************************************************************/
package org.datanucleus.store.types;

import org.datanucleus.state.FetchPlanState;

/**
 * Representation of a wrapper/proxy for a mutable SCO type supported.
 * An implementation of this interface must have a constructor with the arguments 
 * <i>{@link org.datanucleus.state.ObjectProvider} ownerOP</i>, <i>String fieldName</i>.
 * The constructor must be capable of taking nulls for these arguments to create a non-managed wrapper
 * which effectively just acts like an unwrapped object.
 */
public interface SCO
{
    /**
     * Method to initialise the SCO for use using an existing object of the same or compatible type.
     * @param value the object from which to copy the value.
     * @param forInsert Whether the object needs inserting in the datastore with this value
     * @param forUpdate Whether the object needs updating in the datastore with this value
     * @throws ClassCastException Thrown if the given object is not of a type that's compatible with this
     *                            second-class wrapper object.
     */
    void initialise(Object value, boolean forInsert, boolean forUpdate) throws ClassCastException;

    /**
     * Method to initialise the SCO for use.
     * This can be utilised to perform any eager loading of information from the datastore.
     */
    void initialise();

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
    Object getValue();

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
    Object detachCopy(FetchPlanState state);

    /**
     * Method to return an attached copy of this object.
     * Attaches all components of this object that are also persistable.
     * @param value The object value from the detached instance
     */
    void attachCopy(Object value);
}