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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.DiscriminatorMetaData;
import org.datanucleus.metadata.EmbeddedMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.schema.naming.ColumnType;

/**
 * Representation of a table for a class where the class is stored in "complete-table" inheritance (or in JPA "TablePerClass")
 * whereby all members (in this class and superclasses) are handled in this table. Also assumes that any persistable fields
 * and collection/map fields are stored in this table (i.e not usable where you have foreign keys in the datastore).
 * Currently assumes one column per basic member.
 */
public class CompleteClassTable implements Table
{
    StoreManager storeMgr;

    AbstractClassMetaData cmd;

    String identifier;

    /** Map of DatastoreColumn, keyed by the member of the class. TODO Support multiple columns per member. */
    Map<AbstractMemberMetaData, BasicColumn> columnByMember = new HashMap<AbstractMemberMetaData, BasicColumn>();

    /** Map of DatastoreColumn, keyed by the position (starting at 0 and increasing). */
    Map<Integer, BasicColumn> columnByPosition = new HashMap<Integer, BasicColumn>();

    public CompleteClassTable(StoreManager storeMgr, AbstractClassMetaData cmd)
    {
        this.storeMgr = storeMgr;
        this.cmd = cmd;

        if (cmd.getTable() != null)
        {
            this.identifier = cmd.getTable();
        }
        else
        {
            this.identifier = storeMgr.getNamingFactory().getTableName(cmd);
        }

        List<BasicColumn> columns = new ArrayList<BasicColumn>();
        ClassLoaderResolver clr = storeMgr.getNucleusContext().getClassLoaderResolver(null);
        int numMembers = cmd.getAllMemberPositions().length;
        for (int i=0;i<numMembers;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(i);
            RelationType relationType = mmd.getRelationType(clr);
            if (relationType == RelationType.NONE)
            {
                processBasicMember(columns, mmd);
            }
            else if (mmd.isEmbedded())
            {
                processEmbeddedMember(columns, mmd, clr);
            }
            else
            {
                processBasicMember(columns, mmd);
            }
        }

        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            // Add datastore-identity column
            ColumnMetaData colmd = cmd.getIdentityMetaData().getColumnMetaData();
            if (colmd == null || colmd.getName() == null)
            {
                colmd = (colmd != null ? new ColumnMetaData(colmd) : new ColumnMetaData());                    
                colmd.setName(storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.DATASTOREID_COLUMN));
            }
            BasicColumn col = new BasicColumn(this, storeMgr, colmd);
            columns.add(col);
        }

        if (cmd.isVersioned())
        {
            // Add version column
            VersionMetaData vermd = cmd.getVersionMetaDataForClass();
            ColumnMetaData colmd = vermd.getColumnMetaData();
            if (colmd == null || colmd.getName() == null)
            {
                colmd = (colmd != null ? new ColumnMetaData(colmd) : new ColumnMetaData());
                colmd.setName(storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.VERSION_COLUMN));
            }
            BasicColumn col = new BasicColumn(this, storeMgr, colmd);
            columns.add(col);
        }

        if (cmd.hasDiscriminatorStrategy())
        {
            // Add discriminator column
            DiscriminatorMetaData dismd = cmd.getDiscriminatorMetaDataRoot();
            ColumnMetaData colmd = dismd.getColumnMetaData();
            if (colmd == null || cmd.getDiscriminatorColumnName() == null)
            {
                colmd = (colmd != null ? new ColumnMetaData(colmd) : new ColumnMetaData());
                colmd.setName(storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.DISCRIMINATOR_COLUMN));
            }
            BasicColumn col = new BasicColumn(this, storeMgr, colmd);
            columns.add(col);
        }

        if (storeMgr.getStringProperty(PropertyNames.PROPERTY_MAPPING_TENANT_ID) != null)
        {
            // Multitenancy discriminator present : Add restriction for this tenant
            if ("true".equalsIgnoreCase(cmd.getValueForExtension("multitenancy-disable")))
            {
                // Don't bother with multitenancy for this class
            }
            else
            {
                ColumnMetaData colmd = new ColumnMetaData();
                colmd.setName(storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.MULTITENANCY_COLUMN));
                BasicColumn col = new BasicColumn(this, storeMgr, colmd);
                columns.add(col);
            }
        }

        int numCols = columns.size();

        // First pass to populate those with column position defined
        Iterator<BasicColumn> colIter = columns.iterator();
        while (colIter.hasNext())
        {
            BasicColumn col = colIter.next();
            ColumnMetaData colmd = col.getColumnMetaData();
            Integer pos = colmd.getPosition();
            if (pos != null)
            {
                if (columnByPosition.containsKey(pos))
                {
                    BasicColumn col2 = columnByPosition.get(pos);
                    throw new NucleusUserException("Table " + identifier + " has column " + col.getIdentifier() +
                        " specified to have column position " + pos +
                        " yet that position is also defined for column " + col2.identifier);
                }
                else if (pos >= numCols)
                {
                    throw new NucleusUserException("Table " + identifier + " has column " + col.getIdentifier() +
                        " specified to have position " + pos +
                        " yet the number of columns is " + numCols + "." +
                    " Column positions should be from 0 and have no gaps");
                }
                columnByPosition.put(colmd.getPosition(), col);
                colIter.remove();
            }
        }
        if (!columns.isEmpty())
        {
            int pos = 0;
            for (BasicColumn col : columns)
            {
                // Find the next position available
                while (true)
                {
                    if (!columnByPosition.containsKey(pos))
                    {
                        break;
                    }
                    pos++;
                }
                columnByPosition.put(pos, col);
            }
        }
    }

    protected void processBasicMember(List<BasicColumn> cols, AbstractMemberMetaData mmd)
    {
        ColumnMetaData colmd = null;

        ColumnMetaData[] colmds = mmd.getColumnMetaData();
        if (colmds == null || colmds.length == 0)
        {
            colmd = new ColumnMetaData();
        }
        else if (colmds.length > 1)
        {
            // TODO Handle member with multiple columns
            throw new NucleusUserException("Dont currently support member having more than 1 column");
        }
        else
        {
            colmd = colmds[0];
        }
        if (colmd.getName() == null)
        {
            colmd.setName(storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0));
        }

        BasicColumn col = new BasicColumn(this, storeMgr, colmd);
        col.setMemberMetaData(mmd);
        cols.add(col);
        columnByMember.put(mmd, col);
    }

    protected void processEmbeddedMember(List<BasicColumn> cols, AbstractMemberMetaData ownerMmd,
            ClassLoaderResolver clr)
    {
        // TODO This needs updating to work like the Cassandra plugin SchemaHandler does, since it makes a more complete job
        EmbeddedMetaData emd = ownerMmd.getEmbeddedMetaData();
        AbstractClassMetaData embCmd = storeMgr.getNucleusContext().getMetaDataManager().getMetaDataForClass(ownerMmd.getType(), clr);
        AbstractMemberMetaData[] embMmds = emd.getMemberMetaData();
        for (int i=0;i<embMmds.length;i++)
        {
            AbstractMemberMetaData mmd = embCmd.getMetaDataForMember(embMmds[i].getName());
            if (embMmds[i].getEmbeddedMetaData() == null)
            {
                // Non-embedded field so process
                processBasicMember(cols, embMmds[i]);
            }
            else
            {
                // Nested embedded field
                RelationType relationType = mmd.getRelationType(clr);
                if (RelationType.isRelationSingleValued(relationType))
                {
                    processEmbeddedMember(cols, mmd, clr);
                }
                else
                {
                    // TODO Handle embedded collections/maps
                    throw new NucleusUserException("Dont currently support embedded collections for this datastore");
                }
            }
        }
    }

    public AbstractClassMetaData getClassMetaData()
    {
        return cmd;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public int getNumberOfColumns()
    {
        return columnByPosition.size();
    }

    public BasicColumn getColumnForPosition(int pos)
    {
        return columnByPosition.get(pos);
    }

    public BasicColumn getColumnForMember(AbstractMemberMetaData mmd)
    {
        return columnByMember.get(mmd);
    }
}