/**********************************************************************
Copyright (c) 2002 Kelly Grizzle (TJDO) and others. All rights reserved.
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
2003 Erik Bengtson - Refactored OID
2003 Andy Jefferson - fixed OID(String)
2003 Andy Jefferson - coding standards
2004 Andy Jefferson - fixes to allow full use of long or String OIDs
2005 Erik Bengtson - removed oidType
    ...
**********************************************************************/
package org.datanucleus.identity;

import org.datanucleus.ClassNameConstants;
import org.datanucleus.util.Localiser;

/**
 * An object identifier, typically used for datastore identity.
 * The behaviour of this class is governed by JDO spec 5.4.3.
 * Utilises a String form of the style "3258[OID]mydomain.MyClass".
 */
public class DatastoreIdImpl implements java.io.Serializable, DatastoreId, Comparable
{
    private static final long serialVersionUID = -1841930829956222995L;

    protected static final transient String STRING_DELIMITER = "[OID]";

    // JDO spec 5.4.3 says: all serializable fields of ID classes are required to be public.

    public final Object keyAsObject;

    public final String targetClassName;

    public final String toString;

    public final int hashCode;

    public DatastoreIdImpl(String pcClass, Object key)
    {
        this.targetClassName = pcClass;
        this.keyAsObject = key;

        StringBuilder s = new StringBuilder();
        s.append(this.keyAsObject.toString());
        s.append(STRING_DELIMITER);
        s.append(this.targetClassName);
        toString = s.toString();
        hashCode = toString.hashCode();        
    }

    /**
     * Constructs a DatastoreId from its string representation that is consistent with the output of toString().
     * @param str the string representation of a DatastoreId
     * @exception IllegalArgumentException if the given string representation is not valid.
     * @see #toString
     */
    public DatastoreIdImpl(String str)
    throws IllegalArgumentException
    {
        if (str.length() < 2)
        {
            throw new IllegalArgumentException(Localiser.msg("002003", str));
        }
        else if (str.indexOf(STRING_DELIMITER) < 0)
        {
            throw new IllegalArgumentException(Localiser.msg("002003", str));
        }

        int start = 0;
        int end = str.indexOf(STRING_DELIMITER, start);
        String oidStr = str.substring(start, end);
        Object oidValue = null;
        try
        {
            // Use Long if possible, else String
            oidValue = Long.valueOf(oidStr);
        }
        catch (NumberFormatException nfe)
        {
            oidValue = oidStr;
        }
        keyAsObject = oidValue;

        start = end + STRING_DELIMITER.length();
        this.targetClassName = str.substring(start, str.length());
        
        toString = str;
        hashCode = toString.hashCode();
    }

    public Object getKeyAsObject()
    {
        return keyAsObject;
    }

    public String getTargetClassName()
    {
        return targetClassName;
    }

    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }
        if (!(obj.getClass().getName().equals(ClassNameConstants.IDENTITY_OID_IMPL)))
        {
            return false;
        }
        if (hashCode() != obj.hashCode())
        {
            return false;
        }
        if (!((DatastoreId)obj).toString().equals(toString))
        {
            // Hashcodes are the same but the values aren't
            return false;
        }
        return true;
    }

    public int compareTo(Object o)
    {
        if (o instanceof DatastoreIdImpl)
        {
            DatastoreIdImpl c = (DatastoreIdImpl)o;
            return this.toString.compareTo(c.toString);
        }
        else if (o == null)
        {
            throw new ClassCastException("object is null");
        }
        throw new ClassCastException(this.getClass().getName() + " != " + o.getClass().getName());
    }

    public int hashCode()
    {
        return hashCode;
    }

    /**
     * Creates a String representation of the datastore identity, formed from the target class name and the key value. This will be something like
     * <pre>3254[OID]mydomain.MyClass</pre>
     * @return The String form of the identity
     */
    public String toString()
    {
        return toString;
    }
}