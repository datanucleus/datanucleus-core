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
import org.datanucleus.util.ClassUtils;

/**
 * Method to generate the method "__jdoFieldTypesInit" using ASM.
 * <pre>
 * private static final Class[] __jdoFieldTypesInit()
 * {
 *     return new Class[] { ___jdo$loadClass("org.apache.jdo.test.B"), Long.TYPE,
 *              ___jdo$loadClass("java.lang.String") };
 * }
 * </pre>
 */
public class InitFieldTypes extends ClassMethod
{
    public static InitFieldTypes getInstance(ClassEnhancer enhancer)
    {
        return new InitFieldTypes(enhancer, enhancer.getNamer().getFieldTypesInitMethodName(),
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
            Class[].class, null, null);
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
    public InitFieldTypes(ClassEnhancer enhancer, String name, int access, 
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
            // Create array of Classes
            EnhanceUtils.addBIPUSHToMethod(visitor, fields.length);
            visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");

            // Populate the array elements
            for (int i=0;i<fields.length;i++)
            {
                visitor.visitInsn(Opcodes.DUP);
                EnhanceUtils.addBIPUSHToMethod(visitor, i);
                if (fields[i].getType().isPrimitive())
                {
                    String wrapperTypeName = ClassUtils.getWrapperTypeNameForPrimitiveTypeName(fields[i].getTypeName());
                    visitor.visitFieldInsn(Opcodes.GETSTATIC, 
                        wrapperTypeName.replace('.', '/'), "TYPE", "Ljava/lang/Class;");
                }
                else
                {
                    visitor.visitLdcInsn(fields[i].getTypeName());
                    visitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        enhancer.getClassMetaData().getFullClassName().replace('.', '/'), 
                        getNamer().getLoadClassMethodName(), "(Ljava/lang/String;)Ljava/lang/Class;");
                }
                visitor.visitInsn(Opcodes.AASTORE);
            }

            visitor.visitInsn(Opcodes.ARETURN);
            visitor.visitMaxs(4, 0);
        }
        else
        {
            // Create an empty array
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
            visitor.visitInsn(Opcodes.ARETURN);
            visitor.visitMaxs(1, 0);
        }
        visitor.visitEnd();
    }
}