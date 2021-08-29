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
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.xml.XmlMetaDataParser;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Utilities needed for the processing of MetaData.
 */
public class MetaDataUtils
{
    private static MetaDataUtils instance;

    /**
     * Gets an instance of MetaDataUtils
     * @return a singleton instance of MetaDataUtils
     */
    public static synchronized MetaDataUtils getInstance()
    {
        if (instance == null)
        {
            instance = new MetaDataUtils();
        }
        return instance;
    }

    /**
     * Protected constructor to prevent outside instantiation
     */
    protected MetaDataUtils()
    {
    }

    /**
     * Convenience method to determine if an array is storable in a single column as a byte
     * array.
     * @param fmd The field
     * @return Whether this is an array that can be stored in a single column as non-serialised
     */
    public boolean arrayStorableAsByteArrayInSingleColumn(AbstractMemberMetaData fmd)
    {
        if (fmd == null || !fmd.hasArray())
        {
            return false;
        }

        String arrayComponentType = fmd.getType().getComponentType().getName();
        if (arrayComponentType.equals(ClassNameConstants.BOOLEAN) ||
            arrayComponentType.equals(ClassNameConstants.BYTE) ||
            arrayComponentType.equals(ClassNameConstants.CHAR) ||
            arrayComponentType.equals(ClassNameConstants.DOUBLE) ||
            arrayComponentType.equals(ClassNameConstants.FLOAT) ||
            arrayComponentType.equals(ClassNameConstants.INT) ||
            arrayComponentType.equals(ClassNameConstants.LONG) ||
            arrayComponentType.equals(ClassNameConstants.SHORT) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_LANG_BOOLEAN) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_LANG_BYTE) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_LANG_CHARACTER) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_LANG_DOUBLE) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_LANG_FLOAT) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_LANG_INTEGER) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_LANG_LONG) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_LANG_SHORT) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_MATH_BIGDECIMAL) ||
            arrayComponentType.equals(ClassNameConstants.JAVA_MATH_BIGINTEGER))
        {
            // These types can be stored as a single-column but setting the bytes only
            return true;
        }

        // All other arrays must be serialised into a single column
        return false;
    }

    /**
     * Convenience method that returns if a field stores a persistable object.
     * Doesn't care if the persistable object is serialised or embedded, just that it is persistable.
     * @param mmd MetaData for the field
     * @param ec ExecutionContext
     * @return Whether it stores a persistable object
     */
    public boolean storesPersistable(AbstractMemberMetaData mmd, ExecutionContext ec)
    {
        if (mmd == null)
        {
            return false;
        }

        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        MetaDataManager mmgr = ec.getMetaDataManager();
        if (mmd.hasCollection())
        {
            if (mmd.getCollection().elementIsPersistent())
            {
                // Collection of PC elements
                return true;
            }

            String elementType = mmd.getCollection().getElementType();
            Class elementCls = clr.classForName(elementType);
            if (mmgr.getMetaDataForImplementationOfReference(elementCls, null, clr) != null)
            {
                // Collection of reference type for FCOs
                return true;
            }
            if (elementCls != null && ClassUtils.isReferenceType(elementCls))
            {
                try
                {
                    String[] impls = getImplementationNamesForReferenceField(mmd, FieldRole.ROLE_COLLECTION_ELEMENT, clr, mmgr);
                    if (impls != null)
                    {
                        elementCls = clr.classForName(impls[0]);
                        if (ec.getApiAdapter().isPersistable(elementCls))
                        {
                            // Collection of reference type for FCOs
                            return true;
                        }
                    }
                }
                catch (NucleusUserException nue)
                {
                    // No implementations found so not persistable
                }
            }
        }
        else if (mmd.hasMap())
        {
            if (mmd.getMap().keyIsPersistent())
            {
                // Map of PC keys
                return true;
            }

            String keyType = mmd.getMap().getKeyType();
            Class keyCls = clr.classForName(keyType);
            if (mmgr.getMetaDataForImplementationOfReference(keyCls, null, clr) != null)
            {
                // Map with keys of reference type for FCOs
                return true;
            }
            if (keyCls != null && ClassUtils.isReferenceType(keyCls))
            {
                try
                {
                    String[] impls = getImplementationNamesForReferenceField(mmd, FieldRole.ROLE_MAP_KEY, clr, mmgr);
                    if (impls != null)
                    {
                        keyCls = clr.classForName(impls[0]);
                        if (ec.getApiAdapter().isPersistable(keyCls))
                        {
                            // Map with keys of reference type for FCOs
                            return true;
                        }
                    }
                }
                catch (NucleusUserException nue)
                {
                    // No implementations found so not persistable
                }
            }

            if (mmd.getMap().valueIsPersistent())
            {
                // Map of PC values
                return true;
            }

            String valueType = mmd.getMap().getValueType();
            Class valueCls = clr.classForName(valueType);
            if (mmgr.getMetaDataForImplementationOfReference(valueCls, null, clr) != null)
            {
                // Map with values of reference type for FCOs
                return true;
            }
            if (valueCls != null && ClassUtils.isReferenceType(valueCls))
            {
                try
                {
                    String[] impls = getImplementationNamesForReferenceField(mmd, FieldRole.ROLE_MAP_VALUE, clr, mmgr);
                    if (impls != null)
                    {
                        valueCls = clr.classForName(impls[0]);
                        if (ec.getApiAdapter().isPersistable(valueCls))
                        {
                            // Map with values of reference type for FCOs
                            return true;
                        }
                    }
                }
                catch (NucleusUserException nue)
                {
                    // No implementations found so not persistable
                }
            }
        }
        else if (mmd.hasArray())
        {
            if (mmgr.getApiAdapter().isPersistable(mmd.getType().getComponentType()))
            {
                // persistable[]
                return true;
            }

            String elementType = mmd.getArray().getElementType();
            Class elementCls = clr.classForName(elementType);
            if (mmgr.getApiAdapter().isPersistable(elementCls))
            {
                // Array of reference type for FCOs
                return true;
            }
            else if (mmgr.getMetaDataForImplementationOfReference(elementCls, null, clr) != null)
            {
                // Array of reference type for FCOs
                return true;
            }
            else if (elementCls != null && ClassUtils.isReferenceType(elementCls))
            {
                try
                {
                    String[] impls = getImplementationNamesForReferenceField(mmd, FieldRole.ROLE_ARRAY_ELEMENT, clr, mmgr);
                    if (impls != null)
                    {
                        elementCls = clr.classForName(impls[0]);
                        if (ec.getApiAdapter().isPersistable(elementCls))
                        {
                            // Array of reference type for FCOs
                            return true;
                        }
                    }
                }
                catch (NucleusUserException nue)
                {
                    // No implementations found so not persistable
                }
            }
        }
        else
        {
            // 1-1 relation with PC
            if (ClassUtils.isReferenceType(mmd.getType()) && mmgr.getMetaDataForImplementationOfReference(mmd.getType(), null, clr) != null)
            {
                // Reference type for an FCO
                return true;
            }
            if (mmgr.getMetaDataForClass(mmd.getType(), clr) != null)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience method that returns if a member stores a First-Class object (FCO).
     * If a field object is serialised/embedded then doesn't count the object as FCO - use storesPersistable() if you want that not checking.
     * @param mmd MetaData for the member
     * @param ec ExecutionContext
     * @return Whether it stores a FCO
     */
    public boolean storesFCO(AbstractMemberMetaData mmd, ExecutionContext ec)
    {
        if (mmd == null)
        {
            return false;
        }

        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        MetaDataManager mgr = ec.getMetaDataManager();
        if (mmd.isSerialized() || mmd.isEmbedded())
        {
            // Serialised or embedded fields have no FCO
            return false;
        }
        else if (mmd.hasCollection() && !mmd.getCollection().isSerializedElement() && !mmd.getCollection().isEmbeddedElement())
        {
            if (mmd.getCollection().elementIsPersistent())
            {
                // Collection of PC elements
                return true;
            }

            String elementType = mmd.getCollection().getElementType();
            Class elementCls = clr.classForName(elementType);
            if (elementCls != null && ClassUtils.isReferenceType(elementCls) && mgr.getMetaDataForImplementationOfReference(elementCls, null, clr) != null)
            {
                // Collection of reference type for FCOs
                return true;
            }
        }
        else if (mmd.hasMap())
        {
            if (mmd.getMap().keyIsPersistent() && !mmd.getMap().isEmbeddedKey() && !mmd.getMap().isSerializedKey())
            {
                // Map of PC keys
                return true;
            }

            String keyType = mmd.getMap().getKeyType();
            Class keyCls = clr.classForName(keyType);
            if (keyCls != null && ClassUtils.isReferenceType(keyCls) && mgr.getMetaDataForImplementationOfReference(keyCls, null, clr) != null)
            {
                // Map with keys of reference type for FCOs
                return true;
            }
                
            if (mmd.getMap().valueIsPersistent() && !mmd.getMap().isEmbeddedValue() && !mmd.getMap().isSerializedValue())
            {
                // Map of PC values
                return true;
            }

            String valueType = mmd.getMap().getValueType();
            Class valueCls = clr.classForName(valueType);
            if (valueCls != null && ClassUtils.isReferenceType(valueCls) && mgr.getMetaDataForImplementationOfReference(valueCls, null, clr) != null)
            {
                // Map with values of reference type for FCOs
                return true;
            }
        }
        else if (mmd.hasArray() && !mmd.getArray().isSerializedElement() && !mmd.getArray().isEmbeddedElement())
        {
            if (mgr.getApiAdapter().isPersistable(mmd.getType().getComponentType()))
            {
                // persistable[]
                return true;
            }
        }
        else
        {
            // 1-1 relation with PC
            if (ClassUtils.isReferenceType(mmd.getType()) && mgr.getMetaDataForImplementationOfReference(mmd.getType(), null, clr) != null)
            {
                // Reference type for an FCO
                return true;
            }
            if (mgr.getMetaDataForClass(mmd.getType(), clr) != null)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience method that splits a comma-separated list of values into a String array (removing whitespace).
     * @param attr The attribute value
     * @return The string components
     */
    public String[] getValuesForCommaSeparatedAttribute(String attr)
    {
        if (attr == null || attr.length() == 0)
        {
            return null;
        }

        String[] values=StringUtils.split(attr, ",");

        // Remove any whitespace around the values
        if (values != null)
        {
            for (int i=0;i<values.length;i++)
            {
                values[i] = values[i].trim();
            }
        }
        return values;
    }

    /**
     * Convenience method to return the class names of the available implementation types for 
     * an interface/Object field, given its required role. Removes all duplicates from the list.
     * @param fmd MetaData for the field
     * @param fieldRole The role of the field
     * @param clr the ClassLoaderResolver
     * @param mmgr MetaData manager
     * @return Names of the classes of the possible implementations of this interface/Object
     * @throws NucleusUserException if no implementation types are found for the reference type field
     */
    public String[] getImplementationNamesForReferenceField(AbstractMemberMetaData fmd, FieldRole fieldRole, ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        String[] implTypes = null;

        if (fieldRole == FieldRole.ROLE_FIELD)
        {
            implTypes = fmd.getFieldTypes();

            // Check if the user has defined an interface type being "implemented by" an interface ("persistent-interface")
            if (implTypes != null && implTypes.length == 1)
            {
                // Single class/interface name specified
                Class implCls = clr.classForName(implTypes[0].trim());
                if (implCls.isInterface())
                {
                    // The specified "implementation" is itself an interface so assume it is a "persistent-interface"
                    implTypes = mmgr.getClassesImplementingInterface(implTypes[0], clr);
                }
            }
        }
        else if (FieldRole.ROLE_COLLECTION_ELEMENT == fieldRole)
        {
            implTypes = fmd.getCollection().getElementTypes();
        }
        else if (FieldRole.ROLE_ARRAY_ELEMENT == fieldRole)
        {
            implTypes = fmd.getArray().getElementTypes();
        }
        else if (FieldRole.ROLE_MAP_KEY == fieldRole)
        {
            implTypes = fmd.getMap().getKeyTypes();
        }
        else if (FieldRole.ROLE_MAP_VALUE == fieldRole)
        {
            implTypes = fmd.getMap().getValueTypes();
        }

        if (implTypes == null)
        {
            // Nothing specified, so check if it is an interface and if so use the <implements> definition to get some types
            String type = null;
            if (fmd.hasCollection() && fieldRole == FieldRole.ROLE_COLLECTION_ELEMENT)
            {
                type = fmd.getCollection().getElementType();
            }
            else if (fmd.hasMap() && fieldRole == FieldRole.ROLE_MAP_KEY)
            {
                type = fmd.getMap().getKeyType();
            }
            else if (fmd.hasMap() && fieldRole == FieldRole.ROLE_MAP_VALUE)
            {
                type = fmd.getMap().getValueType();
            }
            else if (fmd.hasArray() && fieldRole == FieldRole.ROLE_ARRAY_ELEMENT)
            {
                type = fmd.getArray().getElementType();
                if (type == null)
                {
                    type = fmd.getType().getComponentType().getName();
                }
            }
            else
            {
                type = fmd.getTypeName();
            }

            if (!type.equals(ClassNameConstants.Object))
            {
                implTypes = mmgr.getClassesImplementingInterface(type,clr);
            }

            if (implTypes == null)
            {
                // Generate error since no implementations available
                throw new InvalidMemberMetaDataException("044161", fmd.getClassName(), fmd.getName(), type);
            }
        }

        // Remove all duplicates from the list but retain the original ordering
        return new LinkedHashSet<>(Arrays.asList(implTypes)).toArray(new String[0]);
    }

    /**
     * Convenience method to return a boolean from the String value.
     * If the string is null then dflt is returned.
     * @param str The string (should be "true", "false")
     * @param dflt The default
     * @return The boolean to use
     */
    public static boolean getBooleanForString(String str, boolean dflt)
    {
        if (StringUtils.isWhitespace(str))
        {
            return dflt;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * Searches the meta data tree upwards starting with the given leaf, stops as 
     * soon as it finds an extension with the given key.
     *
     * @param metadata Leaf of the meta data tree, where the search should start
     * @param key The key of the extension
     * @return The value of the extension (null if not existing)
     */
    public static String getValueForExtensionRecursively(MetaData metadata, String key)
    {
        if (metadata == null)
        {
            return null;
        }

        String value = metadata.getValueForExtension(key);
        if (value == null)
        {
            value = getValueForExtensionRecursively(metadata.getParent(), key);
        }

        return value;
    }

    /**
     * Searches the meta data tree upwards starting with the given leaf, stops as 
     * soon as it finds an extension with the given key.
     *
     * @param metadata Leaf of the meta data tree, where the search should start
     * @param key The key of the extension
     * @return The values of the extension (null if not existing)
     */
    public static String[] getValuesForExtensionRecursively(MetaData metadata, String key)
    {
        if (metadata == null)
        {
            return null;
        }

        String[] values = metadata.getValuesForExtension(key);
        if (values == null)
        {
            values = getValuesForExtensionRecursively(metadata.getParent(), key);
        }

        return values;
    }

    /**
     * Convenience method to return if a jdbc-type is numeric.
     * @param jdbcType The type string
     * @return Whether it is numeric
     */
    public static boolean isJdbcTypeNumeric(JdbcType jdbcType)
    {
        if (jdbcType == null)
        {
            return false;
        }
        switch (jdbcType)
        {
            case INTEGER :
            case SMALLINT :
            case TINYINT :
            case NUMERIC :
            case BIGINT :
                return true;
            default :
                return false;
        }
    }

    /**
     * Convenience method to return if a jdbc-type is floating point based.
     * @param jdbcType The type string
     * @return Whether it is floating point ased
     */
    public static boolean isJdbcTypeFloatingPoint(JdbcType jdbcType)
    {
        if (jdbcType == null)
        {
            return false;
        }
        switch (jdbcType)
        {
            case DECIMAL :
            case FLOAT :
            case REAL :
                return true;
            default :
                return false;
        }
    }

    /**
     * Convenience method to return if a jdbc-type is character based.
     * @param jdbcType The type string
     * @return Whether it is character based
     */
    public static boolean isJdbcTypeString(JdbcType jdbcType)
    {
        if (jdbcType == null)
        {
            return false;
        }
        switch (jdbcType)
        {
            case CHAR :
            case VARCHAR :
            case CLOB :
            case LONGVARCHAR :
            case NCHAR :
            case NVARCHAR :
            case LONGNVARCHAR :
                return true;
            default :
                return false;
        }
    }

    public static JdbcType getJdbcTypeForEnum(AbstractMemberMetaData mmd, FieldRole role, ClassLoaderResolver clr)
    {
        JdbcType jdbcType = JdbcType.VARCHAR;
        if (mmd != null)
        {
            String methodName = null;
            Class enumType = null;
            ColumnMetaData[] colmds = null;
            if (role == FieldRole.ROLE_FIELD)
            {
                enumType = mmd.getType();
                if (mmd.hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
                {
                    methodName = mmd.getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
                }
                colmds = mmd.getColumnMetaData();
            }
            else if (role == FieldRole.ROLE_COLLECTION_ELEMENT || role == FieldRole.ROLE_ARRAY_ELEMENT)
            {
                if (mmd.getElementMetaData() != null)
                {
                    enumType = clr.classForName(mmd.hasCollection() ? mmd.getCollection().getElementType() : mmd.getArray().getElementType());
                    if (mmd.getElementMetaData().hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
                    {
                        methodName = mmd.getElementMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
                    }
                    colmds = mmd.getElementMetaData().getColumnMetaData();
                }
            }
            else if (role == FieldRole.ROLE_MAP_KEY)
            {
                if (mmd.getKeyMetaData() != null)
                {
                    enumType = clr.classForName(mmd.getMap().getKeyType());
                    if (mmd.getKeyMetaData().hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
                    {
                        methodName = mmd.getKeyMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
                    }
                    colmds = mmd.getKeyMetaData().getColumnMetaData();
                }
            }
            else if (role == FieldRole.ROLE_MAP_VALUE)
            {
                if (mmd.getValueMetaData() != null)
                {
                    enumType = clr.classForName(mmd.getMap().getValueType());
                    if (mmd.getValueMetaData().hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
                    {
                        methodName = mmd.getValueMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
                    }
                    colmds = mmd.getValueMetaData().getColumnMetaData();
                }
            }

            if (methodName == null)
            {
                if (colmds != null && colmds.length == 1 && colmds[0].getJdbcType() != null)
                {
                    jdbcType = colmds[0].getJdbcType();
                }
            }
            else
            {
                try
                {
                    Method getterMethod = ClassUtils.getMethodForClass(enumType, methodName, null);
                    Class returnType = getterMethod.getReturnType();
                    if (returnType == short.class || returnType == int.class || returnType == long.class || Number.class.isAssignableFrom(returnType))
                    {
                        return JdbcType.INTEGER;
                    }
                    return JdbcType.VARCHAR;
                }
                catch (Exception e)
                {
                    NucleusLogger.PERSISTENCE.warn("Specified enum value-getter for method " + methodName + " on field " + mmd.getFullFieldName() + " gave an error on extracting the value", e);
                }
            }
        }

        return jdbcType;
    }

    /**
     * Convenience method to return the class metadata for the candidate and optionally its subclasses.
     * Caters for the class being a persistent interface.
     * @param cls The class
     * @param subclasses Include subclasses?
     * @param ec ExecutionContext
     * @return The metadata, starting with the candidate
     * @throws NucleusUserException if candidate is an interface with no metadata (i.e not persistent)
     */
    public static List<AbstractClassMetaData> getMetaDataForCandidates(Class cls, boolean subclasses, ExecutionContext ec)
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        List<AbstractClassMetaData> cmds = new ArrayList<>();
        if (cls.isInterface())
        {
            // Query of interface(+subclasses)
            AbstractClassMetaData icmd = ec.getMetaDataManager().getMetaDataForInterface(cls, clr);
            if (icmd == null)
            {
                throw new NucleusUserException("Attempting to query an interface yet it is not declared 'persistent'." +
                    " Define the interface in metadata as being persistent to perform this operation, and make sure" +
                    " any implementations use the same identity and identity member(s)");
            }

            String[] impls = ec.getMetaDataManager().getClassesImplementingInterface(cls.getName(), clr);
            for (String implName : impls)
            {
                AbstractClassMetaData implCmd = ec.getMetaDataManager().getMetaDataForClass(implName, clr);
                cmds.add(implCmd);

                if (subclasses)
                {
                    String[] subclassNames = ec.getMetaDataManager().getSubclassesForClass(implCmd.getFullClassName(), true);
                    if (subclassNames != null && subclassNames.length > 0)
                    {
                        for (String subclassName : subclassNames)
                        {
                            cmds.add(ec.getMetaDataManager().getMetaDataForClass(subclassName, clr));
                        }
                    }
                }
            }
        }
        else
        {
            // Query of candidate(+subclasses)
            AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(cls, clr);
            cmds.add(cmd);
            if (subclasses)
            {
                String[] subclassNames = ec.getMetaDataManager().getSubclassesForClass(cls.getName(), true);
                if (subclassNames != null && subclassNames.length > 0)
                {
                    for (int j=0;j<subclassNames.length;j++)
                    {
                        AbstractClassMetaData subcmd = ec.getMetaDataManager().getMetaDataForClass(subclassNames[j], clr);
                        cmds.add(subcmd);
                    }
                }
            }
        }
        return cmds;
    }

    /**
     * Method to take the provided input files and returns the FileMetaData that they implies.
     * Loads the files into the provided MetaDataManager in the process.
     * @param metaDataMgr Manager for MetaData
     * @param clr ClassLoader resolver
     * @param inputFiles Input metadata/class files
     * @return The FileMetaData for the input
     * @throws NucleusException Thrown if error(s) occur in processing the input
     */
    public static FileMetaData[] getFileMetaDataForInputFiles(MetaDataManager metaDataMgr, ClassLoaderResolver clr, String[] inputFiles)
    {
        FileMetaData[] filemds = null;

        // Read in the specified MetaData files - errors in MetaData will return exceptions and so we stop
        String msg = null;
        try
        {
            // Split the input files into MetaData files and classes
            Set<String> metadataFiles = new HashSet<>();
            Set<String> classNames = new HashSet<>();
            for (int i=0;i<inputFiles.length;i++)
            {
                if (inputFiles[i].endsWith(".class"))
                {
                    // Class file
                    URL classFileURL = null;
                    try
                    {
                        classFileURL = new URL("file:" + inputFiles[i]);
                    }
                    catch (Exception e)
                    {
                        msg = Localiser.msg("014013", inputFiles[i]);
                        NucleusLogger.METADATA.error(msg);
                        throw new NucleusUserException(msg);
                    }

                    String className = null;
                    try
                    {
                        className = ClassUtils.getClassNameForFileURL(classFileURL);
                        classNames.add(className);
                    }
                    catch (Throwable e)
                    {
                        // Fallback to method that parses along the filename
                        className = ClassUtils.getClassNameForFileName(inputFiles[i], clr);
                        if (className != null)
                        {
                            classNames.add(className);
                        }
                        else
                        {
                            NucleusLogger.METADATA.info("File \"" + inputFiles[i] + "\" could not be resolved to a class name, so ignoring." +
                                " Specify it as a class explicitly using persistence.xml to overcome this", e);
                        }
                    }
                }
                else
                {
                    // MetaData file
                    metadataFiles.add(inputFiles[i]);
                }
            }

            // Initialise the MetaDataManager using the mapping files and class names
            FileMetaData[] filemds1 = metaDataMgr.loadMetadataFiles(metadataFiles.toArray(new String[metadataFiles.size()]), null);
            FileMetaData[] filemds2 = metaDataMgr.loadClasses(classNames.toArray(new String[classNames.size()]), null);
            filemds = new FileMetaData[filemds1.length + filemds2.length];
            int pos = 0;
            for (int i=0;i<filemds1.length;i++)
            {
                filemds[pos++] = filemds1[i];
            }
            for (int i=0;i<filemds2.length;i++)
            {
                filemds[pos++] = filemds2[i];
            }
        }
        catch (Exception e)
        {
            // Error reading input files
            msg = Localiser.msg("014014", e.getMessage());
            NucleusLogger.METADATA.error(msg, e);
            throw new NucleusUserException(msg, e);
        }

        return filemds;
    }

    /**
     * Method to parse the available "persistence.xml" files returning the metadata for all found.
     * Searches for all files "META-INF/persistence.xml" in the CLASSPATH of the current thread.
     * @param pluginMgr PluginManager
     * @param persistenceFilename Name of persistence file (if null will use "persistence.xml")
     * @param validate Whether to validate the persistence file
     * @param namespaceAware Whether to support namespaces
     * @param clr ClassLoader resolver
     * @return The metadata for all "persistence.xml" files
     */
    public static PersistenceFileMetaData[] parsePersistenceFiles(PluginManager pluginMgr, String persistenceFilename, boolean validate, boolean namespaceAware, ClassLoaderResolver clr)
    {
        XmlMetaDataParser parser = new XmlMetaDataParser(null, pluginMgr, validate, namespaceAware);

        if (persistenceFilename != null)
        {
            // User has specified filename for persistence.xml
            try
            {
                URL fileURL = new URL(persistenceFilename);
                MetaData permd = parser.parseXmlMetaDataURL(fileURL, "persistence");
                return new PersistenceFileMetaData[] {(PersistenceFileMetaData)permd};
            }
            catch (MalformedURLException mue)
            {
                // User provided file is not found
                NucleusLogger.METADATA.error("Error reading user-specified persistence.xml file " + persistenceFilename, mue);
                return null;
            }
        }

        Set<MetaData> metadata = new LinkedHashSet<>();
        try
        {
            // Find all "META-INF/persistence.xml" files in the CLASSPATH of the current thread
            Enumeration files = clr.getResources("META-INF/persistence.xml", Thread.currentThread().getContextClassLoader());
            if (!files.hasMoreElements())
            {
                return null;
            }

            for ( ; files.hasMoreElements() ;)
            {
                // Parse the "persistence.xml"
                URL fileURL = (URL)files.nextElement();
                MetaData permd = parser.parseXmlMetaDataURL(fileURL, "persistence");
                metadata.add(permd);
            }
        }
        catch (IOException ioe)
        {
            // Do nothing
            NucleusLogger.METADATA.warn(StringUtils.getStringFromStackTrace(ioe));
        }

        return metadata.toArray(new PersistenceFileMetaData[metadata.size()]);
    }

    /**
     * Convenience method to parse the available persistence.xml file(s) and find the metadata for the specified persistence-unit.
     * @param pluginMgr Plugin Manager
     * @param persistenceFilename Filename of the persistence.xml (or null if using default "META-INF/persistence.xml")
     * @param unitName Name of the persistence unit
     * @param validate Whether to validate the XML
     * @param namespaceAware Whether the XML is namespace aware
     * @param clr ClassLoader resolver
     * @return Metadata for the persistence-unit (if found), or null (if not found)
     */
    public static PersistenceUnitMetaData getMetaDataForPersistenceUnit(PluginManager pluginMgr, String persistenceFilename, String unitName, boolean validate, boolean namespaceAware, ClassLoaderResolver clr)
    {
        PersistenceFileMetaData[] files = MetaDataUtils.parsePersistenceFiles(pluginMgr, persistenceFilename, validate, namespaceAware, clr);
        if (files == null)
        {
            // No "persistence.xml" files found
            throw new NucleusUserException(Localiser.msg("044046"));
        }

        for (PersistenceFileMetaData pfmd : files)
        {
            PersistenceUnitMetaData[] unitmds = pfmd.getPersistenceUnits();
            if (unitmds != null)
            {
                for (PersistenceUnitMetaData pumd : unitmds)
                {
                    if (pumd.getName().equals(unitName))
                    {
                        // Found the required unit
                        return pumd;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Convenience method for whether to persist the provided column as numeric.
     * Returns true if it has the jdbcType defined as "int"/"integer"
     * @param colmd Metadata for the column
     * @return Whether explicitly specified to use numeric, otherwise returns false.
     */
    public static boolean persistColumnAsNumeric(ColumnMetaData colmd)
    {
        boolean useLong = false;
        if (colmd != null && MetaDataUtils.isJdbcTypeNumeric(colmd.getJdbcType()))
        {
            useLong = true;
        }
        return useLong;
    }

    /**
     * Convenience method for whether to persist the provided column as string-based.
     * Returns true if it has the jdbcType defined as "varchar"/"char"
     * @param colmd Metadata for the column
     * @return Whether explicitly specified to use String, otherwise returns false.
     */
    public static boolean persistColumnAsString(ColumnMetaData colmd)
    {
        boolean useString = false;
        if (colmd != null && MetaDataUtils.isJdbcTypeString(colmd.getJdbcType()))
        {
            useString = true;
        }
        return useString;
    }

    public static Class getTypeOfDatastoreIdentity(DatastoreIdentityMetaData dimd)
    {
        if (dimd == null)
        {
            return long.class;
        }
        if (dimd.getValueStrategy() == ValueGenerationStrategy.UUIDHEX || dimd.getValueStrategy() == ValueGenerationStrategy.UUIDSTRING)
        {
            return String.class;
        }
        return long.class;
    }

    /**
     * Convenience method to return whether a member is stored as embedded.
     * This caters for not just "mmd.isEmbedded" returning true, but also the "embeddedOnly" of
     * the related type, as well as whether there is &lt;embedded&gt; metadata for this member
     * @param mmd Metadata for the member
     * @param relationType The relation type for this member
     * @param clr ClassLoader resolver
     * @param mmgr MetaData manager
     * @return Whether it is embedded
     */
    public static boolean isMemberEmbedded(AbstractMemberMetaData mmd, RelationType relationType, ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        boolean embedded = false;

        if (mmd.isEmbedded())
        {
            // Is field marked as embedded?
            embedded = true;
        }
        else if (mmd.getEmbeddedMetaData() != null)
        {
            // Does this field have @Embedded/<embedded> definition?
            embedded = true;
        }
        else if (RelationType.isRelationMultiValued(relationType))
        {
            // Is this an embedded element/key/value?
            if (mmd.hasCollection() && mmd.getElementMetaData() != null && mmd.getElementMetaData().getEmbeddedMetaData() != null)
            {
                // Embedded collection element
                embedded = true;
            }
            else if (mmd.hasArray() && mmd.getElementMetaData() != null && mmd.getElementMetaData().getEmbeddedMetaData() != null)
            {
                // Embedded array element
                embedded = true;
            }
            else if (mmd.hasMap() && 
                    ((mmd.getKeyMetaData() != null && mmd.getKeyMetaData().getEmbeddedMetaData() != null) || 
                    (mmd.getValueMetaData() != null && mmd.getValueMetaData().getEmbeddedMetaData() != null)))
            {
                // Embedded map key/value
                embedded = true;
            }
        }
        else
        {
            // Observe "embeddedOnly" of the persisted type
            if (RelationType.isRelationSingleValued(relationType))
            {
                AbstractClassMetaData mmdCmd = mmgr.getMetaDataForClass(mmd.getType(), clr);
                if (mmdCmd != null && mmdCmd.isEmbeddedOnly())
                {
                    embedded = true;
                }
            }
            else if (RelationType.isRelationMultiValued(relationType))
            {
                // TODO Check this too
            }
        }
        return embedded;
    }

    /**
     * Convenience method to return if the specified member is embedded.
     * Only applies to relation fields, since all other fields are always "embedded".
     * TODO Likely ought to change last arg to List&lt;AbstractMemberMetaData&gt; for multilevel of embedded
     * @param mmgr Metadata manager
     * @param clr ClassLoader resolver
     * @param mmd Metadata for the member we are interested in
     * @param relationType Relation type of the member we are interested in
     * @param ownerMmd Optional metadata for the owner member (for nested embeddeds only. Set to null if not relevant to the member in question).
     * @return Whether the member is embedded
     */
    public boolean isMemberEmbedded(MetaDataManager mmgr, ClassLoaderResolver clr, AbstractMemberMetaData mmd, RelationType relationType, AbstractMemberMetaData ownerMmd)
    {
        boolean embedded = false;
        if (relationType != RelationType.NONE)
        {
            // Determine if this relation field is embedded
            if (RelationType.isRelationSingleValued(relationType))
            {
                AbstractClassMetaData mmdCmd = mmgr.getMetaDataForClass(mmd.getType(), clr);
                if (mmdCmd != null && mmdCmd.isEmbeddedOnly())
                {
                    // Member type is embedded-only, so has to be embedded
                    return true;
                }
            }
            if (mmd.isEmbedded() || mmd.getEmbeddedMetaData() != null)
            {
                // Does this field have @Embedded definition?
                return true;
            }
            else if (RelationType.isRelationMultiValued(relationType))
            {
                // Is this an embedded element/key/value?
                if (mmd.hasCollection() && mmd.getElementMetaData() != null)
                {
                    if (mmd.getElementMetaData().getEmbeddedMetaData() != null)
                    {
                        // Full embedded definition for element provided
                        return true;
                    }
                    else if (mmd.getCollection().elementIsPersistent() && mmd.getCollection().isEmbeddedElement())
                    {
                        // Element simply marked as embedded, but is persistent
                        return true;
                    }
                }
                else if (mmd.hasArray() && mmd.getElementMetaData() != null)
                {
                    if (mmd.getElementMetaData().getEmbeddedMetaData() != null)
                    {
                        // Full embedded definition for element provided
                        return true;
                    }
                    else if (mmd.getArray().elementIsPersistent() && mmd.getArray().isEmbeddedElement())
                    {
                        // Element simply marked as embedded, but is persistent
                        return true;
                    }
                }
                else if (mmd.hasMap())
                {
                    if (mmd.getKeyMetaData() != null && mmd.getKeyMetaData().getEmbeddedMetaData() != null)
                    {
                        // Full embedded definition for key provided
                        return true; 
                    }
                    else if (mmd.getMap().keyIsPersistent() && mmd.getMap().isEmbeddedKey())
                    {
                        // Key simply marked as embedded, but is persistent
                        return true;
                    }
                    if (mmd.getValueMetaData() != null && mmd.getValueMetaData().getEmbeddedMetaData() != null)
                    {
                        // Full embedded definition for value provided
                        return true;
                    }
                    else if (mmd.getMap().valueIsPersistent() && mmd.getMap().isEmbeddedValue())
                    {
                        // Value simply marked as embedded, but is persistent
                        return true;
                    }
                }
            }

            if (RelationType.isRelationSingleValued(relationType) && ownerMmd != null)
            {
                // Maybe part of a nested embedded?
                if (ownerMmd.hasCollection())
                {
                    // This is a field of the element of the collection, so check for any metadata spec for it
                    EmbeddedMetaData embmd = ownerMmd.getElementMetaData().getEmbeddedMetaData();
                    if (embmd != null)
                    {
                        AbstractMemberMetaData[] embMmds = embmd.getMemberMetaData();
                        if (embMmds != null)
                        {
                            for (AbstractMemberMetaData embMmd : embMmds)
                            {
                                if (embMmd.getName().equals(mmd.getName()))
                                {
                                    if (embMmd.isEmbedded() || embMmd.getEmbeddedMetaData() != null)
                                    {
                                        // Embedded Field is marked in nested embedded definition as embedded
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                else if (ownerMmd.getEmbeddedMetaData() != null)
                {
                    // Is this a nested embedded (from JDO definition) with specification for this field?
                    AbstractMemberMetaData[] embMmds = ownerMmd.getEmbeddedMetaData().getMemberMetaData();
                    if (embMmds != null)
                    {
                        for (int i=0;i<embMmds.length;i++)
                        {
                            if (embMmds[i].getName().equals(mmd.getName()))
                            {
                                // Does it have an <embedded> block?
                                return embMmds[i].getEmbeddedMetaData() != null;
                            }
                        }
                    }
                }
            }
        }

        return embedded;
    }
}