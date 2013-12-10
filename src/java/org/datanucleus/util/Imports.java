/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved.
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
2003 Andy Jefferson - commented
    ...
**********************************************************************/
package org.datanucleus.util;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusUserException;

/**
 * Utility class handling Imports.
 */
public class Imports
{
    private static final Localiser LOCALISER=Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    private HashMap primitives = new HashMap();
    private HashMap importedClassesByName = new HashMap();
    private HashSet importedPackageNames = new HashSet();

    /**
     * Constructor.
     **/
    public Imports()
    {
        //imported automatically into queries
        primitives.put("boolean", boolean.class);
        primitives.put("byte", byte.class);
        primitives.put("char", char.class);
        primitives.put("short", short.class);
        primitives.put("int", int.class);
        primitives.put("long", long.class);
        primitives.put("float", float.class);
        primitives.put("double", double.class);
        importedPackageNames.add("java.lang");
    }

    /**
     * Method to import the package given by the specified class.
     * @param className The class name
     **/
    public void importPackage(String className)
    {
        int lastDot = className.lastIndexOf('.');

        if (lastDot > 0)
        {
            importedPackageNames.add(className.substring(0, lastDot));
        }
    }

    /**
     * Method to import the specified class.
     * @param className Class to import
     */
    public void importClass(String className)
    {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0)
        {
            importedClassesByName.put(className.substring(lastDot+1), className);
        }
    }

    /**
     * Utility to parse the imports.
     * @param imports The Imports string
     * @throws NucleusUserException when finding an invalid declaration
     */
    public void parseImports(String imports)
    {
        StringTokenizer t1 = new StringTokenizer(imports, ";");

        while (t1.hasMoreTokens())
        {
            String importDecl = t1.nextToken().trim();

            if (importDecl.length() == 0 && !t1.hasMoreTokens())
            {
                break;
            }

            StringTokenizer t2 = new StringTokenizer(importDecl, " ");

            if (t2.countTokens() != 2 || !t2.nextToken().equals("import"))
            {
                throw new NucleusUserException(LOCALISER.msg("021002",importDecl));
            }

            String importName = t2.nextToken();
            int lastDot = importName.lastIndexOf(".");
            String lastPart = importName.substring(lastDot + 1);

            if (lastPart.equals("*"))
            {
                if (lastDot < 1)
                {
                    throw new NucleusUserException(LOCALISER.msg("021003",importName));
                }

                importedPackageNames.add(importName.substring(0, lastDot));
            }
            else
            {
                if (importedClassesByName.put(lastPart, importName) != null)
                {
                    // Duplicated imports are valid (see spec 14.4 "declareImports"), so just log it for info
                    NucleusLogger.QUERY.info(LOCALISER.msg("021004", importName));
                }
            }
        }
    }

    /**
     * Utility to resolve a class declaration.
     * @param classDecl The class declaration
     * @param clr ClassLoaderResolver
     * @param primaryClassLoader The primary ClassLoader for the class 
     * @return The class 
     * @throws ClassNotResolvedException
     * @throws NucleusUserException if a type is duplicately defined
     **/
    public Class resolveClassDeclaration(String classDecl, ClassLoaderResolver clr, ClassLoader primaryClassLoader)
    {
        boolean isArray = classDecl.indexOf('[') >= 0;
        if (isArray)
        {
            classDecl = classDecl.substring(0, classDecl.indexOf('['));
        }

        Class c;
        if (classDecl.indexOf('.') < 0)
        {
            c = (Class)primitives.get(classDecl);
            if (c == null)
            {
                String cd = (String)importedClassesByName.get(classDecl);
                if (cd != null)
                {
                    c = clr.classForName(cd, primaryClassLoader);
                }
            }

            if (c == null)
            {
                Iterator packageNames = importedPackageNames.iterator();
                while (packageNames.hasNext())
                {
                    String packageName = (String)packageNames.next();

                    try
                    {
                        Class c1 = clr.classForName(packageName + '.' + classDecl, primaryClassLoader);
                        if (c != null && c1 != null)
                        {
                            // Duplicate definition of type
                            throw new NucleusUserException(LOCALISER.msg("021008",c.getName(),c1.getName()));
                        }

                        c = c1;
                    }
                    catch (ClassNotResolvedException e)
                    {
                        // Do nothing
                    }
                }

                if (c == null)
                {
                    throw new ClassNotResolvedException(classDecl);
                }

                if (NucleusLogger.GENERAL.isDebugEnabled())
                {
                    NucleusLogger.GENERAL.debug(LOCALISER.msg("021010", classDecl, c.getName()));
                }
            }
        }
        else
        {
            c = clr.classForName(classDecl, primaryClassLoader);
        }

        if (isArray)
        {
            c = Array.newInstance(c, 0).getClass();
        }

        return c;
    }
}