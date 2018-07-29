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

/**
 * Method to generate the method "dnReplaceStateManager" using ASM.
 * <pre>
 * public final synchronized void dnReplaceStateManager(StateManager stateManager)
 * {
 *     if (dnStateManager != null)
 *     {
 *         dnStateManager = dnStateManager.replacingStateManager(this, stateManager);
 *     }
 *     else
 *     {
 *         dnStateManager = stateManager;
 *         dnFlags = (byte) 1;
 *     }
 * }
 * </pre>
 */
public class ReplaceStateManager extends ClassMethod
{
    public static ReplaceStateManager getInstance(ClassEnhancer enhancer)
    {
        return new ReplaceStateManager(enhancer, enhancer.getNamer().getReplaceStateManagerMethodName(), 
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNCHRONIZED,
            null, new Class[] {enhancer.getNamer().getStateManagerClass()}, new String[] {"sm"});
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
    public ReplaceStateManager(ClassEnhancer enhancer, String name, int access, Object returnType, Object[] argTypes, String[] argNames)
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

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        Label l1 = new Label();
        visitor.visitJumpInsn(Opcodes.IFNULL, l1);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getStateManagerAsmClassName(), "replacingStateManager",
            "(" + getNamer().getPersistableDescriptor() + getNamer().getStateManagerDescriptor() + ")" + getNamer().getStateManagerDescriptor(), true);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        Label l3 = new Label();
        visitor.visitJumpInsn(Opcodes.GOTO, l3);

        visitor.visitLabel(l1);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), getNamer().getStateManagerFieldName(), getNamer().getStateManagerDescriptor());
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitInsn(Opcodes.ICONST_1);
        visitor.visitFieldInsn(Opcodes.PUTFIELD, getClassEnhancer().getASMClassName(), getNamer().getFlagsFieldName(), "B");
        visitor.visitLabel(l3);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        visitor.visitInsn(Opcodes.RETURN);

        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitLocalVariable(argNames[0], getNamer().getStateManagerDescriptor(), null, startLabel, endLabel, 1);
        visitor.visitMaxs(4, 2);

        visitor.visitEnd();
    }
}
