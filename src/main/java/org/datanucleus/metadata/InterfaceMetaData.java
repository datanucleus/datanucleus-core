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
2007 Xuan Baldauf - little reduction in code duplication to anticipate changes regarding issue
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Representation of the MetaData of a "persistent-interface".
 */
public class InterfaceMetaData extends AbstractClassMetaData
{
    private static final long serialVersionUID = -7719837155678222822L;

    /**
     * Constructor.
     * Takes the basic string information found in the MetaData file.
     * @param parent The package to which this class belongs
     * @param name Name of class
     */
    public InterfaceMetaData(final PackageMetaData parent, final String name)
    {
        super(parent, name);
    }

    /**
     * Method to provide the details of the class being represented by this
     * MetaData. This can be used to firstly provide defaults for attributes
     * that aren't specified in the MetaData, and secondly to report any errors
     * with attributes that have been specified that are inconsistent with the
     * class being represented.
     * <P>
     * One possible use of this method would be to take a basic ClassMetaData
     * for a class and call this, passing in the users class. This would then
     * add AbstractMemberMetaData for all fields in this class providing defaults for all of these.
     *
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

            // Load any Annotations definition for this class
            if (!isMetaDataComplete())
            {
                mmgr.addAnnotationsDataToClass(cls, this, clr);
            }

            // Load any ORM definition for this class
            mmgr.addORMDataToClass(cls, clr);

            // If a class is an inner class and is non-static it is invalid
            if (ClassUtils.isInnerClass(fullName) && !Modifier.isStatic(cls.getModifiers()) &&
                persistenceModifier == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                throw new InvalidClassMetaDataException("044063", fullName);
            }

            determineSuperClassName(clr, cls);

            inheritIdentity();
            determineIdentity();
            validateUserInputForIdentity();

            addMetaDataForMembersNotInMetaData(cls);

            // Set inheritance
            validateUserInputForInheritanceMetaData(false);
            determineInheritanceMetaData();
            applyDefaultDiscriminatorValueWhenNotSpecified();

            if (objectidClass == null)
            {
                // No user-defined objectid-class but potentially have SingleFieldIdentity so make sure PK fields are set
                populatePropertyMetaData(clr, cls, true, primary); // PK fields
                determineObjectIdClass();
                populatePropertyMetaData(clr, cls, false, primary); // Non-PK fields
            }
            else
            {
                populatePropertyMetaData(clr, cls, true, primary);
                populatePropertyMetaData(clr, cls, false, primary);
                determineObjectIdClass();
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
     * Method to initialise the object, creating internal convenience arrays.
     * Initialises all sub-objects. 
     * If populate() is going to be used it should be used BEFORE calling this method.
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

            // Validate the objectid-class
            // This must be in initialise() since can be dependent on other classes being populated
            validateObjectIdClass(clr);

            // Count the fields of the relevant category
            Iterator<AbstractMemberMetaData> fields_iter = members.iterator();
            int no_of_managed_fields = 0;
            int no_of_overridden_fields = 0;
            while (fields_iter.hasNext())
            {
                AbstractMemberMetaData fmd = fields_iter.next();

                // Initialise the AbstractMemberMetaData (and its sub-objects)
                fmd.initialise(clr);
                if (fmd.isFieldToBePersisted())
                {
                    if (fmd.fieldBelongsToClass())
                    {
                        no_of_managed_fields++;
                    }
                    else
                    {
                        no_of_overridden_fields++;
                    }
                }
            }

            // Generate the "managed fields" list
            managedMembers = new AbstractMemberMetaData[no_of_managed_fields];
            overriddenMembers = new AbstractMemberMetaData[no_of_overridden_fields];

            fields_iter = members.iterator();
            int field_id = 0;
            int overridden_field_id = 0;
            memberPositionsByName = new HashMap();
            while (fields_iter.hasNext())
            {
                AbstractMemberMetaData fmd = fields_iter.next();

                if (fmd.isFieldToBePersisted())
                {
                    if (fmd.fieldBelongsToClass())
                    {
                        fmd.setFieldId(field_id);
                        managedMembers[field_id] = fmd;
                        memberPositionsByName.put(fmd.getName(), Integer.valueOf(field_id));
                        field_id++;
                    }
                    else
                    {
                        overriddenMembers[overridden_field_id++] = fmd;
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
                }
                else
                {
                    identityMetaData = new IdentityMetaData();
                }
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
            if (multitenancyMetaData != null)
            {
                multitenancyMetaData.initialise(clr);
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
     * Utility to add a defaulted PropertyMetaData to the class. 
     * Provided as a method since then any derived classes can override it.
     * @param name name of field
     * @return the new PropertyMetaData
     */
    protected AbstractMemberMetaData newDefaultedProperty(String name)
    {
        return new PropertyMetaData(this, name);
    }

    /**
     * Populate PropertyMetaData.
     * @param clr The ClassLoader
     * @param cls This class
     * @param pkFields Process pk fields (or non-PK fields if false)
     * @param primary the primary ClassLoader to use (or null)
     * @throws InvalidMetaDataException if the Class for a declared type in a field cannot be loaded by the <code>clr</code>
     * @throws InvalidMetaDataException if a field declared in the MetaData does not exist in the Class
     */
    protected void populatePropertyMetaData(ClassLoaderResolver clr, Class cls, boolean pkFields, ClassLoader primary)
    {
        Collections.sort(members);

        // Populate the AbstractMemberMetaData with their real field values
        // This will populate any containers in these fields also
        Iterator<AbstractMemberMetaData> fields_iter = members.iterator();
        while (fields_iter.hasNext())
        {
            AbstractMemberMetaData fmd = fields_iter.next();
            if (pkFields == fmd.isPrimaryKey())
            {
                Class fieldCls = cls;
                if (!fmd.fieldBelongsToClass())
                {
                    // Field overrides a field in a superclass, so find the class
                    try
                    {
                        fieldCls = clr.classForName(fmd.getClassName(), primary);
                    }
                    catch (ClassNotResolvedException cnre)
                    {
                        // Not found at specified location, so try the same package as this class
                        String fieldClassName = getPackageName() + "." + fmd.getClassName();
                        try
                        {
                            fieldCls = clr.classForName(fieldClassName, primary);
                            fmd.setClassName(fieldClassName);
                        }
                        catch (ClassNotResolvedException cnre2)
                        {
                            NucleusLogger.METADATA.error(Localiser.msg("044080", fieldClassName));
                            throw new InvalidClassMetaDataException("044080", fullName, fieldClassName);
                        }
                    }
                }

                Method cls_method = null;
                try
                {
                    cls_method = fieldCls.getDeclaredMethod(ClassUtils.getJavaBeanGetterName(fmd.getName(),true));
                }
                catch (Exception e)
                {
                    try 
                    {
                        cls_method = fieldCls.getDeclaredMethod(ClassUtils.getJavaBeanGetterName(fmd.getName(),false));
                    }
                    catch (Exception e2)
                    {
                        // MetaData method doesn't exist in the class!
                        throw new InvalidClassMetaDataException("044072", fullName, fmd.getFullFieldName());
                    }
                }
                fmd.populate(clr, null, cls_method, primary, mmgr);
            }
        }
    }

    /**
     * Add MetaData for properties of the interface not declared in MetaData.
     * @param cls Class represented by this metadata
     */
    protected void addMetaDataForMembersNotInMetaData(Class cls)
    {
        Set<String> memberNames = new HashSet<>();
        for (AbstractMemberMetaData mmd : members)
        {
            memberNames.add(mmd.getName());
        }

        // Add MetaData for properties for the interface that aren't in the XML/annotations, using Reflection.
        // NOTE 1 : We ignore properties in superclasses
        // NOTE 2 : We ignore "enhanced" properties (added by the enhancer)
        // NOTE 3 : We ignore inner class fields (containing "$") 
        // NOTE 4 : We sort the properties into ascending alphabetical order
        Collections.sort(members);
        try
        {
            // Get all (reflected) methods in the populating class
            Method[] clsMethods = cls.getDeclaredMethods();
            for (int i=0; i<clsMethods.length; i++)
            {
                // Limit to getter methods in this class, that aren't inner class methods, and that aren't static
                if (clsMethods[i].getDeclaringClass().getName().equals(fullName) &&
                    (clsMethods[i].getName().startsWith("get") || clsMethods[i].getName().startsWith("is")) &&
                    !ClassUtils.isInnerClass(clsMethods[i].getName()) &&
                    !clsMethods[i].isBridge() &&
                    !Modifier.isStatic(clsMethods[i].getModifiers()))
                {
                    // Find if there is metadata for this property
                    String memberName = ClassUtils.getFieldNameForJavaBeanGetter(clsMethods[i].getName());
                    // TODO : This will not check if the name is a property! so we can miss field+property clashes
                    if (!memberNames.contains(memberName))
                    {
                        // Check if a setter is also present
                        String setterName = ClassUtils.getJavaBeanSetterName(memberName);
                        for (int j=0;j<clsMethods.length;j++)
                        {
                            if (clsMethods[j].getName().equals(setterName))
                            {
                                // Getter/Setter for a property but not in MetaData so add
                                NucleusLogger.METADATA.debug(Localiser.msg("044060", fullName, memberName));
                                AbstractMemberMetaData mmd = newDefaultedProperty(memberName);
                                members.add(mmd);
                                memberNames.add(mmd.getName());
                                Collections.sort(members);
                                break;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            NucleusLogger.METADATA.error(e.getMessage(), e);
            throw new NucleusUserException(e.getMessage());
        }
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