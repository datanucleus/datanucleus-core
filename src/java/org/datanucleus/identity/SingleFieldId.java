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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.datanucleus.exceptions.NucleusUserException;

/**
 * This class is the abstract base class for all single field identity classes.
 * TODO All subclasses should implement the same rules as applies to all identity classes, namely String constructor taking output of toString(), etc.
 */
public abstract class SingleFieldId<T> implements Externalizable, Comparable
{
    /** The delimiter for String constructor. */
    protected static final String STRING_DELIMITER = ":";

    /** The class of the target object. TODO Would like to drop this to save space, but is needed to convert to JDO-specific ids. */
    transient private Class targetClass;

    /** The name of the class of the target object. */
    protected String targetClassName;

    protected int hashCode;

    protected SingleFieldId(Class pcClass)
    {
        if (pcClass == null)
        {
            throw new NullPointerException();
        }
        targetClass = pcClass;
        targetClassName = pcClass.getName();
    }

    public SingleFieldId()
    {
    }

    /**
     * Assert that the key is not null. Throw a NucleusUserException if the given key is null.
     * @param key The key
     */
    protected void assertKeyNotNull(Object key)
    {
        if (key == null)
        {
            throw new NucleusUserException("Cannot have an identity with null key");
        }
    }

    /**
     * Return the target class.
     * @return the target class.
     */
    public Class getTargetClass()
    {
        return targetClass;
    }

    public String getTargetClassName()
    {
        return targetClassName;
    }

    public abstract T getKeyAsObject();

    /**
     * Check the class and class name and object type. If restored from serialization, class will be null so compare class name.
     * @param obj the other object
     * @return true if the class or class name is the same
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        else if (obj == null || this.getClass() != obj.getClass())
        {
            return false;
        }
        else
        {
            SingleFieldId other = (SingleFieldId) obj;
            if (targetClass != null && targetClass == other.targetClass)
            {
                return true;
            }
            return targetClassName.equals(other.targetClassName);
        }
    }

    public int hashCode()
    {
        return hashCode;
    }

    /**
     * Write to the output stream.
     * @param out the stream
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(targetClassName);
        out.writeInt(hashCode);
    }

    /**
     * Read from the input stream. Creates a new instance with the target class name set
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        targetClass = null;
        targetClassName = (String) in.readObject();
        hashCode = in.readInt();
    }

    /**
     * Determine the ordering of identity objects. Only the class name is compared. This method is only used by subclasses.
     * @param o Other identity
     * @return The relative ordering between the objects
     */
    protected int compare(SingleFieldId o)
    {
        return targetClassName.compareTo(o.targetClassName);
    }
}