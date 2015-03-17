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
package org.datanucleus.enhancer;

import org.datanucleus.asm.AnnotationVisitor;
import org.datanucleus.asm.Attribute;
import org.datanucleus.asm.ClassVisitor;
import org.datanucleus.asm.Handle;
import org.datanucleus.asm.Label;
import org.datanucleus.asm.MethodVisitor;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.asm.Type;
import org.datanucleus.asm.TypePath;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.util.Localiser;

/**
 * Adapter for property setter methods in persistence-enabled classes.
 * This adapter processes the setXXX method and
 * <ul>
 * <li>Creates dnSetXXX with the same code as was present in setXXX</li>
 * <li>Changes setXXX to have the code below</li>
 * </ul>
 * When detachable this will be (CHECK_WRITE variant)
 * <pre>
 * void setZZZ(YYY zzz)
 * {
 *     if (dnFlags != 0 &amp;&amp; dnStateManager != null)
 *         dnStateManager.setStringField(this, 2, dnGetZZZ(), zzz);
 *     else
 *     {
 *         dnSetXXX(zzz);
 *         if (dnIsDetached() == true)
 *             ((BitSet) dnDetachedState[3]).set(2);
 *     }
 * }
 * </pre>
 * and when not detachable
 * <pre>
 * void setZZZ(YYY zzz)
 * {
 *     if (aFlags &gt; 0 &amp;&amp; dnStateManager != null)
 *         dnStateManager.setObjectField(this, 2, dnGetZZZ(), zzz);
 *     dnSetXXX(zzz);
 * }
 * </pre>
 * There are other variants for MEDIATE_WRITE and NORMAL_WRITE
 */
public class EnhancerPropertySetterAdapter extends MethodVisitor
{
    /** The enhancer for this class. */
    protected ClassEnhancer enhancer;

    /** Name for the method being adapted. */
    protected String methodName;

    /** Descriptor for the method being adapted. */
    protected String methodDescriptor;

    /** MetaData for the property. */
    protected AbstractMemberMetaData mmd;

    /** Visitor for the dnSetXXX method. */
    protected MethodVisitor visitor = null;

    /**
     * Constructor for the method adapter.
     * @param mv MethodVisitor
     * @param enhancer ClassEnhancer for the class with the method
     * @param methodName Name of the method
     * @param methodDesc Method descriptor
     * @param mmd MetaData for the property
     * @param cv ClassVisitor
     */
    public EnhancerPropertySetterAdapter(MethodVisitor mv, ClassEnhancer enhancer, String methodName, String methodDesc,
            AbstractMemberMetaData mmd, ClassVisitor cv)
    {
        super(ClassEnhancer.ASM_API_VERSION, mv);
        this.enhancer = enhancer;
        this.methodName = methodName;
        this.methodDescriptor = methodDesc;
        this.mmd = mmd;

        // Generate dnSetXXX method to include code that this setXXX currently has
        int access = (mmd.isPublic() ? Opcodes.ACC_PUBLIC : 0) | 
            (mmd.isProtected() ? Opcodes.ACC_PROTECTED : 0) | 
            (mmd.isPrivate() ? Opcodes.ACC_PRIVATE : 0) |
            (mmd.isAbstract() ? Opcodes.ACC_ABSTRACT : 0);
        this.visitor = cv.visitMethod(access, enhancer.getNamer().getSetMethodPrefixMethodName() + mmd.getName(), methodDesc, null, null);
    }

    /**
     * Method called at the end of visiting the setXXX method.
     * This is used to add the dnSetXXX method with the same code as is present originally in the setXXX method.
     */
    public void visitEnd()
    {
        visitor.visitEnd();
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
        {
            String msg = ClassMethod.getMethodAdditionMessage(enhancer.getNamer().getSetMethodPrefixMethodName() + mmd.getName(), 
                null, new Object[]{mmd.getType()}, new String[] {"val"});
            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005019", msg));
        }

        if (!mmd.isAbstract())
        {
            // Property is not abstract so generate the setXXX method to use the dnSetXXX we just added
            generateSetXXXMethod(mv, mmd, enhancer.getASMClassName(), enhancer.getClassDescriptor(), enhancer.getNamer());
        }
    }

    /**
     * Convenience method to use the MethodVisitor to generate the code for the method setXXX() for the
     * property with the specified MetaData.
     * @param mv MethodVisitor
     * @param mmd MetaData for the property
     * @param asmClassName ASM class name for the owning class
     * @param asmClassDesc ASM descriptor for the owning class
     * @param namer Namer for methods etc
     */
    public static void generateSetXXXMethod(MethodVisitor mv, AbstractMemberMetaData mmd,
            String asmClassName, String asmClassDesc, EnhancementNamer namer)
    {
        String[] argNames = new String[] {"this", "val"};
        String fieldTypeDesc = Type.getDescriptor(mmd.getType());

        mv.visitCode();

        AbstractClassMetaData cmd = mmd.getAbstractClassMetaData();
        if ((mmd.getPersistenceFlags() & Persistable.MEDIATE_WRITE) == Persistable.MEDIATE_WRITE)
        {
            // MEDIATE_WRITE - see method SetViaMediate
            Label startLabel = new Label();
            mv.visitLabel(startLabel);

            // "if (objPC.dnStateManager == null) objPC.ZZZ = zzz;"
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, asmClassName,
                namer.getStateManagerFieldName(), "L" + namer.getStateManagerAsmClassName() + ";");
            Label l1 = new Label();
            mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            EnhanceUtils.addLoadForType(mv, mmd.getType(), 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, asmClassName,
                namer.getSetMethodPrefixMethodName() + mmd.getName(), "(" + fieldTypeDesc + ")V");
            Label l3 = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, l3);
            mv.visitLabel(l1);

            // "else objPC.dnStateManager.setYYYField(objPC, 0, objPC.ZZZ, zzz);"
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, asmClassName,
                namer.getStateManagerFieldName(), "L" + namer.getStateManagerAsmClassName() + ";");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            EnhanceUtils.addBIPUSHToMethod(mv, mmd.getFieldId());
            if (cmd.getPersistableSuperclass() != null)
            {
                mv.visitFieldInsn(Opcodes.GETSTATIC, asmClassName, namer.getInheritedFieldCountFieldName(), "I");
                mv.visitInsn(Opcodes.IADD);
            }
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, asmClassName, 
                namer.getGetMethodPrefixMethodName() + mmd.getName(), "()" + fieldTypeDesc);
            EnhanceUtils.addLoadForType(mv, mmd.getType(), 1);
            String methodName = "set" + EnhanceUtils.getTypeNameForPersistableMethod(mmd.getType()) + "Field";
            String argTypeDesc = fieldTypeDesc;
            if (methodName.equals("setObjectField"))
            {
                argTypeDesc = EnhanceUtils.CD_Object;
            }
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, namer.getStateManagerAsmClassName(),
                methodName, "(L" + namer.getPersistableAsmClassName() + ";I" + argTypeDesc + argTypeDesc + ")V");

            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

            if (cmd.isDetachable())
            {
                // "if (objPC.dnIsDetached() == true)"
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, asmClassName,
                    namer.getIsDetachedMethodName(), "()Z");
                Label l6 = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, l6);

                // "((BitSet) objPC.dnDetachedState[3]).set(0);"
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, asmClassName,
                    namer.getDetachedStateFieldName(), "[Ljava/lang/Object;");
                mv.visitInsn(Opcodes.ICONST_3);
                mv.visitInsn(Opcodes.AALOAD);
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/util/BitSet");
                EnhanceUtils.addBIPUSHToMethod(mv, mmd.getFieldId());
                if (cmd.getPersistableSuperclass() != null)
                {
                    mv.visitFieldInsn(Opcodes.GETSTATIC, asmClassName,
                        namer.getInheritedFieldCountFieldName(), "I");
                    mv.visitInsn(Opcodes.IADD);
                }
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "set", "(I)V");

                mv.visitLabel(l6);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }

            mv.visitInsn(Opcodes.RETURN);
            Label endLabel = new Label();
            mv.visitLabel(endLabel);
            mv.visitLocalVariable(argNames[0], asmClassDesc, null, startLabel, endLabel, 0);
            mv.visitLocalVariable(argNames[1], fieldTypeDesc, null, startLabel, endLabel, 1);
            mv.visitMaxs(5, 2);
        }
        else if ((mmd.getPersistenceFlags() & Persistable.CHECK_WRITE) == Persistable.CHECK_WRITE)
        {
            // CHECK_WRITE - see method SetViaCheck
            Label startLabel = new Label();
            mv.visitLabel(startLabel);

            // "if (objPC.dnFlags != 0 && objPC.dnStateManager != null)"
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, asmClassName, namer.getFlagsFieldName(), "B");
            Label l1 = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, l1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, asmClassName,
                namer.getStateManagerFieldName(), "L" + namer.getStateManagerAsmClassName() + ";");
            mv.visitJumpInsn(Opcodes.IFNULL, l1);

            // "objPC.dnStateManager.setYYYField(objPC, 8, objPC.ZZZ, val);"
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, asmClassName,
                namer.getStateManagerFieldName(), "L" + namer.getStateManagerAsmClassName() + ";");
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            EnhanceUtils.addBIPUSHToMethod(mv, mmd.getFieldId());
            if (cmd.getPersistableSuperclass() != null)
            {
                mv.visitFieldInsn(Opcodes.GETSTATIC, asmClassName,
                    namer.getInheritedFieldCountFieldName(), "I");
                mv.visitInsn(Opcodes.IADD);
            }
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, asmClassName, 
                namer.getGetMethodPrefixMethodName() + mmd.getName(), "()" + fieldTypeDesc);
            EnhanceUtils.addLoadForType(mv, mmd.getType(), 1);
            String methodName = "set" + EnhanceUtils.getTypeNameForPersistableMethod(mmd.getType()) + "Field";
            String argTypeDesc = fieldTypeDesc;
            if (methodName.equals("setObjectField"))
            {
                argTypeDesc = EnhanceUtils.CD_Object;
            }
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, namer.getStateManagerAsmClassName(),
                methodName, "(L" + namer.getPersistableAsmClassName() + ";I" + argTypeDesc + argTypeDesc + ")V");
            Label l3 = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, l3);
            mv.visitLabel(l1);

            // "objPC.text = val;"
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            EnhanceUtils.addLoadForType(mv, mmd.getType(), 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, asmClassName,
                namer.getSetMethodPrefixMethodName() + mmd.getName(), "(" + fieldTypeDesc + ")V");

            if (cmd.isDetachable())
            {
                // "if (objPC.dnIsDetached() == true)  ((BitSet) objPC.dnDetachedState[3]).set(8);"
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, asmClassName,
                    namer.getIsDetachedMethodName(), "()Z");
                mv.visitJumpInsn(Opcodes.IFEQ, l3);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, asmClassName,
                    namer.getDetachedStateFieldName(), "[Ljava/lang/Object;");
                mv.visitInsn(Opcodes.ICONST_3);
                mv.visitInsn(Opcodes.AALOAD);
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/util/BitSet");
                EnhanceUtils.addBIPUSHToMethod(mv, mmd.getFieldId());
                if (cmd.getPersistableSuperclass() != null)
                {
                    mv.visitFieldInsn(Opcodes.GETSTATIC, asmClassName,
                        namer.getInheritedFieldCountFieldName(), "I");
                    mv.visitInsn(Opcodes.IADD);
                }
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "set", "(I)V");
            }

            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitInsn(Opcodes.RETURN);

            Label endLabel = new Label();
            mv.visitLabel(endLabel);
            mv.visitLocalVariable(argNames[0], asmClassDesc, null, startLabel, endLabel, 0);
            mv.visitLocalVariable(argNames[1], fieldTypeDesc, null, startLabel, endLabel, 1);
            mv.visitMaxs(5, 2);
        }
        else
        {
            // NORMAL - see method SetNormal
            Label startLabel = new Label();
            mv.visitLabel(startLabel);

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            EnhanceUtils.addLoadForType(mv, mmd.getType(), 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, asmClassName,
                namer.getSetMethodPrefixMethodName() + mmd.getName(), "(" + fieldTypeDesc + ")V");
            mv.visitInsn(Opcodes.RETURN);

            Label endLabel = new Label();
            mv.visitLabel(endLabel);
            mv.visitLocalVariable(argNames[0], asmClassDesc, null, startLabel, endLabel, 0);
            mv.visitLocalVariable(argNames[1], fieldTypeDesc, null, startLabel, endLabel, 1);
            mv.visitMaxs(2, 2);
        }

        mv.visitEnd();
    }

    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1)
    {
        // Keep any annotation on the setXXX method, so use "mv"
        return mv.visitAnnotation(arg0, arg1);
    }

    // IMPORTANT : BELOW HERE WE MUST INTERCEPT ALL OTHER METHODS AND RELAY TO "visitor"

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

    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index,
            String desc, boolean visible)
    {
        return visitor.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
    }

    public AnnotationVisitor visitAnnotationDefault()
    {
        return visitor.visitAnnotationDefault();
    }

    public void visitAttribute(Attribute arg0)
    {
        visitor.visitAttribute(arg0);
    }

    public void visitCode()
    {
        visitor.visitCode();
    }

    public void visitFieldInsn(int arg0, String arg1, String arg2, String arg3)
    {
        visitor.visitFieldInsn(arg0, arg1, arg2, arg3);
    }

    public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4)
    {
        visitor.visitFrame(arg0, arg1, arg2, arg3, arg4);
    }

    public void visitIincInsn(int arg0, int arg1)
    {
        visitor.visitIincInsn(arg0, arg1);
    }

    public void visitInsn(int arg0)
    {
        visitor.visitInsn(arg0);
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

    public void visitMethodInsn(int arg0, String arg1, String arg2, String arg3)
    {
        visitor.visitMethodInsn(arg0, arg1, arg2, arg3);
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
