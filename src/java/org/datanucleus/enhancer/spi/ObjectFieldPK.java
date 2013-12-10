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

import java.util.Currency;
import java.util.Date;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.I18nUtils;

/**
 * Single-Field identity with an Object field.
 * Supports java.util.Date, java.util.Locale, java.util.Currency.
 */
public class ObjectFieldPK extends SingleFieldPK
{
    /** The delimiter for String constructor. */
    private static final String STRING_DELIMITER = ":";

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param param the key
     */
    public ObjectFieldPK(Class pcClass, Object param)
    {
        super(pcClass);
        assertKeyNotNull(param);
        String paramString = null;
        String keyString = null;
        String className = null;
        if (param instanceof String)
        {
            /* The paramString is of the form "<className>:<keyString>" */
            paramString = (String) param;
            if (paramString.length() < 3)
            {
                throw new NucleusUserException("ObjectIdentity should be passed a String greater than 3 characters in length");
            }
            int indexOfDelimiter = paramString.indexOf(STRING_DELIMITER);
            if (indexOfDelimiter < 0)
            {
                throw new NucleusUserException("ObjectIdentity should be passed a String with a delimiter present");
            }
            keyString = paramString.substring(indexOfDelimiter + 1);
            className = paramString.substring(0, indexOfDelimiter);

            // Create the object form of the key
            if (className.equals("java.util.Date"))
            {
                keyAsObject = new Date(Long.parseLong(keyString));
            }
            else if (className.equals("java.util.Locale"))
            {
                keyAsObject = I18nUtils.getLocaleFromString(keyString);
            }
            else if (className.equals("java.util.Currency"))
            {
                keyAsObject = Currency.getInstance(keyString);
            }
            else
            {
                throw new NucleusUserException("Class for ObjectIdentity " + className + " is not supported as a PK type");
            }
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
    public ObjectFieldPK()
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
     * Return the String form of the object id. 
     * The class of the object id is written as the first part of the result so that the class can be 
     * reconstructed later. Then the toString of the key instance is appended. During construction, this 
     * process is reversed. The class is extracted from the first part of the String, and the String
     * constructor of the key is used to construct the key itself.
     * @return the String form of the key
     */
    public String toString()
    {
        return keyAsObject.getClass().getName() + STRING_DELIMITER + keyAsObject.toString();
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
            ObjectFieldPK other = (ObjectFieldPK) obj;
            return keyAsObject.equals(other.keyAsObject);
        }
    }

    public int compareTo(Object o)
    {
        if (o instanceof ObjectFieldPK)
        {
            ObjectFieldPK other = (ObjectFieldPK)o;
            return ((String)this.keyAsObject).compareTo((String)other.keyAsObject);
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