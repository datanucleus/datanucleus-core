/**********************************************************************
Copyright (c) 2022 Andy Jefferson and others. All rights reserved.
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
 * Definition of a component of a member.
 * Allows definition of part of a member where a related object is persisted.
 * This is of particular use when dealing with a map and a persistent object is in the key or the value.
 */
public enum MemberComponent
{
    COLLECTION_ELEMENT, // Only place in a collection
    ARRAY_ELEMENT, // Only place in an array
    MAP_KEY,
    MAP_VALUE
}