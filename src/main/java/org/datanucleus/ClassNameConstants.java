/**********************************************************************
Copyright (c) 2005 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus;

import java.io.Serializable;

import org.datanucleus.enhancer.Persistable;
import org.datanucleus.state.StateManager;

/**
 * Constants with classes names (created to reduce overhead on calling Class.class.getName()), namely performance.
 */
public class ClassNameConstants
{
    public static final String Object = Object.class.getName();
    public static final String Serializable = Serializable.class.getName();

    public static final String BOOLEAN = ClassConstants.BOOLEAN.getName();
    public static final String BYTE = ClassConstants.BYTE.getName();
    public static final String CHAR = ClassConstants.CHAR.getName();
    public static final String DOUBLE = ClassConstants.DOUBLE.getName();
    public static final String FLOAT = ClassConstants.FLOAT.getName();
    public static final String INT = ClassConstants.INT.getName();
    public static final String LONG = ClassConstants.LONG.getName();
    public static final String SHORT = ClassConstants.SHORT.getName();

    public static final String JAVA_LANG_BOOLEAN = ClassConstants.JAVA_LANG_BOOLEAN.getName();
    public static final String JAVA_LANG_BYTE = ClassConstants.JAVA_LANG_BYTE.getName();
    public static final String JAVA_LANG_CHARACTER = ClassConstants.JAVA_LANG_CHARACTER.getName();
    public static final String JAVA_LANG_DOUBLE = ClassConstants.JAVA_LANG_DOUBLE.getName();
    public static final String JAVA_LANG_FLOAT = ClassConstants.JAVA_LANG_FLOAT.getName();
    public static final String JAVA_LANG_INTEGER = ClassConstants.JAVA_LANG_INTEGER.getName();
    public static final String JAVA_LANG_LONG = ClassConstants.JAVA_LANG_LONG.getName();
    public static final String JAVA_LANG_SHORT = ClassConstants.JAVA_LANG_SHORT.getName();

    public static final String JAVA_LANG_STRING = ClassConstants.JAVA_LANG_STRING.getName();
    public static final String JAVA_MATH_BIGDECIMAL = ClassConstants.JAVA_MATH_BIGDECIMAL.getName();
    public static final String JAVA_MATH_BIGINTEGER = ClassConstants.JAVA_MATH_BIGINTEGER.getName();
    public static final String JAVA_SQL_DATE = ClassConstants.JAVA_SQL_DATE.getName();
    public static final String JAVA_SQL_TIME = ClassConstants.JAVA_SQL_TIME.getName();
    public static final String JAVA_SQL_TIMESTAMP = ClassConstants.JAVA_SQL_TIMESTAMP.getName();
    public static final String JAVA_UTIL_DATE = ClassConstants.JAVA_UTIL_DATE.getName();
    public static final String JAVA_IO_SERIALIZABLE = ClassConstants.JAVA_IO_SERIALIZABLE.getName();

    public static final String BOOLEAN_ARRAY = boolean[].class.getName();
    public static final String BYTE_ARRAY = byte[].class.getName();
    public static final String CHAR_ARRAY = char[].class.getName();
    public static final String DOUBLE_ARRAY = double[].class.getName();
    public static final String FLOAT_ARRAY = float[].class.getName();
    public static final String INT_ARRAY = int[].class.getName();
    public static final String LONG_ARRAY = long[].class.getName();
    public static final String SHORT_ARRAY = short[].class.getName();

    public static final String JAVA_LANG_BOOLEAN_ARRAY = Boolean[].class.getName();
    public static final String JAVA_LANG_BYTE_ARRAY= Byte[].class.getName();
    public static final String JAVA_LANG_CHARACTER_ARRAY = Character[].class.getName();
    public static final String JAVA_LANG_DOUBLE_ARRAY = Double[].class.getName();
    public static final String JAVA_LANG_FLOAT_ARRAY = Float[].class.getName();
    public static final String JAVA_LANG_INTEGER_ARRAY = Integer[].class.getName();
    public static final String JAVA_LANG_LONG_ARRAY = Long[].class.getName();
    public static final String JAVA_LANG_SHORT_ARRAY = Short[].class.getName();

    // Identity classes
    public static final String IDENTITY_SINGLEFIELD_LONG = ClassConstants.IDENTITY_SINGLEFIELD_LONG.getName();
    public static final String IDENTITY_SINGLEFIELD_INT = ClassConstants.IDENTITY_SINGLEFIELD_INT.getName();
    public static final String IDENTITY_SINGLEFIELD_STRING = ClassConstants.IDENTITY_SINGLEFIELD_STRING.getName();
    public static final String IDENTITY_SINGLEFIELD_CHAR = ClassConstants.IDENTITY_SINGLEFIELD_CHAR.getName();
    public static final String IDENTITY_SINGLEFIELD_BYTE = ClassConstants.IDENTITY_SINGLEFIELD_BYTE.getName();
    public static final String IDENTITY_SINGLEFIELD_OBJECT = ClassConstants.IDENTITY_SINGLEFIELD_OBJECT.getName();
    public static final String IDENTITY_SINGLEFIELD_SHORT = ClassConstants.IDENTITY_SINGLEFIELD_SHORT.getName();
    public static final String IDENTITY_OID_IMPL = ClassConstants.IDENTITY_OID_IMPL.getName();

    public static final String PERSISTABLE = Persistable.class.getName();
    public static final String STATE_MANAGER = StateManager.class.getName();
}