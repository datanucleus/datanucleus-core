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
package org.datanucleus.enhancer;

import java.util.Arrays;

import org.datanucleus.asm.ClassVisitor;
import org.datanucleus.asm.MethodVisitor;
import org.datanucleus.asm.Type;
import org.datanucleus.util.Localiser;

/**
 * Representation of a method that an enhanced class requires.
 */
public abstract class ClassMethod
{
    /** The parent enhancer. */
    protected ClassEnhancer enhancer;

    /** Name of the method. */
    protected String methodName;

    /** Access flags for the method (public, protected etc). */
    protected int access;

    /** Return type for the method */
    protected Object returnType;

    /** Types of the arguments. */
    protected Object[] argTypes;

    /** Names of the arguments. */
    protected String[] argNames;

    /** Exceptions that can be thrown. */
    protected String[] exceptions;

    /** Visitor for use in updating the method of the class (set in initialise). */
    protected MethodVisitor visitor;

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param name Name of the method
     * @param access Access for the method (PUBLIC, PROTECTED etc)
     * @param returnType Return type
     * @param argTypes Argument type(s)
     * @param argNames Argument name(s)
     */
    public ClassMethod(ClassEnhancer enhancer, String name, int access, 
            Object returnType, Object[] argTypes, String[] argNames)
    {
        this(enhancer, name, access, returnType, argTypes, argNames, null);
    }

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param name Name of the method
     * @param access Access for the method (PUBLIC, PROTECTED etc)
     * @param returnType Return type
     * @param argTypes Argument type(s)
     * @param argNames Argument name(s)
     * @param exceptions Exceptions that can be thrown
     */
    public ClassMethod(ClassEnhancer enhancer, String name, int access, 
            Object returnType, Object[] argTypes, String[] argNames, String[] exceptions)
    {
        this.enhancer = enhancer;
        this.methodName = name;
        this.access = access;
        this.returnType = returnType;
        this.argTypes = argTypes;
        this.argNames = argNames;
        this.exceptions = exceptions;
    }

    /**
     * Default implementation of initialise, specifying the method based on the ClassMethod info.
     */
    public void initialise()
    {
        // Do nothing. Already set up in initialise(ClassVisitor)
    }

    /**
     * Method to initialise the class method.
     * @param classVisitor Visitor for the class
     */
    public void initialise(ClassVisitor classVisitor)
    {
        // Add the method to the class using the ClassMethod info
        Type type = null;
        Type[] argtypes = null;
        if (returnType != null)
        {
            type = Type.getType((Class)returnType);
        }
        else
        {
            type = Type.VOID_TYPE;
        }
        if (argTypes != null)
        {
            argtypes = new Type[argTypes.length];
            for (int i=0;i<argTypes.length;i++)
            {
                argtypes[i] = Type.getType((Class)argTypes[i]);
            }
        }
        else
        {
            argtypes = new Type[0];
        }
        String methodDesc = Type.getMethodDescriptor(type, argtypes);
        this.visitor = classVisitor.visitMethod(access, methodName, methodDesc, null, exceptions);
    }

    /**
     * Convenience accessor for the ClassEnhancer
     * @return ClassEnhancer
     */
    protected ClassEnhancer getClassEnhancer()
    {
        return enhancer;
    }

    /**
     * Accessor for the descriptor of the method.
     * @return The descriptor
     */
    public String getDescriptor()
    {
        StringBuilder str = new StringBuilder("(");
        if (argTypes != null && argTypes.length > 0)
        {
            for (int i=0;i<argTypes.length;i++)
            {
                str.append(Type.getDescriptor((Class)argTypes[i]));
            }
        }
        str.append(")");
        if (returnType != null)
        {
            str.append(Type.getDescriptor((Class)returnType));
        }
        else
        {
            str.append("V");
        }
        return str.toString();
    }

    public EnhancementNamer getNamer()
    {
        return enhancer.getNamer();
    }

    /**
     * Accessor for the method name
     * @return Name of the method
     */
    public String getName()
    {
        return methodName;
    }

    /**
     * Accessor for the access
     * @return Access for the method
     */
    public int getAccess()
    {
        return access;
    }

    /**
     * Method to add the contents of the class method.
     */
    public abstract void execute();

    /**
     * Method to close the definition of the class method.
     * This implementation simply logs a debug message to category ENHANCER.
     */
    public void close()
    {
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
        {
            String msg = getMethodAdditionMessage(methodName, returnType, argTypes, argNames);
            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005019", msg));
        }
    }

    /**
     * Return hash code of this instance.
     * @return hash code of this instance
     */
    public int hashCode()
    {
        return methodName.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param o the reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    public boolean equals(Object o)
    {
        if (o instanceof ClassMethod)
        {
            ClassMethod cb = (ClassMethod)o;
            if (cb.methodName.equals(methodName))
            {
                return Arrays.equals(cb.argTypes, argTypes);
            }
        }
        return false;
    }

    /**
     * Convenience method to generate a message that a method has been added.
     * @param methodName Name of the method
     * @param returnType Return type of the method
     * @param argTypes arg types for the method
     * @param argNames arg names for the method
     * @return The message
     */
    public static String getMethodAdditionMessage(String methodName, Object returnType, Object[] argTypes, String[] argNames)
    {
        StringBuilder sb = new StringBuilder();
        if (returnType != null)
        {
            if (returnType instanceof Class)
            {
                sb.append(((Class)returnType).getName()).append(" ");
            }
            else
            {
                sb.append(returnType).append(" ");
            }
        }
        else
        {
            sb.append("void ");
        }
        sb.append(methodName).append("(");
        if (argTypes != null)
        {
            for (int i = 0; i < argTypes.length; i++)
            {
                if (i != 0)
                {
                    sb.append(", ");
                }
                sb.append(argTypes[i]).append(" ").append(argNames[i]);
            }
        }
        sb.append(")");
        return sb.toString();
    }
}