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
package org.datanucleus.metadata;

import java.util.ArrayList;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * The implements element declares a persistence-capable interface implemented by the 
 * persistence-capable class that contains this element. An extent of persistence-capable 
 * classes that implement this interface is managed by the JDO implementation. The extent 
 * can be used for queries or for iteration just like an extent of persistence-capable 
 * instances. The attribute name is required, and is the name of the interface. The java 
 * class naming rules apply: if the interface name is unqualified, the package is the name 
 * of the enclosing package.
 */
public class ImplementsMetaData extends MetaData
{
    private static final long serialVersionUID = -9035890748184431024L;

    /** Name of the interface implemented. */
    protected String name;

    /** Properties implemented. */
    protected final List<PropertyMetaData> properties = new ArrayList();

    /**
     * Constructor.
     * @param name Name of the interface being implemented
     */
    public ImplementsMetaData(final String name)
    {
        this.name = name;
    }

    /**
     * Method to populate the details of the implements.
     * @param clr ClassLoaderResolver to use in loading any classes
     * @param primary the primary ClassLoader to use (or null)
     * @param mmgr MetaData manager
     */
    public synchronized void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr)
    {
        // Check the class that we're modelling exists
        try
        {
            clr.classForName(name);
        }
        catch (ClassNotResolvedException cnre)
        {
            try
            {
                // Try with prefix package of the owning class
                String clsName = ClassUtils.createFullClassName(((ClassMetaData)parent).getPackageName(), name);
                clr.classForName(clsName);
                name = clsName;
            }
            catch (ClassNotResolvedException cnre2)
            {
                NucleusLogger.METADATA.error(Localiser.msg("044097", ((ClassMetaData)parent).getFullClassName(), name));
                throw new InvalidClassMetaDataException("044097", ((ClassMetaData)parent).getFullClassName(), name);
            }
        }
        setPopulated();
    }

    /**
     * Accessor for name.
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Method to add a property to this interface.
     * @param pmd Property
     */
    public void addProperty(PropertyMetaData pmd)
    {
        if (pmd == null)
        {
            return;
        }
        
        properties.add(pmd);
        pmd.parent = this;
    }
    
    // ------------------------------ Utilities --------------------------------

    /**
     * Returns a string representation of the object.
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<implements name=\"" + name + "\">\n");

        // Add properties
        for (int i=0;i<properties.size();i++)
        {
            PropertyMetaData pmd = properties.get(i);
            sb.append(pmd.toString(prefix + indent, indent));
        }

        // Add extensions
        sb.append(super.toString(prefix + indent, indent)); 

        sb.append(prefix + "</implements>\n");
        return sb.toString();
    }
}