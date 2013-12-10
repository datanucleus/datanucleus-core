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
 * Interface providing methods for consuming field values from a persistable object.
 * Based on the JDO interface PersistenceCapable.ObjectIdFieldConsumer.
 */
public interface FieldConsumer
{
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