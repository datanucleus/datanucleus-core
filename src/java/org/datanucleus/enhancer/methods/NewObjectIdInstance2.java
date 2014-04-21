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

import org.datanucleus.asm.Label;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.util.ClassUtils;

/**
 * Method to generate the method "dnNewObjectIdInstance" using ASM.
 * For datastore/nondurable identity this is
 * <pre>
 * public Object dnNewObjectIdInstance(Object key)
 * {
 *     return null;
 * }
 * </pre>
 * and for SingleFieldIdentity
 * <pre>
 * public Object dnNewObjectIdInstance(Object key)
 * {
 *     if (key == null)
 *         throw new IllegalArgumentException("key is null");
 *     if (key instanceof String != true)
 *         return new YYYIdentity(this.getClass(), (YYY) key);
 *     return new YYYIdentity(this.getClass(), (String) key);
 * }
 * </pre>
 * and for user-supplied object ids
 * <pre>
 * public Object dnNewObjectIdInstance(Object key)
 * {
 *     return new UserPrimaryKey((String) key);
 * }
 * </pre>
 */
public class NewObjectIdInstance2 extends ClassMethod
{
    public static NewObjectIdInstance2 getInstance(ClassEnhancer enhancer)
    {
        return new NewObjectIdInstance2(enhancer, enhancer.getNamer().getNewObjectIdInstanceMethodName(),
            Opcodes.ACC_PUBLIC/* | Opcodes.ACC_FINAL*/,
            Object.class, new Class[] {Object.class}, new String[] {"key"});
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
    public NewObjectIdInstance2(ClassEnhancer enhancer, String name, int access, 
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

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        ClassMetaData cmd = enhancer.getClassMetaData();
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            // application identity
            if (!cmd.isInstantiable())
            {
                // Application identity but mapped-superclass with no PK defined, so throw exception
                visitor.visitTypeInsn(Opcodes.NEW, getClassEnhancer().getNamer().getFatalInternalExceptionAsmClassName());
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitLdcInsn("This class has no identity");
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, 
                    getClassEnhancer().getNamer().getFatalInternalExceptionAsmClassName(), "<init>", "(Ljava/lang/String;)V");
                visitor.visitInsn(Opcodes.ATHROW);
                Label endLabel = new Label();
                visitor.visitLabel(endLabel);
                visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                visitor.visitLocalVariable("key", "Ljava/lang/Object;", null, startLabel, endLabel, 1);
                visitor.visitMaxs(3, 2);
            }
            else
            {
                String objectIdClass = cmd.getObjectidClass();
                int[] pkFieldNums = cmd.getPKMemberPositions();
                if (enhancer.getMetaDataManager().getApiAdapter().isSingleFieldIdentityClass(objectIdClass))
                {
                    // SingleFieldIdentity
                    String ACN_objectIdClass = objectIdClass.replace('.', '/');
                    AbstractMemberMetaData fmd = enhancer.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[0]);
                    {
                        // if (key == null) throw new IllegalArgumentException("...");
                        visitor.visitVarInsn(Opcodes.ALOAD, 1);
                        Label l1 = new Label();
                        visitor.visitJumpInsn(Opcodes.IFNONNULL, l1);
                        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                        visitor.visitInsn(Opcodes.DUP);
                        visitor.visitLdcInsn("key is null");
                        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
                        visitor.visitInsn(Opcodes.ATHROW);

                        // Object constructor : "if (key instanceof String != true) return new XXXIdentity(this.getClass(), key);"
                        visitor.visitLabel(l1);
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                        visitor.visitVarInsn(Opcodes.ALOAD, 1);
                        visitor.visitTypeInsn(Opcodes.INSTANCEOF, "java/lang/String");
                        Label l3 = new Label();
                        visitor.visitJumpInsn(Opcodes.IFNE, l3);

                        visitor.visitTypeInsn(Opcodes.NEW, ACN_objectIdClass);
                        visitor.visitInsn(Opcodes.DUP);
                        visitor.visitVarInsn(Opcodes.ALOAD, 0);
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                        visitor.visitVarInsn(Opcodes.ALOAD, 1);

                        String objectTypeInConstructor = EnhanceUtils.getASMClassNameForSingleFieldIdentityConstructor(fmd.getType());
                        Class primitiveType = ClassUtils.getPrimitiveTypeForType(fmd.getType());
                        if (primitiveType != null)
                        {
                            objectTypeInConstructor = fmd.getTypeName().replace('.', '/');
                        }
                        if (!objectIdClass.equals(getNamer().getObjectIdentityClass().getName()) || primitiveType != null)
                        {
                            // Add cast if using an Object based type or an Object wrapper of a primitive
                            visitor.visitTypeInsn(Opcodes.CHECKCAST, objectTypeInConstructor);
                        }
                        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ACN_objectIdClass, "<init>",
                            "(Ljava/lang/Class;" + "L" + objectTypeInConstructor + ";)V");
                        visitor.visitInsn(Opcodes.ARETURN);

                        // String constructor : "return new XXXIdentity(this.getClass(), (String) key);"
                        visitor.visitLabel(l3);
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                        visitor.visitTypeInsn(Opcodes.NEW, ACN_objectIdClass);
                        visitor.visitInsn(Opcodes.DUP);
                        visitor.visitVarInsn(Opcodes.ALOAD, 0);
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                        visitor.visitVarInsn(Opcodes.ALOAD, 1);
                        visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
                        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ACN_objectIdClass, "<init>",
                            "(Ljava/lang/Class;Ljava/lang/String;" + ")V");
                        visitor.visitInsn(Opcodes.ARETURN);

                        Label endLabel = new Label();
                        visitor.visitLabel(endLabel);
                        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                        visitor.visitLocalVariable("key", EnhanceUtils.CD_Object, null, startLabel, endLabel, 1);
                        visitor.visitMaxs(4, 2);
                    }
                }
                else
                {
                    // User-provided app identity, and compound identity
                    String ACN_objectIdClass = objectIdClass.replace('.', '/');

                    visitor.visitTypeInsn(Opcodes.NEW, ACN_objectIdClass);
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ACN_objectIdClass, "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ARETURN);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitLocalVariable(argNames[0], EnhanceUtils.CD_Object, null, startLabel, endLabel, 1);
                    visitor.visitMaxs(3, 2);
                    visitor.visitEnd();
                }
            }
        }
        else
        {
            // datastore/nondurable identity
            visitor.visitInsn(Opcodes.ACONST_NULL);
            visitor.visitInsn(Opcodes.ARETURN);

            Label endLabel = new Label();
            visitor.visitLabel(endLabel);
            visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
            visitor.visitLocalVariable(argNames[0], "Ljava/lang/Object;", null, startLabel, endLabel, 1);
            visitor.visitMaxs(1, 2);
        }

        visitor.visitEnd();
    }
}
