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
2004 Andy Jefferson - added javadocs.
2004 Andy Jefferson - added unique, indexed
2005 Andy Jefferson - changed foreignKey attr to delete-action
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.util.ArrayList;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.StringUtils;

/**
 * This element specifies the mapping for the element component of arrays and collections.
 * If only one column is mapped, and no additional information is needed for the column, then the 
 * column attribute can be used. Otherwise, the column element(s) are used.
 * The serialized attribute specifies that the key values are to be serialized into the named column.
 * The foreign-key attribute specifies the name of a foreign key to be generated.
 */
public abstract class AbstractElementMetaData extends MetaData implements ColumnMetaDataContainer
{
    private static final long serialVersionUID = -6764719335323972803L;

    /** Whether to add a unique constraint */
    protected boolean unique;

    /** Field that this is mapped to. */
    protected String mappedBy;

    /** The indexing value */
    protected IndexedValue indexed = null;

    /** IndexMetaData */
    protected IndexMetaData indexMetaData;

    /** UniqueMetaData. */
    protected UniqueMetaData uniqueMetaData;

    /** ForeignKeyMetaData */
    protected ForeignKeyMetaData foreignKeyMetaData;

    /** Definition of embedding of the element/key/value. Only present if defined by user. */
    protected EmbeddedMetaData embeddedMetaData;

    protected String table;

    protected String columnName;

    protected List<ColumnMetaData> columns = null;

    /**
     * Constructor to create a copy of the passed metadata object.
     * @param aemd The metadata to copy
     */
    public AbstractElementMetaData(AbstractElementMetaData aemd)
    {
        super(null, aemd);
        this.table = aemd.table;
        this.columnName = aemd.columnName;
        this.unique = aemd.unique;
        this.indexed = aemd.indexed;
        this.mappedBy = aemd.mappedBy;

        if (aemd.indexMetaData != null)
        {
            setIndexMetaData(new IndexMetaData(aemd.indexMetaData));
        }
        if (aemd.uniqueMetaData != null)
        {
            setUniqueMetaData(new UniqueMetaData(aemd.uniqueMetaData));
        }
        if (aemd.foreignKeyMetaData != null)
        {
            setForeignKeyMetaData(new ForeignKeyMetaData(aemd.foreignKeyMetaData));
        }
        if (aemd.embeddedMetaData != null)
        {
            setEmbeddedMetaData(new EmbeddedMetaData(aemd.embeddedMetaData));
        }
        if (aemd.columns != null)
        {
            for (ColumnMetaData colmd : aemd.columns)
            {
                addColumn(new ColumnMetaData(colmd));
            }
        }
    }

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public AbstractElementMetaData()
    {
    }

    public MetaDataManager getMetaDataManager()
    {
        if (parent instanceof AbstractMemberMetaData)
        {
            return ((AbstractMemberMetaData)parent).getMetaDataManager();
        }
        return null;
    }

    /**
     * Populate the metadata.
     * @param clr the ClassLoaderResolver
     * @param primary the primary ClassLoader to use (or null)
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary)
    {
        if (embeddedMetaData != null)
        {
            MetaDataManager mmgr = ((AbstractMemberMetaData)parent).getMetaDataManager();
            embeddedMetaData.populate(clr, primary, mmgr);
        }
    }

    /**
     * Method to initialise the object, creating any convenience arrays needed.
     * Initialises all sub-objects. 
     */
    public void initialise(ClassLoaderResolver clr)
    {
        // Cater for user specifying column name, or columns
        if (columns == null && columnName != null)
        {
            ColumnMetaData colmd = new ColumnMetaData();
            colmd.setName(columnName);
            colmd.parent = this;
            addColumn(colmd);
        }

        // Interpret the "indexed" value to create our IndexMetaData where it wasn't specified that way
        if (indexMetaData == null && indexed != null && indexed != IndexedValue.FALSE)
        {
            indexMetaData = new IndexMetaData();
            indexMetaData.setUnique(indexed == IndexedValue.UNIQUE);
            if (columns != null)
            {
                for (ColumnMetaData colmd : columns)
                {
                    indexMetaData.addColumn(colmd.getName());
                }
            }
        }

        if (uniqueMetaData == null && unique)
        {
            uniqueMetaData = new UniqueMetaData();
            uniqueMetaData.setTable(columnName);
            if (columns != null)
            {
                for (ColumnMetaData colmd : columns)
                {
                    uniqueMetaData.addColumn(colmd.getName());
                }
            }
        }

        if (embeddedMetaData != null)
        {
            embeddedMetaData.initialise(clr);
        }

        setInitialised();
    }

    public String getTable()
    {
        return table;
    }

    public void setTable(String table)
    {
        this.table = table;
    }

    public String getMappedBy()
    {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy)
    {
        this.mappedBy = StringUtils.isWhitespace(mappedBy) ? null : mappedBy;
    }

    public IndexedValue getIndexed()
    {
        return indexed;
    }

    public void setIndexed(IndexedValue indexed)
    {
        this.indexed = indexed;
    }

    public boolean isUnique()
    {
        return unique;
    }

    public void setUnique(boolean unique)
    {
        this.unique = unique;
    }

    /**
     * Accessor for foreignKeyMetaData
     * @return Returns the foreignKeyMetaData.
     */
    public final ForeignKeyMetaData getForeignKeyMetaData()
    {
        return foreignKeyMetaData;
    }

    public ForeignKeyAction getDeleteAction()
    {
        if (foreignKeyMetaData != null)
        {
            return foreignKeyMetaData.getDeleteAction();
        }
        return null;
    }

    public void setDeleteAction(String deleteAction)
    {
        if (!StringUtils.isWhitespace(deleteAction))
        {
            this.foreignKeyMetaData = new ForeignKeyMetaData();
            this.foreignKeyMetaData.setDeleteAction(ForeignKeyAction.getForeignKeyAction(deleteAction));
        }
    }

    public void setDeleteAction(ForeignKeyAction deleteAction)
    {
        if (deleteAction != null)
        {
            this.foreignKeyMetaData = new ForeignKeyMetaData();
            this.foreignKeyMetaData.setDeleteAction(deleteAction);
        }
    }

    public ForeignKeyAction getUpdateAction()
    {
        if (foreignKeyMetaData != null)
        {
            return foreignKeyMetaData.getUpdateAction();
        }
        return null;
    }

    public void setUpdateAction(String updateAction)
    {
        if (!StringUtils.isWhitespace(updateAction))
        {
            this.foreignKeyMetaData = new ForeignKeyMetaData();
            this.foreignKeyMetaData.setUpdateAction(ForeignKeyAction.getForeignKeyAction(updateAction));
        }
    }

    public void setUpdateAction(ForeignKeyAction updateAction)
    {
        if (updateAction != null)
        {
            this.foreignKeyMetaData = new ForeignKeyMetaData();
            this.foreignKeyMetaData.setUpdateAction(updateAction);
        }
    }

    public final String getColumnName()
    {
        return columnName;
    }

    public void setColumnName(String columnName)
    {
        if (!StringUtils.isWhitespace(columnName))
        {
            this.columnName = columnName;
            if (columns == null)
            {
                ColumnMetaData colmd = new ColumnMetaData();
                colmd.setName(columnName);
                colmd.parent = this;
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
    }

    /**
     * Accessor for columnMetaData
     * @return Returns the columnMetaData.
     */
    public final ColumnMetaData[] getColumnMetaData()
    {
        if (columns == null)
        {
            return new ColumnMetaData[0];
        }
        return columns.toArray(new ColumnMetaData[columns.size()]);
    }

    /**
     * Add a new ColumnMetaData element
     * @param colmd The Column MetaData 
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
     * Method to create a column metadata, add it, and return it.
     * @return The column metadata
     */
    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        addColumn(colmd);
        return colmd;
    }

    /**
     * Accessor for embeddedMetaData
     * @return Returns the embeddedMetaData.
     */
    public final EmbeddedMetaData getEmbeddedMetaData()
    {
        return embeddedMetaData;
    }

    /**
     * Mutator for the Embedded MetaData 
     * @param embeddedMetaData The embeddedMetaData to set.
     */
    public final void setEmbeddedMetaData(EmbeddedMetaData embeddedMetaData)
    {
        this.embeddedMetaData = embeddedMetaData;
        embeddedMetaData.parent  = this;
    }

    /**
     * Method to create an embedded metadata, add it, and return it.
     * @return The embedded metadata
     */
    public EmbeddedMetaData newEmbeddedMetaData()
    {
        EmbeddedMetaData embmd = new EmbeddedMetaData();
        setEmbeddedMetaData(embmd);
        return embmd;
    }

    /**
     * Mutator for the Foreign Key MetaData 
     * @param foreignKeyMetaData The foreignKeyMetaData to set.
     */
    public final void setForeignKeyMetaData(ForeignKeyMetaData foreignKeyMetaData)
    {
        this.foreignKeyMetaData = foreignKeyMetaData;
        foreignKeyMetaData.parent = this;
    }

    /**
     * Method to create a unique metadata, add it, and return it.
     * @return The unique metadata
     */
    public ForeignKeyMetaData newForeignKeyMetaData()
    {
        ForeignKeyMetaData fkmd = new ForeignKeyMetaData();
        setForeignKeyMetaData(fkmd);
        return fkmd;
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
     * Mutator for the Index MetaData 
     * @param indexMetaData The indexMetaData to set.
     */
    public final void setIndexMetaData(IndexMetaData indexMetaData)
    {
        this.indexMetaData = indexMetaData;
        indexMetaData.parent = this;
    }

    /**
     * Method to create an index metadata, add it, and return it.
     * @return The index metadata
     */
    public IndexMetaData newIndexMetaData()
    {
        IndexMetaData idxmd = new IndexMetaData();
        setIndexMetaData(idxmd);
        return idxmd;
    }

    /**
     * Accessor for uniqueMetaData
     * @return Returns the uniqueMetaData.
     */
    public final UniqueMetaData getUniqueMetaData()
    {
        return uniqueMetaData;
    }

    /**
     * Mutator for the Unique MetaData 
     * @param uniqueMetaData The uniqueMetaData to set.
     */
    public final void setUniqueMetaData(UniqueMetaData uniqueMetaData)
    {
        this.uniqueMetaData = uniqueMetaData;
        uniqueMetaData.parent = this;
    }

    /**
     * Method to create a unique metadata, add it, and return it.
     * @return The unique metadata
     */
    public UniqueMetaData newUniqueMetaData()
    {
        UniqueMetaData unimd = new UniqueMetaData();
        setUniqueMetaData(unimd);
        return unimd;
    }
}