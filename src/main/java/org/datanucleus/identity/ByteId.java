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
 * This class is for identity with a single byte field.
 */
public class ByteId extends SingleFieldId<Byte, ByteId>
{
    private byte key;

    public ByteId(Class<?> pcClass, byte key)
    {
        super(pcClass);
        this.key = key;
        this.hashCode = targetClassName.hashCode() ^ key;
    }

    public ByteId(Class<?> pcClass, Byte key)
    {
        this(pcClass, key != null ? key.byteValue() : 0);
        assertKeyNotNull(key);
    }

    public ByteId(Class<?> pcClass, String str)
    {
        this(pcClass, Byte.parseByte(str));
        assertKeyNotNull(str);
    }

    public ByteId()
    {
    }

    public byte getKey()
    {
        return key;
    }

    public Byte getKeyAsObject()
    {
        return Byte.valueOf(key);
    }

    /**
     * Return the String version of the key.
     * @return the key.
     */
    public String toString()
    {
        return Byte.toString(key);
    }

    @Override
    protected boolean keyEquals(ByteId obj)
    {
        return key == obj.key;
    }

    public int compareTo(ByteId other)
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
        out.writeByte(key);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        key = in.readByte();
    }
}