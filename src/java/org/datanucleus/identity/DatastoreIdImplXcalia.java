/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.util.Localiser;

/**
 * Object identifier, typically used for datastore identity.
 * The behaviour of this class is governed by JDO spec 5.4.3.
 * Utilises a String form of the style "mydomain.MyClass:3258".
 * This is a form similar to Xcalia. Note : Xcalia also allows "{alias}:3258" but this isn't catered for here
 */
public class DatastoreIdImplXcalia implements java.io.Serializable, DatastoreId, Comparable
{
    protected static final transient Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    protected static final transient String STRING_DELIMITER = ":";

    // JDO spec 5.4.3 says: all serializable fields of ObjectID classes are required to be public.

    public final Object keyAsObject;

    public final String targetClassName;

    /** pre-created toString to improve performance **/ 
    public final String toString;

    /** pre-created hasCode to improve performance **/ 
    public final int hashCode;

    public DatastoreIdImplXcalia()
    {
        keyAsObject = null;
        targetClassName = null; 
        toString = null;
        hashCode = -1;
    }

    public DatastoreIdImplXcalia(String pcClass, Object object)
    {
        this.targetClassName = pcClass;
        this.keyAsObject = object;

        StringBuilder s = new StringBuilder();
        s.append(this.targetClassName);
        s.append(STRING_DELIMITER);
        s.append(this.keyAsObject.toString());
        toString = s.toString();
        hashCode = toString.hashCode();        
    }

    /**
     * Constructs an OID from its string representation that is consistent with the output of toString().
     * @param str the string representation of an OID
     * @exception IllegalArgumentException if the given string representation is not valid.
     * @see #toString
     */
    public DatastoreIdImplXcalia(String str)
    throws IllegalArgumentException
    {
        if (str.length() < 2)
        {
            throw new IllegalArgumentException(LOCALISER.msg("038000",str));
        }

        int separatorPosition = str.indexOf(STRING_DELIMITER);
        this.targetClassName = str.substring(0, separatorPosition);
        String oidStr = str.substring(separatorPosition+1);
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
        if (!(obj.getClass().getName().equals(DatastoreIdImplXcalia.class.getName())))
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
        if (o instanceof DatastoreIdImplXcalia)
        {
            DatastoreIdImplXcalia c = (DatastoreIdImplXcalia)o;
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
     * <pre>mydomain.MyClass:3254</pre>
     * @return The String form of the identity
     */
    public String toString()
    {
        return toString;
    }
}