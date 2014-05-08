/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
2006 Kundan Varma - some annotation reading classes
    ...
**********************************************************************/
package org.datanucleus.metadata.annotations;

import java.util.HashMap;

/**
 * Wrapper for an annotation and its various properties etc.
 * This could represent, for example, "javax.persistence.Entity"
 * and have a map with one key "name".
 */
public class AnnotationObject
{
    /** Name of the annotation object (e.g javax.persistence.Entity) */
    String name;

    /** Map of properties for this annotation */
    HashMap<String, Object> nameValueMap;

    /**
     * Constructor.
     * @param name Class name of the annotation object
     * @param map Map of the annotation properties
     */
    public AnnotationObject(String name, HashMap<String, Object> map)
    {
        this.name = name;
        this.nameValueMap = map;
    }

    /**
     * Accessor for the annotation class name
     * @return Annotation class name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Accessor for the annotation properties map
     * @return Annotation properties map
     */
    public HashMap<String, Object> getNameValueMap()
    {
        return nameValueMap;
    }
}