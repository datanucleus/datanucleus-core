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
2004 Erik Bengtson - add dependent keys and values
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Representation of the Meta-Data for a Map.
 */
public class MapMetaData extends ContainerMetaData
{
    private static final long serialVersionUID = -1151740606173916495L;

    public enum MapType
    {
        MAP_TYPE_JOIN, // Join table with refs to key and value
        MAP_TYPE_KEY_IN_VALUE, // Key table with ref to value
        MAP_TYPE_VALUE_IN_KEY // Value table with ref to key
    }

    /** Type of map. */
    protected MapType mapType;

    /** Representation of the key of the map. */
    protected ContainerComponent key;

    /** Representation of the value of the map. */
    protected ContainerComponent value;

    /**
     * Constructor to create a copy of the passed metadata.
     * @param mapmd The metadata to copy
     */
    public MapMetaData(MapMetaData mapmd)
    {
        super(mapmd);

        key = new ContainerComponent();
        key.embedded = mapmd.key.embedded;
        key.serialized = mapmd.key.serialized;
        key.dependent = mapmd.key.dependent;
        key.type = mapmd.key.type;
        key.classMetaData = mapmd.key.classMetaData;

        value = new ContainerComponent();
        value.embedded = mapmd.value.embedded;
        value.serialized = mapmd.value.serialized;
        value.dependent = mapmd.value.dependent;
        value.type = mapmd.value.type;
        value.classMetaData = mapmd.value.classMetaData;
    }

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public MapMetaData()
    {
        key = new ContainerComponent();
        value = new ContainerComponent();
    }

    /**
     * Method to populate any defaults, and check the validity of the MetaData.
     * @param clr ClassLoaderResolver to use for loading any key/value types 
     * @param primary the primary ClassLoader to use (or null)
     * @param mmgr MetaData manager
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr)
    {
        AbstractMemberMetaData mmd = (AbstractMemberMetaData)parent;
        if (!StringUtils.isWhitespace(key.type) && key.type.indexOf(',') > 0)
        {
            throw new InvalidMemberMetaDataException("044143", mmd.getClassName(), mmd.getName());
        }
        if (!StringUtils.isWhitespace(value.type) && value.type.indexOf(',') > 0)
        {
            throw new InvalidMemberMetaDataException("044144", mmd.getClassName(), mmd.getName());
        }

        // Make sure the type in "key", "value" is set
        key.populate(((AbstractMemberMetaData)parent).getAbstractClassMetaData().getPackageName(), clr, primary, mmgr);
        value.populate(((AbstractMemberMetaData)parent).getAbstractClassMetaData().getPackageName(), clr, primary, mmgr);

        // Check the field type and see if it is castable to a Map
        Class field_type = getMemberMetaData().getType();
        if (!java.util.Map.class.isAssignableFrom(field_type))
        {
            throw new InvalidMemberMetaDataException("044145",  mmd.getClassName(), mmd.getName());
        }

        if (java.util.Properties.class.isAssignableFrom(field_type))
        {
            // Properties defaults to <String, String>
            if (key.type == null)
            {
                key.type = String.class.getName();
            }
            if (value.type == null)
            {
                value.type = String.class.getName();
            }
        }

        // "key-type"
        if (key.type == null)
        {
            throw new InvalidMemberMetaDataException("044146",  mmd.getClassName(), mmd.getName());
        }

        // Check that the key type exists
        Class keyTypeClass = null;
        try
        {
            keyTypeClass = clr.classForName(key.type, primary);
        }
        catch (ClassNotResolvedException cnre)
        {
            try
            {
                // Maybe the user specified a java.lang class without fully-qualifying it
                // This is beyond the scope of the JDO spec which expects java.lang cases to be fully-qualified
                keyTypeClass = clr.classForName(ClassUtils.getJavaLangClassForType(key.type), primary);
            }
            catch (ClassNotResolvedException cnre2)
            {
                throw new InvalidMemberMetaDataException("044147", mmd.getClassName(), mmd.getName(), key.type);
            }
        }

        if (!keyTypeClass.getName().equals(key.type))
        {
            // The value-type has been resolved from what was specified in the MetaData - update to the fully-qualified name
            NucleusLogger.METADATA.info(Localiser.msg("044148", getFieldName(),
                getMemberMetaData().getClassName(false), key.type, keyTypeClass.getName()));
            key.type = keyTypeClass.getName();
        }

        // "embedded-key"
        if (key.embedded == null)
        {
            // Assign default for "embedded-key" based on 18.13.2 of JDO 2 spec
            if (mmgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(keyTypeClass))
            {
                key.embedded = Boolean.TRUE;
            }
            else if (mmgr.getApiAdapter().isPersistable(keyTypeClass) || Object.class.isAssignableFrom(keyTypeClass) || keyTypeClass.isInterface())
            {
                key.embedded = Boolean.FALSE;
            }
            else
            {
                key.embedded = Boolean.TRUE;
            }
        }
        if (Boolean.FALSE.equals(key.embedded))
        {
            // If the user has set a non-PC/non-Interface as not embedded, correct it since not supported.
            // Note : this fails when using in the enhancer since not yet PC
            if (!mmgr.getApiAdapter().isPersistable(keyTypeClass) && !keyTypeClass.isInterface() && keyTypeClass != java.lang.Object.class)
            {
                key.embedded = Boolean.TRUE;
            }
        }
        KeyMetaData keymd = ((AbstractMemberMetaData)parent).getKeyMetaData();
        if (keymd != null && keymd.getEmbeddedMetaData() != null)
        {
            // If the user has specified <embedded>, set to true
            key.embedded = Boolean.TRUE;
        }

        if (hasExtension("key-implementation-classes"))
        {
            // Check/fix the validity of the implementation-classes and qualify them where required.
            StringBuilder str = new StringBuilder();
            String[] implTypes = getValuesForExtension("key-implementation-classes");
            for (int i=0;i<implTypes.length;i++)
            {
                String implTypeName = ClassUtils.createFullClassName(getMemberMetaData().getPackageName(), implTypes[i]);
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
                        throw new InvalidMemberMetaDataException("044116", getMemberMetaData().getClassName(), getMemberMetaData().getName(), implTypes[i]);
                    }
                }
            }
            addExtension(VENDOR_NAME, "key-implementation-classes", str.toString()); // Replace with this new value
        }

        // "value-type"
        if (value.type == null)
        {
            throw new InvalidMemberMetaDataException("044149", mmd.getClassName(), mmd.getName());
        }

        // Check that the value-type exists
        Class valueTypeClass = null;
        try
        {
            valueTypeClass = clr.classForName(value.type);
        }
        catch (ClassNotResolvedException cnre)
        {
            try
            {
                // Maybe the user specified a java.lang class without fully-qualifying it
                // This is beyond the scope of the JDO spec which expects java.lang cases to be fully-qualified
                valueTypeClass = clr.classForName(ClassUtils.getJavaLangClassForType(value.type));
            }
            catch (ClassNotResolvedException cnre2)
            {
                throw new InvalidMemberMetaDataException("044150", mmd.getClassName(), mmd.getName(), value.type);
            }
        }

        if (!valueTypeClass.getName().equals(value.type))
        {
            // The value-type has been resolved from what was specified in the MetaData - update to the fully-qualified name
            NucleusLogger.METADATA.info(Localiser.msg("044151", getFieldName(),
                getMemberMetaData().getClassName(false), value.type, valueTypeClass.getName()));
            value.type = valueTypeClass.getName();
        }

        // "embedded-value"
        if (value.embedded == null)
        {
            // Assign default for "embedded-value" based on 18.13.2 of JDO 2 spec
            if (mmgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(valueTypeClass))
            {
                value.embedded = Boolean.TRUE;
            }
            else if (mmgr.getApiAdapter().isPersistable(valueTypeClass) || Object.class.isAssignableFrom(valueTypeClass) || valueTypeClass.isInterface())
            {
                value.embedded = Boolean.FALSE;
            }
            else
            {
                value.embedded = Boolean.TRUE;
            }
        }
        if (value.embedded == Boolean.FALSE)
        {
            // If the user has set a non-PC/non-Interface as not embedded, correct it since not supported.
            // Note : this fails when using in the enhancer since not yet PC
            if (!mmgr.getApiAdapter().isPersistable(valueTypeClass) && !valueTypeClass.isInterface() && valueTypeClass != java.lang.Object.class)
            {
                value.embedded = Boolean.TRUE;
            }
        }
        ValueMetaData valuemd = ((AbstractMemberMetaData)parent).getValueMetaData();
        if (valuemd != null && valuemd.getEmbeddedMetaData() != null)
        {
            // If the user has specified <embedded>, set to true
            value.embedded = Boolean.TRUE;
        }

        if (hasExtension("value-implementation-classes"))
        {
            // Check/fix the validity of the implementation-classes and qualify them where required.
            StringBuilder str = new StringBuilder();
            String[] implTypes = getValuesForExtension("value-implementation-classes");
            for (int i=0;i<implTypes.length;i++)
            {
                String implTypeName = ClassUtils.createFullClassName(getMemberMetaData().getPackageName(), implTypes[i]);
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
                        throw new InvalidMemberMetaDataException("044116", getMemberMetaData().getClassName(), getMemberMetaData().getName(), implTypes[i]);
                    }
                }
            }
            addExtension(VENDOR_NAME, "value-implementation-classes", str.toString()); // Replace with this new value
        }

        key.classMetaData = mmgr.getMetaDataForClassInternal(keyTypeClass, clr);
        value.classMetaData = mmgr.getMetaDataForClassInternal(valueTypeClass, clr);

        // Cater for Key with mapped-by needing to be PK (for JPA)
        if (keymd != null && keymd.mappedBy != null && keymd.mappedBy.equals("#PK")) // Special value set by JPAMetaDataHandler
        {
            // Need to set the mapped-by of <key> to be the PK of the <value>
            if (value.classMetaData.getNoOfPrimaryKeyMembers() != 1)
            {
                // TODO Localise this
                throw new NucleusUserException("DataNucleus does not support use of <map-key> with no name field when the value class has a composite primary key");
            }
            int[] valuePkFieldNums = value.classMetaData.getPKMemberPositions();
            keymd.mappedBy = value.classMetaData.getMetaDataForManagedMemberAtAbsolutePosition(valuePkFieldNums[0]).name;
        }

        // Make sure anything in the superclass is populated too
        super.populate(clr, primary, mmgr);

        setPopulated();
    }

    public MapType getMapType()
    {
        if (mapType == null)
        {
            AbstractMemberMetaData mmd = (AbstractMemberMetaData)parent;
            if (mmd.getJoinMetaData() != null)
            {
                mapType = MapType.MAP_TYPE_JOIN;
            }
            else
            {
                if (mmd.getValueMetaData() != null && mmd.getValueMetaData().getMappedBy() != null)
                {
                    this.mapType = MapType.MAP_TYPE_VALUE_IN_KEY;
                }
                else
                {
                    this.mapType = MapType.MAP_TYPE_KEY_IN_VALUE;
                }
            }
        }
        return mapType;
    }

    /**
     * Accessor for the key-type tag value.
     * May be comma-separated if several key types are possible.
     * @return key-type tag value
     */
    public String getKeyType()
    {
        return key.type;
    }

    public String[] getKeyTypes()
    {
        return ((AbstractMemberMetaData)getParent()).getValuesForExtension("key-implementation-classes");
    }

    /**
     * Convenience accessor for the Key ClassMetaData.
     * @param clr ClassLoader resolver (in case we need to initialise it)
     * @param mmgr MetaData manager (in case we need to initialise it)
     * @return key ClassMetaData
     */
    public AbstractClassMetaData getKeyClassMetaData(final ClassLoaderResolver clr, final MetaDataManager mmgr)
    {
        if (key.classMetaData != null && !key.classMetaData.isInitialised())
        {
            // Do as PrivilegedAction since uses reflection
            // [JDOAdapter.isValidPrimaryKeyClass calls reflective methods]
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    key.classMetaData.initialise(clr, mmgr);
                    return null;
                }
            });
        }
        return key.classMetaData;
    }

    public boolean keyIsPersistent()
    {
        return key.classMetaData != null;
    }

    /**
     * Accessor for the value-type tag value.
     * May be comma-separated if several value types are possible.
     * @return value-type tag value
     */
    public String getValueType()
    {
        return value.type;
    }

    public String[] getValueTypes()
    {
        return ((AbstractMemberMetaData)getParent()).getValuesForExtension("value-implementation-classes");
    }

    /**
     * Convenience accessor for the Value ClassMetaData
     * @param clr ClassLoader resolver (in case we need to initialise it)
     * @param mmgr MetaData manager (in case we need to initialise it)
     * @return value ClassMetaData
     */
    public AbstractClassMetaData getValueClassMetaData(final ClassLoaderResolver clr, final MetaDataManager mmgr)
    {
        if (value.classMetaData != null && !value.classMetaData.isInitialised())
        {
            // Do as PrivilegedAction since uses reflection
            // [JDOAdapter.isValidPrimaryKeyClass calls reflective methods]
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    value.classMetaData.initialise(clr, mmgr);
                    return null;
                }
            });
        }
        return value.classMetaData;
    }

    public boolean valueIsPersistent()
    {
        return value.classMetaData != null;
    }

    /**
     * Accessor for the embedded-key tag value.
     * @return embedded-key tag value
     */
    public boolean isEmbeddedKey()
    {
        if (key.embedded == null)
        {
            return false;
        }
        return key.embedded.booleanValue();
    }

    /**
     * Accessor for the embedded-value tag value.
     * @return embedded-value tag value
     */
    public boolean isEmbeddedValue()
    {
        if (value.embedded == null)
        {
            return false;
        }
        return value.embedded.booleanValue();
    }

    /**
     * Accessor for the serialized-key tag value.
     * @return serialized-key tag value
     */
    public boolean isSerializedKey()
    {
        if (key.serialized == null)
        {
            return false;
        }
        return key.serialized.booleanValue();
    }

    /**
     * Accessor for the serialized-value tag value.
     * @return serialized-value tag value
     */
    public boolean isSerializedValue()
    {
        if (value.serialized == null)
        {
            return false;
        }
        return value.serialized.booleanValue();
    }

    /**
     * Accessor for the dependent-key attribute indicates that the map's
     * key contains references that are to be deleted if the referring instance is deleted.
     * @return dependent-key tag value
     */
    public boolean isDependentKey()
    {
        if (key.dependent == null)
        {
            return false;
        }
        else if (key.classMetaData == null)
        {
            return false;
        }
        else
        {
            return key.dependent.booleanValue();
        }
    }

    /**
     * Accessor for the dependent-value attribute indicates that the
     * map's value contains references that are to be deleted if the
     * referring instance is deleted.
     * @return dependent-value tag value
     */
    public boolean isDependentValue()
    {
        if (value.dependent == null)
        {
            return false;
        }
        else if (value.classMetaData == null)
        {
            return false;
        }
        else
        {
            return value.dependent.booleanValue();
        }
    }

    public MapMetaData setKeyType(String type)
    {
        key.setType(type);
        return this;
    }

    public MapMetaData setEmbeddedKey(boolean embedded)
    {
        key.setEmbedded(embedded);
        return this;
    }

    public MapMetaData setSerializedKey(boolean serialized)
    {
        key.setSerialized(serialized);
        return this;
    }

    public MapMetaData setDependentKey(boolean dependent)
    {
        key.setDependent(dependent);
        return this;
    }

    public MapMetaData setValueType(String type)
    {
        value.setType(type);
        return this;
    }

    public MapMetaData setEmbeddedValue(boolean embedded)
    {
        value.setEmbedded(embedded);
        return this;
    }

    public MapMetaData setSerializedValue(boolean serialized)
    {
        value.setSerialized(serialized);
        return this;
    }

    public MapMetaData setDependentValue(boolean dependent)
    {
        value.setDependent(dependent);
        return this;
    }

    // ----------------------------- Utilities ---------------------------------
 
    /**
     * Accessor for all ClassMetaData referenced by this array.
     * @param orderedCmds List of ordered ClassMetaData objects (added to).
     * @param referencedCmds Set of all ClassMetaData objects (added to).
     * @param clr the ClassLoaderResolver
     * @param mmgr MetaData manager
     */
    void getReferencedClassMetaData(final List<AbstractClassMetaData> orderedCmds, final Set<AbstractClassMetaData> referencedCmds, final ClassLoaderResolver clr, final MetaDataManager mmgr)
    {
        AbstractClassMetaData keyCmd = mmgr.getMetaDataForClass(key.type, clr);
        if (keyCmd != null)
        {
            keyCmd.getReferencedClassMetaData(orderedCmds, referencedCmds, clr, mmgr);
        }

        AbstractClassMetaData valueCmd = mmgr.getMetaDataForClass(value.type, clr);
        if (valueCmd != null)
        {
            valueCmd.getReferencedClassMetaData(orderedCmds, referencedCmds, clr, mmgr);
        }
    }

    /**
     * Returns a string representation of the object.
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<map key-type=\"").append(key.type).append("\" value-type=\"").append(value.type).append("\"");
        if (key.embedded != null)
        {
            sb.append(" embedded-key=\"").append(key.embedded).append("\"");
        }
        if (value.embedded != null)
        {
            sb.append(" embedded-value=\"").append(value.embedded).append("\"");
        }
        if (key.dependent != null)
        {
            sb.append(" dependent-key=\"").append(key.dependent).append("\"");
        }
        if (value.dependent != null)
        {
            sb.append(" dependent-value=\"").append(value.dependent).append("\"");
        }
        if (key.serialized != null)
        {
            sb.append(" serialized-key=\"").append(key.serialized).append("\"");
        }
        if (value.serialized != null)
        {
            sb.append(" serialized-value=\"").append(value.serialized).append("\"");
        }
        sb.append(">\n");

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</map>\n");
        return sb.toString();
    }
}