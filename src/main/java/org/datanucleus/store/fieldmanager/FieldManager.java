/**********************************************************************
Copyright (c) 2002 TJDO and others. All rights reserved.
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
2007 Andy Jefferson - removed dependence on JDO so can be used with JPA
    ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

/**
 * Provide methods to fetch from/to a persistable object to/from the ObjectProvider/DataStore.
 */
public interface FieldManager
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

    /**
     * Method to store a boolean field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeBooleanField(int fieldNumber, boolean value);

    /**
     * Method to store a byte field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeByteField(int fieldNumber, byte value);

    /**
     * Method to store a char field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeCharField(int fieldNumber, char value);

    /**
     * Method to store a double field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeDoubleField(int fieldNumber, double value);

    /**
     * Method to store a float field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeFloatField(int fieldNumber, float value);

    /**
     * Method to store an int field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeIntField(int fieldNumber, int value);

    /**
     * Method to store a long field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeLongField(int fieldNumber, long value);

    /**
     * Method to store a short field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeShortField(int fieldNumber, short value);

    /**
     * Method to store a string field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeStringField(int fieldNumber, String value);

    /**
     * Method to store an object field value in the object at the specified field position.
     * @param fieldNumber Number of the field
     * @param value value to store
     */
    void storeObjectField(int fieldNumber, Object value);
}