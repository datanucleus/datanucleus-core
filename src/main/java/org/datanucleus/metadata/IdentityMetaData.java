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
 * Meta-Data for the datastore-identity of a class. Provides a surrogate datastore field.
 * Also defines the generation strategy for the identity values.
 */
public class IdentityMetaData extends MetaData
{
    private static final long serialVersionUID = 4740941674001139996L;

    /** column name value.  */
    protected String columnName;

    /** Metadata for column. */
    protected ColumnMetaData columnMetaData;

    /** strategy tag value. */
    protected IdentityStrategy strategy = IdentityStrategy.NATIVE;

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

    public IdentityMetaData setColumnName(String columnName)
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

    public IdentityStrategy getValueStrategy()
    {
        return strategy;
    }

    public IdentityMetaData setValueStrategy(IdentityStrategy strategy)
    {
        this.strategy = strategy;
        return this;
    }

    public String getSequence()
    {
        return sequence;
    }

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

    public String getValueGeneratorName()
    {
        return valueGeneratorName;
    }

    public IdentityMetaData setValueGeneratorName(String generator)
    {
        this.valueGeneratorName = (StringUtils.isWhitespace(generator) ? null : generator);
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
        StringBuilder sb = new StringBuilder();
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