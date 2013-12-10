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
package org.datanucleus.enhancer.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** 
 * Single-Field identity with a char/Character field.
 */
public class CharFieldPK extends SingleFieldPK
{
    private char key;

    /**
     * Constructor with class and key.
     * @param pcClass the target class
     * @param key the key
     */
    public CharFieldPK(Class pcClass, char key)
    {
        super(pcClass);
        this.key = key;
        hashCode = hashClassName() ^ this.key;
    }

    /**
     * Constructor with class and key.
     * @param pcClass the target class
     * @param key the key
     */
    public CharFieldPK(Class pcClass, Character key)
    {
        super(pcClass);
        setKeyAsObject(key);
        this.key = key.charValue();
        hashCode = hashClassName() ^ this.key;
    }

    /**
     * Constructor with class and key. The String must have exactly one character.
     * @param pcClass the target class
     * @param str the key
     */
    public CharFieldPK(Class pcClass, String str)
    {
        super(pcClass);
        assertKeyNotNull(str);
        if (str.length() != 1)
        {
            throw new IllegalArgumentException("CharIdentity should have a value of length 1");
        }

        this.key = str.charAt(0);
        hashCode = hashClassName() ^ this.key;
    }

    /**
     * Constructor only for Externalizable.
     */
    public CharFieldPK()
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
            CharFieldPK other = (CharFieldPK) obj;
            return key == other.key;
        }
    }

    public int compareTo(Object o)
    {
        if (o instanceof CharFieldPK)
        {
            CharFieldPK other = (CharFieldPK)o;
            return key - other.key;
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
     * @since 2.0
     */
    protected Object createKeyAsObject()
    {
        return Character.valueOf(key);
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