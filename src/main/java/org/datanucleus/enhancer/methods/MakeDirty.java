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
import org.datanucleus.enhancer.asm.Label;
import org.datanucleus.enhancer.asm.Opcodes;
import org.datanucleus.metadata.AbstractClassMetaData;

/**
 * Method to generate the method "dnMakeDirty" using ASM.
 * <pre>
 * public void dnMakeDirty(String fieldName)
 * {
 *     if (dnStateManager != null)
 *         dnStateManager.makeDirty(this, fieldName);
 *     if (dnIsDetached() &amp;&amp; fieldName != null) 
 *     {
 *         String fldName = null;
 *         if (fieldName.indexOf('.') &ge; 0)
 *             fldName = fieldName.substring(fieldName.lastIndexOf('.') + 1);
 *         else
 *             fldName = fieldName;
 *         for (int i = 0; i &lt; dnFieldNames.length; i++) 
 *         {
 *             if (dnFieldNames[i].equals(fldName)) 
 *             {
 *                 if (((BitSet) dnDetachedState[2]).get(i + dnInheritedFieldCount))
 *                     ((BitSet) dnDetachedState[3]).set(i + dnInheritedFieldCount);
 *                 else
 *                     throw new JDODetachedFieldAccessException("You have just attempted to access a field/property");
 *                 break;
 *             }
 *         }
 *     }
 * }
 * </pre>
 * and if not detachable
 * <pre>public void dnMakeDirty(String fieldName)
 * {
 *     if (dnStateManager != null)
 *         dnStateManager.makeDirty(this, fieldName);
 * }
 * </pre>
 * TODO This currently doesnt cater for a fully-qualified field where the class name part
 * doesnt define a field at that level
 */
public class MakeDirty extends ClassMethod
{
    public static MakeDirty getInstance(ClassEnhancer enhancer)
    {
        return new MakeDirty(enhancer, enhancer.getNamer().getMakeDirtyMethodName(), Opcodes.ACC_PUBLIC,
            null, new Class[] {String.class}, new String[] {"fieldName"});
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
    public MakeDirty(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Method to add the contents of the class method.
     */
    public void execute()
    {
        AbstractClassMetaData cmd = getClassEnhancer().getClassMetaData();
        String pcSuperclassName = cmd.getPersistableSuperclass();

        visitor.visitCode();

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        Label l1 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNULL, l1);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(), "makeDirty", "(" + getNamer().getPersistableDescriptor() + "Ljava/lang/String;" + ")V", true);
        visitor.visitLabel(l1);

        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        if (cmd.isDetachable())
        {
            // if (dnIsDetached())
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), getNamer().getIsDetachedMethodName(), "()Z", false);
            Label l3 = new Label();
            visitor.visitJumpInsn(Opcodes.IFEQ, l3);

            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            visitor.visitJumpInsn(Opcodes.IFNULL, l3);

            visitor.visitInsn(Opcodes.ACONST_NULL);
            visitor.visitVarInsn(Opcodes.ASTORE, 2);
            Label l5 = new Label();
            visitor.visitLabel(l5);
            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            visitor.visitIntInsn(Opcodes.BIPUSH, 46);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indexOf", "(I)I", false);
            Label l6 = new Label();
            visitor.visitJumpInsn(Opcodes.IFLT, l6);

            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            visitor.visitIntInsn(Opcodes.BIPUSH, 46);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "lastIndexOf", "(I)I", false);
            visitor.visitInsn(Opcodes.ICONST_1);
            visitor.visitInsn(Opcodes.IADD);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
            visitor.visitVarInsn(Opcodes.ASTORE, 2);
            Label l8 = new Label();
            visitor.visitJumpInsn(Opcodes.GOTO, l8);
            visitor.visitLabel(l6);

            visitor.visitFrame(Opcodes.F_APPEND,1, new Object[] {"java/lang/String"}, 0, null);
            visitor.visitVarInsn(Opcodes.ALOAD, 1);
            visitor.visitVarInsn(Opcodes.ASTORE, 2);
            visitor.visitLabel(l8);

            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitVarInsn(Opcodes.ISTORE, 3);
            Label l9 = new Label();
            visitor.visitLabel(l9);
            Label l10 = new Label();
            visitor.visitJumpInsn(Opcodes.GOTO, l10);
            Label l11 = new Label();
            visitor.visitLabel(l11);

            visitor.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
            visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(), getNamer().getFieldNamesFieldName(), "[Ljava/lang/String;");
            visitor.visitVarInsn(Opcodes.ILOAD, 3);
            visitor.visitInsn(Opcodes.AALOAD);
            visitor.visitVarInsn(Opcodes.ALOAD, 2);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            Label l12 = new Label();
            visitor.visitJumpInsn(Opcodes.IFEQ, l12);

            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), getNamer().getDetachedStateFieldName(), "[Ljava/lang/Object;");
            visitor.visitInsn(Opcodes.ICONST_2);
            visitor.visitInsn(Opcodes.AALOAD);
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/util/BitSet");
            visitor.visitVarInsn(Opcodes.ILOAD, 3);
            visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(), getNamer().getInheritedFieldCountFieldName(), "I");
            visitor.visitInsn(Opcodes.IADD);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "get", "(I)Z", false);
            Label l14 = new Label();
            visitor.visitJumpInsn(Opcodes.IFEQ, l14);

            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), getNamer().getDetachedStateFieldName(), "[Ljava/lang/Object;");
            visitor.visitInsn(Opcodes.ICONST_3);
            visitor.visitInsn(Opcodes.AALOAD);
            visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/util/BitSet");
            visitor.visitVarInsn(Opcodes.ILOAD, 3);
            visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(), getNamer().getInheritedFieldCountFieldName(), "I");
            visitor.visitInsn(Opcodes.IADD);
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "set", "(I)V", false);

            visitor.visitInsn(Opcodes.RETURN);
            visitor.visitLabel(l14);

            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            if (enhancer.hasOption(ClassEnhancer.OPTION_GENERATE_DETACH_LISTENER))
            {
                // TODO Check this bytecode
                visitor.visitMethodInsn(Opcodes.INVOKESTATIC, getNamer().getDetachListenerAsmClassName(),
                    "getInstance", "()L" + getNamer().getDetachListenerAsmClassName() + ";", false);
                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                visitor.visitLdcInsn("field/property");
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getNamer().getDetachListenerAsmClassName(), 
                    "undetachedFieldAccess", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
            }
            else
            {
                visitor.visitTypeInsn(Opcodes.NEW, getNamer().getDetachedFieldAccessExceptionAsmClassName());
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitLdcInsn("You have just attempted to access a field/property that hasn't been detached. Please detach it first before performing this operation");
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, getNamer().getDetachedFieldAccessExceptionAsmClassName(), 
                    "<init>", "(Ljava/lang/String;)V", false);
                visitor.visitInsn(Opcodes.ATHROW);
            }
            visitor.visitLabel(l12);

            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            visitor.visitIincInsn(3, 1);
            visitor.visitLabel(l10);
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            visitor.visitVarInsn(Opcodes.ILOAD, 3);
            visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(), getNamer().getFieldNamesFieldName(), "[Ljava/lang/String;");
            visitor.visitInsn(Opcodes.ARRAYLENGTH);
            visitor.visitJumpInsn(Opcodes.IF_ICMPLT, l11);
            visitor.visitLabel(l3);

            visitor.visitFrame(Opcodes.F_CHOP,2, null, 0, null);
            if (pcSuperclassName != null)
            {
                // Relay to the superclass to see if it has this field
                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, pcSuperclassName.replace('.', '/'), getNamer().getMakeDirtyMethodName(), "(Ljava/lang/String;)V", false);
            }

            visitor.visitInsn(Opcodes.RETURN);
            Label endLabel = new Label();
            visitor.visitLabel(endLabel);
            visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
            visitor.visitLocalVariable(argNames[0], "Ljava/lang/String;", null, startLabel, endLabel, 1);
            visitor.visitLocalVariable("fldName", "Ljava/lang/String;", null, l5, l3, 2);
            visitor.visitLocalVariable("i", "I", null, l9, l3, 3);
            visitor.visitMaxs(3, 4);
        }
        else
        {
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            visitor.visitInsn(Opcodes.RETURN);
            Label endLabel = new Label();
            visitor.visitLabel(endLabel);
            visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
            visitor.visitLocalVariable(argNames[0], "Ljava/lang/String;", null, startLabel, endLabel, 1);
            visitor.visitMaxs(3, 2);
        }

        visitor.visitEnd();
    }
}
