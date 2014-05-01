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

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.asm.ClassVisitor;
import org.datanucleus.asm.FieldVisitor;
import org.datanucleus.asm.MethodVisitor;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.methods.DefaultConstructor;
import org.datanucleus.enhancer.methods.InitClass;
import org.datanucleus.enhancer.methods.GetNormal;
import org.datanucleus.enhancer.methods.GetViaCheck;
import org.datanucleus.enhancer.methods.GetViaMediate;
import org.datanucleus.enhancer.methods.SetNormal;
import org.datanucleus.enhancer.methods.SetViaCheck;
import org.datanucleus.enhancer.methods.SetViaMediate;
import org.datanucleus.enhancer.methods.WriteObject;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Adapter visitor class for providing enhancement of an existing class using ASM.
 * Is created with its own ClassWriter, and is passed to a ClassReader to visit the class.
 * All parts of the class to be enhanced are fed through the different visitXXX methods here
 * allowing intervention to either enhance an existing method, or to add on new fields/methods/interfaces.
 */
public class EnhancerClassAdapter extends ClassVisitor
{
    /** Localisation of messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", ClassEnhancer.class.getClassLoader());

    /** The underlying enhancer. */
    protected ClassEnhancer enhancer;

    /** Whether a default constructor is present. Set if found, and then processed in visitEnd. */
    protected boolean hasDefaultConstructor = false;

    /** Whether the field serialVersionUID is present. Set if found, and processed in visitEnd. */
    protected boolean hasSerialVersionUID = false;

    /** Whether the field xxxDetachedState is present. Set if found, and processed in visitEnd. */
    protected boolean hasDetachedState = false;

    /** Whether the method writeObject(ObjectOutputStream) is present. Set if found, and processed in visitEnd. */
    protected boolean hasWriteObject = false;

    /** Whether the class already has a static init block. Set if found, and processed in visitEnd. */
    protected boolean hasStaticInitialisation = false;

    /**
     * Constructor.
     * If the writer is null it means we just have to check the enhancement status
     * @param cv The writer visitor
     * @param enhancer ClassEnhancer
     */
    public EnhancerClassAdapter(ClassVisitor cv, ClassEnhancer enhancer)
    {
        super(ClassEnhancer.ASM_API_VERSION, cv);
        this.enhancer = enhancer;
    }

    /**
     * Method called to visit the header of the class.
     * @param version Version of this class
     * @param access Access for the class
     * @param name name of the class
     * @param signature Signature of the class
     * @param superName Superclass name (if any)
     * @param interfaces Interface(s) implemented
     */
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
    {
        if (enhancer.getClassMetaData().getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
        {
            // Check if the class already implements required interfaces
            boolean alreadyPersistable = false;
            boolean alreadyDetachable = false;
            boolean needsPersistable = false;
            boolean needsDetachable = false;
            int numInterfaces = 0;
            if (interfaces != null && interfaces.length > 0)
            {
                numInterfaces = interfaces.length;
                for (int i=0;i<interfaces.length;i++)
                {
                    if (interfaces[i].equals(enhancer.getNamer().getDetachableAsmClassName()))
                    {
                        alreadyDetachable = true;
                    }
                    if (interfaces[i].equals(enhancer.getNamer().getPersistableAsmClassName()))
                    {
                        alreadyPersistable = true;
                    }
                }
            }
            if (!alreadyDetachable && enhancer.getClassMetaData().isDetachable())
            {
                numInterfaces++;
                needsDetachable = true;
            }
            if (!alreadyPersistable)
            {
                numInterfaces++;
                needsPersistable = true;
            }

            String[] intfs = interfaces;
            if (needsDetachable || needsPersistable)
            {
                // Allocate updated array of interfaces
                intfs = new String[numInterfaces];
                int position = 0;
                if (interfaces != null && interfaces.length > 0)
                {
                    for (int i=0;i<interfaces.length;i++)
                    {
                        intfs[position++] = interfaces[i];
                    }
                }

                if (needsDetachable)
                {
                    intfs[position++] = enhancer.getNamer().getDetachableAsmClassName();
                    if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
                    {
                        DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.AddInterface", enhancer.getNamer().getDetachableClass().getName()));
                    }
                }
                if (needsPersistable)
                {
                    intfs[position++] = enhancer.getNamer().getPersistableAsmClassName();
                    if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
                    {
                        DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.AddInterface", enhancer.getNamer().getPersistableClass().getName()));
                    }
                }
            }
            cv.visit(version, access, name, signature, superName, intfs);
        }
        else
        {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
    }

    /**
     * Method called when a field of the class is visited.
     * @param access Access type
     * @param name Name of the field
     * @param desc Descriptor of the field
     * @param signature Signature of the field
     * @param value Value of the field
     * @return FieldVisitor
     */
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
    {
        if (name.equals(enhancer.getNamer().getSerialVersionUidFieldName()))
        {
            // Has serialVersionUID field for use in serialisation
            hasSerialVersionUID = true;
        }
        else if (name.equals(enhancer.getNamer().getDetachedStateFieldName()))
        {
            // Has xxxDetachedState field
            hasDetachedState = true;
        }
        return super.visitField(access, name, desc, signature, value);
    }

    /**
     * Method called when a method of the class is visited.
     * @param access Access for the method
     * @param name Name of the method
     * @param desc Descriptor
     * @param signature Signature
     * @param exceptions Exceptions that this method is declared to throw
     * @return Visitor to visit this (or null if not wanting to visit it)
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        if (name.equals("<init>") && desc != null && desc.equals("()V"))
        {
            // Check for default constructor
            hasDefaultConstructor = true;
        }
        if (name.equals("writeObject") && desc != null && desc.equals("(Ljava/io/ObjectOutputStream;)V"))
        {
            // Has writeObject() for use in serialisation
            hasWriteObject = true;
        }
        if (name.equals("<clinit>") && desc != null && desc.equals("()V"))
        {
            hasStaticInitialisation = true;
        }

        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null)
        {
            return null;
        }

        if (name.equals("jdoPreClear") || name.equals("jdoPostLoad"))
        {
            // jdoPreClear/jdoPostLoad should not be enhanced (JDO2 spec [10.1, 10.3]
            return mv;
        }
        else if (name.equals("readObject") &&
            (desc.equals("(Ljava/io/ObjectOutputStream;)V") || desc.equals("(Ljava/io/ObjectInputStream;)V")))
        {
            // readObject(ObjectInputStream), readObject(ObjectOutputStream) should not be enhanced (JDO2 spec [21.6])
            return mv;
        }

        String propGetterName = ClassUtils.getFieldNameForJavaBeanGetter(name);
        String propSetterName = ClassUtils.getFieldNameForJavaBeanSetter(name);
        if (propGetterName != null)
        {
            AbstractMemberMetaData mmd = enhancer.getClassMetaData().getMetaDataForMember(propGetterName);
            if (mmd != null && mmd instanceof PropertyMetaData && mmd.getPersistenceModifier() != FieldPersistenceModifier.NONE)
            {
                // Property getter method "getXXX" - generated jdoGetXXX
                return new EnhancerPropertyGetterAdapter(mv, enhancer, name, desc, mmd, cv);
            }
        }
        else if (propSetterName != null)
        {
            AbstractMemberMetaData mmd = enhancer.getClassMetaData().getMetaDataForMember(propSetterName);
            if (mmd != null && mmd instanceof PropertyMetaData && mmd.getPersistenceModifier() != FieldPersistenceModifier.NONE)
            {
                // Property setter method "setXXX" - generates jdoSetXXX
                return new EnhancerPropertySetterAdapter(mv, enhancer, name, desc, mmd, cv);
            }
        }

        // normal method, so just enhance it
        return new EnhancerMethodAdapter(mv, enhancer, name, desc);
    }

    /**
     * Method called at the end of the class.
     */
    public void visitEnd()
    {
        AbstractClassMetaData cmd = enhancer.getClassMetaData();
        if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
        {
            // Add any new fields
            List fields = enhancer.getFieldsList();
            Iterator fieldsIter = fields.iterator();
            while (fieldsIter.hasNext())
            {
                ClassField field = (ClassField)fieldsIter.next();
                if (field.getName().equals(enhancer.getNamer().getDetachedStateFieldName()) && hasDetachedState)
                {
                    // No need to add this field since exists
                    continue;
                }

                if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
                {
                    DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.AddField", ((Class)field.getType()).getName() + " " + field.getName()));
                }
                cv.visitField(field.getAccess(), field.getName(), Type.getDescriptor((Class)field.getType()), null, null);
            }

            if (!hasStaticInitialisation)
            {
                // Add a static initialisation block for the class since nothing added yet
                InitClass method = InitClass.getInstance(enhancer);
                method.initialise(cv);
                method.execute();
                method.close();
            }

            if (!hasDefaultConstructor && enhancer.hasOption(ClassEnhancer.OPTION_GENERATE_DEFAULT_CONSTRUCTOR))
            {
                // Add a default constructor
                DefaultConstructor ctr = DefaultConstructor.getInstance(enhancer);
                ctr.initialise(cv);
                ctr.execute();
                ctr.close();
            }

            // Add any new methods
            List methods = enhancer.getMethodsList();
            Iterator<ClassMethod> methodsIter = methods.iterator();
            while (methodsIter.hasNext())
            {
                ClassMethod method = methodsIter.next();
                method.initialise(cv);
                method.execute();
                method.close();
            }

            if (Serializable.class.isAssignableFrom(enhancer.getClassBeingEnhanced()))
            {
                // Class is Serializable
                if (!hasSerialVersionUID)
                {
                    // Needs "serialVersionUID" field
                    Long uid = null;
                    try
                    {
                        uid = (Long) AccessController.doPrivileged(new PrivilegedAction()
                        {
                            public Object run()
                            {
                                return Long.valueOf(ObjectStreamClass.lookup(enhancer.getClassBeingEnhanced()).getSerialVersionUID());
                            }
                        });
                    }
                    catch (Throwable e)
                    {
                        DataNucleusEnhancer.LOGGER.warn(StringUtils.getStringFromStackTrace(e));
                    }
                    ClassField cf = new ClassField(enhancer, enhancer.getNamer().getSerialVersionUidFieldName(),
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, long.class, uid);
                    if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
                    {
                        DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.AddField", ((Class)cf.getType()).getName() + " " + cf.getName()));
                    }
                    cv.visitField(cf.getAccess(), cf.getName(), Type.getDescriptor((Class)cf.getType()), null, cf.getInitialValue());
                }
                if (!hasWriteObject)
                {
                    ClassMethod method = WriteObject.getInstance(enhancer);
                    method.initialise(cv);
                    method.execute();
                    method.close();
                }
            }

            // Add jdoGetXXX, jdoSetXXX for each of the (managed) fields/properties
            AbstractMemberMetaData[] fmds = cmd.getManagedMembers();
            for (int i = 0; i < fmds.length; i++)
            {
                if (fmds[i].getPersistenceModifier() == FieldPersistenceModifier.NONE)
                {
                    // Field/Property is not persistent so ignore
                    continue;
                }

                byte jdoFlag = fmds[i].getPersistenceFlags();
                ClassMethod getMethod = null;
                ClassMethod setMethod = null;
                if (fmds[i] instanceof PropertyMetaData)
                {
                    // jdoGetXXX, jdoSetXXX for property are generated when processing existing getXXX, setXXX methods
                }
                else
                {
                    // Generate jdoGetXXX, jdoSetXXX for field
                    if ((jdoFlag & Persistable.MEDIATE_READ) == Persistable.MEDIATE_READ)
                    {
                        getMethod = new GetViaMediate(enhancer, fmds[i]);
                    }
                    else if ((jdoFlag & Persistable.CHECK_READ) == Persistable.CHECK_READ)
                    {
                        getMethod = new GetViaCheck(enhancer, fmds[i]);
                    }
                    else
                    {
                        getMethod = new GetNormal(enhancer, fmds[i]);
                    }

                    if ((jdoFlag & Persistable.MEDIATE_WRITE) == Persistable.MEDIATE_WRITE)
                    {
                        setMethod = new SetViaMediate(enhancer, fmds[i]);
                    }
                    else if ((jdoFlag & Persistable.CHECK_WRITE) == Persistable.CHECK_WRITE)
                    {
                        setMethod = new SetViaCheck(enhancer, fmds[i]);
                    }
                    else
                    {
                        setMethod = new SetNormal(enhancer, fmds[i]);
                    }
                }

                if (getMethod != null)
                {
                    getMethod.initialise(cv);
                    getMethod.execute();
                    getMethod.close();
                }
                if (setMethod != null)
                {
                    setMethod.initialise(cv);
                    setMethod.execute();
                    setMethod.close();
                }
            }
        }
        cv.visitEnd();
    }
}
