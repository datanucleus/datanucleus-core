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

import org.datanucleus.asm.Opcodes;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;

/**
 * Method to generate the method "__jdoPersistenceCapableSuperclassInit" using ASM.
 * <pre>
 * private static Class __jdoPersistenceCapableSuperclassInit()
 * {
 *     return null;
 * }
 * </pre>
 * or, where the class has a (persistent) superclass
 * <pre>
 * private static Class __jdoPersistenceCapableSuperclassInit()
 * {
 *     return ___jdo$loadClass(pcSuperclassName);
 * }
 * </pre>
 */
public class InitPersistenceCapableSuperclass extends ClassMethod
{
    public static InitPersistenceCapableSuperclass getInstance(ClassEnhancer enhancer)
    {
        return new InitPersistenceCapableSuperclass(enhancer, enhancer.getNamer().getPersistableSuperclassInitMethodName(),
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
            Class.class, null, null);
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
    public InitPersistenceCapableSuperclass(ClassEnhancer enhancer, String name, int access, 
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

        String pcSuperclassName = enhancer.getClassMetaData().getPersistenceCapableSuperclass();
        if (pcSuperclassName != null)
        {
            visitor.visitLdcInsn(pcSuperclassName);
            visitor.visitMethodInsn(Opcodes.INVOKESTATIC, getClassEnhancer().getASMClassName(), 
                getNamer().getLoadClassMethodName(), "(Ljava/lang/String;)Ljava/lang/Class;");
        }
        else
        {
            visitor.visitInsn(Opcodes.ACONST_NULL);
        }
        visitor.visitInsn(Opcodes.ARETURN);
        visitor.visitMaxs(1, 0);

        visitor.visitEnd();
    }
}