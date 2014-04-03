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
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;

/**
 * Method to generate the method "loadClass" using ASM.
 */
public class LoadClass extends ClassMethod
{
    public static LoadClass getInstance(ClassEnhancer enhancer)
    {
        return new LoadClass(enhancer, enhancer.getNamer().getLoadClassMethodName(),
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            Class.class, new Class[] {String.class}, new String[] {"className"});
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
    public LoadClass(ClassEnhancer enhancer, String name, int access, 
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
        Label l1 = new Label();
        Label l2 = new Label();

        visitor.visitTryCatchBlock(l0, l1, l2, "java/lang/ClassNotFoundException");
        visitor.visitLabel(l0);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        visitor.visitLabel(l1);
        visitor.visitInsn(Opcodes.ARETURN);
        visitor.visitLabel(l2);
        visitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/ClassNotFoundException"});

        visitor.visitVarInsn(Opcodes.ASTORE, 1);
        Label l3 = new Label();
        visitor.visitLabel(l3);
        visitor.visitTypeInsn(Opcodes.NEW, "java/lang/NoClassDefFoundError");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitVarInsn(Opcodes.ALOAD, 1);
        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassNotFoundException", "getMessage", "()Ljava/lang/String;");
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/NoClassDefFoundError", "<init>", "(Ljava/lang/String;)V");
        visitor.visitInsn(Opcodes.ATHROW);
        Label l4 = new Label();
        visitor.visitLabel(l4);
        visitor.visitLocalVariable(argNames[0], "Ljava/lang/String;", null, l0, l4, 0);
        visitor.visitLocalVariable("e", "Ljava/lang/ClassNotFoundException;", null, l3, l4, 1);

        visitor.visitMaxs(3, 2);
        visitor.visitEnd();
    }
}
