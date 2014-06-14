/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved. 
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
2004 Andy Jefferson - toString(), "column", javadocs, initialise()
    ...
**********************************************************************/
package org.datanucleus.metadata;

/**
 * For schema generation, it might be useful to specify that a column or columns
 * be indexed, and to provide the name of the index. For this purpose, an index
 * element can be contained within a field, element, key, value, or join
 * element, and this indicates that the column(s) associated with the referenced
 * element should be indexed. Indexes can also be specified at the class level,
 * by including index elements containing column elements. In this case, the
 * column elements are mapped elsewhere, and the column elements contain only
 * the column name.
 */
public class IndexMetaData extends ConstraintMetaData
{
    private static final long serialVersionUID = -2262544953953181136L;
    /**
     * You can use UNIQUE constraints to ensure that no duplicate values are
     * entered in specific columns that do not participate in a primary key.
     * Although both a UNIQUE constraint and a PRIMARY KEY constraint enforce
     * uniqueness, use a UNIQUE constraint instead of a PRIMARY KEY constraint
     * when you want to enforce the uniqueness of:
     * <ul>
     * <li>
     * A column, or combination of columns, that is not the primary key.
     * Multiple UNIQUE constraints can be defined on a table, whereas only one
     * PRIMARY KEY constraint can be defined on a table.
     * </li>
     * <li>
     * A column that allows null values. UNIQUE constraints can be defined on
     * columns that allow null values, whereas PRIMARY KEY constraints can be
     * defined only on columns that do not allow null values.
     * </li>
     * </ul>
     * A UNIQUE constraint can also be referenced by a FOREIGN KEY constraint.
     */
    boolean unique = false;

    public IndexMetaData()
    {
    }

    /**
     * Copy constructor.
     * @param imd The metadata to copy
     */
    public IndexMetaData(IndexMetaData imd)
    {
        super(imd);
        this.unique = imd.unique;
    }

    public final boolean isUnique()
    {
        return unique;
    }

    public IndexMetaData setUnique(boolean unique)
    {
        this.unique = unique;
        return this;
    }

    // -------------------------------- Utilities ------------------------------

    /**
     * Returns a string representation of the object.
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<index unique=\"" + unique + "\"");
        if (table != null)
        {
            sb.append(" table=\"" + table + "\"");
        }
        sb.append(name != null ? (" name=\"" + name + "\">\n") : ">\n");

        if (memberNames != null)
        {
            for (String memberName : memberNames)
            {
                sb.append(prefix).append(indent).append("<field name=\"" + memberName + "\"/>");
            }
        }
        if (columnNames != null)
        {
            for (String columnName : columnNames)
            {
                sb.append(prefix).append(indent).append("<column name=\"" + columnName + "\"/>");
            }
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</index>\n");
        return sb.toString();
    }
}