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
import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Method to generate the method "__jdoFieldFlagsInit" using ASM.
 * <pre>
 * private static final byte[] __jdoFieldFlagsInit()
 * {
 *     return new byte[] { 10, 24, 21 };
 * }
 * </pre>
 */
public class InitFieldFlags extends ClassMethod
{
    public static InitFieldFlags getInstance(ClassEnhancer enhancer)
    {
        return new InitFieldFlags(enhancer, enhancer.getNamer().getFieldFlagsInitMethodName(),
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            byte[].class, null, null);
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
    public InitFieldFlags(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Method to add the contents of the class method.
     */
    public void execute()
    {
        AbstractMemberMetaData fields[] = enhancer.getClassMetaData().getManagedMembers();

        visitor.visitCode();
        if (fields != null && fields.length > 0)
        {
            // Create array of bytes
            EnhanceUtils.addBIPUSHToMethod(visitor, fields.length);
            visitor.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);

            // Populate the array elements
            for (int i=0;i<fields.length;i++)
            {
                visitor.visitInsn(Opcodes.DUP);
                EnhanceUtils.addBIPUSHToMethod(visitor, i);
                EnhanceUtils.addBIPUSHToMethod(visitor, fields[i].getPersistenceFlags());
                visitor.visitInsn(Opcodes.BASTORE);
            }

            visitor.visitInsn(Opcodes.ARETURN);
            visitor.visitMaxs(4, 0);
        }
        else
        {
            // Create an empty array
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
            visitor.visitInsn(Opcodes.ARETURN);
            visitor.visitMaxs(1, 0);
        }
        visitor.visitEnd();
    }
}