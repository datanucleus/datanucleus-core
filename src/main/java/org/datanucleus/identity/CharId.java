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

/**
 * This class is for identity with a single character field.
 */
public class CharId extends SingleFieldId<Character, CharId>
{
    private char key;

    public CharId(Class pcClass, char key)
    {
        super(pcClass);
        this.key = key;
        this.hashCode = targetClassName.hashCode() ^ key;
    }

    public CharId(Class pcClass, Character key)
    {
        this(pcClass, key != null ? key.charValue() : 0);
        assertKeyNotNull(key);
    }

    public CharId(Class pcClass, String str)
    {
        this(pcClass, str.charAt(0));
        assertKeyNotNull(str);
        if (str.length() != 1)
        {
            throw new IllegalArgumentException("Cannot have a char as id when string is of length " + str.length());
        }
    }

    public CharId()
    {
    }

    public char getKey()
    {
        return key;
    }

    public Character getKeyAsObject()
    {
        return Character.valueOf(key);
    }

    /**
     * Return the String form of the key.
     * @return the String form of the key
     */
    public String toString()
    {
        return String.valueOf(key);
    }

    @Override
    protected boolean keyEquals(CharId obj)
    {
        return key == obj.key;
    }

    public int compareTo(CharId other)
    {
        int result = super.compare(other);
        if (result == 0)
        {
            return key - other.key;
        }
        return result;
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
