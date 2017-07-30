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
    ...
**********************************************************************/
package org.datanucleus.enhancer;

import java.net.URL;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;

/**
 * ClassLoader for newly defined classes. Parent classloader is the context classloader
 * obtained during instantiation of this class
 */
public final class EnhancerClassLoader extends ClassLoader
{
    /** Delegate ClassLoaderResolver may be used during class definition if the class has links to other classes. */
    ClassLoaderResolver delegate;

    /** flag to avoid reentrant invocations to loading operations **/
    boolean loadingClass = false;

    /** flag to avoid reentrant invocations to loading operations **/
    boolean loadingResource = false;

    public EnhancerClassLoader()
    {
        super(Thread.currentThread().getContextClassLoader());
    }

    public EnhancerClassLoader(ClassLoaderResolver iDelegate)
    {
        super();
        this.delegate = iDelegate;
    }
    
    /**
     * Define a class in this ClassLoader.
     * @param fullClassName the class name
     * @param bytes the bytes representation of the class
     * @param clr the ClassLoaderResolver to load linked classes
     */
    public synchronized void defineClass(String fullClassName, byte[] bytes, ClassLoaderResolver clr)
    {
    	ClassLoaderResolver oldDelegate = this.delegate; 
        this.delegate = clr;
        try
        {
            defineClass(fullClassName, bytes, 0, bytes.length);
        }
        finally
        {
            this.delegate = oldDelegate;
        }
    }

    /**
     * Overwrite to have an opportunity to load classes from the delegate ClassLoaderResolver
     */
    public synchronized Class loadClass(String name) throws ClassNotFoundException
    {
        //avoid reentrant calls
        if (loadingClass)
        {
            throw new ClassNotFoundException("Class " + name + " not found");
        }
        loadingClass = true;

        try
        {
            // Load from delegate if found, otherwise fallback
        	if (delegate != null)
        	{
        	    try
        	    {
        	        return delegate.classForName(name);
        	    }
        	    catch (ClassNotResolvedException cnrex)
        	    {
        	        throw new ClassNotFoundException(cnrex.toString(), cnrex);
        	    }
        	}
        	return super.loadClass(name);
        }
        catch (ClassNotFoundException ex)
        {
            // Can't see how this can be useful, since tried both delegate already
            if (delegate != null)
            {
                try
                {
                    return delegate.classForName(name);
                }
                catch (ClassNotResolvedException cnrex)
                {
                    throw new ClassNotFoundException(cnrex.toString(), cnrex);
                }
            }
            throw ex;
        }
        finally
        {
            loadingClass = false;
        }
    }

    /**
     * Overwrite to have an opportunity to find resources from the delegate ClassLoaderResolver
     */
    protected synchronized URL findResource(String name)
    {
        //avoid reentrant calls
        if (loadingResource)
        {
            return null;
        }
        loadingResource = true;
        try
        {
            URL url = super.findResource(name);
            if (url == null)
            {
                if (delegate != null)
                {
                    url = delegate.getResource(name, null);
                }
            }
            return url;
        }
        finally
        {
            loadingResource = false;
        }
    }
}