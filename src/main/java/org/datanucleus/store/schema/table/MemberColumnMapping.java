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
package org.datanucleus.store.schema.table;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Mapping definition for a member (field/property) representing the column(s) that it maps to.
 * This class is required as an intermediary between Table and Column so that we can support mapping a member to multiple columns.
 */
public interface MemberColumnMapping
{
    /**
     * Accessor for the metadata for this member.
     * @return Metadata for the member.
     */
    AbstractMemberMetaData getMemberMetaData();

    /**
     * Accessor for the column at the specified position. 0 is the first column.
     * If the position is out of range then returns null;
     * @param position The position, with origin 0
     * @return The Column
     */
    Column getColumn(int position);

    /**
     * Accessor for the columns representing this member.
     * @return The columns
     */
    Column[] getColumns();

    /**
     * Accessor for the number of columns that represents this member.
     * @return Number of columns
     */
    int getNumberOfColumns();

    /**
     * Method to set the TypeConverter used by this member-column.
     * @param typeConv The TypeConverter to use
     */
    void setTypeConverter(TypeConverter typeConv);

    /**
     * Method to set a component TypeConverter for such as a collection element, map key or map value.
     * @param role The role where this converter is used
     * @param conv The converter
     */
    void setTypeConverterForComponent(FieldRole role, TypeConverter conv);

    /**
     * Accessor for the TypeConverter to use for this member-column (if any).
     * @return The TypeConverter
     */
    TypeConverter getTypeConverter();

    /**
     * Accessor for a component (collection element, map key, map value) converter if defined.
     * @param role The role of the component where the converter would be used
     * @return The converter (if any). Null is returned if nothing defined
     */
    TypeConverter getTypeConverterForComponent(FieldRole role);
}