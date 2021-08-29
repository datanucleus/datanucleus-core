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

import org.datanucleus.util.StringUtils;

/**
 * Meta-Data for the datastore-identity of a class. 
 * Provides a surrogate datastore field.
 * Also defines the generation strategy for the identity values.
 */
public class DatastoreIdentityMetaData extends MetaData
{
    private static final long serialVersionUID = 4740941674001139996L;

    /** column name value.  */
    protected String columnName;

    /** Metadata for column. */
    protected ColumnMetaData columnMetaData;

    /** strategy tag value. */
    protected ValueGenerationStrategy strategy = ValueGenerationStrategy.NATIVE;

    /** sequence tag value. */
    protected String sequence;

    /** Name of a value generator if the user wants to override the default generator. */
    protected String valueGeneratorName;

    /**
     * Default constructor. Set fields using setters, before populate().
     */
    public DatastoreIdentityMetaData()
    {
    }

    public ColumnMetaData getColumnMetaData()
    {
        return columnMetaData;
    }

    public void setColumnMetaData(ColumnMetaData columnMetaData)
    {
        this.columnMetaData = columnMetaData;
        this.columnMetaData.parent = this;
    }

    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        setColumnMetaData(colmd);
        return colmd;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public DatastoreIdentityMetaData setColumnName(String columnName)
    {
        if (!StringUtils.isWhitespace(columnName))
        {
            this.columnName = columnName;
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
        }
        else
        {
            this.columnName = null;
        }
        return this;
    }

    public ValueGenerationStrategy getValueStrategy()
    {
        return strategy;
    }

    public DatastoreIdentityMetaData setValueStrategy(ValueGenerationStrategy strategy)
    {
        this.strategy = strategy;
        return this;
    }

    public String getSequence()
    {
        return sequence;
    }

    public DatastoreIdentityMetaData setSequence(String sequence)
    {
        this.sequence = StringUtils.isWhitespace(sequence) ? null : sequence;
        if (this.sequence != null && this.strategy == null)
        {
            // JDO Section 18.6.1. No strategy, but sequence defined means use "sequence"
            this.strategy = ValueGenerationStrategy.SEQUENCE;
        }
        return this;
    }

    public String getValueGeneratorName()
    {
        return valueGeneratorName;
    }

    public DatastoreIdentityMetaData setValueGeneratorName(String generator)
    {
        this.valueGeneratorName = StringUtils.isWhitespace(generator) ? null : generator;
        return this;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("DatastoreIdentityMetaData[");
        str.append("strategy=").append(strategy);
        if (columnMetaData != null)
        {
            str.append(", column=").append(columnName);
        }
        if (sequence != null)
        {
            str.append(", sequence=").append(sequence);
        }
        if (valueGeneratorName != null)
        {
            str.append(", valueGeneratorName=").append(valueGeneratorName);
        }
        str.append("]");
        return str.toString();
    }
}