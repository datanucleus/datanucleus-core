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
2007 Xuan Baldauf - Support for the ability of users to explictly state that an array whose component type is not persistable may still have persistable elements.
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Representation of the Meta-Data for an Array.
 */
public class ArrayMetaData extends ContainerMetaData
{
    private static final long serialVersionUID = -6475718222404272345L;

    /** Representation of the element of the array. */
    protected ContainerComponent element;
    
    /** wether this array may contain persistable elements */
    protected boolean mayContainPersistableElements;

    /**
     * Constructor to create a copy of the passed metadata.
     * @param arrmd The metadata to copy
     */
    public ArrayMetaData(ArrayMetaData arrmd)
    {
        super(arrmd);
        element = new ContainerComponent();
        element.embedded = arrmd.element.embedded;
        element.serialized = arrmd.element.serialized;
        element.dependent = arrmd.element.dependent;
        element.typeName = arrmd.element.typeName;
        element.classMetaData = arrmd.element.classMetaData;
    }

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public ArrayMetaData()
    {
        element = new ContainerComponent();
    }

    /**
     * Method to populate any defaults, and check the validity of the MetaData.
     * @param clr ClassLoaderResolver to use in loading any classes 
     * @param primary the primary ClassLoader to use (or null)
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary)
    {
        AbstractMemberMetaData mmd = (AbstractMemberMetaData)parent;
        if (!StringUtils.isWhitespace(element.typeName) && element.typeName.indexOf(',') > 0)
        {
            throw new InvalidMemberMetaDataException("044140", mmd.getClassName(), mmd.getName());
        }

        // Make sure the type in "element" is set
        element.populate(mmd.getAbstractClassMetaData().getPackageName(), clr, primary);

        // Check the field type and see if it is an array type
        Class fieldType = mmd.getType();
        if (!fieldType.isArray())
        {
            throw new InvalidMemberMetaDataException("044141", mmd.getClassName(), getMemberName());
        }
        Class componentType = fieldType.getComponentType();

        // "embedded-element"
        MetaDataManager mmgr = mmd.getMetaDataManager();
        if (element.embedded == null)
        {
            // Assign default for "embedded-element" based on 18.13.1 of JDO 2 spec
            if (mmgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(componentType))
            {
                element.embedded = Boolean.TRUE;
            }
            else
            {
                // Use "readMetaDataForClass" in case we havent yet initialised the metadata for the element
                AbstractClassMetaData elemCmd = mmgr.readMetaDataForClass(componentType.getName());
                if (elemCmd == null)
                {
                    // Try to load it just in case using annotations and only pulled in one side of the relation
                    try
                    {
                        elemCmd = mmgr.getMetaDataForClass(componentType, clr);
                    }
                    catch (Throwable thr)
                    {
                    }
                }
                if (elemCmd != null)
                {
                    element.embedded = (elemCmd.isEmbeddedOnly() ? Boolean.TRUE : Boolean.FALSE);
                }
                else if (componentType.isInterface() || componentType == Object.class)
                {
                    // Collection<interface> or Object not explicitly marked as embedded defaults to false
                    element.embedded = Boolean.FALSE;
                }
                else
                {
                    // Fallback to true
                    NucleusLogger.METADATA.debug("Member with collection of elementType=" + componentType.getName()+
                        " not explicitly marked as embedded, so defaulting to embedded since not persistable");
                    element.embedded = Boolean.TRUE;
                }
            }
        }
        if (Boolean.FALSE.equals(element.embedded))
        {
            // Use "readMetaDataForClass" in case we havent yet initialised the metadata for the element
            AbstractClassMetaData elemCmd = mmgr.readMetaDataForClass(componentType.getName());
            if (elemCmd == null && !componentType.isInterface() && componentType != java.lang.Object.class)
            {
                // If the user has set a non-PC/non-Interface as not embedded, correct it since not supported.
                // Note : this fails when using in the enhancer since not yet PC
                NucleusLogger.METADATA.debug("Member with array of element type " + componentType.getName() +
                    " marked as not embedded, but only persistable as embedded, so resetting");
                element.embedded = Boolean.TRUE;
            }
        }

        if (!mmgr.isEnhancing() && !getMemberMetaData().isSerialized())
        {
            // Catch situations that we don't support
            if (mmd.getJoinMetaData() == null && !mmgr.getApiAdapter().isPersistable(componentType) && mmgr.supportsORM())
            {
                // We only support persisting particular array types as byte-streams (non-Java-serialised)
                String arrayComponentType = componentType.getName();
                if (!arrayComponentType.equals(ClassNameConstants.BOOLEAN) &&
                    !arrayComponentType.equals(ClassNameConstants.BYTE) &&
                    !arrayComponentType.equals(ClassNameConstants.CHAR) &&
                    !arrayComponentType.equals(ClassNameConstants.DOUBLE) &&
                    !arrayComponentType.equals(ClassNameConstants.FLOAT) &&
                    !arrayComponentType.equals(ClassNameConstants.INT) &&
                    !arrayComponentType.equals(ClassNameConstants.LONG) &&
                    !arrayComponentType.equals(ClassNameConstants.SHORT) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_LANG_BOOLEAN) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_LANG_BYTE) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_LANG_CHARACTER) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_LANG_DOUBLE) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_LANG_FLOAT) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_LANG_INTEGER) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_LANG_LONG) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_LANG_SHORT) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_MATH_BIGDECIMAL) &&
                    !arrayComponentType.equals(ClassNameConstants.JAVA_MATH_BIGINTEGER))
                {
                    // Impossible to persist an array of a non-PC element without a join table or without serialising the array
                    // TODO Should this be an exception?
                    String msg = Localiser.msg("044142", mmd.getClassName(), getMemberName(), arrayComponentType);
                    NucleusLogger.METADATA.warn(msg);
                }
            }
        }

        // Keep a reference to the MetaData for the element
        if (element.typeName != null)
        {
            Class elementCls = clr.classForName(element.typeName, primary);
            if (mmgr.getApiAdapter().isPersistable(elementCls))
            {
                mayContainPersistableElements = true;
            }
            element.classMetaData = mmgr.getMetaDataForClassInternal(elementCls, clr);
        }
        else
        {
            element.typeName = componentType.getName();
            element.classMetaData = mmgr.getMetaDataForClassInternal(componentType, clr);
        }

        if (element.classMetaData != null)
        {
            mayContainPersistableElements = true;
        }

        if (hasExtension(MetaData.EXTENSION_MEMBER_IMPLEMENTATION_CLASSES))
        {
            // Check/fix the validity of the implementation-classes and qualify them where required.
            StringBuilder str = new StringBuilder();
            String[] implTypes = getValuesForExtension(MetaData.EXTENSION_MEMBER_IMPLEMENTATION_CLASSES);
            for (int i=0;i<implTypes.length;i++)
            {
                String implTypeName = ClassUtils.createFullClassName(mmd.getPackageName(), implTypes[i]);
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
                        throw new InvalidMemberMetaDataException("044116", mmd.getClassName(), mmd.getName(), implTypes[i]);
                    }
                }
            }
            addExtension(MetaData.EXTENSION_MEMBER_IMPLEMENTATION_CLASSES, str.toString()); // Replace with this new value
        }

        // Make sure anything in the superclass is populated too
        super.populate();

        setPopulated();
    }

    public boolean elementIsPersistent()
    {
        return element.classMetaData != null;
    }

    /**
     * Convenience accessor for the Element ClassMetaData.
     * @param clr ClassLoader resolver (in case we need to initialise it)
     * @return element ClassMetaData
     */
    public AbstractClassMetaData getElementClassMetaData(final ClassLoaderResolver clr)
    {
        if (element.classMetaData != null && !element.classMetaData.isInitialised())
        {
            // Do as PrivilegedAction since uses reflection
            // [JDOAdapter.isValidPrimaryKeyClass calls reflective methods]
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    element.classMetaData.initialise(clr);
                    return null;
                }
            });
        }
        return element.classMetaData;
    }

    /**
     * Returns whether this array may contain persistable elements (as indicated by the user).
     * TODO Remove this. The element-type of the array defines such things and this is not the solution
     * @return whether this array may contain persistable elements
     */
    public boolean mayContainPersistableElements()
    {
        return mayContainPersistableElements;
    }

    /**
     * Accessor for the embedded-element value
     * @return embedded-element value
     */
    public boolean isEmbeddedElement()
    {
        if (element.embedded == null)
        {
            return false;
        }
        return element.embedded.booleanValue();
    }

    /**
     * Accessor for the serialized-element tag value
     * @return serialized-element tag value
     */
    public boolean isSerializedElement()
    {
        if (element.serialized == null)
        {
            return false;
        }
        return element.serialized.booleanValue();
    }

    /**
     * Accessor for The dependent-element attribute indicates that the
     * collection's element contains a reference that is to be deleted if the
     * referring instance is deleted.
     * 
     * @return dependent-element tag value
     */
    public boolean isDependentElement()
    {
        if (element.dependent == null)
        {
            return false;
        }
        return element.dependent.booleanValue();
    }

    /**
     * Accessor for the element implementation types (when element is a reference type).
     * @return element implementation types
     */
    public String getElementType()
    {
        return element.typeName;
    }

    public String[] getElementTypes()
    {
        return getParent().getValuesForExtension(MetaData.EXTENSION_MEMBER_IMPLEMENTATION_CLASSES);
    }

    // TODO Keep implementation types separate from declared type
    public ArrayMetaData setElementType(String type)
    {
        // This is only valid pre-populate
        if (StringUtils.isWhitespace(type))
        {
            // Arrays don't default to Object
            element.setTypeName(null);
        }
        else
        {
            element.setTypeName(type);
        }
        return this;
    }

    public ArrayMetaData setEmbeddedElement(boolean embedded)
    {
        element.setEmbedded(embedded);
        return this;
    }

    public ArrayMetaData setSerializedElement(boolean serialized)
    {
        element.setSerialized(serialized);
        return this;
    }

    public ArrayMetaData setDependentElement(boolean dependent)
    {
        element.setDependent(dependent);
        return this;
    }

    /**
     * Accessor for all AbstractClassMetaData referenced by this array.
     * @param orderedCmds List of ordered AbstractClassMetaData objects (added to).
     * @param referencedCmds Set of all AbstractClassMetaData objects (added to).
     * @param clr the ClassLoaderResolver
     */
    void getReferencedClassMetaData(final List<AbstractClassMetaData> orderedCmds, final Set<AbstractClassMetaData> referencedCmds, final ClassLoaderResolver clr)
    {
        AbstractClassMetaData elementCmd = getMetaDataManager().getMetaDataForClass(getMemberMetaData().getType().getComponentType(), clr);
        if (elementCmd != null)
        {
            elementCmd.getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
        }
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder(super.toString()).append(" [" + element.typeName + "]");
        if (element.getEmbedded() == Boolean.TRUE)
        {
            str.append(" embedded");
        }
        if (element.getSerialized() == Boolean.TRUE)
        {
            str.append(" serialised");
        }
        if (element.getDependent() == Boolean.TRUE)
        {
            str.append(" dependent");
        }
        return str.toString();
    }
}