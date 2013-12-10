/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
 * Representation of the MetaData of a TableGenerator (JPA).
 */
public class TableGeneratorMetaData extends MetaData
{
    /** Name under which this table generator is known. */
    protected final String name;

    /** Name of the table to use for sequences */
    protected String tableName;

    /** Name of the catalog to use for the table */
    protected String catalogName;

    /** Name of the schema to use for the table */
    protected String schemaName;

    /** Name of the primary-key column name */
    protected String pkColumnName;

    /** Name of the value column name */
    protected String valueColumnName;

    /** Name of the primary-key column value */
    protected String pkColumnValue;

    /** Initial value in the table. */
    protected long initialValue = 0;

    /** Allocation size for ids from the table. */
    protected long allocationSize = 50;

    /**
     * Constructor. Create via PackageMetaData.newTableGeneratorMetadata(...)
     * @param name The generator name
     */
    TableGeneratorMetaData(final String name)
    {
        if (StringUtils.isWhitespace(name))
        {
            throw new InvalidMetaDataException(LOCALISER, "044155");
        }

        this.name = name;
    }

    /**
     * Convenience accessor for the fully-qualified name of the sequence.
     * @return Fully qualfiied name of the sequence (including the package name).
     */
    public String getFullyQualifiedName()
    {
        PackageMetaData pmd = (PackageMetaData)getParent();
        return pmd.getName() + "." + name;
    }

    public String getName()
    {
        return name;
    }

    public String getTableName()
    {
        return tableName;
    }

    public TableGeneratorMetaData setTableName(String tableName)
    {
        this.tableName = (StringUtils.isWhitespace(tableName) ? null : tableName);
        return this;
    }

    public String getCatalogName()
    {
        return catalogName;
    }

    public TableGeneratorMetaData setCatalogName(String catalogName)
    {
        this.catalogName = (StringUtils.isWhitespace(catalogName) ? null : catalogName);
        return this;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public TableGeneratorMetaData setSchemaName(String schemaName)
    {
        this.schemaName = (StringUtils.isWhitespace(schemaName) ? null : schemaName);
        return this;
    }

    public String getPKColumnName()
    {
        return pkColumnName;
    }

    public TableGeneratorMetaData setPKColumnName(String pkColumnName)
    {
        this.pkColumnName = (StringUtils.isWhitespace(pkColumnName) ? null : pkColumnName);
        return this;
    }

    public String getValueColumnName()
    {
        return valueColumnName;
    }

    public TableGeneratorMetaData setValueColumnName(String valueColumnName)
    {
        this.valueColumnName = (StringUtils.isWhitespace(valueColumnName) ? null : valueColumnName);
        return this;
    }

    public String getPKColumnValue()
    {
        return pkColumnValue;
    }

    public TableGeneratorMetaData setPKColumnValue(String pkColumnValue)
    {
        this.pkColumnValue = (StringUtils.isWhitespace(pkColumnValue) ? null : pkColumnValue);
        return this;
    }

    public long getInitialValue()
    {
        return initialValue;
    }

    public TableGeneratorMetaData setInitialValue(long initialValue)
    {
        this.initialValue = initialValue;
        return this;
    }

    public TableGeneratorMetaData setInitialValue(String initialValue)
    {
        if (!StringUtils.isWhitespace(initialValue))
        {
            try
            {
                this.initialValue = Integer.parseInt(initialValue);
            }
            catch (NumberFormatException nfe)
            {
            }
        }
        return this;
    }

    public long getAllocationSize()
    {
        return allocationSize;
    }

    public TableGeneratorMetaData setAllocationSize(long allocationSize)
    {
        this.allocationSize = allocationSize;
        return this;
    }

    public TableGeneratorMetaData setAllocationSize(String allocationSize)
    {
        if (!StringUtils.isWhitespace(allocationSize))
        {
            try
            {
                this.allocationSize = Integer.parseInt(allocationSize);
            }
            catch (NumberFormatException nfe)
            {
            }
        }
        return this;
    }

    /**
     * Returns a string representation of the object.
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix, String indent)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<table-generator name=\"" + name + "\"\n");

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix + "</table-generator>\n");
        return sb.toString();
    }
}