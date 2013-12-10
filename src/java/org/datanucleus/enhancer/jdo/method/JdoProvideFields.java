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
import org.datanucleus.util.JavaUtils;

/**
 * Method to generate the method "jdoProvideFields" using ASM.
 * <pre>
 * public final void jdoProvideFields(int[] fieldIds)
 * {
 *     if (fieldIds == null)
 *         throw new IllegalArgumentException("argment is null");
 *     int i = fieldIds.length - 1;
 *     if (i >= 0)
 *     {
 *         do
 *             jdoProvideField(fieldIds[i]);
 *         while (--i >= 0);
 *     }
 * }
 * </pre>
 */
public class JdoProvideFields extends ClassMethod
{
    public static JdoProvideFields getInstance(ClassEnhancer enhancer)
    {
        return new JdoProvideFields(enhancer, enhancer.getNamer().getProvideFieldsMethodName(),
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            null, new Class[] {int[].class}, new String[] {"indices"});
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
    public JdoProvideFields(ClassEnhancer enhancer, String name, int access, 
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

        Label l0 = new Label();
        visitor.visitLabel(l0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        Label l1 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNONNULL, l1);
        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitLdcInsn("argment is null");
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException",
            "<init>", "(Ljava/lang/String;)V");
        visitor.visitInsn(Opcodes.ATHROW);

        visitor.visitLabel(l1);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitInsn(Opcodes.ARRAYLENGTH);
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitInsn(Opcodes.ISUB);
        visitor.visitVarInsn(Opcodes.ISTORE, 2);
        Label l3 = new Label();
        visitor.visitLabel(l3);
        visitor.visitVarInsn(Opcodes.ILOAD, 2);
        Label l4 = new Label();
        visitor.visitJumpInsn(Opcodes.IFLT, l4);
        Label l5 = new Label();
        visitor.visitLabel(l5);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitVarInsn(Opcodes.ILOAD, 2);
        visitor.visitInsn(Opcodes.IALOAD);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(),
            getNamer().getProvideFieldMethodName(), "(I)V");
        visitor.visitIincInsn(2, -1);
        visitor.visitVarInsn(Opcodes.ILOAD, 2);
        visitor.visitJumpInsn(Opcodes.IFGE, l5);
        visitor.visitLabel(l4);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        visitor.visitInsn(Opcodes.RETURN);

        Label l7 = new Label();
        visitor.visitLabel(l7);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, l0, l7, 0);
        visitor.visitLocalVariable(argNames[0], "[I", null, l0, l7, 1);
        visitor.visitLocalVariable("i", "I", null, l3, l7, 2);
        visitor.visitMaxs(3, 3);

        visitor.visitEnd();
    }
}
