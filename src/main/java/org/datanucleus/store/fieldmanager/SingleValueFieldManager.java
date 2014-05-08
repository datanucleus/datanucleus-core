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
 * Field manager for single field. Stores only a single field value, unlike the
 * StateFieldManager which stores various types.
 * <p>
 * The field number arguments to each method are ignored.
 */
public class SingleValueFieldManager implements FieldManager
{
    private Object fieldValue = null;

    /**
     * Mutator for boolean field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeBooleanField(int fieldNumber, boolean value)
    {
        fieldValue = value ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Accessor for boolean field.
     * @param fieldNumber Number of field 
     * @return Boolean value
     **/
    public boolean fetchBooleanField(int fieldNumber)
    {
        return ((Boolean)fieldValue).booleanValue();
    }

    /**
     * Mutator for char field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeCharField(int fieldNumber, char value)
    {
        fieldValue = Character.valueOf(value);
    }

    /**
     * Accessor for int field.
     * @param fieldNumber Number of field 
     * @return int value
     **/
    public char fetchCharField(int fieldNumber)
    {
        return ((Character)fieldValue).charValue();
    }

    /**
     * Mutator for byte field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeByteField(int fieldNumber, byte value)
    {
        fieldValue = Byte.valueOf(value);
    }

    /**
     * Accessor for byte field.
     * @param fieldNumber Number of field 
     * @return byte value
     **/
    public byte fetchByteField(int fieldNumber)
    {
        return ((Byte)fieldValue).byteValue();
    }

    /**
     * Mutator for boolean field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeShortField(int fieldNumber, short value)
    {
        fieldValue = Short.valueOf(value);
    }

    /**
     * Accessor for short field.
     * @param fieldNumber Number of field 
     * @return short value
     **/
    public short fetchShortField(int fieldNumber)
    {
        return ((Short)fieldValue).shortValue();
    }

    /**
     * Mutator for int field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeIntField(int fieldNumber, int value)
    {
        fieldValue = Integer.valueOf(value);
    }

    /**
     * Accessor for int field.
     * @param fieldNumber Number of field 
     * @return int value
     **/
    public int fetchIntField(int fieldNumber)
    {
        return ((Integer)fieldValue).intValue();
    }

    /**
     * Mutator for long field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeLongField(int fieldNumber, long value)
    {
        fieldValue = Long.valueOf(value);
    }

    /**
     * Accessor for long field.
     * @param fieldNumber Number of field 
     * @return long value
     **/
    public long fetchLongField(int fieldNumber)
    {
        return ((Long)fieldValue).longValue();
    }

    /**
     * Mutator for float field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeFloatField(int fieldNumber, float value)
    {
        fieldValue = Float.valueOf(value);
    }

    /**
     * Accessor for float field.
     * @param fieldNumber Number of field 
     * @return float value
     **/
    public float fetchFloatField(int fieldNumber)
    {
        return ((Float)fieldValue).floatValue();
    }

    /**
     * Mutator for double field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeDoubleField(int fieldNumber, double value)
    {
        fieldValue = Double.valueOf(value);
    }

    /**
     * Accessor for double field.
     * @param fieldNumber Number of field 
     * @return double value
     **/
    public double fetchDoubleField(int fieldNumber)
    {
        return ((Double)fieldValue).doubleValue();
    }

    /**
     * Mutator for String field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeStringField(int fieldNumber, String value)
    {
        fieldValue = value;
    }

    /**
     * Accessor for String field.
     * @param fieldNumber Number of field 
     * @return String value
     **/
    public String fetchStringField(int fieldNumber)
    {
        return (String)fieldValue;
    }

    /**
     * Mutator for Object field.
     * @param fieldNumber Number of field 
     * @param value Value
     **/
    public void storeObjectField(int fieldNumber, Object value)
    {
        fieldValue = value;
    }

    /**
     * Accessor for Object field.
     * @param fieldNumber Number of field 
     * @return Object value
     **/
    public Object fetchObjectField(int fieldNumber)
    {
        return fieldValue;
    }
}
