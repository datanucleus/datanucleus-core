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
import org.datanucleus.metadata.VersionMetaData;

/**
 * Naming factory following JPA specification rules.
 * Refer to DataNucleus docs, but the rules are as follows
 * <ul>
 * <li>Class called "MyClass" will generate table name of "MYCLASS"</li>
 * <li>Field called "myField" will generate column name of "MYFIELD"</li>
 * <li>Datastore-identity column for class "MyClass" will be "MYCLASS_ID" (not part of JPA)</li>
 * <li>Join table will be named after the ownerClass and the otherClass so "MyClass" joining to "MyOtherClass"
 * will have a join table called "MYCLASS_MYOTHERCLASS"</li>
 * <li>1-N uni between "MyClass" (field="myField") and "MyElement" will have FK in "MYELEMENT" of MYFIELD_MYCLASS_ID</li>
 * <li>1-N bi between "MyClass" (field="myField") and "MyElement" (field="myClassRef") will have FK in "MYELEMENT"
 * of name "MYCLASSREF_MYCLASS_ID".</li>
 * <li>1-1 uni between "MyClass" (field="myField") and "MyElement" will have FK in "MYCLASS" of name "MYFIELD_MYELEMENT_ID"</li>
 * <li>Discriminator field columns will, by default, be called "DTYPE"</li>
 * <li>Index field columns will, for field "myField", be called "MYFIELD_ORDER"</li>
 * <li>Version field columns will, by default, be called "VERSION"</li>
 * <li>Adapter index field columns will, by default, be called "IDX"</li>
 * <li>Index names will, by default, be called "{class}_{field}_IDX" or "{class}_{position}_IDX"</li>
 * </ul>
 */
public class JPANamingFactory extends AbstractNamingFactory
{
    public JPANamingFactory(NucleusContext nucCtx)
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
                // Join table name specified at this side of bidir relation
                name = mmd.getTable();
                // TODO This may have "catalog.schema.name"
            }
            else
            {
                // Join table name specified at other side of bidir relation
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
            String otherClass = mmd.getTypeName();
            if (mmd.hasCollection())
            {
                otherClass = mmd.getCollection().getElementType();
            }
            else if (mmd.hasArray())
            {
                otherClass = mmd.getArray().getElementType();
            }
            else if (mmd.hasMap())
            {
                otherClass = mmd.getMap().getValueType();
            }

            if (mmd.hasCollection() && relatedMmds != null && relatedMmds[0].hasCollection() && mmd.getMappedBy() != null)
            {
                // M-N collection and the owner is the other side
                ownerClass = relatedMmds[0].getClassName(false);
                otherClass = relatedMmds[0].getCollection().getElementType();
            }

            otherClass = otherClass.substring(otherClass.lastIndexOf('.')+1);
            name = ownerClass + wordSeparator + otherClass;
        }

        // Apply any truncation necessary
        int maxLength = getMaximumLengthForComponent(SchemaComponent.TABLE);
        if (maxLength > 0 && name.length() > maxLength)
        {
            name = truncate(name, maxLength);
        }

        // Apply any case and quoting
        name = getNameInRequiredCase(name);

        return name;
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
                name = "DTYPE";
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
                ColumnMetaData idcolmd = cmd.getIdentityMetaData().getColumnMetaData();
                if (idcolmd != null)
                {
                    name = idcolmd.getName();
                }
            }
            if (name == null)
            {
                name = cmd.getName() + wordSeparator + "ID";
            }
        }
        else if (type == ColumnType.MULTITENANCY_COLUMN)
        {
            if (cmd.hasExtension("multitenancy-column-name"))
            {
                name = cmd.getValueForExtension("multitenancy-column-name");
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

        return prepareColumnNameForUse(name);
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
                name = mmd.getName() + wordSeparator + "ORDER";
            }
        }
        else if (type == ColumnType.ADAPTER_COLUMN)
        {
            name = "IDX";
        }
        // TODO Add FK column, join owner column etc
        else
        {
            throw new NucleusException("This method does not support columns of type " + type);
        }

        return prepareColumnNameForUse(name);
    }
}