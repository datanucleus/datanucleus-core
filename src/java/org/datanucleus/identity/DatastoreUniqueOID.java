/**********************************************************************
Copyright (c) 2007 Xuan Baldauf and others. All rights reserved.
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
2007 Andy Jefferson - generalised for all datastores
    ...
**********************************************************************/
package org.datanucleus.identity;

/**
 * Identity for use with datastore-identity where the datastore provides a unique "identity" key per object
 * and hence doesn't need the class name. The behaviour of this class is governed by JDO spec 5.4.3.
 * Main benefit over OIDImpl is size.
 */
public class DatastoreUniqueOID implements java.io.Serializable, OID, Comparable
{
    // JDO spec 5.4.3 says: all serializable fields of ObjectID classes are required to be public.

    /** The key value. */
    public final long key;

    /**
    * Creates an OID with no value. Required by the JDO spec.
    */
    public DatastoreUniqueOID()
    {
        this.key = -1;
    }

    /**
     * Constructor taking the long form of the key.
     * @param key The key
     */
    public DatastoreUniqueOID(long key)
    {
        this.key = key;
    }

    /**
     * Constructs an OID from its string representation that is consistent with the output of toString().
     * @param str the string representation of an OID
     * @exception IllegalArgumentException if the given string representation is not valid.
     * @see #toString
     */
    public DatastoreUniqueOID(String str)
    throws IllegalArgumentException
    {
        this.key = Long.parseLong(str);
    }

    /**
     * Accessor for the key value.
     * @return The key value
     */
    public Object getKeyValue()
    {
        return Long.valueOf(key);
    }

    /**
     * Convenience accessor for the long form of the key.
     * @return long primitive form of the key
     */
    public long getKey()
    {
        return key;
    }

    /**
     * Accessor for the persistable class name.
     * @return persistable class name
     */
    public String getPcClass()
    {
        // We do not need a class name.
        throw new UnsupportedOperationException();
    }

    /**
     * Equality operator.
     * @param obj Object to compare against
     * @return Whether they are equal
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj.getClass().equals(this.getClass())))
        {
            return false;
        }
        return key == ((DatastoreUniqueOID)obj).key;
    }

    /**
     * Comparator method.
     * @param o The object to compare against
     * @return The comparison result
     */ 
    public int compareTo(Object o)
    {
        if (o instanceof DatastoreUniqueOID)
        {
            DatastoreUniqueOID c = (DatastoreUniqueOID)o;
            return (int)(this.key - c.key);
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
        // Assume that we wont overflow the int range
        return (int) key; 
    }

    /**
     * Creates a String representation of the datastore identity, formed from the key value. 
     * This will be something like <pre>3254</pre>
     * @return The String form of the identity
     */
    public String toString()
    {
        return Long.toString(key);
    }
}