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

import org.datanucleus.util.StringUtils;

/**
 * Representation of a primary key constraint.
 * Can also be used for specifying surrogate keys, but we doesn't support this.
 */
public class PrimaryKeyMetaData extends MetaData implements ColumnMetaDataContainer
{
    private static final long serialVersionUID = 6303979815375277900L;

    /** PK constraint name. */
    protected String name = null;

    /** Column name of PK. */
    protected String columnName = null;

    protected List<ColumnMetaData> columns = null;

    public PrimaryKeyMetaData()
    {
    }

    public String getName()
    {
        return name;
    }

    public PrimaryKeyMetaData setName(String name)
    {
        this.name = StringUtils.isWhitespace(name) ? null : name;
        return this;
    }

    public PrimaryKeyMetaData setColumnName(String name)
    {
        if (!StringUtils.isWhitespace(name))
        {
            this.columnName = StringUtils.isWhitespace(name) ? null : name;
            if (columns == null)
            {
                ColumnMetaData colmd = newColumnMetadata();
                colmd.setName(columnName);
                addColumn(colmd);
            }
            else if (columns.size() == 1)
            {
                this.columns.iterator().next().setName(columnName);
            }
        }
        else
        {
            this.columnName = null;
        }
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
        if (columns == null)
        {
            columns = new ArrayList<ColumnMetaData>();
        }
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
        if (columns == null)
        {
            return null;
        }
        return columns.toArray(new ColumnMetaData[columns.size()]);
    }
}