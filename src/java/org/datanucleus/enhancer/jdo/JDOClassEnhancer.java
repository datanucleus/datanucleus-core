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
package org.datanucleus.enhancer.jdo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.asm.AnnotationVisitor;
import org.datanucleus.asm.Attribute;
import org.datanucleus.asm.ClassReader;
import org.datanucleus.asm.ClassVisitor;
import org.datanucleus.asm.ClassWriter;
import org.datanucleus.asm.FieldVisitor;
import org.datanucleus.asm.MethodVisitor;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.AbstractClassEnhancer;
import org.datanucleus.enhancer.ClassField;
import org.datanucleus.enhancer.DataNucleusEnhancer;
import org.datanucleus.enhancer.EnhancementNamer;
import org.datanucleus.enhancer.jdo.JDOClassAdapter;
import org.datanucleus.enhancer.jdo.JDOClassChecker;
import org.datanucleus.enhancer.jdo.method.InitFieldFlags;
import org.datanucleus.enhancer.jdo.method.InitFieldNames;
import org.datanucleus.enhancer.jdo.method.InitFieldTypes;
import org.datanucleus.enhancer.jdo.method.InitPersistenceCapableSuperclass;
import org.datanucleus.enhancer.jdo.method.JdoCopyField;
import org.datanucleus.enhancer.jdo.method.JdoCopyFields;
import org.datanucleus.enhancer.jdo.method.JdoCopyKeyFieldsFromObjectId;
import org.datanucleus.enhancer.jdo.method.JdoCopyKeyFieldsFromObjectId2;
import org.datanucleus.enhancer.jdo.method.JdoCopyKeyFieldsToObjectId;
import org.datanucleus.enhancer.jdo.method.JdoCopyKeyFieldsToObjectId2;
import org.datanucleus.enhancer.jdo.method.JdoGetInheritedFieldCount;
import org.datanucleus.enhancer.jdo.method.JdoGetManagedFieldCount;
import org.datanucleus.enhancer.jdo.method.JdoGetObjectId;
import org.datanucleus.enhancer.jdo.method.JdoGetPersistenceManager;
import org.datanucleus.enhancer.jdo.method.JdoGetTransactionalObjectId;
import org.datanucleus.enhancer.jdo.method.JdoGetVersion;
import org.datanucleus.enhancer.jdo.method.JdoIsDeleted;
import org.datanucleus.enhancer.jdo.method.JdoIsDetached;
import org.datanucleus.enhancer.jdo.method.JdoIsDirty;
import org.datanucleus.enhancer.jdo.method.JdoIsNew;
import org.datanucleus.enhancer.jdo.method.JdoIsPersistent;
import org.datanucleus.enhancer.jdo.method.JdoIsTransactional;
import org.datanucleus.enhancer.jdo.method.JdoMakeDirty;
import org.datanucleus.enhancer.jdo.method.JdoNewInstance1;
import org.datanucleus.enhancer.jdo.method.JdoNewInstance2;
import org.datanucleus.enhancer.jdo.method.JdoNewObjectIdInstance1;
import org.datanucleus.enhancer.jdo.method.JdoNewObjectIdInstance2;
import org.datanucleus.enhancer.jdo.method.JdoPreSerialize;
import org.datanucleus.enhancer.jdo.method.JdoProvideField;
import org.datanucleus.enhancer.jdo.method.JdoProvideFields;
import org.datanucleus.enhancer.jdo.method.JdoReplaceDetachedState;
import org.datanucleus.enhancer.jdo.method.JdoReplaceField;
import org.datanucleus.enhancer.jdo.method.JdoReplaceFields;
import org.datanucleus.enhancer.jdo.method.JdoReplaceFlags;
import org.datanucleus.enhancer.jdo.method.JdoReplaceStateManager;
import org.datanucleus.enhancer.jdo.method.JdoSuperClone;
import org.datanucleus.enhancer.jdo.method.LoadClass;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.InvalidMetaDataException;
import org.datanucleus.metadata.MetaDataManager;

/**
 * Class enhancer using ASM (http://asm.objectweb.org).
 * Assumes that the version of ASM is 3.0 or above.
 * ASM operates using a SAXParser-like "visitor-pattern". We utilise this as follows :-
 * <ul>
 * <li><b>enhance</b> : start with a ClassReader for the class to be enhanced, and create a JdoClassAdapter
 * (attached to a ClassWriter) that will perform the modifications, and use that as a visitor for the reader
 * so that the reader sends its events to the adapter. Within the JdoClassAdapter we also make use of a
 * JdoMethodAdapter to update individual methods</li>
 * <li><b>check</b> : take a ClassReader, and create a JdoClassChecker that performs the checks. We then set
 * the checker as a visitor for the reader so that the reader sends its events to the checker.</li>
 * </ul>
 */
public class JDOClassEnhancer extends AbstractClassEnhancer
{
    /** Resource name of the input class (only when the class exists in a class file). */
    protected String inputResourceName;

    /** Bytes of the input class (only when enhancing generated classes with no class file). */
    protected byte[] inputBytes;

    /** Class that is being enhanced. */
    protected final Class cls;

    /** Bytes of the class (after enhancing). */
    protected byte[] classBytes = null;

    /** Bytes for any auto-generated PK class (if generated during enhancement). */
    protected byte[] pkClassBytes = null;

    /** ASM Class name for this class (replace . with /). */
    protected String asmClassName = null;

    /** Class descriptor for this class. */
    protected String classDescriptor = null;

    protected EnhancementNamer namer = null;

    /**
     * Constructor for an enhancer for the class. The class is assumed to be in the CLASSPATH.
     * @param cmd MetaData for the class to be enhanced
     * @param clr ClassLoader resolver
     * @param mmgr MetaData manager
     */
    public JDOClassEnhancer(ClassMetaData cmd, ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        super(cmd, clr, mmgr);

        namer = JDOEnhancementNamer.getInstance();
        cls = clr.classForName(cmd.getFullClassName());
        asmClassName = cmd.getFullClassName().replace('.', '/');
        classDescriptor = Type.getDescriptor(cls);
        inputResourceName = "/" + className.replace('.','/') + ".class";
    }

    /**
     * Constructor for an enhancer to enhance a class defined by the provided bytes.
     * @param cmd MetaData for the class to be enhanced
     * @param clr ClassLoader resolver
     * @param mmgr MetaData manager
     * @param classBytes Bytes of the class to enhance
     */
    public JDOClassEnhancer(ClassMetaData cmd, ClassLoaderResolver clr, MetaDataManager mmgr, byte[] classBytes)
    {
        super(cmd, clr, mmgr);

        namer = JDOEnhancementNamer.getInstance();
        cls = clr.classForName(cmd.getFullClassName());
        asmClassName = cmd.getFullClassName().replace('.', '/');
        classDescriptor = Type.getDescriptor(cls);
        inputBytes = classBytes;
    }

    public void setNamer(EnhancementNamer namer)
    {
        this.namer = namer;
    }

    /**
     * Convenience accessor for the class name that is stored in a particular class.
     * @param filename Name of the file
     * @return The class name
     */
    public static String getClassNameForFileName(String filename)
    {
        MyClassVisitor vis = new MyClassVisitor();
        try
        {
            new ClassReader(new FileInputStream(filename)).accept(vis, 0);
            return vis.getClassName();
        }
        catch (IOException ioe)
        {
            return null;
        }
    }

    /** Convenience class to look up the class name for a file. */
    public static class MyClassVisitor extends ClassVisitor
    {
        public MyClassVisitor()
        {
            super(ASM_API_VERSION);
        }

        String className = null;
        public String getClassName()
        {
            return className;
        }

        public void visitInnerClass(String name, String outerName, String innerName, int access) { }
        public void visit(int version, int access, String name, String sig, String supername, String[] intfs)
        {
            className = name.replace('/', '.');
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible)
        {
            return null;
        }
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
        {
            return null;
        }
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] excpts)
        {
            return null;
        }
        public void visitAttribute(Attribute attr) { }
        public void visitOuterClass(String owner, String name, String desc) { }
        public void visitSource(String source, String debug) { }
        public void visitEnd() { }
    }

    /**
     * Accessor for the class being enhanced.
     * @return Class being enhanced
     */
    public Class getClassBeingEnhanced()
    {
        return cls;
    }

    /**
     * Accessor for the ASM class name
     * @return ASM class name
     */
    public String getASMClassName()
    {
        return asmClassName;
    }

    /**
     * Accessor for the class descriptor for the class being enhanced
     * @return class descriptor
     */
    public String getClassDescriptor()
    {
        return classDescriptor;
    }

    /**
     * Method to initialise the list of methods to add.
     */
    protected void initialiseMethodsList()
    {
        if (cmd.getPersistenceCapableSuperclass() == null)
        {
            // Root persistent class methods
            methodsToAdd.add(JdoCopyKeyFieldsFromObjectId.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsFromObjectId2.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsToObjectId.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsToObjectId2.getInstance(this));
            methodsToAdd.add(JdoGetObjectId.getInstance(this));
            methodsToAdd.add(JdoGetVersion.getInstance(this));
            methodsToAdd.add(JdoPreSerialize.getInstance(this));
            methodsToAdd.add(JdoGetPersistenceManager.getInstance(this));
            methodsToAdd.add(JdoGetTransactionalObjectId.getInstance(this));
            methodsToAdd.add(JdoIsDeleted.getInstance(this));
            methodsToAdd.add(JdoIsDirty.getInstance(this));
            methodsToAdd.add(JdoIsNew.getInstance(this));
            methodsToAdd.add(JdoIsPersistent.getInstance(this));
            methodsToAdd.add(JdoIsTransactional.getInstance(this));
            methodsToAdd.add(JdoMakeDirty.getInstance(this));
            methodsToAdd.add(JdoNewObjectIdInstance1.getInstance(this));
            methodsToAdd.add(JdoNewObjectIdInstance2.getInstance(this));
            methodsToAdd.add(JdoProvideFields.getInstance(this));
            methodsToAdd.add(JdoReplaceFields.getInstance(this));
            methodsToAdd.add(JdoReplaceFlags.getInstance(this));
            methodsToAdd.add(JdoReplaceStateManager.getInstance(this));
        }
        if (cmd.getPersistenceCapableSuperclass() != null && cmd.isRootInstantiableClass())
        {
            // This class is not the root in the inheritance tree, but is the root in terms of being instantiable
            // hence it owns the "identity", so we need real implementations of the identity enhancement methods
            methodsToAdd.add(JdoCopyKeyFieldsFromObjectId.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsFromObjectId2.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsToObjectId.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsToObjectId2.getInstance(this));
            methodsToAdd.add(JdoNewObjectIdInstance1.getInstance(this));
            methodsToAdd.add(JdoNewObjectIdInstance2.getInstance(this));
        }

        if (requiresDetachable())
        {
            methodsToAdd.add(JdoReplaceDetachedState.getInstance(this));
        }
        if (cmd.isDetachable() && cmd.getPersistenceCapableSuperclass() != null)
        {
            methodsToAdd.add(JdoMakeDirty.getInstance(this));
        }

        methodsToAdd.add(JdoIsDetached.getInstance(this));
        methodsToAdd.add(JdoNewInstance1.getInstance(this));
        methodsToAdd.add(JdoNewInstance2.getInstance(this));
        methodsToAdd.add(JdoReplaceField.getInstance(this));
        methodsToAdd.add(JdoProvideField.getInstance(this));
        methodsToAdd.add(JdoCopyField.getInstance(this));
        methodsToAdd.add(JdoCopyFields.getInstance(this));
        methodsToAdd.add(InitFieldNames.getInstance(this));
        methodsToAdd.add(InitFieldTypes.getInstance(this));
        methodsToAdd.add(InitFieldFlags.getInstance(this));
        methodsToAdd.add(JdoGetInheritedFieldCount.getInstance(this));
        methodsToAdd.add(JdoGetManagedFieldCount.getInstance(this));
        methodsToAdd.add(InitPersistenceCapableSuperclass.getInstance(this));
        methodsToAdd.add(LoadClass.getInstance(this));
        methodsToAdd.add(JdoSuperClone.getInstance(this));
    }

    /**
     * Method to initialise the list of fields to add.
     */
    protected void initialiseFieldsList()
    {
        if (cmd.getPersistenceCapableSuperclass() == null)
        {
            // Root persistent class fields
            fieldsToAdd.add(new ClassField(this, namer.getStateManagerFieldName(), 
                Opcodes.ACC_PROTECTED | Opcodes.ACC_TRANSIENT, namer.getStateManagerClass()));
            fieldsToAdd.add(new ClassField(this, namer.getFlagsFieldName(),
                Opcodes.ACC_PROTECTED | Opcodes.ACC_TRANSIENT, byte.class));
        }

        if (requiresDetachable())
        {
            // Detachable fields
            fieldsToAdd.add(new ClassField(this, namer.getDetachedStateFieldName(), 
                Opcodes.ACC_PROTECTED, Object[].class));
        }

        fieldsToAdd.add(new ClassField(this, namer.getFieldFlagsFieldName(), 
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, byte[].class));
        fieldsToAdd.add(new ClassField(this, namer.getPersistableSuperclassFieldName(),
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Class.class));
        fieldsToAdd.add(new ClassField(this, namer.getFieldTypesFieldName(),
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Class[].class));
        fieldsToAdd.add(new ClassField(this, namer.getFieldNamesFieldName(),
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, String[].class));
        fieldsToAdd.add(new ClassField(this, namer.getInheritedFieldCountFieldName(),
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, int.class));
    }

    /**
     * Method to enhance a classes definition.
     * @return Whether it was enhanced with no errors
     */
    public boolean enhance()
    {
        if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE &&
            cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_AWARE)
        {
            return false;
        }

        initialise();

        if (checkClassIsEnhanced(false))
        {
            // Already enhanced
            DataNucleusEnhancer.LOGGER.info(LOCALISER.msg("Enhancer.ClassIsAlreadyEnhanced", className));
            return true;
        }

        try
        {
            // Check for generation of PK
            if (cmd.getIdentityType() == IdentityType.APPLICATION &&
                cmd.getObjectidClass() == null && cmd.getNoOfPrimaryKeyMembers() > 1)
            {
                if (hasOption(OPTION_GENERATE_PK))
                {
                    String pkClassName = cmd.getFullClassName() + AbstractClassMetaData.GENERATED_PK_SUFFIX;
                    if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
                    {
                        DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.GeneratePrimaryKey", cmd.getFullClassName(), pkClassName));
                    }
                    cmd.setObjectIdClass(pkClassName);
                    PrimaryKeyGenerator pkGen = new PrimaryKeyGenerator(cmd, this);
                    pkClassBytes = pkGen.generate();
                }
                else
                {
                    // Throw exception for invalid metadata
                    throw new InvalidMetaDataException(LOCALISER, "044065", cmd.getFullClassName(),
                        cmd.getNoOfPrimaryKeyMembers());
                }
            }

            // Create an adapter using a writer
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            JDOClassAdapter cv = new JDOClassAdapter(cw, this);
            ClassReader cr = null;
            InputStream classReaderInputStream = null;
            try
            {
                // Create a reader for the class and tell it to visit the adapter, performing the changes
                if (inputBytes != null)
                {
                    cr = new ClassReader(inputBytes);
                }
                else
                {
                    classReaderInputStream = clr.getResource(inputResourceName, null).openStream();
                    cr = new ClassReader(classReaderInputStream);
                }
                cr.accept(cv, 0);

                // Save the bytes
                classBytes = cw.toByteArray();
            }
            finally
            {
                if (classReaderInputStream != null)
                {
                    classReaderInputStream.close();
                }
            }
        }
        catch (Exception e)
        {
            DataNucleusEnhancer.LOGGER.error("Error thrown enhancing with ASMClassEnhancer", e);
            return false;
        }

        update = true;
        return true;
    }

    /**
     * Accessor for the class bytes.
     * Only has relevance to be called after enhance().
     * @return The class bytes
     */
    public byte[] getClassBytes()
    {
        return classBytes;
    }

    /**
     * Accessor for the primary-key class bytes (if generating a PK).
     * Only has relevance to be called after enhance().
     * @return The primary-key class bytes
     */
    public byte[] getPrimaryKeyClassBytes()
    {
        return pkClassBytes;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#checkEnhanced()
     */
    public boolean validate()
    {
        if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE &&
            cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_AWARE)
        {
            return false;
        }

        initialise();

        return checkClassIsEnhanced(true);
    }

    /**
     * Convenience method to return if a class is enhanced.
     * @param logErrors Whether to log any errors (missing methods etc) as errors (otherwise info/debug)
     * @return Whether the class is enhanced
     */
    protected boolean checkClassIsEnhanced(boolean logErrors)
    {
        try
        {
            // Create an adapter using a writer
            JDOClassChecker checker = new JDOClassChecker(this, logErrors);

            InputStream classReaderInputStream = null;
            try
            {
                // Create a reader for the class and visit it using the checker
                ClassReader cr = null;
                if (inputBytes != null)
                {
                    cr = new ClassReader(inputBytes);
                }
                else
                {
                    classReaderInputStream = clr.getResource(inputResourceName,null).openStream(); 
                    cr = new ClassReader(classReaderInputStream);
                }
                cr.accept(checker, 0); // [ASM Note : In 2.2 this should be "cr.accept(checker, false);"]
            }
            finally
            {
                if (classReaderInputStream != null)
                {
                    classReaderInputStream.close();
                }
            }

            return checker.isEnhanced();
        }
        catch (Exception e)
        {
            DataNucleusEnhancer.LOGGER.error("Error thrown enhancing with ASMClassEnhancer", e);
        }
        return false;
    }

    public EnhancementNamer getNamer()
    {
        return namer;
    }
}
