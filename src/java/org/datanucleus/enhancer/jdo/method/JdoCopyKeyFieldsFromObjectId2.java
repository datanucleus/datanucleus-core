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
package org.datanucleus.enhancer.jdo.method;

import java.lang.reflect.Modifier;

import org.datanucleus.asm.Label;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.JavaUtils;

/**
 * Method to generate the method "jdoCopyKeyFieldsFromObjectId" using ASM.
 * For datastore/nondurable identity
 * <pre>
 * protected void jdoCopyKeyFieldsFromObjectId(Object object)
 * {
 * }
 * </pre>
 * and for SingleFieldIdentity
 * <pre>
 * protected void jdoCopyKeyFieldsFromObjectId(Object oid)
 * {
 *     if (!(oid instanceof YYYIdentity))
 *         throw new ClassCastException("key class is not YYYIdentity or null");
 *     YYYIdentity o = (YYYIdentity) oid;
 *     id = o.getKey();
 * }
 * </pre>
 * and for user-supplied app identity
 * <pre>
 * protected void jdoCopyKeyFieldsFromObjectId(Object oid)
 * {
 *     if (!(oid instanceof UserPrimaryKey))
 *         throw new ClassCastException("key class is not mydomain.UserPrimarKey or null");
 *     UserPrimaryKey o = (UserPrimaryKey) oid;
 *     try
 *     {
 *         zzz1 = o.zzz1;
 *         zzz2 = o.zzz2;
 *     }
 *     catch(Exception e) {}
 * }
 * </pre>
 * and for CompoundIdentity
 * <pre>
 * protected void jdoCopyKeyFieldsFromObjectId(Object oid)
 * {
 *     if (!(oid instanceof UserPrimaryKey))
 *         throw new ClassCastException("key class is not mydomain.UserPrimarKey or null");
 *     UserPrimaryKey o = (UserPrimaryKey) oid;
 *     try
 *     {
 *         zzz1 = o.zzz1;
 *         zzz2 = (ZZZ) this.jdoGetPersistenceManager().getObjectById(o.zzz, false);
 *     }
 *     catch (Exception e) {}
 * }
 * </pre>
 * (the try-catch is for cases where we set the fields with reflection and it can throw an exception).
 * There are some differences for fields .v. properties and also if fields in the PK are private.
 * @version $Revision: 1.21 $
 */
public class JdoCopyKeyFieldsFromObjectId2 extends ClassMethod
{
    public static JdoCopyKeyFieldsFromObjectId2 getInstance(ClassEnhancer enhancer)
    {
        return new JdoCopyKeyFieldsFromObjectId2(enhancer, enhancer.getNamer().getCopyKeyFieldsFromObjectIdMethodName(), 
            Opcodes.ACC_PROTECTED,
            null, new Class[] {Object.class}, new String[] { "oid" });
    }

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param name Name of method
     * @param access Access type
     * @param returnType Return type
     * @param argTypes Argument types
     * @param argNames Argument names
     */
    public JdoCopyKeyFieldsFromObjectId2(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Method to add the contents of the class method.
     */
    public void execute()
    {
        visitor.visitCode();

        ClassMetaData cmd = enhancer.getClassMetaData();
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            // application identity
            if (!cmd.isInstantiable())
            {
                // Application identity but mapped-superclass with no PK defined, so just "return"
                Label startLabel = new Label();
                visitor.visitLabel(startLabel);
                visitor.visitInsn(Opcodes.RETURN);
                Label endLabel = new Label();
                visitor.visitLabel(endLabel);
                visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                visitor.visitLocalVariable(argNames[0], EnhanceUtils.CD_Object, null, startLabel, endLabel, 1);
                visitor.visitMaxs(0, 2);
            }
            else
            {
                String objectIdClass = cmd.getObjectidClass();
                String ACN_objectIdClass = objectIdClass.replace('.', '/');
                int[] pkFieldNums = enhancer.getClassMetaData().getPKMemberPositions();
                if (enhancer.getMetaDataManager().getApiAdapter().isSingleFieldIdentityClass(objectIdClass))
                {
                    // SingleFieldIdentity
                    Label startLabel = new Label();
                    visitor.visitLabel(startLabel);

                    // if (!(oid instanceof LongIdentity)) throw new ClassCastException("...")
                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    visitor.visitTypeInsn(Opcodes.INSTANCEOF, ACN_objectIdClass);
                    Label l1 = new Label();
                    visitor.visitJumpInsn(Opcodes.IFNE, l1);
                    visitor.visitTypeInsn(Opcodes.NEW, "java/lang/ClassCastException");
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitLdcInsn("key class is not " + objectIdClass + " or null");
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/ClassCastException", "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    // XXXIdentity o = (XXXIdentity) oid;
                    visitor.visitLabel(l1);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }

                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    visitor.visitTypeInsn(Opcodes.CHECKCAST, ACN_objectIdClass);
                    visitor.visitVarInsn(Opcodes.ASTORE, 2);

                    // id = o.getKey();
                    Label l5 = new Label();
                    visitor.visitLabel(l5);
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);

                    AbstractMemberMetaData fmd = enhancer.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[0]);
                    Class primitiveType = ClassUtils.getPrimitiveTypeForType(fmd.getType());
                    if (primitiveType != null)
                    {
                        // The PK field is a primitive wrapper so create wrapper from getKey()
                        String ACN_fieldType = fmd.getTypeName().replace('.', '/');
                        String getKeyReturnDesc = Type.getDescriptor(primitiveType);

                        visitor.visitVarInsn(Opcodes.ALOAD, 2);
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ACN_objectIdClass, 
                            "getKey", "()" + getKeyReturnDesc);
                        visitor.visitMethodInsn(Opcodes.INVOKESTATIC, ACN_fieldType, 
                            "valueOf", "(" + getKeyReturnDesc + ")L"+ ACN_fieldType + ";");
                    }
                    else
                    {
                        // PK field isn't a primitive wrapper
                        visitor.visitVarInsn(Opcodes.ALOAD, 2);
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ACN_objectIdClass, "getKey",
                            "()" + getNamer().getTypeDescriptorForSingleFieldIdentityGetKey(objectIdClass));
                        if (objectIdClass.equals(getNamer().getObjectIdentityClass().getName()))
                        {
                            // Cast to the right type
                            visitor.visitTypeInsn(Opcodes.CHECKCAST, fmd.getTypeName().replace('.', '/'));
                        }
                    }
                    if (fmd instanceof PropertyMetaData)
                    {
                        // Persistent property so use jdoSetXXX(...)
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                            getNamer().getSetMethodPrefixMethodName() + fmd.getName(), "(" + Type.getDescriptor(fmd.getType()) + ")V");
                    }
                    else
                    {
                        // Persistent field so use xxx = ...
                        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
                            fmd.getName(), Type.getDescriptor(fmd.getType()));
                    }
                    visitor.visitInsn(Opcodes.RETURN);

                    Label l7 = new Label();
                    visitor.visitLabel(l7);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, l7, 0);
                    visitor.visitLocalVariable(argNames[0], EnhanceUtils.CD_Object, null, startLabel, l7, 1);
                    visitor.visitLocalVariable("o", getNamer().getSingleFieldIdentityDescriptor(objectIdClass), null, l5, l7, 2);
                    visitor.visitMaxs(3, 3);
                }
                else
                {
                    // User-provided app identity, and compound identity
                    // Put try-catch around the field setting (for reflection cases)
                    Label l0 = new Label();
                    Label l1 = new Label();
                    Label l2 = new Label();
                    visitor.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");

                    Label startLabel = new Label();
                    visitor.visitLabel(startLabel);

                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    visitor.visitTypeInsn(Opcodes.INSTANCEOF, ACN_objectIdClass);
                    Label l4 = new Label();
                    visitor.visitJumpInsn(Opcodes.IFNE, l4);
                    visitor.visitTypeInsn(Opcodes.NEW, "java/lang/ClassCastException");
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitLdcInsn("key class is not " + objectIdClass + " or null");
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/ClassCastException",
                        "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    visitor.visitLabel(l4);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }

                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    visitor.visitTypeInsn(Opcodes.CHECKCAST, ACN_objectIdClass);
                    visitor.visitVarInsn(Opcodes.ASTORE, 2);

                    visitor.visitLabel(l0);

                    // Copy the PK members using the appropriate method for each field/property
                    Label reflectionFieldStart = null;
                    for (int i=0;i<pkFieldNums.length;i++)
                    {
                        AbstractMemberMetaData fmd = enhancer.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[i]);
                        String fieldTypeDesc = Type.getDescriptor(fmd.getType());
                        String fieldTypeName = fmd.getTypeName().replace('.', '/');
                        int pkFieldModifiers = ClassUtils.getModifiersForFieldOfClass(enhancer.getClassLoaderResolver(), 
                            objectIdClass, fmd.getName());

                        // Check if the PK field type is a PC (CompoundIdentity)
                        AbstractClassMetaData acmd = enhancer.getMetaDataManager().getMetaDataForClass(
                            fmd.getType(), enhancer.getClassLoaderResolver());
                        if (acmd != null && acmd.getIdentityType() != IdentityType.NONDURABLE)
                        {
                            // CompoundIdentity, this field of the PK is a PC
                            visitor.visitVarInsn(Opcodes.ALOAD, 0);
                            visitor.visitVarInsn(Opcodes.ALOAD, 0);
                            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(),
                                getNamer().getGetPersistenceManagerMethodName(), "()L" + getNamer().getPersistenceManagerAsmClassName() + ";");
                            visitor.visitVarInsn(Opcodes.ALOAD, 2);

                            // TODO Cater for property/private field cases
                            visitor.visitFieldInsn(Opcodes.GETFIELD, ACN_objectIdClass,
                                fmd.getName(), "L" + acmd.getObjectidClass().replace('.', '/') + ";");

                            visitor.visitInsn(Opcodes.ICONST_0);
                            visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getPersistenceManagerAsmClassName(),
                                "getObjectById", "(Ljava/lang/Object;Z)Ljava/lang/Object;");
                            visitor.visitTypeInsn(Opcodes.CHECKCAST, fieldTypeName);
                            if (fmd instanceof PropertyMetaData)
                            {
                                // Persistent property so use jdoSetXXX(...)
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                                    getNamer().getSetMethodPrefixMethodName() + fmd.getName(), "(" + Type.getDescriptor(fmd.getType()) + ")V");
                            }
                            else if (Modifier.isPublic(pkFieldModifiers))
                            {
                                // Persistent field that is public so use "xxx = ..."
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
                                    fmd.getName(), Type.getDescriptor(fmd.getType()));
                            }
                            else
                            {
                                // Persistent field that is protected/private so use reflection
                                // TODO Use reflection rather than "xxx = ..."
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
                                    fmd.getName(), Type.getDescriptor(fmd.getType()));
                            }
                        }
                        else
                        {
                            // Standard application-identity
                            if (fmd instanceof PropertyMetaData)
                            {
                                // Field in PK is property, hence use getXXX in PK to access value
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ACN_objectIdClass, 
                                    ClassUtils.getJavaBeanGetterName(fmd.getName(), fmd.getTypeName().equals("boolean")),
                                    "()" + Type.getDescriptor(fmd.getType()));
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                                    getNamer().getSetMethodPrefixMethodName() + fmd.getName(), "(" + Type.getDescriptor(fmd.getType()) + ")V");
                            }
                            else if (Modifier.isPublic(pkFieldModifiers))
                            {
                                // Field in PK is public so access directly
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitFieldInsn(Opcodes.GETFIELD, ACN_objectIdClass,
                                    fmd.getName(), fieldTypeDesc);
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
                                    fmd.getName(), fieldTypeDesc);
                            }
                            else
                            {
                                // Field in PK is protected/private so use reflection, generating
                                // "Field field = o.getClass().getDeclaredField("pmIDFloat");"
                                // "field.setAccessible(true);"
                                // "pmIDFloat = (Float) field.get(o);"

                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", 
                                    "getClass", "()Ljava/lang/Class;");
                                visitor.visitLdcInsn(fmd.getName());
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", 
                                    "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
                                visitor.visitVarInsn(Opcodes.ASTORE, 3);
                                if (reflectionFieldStart == null)
                                {
                                    reflectionFieldStart = new Label();
                                    visitor.visitLabel(reflectionFieldStart);
                                }
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitInsn(Opcodes.ICONST_1);
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                    "setAccessible", "(Z)V");
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                if (fmd.getTypeName().equals("boolean"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getBoolean", "(Ljava/lang/Object;)Z");
                                }
                                else if (fmd.getTypeName().equals("byte"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getByte", "(Ljava/lang/Object;)B");
                                }
                                else if (fmd.getTypeName().equals("char"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getChar", "(Ljava/lang/Object;)C");
                                }
                                else if (fmd.getTypeName().equals("double"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getDouble", "(Ljava/lang/Object;)D");
                                }
                                else if (fmd.getTypeName().equals("float"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getFloat", "(Ljava/lang/Object;)F");
                                }
                                else if (fmd.getTypeName().equals("int"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getInt", "(Ljava/lang/Object;)I");
                                }
                                else if (fmd.getTypeName().equals("long"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getLong", "(Ljava/lang/Object;)L");
                                }
                                else if (fmd.getTypeName().equals("short"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getShort", "(Ljava/lang/Object;)S");
                                }
                                else if (fmd.getTypeName().equals("java.lang.String"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
                                    visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
                                }
                                else
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
                                    visitor.visitTypeInsn(Opcodes.CHECKCAST, fieldTypeName);
                                }
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), 
                                    fmd.getName(), fieldTypeDesc);
                            }
                        }
                    }

                    // catch of the try-catch
                    visitor.visitLabel(l1);
                    Label l16 = new Label();
                    visitor.visitJumpInsn(Opcodes.GOTO, l16);
                    visitor.visitLabel(l2);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_FULL, 3, new Object[] {getClassEnhancer().getASMClassName(), "java/lang/Object", ACN_objectIdClass}, 1, new Object[] {"java/lang/Exception"});
                    }

                    visitor.visitVarInsn(Opcodes.ASTORE, 3);
                    visitor.visitLabel(l16);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }

                    visitor.visitInsn(Opcodes.RETURN);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitLocalVariable(argNames[0], EnhanceUtils.CD_Object, null, startLabel, endLabel, 1);
                    visitor.visitLocalVariable("o", "L" + ACN_objectIdClass + ";", null, l0, endLabel, 2);
                    if (reflectionFieldStart != null)
                    {
                        visitor.visitLocalVariable("field", "Ljava/lang/reflect/Field;", null, reflectionFieldStart, l2, 3);
                        visitor.visitMaxs(3, 4);
                    }
                    else
                    {
                        visitor.visitMaxs(3, 3);
                    }
                }
            }
        }
        else
        {
            // datastore/nondurable identity
            Label startLabel = new Label();
            visitor.visitLabel(startLabel);
            visitor.visitInsn(Opcodes.RETURN);
            Label l1 = new Label();
            visitor.visitLabel(l1);
            visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, l1, 0);
            visitor.visitLocalVariable(argNames[0], EnhanceUtils.CD_Object, null, startLabel, l1, 1);
            visitor.visitMaxs(0, 2);
        }

        visitor.visitEnd();
    }
}
