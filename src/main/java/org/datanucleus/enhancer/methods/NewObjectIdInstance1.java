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
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.util.ClassUtils;

/**
 * Method to generate the method "dnNewObjectIdInstance" using ASM.
 * For datastore/nondurable identity this is
 * <pre>
 * public Object dnNewObjectIdInstance()
 * {
 *     return null;
 * }
 * </pre>
 * and for SingleFieldIdentity
 * <pre>
 * public Object dnNewObjectIdInstance()
 * {
 *     return new YYYIdentity(getClass(), id);
 * }
 * </pre>
 * and for user-supplied object identity class
 * <pre>
 * public Object dnNewObjectIdInstance()
 * {
 *     return new UserPrimaryKey();
 * }
 * </pre>
 */
public class NewObjectIdInstance1 extends ClassMethod
{
    public static NewObjectIdInstance1 getInstance(ClassEnhancer enhancer)
    {
        return new NewObjectIdInstance1(enhancer, enhancer.getNamer().getNewObjectIdInstanceMethodName(),
            Opcodes.ACC_PUBLIC/* | Opcodes.ACC_FINAL*/,
            Object.class, null, null);
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
    public NewObjectIdInstance1(ClassEnhancer enhancer, String name, int access, 
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
                visitor.visitMaxs(3, 1);
            }
            else
            {
                String objectIdClass = cmd.getObjectidClass();
                int[] pkFieldNums = cmd.getPKMemberPositions();
                if (IdentityUtils.isSingleFieldIdentityClass(objectIdClass))
                {
                    // SingleFieldIdentity
                    String ACN_objectIdClass = objectIdClass.replace('.', '/');
                    AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[0]);

                    visitor.visitTypeInsn(Opcodes.NEW, ACN_objectIdClass);
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    if (fmd instanceof PropertyMetaData)
                    {
                        // Persistent property so use dnGetXXX()
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                            getNamer().getGetMethodPrefixMethodName() + fmd.getName(), "()" + Type.getDescriptor(fmd.getType()));
                    }
                    else
                    {
                        // Persistent field so use xxx
                        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                            fmd.getName(), Type.getDescriptor(fmd.getType()));
                    }
                    Class primitiveType = ClassUtils.getPrimitiveTypeForType(fmd.getType());
                    if (primitiveType != null)
                    {
                        // Using object wrapper of primitive so use wrapper constructor
                        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ACN_objectIdClass, "<init>",
                            "(Ljava/lang/Class;" + Type.getDescriptor(fmd.getType()) + ")V");
                    }
                    else
                    {
                        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ACN_objectIdClass, "<init>",
                            "(Ljava/lang/Class;" + getNamer().getTypeDescriptorForSingleFieldIdentityGetKey(objectIdClass) + ")V");
                    }

                    visitor.visitInsn(Opcodes.ARETURN);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitMaxs(4, 1);
                }
                else
                {
                    // User-provided app identity, and compound identity
                    String ACN_objectIdClass = objectIdClass.replace('.', '/');

                    visitor.visitTypeInsn(Opcodes.NEW, ACN_objectIdClass);
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, ACN_objectIdClass, "<init>", "()V");
                    visitor.visitInsn(Opcodes.ARETURN);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitMaxs(2, 1);
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
            visitor.visitMaxs(1, 1);
        }

        visitor.visitEnd();
    }
}