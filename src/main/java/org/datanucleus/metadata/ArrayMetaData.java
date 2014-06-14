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
2007 Xuan Baldauf - Support for the ability of users to explictly state that an array whose component type is not 
                    persistable may still have persistable elements. See http://www.jpox.org/servlet/jira/browse/CORE-3274
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.api.ApiAdapter;
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
        element.type = arrmd.element.type;
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
     * @param mmgr MetaData manager
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr)
    {
        AbstractMemberMetaData mmd = (AbstractMemberMetaData)parent;
        if (!StringUtils.isWhitespace(element.type) && element.type.indexOf(',') > 0)
        {
            throw new InvalidMemberMetaDataException("044140", mmd.getClassName(), mmd.getName());
        }

        ApiAdapter api = mmgr.getApiAdapter();

        // Make sure the type in "element" is set
        element.populate(((AbstractMemberMetaData)parent).getAbstractClassMetaData().getPackageName(), 
            clr, primary, mmgr);

        // Check the field type and see if it is an array type
        Class field_type = getMemberMetaData().getType();
        if (!field_type.isArray())
        {
            throw new InvalidMemberMetaDataException("044141", mmd.getClassName(), getFieldName());
        }

        // "embedded-element"
        if (element.embedded == null)
        {
            // Assign default for "embedded-element" based on 18.13.1 of JDO 2 spec
            // Note : this fails when using in the enhancer since not yet PC
            Class component_type = field_type.getComponentType();
            if (mmgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(component_type))
            {
                element.embedded = Boolean.TRUE;
            }
            else if (api.isPersistable(component_type) || Object.class.isAssignableFrom(component_type) ||
                component_type.isInterface())
            {
                element.embedded = Boolean.FALSE;
            }
            else
            {
                element.embedded = Boolean.TRUE;
            }
        }
        if (Boolean.FALSE.equals(element.embedded))
        {
            // If the user has set a non-PC/non-Interface as not embedded, correct it since not supported.
            // Note : this fails when using in the enhancer since not yet PC
            Class component_type = field_type.getComponentType();
            if (!api.isPersistable(component_type) && !component_type.isInterface() &&
                component_type != java.lang.Object.class)
            {
                element.embedded = Boolean.TRUE;
            }
        }

        if (!mmgr.isEnhancing() && !getMemberMetaData().isSerialized())
        {
            // Catch situations that we don't support
            if (getMemberMetaData().getJoinMetaData() == null &&
                !api.isPersistable(getMemberMetaData().getType().getComponentType()) &&
                mmgr.supportsORM())
            {
                // We only support persisting particular array types as byte-streams (non-Java-serialised)
                String arrayComponentType = getMemberMetaData().getType().getComponentType().getName();
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
                    String msg = Localiser.msg("044142", mmd.getClassName(), getFieldName(), getMemberMetaData().getType().getComponentType().getName());
                    NucleusLogger.METADATA.warn(msg);
                }
            }
        }

        // Keep a reference to the MetaData for the element
        if (element.type != null)
        {
            Class elementCls = clr.classForName(element.type, primary);
            
            if (api.isPersistable(elementCls))
            {
                mayContainPersistableElements = true;
            }
            
            
            element.classMetaData = mmgr.getMetaDataForClassInternal(elementCls, clr);
        }
        else
        {
            element.type = field_type.getComponentType().getName();
            element.classMetaData = mmgr.getMetaDataForClassInternal(field_type.getComponentType(), clr);
        }
        
        if (element.classMetaData!=null)
        {
            mayContainPersistableElements = true;
        }

        // Make sure anything in the superclass is populated too
        super.populate(clr, primary, mmgr);

        setPopulated();
    }

    /**
     * Accessor for the element implementation types (when element is a reference type).
     * The return can contain comma-separated values.
     * @return element implementation types
     */
    public String getElementType()
    {
        return element.type;
    }

    public boolean elementIsPersistent()
    {
        return element.classMetaData != null;
    }

    /**
     * Convenience accessor for the Element ClassMetaData.
     * @param clr ClassLoader resolver (in case we need to initialise it)
     * @param mmgr MetaData manager (in case we need to initialise it)
     * @return element ClassMetaData
     */
    public AbstractClassMetaData getElementClassMetaData(final ClassLoaderResolver clr, final MetaDataManager mmgr)
    {
        if (element.classMetaData != null && !element.classMetaData.isInitialised())
        {
            // Do as PrivilegedAction since uses reflection
            // [JDOAdapter.isValidPrimaryKeyClass calls reflective methods]
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    element.classMetaData.initialise(clr, mmgr);
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
        else
        {
            return element.embedded.booleanValue();
        }
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
        else
        {
            return element.serialized.booleanValue();
        }
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
        else
        {
            return element.dependent.booleanValue();
        }
    }

    public ArrayMetaData setElementType(String type)
    {
        if (StringUtils.isWhitespace(type))
        {
            // Arrays don't default to Object
            element.type = null;
        }
        else
        {
            element.setType(type);
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
     * @param orderedCMDs List of ordered AbstractClassMetaData objects (added to).
     * @param referencedCMDs Set of all AbstractClassMetaData objects (added to).
     * @param clr the ClassLoaderResolver
     * @param mmgr MetaData manager
     */
    void getReferencedClassMetaData(final List orderedCMDs, final Set referencedCMDs,
            final ClassLoaderResolver clr, final MetaDataManager mmgr)
    {
        AbstractClassMetaData element_cmd = 
            mmgr.getMetaDataForClass(getMemberMetaData().getType().getComponentType(), clr);
        if (element_cmd != null)
        {
            element_cmd.getReferencedClassMetaData(orderedCMDs, referencedCMDs, clr, mmgr);
        }
    }

    /**
     * Returns a string representation of the object.
     * @param prefix The prefix string 
     * @param indent The indent string 
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<array");
        if (element.type != null)
        {
            sb.append(" element-type=\"").append(element.type).append("\"");
        }
        if (element.embedded != null)
        {
            sb.append(" embedded-element=\"").append(element.embedded).append("\"");
        }
        if (element.serialized != null)
        {
            sb.append(" serialized-element=\"").append(element.serialized).append("\"");
        }
        if (element.dependent != null)
        {
            sb.append(" dependent-element=\"").append(element.dependent).append("\"");
        }
        
        if (getNoOfExtensions() > 0)
        {
            sb.append(">\n");

            // Add extensions
            sb.append(super.toString(prefix + indent,indent));

            sb.append(prefix).append("</array>\n");
        }
        else
        {
            sb.append(prefix).append("/>\n");
        }

        return sb.toString();
    }
}