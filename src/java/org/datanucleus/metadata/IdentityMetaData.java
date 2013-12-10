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
    ...
**********************************************************************/
package org.datanucleus.metadata;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.StringUtils;

/**
 * Meta-Data for the datastore-identity of a class. Provides a surrogate datastore field.
 * Also defines the generation strategy for the identity values.
 */
public class IdentityMetaData extends MetaData
{
    /** column name value.  */
    protected String columnName;

    /** Metadata for column. */
    protected ColumnMetaData columnMetaData;

    /** strategy tag value. */
    protected IdentityStrategy strategy;

    /** sequence tag value. */
    protected String sequence;

    /** Name of a value generator if the user wants to override the default generator. */
    protected String valueGeneratorName;

    /**
     * Default constructor. Set fields using setters, before populate().
     */
    public IdentityMetaData()
    {
    }

    /**
     * Method to initialise all internal convenience arrays needed.
     */
    public void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        if (strategy == null)
        {
            strategy = IdentityStrategy.NATIVE;
        }

        // Cater for user specifying column name, or column
        if (columnMetaData == null && columnName != null)
        {
            columnMetaData = new ColumnMetaData();
            columnMetaData.setName(columnName);
            columnMetaData.parent = this;
            columnMetaData.initialise(clr, mmgr);
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
     * Accessor for the column name.
     * @return column name
     */
    public String getColumnName()
    {
        return columnName;
    }

    /**
     * @param columnName the columnName to set
     */
    public IdentityMetaData setColumnName(String columnName)
    {
        this.columnName = (StringUtils.isWhitespace(columnName) ? null : columnName);
        return this;
    }

    /**
     * Accessor for the strategy tag value
     * @return strategy tag value
     */
    public IdentityStrategy getValueStrategy()
    {
        return strategy;
    }

    /**
     * @param strategy the strategy to set
     */
    public IdentityMetaData setValueStrategy(IdentityStrategy strategy)
    {
        this.strategy = strategy;
        return this;
    }

    /**
     * Accessor for the sequence name
     * @return sequence name
     */
    public String getSequence()
    {
        return sequence;
    }

    /**
     * @param sequence the sequence to set
     */
    public IdentityMetaData setSequence(String sequence)
    {
        this.sequence = (StringUtils.isWhitespace(sequence) ? null : sequence);
        if (this.sequence != null && this.strategy == null)
        {
            // JDO2 Section 18.6.1. No strategy, but sequence defined means use "sequence"
            this.strategy = IdentityStrategy.SEQUENCE;
        }
        return this;
    }

    /**
     * Name of a (user-provided) value generator to override the default generator for this strategy.
     * @return Name of user provided value generator
     */
    public String getValueGeneratorName()
    {
        return valueGeneratorName;
    }

    /**
     * Mutator for the name of the value generator to use for this strategy.
     * @param generator Name of value generator
     */
    public IdentityMetaData setValueGeneratorName(String generator)
    {
        if (StringUtils.isWhitespace(generator))
        {
            valueGeneratorName = null;
        }
        else
        {
            this.valueGeneratorName = generator;
        }
        return this;
    }

    // ------------------------------ Utilities --------------------------------

    /**
     * Returns a string representation of the object using a prefix
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuffer sb = new StringBuffer();
        if (strategy != null)
        {
            sb.append(prefix).append("<datastore-identity strategy=\"" + strategy + "\"");
        }
        else
        {
            sb.append(prefix).append("<datastore-identity");
        }

        if (columnName != null)
        {
            sb.append("\n").append(prefix).append("        column=\"" + columnName + "\"");
        }
        if (sequence != null)
        {
            sb.append("\n").append(prefix).append("        sequence=\"" + sequence + "\"");
        }

        if ((columnMetaData != null) || getNoOfExtensions() > 0)
        {
            sb.append(">\n");

            // Column MetaData
            if (columnMetaData != null)
            {
                sb.append(columnMetaData.toString(prefix + indent,indent));
            }
     
            // Add extensions
            sb.append(super.toString(prefix + indent,indent));

            sb.append(prefix).append("</datastore-identity>\n");
        }
        else
        {
            sb.append("/>\n");
        }

        return sb.toString();
    }
}