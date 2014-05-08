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
package org.datanucleus.store.fieldmanager;

/**
 * Interface providing methods for supplying field values to a persistable object.
 * Based on the JDO interface PersistenceCapable.ObjectIdFieldSupplier.
 */
public interface FieldSupplier
{
    /**
     * Fetch a boolean field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    boolean fetchBooleanField(int fieldNumber);

    /**
     * Fetch a byte field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    byte fetchByteField(int fieldNumber);

    /**
     * Fetch a char field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    char fetchCharField(int fieldNumber);

    /**
     * Fetch a double field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    double fetchDoubleField(int fieldNumber);

    /**
     * Fetch a float field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    float fetchFloatField(int fieldNumber);

    /**
     * Fetch an int field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    int fetchIntField(int fieldNumber);

    /**
     * Fetch a long field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    long fetchLongField(int fieldNumber);

    /**
     * Fetch a short field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    short fetchShortField(int fieldNumber);

    /**
     * Fetch a string field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    String fetchStringField(int fieldNumber);

    /**
     * Fetch an object field at the specified field number, returning it.
     * @param fieldNumber Number of the field
     * @return The value
     */
    Object fetchObjectField(int fieldNumber);
}