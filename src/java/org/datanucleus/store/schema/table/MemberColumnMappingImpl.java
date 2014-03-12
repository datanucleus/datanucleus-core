/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Mapping definition for a member (field/property) representing the column(s) that it maps to.
 * This class is required as an intermediary between Table and Column so that we can support mapping a member to multiple columns.
 */
public class MemberColumnMappingImpl implements MemberColumnMapping
{
    protected AbstractMemberMetaData mmd;

    protected TypeConverter typeConverter;

    protected Column[] columns;

    public MemberColumnMappingImpl(AbstractMemberMetaData mmd, Column col)
    {
        this.mmd = mmd;
        this.columns = new Column[]{col};
    }

    public MemberColumnMappingImpl(AbstractMemberMetaData mmd, Column[] cols, TypeConverter typeConv)
    {
        this.mmd = mmd;
        this.columns = cols;
        this.typeConverter = typeConv;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.MemberColumnMapping#setTypeConverter(org.datanucleus.store.types.converters.TypeConverter)
     */
    @Override
    public void setTypeConverter(TypeConverter typeConv)
    {
        this.typeConverter = typeConv;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.MemberColumnMapping#getMemberMetaData()
     */
    @Override
    public AbstractMemberMetaData getMemberMetaData()
    {
        return mmd;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.MemberColumnMapping#getColumn(int)
     */
    @Override
    public Column getColumn(int position)
    {
        if (position >= getNumberOfColumns())
        {
            return null;
        }
        return columns[position];
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.MemberColumnMapping#getColumns()
     */
    @Override
    public Column[] getColumns()
    {
        return columns;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.MemberColumnMapping#getNumberOfColumns()
     */
    @Override
    public int getNumberOfColumns()
    {
        return (columns != null ? columns.length : 0);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.MemberColumnMapping#getTypeConverter()
     */
    @Override
    public TypeConverter getTypeConverter()
    {
        return typeConverter;
    }
}