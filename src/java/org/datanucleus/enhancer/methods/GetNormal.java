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
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Method to generate the method "jdoGetZZZ" using ASM for NORMAL_GET fields.
 * <pre>
 * static YYY jdoGetZZZ(MyClass objPC)
 * {
 *     return objPC.ZZZ;
 * }
 * </pre>
 */
public class GetNormal extends ClassMethod
{
    /** Field that this jdoGetXXX is for. */
    protected AbstractMemberMetaData fmd;

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param fmd MetaData for the field we are generating for
     */
    public GetNormal(ClassEnhancer enhancer, AbstractMemberMetaData fmd)
    {
        super(enhancer, enhancer.getNamer().getGetMethodPrefixMethodName() + fmd.getName(),
            (fmd.isPublic() ? Opcodes.ACC_PUBLIC : 0) | (fmd.isProtected() ? Opcodes.ACC_PROTECTED : 0) | 
            (fmd.isPrivate() ? Opcodes.ACC_PRIVATE : 0) | Opcodes.ACC_STATIC, fmd.getType(), null, null);

        // Set the arg type/name
        argTypes = new Class[] {getClassEnhancer().getClassBeingEnhanced()};
        argNames = new String[] {"objPC"};

        this.fmd = fmd;
    }

    /**
     * Method to add the contents of the class method.
     */
    public void execute()
    {
        visitor.visitCode();

        String fieldTypeDesc = Type.getDescriptor(fmd.getType());

        Label startLabel = new Label();
        visitor.visitLabel(startLabel);

        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, getClassEnhancer().getASMClassName(), fmd.getName(), fieldTypeDesc);
        EnhanceUtils.addReturnForType(visitor, (Class)returnType);

        Label endLabel = new Label();
        visitor.visitLabel(endLabel);
        visitor.visitLocalVariable(argNames[0], getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
        visitor.visitMaxs(1, 1);

        visitor.visitEnd();
    }
}