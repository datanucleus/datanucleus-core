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
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Method to generate the method "jdoGetZZZ" using ASM for MEDIATE_READ fields.
 * <pre>
 * static YYY jdoGetZZZ(MyClass objPC)
 * {
 *     if (objPC.jdoStateManager != null &amp;&amp; !objPC.jdoStateManager.isLoaded(objPC, 0))
 *         return (YYY) objPC.jdoStateManager.getObjectField(objPC, 0, objPC.ZZZ);
 *     if (objPC.jdoIsDetached() != false &amp;&amp;
 *         ((BitSet) objPC.jdoDetachedState[2]).get(0) != true &amp;&amp;
 *         ((BitSet) objPC.jdoDetachedState[3]).get(0) != true)
 *         throw new JDODetachedFieldAccessException(...);
 *     return objPC.ZZZ;
 * }
 * </pre>
 * with the last part only applying when Detachable
 */
public class JdoGetViaMediate extends ClassMethod
{
    /** Field that this jdoGetXXX is for. */
    protected AbstractMemberMetaData fmd;

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param fmd MetaData for the field we are generating for
     */
    public JdoGetViaMediate(ClassEnhancer enhancer, AbstractMemberMetaData fmd)
    {
        super(enhancer, enhancer.getNamer().getGetMethodPrefixMethodName() + fmd.getName(),
            (fmd.isPublic() ? Opcodes.ACC_PUBLIC : 0) | (fmd.isProtected() ? Opcodes.ACC_PROTECTED : 0) | 
            (fmd.isPrivate() ? Opcodes.ACC_PRIVATE : 0) | Opcodes.ACC_STATIC, fmd.getType(), null, null);

        // Set the arg type/name
        argTypes = new Class[] {getClassEnhancer().getClassBeingEnhanced()};
        argNames = new String[] {"objPC"};

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

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), "L" + getNamer().getStateManagerAsmClassName() + ";");
        Label l1 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNULL, l1);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), "L" + getNamer().getStateManagerAsmClassName() + ";");
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
        if (enhancer.getClassMetaData().getPersistenceCapableSuperclass() != null)
        {
            visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(), 
                getNamer().getInheritedFieldCountFieldName(), "I");
            visitor.visitInsn(Opcodes.IADD);
        }
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(),
            "isLoaded", "(L" + getNamer().getPersistableAsmClassName() + ";I)Z");
        visitor.visitJumpInsn(Opcodes.IFNE, l1);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), "L" + getNamer().getStateManagerAsmClassName() + ";");
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
        if (enhancer.getClassMetaData().getPersistenceCapableSuperclass() != null)
        {
            visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(), 
                getNamer().getInheritedFieldCountFieldName(), "I");
            visitor.visitInsn(Opcodes.IADD);
        }
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            fmd.getName(), fieldTypeDesc);
        String jdoMethodName = "get" + EnhanceUtils.getTypeNameForJDOMethod(fmd.getType()) + "Field";
        String argTypeDesc = fieldTypeDesc;
        if (jdoMethodName.equals("getObjectField"))
        {
            argTypeDesc = EnhanceUtils.CD_Object;
        }
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(),
            jdoMethodName, "(L" + getNamer().getPersistableAsmClassName() + ";I" + argTypeDesc + ")" + argTypeDesc);
        if (jdoMethodName.equals("getObjectField"))
        {
            // Cast any object fields to the correct type
            visitor.visitTypeInsn(Opcodes.CHECKCAST, fmd.getTypeName().replace('.', '/'));
        }
        EnhanceUtils.addReturnForType(visitor, fmd.getType());

        visitor.visitLabel(l1);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        if (enhancer.getClassMetaData().isDetachable())
        {
            // "if (objPC.jdoIsDetached() != false && ((BitSet) objPC.jdoDetachedState[2]).get(5) != true)"
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                getNamer().getIsDetachedMethodName(), "()Z");

            Label l4 = new Label();
            visitor.visitJumpInsn(Opcodes.IFEQ, l4);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                getNamer().getDetachedStateFieldName(), "[Ljava/lang/Object;");
            visitor.visitInsn(Opcodes.ICONST_2);
            visitor.visitInsn(Opcodes.AALOAD);
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/util/BitSet");
            EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
            if (enhancer.getClassMetaData().getPersistenceCapableSuperclass() != null)
            {
                visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(), 
                    getNamer().getInheritedFieldCountFieldName(), "I");
                visitor.visitInsn(Opcodes.IADD);
            }
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "get", "(I)Z");
            visitor.visitJumpInsn(Opcodes.IFNE, l4);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                getNamer().getDetachedStateFieldName(), "[Ljava/lang/Object;");
            visitor.visitInsn(Opcodes.ICONST_3);
            visitor.visitInsn(Opcodes.AALOAD);
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/util/BitSet");
            EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
            if (enhancer.getClassMetaData().getPersistenceCapableSuperclass() != null)
            {
                visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(), 
                    getNamer().getInheritedFieldCountFieldName(), "I");
                visitor.visitInsn(Opcodes.IADD);
            }
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "get", "(I)Z");
            visitor.visitJumpInsn(Opcodes.IFNE, l4);

            if (enhancer.hasOption(ClassEnhancer.OPTION_GENERATE_DETACH_LISTENER))
            {
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, getNamer().getDetachListenerAsmClassName(), 
                    "getInstance", "()L" + getNamer().getDetachListenerAsmClassName() + ";");
                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                visitor.visitLdcInsn(fmd.getName());
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getNamer().getDetachListenerAsmClassName(),
                    "undetachedFieldAccess", "(Ljava/lang/Object;Ljava/lang/String;)V");
            }
            else
            {
                // "throw new DetachedFieldAccessException(...)"
                visitor.visitTypeInsn(Opcodes.NEW, getNamer().getDetachedFieldAccessExceptionAsmClassName());
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitLdcInsn(LOCALISER.msg("Enhancer.DetachedFieldAccess", fmd.getName()));
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, getNamer().getDetachedFieldAccessExceptionAsmClassName(),
                    "<init>", "(Ljava/lang/String;)V");
                visitor.visitInsn(Opcodes.ATHROW);
            }

            visitor.visitLabel(l4);
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), fmd.getName(), fieldTypeDesc);
        EnhanceUtils.addReturnForType(visitor, fmd.getType());

        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable(argNames[0], getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitMaxs(4, 1);

        visitor.visitEnd();
    }
}