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

import org.datanucleus.enhancer.EnhancementHelper;
import org.datanucleus.exceptions.NucleusUserException;

/**
 * This class is for identity with a single Object type field.
 */
public class ObjectId extends SingleFieldId
{
    private Object key;

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
        String keyClassName = null;
        if (param instanceof String)
        {
            // The paramString is of the form "<keyClassName>:<keyString>"
            paramString = (String) param;
            if (paramString.length() < 3)
            {
                throw new NucleusUserException("ObjectId constructor from String was expecting a longer string than " + paramString);
            }
            int indexOfDelimiter = paramString.indexOf(STRING_DELIMITER);
            if (indexOfDelimiter < 0)
            {
                throw new NucleusUserException("ObjectId constructor from String was expecting a delimiter of " + STRING_DELIMITER + " but not present!");
            }
            keyString = paramString.substring(indexOfDelimiter + 1);
            keyClassName = paramString.substring(0, indexOfDelimiter);
            key = EnhancementHelper.construct(keyClassName, keyString);
        }
        else
        {
            key = param;
        }
        hashCode = targetClassName.hashCode() ^ key.hashCode();
    }

    public ObjectId()
    {
    }

    public Object getKey()
    {
        return key;
    }

    public Object getKeyAsObject()
    {
        return key;
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
        return key.getClass().getName() + STRING_DELIMITER + key.toString();
    }

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
            return key.equals(((ObjectId)obj).key);
        }
    }

    public int compareTo(Object o)
    {
        if (o instanceof ObjectId)
        {
            ObjectId other = (ObjectId) o;
            int result = super.compare(other);
            if (result == 0)
            {
                if (other.key instanceof Comparable && key instanceof Comparable)
                {
                    return ((Comparable) key).compareTo(other.key);
                }
                throw new ClassCastException("The key class (" + key.getClass().getName() + ") does not implement Comparable");
            }
            return result;
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
        out.writeObject(key);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        key = in.readObject();
    }
}