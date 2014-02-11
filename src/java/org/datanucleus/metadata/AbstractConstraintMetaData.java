/**********************************************************************
 Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract representation of an ORM constraint.
 */
public class AbstractConstraintMetaData extends MetaData
{
    /** the constraint name */
    protected String name;

    /** the constraint table name. Name of the table to which this applies (null implies the enclosing class' table). */
    protected String table;

    /** The member names for this constraint. */
    protected List<String> memberNames = null;

    /** The column names for this constraint. */
    protected List<String> columnNames = null;

    /**
     * Default constructor. Set fields using setters before populate().
     */
    public AbstractConstraintMetaData()
    {
    }

    /**
     * Copy constructor.
     */
    public AbstractConstraintMetaData(AbstractConstraintMetaData acmd)
    {
        super(null, acmd);
        this.name = acmd.name;
        this.table = acmd.table;

        if (acmd.memberNames != null)
        {
            for (String memberName : acmd.memberNames)
            {
                addMember(memberName);
            }
        }
        if (acmd.columnNames != null)
        {
            for (String columnName : acmd.columnNames)
            {
                addColumn(columnName);
            }
        }
    }

    /**
     * Add a new member that is part of this constraint.
     * @param memberName member name for the field/property
     */
    public void addMember(String memberName)
    {
        if (memberNames == null)
        {
            memberNames = new ArrayList<String>();
        }
        memberNames.add(memberName);
    }

    public final String[] getMemberNames()
    {
        if (memberNames == null)
        {
            return null;
        }
        return memberNames.toArray(new String[memberNames.size()]);
    }

    public int getNumberOfMembers()
    {
        return (memberNames != null ? memberNames.size() : 0);
    }

    /**
     * Add a new column that is part of the constraint.
     * @param columnName Name for the column
     */
    public void addColumn(String columnName)
    {
        if (columnNames == null)
        {
            columnNames = new ArrayList<String>();
        }
        columnNames.add(columnName);
    }

    public final String[] getColumnNames()
    {
        if (columnNames == null)
        {
            return null;
        }
        return columnNames.toArray(new String[columnNames.size()]);
    }

    public int getNumberOfColumns()
    {
        return (columnNames != null ? columnNames.size() : 0);
    }
}