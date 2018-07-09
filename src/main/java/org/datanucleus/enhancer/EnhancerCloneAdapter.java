/**********************************************************************
Copyright (c) 2018 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancer;

import org.datanucleus.enhancer.asm.AnnotationVisitor;
import org.datanucleus.enhancer.asm.Attribute;
import org.datanucleus.enhancer.asm.ClassVisitor;
import org.datanucleus.enhancer.asm.Handle;
import org.datanucleus.enhancer.asm.Label;
import org.datanucleus.enhancer.asm.MethodVisitor;
import org.datanucleus.enhancer.asm.Opcodes;
import org.datanucleus.enhancer.asm.TypePath;
import org.datanucleus.util.Localiser;

/**
 * Adapter for clone() method in persistence-enabled classes.
 * This adapter processes any clone() method and
 * <ul>
 * <li>Creates dnClone() with the same code as was present in the user-provided clone()</li>
 * <li>Changes clone to have the code below</li>
 * </ul>
 * <pre>
 * Object clone()
 * {
 *     XXX copy = (XXX)dnClone();
 *     copy.dnFlags = (byte) 0;
 *     copy.dnStateManager = null;
 *     return copy;
 * }
 * </pre>
 */
public class EnhancerCloneAdapter extends MethodVisitor
{
    /** The enhancer for this class. */
    protected ClassEnhancer enhancer;

    /** Visitor for the dnGetXXX method. */
    protected MethodVisitor visitor = null;

    /**
     * Constructor for the clone adapter.
     * @param mv MethodVisitor
     * @param enhancer ClassEnhancer for the class with the method
     * @param methodName Name of the method
     * @param methodDesc Method descriptor
     * @param mmd MetaData for the property
     * @param cv ClassVisitor
     */
    public EnhancerCloneAdapter(MethodVisitor mv, ClassEnhancer enhancer, ClassVisitor cv)
    {
        super(ClassEnhancer.ASM_API_VERSION, mv);
        this.enhancer = enhancer;

        // Generate dnClone method to include code that this clone() method currently has
        int access = Opcodes.ACC_PUBLIC;
        this.visitor = cv.visitMethod(access, enhancer.getNamer().getCloneMethodName(), "()Ljava/lang/Object;", null, new String[] {CloneNotSupportedException.class.getName().replace('.', '/')});
    }

    /**
     * Method called at the end of visiting the clone() method.
     * This is used to add the dnClone() method with the same code as is present originally in the clone() method.
     */
    public void visitEnd()
    {
        visitor.visitEnd();
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
        {
            String msg = ClassMethod.getMethodAdditionMessage(enhancer.getNamer().getCloneMethodName(), "Object", null, null);
            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005019", msg));
        }

        // Generate the clone method to use the dnClone we just added
        generateCloneMethod(mv, enhancer, enhancer.getNamer());
    }

    /**
     * Convenience method to use the MethodVisitor to generate the code for the method clone().
     * @param mv MethodVisitor
     * @param enhancer Class Enhancer
     * @param namer Namer for methods etc
     */
    public static void generateCloneMethod(MethodVisitor mv, ClassEnhancer enhancer, EnhancementNamer namer)
    {
        mv.visitCode();

        Label startLabel = new Label();
        mv.visitLabel(startLabel);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, enhancer.getASMClassName(), namer.getCloneMethodName(), "()" + EnhanceUtils.CD_Object, false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, enhancer.getASMClassName());
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        Label l1 = new Label();
        mv.visitLabel(l1);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitFieldInsn(Opcodes.PUTFIELD, enhancer.getASMClassName(), namer.getFlagsFieldName(), "B");

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitFieldInsn(Opcodes.PUTFIELD, enhancer.getASMClassName(), namer.getStateManagerFieldName(), namer.getStateManagerDescriptor());

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);

        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", enhancer.getClassDescriptor(), null, startLabel, endLabel, 0);
        mv.visitLocalVariable("copy", enhancer.getClassDescriptor(), null, l1, endLabel, 1);
        mv.visitMaxs(2, 2);

        mv.visitEnd();
    }

    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1)
    {
        // Keep any annotations on the clone method, so use "mv"
        return mv.visitAnnotation(arg0, arg1);
    }

    // IMPORTANT : BELOW HERE WE MUST INTERCEPT ALL OTHER METHODS AND RELAY TO "visitor"

    public AnnotationVisitor visitAnnotationDefault()
    {
        return visitor.visitAnnotationDefault();
    }

    public void visitParameter(String name, int access)
    {
        visitor.visitParameter(name, access);
    }

    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
    {
        return visitor.visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
    {
        visitor.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs)
    {
        visitor.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
    {
        return visitor.visitInsnAnnotation(typeRef, typePath, desc, visible);
    }

    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
    {
        return visitor.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
    }

    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible)
    {
        return visitor.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
    }

    public void visitAttribute(Attribute arg0)
    {
        visitor.visitAttribute(arg0);
    }

    public void visitCode()
    {
        visitor.visitCode();
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc)
    {
        visitor.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4)
    {
        visitor.visitFrame(arg0, arg1, arg2, arg3, arg4);
    }

    public void visitIincInsn(int arg0, int arg1)
    {
        visitor.visitIincInsn(arg0, arg1);
    }

    public void visitInsn(int opcode)
    {
        visitor.visitInsn(opcode);
    }

    public void visitIntInsn(int arg0, int arg1)
    {
        visitor.visitIntInsn(arg0, arg1);
    }

    public void visitJumpInsn(int arg0, Label arg1)
    {
        visitor.visitJumpInsn(arg0, arg1);
    }

    public void visitLabel(Label arg0)
    {
        visitor.visitLabel(arg0);
    }

    public void visitLdcInsn(Object arg0)
    {
        visitor.visitLdcInsn(arg0);
    }

    public void visitLineNumber(int arg0, Label arg1)
    {
        visitor.visitLineNumber(arg0, arg1);
    }

    public void visitLocalVariable(String arg0, String arg1, String arg2, Label arg3, Label arg4, int arg5)
    {
        visitor.visitLocalVariable(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2)
    {
        visitor.visitLookupSwitchInsn(arg0, arg1, arg2);
    }

    public void visitMaxs(int arg0, int arg1)
    {
        visitor.visitMaxs(arg0, arg1);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc)
    {
        visitor.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitMultiANewArrayInsn(String arg0, int arg1)
    {
        visitor.visitMultiANewArrayInsn(arg0, arg1);
    }

    public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1, boolean arg2)
    {
        return visitor.visitParameterAnnotation(arg0, arg1, arg2);
    }

    public void visitTableSwitchInsn(int arg0, int arg1, Label arg2, Label... arg3)
    {
        visitor.visitTableSwitchInsn(arg0, arg1, arg2, arg3);
    }

    public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2, String arg3)
    {
        visitor.visitTryCatchBlock(arg0, arg1, arg2, arg3);
    }

    public void visitTypeInsn(int arg0, String arg1)
    {
        visitor.visitTypeInsn(arg0, arg1);
    }

    public void visitVarInsn(int arg0, int arg1)
    {
        visitor.visitVarInsn(arg0, arg1);
    }
}
