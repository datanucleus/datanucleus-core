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

/**
 * Method to generate the method "dnSuperClone" using ASM.
 * <pre>
 * private Object dnSuperClone() throws CloneNotSupportedException
 * {
 *     MyClass o = (MyClass) super.clone();
 *     o.dnFlags = (byte) 0;
 *     o.dnStateManager = null;
 *     return o;
 * }
 * </pre>
 */
public class SuperClone extends ClassMethod
{
    public static SuperClone getInstance(ClassEnhancer enhancer)
    {
        return new SuperClone(enhancer, enhancer.getNamer().getSuperCloneMethodName(), 
            Opcodes.ACC_PRIVATE,
            Object.class, null, null, new String[] {CloneNotSupportedException.class.getName().replace('.', '/')});
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
    public SuperClone(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames, String[] exceptions)
    {
        super(enhancer, name, access, returnType, argTypes, argNames, exceptions);
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
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, EnhanceUtils.ACN_Object,
            "clone", "()" + EnhanceUtils.CD_Object);
        visitor.visitTypeInsn(Opcodes.CHECKCAST, getClassEnhancer().getASMClassName());
        visitor.visitVarInsn(Opcodes.ASTORE, 1);
        Label l1 = new Label();
        visitor.visitLabel(l1);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitInsn(Opcodes.ICONST_0);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), getNamer().getFlagsFieldName(), "B");
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitInsn(Opcodes.ACONST_NULL);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitInsn(Opcodes.ARETURN);

        Label l4 = new Label();
        visitor.visitLabel(l4);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, l0, l4, 0);
        visitor.visitLocalVariable("o", getClassEnhancer().getClassDescriptor(), null, l1, l4, 1);
        visitor.visitMaxs(2, 2);

        visitor.visitEnd();
    }
}