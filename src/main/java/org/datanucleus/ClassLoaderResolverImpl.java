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
2004 Erik Bengtson - added extra classForName method
    ...
**********************************************************************/
package org.datanucleus;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.WeakValueMap;

/**
 * A basic implementation of a ClassLoaderResolver. 
 * A ClassLoaderResolver provides a series of methods for resolving classes from their names. 
 * It supports up to 3 class loaders, based loosely on JDO Spec section 12.5.
 * The class loaders will be used in this order:
 * <ul>
 *   <li>The loader that loaded the class or instance referred to in the API that caused this class to be loaded.
 *   <ul>
 *     <li>In case of query, this is the loader of the candidate class, or the loader of the object passed to the newQuery</li>
 *     <LI>In case of navigation from a persistent instance, this is the loader of the class of the instance.</li>
 *     <LI>In the case of getExtent with subclasses, this is the loader of the candidate class.</li>
 *     <LI>In the case of getObjectById, this is the loader of the object id instance.</li>
 *     <LI>Other cases do not have an explicit loader.</li>
 *   </ul></li>
 *   <li>The loader returned in the current context by Thread.getContextClassLoader().</li>
 *   <li>The loader returned by Thread.getContextClassLoader() at the time of creating an ExecutionContext.</li>
 *   <li>The loader registered for dynamically creating and loading classes at runtime.</li>
 * </ul>
 * TODO Provide a way of flushing a classname from the cached classes so we can reload a class
 */
public class ClassLoaderResolverImpl implements ClassLoaderResolver
{
    /** ClassLoader initialised by the context (ExecutionContext). */
    protected final ClassLoader contextLoader;

    /** Hash code cache for performance improvement */
    protected int contextLoaderHashCode = 0;

    /** ClassLoader registered to load runtime created classes. */
    protected ClassLoader runtimeLoader;

    /** Hash code cache for performance improvement */
    protected int runtimeLoaderHashCode = 0;

    /** ClassLoader registered to load classes (e.g set in the persistence properties as the primary loader). */
    protected ClassLoader userRegisteredLoader;

    /** Hash code cache for performance improvement */
    protected int userRegisteredLoaderHashCode = 0;

    /** Cache for loaded classes */
    protected Map<String, Class> loadedClasses = Collections.synchronizedMap(new WeakValueMap());

    /** Cache for loaded classes */
    protected Map<String, Class> unloadedClasses = Collections.synchronizedMap(new WeakValueMap());

    /** Cache for resources */
    protected Map<String, URL> resources = Collections.synchronizedMap(new WeakValueMap());

    /** The primary class */
    ThreadLocal primary = new ThreadLocal();

    /**
     * Constructor for ExecutionContext cases.
     * @param ctxLoader Loader from ExecutionContext initialisation time.
     */
    public ClassLoaderResolverImpl(ClassLoader ctxLoader)
    {
        contextLoader = ctxLoader;
        if (contextLoader != null)
        {
            contextLoaderHashCode = contextLoader.hashCode();
        }
    }

    /**
     * Constructor for non-PersistenceManager cases so there is no
     * PM context loader.
     */
    public ClassLoaderResolverImpl()
    {
        contextLoader = null;
    }

    /**
     * JDO's Class Loading mechanism (Spec 1.0.1 Chapter 12.5).
     * Try 3 loaders, starting with user-supplied loader, then try
     * the current thread's loader, and finally try the PM context
     * loader. This method does not initialize the class
     * @param name Name of the Class to be loaded
     * @param primary primary ClassLoader to use (or null)
     * @return The class given the name, using the required loader.
     * @throws ClassNotResolvedException if the class can't be found in the classpath
     */
    public Class classForName(String name, ClassLoader primary)
    {
        if (name == null)
        {
            // Avoid the NPE and just throw a "not resolved" for null class
            String msg = Localiser.msg("001000");
            throw new ClassNotResolvedException(msg);
        }
        if (name.equals(ClassNameConstants.BYTE))
        {
            return byte.class;
        }
        else if (name.equals(ClassNameConstants.CHAR))
        {
            return char.class;
        }
        else if (name.equals(ClassNameConstants.INT))
        {
            return int.class;
        }
        else if (name.equals(ClassNameConstants.LONG))
        {
            return long.class;
        }
        else if (name.equals(ClassNameConstants.DOUBLE))
        {
            return double.class;
        }
        else if (name.equals(ClassNameConstants.FLOAT))
        {
            return float.class;
        }
        else if (name.equals(ClassNameConstants.SHORT))
        {
            return short.class;
        }
        else if (name.equals(ClassNameConstants.BOOLEAN))
        {
            return boolean.class;
        }
        else if (name.equals(ClassNameConstants.JAVA_LANG_STRING))
        {
            return String.class;
        }

        ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
        String cacheKey = newCacheKey(name, primary, threadClassLoader);

        //lookup in loaded and unloaded classes cache
        Class cls = loadedClasses.get(cacheKey);
        if (cls != null)
        {
            return cls;
        }

        cls = unloadedClasses.get(cacheKey);
        if (cls != null)
        {
            return cls;
        }

        // Try the supplied loader first
        cls = classOrNull(name, primary);

        if (cls == null && this.primary.get() != null)
        {
            // Try the primary for this current thread
            cls = classOrNull(name, (ClassLoader) this.primary.get());
        }
        if (cls == null)
        {
            // Try the loader for the current thread
            cls = classOrNull(name, threadClassLoader);
        }
        if (cls == null)
        {
            // Try the loader for the context
            cls = classOrNull(name, contextLoader);
        }
        if (cls == null && runtimeLoader != null)
        {
            // Try the registered loader for runtime created classes
            cls = classOrNull(name, runtimeLoader);
        }
        if (cls == null && userRegisteredLoader != null)
        {
            // Try the user registered loader for classes
            cls = classOrNull(name, userRegisteredLoader);
        }

        if (cls == null)
        {
            throw new ClassNotResolvedException(Localiser.msg("001000", name));
        }

        //put in unloaded cache, since it was not loaded here
        unloadedClasses.put(cacheKey, cls);

        return cls;
    }

    /**
     * JDO's Class Loading mechanism (Spec 1.0.1 Chapter 12.5).
     * Try 3 loaders, starting with user-supplied loader, then try
     * the current thread's loader, and finally try the PM context loader.
     * @param name Name of the Class to be loaded
     * @param primary primary ClassLoader to use (or null)
     * @return The class given the name, using the required loader.
     * @throws ClassNotResolvedException if the class can't be found in the classpath
     */
    private Class classForNameWithInitialize(String name, ClassLoader primary)
    {
        if (name == null)
        {
            // Avoid the NPE and just throw a "not resolved"
            String msg = Localiser.msg("001000");
            throw new ClassNotResolvedException(msg);
        }
        if (name.equals(ClassNameConstants.BYTE))
        {
            return byte.class;
        }
        else if (name.equals(ClassNameConstants.CHAR))
        {
            return char.class;
        }
        else if (name.equals(ClassNameConstants.INT))
        {
            return int.class;
        }
        else if (name.equals(ClassNameConstants.LONG))
        {
            return long.class;
        }
        else if (name.equals(ClassNameConstants.DOUBLE))
        {
            return double.class;
        }
        else if (name.equals(ClassNameConstants.FLOAT))
        {
            return float.class;
        }
        else if (name.equals(ClassNameConstants.SHORT))
        {
            return short.class;
        }
        else if (name.equals(ClassNameConstants.BOOLEAN))
        {
            return boolean.class;
        }
        else if (name.equals(ClassNameConstants.JAVA_LANG_STRING))
        {
            return String.class;
        }

        ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
        String cacheKey = newCacheKey(name, primary, threadClassLoader);

        //only lookup in loaded classes cache
        Class cls = loadedClasses.get(cacheKey);

        if (cls != null)
        {
            return cls;
        }

        // Try the supplied loader first
        cls = ClassOrNullWithInitialize(name, primary);

        if (cls == null && this.primary.get() != null)
        {
            // Try the primary for this current thread
            cls = ClassOrNullWithInitialize(name, (ClassLoader) this.primary.get());
        }
        if (cls == null)
        {
            // Try the loader for the current thread
            cls = ClassOrNullWithInitialize(name, threadClassLoader);
        }
        if (cls == null)
        {
            // Try the loader for the context
            cls = ClassOrNullWithInitialize(name, contextLoader);
        }
        if (cls == null && runtimeLoader != null)
        {
            // Try the loader for runtime created classes
            cls = ClassOrNullWithInitialize(name, runtimeLoader);
        }
        if (cls == null && userRegisteredLoader != null)
        {
            // Try the user provided loader
            cls = ClassOrNullWithInitialize(name, userRegisteredLoader);
        }

        if (cls == null)
        {
            String msg = Localiser.msg("001000", name);
            throw new ClassNotResolvedException(msg);
        }
        loadedClasses.put(cacheKey, cls);

        return cls;
    }

    /**
     * Compute the key hashCode based for all classLoaders
     * @param prefix the key prefix
     * @param primary the primary ClassLoader, or null
     * @param contextClassLoader the context ClassLoader, or null
     * @return the computed hashCode
     */
    private String newCacheKey(String prefix, ClassLoader primary, ClassLoader contextClassLoader)
    {
        int h = 3;
        if (primary != null)
        {
            h = h ^ primary.hashCode();
        }
        if (contextClassLoader != null)
        {
            h = h ^ contextClassLoader.hashCode();
        }
        h = h ^ contextLoaderHashCode;
        h = h ^ runtimeLoaderHashCode;
        h = h ^ userRegisteredLoaderHashCode;
        return prefix + h;
    }

    /**
     * JDO's Class Loading mechanism (Spec 1.0.1 Chapter 12.5)
     * @param name Name of the Class to be loaded
     * @param primary the primary ClassLoader to use (or null)
     * @param initialize whether to initialize the class or not.
     * @return The Class given the name, using the specified ClassLoader
     * @throws ClassNotResolvedException if the class can't be found in the classpath
     */
    public Class classForName(String name, ClassLoader primary, boolean initialize)
    {
        if (initialize)
        {
            return classForNameWithInitialize(name, primary);
        }
        return classForName(name, primary);
    }

    /**
     * JDO's Class Loading mechanism (Spec 1.0.1 Chapter 12.5). This method does not initialize the class
     * @param name Name of the Class to be loaded
     * @return The class given the name, using the required loader.
     */
    public Class classForName(String name)
    {
        return classForName(name, null);
    }

    /**
     * JDO's Class Loading mechanism (Spec 1.0.1 Chapter 12.5)
     * @param name Name of the Class to be loaded
     * @param initialize whether to initialize the class or not.
     * @return The Class given the name, using the specified ClassLoader
     * @throws ClassNotResolvedException if the class can't be found in the classpath
     */
    public Class classForName(String name, boolean initialize)
    {
        return classForName(name, null, initialize);
    }

    /**
     * Utility to check the assignability of 2 classes in accordance with JDO's
     * Class Loading mechanism. This will check
     * <I>class_1.isAssignableFrom(class_2);</I>
     * @param class_name_1 Name of first class
     * @param class_name_2 Name of second class
     * @return Whether Class 2 is assignable from Class 1
     */
    public boolean isAssignableFrom(String class_name_1, String class_name_2)
    {
        if (class_name_1 == null || class_name_2 == null)
        {
            return false;
        }

        if (class_name_1.equals(class_name_2))
        {
            // Shortcut for case of the same class
            return true;
        }

        Class class_1 = classForName(class_name_1);
        Class class_2 = classForName(class_name_2);

        return class_1.isAssignableFrom(class_2);
    }

    /**
     * Utility to check the assignability of 2 classes in accordance with JDO's
     * Class Loading mechanism. This will check
     * <I>class_1.isAssignableFrom(class_2);</I>
     * @param class_name_1 Name of first class
     * @param class_2 Second class
     * @return Whether Class 2 is assignable from Class 1
     */
    public boolean isAssignableFrom(String class_name_1, Class class_2)
    {
        if (class_name_1 == null || class_2 == null)
        {
            return false;
        }

        if (class_name_1.equals(class_2.getName()))
        {
            // Shortcut for case of the same class
            return true;
        }

        try
        {
            Class class_1 = null;
            if (class_2.getClassLoader() != null)
            {
                // Use class_2's loader if possible
                class_1 = class_2.getClassLoader().loadClass(class_name_1);
            }
            else
            {
                // Use the boot class loader
                class_1 = Class.forName(class_name_1);
            }
            return class_1.isAssignableFrom(class_2);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Utility to check the assignability of 2 classes in accordance with JDO's
     * Class Loading mechanism. This will check
     * <I>class_1.isAssignableFrom(class_2);</I>
     * @param class_1 First class
     * @param class_name_2 Name of second class
     * @return Whether Class 2 is assignable from Class 1
     */
    public boolean isAssignableFrom(Class class_1, String class_name_2)
    {
        if (class_1 == null || class_name_2 == null)
        {
            return false;
        }

        if (class_1.getName().equals(class_name_2))
        {
            // Shortcut for case of the same class
            return true;
        }

        try
        {
            Class class_2 = null;
            if (class_1.getClassLoader() != null)
            {
                // Use class_1's loader if possible
                class_2 = class_1.getClassLoader().loadClass(class_name_2);
            }
            else
            {
                // Use the boot class loader
                class_2 = Class.forName(class_name_2);
            }
            return class_1.isAssignableFrom(class_2);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Returns the Class. This method does not initialize the class
     * @param name the class name
     * @param loader the ClassLoader
     * @return Class the class loaded; null if not found
     */
    private Class classOrNull(String name, ClassLoader loader)
    {
        try
        {
            // JDK1.6+ needs Class.forName(...) here rather than loader.loadClass(...) since
            // array types dont load in JDK1.6+ due to some change of default
            return loader == null ? null : Class.forName(name, false, loader);
        }
        catch (ClassNotFoundException cnfe)
        {
            // Ignore
        }
        catch (NoClassDefFoundError ncdfe)
        {
            // Ignore
        }
        return null;
    }

    /**
     * Returns the Class. Initializes it if found
     * @param name the class name
     * @param loader the ClassLoader
     * @return Class the class loaded; null if not found
     */
    private Class ClassOrNullWithInitialize(String name, ClassLoader loader)
    {
        try
        {
            return loader == null ? null : Class.forName(name, true, loader);
        }
        catch (ClassNotFoundException cnfe)
        {
            return null;
        }
        catch (NoClassDefFoundError ncdfe)
        {
            // Some Windows JRE's throw this
            return null;
        }
    }

    /**
     * ClassLoader registered to load classes created at runtime.
     * @param loader The ClassLoader in which classes are defined
     */
    public void setRuntimeClassLoader(ClassLoader loader)
    {
        this.runtimeLoader = loader;
        if (runtimeLoader == null)
        {
            runtimeLoaderHashCode = 0;
        }
        else
        {
            runtimeLoaderHashCode = loader.hashCode();
        }
    }

    /**
     * ClassLoader registered by users to load classes. One ClassLoader can
     * be registered, and if one ClassLoader is already registered, the registered ClassLoader
     * is replaced by <code>loader</code>.
     * @param loader The ClassLoader in which classes are loaded
     */
    public void registerUserClassLoader(ClassLoader loader)
    {
        this.userRegisteredLoader = loader;
        if (userRegisteredLoader == null)
        {
            userRegisteredLoaderHashCode = 0;
        }
        else
        {
            userRegisteredLoaderHashCode = loader.hashCode();
        }
    }

    /**
     * Finds all the resources with the given name.
     * @param resourceName the resource name. If <code>resourceName</code> starts with "/", remove it before searching.
     * @param primary the primary ClassLoader to use (or null)
     * @return An enumeration of URL objects for the resource. If no resources could be found, the enumeration will be empty. 
     *     Resources that the class loader doesn't have access to will not be in the enumeration.
     * @throws IOException If I/O errors occur
     * @see ClassLoader#getResources(java.lang.String)
     */
    public Enumeration getResources(final String resourceName, final ClassLoader primary) throws IOException
    {
        final List list = new ArrayList();
        final ClassLoader userClassLoader = (ClassLoader) this.primary.get();
        final ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
        
        AccessController.doPrivileged(new PrivilegedAction()
        {
        	public Object run()
        	{
        		try
        		{
        			String name = resourceName;
        			if (name.startsWith("/"))
        			{
        				name = name.substring(1);
        			}
        			if (primary != null)
        			{
        				Enumeration primaryResourceEnum = primary.getResources(name);
        				while (primaryResourceEnum.hasMoreElements())
        				{
        					list.add(primaryResourceEnum.nextElement());
        				}
        			}

        			if (userClassLoader != null)
        			{
        				Enumeration primaryResourceEnum = userClassLoader.getResources(name);
        				while (primaryResourceEnum.hasMoreElements())
        				{
        					list.add(primaryResourceEnum.nextElement());
        				}
        			}

        			if (threadClassLoader != null)
        			{
        				Enumeration resourceEnum = threadClassLoader.getResources(name);
        				while (resourceEnum.hasMoreElements())
        				{
        					list.add(resourceEnum.nextElement());
        				}
        			}

        			if (contextLoader != null)
        			{
        				Enumeration pmResourceEnum = contextLoader.getResources(name);
        				while (pmResourceEnum.hasMoreElements())
        				{
        					list.add(pmResourceEnum.nextElement());
        				}
        			}

        			if (runtimeLoader != null)
        			{
        				Enumeration loaderResourceEnum = runtimeLoader.getResources(name);
        				while (loaderResourceEnum.hasMoreElements())
        				{
        					list.add(loaderResourceEnum.nextElement());
        				}
        			}

        			if (userRegisteredLoader != null)
        			{
        				Enumeration loaderResourceEnum = userRegisteredLoader.getResources(name);
        				while (loaderResourceEnum.hasMoreElements())
        				{
        					list.add(loaderResourceEnum.nextElement());
        				}
        			}
        		}
        		catch(IOException ex)
        		{
        			throw new NucleusException(ex.getMessage(),ex);
        		}
        		return null;
        	}
        });
        // remove duplicates (whilst retaining ordering) and return as Enumeration
        return Collections.enumeration(new LinkedHashSet(list));
    }

    /**
     * Finds the resource with the given name.
     * @param resourceName the path to resource name relative to the classloader root path. 
     *     If <code>resourceName</code> starts with "/", remove it before searching.
     * @param primary the primary ClassLoader to use (or null)
     * @return A URL object for reading the resource, or null if the resource could not be found or the invoker 
     *     doesn't have adequate privileges to get the resource.
     * @see ClassLoader#getResource(java.lang.String)
     */
    public URL getResource(final String resourceName, final ClassLoader primary)
    {
        final ClassLoader userClassLoader = (ClassLoader) this.primary.get();
        URL url = (URL)AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                String resName = resourceName;
                URL url = resources.get(resName);
                if (url != null)
                {
                    return url;
                }

                if (resName.startsWith("/"))
                {
                    resName = resName.substring(1);
                }

                if (primary != null)
                {
                    url = primary.getResource(resName);
                    if (url != null)
                    {
                        resources.put(resName, url);
                        return url;
                    }
                }

                if (userClassLoader != null)
                {
                    url = userClassLoader.getResource(resName);
                    if (url != null)
                    {
                        resources.put(resName, url);
                        return url;
                    }
                }

                ClassLoader threadClassLoader = Thread.currentThread().getContextClassLoader();
                if (threadClassLoader != null)
                {
                    url = threadClassLoader.getResource(resName);
                    if (url != null)
                    {
                        resources.put(resName, url);
                        return url;
                    }
                }

                if (contextLoader != null)
                {
                    url = contextLoader.getResource(resName);
                    if (url != null)
                    {
                        resources.put(resName, url);
                        return url;
                    }
                }

                if (runtimeLoader != null)
                {
                    url = runtimeLoader.getResource(resName);
                    if (url != null)
                    {
                        resources.put(resName, url);
                        return url;
                    }
                }

                if (userRegisteredLoader != null)
                {
                    url = userRegisteredLoader.getResource(resName);
                    if (url != null)
                    {
                        resources.put(resName, url);
                        return url;
                    }
                }
                return null;
            }
        });

        return url;
    }

    /**
     * Sets the primary classloader for the current thread
     * @param primary the primary classloader
     */
    public void setPrimary(ClassLoader primary)
    {
        this.primary.set(primary);
    }

    /**
     * Unsets the primary classloader for the current thread
     */
    public void unsetPrimary()
    {
        this.primary.set(null);
    }

    public String toString()
    {
        return "ClassLoaderResolver: primary=" + primary +
            " contextLoader=" + contextLoader +
            " runtimeLoader=" + runtimeLoader + 
            " registeredLoader=" + userRegisteredLoader;
    }
}