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

/**
 * Method to generate the method "dnNewInstance" using ASM.
 * <pre>
 * public Persistable dnNewInstance(StateManager sm, Object o)
 * {
 *     Answer result = new Answer();
 *     result.dnFlags = (byte) 1;
 *     result.dnStateManager = sm;
 *     result.dnCopyKeyFieldsFromObjectId(o);
 *     return result;
 * }
 * </pre>
 * and throw an exception when the class is abstract
 */
public class NewInstance2 extends ClassMethod
{
    public static NewInstance2 getInstance(ClassEnhancer enhancer)
    {
        return new NewInstance2(enhancer, enhancer.getNamer().getNewInstanceMethodName(), Opcodes.ACC_PUBLIC,
            enhancer.getNamer().getPersistableClass(), new Class[] {enhancer.getNamer().getStateManagerClass(), Object.class}, 
            new String[] {"sm", "obj"});
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
    public NewInstance2(ClassEnhancer enhancer, String name, int access, 
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

        if (enhancer.getClassMetaData().isAbstract())
        {
            visitor.visitTypeInsn(Opcodes.NEW, getNamer().getFatalInternalExceptionAsmClassName());
            visitor.visitInsn(Opcodes.DUP);
            visitor.visitLdcInsn("Cannot instantiate abstract class.");
            visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, getNamer().getFatalInternalExceptionAsmClassName(),
                "<init>", "(Ljava/lang/String;)V");
            visitor.visitInsn(Opcodes.ATHROW);

            Label endLabel = new Label();
            visitor.visitLabel(endLabel);
            visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
            visitor.visitLocalVariable(argNames[0], getNamer().getStateManagerDescriptor(), null, startLabel, endLabel, 1);
            visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
            visitor.visitMaxs(3, 3);
        }
        else
        {
            visitor.visitTypeInsn(Opcodes.NEW, getClassEnhancer().getASMClassName());
            visitor.visitInsn(Opcodes.DUP);
            visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, getClassEnhancer().getASMClassName(), "<init>", "()V");
            visitor.visitVarInsn(Opcodes.ASTORE, 3);
            Label l1 = new Label();
            visitor.visitLabel(l1);
            visitor.visitVarInsn(Opcodes.ALOAD, 3);
            visitor.visitInsn(Opcodes.ICONST_1);
            visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), getNamer().getFlagsFieldName(), "B");
            visitor.visitVarInsn(Opcodes.ALOAD, 3);
            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), 
                getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
            visitor.visitVarInsn(Opcodes.ALOAD, 3);
            visitor.visitVarInsn(Opcodes.ALOAD, 2);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                getNamer().getCopyKeyFieldsFromObjectIdMethodName(), "(" + EnhanceUtils.CD_Object + ")V");
            visitor.visitVarInsn(Opcodes.ALOAD, 3);
            visitor.visitInsn(Opcodes.ARETURN);

            Label endLabel = new Label();
            visitor.visitLabel(endLabel);
            visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
            visitor.visitLocalVariable(argNames[0], getNamer().getStateManagerDescriptor(), null, startLabel, endLabel, 1);
            visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
            visitor.visitLocalVariable("result", getClassEnhancer().getClassDescriptor(), null, l1, endLabel, 3);
            visitor.visitMaxs(2, 4);
        }

        visitor.visitEnd();
    }
}