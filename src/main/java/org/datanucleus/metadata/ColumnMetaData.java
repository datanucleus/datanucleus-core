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

import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Representation of the Meta-Data for a column mapping of a field.
 */
public class ColumnMetaData extends MetaData
{
    private static final long serialVersionUID = -751430163728764079L;

    /** column name. */
    protected String name;

    /** target column name (for matching across a FK). */
    protected String target;

    /** target field/property name (for matching across a FK). */
    protected String targetMember;

    /** jdbc-type to use (if any). */
    protected JdbcType jdbcType;

    /** sql-type to use (if any). Takes priority over jdbc-type. */
    protected String sqlType;

    /** length to use (if any). Also known as "precision" when for floating point types. */
    protected Integer length;

    /** scale to use (if any). */
    protected Integer scale;

    /** Whether the column accepts nulls. */
    protected Boolean allowsNull;

    /** Default value for the column (when constructing the table with this column). */
    protected String defaultValue;

    /** value to use when inserting this column in the datastore (the column is not mapped to a field/property) */
    protected String insertValue;

    /** Whether this column is to be inserted when the owning object is inserted. TODO Merge this with AbstractMemberMetaData.isInsertable. */
    protected boolean insertable = true;

    /** Whether this column can be updated when the owning object is updated. TODO Merge this with AbstractMemberMetaData.isUpdateable. */
    protected boolean updateable = true;

    /** Whether this column should be marked as UNIQUE. */
    protected boolean unique = false;

    /** Optional column DDL appended to the column definition defined by DataNucleus. */
    protected String columnDdl = null;

    /** Column position for the table as a whole (0-origin). */
    protected Integer position = null;

    /**
     * Creates a ColumnMetaData by copying contents from <code>colmd</code>.
     * @param colmd MetaData for the column
     */
    public ColumnMetaData(final ColumnMetaData colmd)
    {
        super(null, colmd);
        name = colmd.getName();
        target = colmd.getTarget();
        targetMember = colmd.getTargetMember();
        jdbcType = colmd.getJdbcType();
        sqlType = colmd.getSqlType();
        length = colmd.getLength();
        scale = colmd.getScale();
        allowsNull = colmd.allowsNull;
        defaultValue = colmd.getDefaultValue();
        insertValue = colmd.getInsertValue();
        insertable = colmd.getInsertable();
        updateable = colmd.getUpdateable();
        unique = colmd.getUnique();
        position = colmd.getPosition();
    }

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public ColumnMetaData()
    {
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public ColumnMetaData setDefaultValue(String defaultValue)
    {
        this.defaultValue = StringUtils.isWhitespace(defaultValue) ? null : defaultValue;
        return this;
    }

    public String getColumnDdl()
    {
        return columnDdl;
    }

    public void setColumnDdl(String columnDdl)
    {
        this.columnDdl = columnDdl;
    }

    public boolean getInsertable()
    {
        return insertable;
    }

    public ColumnMetaData setInsertable(boolean insertable)
    {
        this.insertable = insertable;
        return this;
    }

    public ColumnMetaData setInsertable(String insertable)
    {
        if (!StringUtils.isWhitespace(insertable))
        {
            this.insertable = Boolean.parseBoolean(insertable);
        }
        return this;
    }

    public String getInsertValue()
    {
        return insertValue;
    }

    public ColumnMetaData setInsertValue(String insertValue)
    {
        this.insertValue = StringUtils.isWhitespace(insertValue) ? null : insertValue;
        return this;
    }

    public JdbcType getJdbcType()
    {
        return jdbcType;
    }

    public String getJdbcTypeName()
    {
        return jdbcType != null ? jdbcType.toString() : null;
    }
    public ColumnMetaData setJdbcType(JdbcType type)
    {
        this.jdbcType = type;
        return this;
    }

    public ColumnMetaData setJdbcType(String jdbcTypeName)
    {
        if (StringUtils.isWhitespace(jdbcTypeName))
        {
            jdbcType = null;
        }
        else
        {
            try
            {
                jdbcType = Enum.valueOf(JdbcType.class, jdbcTypeName.toUpperCase());
            }
            catch (IllegalArgumentException iae)
            {
                NucleusLogger.METADATA.warn("Metadata has jdbc-type of '" + jdbcTypeName + "' yet this is not valid. Ignored");
            }
        }
        return this;
    }

    public Integer getLength()
    {
        return length;
    }

    public ColumnMetaData setLength(Integer length)
    {
        if (length != null && length.intValue() > 0)
        {
            this.length = length;
        }
        return this;
    }

    public ColumnMetaData setLength(String length)
    {
        if (!StringUtils.isWhitespace(length))
        {
            try
            {
                int val = Integer.parseInt(length);
                if (val > 0)
                {
                    this.length = val;
                }
            }
            catch (NumberFormatException nfe)
            {
            }
        }
        return this;
    }

    public String getName()
    {
        return name;
    }

    public ColumnMetaData setName(String name)
    {
        this.name = StringUtils.isWhitespace(name) ? null : name;
        return this;
    }

    public Integer getScale()
    {
        return scale;
    }

    public ColumnMetaData setScale(Integer scale)
    {
        if (scale != null && scale.intValue() > 0)
        {
            this.scale = scale;
        }
        return this;
    }

    public ColumnMetaData setScale(String scale)
    {
        if (!StringUtils.isWhitespace(scale))
        {
            try
            {
                int val = Integer.parseInt(scale);
                if (val > 0)
                {
                    this.scale = val;
                }
            }
            catch (NumberFormatException nfe)
            {
            }
        }
        return this;
    }

    public String getSqlType()
    {
        return sqlType;
    }

    public ColumnMetaData setSqlType(String sqlType)
    {
        this.sqlType = StringUtils.isWhitespace(sqlType) ? null : sqlType;
        return this;
    }

    public String getTarget()
    {
        return target;
    }

    public ColumnMetaData setTarget(String target)
    {
        this.target = StringUtils.isWhitespace(target) ? null : target;
        return this;
    }

    public String getTargetMember()
    {
        return targetMember;
    }

    public ColumnMetaData setTargetMember(String targetMember)
    {
        this.targetMember = StringUtils.isWhitespace(targetMember) ? null : targetMember;
        return this;
    }

    public Integer getPosition()
    {
        return position;
    }

    public ColumnMetaData setPosition(int pos)
    {
        if (pos >= 0)
        {
            this.position = pos;
        }
        else
        {
            this.position = null;
        }

        return this;
    }

    public ColumnMetaData setPosition(String pos)
    {
        if (!StringUtils.isWhitespace(pos))
        {
            try
            {
                int val = Integer.parseInt(pos);
                if (val >= 0)
                {
                    this.position = val;
                }
            }
            catch (NumberFormatException nfe)
            {
            }
        }
        return this;
    }

    public boolean getUnique()
    {
        return unique;
    }

    public ColumnMetaData setUnique(boolean unique)
    {
        this.unique = unique;
        return this;
    }

    public ColumnMetaData setUnique(String unique)
    {
        if (!StringUtils.isWhitespace(unique))
        {
            this.unique = Boolean.parseBoolean(unique);
        }
        return this;
    }

    public boolean getUpdateable()
    {
        return updateable;
    }

    public ColumnMetaData setUpdateable(boolean updateable)
    {
        this.updateable = updateable;
        return this;
    }

    public ColumnMetaData setUpdateable(String updateable)
    {
        if (!StringUtils.isWhitespace(updateable))
        {
            this.updateable = Boolean.parseBoolean(updateable);
        }
        return this;
    }

    public boolean isAllowsNull()
    {
        if (allowsNull == null)
        {
            return false;
        }
        return allowsNull.booleanValue();
    }

    public Boolean getAllowsNull()
    {
        return allowsNull;
    }

    public ColumnMetaData setAllowsNull(Boolean allowsNull)
    {
        this.allowsNull = allowsNull;
        return this;
    }

    public ColumnMetaData setAllowsNull(String allowsNull)
    {
        if (!StringUtils.isWhitespace(allowsNull))
        {
            this.allowsNull = Boolean.parseBoolean(allowsNull);
        }
        return this;
    }
}
