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
package org.datanucleus.enhancer.methods;

import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.enhancer.asm.Label;
import org.datanucleus.enhancer.asm.Opcodes;

/**
 * Method to generate a default "clone" method, using ASM, that has the effect of nulling the state manager etc.
 * <pre>
 * public Object clone() throws CloneNotSupportedException
 * {
 *     MyClass copy = (MyClass) super.clone();
 *     copy.dnFlags = (byte) 0;
 *     copy.dnStateManager = null;
 *     return copy;
 * }
 * </pre>
 */
public class Clone extends ClassMethod
{
    public static Clone getInstance(ClassEnhancer enhancer)
    {
        return new Clone(enhancer, "clone", Opcodes.ACC_PUBLIC, Object.class, null, null, new String[] {CloneNotSupportedException.class.getName().replace('.', '/')});
    }

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param name Name of method
     * @param access Access type
     * @param returnType Return type
     * @param argTypes Argument types
     * @param argNames Argument names
     * @param exceptions Any exceptions thrown
     */
    public Clone(ClassEnhancer enhancer, String name, int access, Object returnType, Object[] argTypes, String[] argNames, String[] exceptions)
    {
        super(enhancer, name, access, returnType, argTypes, argNames, exceptions);
    }

    /**
     * Method to add the contents of the method.
     */
    public void execute()
    {
        visitor.visitCode();

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, EnhanceUtils.ACN_Object, "clone", "()" + EnhanceUtils.CD_Object);
        visitor.visitTypeInsn(Opcodes.CHECKCAST, getClassEnhancer().getASMClassName());
        visitor.visitVarInsn(Opcodes.ASTORE, 1);

        Label l1 = new Label();
        visitor.visitLabel(l1);

        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitInsn(Opcodes.ICONST_0);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), getNamer().getFlagsFieldName(), "B");

        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitInsn(Opcodes.ACONST_NULL);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());

        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitInsn(Opcodes.ARETURN);

        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitLocalVariable("copy", getClassEnhancer().getClassDescriptor(), null, l1, endLabel, 1);
        visitor.visitMaxs(2, 2);

        visitor.visitEnd();
    }
}