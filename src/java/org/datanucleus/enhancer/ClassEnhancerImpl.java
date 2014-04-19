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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.asm.AnnotationVisitor;
import org.datanucleus.asm.Attribute;
import org.datanucleus.asm.ClassReader;
import org.datanucleus.asm.ClassVisitor;
import org.datanucleus.asm.ClassWriter;
import org.datanucleus.asm.FieldVisitor;
import org.datanucleus.asm.MethodVisitor;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.EnhancerClassAdapter;
import org.datanucleus.enhancer.EnhancerClassChecker;
import org.datanucleus.enhancer.methods.InitFieldFlags;
import org.datanucleus.enhancer.methods.InitFieldNames;
import org.datanucleus.enhancer.methods.InitFieldTypes;
import org.datanucleus.enhancer.methods.InitPersistenceCapableSuperclass;
import org.datanucleus.enhancer.methods.CopyField;
import org.datanucleus.enhancer.methods.CopyFields;
import org.datanucleus.enhancer.methods.CopyKeyFieldsFromObjectId;
import org.datanucleus.enhancer.methods.CopyKeyFieldsFromObjectId2;
import org.datanucleus.enhancer.methods.CopyKeyFieldsToObjectId;
import org.datanucleus.enhancer.methods.CopyKeyFieldsToObjectId2;
import org.datanucleus.enhancer.methods.GetInheritedFieldCount;
import org.datanucleus.enhancer.methods.GetManagedFieldCount;
import org.datanucleus.enhancer.methods.GetObjectId;
import org.datanucleus.enhancer.methods.GetPersistenceManager;
import org.datanucleus.enhancer.methods.GetTransactionalObjectId;
import org.datanucleus.enhancer.methods.GetVersion;
import org.datanucleus.enhancer.methods.IsDeleted;
import org.datanucleus.enhancer.methods.IsDetached;
import org.datanucleus.enhancer.methods.IsDirty;
import org.datanucleus.enhancer.methods.IsNew;
import org.datanucleus.enhancer.methods.IsPersistent;
import org.datanucleus.enhancer.methods.IsTransactional;
import org.datanucleus.enhancer.methods.MakeDirty;
import org.datanucleus.enhancer.methods.NewInstance1;
import org.datanucleus.enhancer.methods.NewInstance2;
import org.datanucleus.enhancer.methods.NewObjectIdInstance1;
import org.datanucleus.enhancer.methods.NewObjectIdInstance2;
import org.datanucleus.enhancer.methods.PreSerialize;
import org.datanucleus.enhancer.methods.ProvideField;
import org.datanucleus.enhancer.methods.ProvideFields;
import org.datanucleus.enhancer.methods.ReplaceDetachedState;
import org.datanucleus.enhancer.methods.ReplaceField;
import org.datanucleus.enhancer.methods.ReplaceFields;
import org.datanucleus.enhancer.methods.ReplaceFlags;
import org.datanucleus.enhancer.methods.ReplaceStateManager;
import org.datanucleus.enhancer.methods.SuperClone;
import org.datanucleus.enhancer.methods.LoadClass;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.InvalidMetaDataException;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Class enhancer using ASM (see http://asm.objectweb.org but included in DataNucleus core repackaged).
 * ASM operates using a SAXParser-like "visitor-pattern". We utilise this as follows :-
 * <ul>
 * <li><b>enhance</b> : start with a ClassReader for the class to be enhanced, and create a EnhancerClassAdapter
 * (attached to a ClassWriter) that will perform the modifications, and use that as a visitor for the reader
 * so that the reader sends its events to the adapter. Within the EnhancerClassAdapter we also make use of a
 * EnhancerMethodAdapter to update individual methods</li>
 * <li><b>check</b> : take a ClassReader, and create a EnhancerClassChecker that performs the checks. We then set
 * the checker as a visitor for the reader so that the reader sends its events to the checker.</li>
 * </ul>
 */
public class ClassEnhancerImpl implements ClassEnhancer
{
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Class Loader Resolver to use for any loading issues. */
    protected final ClassLoaderResolver clr;

    /** MetaData Manager to use. */
    protected final MetaDataManager metaDataMgr;

    /** MetaData for the class being enhanced. */
    protected final ClassMetaData cmd;

    /** Class name of the class being enhanced */
    public final String className;

    /** Flag specifying if the class needs updating. */
    protected boolean update = false;

    /** List of fields to be added to the class. */
    protected List<ClassField> fieldsToAdd = new ArrayList<ClassField>();

    /** List of methods to be added to the class. */
    protected List<ClassMethod> methodsToAdd = new ArrayList<ClassMethod>();

    /** Flag for whether we are initialised. */
    protected boolean initialised = false;

    /** Options for enhancement. */
    protected Collection<String> options = new HashSet<String>();

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
     * @param namer The namer
     */
    public ClassEnhancerImpl(ClassMetaData cmd, ClassLoaderResolver clr, MetaDataManager mmgr, EnhancementNamer namer)
    {
        this.clr = clr;
        this.cmd = cmd;
        this.className = cmd.getFullClassName();
        this.metaDataMgr = mmgr;
        this.namer = namer;
        this.cls = clr.classForName(cmd.getFullClassName());
        this.asmClassName = cmd.getFullClassName().replace('.', '/');
        this.classDescriptor = Type.getDescriptor(cls);
        this.inputResourceName = "/" + className.replace('.','/') + ".class";
    }

    /**
     * Constructor for an enhancer to enhance a class defined by the provided bytes.
     * @param cmd MetaData for the class to be enhanced
     * @param clr ClassLoader resolver
     * @param mmgr MetaData manager
     * @param namer The namer
     * @param classBytes Bytes of the class to enhance
     */
    public ClassEnhancerImpl(ClassMetaData cmd, ClassLoaderResolver clr, MetaDataManager mmgr, EnhancementNamer namer, byte[] classBytes)
    {
        this.clr = clr;
        this.cmd = cmd;
        this.className = cmd.getFullClassName();
        this.metaDataMgr = mmgr;
        this.namer = namer;
        this.cls = clr.classForName(cmd.getFullClassName());
        this.asmClassName = cmd.getFullClassName().replace('.', '/');
        this.classDescriptor = Type.getDescriptor(cls);
        this.inputBytes = classBytes;
    }

    /**
     * Initialisation of the information for enhancing this class.
     */
    protected void initialise()
    {
        if (initialised)
        {
            return;
        }

        initialiseFieldsList();
        initialiseMethodsList();
        initialised = true;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getClassName()
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * Accessor for the methods required.
     * @return List of methods required for enhancement
     */
    public List<ClassMethod> getMethodsList()
    {
        return methodsToAdd;
    }

    /**
     * Accessor for the fields required.
     * @return List of fields required for enhancement
     */
    public List<ClassField> getFieldsList()
    {
        return fieldsToAdd;
    }

    /**
     * Accessor for the ClassLoaderResolver
     * @return ClassLoader resolver
     */
    public ClassLoaderResolver getClassLoaderResolver()
    {
        return clr;
    }

    public MetaDataManager getMetaDataManager()
    {
        return metaDataMgr;
    }

    public ClassMetaData getClassMetaData()
    {
        return cmd;
    }

    /**
     * Convenience method for whether this class needs to implement Detachable
     * @return Whether we need to implement the Detachable interface
     */
    protected boolean requiresDetachable()
    {
        boolean isDetachable = cmd.isDetachable();
        boolean hasPcsc = (cmd.getPersistenceCapableSuperclass() != null);

        if (!hasPcsc && isDetachable)
        {
            // No superclass and we need to be detachable
            return true;
        }
        else if (hasPcsc)
        {
            if (!cmd.getSuperAbstractClassMetaData().isDetachable() && isDetachable)
            {
                // Superclass isnt detachable, but we need to be
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the class is Persistable or is going to be enhanced based on the metadata
     * @param className the class name
     * @return true if Persistable
     */
    public boolean isPersistable(String className)
    {
        if (className.equals(this.className) && 
            (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_AWARE))
        {
            // This is our class so yes it will be PersistenceCapable
            return true;
        }

        NucleusContext nucleusCtx = metaDataMgr.getNucleusContext();
        Class cls = clr.classForName(className, new EnhancerClassLoader(clr)); // Allow for Enhancer classLoader
        if (nucleusCtx.getApiAdapter().isPersistable(cls))
        {
            // The specified class is already PersistenceCapable
            return true;
        }

        AbstractClassMetaData cmd = metaDataMgr.getMetaDataForClass(cls, clr);
        if (cmd != null && cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
        {
            // The specified class has MetaData and will be enhanced shortly
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#setOptions(java.util.Collection)
     */
    public void setOptions(Collection<String> options)
    {
        if (options == null || options.isEmpty())
        {
            return;
        }
        this.options.addAll(options);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#hasOption(java.lang.String)
     */
    public boolean hasOption(String name)
    {
        return options.contains(name);
    }

    /**
     * Method to save the class definition bytecode into a class file.
     * If directoryName is specified it will be written to $directoryName/className.class
     * else will overwrite the existing class.
     * @param directoryName Name of a directory (or null to overwrite the class)
     * @throws IOException If an I/O error occurs in the write.
     */
    public void save(String directoryName)
    throws IOException
    {
        if (!update)
        {
            // Not updated so nothing to do here
            return;
        }

        File classFile = null;
        File pkClassFile = null;
        if (directoryName != null)
        {
            File baseDir = new File(directoryName);
            if (!baseDir.exists())
            {
                baseDir.mkdirs();
            }
            else if (!baseDir.isDirectory())
            {
                throw new RuntimeException("Target directory " + directoryName + " is not a directory");
            }

            String sep = System.getProperty("file.separator");
            String baseName = cmd.getFullClassName().replace('.', sep.charAt(0));

            classFile = new File(directoryName, baseName + ".class");
            classFile.getParentFile().mkdirs();

            if (getPrimaryKeyClassBytes() != null)
            {
                pkClassFile = new File(directoryName, baseName + AbstractClassMetaData.GENERATED_PK_SUFFIX + ".class");
            }
        }
        else
        {
            String baseName = className.replace('.','/');
            URL classURL = clr.getResource(baseName + ".class", null);
            URL convertedPath = metaDataMgr.getNucleusContext().getPluginManager().resolveURLAsFileURL(classURL);
            String classFilename = convertedPath.getFile();
            classFile = StringUtils.getFileForFilename(classFilename);

            String pkClassFilename = classFilename.substring(0, classFilename.length()-6) + AbstractClassMetaData.GENERATED_PK_SUFFIX + ".class";
            pkClassFile = StringUtils.getFileForFilename(pkClassFilename);
        }

        // Write the class
        FileOutputStream out = null;
        try
        {
            DataNucleusEnhancer.LOGGER.info(LOCALISER.msg("Enhancer.WriteClass", classFile));
            out = new FileOutputStream(classFile);
            out.write(getClassBytes());
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

        byte[] pkClassBytes = getPrimaryKeyClassBytes();
        if (pkClassBytes != null)
        {
            // Write the generated PK class
            try
            {
                DataNucleusEnhancer.LOGGER.info(LOCALISER.msg("Enhancer.WritePrimaryKeyClass", pkClassFile));
                out = new FileOutputStream(pkClassFile);
                out.write(pkClassBytes);
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
            methodsToAdd.add(CopyKeyFieldsFromObjectId.getInstance(this));
            methodsToAdd.add(CopyKeyFieldsFromObjectId2.getInstance(this));
            methodsToAdd.add(CopyKeyFieldsToObjectId.getInstance(this));
            methodsToAdd.add(CopyKeyFieldsToObjectId2.getInstance(this));
            methodsToAdd.add(GetObjectId.getInstance(this));
            methodsToAdd.add(GetVersion.getInstance(this));
            methodsToAdd.add(PreSerialize.getInstance(this));
            methodsToAdd.add(GetPersistenceManager.getInstance(this));
            methodsToAdd.add(GetTransactionalObjectId.getInstance(this));
            methodsToAdd.add(IsDeleted.getInstance(this));
            methodsToAdd.add(IsDirty.getInstance(this));
            methodsToAdd.add(IsNew.getInstance(this));
            methodsToAdd.add(IsPersistent.getInstance(this));
            methodsToAdd.add(IsTransactional.getInstance(this));
            methodsToAdd.add(MakeDirty.getInstance(this));
            methodsToAdd.add(NewObjectIdInstance1.getInstance(this));
            methodsToAdd.add(NewObjectIdInstance2.getInstance(this));
            methodsToAdd.add(ProvideFields.getInstance(this));
            methodsToAdd.add(ReplaceFields.getInstance(this));
            methodsToAdd.add(ReplaceFlags.getInstance(this));
            methodsToAdd.add(ReplaceStateManager.getInstance(this));
        }
        if (cmd.getPersistenceCapableSuperclass() != null && cmd.isRootInstantiableClass())
        {
            // This class is not the root in the inheritance tree, but is the root in terms of being instantiable
            // hence it owns the "identity", so we need real implementations of the identity enhancement methods
            methodsToAdd.add(CopyKeyFieldsFromObjectId.getInstance(this));
            methodsToAdd.add(CopyKeyFieldsFromObjectId2.getInstance(this));
            methodsToAdd.add(CopyKeyFieldsToObjectId.getInstance(this));
            methodsToAdd.add(CopyKeyFieldsToObjectId2.getInstance(this));
            methodsToAdd.add(NewObjectIdInstance1.getInstance(this));
            methodsToAdd.add(NewObjectIdInstance2.getInstance(this));
        }

        if (requiresDetachable())
        {
            methodsToAdd.add(ReplaceDetachedState.getInstance(this));
        }
        if (cmd.isDetachable() && cmd.getPersistenceCapableSuperclass() != null)
        {
            methodsToAdd.add(MakeDirty.getInstance(this));
        }

        methodsToAdd.add(IsDetached.getInstance(this));
        methodsToAdd.add(NewInstance1.getInstance(this));
        methodsToAdd.add(NewInstance2.getInstance(this));
        methodsToAdd.add(ReplaceField.getInstance(this));
        methodsToAdd.add(ProvideField.getInstance(this));
        methodsToAdd.add(CopyField.getInstance(this));
        methodsToAdd.add(CopyFields.getInstance(this));
        methodsToAdd.add(InitFieldNames.getInstance(this));
        methodsToAdd.add(InitFieldTypes.getInstance(this));
        methodsToAdd.add(InitFieldFlags.getInstance(this));
        methodsToAdd.add(GetInheritedFieldCount.getInstance(this));
        methodsToAdd.add(GetManagedFieldCount.getInstance(this));
        methodsToAdd.add(InitPersistenceCapableSuperclass.getInstance(this));
        methodsToAdd.add(LoadClass.getInstance(this));
        methodsToAdd.add(SuperClone.getInstance(this));
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
            EnhancerClassAdapter cv = new EnhancerClassAdapter(cw, this);
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
            EnhancerClassChecker checker = new EnhancerClassChecker(this, logErrors);

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
