/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
2007 Xuan Baldauf - Implement solution for issue CORE-3272
2007 Xuan Baldauf - allow users to explictly state that an array whose component type is not PC may still have PC elements. See CORE-3274
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.types.ContainerHandler;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of MetaData for a field/property of a class/interface.
 * The term "member" is used to represent either a field or a method(property). The term
 * property is used to represent the name after cutting off any Java-beans style "get" prefix.
 * This class is extended for fields (FieldMetaData) and properties (PropertyMetaData) to provide
 * the explicit support for those components.
 */
public abstract class AbstractMemberMetaData extends MetaData implements Comparable, ColumnMetaDataContainer
{
    private static final long serialVersionUID = -7689828287704042919L;

    /** Whether we currently allow persistence of static fields. */
    public static final boolean PERSIST_STATIC = false;
    /** Whether we currently allow persistence of final fields. */
    public static final boolean PERSIST_FINAL = false;
    /** Whether we currently allow persistence of transient fields. */
    public static final boolean PERSIST_TRANSIENT = false;

    /** Contains the metadata for column(s). */
    protected ColumnMetaData[] columnMetaData;

    /** Meta-Data of any container. */
    protected ContainerMetaData containerMetaData;

    /** EmbeddedMetaData. */
    protected EmbeddedMetaData embeddedMetaData;

    /** JoinMetaData. */
    protected JoinMetaData joinMetaData;

    /** ElementMetaData. */
    protected ElementMetaData elementMetaData;

    /** KeyMetaData. */
    protected KeyMetaData keyMetaData;

    /** ValueMetaData. */
    protected ValueMetaData valueMetaData;

    /** IndexMetaData. */
    protected IndexMetaData indexMetaData;

    /** The indexing value */
    protected IndexedValue indexed = null;

    /** UniqueMetaData. */
    protected UniqueMetaData uniqueMetaData;

    /** Whether to add a unique constraint. */
    protected boolean uniqueConstraint = false;

    /** OrderMetaData. */
    protected OrderMetaData orderMetaData;

    /** ForeignKeyMetaData. */
    protected ForeignKeyMetaData foreignKeyMetaData;

    /** default-fetch-group tag value. */
    protected Boolean defaultFetchGroup;

    /** column tag value.  */
    protected String column;

    /** mapped-by tag value. */
    protected String mappedBy;

    /** embedded tag value. */
    protected Boolean embedded;

    /** Whether this field contains a reference that should be deleted when deleting this field.  */
    protected Boolean dependent;

    /** serialized tag value. */
    protected Boolean serialized;

    /** cacheable tag value. */
    protected boolean cacheable = true;

    /** Whether to persist this relation when persisting the owning object. */
    protected Boolean cascadePersist;

    /** Whether to update this relation when updating the owning object. */
    protected Boolean cascadeUpdate;

    /** Whether to delete this relation when deleting the owning object (JPA). TODO Link this to dependent */
    protected Boolean cascadeDelete;

    /** Whether to detach this relation when detaching the owning object (JPA). */
    protected Boolean cascadeDetach;

    /** Whether to refresh this relation when refreshing the owning object (JPA). */
    protected Boolean cascadeRefresh;

    /** Whether to remove orphans when deleting the owning object (JPA). */
    protected boolean cascadeRemoveOrphans = false;

    /** load-fetch-group value. */
    protected String loadFetchGroup;

    /** Default recursion-depth according to proposed final draft spec, [12.7.2]. */
    public static final int DEFAULT_RECURSION_DEPTH = 1;

    /** Indicates the recursion-depth is not defined.  Use default value. */
    public static final int UNDEFINED_RECURSION_DEPTH = 0;

    /** recursion-depth value. */
    protected int recursionDepth = UNDEFINED_RECURSION_DEPTH;

    /** Field name. */
    protected final String name;

    /** null-value tag value (default is NONE). */
    protected NullValue nullValue = NullValue.NONE;

    /** persistence-modifier tag value. */
    protected FieldPersistenceModifier persistenceModifier = FieldPersistenceModifier.DEFAULT;

    /** primary key tag value. */
    protected Boolean primaryKey;

    /** Table name for this field. */
    protected String table;

    /** Catalog for the table specified for this field. */
    protected String catalog;

    /** Schema for the table specified for this field. */
    protected String schema;

    /**
     * The value-strategy attribute specifies the strategy used to generate
     * values for the field. This attribute has the same values and meaning as
     * the strategy attribute in datastoreidentity.
     */
    protected IdentityStrategy valueStrategy;

    /** Name of a value generator if the user wants to override the default generator. */
    protected String valueGeneratorName;

    /**
     * If the value-strategy is sequence, the sequence attribute specifies the
     * name of the sequence to use to automatically generate a value for the field.
     */
    protected String sequence;

    /**
     * Name of the class to which this field really belongs. Will be null if the field belongs
     * to the parent ClassMetaData, and will have a value if it is an overriding field.
     */
    protected String className = null;

    /** Cache result of {@link #getFullFieldName()}. */
    protected String fullFieldName = null;

    /** Field type being represented. */
    protected Class type;

    /** The member (field/method) being represented here. Note, this prevents Serialization. */
    protected Member memberRepresented;

    /** Id of the field in its class (only for fields that are managed). If the value is -1, the field is NOT managed or the object hasn't been populated. */
    protected int fieldId=-1;

    /** The relation type of this field (1-1, 1-N, M-N, N-1). */
    protected RelationType relationType = null;

    /** MetaData for the other end of a relation when this member is a bidirectional relation. This may be multiple fields if the FK is shared. */
    protected AbstractMemberMetaData[] relatedMemberMetaData = null;

    /** Temporary flag to signify if the field is ordered. */
    protected boolean ordered = false;

    // -------------------------------------------------------------------------
    // These fields are only used when the MetaData is read by the parser and
    // elements are dynamically added to the other elements. At runtime, "columnMetaData"
    // should be used.

    /** Columns ColumnMetaData */
    protected List<ColumnMetaData> columns = new ArrayList();

    /** Name of the target entity (when used with JPA MetaData on OneToOne, OneToMany etc) */
    protected String targetClassName = null;

    /** Wrapper for the ugly JPA "lob" so that when being populated we should make this serialised in some way. */
    protected boolean storeInLob = false;

    /** Placeholder for the JPA "mapsId" attribute, in case a store plugin wants to use it */
    protected String mapsIdAttribute = null;

    /** Placeholder for the JPA relation type ManyToOne, OneToOne etc so we can store what the user specified. */
    protected String relationTypeString = null;

    /** Flags for use in enhancement process [see JDO spec 21.14] */
    protected byte persistenceFlags;

    /**
     * Convenience constructor to copy the specification from the passed member.
     * This is used when we have an overriding field and we make a copy of the baseline
     * member as a starting point.
     * @param parent The parent
     * @param mmd The member metadata to copy
     */
    public AbstractMemberMetaData(MetaData parent, AbstractMemberMetaData mmd)
    {
        super(parent, mmd);

        // Copy the simple field values
        this.name = mmd.name;
        this.primaryKey = mmd.primaryKey;
        this.defaultFetchGroup = mmd.defaultFetchGroup;
        this.column = mmd.column;
        this.mappedBy = mmd.mappedBy;
        this.dependent = mmd.dependent;
        this.embedded = mmd.embedded;
        this.serialized = mmd.serialized;
        this.cascadePersist = mmd.cascadePersist;
        this.cascadeUpdate = mmd.cascadeUpdate;
        this.cascadeDelete = mmd.cascadeDelete;
        this.cascadeDetach = mmd.cascadeDetach;
        this.cascadeRefresh = mmd.cascadeRefresh;
        this.nullValue = mmd.nullValue;
        this.persistenceModifier = mmd.persistenceModifier;
        this.table = mmd.table;
        this.indexed = mmd.indexed;
        this.valueStrategy = mmd.valueStrategy;
        this.valueGeneratorName = mmd.valueGeneratorName;
        this.sequence = mmd.sequence;
        this.uniqueConstraint = mmd.uniqueConstraint;
        this.loadFetchGroup = mmd.loadFetchGroup;
        this.storeInLob = mmd.storeInLob;
        this.mapsIdAttribute = mmd.mapsIdAttribute;
        this.relationTypeString = mmd.relationTypeString;
        this.column = mmd.column;

        // Create copies of the object fields
        if (mmd.joinMetaData != null)
        {
            setJoinMetaData(new JoinMetaData(mmd.joinMetaData));
        }
        if (mmd.elementMetaData != null)
        {
            setElementMetaData(new ElementMetaData(mmd.elementMetaData));
        }
        if (mmd.keyMetaData != null)
        {
            setKeyMetaData(new KeyMetaData(mmd.keyMetaData));
        }
        if (mmd.valueMetaData != null)
        {
            setValueMetaData(new ValueMetaData(mmd.valueMetaData));
        }
        if (mmd.orderMetaData != null)
        {
            setOrderMetaData(new OrderMetaData(mmd.orderMetaData));
        }
        if (mmd.indexMetaData != null)
        {
            setIndexMetaData(new IndexMetaData(mmd.indexMetaData));
        }
        if (mmd.uniqueMetaData != null)
        {
            setUniqueMetaData(new UniqueMetaData(mmd.uniqueMetaData));
        }
        if (mmd.foreignKeyMetaData != null)
        {
            setForeignKeyMetaData(new ForeignKeyMetaData(mmd.foreignKeyMetaData));
        }
        if (mmd.embeddedMetaData != null)
        {
            setEmbeddedMetaData(new EmbeddedMetaData(mmd.embeddedMetaData));
        }
        if (mmd.containerMetaData != null)
        {
            if (mmd.containerMetaData instanceof CollectionMetaData)
            {
                setContainer(new CollectionMetaData((CollectionMetaData)mmd.containerMetaData));
            }
            else if (mmd.containerMetaData instanceof MapMetaData)
            {
                setContainer(new MapMetaData((MapMetaData)mmd.containerMetaData));
            }
            else if (mmd.containerMetaData instanceof ArrayMetaData)
            {
                setContainer(new ArrayMetaData((ArrayMetaData)mmd.containerMetaData));
            }
        }
        for (int i=0;i<mmd.columns.size();i++)
        {
            addColumn(new ColumnMetaData(mmd.columns.get(i)));
        }
    }

    public MetaDataManager getMetaDataManager()
    {
        return getAbstractClassMetaData().getMetaDataManager();
    }

    /**
     * Constructor for a fields metadata. Set the fields using setters, before populate().
     * @param parent parent MetaData instance TODO Remove this
     * @param name field/property name 
     */
    public AbstractMemberMetaData(MetaData parent, final String name)
    {
        super(parent);

        if (name == null)
        {
            throw new NucleusUserException(Localiser.msg("044041","name", getClassName(true), "field"));
        }
        if (name.indexOf('.') >= 0)
        {
            // TODO Check if this is a valid className
            this.className = name.substring(0,name.lastIndexOf('.'));
            this.name = name.substring(name.lastIndexOf('.')+1);
        }
        else
        {
            this.name = name;
        }
    }

    /**
     * Method to provide the details of the field being represented by this MetaData hence populating
     * certain parts of the MetaData. This is used to firstly provide defaults for attributes that aren't 
     * specified in the MetaData, and secondly to report any errors with attributes that have been specifed 
     * that are inconsistent with the field being represented. 
     * Either a field or a method should be passed in (one or the other) depending on what is being represented
     * by this "member".
     * @param clr ClassLoaderResolver to use for any class loading 
     * @param field Field that we are representing (if it's a field)
     * @param method Method(property) that we are representing (if it's a method).
     * @param primary the primary ClassLoader to use (or null)
     * @param mmgr MetaData manager
     */
    public synchronized void populate(ClassLoaderResolver clr, Field field, Method method, ClassLoader primary, MetaDataManager mmgr)
    {
        if (isPopulated() || isInitialised())
        {
            return;
        }

        if (mmgr != null)
        {
            // Set defaults for cascading when not yet set
            ApiAdapter apiAdapter = mmgr.getNucleusContext().getApiAdapter();
            if (cascadePersist == null)
            {
                cascadePersist = apiAdapter.getDefaultCascadePersistForField();
            }
            if (cascadeUpdate == null)
            {
                cascadeUpdate = apiAdapter.getDefaultCascadeUpdateForField();
            }
            if (cascadeDelete == null)
            {
                cascadeDelete = apiAdapter.getDefaultCascadeDeleteForField();
            }
            if (cascadeDetach == null)
            {
                cascadeDetach = false;
            }
            if (cascadeRefresh == null)
            {
                cascadeRefresh = apiAdapter.getDefaultCascadeRefreshForField();
            }
        }

        if (field == null && method == null)
        {
            NucleusLogger.METADATA.error(Localiser.msg("044106", getClassName(), getName()));
            throw new InvalidMemberMetaDataException("044106", getClassName(), getName());
        }

        // No class loader, so use System
        if (clr == null)
        {
            NucleusLogger.METADATA.warn(Localiser.msg("044067",name,getClassName(true)));
            clr = mmgr.getNucleusContext().getClassLoaderResolver(null);
        }

        memberRepresented = field != null ? field : method;
        if (type == null)
        {
            // Type not yet set so set from field/method (will only be set if we are imposing the type due to Java generics TypeVariable usage)
            if (field != null)
            {
                this.type = field.getType();
            }
            else if (method != null)
            {
                this.type = method.getReturnType();
            }
        }

        if (className != null)
        {
            // Property is overriding a superclass property, so check that it is valid
            Class thisClass = null;
            if (parent instanceof EmbeddedMetaData)
            {
                // <embedded> is contained in a <field>, <element>, <key>, <value>
                // but could be multiple levels deep so adopt a generic strategy for finding the parent class
                MetaData superMd = parent.getParent();
                thisClass = ((AbstractMemberMetaData)superMd).getType();
            }
            else
            {
                // Overriding field in a superclass of this class
                try
                {
                    thisClass = clr.classForName(getAbstractClassMetaData().getPackageName() + "." + getAbstractClassMetaData().getName());
                }
                catch (ClassNotResolvedException cnre)
                {
                    // Do nothing
                }
            }

            Class fieldClass = null;
            try
            {
                fieldClass = clr.classForName(className);
            }
            catch (ClassNotResolvedException cnre)
            {
                try
                {
                    fieldClass = clr.classForName(getAbstractClassMetaData().getPackageName() + "." + className);
                    className = getAbstractClassMetaData().getPackageName() + "." + className;
                }
                catch (ClassNotResolvedException cnre2)
                {
                    NucleusLogger.METADATA.error(Localiser.msg("044113", getClassName(), getName(), className));
                    NucleusException ne = new InvalidMemberMetaDataException("044113", getClassName(), getName(), className);
                    ne.setNestedException(cnre);
                    throw ne;
                }
            }
            if (fieldClass != null && !fieldClass.isAssignableFrom(thisClass))
            {
                // TODO We could also check if persistable, but won't work when enhancing
                NucleusLogger.METADATA.error(Localiser.msg("044114", getClassName(), getName(), className));
                throw new InvalidMemberMetaDataException("044114", getClassName(), getName(), className);
            }
        }

        if (primaryKey == null)
        {
            // Primary key not set by user so initialise it to false
            primaryKey = Boolean.FALSE;
        }

        // Update "embedded" based on type
        if (primaryKey == Boolean.FALSE && embedded == null)
        {
            Class element_type=getType();
            if (element_type.isArray())
            {
                element_type = element_type.getComponentType();
                if (mmgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(element_type))
                {
                    embedded = Boolean.TRUE;
                }
            }
            else if (mmgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(element_type))
            {
                embedded = Boolean.TRUE;
            }
        }
        if (embedded == null)
        {
            embedded = Boolean.FALSE;
        }

        // Update "persistence-modifier" according to type etc
        if (FieldPersistenceModifier.DEFAULT.equals(persistenceModifier))
        {
            boolean isPcClass = getType().isArray() ? isFieldArrayTypePersistable(mmgr) : mmgr.isFieldTypePersistable(type);
            if (!isPcClass)
            {
                if (getType().isArray() && getType().getComponentType().isInterface())
                {
                    isPcClass = mmgr.getMetaDataForClassInternal(getType().getComponentType(), clr) != null;
                }
                else if (getType().isInterface())
                {
                    isPcClass = mmgr.getMetaDataForClassInternal(getType(), clr) != null;
                }
            }

            persistenceModifier = getDefaultFieldPersistenceModifier(getType(), memberRepresented.getModifiers(), isPcClass, mmgr);
        }
        // TODO If this field is NONE in superclass, make it NONE here too

        // If type is a container, load create the metadata. The field will be handled as a container if it
        // has a ContainerHandler registered against it.
        TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();
        ContainerHandler containerHandler = typeMgr.getContainerHandler(type);

        if (containerHandler == null)
        {
            // No container handler registered for this type
            if (hasContainer())
            {
                // Container metadata specified on a type that is not a valid or supported container
                NucleusLogger.METADATA.error(Localiser.msg("044212", getClassName(), getName(), type));
                NucleusException ne = new InvalidMemberMetaDataException("044212", getClassName(), getName(), type);
                throw ne;
            }
        }
        else
        {
            // Field is a container type
            if (!hasContainer())
            {
                // No container metadata has not been specified yet, create a default empty one
                setContainer(containerHandler.newMetaData());
            }

            containerHandler.populateMetaData(clr, primary, mmgr, this);
        }

        // Update "default-fetch-group" according to type
        if (defaultFetchGroup == null && persistenceModifier.equals(FieldPersistenceModifier.NONE))
        {
            defaultFetchGroup = Boolean.FALSE;
        }
        else if (defaultFetchGroup == null && persistenceModifier.equals(FieldPersistenceModifier.TRANSACTIONAL))
        {
            defaultFetchGroup = Boolean.FALSE;
        }
        else if (defaultFetchGroup == null)
        {
            defaultFetchGroup = Boolean.FALSE;
            if (!primaryKey.equals(Boolean.TRUE))
            {
                if (hasContainer())
                {
                    defaultFetchGroup = containerHandler.isDefaultFetchGroup(clr, mmgr, this);
                }
                else if (typeMgr.isDefaultFetchGroup(getType()))
                {
                    // If still not determined rely on the type
                    defaultFetchGroup = Boolean.TRUE;
                }
            }
        }

        // Field is not specified as "persistent" yet has DFG or primary-key !
        if (persistenceModifier.equals(FieldPersistenceModifier.TRANSACTIONAL) ||
            persistenceModifier.equals(FieldPersistenceModifier.NONE))
        {
            if (defaultFetchGroup == Boolean.TRUE || primaryKey == Boolean.TRUE)
            {
                throw new InvalidMemberMetaDataException("044109", getClassName(), name, this.getType().getName(), persistenceModifier.toString());
            }
        }

        if (storeInLob)
        {
            // Set up the jdbcType/serialized settings according to the field type in line with JPA
            boolean useClob = false;
            if (type == String.class ||
                (type.isArray() && type.getComponentType() == Character.class) || (type.isArray() && type.getComponentType() == char.class))
            {
                useClob = true;
                if (columns == null || columns.isEmpty())
                {
                    // Create a CLOB column. What if the RDBMS doesn't support CLOB ?
                    ColumnMetaData colmd = new ColumnMetaData();
                    colmd.setName(column);
                    colmd.setJdbcType("CLOB");
                    addColumn(colmd);
                }
                else
                {
                    ColumnMetaData colmd = columns.get(0);
                    colmd.setJdbcType("CLOB");
                }
            }
            if (!useClob)
            {
                serialized = Boolean.TRUE;
            }
        }
        
        if (!mmgr.isDefaultNullable() && !hasContainer())
        {
            // Find column metadata definition, creating one if not specified
            ColumnMetaData colMmd;

            if (columns == null || columns.isEmpty())
            {
                newColumnMetaData();
            }
            colMmd = getColumnMetaData()[0];

            // Set column not-null by default
            if (colMmd.getAllowsNull() == null)
            {
                colMmd.setAllowsNull(Boolean.FALSE);
            }
        }
        
        if (this.containerMetaData != null && this.dependent != null)
        {
            // Check for invalid dependent field specifications
            NucleusLogger.METADATA.error(Localiser.msg("044110", getClassName(), getName(), ((ClassMetaData)this.parent).getName()));            
            throw new InvalidMemberMetaDataException("044110", getClassName(), getName(), ((ClassMetaData)this.parent).getName());
        }

        if (embedded == Boolean.TRUE && embeddedMetaData == null)
        {
            // User specified "embedded" on the member, yet no embedded definition so add one
            AbstractClassMetaData memberCmd = mmgr.getMetaDataForClassInternal(getType(), clr);
            if (memberCmd != null)
            {
                embeddedMetaData = new EmbeddedMetaData();
                embeddedMetaData.setParent(this);
            }
        }
        
        if (embeddedMetaData != null)
        {
            // Update with any extensions (for lack of features in JPA)
            if (hasExtension("null-indicator-column"))
            {
                embeddedMetaData.setNullIndicatorColumn(getValueForExtension("null-indicator-column"));
                if (hasExtension("null-indicator-value"))
                {
                    embeddedMetaData.setNullIndicatorValue(getValueForExtension("null-indicator-value"));
                }
            }

            // Populate any embedded object
            embeddedMetaData.populate(clr, primary, mmgr);
            embedded = Boolean.TRUE;
        }

        if (containerMetaData != null && persistenceModifier == FieldPersistenceModifier.PERSISTENT)
        {
            // Populate any container
            if (containerMetaData instanceof CollectionMetaData)
            {
//                if (cascadeDelete)
//                {
//                    // User has set cascade-delete (JPA) so set the element as dependent
//                    getCollection().element.dependent = Boolean.TRUE;
//                }
//                getCollection().populate(clr, primary, mmgr);
            }
            else if (containerMetaData instanceof MapMetaData)
            {
//                String keyCascadeVal = getValueForExtension("cascade-delete-key");
//                if (cascadeDelete)
//                {
//                    // User has set cascade-delete (JPA) so set the value as dependent
//                    getMap().key.dependent = Boolean.FALSE; // JPA spec doesn't define what this should be
//                    getMap().value.dependent = Boolean.TRUE;
//                }
//                if (keyCascadeVal != null)
//                {
//                    if (keyCascadeVal.equalsIgnoreCase("true"))
//                    {
//                        getMap().key.dependent = Boolean.TRUE;
//                    }
//                    else
//                    {
//                        getMap().key.dependent = Boolean.FALSE;
//                    }
//                }
//                getMap().populate(clr, primary, mmgr);
            }
            else if (containerMetaData instanceof ArrayMetaData)
            {
//                if (cascadeDelete)
//                {
//                    // User has set cascade-delete (JPA) so set the element as dependent
//                    getArray().element.dependent = Boolean.TRUE;
//                }
//                getArray().populate(clr, primary, mmgr);
            }
        }

        if (mmgr.isFieldTypePersistable(type) && cascadeDelete)
        {
            setDependent(true);
        }

        if (hasExtension("implementation-classes"))
        {
            // Check the validity of the implementation-classes and qualify them where required.
            StringBuilder str = new StringBuilder();
            String[] implTypes = getValuesForExtension("implementation-classes");
            for (int i=0;i<implTypes.length;i++)
            {
                String implTypeName = ClassUtils.createFullClassName(getAbstractClassMetaData().getPackageName(), implTypes[i]);
                if (i > 0)
                {
                    str.append(",");
                }

                try
                {
                    clr.classForName(implTypeName);
                    str.append(implTypeName);
                }
                catch (ClassNotResolvedException cnre)
                {
                    try
                    {
                        // Maybe the user specified a java.lang class without fully-qualifying it
                        // This is beyond the scope of the JDO spec which expects java.lang cases to be fully-qualified
                        String langClassName = ClassUtils.getJavaLangClassForType(implTypeName);
                        clr.classForName(langClassName);
                        str.append(langClassName);
                    }
                    catch (ClassNotResolvedException cnre2)
                    {
                        // Implementation type not found
                        throw new InvalidMemberMetaDataException("044116", getClassName(), getName(), implTypes[i]);
                    }
                }
            }
            addExtension(VENDOR_NAME, "implementation-classes", str.toString()); // Replace with this new value
        }

        // Set up persistence flags for enhancement process
        byte serializable = 0;
        if (Serializable.class.isAssignableFrom(getType()) || getType().isPrimitive())
        {
            serializable = Persistable.SERIALIZABLE;
        }

        if (FieldPersistenceModifier.NONE.equals(persistenceModifier))
        {
            persistenceFlags = 0;
        }
        else if (FieldPersistenceModifier.TRANSACTIONAL.equals(persistenceModifier) &&
                 Modifier.isTransient(memberRepresented.getModifiers()))
        {
            persistenceFlags = (byte) (Persistable.CHECK_WRITE | serializable);
        }
        else if (primaryKey.booleanValue())
        {
            persistenceFlags = (byte) (Persistable.MEDIATE_WRITE | serializable);
        }
        else if (defaultFetchGroup.booleanValue())
        {
            persistenceFlags = (byte) (Persistable.CHECK_READ | Persistable.CHECK_WRITE | serializable);
        }
        else if (!defaultFetchGroup.booleanValue())
        {
            persistenceFlags = (byte) (Persistable.MEDIATE_READ | Persistable.MEDIATE_WRITE | serializable);
        }
        else
        {
            persistenceFlags = 0;
        }

        // Set fields that are not relations
        if (persistenceModifier != FieldPersistenceModifier.PERSISTENT)
        {
            // Not a relation field so set relation information
            relationType = RelationType.NONE;
        }
        else if (containerMetaData == null && !mmgr.isFieldTypePersistable(type))
        {
            if (!type.getName().equals(ClassNameConstants.Object) && !type.isInterface())
            {
                // Not a container field, not a persistable type, nor a reference type so not a relation field
                relationType = RelationType.NONE;
            }
        }

        if (serialized == Boolean.TRUE && hasExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME))
        {
            NucleusLogger.METADATA.warn(Localiser.msg("044127", getClassName(), getName(), getValueForExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME)));
            serialized = Boolean.FALSE;
        }

        setPopulated();
    }

    public String getPackageName()
    {
        return className.substring(0, className.lastIndexOf('.'));
    }

    /**
     * Accessor for the default "persistence-modifier" for a field given the
     * class, its modifier and whether it is a PersistentCapable class.
     * @param c The class
     * @param modifier The modifiers for the field
     * @param isPCclass Whether it is persistence capable.
     * @param mmgr MetaData manager
     * @return The default field PersistenceModifier.
     */
    public final FieldPersistenceModifier getDefaultFieldPersistenceModifier(Class c, int modifier, boolean isPCclass, MetaDataManager mmgr)
    {
        // Set modifier for static, final, transient as per capabilities we currently handle
        if (!PERSIST_FINAL && Modifier.isFinal(modifier) && this instanceof FieldMetaData)
        {
            return FieldPersistenceModifier.NONE;
        }
        if (!PERSIST_STATIC && Modifier.isStatic(modifier))
        {
            return FieldPersistenceModifier.NONE;
        }
        if (!PERSIST_TRANSIENT && Modifier.isTransient(modifier))
        {
            return FieldPersistenceModifier.NONE;
        }

        if (isPCclass)
        {
            // All PC class fields are persistent by default
            return FieldPersistenceModifier.PERSISTENT;
        }

        if (c == null)
        {
            throw new NucleusException("class is null");
        }
        if (c.isArray() && mmgr.getNucleusContext().getApiAdapter().isPersistable(c.getComponentType()))
        {
            // Arrays of PC types are, by default, persistent
            return FieldPersistenceModifier.PERSISTENT;
        }

        if (mmgr.getNucleusContext().getTypeManager().isDefaultPersistent(c))
        {
            return FieldPersistenceModifier.PERSISTENT;
        }
        return FieldPersistenceModifier.NONE;
    }

    /**
     * Initialisation method.
     * This should be called AFTER using the populate method if you are going to use populate.
     * It creates the internal convenience arrays etc needed for normal operation.
     */
    public synchronized void initialise(ClassLoaderResolver clr)
    {
        if (persistenceModifier == FieldPersistenceModifier.NONE)
        {
            setInitialised();
            return;
        }

        MetaDataManager mmgr = getMetaDataManager();

        // Cater for user specifying column name, or columns
        if (columns.isEmpty() && column != null)
        {
            columnMetaData = new ColumnMetaData[1];
            columnMetaData[0] = new ColumnMetaData();
            columnMetaData[0].setName(column);
            columnMetaData[0].parent = this;
            columnMetaData[0].initialise(clr);
        }
        else if (columns.size() == 1 && column != null)
        {
            // Cater for user specifying <column> and <field column="...">
            columnMetaData = new ColumnMetaData[1];
            columnMetaData[0] = columns.get(0);
            if (columnMetaData[0].getName() == null)
            {
                columnMetaData[0].setName(column);
            }
            columnMetaData[0].initialise(clr);
        }
        else
        {
            columnMetaData = new ColumnMetaData[columns.size()];
            for (int i=0; i<columnMetaData.length; i++)
            {
                columnMetaData[i] = columns.get(i);
                columnMetaData[i].initialise(clr);
            }
        }
        // Initialise all sub-objects
        if (containerMetaData != null)
        {
            containerMetaData.initialise(clr);
            if (containerMetaData instanceof CollectionMetaData)
            {
                CollectionMetaData collmd = (CollectionMetaData)containerMetaData;
                if (collmd.element.classMetaData != null && collmd.element.classMetaData.isEmbeddedOnly())
                {
                    // Element is persistent yet embedded only so mark as embedded in metadata
                    if (elementMetaData == null)
                    {
                        elementMetaData = new ElementMetaData();
                        elementMetaData.parent = this;
                        elementMetaData.populate(clr, null, mmgr);
                    }
                    if (elementMetaData.getEmbeddedMetaData() == null)
                    {
                        EmbeddedMetaData elemEmbmd = new EmbeddedMetaData();
                        elemEmbmd.parent = elementMetaData;
                        elemEmbmd.populate(clr, null, mmgr);
                        elementMetaData.setEmbeddedMetaData(elemEmbmd);
                        collmd.element.embedded = Boolean.TRUE;
                    }
                }
            }
            else if (containerMetaData instanceof MapMetaData)
            {
                MapMetaData mapmd = (MapMetaData)containerMetaData;
                if (mapmd.key.classMetaData != null && mapmd.key.classMetaData.isEmbeddedOnly())
                {
                    // Key is persistent yet embedded only so mark as embedded in metadata
                    if (keyMetaData == null)
                    {
                        keyMetaData = new KeyMetaData();
                        keyMetaData.parent = this;
                        keyMetaData.populate(clr, null, mmgr);
                    }
                    if (keyMetaData.getEmbeddedMetaData() == null)
                    {
                        EmbeddedMetaData keyEmbmd = new EmbeddedMetaData();
                        keyEmbmd.parent = keyMetaData;
                        keyEmbmd.populate(clr, null, mmgr);
                        keyMetaData.setEmbeddedMetaData(keyEmbmd);
                        mapmd.key.embedded = Boolean.TRUE;
                    }
                }
                if (mapmd.value.classMetaData != null && mapmd.value.classMetaData.isEmbeddedOnly())
                {
                    // Value is persistent yet embedded only so mark as embedded in metadata
                    if (valueMetaData == null)
                    {
                        valueMetaData = new ValueMetaData();
                        valueMetaData.parent = this;
                        valueMetaData.populate(clr, null, mmgr);
                    }
                    if (valueMetaData.getEmbeddedMetaData() == null)
                    {
                        EmbeddedMetaData valueEmbmd = new EmbeddedMetaData();
                        valueEmbmd.parent = valueMetaData;
                        valueEmbmd.populate(clr, null, mmgr);
                        valueMetaData.setEmbeddedMetaData(valueEmbmd);
                        mapmd.value.embedded = Boolean.TRUE;
                    }
                }
            }
        }

        if (embeddedMetaData != null)
        {
            embeddedMetaData.initialise(clr);
        }
        if (joinMetaData != null)
        {
            joinMetaData.initialise(clr);
        }
        if (elementMetaData != null)
        {
            elementMetaData.initialise(clr);
        }
        if (keyMetaData != null)
        {
            keyMetaData.initialise(clr);
        }
        if (valueMetaData != null)
        {
            valueMetaData.initialise(clr);
        }

        // Interpret the "indexed" value to create our IndexMetaData where it wasn't specified that way
        if (indexMetaData == null && columnMetaData != null && indexed != null && indexed != IndexedValue.FALSE)
        {
            indexMetaData = new IndexMetaData();
            indexMetaData.setUnique(indexed == IndexedValue.UNIQUE);
            for (int i=0;i<columnMetaData.length;i++)
            {
                indexMetaData.addColumn(columnMetaData[i].getName());
            }
        }
        else if (indexed == IndexedValue.TRUE && indexMetaData != null)
        {
            // Can't be not indexed and have index metadata, so just obey overriding flag
            indexMetaData = null;
        }
        if (uniqueMetaData == null && uniqueConstraint)
        {
            uniqueMetaData = new UniqueMetaData();
            uniqueMetaData.setTable(column);
            for (int i=0;i<columnMetaData.length;i++)
            {
                uniqueMetaData.addColumn(columnMetaData[i].getName());
            }
        }

        if (orderMetaData != null)
        {
            orderMetaData.initialise(clr);
        }

        if (hasExtension(MetaData.EXTENSION_MEMBER_CASCADE_PERSIST))
        {
            // JDO doesn't have a metadata attribute for this so we use an extension
            String cascadeValue = getValueForExtension(MetaData.EXTENSION_MEMBER_CASCADE_PERSIST);
            if (cascadeValue.equalsIgnoreCase("true"))
            {
                cascadePersist = true;
            }
            else if (cascadeValue.equalsIgnoreCase("false"))
            {
                cascadePersist = false;
            }
        }
        if (hasExtension(MetaData.EXTENSION_MEMBER_CASCADE_UPDATE))
        {
            // JDO doesn't have a metadata attribute for this so we use an extension
            String cascadeValue = getValueForExtension(MetaData.EXTENSION_MEMBER_CASCADE_UPDATE);
            if (cascadeValue.equalsIgnoreCase("true"))
            {
                cascadeUpdate = true;
            }
            else if (cascadeValue.equalsIgnoreCase("false"))
            {
                cascadeUpdate = false;
            }
        }
        if (hasExtension(MetaData.EXTENSION_MEMBER_CASCADE_REFRESH))
        {
            // JDO doesn't have a metadata attribute for this so we use an extension
            String cascadeValue = getValueForExtension(MetaData.EXTENSION_MEMBER_CASCADE_REFRESH);
            if (cascadeValue.equalsIgnoreCase("true"))
            {
                cascadeRefresh = true;
            }
            else if (cascadeValue.equalsIgnoreCase("false"))
            {
                cascadeRefresh = false;
            }
        }

        setInitialised();
    }

    /**
     * Utility to return if this array field has elements that are Persistable.
     * Not valid for use by the enhancer. Must be overridden for that mode.
     * If the field is not an array will return false.
     * @param mmgr MetaData manager
     * @return Whether the field type is persistable.
     * TODO Move this out into a org.datanucleus.store.types class
     */
    public boolean isFieldArrayTypePersistable(MetaDataManager mmgr)
    {
        if (!type.isArray())
        {
            return false;
        }

        if (mmgr.isEnhancing())
        {
            // Enhancing so return if we have MetaData that is persistable
            AbstractClassMetaData cmd = mmgr.readMetaDataForClass(type.getComponentType().getName());
            if (cmd != null && cmd instanceof ClassMetaData && cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                return true;
            }
        }
        return mmgr.getNucleusContext().getApiAdapter().isPersistable(type.getComponentType());
    }

    /**
     * Convenience method to return if this field/property is static.
     * When the object is not "populated" always returns false; 
     * @return Whether the field/property is static
     */
    public boolean isStatic()
    {
        if (!isPopulated() || memberRepresented == null)
        {
            return false;
        }
        return Modifier.isStatic(memberRepresented.getModifiers());
    }

    /**
     * Convenience method to return if this field/property is final.
     * When the object is not "populated" always returns false; 
     * @return Whether the field is field/property
     */
    public boolean isFinal()
    {
        if (!isPopulated() || memberRepresented == null)
        {
            return false;
        }
        return Modifier.isFinal(memberRepresented.getModifiers());
    }

    /**
     * Convenience method to return if this field/property is transient.
     * When the object is not "populated" always returns false; 
     * @return Whether the field/property is transient
     */
    public boolean isTransient()
    {
        if (!isPopulated() || memberRepresented == null)
        {
            return false;
        }
        return Modifier.isTransient(memberRepresented.getModifiers());
    }

    /**
     * Convenience method to return if this field/property is public.
     * When the object is not "populated" always returns false; 
     * @return Whether the field/property is public
     */
    public boolean isPublic()
    {
        if (!isPopulated() || memberRepresented == null)
        {
            return false;
        }
        return Modifier.isPublic(memberRepresented.getModifiers());
    }

    /**
     * Convenience method to return if this field/property is protected.
     * When the object is not "populated" always returns false; 
     * @return Whether the field/property is protected
     */
    public boolean isProtected()
    {
        if (!isPopulated() || memberRepresented == null)
        {
            return false;
        }
        return Modifier.isProtected(memberRepresented.getModifiers());
    }

    /**
     * Convenience method to return if this field/property is private.
     * When the object is not "populated" always returns false; 
     * @return Whether the field/property is private
     */
    public boolean isPrivate()
    {
        if (!isPopulated() || memberRepresented == null)
        {
            return false;
        }
        return Modifier.isPrivate(memberRepresented.getModifiers());
    }

    /**
     * Convenience method to return if this field represents an abstract property.
     * @return Whether the property is abstract
     */
    public boolean isAbstract()
    {
        if (!isPopulated() || memberRepresented == null)
        {
            return false;
        }
        return Modifier.isAbstract(memberRepresented.getModifiers());
    }

    public IdentityStrategy getValueStrategy()
    {
        return valueStrategy;
    }

    public void setValueStrategy(IdentityStrategy valueStrategy)
    {
        this.valueStrategy = valueStrategy;
    }

    public void setValueStrategy(String strategy)
    {
        this.valueStrategy = strategy == null ? null : IdentityStrategy.getIdentityStrategy(strategy);
    }

    /**
     * Name of a (user-provided) value generator to override the default generator for this strategy.
     * @return Name of user provided value generator
     */
    public String getValueGeneratorName()
    {
        return valueGeneratorName;
    }

    /**
     * If the value-strategy is sequence, the sequence attribute specifies the
     * name of the sequence to use to automatically generate a value for the field.
     * @return the sequence name 
     */
    public String getSequence()
    {
        return sequence;
    }

    /**
     * If the value-strategy is sequence, the sequence attribute specifies the
     * name of the sequence to use to automatically generate a value for the field.
     * @param sequence the sequence name 
     */
    public void setSequence(String sequence)
    {
        this.sequence = StringUtils.isWhitespace(sequence) ? null : sequence;
    }

    public boolean isCacheable()
    {
        if (hasExtension(MetaData.EXTENSION_MEMBER_CACHEABLE))
        {
            // JPA doesn't have way of specifying field cacheability so use extension
            return getValueForExtension(MetaData.EXTENSION_MEMBER_CACHEABLE).equalsIgnoreCase("false") ? false : true;
        }
        return cacheable;
    }

    public void setCacheable(boolean cache)
    {
        cacheable = cache;
    }

    // TODO Make use of this in DN core
    public String getLoadFetchGroup()
    {
        return loadFetchGroup;
    }

    public void setLoadFetchGroup(String loadFetchGroup)
    {
        this.loadFetchGroup = loadFetchGroup;
    }

    public String getTypeConverterName()
    {
        if (hasExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME))
        {
            return getValueForExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME);
        }
        return null;
    }

    public void setTypeConverterName(String name)
    {
        // Store it as an extension
        addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, name);
    }

    public boolean isTypeConversionDisabled()
    {
        return hasExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_DISABLED);
    }

    public void setTypeConverterDisabled()
    {
        addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_DISABLED, "true");
    }

    public int getRecursionDepth()
    {
        return recursionDepth;
    }

    public void setRecursionDepth(int depth)
    {
        this.recursionDepth = depth;
    }

    public void setRecursionDepth(String depth)
    {
        if (!StringUtils.isWhitespace(depth))
        {
            try
            {
                this.recursionDepth = Integer.parseInt(depth);
            }
            catch (NumberFormatException nfe) 
            {
            }
        }
    }

    /**
     * Extension accessor where the user has requested that the fetch of this field fetch just the FK.
     * Only applies to a 1-1 UNI, or 1-1 BI with FK at this side.
     * @return Whether to fetch just the FK (and not join to get the fields of the other side)
     */
    public boolean fetchFKOnly()
    {
        if (hasExtension(MetaData.EXTENSION_MEMBER_FETCH_FK_ONLY))
        {
            return Boolean.valueOf(getValueForExtension(MetaData.EXTENSION_MEMBER_FETCH_FK_ONLY));
        }
        return false;
    }

    /**
     * Convenience method to navigate back through the parents to find the overall
     * ClassMetaData handling this object. This is to cater specifically for nested
     * embedded fields where you can nest object several levels deep.
     * @param metadata The metadata to check
     * @return The overall class metadata for this element
     */
    protected static MetaData getOverallParentClassMetaData(MetaData metadata)
    {
        if (metadata == null)
        {
            return null;
        }
        else if (metadata instanceof AbstractClassMetaData)
        {
            return metadata;
        }
        else
        {
            return getOverallParentClassMetaData(metadata.getParent());
        }
    }

    /**
     * Convenience accessor for the MetaData of the parent class.
     * @return Returns the MetaData of the parent class.
     */
    public AbstractClassMetaData getAbstractClassMetaData()
    {
        // TODO Consider replacing this method with the getOverallParentClassMetaData above since its generalised
        if (parent == null)
        {
            return null;
        }
        else if (parent instanceof AbstractClassMetaData)
        {
            return (AbstractClassMetaData)parent;
        }
        else if (parent instanceof EmbeddedMetaData)
        {
            // <embedded> is contained in a <field>, <element>, <key>, <value>
            // but could be multiple levels deep so adopt a generic strategy for finding the parent class
            return (AbstractClassMetaData)getOverallParentClassMetaData(parent.getParent().getParent());
        }
        return null;
    }

    /**
     * Accessor for orderMetaData
     * @return Returns the orderMetaData.
     */
    public final OrderMetaData getOrderMetaData()
    {
        return orderMetaData;
    }
    
    /**
     * Accessor for the field name 
     * @return field name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Accessor for the full field name. This prepends the class name. 
     * @return full field name.
     */
    public String getFullFieldName()
    {
        if (fullFieldName == null)
        {
            if (className != null)
            {
                fullFieldName = className + "." + name;
            }
            else 
            {
                fullFieldName = getClassName(true) + "." + name;
            }
        }
        return fullFieldName;
    }

    /**
     * Accessor for whether the field is for a superclass, and not for this class.
     * @return Whether the field belongs to a superclass
     */
    public boolean fieldBelongsToClass()
    {
        return className == null;
    }

    /**
     * Accessor for the fully-qualified class name owning this field.
     * @return The class name
     */
    public String getClassName()
    {
        return getClassName(true);
    }

    /**
     * Convenience method so that ClassMetaData can update the name of the superclass
     * to which this field belongs.
     * @param className Name of the class
     */
    void setClassName(String className)
    {
        this.className = className;
    }

    /**
     * Convenience to return the class name that this a field of.
     * @param fully_qualified Whether the name should be fully qualified.
     * @return Class name
     */
    public String getClassName(boolean fully_qualified)
    {
        if (className != null)
        {
            return className;
        }

        if (parent == null)
        {
            return null;
        }
        else if (parent instanceof AbstractClassMetaData)
        {
            AbstractClassMetaData cmd = (AbstractClassMetaData)parent;
            if (fully_qualified)
            {
                return cmd.getFullClassName();
            }
            return cmd.getName();
        }
        else if (parent instanceof EmbeddedMetaData)
        {
            // <embedded> is contained in a <field>, <element>, <key>, <value>
            MetaData parentMd = ((EmbeddedMetaData)parent).getParent();
            String typeName = null;
            if (parentMd instanceof AbstractMemberMetaData)
            {
                typeName = ((AbstractMemberMetaData)parentMd).getTypeName();
            }
            else if (parentMd instanceof ElementMetaData)
            {
                AbstractMemberMetaData fmd = (AbstractMemberMetaData)((ElementMetaData)parentMd).getParent();
                typeName = fmd.getCollection().getElementType();
            }
            else if (parentMd instanceof KeyMetaData)
            {
                AbstractMemberMetaData fmd = (AbstractMemberMetaData)((KeyMetaData)parentMd).getParent();
                typeName = fmd.getMap().getKeyType();
            }
            else if (parentMd instanceof ValueMetaData)
            {
                AbstractMemberMetaData fmd = (AbstractMemberMetaData)((ValueMetaData)parentMd).getParent();
                typeName = fmd.getMap().getValueType();
            }
            else
            {
                // Should be impossible
                return null;
            }
            if (!fully_qualified && typeName.indexOf('.') > 0)
            {
                return typeName.substring(typeName.lastIndexOf('.')+1);
            }
            return typeName;
        }
        else if (parent instanceof UniqueMetaData)
        {
            MetaData grandparent = ((UniqueMetaData)parent).getParent();
            if (grandparent instanceof AbstractClassMetaData)
            {
                String fullClassName = ((AbstractClassMetaData)grandparent).getFullClassName();
                if (!fully_qualified && fullClassName.indexOf('.') > 0)
                {
                    return fullClassName.substring(fullClassName.lastIndexOf('.')+1);
                }
                return fullClassName;
            }
            // TODO Cater for other parent options
        }
        return null;
    }

    public FieldPersistenceModifier getPersistenceModifier()
    {
        return persistenceModifier;
    }

    public void setPersistenceModifier(FieldPersistenceModifier modifier)
    {
    	this.persistenceModifier = modifier;
    }

    public void setNotPersistent()
    {
        persistenceModifier = FieldPersistenceModifier.NONE;
    }

    public void setTransactional()
    {
        persistenceModifier = FieldPersistenceModifier.TRANSACTIONAL;
    }

    public boolean isDefaultFetchGroup()
    {
        if (defaultFetchGroup == null)
        {
            return false;
        }
        return defaultFetchGroup.booleanValue();
    }

    public void setDefaultFetchGroup(boolean dfg)
    {
         this.defaultFetchGroup = Boolean.valueOf(dfg);
    }

    public boolean isDependent()
    {
        if (dependent == null)
        {
            return false;
        }
        return dependent.booleanValue();
    }

    public void setDependent(boolean dependent)
    {
        this.dependent = Boolean.valueOf(dependent);
    }

    public boolean isEmbedded()
    {
        return embedded == null ? false : embedded.booleanValue();
    }

    public void setEmbedded(boolean val)
    {
        this.embedded = val;
    }

    public boolean isSerialized()
    {
        return serialized == null ? false : serialized.booleanValue();
    }

    public void setSerialised(boolean flag)
    {
        serialized = flag;
    }

    public String getMapsIdAttribute()
    {
        return mapsIdAttribute;
    }

    /**
     * Accessor for the whether this field should be cascaded at persist
     * @return Whether to cascade at persist
     */
    public boolean isCascadePersist()
    {
        return cascadePersist;
    }

    /**
     * Accessor for the whether this field should be cascaded at update
     * @return Whether to cascade at update
     */
    public boolean isCascadeUpdate()
    {
        return cascadeUpdate;
    }

    /**
     * Accessor for the whether this field should be cascaded at delete
     * @return Whether to cascade at delete
     */
    public boolean isCascadeDelete()
    {
        // TODO Never called?
        return cascadeDelete;
    }

    /**
     * Accessor for the whether this field should be cascaded at detach (JPA only).
     * @return Whether to cascade at detach
     */
    public boolean isCascadeDetach()
    {
        return cascadeDetach;
    }

    /**
     * Accessor for the whether this field should be cascaded at refresh
     * @return Whether to cascade at refresh
     */
    public boolean isCascadeRefresh()
    {
        return cascadeRefresh;
    }

    /**
     * Accessor for the whether this field should remove orphans at delete.
     * @return Whether to reove orphans at delete
     */
    public boolean isCascadeRemoveOrphans()
    {
        return cascadeRemoveOrphans;
    }

    public boolean isPrimaryKey()
    {
        return primaryKey == null ? false : primaryKey.booleanValue();
    }

    public AbstractMemberMetaData setPrimaryKey(boolean flag)
    {
        primaryKey = flag;
        if (primaryKey)
        {
            this.defaultFetchGroup = Boolean.TRUE;
        }
        return this;
    }

    public AbstractMemberMetaData setColumn(String col)
    {
        this.column = StringUtils.isWhitespace(col) ? null : col;
        return this;
    }

    public String getTable()
    {
        return table;
    }

    public AbstractMemberMetaData setTable(String table)
    {
        this.table = StringUtils.isWhitespace(table) ? null : table;
        return this;
    }

    public String getCatalog()
    {
        return catalog;
    }

    public AbstractMemberMetaData setCatalog(String catalog)
    {
        this.catalog = StringUtils.isWhitespace(catalog) ? null : catalog;
        return this;
    }

    public String getSchema()
    {
        return schema;
    }

    public AbstractMemberMetaData setSchema(String schema)
    {
        this.schema = StringUtils.isWhitespace(schema) ? null : schema;
        return this;
    }

    public boolean isUnique()
    {
        return uniqueConstraint;
    }

    public AbstractMemberMetaData setUnique(String unique)
    {
        if (!StringUtils.isWhitespace(unique))
        {
            uniqueConstraint = Boolean.parseBoolean(unique);
        }
        return this;
    }

    public AbstractMemberMetaData setUnique(boolean unique)
    {
        uniqueConstraint = unique;
        return this;
    }

    public IndexedValue getIndexed()
    {
        return indexed;
    }

    public AbstractMemberMetaData setIndexed(IndexedValue val)
    {
        this.indexed = val;
        return this;
    }

    public NullValue getNullValue()
    {
        return nullValue;
    }

    public AbstractMemberMetaData setNullValue(NullValue val)
    {
        this.nullValue = val;
        return this;
    }

    /**
     * Accessor for the field id.
     * Not set when the field is an overriding field.
     * @return field id
     */
    public int getFieldId()
    {
        return fieldId;
    }

    /**
     * Accessor for the implementation type(s) that can be stored in this field when it is a reference type.
     * @return Returns the implementation type(s) for the field.
     */
    public final String[] getFieldTypes()
    {
        return getValuesForExtension("implementation-classes");
//        return fieldTypes;
    }

    /**
     * Mutator for the possible field type(s) that this reference field can store.
     * @param types The types (comma-separated)
     */
    public void setFieldTypes(String types)
    {
        if (!StringUtils.isWhitespace(types))
        {
            addExtension("implementation-classes", types);
        }
    }

    /**
     * Accessor for the field id
     * @return field id
     */
    public int getAbsoluteFieldNumber()
    {
        if (className == null)
        {
            // Normal field, parented by its true class
            return fieldId + getAbstractClassMetaData().getNoOfInheritedManagedMembers();
        }

        // Overriding field, parented by a foster class
        return getAbstractClassMetaData().getAbsolutePositionOfMember(name);
    }

    /**
     * Accessor for the member being represented.
     * @return The member
     */
    public Member getMemberRepresented()
    {
        return memberRepresented;
    }

    /**
     * Accessor for the field type
     * @return Reflection field type
     */
    public Class getType()
    {
        return type;
    }

    /**
     * Accessor for the field type name
     * @return Reflection field type name
     */
    public String getTypeName()
    {
        if (type == null)
        {
            return null;
        }
        return type.getName();
    }

    /**
     * Accessor for the container for this field.
     * @return The MetaData of the container for this field.
     **/
    public ContainerMetaData getContainer()
    {
        return containerMetaData;
    }

    /**
     * Accessor for an array container for this field. Returns null if no array
     * attached.
     * @return The MetaData of the container for this field if an array
     **/
    public ArrayMetaData getArray()
    {
        if (containerMetaData != null && containerMetaData instanceof ArrayMetaData)
        {
            return (ArrayMetaData)containerMetaData;
        }
        return null;
    }

    /**
     * Accessor for a collection container for this field. Returns null if no
     * collection attached.
     * @return The MetaData of the container for this field if a Collection.
     **/
    public CollectionMetaData getCollection()
    {
        if (containerMetaData != null && containerMetaData instanceof CollectionMetaData)
        {
            return (CollectionMetaData)containerMetaData;
        }
        return null;
    }

    /**
     * Accessor for a map container for this field. Returns null if no map
     * attached.
     * @return The MetaData of the container for this field if a Map.
     **/
    public MapMetaData getMap()
    {
        if (containerMetaData != null && containerMetaData instanceof MapMetaData)
        {
            return (MapMetaData)containerMetaData;
        }
        return null;
    }

    public final String getMappedBy()
    {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy)
    {
        this.mappedBy = StringUtils.isWhitespace(mappedBy) ? null : mappedBy;
    }

    /**
     * Acessor for the columns
     * @return Returns the columnMetaData.
     */
    public final ColumnMetaData[] getColumnMetaData()
    {
        return columnMetaData;
    }

    /**
     * Accessor for elementMetaData
     * @return Returns the elementMetaData.
     */
    public final ElementMetaData getElementMetaData()
    {
        return elementMetaData;
    }

    /**
     * Accessor for keyMetaData
     * @return Returns the keyMetaData.
     */
    public final KeyMetaData getKeyMetaData()
    {
        return keyMetaData;
    }

    /**
     * Accessor for valueMetaData
     * @return Returns the valueMetaData.
     */
    public final ValueMetaData getValueMetaData()
    {
        return valueMetaData;
    }

    /**
     * Accessor for embeddedMetaData
     * @return Returns the embeddedMetaData.
     */
    public final EmbeddedMetaData getEmbeddedMetaData()
    {
        return embeddedMetaData;
    }

    public void setDeleteAction(String action)
    {
        if (action != null)
        {
            foreignKeyMetaData = new ForeignKeyMetaData();
            foreignKeyMetaData.setDeleteAction(ForeignKeyAction.getForeignKeyAction(action));
        }
    }

    /**
     * Accessor for foreignKeyMetaData
     * @return Returns the foreignKeyMetaData.
     */
    public final ForeignKeyMetaData getForeignKeyMetaData()
    {
        return foreignKeyMetaData;
    }

    /**
     * Accessor for indexMetaData
     * @return Returns the indexMetaData.
     */
    public final IndexMetaData getIndexMetaData()
    {
        return indexMetaData;
    }

    /**
     * Accessor for uniqueMetaData
     * @return Returns the uniqueMetaData.
     */
    public final UniqueMetaData getUniqueMetaData()
    {
        return uniqueMetaData;
    }

    /**
     * Accessor for joinMetaData
     * @return Returns the joinMetaData.
     */
    public final JoinMetaData getJoinMetaData()
    {
        return joinMetaData;
    }

    /**
     * Add a new ColumnMetaData element
     * @param colmd the ColumnMetaData to add
     */
    public void addColumn(ColumnMetaData colmd)
    {
        columns.add(colmd);
        colmd.parent = this;
        columnMetaData = new ColumnMetaData[columns.size()];
        for (int i=0; i<columnMetaData.length; i++)
        {
            columnMetaData[i] = columns.get(i);
        }
    }
    
    public void clearColumns()
    {
        columnMetaData = null;
        columns.clear();
        column = null;
    }

    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        addColumn(colmd);
        return colmd;
    }

    /**
     * Accessor for whether the field has a container.
     * @return Whether it represents a container.
     */
    public boolean hasContainer()
    {
        return containerMetaData != null;
    }

    /**
     * Accessor for whether the field has an array
     * @return return true if has array
     */
    public boolean hasArray()
    {
        return containerMetaData == null ? false : (containerMetaData instanceof ArrayMetaData);
    }

    /**
     * Accessor for whether the field has a collection
     * @return return true if has collection
     */
    public boolean hasCollection()
    {
        return containerMetaData == null ? false : (containerMetaData instanceof CollectionMetaData);
    }

    /**
     * Accessor for whether the field has a collection that holds only one element.
     * @return Whether this is a collection with single element
     */
    public boolean isSingleCollection()
    {
        return containerMetaData instanceof CollectionMetaData && ((CollectionMetaData) containerMetaData).singleElement;
    }

    /**
     * Accessor for whether the field has a map.
     * @return return true if has map
     */
    public boolean hasMap()
    {
        return containerMetaData == null ? false : (containerMetaData instanceof MapMetaData);
    }

    /**
     * Accessor for the persistence flags
     * @return Persistence flags (for enhancing)
     */
    public byte getPersistenceFlags()
    {
        return persistenceFlags;
    }

    /**
     * Accessor for whether the field is to be persisted by the persistence process.
     * Currently omits static and final fields, and fields explicitly marked as not persistent.
     * @return Whether it is managed
     */
    public boolean isFieldToBePersisted()
    {
        if (isPopulated())
        {
            if (!PERSIST_STATIC && isStatic())
            {
                return false;
            }
            if (!PERSIST_FINAL && isFinal() && this instanceof FieldMetaData)
            {
                if (persistenceModifier == FieldPersistenceModifier.PERSISTENT)
                {
                    throw new InvalidMetaDataException("044118", getClassName(), getName());
                }
                return false;
            }
        }

        if (persistenceModifier == null)
        {
            return false;
        }
        else if (persistenceModifier.equals(FieldPersistenceModifier.NONE))
        {
            return false;
        }
        return true;
    }
    
    public boolean isOrdered()
    {
        return ordered;
    }
    
    public String getTargetClassName()
    {
        return targetClassName;
    }

    // ------------------------------ Mutators ---------------------------------

    /**
     * Mutator for whether the collection stored in this field is ordered.
     * Only valid until the metadata is initialised.
     */
    public void setOrdered()
    {
        ordered = true;
    }

    /**
     * Mutator for the target class name. Only valid until the metadata is initialised.
     * @param target Target class name
     */
    public void setTargetClassName(String target)
    {
        if (!StringUtils.isWhitespace(target))
        {
            this.targetClassName = target;
        }
    }

    /**
     * Mutator for whetehr to store as a "lob".
     */
    public void setStoreInLob()
    {
        storeInLob = true;
    }

    public void setMapsIdAttribute(String attr)
    {
        // Note that empty string implies use the default id attribute. Null means this is not set
        this.mapsIdAttribute = attr;
        if (mapsIdAttribute != null)
        {
            // TODO Make use of this in store plugins where required
            NucleusLogger.METADATA.warn("@MapsId specified on member " + getFullFieldName() + " yet not currently supported (" + mapsIdAttribute + ")");
        }
    }

    public void setRelationTypeString(String relType)
    {
        this.relationTypeString = relType;
    }
    public String getRelationTypeString()
    {
        return relationTypeString;
    }

    /**
     * Mutator for the cascading of persist operations on this field.
     * @param cascade Whether to cascade at persist
     */
    public void setCascadePersist(boolean cascade)
    {
        this.cascadePersist = cascade;
    }

    /**
     * Mutator for the cascading of update operations on this field.
     * @param cascade Whether to cascade at update
     */
    public void setCascadeUpdate(boolean cascade)
    {
        this.cascadeUpdate = cascade;
    }

    /**
     * Mutator for the cascading of delete operations on this field.
     * @param cascade Whether to cascade at delete
     */
    public void setCascadeDelete(boolean cascade)
    {
        this.cascadeDelete = cascade;
    }

    /**
     * Mutator for the cascading of detach operations on this field.
     * @param cascade Whether to cascade at detach
     */
    public void setCascadeDetach(boolean cascade)
    {
        this.cascadeDetach = cascade;
    }

    /**
     * Mutator for the cascading of refresh operations on this field.
     * @param cascade Whether to cascade at refresh
     */
    public void setCascadeRefresh(boolean cascade)
    {
        this.cascadeRefresh = cascade;
    }

    /**
     * Mutator for the cascading of orphan removal operations on this field.
     * @param cascade Whether to remove orphans on remove
     */
    public void setCascadeRemoveOrphans(boolean cascade)
    {
        this.cascadeRemoveOrphans = cascade;
    }

    /**
     * Mutator for the name of the value generator to use for this strategy.
     * @param generator Name of value generator
     */
    public void setValueGeneratorName(String generator)
    {
        if (StringUtils.isWhitespace(generator))
        {
            this.valueGeneratorName = null;
        }
        else
        {
            this.valueGeneratorName = generator;
        }
    }

    /**
     * Method to set the container for this field (if this field represents a
     * container (collection, map, array).
     * @param conmd The MetaData of the container for this field.
     **/
    public void setContainer(ContainerMetaData conmd)
    {
        containerMetaData = conmd;
        containerMetaData.parent = this;
    }

    /**
     * Method to create a new collection metadata, set it, and return it.
     * @return The collection metadata
     */
    public CollectionMetaData newCollectionMetaData()
    {
        CollectionMetaData collmd = new CollectionMetaData();
        setContainer(collmd);
        return collmd;
    }

    /**
     * Method to create a new array metadata, set it, and return it.
     * @return The array metadata
     */
    public ArrayMetaData newArrayMetaData()
    {
        ArrayMetaData arrmd = new ArrayMetaData();
        setContainer(arrmd);
        return arrmd;
    }

    /**
     * Method to create a new map metadata, set it, and return it.
     * @return The map metadata
     */
    public MapMetaData newMapMetaData()
    {
        MapMetaData mapmd = new MapMetaData();
        setContainer(mapmd);
        return mapmd;
    }

    /**
     * Mutator for the element MetaData 
     * @param elementMetaData The elementMetaData to set.
     */
    public final void setElementMetaData(ElementMetaData elementMetaData)
    {
        this.elementMetaData = elementMetaData;
        this.elementMetaData.parent = this;
    }

    /**
     * Method to create a new element metadata, set it, and return it.
     * @return The element metadata
     */
    public ElementMetaData newElementMetaData()
    {
        ElementMetaData elemmd = new ElementMetaData();
        setElementMetaData(elemmd);
        return elemmd;
    }

    /**
     * Mutator for the key MetaData 
     * @param keyMetaData The keyMetaData to set.
     */
    public final void setKeyMetaData(KeyMetaData keyMetaData)
    {
        this.keyMetaData = keyMetaData;
        this.keyMetaData.parent = this;
    }

    /**
     * Method to create a new key metadata, set it, and return it.
     * @return The key metadata
     */
    public KeyMetaData newKeyMetaData()
    {
        KeyMetaData keymd = new KeyMetaData();
        setKeyMetaData(keymd);
        return keymd;
    }

    /**
     * Mutator for the value MetaData 
     * @param valueMetaData The valueMetaData to set.
     */
    public final void setValueMetaData(ValueMetaData valueMetaData)
    {
        this.valueMetaData = valueMetaData;
        this.valueMetaData.parent = this;
    }

    /**
     * Method to create a new value metadata, set it, and return it.
     * @return The value metadata
     */
    public ValueMetaData newValueMetaData()
    {
        ValueMetaData valuemd = new ValueMetaData();
        setValueMetaData(valuemd);
        return valuemd;
    }

    /**
     * Mutator for the order MetaData 
     * @param orderMetaData The orderMetaData to set.
     */
    public final void setOrderMetaData(OrderMetaData orderMetaData)
    {
        this.orderMetaData = orderMetaData;
        this.orderMetaData.parent = this;
    }

    /**
     * Method to create a new order metadata, set it, and return it.
     * @return The order metadata
     */
    public OrderMetaData newOrderMetaData()
    {
        OrderMetaData ordermd = new OrderMetaData();
        setOrderMetaData(ordermd);
        return ordermd;
    }

    /**
     * Mutator for the embedded MetaData 
     * @param embeddedMetaData The embeddedMetaData to set.
     */
    public final void setEmbeddedMetaData(EmbeddedMetaData embeddedMetaData)
    {
        this.embeddedMetaData = embeddedMetaData;
        this.embeddedMetaData.parent = this;
    }

    /**
     * Method to create a new embedded metadata, set it, and return it.
     * @return The embedded metadata
     */
    public EmbeddedMetaData newEmbeddedMetaData()
    {
        EmbeddedMetaData embmd = new EmbeddedMetaData();
        setEmbeddedMetaData(embmd);
        return embmd;
    }

    /**
     * Mutator for the foreignKey MetaData 
     * @param foreignKeyMetaData The foreignKeyMetaData to set.
     */
    public final void setForeignKeyMetaData(ForeignKeyMetaData foreignKeyMetaData)
    {
        this.foreignKeyMetaData = foreignKeyMetaData;
        this.foreignKeyMetaData.parent = this;
    }

    /**
     * Method to create a new FK metadata, set it, and return it.
     * @return The FK metadata
     */
    public ForeignKeyMetaData newForeignKeyMetaData()
    {
        ForeignKeyMetaData fkmd = new ForeignKeyMetaData();
        setForeignKeyMetaData(fkmd);
        return fkmd;
    }

    /**
     * Mutator for the index MetaData 
     * @param indexMetaData The indexMetaData to set.
     */
    public final void setIndexMetaData(IndexMetaData indexMetaData)
    {
        this.indexMetaData = indexMetaData;
        this.indexMetaData.parent = this;
    }

    /**
     * Method to create a new index metadata, set it, and return it.
     * @return The index metadata
     */
    public IndexMetaData newIndexMetaData()
    {
        IndexMetaData idxmd = new IndexMetaData();
        setIndexMetaData(idxmd);
        return idxmd;
    }

    /**
     * Mutator for the unique MetaData 
     * @param uniqueMetaData The uniqueMetaData to set.
     */
    public final void setUniqueMetaData(UniqueMetaData uniqueMetaData)
    {
        this.uniqueMetaData = uniqueMetaData;
        this.uniqueMetaData.parent = this;
    }

    /**
     * Method to create a new unique metadata, set it, and return it.
     * @return The unique metadata
     */
    public UniqueMetaData newUniqueMetaData()
    {
        UniqueMetaData unimd = new UniqueMetaData();
        setUniqueMetaData(unimd);
        return unimd;
    }

    /**
     * Mutator for the join MetaData 
     * @param joinMetaData The joinMetaData to set.
     */
    public final void setJoinMetaData(JoinMetaData joinMetaData)
    {
        this.joinMetaData = joinMetaData;
        this.joinMetaData.parent = this;
    }

    /**
     * Method to create a new join metadata, set it, and return it.
     * @return The join metadata
     */
    public JoinMetaData newJoinMetaData()
    {
        JoinMetaData joinmd = new JoinMetaData();
        setJoinMetaData(joinmd);
        return joinmd;
    }

    /**
     * Method to create a new JoinMetaData, set it, and return it.
     * @return The join metadata
     */
    public JoinMetaData newJoinMetadata()
    {
        JoinMetaData joinmd = new JoinMetaData();
        setJoinMetaData(joinmd);
        return joinmd;
    }

    /**
     * Mutator for the field id.
     * Given package access since updated by ClassMetaData typically.
     * Only used when the field is not an overriding field.
     * @param field_id Id of the field
     */
    void setFieldId(int field_id)
    {
        fieldId = field_id;
    }

    // ------------------------------ Utilities --------------------------------

    /**
     * Convenience method that sets up the relation type of this field, and the reference to 
     * any related field when it is bidirectional. If the relation is bidirectional then will also
     * set the other side of the relation (to relate to this side).
     * Any member that refers to a PERSISTENT INTERFACE will have relation type as NONE.
     * This should ultimately be changed to reflect the real relation, and relationMemberMetaData
     * should also be set accordingly.
     * @param clr ClassLoader resolver
     * @throws NucleusUserException If mapped-by doesnt exist at other side
     */
    protected void setRelation(ClassLoaderResolver clr)
    {
        if (relationType != null)
        {
            // Already set
            return;
        }

        MetaDataManager mmgr = getMetaDataManager();

        // Find the metadata for the field object
        AbstractClassMetaData otherCmd = null;
        if (hasCollection())
        {
            otherCmd = mmgr.getMetaDataForClass(getCollection().getElementType(), clr);
            if (otherCmd == null)
            {
                // Maybe a reference field
                Class elementCls = clr.classForName(getCollection().getElementType());
                if (ClassUtils.isReferenceType(elementCls))
                {
                    try
                    {
                        String[] implNames = MetaDataUtils.getInstance().getImplementationNamesForReferenceField(this, 
                            FieldRole.ROLE_COLLECTION_ELEMENT, clr, mmgr);
                        if (implNames != null && implNames.length > 0)
                        {
                            // Take the first implementation
                            otherCmd = mmgr.getMetaDataForClass(implNames[0], clr);
                        }
                    }
                    catch (NucleusUserException jpe)
                    {
                        if (!getCollection().isSerializedElement() && mappedBy != null)
                        {
                            // Non-serialised, and with mapped-by so we need implementation classes
                            throw jpe;
                        }

                        // Serialised element with no implementation types so ignore it
                        NucleusLogger.METADATA.debug("Field " + getFullFieldName() +
                            " is a collection of elements of reference type yet no implementation-classes are provided. Assuming they arent persistable");
                    }
                }
            }
        }
        else if (hasMap())
        {
            otherCmd = ((MapMetaData) containerMetaData).getValueClassMetaData(clr);
            //TODO [CORE-2585] valueCMD may be null because its type is an interface (non persistent interface), 
            //so we should handle the implementation classes
            if (otherCmd == null)
            {
                // Value not PC so use the Key if it is specified
                otherCmd = ((MapMetaData)containerMetaData).getKeyClassMetaData(clr);
            }
            if (otherCmd == null)
            {
                // Maybe a reference key/value
            }
        }
        else if (hasArray())
        {
            otherCmd = ((ArrayMetaData)containerMetaData).getElementClassMetaData(clr);
        }
        else
        {
            if (getType().isInterface())
            {
                // Reference field - take the metadata of the first implementation if persistable
                try
                {
                    String[] implNames = MetaDataUtils.getInstance().getImplementationNamesForReferenceField(this, FieldRole.ROLE_FIELD, clr, mmgr);
                    if (implNames != null && implNames.length > 0)
                    {
                        otherCmd = mmgr.getMetaDataForClass(implNames[0], clr);
                    }
                }
                catch (NucleusUserException nue)
                {
                    // No metadata so must be non-persistable interface
                    otherCmd = null;
                }
            }
            // TODO Check for field with generic type where generic type is persistable
            else if (getType().getName().equals(ClassNameConstants.Object) && getFieldTypes() != null)
            {
                // Reference field - take the metadata of the first field type (if specified, and if persistable)
                otherCmd = mmgr.getMetaDataForClass(getFieldTypes()[0], clr);
            }
            else
            {
                otherCmd = mmgr.getMetaDataForClass(getType(), clr);
            }
        }

        //TODO [CORE-2585] when the element or value type is an interface (non persistent interface), 
        //we should look at the implementation classes for the "otherCmd"
        //for now the relationType will be Relation.NONE in these cases because the otherCmd is null
        if (otherCmd == null)
        {
            if (hasArray() && getArray().mayContainPersistableElements())
            { 
                relatedMemberMetaData = null;
                relationType = RelationType.ONE_TO_MANY_UNI;
            }
            else
            {
                // Field cannot have a relation
                relatedMemberMetaData = null;
                relationType = RelationType.NONE;
            }
        }
        else
        {
            // Field is bidirectional
            if (mappedBy != null)
            {
                // This class has the "mapped-by" specified
                AbstractMemberMetaData otherMmd = otherCmd.getMetaDataForMember(mappedBy);
                if (otherMmd == null)
                {
                    throw new NucleusUserException(Localiser.msg("044115", 
                        getAbstractClassMetaData().getFullClassName(), name, mappedBy, otherCmd.getFullClassName())).setFatal();
                }

                relatedMemberMetaData = new AbstractMemberMetaData[] {otherMmd};
                if (isSingleCollection())
                {
                    if (relatedMemberMetaData[0].isSingleCollection())
                    {
                        relationType = RelationType.ONE_TO_ONE_BI;
                    }
                }
                else if (hasContainer() && relatedMemberMetaData[0].hasContainer())
                {
                    relationType = RelationType.MANY_TO_MANY_BI;
                }
                else if (hasContainer() && !relatedMemberMetaData[0].hasContainer())
                {
                    relationType = RelationType.ONE_TO_MANY_BI;
                }
                else if (!hasContainer() && relatedMemberMetaData[0].hasContainer())
                {
                    relationType = RelationType.MANY_TO_ONE_BI;
                }
                else
                {
                    relationType = RelationType.ONE_TO_ONE_BI;
                }
            }
            else
            {
                // The "other" class maybe has "mapped-by" across to this field so navigate through the fields to find this one
                int[] otherFieldNumbers = otherCmd.getAllMemberPositions();
                Set relatedFields = new HashSet();
                for (int i=0;i<otherFieldNumbers.length;i++)
                {
                    AbstractMemberMetaData otherFmd = otherCmd.getMetaDataForManagedMemberAtAbsolutePosition(otherFieldNumbers[i]);
                    if (otherFmd.getMappedBy() != null && otherFmd.getMappedBy().equals(name))
                    {
                        // e.g Look at org.datanucleus.samples.inheritance.marbles
                        if (otherFmd.hasContainer())
                        {
                            // N-1, M-N
                            if ((otherFmd.hasCollection() && otherFmd.getCollection().getElementType().equals(getClassName(true))) ||
                                (otherFmd.hasArray() && otherFmd.getArray().getElementType().equals(getClassName(true))) ||
                                (otherFmd.hasMap() && otherFmd.getMap().getKeyType().equals(getClassName(true))) ||
                                (otherFmd.hasMap() && otherFmd.getMap().getValueType().equals(getClassName(true))))
                            {
                                relatedFields.add(otherFmd);
                                if(isSingleCollection())
                                {
                                    relationType = otherFmd.isSingleCollection() ? RelationType.ONE_TO_ONE_BI : RelationType.MANY_TO_ONE_BI;
                                }
                                else if (hasContainer())
                                {
                                    // Should we mark Arrays, Lists, Maps as M-N ?
                                    relationType = RelationType.MANY_TO_MANY_BI;
                                }
                                else
                                {
                                    relationType = RelationType.MANY_TO_ONE_BI;
                                }
                            }
                            else
                            {
                                // Maybe we have Collection<Interface> and Interface bidir relation
                                String elementType = null;
                                if (otherFmd.hasCollection())
                                {
                                    elementType = otherFmd.getCollection().getElementType();
                                }
                                else if (otherFmd.hasArray())
                                {
                                    elementType = otherFmd.getArray().getElementType();
                                }
                                if (elementType != null)
                                {
                                    Class elementCls = clr.classForName(elementType);
                                    if (elementCls.isInterface())
                                    {
                                        Class thisCls = clr.classForName(getClassName(true));
                                        if (elementCls.isAssignableFrom(thisCls))
                                        {
                                            relatedFields.add(otherFmd);
                                            // This class implements the element interface type, so matches
                                            if (hasContainer())
                                            {
                                                relationType = RelationType.MANY_TO_MANY_BI;
                                            }
                                            else
                                            {
                                                relationType = RelationType.MANY_TO_ONE_BI;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            // 1-1, 1-N
                            Class cls = clr.classForName(getClassName(true));
                            if (otherFmd.getType().isAssignableFrom(cls) || cls.isAssignableFrom(otherFmd.getType()))
                            {
                                // Consistent 1-1, 1-N types (allow subclasses of the defined types)
                                relatedFields.add(otherFmd);
                                if (hasContainer())
                                {
                                    relationType = RelationType.ONE_TO_MANY_BI;
                                }
                                else
                                {
                                    relationType = RelationType.ONE_TO_ONE_BI;
                                }
                            }
                        }
                    }
                }
                if (!relatedFields.isEmpty())
                {
                    relatedMemberMetaData = (AbstractMemberMetaData[])relatedFields.toArray(new AbstractMemberMetaData[relatedFields.size()]);
                    relatedFields.clear();
                    relatedFields = null;
                }
                else
                {
                    // No "mapped-by" found at either end so is unidirectional
                    if (hasContainer())
                    {
                        relationType = isSingleCollection() ? RelationType.ONE_TO_ONE_UNI : RelationType.ONE_TO_MANY_UNI;
                    }
                    else if (joinMetaData != null)
                    {
                        relationType = RelationType.MANY_TO_ONE_UNI;
                    }
                    else
                    {
                        relationType = RelationType.ONE_TO_ONE_UNI;
                    }
                }
            }
        }
    }

    /**
     * Accessor for the relation type for this field.
     * @param clr ClassLoader resolver
     * @return The relation type.
     */
    public RelationType getRelationType(ClassLoaderResolver clr)
    {
        if (relationType == null)
        {
            // Is possible that this could be done in the populate() step but depends on availability of the related class
            setRelation(clr);
        }
        return relationType;
    }

    /**
     * Convenience method to return if this member relates to a persistent interface.
     * All members that have a relation will return NONE from getRelationType but can be
     * accessed through here.
     * TODO Merge this with relation methods so we only need the relationType/relatedMemberMetaData.
     * @param clr ClassLoader resolver
     * @return Whether it is for a persistent interface
     */
    public boolean isPersistentInterface(ClassLoaderResolver clr)
    {
        MetaDataManager mmgr = getMetaDataManager();
        if (hasCollection())
        {
            if (mmgr.isPersistentInterface(getCollection().getElementType()))
            {
                return true;
            }
        }
        else if (hasMap())
        {
            if (mmgr.isPersistentInterface(getMap().getKeyType()))
            {
                return true;
            }
            if (mmgr.isPersistentInterface(getMap().getValueType()))
            {
                return true;
            }
        }
        else if (hasArray())
        {
            if (mmgr.isPersistentInterface(getArray().getElementType()))
            {
                return true;
            }
        }
        else
        {
            if (getType().isInterface())
            {
                if (mmgr.isPersistentInterface(getTypeName()))
                {
                    return true;
                }

                String[] fieldTypes = getFieldTypes();
                if (fieldTypes != null && mmgr.isPersistentInterface(fieldTypes[0]))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convenience method for whether this field is the owner of the relation.
     * If the field has no relation will return true.
     * If the field is in a unidirectional relation will return true.
     * If the field is in a bidirectional relation and has no mapped-by will return true.
     * Otherwise returns false.
     * @param clr ClassLoader resolver
     * @return Whether it is the owner side of a relation
     */
    public boolean isRelationOwner(ClassLoaderResolver clr)
    {
        if (relationType == null)
        {
            setRelation(clr);
        }

        if (relationType == RelationType.NONE)
        {
            return true;
        }
        else if (relationType == RelationType.ONE_TO_MANY_UNI || relationType == RelationType.ONE_TO_ONE_UNI)
        {
            return true;
        }
        else if (relationType == RelationType.MANY_TO_MANY_BI || relationType == RelationType.MANY_TO_ONE_BI ||
            relationType == RelationType.ONE_TO_MANY_BI || relationType == RelationType.ONE_TO_ONE_BI)
        {
            return mappedBy == null;
        }
        else if (relationType == RelationType.MANY_TO_ONE_UNI)
        {
            return true;
        }
        return false;
    }

    /**
     * Accessor for the FieldMetaData of any related field/property (where this field is part of a 
     * bidirectional relation). Allows for 1-1, 1-N, and M-N. If this field is not part of a bidirectional 
     * relation (no "mapped-by" at either end) then it returns null.
     * @param clr the ClassLoaderResolver
     * @return The MetaData for the field/property at the "other end".
     */
    public AbstractMemberMetaData[] getRelatedMemberMetaData(ClassLoaderResolver clr)
    {
        if (relationType == null)
        {
            // Is possible that this could be done in the populate() step but depends on availability of the related class
            setRelation(clr);
        }
        // TODO This doesnt allow for 1-1, 1-N reference fields where there may be multiple "other fields".
        return relatedMemberMetaData;
    }

    /**
     * Convenience accessor for the MetaData for the field/property at the other side of the bidirectional
     * relation given the objects at this side and the other side.
     * TODO Note that this only applies to 1-1, N-1 fields currently
     * @param clr ClassLoader Resolver
     * @param thisPC This object
     * @param otherPC The related object
     * @return The MetaData for the field in the related object
     */
    public AbstractMemberMetaData getRelatedMemberMetaDataForObject(ClassLoaderResolver clr, Object thisPC, Object otherPC)
    {
        if (relationType == null)
        {
            setRelation(clr);
        }

        if (relatedMemberMetaData == null)
        {
            return null;
        }

        // TODO Cater for 1-N, M-N types
        for (int i=0;i<relatedMemberMetaData.length;i++)
        {
            if (relationType == RelationType.ONE_TO_ONE_BI)
            {
                AbstractMemberMetaData relMmd = relatedMemberMetaData[i];
                Class relatedType = relMmd.isSingleCollection() ? clr.classForName(relMmd.getCollection().getElementType()) : relMmd.getType();
                Class type = isSingleCollection() ? clr.classForName(getCollection().getElementType()) : getType(); 
                if (relatedType.isAssignableFrom(thisPC.getClass()) &&
                    type.isAssignableFrom(otherPC.getClass()))
                {
                    return relatedMemberMetaData[i];
                }
            }
            else if (relationType == RelationType.MANY_TO_ONE_BI)
            {
                // Just allow for Collections
                if (relatedMemberMetaData[i].hasCollection())
                {
                    Class elementType = clr.classForName(relatedMemberMetaData[i].getCollection().getElementType());
                    if (elementType.isAssignableFrom(thisPC.getClass()) &&
                        getType().isAssignableFrom(otherPC.getClass()))
                    {
                        return relatedMemberMetaData[i];
                    }
                }
                else if (relatedMemberMetaData[i].hasMap())
                {
                    Class valueType = clr.classForName(relatedMemberMetaData[i].getMap().getValueType());
                    if (valueType.isAssignableFrom(thisPC.getClass()) &&
                        getType().isAssignableFrom(otherPC.getClass()))
                    {
                        return relatedMemberMetaData[i];
                    }
                    Class keyType = clr.classForName(relatedMemberMetaData[i].getMap().getKeyType());
                    if (keyType.isAssignableFrom(thisPC.getClass()) &&
                        getType().isAssignableFrom(otherPC.getClass()))
                    {
                        return relatedMemberMetaData[i];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Accessor for all ClassMetaData referenced by this Field.
     * Part of the "persistence-by-reachability" concept. 
     * @param orderedCmds List of ordered ClassMetaData objects (added to).
     * @param referencedCmds Set of referenced ClassMetaData objects (added to)
     * @param clr the ClassLoaderResolver
     */
    void getReferencedClassMetaData(final List<AbstractClassMetaData> orderedCmds, final Set<AbstractClassMetaData> referencedCmds, final ClassLoaderResolver clr)
    {
        MetaDataManager mmgr = getMetaDataManager();

        AbstractClassMetaData theTypeCmd = mmgr.getMetaDataForClass(getType(), clr);
        if (theTypeCmd != null)
        {
            theTypeCmd.getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
        }

        if (hasCollection())
        {
            getCollection().getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
        }
        else if (hasMap())
        {
            getMap().getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
        }
        else if (hasArray())
        {
            getArray().getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
        }
    }
    
    /**
     *  Calculate wether this field should be a second class mutable field.
     *  This calculation is a bit expensive.
     *  Please note that this data will be cached in {@link AbstractClassMetaData#scoMutableMemberFlags}.
     *  @param mmgr MetaData manager
     *  @return wether this field should be a second class mutable field.
     */
    public boolean calcIsSecondClassMutable(MetaDataManager mmgr)
    {
        if (hasExtension("is-second-class"))
        {
            String isSecondClass = getValueForExtension("is-second-class");
            if (isSecondClass.equalsIgnoreCase("true"))
            {
                return true;
            }
            else if (isSecondClass.equalsIgnoreCase("false"))
            {
                return false;
            }
            else if (isSecondClass.equalsIgnoreCase("default"))
            {
                // fall through to default behaviour
            }
            else
            {
                // Invalid value
                throw new InvalidMetaDataException("044002", "is-second-class", "true/false/default", isSecondClass);
            }
        }
        else
        {
            // fall through to default behaviour
        }

        return mmgr.getNucleusContext().getTypeManager().isSecondClassMutableType(getTypeName());
    }

    /**
     * Convenience method to return if the field/property is insertable.
     * @return Whether we are allowed to insert it
     */
    public boolean isInsertable()
    {
        if (hasCollection() || hasArray())
        {
            if (elementMetaData != null && elementMetaData.getColumnMetaData() != null && elementMetaData.getColumnMetaData().length > 0)
            {
                return elementMetaData.getColumnMetaData()[0].getInsertable();
            }
        }
        else if (hasMap())
        {
            return true;
        }
        else
        {
            if (columnMetaData != null && columnMetaData.length > 0)
            {
                return columnMetaData[0].getInsertable();
            }

            String insertVal = getValueForExtension(MetaData.EXTENSION_MEMBER_INSERTABLE);
            if (!StringUtils.isWhitespace(insertVal))
            {
                return Boolean.parseBoolean(insertVal);
            }
        }
        return true;
    }

    /**
     * Convenience method to return if the field/property is updateable.
     * @return Whether we are allowed to update it
     */
    public boolean isUpdateable()
    {
        if (hasCollection() || hasArray())
        {
            if (elementMetaData != null && elementMetaData.getColumnMetaData() != null && elementMetaData.getColumnMetaData().length > 0)
            {
                return elementMetaData.getColumnMetaData()[0].getUpdateable();
            }
        }
        else if (hasMap())
        {
            return true;
        }
        else
        {
            if (columnMetaData != null && columnMetaData.length > 0)
            {
                return columnMetaData[0].getUpdateable();
            }

            String updateVal = getValueForExtension(MetaData.EXTENSION_MEMBER_UPDATEABLE);
            if (!StringUtils.isWhitespace(updateVal))
            {
                return Boolean.parseBoolean(updateVal);
            }
        }
        return true;
    }

    // TODO Enable these and fix compareTo() to do the equivalent
    /*public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        return compareTo(o) == 0;
    }
    public int hashCode()
    {
        return getFullFieldName().hashCode();
    }*/

    /**
     * Comparator method. This allows the ClassMetaData to search for an AbstractMemberMetaData with a particular name.
     * @param o The object to compare against
     * @return The comparison result
     */ 
    public int compareTo(Object o)
    {
        if (o instanceof AbstractMemberMetaData)
        {
            // TODO Currently only uses the name since we only use sorting from ClassMetaData; maybe an idea to use the className also (see equals/hashCode).
            AbstractMemberMetaData c = (AbstractMemberMetaData)o;
            return this.name.compareTo(c.name);
        }
        throw new ClassCastException(this.getClass().getName() + " != " + o.getClass().getName());
    }
}