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
 * This class is for identity with a single int field.
 */
public class IntId extends SingleFieldId
{
    private int key;

    private void construct(int key)
    {
        this.key = key;
        hashCode = hashClassName() ^ key;
    }

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param key the key
     */
    public IntId(Class pcClass, int key)
    {
        super(pcClass);
        construct(key);
    }

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param key the key
     */
    public IntId(Class pcClass, Integer key)
    {
        super(pcClass);
        setKeyAsObject(key);
        construct(key.intValue());
    }

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param str the key
     */
    public IntId(Class pcClass, String str)
    {
        super(pcClass);
        assertKeyNotNull(str);
        construct(Integer.parseInt(str));
    }

    /**
     * Constructor only for Externalizable.
     */
    public IntId()
    {
    }

    /**
     * Return the key.
     * @return the key
     */
    public int getKey()
    {
        return key;
    }

    /**
     * Return the String form of the key.
     * @return the String form of the key
     */
    public String toString()
    {
        return Integer.toString(key);
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
            IntId other = (IntId) obj;
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
        if (o instanceof IntId)
        {
            IntId other = (IntId) o;
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
        return new Integer(key);
    }

    /**
     * Write this object. Write the superclass first.
     * @param out the output
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeInt(key);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        key = in.readInt();
    }
}