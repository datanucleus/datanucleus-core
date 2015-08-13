/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved. 
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

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * MetaData representation of a "persistence.xml" persistence unit.
 * Corresponds to the JPA spec section 6.2.1
 */
public class PersistenceUnitMetaData extends MetaData
{
    private static final long serialVersionUID = 6021663206256915679L;

    /** Name of the persistence unit. */
    String name = null;

    /** Root of the persistence unit. */
    URI rootURI = null;

    /** Transaction type for this persistence unit. */
    TransactionType transactionType = null;

    /** Description of the persistence unit. */
    String description = null;

    /** Provider for the persistence unit. */
    String provider = null;

    /** Validation Mode for Bean Validator. */
    String validationMode = null;

    /** JTA data source for the persistence unit. */
    String jtaDataSource = null;

    /** Non-JTA data source for the persistence unit. */
    String nonJtaDataSource = null;

    /** Names of the classes specified. */
    Set<String> classNames = null;

    /** Names/URLs of the JAR files specified. */
    Set jarFiles = null;

    /** Names of the mapping files specified. */
    Set<String> mappingFileNames = null;

    /** Vendor properties. */
    Properties properties = null;

    /** Whether to exclude unlisted classes. */
    boolean excludeUnlistedClasses = false; // TODO Default to true

    /** Caching policy for persistable objects. */
    String caching = "UNSPECIFIED";

    /**
     * Constructor.
     * @param name Name of the persistence unit
     * @param transactionType Transaction type for this unit
     * @param rootURI Root of the persistence-unit
     */
    public PersistenceUnitMetaData(String name, String transactionType, URI rootURI)
    {
        this.name = name;
        this.transactionType = TransactionType.getValue(transactionType);
        this.rootURI = rootURI;
    }

    public String getName()
    {
        return name;
    }

    /**
     * Accessor for the persistence unit root.
     * @return Root of the persistence unit
     */
    public URI getRootURI()
    {
        return rootURI;
    }

    public TransactionType getTransactionType()
    {
        return transactionType;
    }

    /**
     * Accessor for the persistence unit caching policy.
     * @return Caching policy: ALL, NONE, ENABLE_SELECTIVE, DISABLE_SELECTIVE, UNSPECIFIED.
     */
    public String getCaching()
    {
        return caching;
    }

    /**
     * Mutator for the unit caching policy
     * @param cache The caching policy: ALL, NONE, ENABLE_SELECTIVE, DISABLE_SELECTIVE, UNSPECIFIED.
     */
    public void setCaching(String cache)
    {
        this.caching = cache;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String desc)
    {
        this.description = desc;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getJtaDataSource()
    {
        return jtaDataSource;
    }

    public void setJtaDataSource(String data)
    {
        this.jtaDataSource = data;
    }

    public String getNonJtaDataSource()
    {
        return nonJtaDataSource;
    }

    public void setNonJtaDataSource(String data)
    {
        this.nonJtaDataSource = data;
    }

    /**
     * Mutator for the validation mode
     * @param validationMode AUTO, CALLBACK or NONE
     */
    public void setValidationMode(String validationMode)
    {
        this.validationMode = validationMode;
    }
    
    /**
     * Accessor to the Validation Mode
     * @return AUTO, CALLBACK or NONE
     */
    public String getValidationMode()
    {
        return validationMode;
    }

    // TODO Remove this when we change calls to use setExcludeUnlistedClasses(boolean)
    public void setExcludeUnlistedClasses()
    {
        excludeUnlistedClasses = true;
    }

    public void setExcludeUnlistedClasses(boolean flag)
    {
        excludeUnlistedClasses = flag;
    }

    public boolean getExcludeUnlistedClasses()
    {
        return excludeUnlistedClasses;
    }

    public void addClassName(String className)
    {
        if (classNames == null)
        {
            this.classNames = new HashSet();
        }
        this.classNames.add(className);
    }

    public void addClassNames(Set<String> classNames)
    {
        if (this.classNames == null)
        {
            this.classNames = new HashSet();
        }
        this.classNames.addAll(classNames);
    }

    public void addJarFile(String jarName)
    {
        if (jarFiles == null)
        {
            jarFiles = new HashSet();
        }
        jarFiles.add(jarName);
    }

    public void addJarFiles(Set<String> jarNames)
    {
        if (jarFiles == null)
        {
            jarFiles = new HashSet();
        }
        jarFiles.addAll(jarNames);
    }

    /**
     * Method to add a jar file to the persistence unit.
     * @param jarURL Jar file URL
     */
    public void addJarFile(URL jarURL)
    {
        if (jarFiles == null)
        {
            jarFiles = new HashSet();
        }
        jarFiles.add(jarURL);
    }

    /**
     * Convenience method to clear out all jar files.
     */
    public void clearJarFiles()
    {
        if (jarFiles != null)
        {
            jarFiles.clear();
        }
        jarFiles = null;
    }

    public void addMappingFile(String mappingFile)
    {
        if (mappingFileNames == null)
        {
            mappingFileNames = new HashSet();
        }
        mappingFileNames.add(mappingFile);
    }

    public void addProperty(String key, String value)
    {
        if (key == null || value == null)
        {
            // Ignore null properties
            return;
        }
        if (properties == null)
        {
            properties = new Properties();
        }
        properties.setProperty(key, value);
    }

    public Set<String> getClassNames()
    {
        return classNames;
    }

    public Set<String> getMappingFiles()
    {
        return mappingFileNames;
    }

    /**
     * Accessor for the jar files for this persistence unit.
     * The contents of the Set may be Strings (the names) or URLs
     * @return The jar names
     */
    public Set getJarFiles()
    {
        return jarFiles;
    }

    public Properties getProperties()
    {
        return properties;
    }

    // ------------------------------- Utilities -------------------------------

    /**
     * Returns a string representation of the object using a prefix
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix, String indent)
    {
        // Field needs outputting so generate metadata
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<persistence-unit name=\"" + name + "\"");
        if (transactionType != null)
        {
            sb.append(" transaction-type=\"" + transactionType + "\"");
        }
        sb.append(">\n");

        // Description of unit
        if (description != null)
        {
            sb.append(prefix).append(indent).append("<description>" + description + "</description>\n");
        }

        // Provider for unit
        if (provider != null)
        {
            sb.append(prefix).append(indent).append("<provider>" + provider + "</provider>\n");
        }

        // JTA data source for unit
        if (jtaDataSource != null)
        {
            sb.append(prefix).append(indent).append("<jta-data-source>" + jtaDataSource + "</jta-data-source>\n");
        }

        // Non-JTA data source for unit
        if (nonJtaDataSource != null)
        {
            sb.append(prefix).append(indent).append("<non-jta-data-source>" + nonJtaDataSource + "</non-jta-data-source>\n");
        }

        // Add class names
        if (classNames != null)
        {
            for (String className : classNames)
            {
                sb.append(prefix).append(indent).append("<class>" + className + "</class>\n");
            }
        }

        // Add mapping files
        if (mappingFileNames != null)
        {
            for (String mappingFileName : mappingFileNames)
            {
                sb.append(prefix).append(indent).append("<mapping-file>" + mappingFileName + "</mapping-file>\n");
            }
        }

        // Add jar files
        if (jarFiles != null)
        {
            for (Object jarFile : jarFiles)
            {
                sb.append(prefix).append(indent).append("<jar-file>" + jarFile + "</jar-file>\n");
            }
        }

        // Add properties
        if (properties != null)
        {
            sb.append(prefix).append(indent).append("<properties>\n");
            Set entries = properties.entrySet();
            Iterator iter = entries.iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry)iter.next();
                sb.append(prefix).append(indent).append(indent).append("<property name=" + entry.getKey() + 
                    " value=" + entry.getValue() + "</property>\n");
            }
            sb.append(prefix).append(indent).append("</properties>\n");
        }

        if (excludeUnlistedClasses)
        {
            sb.append(prefix).append(indent).append("<exclude-unlisted-classes/>\n");
        }

        sb.append(prefix).append("</persistence-unit>\n");
        return sb.toString();
    }
}
