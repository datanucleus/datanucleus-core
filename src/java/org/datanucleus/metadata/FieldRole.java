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
package org.datanucleus.metadata;

/**
 * Series of roles that fields can be performing in the (ORM) mapping process.
 * This is typically for use by mapped datastores, but relates to MetaData definitions too, so is
 * stored in org.datanucleus.metadata.
 */
public class FieldRole
{
    /** User defined name specified for the field, for use only with identifier creation. */
    // TODO Move this since refers to a different concept
    public static final int ROLE_CUSTOM = -1;

    /** No role defined for this field. */
    public static final int ROLE_NONE = 0;

    /** Field is the owner of a relation. */
    public static final int ROLE_OWNER = 1;

    /** Field is a reference to another object. */
    public static final int ROLE_FIELD = 2;

    /** Field is to be treated as the element of a collection. */
    public static final int ROLE_COLLECTION_ELEMENT = 3;

    /** Field is to be treated as the element of an array. */
    public static final int ROLE_ARRAY_ELEMENT = 4;

    /** Field is to be treated as the key of a map. */
    public static final int ROLE_MAP_KEY = 5;

    /** Field is to be treated as the value of a map. */
    public static final int ROLE_MAP_VALUE = 6;

    /** Field is to be treated as an ordering or index in a List. */
    public static final int ROLE_INDEX = 7;

    /** Field is to be treated as the relation to a persistable (via join table). */
    public static final int ROLE_PERSISTABLE_RELATION = 8;
}