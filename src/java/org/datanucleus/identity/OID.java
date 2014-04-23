/**********************************************************************
 Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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

/**
 * Interface for a datastore-identity class to implement.
 * Please refer to the JDO specification 5.4.3 for precise requirements of such a class. These include
 * <ul>
 * <li>Has to implement Serializable</li>
 * <li>Serializable fields have to be public</li>
 * <li>Has to have a constructor taking a String (the same String that toString() returns)</li>
 * </ul>
 */
public interface OID
{
    /**
     * Provides the identity in a form that can be used by the database as a key.
     * @return The key value
     */
    Object getKeyAsObject();

    /**
     * Accessor for the target class name for the persistable object this represents.
     * @return the class name of the persistable
     */
    String getTargetClassName();

    boolean equals(Object obj);

    int hashCode();

    /**
     * Returns the string representation of the identity.
     * The string representation should contain enough information to be usable as input to a String constructor to create the identity.
     * @return the string representation of the identity.
     */
    String toString();
}