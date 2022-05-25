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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.DiscriminatorMetaData;
import org.datanucleus.metadata.EmbeddedMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.JdbcType;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.MultitenancyMetaData;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.metadata.SoftDeleteMetaData;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.schema.naming.ColumnType;
import org.datanucleus.store.schema.naming.NamingFactory;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.converters.MultiColumnConverter;
import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Representation of a table for a class where the class is stored in "complete-table" inheritance (or in JPA "TablePerClass")
 * whereby all members (in this class and superclasses) are handled in this table. Also assumes that any persistable fields
 * and collection/map fields are stored in this table (i.e not usable where you have foreign keys in the datastore).
 * Allows for each member to have potentially multiple columns (using MemberColumnMapping).
 * Each column generated will have its position set (origin = 0) and respects "ColumnMetaData.position".
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

    Column softDeleteColumn;

    // TODO Support create-timestamp surrogate
    // TODO Support update-timestamp surrogate

    /** Map of member-column mapping, keyed by the metadata for the member. */
    Map<String, MemberColumnMapping> mappingByMember = new HashMap<>();

    /** Map of member-column mapping, keyed by the navigated path of embedded members. */
    Map<String, MemberColumnMapping> mappingByEmbeddedMember = new HashMap<>();

    /** Map of DatastoreColumn, keyed by the column identifier. */
    Map<String, Column> columnByName = new HashMap<String, Column>();

    SchemaVerifier schemaVerifier;

    public CompleteClassTable(StoreManager storeMgr, AbstractClassMetaData cmd, SchemaVerifier verifier)
    {
        this.storeMgr = storeMgr;
        this.cmd = cmd;
        this.schemaVerifier = verifier;

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

        MetaDataManager mmgr = storeMgr.getMetaDataManager();
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

            // TODO Make use of cmd.overriddenMembers
            RelationType relationType = mmd.getRelationType(clr);
            if (relationType != RelationType.NONE && MetaDataUtils.getInstance().isMemberEmbedded(mmgr, clr, mmd, relationType, null))
            {
                // EMBEDDED MEMBER
                List<AbstractMemberMetaData> embMmds = new ArrayList<AbstractMemberMetaData>();
                embMmds.add(mmd);
                if (RelationType.isRelationSingleValued(relationType))
                {
                    // Embedded PC field
                    boolean nested = false;
                    if (storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM_EMBEDDED_PC_NESTED))
                    {
                        nested = !storeMgr.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_EMBEDDED_PC_FLAT);
                        String nestedStr = mmd.getValueForExtension("nested");
                        if (nestedStr != null && nestedStr.toLowerCase().equals("" + !nested))
                        {
                            nested = !nested;
                        }
                    }

                    if (nested)
                    {
                        // Embedded object stored as nested under this in the owner table (where the datastore supports that)
                        // Create column for the embedded owner field (that holds the nested embedded object), typically for the column name only
                        ColumnMetaData[] colmds = mmd.getColumnMetaData();
                        String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                        ColumnImpl col = addColumn(mmd, colName, null);
                        if (colmds != null && colmds.length == 1)
                        {
                            col.setColumnMetaData(colmds[0]);
                            if (colmds[0].getPosition() != null)
                            {
                                col.setPosition(colmds[0].getPosition());
                            }
                            if (colmds[0].getJdbcType() != null)
                            {
                                col.setJdbcType(colmds[0].getJdbcType());
                            }
                        }
                        MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                        col.setMemberColumnMapping(mapping);
                        if (schemaVerifier != null)
                        {
                            schemaVerifier.attributeMember(mapping, mmd);
                        }
                        mappingByMember.put(mmd.getFullFieldName(), mapping);
                        // TODO Consider adding the embedded info under the above column as related information
                    }

                    // Recurse through the embedded member
                    processEmbeddedMember(embMmds, mmgr.getMetaDataForClass(mmd.getType(), clr), clr, mmd.getEmbeddedMetaData(), nested);
                }
                else if (RelationType.isRelationMultiValued(relationType))
                {
                    if (mmd.hasCollection())
                    {
                        // Embedded collection element (nested into owner table where supported)
                        if (storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM_EMBEDDED_COLLECTION_NESTED))
                        {
                            // Add column for the collection (since the store needs a name to reference it by)
                            ColumnMetaData[] colmds = mmd.getColumnMetaData();
                            String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                            ColumnImpl col = addColumn(mmd, colName, null);
                            if (colmds != null && colmds.length == 1)
                            {
                                col.setColumnMetaData(colmds[0]);
                                if (colmds[0].getPosition() != null)
                                {
                                    col.setPosition(colmds[0].getPosition());
                                }
                                if (colmds[0].getJdbcType() != null)
                                {
                                    col.setJdbcType(colmds[0].getJdbcType());
                                }
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                            col.setMemberColumnMapping(mapping);
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeMember(mapping, mmd);
                            }
                            mappingByMember.put(mmd.getFullFieldName(), mapping);
                            // TODO Consider adding the embedded info under the above column as related information

                            // Recurse through the embedded collection element
                            EmbeddedMetaData embmd = mmd.getElementMetaData() != null ? mmd.getElementMetaData().getEmbeddedMetaData() : null;
                            processEmbeddedMember(embMmds, mmgr.getMetaDataForClass(mmd.getCollection().getElementType(), clr), clr, embmd, true);
                        }
                        else
                        {
                            NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is an embedded collection. Not supported for this datastore, so ignoring");
                            continue;
                        }
                    }
                    else if (mmd.hasMap())
                    {
                        // Embedded map key/value (nested into owner table where supported)
                        if (storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM_EMBEDDED_MAP_NESTED))
                        {
                            // Add column for the map (since the store needs a name to reference it by)
                            ColumnMetaData[] colmds = mmd.getColumnMetaData();
                            String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                            ColumnImpl col = addColumn(mmd, colName, null);
                            if (colmds != null && colmds.length == 1)
                            {
                                col.setColumnMetaData(colmds[0]);
                                if (colmds[0].getPosition() != null)
                                {
                                    col.setPosition(colmds[0].getPosition());
                                }
                                if (colmds[0].getJdbcType() != null)
                                {
                                    col.setJdbcType(colmds[0].getJdbcType());
                                }
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                            col.setMemberColumnMapping(mapping);
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeMember(mapping, mmd);
                            }
                            mappingByMember.put(mmd.getFullFieldName(), mapping);

                            if (mmd.getMap().keyIsPersistent())
                            {
                                // Recurse through the embedded key
                                EmbeddedMetaData embmd = mmd.getKeyMetaData() != null ? mmd.getKeyMetaData().getEmbeddedMetaData() : null;
                                processEmbeddedMember(embMmds, mmgr.getMetaDataForClass(mmd.getMap().getKeyType(), clr), clr, embmd, true);
                            }
                            else
                            {
                                // TODO Add basic key column
                            }
                            if (mmd.getMap().valueIsPersistent())
                            {
                                // Recurse through the embedded value
                                EmbeddedMetaData embmd = mmd.getValueMetaData() != null ? mmd.getValueMetaData().getEmbeddedMetaData() : null;
                                processEmbeddedMember(embMmds, mmgr.getMetaDataForClass(mmd.getMap().getValueType(), clr), clr, embmd, true);
                            }
                            else
                            {
                                // TODO Add basic value column
                            }
                        }
                        else
                        {
                            NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is an embedded map. Not supported for this datastore, so ignoring");
                            continue;
                        }
                    }
                    else if (mmd.hasArray())
                    {
                        // Embedded array element (nested into owner table where supported)
                        if (storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM_EMBEDDED_ARRAY_NESTED))
                        {
                            // Add column for the array (since the store needs a name to reference it by)
                            ColumnMetaData[] colmds = mmd.getColumnMetaData();
                            String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                            ColumnImpl col = addColumn(mmd, colName, null);
                            if (colmds != null && colmds.length == 1)
                            {
                                col.setColumnMetaData(colmds[0]);
                                if (colmds[0].getPosition() != null)
                                {
                                    col.setPosition(colmds[0].getPosition());
                                }
                                if (colmds[0].getJdbcType() != null)
                                {
                                    col.setJdbcType(colmds[0].getJdbcType());
                                }
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                            col.setMemberColumnMapping(mapping);
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeMember(mapping, mmd);
                            }
                            mappingByMember.put(mmd.getFullFieldName(), mapping);
                            // TODO Consider adding the embedded info under the above column as related information

                            // Recurse through the embedded array element
                            EmbeddedMetaData embmd = mmd.getElementMetaData() != null ? mmd.getElementMetaData().getEmbeddedMetaData() : null;
                            processEmbeddedMember(embMmds, mmgr.getMetaDataForClass(mmd.getArray().getElementType(), clr), clr, embmd, true);
                        }
                        else
                        {
                            NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is an embedded array. Not supported for this datastore, so ignoring");
                            continue;
                        }
                    }
                }
            }
            else
            {
                // STANDARD MEMBER
                ColumnMetaData[] colmds = mmd.getColumnMetaData();
                if ((colmds == null || colmds.length == 0) && mmd.hasCollection() && mmd.getElementMetaData() != null)
                {
                    // Column is for a collection, and column info stored under <element>
                    colmds = mmd.getElementMetaData().getColumnMetaData();
                }

                if (relationType != RelationType.NONE)
                {
                    // 1-1/N-1 stored as single column with persistable-id
                    // 1-N/M-N stored as single column with collection<persistable-id>
                    String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                    ColumnImpl col = addColumn(mmd, colName, null);
                    if (colmds != null && colmds.length == 1)
                    {
                        col.setColumnMetaData(colmds[0]);
                        if (colmds[0].getPosition() != null)
                        {
                            col.setPosition(colmds[0].getPosition());
                        }
                        if (colmds[0].getJdbcType() != null)
                        {
                            col.setJdbcType(colmds[0].getJdbcType());
                        }
                    }
                    MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                    col.setMemberColumnMapping(mapping);
                    if (schemaVerifier != null)
                    {
                        schemaVerifier.attributeMember(mapping, mmd);
                    }
                    mappingByMember.put(mmd.getFullFieldName(), mapping);
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
                            Column[] cols = new Column[colJavaTypes.length];
                            for (int j=0;j<colJavaTypes.length;j++)
                            {
                                String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, j);
                                ColumnImpl col = addColumn(mmd, colName);
                                if (colmds != null && colmds.length == 1)
                                {
                                    col.setColumnMetaData(colmds[0]);
                                    if (colmds[j].getPosition() != null)
                                    {
                                        col.setPosition(colmds[j].getPosition());
                                    }
                                    if (colmds[j].getJdbcType() != null)
                                    {
                                        col.setJdbcType(colmds[j].getJdbcType());
                                    }
                                }
                                cols[j] = col;
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, cols, typeConv);
                            for (int j=0;j<colJavaTypes.length;j++)
                            {
                                ((ColumnImpl)cols[j]).setMemberColumnMapping(mapping);
                            }
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeMember(mapping, mmd);
                            }
                            mappingByMember.put(mmd.getFullFieldName(), mapping);
                        }
                        else
                        {
                            String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                            ColumnImpl col = addColumn(mmd, colName);
                            if (colmds != null && colmds.length == 1)
                            {
                                col.setColumnMetaData(colmds[0]);
                                if (colmds[0].getPosition() != null)
                                {
                                    col.setPosition(colmds[0].getPosition());
                                }
                                if (colmds[0].getJdbcType() != null)
                                {
                                    col.setJdbcType(colmds[0].getJdbcType());
                                }
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                            col.setMemberColumnMapping(mapping);
                            mapping.setTypeConverter(typeConv);
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeMember(mapping, mmd);
                            }
                            mappingByMember.put(mmd.getFullFieldName(), mapping);
                        }
                    }
                    else
                    {
                        // Create column for basic type
                        String colName = storeMgr.getNamingFactory().getColumnName(mmd, ColumnType.COLUMN, 0);
                        ColumnImpl col = addColumn(mmd, colName);
                        if (colmds != null && colmds.length == 1)
                        {
                            col.setColumnMetaData(colmds[0]);
                            if (colmds[0].getPosition() != null)
                            {
                                col.setPosition(colmds[0].getPosition());
                            }
                            if (colmds[0].getJdbcType() != null)
                            {
                                col.setJdbcType(colmds[0].getJdbcType());
                            }
                        }

                        MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                        if (mmd.hasCollection())
                        {
                            if (mmd.getElementMetaData() != null && mmd.getElementMetaData().hasExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME))
                            {
                                TypeConverter elemConv = typeMgr.getTypeConverterForName(mmd.getElementMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME));
                                mapping.setTypeConverterForComponent(FieldRole.ROLE_COLLECTION_ELEMENT, elemConv);
                            }
                        }
                        else if (mmd.hasMap())
                        {
                            if (mmd.getKeyMetaData() != null && mmd.getKeyMetaData().hasExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME))
                            {
                                TypeConverter keyConv = typeMgr.getTypeConverterForName(mmd.getKeyMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME));
                                mapping.setTypeConverterForComponent(FieldRole.ROLE_MAP_KEY, keyConv);
                            }
                            if (mmd.getValueMetaData() != null && mmd.getValueMetaData().hasExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME))
                            {
                                TypeConverter valConv = typeMgr.getTypeConverterForName(mmd.getValueMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME));
                                mapping.setTypeConverterForComponent(FieldRole.ROLE_MAP_VALUE, valConv);
                            }
                        }
                        col.setMemberColumnMapping(mapping);
                        if (schemaVerifier != null)
                        {
                            schemaVerifier.attributeMember(mapping, mmd);
                        }
                        mappingByMember.put(mmd.getFullFieldName(), mapping);
                    }
                }
            }
        }

        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            // Add surrogate datastore-identity column
            String colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.DATASTOREID_COLUMN);
            ColumnImpl col = addColumn(null, colName, ColumnType.DATASTOREID_COLUMN);
            if (schemaVerifier != null)
            {
                schemaVerifier.attributeMember(new MemberColumnMappingImpl(null, col));
            }
            if (cmd.getDatastoreIdentityMetaData() != null && cmd.getDatastoreIdentityMetaData().getColumnMetaData() != null)
            {
                if (cmd.getDatastoreIdentityMetaData().getColumnMetaData().getPosition() != null)
                {
                    col.setPosition(cmd.getDatastoreIdentityMetaData().getColumnMetaData().getPosition());
                }
                if (cmd.getDatastoreIdentityMetaData().getColumnMetaData().getJdbcType() != null)
                {
                    col.setJdbcType(cmd.getDatastoreIdentityMetaData().getColumnMetaData().getJdbcType());
                }
            }
            this.datastoreIdColumn = col;
        }

        VersionMetaData vermd = cmd.getVersionMetaDataForClass();
        if (vermd != null)
        {
            // Add surrogate version column
            if (vermd.getMemberName() == null)
            {
                String colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.VERSION_COLUMN);
                ColumnImpl col = addColumn(null, colName, ColumnType.VERSION_COLUMN);
                if (schemaVerifier != null)
                {
                    schemaVerifier.attributeMember(new MemberColumnMappingImpl(null, col));
                }
                if (vermd.getColumnMetaData() != null)
                {
                    if (vermd.getColumnMetaData().getPosition() != null)
                    {
                        col.setPosition(vermd.getColumnMetaData().getPosition());
                    }
                    if (vermd.getColumnMetaData().getJdbcType() != null)
                    {
                        col.setJdbcType(vermd.getColumnMetaData().getJdbcType());
                    }
                }
                this.versionColumn = col;
            }
        }

        if (cmd.hasDiscriminatorStrategy())
        {
            // Add discriminator column
            String colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.DISCRIMINATOR_COLUMN);
            ColumnImpl col = addColumn(null, colName, ColumnType.DISCRIMINATOR_COLUMN);
            if (schemaVerifier != null)
            {
                schemaVerifier.attributeMember(new MemberColumnMappingImpl(null, col));
            }
            DiscriminatorMetaData dismd = cmd.getDiscriminatorMetaDataForTable();
            if (dismd != null && dismd.getColumnMetaData() != null)
            {
                if (dismd.getColumnMetaData().getPosition() != null)
                {
                    col.setPosition(dismd.getColumnMetaData().getPosition());
                }
                if (dismd.getColumnMetaData().getJdbcType() != null)
                {
                    col.setJdbcType(dismd.getColumnMetaData().getJdbcType());
                }
            }
            this.discriminatorColumn = col;
        }

        MultitenancyMetaData mtmd = cmd.getMultitenancyMetaData();
        if (mtmd != null)
        {
            // Multitenancy discriminator present : Add restriction for this tenant
            ColumnMetaData colmd = mtmd.getColumnMetaData();
            String colName = mtmd.getColumnName();
            if (colmd != null)
            {
                colName = colmd.getName();
            }
            if (StringUtils.isWhitespace(colName))
            {
                // Generate name since not specified
                colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.MULTITENANCY_COLUMN);
            }
            Column col = addColumn(null, colName, ColumnType.MULTITENANCY_COLUMN); // TODO Support column position
            col.setJdbcType(colmd.getJdbcType() != null ? colmd.getJdbcType() : JdbcType.VARCHAR);
            if (schemaVerifier != null)
            {
                schemaVerifier.attributeMember(new MemberColumnMappingImpl(null, col));
            }
            this.multitenancyColumn = col;
        }

        SoftDeleteMetaData sdmd = cmd.getSoftDeleteMetaData();
        if (sdmd != null)
        {
            // Add surrogate soft-delete column TODO Cater for this specified in superclass applying to this class also?
            ColumnMetaData colmd = sdmd.getColumnMetaData();
            String colName = sdmd.getColumnName();
            if (colmd != null)
            {
                colName = colmd.getName();
            }
            if (StringUtils.isWhitespace(colName))
            {
                // Generate name since not specified
                colName = storeMgr.getNamingFactory().getColumnName(cmd, ColumnType.SOFTDELETE_COLUMN);
            }
            Column col = addColumn(null, colName, ColumnType.SOFTDELETE_COLUMN);
            col.setJdbcType(colmd.getJdbcType() != null ? colmd.getJdbcType() : JdbcType.BOOLEAN);
            if (schemaVerifier != null)
            {
                schemaVerifier.attributeMember(new MemberColumnMappingImpl(null, col));
            }
            this.softDeleteColumn = col;
        }
        if (cmd.hasExtension(MetaData.EXTENSION_CLASS_CREATEUSER))
        {
            // TODO Support this
        }
        if (cmd.hasExtension(MetaData.EXTENSION_CLASS_CREATETIMESTAMP))
        {
            // TODO Support this
        }
        if (cmd.hasExtension(MetaData.EXTENSION_CLASS_UPDATEUSER))
        {
            // TODO Support this
        }
        if (cmd.hasExtension(MetaData.EXTENSION_CLASS_UPDATETIMESTAMP))
        {
            // TODO Support this
        }

        // Reorder all columns to respect column positioning information. Note this assumes the user has provided complete information
        List<Column> unorderedCols = new ArrayList();
        Column[] cols = new Column[columns.size()];
        for (Column col : columns)
        {
            if (col.getPosition() >= columns.size())
            {
                NucleusLogger.DATASTORE_SCHEMA.warn("Column with name " + col.getName() + " is specified with position=" + col.getPosition() + " which is invalid." +
                   " This table has " + columns.size() + " columns");
                unorderedCols.add(col);
            }
            else if (col.getPosition() >= 0)
            {
                if (cols[col.getPosition()] != null)
                {
                    NucleusLogger.DATASTORE_SCHEMA.warn("Column with name " + col.getName() + " defined for position=" + col.getPosition() + " yet there is also " +
                        cols[col.getPosition()].getName() + " at that position! Ignoring");
                    unorderedCols.add(col);
                }
                else
                {
                    cols[col.getPosition()] = col;
                }
            }
            else
            {
                unorderedCols.add(col);
            }
        }
        if (!unorderedCols.isEmpty())
        {
            for (int i=0;i<cols.length;i++)
            {
                if (cols[i] == null)
                {
                     cols[i] = unorderedCols.get(0);
                     cols[i].setPosition(i);
                     unorderedCols.remove(0);
                }
            }
        }

        columns = new ArrayList<Column>();
        for (Column col : cols)
        {
            MemberColumnMapping mapping = col.getMemberColumnMapping();
            if (mapping != null)
            {
                if (!mapping.getMemberMetaData().isInsertable() && !mapping.getMemberMetaData().isUpdateable())
                {
                    // Ignored
                    NucleusLogger.DATASTORE_SCHEMA.debug("Not adding column " + col.getName() + " for member=" + mapping.getMemberMetaData().getFullFieldName() + 
                        " since is not insertable/updateable");
                }
                else
                {
                    boolean allowAddition = true;
                    if (columnByName.containsKey(col.getName()))
                    {
                        Column otherCol = columnByName.get(col.getName());
                        if (((ColumnImpl)col).isNested() || ((ColumnImpl)otherCol).isNested())
                        {
                            // We allow re-use of names with one nested. TODO Need to check if nested in the same part but the current structure doesn't handle that
                        }
                        else
                        {
                            if (mapping.getMemberMetaData() instanceof PropertyMetaData && otherCol.getMemberColumnMapping().getMemberMetaData() instanceof PropertyMetaData)
                            {
                                // We allow re-use of property names, since the subclass can override the superclass
                                allowAddition = false;
                            }
                            else
                            {
                                String msg = "Unable to add column with name=" + col.getName() + " for member=" + mapping.getMemberMetaData() + 
                                        " to table=" + getName() + " for class=" + cmd.getFullClassName() + " since one with same name already exists (superclass?).";
                                NucleusLogger.DATASTORE_SCHEMA.error(msg);
                                throw new NucleusUserException(msg);
                            }
                        }
                    }
                    if (allowAddition)
                    {
                        columns.add(col);
                        columnByName.put(col.getName(), col);
                    }
                }
            }
            else
            {
                columns.add(col);
                columnByName.put(col.getName(), col);
            }
        }
    }

    protected TypeConverter getTypeConverterForMember(AbstractMemberMetaData mmd, ColumnMetaData[] colmds, TypeManager typeMgr)
    {
        TypeConverter typeConv = null;
        String typeConvName = mmd.getTypeConverterName();
        if (typeConvName != null)
        {
            // User has specified the TypeConverter
            typeConv = typeMgr.getTypeConverterForName(typeConvName);
            if (typeConv == null)
            {
                throw new NucleusUserException(Localiser.msg("044062", mmd.getFullFieldName(), typeConvName));
            }
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
                // Single column, so try to match the JDBC type IF provided
                JdbcType jdbcType = colmds != null && colmds.length > 0 ? colmds[0].getJdbcType() : null;
                if (jdbcType != null)
                {
                    // JDBC type specified so don't just take the default
                    if (MetaDataUtils.isJdbcTypeString(jdbcType))
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), String.class);
                    }
                    else if (MetaDataUtils.isJdbcTypeNumeric(jdbcType))
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), Long.class);
                    }
                    else if (jdbcType == JdbcType.TIMESTAMP)
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), Timestamp.class);
                    }
                    else if (jdbcType == JdbcType.TIME)
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), Time.class);
                    }
                    else if (jdbcType == JdbcType.DATE)
                    {
                        typeConv = typeMgr.getTypeConverterForType(mmd.getType(), Date.class);
                    }
                    // TODO Support other JDBC types
                }
                else
                {
                    // Fallback to default type converter for this member type (if any)
                    // NOTE We disable this because each datastore may choose to support other types so it should be for those to set any fallback TypeConverter
                    //typeConv = typeMgr.getDefaultTypeConverterForType(mmd.getType());
                }
            }
        }

        if (schemaVerifier != null)
        {
            // Make sure that the schema verifier supports this conversion
            typeConv = schemaVerifier.verifyTypeConverterForMember(mmd, typeConv);
        }
        return typeConv;
    }

    /**
     * Handler for an embedded member.
     * @param mmds Chain of member metadata to the embedded member
     * @param embCmd Class metadata for the embedded member type
     * @param clr ClassLoader resolver
     * @param embmd Any EmbeddedMetaData defining column info
     * @param ownerNested Whether the owner is nested
     */
    protected void processEmbeddedMember(List<AbstractMemberMetaData> mmds, AbstractClassMetaData embCmd, ClassLoaderResolver clr, EmbeddedMetaData embmd, boolean ownerNested)
    {
        TypeManager typeMgr = storeMgr.getNucleusContext().getTypeManager();
        MetaDataManager mmgr = storeMgr.getMetaDataManager();
        NamingFactory namingFactory = storeMgr.getNamingFactory();
        AbstractMemberMetaData lastMmd = mmds.get(mmds.size()-1);

        // Go through all members of the embedded type
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
            AbstractMemberMetaData embmdMmd = null;
            if (embmd != null)
            {
                List<AbstractMemberMetaData> embmdMmds = embmd.getMemberMetaData();
                if (embmdMmds != null)
                {
                    for (AbstractMemberMetaData thisMmd : embmdMmds)
                    {
                        if (thisMmd.getName().equals(mmd.getName()))
                        {
                            embmdMmd = thisMmd;
                            break;
                        }
                    }
                }
            }

            RelationType relationType = mmd.getRelationType(clr);
            if (relationType != RelationType.NONE && MetaDataUtils.getInstance().isMemberEmbedded(mmgr, clr, mmd, relationType, lastMmd))
            {
                if (RelationType.isRelationSingleValued(relationType))
                {
                    // Nested embedded PC, so recurse
                    boolean nested = false;
                    if (storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM_EMBEDDED_PC_NESTED))
                    {
                        nested = !storeMgr.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_EMBEDDED_PC_FLAT);
                        String nestedStr = mmd.getValueForExtension("nested");
                        if (nestedStr != null && nestedStr.toLowerCase().equals("" + !nested))
                        {
                            nested = !nested;
                        }
                    }

                    List<AbstractMemberMetaData> embMmds = new ArrayList<AbstractMemberMetaData>(mmds);
                    embMmds.add(mmd);
                    if (nested)
                    {
                        // Embedded object stored as nested under this in the owner table (where the datastore supports that)
                        // Add column for the owner of the embedded object, typically for the column name only
                        ColumnMetaData[] colmds = mmd.getColumnMetaData();
                        String colName = namingFactory.getColumnName(embMmds, 0);
                        ColumnImpl col = addEmbeddedColumn(colName, null);
                        col.setNested(true);
                        if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getPosition() != null)
                        {
                            col.setPosition(embmdMmd.getColumnMetaData()[0].getPosition());
                        }
                        else if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                        {
                            col.setPosition(colmds[0].getPosition());
                        }
                        if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getJdbcType() != null)
                        {
                            col.setJdbcType(embmdMmd.getColumnMetaData()[0].getJdbcType());
                        }
                        else if (colmds != null && colmds.length == 1 && colmds[0].getJdbcType() != null)
                        {
                            col.setJdbcType(colmds[0].getJdbcType());
                        }
                        MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                        col.setMemberColumnMapping(mapping);
                        if (schemaVerifier != null)
                        {
                            schemaVerifier.attributeEmbeddedMember(mapping, embMmds);
                        }
                        mappingByEmbeddedMember.put(getEmbeddedMemberNavigatedPath(embMmds), mapping);
                        // TODO Create mapping for the related info under the above column
                    }

                    // Recurse through the embedded member
                    processEmbeddedMember(embMmds, mmgr.getMetaDataForClass(mmd.getType(), clr), clr, embmdMmd != null ? embmdMmd.getEmbeddedMetaData() : null, nested);
                }
                else
                {
                    if (mmd.hasCollection())
                    {
                        // Nested embedded collection, so recurse
                        if (storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM_EMBEDDED_COLLECTION_NESTED))
                        {
                            List<AbstractMemberMetaData> embMmds = new ArrayList<AbstractMemberMetaData>(mmds);
                            embMmds.add(mmd);

                            // Add column for the collection (since the store needs a name to reference it by)
                            ColumnMetaData[] colmds = mmd.getColumnMetaData();
                            String colName = namingFactory.getColumnName(embMmds, 0);
                            ColumnImpl col = addEmbeddedColumn(colName, null);
                            col.setNested(true);
                            if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getPosition() != null)
                            {
                                col.setPosition(embmdMmd.getColumnMetaData()[0].getPosition());
                            }
                            else if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                            {
                                col.setPosition(colmds[0].getPosition());
                            }
                            if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getJdbcType() != null)
                            {
                                col.setJdbcType(embmdMmd.getColumnMetaData()[0].getJdbcType());
                            }
                            else if (colmds != null && colmds.length == 1 && colmds[0].getJdbcType() != null)
                            {
                                col.setJdbcType(colmds[0].getJdbcType());
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                            col.setMemberColumnMapping(mapping);
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeEmbeddedMember(mapping, embMmds);
                            }
                            mappingByEmbeddedMember.put(getEmbeddedMemberNavigatedPath(embMmds), mapping);
                            // TODO Create mapping for the related info under the above column

                            // Recurse through the embedded collection element
                            processEmbeddedMember(embMmds, mmgr.getMetaDataForClass(mmd.getCollection().getElementType(), clr), clr, embmdMmd != null ? embmdMmd.getEmbeddedMetaData() : null, true);
                        }
                        else
                        {
                            NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is a (nested) embedded collection. Not supported for this datastore so ignoring");
                            continue;
                        }
                    }
                    else if (mmd.hasMap())
                    {
                        // Nested embedded map, so recurse
                        if (storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM_EMBEDDED_MAP_NESTED))
                        {
                            // TODO Support nested embedded map key/value
                            NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is a (nested) embedded map. Not yet supported so ignoring");
                        }
                        else
                        {
                            NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is a (nested) embedded map. Not supported for this datastore so ignoring");
                            continue;
                        }
                    }
                    else if (mmd.hasArray())
                    {
                        // Nested embedded array, so recurse
                        if (storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM_EMBEDDED_ARRAY_NESTED))
                        {
                            List<AbstractMemberMetaData> embMmds = new ArrayList<AbstractMemberMetaData>(mmds);
                            embMmds.add(mmd);

                            // Add column for the array (since the store needs a name to reference it by) TODO Extract this block out and reuse it
                            ColumnMetaData[] colmds = mmd.getColumnMetaData();
                            String colName = namingFactory.getColumnName(embMmds, 0);
                            ColumnImpl col = addEmbeddedColumn(colName, null);
                            col.setNested(true);
                            if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getPosition() != null)
                            {
                                col.setPosition(embmdMmd.getColumnMetaData()[0].getPosition());
                            }
                            else if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                            {
                                col.setPosition(colmds[0].getPosition());
                            }
                            if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getJdbcType() != null)
                            {
                                col.setJdbcType(embmdMmd.getColumnMetaData()[0].getJdbcType());
                            }
                            else if (colmds != null && colmds.length == 1 && colmds[0].getJdbcType() != null)
                            {
                                col.setJdbcType(colmds[0].getJdbcType());
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                            col.setMemberColumnMapping(mapping);
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeEmbeddedMember(mapping, embMmds);
                            }
                            mappingByEmbeddedMember.put(getEmbeddedMemberNavigatedPath(embMmds), mapping);
                            // TODO Create mapping for the related info under the above column

                            // Recurse through the embedded array element
                            processEmbeddedMember(embMmds, mmgr.getMetaDataForClass(mmd.getArray().getElementType(), clr), clr, embmdMmd != null ? embmdMmd.getEmbeddedMetaData() : null, true);
                        }
                        else
                        {
                            NucleusLogger.DATASTORE_SCHEMA.warn("Member " + mmd.getFullFieldName() + " is a (nested) embedded array. Not supported for this datastore so ignoring");
                            continue;
                        }
                    }
                }
            }
            else
            {
                List<AbstractMemberMetaData> embMmds = new ArrayList<AbstractMemberMetaData>(mmds);
                embMmds.add(mmd);
                ColumnMetaData[] colmds = mmd.getColumnMetaData();

                if (relationType != RelationType.NONE)
                {
                    // 1-1/N-1 stored as single column with persistable-id
                    // 1-N/M-N stored as single column with collection<persistable-id>
                    // Create column for basic type
                    String colName = namingFactory.getColumnName(embMmds, 0);
                    ColumnImpl col = addEmbeddedColumn(colName, null);
                    col.setNested(ownerNested);
                    if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getPosition() != null)
                    {
                        col.setPosition(embmdMmd.getColumnMetaData()[0].getPosition());
                    }
                    else if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                    {
                        col.setPosition(colmds[0].getPosition());
                    }
                    if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getJdbcType() != null)
                    {
                        col.setJdbcType(embmdMmd.getColumnMetaData()[0].getJdbcType());
                    }
                    else if (colmds != null && colmds.length == 1 && colmds[0].getJdbcType() != null)
                    {
                        col.setJdbcType(colmds[0].getJdbcType());
                    }
                    MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                    col.setMemberColumnMapping(mapping);
                    if (schemaVerifier != null)
                    {
                        schemaVerifier.attributeEmbeddedMember(mapping, embMmds);
                    }
                    mappingByEmbeddedMember.put(getEmbeddedMemberNavigatedPath(embMmds), mapping);
                }
                else
                {
                    TypeConverter typeConv = getTypeConverterForMember(mmd, colmds, typeMgr); // TODO Pass in embedded colmds if they have jdbcType info?
                    if (typeConv != null)
                    {
                        // Create column(s) for this type using a TypeConverter
                        if (typeConv instanceof MultiColumnConverter)
                        {
                            Class[] colJavaTypes = ((MultiColumnConverter)typeConv).getDatastoreColumnTypes();
                            Column[] cols = new Column[colJavaTypes.length];
                            for (int j=0;j<colJavaTypes.length;j++)
                            {
                                String colName = namingFactory.getColumnName(embMmds, j);
                                ColumnImpl col = addEmbeddedColumn(colName, typeConv);
                                col.setNested(ownerNested);
                                if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == colJavaTypes.length && embmdMmd.getColumnMetaData()[j].getPosition() != null)
                                {
                                    col.setPosition(embmdMmd.getColumnMetaData()[j].getPosition());
                                }
                                else if (colmds != null && colmds.length == colJavaTypes.length && colmds[j].getPosition() != null)
                                {
                                    col.setPosition(colmds[j].getPosition());
                                }
                                if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == colJavaTypes.length && embmdMmd.getColumnMetaData()[j].getJdbcType() != null)
                                {
                                    col.setJdbcType(embmdMmd.getColumnMetaData()[j].getJdbcType());
                                }
                                else if (colmds != null && colmds.length == colJavaTypes.length && colmds[j].getJdbcType() != null)
                                {
                                    col.setJdbcType(colmds[j].getJdbcType());
                                }
                                cols[j] = col;
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, cols, typeConv);
                            for (int j=0;j<colJavaTypes.length;j++)
                            {
                                ((ColumnImpl)cols[j]).setMemberColumnMapping(mapping);
                            }
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeEmbeddedMember(mapping, embMmds);
                            }
                            mappingByEmbeddedMember.put(getEmbeddedMemberNavigatedPath(embMmds), mapping);
                        }
                        else
                        {
                            String colName = namingFactory.getColumnName(embMmds, 0);
                            ColumnImpl col = addEmbeddedColumn(colName, typeConv);
                            col.setNested(ownerNested);
                            if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getPosition() != null)
                            {
                                col.setPosition(embmdMmd.getColumnMetaData()[0].getPosition());
                            }
                            else if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                            {
                                col.setPosition(colmds[0].getPosition());
                            }
                            if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getJdbcType() != null)
                            {
                                col.setJdbcType(embmdMmd.getColumnMetaData()[0].getJdbcType());
                            }
                            else if (colmds != null && colmds.length == 1 && colmds[0].getJdbcType() != null)
                            {
                                col.setJdbcType(colmds[0].getJdbcType());
                            }
                            MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                            col.setMemberColumnMapping(mapping);
                            mapping.setTypeConverter(typeConv);
                            if (schemaVerifier != null)
                            {
                                schemaVerifier.attributeEmbeddedMember(mapping, embMmds);
                            }
                            mappingByEmbeddedMember.put(getEmbeddedMemberNavigatedPath(embMmds), mapping);
                        }
                    }
                    else
                    {
                        // Create column for basic type
                        String colName = namingFactory.getColumnName(embMmds, 0);
                        ColumnImpl col = addEmbeddedColumn(colName, null);
                        col.setNested(ownerNested);
                        AbstractMemberMetaData theMmd = embMmds.get(0);
                        if (theMmd.isPrimaryKey())
                        {
                            col.setPrimaryKey();
                        }
                        if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getPosition() != null)
                        {
                            col.setPosition(embmdMmd.getColumnMetaData()[0].getPosition());
                        }
                        else if (colmds != null && colmds.length == 1 && colmds[0].getPosition() != null)
                        {
                            col.setPosition(colmds[0].getPosition());
                        }
                        if (embmdMmd != null && embmdMmd.getColumnMetaData() != null && embmdMmd.getColumnMetaData().length == 1 && embmdMmd.getColumnMetaData()[0].getJdbcType() != null)
                        {
                            col.setJdbcType(embmdMmd.getColumnMetaData()[0].getJdbcType());
                        }
                        else if (colmds != null && colmds.length == 1 && colmds[0].getJdbcType() != null)
                        {
                            col.setJdbcType(colmds[0].getJdbcType());
                        }
                        MemberColumnMapping mapping = new MemberColumnMappingImpl(mmd, col);
                        col.setMemberColumnMapping(mapping);
                        if (schemaVerifier != null)
                        {
                            schemaVerifier.attributeEmbeddedMember(mapping, embMmds);
                        }
                        mappingByEmbeddedMember.put(getEmbeddedMemberNavigatedPath(embMmds), mapping);
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
        // TODO Set defaultable method on Column
        ColumnImpl col = new ColumnImpl(this, colName, colType);
        if (mmd != null)
        {
            if (mmd.isPrimaryKey())
            {
                col.setPrimaryKey();
            }
        }
        else
        {
            if (colType == ColumnType.DATASTOREID_COLUMN)
            {
                col.setPrimaryKey();
            }
        }
        columns.add(col);
        return col;
    }

    protected ColumnImpl addEmbeddedColumn(String colName, TypeConverter typeConv)
    {
        // TODO Set defaultable method on Column
        ColumnImpl col = new ColumnImpl(this, colName, ColumnType.COLUMN);
        columns.add(col);
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

    public String getName()
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

    public Column getColumnForPosition(int pos)
    {
        if (pos < 0 || pos >= columns.size())
        {
            throw new ArrayIndexOutOfBoundsException("There are " + columns.size() + " columns, so specify a value between 0 and " + (columns.size()-1));
        }
        return columns.get(pos);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.table.Table#getSurrogateColumn(org.datanucleus.store.schema.table.SurrogateColumnType)
     */
    @Override
    public Column getSurrogateColumn(SurrogateColumnType colType)
    {
        if (colType == SurrogateColumnType.DATASTORE_ID)
        {
            return datastoreIdColumn;
        }
        else if (colType == SurrogateColumnType.DISCRIMINATOR)
        {
            return discriminatorColumn;
        }
        else if (colType == SurrogateColumnType.VERSION)
        {
            return versionColumn;
        }
        else if (colType == SurrogateColumnType.MULTITENANCY)
        {
            return multitenancyColumn;
        }
        else if (colType == SurrogateColumnType.CREATE_TIMESTAMP)
        {
            // TODO Support this
        }
        else if (colType == SurrogateColumnType.UPDATE_TIMESTAMP)
        {
            // TODO Support this
        }
        else if (colType == SurrogateColumnType.CREATE_USER)
        {
            // TODO Support this
        }
        else if (colType == SurrogateColumnType.UPDATE_USER)
        {
            // TODO Support this
        }
        else if (colType == SurrogateColumnType.SOFTDELETE)
        {
            return softDeleteColumn;
        }
        return null;
    }

    public Column getColumnForName(String name)
    {
        Column col = columnByName.get(name);
        if (col != null)
        {
            return col;
        }
        if (!name.startsWith("\""))
        {
            col = columnByName.get("\"" + name + "\"");
        }
        return col;
    }

    public MemberColumnMapping getMemberColumnMappingForMember(AbstractMemberMetaData mmd)
    {
        return mappingByMember.get(mmd.getFullFieldName());
    }

    public MemberColumnMapping getMemberColumnMappingForEmbeddedMember(List<AbstractMemberMetaData> mmds)
    {
        return mappingByEmbeddedMember.get(getEmbeddedMemberNavigatedPath(mmds));
    }

    public Set<MemberColumnMapping> getMemberColumnMappings()
    {
        Set<MemberColumnMapping> mappings = new HashSet(mappingByMember.values());
        mappings.addAll(mappingByEmbeddedMember.values());
        return mappings;
    }

    public String toString()
    {
        return "Table: " + identifier;
    }

    public String debugString()
    {
        StringBuilder str = new StringBuilder();
        str.append("Table: ");
        if (catalogName != null)
        {
            str.append(catalogName).append('.');
        }
        if (schemaName != null)
        {
            str.append(schemaName).append('.');
        }
        str.append(identifier).append("\n");
        str.append("{\n");
        for (Column col : columns)
        {
            str.append("  ").append(col.toString()).append("\n");
        }
        str.append("}");
        return str.toString();
    }
}