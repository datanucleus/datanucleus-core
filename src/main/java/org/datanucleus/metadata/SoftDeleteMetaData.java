/**********************************************************************
Copyright (c) 2021 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.StringUtils;

/**
 * MetaData representation for a soft delete indicator column.
 */
public class SoftDeleteMetaData extends MetaData
{
    private static final long serialVersionUID = 1526198088851283681L;

    /** Column name for soft delete. */
    protected String columnName = null;

    /** Multitenancy column details. */
    protected ColumnMetaData columnMetaData=null;

    protected Boolean indexed = null;

    /** Detail of any indexing of the soft delete column (optional). */
    protected IndexMetaData indexMetaData;

    public SoftDeleteMetaData()
    {
    }

    /**
     * Copy constructor.
     * @param sdmd SoftDeleteMetaData to copy
     */
    public SoftDeleteMetaData(final SoftDeleteMetaData sdmd)
    {
        super(null, sdmd);
        this.columnName = sdmd.columnName;
        this.indexed = sdmd.indexed;
        if (sdmd.columnMetaData != null)
        {
            setColumnMetaData(new ColumnMetaData(sdmd.columnMetaData));
        }
        if (sdmd.indexMetaData != null)
        {
            setIndexMetaData(new IndexMetaData(sdmd.indexMetaData));
        }
    }

    /**
     * Initialisation method. 
     * This should be called AFTER using the populate method if you are going to use populate. 
     * It creates the internal convenience arrays etc needed for normal operation.
     * @param clr Not used
     */
    public void initialise(ClassLoaderResolver clr)
    {
        // Interpret the "indexed" value to create our IndexMetaData where it wasn't specified that way
        if (indexMetaData == null && indexed == Boolean.TRUE)
        {
            indexMetaData = new IndexMetaData();
            indexMetaData.setUnique(false);
            if (columnMetaData != null)
            {
                indexMetaData.addColumn(columnMetaData.getName());
            }
            indexMetaData.parent = this;
        }

        setInitialised();
    }

    public String getColumnName()
    {
        // Return the column variant if specified, otherwise the name field
        if (columnMetaData != null && columnMetaData.getName() != null)
        {
            return columnMetaData.getName();
        }
        return columnName;
    }

    public SoftDeleteMetaData setColumnName(String columnName)
    {
        if (!StringUtils.isWhitespace(columnName))
        {
            if (columnMetaData == null)
            {
                columnMetaData = new ColumnMetaData();
                columnMetaData.setName(columnName);
                columnMetaData.parent = this;
            }
            else
            {
                columnMetaData.setName(columnName);
            }
            this.columnName = columnName;
        }
        else
        {
            this.columnName = null;
        }
        return this;
    }

    /**
     * Accessor for column MetaData.
     * @return Returns the column MetaData.
     */
    public ColumnMetaData getColumnMetaData()
    {
        return columnMetaData;
    }

    /**
     * Method to create a new ColumnMetaData, add it, and return it.
     * @return The Column metadata
     */
    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        colmd.setJdbcType(JdbcType.BOOLEAN); // Default to boolean
        if (columnName != null)
        {
            colmd.setName(columnName);
        }
        setColumnMetaData(colmd);
        return colmd;
    }

    /**
     * Mutator for column MetaData.
     * @param columnMetaData The column MetaData to set.
     */
    public void setColumnMetaData(ColumnMetaData columnMetaData)
    {
        this.columnMetaData = columnMetaData;
        this.columnMetaData.parent = this;
    }

    public SoftDeleteMetaData setIndexed(boolean indexed)
    {
        this.indexed = indexed;
        return this;
    }

    /**
     * Accessor for indexMetaData
     * @return Returns the indexMetaData.
     */
    public final IndexMetaData getIndexMetaData()
    {
        return indexMetaData;
    }

    /**
     * Mutator for the index MetaData 
     * @param indexMetaData The indexMetaData to set.
     */
    public final void setIndexMetaData(IndexMetaData indexMetaData)
    {
        this.indexMetaData = indexMetaData;
        this.indexMetaData.parent = this;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("SoftDeleteMetaData[");
        if (columnName != null)
        {
            str.append("columnName=").append(columnName);
        }
        if (columnMetaData != null)
        {
            str.append(", column=").append(columnMetaData);
        }
        if (indexed != null)
        {
            str.append(", indexed=").append(indexed);
        }
        str.append("]");
        return str.toString();
    }
}
