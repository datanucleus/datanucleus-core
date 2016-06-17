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

import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.enhancer.asm.Label;
import org.datanucleus.enhancer.asm.Opcodes;

/**
 * Method to generate the method "dnIsDirty" using ASM.
 * <pre>
 * public final boolean dnIsDirty()
 * {
 *     if (dnStateManager != null)
 *         return dnStateManager.isDirty(this);
 *     if (this.dnIsDetached() != true)
 *         return false;
 *     if (((BitSet) dnDetachedState[3]).length() &le; 0)
 *         return false;
 *     return true;
 * }
 * </pre>
 * or if not detachable
 * <pre>
 * public final boolean dnIsDirty()
 * {
 *     if (dnStateManager != null)
 *         return dnStateManager.isDirty(this);
 *     return true;
 * }
 * </pre>
 */
public class IsDirty extends ClassMethod
{
    public static IsDirty getInstance(ClassEnhancer enhancer)
    {
        return new IsDirty(enhancer, enhancer.getNamer().getIsDirtyMethodName(),
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, boolean.class, null, null);
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
    public IsDirty(ClassEnhancer enhancer, String name, int access, 
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

        boolean detachable = enhancer.getClassMetaData().isDetachable();

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        Label l1 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNULL, l1);
        Label l2 = new Label();
        visitor.visitLabel(l2);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(),
            "isDirty", "(" + getNamer().getPersistableDescriptor() + ")Z");
        visitor.visitInsn(Opcodes.IRETURN);
        visitor.visitLabel(l1);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        if (!detachable)
        {
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitInsn(Opcodes.IRETURN);
        }
        else
        {
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(),
                getNamer().getIsDetachedMethodName(), "()Z");
            Label l3 = new Label();
            visitor.visitJumpInsn(Opcodes.IFNE, l3);
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitInsn(Opcodes.IRETURN);
            visitor.visitLabel(l3);
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                getNamer().getDetachedStateFieldName(), "[" + EnhanceUtils.CD_Object);
            visitor.visitInsn(Opcodes.ICONST_3);
            visitor.visitInsn(Opcodes.AALOAD);
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/util/BitSet");
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "length", "()I");
            Label l5 = new Label();
            visitor.visitJumpInsn(Opcodes.IFGT, l5);
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitInsn(Opcodes.IRETURN);

            visitor.visitLabel(l5);
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

            visitor.visitInsn(Opcodes.ICONST_1);
            visitor.visitInsn(Opcodes.IRETURN);
        }

        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitMaxs(2, 1);

        visitor.visitEnd();
    }
}
