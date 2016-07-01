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

import java.util.HashMap;
import java.util.Map;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.StringUtils;

/**
 * Mapping definition for a member (field/property) representing the column(s) that it maps to.
 * This class is required as an intermediary between Table and Column so that we can support mapping a member to multiple columns.
 */
public class MemberColumnMappingImpl implements MemberColumnMapping
{
    protected AbstractMemberMetaData mmd;

    protected TypeConverter typeConverter;

    protected Map<FieldRole, TypeConverter> componentConverters;

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

    public void setTypeConverterForComponent(FieldRole role, TypeConverter conv)
    {
        if (this.typeConverter != null)
        {
            // TODO Is this correct, or will there sometimes be a TypeConverter and we need to remove it if converting a collection element for example
            throw new NucleusException("Cannot set a component converter for " + mmd.getFullFieldName() + " since already has a TypeConverter defined for the field");
        }
        if (role == FieldRole.ROLE_COLLECTION_ELEMENT && !mmd.hasCollection())
        {
            throw new NucleusException("Cannot set a TypeConverter for the collection element on member " + mmd.getFullFieldName() + " since no collection");
        }
        if ((role == FieldRole.ROLE_MAP_KEY || role == FieldRole.ROLE_MAP_VALUE) && !mmd.hasMap())
        {
            throw new NucleusException("Cannot set a TypeConverter for the map key/value on member " + mmd.getFullFieldName() + " since no map");
        }

        if (componentConverters == null)
        {
            componentConverters = new HashMap();
        }
        componentConverters.put(role, conv);
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
        return columns != null ? columns.length : 0;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.MemberColumnMapping#getTypeConverter()
     */
    @Override
    public TypeConverter getTypeConverter()
    {
        return typeConverter;
    }

    public TypeConverter getTypeConverterForComponent(FieldRole role)
    {
        if (componentConverters != null)
        {
            return componentConverters.get(role);
        }
        return null;
    }

    public String toString()
    {
        return "Member: " + mmd.getFullFieldName() + " converter=" + typeConverter + " columns=" + StringUtils.objectArrayToString(columns);
    }
}