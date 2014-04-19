/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancer;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.asm.MethodVisitor;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.asm.Type;
import org.datanucleus.util.Localiser;

/**
 * Utility class for bytecode enhancement using ASM.
 * ASM operates around two basic pieces of information about any type.
 * <ul>
 * <li><b>ASM class name (ACN)</b> : this is the normal fully qualified class name but replacing
 * the dots with slashes. So java.lang.String will have an ASM class name of "java/lang/String".</li>
 * <li><b>Class Descriptor (CD)</b> : this is used where a type is referred to in a calling sequence etc.
 * and for object types is typically things like "Ljava.lang.String;", but there are variants for primitives.</li>
 * </ul>
 */
public final class EnhanceUtils
{
    /** Localiser for messages. */
    protected static Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** ASM class name for boolean. */
    public final static String ACN_boolean = ClassNameConstants.BOOLEAN;

    /** ASM class name for byte. */
    public final static String ACN_byte = ClassNameConstants.BYTE;

    /** ASM class name for char. */
    public final static String ACN_char = ClassNameConstants.CHAR;

    /** ASM class name for double. */
    public final static String ACN_double = ClassNameConstants.DOUBLE;

    /** ASM class name for float. */
    public final static String ACN_float = ClassNameConstants.FLOAT;

    /** ASM class name for int. */
    public final static String ACN_int = ClassNameConstants.INT;

    /** ASM class name for long. */
    public final static String ACN_long = ClassNameConstants.LONG;

    /** ASM class name for short. */
    public final static String ACN_short = ClassNameConstants.SHORT;

    /** ASM class name for Boolean. */
    public final static String ACN_Boolean = ClassNameConstants.JAVA_LANG_BOOLEAN.replace('.', '/');

    /** ASM class name for Byte. */
    public final static String ACN_Byte = ClassNameConstants.JAVA_LANG_BYTE.replace('.', '/');

    /** ASM class name for Character. */
    public final static String ACN_Character = ClassNameConstants.JAVA_LANG_CHARACTER.replace('.', '/');

    /** ASM class name for Double. */
    public final static String ACN_Double = ClassNameConstants.JAVA_LANG_DOUBLE.replace('.', '/');

    /** ASM class name for Float. */
    public final static String ACN_Float = ClassNameConstants.JAVA_LANG_FLOAT.replace('.', '/');

    /** ASM class name for Integer. */
    public final static String ACN_Integer = ClassNameConstants.JAVA_LANG_INTEGER.replace('.', '/');

    /** ASM class name for Long. */
    public final static String ACN_Long = ClassNameConstants.JAVA_LANG_LONG.replace('.', '/');

    /** ASM class name for Short. */
    public final static String ACN_Short = ClassNameConstants.JAVA_LANG_SHORT.replace('.', '/');

    /** ASM class name for java.lang.String. */
    public final static String ACN_String = ClassNameConstants.JAVA_LANG_STRING.replace('.', '/');

    /** ASM class name for java.lang.Object. */
    public final static String ACN_Object = Object.class.getName().replace('.', '/');

    /** Class descriptor for String. */
    public final static String CD_String = Type.getDescriptor(String.class);

    /** Descriptor for java.lang.Object. */
    public final static String CD_Object = Type.getDescriptor(Object.class);

    /**
     * private constructor to prevent instantiation.
     */
    private EnhanceUtils()
    {
    }

    /**
     * Convenience method to add a BIPUSH-type int to the visitor.
     * @param visitor The MethodVisitor
     * @param i number
     */
    public static void addBIPUSHToMethod(MethodVisitor visitor, final int i)
    {
        if (i < 6)
        {
            switch (i)
            {
                case 0 :
                    visitor.visitInsn(Opcodes.ICONST_0);
                    break;
                case 1 :
                    visitor.visitInsn(Opcodes.ICONST_1);
                    break;
                case 2 :
                    visitor.visitInsn(Opcodes.ICONST_2);
                    break;
                case 3 :
                    visitor.visitInsn(Opcodes.ICONST_3);
                    break;
                case 4 :
                    visitor.visitInsn(Opcodes.ICONST_4);
                    break;
                case 5 :
                    visitor.visitInsn(Opcodes.ICONST_5);
                    break;
            }
        }
        else if (i < Byte.MAX_VALUE)
        {
            visitor.visitIntInsn(Opcodes.BIPUSH, i);
        }
        else if (i < Short.MAX_VALUE)
        {
            visitor.visitIntInsn(Opcodes.SIPUSH, i);
        }
    }

    /**
     * Convenience method to add a return statement based on the type to be returned.
     * @param visitor The MethodVisitor
     * @param type The type to return
     */
    public static void addReturnForType(MethodVisitor visitor, Class type)
    {
        if (type == int.class || type == boolean.class || type == byte.class || type == char.class || type == short.class)
        {
            visitor.visitInsn(Opcodes.IRETURN);
        }
        else if (type == double.class)
        {
            visitor.visitInsn(Opcodes.DRETURN);
        }
        else if (type == float.class)
        {
            visitor.visitInsn(Opcodes.FRETURN);
        }
        else if (type == long.class)
        {
            visitor.visitInsn(Opcodes.LRETURN);
        }
        else
        {
            visitor.visitInsn(Opcodes.ARETURN);
        }
    }

    /**
     * Convenience method to add a load statement based on the type to be loaded.
     * @param visitor The MethodVisitor
     * @param type The type to load
     * @param number Number to load
     */
    public static void addLoadForType(MethodVisitor visitor, Class type, int number)
    {
        if (type == int.class || type == boolean.class || type == byte.class || type == char.class || type == short.class)
        {
            visitor.visitVarInsn(Opcodes.ILOAD, number);
        }
        else if (type == double.class)
        {
            visitor.visitVarInsn(Opcodes.DLOAD, number);
        }
        else if (type == float.class)
        {
            visitor.visitVarInsn(Opcodes.FLOAD, number);
        }
        else if (type == long.class)
        {
            visitor.visitVarInsn(Opcodes.LLOAD, number);
        }
        else
        {
            visitor.visitVarInsn(Opcodes.ALOAD, number);
        }
    }

    /**
     * Convenience method to give the JDO method name given the type.
     * This is for the assorted methods on the StateManager called things like "replacingStringField",
     * "replacingObjectField", "providedIntField", etc. Just returns the "type" part of the name.
     * <ul>
     * <li>Boolean, bool : returns "Boolean"</li>
     * <li>Byte, byte : returns "Byte"</li>
     * <li>Character, char : returns "Char"</li>
     * <li>Double, double : returns "Double"</li>
     * <li>Float, float : returns "Float"</li>
     * <li>Integer, int : returns "Int"</li>
     * <li>Long, long : returns "Long"</li>
     * <li>Short, short : returns "Short"</li>
     * <li>String : returns "String"</li>
     * <li>all others : returns "Object"</li>
     * </ul>
     * @param cls The type of the field
     * @return Name for the method
     */
    public static String getTypeNameForPersistableMethod(Class cls)
    {
        if (cls == null)
        {
            return null;
        }
        else if (cls == ClassConstants.BOOLEAN)
        {
            return "Boolean";
        }
        else if (cls == ClassConstants.BYTE)
        {
            return "Byte";
        }
        else if (cls == ClassConstants.CHAR)
        {
            return "Char";
        }
        else if (cls == ClassConstants.DOUBLE)
        {
            return "Double";
        }
        else if (cls == ClassConstants.FLOAT)
        {
            return "Float";
        }
        else if (cls == ClassConstants.INT)
        {
            return "Int";
        }
        else if (cls == ClassConstants.LONG)
        {
            return "Long";
        }
        else if (cls == ClassConstants.SHORT)
        {
            return "Short";
        }
        else if (cls == ClassConstants.JAVA_LANG_STRING)
        {
            return "String";
        }
        // Byte, Boolean, Character, Double, Float, Integer, Long, Short go through Object too
        return "Object";
    }

    /**
     * Return the ASM type descriptor for the input class.
     * @param clsName The input class name
     * @return The ASM type descriptor name
     */
    public static String getTypeDescriptorForType(String clsName)
    {
        if (clsName == null)
        {
            return null;
        }
        else if (clsName.equals(ClassNameConstants.BOOLEAN))
        {
            return Type.BOOLEAN_TYPE.getDescriptor();
        }
        else if (clsName.equals(ClassNameConstants.BYTE))
        {
            return Type.BYTE_TYPE.getDescriptor();
        }
        else if (clsName.equals(ClassNameConstants.CHAR))
        {
            return Type.CHAR_TYPE.getDescriptor();
        }
        else if (clsName.equals(ClassNameConstants.DOUBLE))
        {
            return Type.DOUBLE_TYPE.getDescriptor();
        }
        else if (clsName.equals(ClassNameConstants.FLOAT))
        {
            return Type.FLOAT_TYPE.getDescriptor();
        }
        else if (clsName.equals(ClassNameConstants.INT))
        {
            return Type.INT_TYPE.getDescriptor();
        }
        else if (clsName.equals(ClassNameConstants.LONG))
        {
            return Type.LONG_TYPE.getDescriptor();
        }
        else if (clsName.equals(ClassNameConstants.SHORT))
        {
            return Type.SHORT_TYPE.getDescriptor();
        }
        else if (clsName.equals(ClassNameConstants.JAVA_LANG_STRING))
        {
            return CD_String;
        }
        else
        {
            return "L" + clsName.replace('.', '/') + ";";
        }
    }
    /**
     * Convenience method to give the descriptor for use in a JDO "field" method.
     * This is for the assorted methods on the JDO StateManager called things like "replacingStringField",
     * "replacingObjectField", "providedIntField", etc. Returns the ASM descriptor equivalent for the method used
     * <ul>
     * <li>Boolean, bool : returns "Boolean"</li>
     * <li>Byte, byte : returns "Byte"</li>
     * <li>Character, char : returns "Char"</li>
     * <li>Double, double : returns "Double"</li>
     * <li>Float, float : returns "Float"</li>
     * <li>Integer, int : returns "Int"</li>
     * <li>Long, long : returns "Long"</li>
     * <li>Short, short : returns "Short"</li>
     * <li>String : returns "String"</li>
     * <li>all others : returns "Object"</li>
     * </ul>
     * TODO Cache these descriptors/classes etc
     * @param cls The type of the field
     * @return Name for the method
     */
    public static String getTypeDescriptorForJDOMethod(Class cls)
    {
        if (cls == null)
        {
            return null;
        }
        else if (cls == ClassConstants.BOOLEAN)
        {
            return Type.BOOLEAN_TYPE.getDescriptor();
        }
        else if (cls == ClassConstants.BYTE)
        {
            return Type.BYTE_TYPE.getDescriptor();
        }
        else if (cls == ClassConstants.CHAR)
        {
            return Type.CHAR_TYPE.getDescriptor();
        }
        else if (cls == ClassConstants.DOUBLE)
        {
            return Type.DOUBLE_TYPE.getDescriptor();
        }
        else if (cls == ClassConstants.FLOAT)
        {
            return Type.FLOAT_TYPE.getDescriptor();
        }
        else if (cls == ClassConstants.INT)
        {
            return Type.INT_TYPE.getDescriptor();
        }
        else if (cls == ClassConstants.LONG)
        {
            return Type.LONG_TYPE.getDescriptor();
        }
        else if (cls == ClassConstants.SHORT)
        {
            return Type.SHORT_TYPE.getDescriptor();
        }
        else if (cls == ClassConstants.JAVA_LANG_STRING)
        {
            return CD_String;
        }
        return CD_Object;
    }

    /**
     * Convenience method to return the ASM class name to use as input via the SingleFieldIdentity constructor.
     * Means that if the fieldType is primitive we return the ASM class name of the object wrapper.
     * @param fieldType Type of the field
     * @return ASM class name to use as input in the constructor
     */
    public static String getASMClassNameForSingleFieldIdentityConstructor(Class fieldType)
    {
        if (fieldType == null)
        {
            return null;
        }
        else if (fieldType == ClassConstants.BYTE || fieldType == ClassConstants.JAVA_LANG_BYTE)
        {
            return ACN_Byte;
        }
        else if (fieldType == ClassConstants.CHAR || fieldType == ClassConstants.JAVA_LANG_CHARACTER)
        {
            return ACN_Character;
        }
        else if (fieldType == ClassConstants.INT || fieldType == ClassConstants.JAVA_LANG_INTEGER)
        {
            return ACN_Integer;
        }
        else if (fieldType == ClassConstants.LONG || fieldType == ClassConstants.JAVA_LANG_LONG)
        {
            return ACN_Long;
        }
        else if (fieldType == ClassConstants.SHORT || fieldType == ClassConstants.JAVA_LANG_SHORT)
        {
            return ACN_Short;
        }
        else if (fieldType == ClassConstants.JAVA_LANG_STRING)
        {
            return ACN_String;
        }
        else
        {
            return ACN_Object;
        }
    }

    public static int getAsmVersionForJRE()
    {
        // TODO Use V1_8 if the user is using JRE 1.8
        return Opcodes.V1_7;
    }
}