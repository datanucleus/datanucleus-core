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
2007 Xuan Baldauf - little reduction in code duplication to anticipate changes regarding issue http://www.jpox.org/servlet/jira/browse/CORE-3272
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * Representation of the MetaData of a "persistent-interface".
 */
public class InterfaceMetaData extends AbstractClassMetaData
{
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
     * Method to initialise the object, creating internal convenience arrays.
     * Initialises all sub-objects. If populate() is going to be used it should
     * be used BEFORE calling this method.
     * @param clr ClassLoader resolver
     * @param mmgr MetaData manager
     */
    public synchronized void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
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
                    pcSuperclassMetaData.initialise(clr, mmgr);
                }
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(LOCALISER.msg("044076",fullName));
            }

            // Validate the objectid-class
            // This must be in initialise() since can be dependent on other classes being populated
            validateObjectIdClass(clr, mmgr);

            // Count the fields of the relevant category
            Iterator fields_iter = members.iterator();
            int no_of_managed_fields = 0;
            int no_of_overridden_fields = 0;
            while (fields_iter.hasNext())
            {
                AbstractMemberMetaData fmd = (AbstractMemberMetaData)fields_iter.next();

                // Initialise the AbstractMemberMetaData (and its sub-objects)
                fmd.initialise(clr, mmgr);
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
                AbstractMemberMetaData fmd = (AbstractMemberMetaData)fields_iter.next();

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
                    pcSuperclassMetaData.initialise(clr, mmgr);
                }
                noOfInheritedManagedMembers = pcSuperclassMetaData.getNoOfInheritedManagedMembers() + pcSuperclassMetaData.getNoOfManagedMembers();
            }

            // Set up the various convenience arrays of field numbers
            initialiseMemberPositionInformation(mmgr);

            joinMetaData = new JoinMetaData[joins.size()];
            for (int i=0; i<joinMetaData.length; i++)
            {
                joinMetaData[i] = joins.get(i);
                joinMetaData[i].initialise(clr, mmgr);
            }

            indexMetaData = new IndexMetaData[indexes.size()];
            for (int i=0; i<indexMetaData.length; i++)
            {
                indexMetaData[i] = indexes.get(i);
            }
            foreignKeyMetaData = new ForeignKeyMetaData[foreignKeys.size()];
            for (int i=0; i<foreignKeyMetaData.length; i++)
            {
                foreignKeyMetaData[i] = foreignKeys.get(i);
            }
            uniqueMetaData = new UniqueMetaData[uniqueConstraints.size()];
            for (int i=0; i<uniqueMetaData.length; i++)
            {
                uniqueMetaData[i] = uniqueConstraints.get(i);
            }

            if (fetchGroups != null)
            {
                fetchGroupMetaDataByName = new HashMap();
                for (FetchGroupMetaData fgmd : fetchGroups)
                {
                    fgmd.initialise(clr, mmgr);
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
                versionMetaData.initialise(clr, mmgr);
            }
            if (identityMetaData != null)
            {
                identityMetaData.initialise(clr, mmgr);
            }
            if (inheritanceMetaData != null)
            {
                inheritanceMetaData.initialise(clr, mmgr);
            }

            if (identityType == IdentityType.APPLICATION)
            {
                usesSingleFieldIdentityClass = IdentityUtils.isSingleFieldIdentityClass(getObjectidClass());
            }

            // Clear out the collections that we used when loading the MetaData
            joins.clear();
            joins = null;
            foreignKeys.clear();
            foreignKeys = null;
            indexes.clear();
            indexes = null;
            uniqueConstraints.clear();
            uniqueConstraints = null;
            setInitialised();
        }
        finally
        {
            initialising = false;

            mmgr.abstractClassMetaDataInitialised(this);
        }
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
     * add AbstractMemberMetaData for all fields in this class providing defaults for
     * all of these.
     *
     * @param clr ClassLoaderResolver to use in loading any classes
     * @param primary the primary ClassLoader to use (or null)
     * @param mmgr MetaData manager
     */
    public synchronized void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr)
    {
        if (isInitialised() || isPopulated())
        {
            NucleusLogger.METADATA.error(LOCALISER.msg("044068", name));
            throw new NucleusException(LOCALISER.msg("044068", fullName)).setFatal();
        }
        if (populating)
        {
            return;
        }

        try
        {
            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(LOCALISER.msg("044075", fullName));
            }
            populating = true;

            Class cls = loadClass(clr, primary, mmgr);

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
                throw new InvalidClassMetaDataException(LOCALISER, "044063", fullName);
            }

            determineSuperClassName(clr, cls, mmgr);

            inheritIdentity();
            determineIdentity();
            validateUserInputForIdentity();

            addMetaDataForMembersNotInMetaData(cls);

            // Set inheritance
            validateUserInputForInheritanceMetaData(false);
            determineInheritanceMetaData(mmgr);
            applyDefaultDiscriminatorValueWhenNotSpecified(mmgr);

            if (objectidClass == null)
            {
                // No user-defined objectid-class but potentially have SingleFieldIdentity so make sure PK fields are set
                populatePropertyMetaData(clr, cls, true, primary, mmgr); // PK fields
                determineObjectIdClass(mmgr);
                populatePropertyMetaData(clr, cls, false, primary, mmgr); // Non-PK fields
            }
            else
            {
                populatePropertyMetaData(clr, cls, true, primary, mmgr);
                populatePropertyMetaData(clr, cls, false, primary, mmgr);
                determineObjectIdClass(mmgr);
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
     * @param mmgr MetaData manager
     * @throws InvalidMetaDataException if the Class for a declared type in a field cannot be loaded by the <code>clr</code>
     * @throws InvalidMetaDataException if a field declared in the MetaData does not exist in the Class
     */
    protected void populatePropertyMetaData(ClassLoaderResolver clr, Class cls, boolean pkFields, 
            ClassLoader primary, MetaDataManager mmgr)
    {
        Collections.sort(members);

        // Populate the AbstractMemberMetaData with their real field values
        // This will populate any containers in these fields also
        Iterator fields_iter = members.iterator();
        while (fields_iter.hasNext())
        {
            AbstractMemberMetaData fmd=(AbstractMemberMetaData)fields_iter.next();
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
                            NucleusLogger.METADATA.error(LOCALISER.msg("044080", fieldClassName));
                            throw new InvalidClassMetaDataException(LOCALISER, "044080", fullName, fieldClassName);
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
                        throw new InvalidClassMetaDataException(LOCALISER, "044072", fullName, fmd.getFullFieldName());
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
        // Add MetaData for properties for the interface that aren't in the XML/annotations.
        // We use Reflection here since JDOImplHelper would only give use info
        // for enhanced files (and the enhancer needs unenhanced as well). 
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
                    !Modifier.isStatic(clsMethods[i].getModifiers()))
                {
                    // Find if there is metadata for this property
                    String memberName = ClassUtils.getFieldNameForJavaBeanGetter(clsMethods[i].getName());
                    // TODO : This will not check if the name is a property! so we can miss field+property clashes
                    if (Collections.binarySearch(members, memberName) < 0)
                    {
                        // Check if a setter is also present
                        String setterName = ClassUtils.getJavaBeanSetterName(memberName);
                        for (int j=0;j<clsMethods.length;j++)
                        {
                            if (clsMethods[j].getName().equals(setterName))
                            {
                                // Getter/Setter for a property but not in MetaData so add
                                NucleusLogger.METADATA.debug(LOCALISER.msg("044060", fullName, memberName));
                                AbstractMemberMetaData mmd = newDefaultedProperty(memberName);
                                members.add(mmd);
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

    // ------------------------------ Utilities --------------------------------

    /**
     * Returns a string representation of the object.
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<interface name=\"" + name + "\"\n");
        if (identityType != null)
        {
            sb.append(prefix).append("       identity-type=\"" + identityType + "\"\n");
        }
        if (objectidClass != null)
        {
            sb.append(prefix).append("       objectid-class=\"" + objectidClass + "\"\n");
        }
        if (!requiresExtent)
        {
            sb.append(prefix).append("       requires-extent=\"false\"");
        }
        if (embeddedOnly)
        {
            sb.append(prefix).append("       embedded-only=\"true\"\n");
        }
        if (detachable)
        {
            sb.append(prefix).append("       detachable=\"true\"\n");
        }
        if (table != null)
        {
            sb.append(prefix).append("       table=\"" + table + "\"\n");
        }
        sb.append(">\n");

        // Identity
        if (identityMetaData != null)
        {
            sb.append(identityMetaData.toString(prefix + indent, indent));
        }

        // PrimaryKey
        if (primaryKeyMetaData != null)
        {
            sb.append(primaryKeyMetaData.toString(prefix + indent,indent));
        }

        // Inheritance
        if (inheritanceMetaData != null)
        {
            sb.append(inheritanceMetaData.toString(prefix + indent, indent));
        }

        // Version
        if (versionMetaData != null)
        {
            sb.append(versionMetaData.toString(prefix + indent, indent));
        }

        // Add joins
        if (joins != null)
        {
            for (int i=0; i<joins.size(); i++)
            {
                JoinMetaData jmd = joins.get(i);
                sb.append(jmd.toString(prefix + indent, indent));
            }
        }

        // Add foreign-keys
        if (foreignKeys != null)
        {
            for (int i=0; i<foreignKeys.size(); i++)
            {
                ForeignKeyMetaData fkmd = foreignKeys.get(i);
                sb.append(fkmd.toString(prefix + indent, indent));
            }
        }
        
        // Add indexes
        if (indexes != null)
        {
            for (int i=0; i<indexes.size(); i++)
            {
                IndexMetaData imd = indexes.get(i);
                sb.append(imd.toString(prefix + indent, indent));
            }
        }
        
        // Add unique constraints
        if (uniqueConstraints != null)
        {
            for (int i=0; i<uniqueConstraints.size(); i++)
            {
                UniqueMetaData unimd = uniqueConstraints.get(i);
                sb.append(unimd.toString(prefix + indent, indent));
            }
        }
        
        // Add properties
        if (members != null)
        {
            for (int i=0;i<members.size();i++)
            {
                PropertyMetaData pmd = (PropertyMetaData)members.get(i);
                sb.append(pmd.toString(prefix + indent, indent));
            }
        }
        
        // Add queries
        if (queries != null)
        {
            Iterator iter = queries.iterator();
            while (iter.hasNext())
            {
                QueryMetaData q = (QueryMetaData)iter.next();
                sb.append(q.toString(prefix + indent,indent));
            }
        }
        
        // Add fetch-groups
        if (fetchGroups != null)
        {
            for (FetchGroupMetaData fgmd : fetchGroups)
            {
                sb.append(fgmd.toString(prefix + indent, indent));
            }
        }

        // Add extensions
        sb.append(super.toString(prefix + indent, indent)); 

        sb.append(prefix + "</interface>\n");
        return sb.toString();
    }
}