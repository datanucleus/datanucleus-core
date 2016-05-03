/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.schema.naming;

import org.datanucleus.NucleusContext;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.VersionMetaData;

/**
 * Naming factory used by DataNucleus v3.x onwards.
 * Refer to DataNucleus docs, but the rules are as follows
 * <ul>
 * <li>Class called "MyClass" will generate table name of "MYCLASS"</li>
 * <li>Field called "myField" will generate column name of "MYFIELD"</li>
 * <li>Datastore id field for class "MyClass" will have be "MYCLASS_ID"</li>
 * <li>Join table will be named after the class and field, so "MyClass" with field "myField" will become
 * a table with name "MYCLASS_MYFIELD".</li>
 * <li>Columns of a join table will be named after the PK fields of the owner and element. So something
 * like "MYCLASS_ID_OID" and "MYELEMENT_ID_EID"</li>
 * <li>Discriminator field columns will, by default, be called "DISCRIMINATOR"</li>
 * <li>Index field columns will, by default, be called "IDX"</li>
 * <li>Version field columns will, by default, be called "VERSION"</li>
 * <li>Adapter index field columns will, by default, be called "IDX"</li>
 * <li>Index names will, by default, be called "{class}_{field}_IDX" or "{class}_{position}_IDX"</li>
 * <li>Sequence names will default to being called "{seqName}_SEQ" where seqName is the 'name' of the SequenceMetaData</li>
 * </ul>
 * <p>
 * Note that in addition to the above rules, 
 * <ul>
 * <li>if there are limitations on length of name for a particular component then the name will be truncated.</li>
 * <li>the name will be changed to match any specified "case" (see setNamingCase)</li>
 * </ul>
 */
public class DN2NamingFactory extends AbstractNamingFactory
{
    public DN2NamingFactory(NucleusContext nucCtx)
    {
        super(nucCtx);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.naming.NamingFactory#getTableName(org.datanucleus.metadata.AbstractMemberMetaData)
     */
    public String getTableName(AbstractMemberMetaData mmd)
    {
        String name = null;
        AbstractMemberMetaData[] relatedMmds = null;
        if (mmd.hasContainer())
        {
            if (mmd.getTable() != null)
            {
                name = mmd.getTable();
                // TODO This may have "catalog.schema.name"
            }
            else
            {
                relatedMmds = mmd.getRelatedMemberMetaData(clr);
                if (relatedMmds != null && relatedMmds[0].getTable() != null)
                {
                    name = relatedMmds[0].getTable();
                    // TODO This may have "catalog.schema.name"
                }
            }
        }
        if (name == null)
        {
            String ownerClass = mmd.getClassName(false);
            name = ownerClass + wordSeparator + mmd.getName();
        }

        // Apply any truncation/casing necessary
        return prepareIdentifierNameForUse(name, SchemaComponent.TABLE);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.naming.NamingFactory#getColumnName(org.datanucleus.metadata.AbstractClassMetaData, org.datanucleus.store.schema.naming.ColumnType)
     */
    public String getColumnName(AbstractClassMetaData cmd, ColumnType type)
    {
        String name = null;
        if (type == ColumnType.DISCRIMINATOR_COLUMN)
        {
            name = cmd.getDiscriminatorColumnName();
            if (name == null)
            {
                name = "DISCRIMINATOR";
            }
        }
        else if (type == ColumnType.VERSION_COLUMN)
        {
            VersionMetaData vermd = cmd.getVersionMetaData();
            if (vermd != null)
            {
                ColumnMetaData colmd = vermd.getColumnMetaData();
                if (colmd != null && colmd.getName() != null)
                {
                    name = colmd.getName();
                }
            }
            if (name == null)
            {
                name = "VERSION";
            }
        }
        else if (type == ColumnType.DATASTOREID_COLUMN)
        {
            if (cmd.getIdentityMetaData() != null)
            {
                ColumnMetaData idcolmds = cmd.getIdentityMetaData().getColumnMetaData();
                if (idcolmds != null)
                {
                    name = idcolmds.getName();
                }
            }
            if (name == null)
            {
                name = cmd.getName() + wordSeparator + "ID";
            }
        }
        else if (type == ColumnType.MULTITENANCY_COLUMN)
        {
            if (cmd.hasExtension(MetaData.EXTENSION_CLASS_MULTITENANCY_COLUMN_NAME))
            {
                name = cmd.getValueForExtension(MetaData.EXTENSION_CLASS_MULTITENANCY_COLUMN_NAME);
            }
            if (name == null)
            {
                name = "TENANT" + wordSeparator + "ID";
            }
        }
        else
        {
            throw new NucleusException("This method does not support columns of type " + type);
        }

        return prepareIdentifierNameForUse(name, SchemaComponent.COLUMN);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.naming.NamingFactory#getColumnName(org.datanucleus.metadata.AbstractMemberMetaData, org.datanucleus.store.schema.naming.ColumnType, int)
     */
    public String getColumnName(AbstractMemberMetaData mmd, ColumnType type, int position)
    {
        String name = null;
        if (type == ColumnType.COLUMN)
        {
            ColumnMetaData[] colmds = mmd.getColumnMetaData();
            if (colmds != null && colmds.length > position)
            {
                name = colmds[position].getName();
            }
            else if (mmd.hasCollection())
            {
                // Try element columnMetaData
                colmds = mmd.getElementMetaData().getColumnMetaData();
                if (colmds != null && colmds.length > position)
                {
                    name = colmds[position].getName();
                }
            }
            if (name == null)
            {
                name = mmd.getName();
            }
        }
        else if (type == ColumnType.INDEX_COLUMN)
        {
            if (mmd.getOrderMetaData() != null)
            {
                ColumnMetaData[] colmds = mmd.getOrderMetaData().getColumnMetaData();
                if (colmds != null && colmds.length > position)
                {
                    name = colmds[position].getName();
                }
            }
            if (name == null)
            {
                name = "IDX";
            }
        }
        else if (type == ColumnType.ADAPTER_COLUMN)
        {
            name = "IDX";
        }
        else if (type == ColumnType.FK_COLUMN)
        {
            // TODO Support this
            throw new NucleusException("This method does not support columns of type " + type);
        }
        else if (type == ColumnType.JOIN_OWNER_COLUMN)
        {
            if (mmd.hasContainer())
            {
                if (mmd.getJoinMetaData() != null)
                {
                    // 1-N/M-N join table
                    ColumnMetaData[] colmds = mmd.getJoinMetaData().getColumnMetaData();
                    if (colmds != null && colmds.length > position)
                    {
                        name = colmds[position].getName();
                    }
                }
                else
                {
                    
                }
            }
            else
            {
                // N-1 TODO Check if this is set
            }
            if (name == null)
            {
                if (mmd.hasContainer())
                {
                    name = mmd.getName() + wordSeparator + "ID_OID";
                }
            }
        }
        else
        {
            throw new NucleusException("This method does not support columns of type " + type);
        }

        return prepareIdentifierNameForUse(name, SchemaComponent.COLUMN);
    }
}