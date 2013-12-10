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

import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Representation of a naming factory for schema components (tables, columns, etc).
 */
public interface NamingFactory
{
    /**
     * Method to set the maximum length of the name of the specified schema component.
     * @param cmpt The component
     * @param max The maximum it accepts
     * @return This naming factory
     */
    NamingFactory setMaximumLength(SchemaComponent cmpt, int max);

    /**
     * Method to set the quote string to use (when the identifiers need to be quoted).
     * See <pre>setIdentifierCase</pre>.
     * @param quote The quote string
     * @return This naming factory
     */
    NamingFactory setQuoteString(String quote);

    /**
     * Method to set the word separator of the names.
     * @param sep Separator
     * @return This naming factory
     */
    NamingFactory setWordSeparator(String sep);

    /**
     * Method to set the required case of the names.
     * @param nameCase Required case
     * @return This naming factory
     */
    NamingFactory setNamingCase(NamingCase nameCase);

    /**
     * Method to return the name of the table for the specified class.
     * @param cmd Metadata for the class
     * @return Name of the table
     */
    String getTableName(AbstractClassMetaData cmd);

    /**
     * Method to return the name of the (join) table for the specified field.
     * @param mmd Metadata for the field/property needing a join table
     * @return Name of the table
     */
    String getTableName(AbstractMemberMetaData mmd);

    /**
     * Method to return the name of the column for the specified class (version, datastore-id, discriminator etc).
     * @param cmd Metadata for the class
     * @param type Column type
     * @return Name of the column
     */
    String getColumnName(AbstractClassMetaData cmd, ColumnType type);

    /**
     * Method to return the name of the column for the specified field.
     * If you have multiple columns for a field then call the other <pre>getColumnName</pre> method.
     * @param mmd Metadata for the field
     * @param type Type of column
     * @return The column name
     */
    String getColumnName(AbstractMemberMetaData mmd, ColumnType type);

    /**
     * Method to return the name of the column for the position of the specified field.
     * Normally the position will be 0 since most fields map to a single column, but where you have a FK
     * to an object with composite id, or where the Java type maps to multiple columns then the position is used.
     * @param mmd Metadata for the field
     * @param type Type of column
     * @param position Position of the column
     * @return The column name
     */
    String getColumnName(AbstractMemberMetaData mmd, ColumnType type, int position);
}