/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.schema.table;

import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.JdbcType;
import org.datanucleus.store.schema.naming.ColumnType;

/**
 * Representation of a column in a table.
 */
public class ColumnImpl implements Column
{
    Table table;

    ColumnType columnType = ColumnType.COLUMN;

    String identifier;

    boolean primaryKey = false;

    boolean nullable = false;

    boolean defaultable = false;

    Object defaultValue = null;

    boolean unique = false;

    JdbcType jdbcType;

    String typeName;

    int position = -1;

    MemberColumnMapping mapping = null;

    ColumnMetaData colmd = null;

    boolean nested = false;

    public ColumnImpl(Table tbl, String identifier, ColumnType colType)
    {
        this.table = tbl;
        this.identifier = identifier;
        this.columnType = colType;
    }

    public boolean isNested()
    {
        return nested;
    }
    public void setNested(boolean nested)
    {
        this.nested = nested;
    }

    public Table getTable()
    {
        return table;
    }

    public MemberColumnMapping getMemberColumnMapping()
    {
        return mapping;
    }

    public void setMemberColumnMapping(MemberColumnMapping mapping)
    {
        this.mapping = mapping;
    }

    public String getName()
    {
        return identifier;
    }

    public boolean isPrimaryKey()
    {
        return primaryKey;
    }

    public Column setPrimaryKey()
    {
        this.primaryKey = true;
        return this;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.Column#setNullable(boolean)
     */
    @Override
    public Column setNullable(boolean flag)
    {
        this.nullable = flag;
        return this;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.Column#isNullable()
     */
    @Override
    public boolean isNullable()
    {
        return nullable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.Column#setDefaultable(java.lang.Object)
     */
    @Override
    public Column setDefaultable(Object defaultValue)
    {
        this.defaultable = true;
        this.defaultValue = defaultValue; // TODO Convert to required type
        return this;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.Column#isDefaultable()
     */
    @Override
    public boolean isDefaultable()
    {
        return defaultable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.Column#getDefaultValue()
     */
    @Override
    public Object getDefaultValue()
    {
        return defaultValue;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.Column#setUnique(boolean)
     */
    @Override
    public Column setUnique(boolean flag)
    {
        this.unique = flag;
        return this;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.Column#isUnique()
     */
    @Override
    public boolean isUnique()
    {
        return unique;
    }

    public ColumnType getColumnType()
    {
        return columnType;
    }

    public Column setJdbcType(JdbcType type)
    {
        this.jdbcType = type;
        return this;
    }
    public JdbcType getJdbcType()
    {
        return jdbcType;
    }

    public Column setTypeName(String type)
    {
        this.typeName = type;
        return this;
    }
    public String getTypeName()
    {
        return typeName;
    }

    public Column setPosition(int pos)
    {
        this.position = pos;
        return this;
    }
    public int getPosition()
    {
        return position;
    }

    public Column setColumnMetaData(ColumnMetaData md)
    {
        this.colmd = md;
        if (md != null && md.getDefaultValue() != null)
        {
        	setDefaultable(md.getDefaultValue());
        }
        return this;
    }

    public ColumnMetaData getColumnMetaData()
    {
        return colmd;
    }

    public String toString()
    {
        if (mapping != null)
        {
            return "Column: " + identifier + " member=" + mapping.getMemberMetaData().getFullFieldName() + (primaryKey ? " (PK)" : "") + (position >= 0 ? (" [" + position + "]") : "");
        }
        return "Column : " + identifier + " type=" + columnType + (primaryKey ? " (PK)" : "") + (position >= 0 ? (" [" + position + "]") : "");
    }
}