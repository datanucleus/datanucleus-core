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
2004 Andy Jefferson - added description of addition process.
2004 Andy Jefferson - added constructor, and try-catch on initialisation
2006 Andy Jefferson - renamed to PersistenceConfiguration so that it is API agnostic
2008 Andy Jefferson - rewritten to have properties map and not need Java beans setters/getters
2011 Andy Jefferson - default properties, user properties, datastore properties concepts
    ...
**********************************************************************/
package org.datanucleus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.properties.BooleanPropertyValidator;
import org.datanucleus.properties.FrequentlyAccessedProperties;
import org.datanucleus.properties.IntegerPropertyValidator;
import org.datanucleus.properties.PropertyValidator;
import org.datanucleus.properties.PropertyStore;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Class providing configuration for the context. 
 * Properties are defined by the context, and optionally defined in plugin.xml for any datastore/api plugins.
 * Property values are stored in maps. 
 * <ul>
 * <li>The first is the default value for the property (where a default is defined). The default comes from
 *     either the plugin defining it, or for the API being used (overrides any plugin default).</li>
 * <li>The second is the user-provided value (where the user has provided one). This is held in the superclass PropertyStore.</li>
 * </ul>
 * Components can then access these properties using any of the convenience accessors for boolean, Boolean, long, int, Object, String types.
 * When accessing properties the user-provided value is taken first (if available), otherwise the default value is used (or null).
 */
public class Configuration extends PropertyStore implements Serializable
{
    private static final long serialVersionUID = 1483628590934722025L;

    private NucleusContext nucCtx;

    /** Mapping for the properties of the plugins, PropertyMapping, keyed by the property name. */
    private Map<String, PropertyMapping> propertyMappings = new HashMap<>();

    /** Map of default properties, used as a fallback. Key is lower-case. */
    private Map<String, Object> defaultProperties = new HashMap<>();

    private Map<String, PropertyValidator> propertyValidators = new HashMap<>();
    
    private volatile Map<String, Object> managerOverrideableProperties;
    
    private FrequentlyAccessedProperties defaultFrequentProperties = new FrequentlyAccessedProperties();

    /**
     * Convenience class wrapping the plugin property specification information.
     */
    static class PropertyMapping implements Serializable
    {
        private static final long serialVersionUID = 9004376979051886506L;
        String name; // Case sensitive if we have an internal name
        String internalName; // Lowercase
        String validatorName;
        boolean datastore;
        boolean managerOverride;
        public PropertyMapping(String name, String intName, String validator, boolean datastore, boolean managerOverride)
        {
            this.name = name;
            this.internalName = intName;
            this.validatorName = validator;
            this.datastore = datastore;
            this.managerOverride = managerOverride;
        }
    }

    /**
     * Create a configuration object for the specified NucleusContext.
     * Initialises all basic properties with suitable defaults, including any specified in meta-data in plugins.
     * @param nucCtx NucleusContext
     */
    public Configuration(NucleusContext nucCtx)
    {
        this.nucCtx = nucCtx;
        this.frequentProperties.setDefaults(defaultFrequentProperties);

        // Load up properties for the context that is in use
        nucCtx.applyDefaultProperties(this);

        // Add properties from plugins
        ConfigurationElement[] propElements = nucCtx.getPluginManager().getConfigurationElementsForExtension("org.datanucleus.persistence_properties", null, null);
        if (propElements != null)
        {
            for (int i=0;i<propElements.length;i++)
            {
                String name = propElements[i].getAttribute("name");
                String intName = propElements[i].getAttribute("internal-name");

                String value = propElements[i].getAttribute("value");
                String datastoreString = propElements[i].getAttribute("datastore");
                String validatorName = propElements[i].getAttribute("validator");
                boolean datastore = Boolean.valueOf(datastoreString);
                String mgrOverrideString = propElements[i].getAttribute("manager-overrideable");
                boolean mgrOverride = Boolean.valueOf(mgrOverrideString);

                addDefaultProperty(name, intName, value, validatorName, datastore, mgrOverride);
            }
        }
    }

    /**
     * Accessor for the names of the supported persistence properties.
     * @return The persistence properties that we support
     */
    public Set<String> getSupportedProperties()
    {
        return propertyMappings.keySet();
    }

    /**
     * Convenience method to return all properties that are user-specified and should be specified on the StoreManager.
     * @return Datastore properties
     */
    public Map<String, Object> getDatastoreProperties()
    {
        Map<String, Object> props = new HashMap<>();

        for (Map.Entry<String, Object> propEntry : properties.entrySet())
        {
            String name = propEntry.getKey();
            if (isPropertyForDatastore(name))
            {
                props.put(name, propEntry.getValue());
            }
        }
        return props;
    }

    /**
     * Method that removes all properties from this store that are marked as "datastore".
     */
    public void removeDatastoreProperties()
    {
        Iterator<String> propKeyIter = properties.keySet().iterator();
        while (propKeyIter.hasNext())
        {
            String name = propKeyIter.next();
            if (isPropertyForDatastore(name))
            {
                propKeyIter.remove();
            }
        }
    }

    /**
     * Accessor for whether the specified property name should be stored with the StoreManager.
     * @param name Name of the property
     * @return Whether it is for the datastore
     */
    private boolean isPropertyForDatastore(String name)
    {
        PropertyMapping mapping = propertyMappings.get(name);
        return mapping != null ? mapping.datastore : false;
    }

    public String getInternalNameForProperty(String name)
    {
        PropertyMapping mapping = propertyMappings.get(name);
        return mapping != null && mapping.internalName != null ? mapping.internalName : name;
    }

    /**
     * Convenience method to return all properties that are overrideable on the PM/EM.
     * @return PM/EM overrideable properties
     */
    public Map<String, Object> getManagerOverrideableProperties()
    {
        if (managerOverrideableProperties != null) 
        {
            return managerOverrideableProperties;
        }

        Map<String, Object> props = new LinkedHashMap<>();
        for (PropertyMapping mapping : propertyMappings.values())
        {
            if (mapping.managerOverride)
            {
                String propName = mapping.internalName != null ? mapping.internalName.toLowerCase() : mapping.name.toLowerCase();
                props.put(propName, getProperty(propName));
            }
            else if (mapping.internalName != null)
            {
                PropertyMapping intMapping = propertyMappings.get(mapping.internalName.toLowerCase());
                if (intMapping != null && intMapping.managerOverride)
                {
                    props.put(mapping.name.toLowerCase(), getProperty(mapping.internalName));
                }
            }
        }

        managerOverrideableProperties = Collections.unmodifiableMap(props);        
        return managerOverrideableProperties;
    }

    /**
     * Returns the names of the properties that are manager overrideable (using their original cases, not lowercase).
     * @return The supported manager-overrideable property names
     */
    public Set<String> getManagedOverrideablePropertyNames()
    {
        Set<String> propNames = new HashSet<String>();
        for (PropertyMapping mapping : propertyMappings.values())
        {
            if (mapping.managerOverride)
            {
                propNames.add(mapping.name);
            }
        }
        return propNames;
    }

    /**
     * Accessor for the case-sensitive (external) name for the passed (likely lowercase) name and prefix.
     * @param propName The (likely lowercase) name
     * @param propPrefix The prefix for the property name
     * @return The case-sensitive (external) property name
     */
    public String getPropertyNameWithInternalPropertyName(String propName, String propPrefix)
    {
        if (propName == null)
        {
            return null;
        }
        for (PropertyMapping mapping : propertyMappings.values())
        {
            if (mapping.internalName != null && mapping.internalName.toLowerCase().equals(propName.toLowerCase()) && mapping.name.startsWith(propPrefix))
            {
                return mapping.name;
            }
        }
        return null;
    }

    /**
     * Accessor for the case-sensitive name for the passed (lowercase) name.
     * Works on the basis that the <I>propertyMappings</i> keys are stored in the case-senistive form.
     * @param propName The (lowercase) name
     * @return Case sensitive name
     */
    public String getCaseSensitiveNameForPropertyName(String propName)
    {
        if (propName == null)
        {
            return null;
        }
        for (PropertyMapping mapping : propertyMappings.values())
        {
            if (mapping.name.toLowerCase().equals(propName.toLowerCase()))
            {
                return mapping.name;
            }
        }
        return propName;
    }

    /**
     * Method to set the persistence property defaults based on what is defined for plugins.
     * This should only be called after the other setDefaultProperties method is called, which sets up the mappings
     * @param props Properties to use in the default set
     */
    public void setDefaultProperties(Map<String, Object> props)
    {
        if (props != null && !props.isEmpty())
        {
            for (Map.Entry<String, Object> entry : props.entrySet())
            {
                String key = entry.getKey();
                String keyLC = key.toLowerCase();

                PropertyMapping mapping = propertyMappings.get(keyLC);
                Object propValue = entry.getValue();
                if (mapping != null && mapping.validatorName != null && propValue instanceof String)
                {
                    propValue = getValueForPropertyWithValidator((String)propValue, mapping.validatorName);
                }

                defaultProperties.put(keyLC, propValue);
                defaultFrequentProperties.setProperty(keyLC, propValue);
            }
        }
    }

    public void addDefaultBooleanProperty(String name, String internalName, Boolean value, boolean datastore, boolean managerOverrideable)
    {
        addDefaultProperty(name, internalName, value!=null?""+value:null, BooleanPropertyValidator.class.getName(), datastore, managerOverrideable);
    }

    public void addDefaultIntegerProperty(String name, String internalName, Integer value, boolean datastore, boolean managerOverrideable)
    {
        addDefaultProperty(name, internalName, value!=null?""+value:null, IntegerPropertyValidator.class.getName(), datastore, managerOverrideable);
    }

    public void addDefaultProperty(String name, String internalName, String value, String validatorName, boolean datastore, boolean managerOverrideable)
    {
        managerOverrideableProperties = null;

        String nameLC = name.toLowerCase();

        // Add the mapping, keyed by lower case name
        propertyMappings.put(nameLC, new PropertyMapping(name, internalName, validatorName, datastore, managerOverrideable));

        String storedName = internalName != null ? internalName.toLowerCase() : nameLC;
        if (!defaultProperties.containsKey(storedName))
        {
            // Check if provided via a System property (case sensitive), otherwise use passed default
            Object propValue = System.getProperty(name);
            if (propValue == null)
            {
                propValue = value;
            }

            if (propValue != null)
            {
                if (validatorName != null)
                {
                    propValue = getValueForPropertyWithValidator(value, validatorName);
                }
                defaultProperties.put(storedName, propValue);
                defaultFrequentProperties.setProperty(storedName, propValue);
            }
        }
    }

    protected Object getValueForPropertyWithValidator(String value, String validatorName)
    {
        if (validatorName.equals(BooleanPropertyValidator.class.getName()))
        {
            return Boolean.valueOf(value);
        }
        else if (validatorName.equals(IntegerPropertyValidator.class.getName()))
        {
            return Integer.valueOf(value);
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.properties.PropertyStore#hasProperty(java.lang.String)
     */
    @Override
    public boolean hasProperty(String name)
    {
        if (properties.containsKey(name))
        {
            return true;
        }
        else if (defaultProperties.containsKey(name))
        {
            return true;
        }
        return false;
    }

    /**
     * Accessor for the specified property as an Object.
     * Returns user-specified value if provided, otherwise the default value, otherwise null.
     * @param name Name of the property
     * @return Value for the property
     */
    public Object getProperty(String name)
    {
        // Use local property value if present, otherwise relay back to default value
        if (properties.containsKey(name))
        {
            return super.getProperty(name);
        }
        return defaultProperties.get(name);
    }

    /**
     * Method to set the persistence properties using those defined in a file.
     * @param filename Name of the file containing the properties
     */
    public synchronized void setPropertiesUsingFile(String filename)
    {
        if (filename == null)
        {
            return;
        }

        Map<String, Object> props = null;
        try
        {
            Properties propsFromFile = getPropertiesFromPropertiesFile(filename);
            props = new HashMap<>();
            for (String key : propsFromFile.stringPropertyNames()) 
            {
                props.put(key, propsFromFile.getProperty(key));
            }

            setPropertyInternal(PropertyNames.PROPERTY_PROPERTIES_FILE, filename);
        }
        catch (NucleusUserException nue)
        {
            properties.remove(PropertyNames.PROPERTY_PROPERTIES_FILE);
            throw nue;
        }
        if (props != null && !props.isEmpty())
        {
            setPersistenceProperties(props);
        }
    }

    /**
     * Accessor for the persistence properties default values.
     * This returns the defaulted properties
     * @return The persistence properties
     */
    public Map<String, Object> getPersistencePropertiesDefaults()
    {
        return Collections.unmodifiableMap(defaultProperties);
    }

    /**
     * Accessor for the persistence properties.
     * This returns just the user-supplied properties, not the defaulted properties
     * @return The persistence properties
     */
    public Map<String, Object> getPersistenceProperties()
    {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Accessor for all properties starting with the provided prefix.
     * @param prefix Prefix (lowercase, since all properties are stored in lowercase)
     * @return The properties with this prefix
     */
    public Set<String> getPropertyNamesWithPrefix(String prefix)
    {
        Set<String> propNames = null;
        for (String name : properties.keySet())
        {
            if (name.startsWith(prefix))
            {
                if (propNames == null)
                {
                    propNames = new HashSet<>();
                }
                propNames.add(name);
            }
        }
        return propNames;
    }

    /**
     * Set the properties for this configuration.
     * Note : this has this name so it has a getter/setter pair for use by things like Spring.
     * @see #getPersistenceProperties()
     * @param props The persistence properties
     */
    public void setPersistenceProperties(Map<String, Object> props)
    {
        for (Map.Entry<String, Object> entry : props.entrySet())
        {
            setProperty(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Convenience method to set a persistence property.
     * Uses any validator defined for the property to govern whether the value is suitable.
     * @param name Name of the property
     * @param value Value
     */
    public void setProperty(String name, Object value)
    {
        if (name != null)
        {
            String propertyName = name.trim();
            PropertyMapping mapping = propertyMappings.get(propertyName.toLowerCase());
            if (mapping != null)
            {
                if (mapping.validatorName != null)
                {
                    validatePropertyValue(mapping.internalName != null ? mapping.internalName.toLowerCase() : propertyName.toLowerCase(), value, mapping.validatorName);

                    if (value != null && value instanceof String)
                    {
                        // Update the value to be consistent with the validator
                        value = getValueForPropertyWithValidator((String)value, mapping.validatorName);
                    }
                }

                setPropertyInternal((mapping.internalName != null) ? mapping.internalName : mapping.name, value);

                if (propertyName.equals(PropertyNames.PROPERTY_PROPERTIES_FILE))
                {
                    // Load all properties from the specified file
                    setPropertiesUsingFile((String)value);
                }
            }
            else
            {
                // Unknown property so just add it.
                setPropertyInternal(propertyName, value);
                if (!propertyMappings.isEmpty())
                {
                    NucleusLogger.PERSISTENCE.info(Localiser.msg("008015", propertyName));
                }
            }
        }
    }

    public void validatePropertyValue(String name, Object value)
    {
        String validatorName = null;
        PropertyMapping mapping = propertyMappings.get(name.toLowerCase());
        if (mapping != null)
        {
            validatorName = mapping.validatorName;
            if (validatorName != null)
            {
                validatePropertyValue(name.toLowerCase(), value, validatorName);
            }
        }
    }

    /**
     * Convenience method to validate the value for a property using the provided validator.
     * @param name The property name
     * @param value The value
     * @param validatorName Name of the validator class
     * @throws IllegalArgumentException if doesnt validate correctly
     */
    private void validatePropertyValue(String name, Object value, String validatorName)
    {
        if (validatorName == null)
        {
            return;
        }

        PropertyValidator validator = propertyValidators.get(validatorName);
        if (validator == null)
        {
            // Not yet instantiated so try to create validator
            try
            {
                validator = (PropertyValidator)nucCtx.getPluginManager().createExecutableExtension("org.datanucleus.persistence_properties", "name", name, "validator", null, null);
                if (validator == null)
                {
                    // Core properties are not in plugin.xml, so load via class loader since the class is in this bundle
                    Class validatorCls = nucCtx.getClassLoaderResolver(getClass().getClassLoader()).classForName(validatorName);
                    validator = (PropertyValidator)validatorCls.getDeclaredConstructor().newInstance();
                }
                if (validator != null)
                {
                    propertyValidators.put(validatorName, validator);
                }
            }
            catch (Exception e)
            {
                NucleusLogger.PERSISTENCE.warn("Error creating validator of type " + validatorName, e);
            }
        }

        if (validator != null)
        {
            if (!validator.validate(name, value))
            {
                throw new IllegalArgumentException(Localiser.msg("008012", name, value));
            }
        }
    }

    public synchronized boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof Configuration))
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }

        Configuration config = (Configuration)obj;
        if (properties == null)
        {
            if (config.properties != null)
            {
                return false;
            }
        }
        else if (!properties.equals(config.properties))
        {
            return false;
        }

        if (defaultProperties == null)
        {
            if (config.defaultProperties != null)
            {
                return false;
            }
        }
        else if (!defaultProperties.equals(config.defaultProperties))
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return (properties != null ? properties.hashCode() : 0) ^ (defaultProperties!=null ? defaultProperties.hashCode() : 0);
    }

    /**
     * Method to return the persistence properties from the specified properties file.
     * The lines of the file will be of format
     * <pre>
     * mypropertyname=myvalue
     * </pre>
     * @param filename Name of the file containing the properties
     * @return the Persistence Properties in this file
     * @throws NucleusUserException if file not readable
     */
    public static synchronized Properties getPropertiesFromPropertiesFile(String filename)
    {
        if (filename == null)
        {
            return null;
        }

        // try to load the properties file
        Properties props = new Properties();
        File file = new File(filename);
        if (file.exists())
        {
            try
            {
                InputStream is = new FileInputStream(file);
                props.load(is);
                is.close();
            }
            catch (FileNotFoundException e)
            {
                throw new NucleusUserException(Localiser.msg("008014", filename), e).setFatal();
            }
            catch (IOException e)
            {
                throw new NucleusUserException(Localiser.msg("008014", filename), e).setFatal();
            }
        }
        else
        {
            // Try to load it as a resource in the CLASSPATH
            try
            {
                InputStream is = Configuration.class.getClassLoader().getResourceAsStream(filename);
                props.load(is);
                is.close();
            }
            catch (Exception e)
            {
                // Still not loadable so throw exception
                throw new NucleusUserException(Localiser.msg("008014", filename), e).setFatal();
            }
        }

        return props;
    }
}