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

import org.datanucleus.asm.Label;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.util.JavaUtils;

/**
 * Method to generate the method "jdoReplaceDetachedState" using ASM.
 * <pre>
 * public final synchronized void jdoReplaceDetachedState()
 * {
 *     if (jdoStateManager == null)
 *         throw new IllegalStateException("state manager is null");
 *     this.jdoDetachedState = this.jdoStateManager.replacingDetachedState(this, jdoDetachedState);
 * }
 * </pre>
 */
public class JdoReplaceDetachedState extends ClassMethod
{
    public static JdoReplaceDetachedState getInstance(ClassEnhancer enhancer)
    {
        return new JdoReplaceDetachedState(enhancer, enhancer.getNamer().getReplaceDetachedStateMethodName(), 
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNCHRONIZED,
            null, null, null);
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
    public JdoReplaceDetachedState(ClassEnhancer enhancer, String name, int access, 
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
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        Label l1 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNONNULL, l1);
        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalStateException");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitLdcInsn("state manager is null");
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalStateException",
            "<init>", "(Ljava/lang/String;)V");
        visitor.visitInsn(Opcodes.ATHROW);

        visitor.visitLabel(l1);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getDetachedStateFieldName(), "[" + EnhanceUtils.CD_Object);
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(), "replacingDetachedState",
            "(L" + getNamer().getDetachableAsmClassName() + ";[" + EnhanceUtils.CD_Object + ")[" + EnhanceUtils.CD_Object);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getDetachedStateFieldName(), "[" + EnhanceUtils.CD_Object);
        visitor.visitInsn(Opcodes.RETURN);

        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitMaxs(4, 1);

        visitor.visitEnd();
    }
}
