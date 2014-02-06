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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.properties.BooleanPropertyValidator;
import org.datanucleus.properties.CorePropertyValidator;
import org.datanucleus.properties.IntegerPropertyValidator;
import org.datanucleus.properties.PropertyValidator;
import org.datanucleus.properties.PropertyStore;
import org.datanucleus.properties.StringPropertyValidator;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.PersistenceUtils;

/**
 * Class providing configuration for the context. 
 * Properties are defined in plugin.xml (aliases, default value, validators etc). 
 * Property values are stored in two maps. 
 * <ul>
 * <li>The first is the default value for the property (where a default is defined). The default comes from
 *     either the plugin defining it, or for the API being used (overrides any plugin default).</li>
 * <li>The second is the user-provided value (where the user has provided one).</li>
 * </ul>
 * Components can then access these properties using any of the convenience accessors for boolean, Boolean, long, 
 * int, Object, String types. When accessing properties the user-provided value is taken first (if available),
 * otherwise the default value is used (or null).
 */
public class Configuration extends PropertyStore implements Serializable
{
    /** Localisation of messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        ClassConstants.NUCLEUS_CONTEXT_LOADER);

    private NucleusContext nucCtx;

    /** Map of default properties, used as a fallback. */
    private Map<String, Object> defaultProperties = new HashMap<String, Object>();

    /** Mapping for the properties of the plugins, PropertyMapping, keyed by the property name. */
    private Map<String, PropertyMapping> propertyMappings = new HashMap<String, PropertyMapping>();

    private Map<String, PropertyValidator> propertyValidators = new HashMap();

    /**
     * Convenience class wrapping the plugin property specification information.
     */
    static class PropertyMapping implements Serializable
    {
        String name;
        String internalName;
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
     * Constructor for this NucleusContext.
     * Initialises all basic properties with suitable defaults, including any specified in meta-data in plugins.
     * @param nucCtx NucleusContext
     */
    public Configuration(NucleusContext nucCtx)
    {
        this.nucCtx = nucCtx;

        // TODO Only load up properties for the context that is in use, so most of these are not required in enhancement context
        // NucleusContext level features
        addDefaultBooleanProperty(PropertyNames.PROPERTY_IGNORE_CACHE, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_OPTIMISTIC, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_MULTITHREADED, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_RETAIN_VALUES, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_RESTORE_VALUES, null, false, false, true);
        addDefaultProperty(PropertyNames.PROPERTY_JMX_TYPE, null, null, null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_ENABLE_STATISTICS, null, false, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_PMF_NAME, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_PERSISTENCE_XML_FILENAME, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_SERVER_TIMEZONE_ID, null, null,
            CorePropertyValidator.class.getName(), false, false);
        addDefaultProperty(PropertyNames.PROPERTY_PROPERTIES_FILE, null, null, null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_LOAD_CLASSES, null, false, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_EXECUTION_CONTEXT_REAPER_THREAD, null, false, false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_EXECUTION_CONTEXT_MAX_IDLE, null, 20, false, false);

        addDefaultBooleanProperty(PropertyNames.PROPERTY_OBJECT_PROVIDER_REAPER_THREAD, null, false, false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_OBJECT_PROVIDER_MAX_IDLE, null, 0, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_OBJECT_PROVIDER_CLASS_NAME, null, null, null, false, false);

        addDefaultProperty(PropertyNames.PROPERTY_DATASTORE_IDENTITY_TYPE, null, "datanucleus", null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_IDENTITY_STRING_TRANSLATOR_TYPE, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_IDENTITY_KEY_TRANSLATOR_TYPE, null, null, null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_USE_IMPLEMENTATION_CREATOR, null, true, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME, null, "datanucleus", null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY, null, null, null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_LOCALISE_MESSAGECODES, null, false, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_LOCALISE_LANGUAGE, null, null, null, false, false);

        // Plugin management
        addDefaultProperty(PropertyNames.PROPERTY_PLUGIN_REGISTRY_CLASSNAME, null, null, null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_PLUGIN_ALLOW_USER_BUNDLES, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_PLUGIN_VALIDATEPLUGINS, null, false, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_PLUGIN_REGISTRYBUNDLECHECK, null, "EXCEPTION", 
            CorePropertyValidator.class.getName(), false, false);

        // Transactions
        addDefaultProperty(PropertyNames.PROPERTY_TRANSACTION_TYPE, null, null,
            CorePropertyValidator.class.getName(), false, false);
        addDefaultProperty(PropertyNames.PROPERTY_TRANSACTION_JTA_LOCATOR, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_TRANSACTION_JTA_JNDI_LOCATION, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION, null, "read-committed", 
            CorePropertyValidator.class.getName(), false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_NONTX_READ, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_NONTX_WRITE, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_NONTX_ATOMIC, null, true, false, true);

        // Flush process
        addDefaultIntegerProperty(PropertyNames.PROPERTY_FLUSH_AUTO_OBJECT_LIMIT, null, 1, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_FLUSH_MODE, null, null, 
            CorePropertyValidator.class.getName(), false, true);

        // MetaData
        addDefaultBooleanProperty(PropertyNames.PROPERTY_METADATA_ALWAYS_DETACHABLE, null, false, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_METADATA_XML_VALIDATE, null, false, false, false);
        // TODO Deprecated so remove in future release
        addDefaultBooleanProperty("datanucleus.metadata.validate", PropertyNames.PROPERTY_METADATA_XML_VALIDATE, false, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_METADATA_XML_NAMESPACE_AWARE, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_METADATA_AUTOREGISTER, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_XML, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_ANNOTATIONS, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_LOAD_AT_RUNTIME, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_METADATA_SUPPORT_ORM, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_METADATA_JDO_SUFFIX, null, "jdo", null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_METADATA_ORM_SUFFIX, null, "orm", null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_METADATA_JDOQUERY_SUFFIX, null, "jdoquery", null, false, false);

        // Value Generation
        addDefaultProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ISOLATION, null, "read-committed", 
            CorePropertyValidator.class.getName(), false, false);
        addDefaultProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ATTRIBUTE, null, "New", 
            CorePropertyValidator.class.getName(), false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_VALUEGEN_SEQUENCE_ALLOCSIZE, null, 10, false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_VALUEGEN_INCREMENT_ALLOCSIZE, null, 10, false, false);

        // Bean Validation
        addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_MODE, null, "auto", 
            CorePropertyValidator.class.getName(), false, false);
        addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREPERSIST, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREUPDATE, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREREMOVE, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_FACTORY, null, null, null, false, false);

        // Auto-Start Mechanism
        addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_MECHANISM, null, "None", null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_MODE, null, "Quiet", 
            CorePropertyValidator.class.getName(), true, false);
        addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_XMLFILE, null, "datanucleusAutoStart.xml", null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_CLASSNAMES, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_METADATAFILES, null, null, null, true, false);

        // Schema Generation
        addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE, null, "none",
            CorePropertyValidator.class.getName(), false, false);
        addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_MODE, null, "none",
            CorePropertyValidator.class.getName(), false, false);
        addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_TARGET, null, "datanucleus-schema-create.ddl", 
            null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_TARGET, null, "datanucleus-schema-drop.ddl", 
            null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_SOURCE, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_SOURCE, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_LOAD_SOURCE, null, null, null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_SCHEMA, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_TABLES, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_COLUMNS, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_CONSTRAINTS, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_VALIDATE_SCHEMA, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_VALIDATE_TABLES, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_VALIDATE_COLUMNS, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_VALIDATE_CONSTRAINTS, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_WARNONERROR, null, false, true, false);

        // Schema and identifier naming
        addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_CASE, null, null,
            CorePropertyValidator.class.getName(), true, false);
        addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_TABLE_PREFIX, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_TABLE_SUFFIX, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_WORD_SEPARATOR, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_FACTORY, null, "datanucleus2", null, true, false);

        // Datastore
        addDefaultProperty(PropertyNames.PROPERTY_STORE_MANAGER_TYPE, null, null, null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_STORE_ALLOW_REFS_WITHOUT_IMPLS, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY, null, false, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_DATASTORE_FIXED, null, false, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION, null, "EXCEPTION", 
            CorePropertyValidator.class.getName(), true, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_DATASTORE_READ_TIMEOUT, null, null, true, true);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_DATASTORE_WRITE_TIMEOUT, null, null, true, true);
        addDefaultProperty(PropertyNames.PROPERTY_MAPPING, null, null, 
            StringPropertyValidator.class.getName(), true, false);
        addDefaultProperty(PropertyNames.PROPERTY_MAPPING_CATALOG, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_MAPPING_SCHEMA, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_MAPPING_TENANT_ID, null, null, null, true, false);

        // ExecutionContext level features
        addDefaultBooleanProperty(PropertyNames.PROPERTY_PERSISTENCE_BY_REACHABILITY_AT_COMMIT, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS_CHECKS, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_SERIALIZE_READ, null, false, false, true);
        addDefaultProperty(PropertyNames.PROPERTY_DELETION_POLICY, null, "JDO2", 
            CorePropertyValidator.class.getName(), false, true);
        addDefaultProperty(PropertyNames.PROPERTY_DEFAULT_INHERITANCE_STRATEGY, null, "JDO2", 
            CorePropertyValidator.class.getName(), false, false);
        // TODO Would be nice to set the default here to "false" but JDO TCK "instanceCallbacks" fails
        addDefaultBooleanProperty(PropertyNames.PROPERTY_FIND_OBJECT_VALIDATE_WHEN_CACHED, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_FIND_OBJECT_TYPE_CONVERSION, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_ALLOW_CALLBACKS, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_DETACH_ON_CLOSE, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_COPY_ON_ATTACH, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_ATTACH_SAME_DATASTORE, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_ALLOW_ATTACH_OF_TRANSIENT, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_DETACH_AS_WRAPPED, null, false, false, true);
        addDefaultProperty(PropertyNames.PROPERTY_DETACH_DETACHMENT_FIELDS, null, "load-fields", 
            CorePropertyValidator.class.getName(), false, false);
        addDefaultProperty(PropertyNames.PROPERTY_DETACH_DETACHED_STATE, null, "fetch-groups", 
            CorePropertyValidator.class.getName(), false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_MAX_FETCH_DEPTH, null, 1, false, true);

        // Connection
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_URL, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_USER_NAME, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_PASSWORD, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_PASSWORD_DECRYPTER, null, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2_NAME, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_RESOURCETYPE, null, null, 
            CorePropertyValidator.class.getName(), true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_RESOURCETYPE2, null, null,
            CorePropertyValidator.class.getName(), true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_POOLINGTYPE, null, null, null, true, false);
        addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_POOLINGTYPE2, null, null, null, true, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CONNECTION_NONTX_RELEASE_AFTER_USE, null, true, true, false);

        // Cache
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_L1_TYPE, null, "soft", null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_TYPE, null, "soft", null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_COLLECTIONS, null, true, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_COLLECTIONS_LAZY, null, null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_MODE, null, "UNSPECIFIED", 
            CorePropertyValidator.class.getName(), false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_NAME, null, "datanucleus", null, false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_L2_MAXSIZE, null, -1, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_LOADFIELDS, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CLEARATCLOSE, null, true, false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_L2_TIMEOUT, null, -1, false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_L2_BATCHSIZE, null, 100, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_READ_THROUGH, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_WRITE_THROUGH, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_STATISTICS_ENABLED, null, false, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_STORE_BY_VALUE, null, true, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_RETRIEVE_MODE, null, "use",
            CorePropertyValidator.class.getName(), false, true);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_STORE_MODE, null, "use",
            CorePropertyValidator.class.getName(), false, true);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_UPDATE_MODE, null, "commit-and-datastore-read", 
            CorePropertyValidator.class.getName(), false, true);

        addDefaultProperty(PropertyNames.PROPERTY_CACHE_QUERYCOMPILE_TYPE, null, "soft", null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_QUERYCOMPILEDATASTORE_TYPE, null, "soft", null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_TYPE, null, "soft", null, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_NAME, null, "datanucleus-query", null, false, false);
        addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_MAXSIZE, null, -1, false, false);

        // Queries
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_SQL_ALLOWALL, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL, null, false, false, true);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_FLUSH_BEFORE_EXECUTE, null, false, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_USE_FETCHPLAN, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_CHECK_UNUSED_PARAMS, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_COMPILE_OPTIMISED, null, false, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_LOAD_RESULTS_AT_COMMIT, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_COMPILATION_CACHED, null, true, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_RESULTS_CACHED, null, false, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_EVALUATE_IN_MEMORY, null, false, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_RESULTCACHE_VALIDATEOBJECTS, null, true, false, false);
        addDefaultProperty(PropertyNames.PROPERTY_QUERY_RESULT_SIZE_METHOD, null, "last", null, false, false);
        addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_COMPILE_NAMED_QUERIES_AT_STARTUP, null, false, false, false);

        // Add properties from plugins
        ConfigurationElement[] propElements =
            nucCtx.getPluginManager().getConfigurationElementsForExtension("org.datanucleus.persistence_properties", null, null);
        if (propElements != null)
        {
            for (int i=0;i<propElements.length;i++)
            {
                String name = propElements[i].getAttribute("name");
                String intName = propElements[i].getAttribute("internal-name");
                String value = propElements[i].getAttribute("value");
                String datastoreString = propElements[i].getAttribute("datastore");
                String validatorName = propElements[i].getAttribute("validator");
                boolean datastore = (datastoreString != null && datastoreString.equalsIgnoreCase("true"));
                String mgrOverrideString = propElements[i].getAttribute("manager-overrideable");
                boolean mgrOverride = (mgrOverrideString != null && mgrOverrideString.equalsIgnoreCase("true"));

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
        Map<String, Object> props = new HashMap<String, Object>();
        Iterator<String> propKeyIter = properties.keySet().iterator();
        while (propKeyIter.hasNext())
        {
            String name = propKeyIter.next();
            if (isPropertyForDatastore(name))
            {
                props.put(name, properties.get(name));
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
    public boolean isPropertyForDatastore(String name)
    {
        PropertyMapping mapping = propertyMappings.get(name.toLowerCase(Locale.ENGLISH));
        return (mapping != null ? mapping.datastore : false);
    }

    public String getInternalNameForProperty(String name)
    {
        PropertyMapping mapping = propertyMappings.get(name.toLowerCase(Locale.ENGLISH));
        return (mapping != null && mapping.internalName != null ? mapping.internalName : name);
    }

    /**
     * Convenience method to return all properties that are overrideable on the PM/EM.
     * @return PM/EM overrideable properties
     */
    public Map<String, Object> getManagerOverrideableProperties()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        Iterator<Map.Entry<String, PropertyMapping>> propIter = propertyMappings.entrySet().iterator();
        while (propIter.hasNext())
        {
            Map.Entry<String, PropertyMapping> entry = propIter.next();
            PropertyMapping mapping = entry.getValue();
            if (mapping.managerOverride)
            {
                String propName = (mapping.internalName != null ? 
                        mapping.internalName.toLowerCase(Locale.ENGLISH) : mapping.name.toLowerCase(Locale.ENGLISH));
                props.put(propName, getProperty(propName));
            }
            else if (mapping.internalName != null)
            {
                PropertyMapping intMapping = propertyMappings.get(mapping.internalName.toLowerCase(Locale.ENGLISH));
                if (intMapping != null && intMapping.managerOverride)
                {
                    props.put(mapping.name.toLowerCase(Locale.ENGLISH), getProperty(mapping.internalName));
                }
            }
        }
        return props;
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

    public String getPropertyNameWithInternalPropertyName(String propName, String propPrefix)
    {
        if (propName == null)
        {
            return null;
        }
        for (PropertyMapping mapping : propertyMappings.values())
        {
            if (mapping.internalName != null && mapping.internalName.toLowerCase().equals(propName.toLowerCase()) &&
                mapping.name.startsWith(propPrefix))
            {
                return mapping.name;
            }
        }
        return null;
    }

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
    public void setDefaultProperties(Map props)
    {
        if (props != null && props.size() > 0)
        {
            Iterator<Map.Entry> entryIter = props.entrySet().iterator();
            while (entryIter.hasNext())
            {
                Map.Entry entry = entryIter.next();
                PropertyMapping mapping = propertyMappings.get(((String)entry.getKey()).toLowerCase(Locale.ENGLISH));
                Object propValue = entry.getValue();
                if (mapping != null && mapping.validatorName != null && propValue instanceof String)
                {
                    propValue = getValueForPropertyWithValidator((String)propValue, mapping.validatorName);
                }
                defaultProperties.put(((String)entry.getKey()).toLowerCase(Locale.ENGLISH), propValue);
            }
        }
    }

    private void addDefaultBooleanProperty(String name, String internalName, Boolean value, 
            boolean datastore, boolean managerOverrideable)
    {
        addDefaultProperty(name, internalName, value!=null?""+value:null, 
            BooleanPropertyValidator.class.getName(), datastore, managerOverrideable);
    }

    private void addDefaultIntegerProperty(String name, String internalName, Integer value, 
            boolean datastore, boolean managerOverrideable)
    {
        addDefaultProperty(name, internalName, value!=null?""+value:null,
            IntegerPropertyValidator.class.getName(), datastore, managerOverrideable);
    }

    private void addDefaultProperty(String name, String internalName, String value, 
            String validatorName, boolean datastore, boolean managerOverrideable)
    {
        // Add the mapping
        propertyMappings.put(name.toLowerCase(Locale.ENGLISH), new PropertyMapping(name, internalName, validatorName,
            datastore, managerOverrideable));

        String storedName = internalName != null ? internalName.toLowerCase(Locale.ENGLISH) : name.toLowerCase(Locale.ENGLISH);
        if (!defaultProperties.containsKey(storedName))
        {
            // Add the default property+value
            Object propValue = System.getProperty(name);
            // TODO Feed system value through validator if provided
            if (propValue == null)
            {
                // No system value so use code default
                propValue = value;
            }

            if (propValue != null)
            {
                if (validatorName != null)
                {
                    propValue = getValueForPropertyWithValidator(value, validatorName);
                }
                this.defaultProperties.put(storedName, propValue);
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
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return true;
        }
        else if (defaultProperties.containsKey(name.toLowerCase(Locale.ENGLISH)))
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
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return super.getProperty(name);
        }
        return defaultProperties.get(name.toLowerCase(Locale.ENGLISH));
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

        Properties props = null;
        try
        {
            props = PersistenceUtils.setPropertiesUsingFile(filename);
            setPropertyInternal("datanucleus.propertiesFile", filename);
        }
        catch (NucleusUserException nue)
        {
            properties.remove("datanucleus.propertiesFile");
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
     * @see #getPersistenceProperties()
     * @return The persistence properties
     */
    public Map<String, Object> getPersistenceProperties()
    {
        return Collections.unmodifiableMap(properties);
    }

    public Set<String> getPropertyNamesWithPrefix(String prefix)
    {
        Set<String> propNames = null;
        Iterator<String> nameIter = properties.keySet().iterator();
        while (nameIter.hasNext())
        {
            String name = nameIter.next();
            if (name.startsWith(prefix.toLowerCase(Locale.ENGLISH)))
            {
                if (propNames == null)
                {
                    propNames = new HashSet<String>();
                }
                propNames.add(name);
            }
        }
        return propNames;
    }

    /**
     * Set the properties for this configuration.
     * Note : this has this name so it has a getter/setter pair for use by things like Spring.
     * @see #getPersistencePropertiesDefaults()
     * @param props The persistence properties
     */
    public void setPersistenceProperties(Map props)
    {
        Set entries = props.entrySet();
        Iterator<Map.Entry> entryIter = entries.iterator();
        while (entryIter.hasNext())
        {
            Map.Entry entry = entryIter.next();
            Object keyObj = entry.getKey();
            if (keyObj instanceof String)
            {
                String key = (String)keyObj;
                setProperty(key, entry.getValue());
            }
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
            PropertyMapping mapping = propertyMappings.get(propertyName.toLowerCase(Locale.ENGLISH));
            if (mapping != null)
            {
                if (mapping.validatorName != null)
                {
                    validatePropertyValue(mapping.internalName != null ? mapping.internalName : propertyName, value,
                            mapping.validatorName);

                    if (value != null && value instanceof String)
                    {
                        // Update the value to be consistent with the validator
                        value = getValueForPropertyWithValidator((String)value, mapping.validatorName);
                    }
                }

                if (mapping.internalName != null)
                {
                    setPropertyInternal(mapping.internalName, value);
                }
                else
                {
                    setPropertyInternal(mapping.name, value);
                }

                // Special behaviour properties
                if (propertyName.equals("datanucleus.propertiesFile"))
                {
                    // Load all properties from the specified file
                    setPropertiesUsingFile((String)value);
                }
                else if (propertyName.equals("datanucleus.localisation.messageCodes"))
                {
                    // Set global log message code flag
                    boolean included = getBooleanProperty("datanucleus.localisation.messageCodes");
                    Localiser.setDisplayCodesInMessages(included);
                }
                else if (propertyName.equals("datanucleus.localisation.language"))
                {
                    String language = getStringProperty("datanucleus.localisation.language");
                    Localiser.setLanguage(language);
                }
            }
            else
            {
                // Unknown property so just add it.
                setPropertyInternal(propertyName, value);
                if (propertyMappings.size() > 0)
                {
                    NucleusLogger.PERSISTENCE.info(LOCALISER.msg("008015", propertyName));
                }
            }
        }
    }

    public void validatePropertyValue(String name, Object value)
    {
        String validatorName = null; // TODO Work this out
        PropertyMapping mapping = propertyMappings.get(name.toLowerCase(Locale.ENGLISH));
        if (mapping != null)
        {
            validatorName = mapping.validatorName;
        }
        if (validatorName != null)
        {
            validatePropertyValue(name, value, validatorName);
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
                Class validatorCls = nucCtx.getClassLoaderResolver(getClass().getClassLoader()).classForName(validatorName);
                validator = (PropertyValidator)validatorCls.newInstance();
                propertyValidators.put(validatorName, validator);
            }
            catch (Exception e)
            {
                NucleusLogger.PERSISTENCE.warn("Error creating validator of type " + validatorName, e);
            }
        }

        if (validator != null)
        {
            boolean validated = validator.validate(name, value);
            if (!validated)
            {
                throw new IllegalArgumentException(LOCALISER.msg("008012", name, value));
            }
        }
    }

    /**
     * Equality operator.
     * @param obj Object to compare against.
     * @return Whether the objects are equal.
     */
    public synchronized boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof Configuration))
        {
            return false;
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
}