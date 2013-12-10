/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved. 
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

import java.util.ArrayList;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.StringUtils;

/**
 * Representation of a primary key constraint.
 * Can also be used for specifying surrogate keys, but we doesn't support this.
 */
public class PrimaryKeyMetaData extends MetaData implements ColumnMetaDataContainer
{
    /** PK constraint name. */
    protected String name = null;

    /** Column name of PK. */
    protected String columnName = null;

    /** MetaData for columns to be used in PK. */
    protected ColumnMetaData[] columnMetaData=null;

    // -------------------------------------------------------------------------
    // Fields below here are used in the metadata parse process where the parser
    // dynamically adds fields/columns as it encounters them in the MetaData files.
    // They are typically cleared at the point of initialise() and not used thereafter.

    /**
     * the columns elements to be included in the index. Suitable to be empty
     * when this metadata is contained within a field, element, key, value, or join elements
     */
    protected List columns = new ArrayList();

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public PrimaryKeyMetaData()
    {
    }

    /**
     * Initialisation method. This should be called AFTER using the populate
     * method if you are going to use populate. It creates the internal
     * convenience arrays etc needed for normal operation.
     */
    public void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        // Set up the columnMetaData
        if (columns.size() == 0 && columnName != null)
        {
            columnMetaData = new ColumnMetaData[1];
            columnMetaData[0] = new ColumnMetaData();
            columnMetaData[0].setName(columnName);
            columnMetaData[0].parent = this;
            columnMetaData[0].initialise(clr, mmgr);
        }
        else
        {
            columnMetaData = new ColumnMetaData[columns.size()];
            for (int i=0; i<columnMetaData.length; i++)
            {
                columnMetaData[i] = (ColumnMetaData) columns.get(i);
                columnMetaData[i].initialise(clr, mmgr);
            }
        }

        // Clean out parsing data
        columns.clear();
        columns = null;

        setInitialised();
    }

    public String getName()
    {
        return name;
    }

    public PrimaryKeyMetaData setName(String name)
    {
        this.name = (StringUtils.isWhitespace(name) ? null : name);
        return this;
    }

    public PrimaryKeyMetaData setColumnName(String name)
    {
        this.columnName = (StringUtils.isWhitespace(name) ? null : name);
        return this;
    }

    public String getColumnName()
    {
        return columnName;
    }

    /**
     * Add a new ColumnMetaData element
     * @param colmd The ColumnMetaData to add
     */
    public void addColumn(ColumnMetaData colmd)
    {
        columns.add(colmd);
        colmd.parent = this;
    }

    /**
     * Method to create a new column, add it, and return it.
     * @return The column metadata
     */
    public ColumnMetaData newColumnMetadata()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        addColumn(colmd);
        return colmd;
    }
    
    /**
     * Accessor for columnMetaData
     * @return Returns the columnMetaData.
     */
    public final ColumnMetaData[] getColumnMetaData()
    {
        return columnMetaData;
    }

    //  ----------------------------- Utilities ---------------------------------

    /**
     * Returns a string representation of the object using a prefix
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<primary-key" + 
            (name != null ? (" name=\"" + name + "\"") : "") +
            (columnName != null ? (" column=\"" + columnName + "\"") : "") +
            ">\n");

        // Add columns
        if (columnMetaData != null)
        {
            for (int i=0;i<columnMetaData.length;i++)
            {
                sb.append(columnMetaData[i].toString(prefix + indent,indent));
            }
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</primary-key>\n");

        return sb.toString();
    }
}