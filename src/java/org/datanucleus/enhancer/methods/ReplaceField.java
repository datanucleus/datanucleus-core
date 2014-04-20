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
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.PropertyMetaData;

/**
 * Method to generate the method "jdoReplaceField" using ASM.
 * <pre>
 * public void jdoReplaceField(int fieldIndex)
 * {
 *     if (jdoStateManager == null)
 *         throw new IllegalStateException("state manager is null");
 *     switch (fieldIndex)
 *     {
 *         case 0:
 *         {
 *             MyClass ref = this;
 *             ref.param1 = (MyOtherClass) ref.jdoStateManager.replacingObjectField(this, fieldIndex);
 *             break;
 *         }
 *         default:
 *             throw new IllegalArgumentException("out of field index :" + fieldIndex);
 *     }
 * }
 * </pre>
 * or with superclass
 * <pre>
 * public void jdoReplaceField(int fieldIndex)
 * {
 *     if (jdoStateManager == null)
 *         throw new IllegalStateException("state manager is null");
 *     switch (fieldIndex - jdoInheritedFieldCount)
 *     {
 *         case 0:
 *         {
 *             MyClass ref = this;
 *             ref.param1 = (MyOtherClass)ref.jdoStateManager.replacingStringField(this, fieldIndex);
 *             break;
 *         }
 *         default:
 *             super.jdoReplaceField(fieldIndex);
 *     }
 * }
 * </pre>
 */
public class ReplaceField extends ClassMethod
{
    public static ReplaceField getInstance(ClassEnhancer enhancer)
    {
        return new ReplaceField(enhancer, enhancer.getNamer().getReplaceFieldMethodName(), Opcodes.ACC_PUBLIC,
            null, new Class[] {int.class}, new String[] {"index"});
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
    public ReplaceField(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Method to add the contents of the class method.
     */
    public void execute()
    {
        AbstractMemberMetaData fields[] = enhancer.getClassMetaData().getManagedMembers();
        String pcSuperclassName = enhancer.getClassMetaData().getPersistableSuperclass();

        visitor.visitCode();

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        if (pcSuperclassName != null)
        {
            if (fields.length > 0)
            {
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
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                visitor.visitVarInsn(Opcodes.ILOAD, 1);
                visitor.visitFieldInsn(Opcodes.GETSTATIC, getClassEnhancer().getASMClassName(),
                    getNamer().getInheritedFieldCountFieldName(), "I");
                visitor.visitInsn(Opcodes.ISUB);

                Label[] fieldOptions = new Label[fields.length];
                for (int i=0;i<fields.length;i++)
                {
                    fieldOptions[i] = new Label();
                }
                Label defaultLabel = new Label();
                Label endSwitchLabel = new Label();

                // switch:
                visitor.visitTableSwitchInsn(0, fields.length-1, defaultLabel, fieldOptions);

                for (int i=0;i<fields.length;i++)
                {
                    visitor.visitLabel(fieldOptions[i]);
                    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                        getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitVarInsn(Opcodes.ILOAD, 1);
                    String methodNameType = EnhanceUtils.getTypeNameForPersistableMethod(fields[i].getType());
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(),
                        "replacing" + methodNameType + "Field",
                        "(" + getNamer().getPersistableDescriptor() + "I)" + EnhanceUtils.getTypeDescriptorForJDOMethod(fields[i].getType()));
                    if (methodNameType.equals("Object"))
                    {
                        // Check any Object types for casting
                        visitor.visitTypeInsn(Opcodes.CHECKCAST, fields[i].getTypeName().replace('.', '/'));
                    }
                    if (fields[i] instanceof PropertyMetaData)
                    {
                        // Persistent properties so use jdoSetXXX(...)
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                            getNamer().getSetMethodPrefixMethodName() + fields[i].getName(), "(" + Type.getDescriptor(fields[i].getType()) + ")V");
                    }
                    else
                    {
                        // Persistent field so use xxx = ...
                        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
                            fields[i].getName(), Type.getDescriptor(fields[i].getType()));
                    }
                    visitor.visitJumpInsn(Opcodes.GOTO, endSwitchLabel);
                }

                // default:
                visitor.visitLabel(defaultLabel);
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                visitor.visitVarInsn(Opcodes.ILOAD, 1);
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, pcSuperclassName.replace('.', '/'),
                    getNamer().getReplaceFieldMethodName(), "(I)V");

                // End of switch
                visitor.visitLabel(endSwitchLabel);
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                visitor.visitInsn(Opcodes.RETURN);

                Label endLabel = new Label();
                visitor.visitLabel(endLabel);
                visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                visitor.visitLocalVariable(argNames[0], "I", null, startLabel, endLabel, 1);
                visitor.visitMaxs(4, 2);
            }
            else
            {
                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                visitor.visitVarInsn(Opcodes.ILOAD, 1);
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, pcSuperclassName.replace('.', '/'),
                    getNamer().getReplaceFieldMethodName(), "(I)V");
                visitor.visitInsn(Opcodes.RETURN);

                Label endLabel = new Label();
                visitor.visitLabel(endLabel);
                visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                visitor.visitLocalVariable(argNames[0], "I", null, startLabel, endLabel, 1);
                visitor.visitMaxs(2, 2);
            }
        }
        else
        {
            if (fields.length > 0)
            {
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
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                visitor.visitVarInsn(Opcodes.ILOAD, 1);

                Label[] fieldOptions = new Label[fields.length];
                for (int i=0;i<fields.length;i++)
                {
                    fieldOptions[i] = new Label();
                }
                Label defaultLabel = new Label();
                Label endSwitchLabel = new Label();

                // switch:
                visitor.visitTableSwitchInsn(0, fields.length-1, defaultLabel, fieldOptions);

                for (int i=0;i<fields.length;i++)
                {
                    visitor.visitLabel(fieldOptions[i]);
                    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                        getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitVarInsn(Opcodes.ILOAD, 1);
                    String methodNameType = EnhanceUtils.getTypeNameForPersistableMethod(fields[i].getType());
                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(),
                        "replacing" + methodNameType + "Field",
                        "(" + getNamer().getPersistableDescriptor() + "I)" + EnhanceUtils.getTypeDescriptorForJDOMethod(fields[i].getType()));
                    if (methodNameType.equals("Object"))
                    {
                        // Check any Object types for casting
                        visitor.visitTypeInsn(Opcodes.CHECKCAST, fields[i].getTypeName().replace('.', '/'));
                    }
                    if (fields[i] instanceof PropertyMetaData)
                    {
                        // Persistent properties so use jdoSetXXX(...)
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                            getNamer().getSetMethodPrefixMethodName() + fields[i].getName(), "(" + Type.getDescriptor(fields[i].getType()) + ")V");
                    }
                    else
                    {
                        // Persistent field so use xxx = ...
                        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
                            fields[i].getName(), Type.getDescriptor(fields[i].getType()));
                    }
                    visitor.visitJumpInsn(Opcodes.GOTO, endSwitchLabel);
                }

                // default:
                visitor.visitLabel(defaultLabel);
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuffer");
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitLdcInsn("out of field index :");
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
                visitor.visitVarInsn(Opcodes.ILOAD, 1);
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(I)Ljava/lang/StringBuffer;");
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
                visitor.visitInsn(Opcodes.ATHROW);

                // End of switch
                visitor.visitLabel(endSwitchLabel);
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                visitor.visitInsn(Opcodes.RETURN);

                Label endLabel = new Label();
                visitor.visitLabel(endLabel);
                visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                visitor.visitLocalVariable(argNames[0], "I", null, startLabel, endLabel, 1);
                visitor.visitMaxs(5, 2);
            }
            else
            {
                visitor.visitInsn(Opcodes.RETURN);

                Label endLabel = new Label();
                visitor.visitLabel(endLabel);
                visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                visitor.visitLocalVariable(argNames[0], "I", null, startLabel, endLabel, 1);
                visitor.visitMaxs(0, 2);
            }
        }

        visitor.visitEnd();
    }
}
