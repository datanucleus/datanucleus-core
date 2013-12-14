/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Convenience class to handle the merging of MetaData.
 * This is used in the following situations
 * <ul> 
 * <li>JDO : Merging ORM MetaData into JDO MetaData</li>
 * <li>JPA : Merging Annotations information into JPA MetaData</li>
 * <li>JDO : Merging Annotations information into JDO MetaData</li>
 * </ul>
 */
public class MetaDataMerger
{
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Method to take a file JDO MetaData definition and merge in the ORM MetaData definition.
     * If something is specified in the JDO MetaData and also in the ORM MetaData then the ORM MetaData takes precedence.
     * @param primaryFmd The JDO Field definition (to be updated)
     * @param ormFmd The ORM Field definition (to be merged into the JDO Field definition)
     * @throws NucleusException if an error occurs while merging the ORM info
     */
    public static void mergeFileORMData(FileMetaData primaryFmd, FileMetaData ormFmd)
    {
        if (ormFmd == null || primaryFmd == null)
        {
            return;
        }
        if (primaryFmd.isInitialised() || primaryFmd.isPopulated())
        {
            throw new NucleusException(LOCALISER.msg("MetaData.File.AlreadyPopulatedError", 
                primaryFmd.getFilename())).setFatal();
        }
        if (NucleusLogger.METADATA.isDebugEnabled())
        {
            NucleusLogger.METADATA.debug(LOCALISER.msg("044056", primaryFmd.getFilename()));
        }

        if (ormFmd.getCatalog() != null)
        {
            primaryFmd.setCatalog(ormFmd.getCatalog());
        }
        if (ormFmd.getSchema() != null)
        {
            primaryFmd.setSchema(ormFmd.getSchema());
        }
    }

    /**
     * Method to take a class JDO MetaData definition and merge in the ORM MetaData definition.
     * If something is specified in the JDO MetaData and also in the ORM MetaData then the ORM MetaData takes precedence.
     * This is tied pretty intrinsically to the AbstractClassMetaData class and so could have been included there.
     * @param primaryCmd The JDO Class definition (to be updated)
     * @param ormCmd The ORM Class definition (to be merged into the JDO Class definition)
     * @param mmgr MetaData manager
     * @throws NucleusException if an error occurs while merging the ORM info
     */
    public static void mergeClassORMData(AbstractClassMetaData primaryCmd, AbstractClassMetaData ormCmd,
            MetaDataManager mmgr)
    {
        if (ormCmd == null || primaryCmd == null)
        {
            return;
        }
        if (primaryCmd.isInitialised() || primaryCmd.isPopulated())
        {
            throw new NucleusException(LOCALISER.msg("044068", primaryCmd.name)).setFatal();
        }
        if (NucleusLogger.METADATA.isDebugEnabled())
        {
            NucleusLogger.METADATA.debug(LOCALISER.msg("044096", primaryCmd.getFullClassName()));
        }

        // Merge the attributes where they are set in the ORM
        // A). Simple data
        if (ormCmd.getCatalog() != null)
        {
            primaryCmd.catalog = ormCmd.getCatalog();
        }
        if (ormCmd.getSchema() != null)
        {
            primaryCmd.schema = ormCmd.getSchema();
        }
        if (ormCmd.getTable() != null)
        {
            primaryCmd.table = ormCmd.getTable();
        }
        if (ormCmd.detachable)
        {
            primaryCmd.detachable = true;
        }
        if (!ormCmd.requiresExtent)
        {
            primaryCmd.requiresExtent = false;
        }
        if (ormCmd.embeddedOnly)
        {
            primaryCmd.embeddedOnly = true;
        }

        // B). Object data. Assume that if it exists at all we copy it all
        if (ormCmd.getPrimaryKeyMetaData() != null)
        {
            primaryCmd.setPrimaryKeyMetaData(ormCmd.getPrimaryKeyMetaData());
        }
        if (ormCmd.getInheritanceMetaData() != null)
        {
            primaryCmd.setInheritanceMetaData(ormCmd.getInheritanceMetaData());
        }
        if (ormCmd.getIdentityMetaData() != null)
        {
            primaryCmd.setIdentityMetaData(ormCmd.getIdentityMetaData());
        }
        if (ormCmd.getVersionMetaData() != null)
        {
            primaryCmd.setVersionMetaData(ormCmd.getVersionMetaData());
        }
        if (ormCmd.listeners != null)
        {
            if (primaryCmd.listeners == null)
            {
                primaryCmd.listeners = new ArrayList();
            }
            primaryCmd.listeners.addAll(ormCmd.listeners);
        }
        if (ormCmd.queries != null)
        {
            if (primaryCmd.queries == null)
            {
                primaryCmd.queries = new ArrayList();
            }
            else
            {
                primaryCmd.queries.clear();
            }
            primaryCmd.queries.addAll(ormCmd.queries);
        }
        if (ormCmd.joins.size() > 0)
        {
            primaryCmd.joins.clear();
            Iterator iter = ormCmd.joins.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addJoin((JoinMetaData)iter.next());
            }
        }
        if (ormCmd.indexes.size() > 0)
        {
            primaryCmd.indexes.clear();
            Iterator iter = ormCmd.indexes.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addIndex((IndexMetaData)iter.next());
            }
        }
        if (ormCmd.foreignKeys.size() > 0)
        {
            primaryCmd.foreignKeys.clear();
            Iterator iter = ormCmd.foreignKeys.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addForeignKey((ForeignKeyMetaData)iter.next());
            }
        }
        if (ormCmd.uniqueConstraints.size() > 0)
        {
            primaryCmd.uniqueConstraints.clear();
            Iterator iter = ormCmd.uniqueConstraints.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addUniqueConstraint((UniqueMetaData)iter.next());
            }
        }
        if (ormCmd.fetchGroups.size() > 0)
        {
            primaryCmd.fetchGroups.clear();
            Iterator iter = ormCmd.fetchGroups.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addFetchGroup((FetchGroupMetaData)iter.next());
            }
        }
        if (ormCmd.unmappedColumns != null)
        {
            primaryCmd.unmappedColumns = null;
            Iterator<ColumnMetaData> iter = ormCmd.unmappedColumns.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addUnmappedColumn(iter.next());
            }
        }

        // C). Add on any fields that weren't defined previously
        for (int i=0;i<ormCmd.getNoOfMembers();i++)
        {
            AbstractMemberMetaData ormFmd = ormCmd.getMetaDataForMemberAtRelativePosition(i);
            AbstractMemberMetaData primaryFmd = primaryCmd.getMetaDataForMember(ormFmd.getName());
            if (Boolean.TRUE.equals(ormFmd.primaryKey) && (primaryFmd == null || Boolean.FALSE.equals(primaryFmd.primaryKey)))
            {
                // Root metadata (annotations/JDO file) had no PK info for this field but the ORM is trying to set it as the PK!
                throw new NucleusUserException(LOCALISER.msg("044025", ormFmd.getFullFieldName())).setFatal();
            }

            if (primaryFmd == null)
            {
                // Field not specified in JDO MetaData but is in ORM MetaData
                AbstractMemberMetaData fmd = null;
                if (ormFmd.className != null)
                {
                    // Overridden field for superclass that we have no JDO field for
                    // Copy the fmd for the actual class (if any).
                    // TODO Replace this with a copy of the JDO version of the field if available
                    AbstractMemberMetaData jdoFmd = mmgr.readMetaDataForMember(ormFmd.className, ormFmd.name);
                    if (jdoFmd == null)
                    {
                        jdoFmd = mmgr.readMetaDataForMember(ormCmd.getPackageName() + "." + ormFmd.className, ormFmd.name);
                    }
                    if (jdoFmd != null)
                    {
                        // Make a copy of the base field definition and merge the ORM
                        if (jdoFmd instanceof FieldMetaData)
                        {
                            // Copy the JDO definition of the superclass since no JDO definition in this class
                            fmd = new FieldMetaData(primaryCmd, jdoFmd);
                        }
                        else
                        {
                            // Copy the JDO definition of the superclass since no JDO definition in this class
                            fmd = new PropertyMetaData(primaryCmd, (PropertyMetaData)jdoFmd);
                        }
                        fmd.className = ormFmd.className;
                        MetaDataMerger.mergeMemberORMData(fmd, ormFmd);
                    }
                    else
                    {
                        // No base field definition so just copy the ORM
                        if (ormFmd instanceof FieldMetaData)
                        {
                            // Copy ORM field since no available definition
                            fmd = new FieldMetaData(primaryCmd, ormFmd);
                        }
                        else
                        {
                            // Copy ORM property since no available definition
                            fmd = new PropertyMetaData(primaryCmd, (PropertyMetaData)ormFmd);
                        }
                        fmd.className = ormFmd.className;
                    }
                }
                else
                {
                    // Create a copy of the ORM field MetaData and add to JDO MetaData
                    if (ormFmd instanceof FieldMetaData)
                    {
                        fmd = new FieldMetaData(primaryCmd, ormFmd);
                    }
                    else
                    {
                        fmd = new PropertyMetaData(primaryCmd, (PropertyMetaData)ormFmd);
                    }
                }
                primaryCmd.addMember(fmd);
            }
            else
            {
                // Field specified in JDO MetaData so merge in ORM MetaData
                MetaDataMerger.mergeMemberORMData(primaryFmd, ormFmd);
            }
        }

        // Add any extensions supplied in the ORM MetaData
        ExtensionMetaData[] ormExtensions = ormCmd.getExtensions();
        if (ormExtensions != null)
        {
            for (int i=0;i<ormExtensions.length;i++)
            {
                primaryCmd.addExtension(ormExtensions[i].vendorName, ormExtensions[i].key, ormExtensions[i].value);
            }
        }
    }

    /**
     * Method to take a field JDO MetaData definition and merge in the ORM MetaData definition.
     * This is tied pretty intrinsically to the AbstractMemberMetaData class and so could have been included there.
     * @param primaryFmd The JDO Field definition (to be updated)
     * @param ormFmd The ORM Field definition (to be merged into the JDO Class definition)
     * @throws NucleusException if an error occurs while merging the ORM info
     */
    static void mergeMemberORMData(AbstractMemberMetaData primaryFmd, AbstractMemberMetaData ormFmd)
    {
        if (ormFmd == null || primaryFmd == null)
        {
            return;
        }

        if (primaryFmd.isInitialised() || primaryFmd.isPopulated())
        {
            throw new NucleusException(LOCALISER.msg("044107", primaryFmd.getClassName(), primaryFmd.getName())).setFatal();
        }

        if (ormFmd.persistenceModifier != null &&
            ormFmd.persistenceModifier != FieldPersistenceModifier.DEFAULT &&
            primaryFmd.persistenceModifier != ormFmd.persistenceModifier)
        {
            // Take the persistence-modifier from ORM since it is changed
            primaryFmd.persistenceModifier = ormFmd.persistenceModifier;
        }
        if (ormFmd.className != null)
        {
            // If the ORM is an overriding field, make sure we have the (real) class name
            primaryFmd.className = ormFmd.className;
        }
        if (ormFmd.containerMetaData != null)
        {
            primaryFmd.containerMetaData = ormFmd.containerMetaData;
            primaryFmd.containerMetaData.parent = primaryFmd;
        }

        // Update our O/R mapping details
        if (ormFmd.defaultFetchGroup != null)
        {
            primaryFmd.defaultFetchGroup = ormFmd.defaultFetchGroup;
        }
        /*if (Boolean.FALSE.equals(primaryFmd.primaryKey) && Boolean.TRUE.equals(ormFmd.primaryKey))
        {
            primaryFmd.primaryKey = Boolean.valueOf(ormFmd.isPrimaryKey());
        }*/
        if (ormFmd.getTable() != null)
        {
            primaryFmd.table = ormFmd.getTable();
        }
        if (ormFmd.getCatalog() != null)
        {
            primaryFmd.catalog = ormFmd.getCatalog();
        }
        if (ormFmd.getSchema() != null)
        {
            primaryFmd.schema = ormFmd.getSchema();
        }
        if (ormFmd.column != null)
        {
            primaryFmd.column = ormFmd.column;
        }
        if (ormFmd.dependent != null)
        {
            primaryFmd.dependent = ormFmd.dependent;
        }
        if (ormFmd.getMappedBy() != null)
        {
            primaryFmd.mappedBy = ormFmd.getMappedBy();
        }
        if (ormFmd.getValueStrategy() != null)
        {
            primaryFmd.valueStrategy = ormFmd.getValueStrategy();
        }
        if (ormFmd.getSequence() != null)
        {
            primaryFmd.sequence = ormFmd.getSequence();
        }
        if (ormFmd.indexed != null)
        {
            primaryFmd.indexed = ormFmd.indexed;
        }
        if (ormFmd.nullValue != NullValue.NONE)
        {
            primaryFmd.nullValue = ormFmd.nullValue;
        }

        if (ormFmd.getJoinMetaData() != null)
        {
            primaryFmd.setJoinMetaData(ormFmd.joinMetaData);
        }
        if (ormFmd.getEmbeddedMetaData() != null)
        {
            primaryFmd.setEmbeddedMetaData(ormFmd.embeddedMetaData);
        }
        if (ormFmd.getElementMetaData() != null)
        {
            primaryFmd.setElementMetaData(ormFmd.elementMetaData);
        }
        if (ormFmd.getKeyMetaData() != null)
        {
            primaryFmd.setKeyMetaData(ormFmd.keyMetaData);
        }
        if (ormFmd.getValueMetaData() != null)
        {
            primaryFmd.setValueMetaData(ormFmd.valueMetaData);
        }
        if (ormFmd.getOrderMetaData() != null)
        {
            primaryFmd.setOrderMetaData(ormFmd.orderMetaData);
        }
        if (ormFmd.getForeignKeyMetaData() != null)
        {
            primaryFmd.foreignKeyMetaData = ormFmd.getForeignKeyMetaData();
            if (primaryFmd.foreignKeyMetaData != null)
            {
                primaryFmd.foreignKeyMetaData.parent = primaryFmd;
            }
        }
        if (ormFmd.getIndexMetaData() != null)
        {
            primaryFmd.indexMetaData = ormFmd.getIndexMetaData();
            if (primaryFmd.indexMetaData != null)
            {
                primaryFmd.indexMetaData.parent = primaryFmd;
            }
        }
        if (ormFmd.getUniqueMetaData() != null)
        {
            primaryFmd.uniqueMetaData = ormFmd.getUniqueMetaData();
            if (primaryFmd.uniqueMetaData != null)
            {
                primaryFmd.uniqueMetaData.parent = primaryFmd;
            }
        }

        ColumnMetaData[] ormColumns = ormFmd.getColumnMetaData();
        if (ormColumns != null)
        {
            primaryFmd.columns.clear();
            for (int i = 0; i < ormColumns.length; i++)
            {
                primaryFmd.columns.add(ormColumns[i]);
            }
        }

        // Add any extensions supplied in the ORM file
        ExtensionMetaData[] ormExtensions = ormFmd.getExtensions();
        if (ormExtensions != null)
        {
            for (int i=0;i<ormExtensions.length;i++)
            {
                primaryFmd.addExtension(ormExtensions[i].vendorName, ormExtensions[i].key, ormExtensions[i].value);
            }
        }
    }

    /**
     * Method to take a class MetaData definition and merge in any Annotations "MetaData" definition.
     * If something is specified in the MetaData and also in the annotations then the MetaData takes precedence.
     * This is tied pretty intrinsically to the AbstractClassMetaData class and so could have been included there.
     * @param primaryCmd The MetaData definition (to be updated)
     * @param annotCmd The annotations Class definition (to be merged into the MetaData definition)
     * @param mmgr MetaData manager
     * @throws NucleusException if an error occurs while merging the annotations info
     */
    public static void mergeClassAnnotationsData(AbstractClassMetaData primaryCmd, 
            AbstractClassMetaData annotCmd, MetaDataManager mmgr)
    {
        if (annotCmd == null || primaryCmd == null)
        {
            return;
        }
        if (primaryCmd.isInitialised() || primaryCmd.isPopulated())
        {
            throw new NucleusException(LOCALISER.msg("044068", primaryCmd.name)).setFatal();
        }

        if (NucleusLogger.METADATA.isDebugEnabled())
        {
            NucleusLogger.METADATA.debug(LOCALISER.msg("044095", primaryCmd.getFullClassName()));
        }

        // Merge any annotated information that is hanging off the package
        PackageMetaData annotPmd = annotCmd.getPackageMetaData();
        if (annotPmd.getSequences() != null)
        {
            // Register the sequences since the register process for the primaryCmd has passed
            mmgr.registerSequencesForFile(annotCmd.getPackageMetaData().getFileMetaData());
            SequenceMetaData[] seqmds = annotPmd.getSequences();
            for (int i=0;i<seqmds.length;i++)
            {
                primaryCmd.getPackageMetaData().addSequence(seqmds[i]);
            }
        }
        if (annotPmd.getTableGenerators() != null)
        {
            // Register the table generators since the register process for the primaryCmd has passed
            mmgr.registerTableGeneratorsForFile(annotCmd.getPackageMetaData().getFileMetaData());
            TableGeneratorMetaData[] tablegenmds = annotPmd.getTableGenerators();
            for (int i=0;i<tablegenmds.length;i++)
            {
                primaryCmd.getPackageMetaData().addTableGenerator(tablegenmds[i]);
            }
        }

        // Merge the attributes where they arent set on the primary and are on the annotations
        // A). Simple attributes
        if (primaryCmd.entityName == null && annotCmd.entityName != null)
        {
            primaryCmd.entityName = annotCmd.entityName;
        }
        if (annotCmd.detachable)
        {
            primaryCmd.detachable = true;
        }
        if (!annotCmd.requiresExtent)
        {
            primaryCmd.requiresExtent = false;
        }
        if (annotCmd.embeddedOnly)
        {
            primaryCmd.embeddedOnly = true;
        }
        if (primaryCmd.identityType == null && annotCmd.identityType != null)
        {
            primaryCmd.identityType = annotCmd.identityType;
        }
        if (primaryCmd.objectidClass == null && annotCmd.objectidClass != null)
        {
            primaryCmd.objectidClass = annotCmd.objectidClass;
        }

        if (primaryCmd.catalog == null && annotCmd.catalog != null)
        {
            primaryCmd.catalog = annotCmd.catalog;
        }
        if (primaryCmd.schema == null && annotCmd.schema != null)
        {
            primaryCmd.schema = annotCmd.schema;
        }
        if (primaryCmd.table == null && annotCmd.table != null)
        {
            primaryCmd.table = annotCmd.table;
        }

        // B). Object data - assume that we copy it all if not set at all
        if (primaryCmd.versionMetaData == null && annotCmd.versionMetaData != null)
        {
            primaryCmd.setVersionMetaData(annotCmd.versionMetaData);
        }
        if (primaryCmd.identityMetaData == null && annotCmd.identityMetaData != null)
        {
            primaryCmd.setIdentityMetaData(annotCmd.identityMetaData);
        }
        if (primaryCmd.inheritanceMetaData == null && annotCmd.inheritanceMetaData != null)
        {
            primaryCmd.setInheritanceMetaData(annotCmd.inheritanceMetaData);
        }
        if (primaryCmd.primaryKeyMetaData == null && annotCmd.primaryKeyMetaData != null)
        {
            primaryCmd.setPrimaryKeyMetaData(annotCmd.primaryKeyMetaData);
        }
        if (primaryCmd.listeners == null && annotCmd.listeners != null)
        {
            // No MetaData listeners so just use those of the annotations
            Iterator iter = annotCmd.listeners.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addListener((EventListenerMetaData)iter.next());
            }
        }
        else if (primaryCmd.listeners != null && annotCmd.listeners != null)
        {
            // We have listeners in MetaData and also in Annotations. Listeners can be for the actual class, or for
            // any EntityListener, so use overriding in those two groups
            if (primaryCmd.getListenerForClass(primaryCmd.getFullClassName()) == null)
            {
                // Primary has just Listeners and no callbacks
                if (annotCmd.getListenerForClass(primaryCmd.getFullClassName()) != null)
                {
                    // Add on callbacks from annotations
                    primaryCmd.addListener(annotCmd.getListenerForClass(primaryCmd.getFullClassName()));
                }
            }
            else if (primaryCmd.getListenerForClass(primaryCmd.getFullClassName()) != null && 
                primaryCmd.getListeners().size() == 1)
            {
                // Primary has just callbacks and no listeners so take any listeners from annotations
                List annotListeners = annotCmd.getListeners();
                Iterator annotIter = annotListeners.iterator();
                while (annotIter.hasNext())
                {
                    EventListenerMetaData elmd = (EventListenerMetaData)annotIter.next();
                    if (!elmd.getClassName().equals(primaryCmd.getFullClassName()))
                    {
                        // Add on listeners from annotations
                        primaryCmd.addListener(elmd);
                    }
                }
            }
        }
        if (annotCmd.excludeDefaultListeners != null && primaryCmd.excludeDefaultListeners == null)
        {
            primaryCmd.excludeDefaultListeners = annotCmd.excludeDefaultListeners;
        }
        if (annotCmd.excludeSuperClassListeners != null && primaryCmd.excludeSuperClassListeners == null)
        {
            primaryCmd.excludeSuperClassListeners = annotCmd.excludeSuperClassListeners;
        }

        if (primaryCmd.queries == null && annotCmd.queries != null)
        {
            Iterator iter = annotCmd.queries.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addQuery((QueryMetaData)iter.next());
            }
        }
        if (primaryCmd.joins.size() == 0 && annotCmd.joins.size() > 0)
        {
            Iterator iter = annotCmd.joins.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addJoin((JoinMetaData)iter.next());
            }
        }
        if (primaryCmd.indexes.size() == 0 && annotCmd.indexes.size() > 0)
        {
            Iterator iter = annotCmd.indexes.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addIndex((IndexMetaData)iter.next());
            }
        }
        if (primaryCmd.foreignKeys.size() == 0 && annotCmd.foreignKeys.size() > 0)
        {
            Iterator iter = annotCmd.foreignKeys.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addForeignKey((ForeignKeyMetaData)iter.next());
            }
        }
        if (primaryCmd.uniqueConstraints.size() == 0 && annotCmd.uniqueConstraints.size() > 0)
        {
            Iterator iter = annotCmd.uniqueConstraints.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addUniqueConstraint((UniqueMetaData)iter.next());
            }
        }
        if (primaryCmd.fetchGroups.size() == 0 && annotCmd.fetchGroups.size() > 0)
        {
            Iterator iter = annotCmd.fetchGroups.iterator();
            while (iter.hasNext())
            {
                primaryCmd.addFetchGroup((FetchGroupMetaData)iter.next());
            }
        }

        // C). Add on any fields that weren't defined previously
        for (int i=0;i<annotCmd.getNoOfMembers();i++)
        {
            AbstractMemberMetaData annotFmd = annotCmd.getMetaDataForMemberAtRelativePosition(i);
            AbstractMemberMetaData primaryFmd = primaryCmd.getMetaDataForMember(annotFmd.getName());
            if (primaryFmd == null)
            {
                // Field not specified in MetaData but is in Annotations
                AbstractMemberMetaData fmd = null;
                if (annotFmd.className != null)
                {
                    // Overridden field for superclass that we have no MetaData field for
                    // Copy the fmd for the actual class (if any).
                    // TODO Replace this with a copy of the metadata version of the field if available
                    AbstractMemberMetaData baseFmd = mmgr.readMetaDataForMember(annotFmd.className, annotFmd.name);
                    if (baseFmd == null)
                    {
                        baseFmd = mmgr.readMetaDataForMember(annotCmd.getPackageName() + "." + annotFmd.className, annotFmd.name);
                    }
                    if (baseFmd != null)
                    {
                        // Make a copy of the base field definition and merge the Annotations
                        if (baseFmd instanceof FieldMetaData)
                        {
                            // Copy the JDO definition of the superclass since no JDO definition in this class
                            fmd = new FieldMetaData(primaryCmd, baseFmd);
                        }
                        else
                        {
                            // Copy the JDO definition of the superclass since no JDO definition in this class
                            fmd = new PropertyMetaData(primaryCmd, (PropertyMetaData)baseFmd);
                        }
                        fmd.className = annotFmd.className;
                        MetaDataMerger.mergeMemberAnnotationsData(fmd, annotFmd);
                    }
                    else
                    {
                        // No base field definition so just copy the Annotations
                        if (annotFmd instanceof FieldMetaData)
                        {
                            // Create default field since no available definition
                            fmd = new FieldMetaData(primaryCmd, annotFmd);
                        }
                        else
                        {
                            // Create default property since no available definition
                            fmd = new PropertyMetaData(primaryCmd, (PropertyMetaData)annotFmd);
                        }
                        fmd.className = annotFmd.className;
                    }
                }
                else
                {
                    // Create a copy of the Annotations "MetaData" and add
                    if (annotFmd instanceof FieldMetaData)
                    {
                        // Annotation definition of the field
                        // creates a new fmd with this as parent and add to fields
                        fmd = new FieldMetaData(primaryCmd, annotFmd);
                    }
                    else
                    {
                        // Annotation definition of the property
                        // creates a new fmd with this as parent and add to fields
                        fmd = new PropertyMetaData(primaryCmd, (PropertyMetaData)annotFmd);
                    }
                }
                primaryCmd.addMember(fmd);
            }
            else
            {
                // Field specified in JDO MetaData so merge in Annotations
                MetaDataMerger.mergeMemberAnnotationsData(primaryFmd, annotFmd);
            }
        }

        // Add any extensions supplied in the annotations
        ExtensionMetaData[] ormExtensions = annotCmd.getExtensions();
        if (ormExtensions != null)
        {
            for (int i=0;i<ormExtensions.length;i++)
            {
                primaryCmd.addExtension(ormExtensions[i].vendorName, ormExtensions[i].key, ormExtensions[i].value);
            }
        }
    }

    /**
     * Method to take a field/property MetaData definition and merge in the Annotations "MetaData" definition.
     * This is tied pretty intrinsically to the AbstractMemberMetaData class and so could have been included there.
     * @param primaryFmd The MetaData Field definition (to be updated)
     * @param annotFmd The Annotations "MetaData" Field definition (to be merged into the MetaData definition)
     * @throws NucleusException if an error occurs while merging the annotation info
     */
    static void mergeMemberAnnotationsData(AbstractMemberMetaData primaryFmd, AbstractMemberMetaData annotFmd)
    {
        if (annotFmd == null || primaryFmd == null)
        {
            return;
        }

        if (primaryFmd.isInitialised() || primaryFmd.isPopulated())
        {
            throw new NucleusException(LOCALISER.msg("044107", primaryFmd.getClassName(), primaryFmd.getName())).setFatal();
        }

        if (primaryFmd.className == null && annotFmd.className != null)
        {
            // If the Annotation is an overriding field, make sure we have the (real) class name
            primaryFmd.className = annotFmd.className;
        }
        if (primaryFmd.containerMetaData == null && annotFmd.containerMetaData != null)
        {
            primaryFmd.containerMetaData = annotFmd.containerMetaData;
            primaryFmd.containerMetaData.parent = primaryFmd;
        }

        if (annotFmd.storeInLob)
        {
            primaryFmd.storeInLob = true;
        }
        if (annotFmd.persistenceModifier != FieldPersistenceModifier.DEFAULT &&
            primaryFmd.persistenceModifier == FieldPersistenceModifier.DEFAULT)
        {
            primaryFmd.persistenceModifier = annotFmd.persistenceModifier;
        }
        if (annotFmd.defaultFetchGroup != null && primaryFmd.defaultFetchGroup == null)
        {
            primaryFmd.defaultFetchGroup = annotFmd.defaultFetchGroup;
        }
        if (annotFmd.primaryKey != null)
        {
            // "primary-key" will always have a value in XML so we just override if the annotation had it set
            // This means that we don't allow overriding of the primary-key via XML
            primaryFmd.primaryKey = annotFmd.primaryKey;
        }
        if (primaryFmd.table == null && annotFmd.table != null)
        {
            primaryFmd.table = annotFmd.table;
        }
        if (primaryFmd.catalog == null && annotFmd.catalog != null)
        {
            primaryFmd.catalog = annotFmd.catalog;
        }
        if (primaryFmd.schema == null && annotFmd.schema != null)
        {
            primaryFmd.schema = annotFmd.schema;
        }
        if (primaryFmd.column == null && annotFmd.column != null)
        {
            primaryFmd.column = annotFmd.column;
        }
        if (primaryFmd.dependent == null && annotFmd.dependent != null)
        {
            primaryFmd.dependent = annotFmd.dependent;
        }
        if (primaryFmd.mappedBy == null && annotFmd.mappedBy != null)
        {
            primaryFmd.mappedBy = annotFmd.mappedBy;
        }
        if (primaryFmd.valueStrategy == null && annotFmd.valueStrategy != null)
        {
            primaryFmd.valueStrategy = annotFmd.valueStrategy;
        }
        if (primaryFmd.sequence == null && annotFmd.sequence != null)
        {
            primaryFmd.sequence = annotFmd.sequence;
        }
        if (primaryFmd.valueGeneratorName == null && annotFmd.valueGeneratorName != null)
        {
            primaryFmd.valueGeneratorName = annotFmd.valueGeneratorName;
        }
        if (primaryFmd.indexed == null && annotFmd.indexed != null)
        {
            primaryFmd.indexed = annotFmd.indexed;
        }
        if (annotFmd.nullValue != NullValue.NONE)
        {
            primaryFmd.nullValue = annotFmd.nullValue;
        }
        if (annotFmd.cascadePersist != null && primaryFmd.cascadePersist == null)
        {
            primaryFmd.cascadePersist = annotFmd.cascadePersist;
        }
        if (annotFmd.cascadeUpdate != null && primaryFmd.cascadeUpdate == null)
        {
            primaryFmd.cascadeUpdate = annotFmd.cascadeUpdate;
        }
        if (annotFmd.cascadeDelete != null && primaryFmd.cascadeDelete == null)
        {
            primaryFmd.cascadeDelete = annotFmd.cascadeDelete;
        }
        if (annotFmd.cascadeRefresh != null && primaryFmd.cascadeRefresh == null)
        {
            primaryFmd.cascadeRefresh = annotFmd.cascadeRefresh;
        }

        if (primaryFmd.joinMetaData == null && annotFmd.joinMetaData != null)
        {
            primaryFmd.setJoinMetaData(annotFmd.joinMetaData);
        }
        if (primaryFmd.embeddedMetaData == null && annotFmd.embeddedMetaData != null)
        {
            primaryFmd.setEmbeddedMetaData(annotFmd.embeddedMetaData);
        }
        if (primaryFmd.elementMetaData == null && annotFmd.elementMetaData != null)
        {
            primaryFmd.setElementMetaData(annotFmd.elementMetaData);
        }
        if (primaryFmd.keyMetaData == null && annotFmd.keyMetaData != null)
        {
            primaryFmd.setKeyMetaData(annotFmd.keyMetaData);
        }
        if (primaryFmd.valueMetaData == null && annotFmd.valueMetaData != null)
        {
            primaryFmd.setValueMetaData(annotFmd.valueMetaData);
        }
        if (primaryFmd.orderMetaData == null && annotFmd.orderMetaData != null)
        {
            primaryFmd.setOrderMetaData(annotFmd.orderMetaData);
        }
        if (primaryFmd.foreignKeyMetaData == null && annotFmd.foreignKeyMetaData != null)
        {
            primaryFmd.foreignKeyMetaData = annotFmd.foreignKeyMetaData;
            if (primaryFmd.foreignKeyMetaData != null)
            {
                primaryFmd.foreignKeyMetaData.parent = primaryFmd;
            }
        }
        if (primaryFmd.indexMetaData == null && annotFmd.indexMetaData != null)
        {
            primaryFmd.indexMetaData = annotFmd.indexMetaData;
            if (primaryFmd.indexMetaData != null)
            {
                primaryFmd.indexMetaData.parent = primaryFmd;
            }
        }
        if (primaryFmd.uniqueMetaData == null && annotFmd.uniqueMetaData != null)
        {
            primaryFmd.uniqueMetaData = annotFmd.uniqueMetaData;
            if (primaryFmd.uniqueMetaData != null)
            {
                primaryFmd.uniqueMetaData.parent = primaryFmd;
            }
        }

        if (primaryFmd.columns.size() == 0 && annotFmd.columns.size() > 0)
        {
            // Columns specified in annotations but not in MetaData
            ColumnMetaData[] annotColumns = annotFmd.getColumnMetaData();
            if (annotColumns != null)
            {
                for (int i = 0; i < annotColumns.length; i++)
                {
                    primaryFmd.columns.add(annotColumns[i]);
                }
            }
        }

        // Add any extensions supplied in the annotations
        ExtensionMetaData[] annotExtensions = annotFmd.getExtensions();
        if (annotExtensions != null)
        {
            for (int i=0;i<annotExtensions.length;i++)
            {
                primaryFmd.addExtension(annotExtensions[i].vendorName, annotExtensions[i].key, annotExtensions[i].value);
            }
        }
    }
}