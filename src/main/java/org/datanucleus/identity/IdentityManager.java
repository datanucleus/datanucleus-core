/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.identity;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractClassMetaData;

/**
 * Manager for identity creation etc.
 */
public interface IdentityManager
{
    Class getDatastoreIdClass();

    /**
     * Accessor for the current identity string translator to use (if any).
     * @return Identity string translator instance (or null if persistence property not set)
     */
    IdentityStringTranslator getIdentityStringTranslator();

    /**
     * Accessor for the current identity key translator to use (if any).
     * @return Identity key translator instance (or null if persistence property not set)
     */
    IdentityKeyTranslator getIdentityKeyTranslator();

    /**
     * Method to return a datastore identity, representing the persistable object with specified class name and key value.
     * @param className The class name for the persistable
     * @param value The key value for the persistable
     * @return The datastore id
     */
    DatastoreId getDatastoreId(String className, Object value);

    /**
     * Method to return a datastore-unique datastore identity, with the specified value.
     * @param value The long value that is unique across the datastore.
     * @return The datastore id
     */
    DatastoreId getDatastoreId(long value);

    /**
     * Method to return a datastore identity, for the specified string which comes from the output of toString().
     * @param oidString The toString() value
     * @return The datastore id
     */
    DatastoreId getDatastoreId(String oidString);

    /**
     * Method to return a single-field identity, for the persistable type specified, and for the idType of SingleFieldId.
     * @param idType Type of the id
     * @param pcType Type of the Persistable
     * @param key The value for the identity (the Long, or Int, or ... etc).
     * @return Single field identity
     */
    SingleFieldId getSingleFieldId(Class idType, Class pcType, Object key);

    /**
     * Utility to create a new application identity when you know the metadata for the target class,
     * and the toString() output of the identity.
     * @param clr ClassLoader resolver
     * @param acmd MetaData for the target class
     * @param keyToString String form of the key
     * @return The identity
     */
    Object getApplicationId(ClassLoaderResolver clr, AbstractClassMetaData acmd, String keyToString);

    /**
     * Method to create a new object identity for the passed object with the supplied MetaData.
     * Only applies to application-identity cases.
     * @param pc The persistable object
     * @param cmd Its metadata
     * @return The new identity object
     */
    Object getApplicationId(Object pc, AbstractClassMetaData cmd);

    /**
     * Method to return a new object identity for the specified class, and key (possibly toString() output).
     * @param cls Persistable class
     * @param key form of the object id
     * @return The object identity
     */
    Object getApplicationId(Class cls, Object key);
}