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

import java.io.File;
import java.io.FileOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassLoaderResolverImpl;
import org.datanucleus.asm.ClassWriter;
import org.datanucleus.asm.Label;
import org.datanucleus.asm.MethodVisitor;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.ClassEnhancerImpl;
import org.datanucleus.enhancer.EnhancerPropertyGetterAdapter;
import org.datanucleus.enhancer.EnhancerPropertySetterAdapter;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.InterfaceMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.util.ClassUtils;

/**
 * Implementation generator using ASM bytecode manipulation library.
 */
public class ImplementationGenerator
{
    protected final MetaDataManager metaDataMgr;

    /** Meta data for the abstract-class/interface **/
    protected final AbstractClassMetaData inputCmd;

    /** Class name of the implementation. **/
    protected final String className;

    /** Fully-qualified class name (including package) of the implementation. **/
    protected final String fullClassName;

    /** Class name for the superclass. **/
    protected String fullSuperclassName = "java.lang.Object";

    /** bytes for the implementation class. **/
    protected byte[] bytes;

    /** Writer for the implementation class. */
    ClassWriter writer;

    /** ASM class name of the implementation class. */
    String asmClassName;

    /** ASM type descriptor for the implementation class. */
    String asmTypeDescriptor;

    EnhancementNamer namer = JDOEnhancementNamer.getInstance();

    /**
     * Constructor for an implementation of a persistent interface.
     * @param interfaceMetaData MetaData for the persistent interface
     * @param implClassName Name of the implementation class to generate (omitting packages)
     * @param mmgr MetaData manager
     */
    public ImplementationGenerator(InterfaceMetaData interfaceMetaData, String implClassName, MetaDataManager mmgr)
    {
        this.className = implClassName;
        this.fullClassName = interfaceMetaData.getPackageName() + '.' + className;
        this.inputCmd = interfaceMetaData;
        this.metaDataMgr = mmgr;

        asmClassName = fullClassName.replace('.', '/');
        asmTypeDescriptor = "L" + asmClassName + ";";

        List<String> interfaces = new ArrayList<String>();
        InterfaceMetaData imd = interfaceMetaData;
        do
        {
            String intfTypeName = imd.getFullClassName().replace('.', '/');
            interfaces.add(intfTypeName);
            imd = (InterfaceMetaData) imd.getSuperAbstractClassMetaData();
        }
        while (imd != null);

        // Start the class
        writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(EnhanceUtils.getAsmVersionForJRE(), Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, fullClassName.replace('.', '/'),
            null, fullSuperclassName.replace('.', '/'), interfaces.toArray(new String[interfaces.size()]));

        // Create fields, default ctor, and methods
        createPropertyFields();
        createDefaultConstructor();
        createPropertyMethods();

        // End the class
        writer.visitEnd();
        bytes = writer.toByteArray();
    }

    /**
     * Constructor for an implementation of an abstract class.
     * @param cmd MetaData for the abstract class
     * @param implClassName Name of the implementation class to generate (omitting packages)
     * @param mmgr MetaData manager
     */
    public ImplementationGenerator(ClassMetaData cmd, String implClassName, MetaDataManager mmgr)
    {
        this.className = implClassName;
        this.fullClassName = cmd.getPackageName() + '.' + className;
        this.inputCmd = cmd;
        this.metaDataMgr = mmgr;

        asmClassName = fullClassName.replace('.', '/');
        asmTypeDescriptor = "L" + asmClassName + ";";
        fullSuperclassName = cmd.getFullClassName();

        // Start the class
        writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(EnhanceUtils.getAsmVersionForJRE(), Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, fullClassName.replace('.', '/'),
            null, fullSuperclassName.replace('.', '/'), null);

        // Create fields, default ctor, and methods
        createPropertyFields();
        createDefaultConstructor();
        createPropertyMethods();

        // End the class
        writer.visitEnd();
        bytes = writer.toByteArray();
    }

    /**
     * Accessor for the byte representation of the generated class.
     * @return the byte representation of the class
     */
    public byte[] getBytes()
    {
        return bytes;
    }

    /**
     * Creates fields for the properties of this class and super classes.
     */
    protected void createPropertyFields()
    {
        AbstractClassMetaData acmd = inputCmd;
        do
        {
            createPropertyFields(acmd);
            acmd = acmd.getSuperAbstractClassMetaData();
        }
        while (acmd != null);
    }

    /**
     * Create getters and setters methods for this class and super classes
     */
    protected void createPropertyMethods()
    {
        AbstractClassMetaData acmd = inputCmd;
        do
        {
            createPropertyMethods(acmd);
            acmd = acmd.getSuperAbstractClassMetaData();
        }
        while (acmd != null);
    }  

    /**
     * Create getters and setters methods.
     * @param acmd AbstractClassMetaData
     */
    protected void createPropertyMethods(AbstractClassMetaData acmd)
    {
        if (acmd == null)
        {
            return;
        }

        AbstractMemberMetaData[] memberMetaData = acmd.getManagedMembers();
        for (int i=0; i<memberMetaData.length; i++)
        {
            createGetter(memberMetaData[i]);
            createSetter(memberMetaData[i]);
        }
    }

    /**
     * Convenience method to dump the generated class to the specified file.
     * @param filename Name of the file to dump to
     */
    public void dumpToFile(String filename)
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(new File(filename));
            out.write(getBytes());
            DataNucleusEnhancer.LOGGER.info("Generated class for " + fullClassName + " dumped to " + filename);
        }
        catch (Exception e)
        {
            DataNucleusEnhancer.LOGGER.error("Failure to dump generated class to file", e);
        }
        finally
        {
            try
            {
                out.close();
                out = null;
            }
            catch (Exception ignore)
            {
                // ignore exception in closing the stream
            }
        }
    }

    /**
     * Enhance the implementation of the class/interface.
     * @param clr ClassLoader resolver
     */
    @SuppressWarnings("unchecked")
    public void enhance(final ClassLoaderResolver clr)
    {
        // define the generated class in the classloader so we populate the metadata
        final EnhancerClassLoader loader = new EnhancerClassLoader();
        loader.defineClass(fullClassName, getBytes(), clr);

        // Create MetaData for implementation of interface
        final ClassLoaderResolver genclr = new ClassLoaderResolverImpl(loader);
        final ClassMetaData implementationCmd;
        if (inputCmd instanceof InterfaceMetaData)
        {
            implementationCmd = new ClassMetaData((InterfaceMetaData)inputCmd, className, true);
        }
        else
        {
            implementationCmd = new ClassMetaData((ClassMetaData)inputCmd, className);
        }

        // Do as PrivilegedAction since populate()/initialise() use reflection to get additional fields
        AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                implementationCmd.populate(genclr, null, metaDataMgr);
                implementationCmd.initialise(genclr, metaDataMgr);
                return null;
            }
        });

        // enhance the class and update the byte definition
        ClassEnhancer gen = new ClassEnhancerImpl(implementationCmd, genclr, metaDataMgr, JDOEnhancementNamer.getInstance(), getBytes());
        gen.enhance();
        bytes = gen.getClassBytes();
    }

    /**
     * Create the fields for the implementation.
     * @param acmd MetaData for the class/interface
     */
    protected void createPropertyFields(AbstractClassMetaData acmd)
    {
        if (acmd == null)
        {
            return;
        }

        AbstractMemberMetaData[] propertyMetaData = acmd.getManagedMembers();
        for (int i=0; i<propertyMetaData.length; i++)
        {
            writer.visitField(Opcodes.ACC_PRIVATE, propertyMetaData[i].getName(), Type.getDescriptor(propertyMetaData[i].getType()), null, null).visitEnd();
        }
    }

    /**
     * Create a default constructor, assuming that there is no persistent superclass.
     */
    protected void createDefaultConstructor()
    {
        MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        visitor.visitCode();
        Label l0 = new Label();
        visitor.visitLabel(l0);
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, fullSuperclassName.replace('.', '/'), "<init>", "()V");
        visitor.visitInsn(Opcodes.RETURN);
        Label l1 = new Label();
        visitor.visitLabel(l1);
        visitor.visitLocalVariable("this", asmTypeDescriptor, null, l0, l1, 0);
        visitor.visitMaxs(1, 1);
        visitor.visitEnd();
    }

    /**
     * Create a getter method for a /property.
     * @param mmd MetaData for the property
     */
    protected void createGetter(AbstractMemberMetaData mmd)
    {
        boolean isBoolean = mmd.getTypeName().equals("boolean");
        String getterName = ClassUtils.getJavaBeanGetterName(mmd.getName(), isBoolean);
        String jdoGetterName = namer.getGetMethodPrefixMethodName() + mmd.getName();
        if (inputCmd instanceof InterfaceMetaData)
        {
            // Interface so generate getXXX
            String fieldDesc = Type.getDescriptor(mmd.getType());
            MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, getterName, 
                "()" + fieldDesc, null, null);
            visitor.visitCode();
            Label l0 = new Label();
            visitor.visitLabel(l0);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, asmClassName, mmd.getName(), fieldDesc);
            EnhanceUtils.addReturnForType(visitor, mmd.getType());
            Label l1 = new Label();
            visitor.visitLabel(l1);
            visitor.visitLocalVariable("this", asmTypeDescriptor, null, l0, l1, 0);
            visitor.visitMaxs(1, 1);
            visitor.visitEnd();
        }
        else
        {
            // Abstract class so generate getXXX
            String fieldDesc = Type.getDescriptor(mmd.getType());
            int getAccess = (mmd.isPublic() ? Opcodes.ACC_PUBLIC : 0) |
                (mmd.isProtected() ? Opcodes.ACC_PROTECTED : 0) |
                (mmd.isPrivate() ? Opcodes.ACC_PRIVATE : 0);
            MethodVisitor getVisitor = writer.visitMethod(getAccess, getterName, "()" + fieldDesc, null, null);
            EnhancerPropertyGetterAdapter.generateGetXXXMethod(getVisitor, mmd, asmClassName, asmTypeDescriptor, false, namer);

            // Abstract class so generate jdoGetXXX
            int access = (mmd.isPublic() ? Opcodes.ACC_PUBLIC : 0) | 
                (mmd.isProtected() ? Opcodes.ACC_PROTECTED : 0) | 
                (mmd.isPrivate() ? Opcodes.ACC_PRIVATE : 0);
            MethodVisitor visitor = writer.visitMethod(access, jdoGetterName, "()" + fieldDesc, null, null);
            visitor.visitCode();
            Label l0 = new Label();
            visitor.visitLabel(l0);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            visitor.visitFieldInsn(Opcodes.GETFIELD, asmClassName, mmd.getName(), fieldDesc);
            EnhanceUtils.addReturnForType(visitor, mmd.getType());
            Label l1 = new Label();
            visitor.visitLabel(l1);
            visitor.visitLocalVariable("this", asmTypeDescriptor, null, l0, l1, 0);
            visitor.visitMaxs(1, 1);
            visitor.visitEnd();
        }
    }

    /**
     * Create a setter method for a property.
     * @param mmd MetaData for the property
     */
    protected void createSetter(AbstractMemberMetaData mmd)
    {
        String setterName = ClassUtils.getJavaBeanSetterName(mmd.getName());
        String jdoSetterName = namer.getSetMethodPrefixMethodName() + mmd.getName();
        if (inputCmd instanceof InterfaceMetaData)
        {
            // Interface so generate setXXX
            String fieldDesc = Type.getDescriptor(mmd.getType());
            MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, setterName, "(" + fieldDesc + ")V", null, null);
            visitor.visitCode();
            Label l0 = new Label();
            visitor.visitLabel(l0);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            EnhanceUtils.addLoadForType(visitor, mmd.getType(), 1);
            visitor.visitFieldInsn(Opcodes.PUTFIELD, asmClassName, mmd.getName(), fieldDesc);
            visitor.visitInsn(Opcodes.RETURN);
            Label l2 = new Label();
            visitor.visitLabel(l2);
            visitor.visitLocalVariable("this", asmTypeDescriptor, null, l0, l2, 0);
            visitor.visitLocalVariable("val", fieldDesc, null, l0, l2, 1);
            visitor.visitMaxs(2, 2);
            visitor.visitEnd();
        }
        else
        {
            // Abstract class so generate setXXX
            String fieldDesc = Type.getDescriptor(mmd.getType());
            int setAccess = (mmd.isPublic() ? Opcodes.ACC_PUBLIC : 0) |
                (mmd.isProtected() ? Opcodes.ACC_PROTECTED : 0) |
                (mmd.isPrivate() ? Opcodes.ACC_PRIVATE : 0);
            MethodVisitor setVisitor = writer.visitMethod(setAccess, setterName, "(" + fieldDesc + ")V", null, null);
            EnhancerPropertySetterAdapter.generateSetXXXMethod(setVisitor, mmd, asmClassName, asmTypeDescriptor, namer);

            // Abstract class so generate jdoSetXXX
            int access = (mmd.isPublic() ? Opcodes.ACC_PUBLIC : 0) | 
                (mmd.isProtected() ? Opcodes.ACC_PROTECTED : 0) | 
                (mmd.isPrivate() ? Opcodes.ACC_PRIVATE : 0);
            MethodVisitor visitor = writer.visitMethod(access, jdoSetterName, "(" + fieldDesc + ")V", null, null);
            visitor.visitCode();
            Label l0 = new Label();
            visitor.visitLabel(l0);
            visitor.visitVarInsn(Opcodes.ALOAD, 0);
            EnhanceUtils.addLoadForType(visitor, mmd.getType(), 1);
            visitor.visitFieldInsn(Opcodes.PUTFIELD, asmClassName, mmd.getName(), fieldDesc);
            visitor.visitInsn(Opcodes.RETURN);
            Label l2 = new Label();
            visitor.visitLabel(l2);
            visitor.visitLocalVariable("this", asmTypeDescriptor, null, l0, l2, 0);
            visitor.visitLocalVariable("val", fieldDesc, null, l0, l2, 1);
            visitor.visitMaxs(2, 2);
            visitor.visitEnd();
        }
    }
}