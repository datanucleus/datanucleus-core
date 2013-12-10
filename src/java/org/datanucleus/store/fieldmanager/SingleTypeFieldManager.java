/**********************************************************************
Copyright (c) 2002 Kelly Grizzle (TJDO) and others. All rights reserved.
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
2002 Mike Martin - unknown changes
2003 Andy Jefferson - commented
    ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

/**
 * A simple field manager that stores/fetches a single field value per type
 * in memory.
 * <p>
 * Calls to the store methods save the value in a local field; calls to the
 * fetch methods return the previously stored value for that type, or the
 * "empty" default value if nothing has been stored.
 * <p>
 * The field number arguments to each method are ignored.
 */
public class SingleTypeFieldManager implements FieldManager
{
    private boolean booleanValue = false;
    private char charValue = 0;
    private byte byteValue = 0;
    private short shortValue = 0;
    private int intValue = 0;
    private long longValue = 0;
    private float floatValue = 0;
    private double doubleValue = 0;
    private String stringValue = null;
    private Object objectValue = null;

    /**
     * Default Constructor.
     **/
    public SingleTypeFieldManager()
    {
    	//default constructor
    }

    /**
     * Constructor.
     * @param booleanValue Boolean 
     **/
    public SingleTypeFieldManager(boolean booleanValue)
    {
        this.booleanValue = booleanValue;
    }

    /**
     * Constructor.
     * @param charValue char 
     **/
    public SingleTypeFieldManager(char charValue)
    {
        this.charValue = charValue;
    }

    /**
     * Constructor.
     * @param byteValue byte 
     **/
    public SingleTypeFieldManager(byte byteValue)
    {
        this.byteValue = byteValue;
    }

    /**
     * Constructor.
     * @param shortValue short 
     **/
    public SingleTypeFieldManager(short shortValue)
    {
        this.shortValue = shortValue;
    }

    /**
     * Constructor.
     * @param intValue int 
     **/
    public SingleTypeFieldManager(int intValue)
    {
        this.intValue = intValue;
    }

    /**
     * Constructor.
     * @param longValue Long 
     **/
    public SingleTypeFieldManager(long longValue)
    {
        this.longValue = longValue;
    }

    /**
     * Constructor.
     * @param floatValue Float 
     **/
    public SingleTypeFieldManager(float floatValue)
    {
        this.floatValue = floatValue;
    }

    /**
     * Constructor.
     * @param doubleValue Double 
     **/
    public SingleTypeFieldManager(double doubleValue)
    {
        this.doubleValue = doubleValue;
    }

    /**
     * Constructor.
     * @param stringValue String 
     **/
    public SingleTypeFieldManager(String stringValue)
    {
        this.stringValue = stringValue;
    }

    /**
     * Constructor.
     * @param objectValue Object 
     **/
    public SingleTypeFieldManager(Object objectValue)
    {
        this.objectValue = objectValue;
    }

    /**
     * Mutator for boolean field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeBooleanField(int fieldNum, boolean value)
    {
        booleanValue = value;
    }

    /**
     * Accessor for boolean field.
     * @param fieldNum Number of field 
     * @return Boolean value
     **/
    public boolean fetchBooleanField(int fieldNum)
    {
        return booleanValue;
    }

    /**
     * Mutator for char field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeCharField(int fieldNum, char value)
    {
        charValue = value;
    }

    /**
     * Accessor for char field.
     * @param fieldNum Number of field 
     * @return Char value
     **/
    public char fetchCharField(int fieldNum)
    {
        return charValue;
    }

    /**
     * Mutator for byte field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeByteField(int fieldNum, byte value)
    {
        byteValue = value;
    }

    /**
     * Accessor for byte field.
     * @param fieldNum Number of field 
     * @return Byte value
     **/
    public byte fetchByteField(int fieldNum)
    {
        return byteValue;
    }

    /**
     * Mutator for short field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeShortField(int fieldNum, short value)
    {
        shortValue = value;
    }

    /**
     * Accessor for short field.
     * @param fieldNum Number of field 
     * @return Short value
     **/
    public short fetchShortField(int fieldNum)
    {
        return shortValue;
    }

    /**
     * Mutator for int field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeIntField(int fieldNum, int value)
    {
        intValue = value;
    }

    /**
     * Accessor for int field.
     * @param fieldNum Number of field 
     * @return Int value
     **/
    public int fetchIntField(int fieldNum)
    {
        return intValue;
    }

    /**
     * Mutator for long field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeLongField(int fieldNum, long value)
    {
        longValue = value;
    }

    /**
     * Accessor for long field.
     * @param fieldNum Number of field 
     * @return Long value
     **/
    public long fetchLongField(int fieldNum)
    {
        return longValue;
    }

    /**
     * Mutator for float field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeFloatField(int fieldNum, float value)
    {
        floatValue = value;
    }

    /**
     * Accessor for float field.
     * @param fieldNum Number of field 
     * @return Float value
     **/
    public float fetchFloatField(int fieldNum)
    {
        return floatValue;
    }

    /**
     * Mutator for double field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeDoubleField(int fieldNum, double value)
    {
        doubleValue = value;
    }

    /**
     * Accessor for double field.
     * @param fieldNum Number of field 
     * @return Double value
     **/
    public double fetchDoubleField(int fieldNum)
    {
        return doubleValue;
    }

    /**
     * Mutator for String field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeStringField(int fieldNum, String value)
    {
        stringValue = value;
    }

    /**
     * Accessor for string field.
     * @param fieldNum Number of field 
     * @return String value
     **/
    public String fetchStringField(int fieldNum)
    {
        return stringValue;
    }

    /**
     * Mutator for Object field.
     * @param fieldNum Number of field 
     * @param value Value
     **/
    public void storeObjectField(int fieldNum, Object value)
    {
        objectValue = value;
    }

    /**
     * Accessor for object field.
     * @param fieldNum Number of field 
     * @return Object value
     **/
    public Object fetchObjectField(int fieldNum)
    {
       return objectValue;
    }
}