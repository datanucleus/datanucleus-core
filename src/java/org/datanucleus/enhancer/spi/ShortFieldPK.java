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
 * Single-Field identity with a short/Short field.
 */
public class ShortFieldPK extends SingleFieldPK
{
    private short key;

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param key the key
     */
    public ShortFieldPK(Class pcClass, short key)
    {
        super(pcClass);
        this.key = key;
        hashCode = hashClassName() ^ this.key;
    }

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param key the key
     */
    public ShortFieldPK(Class pcClass, Short key)
    {
        super(pcClass);
        setKeyAsObject(key);
        this.key = key.shortValue();
        hashCode = hashClassName() ^ this.key;
    }

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param str the key
     */
    public ShortFieldPK(Class pcClass, String str)
    {
        super(pcClass);
        assertKeyNotNull(str);
        this.key = Short.parseShort(str);
        hashCode = hashClassName() ^ this.key;
    }

    /**
     * Constructor only for Externalizable.
     */
    public ShortFieldPK()
    {
    }

    /**
     * Return the key.
     * @return the key
     */
    public short getKey()
    {
        return key;
    }

    /**
     * Return the String form of the key.
     * @return the String form of the key
     */
    public String toString()
    {
        return Short.toString(key);
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
            ShortFieldPK other = (ShortFieldPK) obj;
            return key == other.key;
        }
    }

    public int compareTo(Object o)
    {
        if (o instanceof ShortFieldPK)
        {
            ShortFieldPK other = (ShortFieldPK)o;
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
     */
    protected Object createKeyAsObject()
    {
        return Short.valueOf(key);
    }

    /**
     * Write this object. Write the superclass first.
     * @param out the output
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeShort(key);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        key = in.readShort();
    }
}