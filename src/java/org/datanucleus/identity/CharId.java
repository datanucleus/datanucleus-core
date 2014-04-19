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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.jdo.spi.I18NHelper;

/**
 * This class is for identity with a single character field.
 */
public class CharId extends SingleFieldId
{
    /**
     * The Internationalization message helper. TODO Remove this
     */
    private static I18NHelper msg = I18NHelper.getInstance("javax.jdo.Bundle"); // NOI18N

    private char key;

    private void construct(char key)
    {
        this.key = key;
        hashCode = hashClassName() ^ key;
    }

    /**
     * Constructor with class and key.
     * @param pcClass the target class
     * @param key the key
     */
    public CharId(Class pcClass, char key)
    {
        super(pcClass);
        construct(key);
    }

    /**
     * Constructor with class and key.
     * @param pcClass the target class
     * @param key the key
     */
    public CharId(Class pcClass, Character key)
    {
        super(pcClass);
        setKeyAsObject(key);
        construct(key.charValue());
    }

    /**
     * Constructor with class and key. The String must have exactly one character.
     * @param pcClass the target class
     * @param str the key
     */
    public CharId(Class pcClass, String str)
    {
        super(pcClass);
        assertKeyNotNull(str);
        if (str.length() != 1)
            throw new IllegalArgumentException(msg.msg("EXC_StringWrongLength")); // NOI18N
        construct(str.charAt(0));
    }

    /**
     * Constructor only for Externalizable.
     */
    public CharId()
    {
    }

    /**
     * Return the key.
     * @return the key
     */
    public char getKey()
    {
        return key;
    }

    /**
     * Return the String form of the key.
     * @return the String form of the key
     */
    public String toString()
    {
        return String.valueOf(key);
    }

    /**
     * Determine if the other object represents the same object id.
     * @param obj the other object
     * @return true if both objects represent the same object id
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        else if (!super.equals(obj))
        {
            return false;
        }
        else
        {
            CharId other = (CharId) obj;
            return key == other.key;
        }
    }

    /**
     * Determine the ordering of identity objects.
     * @param o Other identity
     * @return The relative ordering between the objects
     */
    public int compareTo(Object o)
    {
        if (o instanceof CharId)
        {
            CharId other = (CharId) o;
            int result = super.compare(other);
            if (result == 0)
            {
                return (key - other.key);
            }
            else
            {
                return result;
            }
        }
        else if (o == null)
        {
            throw new ClassCastException("object is null");
        }
        throw new ClassCastException(this.getClass().getName() + " != " + o.getClass().getName());
    }

    /**
     * Create the key as an Object.
     * @return the key as an Object
     */
    protected Object createKeyAsObject()
    {
        return new Character(key);
    }

    /**
     * Write this object. Write the superclass first.
     * @param out the output
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeChar(key);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        key = in.readChar();
    }
}
