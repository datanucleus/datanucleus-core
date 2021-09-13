/**********************************************************************
Copyright (c) 2021 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.converters;

import java.lang.reflect.Method;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * Helper class providing conversion methods for use with enums.
 */
public class EnumConversionHelper
{
    public static Object getEnumForStoredValue(AbstractMemberMetaData mmd, FieldRole role, Object value, ClassLoaderResolver clr)
    {
        Class enumType = mmd.getType();
        String valueGetterMethodName = null;
        if (role == FieldRole.ROLE_FIELD && mmd.hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
        {
            valueGetterMethodName = mmd.getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
        }
        else if (role == FieldRole.ROLE_COLLECTION_ELEMENT)
        {
            enumType = clr.classForName(mmd.getCollection().getElementType());
            if (mmd.getElementMetaData() != null && mmd.getElementMetaData().hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
            {
                valueGetterMethodName = mmd.getElementMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
            }
        }
        else if (role == FieldRole.ROLE_ARRAY_ELEMENT)
        {
            enumType = clr.classForName(mmd.getArray().getElementType());
            if (mmd.getElementMetaData() != null && mmd.getElementMetaData().hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
            {
                valueGetterMethodName = mmd.getElementMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
            }
        }
        else if (role == FieldRole.ROLE_MAP_KEY)
        {
            enumType = clr.classForName(mmd.getMap().getKeyType());
            if (mmd.getKeyMetaData() != null && mmd.getKeyMetaData().hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
            {
                valueGetterMethodName = mmd.getKeyMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
            }
        }
        else if (role == FieldRole.ROLE_MAP_VALUE)
        {
            enumType = clr.classForName(mmd.getMap().getValueType());
            if (mmd.getValueMetaData() != null && mmd.getValueMetaData().hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
            {
                valueGetterMethodName = mmd.getValueMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
            }
        }

        if (valueGetterMethodName != null)
        {
            // Try using the enumConstants and the valueGetter
            Object[] enumConstants = enumType.getEnumConstants();
            Method valueGetterMethod = ClassUtils.getMethodForClass(enumType, valueGetterMethodName, null);
            if (valueGetterMethod != null)
            {
                // Search for this stored value from the enum constants
                for (int i=0;i<enumConstants.length;i++)
                {
                    try
                    {
                        Object enumValue = valueGetterMethod.invoke(enumConstants[i]);
                        if (enumValue.getClass() == value.getClass())
                        {
                            if (enumValue.equals(value))
                            {
                                return enumConstants[i];
                            }
                        }
                        else if (enumValue instanceof Number && value instanceof Number)
                        {
                            // Allow for comparisons between Long/Short/Integer
                            if (((Number)enumValue).intValue() == ((Number)value).intValue())
                            {
                                return enumConstants[i];
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        // Exception in invocation. Do something
                    }
                }
            }
        }

        return value instanceof String ? Enum.valueOf(enumType, (String)value) : enumType.getEnumConstants()[(int)value];
    }

    /**
     * Convenience method to return the "value" of an Enum, for a field and role.
     * Firstly checks for a defined method on the Enum that returns the "value", otherwise falls back to use the ordinal.
     * @param mmd Metadata for the member
     * @param role Role of the Enum in this member
     * @param myEnum The enum
     * @return The "value" (String or Integer)
     */
    public static Object getStoredValueFromEnum(AbstractMemberMetaData mmd, FieldRole role, Enum myEnum)
    {
        String methodName = null;

        boolean numeric = false; // When nothing is specified we align to the JDO default (since JPA will always have jdbcType)
        if (mmd != null)
        {
            ColumnMetaData[] colmds = null;
            if (role == FieldRole.ROLE_FIELD)
            {
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
                    if (mmd.getValueMetaData().hasExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER))
                    {
                        methodName = mmd.getValueMetaData().getValueForExtension(MetaData.EXTENSION_MEMBER_ENUM_VALUE_GETTER);
                    }
                    colmds = mmd.getValueMetaData().getColumnMetaData();
                }
            }

            if (methodName == null)
            {
                if (colmds != null && colmds.length == 1)
                {
                    if (MetaDataUtils.isJdbcTypeNumeric(colmds[0].getJdbcType()))
                    {
                        numeric = true;
                    }
                    else if (MetaDataUtils.isJdbcTypeString(colmds[0].getJdbcType()))
                    {
                        numeric = false;
                    }
                }
            }
        }

        if (methodName != null)
        {
            try
            {
                Method getterMethod = ClassUtils.getMethodForClass(myEnum.getClass(), methodName, null);
                return getterMethod.invoke(myEnum);
            }
            catch (Exception e)
            {
                NucleusLogger.PERSISTENCE.warn("Specified enum value-getter for method " + methodName + " on field " + mmd.getFullFieldName() + " gave an error on extracting the value", e);
            }
        }

        // Fallback to standard Enum handling via ordinal() or name()
        return numeric ? myEnum.ordinal() : myEnum.name();
    }
}