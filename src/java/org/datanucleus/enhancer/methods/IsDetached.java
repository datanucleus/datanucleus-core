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
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;

/**
 * Method to generate the method "jdoIsDetached" using ASM.
 * <pre>
 * public boolean jdoIsDetached()
 * {
 *     if (jdoStateManager == null &amp;&amp; jdoDetachedState != null) 
 *     {
 *         return true;
 *     }
 *     return false;
 * }
 * </pre>
 * and if not detachable will get
 * <pre>
 * public boolean jdoIsDetached()
 * {
 *     return false;
 * }
 * </pre>
 */
public class IsDetached extends ClassMethod
{
    public static IsDetached getInstance(ClassEnhancer enhancer)
    {
        return new IsDetached(enhancer, enhancer.getNamer().getIsDetachedMethodName(), Opcodes.ACC_PUBLIC,
            boolean.class, null, null);
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
    public IsDetached(ClassEnhancer enhancer, String name, int access, 
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
        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        if (getClassEnhancer().getClassMetaData().isDetachable())
        {
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), 
                getNamer().getStateManagerFieldName(), "L" + getNamer().getStateManagerAsmClassName() + ";");
            Label l1 = new Label();
            visitor.visitJumpInsn(Opcodes.IFNONNULL, l1);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), 
                getNamer().getDetachedStateFieldName(), "[Ljava/lang/Object;");
            visitor.visitJumpInsn(Opcodes.IFNULL, l1);

            visitor.visitInsn(Opcodes.ICONST_1);
            visitor.visitInsn(Opcodes.IRETURN);
            visitor.visitLabel(l1);
            visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        visitor.visitInsn(Opcodes.ICONST_0);
        visitor.visitInsn(Opcodes.IRETURN);
        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitMaxs(1, 1);
        visitor.visitEnd();
    }
}
