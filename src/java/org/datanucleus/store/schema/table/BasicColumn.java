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
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.store.StoreManager;

/**
 * Representation of a column in a table.
 */
public class BasicColumn implements Column
{
    Table table;

    ColumnMetaData colmd;

    String identifier;

    /** Optional member metadata. Will be null when this is for datastore-id, or version etc. */
    AbstractMemberMetaData mmd;

    public BasicColumn(Table tbl, StoreManager storeMgr, ColumnMetaData colmd)
    {
        this.table = tbl;
        this.colmd = colmd;
        this.identifier = colmd.getName();
    }

    public Table getTable()
    {
        return table;
    }

    public ColumnMetaData getColumnMetaData()
    {
        return colmd;
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

    public String toString()
    {
        return "Column " + identifier;
    }
}