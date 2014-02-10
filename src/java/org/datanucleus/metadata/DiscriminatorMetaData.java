/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved. 
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
2004 Andy Jefferson - added initialise() method
    ...
**********************************************************************/
package org.datanucleus.metadata;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Representation of a discriminator in an inheritance strategy.
 */
public class DiscriminatorMetaData extends MetaData
{
    /** strategy tag value. */
    protected DiscriminatorStrategy strategy = null;

    /** Column name of discriminator */
    protected String columnName = null;

    /** Value for discriminator column */
    protected String value = null;

    /** Whether the discriminator is indexed or not and whether it is unique */
    protected IndexedValue indexed = null;

    /** Discriminator column */
    protected ColumnMetaData columnMetaData=null;

    /** Definition of any indexing of the discriminator column. */
    protected IndexMetaData indexMetaData;

    public DiscriminatorMetaData()
    {
    }

    /**
     * Copy constructor.
     * @param dmd DiscriminatorMetaData to copy
     */
    public DiscriminatorMetaData(final DiscriminatorMetaData dmd)
    {
        super(null, dmd);
        this.columnName = dmd.columnName;
        this.value = dmd.value;
        this.strategy = dmd.strategy;
        this.indexed = dmd.indexed;
        if (dmd.columnMetaData != null)
        {
            setColumnMetaData(new ColumnMetaData(dmd.columnMetaData));
        }
        if (dmd.indexMetaData != null)
        {
            setIndexMetaData(new IndexMetaData(dmd.indexMetaData));
        }
    }

    /**
     * Initialisation method. This should be called AFTER using the populate
     * method if you are going to use populate. It creates the internal
     * convenience arrays etc needed for normal operation.
     */
    public void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        if (value != null && strategy == null)
        {
            // Default to value strategy if a value is specified with no strategy
            this.strategy = DiscriminatorStrategy.VALUE_MAP;
        }
        else if (strategy == null)
        {
            // Default to class strategy if no value is specified and no strategy
            this.strategy = DiscriminatorStrategy.CLASS_NAME;
        }

        if (strategy == DiscriminatorStrategy.VALUE_MAP && value == null)
        {
            AbstractClassMetaData cmd = (AbstractClassMetaData)parent.getParent();
            if (cmd instanceof InterfaceMetaData || (cmd instanceof ClassMetaData && !((ClassMetaData)cmd).isAbstract()))
            {
                // Using "value-map" and no value provided so fall back to class name.
                String className = cmd.getFullClassName();
                NucleusLogger.METADATA.warn(LOCALISER.msg("044103", className));
                this.value = className;
            }
        }

        // Interpret the "indexed" value to create our IndexMetaData where it wasn't specified that way
        if (indexMetaData == null && columnMetaData != null && indexed != null && indexed != IndexedValue.FALSE)
        {
            indexMetaData = new IndexMetaData();
            indexMetaData.setUnique(indexed == IndexedValue.UNIQUE);
            indexMetaData.addColumn(columnMetaData);
            indexMetaData.parent = this;
        }

        setInitialised();
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
     * Mutator for column MetaData.
     * @param columnMetaData The column MetaData to set.
     */
    public void setColumnMetaData(ColumnMetaData columnMetaData)
    {
        this.columnMetaData = columnMetaData;
        this.columnMetaData.parent = this;
    }

    /**
     * Method to create a new ColumnMetaData, add it, and return it.
     * @return The Column metadata
     */
    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        setColumnMetaData(colmd);
        return colmd;
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

    /**
     * Method to create a new Index metadata, add it, and return it.
     * @return The Index metadata
     */
    public IndexMetaData newIndexMetaData()
    {
        IndexMetaData idxmd = new IndexMetaData();
        setIndexMetaData(idxmd);
        return idxmd;
    }

    public String getValue()
    {
        return value;
    }

    public DiscriminatorMetaData setValue(String value)
    {
        if (!StringUtils.isWhitespace(value))
        {
            this.value = value;
        }
        return this;
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

    public DiscriminatorMetaData setColumnName(String columnName)
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

    public final DiscriminatorStrategy getStrategy()
    {
        return strategy;
    }

    public DiscriminatorMetaData setStrategy(DiscriminatorStrategy strategy)
    {
        this.strategy = strategy;
        return this;
    }

    public DiscriminatorMetaData setStrategy(String strategy)
    {
        this.strategy = DiscriminatorStrategy.getDiscriminatorStrategy(strategy);
        return this;
    }

    public IndexedValue getIndexed()
    {
        return indexed;
    }

    public DiscriminatorMetaData setIndexed(IndexedValue indexed)
    {
        this.indexed = indexed;
        return this;
    }

    public DiscriminatorMetaData setIndexed(String indexed)
    {
        this.indexed = IndexedValue.getIndexedValue(indexed);
        return this;
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
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<discriminator");
        if (strategy != null)
        {
            sb.append(" strategy=\"" + strategy.toString() + "\"");
        }
        if (columnName != null && columnMetaData == null)
        {
            sb.append(" column=\"" + columnName + "\"");
        }
        if (value != null)
        {
            sb.append(" value=\"" + value + "\"");
        }
        if (indexed != null)
        {
            sb.append(" indexed=\"" + indexed.toString() + "\"");
        }
        sb.append(">\n");

        // Column MetaData
        if (columnMetaData != null)
        {
            sb.append(columnMetaData.toString(prefix + indent,indent));
        }

        // Add index
        if (indexMetaData != null)
        {
            sb.append(indexMetaData.toString(prefix + indent,indent));
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</discriminator>\n");

        return sb.toString();
    }
}