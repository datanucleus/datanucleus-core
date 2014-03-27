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

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.schema.naming.ColumnType;

/**
 * Representation of a column in a table.
 */
public class ColumnImpl implements Column
{
    Table table;

    ColumnType columnType = ColumnType.COLUMN;

    String identifier;

    boolean primaryKey;

    String typeName;

    int position = -1;

    /** Member metadata when this column is for a field/property. Will be null when this is for datastore-id, or version etc. */
    AbstractMemberMetaData mmd;

    public ColumnImpl(Table tbl, String identifier, ColumnType colType)
    {
        this.table = tbl;
        this.identifier = identifier;
        this.columnType = colType;
    }

    public Table getTable()
    {
        return table;
    }

    public AbstractMemberMetaData getMemberMetaData()
    {
        return mmd;
    }

    public void setMemberMetaData(AbstractMemberMetaData mmd)
    {
        this.mmd = mmd;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public boolean isPrimaryKey()
    {
        return primaryKey;
    }

    public void setPrimaryKey()
    {
        this.primaryKey = true;
    }

    public ColumnType getColumnType()
    {
        return columnType;
    }

    public void setTypeName(String type)
    {
        this.typeName = type;
    }
    public String getTypeName()
    {
        return typeName;
    }

    public void setPosition(int pos)
    {
        this.position = pos;
    }
    public int getPosition()
    {
        return position;
    }

    public String toString()
    {
        if (mmd != null)
        {
            return "Column: " + identifier + " member=" + mmd.getFullFieldName() + (primaryKey ? " (PK)" : "") + (position >= 0 ? (" [" + position + "]") : "");
        }
        return "Column : " + identifier + " type=" + columnType + (primaryKey ? " (PK)" : "") + (position >= 0 ? (" [" + position + "]") : "");
    }
}