/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.metadata.annotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.datanucleus.util.ClassUtils;

/**
 * Wrapper for a field or a method (property) that is annotated.
 */
public class Member
{
    /** The Field/Method name. */
    String name;

    /** The type. */
    Class type;

    /** Modifiers for the field / method. **/
    int modifiers;

    /** generic type. **/
    Type genericType;

    /** Whether this represents a property. **/
    boolean property;

    /**
     * Constructor.
     * @param field The field
     */
    public Member(Field field)
    {
        this.name = field.getName();
        this.type = field.getType();
        this.modifiers = field.getModifiers();
        this.genericType = field.getGenericType();
        this.property = false;
    }

    /**
     * Constructor.
     * @param method The method
     */
    public Member(Method method)
    {
        this.name = ClassUtils.getFieldNameForJavaBeanGetter(method.getName());
        this.type = method.getReturnType();
        this.modifiers = method.getModifiers();
        this.genericType = method.getGenericReturnType();
        this.property = true;
    }

    /**
     * Field name or Method name (without get/set/is prefix)
     * @return Name of the field or property.
     */
    public String getName()
    {
        return name;
    }

    /**
     * If this class is a field or method (property).
     * @return true if it is a method (property). false if it is a field.
     */
    public boolean isProperty()
    {
        return property;
    }    

    /**
     * Accessor to the field or method return type.
     * @return Type of the field/property
     */
    public Class getType()
    {
        return type;
    }

    /**
     * Accessor to the field / method modifiers
     * @return modifiers for the field/method.
     */
    public int getModifiers()
    {
        return modifiers;
    }

    /**
     * Accessor to the generic type
     * @return Generic type
     */
    public Type getGenericType()
    {
        return genericType;
    }
}