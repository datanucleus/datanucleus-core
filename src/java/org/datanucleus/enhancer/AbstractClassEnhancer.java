/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of a class enhancer.
 * To be extended by implementing enhancers.
 */
public abstract class AbstractClassEnhancer implements ClassEnhancer
{
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", ClassConstants.NUCLEUS_CONTEXT_LOADER);

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

    /**
     * Constructor.
     * @param cmd MetaData for the class to be enhanced
     * @param clr ClassLoader resolver
     * @param mmgr MetaData manager
     */
    public AbstractClassEnhancer(ClassMetaData cmd, ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        this.clr = clr;
        this.cmd = cmd;
        this.className = cmd.getFullClassName();
        this.metaDataMgr = mmgr;
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
     * Method to initialise the list of methods to add.
     */
    protected abstract void initialiseMethodsList();

    /**
     * Method to initialise the list of fields to add.
     */
    protected abstract void initialiseFieldsList();

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
}