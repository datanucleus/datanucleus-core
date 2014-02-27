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
import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Representation of a column in a table.
 */
public class ColumnImpl implements Column
{
    Table table;

    ColumnMetaData colmd;

    String identifier;

    String type; // TODO Provide way of setting this

    TypeConverter typeConverter = null;

    int position = -1;

    /** Member metadata when this column is for a field/property. Will be null when this is for datastore-id, or version etc. */
    AbstractMemberMetaData mmd;

    public ColumnImpl(Table tbl, String identifier)
    {
        this.table = tbl;
        this.identifier = identifier;
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

    public String getType()
    {
        return type;
    }

    public void setTypeConverter(TypeConverter conv)
    {
        this.typeConverter = conv;
    }

    public TypeConverter getTypeConverter()
    {
        return typeConverter;
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
        return "Column " + identifier;
    }
}