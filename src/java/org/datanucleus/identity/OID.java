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
 * Please refer to the JDO2 specification 5.4.3 for precise requirements of such a class. These include
 * <ul>
 * <li>Has to implement Serializable</li>
 * <li>Serializable fields have to be public</li>
 * <li>Has to have a constructor taking a String (the same String that toString() returns)</li>
 * </ul>
 */
public interface OID
{
    /**
     * Provides the OID in a form that can be used by the database as a key.
     * @return The key value
     */
    public abstract Object getKeyValue();

    /**
     * Accessor for the PC class name 
     * @return the PC Class
     */
    public abstract String getPcClass();

    /**
     * Equality operator.
     * @param obj Object to compare against
     * @return Whether they are equal
     */
    public abstract boolean equals(Object obj);

    /**
     * Accessor for the hashcode
     * @return Hashcode for this object
     */
    public abstract int hashCode();

    /**
     * Returns the string representation of the OID.
     * The string representation should contain enough information to be usable as input to a String constructor
     * to create the OID.
     * @return the string representation of the OID.
     */
    public abstract String toString();
}