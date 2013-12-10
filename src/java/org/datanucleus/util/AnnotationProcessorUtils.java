/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Series of method to aid in the writing of annotation processors.
 */
public class AnnotationProcessorUtils
{
    private static Set<String> LIST_CLASSNAMES = null;
    private static Set<String> SET_CLASSNAMES = null;
    private static Set<String> MAP_CLASSNAMES = null;
    private static Set<String> COLLECTION_CLASSNAMES = null;
    static
    {
        LIST_CLASSNAMES = new HashSet<String>();
        LIST_CLASSNAMES.add("java.util.List");
        LIST_CLASSNAMES.add("java.util.ArrayList");
        LIST_CLASSNAMES.add("java.util.AbstractList");
        LIST_CLASSNAMES.add("java.util.Stack");
        LIST_CLASSNAMES.add("java.util.Vector");
        LIST_CLASSNAMES.add("java.util.LinkedList");
        SET_CLASSNAMES = new HashSet<String>();
        SET_CLASSNAMES.add("java.util.Set");
        SET_CLASSNAMES.add("java.util.HashSet");
        SET_CLASSNAMES.add("java.util.AbstractSet");
        SET_CLASSNAMES.add("java.util.LinkedHashSet");
        SET_CLASSNAMES.add("java.util.TreeSet");
        SET_CLASSNAMES.add("java.util.SortedSet");
        MAP_CLASSNAMES = new HashSet<String>();
        MAP_CLASSNAMES.add("java.util.Map");
        MAP_CLASSNAMES.add("java.util.HashMap");
        MAP_CLASSNAMES.add("java.util.AbstractMap");
        MAP_CLASSNAMES.add("java.util.Hashtable");
        MAP_CLASSNAMES.add("java.util.LinkedHashMap");
        SET_CLASSNAMES.add("java.util.TreeMap");
        MAP_CLASSNAMES.add("java.util.SortedMap");
        MAP_CLASSNAMES.add("java.util.Properties");
        COLLECTION_CLASSNAMES = new HashSet<String>();
        COLLECTION_CLASSNAMES.add("java.util.Collection");
        COLLECTION_CLASSNAMES.add("java.util.AbstractCollection");
        COLLECTION_CLASSNAMES.add("java.util.Queue");
        COLLECTION_CLASSNAMES.add("java.util.PriorityQueue");
    }

    public static enum TypeCategory
    {
        COLLECTION("CollectionAttribute"),
        SET("SetAttribute"),
        LIST("ListAttribute"),
        MAP("MapAttribute"),
        ATTRIBUTE("SingularAttribute");

        private String type;

        private TypeCategory(String type)
        {
            this.type = type;
        }

        public String getTypeName()
        {
            return type;
        }
    }

    /**
     * Method to return the JPA2 type category for a type.
     * @param typeName The type name (e.g java.lang.String, java.util.Collection)
     * @return The type category
     */
    public static TypeCategory getTypeCategoryForTypeMirror(String typeName)
    {
        if (COLLECTION_CLASSNAMES.contains(typeName))
        {
            return TypeCategory.COLLECTION;
        }
        else if (SET_CLASSNAMES.contains(typeName))
        {
            return TypeCategory.SET;
        }
        else if (LIST_CLASSNAMES.contains(typeName))
        {
            return TypeCategory.LIST;
        }
        else if (MAP_CLASSNAMES.contains(typeName))
        {
            return TypeCategory.MAP;
        }
        return TypeCategory.ATTRIBUTE;
    }

    /**
     * Convenience accessor for all field members of the supplied type element.
     * @param el The type element
     * @return The field members
     */
    public static List<? extends Element> getFieldMembers(TypeElement el)
    {
        List<? extends Element> members = el.getEnclosedElements();
        List<Element> fieldMembers = new ArrayList<Element>();
        Iterator<? extends Element> memberIter = members.iterator();
        while (memberIter.hasNext())
        {
            Element member = memberIter.next();
            if (member.getKind() == ElementKind.FIELD)
            {
                fieldMembers.add(member);
            }
        }
        return fieldMembers;
    }

    /**
     * Convenience accessor for all property members of the supplied type element.
     * @param el The type element
     * @return The property members
     */
    public static List<? extends Element> getPropertyMembers(TypeElement el)
    {
        List<? extends Element> members = el.getEnclosedElements();
        List<Element> propertyMembers = new ArrayList<Element>();
        Iterator<? extends Element> memberIter = members.iterator();
        while (memberIter.hasNext())
        {
            Element member = memberIter.next();
            if (member.getKind() == ElementKind.METHOD)
            {
                ExecutableElement method = (ExecutableElement)member;
                if (isJavaBeanGetter(method) || isJavaBeanSetter(method))
                {
                    propertyMembers.add(member);
                }
            }
        }
        return propertyMembers;
    }

    /**
     * Convenience method to return if the provided method is a java bean getter.
     * @param method The method
     * @return Whether it is a java bean getter
     */
    public static boolean isJavaBeanGetter(ExecutableElement method)
    {
        String methodName = method.getSimpleName().toString();
        if (method.getKind() == ElementKind.METHOD && method.getParameters().isEmpty())
        {
            if (returnsBoolean(method) && methodName.startsWith("is"))
            {
                return true;
            }
            else if (methodName.startsWith("get") && !returnsVoid(method))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Accessor for the member name for a field or Java bean getter/setter.
     * If this is a field then just returns the field name, and if "getXxx"/"setXxx" then returns "xxx"
     * @param el The element
     * @return The member name
     */
    public static String getMemberName(Element el)
    {
        if (el.getKind() == ElementKind.FIELD)
        {
            return el.toString();
        }
        else if (el.getKind() == ElementKind.METHOD)
        {
            ExecutableElement method = (ExecutableElement) el;
            if (isJavaBeanGetter(method) || isJavaBeanSetter(method))
            {
                String name = method.getSimpleName().toString();
                if (name.startsWith("is"))
                {
                    name = name.substring(2);
                }
                else
                {
                    name = name.substring(3);
                }
                return Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
        }
        return null;
    }

    /**
     * Convenience method to return if the provided method is a java bean setter.
     * @param method The method
     * @return Whether it is a java bean setter
     */
    public static boolean isJavaBeanSetter(ExecutableElement method)
    {
        String methodName = method.getSimpleName().toString();
        return method.getKind() == ElementKind.METHOD && methodName.startsWith("set") &&
            method.getParameters().isEmpty() && !returnsVoid(method);
    }

    /**
     * Convenience method to return if the provided element represents a method (otherwise a field).
     * @param elem The element
     * @return Whether it represents a method
     */
    public static boolean isMethod(Element elem)
    {
        return elem != null && ExecutableElement.class.isInstance(elem) && elem.getKind() == ElementKind.METHOD;
    }

    /**
     * Accessor for the declared type of this element.
     * If this is a field then returns the declared type of the field.
     * If this is a java bean getter then returns the return type.
     * @param elem The element
     * @return The declared type
     */
    public static TypeMirror getDeclaredType(Element elem)
    {
        if (elem.getKind() == ElementKind.FIELD)
        {
            return elem.asType();
        }
        else if (elem.getKind() == ElementKind.METHOD)
        {
            return ((ExecutableElement) elem).getReturnType();
        }
        else
        {
            throw new IllegalArgumentException("Unable to get type for " + elem);
        }
    }

    /**
     * Accessor for the value for an annotation attribute.
     * @param elem The element
     * @param annotCls Annotation class
     * @param attribute The attribute we're interested in
     * @return The value
     */
    public static Object getValueForAnnotationAttribute(Element elem, Class annotCls, String attribute)
    {
        List<? extends AnnotationMirror> anns = elem.getAnnotationMirrors();
        Iterator<? extends AnnotationMirror> annIter = anns.iterator();
        while (annIter.hasNext())
        {
            AnnotationMirror ann = annIter.next();
            if (ann.getAnnotationType().toString().equals(annotCls.getName()))
            {
                Map<? extends ExecutableElement, ? extends AnnotationValue> values = ann.getElementValues();
                for (Map.Entry entry : values.entrySet())
                {
                    ExecutableElement ex = (ExecutableElement) entry.getKey();
                    if (ex.getSimpleName().toString().equals(attribute))
                    {
                        return ((AnnotationValue)entry.getValue()).getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Convenience method returning if the provided method returns void.
     * @param method The method
     * @return Whether it returns void
     */
    public static boolean returnsVoid(ExecutableElement method)
    {
        TypeMirror type = method.getReturnType();
        return (type != null && type.getKind() == TypeKind.VOID);
    }

    /**
     * Convenience method returning if the provided method returns boolean.
     * @param method The method
     * @return Whether it returns boolean
     */
    public static boolean returnsBoolean(ExecutableElement method)
    {
        TypeMirror type = method.getReturnType();
        return (type != null && (type.getKind() == TypeKind.BOOLEAN  || "java.lang.Boolean".equals(type.toString())));
    }

    /**
     * Convenience method to return if the provided type is a primitive.
     * @param type The type
     * @return Whether it is a primitive
     */
    public static boolean typeIsPrimitive(TypeMirror type)
    {
        TypeKind kind = type.getKind();
        return kind == TypeKind.BOOLEAN || kind == TypeKind.BYTE || kind == TypeKind.CHAR || 
            kind == TypeKind.DOUBLE || kind == TypeKind.FLOAT || kind == TypeKind.INT || 
            kind == TypeKind.LONG || kind == TypeKind.SHORT;
    }

    /**
     * Method to return the declared type name of the provided TypeMirror.
     * @param processingEnv Processing environment
     * @param type The type (mirror)
     * @param box Whether to (auto)box this type
     * @return The type name (e.g "java.lang.String")
     */
    public static String getDeclaredTypeName(ProcessingEnvironment processingEnv, TypeMirror type, boolean box)
    {
        if (type == null || type.getKind() == TypeKind.NULL || type.getKind() == TypeKind.WILDCARD)
        {
            return "java.lang.Object";
        }
        if (type.getKind() == TypeKind.ARRAY)
        {
            TypeMirror comp = ((ArrayType)type).getComponentType();
            return getDeclaredTypeName(processingEnv, comp, false);
        }

        if (box && AnnotationProcessorUtils.typeIsPrimitive(type))
        {
            type = processingEnv.getTypeUtils().boxedClass((PrimitiveType)type).asType();
        }
        if (AnnotationProcessorUtils.typeIsPrimitive(type))
        {
            return ((PrimitiveType)type).toString();
        }
        return processingEnv.getTypeUtils().asElement(type).toString();
    }
}