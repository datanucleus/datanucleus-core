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
    /** Whether to add a unique constraint */
    protected boolean unique;

    /** column name value. */
    protected String columnName;

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

    /** EmbeddedMetaData */
    protected EmbeddedMetaData embeddedMetaData;

    /** Columns ColumnMetaData */
    protected final List<ColumnMetaData> columns = new ArrayList<ColumnMetaData>();

    // -------------------------------------------------------------------------
    // Fields below here are not represented in the output MetaData. They are
    // for use internally in the operation of the JDO system. The majority are
    // for convenience to save iterating through the fields since the fields
    // are fixed once initialised.

    protected ColumnMetaData columnMetaData[];

    /**
     * Constructor to create a copy of the passed metadata object.
     * @param aemd The metadata to copy
     */
    public AbstractElementMetaData(AbstractElementMetaData aemd)
    {
        super(null, aemd);
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
        for (int i=0;i<columns.size();i++)
        {
            addColumn(new ColumnMetaData(aemd.columns.get(i)));
        }
    }

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public AbstractElementMetaData()
    {
    }

    /**
     * Populate the metadata.
     * @param clr the ClassLoaderResolver
     * @param primary the primary ClassLoader to use (or null)
     * @param mmgr MetaData manager
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr)
    {
        if (embeddedMetaData != null)
        {
            embeddedMetaData.populate(clr, primary, mmgr);
        }
    }

    /**
     * Method to initialise the object, creating any convenience arrays needed.
     * Initialises all sub-objects. 
     */
    public void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
    {
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

        if (foreignKeyMetaData != null)
        {
            foreignKeyMetaData.initialise(clr, mmgr);
        }

        if (embeddedMetaData != null)
        {
            embeddedMetaData.initialise(clr, mmgr);
        }

        setInitialised();
    }

    public final String getColumnName()
    {
        return columnName;
    }

    public void setColumnName(String columnName)
    {
        this.columnName = (StringUtils.isWhitespace(columnName) ? null : columnName);
    }

    public String getMappedBy()
    {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy)
    {
        this.mappedBy = (StringUtils.isWhitespace(mappedBy) ? null : mappedBy);
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

    /**
     * Accessor for columnMetaData
     * @return Returns the columnMetaData.
     */
    public final ColumnMetaData[] getColumnMetaData()
    {
        return columnMetaData;
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
     * Accessor for foreignKeyMetaData
     * @return Returns the foreignKeyMetaData.
     */
    public final ForeignKeyMetaData getForeignKeyMetaData()
    {
        return foreignKeyMetaData;
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
     * Accessor for uniqueMetaData
     * @return Returns the uniqueMetaData.
     */
    public final UniqueMetaData getUniqueMetaData()
    {
        return uniqueMetaData;
    }

    /**
     * Add a new ColumnMetaData element
     * @param colmd The Column MetaData 
     */
    public void addColumn(ColumnMetaData colmd)
    {
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