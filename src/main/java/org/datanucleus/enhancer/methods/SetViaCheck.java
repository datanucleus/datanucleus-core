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
import org.datanucleus.enhancer.asm.Type;
import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Method to generate the method "setZZZ" using ASM for CHECK_WRITE fields.
 * <pre>
 * static void dnSetZZZ(MyClass objPC, YYY zzz)
 * {
 *     if (objPC.dnFlags != 0 &amp;&amp; objPC.dnStateManager != null)
 *         objPC.dnStateManager.setStringField(objPC, 2, objPC.ZZZ, zzz);
 *     else
 *     {
 *         objPC.ZZZ = zzz;
 *         if (objPC.dnIsDetached() == true)
 *             ((BitSet) objPC.dnDetachedState[3]).set(2);
 *     }
 * }
 * </pre>
 * with the last part only applying when Detachable
 */
public class SetViaCheck extends ClassMethod
{
    /** Field that this dnSetZZZ is for. */
    protected AbstractMemberMetaData fmd;

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param fmd MetaData for the field we are generating for
     */
    public SetViaCheck(ClassEnhancer enhancer, AbstractMemberMetaData fmd)
    {
        super(enhancer, enhancer.getNamer().getSetMethodPrefixMethodName() + fmd.getName(),
            (fmd.isPublic() ? Opcodes.ACC_PUBLIC : 0) | (fmd.isProtected() ? Opcodes.ACC_PROTECTED : 0) | 
            (fmd.isPrivate() ? Opcodes.ACC_PRIVATE : 0) | Opcodes.ACC_STATIC, null, null, null);

        // Set the arg types/names
        argTypes = new Class[] {getClassEnhancer().getClassBeingEnhanced(), fmd.getType()};
        argNames = new String[] {"objPC", "val"};

        this.fmd = fmd;
    }

    /**
     * Method to add the contents of the class method.
     */
    public void execute()
    {
        visitor.visitCode();

        String fieldTypeDesc = Type.getDescriptor(fmd.getType());

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        // "if (objPC.dnFlags != 0 && objPC.dnStateManager != null)"
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), getNamer().getFlagsFieldName(), "B");
        Label l1 = new Label();
        visitor.visitJumpInsn(Opcodes.IFEQ, l1);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), "L" + getNamer().getStateManagerAsmClassName() + ";");
        visitor.visitJumpInsn(Opcodes.IFNULL, l1);

        // "objPC.dnStateManager.setYYYField(objPC, 8, objPC.ZZZ, val);"
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), "L" + getNamer().getStateManagerAsmClassName() + ";");
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
        if (enhancer.getClassMetaData().getPersistableSuperclass() != null)
        {
            visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(),
                getNamer().getInheritedFieldCountFieldName(), "I");
            visitor.visitInsn(Opcodes.IADD);
        }
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            fmd.getName(), fieldTypeDesc);
        EnhanceUtils.addLoadForType(visitor, fmd.getType(), 1);
        String dnMethodName = "set" + EnhanceUtils.getTypeNameForPersistableMethod(fmd.getType()) + "Field";
        String argTypeDesc = fieldTypeDesc;
        if (dnMethodName.equals("setObjectField"))
        {
            argTypeDesc = EnhanceUtils.CD_Object;
        }
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(),
            dnMethodName, "(L" + getNamer().getPersistableAsmClassName() + ";I" + argTypeDesc + argTypeDesc + ")V");
        Label l3 = new Label();
        visitor.visitJumpInsn(Opcodes.GOTO, l3);

        // "objPC.text = val;"
        visitor.visitLabel(l1);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        EnhanceUtils.addLoadForType(visitor, fmd.getType(), 1);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
            fmd.getName(), fieldTypeDesc);

        if (enhancer.getClassMetaData().isDetachable())
        {
            // "if (objPC.dnIsDetached() == true)  ((BitSet) objPC.dnDetachedState[3]).set(8);"
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(),
                getNamer().getIsDetachedMethodName(), "()Z");
            visitor.visitJumpInsn(Opcodes.IFEQ, l3);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                getNamer().getDetachedStateFieldName(), "[Ljava/lang/Object;");
            visitor.visitInsn(Opcodes.ICONST_3);
            visitor.visitInsn(Opcodes.AALOAD);
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/util/BitSet");
            EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
            if (enhancer.getClassMetaData().getPersistableSuperclass() != null)
            {
                visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(),
                    getNamer().getInheritedFieldCountFieldName(), "I");
                visitor.visitInsn(Opcodes.IADD);
            }
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "set", "(I)V");
        }

        visitor.visitLabel(l3);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        visitor.visitInsn(Opcodes.RETURN);

        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable(argNames[0], getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitLocalVariable(argNames[1], fieldTypeDesc, null, startLabel, endLabel, 1);
        visitor.visitMaxs(5, 2);

        visitor.visitEnd();
    }
}
