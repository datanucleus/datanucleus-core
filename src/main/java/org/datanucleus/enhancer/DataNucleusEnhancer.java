/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
2004 Marco Schulze (NightLabs.de) - added verbose output of classpath
2004 Andy Jefferson - updated formatting of user output. Added version, vendor
2006 Andy Jefferson - restructured to have modular ClassEnhancer
2007 Andy Jefferson - swap across to using ASM
2008 Andy Jefferson - rewrite to match JDOEnhancer API
2008 Andy Jefferson - drop BCEL option
    ...
**********************************************************************/
package org.datanucleus.enhancer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.FileMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.PackageMetaData;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * DataNucleus Byte-Code Enhancer.
 * This provides the entry point for enhancement. Enhancement is performed using a ClassEnhancer.
 * Currently provides a ClassEnhancer using ASM.
 * <p>
 * You can use the DataNucleusEnhancer in two ways :-
 * <ul>
 * <li>Via the command line, entering via the main method. This creates a DataNucleusEnhancer object,
 * sets the options necessary and calls the execute method.</li>
 * <li>Programmatically, creating the DataNucleusEnhancer object settings options and running etc.</li>
 * </ul>
 * <p>
 * The programmatic way would be something like this :-
 * <pre>
 * DataNucleusEnhancer enhancer = new DataNucleusEnhancer();
 * enhancer.setVerbose();
 * enhancer.enhancePersistenceUnit("myPersistenceUnit");
 * </pre>
 * enhancing all classes specified by the persistence unit.
 */
public class DataNucleusEnhancer
{
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", ClassEnhancer.class.getClassLoader());

    /** Logger for enhancing. */
    public static final NucleusLogger LOGGER = NucleusLogger.getLoggerInstance("DataNucleus.Enhancer");

    private MetaDataManager metadataMgr;

    /** ClassLoader resolver for use during the enhancement process. */
    private ClassLoaderResolver clr;

    /** Name of the API to use for enhancement (default is JDO). */
    private String apiName = "JDO";

    private String enhancerVersion = null;

    /** Optional output directory where any writing of enhanced classes goes. */
    private String outputDirectory = null;

    /** Whether we should provide verbose output. */
    private boolean verbose = false;

    /** Whether to output to System.out (as well as logging). */
    private boolean systemOut = false;

    /** Whether to allow generation of the PK when needed. */
    private boolean generatePK = true;

    /** Whether to allow generation of the default constructor when needed. */
    private boolean generateConstructor = true;

    /** Whether to use Detach Listener */
    private boolean detachListener = false;

    /** User-provided class loader. */
    protected ClassLoader userClassLoader = null;

    /** Set of components registered for enhancing. */
    private Collection<EnhanceComponent> componentsToEnhance = new ArrayList<EnhanceComponent>();

    /** Map storing input bytes of any classes to enhance, keyed by the class name (if specified using bytes). */
    private Map<String, byte[]> bytesForClassesToEnhanceByClassName = null;

    /**
     * Map of the enhanced bytes of all classes just enhanced, keyed by the class name.
     * Only populated after call of enhance().
     */
    private Map<String, byte[]> enhancedBytesByClassName = null;

    /**
     * Map of the bytes of the PK classes created, keyed by the class name.
     * Only populated after call of enhance().
     */
    private Map<String, byte[]> pkClassBytesByClassName = null;

    static class EnhanceComponent
    {
        public final static int CLASS = 0;
        public final static int CLASS_FILE = 1;
        public final static int MAPPING_FILE = 2;
        public final static int JAR_FILE = 3;
        public final static int PERSISTENCE_UNIT = 4;
        int type;
        Object value;
        public EnhanceComponent(int type, Object value)
        {
            this.type = type;
            this.value = value;
        }
        public Object getValue()
        {
            return value;
        }
        public int getType()
        {
            return type;
        }
    }

    /**
     * Constructor for an enhancer specifying the API and class enhancer and optional properties.
     * @param apiName Name of the API to use; defines which MetaDataManager to utilise.
     * @param props properties controlling enhancement
     */
    public DataNucleusEnhancer(String apiName, Properties props)
    {
        this.apiName = apiName;

        // Create NucleusContext for enhancement
        // TODO Aim to separate MetaDataManager from NucleusContext so we can just have MetaDataManager here
        NucleusContext nucleusContext = new EnhancementNucleusContextImpl(apiName, props);
        if (props != null)
        {
            // Superimpose any user-provided properties
            nucleusContext.getConfiguration().setPersistenceProperties(props);
        }
        this.metadataMgr = nucleusContext.getMetaDataManager();
        this.clr = nucleusContext.getClassLoaderResolver(null);
        this.enhancerVersion = nucleusContext.getPluginManager().getVersionForBundle("org.datanucleus");
    }

    /**
     * Accessor for the MetaDataManager.
     * Allows users to register their own MetaData if defined dynamically.
     * @return MetaData Manager
     */
    public MetaDataManager getMetaDataManager()
    {
        return metadataMgr;
    }

    /**
     * Acessor for the output directory.
     * @return the output directory
     */
    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * Mutator for the output directory where any classes will be written.
     * The default is to use any current class file location and overwrite it
     * @param dir the output directory
     * @return The enhancer
     */
    public DataNucleusEnhancer setOutputDirectory(String dir)
    {
        resetEnhancement();
        this.outputDirectory = dir;
        return this;
    }

    /**
     * Accessor for the user-defined class loader for enhancement (if any).
     * @return The class loader
     */
    public ClassLoader getClassLoader()
    {
        return userClassLoader;
    }

    /**
     * Method to set the class loader to use for loading the class(es) to be enhanced.
     * @param loader The loader
     * @return The enhancer
     */
    public DataNucleusEnhancer setClassLoader(ClassLoader loader)
    {
        resetEnhancement();
        this.userClassLoader = loader;
        if (userClassLoader != null)
        {
            // Set it now in case the user wants to make use of MetaDataManager or something
            clr.registerUserClassLoader(userClassLoader);
        }
        return this;
    }

    /**
     * Accessor for the verbose setting
     * @return the verbose
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * Mutator for the verbose flag
     * @param verbose the verbose to set
     * @return The enhancer
     */
    public DataNucleusEnhancer setVerbose(boolean verbose)
    {
        resetEnhancement();
        this.verbose = verbose;
        return this;
    }

    /**
     * Mutator for whether to output to system out.
     * @param sysout Whether to use sysout
     * @return The enhancer
     */
    public DataNucleusEnhancer setSystemOut(boolean sysout)
    {
        resetEnhancement();
        systemOut = sysout;
        return this;
    }

    /**
     * Mutator for whether to allow generation of PKs where needed.
     * @param flag Whether to enable this
     * @return The enhancer
     */
    public DataNucleusEnhancer setGeneratePK(boolean flag)
    {
        resetEnhancement();
        generatePK = flag;
        return this;
    }

    /**
     * Mutator for whether to allow generation of default constructor where needed.
     * @param flag Whether to enable this
     * @return The enhancer
     */
    public DataNucleusEnhancer setGenerateConstructor(boolean flag)
    {
        resetEnhancement();
        generateConstructor = flag;
        return this;
    }

    /**
     * Mutator for whether to allow generation of default constructor where needed.
     * @param flag Whether to enable this
     * @return The enhancer
     */
    public DataNucleusEnhancer setDetachListener(boolean flag)
    {
        resetEnhancement();
        detachListener = flag;
        return this;
    }


    /**
     * Method to add the specified class (and its input bytes) to the list of classes to enhance.
     * @param className Name of the class (in the format "mydomain.MyClass")
     * @param bytes Bytes of the class
     * @return The enhancer
     */
    public DataNucleusEnhancer addClass(String className, byte[] bytes)
    {
        if (className == null)
        {
            return this;
        }

        if (bytesForClassesToEnhanceByClassName == null)
        {
            bytesForClassesToEnhanceByClassName = new HashMap<String, byte[]>();
        }
        bytesForClassesToEnhanceByClassName.put(className, bytes);
        componentsToEnhance.add(new EnhanceComponent(EnhanceComponent.CLASS, className));

        return this;
    }

    /**
     * Method to add the specified classes to the list of classes to enhance.
     * @param classNames Names of the classes
     * @return The enhancer
     */
    @SuppressWarnings("unchecked")
    public DataNucleusEnhancer addClasses(String... classNames)
    {
        if (classNames == null)
        {
            return this;
        }

        Collection names = new HashSet();
        for (int i=0;i<classNames.length;i++)
        {
            if (classNames[i].endsWith(".class"))
            {
                // Absolute/relative class file (should really be via addFiles())
                String name = classNames[i];
                String msg = null;
                if (!StringUtils.getFileForFilename(classNames[i]).exists())
                {
                    msg = LOCALISER.msg("Enhancer.InputFiles.Invalid", classNames[i]);
                    addMessage(msg, true);
                    name = null;
                }
                else
                {
                    // Extra the name of the class from the name of the file
                    name = ClassEnhancerImpl.getClassNameForFileName(classNames[i]);
                }
                if (name != null)
                {
                    names.add(name);
                }
            }
            else
            {
                // Assumed to be actual class name ("mydomain.MyClass")
                try
                {
                    // Check for class existence
                    clr.classForName(classNames[i], false);
                }
                catch (ClassNotResolvedException cnre)
                {
                    addMessage("Class " + classNames[i] + " not found in CLASSPATH! : " + cnre.getMessage(), true);
                }
                names.add(classNames[i]);
            }
        }

        if (names.size() > 0)
        {
            componentsToEnhance.add(new EnhanceComponent(EnhanceComponent.CLASS,
                names.toArray(new String[names.size()])));
        }
        return this;
    }

    /**
     * Method to add the specified files to the list of components to enhance.
     * @param filenames Names of the files
     * @return The enhancer
     */
    public DataNucleusEnhancer addFiles(String... filenames)
    {
        if (filenames == null)
        {
            return this;
        }

        // Split mapping files, class files, jar files into the respective type
        Collection<String> classFiles = new ArrayList<String>(); // List to keep ordering
        Collection<String> mappingFiles = new ArrayList<String>(); // List to keep ordering
        Collection<String> jarFiles = new HashSet<String>();
        for (int i=0;i<filenames.length;i++)
        {
            if (filenames[i].endsWith(".class"))
            {
                classFiles.add(filenames[i]);
            }
            else if (filenames[i].endsWith(".jar"))
            {
                jarFiles.add(filenames[i]);
            }
            else
            {
                mappingFiles.add(filenames[i]);
            }
        }

        if (mappingFiles.size() > 0)
        {
            componentsToEnhance.add(new EnhanceComponent(EnhanceComponent.MAPPING_FILE,
                mappingFiles.toArray(new String[mappingFiles.size()])));
        }
        if (jarFiles.size() > 0)
        {
            componentsToEnhance.add(new EnhanceComponent(EnhanceComponent.JAR_FILE,
                jarFiles.toArray(new String[jarFiles.size()])));
        }
        if (classFiles.size() > 0)
        {
            componentsToEnhance.add(new EnhanceComponent(EnhanceComponent.CLASS_FILE,
                classFiles.toArray(new String[classFiles.size()])));
        }

        return this;
    }

    /**
     * Method to add the classes defined by the specified jar to the list of components to enhance.
     * @param jarFileName Name of the jar file
     * @return The enhancer
     */
    public DataNucleusEnhancer addJar(String jarFileName)
    {
        if (jarFileName == null)
        {
            return this;
        }

        componentsToEnhance.add(new EnhanceComponent(EnhanceComponent.JAR_FILE, jarFileName));

        return this;
    }

    /**
     * Method to add the classes defined by the persistence-unit to the list of classes to enhance.
     * @param persistenceUnitName Name of the persistence-unit
     * @return The enhancer
     */
    public DataNucleusEnhancer addPersistenceUnit(String persistenceUnitName)
    {
        if (persistenceUnitName == null)
        {
            return this;
        }

        componentsToEnhance.add(new EnhanceComponent(EnhanceComponent.PERSISTENCE_UNIT, persistenceUnitName));

        return this;
    }

    /**
     * Method to enhance all classes defined by addClass, addClasses, addJar, addPersistenceUnit, addFiles.
     * @return Number of classes enhanced
     */
    public int enhance()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Enhancing classes");
        }

        if (componentsToEnhance.isEmpty())
        {
            return 0; // Nothing to enhance
        }

        // Load the meta-data for the registered components to enhance.
        long startTime = System.currentTimeMillis();
        Collection<FileMetaData> fileMetaData = getFileMetadataForInput();

        // Enhance the classes implied by the FileMetaData
        long inputTime = System.currentTimeMillis();
        HashSet<String> classNames = new HashSet<String>();
        Iterator<FileMetaData> filemdIter = fileMetaData.iterator();
        boolean success = true;
        while (filemdIter.hasNext())
        {
            FileMetaData filemd = filemdIter.next();
            for (int packagenum = 0; packagenum < filemd.getNoOfPackages(); packagenum++)
            {
                PackageMetaData pmd = filemd.getPackage(packagenum);
                for (int classnum = 0; classnum < pmd.getNoOfClasses(); classnum++)
                {
                    ClassMetaData cmd = pmd.getClass(classnum);
                    if (classNames.contains(cmd.getFullClassName()))
                    {
                        // Already processed, maybe via annotations and this is MetaData
                        continue;
                    }

                    classNames.add(cmd.getFullClassName());
                    byte[] bytes = bytesForClassesToEnhanceByClassName != null ?
                            bytesForClassesToEnhanceByClassName.get(cmd.getFullClassName()) : null;
                    ClassEnhancer classEnhancer = getClassEnhancer(cmd, bytes);
                    // Enhance, but don't store if based on input bytes
                    boolean clsSuccess = enhanceClass(cmd, classEnhancer, bytes == null);
                    if (!clsSuccess)
                    {
                        success = false;
                    }
                }
            }
        }

        if (!success)
        {
            throw new NucleusException("Failure during enhancement of classes - see the log for details");
        }

        // Log info about timings
        long enhanceTime = System.currentTimeMillis();
        String msg = null;
        if (verbose)
        {
            msg = LOCALISER.msg("Enhancer.Success", classNames.size(), "" + (inputTime-startTime),
                "" + (enhanceTime-inputTime), "" + (enhanceTime-startTime));
        }
        else
        {
            msg = LOCALISER.msg("Enhancer.Success.Simple", classNames.size());
        }
        addMessage(msg, false);

        // Remove the input specification
        if (bytesForClassesToEnhanceByClassName != null)
        {
            bytesForClassesToEnhanceByClassName.clear();
            bytesForClassesToEnhanceByClassName = null;
        }
        componentsToEnhance.clear();

        return classNames.size();
    }

    /**
     * Method to validate all classes defined by addClass, addClasses, addJar, addPersistenceUnit, addFiles.
     * @return Number of classes validated
     */
    public int validate()
    {
        if (componentsToEnhance.isEmpty())
        {
            return 0; // Nothing to validate
        }

        // Load the meta-data for the registered components to enhance.
        long startTime = System.currentTimeMillis();
        Collection<FileMetaData> fileMetaData = getFileMetadataForInput();

        // Validate the classes implied by the FileMetaData
        long inputTime = System.currentTimeMillis();
        HashSet<String> classNames = new HashSet<String>();
        Iterator<FileMetaData> filemdIter = fileMetaData.iterator();
        while (filemdIter.hasNext())
        {
            FileMetaData filemd = filemdIter.next();
            for (int packagenum = 0; packagenum < filemd.getNoOfPackages(); packagenum++)
            {
                PackageMetaData pmd = filemd.getPackage(packagenum);
                for (int classnum = 0; classnum < pmd.getNoOfClasses(); classnum++)
                {
                    ClassMetaData cmd = pmd.getClass(classnum);
                    if (classNames.contains(cmd.getFullClassName()))
                    {
                        // Already processed, maybe via annotations and this is MetaData
                        continue;
                    }

                    classNames.add(cmd.getFullClassName());
                    byte[] bytes = bytesForClassesToEnhanceByClassName != null ?
                            bytesForClassesToEnhanceByClassName.get(cmd.getFullClassName()) : null;
                    ClassEnhancer classEnhancer = getClassEnhancer(cmd, bytes);
                    validateClass(cmd, classEnhancer);
                }
            }
        }

        // Log info about timings
        long enhanceTime = System.currentTimeMillis();
        String msg = null;
        if (verbose)
        {
            msg = LOCALISER.msg("Enhancer.Success", classNames.size(), "" + (inputTime-startTime),
                "" + (enhanceTime-inputTime), "" + (enhanceTime-startTime));
        }
        else
        {
            msg = LOCALISER.msg("Enhancer.Success.Simple", classNames.size());
        }
        addMessage(msg, false);

        // Remove the input specification
        if (bytesForClassesToEnhanceByClassName != null)
        {
            bytesForClassesToEnhanceByClassName.clear();
            bytesForClassesToEnhanceByClassName = null;
        }
        componentsToEnhance.clear();

        return classNames.size();
    }

    /**
     * Method that processes the registered components to enhance, and loads the metadata for
     * them into the MetaDataManager, returning the associated FileMetaData.
     * @return The FileMetaData for the registered components.
     */
    protected Collection<FileMetaData> getFileMetadataForInput()
    {
        Iterator<EnhanceComponent> iter = componentsToEnhance.iterator();
        Collection<FileMetaData> fileMetaData = new ArrayList<FileMetaData>();
        while (iter.hasNext())
        {
            EnhanceComponent comp = iter.next();
            FileMetaData[] filemds = null;
            switch (comp.getType())
            {
                case EnhanceComponent.CLASS : // Of the form "mydomain.MyClass"
                    if (comp.getValue() instanceof String)
                    {
                        // Single class
                        String className = (String)comp.getValue();
                        if (bytesForClassesToEnhanceByClassName != null &&
                            bytesForClassesToEnhanceByClassName.get(className) != null)
                        {
                            // Retrieve the meta-data "file"
                            AbstractClassMetaData cmd = metadataMgr.getMetaDataForClass(className, clr);
                            if (cmd != null)
                            {
                                filemds = new FileMetaData[] {cmd.getPackageMetaData().getFileMetaData()};
                            }
                            else
                            {
                                // No meta-data has been registered for this byte-defined class!
                            }
                        }
                        else
                        {
                            filemds = metadataMgr.loadClasses(new String[] {(String)comp.getValue()},
                                userClassLoader);
                        }
                    }
                    else
                    {
                        // Multiple classes
                        filemds = metadataMgr.loadClasses((String[])comp.getValue(), userClassLoader);
                    }
                    break;

                case EnhanceComponent.CLASS_FILE : // Absolute/relative filename(s)
                    if (comp.getValue() instanceof String)
                    {
                        // Single class file
                        String className = null;
                        String classFilename = (String)comp.getValue();
                        if (!StringUtils.getFileForFilename(classFilename).exists())
                        {
                            String msg = LOCALISER.msg("Enhancer.InputFiles.Invalid", classFilename);
                            addMessage(msg, true);
                        }
                        else
                        {
                            className = ClassEnhancerImpl.getClassNameForFileName(classFilename);
                        }
                        if (className != null)
                        {
                            filemds = metadataMgr.loadClasses(new String[] {className}, userClassLoader);
                        }
                    }
                    else
                    {
                        // Multiple class files
                        Collection<String> classNames = new ArrayList<String>();
                        String[] classFilenames = (String[])comp.getValue();
                        for (int i=0;i<classFilenames.length;i++)
                        {
                            String className = null;
                            if (!StringUtils.getFileForFilename(classFilenames[i]).exists())
                            {
                                String msg = LOCALISER.msg("Enhancer.InputFiles.Invalid", classFilenames[i]);
                                addMessage(msg, true);
                            }
                            else
                            {
                                className = ClassEnhancerImpl.getClassNameForFileName(classFilenames[i]);
                            }
                            if (className != null)
                            {
                                classNames.add(className);
                            }
                        }
                        filemds = metadataMgr.loadClasses(classNames.toArray(new String[classNames.size()]),
                            userClassLoader);
                    }
                    break;

                case EnhanceComponent.MAPPING_FILE : // Absolute/relative filename(s)
                    if (comp.getValue() instanceof String)
                    {
                        // Single mapping file
                        filemds = metadataMgr.loadMetadataFiles(new String[] {(String)comp.getValue()},
                            userClassLoader);
                    }
                    else
                    {
                        // Multiple mapping files
                        filemds = metadataMgr.loadMetadataFiles((String[])comp.getValue(), userClassLoader);
                    }
                    break;

                case EnhanceComponent.JAR_FILE : // Absolute/relative filename(s)
                    if (comp.getValue() instanceof String)
                    {
                        // Single jar file
                        filemds = metadataMgr.loadJar((String)comp.getValue(), userClassLoader);
                    }
                    else
                    {
                        // Multiple jar files
                        String[] jarFilenames = (String[])comp.getValue();
                        Collection<FileMetaData> filemdsColl = new HashSet<FileMetaData>();
                        for (int i=0;i<jarFilenames.length;i++)
                        {
                            FileMetaData[] fmds = metadataMgr.loadJar(jarFilenames[i], userClassLoader);
                            for (int j=0;j<fmds.length;j++)
                            {
                                filemdsColl.add(fmds[j]);
                            }
                        }
                        filemds = filemdsColl.toArray(new FileMetaData[filemdsColl.size()]);
                    }
                    break;

                case EnhanceComponent.PERSISTENCE_UNIT :
                    PersistenceUnitMetaData pumd = null;
                    try
                    {
                        pumd = metadataMgr.getMetaDataForPersistenceUnit((String)comp.getValue());
                    }
                    catch (NucleusException ne)
                    {
                        // No "persistence.xml" files found yet they have specified a persistence-unit name!
                        throw new NucleusEnhanceException(
                            LOCALISER.msg("Enhancer.PersistenceUnit.NoPersistenceFiles", comp.getValue()));
                    }
                    if (pumd == null)
                    {
                        throw new NucleusEnhanceException(
                            LOCALISER.msg("Enhancer.PersistenceUnit.NoSuchUnit", comp.getValue()));
                    }
                    filemds = metadataMgr.loadPersistenceUnit(pumd, userClassLoader);
                    break;

                default :
                    break;
            }
            if (filemds != null)
            {
                for (int i=0;i<filemds.length;i++)
                {
                    fileMetaData.add(filemds[i]);
                }
            }
        }
        return fileMetaData;
    }

    /**
     * Accessor for the enhanced bytes of any classes just enhanced.
     * @param className Name of the class
     * @return The bytes
     * @throws NucleusException if no bytes are available for the specified class
     */
    public byte[] getEnhancedBytes(String className)
    {
        if (enhancedBytesByClassName != null)
        {
            byte[] bytes = enhancedBytesByClassName.get(className);
            if (bytes != null)
            {
                return bytes;
            }
        }

        throw new NucleusException("No enhanced bytes available for " + className);
    }

    /**
     * Accessor for the bytes of any pk classes just created.
     * @param className Name of the class
     * @return The bytes
     * @throws NucleusException if no bytes are available for the specified class
     */
    public byte[] getPkClassBytes(String className)
    {
        if (pkClassBytesByClassName != null)
        {
            byte[] bytes = pkClassBytesByClassName.get(className);
            if (bytes != null)
            {
                return bytes;
            }
        }

        throw new NucleusException("No pk class bytes available for " + className);
    }

    /**
     * Method to throw away any previously stored enhancement results.
     */
    protected void resetEnhancement()
    {
        if (enhancedBytesByClassName != null)
        {
            enhancedBytesByClassName.clear();
            enhancedBytesByClassName = null;
        }
        if (pkClassBytesByClassName != null)
        {
            pkClassBytesByClassName.clear();
            pkClassBytesByClassName = null;
        }
    }

    /**
     * Method to return an instance of the ClassEnhancer for use with this class.
     * @param cmd MetaData for the class
     * @param bytes Bytes (if provided)
     * @return ClassEnhancer instance to use
     */
    @SuppressWarnings("unchecked")
    protected ClassEnhancer getClassEnhancer(ClassMetaData cmd, byte[] bytes)
    {
        // Obtain the required ClassEnhancer to do the work
        ClassEnhancer classEnhancer = null;
        EnhancementNamer namer = (apiName.equalsIgnoreCase("jpa") ? JPAEnhancementNamer.getInstance() : JDOEnhancementNamer.getInstance());
        if (bytes != null)
        {
            classEnhancer = new ClassEnhancerImpl(cmd, clr, metadataMgr, namer, bytes);
        }
        else
        {
            classEnhancer = new ClassEnhancerImpl(cmd, clr, metadataMgr, namer);
        }

        Collection<String> options = new HashSet<String>();
        if (generatePK)
        {
            options.add(ClassEnhancer.OPTION_GENERATE_PK);
        }
        if (generateConstructor)
        {
            options.add(ClassEnhancer.OPTION_GENERATE_DEFAULT_CONSTRUCTOR);
        }
        if (detachListener)
        {
            options.add(ClassEnhancer.OPTION_GENERATE_DETACH_LISTENER);
        }
        classEnhancer.setOptions(options);

        return classEnhancer;
    }

    /**
     * Method to add a message at the required output level.
     * @param msg The message
     * @param error Whether the message is an error, so log at error level (otherwise info)
     */
    protected void addMessage(String msg, boolean error)
    {
        if (error)
        {
            LOGGER.error(msg);
        }
        else
        {
            LOGGER.info(msg);
        }
        if (systemOut)
        {
            System.out.println(msg);
        }
    }

    /**
     * Method to enhance the class defined by the MetaData.
     * @param cmd MetaData for the class
     * @param enhancer ClassEnhancer to use
     * @param store Whether to store the class after enhancing
     * @return Whether the operation performed without error
     */
    protected boolean enhanceClass(ClassMetaData cmd, ClassEnhancer enhancer, boolean store)
    {
        boolean success = true;
        try
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(LOCALISER.msg("Enhancer.EnhanceClassStart", cmd.getFullClassName()));
            }
            boolean enhanced = enhancer.enhance();
            if (enhanced)
            {
                // Store the enhanced bytes
                if (enhancedBytesByClassName == null)
                {
                    enhancedBytesByClassName = new HashMap<String, byte[]>();
                }
                enhancedBytesByClassName.put(cmd.getFullClassName(), enhancer.getClassBytes());
                byte[] pkClassBytes = enhancer.getPrimaryKeyClassBytes();
                if (pkClassBytes != null)
                {
                    if (pkClassBytesByClassName == null)
                    {
                        pkClassBytesByClassName = new HashMap<String, byte[]>();
                    }
                    pkClassBytesByClassName.put(cmd.getFullClassName(), pkClassBytes);
                }

                if (store)
                {
                    enhancer.save(outputDirectory);
                }
                if (isVerbose())
                {
                    if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
                    {
                        addMessage("ENHANCED (Persistable) : " + cmd.getFullClassName(), false);
                    }
                    else if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_AWARE)
                    {
                        addMessage("ENHANCED (PersistenceAware) : " + cmd.getFullClassName(), false);
                    }
                    else
                    {
                        addMessage("NOT ENHANCED (NonPersistent) : " + cmd.getFullClassName(), false);
                    }
                }
            }
            else
            {
                if (isVerbose())
                {
                    if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
                    {
                        addMessage("ERROR (Persistable) : " + cmd.getFullClassName(), false);
                    }
                    else if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_AWARE)
                    {
                        addMessage("ERROR (PersistenceAware) : " + cmd.getFullClassName(), false);
                    }
                    else
                    {
                        addMessage("NOT ENHANCED (NonPersistent) : " + cmd.getFullClassName(), false);
                    }
                }
                if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE ||
                        cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_AWARE)
                {
                    // Error in enhancement
                    success = false;
                }
            }
        }
        catch (IOException ioe)
        {
            // Exception thrown in saving the enhanced file
            if (isVerbose())
            {
                addMessage("ERROR (NonPersistent) : " + cmd.getFullClassName(), false);
            }

            String msg = LOCALISER.msg("Enhancer.ErrorEnhancingClass", cmd.getFullClassName(), ioe.getMessage());
            LOGGER.error(msg, ioe);
            System.out.println(msg);

            success = false;
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LOCALISER.msg("Enhancer.EnhanceClassEnd", cmd.getFullClassName()));
        }

        return success;
    }

    /**
     * Method to validate the enhancement state of the class defined by the MetaData.
     * @param cmd MetaData for the class
     * @param enhancer ClassEnhancer to use
     * @return Always returns true since there is nothing that can go wrong
     */
    protected boolean validateClass(ClassMetaData cmd, ClassEnhancer enhancer)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LOCALISER.msg("Enhancer.ValidateClassStart", cmd.getFullClassName()));
        }

        boolean enhanced = enhancer.validate();
        if (enhanced)
        {
            if (isVerbose())
            {
                if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
                {
                    addMessage("ENHANCED (Persistable) : " + cmd.getFullClassName(), false);
                }
                else if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_AWARE)
                {
                    addMessage("ENHANCED (PersistenceAware) : " + cmd.getFullClassName(), false);
                }
                else
                {
                    addMessage("NOT ENHANCED (NonPersistent) : " + cmd.getFullClassName(), false);
                }
            }
        }
        else
        {
            if (isVerbose())
            {
                if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
                {
                    addMessage("NOT ENHANCED (Persistable) : " + cmd.getFullClassName(), false);
                }
                else if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_AWARE)
                {
                    addMessage("NOT ENHANCED (PersistenceAware) : " + cmd.getFullClassName(), false);
                }
                else
                {
                    addMessage("NOT ENHANCED (NonPersistent) : " + cmd.getFullClassName(), false);
                }
            }
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LOCALISER.msg("Enhancer.ValidateClassEnd", cmd.getFullClassName()));
        }

        return true;
    }

    /**
     * Accessor for global properties defining this enhancer.
     * Provides "VersionNumber", "VendorName" as the minimum, but typically also returns
     * "API", and "EnhancerName"
     * @return The properties.
     */
    public Properties getProperties()
    {
        Properties props = new Properties();
        props.setProperty("VendorName", "DataNucleus");
        props.setProperty("VersionNumber", enhancerVersion);
        props.setProperty("API", apiName);
        return props;
    }

    public String getEnhancerVersion()
    {
        return enhancerVersion;
    }

    /**
     * Entry point for command line enhancer.
     * @param args Command line arguments
     * @throws Exception Thrown if an error occurs
     */
    public static void main(String args[])
    throws Exception
    {
        // Create the enhancer, and set the various options
        final CommandLineHelper clh = new CommandLineHelper(args);
        final boolean quiet = clh.isQuiet();
        final DataNucleusEnhancer enhancer = clh.createDataNucleusEnhancer();

        // Perform the enhancement/validation using the specified input
        final String persistenceUnitName = clh.getPersistenceUnitName();
        final String directoryName = clh.getDirectory();
        final String[] filenames = clh.getFiles();
        int numClasses = 0;
        try
        {
            if (persistenceUnitName != null)
            {
                // Process persistence-unit
                enhancer.addPersistenceUnit(persistenceUnitName);
            }
            else if (directoryName != null)
            {
                File dir = new File(directoryName);
                if (!dir.exists())
                {
                    System.out.println(directoryName + " is not a directory. please set this as a directory");
                    System.exit(1);
                }

                Collection<File> files = ClassUtils.getFilesForDirectory(dir);
                int i = 0;
                String[] fileNames = new String[files.size()];
                for (File file : files)
                {
                    fileNames[i++] = file.getPath();
                }
                enhancer.addFiles(fileNames);
            }
            else
            {
                // Process class/mapping-files
                enhancer.addFiles(filenames);
            }

            if (clh.isValidating())
            {
                numClasses = enhancer.validate();
            }
            else
            {
                numClasses = enhancer.enhance();
            }
        }
        catch (NucleusException ne)
        {
            System.out.println(ne.getMessage());
            String msg = LOCALISER.msg("Enhancer.Failure");
            LOGGER.error(msg, ne);
            if (!quiet)
            {
                System.out.println(msg);
            }
            System.exit(1);
        }

        if (numClasses == 0)
        {
            String msg = LOCALISER.msg("Enhancer.NoClassesEnhanced");
            LOGGER.info(msg);
            if (!quiet)
            {
                System.out.println(msg);
            }
        }
    }
}