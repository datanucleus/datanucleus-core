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

/**
 * Method to generate the method "jdoReplaceFields" using ASM.
 * <pre>
 * public final void jdoReplaceFields(int[] fieldIds)
 * {
 *     if (fieldIds == null)
 *         throw new IllegalArgumentException("argument is null");
 *     int i = fieldIds.length;
 *     if (i &gt; 0)
 *     {
 *         int j = 0;
 *         do
 *             jdoReplaceField(fieldIds[j]);
 *         while (++j &lt; i);
 *     }
 * }
 * </pre>
 */
public class ReplaceFields extends ClassMethod
{
    public static ReplaceFields getInstance(ClassEnhancer enhancer)
    {
        return new ReplaceFields(enhancer, enhancer.getNamer().getReplaceFieldsMethodName(),
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
    public ReplaceFields(ClassEnhancer enhancer, String name, int access, 
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
        visitor.visitLdcInsn("argument is null");
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException",
            "<init>", "(Ljava/lang/String;)V");
        visitor.visitInsn(Opcodes.ATHROW);

        visitor.visitLabel(l1);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitInsn(Opcodes.ARRAYLENGTH);
        visitor.visitVarInsn(Opcodes.ISTORE, 2);
        Label l3 = new Label();
        visitor.visitLabel(l3);
        visitor.visitVarInsn(Opcodes.ILOAD, 2);
        Label l4 = new Label();
        visitor.visitJumpInsn(Opcodes.IFLE, l4);
        visitor.visitInsn(Opcodes.ICONST_0);
        visitor.visitVarInsn(Opcodes.ISTORE, 3);
        Label l6 = new Label();
        visitor.visitLabel(l6);
        visitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {Opcodes.INTEGER, Opcodes.INTEGER}, 0, null);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitVarInsn(Opcodes.ILOAD, 3);
        visitor.visitInsn(Opcodes.IALOAD);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(),
            getNamer().getReplaceFieldMethodName(), "(I)V");
        visitor.visitIincInsn(3, 1);
        visitor.visitVarInsn(Opcodes.ILOAD, 3);
        visitor.visitVarInsn(Opcodes.ILOAD, 2);
        visitor.visitJumpInsn(Opcodes.IF_ICMPLT, l6);
        visitor.visitLabel(l4);
        visitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
        visitor.visitInsn(Opcodes.RETURN);

        Label l8 = new Label();
        visitor.visitLabel(l8);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, l0, l8, 0);
        visitor.visitLocalVariable(argNames[0], "[I", null, l0, l8, 1);
        visitor.visitLocalVariable("i", "I", null, l3, l8, 2);
        visitor.visitLocalVariable("j", "I", null, l6, l4, 3);
        visitor.visitMaxs(3, 4);

        visitor.visitEnd();
    }
}
