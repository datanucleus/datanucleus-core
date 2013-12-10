/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.schema.naming;

/**
 * Enum defining the types of columns.
 */
public enum ColumnType
{
    /** Normal column. */
    COLUMN,
    /** Column for (surrogate) datastore id. */
    DATASTOREID_COLUMN,
    /** Column for version. */
    VERSION_COLUMN,
    /** Column for discriminator. */
    DISCRIMINATOR_COLUMN,
    /** Column for multitenancy. */
    MULTITENANCY_COLUMN,
    /** Column for index position of a list/array. */
    INDEX_COLUMN,
    /** FK column (either 1-1, or 1-N in element, or 1-N join table to element. */
    FK_COLUMN,
    /** 1-N join table column back to owner. */
    JOIN_OWNER_COLUMN,
    /** Adapter column (for join table primary-key). */
    ADAPTER_COLUMN
}