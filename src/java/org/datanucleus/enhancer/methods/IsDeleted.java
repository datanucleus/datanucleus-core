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

/**
 * Method to generate the method "jdoIsDeleted" using ASM.
 */
public class IsDeleted extends IsXXX
{
    public static IsDeleted getInstance(ClassEnhancer enhancer)
    {
        return new IsDeleted(enhancer, enhancer.getNamer().getIsDeletedMethodName(),
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            boolean.class, null, null);
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
    public IsDeleted(ClassEnhancer enhancer, String name, int access, Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Method returning the name of the method on the StateManager that gives the return info.
     * @return Name of the StateManager method (isNew, isPersistent, isDeleted etc)
     */
    protected String getStateManagerIsMethod()
    {
        return "isDeleted";
    }
}