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

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.datanucleus.asm.Label;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;

/**
 * Method to generate the method "writeObject" using ASM.
 * <pre>
 * private void writeObject(ObjectOutputStream out)
 * throws IOException
 * {
 *     jdoPreSerialize();
 *     out.defaultWriteObject();
 * }
 * </pre>
 */
public class WriteObject extends ClassMethod
{
    public static WriteObject getInstance(ClassEnhancer enhancer)
    {
        return new WriteObject(enhancer, "writeObject", 
            Opcodes.ACC_PRIVATE,
            null, new Class[] {ObjectOutputStream.class}, new String[] {"out"},
            new String[] {IOException.class.getName().replace('.', '/')});
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
    public WriteObject(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param name Name of method
     * @param access Access type
     * @param returnType Return type
     * @param argTypes Argument types
     * @param argNames Argument names
     * @param exceptions Exceptions that are thrown
     */
    public WriteObject(ClassEnhancer enhancer, String name, int access, 
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

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(), 
            getNamer().getPreSerializeMethodName(), "()V");
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/ObjectOutputStream", "defaultWriteObject", "()V");
        visitor.visitInsn(Opcodes.RETURN);

        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitLocalVariable(argNames[0], "Ljava/io/ObjectOutputStream;", null, startLabel, endLabel, 1);
        visitor.visitMaxs(1, 2);

        visitor.visitEnd();
    }
}