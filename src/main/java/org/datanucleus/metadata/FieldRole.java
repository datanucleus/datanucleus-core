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
public enum FieldRole
{
    /** No role defined for this field. */
    ROLE_NONE,

    /** Field is the owner of a relation. */
    ROLE_OWNER,

    /** Field is a reference to another (persistable) object. */
    ROLE_FIELD,

    /** Field is to be treated as the element of a collection. */
    ROLE_COLLECTION_ELEMENT,

    /** Field is to be treated as the element of an array. */
    ROLE_ARRAY_ELEMENT,

    /** Field is to be treated as the key of a map. */
    ROLE_MAP_KEY,

    /** Field is to be treated as the value of a map. */
    ROLE_MAP_VALUE,

    /** Field is to be treated as an ordering or index in a List. */
    ROLE_INDEX,

    /** Field is to be treated as the relation to a persistable (via join table). */
    ROLE_PERSISTABLE_RELATION;
}