/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.plugin;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Plug-in (OSGi Bundle) definition. Represents the XML declaration
 */
public class Bundle
{
    /** unique id - bundle symbolic name * */
    final private String symbolicName;

    /** vendor name * */
    final private String vendorName;

    /** name * */
    final private String name;

    /** plugin version * */
    final private String version;

    /** location of the manifest.mf file * */
    final private URL manifestLocation;

    /** Set of BundleDescription objects representing Require-Bundle entries **/
    private List requireBundle;
    
    /**
     * Constructor
     * @param symbolicName the unique id - bundle symbolic name
     * @param name the name
     * @param vendorName the vendor name
     * @param version the version number
     * @param manifestLocation the path to the declaration file
     */
    public Bundle(String symbolicName, String name, String vendorName, String version, URL manifestLocation)
    {
        this.symbolicName = symbolicName;
        this.name = name;
        this.vendorName = vendorName;
        this.version = version;
        this.manifestLocation = manifestLocation;
    }

    /**
     * Accessor for the plug-in id - bundle symbolic name
     * @return id of the plug-in
     */
    public String getSymbolicName()
    {
        return symbolicName;
    }

    /**
     * Accessor for the provider name of this plug-in
     * @return provider name
     */
    public String getVendorName()
    {
        return vendorName;
    }

    /**
     * Acessor for the version of this plug-in
     * @return version
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Acessor for the location of the manifest.mf file
     * @return the manifest.mf location
     */
    public URL getManifestLocation()
    {
        return manifestLocation;
    }

    /**
     * Acessor for the plug-in name
     * @return plug-in name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Acessor for the RequireBundle.
     * @param requireBundle A List of {@link Bundle.BundleDescription} elements
     */
    public void setRequireBundle(List requireBundle)
    {
        this.requireBundle = requireBundle;
    }

    /**
     * Acessor for the RequireBundle
     * @return A List of {@link Bundle.BundleDescription} elements
     */
    public List getRequireBundle()
    {
        return requireBundle;
    }
    
    /**
     * Description of bundles
     * bundle-description = symbolic-name (';' parameter )*
     * 
     * See OSGI 3.0 $ 1.4.2
     * 
     */
    public static class BundleDescription
    {
        private String bundleSymbolicName;

        /**
         * List of parameters for the BundleDescription
         */
        private Map parameters = new HashMap();

        public String getBundleSymbolicName()
        {
            return bundleSymbolicName;
        }

        public void setBundleSymbolicName(String bundleSymbolicName)
        {
            this.bundleSymbolicName = bundleSymbolicName;
        }

        public String getParameter(String name)
        {
            return (String) parameters.get(name);
        }

        public void setParameter(String name, String value)
        {
            parameters.put(name, value);
        }    

        public void setParameters(Map parameters)
        {
            this.parameters.putAll(parameters);
        }    

        public String toString()
        {
            return "BundleDescription [Symbolic Name] "+ bundleSymbolicName + " [Parameters] "+parameters;
        }
    }

    /**
     * Bundle Version - according to OSGi spec 3.0 $3.2.4
     */
    public static class BundleVersion
    {
        public int major;
        public int minor;
        public int micro;
        public String qualifier = "";

        public int hashCode()
        {
            return major ^ minor ^ micro ^ qualifier.hashCode();
        }

        public boolean equals(Object object)
        {
            if (object == null)
            {
                return false;
            }
            return (this.compareTo(object) == 0);
        }
        public int compareTo(Object object)
        {
            if (object == this)
            {
                return 0;
            }

            BundleVersion other = (BundleVersion)object;
            int result = major - other.major;
            if (result != 0)
            {
                return result;
            }

            result = minor - other.minor;
            if (result != 0)
            {
                return result;
            }

            result = micro - other.micro;
            if (result != 0)
            {
                return result;
            }
            else
            {
                return qualifier.compareTo(other.qualifier);
            }
        }
        public String toString()
        {
            return ""+major+"."+minor+"."+micro+(qualifier.length()>0?"."+qualifier:"");
        }
    }

    /**
     * Bundle Range - according to OSGi spec 3.0 �3.2.5
     */
    public static class BundleVersionRange
    {
        public BundleVersion floor;
        public BundleVersion ceiling;
        public boolean floor_inclusive = true;
        public boolean ceiling_inclusive = false;
        
        public String toString()
        {
            return "Bundle VersionRange [Floor] "+floor+" inclusive:"+floor_inclusive+" [Ceiling] "+ceiling+" inclusive:"+ceiling_inclusive;
        }
    }

    public String toString()
    {
        return "Bundle [Symbolic Name]"+symbolicName+" [Version] "+version;
    }
}