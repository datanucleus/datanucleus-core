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

import javax.jdo.JDOFatalInternalException;

/**
 * This class is for identity with a single String field.
 */
public class StringId extends SingleFieldId
{
    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param key the key
     */
    public StringId(Class pcClass, String key)
    {
        super(pcClass);
        setKeyAsObject(key);
        hashCode = hashClassName() ^ key.hashCode();
    }

    /**
     * Constructor only for Externalizable.
     */
    public StringId()
    {
    }

    /**
     * Return the key.
     * @return the key
     */
    public String getKey()
    {
        return (String) keyAsObject;
    }

    /**
     * Create the key as an Object.
     * @return the key as an Object;
     */
    protected Object createKeyAsObject()
    {
        throw new JDOFatalInternalException("StringId.createKeyAsObject should never be called. Report this");
    }

    /**
     * Return the String form of the key.
     * @return the String form of the key
     */
    public String toString()
    {
        return (String) keyAsObject;
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
            StringId other = (StringId) obj;
            return keyAsObject.equals(other.keyAsObject);
        }
    }

    /**
     * Determine the ordering of identity objects.
     * @param o Other identity
     * @return The relative ordering between the objects
     */
    public int compareTo(Object o)
    {
        if (o instanceof StringId)
        {
            StringId other = (StringId) o;
            int result = super.compare(other);
            if (result == 0)
            {
                return ((String) keyAsObject).compareTo((String) other.keyAsObject);
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
     * Write this object. Write the superclass first.
     * @param out the output
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeObject(keyAsObject);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        keyAsObject = in.readObject();
    }
}