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
public class OIDImpl implements java.io.Serializable, OID, Comparable
{
    /** Localiser for messages. */
    protected static final transient Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Separator to use between fields. */
    private transient static final String oidSeparator = "[OID]";

    // JDO spec 5.4.3 says: all serializable fields of ObjectID classes are required to be public.

    /** The key value. */
    public final Object oid;

    /** The persistable class name */
    public final String pcClass;

    /** pre-created toString to improve performance **/ 
    public final String toString;

    /** pre-created hasCode to improve performance **/ 
    public final int hashCode;

    /**
    * Creates an OID with no value. Required by the JDO spec
    */
    public OIDImpl()
    {
        oid = null;
        pcClass = null; 
        toString = null;
        hashCode = -1;
    }

    /**
     * Create a string datastore identity.
     * @param pcClass The persistable class that this represents
     * @param object The value
     */
    public OIDImpl(String pcClass, Object object)
    {
        this.pcClass = pcClass;
        this.oid = object;

        StringBuilder s = new StringBuilder();
        s.append(this.oid.toString());
        s.append(oidSeparator);
        s.append(this.pcClass);
        toString = s.toString();
        hashCode = toString.hashCode();        
    }

    /**
     * Constructs an OID from its string representation that is consistent with the output of toString().
     * @param str the string representation of an OID
     * @exception IllegalArgumentException if the given string representation is not valid.
     * @see #toString
     */
    public OIDImpl(String str)
    throws IllegalArgumentException
    {
        if (str.length() < 2)
        {
            throw new IllegalArgumentException(LOCALISER.msg("038000", str));
        }
        else if (str.indexOf(oidSeparator) < 0)
        {
            throw new IllegalArgumentException(LOCALISER.msg("038000", str));
        }

        int start = 0;
        int end = str.indexOf(oidSeparator, start);
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
        oid = oidValue;

        start = end + oidSeparator.length();
        this.pcClass = str.substring(start, str.length());
        
        toString = str;
        hashCode = toString.hashCode();
    }

    /**
     * Accessor for the key value.
     * @return The key value
     */
    public Object getKeyValue()
    {
        return oid;
    }

    /**
     * Accessor for the persistable class name.
     * @return persistable class name
     */
    public String getTargetClassName()
    {
        return pcClass;
    }

    /**
     * Equality operator.
     * @param obj Object to compare against
     * @return Whether they are equal
     */
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
        if (!((OID)obj).toString().equals(toString))
        {
            // Hashcodes are the same but the values aren't
            return false;
        }
        return true;
    }

    /**
     * Comparator method.
     * @param o The object to compare against
     * @return The comparison result
     */ 
    public int compareTo(Object o)
    {
        if (o instanceof OIDImpl)
        {
            OIDImpl c = (OIDImpl)o;
            return this.toString.compareTo(c.toString);
        }
        else if (o == null)
        {
            throw new ClassCastException("object is null");
        }
        throw new ClassCastException(this.getClass().getName() + " != " + o.getClass().getName());
    }

    /**
     * Accessor for the hashcode
     * @return Hashcode for this object
     */
    public int hashCode()
    {
        return hashCode;
    }

    /**
     * Creates a String representation of the datastore identity, formed from the PC class name
     * and the key value. This will be something like
     * <pre>3254[OID]mydomain.MyClass</pre>
     * @return The String form of the identity
     */
    public String toString()
    {
        return toString;
    }
}