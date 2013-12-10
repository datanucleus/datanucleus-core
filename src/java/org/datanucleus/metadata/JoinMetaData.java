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
2004 Andy Jefferson - addition of table name
2005 Andy Jefferson - added primary-key, unique, and others
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.util.ArrayList;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.StringUtils;

/**
 * Secondary tables and join tables are mapped using a join condition that
 * associates a column or columns in the secondary or join table with a column
 * or columns in the primary table, typically the primary tables primary key
 * columns. Column elements used for relationship mapping or join conditions
 * specify the column name and optionally the target column name. The target
 * column name is the name of the column in the associated table corresponding
 * to the named column. The target column name is optional when the target
 * column is the single primary key column of the associated table.
 */
public class JoinMetaData extends MetaData implements ColumnMetaDataContainer
{
    /** the foreign-key element. */
    ForeignKeyMetaData foreignKeyMetaData;

    /** the index element. */
    IndexMetaData indexMetaData;

    /** the unique element. */
    UniqueMetaData uniqueMetaData;

    /** PrimaryKey MetaData */
    protected PrimaryKeyMetaData primaryKeyMetaData;

    /** column elements */
    final List<ColumnMetaData> columns = new ArrayList();
       
    /** if is outer join. Outer joins return all elements from at least one of the sides joined. */
    boolean outer = false;

    /** the table name. */
    String table;

    /** the catalog name. */
    String catalog;

    /** the schema name. */
    String schema;

    /** the column name. */
    String columnName;

    /** The indexing value */
    protected IndexedValue indexed=null;

    /** Whether to add a unique constraint. */
    protected boolean unique;

    // -------------------------------------------------------------------------
    // Fields below here are not represented in the output MetaData. They are
    // for use internally in the operation of the JDO system. The majority are
    // for convenience to save iterating through the fields since the fields
    // are fixed once initialised.

    /**
     * Contains the metadata for column
     */
    protected ColumnMetaData[] columnMetaData;

    /**
     * Constructor to create a copy of the passed JoinMetaData.
     * @param joinmd Existing join metadata to copy
     */
    public JoinMetaData(JoinMetaData joinmd)
    {
        this.table = joinmd.table;
        this.catalog = joinmd.catalog;
        this.schema = joinmd.schema;
        this.columnName = joinmd.columnName;
        this.outer = joinmd.outer;
        this.indexed = joinmd.indexed;
        this.unique = joinmd.unique;
        for (int i=0;i<joinmd.columns.size();i++)
        {
            addColumn(new ColumnMetaData(joinmd.columns.get(i)));
        }
    }

    /**
     * Default constructor. Use setters to set fields, before populate().
     */
    public JoinMetaData()
    {        
    }

    /**
     * Method to initialise the object, creating internal convenience arrays.
     * Initialises all sub-objects.
     */
    public void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        if (table != null && parent instanceof AbstractMemberMetaData)
        {
            // "table" has been specified but this join is within <field> or <property> so is not applicable
            //TODO fix message for property and field
            AbstractMemberMetaData mmd = (AbstractMemberMetaData) parent;
            throw new InvalidMemberMetaDataException(LOCALISER, "044130", mmd.getClassName(), mmd.getFullFieldName());
        }

        // Cater for user specifying column name, or columns
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
                columnMetaData[i] = columns.get(i);
                columnMetaData[i].initialise(clr, mmgr);
            }
        }

        if (foreignKeyMetaData != null)
        {
            foreignKeyMetaData.initialise(clr, mmgr);
        }

        // Interpret the "indexed" value to create our IndexMetaData where it wasn't specified that way
        if (indexMetaData == null && columnMetaData != null && indexed != null && indexed != IndexedValue.FALSE)
        {
            indexMetaData = new IndexMetaData();
            indexMetaData.setUnique(indexed == IndexedValue.UNIQUE);
            for (int i=0;i<columnMetaData.length;i++)
            {
                indexMetaData.addColumn(columnMetaData[i]);
            }
        }
        if (indexMetaData != null)
        {
            indexMetaData.initialise(clr, mmgr);
        }

        if (uniqueMetaData == null && unique)
        {
            uniqueMetaData = new UniqueMetaData();
            uniqueMetaData.setTable(columnName);
            for (int i=0;i<columnMetaData.length;i++)
            {
                uniqueMetaData.addColumn(columnMetaData[i]);
            }
        }
        if (uniqueMetaData != null)
        {
            uniqueMetaData.initialise(clr, mmgr);
        }

        if (primaryKeyMetaData != null)
        {
            primaryKeyMetaData.initialise(clr, mmgr);
        }

        setInitialised();
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
     * Method to create a new column metadata, add it, and return it.
     * @return The column metadata
     */
    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        addColumn(colmd);
        return colmd;
    }

    public final boolean isOuter()
    {
        return outer;
    }

    public JoinMetaData setOuter(boolean outer)
    {
        this.outer = outer;
        return this;
    }

    public String getDeleteAction()
    {
        if (foreignKeyMetaData != null)
        {
            return foreignKeyMetaData.getDeleteAction().toString();
        }
        return null;
    }

    public JoinMetaData setDeleteAction(String deleteAction)
    {
        if (!StringUtils.isWhitespace(deleteAction))
        {
            this.foreignKeyMetaData = new ForeignKeyMetaData();
            this.foreignKeyMetaData.setDeleteAction(ForeignKeyAction.getForeignKeyAction(deleteAction));
        }
        return this;
    }

    public IndexedValue getIndexed()
    {
        return indexed;
    }

    public JoinMetaData setIndexed(IndexedValue indexed)
    {
        if (indexed != null)
        {
            this.indexed = indexed;
        }
        return this;
    }

    public boolean isUnique()
    {
        return unique;
    }

    public JoinMetaData setUnique(boolean unique)
    {
        this.unique = unique;
        return this;
    }

    public JoinMetaData setUnique(String unique)
    {
        this.unique = MetaDataUtils.getBooleanForString(unique, false);
        return this;
    }

    public final String getTable()
    {
        return table;
    }

    public JoinMetaData setTable(String table)
    {
        this.table = (StringUtils.isWhitespace(table) ? null : table);
        return this;
    }

    public final String getCatalog()
    {
        return catalog;
    }

    public JoinMetaData setCatalog(String catalog)
    {
        this.catalog = (StringUtils.isWhitespace(catalog) ? null : catalog);
        return this;
    }

    public final String getSchema()
    {
        return schema;
    }

    public JoinMetaData setSchema(String schema)
    {
        this.schema = (StringUtils.isWhitespace(schema) ? null : schema);
        return this;
    }

    public final String getColumnName()
    {
        return columnName;
    }

    public JoinMetaData setColumnName(String columnName)
    {
        this.columnName = (StringUtils.isWhitespace(columnName) ? null : columnName);
        return this;
    }

    /**
     * Accessor for columnMetaData
     * @return Returns the columnMetaData.
     */
    public final ColumnMetaData[] getColumnMetaData()
    {
        return columnMetaData;
    }

    /**
     * @return Returns the indexMetaData.
     */
    public final IndexMetaData getIndexMetaData()
    {
        return indexMetaData;
    }

    /**
     * @return Returns the uniquexMetaData.
     */
    public final UniqueMetaData getUniqueMetaData()
    {
        return uniqueMetaData;
    }

    /**
     * @return Returns the foreignKeyMetaData.
     */
    public final ForeignKeyMetaData getForeignKeyMetaData()
    {
        return foreignKeyMetaData;
    }

    /**
     * Accessor for primaryKeyMetaData
     * @return Returns the primaryKey MetaData.
     */
    public final PrimaryKeyMetaData getPrimaryKeyMetaData()
    {
        return primaryKeyMetaData;
    }

    /**
     * @param foreignKeyMetaData The foreignKeyMetaData to set.
     */
    public final void setForeignKeyMetaData(ForeignKeyMetaData foreignKeyMetaData)
    {
        this.foreignKeyMetaData = foreignKeyMetaData;
        foreignKeyMetaData.parent = this;
    }

    /**
     * Method to create a new FK metadata, set to use it, and return it.
     * @return The FK metadata
     */
    public ForeignKeyMetaData newForeignKeyMetaData()
    {
        ForeignKeyMetaData fkmd = new ForeignKeyMetaData();
        setForeignKeyMetaData(fkmd);
        return fkmd;
    }

    /**
     * @param indexMetaData The indexMetaData to set.
     */
    public final void setIndexMetaData(IndexMetaData indexMetaData)
    {
        this.indexMetaData = indexMetaData;
        indexMetaData.parent = this;
    }

    /**
     * Method to create a new index metadata, set to use it, and return it.
     * @return The index metadata
     */
    public IndexMetaData newIndexMetaData()
    {
        IndexMetaData idxmd = new IndexMetaData();
        setIndexMetaData(idxmd);
        return idxmd;
    }

    /**
     * @param uniqueMetaData The uniqueMetaData to set.
     */
    public final void setUniqueMetaData(UniqueMetaData uniqueMetaData)
    {
        this.uniqueMetaData = uniqueMetaData;
        uniqueMetaData.parent = this;
    }

    /**
     * Method to create a new unique metadata, set to use it, and return it.
     * @return The unique metadata
     */
    public UniqueMetaData newUniqueMetaData()
    {
        UniqueMetaData unimd = new UniqueMetaData();
        setUniqueMetaData(unimd);
        return unimd;
    }

    /**
     * Mutator for the PrimaryKey MetaData.
     * @param primaryKeyMetaData The PrimaryKey MetaData to set.
     */
    public final void setPrimaryKeyMetaData(PrimaryKeyMetaData primaryKeyMetaData)
    {
        this.primaryKeyMetaData = primaryKeyMetaData;
        primaryKeyMetaData.parent = this;
    }

    /**
     * Method to create a new PK metadata, set to use it, and return it.
     * @return The PK metadata
     */
    public PrimaryKeyMetaData newPrimaryKeyMetaData()
    {
        PrimaryKeyMetaData pkmd = new PrimaryKeyMetaData();
        setPrimaryKeyMetaData(pkmd);
        return pkmd;
    }

    // ----------------------------- Utilities ---------------------------------

    /**
     * Returns a string representation of the object using a prefix
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<join");
        if (table != null)
        {
            sb.append(" table=\"" + table + "\"");
        }
        if (columnName != null)
        {
            sb.append(" column=\"" + columnName + "\"");
        }
        sb.append(" outer=\"" + outer + "\"");
        sb.append(">\n");

        // Primary-key
        if (primaryKeyMetaData != null)
        {
            sb.append(primaryKeyMetaData.toString(prefix + indent, indent));
        }

        // Add columns
        if (columnMetaData != null)
        {
            for (int i=0;i<columnMetaData.length;i++)
            {
                sb.append(columnMetaData[i].toString(prefix + indent,indent));
            }
        }

        // Foreign-key
        if (foreignKeyMetaData != null)
        {
            sb.append(foreignKeyMetaData.toString(prefix + indent, indent));
        }

        // Index
        if (indexMetaData != null)
        {
            sb.append(indexMetaData.toString(prefix + indent, indent));
        }

        // Unique
        if (uniqueMetaData != null)
        {
            sb.append(uniqueMetaData.toString(prefix + indent, indent));
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</join>\n");
        return sb.toString();
    }
}