/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved. 
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
2004 Andy Jefferson - toString(), MetaData, javadocs
2004 Andy Jefferson - nullIndicatorColumn/Value, ownerField
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * This element specifies the mapping for an embedded type. It contains multiple field elements, 
 * one for each field in the type.
 * <P>
 * The <B>null-indicator-column</B> optionally identifies the name of the column used to indicate 
 * whether the embedded instance is null. By default, if the value of this column is null, then the
 * embedded instance is null. This column might be mapped to a field of the embedded instance but 
 * might be a synthetic column for the sole purpose of indicating a null reference.
 * The <B>null-indicator-value</B> specifies the value to indicate that the embedded instance is null. 
 * This is only used for non-nullable columns.
 * If <B>null-indicator-column</B> is omitted, then the embedded instance is assumed always to exist.
 */
public class EmbeddedMetaData extends MetaData
{
    /** Name of the field/property in the embedded object that refers to the owner (bidirectional relation). */
    protected String ownerMember;

    /** Name of a column used for determining if the embedded object is null */
    protected String nullIndicatorColumn;

    /** Value in the null column indicating that the embedded object is null */
    protected String nullIndicatorValue;

    /** Discriminator for use when embedding objects with inheritance. */
    protected DiscriminatorMetaData discriminatorMetaData;

    /** Fields/properties of the embedded object. */
    protected final List members = new ArrayList();

    // -------------------------------------------------------------------------
    // Fields below here are not represented in the output MetaData. They are
    // for use internally in the operation of the JDO system. The majority are
    // for convenience to save iterating through the fields since the fields
    // are fixed once initialised.

    protected AbstractMemberMetaData fieldMetaData[];

    /**
     * Constructor to create a copy of the passed metadata.
     * @param embmd The metadata to copy
     */
    public EmbeddedMetaData(EmbeddedMetaData embmd)
    {
        super(null, embmd);
        this.ownerMember = embmd.ownerMember;
        this.nullIndicatorColumn = embmd.nullIndicatorColumn;
        this.nullIndicatorValue = embmd.nullIndicatorValue;
        for (int i=0;i<embmd.members.size();i++)
        {
            if (embmd.members.get(i) instanceof FieldMetaData)
            {
                addMember(new FieldMetaData(this,(AbstractMemberMetaData)embmd.members.get(i)));
            }
            else
            {
                addMember(new PropertyMetaData(this,(PropertyMetaData)embmd.members.get(i)));
            }
        }
    }

    /**
     * Default constructor. Use setters to set fields, before calling populate().
     */
    public EmbeddedMetaData()
    {
    }

    /**
     * Method to populate the embedded MetaData.
     * This performs checks on the validity of the field types for embedding.
     * @param clr The class loader to use where necessary
     * @param primary the primary ClassLoader to use (or null)
     * @param mmgr MetaData manager
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr)
    {
        // Find the class that the embedded fields apply to
        MetaData md = getParent();
        AbstractMemberMetaData apmd = null; // Field that has <embedded>
        AbstractClassMetaData embCmd = null; // Definition for the embedded class
        String embeddedType = null; // Name of the embedded type
        if (md instanceof AbstractMemberMetaData)
        {
            // PC embedded in PC object
            apmd = (AbstractMemberMetaData)md;
            embeddedType = apmd.getTypeName();
            embCmd = mmgr.getMetaDataForClassInternal(apmd.getType(), clr);
            if (embCmd == null && apmd.getFieldTypes() != null && apmd.getFieldTypes().length == 1)
            {
                // The specified field is not embeddable, nor is it persistent-interface, so try field-type for embedding
                embCmd = mmgr.getMetaDataForClassInternal(clr.classForName(apmd.getFieldTypes()[0]), clr);
            }
            if (embCmd == null)
            {
                NucleusLogger.METADATA.error(LOCALISER.msg("044121", apmd.getFullFieldName(), apmd.getTypeName()));
                throw new InvalidMemberMetaDataException(LOCALISER, "044121", apmd.getClassName(), apmd.getName(), 
                    apmd.getTypeName());
            }
        }
        else if (md instanceof ElementMetaData)
        {
            // PC element embedded in collection
            ElementMetaData elemmd = (ElementMetaData)md;
            apmd = (AbstractMemberMetaData)elemmd.getParent();
            embeddedType = apmd.getCollection().getElementType();
            try
            {
                Class cls = clr.classForName(embeddedType, primary);
                embCmd = mmgr.getMetaDataForClassInternal(cls, clr);
            }
            catch (ClassNotResolvedException cnre)
            {
                // Should be caught by populating the Collection
            }
            if (embCmd == null)
            {
                NucleusLogger.METADATA.error(LOCALISER.msg("044122", apmd.getFullFieldName(), embeddedType));
                throw new InvalidMemberMetaDataException(LOCALISER, "044122", apmd.getClassName(), apmd.getName(),
                    embeddedType);
            }
        }
        else if (md instanceof KeyMetaData)
        {
            // PC key embedded in Map
            KeyMetaData keymd = (KeyMetaData)md;
            apmd = (AbstractMemberMetaData)keymd.getParent();
            embeddedType = apmd.getMap().getKeyType();
            try
            {
                Class cls = clr.classForName(embeddedType, primary);
                embCmd = mmgr.getMetaDataForClassInternal(cls, clr);
            }
            catch (ClassNotResolvedException cnre)
            {
                // Should be caught by populating the Map
            }
            if (embCmd == null)
            {
                NucleusLogger.METADATA.error(LOCALISER.msg("044123", apmd.getFullFieldName(), embeddedType));
                throw new InvalidMemberMetaDataException(LOCALISER, "044123", apmd.getClassName(), apmd.getName(), 
                    embeddedType);
            }
        }
        else if (md instanceof ValueMetaData)
        {
            // PC value embedded in Map
            ValueMetaData valuemd = (ValueMetaData)md;
            apmd = (AbstractMemberMetaData)valuemd.getParent();
            embeddedType = apmd.getMap().getValueType();
            try
            {
                Class cls = clr.classForName(embeddedType, primary);
                embCmd = mmgr.getMetaDataForClassInternal(cls, clr);
            }
            catch (ClassNotResolvedException cnre)
            {
                // Should be caught by populating the Map
            }
            if (embCmd == null)
            {
                NucleusLogger.METADATA.error(LOCALISER.msg("044124", apmd.getFullFieldName(), embeddedType));
                throw new InvalidMemberMetaDataException(LOCALISER, "044124", apmd.getClassName(), apmd.getName(), 
                    embeddedType);
            }
        }

        // Check that all "members" are of the correct type for the embedded object
        Iterator memberIter = members.iterator();
        while (memberIter.hasNext())
        {
            Object fld = memberIter.next();
            // TODO Should allow PropertyMetaData here I think
            if (embCmd instanceof InterfaceMetaData && fld instanceof FieldMetaData)
            {
                // Cannot have a field within a persistent interface
                throw new InvalidMemberMetaDataException(LOCALISER, "044129", apmd.getClassName(), apmd.getName(),
                    ((AbstractMemberMetaData)fld).getName());
            }
        }

        // Add fields for the class that aren't in the <embedded> block using Reflection.
        // NOTE 1 : We ignore fields in superclasses
        // NOTE 2 : We ignore "enhanced" fields (added by the JDO enhancer)
        // NOTE 3 : We ignore inner class fields (containing "$") 
        // NOTE 4 : We sort the fields into ascending alphabetical order
        Class embeddedClass = null;
        Collections.sort(members);
        try
        {
            // Load the embedded class
            embeddedClass = clr.classForName(embeddedType, primary);

            // TODO Cater for properties in the populating class when the user defines using setters

            // Get all (reflected) fields in the populating class
            Field[] cls_fields=embeddedClass.getDeclaredFields();
            for (int i=0;i<cls_fields.length;i++)
            {
                // Limit to fields in this class, that aren't enhanced fields
                // that aren't inner class fields, and that aren't static
                if (cls_fields[i].getDeclaringClass().getName().equals(embeddedType) &&
                    !cls_fields[i].getName().startsWith("jdo") &&
                    !ClassUtils.isInnerClass(cls_fields[i].getName()) &&
                    !Modifier.isStatic(cls_fields[i].getModifiers()))
                {
                    // Find if there is a AbstractMemberMetaData for this field.
                    // This is possible as AbstractMemberMetaData implements Comparable
                    if (Collections.binarySearch(members, cls_fields[i].getName()) < 0)
                    {
                        // Start from the metadata of the field in the owning class if available
                        AbstractMemberMetaData embMmd = embCmd.getMetaDataForMember(cls_fields[i].getName());
                        FieldMetaData omittedFmd = null;
                        if (embMmd != null)
                        {
                            FieldPersistenceModifier fieldModifier = embMmd.getPersistenceModifier();
                            if (fieldModifier == FieldPersistenceModifier.DEFAULT)
                            {
                                // Modifier not yet set, so work it out
                                fieldModifier = embMmd.getDefaultFieldPersistenceModifier(cls_fields[i].getType(),
                                        cls_fields[i].getModifiers(), 
                                        mmgr.isFieldTypePersistable(cls_fields[i].getType()), mmgr);
                            }

                            if (fieldModifier == FieldPersistenceModifier.PERSISTENT)
                            {
                                // Only add if the owning class defines it as persistent
                                omittedFmd = new FieldMetaData(this, embMmd);
                                omittedFmd.setPrimaryKey(false); // Embedded field can't default to being part of PK, user has to set that
                            }
                        }
                        else
                        {
                            // No metadata defined, so add a default FieldMetaData for this field
                            omittedFmd = new FieldMetaData(this, cls_fields[i].getName());
                        }
                        if (omittedFmd != null)
                        {
                            NucleusLogger.METADATA.debug(LOCALISER.msg("044125", apmd.getClassName(), 
                                cls_fields[i].getName(), embeddedType));
                            members.add(omittedFmd);
                            Collections.sort(members);
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

        // add properties of interface, only if interface
        if (embCmd instanceof InterfaceMetaData)
        {
            try
            {
                // Get all (reflected) fields in the populating class
                Method[] clsMethods = embeddedClass.getDeclaredMethods();
                for (int i=0; i<clsMethods.length; i++)
                {
                    // Limit to methods in this class, that aren't enhanced fields
                    // that aren't inner class fields, and that aren't static
                    if (clsMethods[i].getDeclaringClass().getName().equals(embeddedType) &&
                        (clsMethods[i].getName().startsWith("get") || clsMethods[i].getName().startsWith("is")) &&
                        !ClassUtils.isInnerClass(clsMethods[i].getName()))
                    {
                        String fieldName = ClassUtils.getFieldNameForJavaBeanGetter(clsMethods[i].getName() );
                        // Find if there is a PropertyMetaData for this field.
                        // This is possible as PropertyMetaData implements Comparable
                        if (Collections.binarySearch(members,fieldName) < 0)
                        {
                            // Add a default PropertyMetaData for this field.
                            NucleusLogger.METADATA.debug(LOCALISER.msg("044060", apmd.getClassName(), fieldName));
                            PropertyMetaData pmd=new PropertyMetaData(this, fieldName);
                            members.add(pmd);
                            Collections.sort(members);
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
        Collections.sort(members);

        memberIter = members.iterator();
        while (memberIter.hasNext())
        {
            Class embFmdClass = embeddedClass;
            AbstractMemberMetaData fieldFmd = (AbstractMemberMetaData)memberIter.next();
            if (!fieldFmd.fieldBelongsToClass())
            {
                try
                {
                    embFmdClass = clr.classForName(fieldFmd.getClassName(true));
                }
                catch (ClassNotResolvedException cnre)
                {
                    // Maybe the user specified just "classBasicName.fieldName", so try with package name of this
                    String fieldClsName = embeddedClass.getPackage().getName() + "." + fieldFmd.getClassName(true);
                    fieldFmd.setClassName(fieldClsName);
                    embFmdClass = clr.classForName(fieldClsName);
                }
            }
            if (fieldFmd instanceof FieldMetaData)
            {
                Field cls_field = null;
                try
                {
                    cls_field = embFmdClass.getDeclaredField(fieldFmd.getName());
                }
                catch (Exception e)
                {
                    // MetaData field doesn't exist in the class!
                    throw new InvalidMemberMetaDataException(LOCALISER, "044071", embFmdClass.getName(), 
                        fieldFmd.getName());
                }
                fieldFmd.populate(clr, cls_field, null, primary, mmgr);
            }
            else
            {
                Method cls_method = null;
                try
                {
                    cls_method = embFmdClass.getDeclaredMethod(
                        ClassUtils.getJavaBeanGetterName(fieldFmd.getName(),true));
                }
                catch(Exception e)
                {
                    try
                    {
                        cls_method = embFmdClass.getDeclaredMethod(
                            ClassUtils.getJavaBeanGetterName(fieldFmd.getName(),false));
                    }
                    catch (Exception e2)
                    {
                        // MetaData field doesn't exist in the class!
                        throw new InvalidMemberMetaDataException(LOCALISER, "044071", embFmdClass.getName(), 
                            fieldFmd.getName());
                    }
                }
                fieldFmd.populate(clr, null, cls_method, primary, mmgr);
            }
        }
    }

    /**
     * Method to initialise the object, creating all internal convenience
     * arrays.
     */
    public void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        fieldMetaData = new AbstractMemberMetaData[members.size()];
        for (int i=0; i<fieldMetaData.length; i++)
        {
            fieldMetaData[i] = (AbstractMemberMetaData) members.get(i);
            fieldMetaData[i].initialise(clr, mmgr);
        }

        if (discriminatorMetaData != null)
        {
            discriminatorMetaData.initialise(clr, mmgr);
        }

        setInitialised();
    }

    /**
     * Accessor for metadata for the embedded members.
     * @return Returns the metadata for any defined members.
     */
    public final AbstractMemberMetaData[] getMemberMetaData()
    {
        return fieldMetaData;
    }

    public final String getOwnerMember()
    {
        return ownerMember;
    }

    public EmbeddedMetaData setOwnerMember(String ownerMember)
    {
        this.ownerMember = (StringUtils.isWhitespace(ownerMember) ? null : ownerMember);
        return this;
    }

    public final String getNullIndicatorColumn()
    {
        return nullIndicatorColumn;
    }

    public EmbeddedMetaData setNullIndicatorColumn(String column)
    {
        this.nullIndicatorColumn = (StringUtils.isWhitespace(column) ? null : column);
        return this;
    }

    public final String getNullIndicatorValue()
    {
        return nullIndicatorValue;
    }

    public EmbeddedMetaData setNullIndicatorValue(String value)
    {
        this.nullIndicatorValue = (StringUtils.isWhitespace(value) ? null : value);
        return this;
    }

    public final DiscriminatorMetaData getDiscriminatorMetaData()
    {
        return discriminatorMetaData;
    }

    public EmbeddedMetaData setDiscriminatorMetaData(DiscriminatorMetaData dismd)
    {
        this.discriminatorMetaData = dismd;
        this.discriminatorMetaData.parent = this;
        return this;
    }

    /**
     * Method to create a new discriminator metadata, assign it to this inheritance, and return it.
     * @return The discriminator metadata
     */
    public DiscriminatorMetaData newDiscriminatorMetadata()
    {
        DiscriminatorMetaData dismd = new DiscriminatorMetaData();
        setDiscriminatorMetaData(dismd);
        return dismd;
    }

    /**
     * Method to add a field/property to the embedded definition.
     * Rejects the addition of duplicate named fields/properties.
     * @param mmd Meta-Data for the field/property.
     */
    public void addMember(AbstractMemberMetaData mmd)
    {
        if (mmd == null)
        {
            return;
        }

        if (isInitialised())
        {
            throw new InvalidMemberMetaDataException(LOCALISER, "044108", mmd.getClassName(), mmd.getName());
        }
        Iterator iter = members.iterator();
        while (iter.hasNext())
        {
            AbstractMemberMetaData md = (AbstractMemberMetaData)iter.next();
            if (mmd.getName().equals(md.getName()))
            {
                throw new InvalidMemberMetaDataException(LOCALISER, "044112", mmd.getClassName(), mmd.getName());
            }
        }
        members.add(mmd);
        mmd.parent = this;
    }

    /**
     * Method to create a new FieldMetaData, add it, and return it.
     * @param name Name of the field
     * @return The FieldMetaData
     */
    public FieldMetaData newFieldMetaData(String name)
    {
        FieldMetaData fmd = new FieldMetaData(this, name);
        addMember(fmd);
        return fmd;
    }

    /**
     * Method to create a new PropertyMetaData, add it, and return it.
     * @param name Name of the property
     * @return The PropertyMetaData
     */
    public PropertyMetaData newPropertyMetaData(String name)
    {
        PropertyMetaData pmd = new PropertyMetaData(this, name);
        addMember(pmd);
        return pmd;
    }

    // ------------------------------- Utilities -------------------------------

    /**
     * Returns a string representation of the object using a prefix
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        // Field needs outputting so generate metadata
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<embedded");
        if (ownerMember != null)
        {
            sb.append(" owner-field=\"" + ownerMember + "\"");
        }
        if (nullIndicatorColumn != null)
        {
            sb.append(" null-indicator-column=\"" + nullIndicatorColumn + "\"");
        }
        if (nullIndicatorValue != null)
        {
            sb.append(" null-indicator-value=\"" + nullIndicatorValue + "\"");
        }
        sb.append(">\n");

        if (discriminatorMetaData != null)
        {
            sb.append(discriminatorMetaData.toString(prefix+indent, indent));
        }

        // Add fields
        for (int i=0; i<members.size(); i++)
        {
            AbstractMemberMetaData f = (AbstractMemberMetaData)members.get(i);
            sb.append(f.toString(prefix + indent,indent));
        }
        
        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix + "</embedded>\n");
        return sb.toString();
    }
}