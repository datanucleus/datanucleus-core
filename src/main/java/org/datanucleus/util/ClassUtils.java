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
    ...
**********************************************************************/
package org.datanucleus.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Utilities for handling classes.
 * These are to supplement the methods provided by the Class object.
 */
public class ClassUtils
{
    /** caching for constructors - using caching, the perf is at least doubled **/
    protected static final Map constructorsCache = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.SOFT);

    /**
     * Accessor for a new instance of an object.
     * Uses reflection to generate the instance using the passed constructor parameter arguments.
     * Caches the constructor used to improve performance for later invocation.
     * @param type Type of object (the class).
     * @param parameterTypes Classes of params for the constructor
     * @param parameters The parameters for the constructor
     * @return The object
     * @throws NucleusException If an error occurs creating the instance
     */
    public static Object newInstance(Class type, Class[] parameterTypes, Object[] parameters)
    {
        Object obj;
        try
        {
            StringBuilder name = new StringBuilder(""+type.getName());
            if (parameterTypes != null)
            {
                for (int i=0;i<parameterTypes.length; i++)
                {
                    name.append("-").append(parameterTypes[i].getName());
                }
            }
            Constructor ctor = (Constructor)constructorsCache.get(name.toString());
            if (ctor == null)
            {
                ctor = type.getConstructor(parameterTypes);
                constructorsCache.put(name.toString(), ctor);
            }
            obj = ctor.newInstance(parameters);
        }
        catch (NoSuchMethodException e)
        {
            throw new NucleusException(Localiser.msg("030004", type.getName(), 
                Arrays.asList(parameterTypes).toString()+" "+Arrays.asList(type.getConstructors()).toString()), new Exception[]{e}).setFatal();
        }
        catch (IllegalAccessException e)
        {
            throw new NucleusException(Localiser.msg("030005", type.getName()), new Exception[]{e}).setFatal();
        }
        catch (InstantiationException e)
        {
            throw new NucleusException(Localiser.msg("030006", type.getName()), new Exception[]{e}).setFatal();
        }
        catch (InvocationTargetException e)
        {
            Throwable t = e.getTargetException();
            if (t instanceof RuntimeException)
            {
                throw (RuntimeException) t;
            }
            else if (t instanceof Error)
            {
                throw (Error) t;
            }
            else
            {
                throw new NucleusException(Localiser.msg("030007", type.getName(), t)).setFatal();
            }
        }
        return obj;
    }

    /**
     * Convenience method to return the constructor of the passed class that accepts the supplied argument types. 
     * Allows for primitive to primitive wrapper conversion. Typically used by the JDOQL/JPQL ResultClass mapping process.
     * @param cls The class
     * @param argTypes The constructor argument types. If we know we need a parameter yet don't know the type then this will have a null for that argument type.
     * @return The constructor
     */
    public static Constructor getConstructorWithArguments(Class cls, Class[] argTypes)
    {
        try
        {
            Constructor[] constructors = cls.getConstructors();
            if (constructors != null)
            {
                // Check the types of the constructor and find one that matches allowing for
                // primitive to wrapper conversion
                for (int i=0;i<constructors.length;i++)
                {
                    Class[] ctrParams = constructors[i].getParameterTypes();
                    boolean ctrIsValid = true;

                    // Discard any constructor with a different number of params
                    if (ctrParams != null && ctrParams.length == argTypes.length)
                    {
                        for (int j=0;j<ctrParams.length;j++)
                        {
                            // Compare the type with the object or any primitive
                            Class primType = ClassUtils.getPrimitiveTypeForType(argTypes[j]);
                            if (argTypes[j] == null && ctrParams[j].isPrimitive())
                            {
                                // Null type for field so has to accept nulls, and primitives don't do that
                                ctrIsValid = false;
                                break;
                            }
                            else if (argTypes[j] != null && !ctrParams[j].isAssignableFrom(argTypes[j]) && (primType == null || ctrParams[j] != primType))
                            {
                                // Different type in this parameter position
                                ctrIsValid = false;
                                break;
                            }
                        }
                    }
                    else
                    {
                        ctrIsValid = false;
                    }

                    if (ctrIsValid)
                    {
                        return constructors[i];
                    }
                }
            }
        }
        catch (SecurityException se)
        {
            // Can't access the constructors
        }

        return null;
    }

    /**
     * Convenience method to return the constructor of the passed class that accepts the supplied argument types. 
     * Allows for primitive to primitive wrapper conversion. Typically used by the JDOQL/JPQL ResultClass mapping process.
     * @param cls The class
     * @param argTypes The constructor argument types. If we know we need a parameter yet don't know the type then this will have a null for that argument type.
     * @param argTypeCheck Whether to check the type of the different arguments. Useful where we don't know the result type of an argument until processing results
     * @return The constructor
     */
    public static Constructor getConstructorWithArguments(Class cls, Class[] argTypes, boolean[] argTypeCheck)
    {
        try
        {
            Constructor[] constructors = cls.getConstructors();
            if (constructors != null)
            {
                // Check the types of the constructor and find one that matches allowing for
                // primitive to wrapper conversion
                for (int i=0;i<constructors.length;i++)
                {
                    Class[] ctrParams = constructors[i].getParameterTypes();
                    boolean ctrIsValid = true;

                    // Discard any constructor with a different number of params
                    if (ctrParams != null && ctrParams.length == argTypes.length)
                    {
                        for (int j=0;j<ctrParams.length;j++)
                        {
                            if (!argTypeCheck[j])
                            {
                                // This argument doesn't need precise type checking
                                break;
                            }

                            // Compare the type with the object or any primitive
                            Class primType = ClassUtils.getPrimitiveTypeForType(argTypes[j]);
                            if (argTypes[j] == null && ctrParams[j].isPrimitive())
                            {
                                // Null type for field so has to accept nulls, and primitives don't do that
                                ctrIsValid = false;
                                break;
                            }
                            else if (argTypes[j] != null && !ctrParams[j].isAssignableFrom(argTypes[j]) && (primType == null || ctrParams[j] != primType))
                            {
                                // Different type in this parameter position
                                ctrIsValid = false;
                                break;
                            }
                        }
                    }
                    else
                    {
                        ctrIsValid = false;
                    }

                    if (ctrIsValid)
                    {
                        return constructors[i];
                    }
                }
            }
        }
        catch (SecurityException se)
        {
            // Can't access the constructors
        }

        return null;
    }

    /**
     * Obtain a method from a class or superclasses using reflection.
     * The method will have the specified name and will take a single argument.
     * Allows for the argument type being primitive or its associated wrapper.
     * @param cls the class to find the declared fields and populate the map
     * @param methodName the method name to find
     * @param argType the argument type
     * @return The Method
     */
    public static Method getMethodWithArgument(Class cls, String methodName, Class argType)
    {
        Method m = ClassUtils.getMethodForClass(cls, methodName, new Class[] {argType});
        if (m == null)
        {
            Class primitive = ClassUtils.getPrimitiveTypeForType(argType);
            if (primitive != null)
            {
                m = ClassUtils.getMethodForClass(cls, methodName, new Class[] {primitive});
            }
        }
        return m;
    }

    /**
     * Obtain a method from a class or superclasses using reflection
     * @param cls the class where to look for the method
     * @param methodName the method name to find 
     * @param argtypes the classes argument of the method
     * @return The Method
     */
    public static Method getMethodForClass(Class cls, String methodName, Class[] argtypes)
    {
        try
        {
            return cls.getDeclaredMethod(methodName, argtypes);
        }
        catch (NoSuchMethodException e)
        {
            if (cls.getSuperclass() != null)
            {
                return ClassUtils.getMethodForClass(cls.getSuperclass(), methodName, argtypes);
            }
        }
        catch (Exception e)
        {
            // do nothing
        }
        return null;
    }

    /**
     * Method to return the class files below the specified directory.
     * @param dir The directory
     * @param normal_classes Whether to include normal classes
     * @param inner_classes Whether to include inner classes
     * @return The class files (Collection of File objects).
     */
    public static Collection<File> getClassFilesForDirectory(File dir, boolean normal_classes, boolean inner_classes)
    {
        if (dir == null)
        {
            return null;
        }

        Collection classes=new HashSet();
        File[] files = dir.listFiles();
        if (files != null)
        {
            for (int i=0;i<files.length;i++)
            {
                if (files[i].isFile())
                {
                    // If this is a class file, add it
                    if (files[i].getName().endsWith(".class"))
                    {
                        boolean is_inner_class=isInnerClass(files[i].getName());
                        if ((normal_classes && !is_inner_class) ||
                            (inner_classes && is_inner_class))
                        {
                            classes.add(files[i]);
                        }
                    }
                }
                else
                {
                    // Check for classes in subdirectories
                    Collection child_classes=getClassFilesForDirectory(files[i],normal_classes,inner_classes);
                    if (child_classes != null && !child_classes.isEmpty())
                    {
                        classes.addAll(child_classes);
                    }
                }
            }
        }

        return classes;
    }

    /**
     * Method to return the files below the specified directory.
     * @param dir The directory
     * @return The files
     */
    public static Collection<File> getFilesForDirectory(File dir)
    {
        if (dir == null)
        {
            return null;
        }

        Collection files = new HashSet();
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null)
        {
            for (int i=0;i<dirFiles.length;i++)
            {
                if (dirFiles[i].isFile())
                {
                    files.add(dirFiles[i]);
                }
                else
                {
                    // Check for files in subdirectories
                    Collection childFiles = getFilesForDirectory(dirFiles[i]);
                    if (childFiles != null && !childFiles.isEmpty())
                    {
                        files.addAll(childFiles);
                    }
                }
            }
        }

        return files;
    }

    /**
     * Convenience accessor for the names of all class files in the jar file with the specified name.
     * The returned class names are of the form "org.datanucleus.MyClass".
     * @param jarFileName Name of the jar file
     * @return The class names
     */
    public static String[] getClassNamesForJarFile(String jarFileName)
    {
        try
        {
            JarFile jar = new JarFile(jarFileName);
            return getClassNamesForJarFile(jar);
        }
        catch (IOException ioe)
        {
            NucleusLogger.GENERAL.warn("Error opening the jar file " + jarFileName + " : " + ioe.getMessage());
        }
        return null;
    }

    /**
     * Convenience accessor for the names of all class files in the jar file with the specified URL.
     * The returned class names are of the form "org.datanucleus.MyClass".
     * @param jarFileURL URL for the jar file
     * @return The class names
     */
    public static String[] getClassNamesForJarFile(URL jarFileURL)
    {
        File jarFile = new File(jarFileURL.getFile()); // TODO Check for errors
        try
        {
            JarFile jar = new JarFile(jarFile);
            return getClassNamesForJarFile(jar);
        }
        catch (IOException ioe)
        {
            NucleusLogger.GENERAL.warn("Error opening the jar file " + jarFileURL.getFile() + " : " + ioe.getMessage());
        }
        return null;
    }

    /**
     * Convenience accessor for the names of all class files in the jar file with the specified URL.
     * The returned class names are of the form "org.datanucleus.MyClass".
     * @param jarFileURI URI for the jar file
     * @return The class names
     */
    public static String[] getClassNamesForJarFile(URI jarFileURI)
    {
        try
        {
            return getClassNamesForJarFile(jarFileURI.toURL());
        }
        catch (MalformedURLException mue)
        {
            throw new NucleusException("Error opening the jar file " + jarFileURI, mue);
        }
    }

    /**
     * Convenience method to return the names of classes specified in the jar file.
     * All inner classes are ignored.
     * @param jar Jar file
     * @return The class names
     */
    private static String[] getClassNamesForJarFile(JarFile jar)
    {
        Enumeration jarEntries = jar.entries();
        Set<String> classes = new HashSet();
        while (jarEntries.hasMoreElements())
        {
            String entry = ((JarEntry)jarEntries.nextElement()).getName();
            if (entry.endsWith(".class") && !ClassUtils.isInnerClass(entry))
            {
                String className = entry.substring(0, entry.length()-6); // Omit ".class"
                className = className.replace(File.separatorChar, '.');
                classes.add(className);
            }
        }
        return classes.toArray(new String[classes.size()]);
    }

    /**
     * Convenience accessor for the names of all "package.jdo" files in the jar file with the specified name.
     * @param jarFileName Name of the jar file
     * @return The "package.jdo" file names
     */
    public static String[] getPackageJdoFilesForJarFile(String jarFileName)
    {
        try
        {
            JarFile jar = new JarFile(jarFileName);
            return getFileNamesWithSuffixForJarFile(jar, "package.jdo");
        }
        catch (IOException ioe)
        {
            NucleusLogger.GENERAL.warn("Error opening the jar file " + jarFileName + " : " + ioe.getMessage());
        }
        return null;
    }

    /**
     * Convenience accessor for the names of all "package.jdo" files in the jar file with the specified URL.
     * @param jarFileURL URL for the jar file
     * @return The "package.jdo" file names
     */
    public static String[] getPackageJdoFilesForJarFile(URL jarFileURL)
    {
        File jarFile = new File(jarFileURL.getFile()); // TODO Check for errors
        try
        {
            JarFile jar = new JarFile(jarFile);
            return getFileNamesWithSuffixForJarFile(jar, "package.jdo");
        }
        catch (IOException ioe)
        {
            NucleusLogger.GENERAL.warn("Error opening the jar file " + jarFileURL.getFile() + " : " + ioe.getMessage());
        }
        return null;
    }

    /**
     * Convenience accessor for the names of all "package.jdo" files in the jar file with the specified URL.
     * @param jarFileURI URI for the jar file
     * @return The "package.jdo" file names
     */
    public static String[] getPackageJdoFilesForJarFile(URI jarFileURI)
    {
        URL jarFileURL = null;
        try
        {
            jarFileURL = jarFileURI.toURL();
        }
        catch (MalformedURLException mue)
        {
            throw new NucleusException("JAR file at " + jarFileURI + " not openable. Invalid URL");
        }
        return getPackageJdoFilesForJarFile(jarFileURL);
    }

    /**
     * Convenience method to return the names of files specified in the jar file that end with
     * the specified suffix.
     * @param jar Jar file
     * @param suffix Suffix for the file (can be the filename without the path)
     * @return The fully-qualified names of the files with this suffix in the jar file
     */
    private static String[] getFileNamesWithSuffixForJarFile(JarFile jar, String suffix)
    {
        Enumeration jarEntries = jar.entries();
        Set<String> files = new HashSet();
        while (jarEntries.hasMoreElements())
        {
            String entry = ((JarEntry)jarEntries.nextElement()).getName();
            if (entry.endsWith(suffix))
            {
                files.add(entry);
            }
        }
        return files.toArray(new String[files.size()]);
    }

    /**
     * Convenience method to return the names of classes specified in the directory and below.
     * All inner classes are ignored.
     * @param dir Directory that we should look below (root of classpath)
     * @return The class names
     */
    public static String[] getClassNamesForDirectoryAndBelow(File dir)
    {
        if (dir == null)
        {
            return null;
        }

        Collection<File> classFiles = getClassFilesForDirectory(dir, true, false);
        if (classFiles == null || classFiles.isEmpty())
        {
            return null;
        }

        String[] classNames = new String[classFiles.size()];
        Iterator<File> iter = classFiles.iterator();
        int i = 0;
        while (iter.hasNext())
        {
            String filename = iter.next().getAbsolutePath();

            // Omit root part and trailing ".class"
            String classname = filename.substring(dir.getAbsolutePath().length()+1, filename.length()-6);
            classNames[i++] = classname.replace(File.separatorChar, '.');
        }
        return classNames;
    }

    /**
     * Method to check whether a classname is for an inner class.
     * Currently checks for the presence of $ in the name.
     * @param class_name The class name
     * @return Whether it is an inner class
     */
    public static boolean isInnerClass(String class_name)
    {
        if (class_name == null)
        {
            return false;
        }
        else if (class_name.indexOf('$') >= 0)
        {
            return true;
        }
        return false;
    }

    /**
     * Method to check for a default constructor on a class.
     * Particular relevance for JDO is the requirement for a default
     * constructor on all Persistence-Capable classes. Doesn't check
     * superclasses for the default constructor.
     * @param the_class The class
     * @return Whether it has a default constructor
     **/
    public static boolean hasDefaultConstructor(Class the_class)
    {
        if (the_class == null)
        {
            return false;
        }
        try
        {
            the_class.getDeclaredConstructor();
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    /**
     * Method to return the superclasses for a class.
     * The superclasses will be ordered.
     * @param the_class The class
     * @return The superclass of this class.
     */
    public static Collection<Class<?>> getSuperclasses(Class<?> the_class)
    {
        List<Class<?>> result = new ArrayList<Class<?>>();
        Class<?> superclass = the_class.getSuperclass();
        while (superclass != null)
        {
            result.add(superclass);
            superclass = superclass.getSuperclass();
        }
        return result;
    }

    /**
     * Method to return the superinterfaces for a class.
     * The superinterfaces will be ordered and unique.
     * @param the_class The class
     * @return The superinterfaces of this class.
     */
    public static Collection<Class<?>> getSuperinterfaces(Class<?> the_class)
    {
        List<Class<?>> result = new ArrayList<Class<?>>();
        collectSuperinterfaces(the_class, result);
        return result;
    }

    private static void collectSuperinterfaces(Class<?> c, List<Class<?>> result)
    {
        // TODO Include interfaces of superclasses? (see ClassUtilsTest.testGetSuperinterfacesViaSuperclass)
//        Class<?> superclass = c.getSuperclass();
//        if (superclass != null)
//        {
//            collectSuperinterfaces(superclass, result);
//        }

        for (Class<?> i : c.getInterfaces())
        {
            if (!result.contains(i))
            {
                result.add(i);
                collectSuperinterfaces(i, result);
            }
        }
    }

    /**
     * Obtain a field from a class or superclasses using reflection.
     * @param cls the class to find the field from
     * @param fieldName the field name to find
     * @return The Field 
     */
    public static Field getFieldForClass(Class cls, String fieldName)
    {
        try
        {
            do
            {
                try
                {
                    return cls.getDeclaredField(fieldName);
                }
                catch (NoSuchFieldException e)
                {
                    cls = cls.getSuperclass();
                }
            }
            while (cls != null);
        }
        catch (Exception e)
        {
            // do nothing
        }
        return null;
    }

    /**
     * Obtain a (Java bean) getter method from a class or superclasses using reflection.
     * Any 'get...' method will take precedence over 'is...' methods.
     * @param cls the class to find the getter from
     * @param beanName the name of the java bean to find the getter for
     * @return The getter Method 
     */
    public static Method getGetterMethodForClass(Class cls, String beanName)
    {
        Method getter = findDeclaredMethodInHeirarchy(cls, getJavaBeanGetterName(beanName, false));
        if (getter == null)
        {
            getter = findDeclaredMethodInHeirarchy(cls, getJavaBeanGetterName(beanName, true));
        }
        return getter;
    }

    /**
     * Obtain a (Java bean) setter method from a class or superclasses using reflection.
     * @param cls the class to find the setter from
     * @param beanName the name of the java bean to find the setter for
     * @param type the type of the java bean to find the setter for
     * @return The setter Method
     */
    public static Method getSetterMethodForClass(Class cls, String beanName, Class type)
    {
        return findDeclaredMethodInHeirarchy(cls, getJavaBeanSetterName(beanName), type);
    }

    private static Method findDeclaredMethodInHeirarchy(Class cls, String methodName, Class... parameterTypes)
    {
        try
        {
            do
            {
                try
                {
                    return cls.getDeclaredMethod(methodName, parameterTypes);
                }
                catch (NoSuchMethodException e)
                {
                    cls = cls.getSuperclass();
                }
            }
            while (cls != null);
        }
        catch (Exception e)
        {
            // do nothing
        }
        return null;
    }

    /**
     * Convenience method to return the object wrapper type for a primitive type name.
     * If the type is not a primitive then just returns the type name
     * @param typeName The primitive type name
     * @return The object wrapper type name for this primitive
     */
    public static String getWrapperTypeNameForPrimitiveTypeName(String typeName)
    {
        if (typeName.equals("boolean"))
        {
            return ClassNameConstants.JAVA_LANG_BOOLEAN;
        }
        else if (typeName.equals("byte"))
        {
            return ClassNameConstants.JAVA_LANG_BYTE;
        }
        else if (typeName.equals("char"))
        {
            return ClassNameConstants.JAVA_LANG_CHARACTER;
        }
        else if (typeName.equals("double"))
        {
            return ClassNameConstants.JAVA_LANG_DOUBLE;
        }
        else if (typeName.equals("float"))
        {
            return ClassNameConstants.JAVA_LANG_FLOAT;
        }
        else if (typeName.equals("int"))
        {
            return ClassNameConstants.JAVA_LANG_INTEGER;
        }
        else if (typeName.equals("long"))
        {
            return ClassNameConstants.JAVA_LANG_LONG;
        }
        else if (typeName.equals("short"))
        {
            return ClassNameConstants.JAVA_LANG_SHORT;
        }
        else
        {
            return typeName;
        }
    }

    /**
     * Convenience method to return the object wrapper type for a primitive type.
     * @param type The primitive type 
     * @return The object wrapper type for this primitive
     */
    public static Class getWrapperTypeForPrimitiveType(Class type)
    {
        if (type == boolean.class)
        {
            return ClassConstants.JAVA_LANG_BOOLEAN;
        }
        else if (type == byte.class)
        {
            return ClassConstants.JAVA_LANG_BYTE;
        }
        else if (type == char.class)
        {
            return ClassConstants.JAVA_LANG_CHARACTER;
        }
        else if (type == double.class)
        {
            return ClassConstants.JAVA_LANG_DOUBLE;
        }
        else if (type == float.class)
        {
            return ClassConstants.JAVA_LANG_FLOAT;
        }
        else if (type == int.class)
        {
            return ClassConstants.JAVA_LANG_INTEGER;
        }
        else if (type == long.class)
        {
            return ClassConstants.JAVA_LANG_LONG;
        }
        else if (type == short.class)
        {
            return ClassConstants.JAVA_LANG_SHORT;
        }
        return null;
    }

    /**
     * Method to return the primitive equivalent of the specified type (if any).
     * Returns null if there is no primitive equivalent.
     * @param type The type
     * @return The primitive equivalent.
     */
    public static Class getPrimitiveTypeForType(Class type)
    {
        if (type == Boolean.class)
        {
            return ClassConstants.BOOLEAN;
        }
        else if (type == Byte.class)
        {
            return ClassConstants.BYTE;
        }
        else if (type == Character.class)
        {
            return ClassConstants.CHAR;
        }
        else if (type == Double.class)
        {
            return ClassConstants.DOUBLE;
        }
        else if (type == Float.class)
        {
            return ClassConstants.FLOAT;
        }
        else if (type == Integer.class)
        {
            return ClassConstants.INT;
        }
        else if (type == Long.class)
        {
            return ClassConstants.LONG;
        }
        else if (type == Short.class)
        {
            return ClassConstants.SHORT;
        }
        else
        {
            return null;
        }
    }

    /**
     * Convenience method to return if the passed type (name) is a primitive wrapper type.
     * @param typeName Name of the type
     * @return Whether it is a primitive wrapper
     */
    public static boolean isPrimitiveWrapperType(String typeName)
    {
        if (typeName.equals(ClassNameConstants.JAVA_LANG_BOOLEAN) ||
            typeName.equals(ClassNameConstants.JAVA_LANG_BYTE) ||
            typeName.equals(ClassNameConstants.JAVA_LANG_CHARACTER) ||
            typeName.equals(ClassNameConstants.JAVA_LANG_DOUBLE) ||
            typeName.equals(ClassNameConstants.JAVA_LANG_FLOAT) ||
            typeName.equals(ClassNameConstants.JAVA_LANG_INTEGER) ||
            typeName.equals(ClassNameConstants.JAVA_LANG_LONG) ||
            typeName.equals(ClassNameConstants.JAVA_LANG_SHORT))
        {
            return true;
        }
        return false;
    }

    /**
     * Convenience method to return if the passed type (name) is a primitive array type.
     * @param typeName Name of the type
     * @return Whether it is a primitive array
     */
    public static boolean isPrimitiveArrayType(String typeName)
    {
        if (typeName.equals(ClassNameConstants.BOOLEAN_ARRAY) ||
            typeName.equals(ClassNameConstants.BYTE_ARRAY) ||
            typeName.equals(ClassNameConstants.CHAR_ARRAY) ||
            typeName.equals(ClassNameConstants.DOUBLE_ARRAY) ||
            typeName.equals(ClassNameConstants.FLOAT_ARRAY) ||
            typeName.equals(ClassNameConstants.INT_ARRAY) ||
            typeName.equals(ClassNameConstants.LONG_ARRAY) ||
            typeName.equals(ClassNameConstants.SHORT_ARRAY))
        {
            return true;
        }
        return false;
    }

    /**
     * Convenience method to return if the passed type (name) is a primitive type.
     * @param typeName Name of the type
     * @return Whether it is a primitive
     */
    public static boolean isPrimitiveType(String typeName)
    {
        if (typeName.equals(ClassNameConstants.BOOLEAN) ||
            typeName.equals(ClassNameConstants.BYTE) ||
            typeName.equals(ClassNameConstants.CHAR) ||
            typeName.equals(ClassNameConstants.DOUBLE) ||
            typeName.equals(ClassNameConstants.FLOAT) ||
            typeName.equals(ClassNameConstants.INT) ||
            typeName.equals(ClassNameConstants.LONG) ||
            typeName.equals(ClassNameConstants.SHORT))
        {
            return true;
        }
        return false;
    }

    /**
     * Convenience method to convert the passed value to an object of the specified type (if possible).
     * If no such conversion is supported will return null. If the required type is a primitive will
     * return an object of the wrapper type.
     * @param value The value
     * @param cls The class
     * @return The converted value (or null)
     */
    public static Object convertValue(Object value, Class cls)
    {
        if (value == null)
        {
            return null;
        }

        Class type = cls;
        if (cls.isPrimitive())
        {
            type = getWrapperTypeForPrimitiveType(cls);
        }
        if (type.isAssignableFrom(value.getClass()))
        {
            // Already in the correct type
            return value;
        }

        if (type == Long.class && value instanceof Number)
        {
            return Long.valueOf(((Number)value).longValue());
        }
        else if (type == Integer.class && value instanceof Number)
        {
            return Integer.valueOf(((Number)value).intValue());
        }
        else if (type == Short.class && value instanceof Number)
        {
            return Short.valueOf(((Number)value).shortValue());
        }
        else if (type == Float.class && value instanceof Number)
        {
            return Float.valueOf(((Number)value).floatValue());
        }
        else if (type == Double.class && value instanceof Number)
        {
            return Double.valueOf(((Number)value).doubleValue());
        }
        else if (type == Boolean.class && value instanceof Long)
        {
            // Convert a Long (0, 1) to Boolean (FALSE, TRUE) and null otherwise
            return (Long)value == 0 ? Boolean.FALSE : ((Long)value == 1 ? Boolean.TRUE : null);
        }
        else if (type == Boolean.class && value instanceof Integer)
        {
            // Convert a Integer (0, 1) to Boolean (FALSE, TRUE) and null otherwise
            return (Integer)value == 0 ? Boolean.FALSE : ((Integer)value == 1 ? Boolean.TRUE : null);
        }
        return null;
    }

    /**
     * Convenience method to return if two types are compatible.
     * Returns true if both types are primitive/wrappers and are of the same type.
     * Returns true if clsName2 is the same or a subclass of cls1.
     * Otherwise returns false;
     * @param cls1 First class
     * @param clsName2 Name of the second class
     * @param clr ClassLoader resolver to use
     * @return Whether they are compatible
     */
    public static boolean typesAreCompatible(Class cls1, String clsName2, ClassLoaderResolver clr)
    {
        if (clr.isAssignableFrom(cls1, clsName2))
        {
            return true;
        }

        // Cater for primitive and primitive wrappers being compatible
        if (cls1.isPrimitive())
        {
            return clr.isAssignableFrom(ClassUtils.getWrapperTypeForPrimitiveType(cls1), clsName2);
        }
        else if (ClassUtils.isPrimitiveWrapperType(cls1.getName()))
        {
            return clr.isAssignableFrom(ClassUtils.getPrimitiveTypeForType(cls1), clsName2);
        }

        return false;
    }

    /**
     * Convenience method to return if two types are compatible.
     * Returns true if both types are primitive/wrappers and are of the same type.
     * Returns true if cls2 is the same or a subclass of cls1.
     * Otherwise returns false;
     * @param cls1 First class
     * @param cls2 Second class
     * @return Whether they are compatible
     */
    public static boolean typesAreCompatible(Class cls1, Class cls2)
    {
        if (cls1.isAssignableFrom(cls2))
        {
            return true;
        }

        // Cater for primitive and primitive wrappers being compatible
        if (cls1.isPrimitive())
        {
            return ClassUtils.getWrapperTypeForPrimitiveType(cls1).isAssignableFrom(cls2);
        }

        return false;
    }

    /**
     * Utility to create the full class name given the package and class name.
     * Some examples 
     * <PRE>
     * packageName=test className=Test, returns result=test.Test
     * packageName=test className=test1.Test, returns result=test1.Test
     * packageName=&lt;null&gt; className=Test, returns result=Test
     * packageName=&lt;null&gt; className=test1.Test, returns result=test1.Test
     * </PRE>
     * @param pkg_name package name.
     * @param cls_name class name.
     * @return generated full class name.
     */
    public static String createFullClassName(String pkg_name, String cls_name)
    {
        if (StringUtils.isWhitespace(cls_name))
        {
            throw new IllegalArgumentException("Class name not specified");
        }
        else if (StringUtils.isWhitespace(pkg_name))
        {
            return cls_name;
        }
        else if (cls_name.indexOf('.') >= 0)
        {
            return cls_name;
        }
        return pkg_name + "." + cls_name;
    }

    /**
     * Convenience method to return the passed type as a java.lang type wherever possible.
     * The passed type will be stripped of any package name and will be checked if it is 
     * a known java.lang class. This is used where the user has specified a class name
     * for a collection or map element/key/value type and meant a java.lang class but didn't
     * fully qualify it.
     * @param type The type name
     * @return The java.lang equivalent (or the input type if not possible)
     */
    public static String getJavaLangClassForType(String type)
    {
        // Strip off any package name
        String baseType = null;
        if (type.lastIndexOf('.') < 0)
        {
            baseType = type;
        }
        else
        {
            baseType = type.substring(type.lastIndexOf('.') + 1);
        }

        // Check against our known (supported) java.lang classes
        switch (baseType) {
            case "String" :
            case "Object" :
            case "Boolean":
            case "Byte":
            case "Character":
            case "Double":
            case "Float":
            case "Integer":
            case "Long":
            case "Short":
            case "Number":
            case "StringBuffer":
            case "StringBuilder":
                return "java.lang." + baseType;
        }

        return type;
    }

    /**
     * Method to check if 2 classes are direct descendents. So one of them is a
     * superclass of the other.
     * @param clr ClassLoaderResolver for loading the classes 
     * @param class_name_1 Name of first class
     * @param class_name_2 Name of second class
     * @return Whether they are direct descendents.
     */
    public static boolean classesAreDescendents(ClassLoaderResolver clr,
                                                String class_name_1,
                                                String class_name_2)
    {
        Class class_1=clr.classForName(class_name_1);
        Class class_2=clr.classForName(class_name_2);
        if (class_1 == null || class_2 == null)
        {
            return false;
        }

        // Check for direct descendents
        if (class_1.isAssignableFrom(class_2) ||
            class_2.isAssignableFrom(class_1))
        {
            return true;
        }

        return false;
    }

    /**
     * Utility to use Reflection to dump out the details of a class.
     * Will list all superclasses, interfaces, methods and fields.
     * Can be used, for example, in checking the methods adding by the
     * enhancement process. The information is dumped out the GENERAL log.
     * @param cls The class to dump out to the log
     */
    public static void dumpClassInformation(Class cls)
    {
        NucleusLogger.GENERAL.info("----------------------------------------");
        NucleusLogger.GENERAL.info("Class Information for class " + cls.getName());

        // Superclasses
        for (Class<?> superclass : ClassUtils.getSuperclasses(cls))
        {
            NucleusLogger.GENERAL.info("    Superclass : " + superclass.getName());
        }

        // TODO Should this be calling ClassUtils.getSuperinterfaces(cls)?
        // Interfaces
        Class[] interfaces=cls.getInterfaces();
        if (interfaces != null)
        {
            for (int i=0;i<interfaces.length;i++)
            {
                NucleusLogger.GENERAL.info("    Interface : " + interfaces[i].getName());
            }
        }

        // Methods
        try
        {
            Method[] methods=cls.getDeclaredMethods();
            for (int i=0;i<methods.length;i++)
            {
                NucleusLogger.GENERAL.info("    Method : " + methods[i].toString());
                Annotation[] annots = methods[i].getAnnotations();
                if (annots != null)
                {
                    for (int j=0;j<annots.length;j++)
                    {
                        NucleusLogger.GENERAL.info("        annotation=" + annots[j]);
                    }
                }
            }
        }
        catch (Exception e)
        {
        }

        // Fields
        try
        {
            Field[] fields=cls.getDeclaredFields();
            for (int i=0;i<fields.length;i++)
            {
                NucleusLogger.GENERAL.info("    Field : " + fields[i].toString());
                Annotation[] annots = fields[i].getAnnotations();
                if (annots != null)
                {
                    for (int j=0;j<annots.length;j++)
                    {
                        NucleusLogger.GENERAL.info("        annotation=" + annots[j]);
                    }
                }
            }
        }
        catch (Exception e)
        {
        }
        NucleusLogger.GENERAL.info("----------------------------------------");
    }
    
    /**
     * Generate a JavaBeans compatible getter name
     * @param fieldName the field name
     * @param isBoolean whether the field is primitive boolean type
     * @return the getter name
     */
    public static String getJavaBeanGetterName(String fieldName, boolean isBoolean)
    {
        if (fieldName == null)
        {
            return null;
        }
        return buildJavaBeanName(isBoolean ? "is" : "get", fieldName);
    }

    /**
     * Generate a JavaBeans compatible setter name
     * @param fieldName the field name
     * @return the setter name
     */
    public static String getJavaBeanSetterName(String fieldName)
    {
        if (fieldName == null)
        {
            return null;
        }
        return buildJavaBeanName("set", fieldName);
    }

    private static String buildJavaBeanName(String prefix, String fieldName)
    {
        int prefixLength = prefix.length();
        StringBuilder sb = new StringBuilder(prefixLength + fieldName.length());
        sb.append(prefix);
        sb.append(fieldName);
        sb.setCharAt(prefixLength, Character.toUpperCase(sb.charAt(prefixLength)));
        return sb.toString();
    }

    /**
     * Generate a field name for JavaBeans compatible getter method
     * @param methodName the method name
     * @return the field name
     */
    public static String getFieldNameForJavaBeanGetter(String methodName)
    {
        if (methodName == null)
        {
            return null;
        }
        if (methodName.startsWith("get"))
        {
            return truncateJavaBeanMethodName(methodName, 3);
        }
        else if (methodName.startsWith("is"))
        {
            return truncateJavaBeanMethodName(methodName, 2);
        }
        return null;
    }

    /**
     * Generate a field name for JavaBeans compatible setter method
     * @param methodName the method name
     * @return the field name
     */
    public static String getFieldNameForJavaBeanSetter(String methodName)
    {
        if (methodName == null)
        {
            return null;
        }
        if (methodName.startsWith("set"))
        {
            return truncateJavaBeanMethodName(methodName, 3);
        }
        return null;
    }

    private static String truncateJavaBeanMethodName(String methodName, int prefixLength)
    {
        if (methodName.length() <= prefixLength)
        {
            return null;
        }
        methodName = methodName.substring(prefixLength);
        if (methodName.length() == 1)
        {
            return methodName.toLowerCase();
        }
        char firstChar = methodName.charAt(0);
        if (Character.isUpperCase(firstChar) && Character.isLowerCase(methodName.charAt(1)))
        {
            return Character.toLowerCase(firstChar) + methodName.substring(1);
        }
        // If capitalised name (e.g URL), don't lowercase first character
        return methodName;
    }

    /**
     * Convenience method to try to find the class name stored in the specified file.
     * Moves along the file, chopping off components from the front until it finds a class
     * in the class path. This is typically used as a fallback after trying the method after this.
     * @param fileName Name of file
     * @param clr ClassLoader resolver
     * @return The class name (if found)
     */
    public static String getClassNameForFileName(final String fileName, ClassLoaderResolver clr)
    {
        if (!fileName.endsWith(".class"))
        {
            // We only support class files
            return null;
        }

        // Omit ".class"
        String name = fileName.substring(0, fileName.length()-6);
        name = name.replace(File.separatorChar, '.');

        while (name.indexOf(".") >= 0)
        {
            String className = name.substring(name.indexOf('.') + 1);
            try
            {
                Class cls = clr.classForName(className);
                if (cls != null)
                {
                    return className;
                }
            }
            catch (ClassNotResolvedException cnre)
            {
            }
            name = className;
        }
        return null;
    }

    /**
     * Utility to find the class name of a class given the absolute file name of its class file.
     * Creates a loader and loads the class directly to find it.
     * @param fileURL URL for the class file
     * @return The name of the class
     * @throws ClassNotFoundException Thrown when the file is not found
     */
    public static String getClassNameForFileURL(final URL fileURL)
    throws ClassNotFoundException
    {
        ClassLoader loader = (ClassLoader) AccessController.doPrivileged(
            new PrivilegedAction()
            {
                public Object run()
                {
                    return new ClassLoader()
                    {
                        protected Class findClass(String name) throws ClassNotFoundException
                        {
                            // Always load the file
                            InputStream in = null;
                            try
                            {
                                in = new BufferedInputStream(fileURL.openStream());
                                ByteArrayOutputStream byteStr = new ByteArrayOutputStream();
                                int byt = -1;
                                while ((byt = in.read()) != -1)
                                {
                                    byteStr.write(byt);
                                }
                                byte byteArr[] = byteStr.toByteArray();
                                return defineClass(null, byteArr, 0, byteArr.length);
                            }
                            catch (final RuntimeException rex)
                            {
                                throw rex;
                            }
                            catch (final Exception ex)
                            {
                                throw new ClassNotFoundException(name);
                            }
                            finally
                            {
                                if (in != null)
                                {
                                    try
                                    {
                                        in.close();
                                    }
                                    catch (final IOException ioe)
                                    {
                                    }
                                }
                            }
                        }
                    };
                }
            });
        // TODO This can fail if the specified class implements inner interface see NUCCORE-632
        Class cls = loader.loadClass("garbage"); // The passed in name is not of relevance since using URL
        return (cls != null ? cls.getName() : null);
    }

    /**
     * Utility to return the package name for a class.
     * Allows for the result of class.getPackage() being null.
     * @param cls The class
     * @return The name of its package (or null if no package e.g a primitive)
     */
    public static String getPackageNameForClass(Class cls)
    {
        // Check getPackage and use that if specified.
        if (cls.getPackage() != null)
        {
            return cls.getPackage().getName();
        }
        int separator = cls.getName().lastIndexOf('.');
        if (separator < 0)
        {
            return null;
        }
        return cls.getName().substring(0, separator);
    }

    /**
     * Utility to return the package name for a class name.
     * @param clsName The class
     * @return The name of its package (or null if no package e.g a primitive)
     */
    public static String getPackageNameForClassName(String clsName)
    {
        // Check getPackage and use that if specified.
        int separator = clsName.lastIndexOf('.');
        if (separator < 0)
        {
            return null;
        }
        return clsName.substring(0, separator);
    }

    /**
     * Utility to return the class name without the package name for a class.
     * @param cls The class
     * @return The name of the class without its package
     */
    public static String getClassNameForClass(Class cls)
    {
        // Just strip off all up to the last separator since Class.getPackage is unreliable
        int separator = cls.getName().lastIndexOf('.');
        if (separator < 0)
        {
            return cls.getName();
        }
        return cls.getName().substring(separator+1);
    }

    /**
     * Convenience method to attempt to return the class for the provided generic type.
     * @param genericType The generic type
     * @param pos The position of the generic arg (in case of multiple)
     * @return The class (if determinable), or null
     */
    public static Class getClassForGenericType(Type genericType, int pos)
    {
        if (genericType instanceof ParameterizedType)
        {
            ParameterizedType paramtype = (ParameterizedType)genericType;
            if (paramtype.getActualTypeArguments().length > pos)
            {
                Type argType = paramtype.getActualTypeArguments()[pos];
                if (argType instanceof Class)
                {
                    return (Class) argType;
                }
                else if (argType instanceof ParameterizedType)
                {
                    return (Class) ((ParameterizedType)argType).getRawType();
                }
                else if (argType instanceof GenericArrayType)
                {
                    // Create array of zero length to get class of array type (is there a better way?)
                    Type cmptType = ((GenericArrayType)argType).getGenericComponentType();
                    return Array.newInstance((Class)cmptType, 0).getClass();
                }
            }
        }
        return null;
    }

    /**
     * Convenience method to extract the element type of a collection when using JDK1.5 generics given the
     * input field.
     * @param field The field
     * @return The name of the element class
     */
    public static String getCollectionElementType(Field field)
    {
        Class elementType = getCollectionElementType(field.getType(), field.getGenericType());
        return (elementType != null ? elementType.getName() : null);
    }
    
    /**
     * Convenience method to extract the element type of a collection when using JDK1.5 generics, given
     * the input method (getter).
     * @param method The method
     * @return The name of the element class
     */
    public static String getCollectionElementType(Method method)
    {
        Class elementType = getCollectionElementType(method.getReturnType(), method.getGenericReturnType());
        return (elementType != null ? elementType.getName() : null);
    }

    /**
     * Convenience method to extract the element type of a collection when using JDK1.5 generics given the
     * input field.
     * @param type the field type
     * @param genericType the generic type
     * @return The element class
     */
    public static Class getCollectionElementType(Class type, Type genericType)
    {
        return getClassForGenericType(genericType, 0);
    }

    /**
     * Convenience method to extract the key type of a map when using JDK1.5 generics given the
     * input field.
     * @param field The field
     * @return The name of the key class
     */
    public static String getMapKeyType(Field field)
    {
        Class keyType = getMapKeyType(field.getType(), field.getGenericType());
        return (keyType != null ? keyType.getName() : null);
    }

    /**
     * Convenience method to extract the key type of a map when using JDK1.5 generics given the
     * input method.
     * @param method The method
     * @return The name of the key class
     */
    public static String getMapKeyType(Method method)
    {
        Class keyType = getMapKeyType(method.getReturnType(), method.getGenericReturnType());
        return (keyType != null ? keyType.getName() : null);
    }

    /**
     * Convenience method to extract the key type of a map when using JDK1.5 generics given the
     * input field.
     * @param type the field type
     * @param genericType the generic type
     * @return The name of the key class
     */
    public static Class getMapKeyType(Class type, Type genericType)
    {
        if (!Map.class.isAssignableFrom(type))
        {
            return null;
        }

        return getClassForGenericType(genericType, 0);
    }    

    /**
     * Convenience method to extract the value type of a map when using JDK1.5 generics given the
     * input field
     * @param field The field
     * @return The name of the value class
     */
    public static String getMapValueType(Field field)
    {
        Class valueType = getMapValueType(field.getType(), field.getGenericType());
        return (valueType != null ? valueType.getName() : null);
    }

    /**
     * Convenience method to extract the value type of a map when using JDK1.5 generics given the
     * input method.
     * @param method The method
     * @return The name of the value class
     */
    public static String getMapValueType(Method method)
    {
        Class valueType = getMapValueType(method.getReturnType(), method.getGenericReturnType());
        return (valueType != null ? valueType.getName() : null);
    }

    /**
     * Convenience method to extract the value type of a map when using JDK1.5 generics given the
     * input field
     * @param type the field type
     * @param genericType the generic type
     * @return The name of the value class
     */
    public static Class getMapValueType(Class type, Type genericType)
    {
        if (!Map.class.isAssignableFrom(type))
        {
            return null;
        }

        return getClassForGenericType(genericType, 1);
    }

    /**
     * Convenience accessor for the modifiers of a field in a class.
     * @param clr ClassLoader resolver
     * @param className Name of the class
     * @param fieldName Name of the field
     * @return The modifiers
     */
    public static int getModifiersForFieldOfClass(ClassLoaderResolver clr, String className, String fieldName)
    {
        try
        {
            Class cls = clr.classForName(className);
            Field fld = cls.getDeclaredField(fieldName);
            return fld.getModifiers();
        }
        catch (Exception e)
        {
            // Class or field not found
        }
        return -1;
    }

    /**
     * Method to return whether the passes type is a "reference" type.
     * A "reference" type is either an interface or an Object, so can be used as a reference to
     * a persistable object.
     * @param cls The type
     * @return Whether it is a reference type
     */
    public static boolean isReferenceType(Class cls)
    {
        if (cls == null)
        {
            return false;
        }
        return cls.isInterface() || cls.getName().equals("java.lang.Object");
    }

    /**
     * Convenience method to say whether a class is present.
     * @param className The class name
     * @param clr ClassLoader resolver
     * @return Whether it is present
     */
    public static boolean isClassPresent(String className, ClassLoaderResolver clr)
    {
        try
        {
            clr.classForName(className);
            return true;
        }
        catch (ClassNotResolvedException cnre)
        {
            return false;
        }
    }

    /**
     * Convenience method to throw a NucleusUserException if the specified class is not loadable 
     * from the ClassLoaderResolver.
     * @param clr ClassLoader resolver
     * @param className Name of the class
     * @param jarName Name of the jar containing the class
     * @throws NucleusUserException if the class is not found
     */
    public static void assertClassForJarExistsInClasspath(ClassLoaderResolver clr, String className,
            String jarName)
    {
        try
        {
            Class cls = clr.classForName(className);
            if (cls == null)
            {
                throw new NucleusUserException(Localiser.msg("001006", className, jarName));
            }
        }
        catch (Error err)
        {
            throw new NucleusUserException(Localiser.msg("001006", className, jarName));
        }
        catch (ClassNotResolvedException cnre)
        {
            throw new NucleusUserException(Localiser.msg("001006", className, jarName));
        }
    }

    /**
     * Convenience method to return if a String array contains a value.
     * @param array The String array
     * @param value The value
     * @return Whether it is contained
     */
    public static boolean stringArrayContainsValue(String[] array, String value)
    {
        if (value == null || array == null)
        {
            return false;
        }
        for (int i=0;i<array.length;i++)
        {
            if (value.equals(array[i]))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to return the value returned by a method on an object using reflection.
     * @param object The object
     * @param methodName Name of the method
     * @param args The arguments
     * @return The value
     */
    public static Object getValueOfMethodByReflection(Object object, String methodName, Object... args)
    {
        Object methodValue;
        if (object == null)
        {
            return null;
        }

        final Method method = getDeclaredMethodPrivileged(object.getClass(), methodName);
        if (method == null)
        {
            throw new NucleusUserException("Cannot access method: " + methodName + " in type " + object.getClass());
        }

        try
        {
            // if the method is not accessible, try to set the accessible flag.
            if (!method.isAccessible())
            {
                try
                {
                    AccessController.doPrivileged(
                        new PrivilegedAction()
                        {
                            public Object run()
                            {
                                method.setAccessible(true);
                                return null;
                            }
                        });
                }
                catch (SecurityException ex)
                {
                    throw new NucleusException("Cannot access method: " + methodName, ex).setFatal();
                }
            }
            methodValue = method.invoke(object, args);
        }
        catch (InvocationTargetException e2)
        {
            throw new NucleusUserException("Cannot access method: " + methodName, e2);
        }
        catch (IllegalArgumentException e2)
        {
            throw new NucleusUserException("Cannot access method: " + methodName, e2);
        }
        catch (IllegalAccessException e2)
        {
            throw new NucleusUserException("Cannot access method: " + methodName, e2);
        }
        return methodValue;
    }

    /**
     * Helper method to return the value of a field of an object using reflection.
     * @param object The object
     * @param fieldName Name of the field
     * @return The value
     */
    public static Object getValueOfFieldByReflection(Object object, String fieldName)
    {
        Object fieldValue;
        if (object == null)
        {
            return null;
        }

        final Field field = getDeclaredFieldPrivileged(object.getClass(), fieldName);
        if (field == null)
        {
            throw new NucleusUserException("Cannot access field: " + fieldName + " in type " + object.getClass());
        }

        try
        {
            // if the field is not accessible, try to set the accessible flag.
            if (!field.isAccessible())
            {
                try
                {
                    AccessController.doPrivileged(
                        new PrivilegedAction()
                        {
                            public Object run()
                            {
                                field.setAccessible(true);
                                return null;
                            }
                        });
                }
                catch (SecurityException ex)
                {
                    throw new NucleusException("Cannot access field: " + fieldName,ex).setFatal();
                }
            }
            fieldValue = field.get(object);
        }
        catch (IllegalArgumentException e2)
        {
            throw new NucleusUserException("Cannot access field: " + fieldName,e2);
        }
        catch (IllegalAccessException e2)
        {
            throw new NucleusUserException("Cannot access field: " + fieldName,e2);
        }
        return fieldValue;
    }

    /**
     * Helper method to retrieve the java.lang.reflect.Field
     * @param clazz the Class instance of the declaring class or interface
     * @param fieldName the field name
     * @return The field
     */
    private static Field getDeclaredFieldPrivileged(final Class clazz, final String fieldName)
    {
        if ((clazz == null) || (fieldName == null))
        {
            return null;
        }

        return (Field) AccessController.doPrivileged(
            new PrivilegedAction()
            {
                public Object run ()
                {
                    Class seekingClass = clazz;
                    do
                    {
                        try
                        {
                            return seekingClass.getDeclaredField(fieldName);
                        }
                        catch (SecurityException ex)
                        {
                            throw new NucleusException("CannotGetDeclaredField",ex).setFatal();
                        }
                        catch (NoSuchFieldException ex)
                        {
                            // do nothing, we will return null later if no 
                            // field is found in this class or superclasses
                        }
                        catch (LinkageError ex)
                        {
                            throw new NucleusException("ClassLoadingError",ex).setFatal();
                        }
                        seekingClass = seekingClass.getSuperclass();
                    } while(seekingClass != null);

                    //no field found
                    return null;
                }
            });
    }

    /**
     * Helper method to retrieve the java.lang.reflect.Method
     * @param clazz the Class instance of the declaring class or interface
     * @param methodName the method name
     * @param argTypes Types of the arguments to this method
     * @return The method
     */
    private static Method getDeclaredMethodPrivileged(final Class clazz, final String methodName, final Class... argTypes)
    {
        if (clazz == null || methodName == null)
        {
            return null;
        }

        return (Method) AccessController.doPrivileged(
            new PrivilegedAction()
            {
                public Object run ()
                {
                    Class seekingClass = clazz;
                    do
                    {
                        try
                        {
                            return seekingClass.getDeclaredMethod(methodName, argTypes);
                        }
                        catch (SecurityException ex)
                        {
                            throw new NucleusException("Cannot get declared method " + methodName, ex).setFatal();
                        }
                        catch (NoSuchMethodException ex)
                        {
                            // do nothing, we will return null later if no 
                            // method is found in this class or superclasses
                        }
                        catch (LinkageError ex)
                        {
                            throw new NucleusException("ClassLoadingError",ex).setFatal();
                        }
                        seekingClass = seekingClass.getSuperclass();
                    } while(seekingClass != null);

                    //no method found
                    return null;
                }
            });
    }

    /**
     * Method to find the value of a field of the provided user-defined identity.
     * The field has to be either accessible, or have a Java-bean style getter.
     * @param id The identity
     * @param fieldName Name of the field
     * @return The value of the field in this identity
     * @throws NucleusUserException if not possible to get the value
     */
    public static Object getValueForIdentityField(Object id, String fieldName)
    {
        // Try Java-bean getter if present
        String getterName = ClassUtils.getJavaBeanGetterName(fieldName, false);
        try
        {
            return ClassUtils.getValueOfMethodByReflection(id, getterName);
        }
        catch (NucleusException ne)
        {
            // No getter method, or not accessible
        }

        // Try field
        try
        {
            return ClassUtils.getValueOfFieldByReflection(id, fieldName);
        }
        catch (NucleusException ne)
        {
            // No field, or not accessible
        }

        throw new NucleusUserException("Not possible to get value of field " + fieldName + " from identity " + id);
    }

    /**
     * Method that returns the class type of a member of the specified class (if present).
     * The member can either be a field, or a Java bean name (for a getter).
     * @param cls The class
     * @param memberName The member name
     * @return The member class (or null if not present)
     */
    public static Class getClassForMemberOfClass(Class cls, String memberName)
    {
        // Try as a Field
        Field fld = ClassUtils.getFieldForClass(cls, memberName);
        if (fld != null)
        {
            return fld.getType();
        }

        // Try as bean getter
        Method method = ClassUtils.getGetterMethodForClass(cls, memberName);
        if (method != null)
        {
            return method.getReturnType();
        }

        return null;
    }

    /**
     * Convenience method to return if the supplied method is a valid java bean getter method.
     * Checks that it is not static, that its name starts "get" or "is", that it has a return type
     * and that it takes no arguments.
     * @param method The method
     * @return Whether this method is a java bean getter
     */
    public static boolean isJavaBeanGetterMethod(Method method)
    {
        if (Modifier.isStatic(method.getModifiers()))
        {
            return false;
        }
        if (!method.getName().startsWith("get") && !method.getName().startsWith("is"))
        {
            return false;
        }
        else if (method.getName().startsWith("get") && method.getName().length() == 3)
        {
            return false;
        }
        else if (method.getName().startsWith("is") && method.getName().length() == 2)
        {
            return false;
        }
        if (method.getReturnType() == null)
        {
            return false;
        }
        if (method.getParameterTypes() == null || method.getParameterTypes().length == 0)
        {
            return true;
        }
        return false;
    }

    /**
     * Utility to clear the supplied flags.
     * @param flags The flags array to clear
     */
    public static void clearFlags(boolean[] flags)
    {
        for (int i = 0; i < flags.length; i++)
        {
            flags[i] = false;
        }
    }

    /**
     * Utility to clear the supplied flags.
     * @param flags Flags to clear
     * @param fields fields numbers where the flags will be cleared
     */
    public static void clearFlags(boolean[] flags, int[] fields)
    {
        for (int i = 0; i < fields.length; i++)
        {
            flags[fields[i]] = false;
        }
    }

    /**
     * Returns an array of integers containing the indices of all elements in
     * <tt>flags</tt> that are in the <tt>state</tt> passed as argument.
     * @param flags Array of flags (true or false)
     * @param state The state to search (true or false)
     * @return The settings of the flags
     */
    public static int[] getFlagsSetTo(boolean[] flags, boolean state)
    {
        int[] temp = new int[flags.length];
        int j = 0;
        for (int i = 0; i < flags.length; i++)
        {
            if (flags[i] == state)
            {
                temp[j++] = i;
            }
        }

        if (j != 0)
        {
            int[] fieldNumbers = new int[j];
            System.arraycopy(temp, 0, fieldNumbers, 0, j);

            return fieldNumbers;
        }
        return null;
    }

    /**
     * Returns an array of integers containing the indices of all elements in
     * <tt>flags</tt> whose index occurs in <tt>indices</tt> and whose value is <tt>state</tt>.
     * @param flags the boolean array
     * @param indices The positions in the array
     * @param state The state that we want to match
     * @return The positions of flags that are set to this state
     */
    public static int[] getFlagsSetTo(boolean[] flags, int[] indices, boolean state)
    {
        if (indices == null)
        {
            return null;
        }

        int[] temp = new int[indices.length];
        int j = 0;

        for (int i = 0; i < indices.length; i++)
        {
            if (flags[indices[i]] == state)
            {
                temp[j++] = indices[i];
            }
        }

        if (j != 0)
        {
            int[] fieldNumbers = new int[j];
            System.arraycopy(temp, 0, fieldNumbers, 0, j);

            return fieldNumbers;
        }
        return null;
    }

    /**
     * Convenience method to get the value of a bit from an int when we are storing (up to 32) boolean in an int
     * for memory utilisation purposes.
     * @param bits The int storing the bits
     * @param bitIndex The index of this bit
     * @return The value of this bit (as a boolean)
     */
    public static boolean getBitFromInt(int bits, int bitIndex)
    {
        if (bitIndex < 0 || bitIndex > 31) 
        {
            throw new IllegalArgumentException();
        }
        return (bits & (1 << bitIndex)) != 0;
    }

    /**
     * Convenience method to set a boolean as a bit in the specified int, for memory utilisation purposes.
     * @param bits The int storing the bits
     * @param bitIndex The index of this bit
     * @param flag The boolean value to store
     * @return The int with this bit set
     */
    public static int setBitInInt(int bits, int bitIndex, boolean flag)
    {
        if (bitIndex < 0 || bitIndex > 31) 
        {
            throw new IllegalArgumentException();
        }
        int mask = 1 << bitIndex;
        return (bits & ~mask) | (flag ? mask : 0);
    }
}