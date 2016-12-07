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
        key.typeName = mapmd.key.typeName;
        key.classMetaData = mapmd.key.classMetaData;

        value = new ContainerComponent();
        value.embedded = mapmd.value.embedded;
        value.serialized = mapmd.value.serialized;
        value.dependent = mapmd.value.dependent;
        value.typeName = mapmd.value.typeName;
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
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary)
    {
        AbstractMemberMetaData mmd = (AbstractMemberMetaData)parent;
        if (!StringUtils.isWhitespace(key.typeName) && key.typeName.indexOf(',') > 0)
        {
            throw new InvalidMemberMetaDataException("044143", mmd.getClassName(), mmd.getName());
        }
        if (!StringUtils.isWhitespace(value.typeName) && value.typeName.indexOf(',') > 0)
        {
            throw new InvalidMemberMetaDataException("044144", mmd.getClassName(), mmd.getName());
        }

        // Make sure the type in "key", "value" is set
        key.populate(mmd.getAbstractClassMetaData().getPackageName(), clr, primary);
        value.populate(mmd.getAbstractClassMetaData().getPackageName(), clr, primary);

        // Check the field type and see if it is castable to a Map
        Class field_type = mmd.getType();
        if (!java.util.Map.class.isAssignableFrom(field_type))
        {
            throw new InvalidMemberMetaDataException("044145",  mmd.getClassName(), mmd.getName());
        }

        if (java.util.Properties.class.isAssignableFrom(field_type))
        {
            // Properties defaults to <String, String>
            if (key.typeName == null)
            {
                key.typeName = String.class.getName();
            }
            if (value.typeName == null)
            {
                value.typeName = String.class.getName();
            }
        }

        // "key-type"
        if (key.typeName == null)
        {
            throw new InvalidMemberMetaDataException("044146",  mmd.getClassName(), mmd.getName());
        }

        // Check that the key type exists TODO Remove this since performed in key.populate
        Class keyTypeClass = null;
        try
        {
            keyTypeClass = clr.classForName(key.typeName, primary);
        }
        catch (ClassNotResolvedException cnre)
        {
            try
            {
                // Maybe the user specified a java.lang class without fully-qualifying it
                // This is beyond the scope of the JDO spec which expects java.lang cases to be fully-qualified
                keyTypeClass = clr.classForName(ClassUtils.getJavaLangClassForType(key.typeName), primary);
            }
            catch (ClassNotResolvedException cnre2)
            {
                throw new InvalidMemberMetaDataException("044147", mmd.getClassName(), mmd.getName(), key.typeName);
            }
        }
        if (!keyTypeClass.getName().equals(key.typeName))
        {
            // The value-type has been resolved from what was specified in the MetaData - update to the fully-qualified name
            NucleusLogger.METADATA.info(Localiser.msg("044148", getFieldName(), mmd.getClassName(false), key.typeName, keyTypeClass.getName()));
            key.typeName = keyTypeClass.getName();
        }

        // "embedded-key"
        MetaDataManager mmgr = mmd.getMetaDataManager();
        if (key.embedded == null)
        {
            // Assign default for "embedded-key" based on 18.13.2 of JDO 2 spec
            if (mmgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(keyTypeClass))
            {
                key.embedded = Boolean.TRUE;
            }
            else
            {
                // Use "readMetaDataForClass" in case we havent yet initialised the metadata for the key
                AbstractClassMetaData keyCmd = mmgr.readMetaDataForClass(keyTypeClass.getName());
                if (keyCmd == null)
                {
                    // Try to load it just in case using annotations and only pulled in one side of the relation
                    try
                    {
                        keyCmd = mmgr.getMetaDataForClass(keyTypeClass, clr);
                    }
                    catch (Throwable thr)
                    {
                    }
                }
                if (keyCmd != null)
                {
                    key.embedded = (keyCmd.isEmbeddedOnly() ? Boolean.TRUE : Boolean.FALSE);
                }
                else if (keyTypeClass.isInterface() || keyTypeClass == Object.class)
                {
                    // Map<interface> or Object not explicitly marked as embedded defaults to false
                    key.embedded = Boolean.FALSE;
                }
                else
                {
                    // Fallback to true
                    NucleusLogger.METADATA.debug("Member with map of keyType=" + keyTypeClass.getName()+
                        " not explicitly marked as embedded, so defaulting to embedded since not persistable");
                    key.embedded = Boolean.TRUE;
                }
            }
        }
        if (Boolean.FALSE.equals(key.embedded))
        {
            // Use "readMetaDataForClass" in case we havent yet initialised the metadata for the key
            AbstractClassMetaData elemCmd = mmgr.readMetaDataForClass(keyTypeClass.getName());
            if (elemCmd == null && !keyTypeClass.isInterface() && keyTypeClass != java.lang.Object.class)
            {
                // If the user has set a non-PC/non-Interface as not embedded, correct it since not supported.
                // Note : this fails when using in the enhancer since not yet PC
                NucleusLogger.METADATA.debug("Member with map with keyType=" + keyTypeClass.getName() +
                    " marked as not embedded, but only persistable as embedded, so resetting");
                key.embedded = Boolean.TRUE;
            }
        }
        KeyMetaData keymd = mmd.getKeyMetaData();
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
            addExtension("key-implementation-classes", str.toString()); // Replace with this new value
        }
        key.classMetaData = mmgr.getMetaDataForClassInternal(keyTypeClass, clr);

        // "value-type"
        if (value.typeName == null)
        {
            throw new InvalidMemberMetaDataException("044149", mmd.getClassName(), mmd.getName());
        }

        // Check that the value-type exists TODO Remove this since performed in value.populate
        Class valueTypeClass = null;
        try
        {
            valueTypeClass = clr.classForName(value.typeName);
        }
        catch (ClassNotResolvedException cnre)
        {
            try
            {
                // Maybe the user specified a java.lang class without fully-qualifying it
                // This is beyond the scope of the JDO spec which expects java.lang cases to be fully-qualified
                valueTypeClass = clr.classForName(ClassUtils.getJavaLangClassForType(value.typeName));
            }
            catch (ClassNotResolvedException cnre2)
            {
                throw new InvalidMemberMetaDataException("044150", mmd.getClassName(), mmd.getName(), value.typeName);
            }
        }
        if (!valueTypeClass.getName().equals(value.typeName))
        {
            // The value-type has been resolved from what was specified in the MetaData - update to the fully-qualified name
            NucleusLogger.METADATA.info(Localiser.msg("044151", getFieldName(), mmd.getClassName(false), value.typeName, valueTypeClass.getName()));
            value.typeName = valueTypeClass.getName();
        }

        // "embedded-value"
        if (value.embedded == null)
        {
            // Assign default for "embedded-value" based on 18.13.2 of JDO 2 spec
            if (mmgr.getNucleusContext().getTypeManager().isDefaultEmbeddedType(valueTypeClass))
            {
                value.embedded = Boolean.TRUE;
            }
            else
            {
                // Use "readMetaDataForClass" in case we havent yet initialised the metadata for the value
                AbstractClassMetaData valCmd = mmgr.readMetaDataForClass(valueTypeClass.getName());
                if (valCmd == null)
                {
                    // Try to load it just in case using annotations and only pulled in one side of the relation
                    try
                    {
                        valCmd = mmgr.getMetaDataForClass(valueTypeClass, clr);
                    }
                    catch (Throwable thr)
                    {
                    }
                }
                if (valCmd != null)
                {
                    value.embedded = (valCmd.isEmbeddedOnly() ? Boolean.TRUE : Boolean.FALSE);
                }
                else if (valueTypeClass.isInterface() || valueTypeClass == Object.class)
                {
                    // Map<interface> or Object not explicitly marked as embedded defaults to false
                    value.embedded = Boolean.FALSE;
                }
                else
                {
                    // Fallback to true
                    NucleusLogger.METADATA.debug("Member with map of valueType=" + valueTypeClass.getName()+
                        " not explicitly marked as embedded, so defaulting to embedded since not persistable");
                    value.embedded = Boolean.TRUE;
                }
            }
        }
        if (value.embedded == Boolean.FALSE)
        {
            // Use "readMetaDataForClass" in case we havent yet initialised the metadata for the value
            AbstractClassMetaData valCmd = mmgr.readMetaDataForClass(valueTypeClass.getName());
            if (valCmd == null && !valueTypeClass.isInterface() && valueTypeClass != java.lang.Object.class)
            {
                // If the user has set a non-PC/non-Interface as not embedded, correct it since not supported.
                // Note : this fails when using in the enhancer since not yet PC
                NucleusLogger.METADATA.debug("Member with map with valueType=" + valueTypeClass.getName() +
                    " marked as not embedded, but only persistable as embedded, so resetting");
                value.embedded = Boolean.TRUE;
            }
        }
        ValueMetaData valuemd = mmd.getValueMetaData();
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
            addExtension("value-implementation-classes", str.toString()); // Replace with this new value
        }
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
        super.populate();

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
        return key.typeName;
    }

    public String[] getKeyTypes()
    {
        return ((AbstractMemberMetaData)getParent()).getValuesForExtension("key-implementation-classes");
    }

    /**
     * Convenience accessor for the Key ClassMetaData.
     * @param clr ClassLoader resolver (in case we need to initialise it)
     * @return key ClassMetaData
     */
    public AbstractClassMetaData getKeyClassMetaData(final ClassLoaderResolver clr)
    {
        if (key.classMetaData != null && !key.classMetaData.isInitialised())
        {
            // Do as PrivilegedAction since uses reflection
            // [JDOAdapter.isValidPrimaryKeyClass calls reflective methods]
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    key.classMetaData.initialise(clr);
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
        return value.typeName;
    }

    public String[] getValueTypes()
    {
        return ((AbstractMemberMetaData)getParent()).getValuesForExtension("value-implementation-classes");
    }

    /**
     * Convenience accessor for the Value ClassMetaData
     * @param clr ClassLoader resolver (in case we need to initialise it)
     * @return value ClassMetaData
     */
    public AbstractClassMetaData getValueClassMetaData(final ClassLoaderResolver clr)
    {
        if (value.classMetaData != null && !value.classMetaData.isInitialised())
        {
            // Do as PrivilegedAction since uses reflection
            // [JDOAdapter.isValidPrimaryKeyClass calls reflective methods]
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    value.classMetaData.initialise(clr);
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
        // This is only valid pre-populate
        key.setTypeName(type);
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
        // This is only valid pre-populate
        value.setTypeName(type);
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
     */
    void getReferencedClassMetaData(final List<AbstractClassMetaData> orderedCmds, final Set<AbstractClassMetaData> referencedCmds, final ClassLoaderResolver clr)
    {
        MetaDataManager mmgr = getMetaDataManager();
        AbstractClassMetaData keyCmd = mmgr.getMetaDataForClass(key.typeName, clr);
        if (keyCmd != null)
        {
            keyCmd.getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
        }

        AbstractClassMetaData valueCmd = mmgr.getMetaDataForClass(value.typeName, clr);
        if (valueCmd != null)
        {
            valueCmd.getReferencedClassMetaData(orderedCmds, referencedCmds, clr);
        }
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder(super.toString());

        str.append(" key=" + key.typeName + " (");
        if (key.getEmbedded() == Boolean.TRUE)
        {
            str.append("embedded ");
        }
        if (key.getSerialized() == Boolean.TRUE)
        {
            str.append("serialised ");
        }
        if (key.getDependent() == Boolean.TRUE)
        {
            str.append("dependent");
        }
        str.append(")");

        str.append(" value=" + value.typeName + " (");
        if (value.getEmbedded() == Boolean.TRUE)
        {
            str.append("embedded ");
        }
        if (value.getSerialized() == Boolean.TRUE)
        {
            str.append("serialised ");
        }
        if (value.getDependent() == Boolean.TRUE)
        {
            str.append("dependent");
        }
        str.append(")");
        return str.toString();
    }
}