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
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.PropertyMetaData;

/**
 * Method to generate the method "jdoCopyField" using ASM.
 * <pre>
 * protected final void jdoCopyField(Answer obj, int index)
 * {
 *     switch (index)
 *     {
 *         case 0:
 *             question = obj.question;
 *             break;
 *         default:
 *             throw new IllegalArgumentException("out of field index :" + index);
 *     }
 * }
 * </pre>
 * or with superclass
 * <pre>
 * protected final void jdoCopyField(ComplexAnswer obj, int index)
 * {
 *     switch (index - jdoInheritedFieldCount)
 *     {
 *         case 0:
 *             param1 = obj.param1;
 *             break;
 *         case 1:
 *             param2 = obj.param2;
 *             break;
 *         default:
 *             super.jdoCopyField(obj, index);
 *     }
 * }
 * </pre>
 * and also with minor variations if the class has no fields
 */
public class CopyField extends ClassMethod
{
    public static CopyField getInstance(ClassEnhancer enhancer)
    {
        return new CopyField(enhancer, enhancer.getNamer().getCopyFieldMethodName(), Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL,
            null, new Class[] {enhancer.getClassBeingEnhanced(), int.class}, new String[] { "obj", "index" });
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
    public CopyField(ClassEnhancer enhancer, String name, int access, 
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
        String pcSuperclassName = enhancer.getClassMetaData().getPersistenceCapableSuperclass();

        visitor.visitCode();

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        if (pcSuperclassName != null)
        {
            Class supercls = enhancer.getClassLoaderResolver().classForName(pcSuperclassName);
            String superclsDescriptor = Type.getDescriptor(supercls);

            if (fields.length > 0)
            {
                visitor.visitVarInsn(Opcodes.ILOAD, 2);
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
                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    if (fields[i] instanceof PropertyMetaData)
                    {
                        // Persistent property so use jdoSetXXX(obj.jdoGetXXX())
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                            getNamer().getGetMethodPrefixMethodName() + fields[i].getName(), "()" + Type.getDescriptor(fields[i].getType()));
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                            getNamer().getSetMethodPrefixMethodName() + fields[i].getName(), "(" + Type.getDescriptor(fields[i].getType()) + ")V");
                    }
                    else
                    {
                        // Persistent field so use xxx = obj.xxx
                        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                            fields[i].getName(), Type.getDescriptor(fields[i].getType()));
                        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
                            fields[i].getName(), Type.getDescriptor(fields[i].getType()));
                    }
                    visitor.visitJumpInsn(Opcodes.GOTO, endSwitchLabel);
                }

                // default :
                visitor.visitLabel(defaultLabel);
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                // super.jdoCopyField(obj, index);
                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitVarInsn(Opcodes.ILOAD, 2);
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, pcSuperclassName.replace('.', '/'),
                    getNamer().getCopyFieldMethodName(), "(" + superclsDescriptor + "I)V");

                // End of switch
                visitor.visitLabel(endSwitchLabel);
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                visitor.visitInsn(Opcodes.RETURN);
            }
            else
            {
                // super.jdoCopyField(obj, index);
                visitor.visitVarInsn(Opcodes.ALOAD, 0);
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitVarInsn(Opcodes.ILOAD, 2);
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, pcSuperclassName.replace('.', '/'),
                    getNamer().getCopyFieldMethodName(), "(" + superclsDescriptor + "I)V");

                visitor.visitInsn(Opcodes.RETURN);
            }
        }
        else
        {
            if (fields.length > 0)
            {
                visitor.visitVarInsn(Opcodes.ILOAD, 2);
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
                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    if (fields[i] instanceof PropertyMetaData)
                    {
                        // Persistent property so use jdoSetXXX(obj.jdoGetXXX())
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                            getNamer().getGetMethodPrefixMethodName() + fields[i].getName(), "()" + Type.getDescriptor(fields[i].getType()));
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
                            getNamer().getSetMethodPrefixMethodName() + fields[i].getName(), "(" + Type.getDescriptor(fields[i].getType()) + ")V");
                    }
                    else
                    {
                        // Persistent field so use xxx = obj.xxx
                        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
                            fields[i].getName(), Type.getDescriptor(fields[i].getType()));
                        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
                            fields[i].getName(), Type.getDescriptor(fields[i].getType()));
                    }
                    visitor.visitJumpInsn(Opcodes.GOTO, endSwitchLabel);
                }

                // default:
                visitor.visitLabel(defaultLabel);
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                // throw new IllegalArgumentException("out of field index :" + index);
                visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuffer");
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitLdcInsn("out of field index :");
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
                visitor.visitVarInsn(Opcodes.ILOAD, 2);
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(I)Ljava/lang/StringBuffer;");
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
                visitor.visitInsn(Opcodes.ATHROW);

                // End of switch
                visitor.visitLabel(endSwitchLabel);
                visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                visitor.visitInsn(Opcodes.RETURN);
            }
            else
            {
                // throw new IllegalArgumentException("out of field index :" + index);
                visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuffer");
                visitor.visitInsn(Opcodes.DUP);
                visitor.visitLdcInsn("out of field index :");
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
                visitor.visitVarInsn(Opcodes.ILOAD, 2);
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(I)Ljava/lang/StringBuffer;");
                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
                visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
                visitor.visitInsn(Opcodes.ATHROW);
            }
        }

        // Set parameter names
        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitLocalVariable(argNames[0], getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 1);
        visitor.visitLocalVariable(argNames[1], "I", null, startLabel, endLabel, 2);

        if (pcSuperclassName != null)
        {
            visitor.visitMaxs(3, 3);
        }
        else
        {
            visitor.visitMaxs(5, 3);
        }

        visitor.visitEnd();
    }
}
