/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.schema.table;

import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.JdbcType;
import org.datanucleus.store.schema.naming.ColumnType;

/**
 * Interface representing a column in a table.
 */
public interface Column
{
    /**
     * Accessor for the column name.
     * @return Name of column
     */
    String getName();

    /**
     * Accessor for the table for this column.
     * @return The table
     */
    Table getTable();

    /**
     * Accessor for the mapping (and hence member) that owns this column.
     * @return The associated mapping (or null if this is a surrogate column)
     */
    MemberColumnMapping getMemberColumnMapping();

    /**
     * Whether this column is (part of) the primary key
     * @return Whether part of the PK
     */
    boolean isPrimaryKey();

    /**
     * Mutator to make the column (part of) the primary key.
     * @return TODO
     */
    Column setPrimaryKey();

    /**
     * Mutator for the nullability of the column.
     * @param nullable Whether this is nullable
     * @return The column with the updated info
     */
    Column setNullable(boolean nullable);

    /**
     * Accessor for whether the column is nullable in the datastore.
     * @return whether the column is nullable
     */
    boolean isNullable();

    /**
     * Mutator for the defaultability of the column.
     * @param defaultValue The default to use
     * @return The column with the updated info
     */
    Column setDefaultable(Object defaultValue); 

    /**
     * Accessor for whether the column is defaultable.
     * @return whether the column is defaultable
     */
    boolean isDefaultable();

    /**
     * Accessor for the default Value
     * @return the default value
     */
    Object getDefaultValue();

    /**
     * Mutator for the uniqueness of the column.
     * @param unique The flag
     * @return The column with the updated info
     */
    Column setUnique(boolean unique);

    /**
     * Accessor for whether the column is unique.
     * @return whether the column is unique
     */
    boolean isUnique();

    /**
     * Accessor for the role that this column serves (if known).
     * @return Role of the column
     */
    ColumnType getColumnType();

    Column setJdbcType(JdbcType jdbcType);

    /**
     * Accessor for the JDBC Type used for this column.
     * @return The Jdbc type
     */
    JdbcType getJdbcType();

    Column setTypeName(String type);

    /**
     * Accessor for the native type name in the datastore for this column.
     * @return The column type name
     */
    String getTypeName();

    Column setPosition(int pos);

    /**
     * Accessor for the position of this column in the table (if specified).
     * @return The position, or -1 if not specified
     */
    int getPosition();

    Column setColumnMetaData(ColumnMetaData md);

    /**
     * Accessor for the metadata for this column (if any).
     * @return Metadata for the column
     */
    ColumnMetaData getColumnMetaData();
}