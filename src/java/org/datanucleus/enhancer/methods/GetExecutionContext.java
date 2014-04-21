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
 * Method to generate the method "dnGetExecutionContext" using ASM.
 * <pre>
 * public final ExecutionContext dnGetExecutionContext()
 * {
 *     return (dnStateManager != null ? dnStateManager.getExecutionContext(this) : null);
 * }
 * </pre>
 */
public class GetExecutionContext extends ClassMethod
{
    public static GetExecutionContext getInstance(ClassEnhancer enhancer)
    {
        return new GetExecutionContext(enhancer, enhancer.getNamer().getGetExecutionContextMethodName(), 
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            enhancer.getNamer().getExecutionContextClass(), null, null);
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
    public GetExecutionContext(ClassEnhancer enhancer, String name, int access, 
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

        Label l0 = new Label();
        visitor.visitLabel(l0);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        Label l1 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNULL, l1);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(),
            getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(),
            "getExecutionContext", // TODO Put this in namer
            "(" + getNamer().getPersistableDescriptor() + ")" + getNamer().getExecutionContextDescriptor());
        Label l2 = new Label();
        visitor.visitJumpInsn(Opcodes.GOTO, l2);
        visitor.visitLabel(l1);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        visitor.visitInsn(Opcodes.ACONST_NULL);
        visitor.visitLabel(l2);
        visitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {getClassEnhancer().getNamer().getExecutionContextAsmClassName()});

        visitor.visitInsn(Opcodes.ARETURN);
        Label l3 = new Label();
        visitor.visitLabel(l3);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, l0, l3, 0);
        visitor.visitMaxs(2, 1);

        visitor.visitEnd();
    }
}
