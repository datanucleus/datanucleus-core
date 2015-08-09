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
package org.datanucleus.metadata;

import java.io.Serializable;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.StringUtils;

/**
 * Representation of the details of an object stored in a container.
 * This can be an element in a collection/array, or the key/value of a Map.
 */
class ContainerComponent implements Serializable
{
    private static final long serialVersionUID = -5662004381416396246L;

    /** Whether the component is stored embedded. */
    protected Boolean embedded;

    /** Whether the component is stored serialised. */
    protected Boolean serialized;

    /** Whether the component is dependent on the container (i.e should be deleted with the container). */
    protected Boolean dependent;

    /** Type of the component. */
    protected String type;

    /** ClassMetaData for the component. */
    protected AbstractClassMetaData classMetaData;

    /**
     * Default constructor. Set fields using setters, before populate().
     */
    public ContainerComponent()
    {
        // Do nothing
    }

    public Boolean getEmbedded()
    {
        return embedded;
    }

    public void setEmbedded(Boolean embedded)
    {
        this.embedded = embedded;
    }

    public Boolean getSerialized()
    {
        return serialized;
    }

    public void setSerialized(Boolean serialized)
    {
        this.serialized = serialized;
    }

    public Boolean getDependent()
    {
        return dependent;
    }

    public void setDependent(Boolean dependent)
    {
        this.dependent = dependent;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = (StringUtils.isWhitespace(type) ? null : type);
    }

    /**
     * Method to update the "type" field to cater for it maybe being in the same package as the
     * owning class, or being in java.lang as per JDO spec rules.
     * @param packageName The package of the owning class
     * @param clr ClassLoader resolver
     * @param primary Primary class loader
     * @param mmgr MetaData manager
     */
    void populate(String packageName, ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr)
    {
        if (type == null)
        {
            // Do nothing
        }
        else if (ClassUtils.isPrimitiveArrayType(type))
        {
            // Do nothing since valid
        }
        else if (ClassUtils.isPrimitiveType(type))
        {
            // Do nothing since valid
        }
        else
        {
            // Make sure it is resolved
            try
            {
                clr.classForName(type, primary, false);
            }
            catch (ClassNotResolvedException cnre)
            {
                // Type is invalid so try with parent package
                String name = ClassUtils.createFullClassName(packageName, type);
                try
                {
                    clr.classForName(name, primary, false);
                    type = name; // Update to be in parent package
                }
                catch (ClassNotResolvedException cnre2)
                {
                    // Type is invalid so try as java.lang
                    name = ClassUtils.getJavaLangClassForType(type);
                    clr.classForName(name, primary, false);
                    type = name; // Update to be in java.lang
                }
            }
        }
    }

    public String toString()
    {
        return "Type=" + type + " embedded=" + embedded + " serialized=" + serialized + " dependent=" + dependent + 
            " cmd=" + classMetaData;
    }
}