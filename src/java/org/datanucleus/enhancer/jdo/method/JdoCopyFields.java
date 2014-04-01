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
 * Method to generate the method "jdoCopyFields" using ASM.
 * <pre>
 * public void jdoCopyFields(Object obj, int[] fieldNumbers)
 * {
 *     if (jdoStateManager == null)
 *         throw new IllegalStateException("state manager is null");
 *     if (fieldNumbers == null)
 *         throw new IllegalStateException("fieldNumbers is null");
 *     if (!(obj instanceof Answer))
 *         throw new IllegalArgumentException
 *                   ("object is not an object of type mydomain.MyClass");
 *     MyClass other = (MyClass) obj;
 *     if (jdoStateManager != other.jdoStateManager)
 *         throw new IllegalArgumentException("state managers do not match");
 *     int i = fieldNumbers.length - 1;
 *     if (i &ge; 0) {
 *         do
 *             jdoCopyField(other, fieldNumbers[i]);
 *         while (--i &ge; 0);
 *     }
 * }
 * </pre>
 */
public class JdoCopyFields extends ClassMethod
{
    public static JdoCopyFields getInstance(ClassEnhancer enhancer)
    {
        return new JdoCopyFields(enhancer, enhancer.getNamer().getCopyFieldsMethodName(), Opcodes.ACC_PUBLIC,
            null, new Class[] {Object.class, int[].class}, new String[] { "obj", "indices" });
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
    public JdoCopyFields(ClassEnhancer enhancer, String name, int access, 
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

        visitor.visitVarInsn(Opcodes.ALOAD, 2);
        Label l3 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNONNULL, l3);
        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalStateException");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitLdcInsn("fieldNumbers is null");
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalStateException",
            "<init>", "(Ljava/lang/String;)V");
        visitor.visitInsn(Opcodes.ATHROW);

        visitor.visitLabel(l3);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitTypeInsn(Opcodes.INSTANCEOF, getClassEnhancer().getASMClassName());
        Label l5 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNE, l5);
        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitLdcInsn("object is not an object of type " + getClassEnhancer().getASMClassName().replace('/', '.'));
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException",
            "<init>", "(Ljava/lang/String;)V");
        visitor.visitInsn(Opcodes.ATHROW);

        visitor.visitLabel(l5);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitTypeInsn(Opcodes.CHECKCAST, getClassEnhancer().getASMClassName());
        visitor.visitVarInsn(Opcodes.ASTORE, 3);

        Label l9 = new Label();
        visitor.visitLabel(l9);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        visitor.visitVarInsn(Opcodes.ALOAD, 3);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());

        Label l10 = new Label();
        visitor.visitJumpInsn(Opcodes.IF_ACMPEQ, l10);
        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitLdcInsn("state managers do not match");
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException",
            "<init>", "(Ljava/lang/String;)V");
        visitor.visitInsn(Opcodes.ATHROW);

        visitor.visitLabel(l10);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_APPEND,1, new Object[] {getClassEnhancer().getASMClassName()}, 0, null);
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 2);
        visitor.visitInsn(Opcodes.ARRAYLENGTH);
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitInsn(Opcodes.ISUB);
        visitor.visitVarInsn(Opcodes.ISTORE, 4);
        Label l12 = new Label();
        visitor.visitLabel(l12);
        visitor.visitVarInsn(Opcodes.ILOAD, 4);
        Label l13 = new Label();
        visitor.visitJumpInsn(Opcodes.IFLT, l13);
        Label l14 = new Label();
        visitor.visitLabel(l14);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 3);
        visitor.visitVarInsn(Opcodes.ALOAD, 2);
        visitor.visitVarInsn(Opcodes.ILOAD, 4);
        visitor.visitInsn(Opcodes.IALOAD);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(),
            getNamer().getCopyFieldMethodName(), "(" + getClassEnhancer().getClassDescriptor() + "I)V");
        visitor.visitIincInsn(4, -1);
        visitor.visitVarInsn(Opcodes.ILOAD, 4);
        visitor.visitJumpInsn(Opcodes.IFGE, l14);
        visitor.visitLabel(l13);
        if (JavaUtils.useStackMapFrames())
        {
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        visitor.visitInsn(Opcodes.RETURN);

        Label l16 = new Label();
        visitor.visitLabel(l16);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, l0, l16, 0);
        visitor.visitLocalVariable(argNames[0], EnhanceUtils.CD_Object, null, l0, l16, 1);
        visitor.visitLocalVariable(argNames[1], "[I", null, l0, l16, 2);
        visitor.visitLocalVariable("other", getClassEnhancer().getClassDescriptor(), null, l9, l16, 3);
        visitor.visitLocalVariable("i", "I", null, l12, l16, 4);
        visitor.visitMaxs(4, 5);

        visitor.visitEnd();
    }
}
