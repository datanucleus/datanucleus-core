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
2005 Andy Jefferson - addition of states
2007 Andy Jefferson - moved extensions to this class, javadocs
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.Localiser;

/**
 * Base class for all MetaData.
 * <h3>MetaData Lifecycle</h3>
 * The states represent the lifecycle of a MetaData object. The lifecycle goes as follows :
 * <OL>
 * <LI>MetaData object is created (values passed in from a parsed file, or manually generated)</LI>
 * <LI>MetaData object is populated (maybe pass in a class that it represents, creating any additional 
 * information that wasn't in the initial data).</LI>
 * <LI>MetaData object is initialised (any internal arrays are set up, and additions of data is blocked 
 * from this point).
 * <LI>MetaData object is added to with runtime information like actual column names and types in use.</LI> 
 * </OL>
 * <h3>MetaData Extensability</h3>
 * <p>
 * All MetaData elements are extensible with extensions for a "vendor-name". Extensions take the form
 * of a key and a value.
 */
public class MetaData implements Serializable
{
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** State representing the start state of MetaData, representing the initial values passed in. */
    public static final int METADATA_CREATED_STATE = 0;

    /** State reflecting that MetaData has been populated with real class definition adding any defaulted info. */
    public static final int METADATA_POPULATED_STATE = 1;

    /** State reflecting that MetaData object has been initialised with any internal info required. */
    public static final int METADATA_INITIALISED_STATE = 2;

    /** State reflecting that MetaData object has been modified with usage information (e.g defaulted column names). */
    public static final int METADATA_USED_STATE = 3;

    /** State of the MetaData. */
    protected int metaDataState = METADATA_CREATED_STATE;

    /** Parent MetaData object, allowing hierarchical MetaData structure. */
    protected MetaData parent;

    /** Vendor name (DataNucleus) used for extensions. */
    public static final String VENDOR_NAME = "datanucleus";

    /** Vendor name (JPOX) used for extensions. */
    public static final String VENDOR_NAME_OLD = "jpox";

    /** List of extensions for this MetaData element. */
    protected Collection<ExtensionMetaData> extensions = null;

    public MetaData()
    {
    }

    /**
     * Constructor. Taking the parent MetaData object (if any).
     * @param parent The parent MetaData object.
     */
    public MetaData(MetaData parent)
    {
        this.parent = parent;
    }

    /**
     * Copy constructor. Taking the parent MetaData object, and an object to copy from.
     * @param parent The parent MetaData object.
     * @param copy The metadata to copy from
     */
    public MetaData(MetaData parent, MetaData copy)
    {
        this.parent = parent;
        if (copy != null && copy.extensions != null)
        {
            Iterator<ExtensionMetaData> extIter = copy.extensions.iterator();
            while (extIter.hasNext())
            {
                ExtensionMetaData extmd = extIter.next();
                addExtension(extmd.getVendorName(), extmd.getKey(), extmd.getValue());
            }
        }
    }

    public void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        setInitialised();
    }

    void setInitialised()
    {
        metaDataState = METADATA_INITIALISED_STATE;
    }

    void setPopulated()
    {
        metaDataState = METADATA_POPULATED_STATE;
    }

    void setUsed()
    {
        metaDataState = METADATA_USED_STATE;
    }

    public void setParent(MetaData md)
    {
        if (isPopulated() || isInitialised())
        {
            throw new NucleusException("Cannot set parent of " + this + " since it is already populated/initialised");
        }
        this.parent = md;
    }

    public MetaData addExtension(String vendor, String key, String value)
    {
        if (vendor == null ||
            (isSupportedVendor(vendor) && (key == null || value == null)))
        {
            throw new InvalidMetaDataException(LOCALISER, "044160", vendor, key, value);
        }

        if (isSupportedVendor(vendor) && hasExtension(key))
        {
            // Remove any existing value
            removeExtension(key);
        }

        if (extensions == null)
        {
            // First extensions so allocate the collection. We dont need ordering so use HashSet
            extensions = new HashSet(2);
        }
        extensions.add(new ExtensionMetaData(vendor, key, value));
        return this;
    }

    public MetaData addExtension(String key, String value)
    {
        return addExtension(VENDOR_NAME, key, value);
    }

    /**
     * Method to create a new ExtensionMetaData, add it, and return it.
     * @param vendor The vendor name
     * @param key Key of the extension
     * @param value Value
     * @return The extension
     */
    public ExtensionMetaData newExtensionMetaData(String vendor, String key, String value)
    {
        if (vendor == null || (isSupportedVendor(vendor) && (key == null || value == null)))
        {
            throw new InvalidMetaDataException(LOCALISER, "044160", vendor, key, value);
        }

        ExtensionMetaData extmd = new ExtensionMetaData(vendor, key, value);
        if (extensions == null)
        {
            extensions = new HashSet(2);
        }
        extensions.add(extmd);
        return extmd;
    }

    public MetaData removeExtension(String key)
    {
        if (extensions == null)
        {
            return this;
        }

        Iterator iter = extensions.iterator();
        while (iter.hasNext())
        {
            ExtensionMetaData ex = (ExtensionMetaData)iter.next();
            if (ex.getKey().equals(key) && isSupportedVendor(ex.getVendorName()))
            {
                iter.remove();
                break;
            }
        }
        return this;
    }

    public MetaData getParent()
    {
        return parent;
    }

    public boolean isPopulated()
    {
        return (metaDataState >= METADATA_POPULATED_STATE);
    }

    public boolean isInitialised()
    {
        return (metaDataState >= METADATA_INITIALISED_STATE);
    }

    public boolean isUsed()
    {
        return (metaDataState == METADATA_USED_STATE);
    }

    public int getNoOfExtensions()
    {
        if (extensions == null)
        {
            return 0;
        }
        return extensions.size();
    }

    public void assertIfInitialised()
    {
        if (isInitialised())
        {
            throw new NucleusException("MetaData is already initialised so attribute cannot be set.");
        }
    }

    public ExtensionMetaData[] getExtensions()
    {
        if (extensions == null || extensions.size() == 0)
        {
            return null;
        }
        return extensions.toArray(new ExtensionMetaData[extensions.size()]);
    }

    public boolean hasExtension(String key)
    {
        if (extensions == null || key == null)
        {
            return false;
        }

        Iterator iter = extensions.iterator();
        while (iter.hasNext())
        {
            ExtensionMetaData ex = (ExtensionMetaData)iter.next();
            if (ex.getKey().equals(key) && isSupportedVendor(ex.getVendorName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Accessor for the value of a particular extension.
     * @param key The key of the extension
     * @return The value of the extension (null if not existing)
     */
    public String getValueForExtension(String key)
    {
        if (extensions == null || key == null)
        {
            return null;
        }

        Iterator iter = extensions.iterator();
        while (iter.hasNext())
        {
            ExtensionMetaData ex = (ExtensionMetaData)iter.next();
            if (ex.getKey().equals(key) && isSupportedVendor(ex.getVendorName()))
            {
                return ex.getValue();
            }
        }
        return null;
    }

    /**
     * Accessor for the value of a particular extension, but
     * splitting it into separate parts. This is for extension tags that have a
     * value as comma separated.
     * @param key The key of the extension
     * @return The value(s) of the extension (null if not existing)
     */
    public String[] getValuesForExtension(String key)
    {
        if (extensions == null || key == null)
        {
            return null;
        }

        Iterator iter = extensions.iterator();
        while (iter.hasNext())
        {
            ExtensionMetaData ex = (ExtensionMetaData)iter.next();
            if (ex.getKey().equals(key) && isSupportedVendor(ex.getVendorName()))
            {
                return MetaDataUtils.getInstance().getValuesForCommaSeparatedAttribute(ex.getValue());
            }
        }
        return null;
    }

    // -------------------------------- Utilities ------------------------------
 
    /**
     * Accessor for a string representation of the object.
     * @return a string representation of the object.
     */
    public String toString()
    {
        return toString("","");
    }

    /**
     * Returns a string representation of the object.
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix, String indent)
    {
        if (extensions == null || extensions.size() == 0)
        {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        Iterator iter = extensions.iterator();
        while (iter.hasNext())
        {
            ExtensionMetaData ex = (ExtensionMetaData)iter.next();
            sb.append(prefix).append(ex.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Convenience method to return supported vendors. Allows us to support
     * datanucleus AND jpox as vendors.
     * @param vendorName Name of vendor
     * @return Whether supported
     */
    private boolean isSupportedVendor(String vendorName)
    {
        return (vendorName.equalsIgnoreCase(VENDOR_NAME) || 
                vendorName.equalsIgnoreCase(VENDOR_NAME_OLD));
    }
}