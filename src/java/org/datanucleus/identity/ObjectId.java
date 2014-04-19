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
import javax.jdo.JDOUserException;
import javax.jdo.spi.JDOImplHelper;

/**
 * This class is for identity with a single Object type field.
 */
public class ObjectId extends SingleFieldId
{
    /** The delimiter for String constructor. */
    private static final String STRING_DELIMITER = ":";

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param param the key
     */
    public ObjectId(Class pcClass, Object param)
    {
        super(pcClass);
        assertKeyNotNull(param);
        String paramString = null;
        String keyString = null;
        String className = null;
        if (param instanceof String)
        {
            // The paramString is of the form "<className>:<keyString>"
            paramString = (String) param;
            if (paramString.length() < 3)
            {
                // TODO Remove this JDO class usage
                throw new JDOUserException("ObjectId constructor from String was expecting a longer string than " + paramString);
            }
            int indexOfDelimiter = paramString.indexOf(STRING_DELIMITER);
            if (indexOfDelimiter < 0)
            {
                // TODO Remove this JDO class usage
                throw new JDOUserException("ObjectId constructor from String was expecting a delimiter of " + STRING_DELIMITER + " but not present!");
            }
            keyString = paramString.substring(indexOfDelimiter + 1);
            className = paramString.substring(0, indexOfDelimiter);
            // TODO Remove this JDO class usage; change to DN variant
            keyAsObject = JDOImplHelper.construct(className, keyString);
        }
        else
        {
            keyAsObject = param;
        }
        hashCode = hashClassName() ^ keyAsObject.hashCode();
    }

    /**
     * Constructor only for Externalizable.
     */
    public ObjectId()
    {
    }

    /**
     * Return the key.
     * @return the key
     */
    public Object getKey()
    {
        return keyAsObject;
    }

    /**
     * Create the key as an Object.
     * @return the key as an Object;
     */
    protected Object createKeyAsObject()
    {
        // TODO Remove this JDO class usage
        throw new JDOFatalInternalException("ObjectId.createKeyAsObject should never be called. Report this");
    }

    /**
     * Return the String form of the object id. The class of the object id is written as the first part of the
     * result so that the class can be reconstructed later. Then the toString of the key instance is appended.
     * During construction, this process is reversed. The class is extracted from the first part of the
     * String, and the String constructor of the key is used to construct the key itself.
     * @return the String form of the key
     */
    @Override
    public String toString()
    {
        return keyAsObject.getClass().getName() + STRING_DELIMITER + keyAsObject.toString();
    }

    /**
     * Determine if the other object represents the same object id.
     * @param obj the other object
     * @return true if both objects represent the same object id
     */
    @Override
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
            ObjectId other = (ObjectId) obj;
            return keyAsObject.equals(other.keyAsObject);
        }
    }

    /**
     * Provide the hash code for this instance. The hash code is the hash code of the contained key.
     * @return the hash code
     */
    @Override
    public int hashCode()
    {
        return keyAsObject.hashCode();
    }

    /**
     * Determine the ordering of identity objects.
     * @param o Other identity
     * @return The relative ordering between the objects
     */
    public int compareTo(Object o)
    {
        if (o instanceof ObjectId)
        {
            ObjectId other = (ObjectId) o;
            int result = super.compare(other);
            if (result == 0)
            {
                if (other.keyAsObject instanceof Comparable && keyAsObject instanceof Comparable)
                {
                    return ((Comparable) keyAsObject).compareTo(other.keyAsObject);
                }
                else
                {
                    throw new ClassCastException("The key class (" + keyAsObject.getClass().getName() + ") does not implement Comparable");
                }
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
    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeObject(keyAsObject);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        keyAsObject = in.readObject();
    }
}