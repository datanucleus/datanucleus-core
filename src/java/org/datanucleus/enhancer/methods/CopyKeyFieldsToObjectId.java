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
package org.datanucleus.enhancer.methods;

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

/**
 * Method to generate the method "jdoCopyKeyFieldsToObjectId" using ASM.
 * For datastore/nondurable identity
 * <pre>
 * public void jdoCopyKeyFieldsToObjectId(Object oid)
 * {
 * }
 * </pre>
 * and for SingleFieldIdentity
 * <pre>
 * public void jdoCopyKeyFieldsToObjectId(Object oid)
 * {
 *     throw new JDOFatalInternalException("It's illegal to call ...");
 * }
 * </pre>
 * and for user-supplied app identity
 * <pre>
 * public void jdoCopyKeyFieldsToObjectId(Object oid)
 * {
 *     if (!(oid instanceof UserPrimaryKey))
 *         throw new ClassCastException("key class is not mydomain.UserPrimaryKey or null");
 *     UserPrimaryKey o = (UserPrimaryKey) oid;
 *     o.zzz1 = zzz1;
 *     o.zzz2 = zzz2;
 * }
 * </pre>
 * and for Compound Identity
 * <pre>
 * public void jdoCopyKeyFieldsToObjectId(Object oid)
 * {
 *     if (!(oid instanceof UserPrimaryKey))
 *         throw new ClassCastException("key class is not mydomain.UserPrimaryKey or null");
 *     UserPrimaryKey o = (UserPrimaryKey) oid;
 *     o.zzz1 = zzz1;
 *     o.zzz2 = (ZZZ2.Key) JDOHelper.getObjectId(zzz2);
 * }
 * </pre>
 * @version $Revision: 1.16 $
 */
public class CopyKeyFieldsToObjectId extends ClassMethod
{
    public static CopyKeyFieldsToObjectId getInstance(ClassEnhancer enhancer)
    {
        return new CopyKeyFieldsToObjectId(enhancer, enhancer.getNamer().getCopyKeyFieldsToObjectIdMethodName(),
            Opcodes.ACC_PUBLIC/* | Opcodes.ACC_FINAL*/,
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
    public CopyKeyFieldsToObjectId(ClassEnhancer enhancer, String name, int access, 
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
                if (enhancer.getMetaDataManager().getApiAdapter().isSingleFieldIdentityClass(objectIdClass))
                {
                    // SingleFieldIdentity
                    Label startLabel = new Label();
                    visitor.visitLabel(startLabel);

                    visitor.visitTypeInsn(Opcodes.NEW, getNamer().getFatalInternalExceptionAsmClassName());
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitLdcInsn("It's illegal to call jdoCopyKeyFieldsToObjectId for a class with single-field identity.");
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, getNamer().getFatalInternalExceptionAsmClassName(), "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitLocalVariable(argNames[0], EnhanceUtils.CD_Object, null, startLabel, endLabel, 1);
                    visitor.visitMaxs(3, 2);
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
                    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    visitor.visitTypeInsn(Opcodes.CHECKCAST, ACN_objectIdClass);
                    visitor.visitVarInsn(Opcodes.ASTORE, 2);

                    visitor.visitLabel(l0);

                    int[] pkFieldNums = enhancer.getClassMetaData().getPKMemberPositions();
                    Label reflectionFieldStart = null;
                    for (int i=0;i<pkFieldNums.length;i++)
                    {
                        AbstractMemberMetaData fmd = enhancer.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[i]);
                        String fieldTypeDesc = Type.getDescriptor(fmd.getType());
                        AbstractClassMetaData acmd = enhancer.getMetaDataManager().getMetaDataForClass(
                            fmd.getType(), enhancer.getClassLoaderResolver());
                        int pkFieldModifiers = ClassUtils.getModifiersForFieldOfClass(enhancer.getClassLoaderResolver(), 
                            objectIdClass, fmd.getName());

                        // Check if the PK field type is a PC (CompoundIdentity)
                        if (acmd != null && acmd.getIdentityType() != IdentityType.NONDURABLE)
                        {
                            // CompoundIdentity, this field of the PK is a PC
                            if (fmd instanceof PropertyMetaData)
                            {
                                // Persistent Property so use o.setXXX((XXX.Key)JDOHelper.getObjectId(jdoGetXXX()))
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                                    getNamer().getGetMethodPrefixMethodName() + fmd.getName(), "()" + Type.getDescriptor(fmd.getType()));
                                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, getNamer().getHelperAsmClassName(), // TODO Remove this and call pc.jdoGetObjectId
                                    "getObjectId", "(Ljava/lang/Object;)Ljava/lang/Object;");
                                visitor.visitTypeInsn(Opcodes.CHECKCAST, acmd.getObjectidClass().replace('.', '/'));
                                // TODO Use properties here
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, ACN_objectIdClass,
                                    fmd.getName(), "L" + acmd.getObjectidClass().replace('.', '/') + ";");
                            }
                            else if (Modifier.isPublic(pkFieldModifiers))
                            {
                                // Persistent Field public, so use o.xxx = (XXX.Key)JDOHelper.getObjectId(xxx);
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), 
                                    fmd.getName(), fieldTypeDesc);
                                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, getNamer().getHelperAsmClassName(), // TODO Remove this and call pc.jdoGetObjectId
                                    "getObjectId", "(Ljava/lang/Object;)Ljava/lang/Object;");
                                visitor.visitTypeInsn(Opcodes.CHECKCAST, acmd.getObjectidClass().replace('.', '/'));
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, ACN_objectIdClass,
                                    fmd.getName(), "L" + acmd.getObjectidClass().replace('.', '/') + ";");
                            }
                            else
                            {
                                // Persistent Field private/protected so use reflection
                                // TODO Use reflection here
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), 
                                    fmd.getName(), fieldTypeDesc);
                                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, getNamer().getHelperAsmClassName(), // TODO Remove this and call pc.jdoGetObjectId
                                    "getObjectId", "(Ljava/lang/Object;)Ljava/lang/Object;");
                                visitor.visitTypeInsn(Opcodes.CHECKCAST, acmd.getObjectidClass().replace('.', '/'));
                                // TODO Use reflection here
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, ACN_objectIdClass,
                                    fmd.getName(), "L" + acmd.getObjectidClass().replace('.', '/') + ";");
                            }
                        }
                        else
                        {
                            // Standard application-identity field
                            if (fmd instanceof PropertyMetaData)
                            {
                                // Persistent Property so use o.setXXX(jdoGetXXX())
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                                    getNamer().getGetMethodPrefixMethodName() + fmd.getName(), "()" + Type.getDescriptor(fmd.getType()));
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ACN_objectIdClass, 
                                    ClassUtils.getJavaBeanSetterName(fmd.getName()), "(" + fieldTypeDesc + ")V");
                            }
                            else if (Modifier.isPublic(pkFieldModifiers))
                            {
                                // Persistent Field public, so use o.xxx = xxx
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), 
                                    fmd.getName(), fieldTypeDesc);
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, ACN_objectIdClass, fmd.getName(), fieldTypeDesc);
                            }
                            else
                            {
                                // Persistent Field private/protected so use reflection, generating
                                // "Field field = o.getClass().getDeclaredField("pmIDFloat");"
                                // "field.setAccessible(true);"
                                // "field.set(o, pmIDFloat);"

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
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitVarInsn(Opcodes.ALOAD, 2);
                                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                                visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                                    fmd.getName(), fieldTypeDesc);
                                if (fmd.getTypeName().equals("boolean"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setBoolean", "(Ljava/lang/Object;Z)V");
                                }
                                else if (fmd.getTypeName().equals("byte"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setByte", "(Ljava/lang/Object;B)V");
                                }
                                else if (fmd.getTypeName().equals("char"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setChar", "(Ljava/lang/Object;C)V");
                                }
                                else if (fmd.getTypeName().equals("double"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setDouble", "(Ljava/lang/Object;D)V");
                                }
                                else if (fmd.getTypeName().equals("float"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setFloat", "(Ljava/lang/Object;F)V");
                                }
                                else if (fmd.getTypeName().equals("int"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setInt", "(Ljava/lang/Object;I)V");
                                }
                                else if (fmd.getTypeName().equals("long"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "setLong", "(Ljava/lang/Object;J)V");
                                }
                                else if (fmd.getTypeName().equals("short"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "setShort", "(Ljava/lang/Object;S)V");
                                }
                                else
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "set", "(Ljava/lang/Object;Ljava/lang/Object;)V");
                                }
                            }
                        }
                    }

                    // catch of the try-catch
                    visitor.visitLabel(l1);
                    Label l16 = new Label();
                    visitor.visitJumpInsn(Opcodes.GOTO, l16);
                    visitor.visitLabel(l2);
                    visitor.visitFrame(Opcodes.F_FULL, 3, new Object[] {
                            getClassEnhancer().getASMClassName(), "java/lang/Object", ACN_objectIdClass},
                            1, new Object[] {"java/lang/Exception"});

                    visitor.visitVarInsn(Opcodes.ASTORE, 3);
                    visitor.visitLabel(l16);
                    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

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
