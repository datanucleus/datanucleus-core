/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
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
2004 Marcus Mennemeier - contributed with the class loading fix    
    ...
**********************************************************************/
package org.datanucleus;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Class to allow resolution and loading of classes in a persistence framework.
 * Implementations are to define the rules for resolving the classes. For example
 * JDO (used outside a J2EE container) would likely differ from EJB3 (used within
 * a J2EE container).
 * 
 * The search class path order is:
 * <ul>
 * <li>the primary classloader argument</li>
 * <li>the primary classloader (set using setPrimary and kept in ThreadLocal variable).</li>
 * <li>the current thread context classloader.</li>
 * <li>the pm classloader.</li>
 * <li>the registered classloader.</li> 
 * <li>the user registered classloader.</li> 
 * </ul>
 */
public interface ClassLoaderResolver
{
    /**
     * Class loading method, allowing specification of a primary loader. This method does not initialize the class
     * @param name Name of the Class to be loaded
     * @param primary the primary ClassLoader to use (or null)
     * @return The Class given the name, using the specified ClassLoader
     * @throws ClassNotResolvedException if the class can't be found in the classpath
     */
    public Class classForName(String name, ClassLoader primary);

    /**
     * Class loading method, allowing specification of a primary loader
     * and whether the class should be initialised or not.
     * @param name Name of the Class to be loaded
     * @param primary the primary ClassLoader to use (or null)
     * @param initialize whether to initialize the class or not.
     * @return The Class given the name, using the specified ClassLoader
     * @throws ClassNotResolvedException if the class can't be found in the classpath
     */
    public Class classForName(String name, ClassLoader primary, boolean initialize);

    /**
     * Class loading method. This method does not initialize the class
     * @param name Name of the Class to be loaded
     * @return The Class given the name, using the specified ClassLoader
     */
    public Class classForName(String name);

    /**
     * Class loading method, allowing for initialisation of the class.
     * @param name Name of the Class to be loaded
     * @param initialize whether to initialize the class or not.
     * @return The Class given the name, using the specified ClassLoader
     */
    public Class classForName(String name, boolean initialize);

    /**
     * Method to test whether the type represented by the specified class_2 
     * parameter can be converted to the type represented by class_name_1 parameter.
     * @param class_name_1 Class name
     * @param class_2 Class to compare against
     * @return Whether they are assignable
     */
    public boolean isAssignableFrom(String class_name_1, Class class_2);

    /**
     * Method to test whether the type represented by the specified class_name_2 
     * parameter can be converted to the type represented by class_1 parameter.
     * @param class_1 First class
     * @param class_name_2 Class name to compare against
     * @return Whether they are assignable
     */
    public boolean isAssignableFrom(Class class_1, String class_name_2);

    /**
     * Method to test whether the type represented by the specified class_name_2 
     * parameter can be converted to the type represented by class_name_1 parameter.
     * @param class_name_1 Class name
     * @param class_name_2 Class name to compare against
     * @return Whether they are assignable
     */
    public boolean isAssignableFrom(String class_name_1, String class_name_2);
    
    /**
     * ClassLoader registered to load classes created at runtime. One ClassLoader can
     * be registered, and if one ClassLoader is already registered, the registered ClassLoader
     * is replaced by <code>loader</code>.
     * @param loader The ClassLoader in which classes are defined
     */
    public void setRuntimeClassLoader(ClassLoader loader);

    /**
     * ClassLoader registered by users to load classes. One ClassLoader can
     * be registered, and if one ClassLoader is already registered, the registered ClassLoader
     * is replaced by <code>loader</code>.
     * @param loader The ClassLoader in which classes are loaded
     */
    public void registerUserClassLoader(ClassLoader loader);

    /**
     * Finds all the resources with the given name.
     * @param resourceName the resource name. If <code>resourceName</code> starts with "/", remove it before searching.
     * @param primary the primary ClassLoader to use (or null)
     * @return An enumeration of URL objects for the resource. If no resources could be found, the enumeration will be empty. 
     * Resources that the class loader doesn't have access to will not be in the enumeration.
     * @throws IOException If I/O errors occur
     * @see ClassLoader#getResources(java.lang.String)
     */
    public Enumeration<URL> getResources(String resourceName, ClassLoader primary) throws IOException;

    /**
     * Finds the resource with the given name.
     * @param resourceName the path to resource name relative to the classloader root path. If <code>resourceName</code> starts with "/", remove it.   
     * @param primary the primary ClassLoader to use (or null)
     * @return A URL object for reading the resource, or null if the resource could not be found or the invoker doesn't have adequate privileges to get the resource. 
     * @throws IOException If I/O errors occur
     * @see ClassLoader#getResource(java.lang.String)
     */
    public URL getResource(String resourceName, ClassLoader primary);
    
    /**
     * Sets the primary classloader for the current thread.
     * The primary should be kept in a ThreadLocal variable.
     * @param primary the primary classloader
     */
    void setPrimary(ClassLoader primary);

    /**
     * Unsets the primary classloader for the current thread
     */
    void unsetPrimary();
}