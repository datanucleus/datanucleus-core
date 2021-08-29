/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.MacroString;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of the MetaData of a class/interface.
 * Has a parent PackageMetaData that can contain the metadata for several classes/interfaces. 
 * Is extended by ClassMetaData and InterfaceMetaData.
 * Of the things that it contains the main ones are the "members" which are the MetaData for all fields and properties that are persistable.
 */
public abstract class AbstractClassMetaData extends MetaData
{
    private static final long serialVersionUID = -2433561862769017940L;

    /** Suffix to add on to the class name for any generated primary key class. */
    public static final String GENERATED_PK_SUFFIX = "_PK";

    /** Manager for this metadata. Set at populate. */
    protected transient MetaDataManager mmgr;

    /** Class name */
    protected final String name;

    /** Entity name. Required by JPA/Jakarta $4.3.1 for accessing this class in queries. */
    protected String entityName;

    /** Whether this class is explicitly marked as MappedSuperclass. Will be false when using JDO. */
    protected boolean mappedSuperclass = false;

    /** 
     * Whether the class is fully defined, and hence instantiable. This is false when it is a JPA/Jakarta MappedSuperclass
     * and has no PK fields defined (will be defined in the derived Entity). 
     * This is different to whether the class is abstract - use ClassMetaData.isAbstract() for that.
     */
    protected boolean instantiable = true;

    /** Whether the class has been explicitly marked as using FIELD access (JPA/Jakarta). */
    protected Boolean accessViaField = null;

    /** Identity-type tag value. */
    protected IdentityType identityType = IdentityType.DATASTORE;

    /** persistence-modifier tag value. */
    protected ClassPersistenceModifier persistenceModifier = ClassPersistenceModifier.PERSISTENCE_CAPABLE;

    /** persistable-superclass tag value (deprecated). */
    protected String persistableSuperclass;

    /** objectid-class tag value. */
    protected String objectidClass;

    /** requires-extent tag value. */
    protected boolean requiresExtent = true;

    /** detachable tag value. */
    protected boolean detachable = false;

    /** embedded-only tag value. */
    protected boolean embeddedOnly = false;

    /** Catalog name (O/R mapping). */
    protected String catalog;

    /** Schema name (O/R mapping). */
    protected String schema;

    /** Table name (O/R mapping). This may be of the form "[database].[catalog].[schema].table" */
    protected String table;

    /** cacheable tag value. */
    protected Boolean cacheable = null;

    /** Full name (e.g org.datanucleus.MyClass) */
    protected final String fullName;

    protected VersionMetaData versionMetaData;

    protected DatastoreIdentityMetaData datastoreIdentityMetaData;

    protected MultitenancyMetaData multitenancyMetaData;

    protected SoftDeleteMetaData softDeleteMetaData;

    /** Flag whether the identity was specified by the user. */
    protected boolean identitySpecified = false;

    protected InheritanceMetaData inheritanceMetaData;

    protected PrimaryKeyMetaData primaryKeyMetaData;

    /** EventListeners. Use a list to preserve ordering. */
    protected List<EventListenerMetaData> listeners = null;

    /** Flag to exclude superclass listeners. */
    protected Boolean excludeSuperClassListeners = null;

    /** Flag to exclude default listeners. */
    protected Boolean excludeDefaultListeners = null;

    /** Convenience lookup map of fetch group by the name. */
    protected Map<String, FetchGroupMetaData> fetchGroupMetaDataByName;

    /** Class MetaData for the persistable superclass (if any) */
    protected AbstractClassMetaData pcSuperclassMetaData = null;

    /** Flag for whether the MetaData here is complete without annotations. Used by JPA. */
    protected boolean metaDataComplete = false;

    /** Whether to lock objects of this type on read operations. */
    protected boolean serializeRead = false;

    /** Named queries */
    protected Collection<QueryMetaData> queries = null;

    /** Named stored procs */
    protected Collection<StoredProcQueryMetaData> storedProcQueries = null;

    /** List of query result MetaData defined for this file. */
    protected Collection<QueryResultMetaData> queryResultMetaData = null;

    /** The columns that are present in the datastore yet not mapped to fields in this class. */
    protected List<ColumnMetaData> unmappedColumns = null;

    protected Set<FetchGroupMetaData> fetchGroups = null;

    protected List<ForeignKeyMetaData> foreignKeys = null;

    protected List<IndexMetaData> indexes = null;

    protected List<UniqueMetaData> uniqueConstraints = null;

    protected List<JoinMetaData> joins = null;

    /** List of all members (fields/properties). */
    protected List<AbstractMemberMetaData> members = new ArrayList<>();

    /** Managed fields/properties of this class. Subset of the AbstractMemberMetaData objects that are in "members", excluding "overriddenMembers". */
    protected AbstractMemberMetaData[] managedMembers;

    /** Fields/properties for superclasses that are overridden in this class. */
    protected AbstractMemberMetaData[] overriddenMembers;

    /** Position numbers of members mapped by the name of the field/property. */
    protected Map<String, Integer> memberPositionsByName;

    /** Positions of all fields/properties (inc superclasses). */
    protected int[] allMemberPositions;

    /** Positions of the primary-key fields/properties (inc superclasses). */
    protected int[] pkMemberPositions;

    /** Positions of the non-primary-key fields/properties (inc superclasses). */
    protected int[] nonPkMemberPositions;

    /** Flags of the non-primary key fields/properties (inc superclasses). */
    protected boolean[] nonPkMemberFlags;

    /** Positions of the default-fetch-group fields/properties (inc superclasses). */
    protected int[] dfgMemberPositions;

    /** Flags of the default-fetch-group state for all fields/properties. */
    protected boolean[] dfgMemberFlags;

    /** Positions of the SCO mutable fields/properties (inc superclasses). */
    protected int[] scoMutableMemberPositions;

    /** Flags of the SCO mutable state for all fields/properties. */
    protected boolean[] scoMutableMemberFlags;

    protected boolean[] scoContainerMemberFlags;

    /** Absolute positions of all SCO fields/properties that aren't containers. */
    protected int[] scoNonContainerMemberPositions = null;

    /** Absolute positions of the fields/properties that have relations. */
    protected int[] relationPositions = null;

    /** No of managed fields/properties in superclasses, that are inherited by this class. */
    protected int noOfInheritedManagedMembers = 0;

    /** if this persistable class uses SingleFieldIdentity */
    protected boolean usesSingleFieldIdentityClass;

    /** number of managed fields/properties from this class plus inherited classes. */
    protected int memberCount;

    protected boolean implementationOfPersistentDefinition = false;

    /** whether the populate method is running **/
    boolean populating = false;

    /** whether the initialise method is running **/
    boolean initialising = false;

    /** Cached result of {@link #hasFetchGroupWithPostLoad()} */
    protected Boolean fetchGroupMetaWithPostLoad;

    /** Cached result of {@link #pkIsDatastoreAttributed(StoreManager)} */
    protected Boolean pkIsDatastoreAttributed = null;

    /** Cached result of {@link #hasRelations(ClassLoaderResolver)} */
    protected Boolean hasRelations = null;

    /** Implementation of "persistent-interface" needing table setting from superclass. */
    protected transient boolean persistentInterfaceImplNeedingTableFromSuperclass = false;

    /** Implementation of "persistent-interface" needing table setting from subclass. */
    protected transient boolean persistentInterfaceImplNeedingTableFromSubclass = false;

    /**
     * Constructor. Set fields using setters, before populate().
     * @param parent The package to which this class/interface belongs
     * @param name (Simple) name of class (omitting the package name)
     */
    protected AbstractClassMetaData(final PackageMetaData parent, final String name)
    {
        super(parent);

        if (StringUtils.isWhitespace(name))
        {
            throw new InvalidMetaDataException("044061", parent.name);
        }

        this.name = name;
        this.fullName = ClassUtils.createFullClassName(parent.name, name);
    }

    /**
     * Constructor for creating the ClassMetaData for an implementation of a "persistent-interface".
     * @param imd MetaData for the "persistent-interface"
     * @param implClassName Name of the implementation class
     * @param copyMembers Whether to copy the fields/properties of the interface too
     */
    public AbstractClassMetaData(InterfaceMetaData imd, String implClassName, boolean copyMembers)
    {
        this((PackageMetaData)imd.parent, implClassName);
        setMappedSuperclass(imd.mappedSuperclass);
        setRequiresExtent(imd.requiresExtent);
        setDetachable(imd.detachable);
        setTable(imd.table);
        setCatalog(imd.catalog);
        setSchema(imd.schema);
        setEntityName(imd.entityName);
        setObjectIdClass(imd.objectidClass);
        setPersistenceModifier(ClassPersistenceModifier.PERSISTENCE_CAPABLE);
        setEmbeddedOnly(imd.embeddedOnly);
        setIdentityType(imd.identityType);

        implementationOfPersistentDefinition = true;

        if (copyMembers)
        {
            // Adds FieldMetaData for each PropertyMetaData on the persistent-interface.
            for (int i=0; i<imd.getMemberCount(); i++)
            {
                FieldMetaData fmd = new FieldMetaData(this, imd.getMetaDataForManagedMemberAtAbsolutePosition(i));
                addMember(fmd);
            }
        }

        setVersionMetaData(imd.versionMetaData);
        setDatastoreIdentityMetaData(imd.datastoreIdentityMetaData);
        setPrimaryKeyMetaData(imd.primaryKeyMetaData);

        if (imd.inheritanceMetaData != null)
        {
            if (imd.inheritanceMetaData.getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE)
            {
                // Flag the table as requiring setting based on the next superclass
                persistentInterfaceImplNeedingTableFromSuperclass = true;
            }
            else if (imd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
            {
                // Flag the table as requiring setting based on the next subclass
                persistentInterfaceImplNeedingTableFromSubclass = true;
            }

            InheritanceMetaData inhmd = new InheritanceMetaData();
            inhmd.setStrategy(InheritanceStrategy.NEW_TABLE);
            if (imd.inheritanceMetaData.getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE)
            {
                AbstractClassMetaData acmd = imd.getSuperAbstractClassMetaData();
                while (acmd != null)
                {
                    if (acmd.getInheritanceMetaData() != null)
                    {
                        if (acmd.getInheritanceMetaData().getStrategy()==InheritanceStrategy.NEW_TABLE)
                        {
                            if (acmd.getInheritanceMetaData().getDiscriminatorMetaData() != null)
                            {
                                inhmd.setDiscriminatorMetaData(new DiscriminatorMetaData(acmd.getInheritanceMetaData().getDiscriminatorMetaData()));
                            }
                            inhmd.setJoinMetaData(acmd.getInheritanceMetaData().getJoinMetaData());
                            break;
                        }
                    }
                    acmd = acmd.getSuperAbstractClassMetaData();
                }
            }
            else if (imd.inheritanceMetaData.getStrategy() == InheritanceStrategy.NEW_TABLE)
            {
                if (imd.getInheritanceMetaData().getDiscriminatorMetaData() != null)
                {
                    inhmd.setDiscriminatorMetaData(new DiscriminatorMetaData(imd.getInheritanceMetaData().getDiscriminatorMetaData()));
                }
                inhmd.setJoinMetaData(imd.getInheritanceMetaData().getJoinMetaData());
            }
            setInheritanceMetaData(inhmd);
        }
        
        if (imd.joins != null)
        {
            for (JoinMetaData joinmd : imd.joins)
            {
                addJoin(joinmd);
            }
        }
        if (imd.foreignKeys != null)
        {
            for (ForeignKeyMetaData fkmd : imd.foreignKeys)
            {
                addForeignKey(fkmd);
            }
        }
        if (imd.indexes != null)
        {
            for (IndexMetaData idxmd : imd.indexes)
            {
                addIndex(idxmd);
            }
        }
        if (imd.uniqueConstraints != null)
        {
            for (UniqueMetaData unimd : imd.uniqueConstraints)
            {
                addUniqueConstraint(unimd);
            }
        }
        if (imd.fetchGroups != null)
        {
            for (FetchGroupMetaData fgmd : imd.fetchGroups)
            {
                addFetchGroup(fgmd);
            }
        }
        if (imd.queries != null)
        {
            for (QueryMetaData query : imd.queries)
            {
                addQuery(query);
            }
        }
        if (imd.storedProcQueries != null)
        {
            for (StoredProcQueryMetaData query : imd.storedProcQueries)
            {
                addStoredProcQuery(query);
            }
        }

        if (imd.listeners != null)
        {
            if (listeners == null)
            {
                listeners = new ArrayList<>();
            }
            listeners.addAll(imd.listeners);
        }
    }

    /**
     * Constructor for creating the ClassMetaData for an implementation of a "persistent-abstract-class".
     * @param cmd MetaData for the implementation of the "persistent-abstract-class"
     * @param implClassName Name of the implementation class
     */
    public AbstractClassMetaData(ClassMetaData cmd, String implClassName)
    {
        this((PackageMetaData)cmd.parent, implClassName);
        setMappedSuperclass(cmd.mappedSuperclass);
        setRequiresExtent(cmd.requiresExtent);
        setDetachable(cmd.detachable);
        setCatalog(cmd.catalog);
        setSchema(cmd.schema);
        setTable(cmd.table);
        setEntityName(cmd.entityName);
        setPersistenceModifier(ClassPersistenceModifier.PERSISTENCE_CAPABLE);
        setEmbeddedOnly(cmd.embeddedOnly);
        setIdentityType(cmd.identityType);

        this.persistableSuperclass = cmd.getFullClassName();
        this.implementationOfPersistentDefinition = true;

        // Mark all artificial fields (added in implementing the abstract class) as non-persistent
        for (int i=0; i<cmd.getMemberCount(); i++)
        {
            FieldMetaData fmd = new FieldMetaData(this, cmd.getMetaDataForManagedMemberAtAbsolutePosition(i));
            fmd.persistenceModifier = FieldPersistenceModifier.NONE;
            fmd.primaryKey = Boolean.FALSE;
            fmd.defaultFetchGroup = Boolean.FALSE;
            addMember(fmd);
        }
    }

    public MetaDataManager getMetaDataManager()
    {
        return mmgr;
    }

    public boolean isInstantiable()
    {
        return instantiable;
    }

    protected AbstractClassMetaData getRootInstantiableClass()
    {
        if (pcSuperclassMetaData == null)
        {
            if (instantiable)
            {
                return this;
            }
        }
        else
        {
            AbstractClassMetaData rootCmd = pcSuperclassMetaData.getRootInstantiableClass();
            return (rootCmd == null && instantiable) ? this : rootCmd;
        }
        return null;
    }

    public boolean isRootInstantiableClass()
    {
        return getRootInstantiableClass() == this;
    }

    /**
     * Return whether this MetaData is for an implementation of a persistent definition.
     * This could be an implementation of a persistent interface or a persistent abstract-class.
     * @return Whether this is an implementation
     */
    public boolean isImplementationOfPersistentDefinition()
    {
        return implementationOfPersistentDefinition;
    }

    /**
     * Method to check whether the Meta-Data has been initialised.
     * @throws NucleusException Thrown if the Meta-Data hasn't been initialised. 
     **/
    protected void checkInitialised()
    {
        if (!isInitialised())
        {
            throw new NucleusException(Localiser.msg("044069",fullName)).setFatal();
        }
    }

    /**
     * Method to check whether the Meta-Data has been populated.
     * @throws NucleusException Thrown if the Meta-Data hasn't been populated. 
     **/
    protected void checkPopulated()
    {
        if (!isPopulated() && !isInitialised())
        {
            throw new NucleusException(Localiser.msg("044070",fullName)).setFatal();
        }
    }

    /**
     * Method to check that the Meta-Data has not been populated yet.
     * @throws NucleusUserException Thrown if the Meta-Data has already been populated. 
     **/
    protected void checkNotYetPopulated()
    {
        if (isPopulated() || isInitialised())
        {
            // TODO Localise message
            throw new NucleusUserException("Already populated/initialised");
        }
    }

    /**
     * Load the persistent interface/class
     * @param clr the ClassLoader
     * @param primary the primary ClassLoader to use (or null)
     * @return the loaded class
     */
    protected Class loadClass(ClassLoaderResolver clr, ClassLoader primary)
    {
        // No class loader, so use default
        if (clr == null)
        {
            NucleusLogger.METADATA.warn(Localiser.msg("044067",fullName));
            clr = mmgr.getNucleusContext().getClassLoaderResolver(null);
        }
    
        // Load the class we are modelling
        Class cls;
        try
        {
            cls = clr.classForName(fullName,primary,false);
            if (cls == null)
            {
                NucleusLogger.METADATA.error(Localiser.msg("044080",fullName));
                throw new InvalidClassMetaDataException("044080", fullName);
            }
        }
        catch (ClassNotResolvedException cnre)
        {
            NucleusLogger.METADATA.error(Localiser.msg("044080",fullName));
            NucleusException ne = new InvalidClassMetaDataException("044080", fullName);
            ne.setNestedException(cnre);
            throw ne;
        }
        return cls;
    }

    /**
     * Determines the identity based on MetaData defaults or user defined MetaData
     */
    protected void determineIdentity()
    {
        // Provide a default identity type if not supplied
        if (this.identityType == null)
        {
            if (objectidClass != null)
            {
                // PK provided so we use application-identity
                identityType = IdentityType.APPLICATION;
            }
            else
            {
                int noOfPkKeys = 0;
                Iterator<AbstractMemberMetaData> memberIter = members.iterator();
                while (memberIter.hasNext())
                {
                    AbstractMemberMetaData mmd = memberIter.next();
                    if (mmd.isPrimaryKey())
                    {
                        noOfPkKeys++;
                    }
                }
                identityType = (noOfPkKeys > 0) ? IdentityType.APPLICATION : IdentityType.DATASTORE;
            }
        }
    }

    /**
     * Determine the nearest superclass that is persistable (if any).
     * @param clr The ClassLoaderResolver
     * @param cls This class
     * @throws InvalidMetaDataException if the super class cannot be loaded by the <code>clr</code>. 
     * @throws InvalidMetaDataException if the declared <code>persistence-capable-superclass</code> is not actually assignable from <code>cls</code> 
     * @throws InvalidMetaDataException if any of the super classes is persistable, but the MetaData says that class is not persistent. 
     */
    protected void determineSuperClassName(ClassLoaderResolver clr, Class cls)
    {
        // Find the true superclass name (using reflection)
        String realPcSuperclassName = null;
        Collection<Class<?>> superclasses;
        if (cls.isInterface())
        {
            superclasses = ClassUtils.getSuperinterfaces(cls);
        }
        else
        {
            superclasses = ClassUtils.getSuperclasses(cls);
        }

        for (Class<?> superclass : superclasses)
        {
            AbstractClassMetaData superCmd = mmgr.getMetaDataForClassInternal(superclass, clr);
            if (superCmd != null && superCmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                realPcSuperclassName = superclass.getName();
                break;
            }
        }

        persistableSuperclass = realPcSuperclassName;
        if (persistableSuperclass != null)
        {
            Class pcsc = null;
            try
            {
                // Load using same class loader resolver as this class
                pcsc = clr.classForName(persistableSuperclass);
            }
            catch (ClassNotResolvedException cnre)
            {
                throw new InvalidClassMetaDataException("044081", fullName, persistableSuperclass);
            }

            if (persistableSuperclass.equals(fullName) ||  !clr.isAssignableFrom(pcsc.getName(), cls) )
            {
                throw new InvalidClassMetaDataException("044082", fullName, persistableSuperclass);
            }

            // Retrieve the Meta-Data for the superclass
            if (mmgr != null)
            {
                // Normal operation will have a MetaDataManager and so ask that for MetaData of superclass.
                pcSuperclassMetaData = mmgr.getMetaDataForClassInternal(pcsc, clr);
                if (pcSuperclassMetaData == null)
                {
                    // Superclass isn't persistence capable since no MetaData could be found.
                    throw new InvalidClassMetaDataException("044083", fullName, persistableSuperclass);
                }
            }
            else
            {
                // The enhancer doesn't need MetaDataManager so just navigate to FileMetaData and find it.
                // NOTE : assumes that the class is specified in the same file 
                String superclassPkgName = persistableSuperclass.substring(0,persistableSuperclass.lastIndexOf('.'));
                PackageMetaData pmd = getPackageMetaData().getFileMetaData().getPackage(superclassPkgName);
                if (pmd != null)
                {
                    String superclassClsName = persistableSuperclass.substring(persistableSuperclass.lastIndexOf('.')+1);
                    pcSuperclassMetaData = pmd.getClass(superclassClsName);
                }
            }
            if (pcSuperclassMetaData == null)
            {
                throw new InvalidClassMetaDataException("044084", fullName, persistableSuperclass);
            }
            if (!pcSuperclassMetaData.isPopulated() && !pcSuperclassMetaData.isInitialised())
            {
                // Populate the superclass since we will be needing it
                pcSuperclassMetaData.populate(clr, cls.getClassLoader(), mmgr);
            }
        }

        if (persistableSuperclass != null && !isDetachable() && pcSuperclassMetaData.isDetachable())
        {
            // Inherit detachable flag from superclass
            detachable = true;
        }
    }

    /**
     * Check for conflicts on super class and this class MetaData identity
     * @throws InvalidMetaDataException if the user tries to overwrite a superclass identity / strategy
     */
    protected void validateUserInputForIdentity()
    {
        // Check that the user isn't trying to override the datastore-identity strategy!
        if (pcSuperclassMetaData != null)
        {
            AbstractClassMetaData baseCmd = getBaseAbstractClassMetaData();
            DatastoreIdentityMetaData baseImd = baseCmd.getDatastoreIdentityMetaData();
            if (baseCmd.identitySpecified && identitySpecified &&
                baseImd != null && baseImd.getValueStrategy() != null &&
                datastoreIdentityMetaData != null && datastoreIdentityMetaData.getValueStrategy() != null &&
                datastoreIdentityMetaData.getValueStrategy() != baseImd.getValueStrategy() &&
                datastoreIdentityMetaData.getValueStrategy() != null && 
                datastoreIdentityMetaData.getValueStrategy() != ValueGenerationStrategy.NATIVE)
            {
                // User made deliberate attempt to change strategy in this subclass
                throw new InvalidClassMetaDataException("044094", fullName, datastoreIdentityMetaData.getValueStrategy(), baseImd.getValueStrategy());
            }

            if (baseCmd.identitySpecified && datastoreIdentityMetaData != null && baseImd != null && baseImd.getValueStrategy() != datastoreIdentityMetaData.getValueStrategy())
            {
                // Make sure the strategy matches the parent (likely just took the default "native" schema)
                datastoreIdentityMetaData.setValueStrategy(baseImd.getValueStrategy());
            }
        }
    }

    /**
     * Convenience accessor for the AbstractClassMetaData of the base object in this hierarchy.
     * @return The AbstractClassMetaData for the base object.
     */
    public AbstractClassMetaData getBaseAbstractClassMetaData()
    {
        if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getBaseAbstractClassMetaData();
        }
        return this;
    }

    /**
     * Convenience method to return if this class is a descendant of the supplied class metadata.
     * @param cmd The class to check against
     * @return Whether the supplied metadata is an ancestor of this
     */
    public boolean isDescendantOf(AbstractClassMetaData cmd)
    {
        if (pcSuperclassMetaData == null)
        {
            return false;
        }
        if (pcSuperclassMetaData == cmd)
        {
            return true;
        }
        return pcSuperclassMetaData.isDescendantOf(cmd);
    }

    protected String getBaseInheritanceStrategy()
    {
        if (inheritanceMetaData != null && inheritanceMetaData.getStrategyForTree() != null)
        {
            return inheritanceMetaData.getStrategyForTree();
        }
        else if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getBaseInheritanceStrategy();
        }
        return null;
    }

    /**
     * Inherit the identity definition from super classes. 
     * @throws InvalidMetaDataException if the MetaData of this class conflicts with super classes definition
     */
    protected void inheritIdentity()
    {
        if (objectidClass != null)
        {
            // Make sure the objectid-class is fully-qualified (may have been specified in simple form)
            this.objectidClass = ClassUtils.createFullClassName(((PackageMetaData)parent).name, objectidClass);
        }

        // "persistence-capable-superclass"
        // Check that the class can be loaded, and is a true superclass 
        if (persistableSuperclass != null)
        {
            // Class has superclass, yet has objectid-class defined! this might result in user errors
            if (objectidClass != null)
            {
                String superObjectIdClass = pcSuperclassMetaData.getObjectidClass();
                if (superObjectIdClass == null || !objectidClass.equals(superObjectIdClass))
                {
                    throw new InvalidClassMetaDataException("044085", fullName, persistableSuperclass);
                }

                // by default users should only specify the object-id class in the root persistent class
                NucleusLogger.METADATA.info(Localiser.msg("044086", name, persistableSuperclass));
            }
            else
            {
                // get the objectid class from superclass
                this.objectidClass = pcSuperclassMetaData.getObjectidClass();
            }

            if (this.identityType == null)
            {
                this.identityType = pcSuperclassMetaData.getIdentityType();
            }
            if (this.identityType != null && !this.identityType.equals(pcSuperclassMetaData.getIdentityType())) 
            {
                // Identity of parent set, but we can't change the identity type from what was specified in the base class
                throw new InvalidClassMetaDataException("044093", fullName);
            }

            if (pcSuperclassMetaData.getIdentityType() == IdentityType.APPLICATION && pcSuperclassMetaData.getNoOfPopulatedPKMembers() > 0)
            {
                // TODO Allow for overriding superclass members
                // Check whether the superclass defines PK fields and this class defines some more
                int noOfPkKeys = 0;
                Iterator<AbstractMemberMetaData> memberIter = members.iterator();
                while (memberIter.hasNext())
                {
                    AbstractMemberMetaData mmd = memberIter.next();
                    if (mmd.isPrimaryKey())
                    {
                        if (mmd.fieldBelongsToClass())
                        {
                            noOfPkKeys++;
                        }
                        else
                        {
                            // TODO Check any overriding doesn't make something PK when it wasn't before
                        }
                    }
                }
                if (noOfPkKeys > 0)
                {
                    throw new InvalidClassMetaDataException("044034", getFullClassName(), noOfPkKeys, pcSuperclassMetaData.getNoOfPopulatedPKMembers());
                }
            }
        }
    }

    /**
     * Utility to add a defaulted PropertyMetaData to the class. 
     * Provided as a method since then any derived classes can override it.
     * @param name name of property
     * @return the new PropertyMetaData
     */
    protected AbstractMemberMetaData newDefaultedProperty(String name)
    {
        return new PropertyMetaData(this, name);
    }

    /**
     * Check if the inheritance MetaData is credible.
     * @param isAbstract Whether this class is abstract
     * @throws InvalidMetaDataException if the strategy is superclass-table, yet there are no super class
     * @throws InvalidMetaDataException if the strategy is superclass-table, yet the super class has not specified a discriminator
     * @throws InvalidMetaDataException if the strategy is superclass-table and discriminator is "value-map", yet no value for the discriminator has been specified
     */
    protected void validateUserInputForInheritanceMetaData(boolean isAbstract)
    {
        if (mappedSuperclass)
        {
            String baseInhStrategy = getBaseInheritanceStrategy();
            if (baseInhStrategy != null && baseInhStrategy.equalsIgnoreCase(InheritanceMetaData.INHERITANCE_TREE_STRATEGY_SINGLE_TABLE) && getSuperclassManagingTable() != null)
            {
                // We have a mapped-superclass part way down an inheritance tree but with a class with table above it
                // and the tree is defined to use single-table strategy, so change the inheritance strategy to persist
                // to the superclass (why anyone would define such a model is their problem and they get what they deserve)
                if (inheritanceMetaData != null)
                {
                    inheritanceMetaData.setStrategy(InheritanceStrategy.SUPERCLASS_TABLE);
                }
            }
        }

        // Check that the inheritance strategy is credible
        if (inheritanceMetaData != null)
        {
            // Check validity of inheritance strategy and discriminator
            if (inheritanceMetaData.getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE)
            {
                AbstractClassMetaData superCmd = getClassManagingTable();
                if (superCmd == null)
                {
                    // We need a superclass table yet there is no superclass with its own table!
                    throw new InvalidClassMetaDataException("044099", fullName);
                }

                DiscriminatorMetaData superDismd = superCmd.getInheritanceMetaData().getDiscriminatorMetaData();
                if (superDismd == null)
                {
                    // If we are using "superclass-table" then the superclass should have specified the discriminator.
                    throw new InvalidClassMetaDataException("044100",  fullName, superCmd.fullName);
                }

                DiscriminatorMetaData dismd = inheritanceMetaData.getDiscriminatorMetaData();
                if (superDismd.getStrategy() == DiscriminatorStrategy.VALUE_MAP &&
                    (dismd == null || dismd.getValue() == null) && !mappedSuperclass && !isAbstract)
                {
                    // If we are using "superclass-table" and the discriminator uses "value-map" then we must specify a value
                    throw new InvalidClassMetaDataException("044102", fullName, superCmd.fullName, superDismd.getColumnName());
                }
            }

            if (isAbstract)
            {
                // Class is abstract but user has defined a discriminator value (that will NEVER be used)
                DiscriminatorMetaData dismd = inheritanceMetaData.getDiscriminatorMetaData();
                if (dismd != null && !StringUtils.isWhitespace(dismd.getValue()))
                {
                    NucleusLogger.METADATA.info(Localiser.msg("044105", fullName));
                }
            }
            else
            {
                DiscriminatorMetaData dismd = inheritanceMetaData.getDiscriminatorMetaData();
                if (dismd != null && dismd.getColumnMetaData() != null)
                {
                    if (pcSuperclassMetaData != null)
                    {
                        // Check whether the user has tried to redefine the discriminator column down the inheritance tree
                        ColumnMetaData superDiscrimColmd = pcSuperclassMetaData.getDiscriminatorColumnMetaData();
                        if (superDiscrimColmd != null)
                        {
                            NucleusLogger.METADATA.debug(Localiser.msg("044126", fullName));
                        }
                    }
                }
            }
        }
    }

    /**
     * Impose a default inheritance strategy when one is not already specified.
     * Uses the persistence property for defaultInheritanceStrategy and works to the JDO2 spec etc.
     */
    protected void determineInheritanceMetaData()
    {
        if (inheritanceMetaData == null)
        {
            // User hasn't specified the <inheritance> for the class
            if (pcSuperclassMetaData != null)
            {
                AbstractClassMetaData baseCmd = getBaseAbstractClassMetaData();
                if (getBaseInheritanceStrategy() != null)
                {
                    // A strategy for the full inheritance tree is defined (like in JPA/Jakarta) so use that
                    String treeStrategy = getBaseInheritanceStrategy();
                    if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_JOINED.equals(treeStrategy))
                    {
                        inheritanceMetaData = new InheritanceMetaData();
                        inheritanceMetaData.setStrategy(InheritanceStrategy.NEW_TABLE);
                        return;
                    }
                    else if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_SINGLE_TABLE.equals(treeStrategy))
                    {
                        inheritanceMetaData = new InheritanceMetaData();
                        inheritanceMetaData.setStrategy(InheritanceStrategy.SUPERCLASS_TABLE);
                        return;
                    }
                    else if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_TABLE_PER_CLASS.equals(treeStrategy))
                    {
                        inheritanceMetaData = new InheritanceMetaData();
                        inheritanceMetaData.setStrategy(InheritanceStrategy.COMPLETE_TABLE);
                        return;
                    }
                }

                if (baseCmd.getInheritanceMetaData() != null && baseCmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.COMPLETE_TABLE)
                {
                    // Root class in tree is set to use COMPLETE_TABLE so all subclasses have own table
                    inheritanceMetaData = new InheritanceMetaData();
                    inheritanceMetaData.setStrategy(InheritanceStrategy.COMPLETE_TABLE);
                }
                else if (pcSuperclassMetaData.getInheritanceMetaData() != null &&
                    pcSuperclassMetaData.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                {
                    // Superclass exists but needs our table so have own table
                    inheritanceMetaData = new InheritanceMetaData();
                    inheritanceMetaData.setStrategy(InheritanceStrategy.NEW_TABLE);
                }
                else
                {
                    // Superclass exists and has a table or uses its superclass, so default based on that
                    if (mmgr.getNucleusContext().getConfiguration().getStringProperty(PropertyNames.PROPERTY_METADATA_DEFAULT_INHERITANCE_STRATEGY).equalsIgnoreCase("TABLE_PER_CLASS"))
                    {
                        // Each class has its own table
                        inheritanceMetaData = new InheritanceMetaData();
                        inheritanceMetaData.setStrategy(InheritanceStrategy.NEW_TABLE);
                    }
                    else
                    {
                        // JDO2 behaviour (root class has table, and others share it)
                        inheritanceMetaData = new InheritanceMetaData();
                        inheritanceMetaData.setStrategy(InheritanceStrategy.SUPERCLASS_TABLE);
                        if ((baseCmd.getInheritanceMetaData() == null || baseCmd.getInheritanceMetaData().getDiscriminatorMetaData() == null) && !mmgr.isEnhancing())
                        {
                            NucleusLogger.METADATA.warn("Class " + getFullClassName() + " is defined to use the same table as " + baseCmd.getFullClassName() +
                                " yet this root class has not been defined with a discriminator!!!");
                        }
                    }
                }
            }
            else
            {
                inheritanceMetaData = new InheritanceMetaData();
                inheritanceMetaData.setStrategy(InheritanceStrategy.NEW_TABLE);
            }
            return;
        }

        if (inheritanceMetaData.getStrategy() == null)
        {
            // User has included <inheritance> but not set the strategy, so populate it for them
            if (getBaseInheritanceStrategy() != null)
            {
                // They set a tree strategy for this level (applying to this and all levels below)
                String treeStrategy = getBaseInheritanceStrategy();
                if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_SINGLE_TABLE.equalsIgnoreCase(treeStrategy))
                {
                    if (pcSuperclassMetaData != null)
                    {
                        if (pcSuperclassMetaData.getInheritanceMetaData() != null && 
                            pcSuperclassMetaData.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                        {
                            // Check if superclass is wanting to persist into table of this class
                            // Can happen if superclass is mapped-superclass, and we want this to have the table
                            inheritanceMetaData.strategy = InheritanceStrategy.NEW_TABLE;
                        }
                        else
                        {
                            inheritanceMetaData.strategy = InheritanceStrategy.SUPERCLASS_TABLE;
                        }
                    }
                    else
                    {
                        if (inheritanceMetaData.getDiscriminatorMetaData() == null &&
                            mmgr.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_USE_DISCRIMINATOR_FOR_SINGLE_TABLE))
                        {
                            // JPA/Jakarta : When using SINGLE_TABLE at the root, then we must have a Discriminator (JPA/Jakarta spec says default length 31)
                            if (NucleusLogger.METADATA.isDebugEnabled())
                            {
                                NucleusLogger.METADATA.debug("Class " + getFullClassName() + " defined to use SINGLE_TABLE for the inheritance tree but no discriminator defined, so adding one");
                            }
                            if (mmgr.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_USE_DISCRIMINATOR_DEFAULT_CLASS_NAME))
                            {
                                inheritanceMetaData.newDiscriminatorMetadata().setStrategy(DiscriminatorStrategy.CLASS_NAME).setIndexed("true").newColumnMetaData().setLength(31);
                            }
                            else
                            {
                                inheritanceMetaData.newDiscriminatorMetadata().setStrategy(DiscriminatorStrategy.VALUE_MAP_ENTITY_NAME).setIndexed("true").newColumnMetaData().setLength(31);
                            }
                        }
                        inheritanceMetaData.strategy = InheritanceStrategy.NEW_TABLE;
                    }
                }
                else if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_TABLE_PER_CLASS.equalsIgnoreCase(treeStrategy))
                {
                    inheritanceMetaData.strategy = InheritanceStrategy.COMPLETE_TABLE;
                }
                else if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_JOINED.equalsIgnoreCase(treeStrategy))
                {
                    inheritanceMetaData.strategy = InheritanceStrategy.NEW_TABLE;
                }
                return;
            }

            if (pcSuperclassMetaData != null)
            {
                String treeStrategy = getBaseInheritanceStrategy();
                InheritanceStrategy baseStrategy = null;
                if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_SINGLE_TABLE.equalsIgnoreCase(treeStrategy))
                {
                    baseStrategy = InheritanceStrategy.SUPERCLASS_TABLE;
                }
                else if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_TABLE_PER_CLASS.equalsIgnoreCase(treeStrategy))
                {
                    baseStrategy = InheritanceStrategy.COMPLETE_TABLE;
                }
                else if (InheritanceMetaData.INHERITANCE_TREE_STRATEGY_JOINED.equalsIgnoreCase(treeStrategy))
                {
                    baseStrategy = InheritanceStrategy.NEW_TABLE;
                }
                else
                {
                    AbstractClassMetaData baseCmd = getBaseAbstractClassMetaData();
                    if (baseCmd.getInheritanceMetaData() != null)
                    {
                        baseStrategy = baseCmd.getInheritanceMetaData().getStrategy();
                    }
                }

                if (baseStrategy == InheritanceStrategy.COMPLETE_TABLE)
                {
                    // Base class in tree is set to use COMPLETE_TABLE so all subclasses have own table
                    inheritanceMetaData.strategy = InheritanceStrategy.COMPLETE_TABLE;
                }
                else if (pcSuperclassMetaData.getInheritanceMetaData() != null && 
                    pcSuperclassMetaData.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE)
                {
                    // Superclass exists but needs our table so have own table
                    inheritanceMetaData.strategy = InheritanceStrategy.NEW_TABLE;
                }
                else
                {
                    // Superclass exists and has a table or uses its superclass, so default based on that
                    if (mmgr.getNucleusContext().getConfiguration().getStringProperty(PropertyNames.PROPERTY_METADATA_DEFAULT_INHERITANCE_STRATEGY).equalsIgnoreCase("TABLE_PER_CLASS"))
                    {
                        // Each class has its own table
                        inheritanceMetaData.strategy = InheritanceStrategy.NEW_TABLE;
                    }
                    else
                    {
                        // JDO2 behaviour (root class has table, and others share it)
                        inheritanceMetaData.strategy = InheritanceStrategy.SUPERCLASS_TABLE;
                    }
                }
            }
            else
            {
                inheritanceMetaData.strategy = InheritanceStrategy.NEW_TABLE;
            }
        }
    }

    protected void applyDefaultDiscriminatorValueWhenNotSpecified()
    {
        if (inheritanceMetaData != null && inheritanceMetaData.getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE)
        {
            AbstractClassMetaData superCmd = getClassManagingTable();
            if (superCmd == null)
            {
                throw new InvalidClassMetaDataException("044064", getFullClassName());
            }

            if (superCmd.getInheritanceMetaData() != null)
            {
                DiscriminatorMetaData superDismd = superCmd.getInheritanceMetaData().getDiscriminatorMetaData();
                DiscriminatorMetaData dismd = inheritanceMetaData.getDiscriminatorMetaData();
                if (superDismd != null && superDismd.getStrategy() == DiscriminatorStrategy.VALUE_MAP && (dismd == null || dismd.getValue() == null))
                {
                    // Impose the full class name as the discriminator value since not set
                    if (dismd == null)
                    {
                        dismd = inheritanceMetaData.newDiscriminatorMetadata();
                    }
                    if (NucleusLogger.METADATA.isDebugEnabled())
                    {
                        NucleusLogger.METADATA.debug("No discriminator value specified for " + getFullClassName() + " so using fully-qualified class name");
                    }
                    dismd.setValue(getFullClassName());
                }
            }
        }

        if (inheritanceMetaData != null)
        {
            // Register the discriminator value if using VALUE_MAP and a value is defined
            DiscriminatorMetaData dismd = inheritanceMetaData.getDiscriminatorMetaData();
            if (dismd != null && getDiscriminatorStrategy() == DiscriminatorStrategy.VALUE_MAP && dismd.getValue() != null)
            {
                mmgr.registerDiscriminatorValueForClass(this, dismd.getValue());
            }
        }
    }

    /**
     * Convenience method to validate the specified "unmapped" columns.
     * @throws InvalidMetaDataException if a column is specified without its name.
     */
    protected void validateUnmappedColumns()
    {
        // Validate any unmapped columns
        if (unmappedColumns != null && !unmappedColumns.isEmpty())
        {
            Iterator unmappedIter = unmappedColumns.iterator();
            while (unmappedIter.hasNext())
            {
                ColumnMetaData colmd = (ColumnMetaData)unmappedIter.next();
                if (colmd.getName() == null)
                {
                    throw new InvalidClassMetaDataException("044119", fullName);
                }
                if (colmd.getJdbcType() == null)
                {
                    throw new InvalidClassMetaDataException("044120", fullName, colmd.getName());
                }
            }
        }
    }

    /**
     * Utility to find the immediate superclass that manages its own table.
     * Checks up the inheritance tree for one that uses "new-table" inheritance strategy.
     * @return Metadata for the superclass that uses "NEW-TABLE" (or null, if none found).
     */
    private AbstractClassMetaData getSuperclassManagingTable()
    {
        if (pcSuperclassMetaData != null)
        {
            if (pcSuperclassMetaData.getInheritanceMetaData().getStrategy() == InheritanceStrategy.NEW_TABLE)
            {
                return pcSuperclassMetaData;
            }
            return pcSuperclassMetaData.getSuperclassManagingTable();
        }
        return null;
    }

    /**
     * Utility to navigate up to superclasses to find the next class with its own table.
     * @return The AbstractClassMetaData of the class managing its own table
     */
    private AbstractClassMetaData getClassManagingTable()
    {
        if (inheritanceMetaData == null)
        {
            return this;
        }
        else if (inheritanceMetaData.getStrategy() == InheritanceStrategy.NEW_TABLE)
        {
            return this;
        }
        else if (inheritanceMetaData.getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE)
        {
            if (pcSuperclassMetaData == null)
            {
                return null;
            }
            return pcSuperclassMetaData.getClassManagingTable();
        }
        return null;
    }

    /**
     * Accessor for the Meta-Data for the superclass of this class.
     * @return MetaData of the superclass
     */
    public final AbstractClassMetaData getSuperAbstractClassMetaData()
    {
        checkPopulated();
        return pcSuperclassMetaData;
    }

    /**
     * Convenience method to calculate and return if the pk has some component that is generated in the datastore.
     * @param storeMgr The storeManager
     * @return Whether the PK is datastore generated
     */
    public boolean pkIsDatastoreAttributed(StoreManager storeMgr)
    {
        if (pkIsDatastoreAttributed == null)
        {
            pkIsDatastoreAttributed = Boolean.FALSE;
            if (identityType == IdentityType.APPLICATION)
            {
                for (int i=0;i<pkMemberPositions.length;i++)
                {
                    if (storeMgr.isValueGenerationStrategyDatastoreAttributed(this, pkMemberPositions[i]))
                    {
                        pkIsDatastoreAttributed = true;
                    }
                }
            }
            else if (identityType == IdentityType.DATASTORE)
            {
                pkIsDatastoreAttributed = storeMgr.isValueGenerationStrategyDatastoreAttributed(this, -1);
            }
        }
        return pkIsDatastoreAttributed.booleanValue();
    }

    /**
     * Determine the object id class.
     * @throws InvalidMetaDataException if the class 0 or more that one primary key field and no <code>objectid-class</code> has been declared in the MetaData
     * @throws InvalidMetaDataException if the <code>objectid-class</code> has not been set and the primary key field does not match a supported SingleFieldIdentity
     * @throws InvalidMetaDataException if the identity type is APPLICATION but not primary key fields have been set
     * @throws InvalidMetaDataException if the <code>objectid-class</code> cannot be loaded by the <code>clr</code>                                                                     
     */
    protected void determineObjectIdClass()
    {
        if (identityType != IdentityType.APPLICATION || objectidClass != null)
        {
            return;
        }

        int no_of_pk_fields = 0;
        AbstractMemberMetaData mmd_pk = null;
        for (AbstractMemberMetaData mmd : members)
        {
            if (mmd.isPrimaryKey())
            {
                mmd_pk = mmd;
                no_of_pk_fields++;
            }
        }

        if (no_of_pk_fields == 0 && inheritanceMetaData.getStrategy() == InheritanceStrategy.SUBCLASS_TABLE && getSuperclassManagingTable() == null)
        {
            // Case where we have no table for this class (abstract) and no superclass with a table, and no pk fields
            NucleusLogger.METADATA.debug(Localiser.msg("044163", getFullClassName()));
            instantiable = false;
            return;
        }

        boolean needsObjectidClass = false;
        if (persistableSuperclass == null)
        {
            needsObjectidClass = true;
        }
        else if (getSuperclassManagingTable() == null)
        {
            needsObjectidClass = true;
        }

        if (needsObjectidClass)
        {
            // Update "objectid-class" if required yet not specified
            if (no_of_pk_fields == 0)
            {
                NucleusLogger.METADATA.error(Localiser.msg("044065", fullName, "" + no_of_pk_fields));
                throw new InvalidClassMetaDataException("044065", fullName, "" + no_of_pk_fields);
            }
            else if (no_of_pk_fields > 1)
            {
                // More than 1 PK yet no objectidClass - maybe added by enhancer later, so log warning
                NucleusLogger.METADATA.warn(Localiser.msg("044065", fullName, "" + no_of_pk_fields));
                if (!mmgr.isEnhancing())
                {
                    objectidClass = fullName + GENERATED_PK_SUFFIX;
                    NucleusLogger.METADATA.debug(Localiser.msg("044164", fullName, "" + getNoOfPrimaryKeyMembers(), objectidClass));
                }
            }
            else
            {
                // Assign associated SingleField identity class
                Class pk_type = mmd_pk.getType();
                if (Byte.class.isAssignableFrom(pk_type) || byte.class.isAssignableFrom(pk_type))
                {
                    objectidClass = ClassNameConstants.IDENTITY_SINGLEFIELD_BYTE;
                }
                else if (Character.class.isAssignableFrom(pk_type) || char.class.isAssignableFrom(pk_type))
                {
                    objectidClass = ClassNameConstants.IDENTITY_SINGLEFIELD_CHAR;
                }
                else if (Integer.class.isAssignableFrom(pk_type) || int.class.isAssignableFrom(pk_type))
                {
                    objectidClass = ClassNameConstants.IDENTITY_SINGLEFIELD_INT;
                }
                else if (Long.class.isAssignableFrom(pk_type) || long.class.isAssignableFrom(pk_type))
                {
                    objectidClass = ClassNameConstants.IDENTITY_SINGLEFIELD_LONG;
                }
                else if (Short.class.isAssignableFrom(pk_type) || short.class.isAssignableFrom(pk_type))
                {
                    objectidClass = ClassNameConstants.IDENTITY_SINGLEFIELD_SHORT;
                }
                else if (String.class.isAssignableFrom(pk_type))
                {
                    objectidClass = ClassNameConstants.IDENTITY_SINGLEFIELD_STRING;
                }
                else if (Object.class.isAssignableFrom(pk_type))
                {
                    objectidClass = ClassNameConstants.IDENTITY_SINGLEFIELD_OBJECT;
                }
                else
                {
                    NucleusLogger.METADATA.error(Localiser.msg("044066", fullName, pk_type.getName()));
                    throw new InvalidClassMetaDataException("044066", fullName, pk_type.getName());
                }
            }
        }
        /*else
        {
            // Check no of primary key fields (inc superclass)
            int no_of_pk_fields = getNoOfPopulatedPKMembers();
            if (no_of_pk_fields == 0 && identityType == IdentityType.APPLICATION)
            {
                // No primary key fields found (even in superclasses)
                throw new InvalidMetaDataException(Localiser, "044077", fullName, objectidClass);
            }
        }*/
    }

    /**
     * Validate the objectid-class of this class.
     * @param clr ClassLoader resolver
     */
    protected void validateObjectIdClass(ClassLoaderResolver clr)
    {
        if (getPersistableSuperclass() == null)
        {
            // Only check root persistable class PK
            if (objectidClass != null)
            {
                ApiAdapter api = mmgr.getApiAdapter();
                Class obj_cls = null;
                try
                {
                    // Load the class, using the same class loader resolver as this class
                    obj_cls = clr.classForName(objectidClass);
                }
                catch (ClassNotResolvedException cnre)
                {
                    // ObjectIdClass not found
                    throw new InvalidClassMetaDataException("044079", fullName, objectidClass);
                }

                boolean validated = false;
                Set<Throwable> errors = new HashSet<>();
                try
                {
                    // Check against the API Adapter in use for this MetaData
                    if (api.isValidPrimaryKeyClass(obj_cls, this, clr, getNoOfPopulatedPKMembers(), mmgr))
                    {
                        validated = true;
                    }
                }
                catch (NucleusException ex)
                {
                    errors.add(ex);
                }
                if (!validated)
                {
                    // Why is this wrapping all exceptions into 1 single exception? 
                    // This needs coordinating with the test expectations in the enhancer unit tests.
                    throw new NucleusUserException(Localiser.msg("019016", getFullClassName(), obj_cls.getName()), errors.toArray(new Throwable[errors.size()]));
                }
            }
        }
    }

    /**
     * Method to provide the details of the class being represented by this MetaData. 
     * This can be used to firstly provide defaults for attributes that aren't specified in the MetaData, and secondly to report any errors
     * with attributes that have been specified that are inconsistent with the class being represented.
     * This method must be invoked by subclasses during populate operations.
     * @param clr ClassLoaderResolver to use in loading any classes
     * @param primary the primary ClassLoader to use (or null)
     * @param mmgr MetaData manager
     */
    abstract public void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr);

    /**
     * Method to initialise all convenience information about member positions and what role each position performs.
     */
    protected void initialiseMemberPositionInformation()
    {
        memberCount = noOfInheritedManagedMembers + managedMembers.length;
        dfgMemberFlags = new boolean[memberCount];
        scoMutableMemberFlags = new boolean[memberCount];
        nonPkMemberFlags = new boolean[memberCount];

        int pkFieldCount=0;
        int dfgFieldCount=0;
        int scmFieldCount=0;
        for (int i=0;i<memberCount;i++)
        {
            AbstractMemberMetaData mmd = getMetaDataForManagedMemberAtAbsolutePositionInternal(i);
            if (mmd.isPrimaryKey())
            {
                pkFieldCount++;
            }
            else
            {
                nonPkMemberFlags[i] = true;
            }
            if (mmd.isDefaultFetchGroup())
            {
                dfgMemberFlags[i] = true;
                dfgFieldCount++;
            }
            if (mmd.calcIsSecondClassMutable(mmgr))
            {
                scoMutableMemberFlags[i] = true;
                scmFieldCount++;
            }
        }
 
        if (pkFieldCount > 0)
        {
            // Primary key fields found
            if (identityType != IdentityType.APPLICATION)
            {
                // Not using application identity!
                throw new InvalidClassMetaDataException("044078", fullName, Integer.valueOf(pkFieldCount), identityType);
            }

            pkMemberPositions = new int[pkFieldCount];
            for (int i=0,pk_num=0;i<memberCount;i++)
            {
                AbstractMemberMetaData mmd = getMetaDataForManagedMemberAtAbsolutePositionInternal(i);
                if (mmd.isPrimaryKey())
                {
                    pkMemberPositions[pk_num++] = i;
                }
            }
        }
        else
        {
            if (instantiable && identityType == IdentityType.APPLICATION)
            {
                // No primary key fields found even though it does permit instances
                throw new InvalidClassMetaDataException("044077", fullName, objectidClass);
            }
        }

        nonPkMemberPositions = new int[memberCount-pkFieldCount];
        for (int i=0,npkf=0;i<memberCount;i++)
        {
            AbstractMemberMetaData mmd = getMetaDataForManagedMemberAtAbsolutePositionInternal(i);
            if (!mmd.isPrimaryKey())
            {
                nonPkMemberPositions[npkf++] = i;
            }
        }

        dfgMemberPositions = new int[dfgFieldCount];
        scoMutableMemberPositions = new int[scmFieldCount];
        for (int i=0,dfgNum=0,scmNum=0;i<memberCount;i++)
        {
            if (dfgMemberFlags[i])
            {
                dfgMemberPositions[dfgNum++] = i;
            }
            if (scoMutableMemberFlags[i])
            {
                scoMutableMemberPositions[scmNum++] = i;
            }
        }
    }

    /**
     * Convenience method to find the discriminator MetaData defining the discrim for the same table
     * as this class is using. Traverses up the inheritance tree to find the highest class that uses
     * "subclass-table" that has discriminator metadata defined, and returns the MetaData.
     * @return DiscriminatorMetaData for the highest class in this tree using subclass-table
     */
    public final DiscriminatorMetaData getDiscriminatorMetaDataForTable()
    {
        if (pcSuperclassMetaData != null)
        {
            if (pcSuperclassMetaData.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUBCLASS_TABLE ||
                pcSuperclassMetaData.getInheritanceMetaData().getStrategy() == InheritanceStrategy.COMPLETE_TABLE)
            {
                DiscriminatorMetaData superDismd = pcSuperclassMetaData.getDiscriminatorMetaDataForTable();
                if (superDismd != null)
                {
                    // Superclass persisted into this table, so defines the discriminator, so use that definition
                    return superDismd;
                }
            }
        }

        // Nothing in superclasses sharing our table so return ours
        return inheritanceMetaData != null ? inheritanceMetaData.getDiscriminatorMetaData() : null;
    }

    /**
     * Convenience accessor for the discriminator strategy applying to this class.
     * This is specified on the class managing the table if at all.
     * @return The discriminator strategy
     */
    public final DiscriminatorStrategy getDiscriminatorStrategyForTable()
    {
        if (inheritanceMetaData == null)
        {
            return null;
        }
        else if (inheritanceMetaData.getStrategy() == InheritanceStrategy.NEW_TABLE && inheritanceMetaData.getDiscriminatorMetaData() != null)
        {
            // Discriminator information specified at this level
            return inheritanceMetaData.getDiscriminatorMetaData().getStrategy();
        }
        else if (getSuperAbstractClassMetaData() != null)
        {
            // Use superclass discriminator information
            return getSuperAbstractClassMetaData().getDiscriminatorStrategy();
        }
        else
        {
            // Undefined
            return null;
        }
    }

    /**
     * Convenience accessor for the discriminator metadata applying to this class.
     * If specified on this class then returns that, otherwise goes up to the superclass (if present)
     * until it finds a discriminator metadata specification.
     * @return The discriminator metadata
     */
    public final DiscriminatorMetaData getDiscriminatorMetaData()
    {
        if (inheritanceMetaData != null && inheritanceMetaData.getDiscriminatorMetaData() != null)
        {
            // Discriminator information specified at this level
            return inheritanceMetaData.getDiscriminatorMetaData();
        }
        else if (getSuperAbstractClassMetaData() != null)
        {
            // Use superclass discriminator information
            return getSuperAbstractClassMetaData().getDiscriminatorMetaData();
        }
        else
        {
            // Undefined
            return null;
        }
    }

    /**
     * Convenience method to return the "root" discriminator metadata definition (that defines the strategy, column etc).
     * Useful when using "complete-table" inheritance, so we apply the root definition to the table for this class.
     * @return Discriminator metadata for the root
     */
    public final DiscriminatorMetaData getDiscriminatorMetaDataRoot()
    {
        DiscriminatorMetaData dismd = null;
        if (pcSuperclassMetaData != null)
        {
            dismd = pcSuperclassMetaData.getDiscriminatorMetaDataRoot();
        }
        if (dismd == null)
        {
            dismd = inheritanceMetaData != null ? inheritanceMetaData.getDiscriminatorMetaData() : null;
        }
        return dismd;
    }

    /**
     * Accessor for whether we have a discriminator defined for this class (may be in superclasses).
     * @return true if discriminatorStrategy is not null and not NONE
     */
    public final boolean hasDiscriminatorStrategy() 
    {
        DiscriminatorStrategy strategy = getDiscriminatorStrategy();
        return strategy != null && strategy != DiscriminatorStrategy.NONE;
    }

    /**
     * Method to return the discriminator strategy being used by this class.
     * Returns the strategy defined on this class (if any), otherwise goes up to the superclass etc until it finds a defined strategy.
     * @return The discriminator strategy
     */
    public final DiscriminatorStrategy getDiscriminatorStrategy()
    {
        if (inheritanceMetaData != null && inheritanceMetaData.getDiscriminatorMetaData() != null && inheritanceMetaData.getDiscriminatorMetaData().getStrategy() != null)
        {
            return inheritanceMetaData.getDiscriminatorMetaData().getStrategy();
        }
        else if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getDiscriminatorStrategy();
        }
        return null;
    }

    /**
     * Return the name of the discriminator column if defined in metadata.
     * If not provided directly for this class, goes up to the superclass to find it.
     * @return The column name for the discriminator
     */
    public String getDiscriminatorColumnName()
    {
        if (inheritanceMetaData != null && inheritanceMetaData.getDiscriminatorMetaData() != null && 
            inheritanceMetaData.getDiscriminatorMetaData().getColumnMetaData() != null && 
            inheritanceMetaData.getDiscriminatorMetaData().getColumnMetaData().getName() != null)
        {
            return inheritanceMetaData.getDiscriminatorMetaData().getColumnMetaData().getName();
        }
        else if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getDiscriminatorColumnName();
        }
        return null;
    }

    /**
     * Return the metadata for the discriminator column if defined in metadata.
     * If not provided directly for this class, goes up to the superclass to find it.
     * @return The column metadata for the discriminator
     */
    public ColumnMetaData getDiscriminatorColumnMetaData()
    {
        if (inheritanceMetaData != null && inheritanceMetaData.getDiscriminatorMetaData() != null && inheritanceMetaData.getDiscriminatorMetaData().getColumnMetaData() != null)
        {
            return inheritanceMetaData.getDiscriminatorMetaData().getColumnMetaData();
        }
        else if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getDiscriminatorColumnMetaData();
        }
        return null;
    }

    /**
     * Method returning the discriminator value to apply to an instance of this class.
     * If using "class-name" then returns the class name, and if using the "entity-name" returns the entity name, otherwise if using "value-map" returns the value
     * specified directly against this class metadata. The returned value is a String unless the user defined
     * the column as holding integer-based values, in which case a Long is returned
     * @return The discriminator value
     */
    public Object getDiscriminatorValue()
    {
        if (hasDiscriminatorStrategy())
        {
            DiscriminatorStrategy str = getDiscriminatorStrategy();
            if (str == DiscriminatorStrategy.CLASS_NAME)
            {
                return getFullClassName();
            }
            else if (str == DiscriminatorStrategy.VALUE_MAP)
            {
                DiscriminatorMetaData dismd = getDiscriminatorMetaDataRoot();
                Object value = getInheritanceMetaData().getDiscriminatorMetaData().getValue();
                if (dismd.getColumnMetaData() != null)
                {
                    ColumnMetaData colmd = dismd.getColumnMetaData();
                    if (MetaDataUtils.isJdbcTypeNumeric(colmd.getJdbcType()))
                    {
                        // Split out integer-based types. Probably not worth splitting out any other types (floating point?)
                        value = Long.parseLong((String)value);
                    }
                }
                return value;
            }
            else if (str == DiscriminatorStrategy.VALUE_MAP_ENTITY_NAME)
            {
                DiscriminatorMetaData dismd = getDiscriminatorMetaDataRoot();
                Object value = getInheritanceMetaData().getDiscriminatorMetaData() != null ? getInheritanceMetaData().getDiscriminatorMetaData().getValue() : null;
                if (value != null)
                {
                    if (dismd.getColumnMetaData() != null)
                    {
                        ColumnMetaData colmd = dismd.getColumnMetaData();
                        if (MetaDataUtils.isJdbcTypeNumeric(colmd.getJdbcType()))
                        {
                            // Split out integer-based types. Probably not worth splitting out any other types (floating point?)
                            value = Long.parseLong((String)value);
                        }
                    }
                    return value;
                }
                return getEntityName();
            }
        }
        return null;
    }

    public IdentityType getIdentityType()
    {
        return identityType;
    }

    public void setIdentityType(IdentityType type)
    {
        checkNotYetPopulated();

        this.identityType = type;
    }

    /**
     * Accessor for the simple class name (without package name).
     * @return class name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Accessor for the full class name including any package name.
     * @return full class name.
     */
    public String getFullClassName()
    {
        return fullName;
    }

    public String getObjectidClass()
    {
        return objectidClass;
    }

    public AbstractClassMetaData setObjectIdClass(String objectidClass)
    {
        this.objectidClass = StringUtils.isWhitespace(objectidClass) ? this.objectidClass : objectidClass;
        return this;
    }

    public String getEntityName()
    {
        return entityName;
    }

    public AbstractClassMetaData setEntityName(String name)
    {
        this.entityName = StringUtils.isWhitespace(name) ? this.entityName : name;
        return this;
    }

    public String getCatalog()
    {
        return this.catalog != null ? this.catalog : ((PackageMetaData)parent).getCatalog();
    }

    public AbstractClassMetaData setCatalog(String catalog)
    {
        this.catalog = StringUtils.isWhitespace(catalog) ? this.catalog : catalog;
        return this;
    }

    public String getSchema()
    {
        return this.schema != null ? this.schema : ((PackageMetaData)parent).getSchema();
    }

    public AbstractClassMetaData setSchema(String schema)
    {
        this.schema = StringUtils.isWhitespace(schema) ? this.schema : schema;
        return this;
    }

    public String getTable()
    {
        return table;
    }

    public AbstractClassMetaData setTable(String table)
    {
        this.table = StringUtils.isWhitespace(table) ? this.table : table;
        return this;
    }

    public boolean isRequiresExtent()
    {
        return requiresExtent;
    }

    public AbstractClassMetaData setRequiresExtent(boolean flag)
    {
        this.requiresExtent = flag;
        return this;
    }

    public AbstractClassMetaData setRequiresExtent(String flag)
    {
        if (!StringUtils.isWhitespace(flag))
        {
            this.requiresExtent = Boolean.parseBoolean(flag);
        }
        return this;
    }

    public boolean isDetachable()
    {
        return detachable;
    }

    public AbstractClassMetaData setDetachable(boolean flag)
    {
        this.detachable = flag;
        return this;
    }

    public AbstractClassMetaData setDetachable(String flag)
    {
        if (!StringUtils.isWhitespace(flag))
        {
            this.detachable = Boolean.parseBoolean(flag);
        }
        return this;
    }

    public Boolean isCacheable()
    {
        return cacheable;
    }

    public AbstractClassMetaData setCacheable(boolean cache)
    {
        cacheable = cache;
        return this;
    }

    public AbstractClassMetaData setCacheable(String cache)
    {
        if (!StringUtils.isWhitespace(cache))
        {
            this.cacheable = Boolean.parseBoolean(cache);
        }
        return this;
    }

    public boolean isEmbeddedOnly()
    {
        return embeddedOnly;
    }

    public AbstractClassMetaData setEmbeddedOnly(boolean flag)
    {
        embeddedOnly = flag;
        return this;
    }

    public AbstractClassMetaData setEmbeddedOnly(String flag)
    {
        if (!StringUtils.isWhitespace(flag))
        {
            this.embeddedOnly = Boolean.parseBoolean(flag);
        }
        return this;
    }

    public PackageMetaData getPackageMetaData()
    {
        if (parent != null)
        {
            return (PackageMetaData)parent;
        }
        return null;
    }

    public String getPackageName()
    {
        return getPackageMetaData().getName();
    }

    public ClassPersistenceModifier getPersistenceModifier()
    {
        return persistenceModifier;
    }

    public AbstractClassMetaData setPersistenceModifier(ClassPersistenceModifier modifier)
    {
        this.persistenceModifier = modifier;
        return this;
    }

    public String getPersistableSuperclass()
    {
        return persistableSuperclass;
    }

    /**
     * Whether this persistable class uses SingleFieldIdentity
     * @return true if using SingleFieldIdentity as objectid class
     */
    public boolean usesSingleFieldIdentityClass()
    {
        return usesSingleFieldIdentityClass;
    }

    /**
     * Check if the argument cmd is the same as this or a descedent.
     * @param cmd the AbstractClassMetaData to be verify if this is an ancestor
     * @return true if the argument is a child or same as this
     */
    public boolean isSameOrAncestorOf(AbstractClassMetaData cmd)
    {
        checkInitialised();
        
        if (cmd == null)
        {
            return false;
        }
        
        if (fullName.equals(cmd.fullName))
        {
            return true;
        }
        
        AbstractClassMetaData parent = cmd.getSuperAbstractClassMetaData();
        while( parent != null )
        {
            if (fullName.equals(parent.fullName))
            {
                return true;
            }
            parent = parent.getSuperAbstractClassMetaData();
        }
        return false;
    }

    /**
     * Accessor for the names of the primary key fields/properties. Only valid after being populated. 
     * Provided as a convenience where we need to get the names of the PK members but cant wait til initialisation.
     * @return names of the PK fields/properties
     */
    public String[] getPrimaryKeyMemberNames()
    {
        if (identityType != IdentityType.APPLICATION)
        {
            return null;
        }

        List<String> memberNames = new ArrayList<>(); // Use list to preserve ordering
        Iterator<AbstractMemberMetaData> iter = members.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData mmd = iter.next();
            if (Boolean.TRUE.equals(mmd.primaryKey))
            {
                memberNames.add(mmd.name);
            }
        }

        if (!memberNames.isEmpty())
        {
            return memberNames.toArray(new String[memberNames.size()]);
        }
        memberNames = null;
        return pcSuperclassMetaData.getPrimaryKeyMemberNames();
    }

    /**
     * Method to check if a member exists in this classes definition.
     * Will include any superclasses in the check.
     * @param memberName Name of member
     * @return return true if exists.
     */
    public boolean hasMember(String memberName)
    {
        Iterator<AbstractMemberMetaData> iter = members.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData mmd = iter.next();
            if (mmd.getName().equals(memberName))
            {
                return true;
            }
        }

        if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.hasMember(memberName);
        }
        return false;
    }

    /**
     * Accessor for the number of members defined for this class (including overrides).
     * This is the total number of members (inc static, final etc) in this class 
     * @return no of fields/properties.
     */
    public int getNoOfMembers()
    {
        return members.size();
    }

    /**
     * Accessor for the Meta-Data for a member. Include superclasses.
     * @param name the name of the member
     * @return Meta-Data for the member
     */
    public AbstractMemberMetaData getMetaDataForMember(String name)
    {
        if (name == null)
        {
            return null;
        }

        Iterator<AbstractMemberMetaData> iter = members.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData mmd = iter.next();
            if (mmd.getName().equals(name))
            {
                return mmd;
            }
        }

        // Check superclass for the field/property with this name
        if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getMetaDataForMember(name);
        }
        return null;
    }

    /**
     * Accessor for the number of managed members (this class only).
     * @return no of managed members in this class
     */
    public int getNoOfManagedMembers()
    {
        // checkInitialised();

        if (managedMembers == null)
        {
            return 0;
        }
        return managedMembers.length;
    }

    /**
     * Accessor for the managed fields/properties in this class (not including superclass, but including overridden).
     * @return MetaData for the managed fields/properties in this class
     */
    public AbstractMemberMetaData[] getManagedMembers()
    {
        checkInitialised();

        return managedMembers;
    }

    /**
     * Accessor for the overridden fields/properties in this class.
     * @return The overridden fields/properties in this class
     */
    public AbstractMemberMetaData[] getOverriddenMembers()
    {
        checkInitialised();

        return overriddenMembers;
    }

    /**
     * Accessor for an overridden field/property with the specified name.
     * @param name Name of the field/property
     * @return The MetaData for the field/property
     */
    public AbstractMemberMetaData getOverriddenMember(String name)
    {
        checkInitialised();

        if (overriddenMembers == null)
        {
            return null;
        }
        for (int i=0;i<overriddenMembers.length;i++)
        {
            if (overriddenMembers[i].getName().equals(name))
            {
                return overriddenMembers[i];
            }
        }
        return null;
    }

    /**
     * Convenience method that navigates up a MetaData inheritance tree until it finds the base member definition for the specified name.
     * @param name Name of the member we require
     * @return The metadata for the member
     */
    protected AbstractMemberMetaData getMemberBeingOverridden(String name)
    {
        Iterator<AbstractMemberMetaData> iter = members.iterator();
        while(iter.hasNext())
        {
            AbstractMemberMetaData mmd = iter.next();
            if (mmd.name.equals(name) && mmd.fieldBelongsToClass())
            {
                return mmd;
            }
        }

        if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getMemberBeingOverridden(name);
        }
        return null;
    }

    /**
     * Accessor for the number of inherited managed members in superclasses.
     * @return No of inherited managed members in superclasses.
     */
    public int getNoOfInheritedManagedMembers()
    {
        checkInitialised();

        return noOfInheritedManagedMembers;
    }
    
    /**
     * Accessor for the number of managed members from this class plus inherited classes.
     * @return The number of managed members from this class plus inherited classes.
     */
    public int getMemberCount()
    {
        return memberCount;
    }

    /**
     * Accessor for the metadata of a member. Does not include superclasses.
     * <B>In general this should never be used; always use "getMetaDataForManagedMemberAtAbsolutePosition".</B>
     * @param index field index relative to this class only starting from 0
     * @return Meta-Data for the member
     */
    public AbstractMemberMetaData getMetaDataForMemberAtRelativePosition(int index)
    {
        if (index < 0 || index >= members.size())
        {
            return null;
        }
        return members.get(index);
    }

    /**
     * Accessor for MetaData for a managed member in this class. 
     * The position is relative to the first member in this class (i.e ignores superclasses).
     * @param position The position of the managed field. 0 = first in the class
     * @return The managed member at that position
     */
    public AbstractMemberMetaData getMetaDataForManagedMemberAtRelativePosition(int position)
    {
        checkInitialised();

        if (managedMembers == null)
        {
            return null;
        }
        if (position < 0 || position >= managedMembers.length)
        {
            return null;
        }

        return managedMembers[position];
    }

    /**
     * Accessor for a managed member including superclass members.
     * Allows for overriding of superclass members in this class and superclasses.
     * @param position The position of the managed member including the superclass. Fields are numbered from 0 in the root persistable superclass.
     * @return The managed member at this "absolute" position.
     */
    public AbstractMemberMetaData getMetaDataForManagedMemberAtAbsolutePosition(int position)
    {
        checkInitialised();

        return getMetaDataForManagedMemberAtAbsolutePositionInternal(position);
    }

    /**
     * Internal method to get the field/property for an absolute field number.
     * If the field for that absolute field position is overridden by a field in this class then this field/property will be returned.
     * @param abs_position The position of the managed field including the superclass. Fields are numbered from 0 in the root superclass.
     * @return The managed field at this "absolute" position.
     */
    protected AbstractMemberMetaData getMetaDataForManagedMemberAtAbsolutePositionInternal(int abs_position)
    {
        // If the field is in a superclass, go there
        if (abs_position < noOfInheritedManagedMembers)
        {
            if (pcSuperclassMetaData == null)
            {
                return null;
            }

            AbstractMemberMetaData mmd = pcSuperclassMetaData.getMetaDataForManagedMemberAtAbsolutePositionInternal(abs_position);
            if (mmd != null)
            {
                // Check for override(s) in this class or superclasses
                for (int i=0;i<overriddenMembers.length;i++)
                {
                    if (overriddenMembers[i].getName().equals(mmd.getName()) && overriddenMembers[i].getClassName().equals(mmd.getClassName()))
                    {
                        // Return the overriding field if we have one (class and field name is the safest comparison)
                        // TODO This doesn't do a merge of the base metadata with the override info. It should!
                        return overriddenMembers[i];
                    }
                }

                return mmd;
            }
            return null;
        }

        // If the field is in this class, return it
        if (abs_position - noOfInheritedManagedMembers >= managedMembers.length)
        {
            return null;
        }
        return managedMembers[abs_position - noOfInheritedManagedMembers];
    }

    /**
     * Accessor for the (relative) position of the field/property with the specified name.
     * <b>The returned position is relative to this class only</b>
     * @param memberName Name of the member
     * @return Relative position of the member in this class
     */
    public int getRelativePositionOfMember(String memberName)
    {
        checkInitialised();

        if (memberName == null)
        {
            return -1;
        }

        Integer i = memberPositionsByName.get(memberName);
        return i == null ? -1 : i.intValue();
    }

    /**
     * Accessor for the absolute position of the field/property with the specified name.
     * The absolute position has origin in the root persistable superclass, starting at 0.
     * @param memberName Name of the member
     * @return Absolute position of the member
     */
    public int getAbsolutePositionOfMember(String memberName)
    {
        checkInitialised();

        if (memberName == null)
        {
            return -1;
        }

        int i = getRelativePositionOfMember(memberName);
        if (i < 0)
        {
            if (pcSuperclassMetaData != null)
            {
                i = pcSuperclassMetaData.getAbsolutePositionOfMember(memberName);
            }
        }
        else
        {
            i += noOfInheritedManagedMembers;
        }

        return i;
    }

    /**
     * Convenience method to check the number of fields/properties in this class that have been populated and
     * that are primary key members. This is only ever called during populate() since the accessor
     * for PK members cant be used yet due to lack of initialisation.
     * Recurses to its superclass if it has a superclass.
     * @return The number of PK members
     */
    private int getNoOfPopulatedPKMembers()
    {
        if (pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getNoOfPopulatedPKMembers();
        }

        Iterator<AbstractMemberMetaData> fields_iter = members.iterator();
        int noOfPks = 0;
        while (fields_iter.hasNext())
        {
            AbstractMemberMetaData mmd = fields_iter.next();
            if (mmd.isPrimaryKey())
            {
                noOfPks++;
            }
        }
        return noOfPks;
    }

    /**
     * Accessor for the number of primary key fields/properties.
     * @return no of primary key fields/properties
     */
    public int getNoOfPrimaryKeyMembers()
    {
        if (pkMemberPositions == null)
        {
            return 0;
        }
        return pkMemberPositions.length;
    }

    /**
     * Accessor for all field/property positions.
     * These are absolute numbers and include superclasses and are really just 0, 1, 2, ... n.
     * @return The positions of all (managed) fields/properties.
     */
    public int[] getAllMemberPositions()
    {
        checkInitialised();
        if (allMemberPositions == null)
        {
            allMemberPositions = new int[memberCount];
            for (int i=0;i<memberCount;i++)
            {
                allMemberPositions[i] = i;
            }
        }
        return allMemberPositions;
    }

    /**
     * Accessor for the field numbers of the primary key fields/properties. 
     * These are absolute numbers (including superclasses).
     * @return The positions of the primary key fields/properties.
     */
    public int[] getPKMemberPositions()
    {
        checkInitialised();
        return pkMemberPositions;
    }

    /**
     * Accessor for the positions of the non primary key fields/properties (inc superclass fields).
     * @return The member positions
     */
    public int[] getNonPKMemberPositions()
    {
        checkInitialised();
        return nonPkMemberPositions;
    }

    /**
     * Accessor for the flags of the non primary key fields/properties (inc superclass members).
     * @return The flags whether the field/property is non primary key
     */
    public boolean[] getNonPKMemberFlags()
    {
        checkInitialised();
        return nonPkMemberFlags;
    }

    /**
     * Accessor for the absolute positions of the default fetch group fields/properties (inc superclasses).
     * @return The positions of the DFG fields/properties (inc superclasses).
     */
    public int[] getDFGMemberPositions()
    {
        checkInitialised();
        return dfgMemberPositions;
    }

    /**
     * Accessor for the flags of the DFG fields/properties (inc superclass).
     * @return The flags whether the field/property is in the DFG
     **/
    public boolean[] getDFGMemberFlags()
    {
        checkInitialised();
        return dfgMemberFlags;
    }

    /**
     * Accessor for the absolute positions of fields/properties that are considered basic.
     * This category includes members of all basic (primitive and immutable
     * object class) types as defined in section 6.4 of the specification,
     * including String, Date and its jdbc subtypes, Locale, Currency, and Enum types.
     * Includes all inherited multivalued positions.
     * WARNING : this includes transient fields. DO NOT USE as a way of getting the persistent fields in the class.
     * @param clr ClassLoader resolver
     * @return The absolute positions
     */
    public int[] getBasicMemberPositions(ClassLoaderResolver clr)
    {
        // Do double pass on members - first pass to get number of members, and second to set up array
        // Could do single pass with ArrayList but want primitives and in JDK1.3/4 can't put direct in ArrayList
        Iterator<AbstractMemberMetaData> iter = members.iterator();
        int numBasics = 0;
        while (iter.hasNext())
        {
            AbstractMemberMetaData mmd = iter.next();
            if (mmd.getRelationType(clr) == RelationType.NONE && !mmd.isPersistentInterface(clr) &&
                !Collection.class.isAssignableFrom(mmd.getType()) &&
                !Map.class.isAssignableFrom(mmd.getType()) &&
                !mmd.getType().isArray())
            {
                numBasics++;
            }
        }
        int[] inheritedBasicPositions = null;
        if (pcSuperclassMetaData != null)
        {
            inheritedBasicPositions = pcSuperclassMetaData.getBasicMemberPositions(clr);
        }

        int[] basicPositions = new int[numBasics + (inheritedBasicPositions != null ? inheritedBasicPositions.length : 0)];
        int number = 0;
        if (inheritedBasicPositions != null)
        {
            for (int i=0;i<inheritedBasicPositions.length;i++)
            {
                basicPositions[number++] = inheritedBasicPositions[i];
            }
        }

        iter = members.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData mmd = iter.next();
            if (mmd.getRelationType(clr) == RelationType.NONE && !mmd.isPersistentInterface(clr) &&
                !Collection.class.isAssignableFrom(mmd.getType()) && !Map.class.isAssignableFrom(mmd.getType()) && !mmd.getType().isArray())
            {
                basicPositions[number++] = mmd.getAbsoluteFieldNumber();
            }
        }
        return basicPositions;
    }

    /**
     * Accessor for the absolute positions of fields/properties that are considered multi-valued
     * This category includes members of all multi-valued types, including
     * Collection, array, and Map types of basic and relationship types.
     * Includes all inherited multivalued positions.
     * @return The absolute positions
     */
    public int[] getMultivaluedMemberPositions()
    {
        // Do double pass on members - first pass to get number of members, and second to set up array
        // Could do single pass with ArrayList but want primitives and in JDK1.3/4 can't put direct in ArrayList
        int numMultivalues = 0;
        for (AbstractMemberMetaData mmd : members)
        {
            if (mmd.getType().isArray() || Collection.class.isAssignableFrom(mmd.getType()) || Map.class.isAssignableFrom(mmd.getType()))
            {
                numMultivalues++;
            }
        }
        int[] inheritedMultivaluePositions = null;
        if (pcSuperclassMetaData != null)
        {
            inheritedMultivaluePositions = pcSuperclassMetaData.getMultivaluedMemberPositions();
        }

        int[] multivaluePositions = new int[numMultivalues + (inheritedMultivaluePositions != null ? inheritedMultivaluePositions.length : 0)];
        int number = 0;
        if (inheritedMultivaluePositions != null)
        {
            for (int i=0;i<inheritedMultivaluePositions.length;i++)
            {
                multivaluePositions[number++] = inheritedMultivaluePositions[i];
            }
        }

        for (AbstractMemberMetaData mmd : members)
        {
            if (mmd.getType().isArray() || Collection.class.isAssignableFrom(mmd.getType()) || Map.class.isAssignableFrom(mmd.getType()))
            {
                multivaluePositions[number++] = mmd.getAbsoluteFieldNumber();
            }
        }
        return multivaluePositions;
    }

    /**
     * Accessor for the absolute positions of the second class mutable fields/properties.
     * @return The field numbers of the second class mutable fields (inc superclasses).
     */
    public int[] getSCOMutableMemberPositions()
    {
        checkInitialised();
        return scoMutableMemberPositions;
    }

    /**
     * Accessor for the absolute positions of all SCO fields/properties that are NOT containers (e.g Dates, Points, etc)
     * @return Field numbers of all SCO non-container fields/properties
     */
    public int[] getSCONonContainerMemberPositions()
    {
        checkInitialised();
        if (scoNonContainerMemberPositions == null)
        {
            // Find number of SCO mutable members that are not SCO containers
            int numberNonContainerSCOFields = 0;
            for (int scoMutableMemberPosition : scoMutableMemberPositions)
            {
                AbstractMemberMetaData mmd = getMetaDataForManagedMemberAtAbsolutePosition(scoMutableMemberPosition);
                if (!(java.util.Collection.class.isAssignableFrom(mmd.getType())) && !(java.util.Map.class.isAssignableFrom(mmd.getType())))
                {
                    numberNonContainerSCOFields++;
                }
            }

            scoNonContainerMemberPositions = new int[numberNonContainerSCOFields];
            int nonContNum = 0;
            for (int scoMutableMemberPosition : scoMutableMemberPositions)
            {
                AbstractMemberMetaData mmd = getMetaDataForManagedMemberAtAbsolutePosition(scoMutableMemberPosition);
                if (!(java.util.Collection.class.isAssignableFrom(mmd.getType())) && !(java.util.Map.class.isAssignableFrom(mmd.getType())))
                {
                    scoNonContainerMemberPositions[nonContNum++] = scoMutableMemberPosition;
                }
            }
        }
        return scoNonContainerMemberPositions;
    }

    /**
     * Accessor for the flags of the SCO mutable fields (inc superclass fields).
     * @return The flags whether the field is second class mutable
     */
    public boolean[] getSCOMutableMemberFlags()
    {
        checkInitialised();
        return scoMutableMemberFlags;
    }

    /**
     * Accessor for the flags of whether members are SCO "containers" (Collection/Map/array).
     * @return The array of SCO container flags
     */
    public boolean[] getSCOContainerMemberFlags()
    {
        if (scoContainerMemberFlags == null)
        {
            checkInitialised();
            scoContainerMemberFlags = new boolean[memberCount];
            for (int i=0;i<memberCount;i++)
            {
                AbstractMemberMetaData mmd = getMetaDataForManagedMemberAtAbsolutePositionInternal(i);
                if (java.util.Collection.class.isAssignableFrom(mmd.getType()) || java.util.Map.class.isAssignableFrom(mmd.getType()) || mmd.getType().isArray())
                {
                    scoContainerMemberFlags[i] = true;
                }
                else
                {
                    scoContainerMemberFlags[i] = false;
                }
            }
        }
        return scoContainerMemberFlags;
    }

    /**
     * Convenience method to return if the class has relations to other objects. Includes superclasses.
     * @param clr ClassLoader resolver
     * @return Whether the class has any relations (that it knows about)
     */
    public boolean hasRelations(ClassLoaderResolver clr)
    {
        if (hasRelations == null)
        {
            hasRelations = getRelationMemberPositions(clr).length > 0;
        }
        return hasRelations.booleanValue();
    }

    public int[] getNonRelationMemberPositions(ClassLoaderResolver clr)
    {
        int[] relPositions = getRelationMemberPositions(clr);
        if (relPositions == null || relPositions.length == 0)
        {
            return getAllMemberPositions();
        }

        int[] allPositions = getAllMemberPositions();
        int[] nonrelPositions = new int[allPositions.length - relPositions.length];
        int nonrelPos = 0;
        int nextRelPos = 0;
        for (int i=0;i<allPositions.length;i++)
        {
            if (nextRelPos == relPositions.length)
            {
                // Already processed last relation position, so add
                nonrelPositions[nonrelPos++] = i;
            }
            else
            {
                if (allPositions[i] == relPositions[nextRelPos])
                {
                    // Relation position so skip
                    nextRelPos++;
                }
                else
                {
                    // Not a relation position
                    nonrelPositions[nonrelPos++] = i;
                }
            }
        }

        return nonrelPositions;
    }

    /**
     * Convenience method to return the absolute positions of all fields/properties that have relations.
     * @param clr ClassLoader resolver
     * @return The absolute positions of all fields/properties that have relations
     */
    public int[] getRelationMemberPositions(ClassLoaderResolver clr)
    {
        if (relationPositions == null)
        {
            int[] superclassRelationPositions = null;
            if (pcSuperclassMetaData != null)
            {
                superclassRelationPositions = pcSuperclassMetaData.getRelationMemberPositions(clr);
            }

            int numRelationsSuperclass = superclassRelationPositions != null ? superclassRelationPositions.length : 0;
            int numRelations = numRelationsSuperclass;
            for (int i=0;i<managedMembers.length;i++)
            {
                if (managedMembers[i].getRelationType(clr) != RelationType.NONE || managedMembers[i].isPersistentInterface(clr))
                {
                    numRelations++;
                }
            }

            relationPositions = new int[numRelations];
            int num = 0;
            if (numRelationsSuperclass > 0)
            {
                for (int i=0;i<superclassRelationPositions.length;i++)
                {
                    relationPositions[num++] = superclassRelationPositions[i];
                }
            }
            if (numRelations > numRelationsSuperclass)
            {
                for (int i=0;i<managedMembers.length;i++)
                {
                    if (managedMembers[i].getRelationType(clr) != RelationType.NONE || managedMembers[i].isPersistentInterface(clr))
                    {
                        relationPositions[num++] = managedMembers[i].getAbsoluteFieldNumber();
                    }
                }
            }
        }
        return relationPositions;
    }

    /**
     * Convenience method to return the absolute positions of fields/properties that have bidirectional relations.
     * @param clr ClassLoader resolver
     * @return Absolute positions of bidirectional relation fields/properties
     */
    public int[] getBidirectionalRelationMemberPositions(ClassLoaderResolver clr)
    {
        if (relationPositions == null)
        {
            getRelationMemberPositions(clr);
        }

        int numBidirs = 0;
        for (int i=0;i<relationPositions.length;i++)
        {
            AbstractMemberMetaData mmd = getMetaDataForManagedMemberAtAbsolutePosition(relationPositions[i]);
            RelationType relationType = mmd.getRelationType(clr);
            if (RelationType.isBidirectional(relationType))
            {
                numBidirs++;
            }
        }

        int[] bidirRelations = new int[numBidirs];
        numBidirs = 0;
        for (int i=0;i<relationPositions.length;i++)
        {
            AbstractMemberMetaData mmd = getMetaDataForManagedMemberAtAbsolutePosition(relationPositions[i]);
            RelationType relationType = mmd.getRelationType(clr);
            if (RelationType.isBidirectional(relationType))
            {
                bidirRelations[numBidirs] = mmd.getAbsoluteFieldNumber();
            }
        }
        return bidirRelations;
    }

    public void setAccessViaField(boolean flag)
    {
    	this.accessViaField = flag;
    }
    public Boolean getAccessViaField()
    {
    	return this.accessViaField;
    }

    public void setMappedSuperclass(boolean mapped)
    {
        this.mappedSuperclass = mapped;
    }
    public boolean isMappedSuperclass()
    {
        return mappedSuperclass;
    }

    public void setSerializeRead(boolean serialise)
    {
        serializeRead = serialise;
    }
    public boolean isSerializeRead()
    {
        return serializeRead;
    }

    /**
     * Method to set that this class is "metadata complete" (see JPA spec).
     * Means that any annotations will be ignored.
     */
    public void setMetaDataComplete()
    {
        metaDataComplete = true;
    }

    /**
     * Accessor for whether this class is fully specified by this metadata and that any annotations should be ignored.
     * @return Whether we should ignore any annotations
     */
    public boolean isMetaDataComplete()
    {
        return metaDataComplete;
    }

    /**
     * Method to add a named query to this class. Rejects the addition of
     * duplicate named queries.
     * @param qmd Meta-Data for the query.
     */
    public void addQuery(QueryMetaData qmd)
    {
        if (qmd == null)
        {
            return;
        }

        if (queries == null)
        {
            queries = new HashSet<>();
        }
        queries.add(qmd);
        qmd.parent = this;
    }

    /**
     * Accessor for the number of named queries.
     * @return no of named queries
     */
    public int getNoOfQueries()
    {
        return queries.size();
    }

    /**
     * Accessor for the metadata of the named queries.
     * @return Meta-Data for the named queries.
     */
    public QueryMetaData[] getQueries()
    {
        return queries == null ? null : ((QueryMetaData[])queries.toArray(new QueryMetaData[queries.size()]));
    }

    /**
     * Method to create a new QueryMetadata, add it to the registered queries and return it.
     * @param queryName Name of the query
     * @return The Query metadata
     */
    public QueryMetaData newQueryMetadata(String queryName)
    {
        if (StringUtils.isWhitespace(queryName))
        {
            throw new InvalidClassMetaDataException("044154", fullName);
        }
        QueryMetaData qmd = new QueryMetaData(queryName);
        addQuery(qmd);
        return qmd;
    }

    /**
     * Method to add a named stored proc query to this class. Rejects the addition of
     * duplicate named queries.
     * @param qmd Meta-Data for the query.
     */
    public void addStoredProcQuery(StoredProcQueryMetaData qmd)
    {
        if (qmd == null)
        {
            return;
        }

        if (storedProcQueries == null)
        {
            storedProcQueries = new HashSet<>();
        }
        storedProcQueries.add(qmd);
        qmd.parent = this;
    }

    /**
     * Accessor for the number of named stored proc queries.
     * @return no of named stored proc queries
     */
    public int getNoOfStoredProcQueries()
    {
        return (storedProcQueries != null) ? storedProcQueries.size() : 0;
    }

    /**
     * Accessor for the metadata of the named stored proc queries.
     * @return Meta-Data for the named stored proc queries.
     */
    public StoredProcQueryMetaData[] getStoredProcQueries()
    {
        return storedProcQueries == null ? null : ((StoredProcQueryMetaData[])storedProcQueries.toArray(new StoredProcQueryMetaData[storedProcQueries.size()]));
    }

    /**
     * Method to create a new StoredProcQueryMetadata, add it to the registered queries and return it.
     * @param queryName Name of the proc query
     * @return The Query metadata
     */
    public StoredProcQueryMetaData newStoredProcQueryMetadata(String queryName)
    {
        if (StringUtils.isWhitespace(queryName))
        {
            throw new InvalidClassMetaDataException("044154", fullName);
        }
        StoredProcQueryMetaData qmd = new StoredProcQueryMetaData(queryName);
        addStoredProcQuery(qmd);
        return qmd;
    }

    /**
     * Method to register a query-result MetaData.
     * @param resultMetaData Query-Result MetaData to register
     */
    public void addQueryResultMetaData(QueryResultMetaData resultMetaData)
    {
        if (queryResultMetaData == null)
        {
            queryResultMetaData = new HashSet<>();
        }
        if (!queryResultMetaData.contains(resultMetaData))
        {
            queryResultMetaData.add(resultMetaData);
            resultMetaData.parent = this;
        }
    }

    /**
     * Get the query result MetaData registered for this class.
     * @return Query Result MetaData defined for this class
     */
    public QueryResultMetaData[] getQueryResultMetaData()
    {
        if (queryResultMetaData == null)
        {
            return null;
        }
        return queryResultMetaData.toArray(new QueryResultMetaData[queryResultMetaData.size()]);
    }

    /**
     * Method to add an index to this class.
     * @param idxmd Meta-Data for the index.
     */
    public void addIndex(IndexMetaData idxmd)
    {
        if (idxmd == null)
        {
            return;
        }
        if (isInitialised())
        {
            throw new NucleusUserException("Already initialised");
        }

        if (indexes == null)
        {
            indexes = new ArrayList<>();
        }
        indexes.add(idxmd);
        idxmd.parent = this;
    }

    public final List<IndexMetaData> getIndexMetaData()
    {
        return indexes;
    }

    /**
     * Method to create a new index metadata, add it, and return it.
     * @return The index metadata
     */
    public IndexMetaData newIndexMetadata()
    {
        IndexMetaData idxmd = new IndexMetaData();
        addIndex(idxmd);
        return idxmd;
    }

    /**
     * Method to add an foreign-key to this class.
     * @param fkmd Meta-Data for the foreign-key.
     */
    public void addForeignKey(ForeignKeyMetaData fkmd)
    {
        if (fkmd == null)
        {
            return;
        }
        if (isInitialised())
        {
            throw new NucleusUserException("Already initialised");
        }

        if (foreignKeys == null)
        {
            foreignKeys = new ArrayList<>();
        }
        foreignKeys.add(fkmd);
        fkmd.parent = this;
    }

    public final List<ForeignKeyMetaData> getForeignKeyMetaData()
    {
        return foreignKeys;
    }

    /**
     * Method to create a new FK metadata, add it, and return it.
     * @return The FK metadata
     */
    public ForeignKeyMetaData newForeignKeyMetadata()
    {
        ForeignKeyMetaData fkmd = new ForeignKeyMetaData();
        addForeignKey(fkmd);
        return fkmd;
    }

    /**
     * Method to add a unique constraint to this class.
     * @param unimd Meta-Data for the unique constraint.
     */
    public void addUniqueConstraint(UniqueMetaData unimd)
    {
        if (unimd == null)
        {
            return;
        }
        if (isInitialised())
        {
            throw new NucleusUserException("Already initialised");
        }

        if (uniqueConstraints == null)
        {
            uniqueConstraints = new ArrayList<>();
        }
        uniqueConstraints.add(unimd);
        unimd.parent = this;
    }

    public final List<UniqueMetaData> getUniqueMetaData()
    {
        return uniqueConstraints;
    }

    /**
     * Method to create a new unique metadata, add it, and return it.
     * @return The unique metadata
     */
    public UniqueMetaData newUniqueMetadata()
    {
        UniqueMetaData unimd = new UniqueMetaData();
        addUniqueConstraint(unimd);
        return unimd;
    }

    /**
     * Method to add an unmapped column.
     * @param colmd The metadata for the unmapped column
     */
    public final void addUnmappedColumn(ColumnMetaData colmd)
    {
        if (unmappedColumns == null)
        {
            unmappedColumns = new ArrayList<>();
        }
        unmappedColumns.add(colmd);
        colmd.parent = this;
    }

    /**
     * Accessor for the unmapped columns required for the datastore table.
     * @return The list of unmapped columns
     */
    public final List<ColumnMetaData> getUnmappedColumns()
    {
        return unmappedColumns;
    }

    public ColumnMetaData newUnmappedColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        addUnmappedColumn(colmd);
        return colmd;
    }

    /**
     * Method to create a new field metadata, add it, and return it.
     * @param fieldName Name of the field
     * @return The metadata
     */
    public FieldMetaData newFieldMetadata(String fieldName)
    {
        FieldMetaData fmd = new FieldMetaData(this, fieldName);
        addMember(fmd);
        return fmd;
    }

    /**
     * Method to create a new property metadata, add it, and return it.
     * @param propName Name of the property
     * @return The metadata
     */
    public PropertyMetaData newPropertyMetadata(String propName)
    {
        PropertyMetaData pmd = new PropertyMetaData(this, propName);
        addMember(pmd);
        return pmd;
    }

    /**
     * Method to add a field/property to this interface.
     * Rejects the addition of duplicate named fields/properties.
     * @param mmd Field/Property MetaData
     */
    public void addMember(AbstractMemberMetaData mmd)
    {
        if (mmd == null)
        {
            return;
        }
        if (isInitialised())
        {
            // TODO Localise this message
            throw new NucleusUserException("adding field/property " + mmd.getName() + " when already initialised!");
        }

        // Check for conflicting fields/properties
        Iterator<AbstractMemberMetaData> iter = members.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData md = iter.next();
            if (mmd.getName().equals(md.getName()))
            {
                if ((mmd instanceof PropertyMetaData && md instanceof PropertyMetaData) || (mmd instanceof FieldMetaData && md instanceof FieldMetaData))
                {
                    // Duplicate entry for the same field or property
                    throw new NucleusUserException(Localiser.msg("044090", fullName, mmd.getName()));
                }
            }

            String existingName = md.getName();
            boolean existingIsProperty = md instanceof PropertyMetaData;
            if (existingIsProperty)
            {
                existingName = ((PropertyMetaData)md).getFieldName();
                if (existingName == null)
                {
                    // "fieldName" not specified so default to the property name
                    existingName = md.getName();
                }
            }
            String newName = mmd.getName();
            boolean newIsProperty = mmd instanceof PropertyMetaData;
            if (newIsProperty)
            {
                newName = ((PropertyMetaData)mmd).getFieldName();
                if (newName == null)
                {
                    // "fieldName" not specified so default to the property name
                    newName = mmd.getName();
                }
            }

            if (existingName.equals(newName))
            {
                if (existingIsProperty && newIsProperty)
                {
                    // Duplicate entry for the same field or property
                    throw new NucleusUserException(Localiser.msg("044090", fullName, mmd.getName()));
                }
                else if (existingIsProperty && !newIsProperty)
                {
                    // We have the property and this is a field so ignore it
                    // TODO Check if one is not persistent before discarding
                    NucleusLogger.METADATA.debug("Ignoring metadata for field " + mmd.getFullFieldName() + 
                        " since we already have MetaData for the property " + md.getFullFieldName());
                    return;
                }
                else if (!existingIsProperty && newIsProperty)
                {
                    // We have the field and this is property so replace the field with it
                    // TODO Check if one is not persistent before discarding
                    NucleusLogger.METADATA.debug("Ignoring existing metadata for field " + md.getFullFieldName() + 
                        " since now we have MetaData for the property " + mmd.getFullFieldName());
                    iter.remove();
                }
            }
        }

        mmd.parent = this;
        members.add(mmd);
    }

    /**
     * Method to add a fetch-group to this class.
     * @param fgmd Meta-Data for the fetch-group.
     */
    public void addFetchGroup(FetchGroupMetaData fgmd)
    {
        if (fgmd == null)
        {
            return;
        }
        if (isInitialised())
        {
            throw new NucleusUserException("Already initialised");
        }

        if (fetchGroups == null)
        {
            fetchGroups = new HashSet<>();
        }
        fetchGroups.add(fgmd);
        fgmd.parent = this;
    }

    /**
     * Method to create a new fetchgroup metadata, add it, and return it.
     * @param name Name of the group
     * @return The fetchgroup metadata
     */
    public FetchGroupMetaData newFetchGroupMetaData(String name)
    {
        FetchGroupMetaData fgmd = new FetchGroupMetaData(name);
        addFetchGroup(fgmd);
        return fgmd;
    }

    /**
     * Accessor for fetch group metadata for the specified groups (if present).
     * The returned metadata is what is defined for this class that matches any of the names in the input set.
     * @param groupNames Names of the fetch groups
     * @return MetaData for the groups
     */
    public Set<FetchGroupMetaData> getFetchGroupMetaData(Collection<String> groupNames)
    {
        Set<FetchGroupMetaData> results = new HashSet<>();
        for (String groupName : groupNames)
        {
            FetchGroupMetaData fgmd = getFetchGroupMetaData(groupName);
            if (fgmd != null)
            {
                results.add(fgmd);
            }
        }
        return results;
    }

    /**
     * Accessor for the fetch group metadata for the group specified.
     * @param groupname Name of the fetch group
     * @return MetaData for this group
     */
    public FetchGroupMetaData getFetchGroupMetaData(String groupname)
    {
        FetchGroupMetaData fgmd = fetchGroupMetaDataByName != null ? fetchGroupMetaDataByName.get(groupname) : null;
        if (fgmd == null && pcSuperclassMetaData != null)
        {
            return pcSuperclassMetaData.getFetchGroupMetaData(groupname);
        }
        return fgmd;
    }

    /**
     * Accessor for all MetaData defined for fetch groups for this class.
     * This doesn't include superclasses.
     * @return Returns the Fetch Group metadata registered on this class
     */
    public final Set<FetchGroupMetaData> getFetchGroupMetaData()
    {
        return fetchGroups;
    }

    /**
     * Whether this class or any super class has any fetch group definition
     * with {@link FetchGroupMetaData#getPostLoad()}==true.
     * @return Whether there is a fetch-group definition with post-load
     */
    public final boolean hasFetchGroupWithPostLoad()  
    {
        if (fetchGroupMetaWithPostLoad == null)
        {
            fetchGroupMetaWithPostLoad = Boolean.FALSE;
            if (fetchGroups != null)
            {
                for (FetchGroupMetaData fgmd : fetchGroups)
                {
                    if (fgmd.getPostLoad().booleanValue())
                    {
                        fetchGroupMetaWithPostLoad = Boolean.TRUE;
                        break;
                    }
                }
            }
        }
        if (getSuperAbstractClassMetaData()!=null)
        {
            return getSuperAbstractClassMetaData().hasFetchGroupWithPostLoad() || fetchGroupMetaWithPostLoad.booleanValue();
        }
        return fetchGroupMetaWithPostLoad.booleanValue();
    }

    /**
     * Method to add a join to this class.
     * Rejects the addition of duplicate named fields.
     * @param jnmd Meta-Data for the join.
     */
    public void addJoin(JoinMetaData jnmd)
    {
        if (jnmd == null)
        {
            return;
        }
        if (isInitialised())
        {
            throw new NucleusUserException("Already initialised");
        }

        if (joins == null)
        {
            joins = new ArrayList<>();
        }
        joins.add(jnmd);
        jnmd.parent = this;
    }

    public final List<JoinMetaData> getJoinMetaData()
    {
        return joins;
    }

    /**
     * Method to create a new join metadata, add it, and return it.
     * @return The join metadata
     */
    public JoinMetaData newJoinMetaData()
    {
        JoinMetaData joinmd = new JoinMetaData();
        addJoin(joinmd);
        return joinmd;
    }

    /**
     * Add a listener class name
     * @param listener the listener metadata. Duplicated classes are ignored
     */
    public void addListener(EventListenerMetaData listener)
    {
        if (listeners == null)
        {
            listeners = new ArrayList<>();
        }
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
            listener.parent = this;
        }
    }

    /**
     * Accessor for the EventListener info for an EventListener class name
     * @param className Name of the event listener class
     * @return EventListener info for this class (or null if the class isnt an EventListener)
     */
    public EventListenerMetaData getListenerForClass(String className)
    {
        if (listeners == null)
        {
            return null;
        }

        for (int i=0;i<listeners.size();i++)
        {
            EventListenerMetaData elmd = listeners.get(i);
            if (elmd.getClassName().equals(className))
            {
                return elmd;
            }
        }

        return null;
    }

    /**
     * Get the event listeners
     * @return the event listeners
     */
    public List<EventListenerMetaData> getListeners()
    {
        return listeners;
    }

    /**
     * Toogle exclude super class listeners
     */
    public void excludeSuperClassListeners()
    {
        this.excludeSuperClassListeners = Boolean.TRUE;
    }
    
    /**
     * Whether super classes listeners are not going to be invoked
     * @return true if super class listeners are not invoked
     */
    public boolean isExcludeSuperClassListeners()
    {
        if (excludeSuperClassListeners != null && Boolean.TRUE.equals(excludeSuperClassListeners))
        {
            return true;
        }
        return false;
    }

    /**
     * Method to exclude default listeners.
     */
    public void excludeDefaultListeners()
    {
        this.excludeDefaultListeners = Boolean.TRUE;
    }

    /**
     * Whether default listeners are not going to be invoked
     * @return true if default listeners are not invoked
     */
    public boolean isExcludeDefaultListeners()
    {
        if (excludeDefaultListeners != null && Boolean.TRUE.equals(excludeDefaultListeners))
        {
            return true;
        }
        return false;
    }

    /**
     * Mutator for the Version MetaData.
     * @param versionMetaData The versionMetaData to set.
     */
    public final void setVersionMetaData(VersionMetaData versionMetaData)
    {
        this.versionMetaData = versionMetaData;
        if (this.versionMetaData != null)
        {
            this.versionMetaData.parent = this;
        }
    }

    /**
     * Accessor for Version MetaData for this class specifically.
     * Note that this just returns what this class had defined, and if this has no version info then
     * you really need what the superclass has (if there is one). Consider using getVersionMetaDataForClass().
     * @return Returns the versionMetaData.
     */
    public final VersionMetaData getVersionMetaData()
    {
        return versionMetaData;
    }

    /**
     * Convenience accessor for the version metadata applying to this class.
     * Differs from getVersionMetaData by searching superclasses.
     * @return The version metadata
     */
    public final VersionMetaData getVersionMetaDataForClass()
    {
        if (versionMetaData != null)
        {
            // Version information specified at this level
            return versionMetaData;
        }
        else if (getSuperAbstractClassMetaData() != null)
        {
            // Use superclass version information
            return getSuperAbstractClassMetaData().getVersionMetaDataForClass();
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns whether objects of this type are versioned.
     * A return of true means that either this class has version information, or a superclass does, and that
     * the version information is required to be stored.
     * @return Whether it is versioned.
     */
    public final boolean isVersioned()
    {
        VersionMetaData vermd = getVersionMetaDataForClass();
        if (vermd != null && vermd.getVersionStrategy() != null && vermd.getVersionStrategy() != VersionStrategy.NONE)
        {
            return true;
        }
        return false;
    }

    /**
     * Convenience method to find the version MetaData defining versioning for the same 'table'
     * as this class is using. Traverses up the inheritance tree to find the highest class that uses
     * "subclass-table" that has version metadata defined, or the class that owns the 'table' where
     * this class uses "superclass-table", and returns the MetaData.
     * @return Version MetaData for the highest class in this tree using subclass-table
     */
    public final VersionMetaData getVersionMetaDataForTable()
    {
        if (pcSuperclassMetaData != null)
        {
            InheritanceStrategy thisStrategy = getInheritanceMetaData().getStrategy();
            InheritanceStrategy superStrategy = pcSuperclassMetaData.getInheritanceMetaData().getStrategy();
            if (thisStrategy == InheritanceStrategy.SUPERCLASS_TABLE && superStrategy == InheritanceStrategy.SUPERCLASS_TABLE)
            {
                // Relay up to the superclass to see if it can find the owner of the table
                VersionMetaData vermd = pcSuperclassMetaData.getVersionMetaDataForTable();
                if (vermd != null)
                {
                    return vermd;
                }
            }
            if (thisStrategy == InheritanceStrategy.SUPERCLASS_TABLE && superStrategy == InheritanceStrategy.NEW_TABLE)
            {
                // Superclass owns the table that we use so relay up to superclass
                VersionMetaData vermd = pcSuperclassMetaData.getVersionMetaDataForTable();
                if (vermd != null)
                {
                    return vermd;
                }
            }
            if (thisStrategy == InheritanceStrategy.NEW_TABLE && superStrategy == InheritanceStrategy.SUBCLASS_TABLE)
            {
                // We own the table but the superclass uses it too, so relay up to superclass
                VersionMetaData vermd = pcSuperclassMetaData.getVersionMetaDataForTable();
                if (vermd != null)
                {
                    // Superclass has versioning info so return that
                    return vermd;
                }
            }
            if (thisStrategy == InheritanceStrategy.SUBCLASS_TABLE && superStrategy == InheritanceStrategy.SUBCLASS_TABLE)
            {
                // Subclass owns the table but the superclass uses it too, so relay up to superclass
                VersionMetaData vermd = pcSuperclassMetaData.getVersionMetaDataForTable();
                if (vermd != null)
                {
                    // Superclass has versioning info so return that
                    return vermd;
                }
            }
            if (thisStrategy == InheritanceStrategy.COMPLETE_TABLE)
            {
                VersionMetaData vermd = pcSuperclassMetaData.getVersionMetaDataForTable();
                if (vermd != null)
                {
                    // Superclass is persisted into this table, so use its version
                    return vermd;
                }
            }
        }

        // Nothing in superclasses sharing our table so return ours
        return versionMetaData;
    }

    /**
     * Method to create a new version metadata, set to use it, and return it.
     * @return The version metadata
     */
    public VersionMetaData newVersionMetadata()
    {
        this.versionMetaData = new VersionMetaData();
        this.versionMetaData.parent = this;
        return this.versionMetaData;
    }

    /**
     * Mutator for the datastore identity MetaData.
     * @param identityMetaData The datastore identity MetaData to set.
     */
    public final void setDatastoreIdentityMetaData(DatastoreIdentityMetaData identityMetaData)
    {
        this.datastoreIdentityMetaData = identityMetaData;
        if (this.datastoreIdentityMetaData != null)
        {
            this.datastoreIdentityMetaData.parent = this;
        }
        identitySpecified = true;
    }

    /**
     * Accessor for datastore identity MetaData
     * @return Returns the datastore identity MetaData.
     */
    public final DatastoreIdentityMetaData getDatastoreIdentityMetaData()
    {
        return datastoreIdentityMetaData;
    }

    /**
     * Convenience method to return the root identity metadata for this inheritance tree.
     * @return DatastoreIdentityMetaData at the base
     */
    public final DatastoreIdentityMetaData getBaseDatastoreIdentityMetaData()
    {
        return (pcSuperclassMetaData != null) ? pcSuperclassMetaData.getBaseDatastoreIdentityMetaData() : datastoreIdentityMetaData;
    }

    /**
     * Method to create a new datastore identity metadata, set to use it, and return it.
     * @return The datastore identity metadata
     */
    public DatastoreIdentityMetaData newDatastoreIdentityMetadata()
    {
        this.datastoreIdentityMetaData = new DatastoreIdentityMetaData();
        this.datastoreIdentityMetaData.parent = this;
        this.identitySpecified = true;
        return this.datastoreIdentityMetaData;
    }

    public final MultitenancyMetaData getMultitenancyMetaData()
    {
        return multitenancyMetaData;
    }

    public final void setMultitenancyMetaData(MultitenancyMetaData mtmd)
    {
        this.multitenancyMetaData = mtmd;
        if (this.multitenancyMetaData != null)
        {
            this.multitenancyMetaData.parent = this;
        }
    }

    public MultitenancyMetaData newMultitenancyMetaData()
    {
        this.multitenancyMetaData = new MultitenancyMetaData();
        this.multitenancyMetaData.parent = this;
        return this.multitenancyMetaData;
    }

    public final SoftDeleteMetaData getSoftDeleteMetaData()
    {
        return softDeleteMetaData;
    }

    public final void setSoftDeleteMetaData(SoftDeleteMetaData sdmd)
    {
        this.softDeleteMetaData = sdmd;
        if (this.softDeleteMetaData != null)
        {
            this.softDeleteMetaData.parent = this;
        }
    }

    public SoftDeleteMetaData newSoftDeleteMetaData()
    {
        this.softDeleteMetaData = new SoftDeleteMetaData();
        this.softDeleteMetaData.parent = this;
        return this.softDeleteMetaData;
    }

    public boolean isSoftDelete()
    {
        return softDeleteMetaData != null;
    }

    /**
     * Mutator for the inheritance MetaData.
     * @param inheritanceMetaData The inheritanceMetaData to set.
     */
    public final void setInheritanceMetaData(InheritanceMetaData inheritanceMetaData)
    {
        this.inheritanceMetaData = inheritanceMetaData;
        if (this.inheritanceMetaData != null)
        {
            this.inheritanceMetaData.parent = this;
        }
    }

    /**
     * Accessor for inheritanceMetaData
     * @return Returns the inheritanceMetaData.
     */
    public final InheritanceMetaData getInheritanceMetaData()
    {
        return inheritanceMetaData;
    }

    /**
     * Method to create a new inheritance metadata, set to use it, and return it.
     * @return The inheritance metadata
     */
    public InheritanceMetaData newInheritanceMetadata()
    {
        this.inheritanceMetaData = new InheritanceMetaData();
        this.inheritanceMetaData.parent = this;
        return this.inheritanceMetaData;
    }

    /**
     * Mutator for the PrimaryKey MetaData.
     * @param primaryKeyMetaData The PrimaryKey MetaData to set.
     */
    public final void setPrimaryKeyMetaData(PrimaryKeyMetaData primaryKeyMetaData)
    {
        this.primaryKeyMetaData = primaryKeyMetaData;
        if (this.primaryKeyMetaData != null)
        {
            this.primaryKeyMetaData.parent = this;
        }
    }

    /**
     * Accessor for primaryKeyMetaData
     * @return Returns the primaryKey MetaData.
     */
    public final PrimaryKeyMetaData getPrimaryKeyMetaData()
    {
        return primaryKeyMetaData;
    }

    /**
     * Method to create a new primary key metadata, set to use it, and return it.
     * @return The primary key metadata
     */
    public PrimaryKeyMetaData newPrimaryKeyMetadata()
    {
        this.primaryKeyMetaData = new PrimaryKeyMetaData();
        this.primaryKeyMetaData.parent = this;
        return this.primaryKeyMetaData;
    }

    /**
     * Method to return the ClassMetaData records for classes referenced by this object. 
     * This adds the entries to orderedCmds ordered by dependency, and to referencedCmds for fast lookups.
     * <p>
     * Uses recursion to add all referenced ClassMetaData for any fields, objectid classes, superclasses, and extension "views".
     * This is the entry point for this process.
     * </p>
     * @param orderedCmds List of ordered ClassMetaData objects (added to).
     * @param referencedCmds Set of all ClassMetaData objects (added to).
     * @param clr the ClassLoaderResolver
     */
    void getReferencedClassMetaData(final List<AbstractClassMetaData> orderedCmds, final Set<AbstractClassMetaData> referencedCmds, final ClassLoaderResolver clr)
    {
        Map<String, Set<String>> viewReferences = new HashMap<>();
        getReferencedClassMetaData(orderedCmds, referencedCmds, viewReferences, clr);
    }

    /**
     * Method to return the ClassMetaData for classes referenced by this object. This method does the actual work of addition.
     * @param orderedCmds List of ordered ClassMetaData objects (added to).
     * @param referencedCmds Set of all ClassMetaData objects (added to).
     * @param viewReferences Map, mapping class name to set of referenced class for all views.
     * @param clr the ClassLoaderResolver
     */
    private void getReferencedClassMetaData(final List<AbstractClassMetaData> orderedCmds, final Set<AbstractClassMetaData> referencedCmds, final Map<String, Set<String>> viewReferences, 
            final ClassLoaderResolver clr)
    {
        // Recursively call getReferencedClassMetaData(...) before adding them to the orderedCmds and referenced. 
        // This will ensure that any classes with dependencies on them are put in the orderedCmds List in the correct order.
        if (!referencedCmds.contains(this))
        {
            // Go ahead and add this class to the referenced Set, it will get added to the orderedCmds List after all classes that this depends on have been added.
            referencedCmds.add(this);

            for (int i=0;i<managedMembers.length;i++)
            {
                managedMembers[i].getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
            }

            // Add on any superclass
            if (persistableSuperclass != null)
            {
                getSuperAbstractClassMetaData().getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
            }

            // Add on any objectid class
            if (objectidClass != null && !usesSingleFieldIdentityClass())
            {
                AbstractClassMetaData idCmd = mmgr.getMetaDataForClass(objectidClass, clr);
                if (idCmd != null)
                {
                    idCmd.getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
                }
            }

            // Add on any view-definition for this class
            String viewDefStr = getValueForExtension(MetaData.EXTENSION_CLASS_VIEW_DEFINITION);
            if (viewDefStr!= null)
            {
                MacroString viewDef = new MacroString(fullName, getValueForExtension(MetaData.EXTENSION_CLASS_VIEW_IMPORTS), viewDefStr);
                viewDef.substituteMacros(new MacroString.MacroHandler()
                    {
                        public void onIdentifierMacro(MacroString.IdentifierMacro im)
                        {
                            if (!getFullClassName().equals(im.className)) //ignore itself
                            {
                                addViewReference(viewReferences, im.className);
                                AbstractClassMetaData viewCmd = mmgr.getMetaDataForClass(im.className, clr);
                                viewCmd.getReferencedClassMetaData(orderedCmds, referencedCmds, viewReferences, clr);
                            }
                        }

                        public void onParameterMacro(MacroString.ParameterMacro pm)
                        {
                            throw new NucleusUserException("Parameter macros not allowed in view definitions: " + pm);
                        }
                    },clr);
            }

            orderedCmds.add(this);
        }
    }

    /**
     * Method to add a reference for views. Check the view references for circular
     * dependencies. If there are any circular dependencies, throw a NucleusUserException.
     * @param viewReferences The Map of classname to Set of referenced Classes to add the reference to.
     * @param referencedName Class name of the referenced class
     * @throws NucleusUserException Thrown if a circular reference is found
     */
    private void addViewReference(Map<String, Set<String>> viewReferences, String referencedName)
    {
        if (fullName.equals(referencedName))
        {
            // Add this reference to the Map.
            Set<String> referencedSet = viewReferences.get(referencedName);
            if (referencedSet == null)
            {
                referencedSet = new HashSet<>();
                viewReferences.put(fullName, referencedSet);
            }
            referencedSet.add(referencedName);

            // Check to see if there is a circular dependency.  This will
            // be true if the referenced class references this class.
            AbstractClassMetaData.checkForCircularViewReferences(viewReferences, fullName, referencedName, null);
        }
    }

    /**
     * Check for any circular view references between referencer and referencee.
     * If one is found, throw a NucleusUserException with the chain of references.
     * @param viewReferences The Map of view references to check.
     * @param referencerName Name of the class that has the reference.
     * @param referenceeName Name of the class that is being referenced.
     * @param referenceChain The List of class names that have been referenced
     * @throws NucleusUserException If a circular reference is found in the view definitions.
     */
    protected static void checkForCircularViewReferences(Map<String, Set<String>> viewReferences, String referencerName, String referenceeName, List<String> referenceChain)
    {
        Set<String> classNames = viewReferences.get(referenceeName);
        if (classNames != null)
        {
            // Initialize the chain of references if needed.  Add the referencee to the chain.
            if (referenceChain == null)
            {
                referenceChain = new ArrayList<>();
                referenceChain.add(referencerName);
            }
            referenceChain.add(referenceeName);

            // Iterate through all referenced classes from the referencee.  If any reference the referencer, throw an exception.
            for (Iterator<String> it=classNames.iterator(); it.hasNext(); )
            {
                String currentName = it.next();
                if (currentName.equals(referencerName))
                {
                    StringBuilder error = new StringBuilder(Localiser.msg("031003"));
                    for (Iterator chainIter=referenceChain.iterator(); chainIter.hasNext(); )
                    {
                        error.append(chainIter.next());
                        if (chainIter.hasNext())
                        {
                            error.append(" -> ");
                        }
                    }

                    throw new NucleusUserException(error.toString()).setFatal();
                }

                // Make recursive call to check for any nested dependencies. e.g A references B, B references C, C references A.
                AbstractClassMetaData.checkForCircularViewReferences(viewReferences, referencerName, currentName, referenceChain);
            }
        }
    }
}
