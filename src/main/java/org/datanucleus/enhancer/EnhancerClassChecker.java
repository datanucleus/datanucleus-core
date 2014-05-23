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

import java.util.HashSet;
import java.util.Iterator;

import org.datanucleus.asm.AnnotationVisitor;
import org.datanucleus.asm.Attribute;
import org.datanucleus.asm.ClassVisitor;
import org.datanucleus.asm.FieldVisitor;
import org.datanucleus.asm.MethodVisitor;
import org.datanucleus.asm.Type;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.util.Localiser;

/**
 * Visitor used to check the enhancement state of a class.
 * Checks the methods/fields present against what is required for enhancement.
 */
public class EnhancerClassChecker extends ClassVisitor
{
    /** Enhancer for the class. */
    protected ClassEnhancer enhancer;

    /** Set of fields required to be present. */
    protected HashSet<ClassField> fieldsRequired = new HashSet<ClassField>();

    /** Set of methods required to be present. */
    protected HashSet<ClassMethod> methodsRequired = new HashSet<ClassMethod>();

    /** Flag for whether the class is enhanced. Set in the visit process. */
    protected boolean enhanced = false;

    /** Whether to log any errors at error level. */
    protected boolean logErrors = true;

    /**
     * Constructor.
     * @param enhancer The class enhancer
     * @param logErrors Whether to log any errors at error level
     */
    public EnhancerClassChecker(ClassEnhancer enhancer, boolean logErrors)
    {
        super(ClassEnhancer.ASM_API_VERSION);

        this.enhancer = enhancer;
        this.logErrors = logErrors;
        if (enhancer.getClassMetaData().getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
        {
            fieldsRequired.addAll(enhancer.getFieldsList());
            methodsRequired.addAll(enhancer.getMethodsList());
        }
    }

    /**
     * Accessor for whether the class is considered enhanced.
     * Should only be run after passing this class to the reader as a visitor.
     * @return Whether the class is enhanced.
     */
    public boolean isEnhanced()
    {
        return enhanced;
    }

    /**
     * Convenience method to report an error in the enhancement of this class.
     * @param msg The message
     */
    protected void reportError(String msg)
    {
        if (logErrors)
        {
            DataNucleusEnhancer.LOGGER.error(msg);
        }
        else
        {
            if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
            {
                DataNucleusEnhancer.LOGGER.debug(msg);
            }
        }
        enhanced = false;
    }

    /**
     * Method to visit the header of the class
     * @param version Version of the class file
     * @param access Access type
     * @param name name of the class
     * @param signature signature of the class
     * @param supername superclass name
     * @param interfaces interface(s)
     */
    public void visit(int version, int access, String name, String signature, String supername, String[] interfaces)
    {
        enhanced = true; // Default to true unless we find a problem
        if (enhancer.getClassMetaData().getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
        {
            if (interfaces == null)
            {
                enhanced = false;
                return;
            }

            if (!hasInterface(interfaces, enhancer.getNamer().getPersistableAsmClassName()))
            {
                reportError(Localiser.msg("Enhancer.Check.InterfaceMissing", enhancer.getClassName(), enhancer.getNamer().getPersistableClass().getName()));
            }

            if (enhancer.getClassMetaData().isDetachable())
            {
                if (!hasInterface(interfaces, enhancer.getNamer().getDetachableAsmClassName()))
                {
                    reportError(Localiser.msg("Enhancer.Check.InterfaceMissing", enhancer.getClassName(), enhancer.getNamer().getDetachableClass().getName()));
                }
            }
        }
    }

    /**
     * Convenience method to check if a particular interface is present in the list.
     * @param interfaces The list of interfaces implemented
     * @param intf The interface we are looking for
     * @return Whether it is present
     */
    protected boolean hasInterface(String[] interfaces, String intf)
    {
        if (interfaces == null || interfaces.length <= 0)
        {
            return false;
        }

        for (int i=0;i<interfaces.length;i++)
        {
            if (interfaces[i].equals(intf))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to visit a class annotations
     * @param desc descriptor of the annotation
     * @param visible Whether visible
     * @return The visitor
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible)
    {
        return null;
    }

    /**
     * Method to visit a non-standard attribute
     * @param attr the attribute
     */
    public void visitAttribute(Attribute attr)
    {
    }

    /**
     * Visit the end of the class
     */
    public void visitEnd()
    {
        if (enhancer.getClassMetaData().getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
        {
            // Check for required fields/methods
            Iterator fieldsIter = fieldsRequired.iterator();
            while (fieldsIter.hasNext())
            {
                ClassField field = (ClassField)fieldsIter.next();
                reportError(Localiser.msg("Enhancer.Check.FieldMissing", enhancer.getClassName(), field.getName()));
            }

            Iterator methodsIter = methodsRequired.iterator();
            while (methodsIter.hasNext())
            {
                ClassMethod method = (ClassMethod)methodsIter.next();
                reportError(Localiser.msg("Enhancer.Check.MethodMissing", enhancer.getClassName(), method.getName()));
            }
        }
        else if (enhancer.getClassMetaData().getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_AWARE)
        {
            // TODO Fix this so we do a real check on whether the existing methods are enhanced ok
            enhanced = false;
        }
    }

    /**
     * Visit a field of the class.
     * @param access Access for the field
     * @param name name of the field
     * @param desc Descriptor of the field
     * @param signature signature of the field
     * @param value initial value
     * @return The visitor for the field
     */
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
    {
        Iterator iter = fieldsRequired.iterator();
        while (iter.hasNext())
        {
            ClassField field = (ClassField)iter.next();
            if (field.getName().equals(name))
            {
                if (field.getAccess() != access)
                {
                    reportError(Localiser.msg("Enhancer.Check.FieldIncorrectAccess", enhancer.getClassName(), name));
                }
                else if (!desc.equals(Type.getDescriptor((Class)field.getType())))
                {
                    reportError(Localiser.msg("Enhancer.Check.FieldIncorrectType", enhancer.getClassName(), name));
                }
                else
                {
                    // Remove the field since it is present
                    iter.remove();
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Visit an inner class of the class
     * @param name Internal name of the class
     * @param outerName name of the outer class
     * @param innerName name of the inner class
     * @param access access of the inner class
     */
    public void visitInnerClass(String name, String outerName, String innerName, int access)
    {
    }

    /**
     * Visit a method of the class
     * @param access Access for the field
     * @param name name of the field
     * @param desc Descriptor of the field
     * @param signature signature of the field
     * @param exceptions Exceptions thrown
     * @return visitor for the method
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        Iterator<ClassMethod> iter = methodsRequired.iterator();
        while (iter.hasNext())
        {
            ClassMethod method = iter.next();
            if (method.getName().equals(name) && method.getDescriptor().equals(desc))
            {
                if (method.getAccess() != access)
                {
                    reportError(Localiser.msg("Enhancer.Check.MethodIncorrectAccess", enhancer.getClassName(), name));
                }
                else
                {
                    // Remove the method since it is present
                    iter.remove();
                    break;
                }
            }
        }

        // TODO Check enhancement of all fields to use dnGetXXX, dnSetXXX
        return null;
    }

    /**
     * Visit an outer class.
     * @param owner owner for the outer class
     * @param name name of the outer class
     * @param desc Descriptor of the outer class
     */
    public void visitOuterClass(String owner, String name, String desc)
    {
    }

    /**
     * Visit the source of the class
     * @param source name of source file
     * @param debug debug info
     */
    public void visitSource(String source, String debug)
    {
    }
}
