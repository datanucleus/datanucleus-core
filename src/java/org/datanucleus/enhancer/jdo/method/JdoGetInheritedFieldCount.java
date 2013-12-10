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

import org.datanucleus.asm.Opcodes;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.metadata.ClassMetaData;

/**
 * Method to generate the method "__jdoGetInheritedFieldCount" using ASM.
 */
public class JdoGetInheritedFieldCount extends ClassMethod
{
    public static JdoGetInheritedFieldCount getInstance(ClassEnhancer enhancer)
    {
        return new JdoGetInheritedFieldCount(enhancer, enhancer.getNamer().getGetInheritedFieldCountMethodName(),
            Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC,
            int.class, null, null);
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
    public JdoGetInheritedFieldCount(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Method to add the contents of the class method.
     */
    public void execute()
    {
        ClassMetaData cmd = enhancer.getClassMetaData();
        String persistenceCapableSuperclass = cmd.getPersistenceCapableSuperclass();

        visitor.visitCode();

        if (persistenceCapableSuperclass != null && persistenceCapableSuperclass.length() > 0)
        {
            visitor.visitMethodInsn(Opcodes.INVOKESTATIC, persistenceCapableSuperclass.replace('.', '/'),
                getNamer().getGetManagedFieldCountMethodName(), "()I");
            visitor.visitInsn(Opcodes.IRETURN);
            visitor.visitMaxs(1, 0);
        }
        else
        {
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitInsn(Opcodes.IRETURN);
            visitor.visitMaxs(1, 0);
        }

        visitor.visitEnd();
    }
}