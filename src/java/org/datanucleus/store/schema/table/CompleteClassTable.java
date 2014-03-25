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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.DiscriminatorMetaData;
import org.datanucleus.metadata.EmbeddedMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.schema.naming.ColumnType;
import org.datanucleus.store.schema.naming.NamingFactory;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.converters.MultiColumnConverter;
import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

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

    String catalogName;

    String schemaName;

    String identifier;

    List<Column> columns = null;

    Column versionColumn;

    Column discriminatorColumn;

    Column datastoreIdColumn;

    Column multitenancyColumn;

    /** Map of column, keyed by the metadata for the member. */
    Map<AbstractMemberMetaData, Column> columnByMember = new HashMap<AbstractMemberMetaData, Column>();

    /** Map of column, keyed by the navigated path of embedded members. */
    Map<String, Column> columnByEmbeddedMember = new HashMap<String, Column>();

    /** Map of DatastoreColumn, keyed by the position (starting at 0 and increasing). */
    Map<Integer, Column> columnByPosition = new HashMap<Integer, Column>();

    // TODO Drop this
    ColumnAttributer columnAttributer;

    MemberColumnAttributer memberColAttributer;

    public CompleteClassTable(StoreManager storeMgr, AbstractClassMetaData cmd, ColumnAttributer colAttr) // TODO Pass in MemberColumnAttributer
    {
        this.storeMgr = storeMgr;
        this.cmd = cmd;
        this.columnAttributer = colAttr;

        if (cmd.getSchema() != null)
        {
            schemaName = cmd.getSchema();
        }
        else
        {
            schemaName = storeMgr.getStringProperty(PropertyNames.PROPERTY_MAPPING_SCHEMA);
        }
        if (cmd.getCatalog() != null)
        {
            catalogName = cmd.getCatalog();
        }
        else
        {
            catalogName = storeMgr.getStringProperty(PropertyNames.PROPERTY_MAPPING_CATALOG);
        }
        this.identifier = storeMgr.getNamingFactory().getTableName(cmd);

        columns = new ArrayList<Column>();

        TypeManager typeMgr = storeMgr.getNucleusContext().getTypeManager();
        ClassLoaderResolver clr = storeMgr.getNucleusContext().getClassLoaderResolver(null);
        int numMembers = cmd.getAllMemberPositions().length;
        for (int i=0;i<numMembers;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(i);
            if (mmd.getPersistenceModifier() != FieldPersistenceModifier.PERSISTENT)
            {
                // Don't need column if not persistent
                continue;
            }

            RelationType relationType = mmd.getRelationType(clr);
            if (relationType != RelationType.NONE && MetaDataUtils.getInstance().isMemberEmbedded(storeMgr.getMetaDataManager(), clr, mmd, relationType, null))
            {
                if (RelationType.isRelationSingleValued(relationType))
                {
                    NucleusLogger.GENERAL.info(">> CCT mmd=" + mmd.getFullFieldName() + " embedded 1-1");
                    // Embedded PC field, so add columns for all fields of the embedded
                    List<AbstractMemberMetaData> embMmds = new ArrayList<AbstractMemberMetaData>();
                    embMmds.add(mmd);
                    processEmbeddedMember(embMmds, clr);
                }
                else if (RelationType.isRelationMultiValued(relationType))
                {
                    // Embedded Collection/Map/array field. TODO How can we embed this?
                    NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is an embedded collection. Not supported so ignoring");
                    continue;
                }
            }
            else
            {
                ColumnMetaData[] colmds = mmd.getColumnMetaData();
                if (relationType != RelationType.NONE)
                {
                    // 1-1/N-1 stored as single column with persistable-id
                    // 1-N/M-N stored as single column with collection<persistable-id>
                    String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                    ColumnImpl col = addColumn(mmd, colName);
                    if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                    {
                        col.setPosition(colmds[0].getPosition());
                    }
                }
                else
                {
                    TypeConverter typeConv = getTypeConverterForMember(mmd, colmds, typeMgr);
                    if (typeConv != null)
                    {
                        // Create column(s) for this TypeConverter
                        if (typeConv instanceof MultiColumnConverter)
                        {
                            Class[] colJavaTypes = ((MultiColumnConverter)typeConv).getDatastoreColumnTypes();
                            for (int j=0;j<colJavaTypes.length;j++)
                            {
                                String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, j);
                                ColumnImpl col = addColumn(mmd, colName);
                                if (colmds != null && colmds.length == 1 && colmds[j].getPosition() != null)
                                {
                                    col.setPosition(colmds[j].getPosition());
                                }
                            }
                        }
                        else
                        {
                            String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                            ColumnImpl col = addColumn(mmd, colName);
                            if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                            {
                                col.setPosition(colmds[0].getPosition());
                            }
                        }
                    }
                    else
                    {
                        // Create column for basic type
                        String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                        ColumnImpl col = addColumn(mmd, colName);
                        if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                        {
                            col.setPosition(colmds[0].getPosition());
                        }
                    }
                }
            }
        }

        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            // Add surrogate datastore-identity column
            String colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.DATASTOREID_COLUMN);
            ColumnImpl col = addColumn(null, colName, ColumnType.DATASTOREID_COLUMN);
            if (cmd.getIdentityMetaData() != null && cmd.getIdentityMetaData().getColumnMetaData() != null && cmd.getIdentityMetaData().getColumnMetaData().getPosition() != null)
            {
                col.setPosition(cmd.getIdentityMetaData().getColumnMetaData().getPosition());
            }
            this.datastoreIdColumn = col;
        }

        VersionMetaData vermd = cmd.getVersionMetaDataForClass();
        if (vermd != null)
        {
            // Add surrogate version column
            if (vermd != null && vermd.getFieldName() == null)
            {
                String colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.VERSION_COLUMN);
                ColumnImpl col = addColumn(null, colName, ColumnType.VERSION_COLUMN);
                if (vermd.getColumnMetaData() != null && vermd.getColumnMetaData().getPosition() != null)
                {
                    col.setPosition(vermd.getColumnMetaData().getPosition());
                }
                this.versionColumn = col;
            }
        }

        if (cmd.hasDiscriminatorStrategy())
        {
            // Add discriminator column
            String colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.DISCRIMINATOR_COLUMN);
            ColumnImpl col = addColumn(null, colName, ColumnType.DISCRIMINATOR_COLUMN);
            DiscriminatorMetaData dismd = cmd.getDiscriminatorMetaDataForTable();
            if (dismd != null && dismd.getColumnMetaData() != null && dismd.getColumnMetaData().getPosition() != null)
            {
                col.setPosition(dismd.getColumnMetaData().getPosition());
            }
            this.discriminatorColumn = col;
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
                String colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.MULTITENANCY_COLUMN);
                Column col = addColumn(null, colName, ColumnType.MULTITENANCY_COLUMN); // TODO Support column position
                this.multitenancyColumn = col;
            }
        }

        // TODO Generate lookup table by position (assumes that position is specified, otherwise we impose own positioning).
        // First pass to populate those with column position defined
        /*Iterator<ColumnImpl> colIter = columns.iterator();
        while (colIter.hasNext())
        {
            ColumnImpl col = colIter.next();
            ColumnMetaData colmd = col.getColumnMetaData();
            Integer pos = colmd.getPosition();
            if (pos != null)
            {
                if (columnByPosition.containsKey(pos))
                {
                    ColumnImpl col2 = columnByPosition.get(pos);
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
            for (ColumnImpl col : columns)
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
        }*/
    }

    protected TypeConverter getTypeConverterForMember(AbstractMemberMetaData mmd, ColumnMetaData[] colmds, TypeManager typeMgr)
    {
        TypeConverter typeConv = null;
        String typeConvName = mmd.getTypeConverterName();
        if (typeConvName != null)
        {
            // User has specified the TypeConverter
            typeConv = typeMgr.getTypeConverterForName(typeConvName);
        }
        else
        {
            // No explicit TypeConverter so maybe there is an auto-apply converter for this member type
            typeConv = typeMgr.getAutoApplyTypeConverterForType(mmd.getType());
        }

        if (typeConv == null)
        {
            // Try to find a TypeConverter matching any column JDBC type definition
            if (colmds != null && colmds.length > 1)
            {
                // Multiple columns, so try to find a converter with the right number of columns (note we could, in future, check the types of columns also)
                Collection<TypeConverter> converters = typeMgr.getTypeConvertersForType(mmd.getType());
                if (converters != null && !converters.isEmpty())
                {
                    for (TypeConverter conv : converters)
                    {
                        if (conv instanceof MultiColumnConverter && ((MultiColumnConverter)conv).getDatastoreColumnTypes().length == colmds.length)
                        {
                            typeConv = conv;
                            break;
                        }
                    }
                }
                if (typeConv == null)
                {
                    // TODO Throw exception since user column specification leaves no possible converter
                }
            }
            else
            {
                // Single column, so try to match the JDBC type if provided
                String jdbcType = (colmds != null && colmds.length > 0 ? colmds[0].getJdbcType() : null);
                if (!StringUtils.isWhitespace(jdbcType))
                {
                    // JDBC type specified so don't just take the default
                    if (jdbcType.equalsIgnoreCase("varchar") || jdbcType.equalsIgnoreCase("char"))
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), String.class);
                    }
                    else if (jdbcType.equalsIgnoreCase("integer"))
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), Long.class);
                    }
                    else if (jdbcType.equalsIgnoreCase("timestamp"))
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), Timestamp.class);
                    }
                    else if (jdbcType.equalsIgnoreCase("time"))
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), Time.class);
                    }
                    else if (jdbcType.equalsIgnoreCase("date"))
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), Date.class);
                    }
                    // TODO Support other JDBC types
                }
                else
                {
                    // Fallback to default type converter for this member type
                    typeConv = typeMgr.getDefaultTypeConverterForType(mmd.getType());
                }
            }
            // TODO Check that this TypeConverter is supported by the store plugin and throw exception or swap it for some other if not acceptable
        }
        return typeConv;
    }

    protected void processEmbeddedMember(List<AbstractMemberMetaData> mmds, ClassLoaderResolver clr)
    {
        AbstractMemberMetaData lastMmd = mmds.get(mmds.size()-1);
        EmbeddedMetaData embmd = mmds.get(0).getEmbeddedMetaData();
        TypeManager typeMgr = storeMgr.getNucleusContext().getTypeManager();
        MetaDataManager mmgr = storeMgr.getMetaDataManager();
        NamingFactory namingFactory = storeMgr.getNamingFactory();
        AbstractClassMetaData embCmd = mmgr.getMetaDataForClass(lastMmd.getType(), clr);
        int[] memberPositions = embCmd.getAllMemberPositions();
        for (int i=0;i<memberPositions.length;i++)
        {
            AbstractMemberMetaData mmd = embCmd.getMetaDataForManagedMemberAtAbsolutePosition(memberPositions[i]);
            if (mmd.getPersistenceModifier() != FieldPersistenceModifier.PERSISTENT)
            {
                // Don't need column if not persistent
                continue;
            }
            if (mmds.size() == 1 && embmd != null && embmd.getOwnerMember() != null && embmd.getOwnerMember().equals(mmd.getName()))
            {
                // Special case of this being a link back to the owner. TODO Repeat this for nested and their owners
                continue;
            }
            NucleusLogger.GENERAL.info(">> CCT embedded mmd=" + mmd.getFullFieldName() + " field of embedded type=" + mmd.getType());

            RelationType relationType = mmd.getRelationType(clr);
            if (relationType != RelationType.NONE && MetaDataUtils.getInstance().isMemberEmbedded(mmgr, clr, mmd, relationType, lastMmd))
            {
                if (RelationType.isRelationSingleValued(relationType))
                {
                    // Nested embedded PC, so recurse
                    List<AbstractMemberMetaData> embMmds = new ArrayList<AbstractMemberMetaData>(mmds);
                    embMmds.add(mmd);
                    processEmbeddedMember(embMmds, clr);
                }
                else
                {
                    // Don't support embedded collections/maps
                    NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is an embedded collection. Not supported so ignoring");
                }
            }
            else
            {
                List<AbstractMemberMetaData> embMmds = new ArrayList<AbstractMemberMetaData>(mmds);
                embMmds.add(mmd);
                ColumnMetaData[] colmds = mmd.getColumnMetaData(); // TODO Is there an embedded definition? we only need jdbc type so not critical

                if (relationType != RelationType.NONE)
                {
                    // 1-1/N-1 stored as single column with persistable-id
                    // 1-N/M-N stored as single column with collection<persistable-id>
                    // Create column for basic type
                    String colName = namingFactory.getColumnName(embMmds, 0);
                    ColumnImpl col = addEmbeddedColumn(embMmds, colName);
                    if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                    {
                        col.setPosition(colmds[0].getPosition());
                    }
                }
                else
                {
                    TypeConverter typeConv = getTypeConverterForMember(mmd, colmds, typeMgr);
                    if (typeConv != null)
                    {
                        // Create column(s) for this TypeConverter
                        if (typeConv instanceof MultiColumnConverter)
                        {
                            Class[] colJavaTypes = ((MultiColumnConverter)typeConv).getDatastoreColumnTypes();
                            for (int j=0;j<colJavaTypes.length;j++)
                            {
                                String colName = namingFactory.getColumnName(embMmds, j);
                                ColumnImpl col = addEmbeddedColumn(embMmds, colName);
                                if (colmds != null && colmds.length == 1 && colmds[j].getPosition() != null)
                                {
                                    col.setPosition(colmds[j].getPosition());
                                }
                            }
                        }
                        else
                        {
                            String colName = namingFactory.getColumnName(embMmds, 0);
                            ColumnImpl col = addEmbeddedColumn(embMmds, colName);
                            if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                            {
                                col.setPosition(colmds[0].getPosition());
                            }
                        }
                    }
                    else
                    {
                        // Create column for basic type
                        String colName = namingFactory.getColumnName(embMmds, 0);
                        ColumnImpl col = addEmbeddedColumn(embMmds, colName);
                        if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                        {
                            col.setPosition(colmds[0].getPosition());
                        }
                    }
                }
            }
        }
    }

    protected ColumnImpl addColumn(AbstractMemberMetaData mmd, String colName)
    {
        return addColumn(mmd, colName, ColumnType.COLUMN);
    }

    protected ColumnImpl addColumn(AbstractMemberMetaData mmd, String colName, ColumnType colType)
    {
        ColumnImpl col = new ColumnImpl(this, colName, colType);
        if (mmd != null)
        {
            col.setMemberMetaData(mmd);
            if (mmd.isPrimaryKey())
            {
                col.setPrimaryKey();
            }
            columnByMember.put(mmd, col);
            columnAttributer.attributeColumn(col, mmd);
        }
        else
        {
            if (colType == ColumnType.DATASTOREID_COLUMN)
            {
                col.setPrimaryKey();
            }
            columnAttributer.attributeColumn(col, null);
        }
        columns.add(col);
        return col;
    }

    protected ColumnImpl addEmbeddedColumn(List<AbstractMemberMetaData> embMmds, String colName)
    {
        NucleusLogger.GENERAL.info(">> addEmbeddedCol name=" + colName + " embMmds=" + StringUtils.collectionToString(embMmds), new Exception());
        ColumnImpl col = new ColumnImpl(this, colName, ColumnType.COLUMN);
        if (embMmds != null && embMmds.size() > 0)
        {
            col.setMemberMetaData(embMmds.get(embMmds.size()-1));
        }
        columns.add(col);
        columnAttributer.attributeEmbeddedColumn(col, embMmds);
        columnByEmbeddedMember.put(getEmbeddedMemberNavigatedPath(embMmds), col);
        return col;
    }

    private String getEmbeddedMemberNavigatedPath(List<AbstractMemberMetaData> mmds)
    {
        Iterator<AbstractMemberMetaData> mmdIter = mmds.iterator();
        StringBuilder strBldr = new StringBuilder(mmdIter.next().getFullFieldName());
        while (mmdIter.hasNext())
        {
            strBldr.append('.').append(mmdIter.next().getName());
        }
        return strBldr.toString();
    }

    public AbstractClassMetaData getClassMetaData()
    {
        return cmd;
    }

    public StoreManager getStoreManager()
    {
        return storeMgr;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public String getCatalogName()
    {
        return catalogName;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public int getNumberOfColumns()
    {
        return columns.size();
    }

    public List<Column> getColumns()
    {
        return columns;
    }

    public Column getDatastoreIdColumn()
    {
        return datastoreIdColumn;
    }

    public Column getVersionColumn()
    {
        return versionColumn;
    }

    public Column getDiscriminatorColumn()
    {
        return discriminatorColumn;
    }

    public Column getMultitenancyColumn()
    {
        return multitenancyColumn;
    }

    public Column getColumnForPosition(int pos)
    {
        throw new UnsupportedOperationException("This class doesnt yet support accessing columns by position");
//        return columnByPosition.get(pos);
    }

    public Column getColumnForMember(AbstractMemberMetaData mmd)
    {
        return columnByMember.get(mmd);
    }

    public Column getColumnForEmbeddedMember(List<AbstractMemberMetaData> mmds)
    {
        return columnByEmbeddedMember.get(getEmbeddedMemberNavigatedPath(mmds));
    }

    public String toString()
    {
        return "Table: " + identifier;
    }
}