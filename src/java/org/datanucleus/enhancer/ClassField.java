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
package org.datanucleus.enhancer;

import org.datanucleus.util.Localiser;

/**
 * Representation of a field that an enhanced class requires.
 */
public class ClassField
{
    /** Localisation of messages */
    protected static Localiser LOCALISER=Localiser.getInstance(
        "org.datanucleus.Localisation", ClassEnhancer.class.getClassLoader());

    /** The parent enhancer. */
    protected ClassEnhancer enhancer;

    /** Name of the field. */
    protected String fieldName;

    /** Access flags for the field (public, protected etc). */
    protected int access;

    /** Type for the field */
    protected Object type;

    /** Initial value for the field. */
    protected Object initialValue;

    /**
     * Constructor
     * @param enhancer Class Enhancer
     * @param name Name of the field
     * @param access Access for the field (PUBLIC, PROTECTED etc)
     * @param type Type of the field
     */
    public ClassField(ClassEnhancer enhancer, String name, int access, Object type)
    {
        this.enhancer = enhancer;
        this.fieldName = name;
        this.access = access;
        this.type = type;
    }

    /**
     * Constructor
     * @param enhancer Class Enhancer
     * @param name Name of the field
     * @param access Access for the field (PUBLIC, PROTECTED etc)
     * @param type Type of the field
     * @param value Initial value
     */
    public ClassField(ClassEnhancer enhancer, String name, int access, Object type, Object value)
    {
        this.enhancer = enhancer;
        this.fieldName = name;
        this.access = access;
        this.type = type;
        this.initialValue = value;
    }

    /**
     * Accessor for the field name
     * @return Name of the field
     */
    public String getName()
    {
        return fieldName;
    }

    /**
     * Accessor for the access
     * @return Access type for the field
     */
    public int getAccess()
    {
        return access;
    }

    /**
     * Accessor for the type
     * @return Type of the field
     */
    public Object getType()
    {
        return type;
    }

    /**
     * Accessor for the value
     * @return Initial value of the field
     */
    public Object getInitialValue()
    {
        return initialValue;
    }

    /**
     * Return hash code of this instance.
     * @return hash code of this instance
     */
    public int hashCode()
    {
        return fieldName.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param o the reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    public boolean equals(Object o)
    {
        if (o instanceof ClassField)
        {
            ClassField cf = (ClassField)o;
            if (cf.fieldName.equals(fieldName))
            {
                return type == cf.type;
            }
        }
        return false;
    }
}