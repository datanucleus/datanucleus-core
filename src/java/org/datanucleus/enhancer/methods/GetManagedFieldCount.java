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
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.metadata.ClassMetaData;

/**
 * Method to generate the method "jdoGetManagedFieldCount" using ASM.
 */
public class GetManagedFieldCount extends ClassMethod
{
    public static GetManagedFieldCount getInstance(ClassEnhancer enhancer)
    {
        return new GetManagedFieldCount(enhancer, enhancer.getNamer().getGetManagedFieldCountMethodName(),
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
    public GetManagedFieldCount(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Method to add the contents of the class method.
     * 
     * Usually this method should generate bytecode as:
     * <code>return jdoFieldNames.length + superClass.jdoGetManagedFieldCount();</code>
     * but due to initializing issues [ENHANCER-58], we use constants instead, e.g.:
     * <code>return {number of managed fields}+superClass.jdoGetManagedFieldCount();</code>
     */
    public void execute()
    {
        ClassMetaData cmd = enhancer.getClassMetaData();
        String persistenceCapableSuperclass = cmd.getPersistableSuperclass();

        visitor.visitCode();

        if (persistenceCapableSuperclass != null && persistenceCapableSuperclass.length() > 0)
        {
            EnhanceUtils.addBIPUSHToMethod(visitor, cmd.getNoOfManagedMembers());
            visitor.visitMethodInsn(Opcodes.INVOKESTATIC, persistenceCapableSuperclass.replace('.', '/'),
                methodName, "()I");
            visitor.visitInsn(Opcodes.IADD);
            visitor.visitInsn(Opcodes.IRETURN);
            visitor.visitMaxs(2, 0);
        }
        else
        {
            EnhanceUtils.addBIPUSHToMethod(visitor, cmd.getNoOfManagedMembers());
            visitor.visitInsn(Opcodes.IRETURN);
            visitor.visitMaxs(1, 0);
        }

        visitor.visitEnd();
    }
}