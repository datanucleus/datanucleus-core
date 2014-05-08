/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved. 
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
2008 Andy Jefferson - fix for ClassCastException with annotations. Simplifications
    ...
**********************************************************************/
package org.datanucleus.enhancer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Class that will enhance a class at runtime, called via "javaagent".
 */
public class RuntimeEnhancer
{
    /** Message resource */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", ClassConstants.NUCLEUS_CONTEXT_LOADER);

    private final ClassLoaderResolver clr;

    private final NucleusContext nucleusContext;

    Map<ClassLoader, EnhancerClassLoader> runtimeLoaderByLoader = new HashMap();

    List<String> classEnhancerOptions = new ArrayList<String>();

    /**
     *  This classloader is used to load any classes that are necessary during enhancement process, 
     *  and avoid using application classloaders to load classes
     */
    public static class EnhancerClassLoader extends ClassLoader
    {
        EnhancerClassLoader(ClassLoader loader)
        {
            super(loader);
        }

        @SuppressWarnings("unchecked")
        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            Class c = super.findLoadedClass(name);
            if (c != null)
            {
                return c;
            }

            if (name.startsWith("java."))
            {
                //we cannot reload these classes due to security constraints
                return super.loadClass(name, resolve);
            }
            else if (name.startsWith("javax."))
            {
                //just relay JDO/JPA annotations to super loader to avoid ClassCastExceptions
                return super.loadClass(name, resolve);
            }
            else if (name.startsWith("org.datanucleus.jpa.annotations") ||
                     name.startsWith("org.datanucleus.api.jpa.annotations"))
            {
                //just relay DN extension annotations to super loader to avoid ClassCastExceptions
                return super.loadClass(name, resolve);
            }

            String resource = name.replace('.', '/') + ".class";
            try
            {
                //read the class bytes, and define the class
                URL url = super.getResource(resource);
                if (url == null)
                {
                    throw new ClassNotFoundException(name);
                }

                InputStream is = url.openStream();
                try
                {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    byte[] b = new byte[2048];
                    int count;
                    while ((count = is.read(b, 0, 2048)) != -1)
                    {
                        os.write(b,0,count);
                    }
                    byte[] bytes = os.toByteArray();
                    return defineClass(name, bytes, 0, bytes.length);
                }
                finally
                {
                    if (is != null)
                    {
                        is.close();
                    }
                }
            }
            catch (SecurityException e)
            {
                return super.loadClass(name, resolve);
            }    
            catch (IOException e)
            {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

    /**
     * Constructor for a runtime enhancer for an API.
     * Creates its own NucleusContext for enhancement. Note that this is because the NucleusContext
     * currently is for runtime or enhancement, so we isolate things; in future we could take in the NucleusContext
     * from whatever operation has it (e.g PMF, EMF).
     * @param api The API
     * @param contextProps Properties for use by the NucleusContext (e.g ClassLoaderResolver class name, pluginRegistry).
     */
    public RuntimeEnhancer(String api, Map contextProps)
    {
        nucleusContext = new EnhancementNucleusContextImpl(api, contextProps);
        clr = nucleusContext.getClassLoaderResolver(null);

        classEnhancerOptions.add(ClassEnhancer.OPTION_GENERATE_PK);
        classEnhancerOptions.add(ClassEnhancer.OPTION_GENERATE_DEFAULT_CONSTRUCTOR);
    }

    public void setClassEnhancerOption(String optionName)
    {
        this.classEnhancerOptions.add(optionName);
    }

    public void unsetClassEnhancerOption(String optionName)
    {
        this.classEnhancerOptions.remove(optionName);
    }
    
    public byte[] enhance(final String className, byte[] classdefinition, ClassLoader loader)
    {
        EnhancerClassLoader runtimeLoader = runtimeLoaderByLoader.get(loader);
        if (runtimeLoader == null)
        {
            runtimeLoader = new EnhancerClassLoader(loader);
            runtimeLoaderByLoader.put(loader, runtimeLoader);
        }

        // load unenhanced versions of classes from the EnhancerClassLoader
        clr.setPrimary(runtimeLoader);

        try
        {
            Class clazz = null;
            try
            {
                clazz = clr.classForName(className);
            }
            catch (ClassNotResolvedException e1)
            {
                DataNucleusEnhancer.LOGGER.debug(StringUtils.getStringFromStackTrace(e1));
                return null;
            }

            AbstractClassMetaData acmd = nucleusContext.getMetaDataManager().getMetaDataForClass(clazz, clr);
            if (acmd == null)
            {
                // metadata/class not found, ignore. happens in two conditions:
                // -class not in classpath
                // -class does not have metadata or annotation, so it's not supposed to be persistent
                DataNucleusEnhancer.LOGGER.debug("Class "+className+" cannot be enhanced because no metadata has been found.");
                return null;
            }

            // Create a ClassEnhancer to enhance this class
            ClassEnhancer classEnhancer = new ClassEnhancerImpl((ClassMetaData)acmd, clr, 
                nucleusContext.getMetaDataManager(), JDOEnhancementNamer.getInstance(), classdefinition);
            // TODO Allow use of JPAEnhancementNamer?
            classEnhancer.setOptions(classEnhancerOptions);
            classEnhancer.enhance();
            return classEnhancer.getClassBytes();
        }
        catch (Throwable ex)
        {
            DataNucleusEnhancer.LOGGER.error(StringUtils.getStringFromStackTrace(ex));
        }
        return null;
    }
}