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
2004 Marco Schulze (NightLabs) - changed the behaviour to warn only if
        an inherited class declares an own objectid and it equals the
        one of the superclass.
2004 Andy Jefferson - Added discriminator/inheritance checks
2004 Erik Bengtson - changes for application identity
2004 Andy Jefferson - moved PK class checks out into JDOUtils
2007 Xuan Baldauf - little reduction in code duplication to anticipate changes regarding issue http://www.jpox.org/servlet/jira/browse/CORE-3272
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Representation of the MetaData of a class. 
 * Extends the abstract definition to include implementations, fields, embedded-only tags. 
 * Has a parent PackageMetaData that can contain the metadata for several classes.
 *
 * <H3>Lifecycle state</H3>
 * This object supports 3 lifecycle states. The first is the raw constructed object which represents pure MetaData (maybe from a MetaData file). 
 * The second is a "populated" object which represents MetaData for a Class with the metadata aligned to be appropriate for that Class.
 * The third is "initialised" once the internal arrays are created.
 * This object, once populated, will represent ALL fields in the class (including static, final and transient fields).
 *
 * <H3>Fields/Properties</H3>
 * This object keeps a list of FieldMetaData/PropertyMetaData objects for the fields of this class. 
 * In addition it has an array of FieldMetaData objects representing those that are actually managed by JDO ("managedFields"). 
 * This second set does not contain things like static, final or transient fields since JDO doesn't support those yet.
 * <P>Fields are of 2 types. The first are normal fields of this class.
 * These have their own "relative" field number, relative to this class.
 * The second type are "overriding" fields which override the baseline field in a superclass. These fields have no "relative" field number since 
 * they are relative to this class (and such a relative field number would make no sense).
 * Fields are all added through addField() during the parse process, and are updated during the populate/initialise process to define their 
 * relative field numbers. Please refer to FieldMetaData for more details of fields.
 *
 * <H3>Numbering of fields</H3>
 * Fields of the class are numbered in 2 ways. The first way is the numbering within a class. 
 * In a class, the field 'id's will start at 0. If a class is inherited, it will also have a second numbering for its fields - the
 * "absolute" numbering. With "absolute" numbering, the fields start at the first field in the root superclass which has absolute number 0, and they are
 * numbered from there, navigating down the hierarchy. In terms of what is stored in the records, the FieldMetaData stores fieldId as the first
 * method (relative to the class it is in). The "absolute" numbering is  always derived from this and the inheritance hierarchy.
 */
public class ClassMetaData extends AbstractClassMetaData
{
    private static final long serialVersionUID = -1029032058753152022L;

    /** List of implements. */
    protected List<ImplementsMetaData> implementations = null;

    /** Is the persistable class abstract. */
    protected boolean isAbstract;

    /**
     * Constructor.
     * Takes the basic string information found in the MetaData file.
     * @param parent The package to which this class belongs
     * @param name Name of class
     */
    public ClassMetaData(final PackageMetaData parent, final String name)
    {
        super(parent, name);
    }

    /**
     * Constructor for creating the ClassMetaData for an implementation of a "persistent-interface".
     * @param imd MetaData for the "persistent-interface"
     * @param implClassName Name of the implementation class
     * @param copyFields Whether to copy the fields of the interface too
     */
    public ClassMetaData(final InterfaceMetaData imd, String implClassName, boolean copyFields)
    {
        super(imd, implClassName, copyFields);
    }

    /**
     * Constructor for creating the ClassMetaData for an implementation of a "persistent-abstract-class".
     * @param cmd MetaData for the implementation of the "persistent-abstract-class"
     * @param implClassName Name of the implementation class
     */
    public ClassMetaData(final ClassMetaData cmd, String implClassName)
    {
        super(cmd, implClassName);
    }

    /**
     * Method to provide the details of the class being represented by this MetaData.
     * This can be used to firstly provide defaults for attributes that aren't specified in the MetaData, 
     * and secondly to report any errors with attributes that have been specified that are inconsistent with the
     * class being represented.
     * <P>
     * One possible use of this method would be to take a basic ClassMetaData for a class and call this, passing in the users class. 
     * This would then add AbstractMemberMetaData for all fields in this class providing defaults for all of these.
     * @param clr ClassLoaderResolver to use in loading any classes
     * @param primary the primary ClassLoader to use (or null)
     * @param mgr MetaData manager
     */
    public synchronized void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mgr)
    {
        if (isInitialised() || isPopulated())
        {
            NucleusLogger.METADATA.error(Localiser.msg("044068", name));
            throw new NucleusException(Localiser.msg("044068", fullName)).setFatal();
        }
        if (populating)
        {
            return;
        }

        this.mmgr = mgr;
        try
        {
            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044075", fullName));
            }
            populating = true;

            Class cls = loadClass(clr, primary);

            isAbstract = Modifier.isAbstract(cls.getModifiers());

            // Load any Annotations definition for this class
            if (!isMetaDataComplete())
            {
                mmgr.addAnnotationsDataToClass(cls, this, clr);
            }

            // Load any ORM definition for this class
            mmgr.addORMDataToClass(cls, clr);

            // If a class is an inner class and is non-static it is invalid
            if (ClassUtils.isInnerClass(fullName) && !Modifier.isStatic(cls.getModifiers()) && persistenceModifier == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                throw new InvalidClassMetaDataException("044063", fullName);
            }

            if (entityName == null)
            {
                // No entity name given so just default to the name of the class (without package)
                this.entityName = name;
            }

            determineSuperClassName(clr, cls);

            inheritIdentity();
            determineIdentity();
            validateUserInputForIdentity();

            addMetaDataForMembersNotInMetaData(cls);

            // Set inheritance
            validateUserInputForInheritanceMetaData(isAbstract());
            determineInheritanceMetaData();
            applyDefaultDiscriminatorValueWhenNotSpecified();

            // Process all members marked as UNKNOWN (i.e override from JPA) and eliminate clashes with any generic overrides from addMetaDataForMembersNotInMetaData(...)
            Iterator<AbstractMemberMetaData> memberIter = members.iterator();
            Set<AbstractMemberMetaData> membersToDelete = null;
            while (memberIter.hasNext())
            {
                AbstractMemberMetaData mmd = memberIter.next();
                if (mmd.className != null && mmd.className.equals("#UNKNOWN"))
                {
                    // Field is for a superclass but we didn't know which at creation so resolve it
                    if (pcSuperclassMetaData == null)
                    {
                        // No superclass so it doesn't make sense so assume to be for this class
                        mmd.className = null;
                    }
                    else
                    {
                        AbstractMemberMetaData superFmd = pcSuperclassMetaData.getMetaDataForMember(mmd.getName());
                        if (superFmd != null)
                        {
                            // Field is for a superclass so set its "className"
                            mmd.className = (superFmd.className != null) ? superFmd.className : superFmd.getClassName();

                            // Copy across other attributes of the original definition
                            mmd.primaryKey = superFmd.primaryKey;
                            mmd.persistenceModifier = superFmd.persistenceModifier;
                            mmd.valueStrategy = superFmd.valueStrategy;
                            mmd.valueGeneratorName = superFmd.valueGeneratorName;
                            mmd.sequence = superFmd.sequence;
                            mmd.cacheable = superFmd.cacheable;
                            mmd.storeInLob = superFmd.storeInLob;
                            // TODO Copy across more attributes

                            Iterator<AbstractMemberMetaData> existingMemberIter = members.iterator();
                            while (existingMemberIter.hasNext())
                            {
                                AbstractMemberMetaData existingMmd = existingMemberIter.next();
                                if (existingMmd.getName().equals(mmd.getName()) && existingMmd != mmd)
                                {
                                    mmd.type = existingMmd.getType();
                                    if (membersToDelete == null)
                                    {
                                        membersToDelete = new HashSet<>();
                                    }
                                    membersToDelete.add(existingMmd);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (membersToDelete != null)
            {
                members.removeAll(membersToDelete);
            }

            if (objectidClass == null)
            {
                // No user-defined objectid-class but potentially have SingleFieldIdentity so make sure PK fields are set
                populateMemberMetaData(clr, cls, true, primary); // PK fields
                determineObjectIdClass();
                populateMemberMetaData(clr, cls, false, primary); // Non-PK fields
            }
            else
            {
                populateMemberMetaData(clr, cls, true, primary);
                populateMemberMetaData(clr, cls, false, primary);
                determineObjectIdClass();
            }

            validateUnmappedColumns();

            // populate the implements
            if (implementations != null)
            {
                for (ImplementsMetaData implmd : implementations)
                {
                    implmd.populate(clr, primary);
                }
            }

            if (persistentInterfaceImplNeedingTableFromSuperclass)
            {
                // Need to go up to next superinterface and make sure its metadata is populated
                // until we find the next interface with metadata with inheritance strategy of "new-table".
                AbstractClassMetaData acmd = getMetaDataForSuperinterfaceManagingTable(cls, clr);
                if (acmd != null)
                {
                    table = acmd.table;
                    schema = acmd.schema;
                    catalog = acmd.catalog;
                }
                persistentInterfaceImplNeedingTableFromSuperclass = false;
            }
            else if (persistentInterfaceImplNeedingTableFromSubclass)
            {
                // TODO Cater for finding the subclass-table that manages our table
                persistentInterfaceImplNeedingTableFromSubclass = false;
            }

            setPopulated();
        }
        catch (RuntimeException e)
        {
            NucleusLogger.METADATA.debug(e);
            throw e;
        }
        finally
        {
            populating = false;
        }
    }

    /**
     * Method to find a superinterface with MetaData that specifies NEW_TABLE inheritance strategy
     * @param cls The class
     * @param clr ClassLoader resolver
     * @return The AbstractClassMetaData for the class managing the table
     */
    private AbstractClassMetaData getMetaDataForSuperinterfaceManagingTable(Class cls, ClassLoaderResolver clr)
    {
        for (Class<?> superintf : ClassUtils.getSuperinterfaces(cls))
        {
            AbstractClassMetaData acmd = mmgr.getMetaDataForInterface(superintf, clr);
            if (acmd != null && acmd.getInheritanceMetaData() != null)
            {
                if (acmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.NEW_TABLE)
                {
                    // Found it
                    return acmd;
                }
                else if (acmd.getInheritanceMetaData().getStrategy() == InheritanceStrategy.SUPERCLASS_TABLE)
                {
                    // Try further up the hierarchy
                    return getMetaDataForSuperinterfaceManagingTable(superintf, clr);
                }
            }
        }
        return null;
    }

    /**
     * Add MetaData of fields/properties not declared in MetaData.
     * Note that if a member is defined using some generic type in the superclass and this class can use a TypeVariable to resolve it then this
     * will add the member to "members" here since the type will be different to this class.
     * @param cls Class represented by this metadata
     */
    protected void addMetaDataForMembersNotInMetaData(Class cls)
    {
        // Access API since we treat things differently for JPA and JDO
        String api = mmgr.getNucleusContext().getApiName();

        Set<String> memberNames = new HashSet<>();
        for (AbstractMemberMetaData mmd : members)
        {
            memberNames.add(mmd.getName());
        }

        // Add fields/properties for the class that don't have MetaData.
        // We use Reflection here since JDOImplHelper would only give use info
        // for enhanced files (and the enhancer needs unenhanced as well). 
        // NOTE 1 : We ignore fields/properties in superclasses
        // NOTE 2 : We ignore "enhanced" fields/properties (added by the enhancer)
        // NOTE 3 : We ignore inner class fields/properties (containing "$")
        // NOTE 4 : We sort the fields/properties into ascending alphabetical order
        Collections.sort(members);
        try
        {
            // check if we have any persistent properties in this class
            boolean hasProperties = false;
            for (int i=0; i<members.size(); i++)
            {
                if (members.get(i) instanceof PropertyMetaData)
                {
                    hasProperties = true;
                    break;
                }
            }
            if (members.size() == 0)
            {
                // Nothing specified in this class so check superclasses and default to the same as those
                if (pcSuperclassMetaData != null)
                {
                    for (int i=0; i<pcSuperclassMetaData.members.size(); i++)
                    {
                        if (pcSuperclassMetaData.members.get(i) instanceof PropertyMetaData)
                        {
                            hasProperties = true;
                            break;
                        }
                    }
                }
            }

            // Add fields/properties in the current class that have been omitted by the user (subject to the API default handling)
            if (hasProperties)
            {
                if (api.equalsIgnoreCase("JPA"))
                {
                    // JPA : when we are using properties go through and add properties for those not specified in the populating class.
                    Method[] clsMethods = cls.getDeclaredMethods();
                    for (int i=0;i<clsMethods.length;i++)
                    {
                        // Limit to valid java bean getter methods in this class (don't allow inner class methods)
                        if (clsMethods[i].getDeclaringClass().getName().equals(fullName) && !clsMethods[i].isBridge() &&
                            ClassUtils.isJavaBeanGetterMethod(clsMethods[i]) && !ClassUtils.isInnerClass(clsMethods[i].getName()))
                        {
                            // Find if there is metadata for this property
                            String propertyName = ClassUtils.getFieldNameForJavaBeanGetter(clsMethods[i].getName());
                            // TODO : This will not check if the name is a property! so we can miss field+property clashes
                            if (!memberNames.contains(propertyName))
                            {
                                // No field/property of this name - add a default PropertyMetaData for this method
                                NucleusLogger.METADATA.debug(Localiser.msg("044060", fullName, propertyName));
                                AbstractMemberMetaData mmd = new PropertyMetaData(this, propertyName);
                                members.add(mmd);
                                memberNames.add(mmd.getName());
                                Collections.sort(members);
                            }
                            else
                            {
                                // Field/property exists
                            }
                        }
                    }
                }
                else
                {
                    // With JDO we only use properties when defined explicitly
                }
            }

            // Process all (reflected) fields in the populating class
            Field[] clsFields = cls.getDeclaredFields();
            for (int i=0;i<clsFields.length;i++)
            {
                // Limit to fields in this class, that aren't enhancer-added fields that aren't inner class fields, and that aren't static
                if (!ClassUtils.isInnerClass(clsFields[i].getName()) && !Modifier.isStatic(clsFields[i].getModifiers()) &&
                    !mmgr.isEnhancerField(clsFields[i].getName()) &&
                    clsFields[i].getDeclaringClass().getName().equals(fullName))
                {
                    // Find if there is metadata for this field
                    // TODO : This will not check if the name is a field! so we can miss field+property clashes
                    if (!memberNames.contains(clsFields[i].getName()))
                    {
                        // No field/property of this name
                        if (hasProperties && api.equalsIgnoreCase("JPA")) // With JPA, if using props then don't add a field as well (since a PropertyMetaData will be present by default)
                        {
                            // Do nothing
                        }
                        else
                        {
                            // Class has fields but field not present, so add as field
                            AbstractMemberMetaData mmd = new FieldMetaData(this, clsFields[i].getName());
                            NucleusLogger.METADATA.debug(Localiser.msg("044060", fullName, clsFields[i].getName()));
                            members.add(mmd);

                            memberNames.add(mmd.getName());
                            Collections.sort(members);
                        }
                    }
                }
            }

            // Process any generic TypeVariables adding member overrides where this class defines the type of the generic in a superclass
            ParameterizedType genSuperclassParamType = cls.getGenericSuperclass() instanceof ParameterizedType ? (ParameterizedType) cls.getGenericSuperclass() : null;
            TypeVariable[] clsDeclTypes = cls.getTypeParameters();
            if (hasProperties)
            {
                // PROPERTIES : Check for any TypeVariables used with Java bean getter/setter methods in superclass(es) and override the metadata in the superclass to use the right type
                Method[] allclsMethods = cls.getMethods();
                for (int i=0;i<allclsMethods.length;i++)
                {
                    // Limit to valid java bean getter methods in this class (don't allow inner class methods)
                    if (!allclsMethods[i].getDeclaringClass().getName().equals(fullName) && 
                        ClassUtils.isJavaBeanGetterMethod(allclsMethods[i]) && !ClassUtils.isInnerClass(allclsMethods[i].getName()) &&
                        allclsMethods[i].getGenericReturnType() != null && allclsMethods[i].getGenericReturnType() instanceof TypeVariable)
                    {
                        TypeVariable methodTypeVar = (TypeVariable) allclsMethods[i].getGenericReturnType();
                        Class declCls = allclsMethods[i].getDeclaringClass();

                        TypeVariable[] declTypes = declCls.getTypeParameters();
                        String propertyName = ClassUtils.getFieldNameForJavaBeanGetter(allclsMethods[i].getName());
                        String propertyNameFull = allclsMethods[i].getDeclaringClass().getName() + "." + propertyName;
                        boolean foundTypeForTypeVariable = false;
                        if (declTypes != null)
                        {
                            for (int j=0;j<declTypes.length;j++)
                            {
                                if (genSuperclassParamType != null && declTypes[j].getName().equals(methodTypeVar.getName()))
                                {
                                    // Declared type is fully defined (rather than just bound)
                                    // This class has TypeVariable definition for superclass
                                    Type[] paramTypeArgs = genSuperclassParamType.getActualTypeArguments();
                                    if (paramTypeArgs != null && paramTypeArgs.length > j && paramTypeArgs[j] instanceof Class)
                                    {
                                        // Declared type is fully defined (rather than just bound)
                                        NucleusLogger.METADATA.debug("Class=" + cls.getName() + " property=" + propertyName +
                                            " declared to return " + methodTypeVar + ", namely TypeVariable(" + j + ") of " + declCls.getName() + " so using " + paramTypeArgs[j]);
                                        if (!memberNames.contains(propertyName))
                                        {
                                            // No property of this name - add a default PropertyMetaData for this method with the type set to what we need
                                            NucleusLogger.METADATA.debug(Localiser.msg("044060", fullName, propertyNameFull));
                                            AbstractMemberMetaData overriddenMmd = getMemberBeingOverridden(propertyName);

                                            AbstractMemberMetaData mmd = new PropertyMetaData(this, propertyNameFull);
                                            mergeMemberMetaDataForOverrideOfType((Class) paramTypeArgs[j], mmd, overriddenMmd);
                                            members.add(mmd);

                                            memberNames.add(mmd.getName());
                                            Collections.sort(members);
                                        }
                                        else
                                        {
                                            // User has overridden the field, so update the type on their definition
                                            AbstractMemberMetaData overrideMmd = getMetaDataForMember(propertyName);
                                            overrideMmd.type = (Class) paramTypeArgs[j];
                                        }
                                        foundTypeForTypeVariable = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!foundTypeForTypeVariable)
                        {
                            // Search bounds of this class for the property type variable (e.g "MyClass<D extends BaseD> extends MySuperClass<D>" will give bound of BaseD)
                            if (clsDeclTypes != null)
                            {
                                for (int j=0;j<clsDeclTypes.length;j++)
                                {
                                    if (clsDeclTypes[j].getName().equals(methodTypeVar.getName()))
                                    {
                                        Type[] boundTypes = clsDeclTypes[j].getBounds();
                                        if (boundTypes != null && boundTypes.length == 1 && boundTypes[0] instanceof Class)
                                        {
                                            // User has class declaration like "public class MyClass<T extends SomeType>" so take SomeType
                                            boolean updateType = true;
                                            AbstractMemberMetaData overriddenMmd = getMetaDataForMember(propertyName);
                                            if (overriddenMmd != null)
                                            {
                                                if (overriddenMmd.getTypeName().equals(((Class)boundTypes[0]).getName()))
                                                {
                                                    // Already overridden the type in a superclass, so ignore
                                                    updateType = false;
                                                }
                                            }

                                            if (updateType)
                                            {
                                                NucleusLogger.METADATA.debug("Class=" + cls.getName() + " property=" + propertyName +
                                                    " declared to return " + methodTypeVar + ", namely TypeVariable(" + j + ") with bound, so using bound of " + boundTypes[0]);
                                                if (!memberNames.contains(propertyName))
                                                {
                                                    // No property of this name - add a default PropertyMetaData for this method with the type set to what we need
                                                    NucleusLogger.METADATA.debug(Localiser.msg("044060", fullName, propertyNameFull));

                                                    AbstractMemberMetaData mmd = new PropertyMetaData(this, propertyNameFull);
                                                    mergeMemberMetaDataForOverrideOfType((Class)boundTypes[0], mmd, overriddenMmd);
                                                    members.add(mmd);

                                                    memberNames.add(mmd.getName());
                                                    Collections.sort(members);
                                                }
                                                else
                                                {
                                                    // User has overridden the field, so update the type on their definition
                                                    AbstractMemberMetaData overrideMmd = getMetaDataForMember(propertyName);
                                                    overrideMmd.type = (Class) boundTypes[0];
                                                }
                                            }
                                            foundTypeForTypeVariable = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                // FIELDS : Check for any TypeVariables used with fields in superclass(es) and override the metadata in the superclass to use the right type
                Class theClass = cls;
                while (theClass.getSuperclass() != null)
                {
                    theClass = theClass.getSuperclass();
                    Field[] theclsFields = theClass.getDeclaredFields();
                    for (int i=0;i<theclsFields.length;i++)
                    {
                        if (!ClassUtils.isInnerClass(theclsFields[i].getName()) && !Modifier.isStatic(theclsFields[i].getModifiers()) &&
                            !mmgr.isEnhancerField(theclsFields[i].getName()) &&
                            theclsFields[i].getGenericType() != null && theclsFields[i].getGenericType() instanceof TypeVariable)
                        {
                            // Superclass field is declared as a generic type, so attempt to resolve it TODO Check whether already resolved in superclass so we don't need to
                            TypeVariable fieldTypeVar = (TypeVariable) theclsFields[i].getGenericType();
                            Class declCls = theclsFields[i].getDeclaringClass();

                            TypeVariable[] declTypes = declCls.getTypeParameters();
                            String fieldName = theclsFields[i].getName();
                            String fieldNameFull = declCls.getName() + "." + theclsFields[i].getName();
                            boolean foundTypeForTypeVariable = false;
                            if (declTypes != null)
                            {
                                for (int j=0;j<declTypes.length;j++)
                                {
                                    if (genSuperclassParamType != null && declTypes[j].getName().equals(fieldTypeVar.getName()))
                                    {
                                        Type[] paramTypeArgs = genSuperclassParamType.getActualTypeArguments();
                                        if (paramTypeArgs != null && paramTypeArgs.length > j && paramTypeArgs[j] instanceof Class)
                                        {
                                            // Declared type is fully defined (rather than just bound)
                                            NucleusLogger.METADATA.debug("Class=" + cls.getName() + " field=" + fieldName +
                                                " declared to be " + fieldTypeVar + ", namely TypeVariable(" + j + ") of " + declCls.getName() + " so using " + paramTypeArgs[j]);
                                            if (!memberNames.contains(fieldName))
                                            {
                                                // No member of this name - add a default FieldMetaData for this field with the type set to what we need
                                                NucleusLogger.METADATA.debug(Localiser.msg("044060", fullName, fieldNameFull));
                                                AbstractMemberMetaData overriddenMmd = getMemberBeingOverridden(fieldName);

                                                // Merge the overridden member with the limited info specified in this override
                                                AbstractMemberMetaData mmd = new FieldMetaData(this, fieldNameFull);
                                                // Note that if we override a single PK field we will have objectIdClass=ObjectId in the generics superclass, and still ObjectId here
                                                // We have to keep to continue like this since the superclass will have been enhanced to have ObjectId in its bytecode contract.
                                                mergeMemberMetaDataForOverrideOfType((Class) paramTypeArgs[j], mmd, overriddenMmd);
                                                members.add(mmd);

                                                memberNames.add(mmd.getName());
                                                Collections.sort(members);
                                            }
                                            else
                                            {
                                                // User has overridden the field, so update the type on their definition
                                                AbstractMemberMetaData overrideMmd = getMetaDataForMember(fieldName);
                                                overrideMmd.type = (Class) paramTypeArgs[j];
                                            }

                                            foundTypeForTypeVariable = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (!foundTypeForTypeVariable)
                            {
                                // Search bounds of this class for the field type variable (e.g "MyClass<D extends BaseD> extends MySuperClass<D>" will give bound of BaseD)
                                if (clsDeclTypes != null)
                                {
                                    for (int j=0;j<clsDeclTypes.length;j++)
                                    {
                                        if (clsDeclTypes[j].getName().equals(fieldTypeVar.getName()))
                                        {
                                            Type[] boundTypes = clsDeclTypes[j].getBounds();
                                            if (boundTypes != null && boundTypes.length == 1 && boundTypes[0] instanceof Class)
                                            {
                                                // User has class declaration like "public class MyClass<T extends SomeType>" so take SomeType
                                                boolean updateType = true;
                                                AbstractMemberMetaData overriddenMmd = getMetaDataForMember(fieldName);
                                                if (overriddenMmd != null)
                                                {
                                                    if (overriddenMmd.getTypeName().equals(((Class)boundTypes[0]).getName()))
                                                    {
                                                        // Already defined the type in a superclass, with same type as this bound so ignore
                                                        updateType = false;
                                                    }
                                                }

                                                if (updateType)
                                                {
                                                    NucleusLogger.METADATA.debug("Class=" + cls.getName() + " field=" + fieldName +
                                                        " declared to be " + fieldTypeVar + ", namely TypeVariable(" + j + ") with bound, so using bound of " + boundTypes[0]);
                                                    if (!memberNames.contains(fieldName))
                                                    {
                                                        // Field defined as generic but not found a meta-data definition, so use boundTypes to add override meta-data with the correct type
                                                        NucleusLogger.METADATA.debug(Localiser.msg("044060", fullName, fieldNameFull));

                                                        AbstractMemberMetaData mmd = new FieldMetaData(this, fieldNameFull);
                                                        mergeMemberMetaDataForOverrideOfType((Class)boundTypes[0], mmd, overriddenMmd);
                                                        members.add(mmd);

                                                        memberNames.add(mmd.getName());
                                                        Collections.sort(members);
                                                    }
                                                    else
                                                    {
                                                        // User has overridden the field, so update the type on their definition
                                                        AbstractMemberMetaData overrideMmd = getMetaDataForMember(fieldName);
                                                        overrideMmd.type = (Class) boundTypes[0];
                                                    }
                                                }
                                                foundTypeForTypeVariable = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            NucleusLogger.METADATA.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Method to merge in an overridden member details into the overriding meta-data, updating the type.
     * If the <cite>overriddenMmd</cite> is null then simply updates the type on the <cite>mmd</cite>.
     * @param type The type to use
     * @param mmd The overriding member meta-data
     * @param overriddenMmd The (base) overridden meta-data
     */
    private void mergeMemberMetaDataForOverrideOfType(Class type, AbstractMemberMetaData mmd, AbstractMemberMetaData overriddenMmd)
    {
        // TODO Use MetaDataMerger to merge in everything else specified in the member
        mmd.type = type;
        if (overriddenMmd != null)
        {
            mmd.primaryKey = overriddenMmd.primaryKey;
            mmd.embedded = overriddenMmd.embedded;
            mmd.serialized = overriddenMmd.serialized;
            mmd.persistenceModifier = overriddenMmd.persistenceModifier;
            mmd.valueStrategy = overriddenMmd.valueStrategy;
        }
    }

    /**
     * Populate MetaData for all members.
     * @param clr The ClassLoaderResolver
     * @param cls This class
     * @param pkMembers Process pk fields/properties (or non-PK if false)
     * @param primary the primary ClassLoader to use (or null)
     * @throws InvalidMetaDataException if the Class for a declared type in a field cannot be loaded by the <code>clr</code>
     * @throws InvalidMetaDataException if a field declared in the MetaData does not exist in the Class
     */
    protected void populateMemberMetaData(ClassLoaderResolver clr, Class cls, boolean pkMembers, ClassLoader primary)
    {
        Collections.sort(members);

        // Populate the real field values. This will populate any containers in these members also
        Iterator<AbstractMemberMetaData> memberIter = members.iterator();
        while (memberIter.hasNext())
        {
            AbstractMemberMetaData mmd = memberIter.next();
            if (pkMembers == mmd.isPrimaryKey())
            {
                Class fieldCls = cls;
                if (!mmd.fieldBelongsToClass())
                {
                    // Field overrides a field in a superclass, so find the class
                    try
                    {
                        fieldCls = clr.classForName(mmd.getClassName());
                    }
                    catch (ClassNotResolvedException cnre)
                    {
                        // Not found at specified location, so try the same package as this class
                        String fieldClassName = getPackageName() + "." + mmd.getClassName();
                        try
                        {
                            fieldCls = clr.classForName(fieldClassName);
                            mmd.setClassName(fieldClassName);
                        }
                        catch (ClassNotResolvedException cnre2)
                        {
                            NucleusLogger.METADATA.error(Localiser.msg("044092", fullName, mmd.getFullFieldName(), fieldClassName));
                            throw new InvalidClassMetaDataException("044092", fullName, mmd.getFullFieldName(), fieldClassName);
                        }
                    }
                }

                boolean populated = false;
                if (mmd instanceof PropertyMetaData)
                {
                    // User class must have a getter and setter for this property as per Java Beans
                    Method getMethod = null;
                    try
                    {
                        // Find the getter
                        // a). Try as a standard form of getter (getXXX)
                        getMethod = fieldCls.getDeclaredMethod(ClassUtils.getJavaBeanGetterName(mmd.getName(), false)); // Only public?
                    }
                    catch (Exception e)
                    {
                        try
                        {
                            // b). Try as a boolean form of getter (isXXX)
                            getMethod = fieldCls.getDeclaredMethod(ClassUtils.getJavaBeanGetterName(mmd.getName(), true)); // Only public?
                        }
                        catch (Exception e2)
                        {
                        }
                    }
                    if (getMethod == null && mmd.getPersistenceModifier() != FieldPersistenceModifier.NONE)
                    {
                        // Property is persistent yet no getter!
                        throw new InvalidClassMetaDataException("044073", fullName, mmd.getName());
                    }

                    Method setMethod = null;
                    try
                    {
                        // Find the setter
                        String setterName = ClassUtils.getJavaBeanSetterName(mmd.getName());
                        Method[] methods = fieldCls.getMethods(); // Only gives public methods
                        for (int i=0;i<methods.length;i++)
                        {
                            if (methods[i].getName().equals(setterName) && methods[i].getParameterTypes() != null && methods[i].getParameterTypes().length == 1)
                            {
                                setMethod = methods[i];
                                break;
                            }
                        }
                        if (setMethod == null)
                        {
                            methods = fieldCls.getDeclaredMethods(); // Also try protected/private of this class
                            for (int i=0;i<methods.length;i++)
                            {
                                if (methods[i].getName().equals(setterName) && methods[i].getParameterTypes() != null && methods[i].getParameterTypes().length == 1 && !methods[i].isBridge())
                                {
                                    setMethod = methods[i];
                                    break;
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                    }
                    if (setMethod == null && mmd.getPersistenceModifier() != FieldPersistenceModifier.NONE)
                    {
                        // Property is persistent yet no setter!
                        throw new InvalidClassMetaDataException("044074", fullName, mmd.getName());
                    }

                    // Populate the property using the getter
                    if (getMethod != null)
                    {
                        mmd.populate(clr, null, getMethod, primary, mmgr);
                        populated = true;
                    }
                }
                else
                {
                    Field clsField = null;
                    try
                    {
                        clsField = fieldCls.getDeclaredField(mmd.getName());
                    }
                    catch (Exception e)
                    {
                    }
                    if (clsField != null)
                    {
                        mmd.populate(clr, clsField, null, primary, mmgr);
                        populated = true;
                    }
                }
                if (!populated)
                {
                    // MetaData field doesn't exist in the class!
                    throw new InvalidClassMetaDataException("044071", fullName, mmd.getFullFieldName());
                }
            }
        }
    }
    
    /**
     * Method to initialise the object, creating internal convenience arrays.
     * Initialises all sub-objects. populate() should be called BEFORE calling this.
     * @param clr ClassLoader resolver
     */
    public synchronized void initialise(ClassLoaderResolver clr)
    {
        if (initialising || isInitialised())
        {
            return;
        }

        checkPopulated();

        try
        {
            initialising = true;

            if (pcSuperclassMetaData != null)
            {
                // We need our superclass to be initialised before us because we rely on information there
                if (!pcSuperclassMetaData.isInitialised())
                {
                    pcSuperclassMetaData.initialise(clr);
                }
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044076",fullName));
            }

            // Validate the objectid-class : this must be in initialise() since can be dependent on other classes being populated
            validateObjectIdClass(clr);

            // Count the fields/properties of the relevant category
            Iterator membersIter = members.iterator();
            int numManaged = 0;
            int numOverridden = 0;
            while (membersIter.hasNext())
            {
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)membersIter.next();
    
                // Initialise the FieldMetaData (and its sub-objects)
                mmd.initialise(clr);
                if (mmd.isFieldToBePersisted())
                {
                    if (mmd.fieldBelongsToClass())
                    {
                        numManaged++;
                    }
                    else
                    {
                        numOverridden++;
                    }
                }
            }

            // Generate the "managed members" list
            managedMembers = new AbstractMemberMetaData[numManaged];
            overriddenMembers = new AbstractMemberMetaData[numOverridden];

            membersIter = members.iterator();
            int memberId = 0;
            int overriddenMemberId = 0;
            memberPositionsByName = new HashMap();
            while (membersIter.hasNext())
            {
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)membersIter.next();
                if (mmd.isFieldToBePersisted())
                {
                    if (mmd.fieldBelongsToClass())
                    {
                        // Definition of a member in this class
                        mmd.setFieldId(memberId);
                        managedMembers[memberId] = mmd;
                        memberPositionsByName.put(mmd.getName(), Integer.valueOf(memberId));
                        memberId++;
                    }
                    else
                    {
                        // Definition of override of a member in a superclass
                        overriddenMembers[overriddenMemberId++] = mmd;
                        if (pcSuperclassMetaData == null)
                        {
                            // User specified override yet no superclass!
                            throw new InvalidClassMetaDataException("044162", fullName, mmd.getFullFieldName());
                        }
                        AbstractMemberMetaData superMmd = pcSuperclassMetaData.getMemberBeingOverridden(mmd.getName());
                        if (superMmd != null)
                        {
                            // Merge in any additional info not specified in the overridden field TODO Use MetaDataMerger to merge in better than this
                            if (superMmd.isPrimaryKey())
                            {
                                mmd.setPrimaryKey(true);
                            }
                        }
                        else
                        {
                            // TODO Catch this, illegal override
                        }
                    }
                }
            }
    
            if (pcSuperclassMetaData != null)
            {
                if (!pcSuperclassMetaData.isInitialised())
                {
                    pcSuperclassMetaData.initialise(clr);
                }
                noOfInheritedManagedMembers = pcSuperclassMetaData.getNoOfInheritedManagedMembers() + pcSuperclassMetaData.getNoOfManagedMembers();
            }
    
            // Set up the various convenience arrays of field numbers
            initialiseMemberPositionInformation();
    
            // Initialise any sub-objects
            if (implementations != null)
            {
                for (ImplementsMetaData implmd : implementations)
                {
                    implmd.initialise(clr);
                }
            }
            if (joins != null)
            {
                for (JoinMetaData joinmd : joins)
                {
                    joinmd.initialise(clr);
                }
            }
            if (foreignKeys != null)
            {
                for (ForeignKeyMetaData fkmd : foreignKeys)
                {
                    fkmd.initialise(clr);
                }
            }
            if (indexes != null)
            {
                for (IndexMetaData idxmd : indexes)
                {
                    idxmd.initialise(clr);
                }
            }
            if (uniqueConstraints != null)
            {
                for (UniqueMetaData unimd : uniqueConstraints)
                {
                    unimd.initialise(clr);
                }
            }
    
            if (fetchGroups != null)
            {
                fetchGroupMetaDataByName = new HashMap();
                for (FetchGroupMetaData fgmd : fetchGroups)
                {
                    fgmd.initialise(clr);
                    fetchGroupMetaDataByName.put(fgmd.getName(), fgmd);
                }
            }
            
            // If using datastore id and user hasn't provided the identity element,
            // add a defaulted one (using the superclass if available)
            if (identityType == IdentityType.DATASTORE && identityMetaData == null)
            {
                if (pcSuperclassMetaData != null)
                {
                    IdentityMetaData superImd = pcSuperclassMetaData.getIdentityMetaData();
                    identityMetaData = new IdentityMetaData();
                    identityMetaData.setColumnName(superImd.getColumnName());
                    identityMetaData.setValueStrategy(superImd.getValueStrategy());
                    identityMetaData.setSequence(superImd.getSequence());
                    identityMetaData.parent = this;
                }
                else
                {
                    identityMetaData = new IdentityMetaData();
                    identityMetaData.parent = this;
                }
            }
    
            if (primaryKeyMetaData != null)
            {
                primaryKeyMetaData.initialise(clr);
            }
            if (versionMetaData != null)
            {
                versionMetaData.initialise(clr);
            }
            if (identityMetaData != null)
            {
                identityMetaData.initialise(clr);
            }
            if (inheritanceMetaData != null)
            {
                inheritanceMetaData.initialise(clr);
            }

            if (identityType == IdentityType.APPLICATION)
            {
                usesSingleFieldIdentityClass = IdentityUtils.isSingleFieldIdentityClass(getObjectidClass());
            }

            setInitialised();
        }
        finally
        {
            initialising = false;

            mmgr.abstractClassMetaDataInitialised(this);
        }
    }

    /**
     * Whether the persistable class is abstract.
     * @return true if the persistable class is abstract
     */
    public boolean isAbstract()
    {
        return isAbstract;
    }   

    /**
     * Utility to add a defaulted FieldMetaData to the class. Provided as a method since then any derived 
     * classes can override it (e.g ClassMetaData can create a FieldMetaData)
     * @param name name of field
     * @return the new FieldMetaData
     */
    protected AbstractMemberMetaData newDefaultedProperty(String name)
    {
        return new FieldMetaData(this, name);
    }

    /**
     * Accessor for the implements MetaData
     * @return Returns the implements MetaData.
     */
    public final List<ImplementsMetaData> getImplementsMetaData()
    {
        return implementations;
    }

    /**
     * Method to add an implements to this class.
     * @param implmd Meta-Data for the implements
     */
    public void addImplements(ImplementsMetaData implmd)
    {
        if (implmd == null)
        {
            return;
        }

        if (isInitialised())
        {
            throw new RuntimeException("Already initialised");
        }

        if (implementations == null)
        {
            implementations = new ArrayList();
        }
        implementations.add(implmd);
        implmd.parent = this;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder(super.toString()).append(" [").append(this.getFullClassName()).append("]");
        if (identityType != null)
        {
            str.append(" identity=").append(identityType.toString());
            if (identityType == IdentityType.APPLICATION)
            {
                str.append("(").append(getNoOfPrimaryKeyMembers()).append(" pkFields, id=").append(objectidClass).append(")");
            }
        }
        str.append(", modifier=" + persistenceModifier);
        if (inheritanceMetaData != null && inheritanceMetaData.getStrategy() != null)
        {
            str.append(", inheritance=").append(inheritanceMetaData.getStrategy().toString());
        }
        if (isInitialised())
        {
            str.append(", managedMembers.size=").append(managedMembers.length);
            str.append(", overriddenMembers.size=").append(overriddenMembers.length);
            str.append("\n");
            str.append("    managed=[");
            for (int i=0;i<managedMembers.length;i++)
            {
                if (i != 0)
                {
                    str.append(",");
                }
                str.append(managedMembers[i] instanceof PropertyMetaData ? "Property(" : "Field(").append(managedMembers[i].getFullFieldName()).append(")");
            }
            str.append("]");
            str.append("\n");
            str.append("    overridden=[");
            for (int i=0;i<overriddenMembers.length;i++)
            {
                if (i != 0)
                {
                    str.append(",");
                }
                str.append(overriddenMembers[i] instanceof PropertyMetaData ? "Property(" : "Field(").append(overriddenMembers[i].getFullFieldName()).append(")");
            }
            str.append("]");
        }
        else
        {
            str.append(", members.size=").append(members.size());
        }
        return str.toString();
    }
}