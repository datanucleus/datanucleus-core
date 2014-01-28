/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.datanucleus.api.ApiAdapter;
import org.datanucleus.api.ApiAdapterFactory;
import org.datanucleus.cache.Level2Cache;
import org.datanucleus.cache.NullLevel2Cache;
import org.datanucleus.enhancer.jdo.JDOImplementationCreator;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.exceptions.TransactionIsolationNotSupportedException;
import org.datanucleus.identity.DatastoreUniqueOID;
import org.datanucleus.identity.IdentityKeyTranslator;
import org.datanucleus.identity.IdentityStringTranslator;
import org.datanucleus.identity.OID;
import org.datanucleus.management.FactoryStatistics;
import org.datanucleus.management.jmx.ManagementManager;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.FileMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.QueryLanguage;
import org.datanucleus.metadata.QueryMetaData;
import org.datanucleus.metadata.TransactionType;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.Extension;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.state.ObjectProviderFactoryImpl;
import org.datanucleus.state.ObjectProviderFactory;
import org.datanucleus.store.StoreData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.autostart.AutoStartMechanism;
import org.datanucleus.store.exceptions.DatastoreInitialisationException;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
import org.datanucleus.store.schema.SchemaScriptAwareStoreManager;
import org.datanucleus.store.schema.SchemaTool;
import org.datanucleus.store.schema.SchemaTool.Mode;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.transaction.NucleusTransactionException;
import org.datanucleus.transaction.TransactionManager;
import org.datanucleus.transaction.jta.JTASyncRegistry;
import org.datanucleus.transaction.jta.JTASyncRegistryUnavailableException;
import org.datanucleus.transaction.jta.TransactionManagerFinder;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;
import org.datanucleus.validation.BeanValidatorHandler;

/**
 * Representation of the context being run within DataNucleus. Provides a series of services and can be used
 * for persistence, or enhancement. Responsible for
 * <ul>
 * <li>properties defining configuration for persistence.</li>
 * <li>creating <i>ExecutionContext</i> objects to handle persistence. Uses a pool of
 *     <i>ExecutionContext</i> objects, reusing them as required.</li>
 * <li>providing a cache across <i>ExecutionContext</i> objects (the "Level 2" cache).</li>
 * <li>manages the plugins involved in this DataNucleus context</li>
 * <li>provides access to the API in use</li>
 * <li>manages metadata for persistable classes</li>
 * <li>provides a factory for creating <i>ObjectProviders</i>. This factory makes use of pooling, allowing reuse.</li>
 * <li>provides access to the datastore via a <i>StoreManager</i></li>
 * <li>provides access to the TypeManager, defining the behaviour for java types.</li>
 * </ul>
 * TODO For DataNucleus 4.0 create PersistenceContext which extends this and has all of the persistence-related
 * fields (storeMgr, jca, txManager, jtaTxManager, etc) and remove "type" variable.
 */
public class NucleusContext implements Serializable
{
    /** Localisation of messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        ClassConstants.NUCLEUS_CONTEXT_LOADER);

    // TODO Split PERSISTENCE into PERSISTENCE_JAVASE, PERSISTENCE_JAVAEE, PERSISTENCE_JAVAEE_JCA
    public enum ContextType
    {
        PERSISTENCE,
        ENHANCEMENT
    }

    private final ContextType type;

    /** Configuration defining features of the persistence process. */
    private final PersistenceConfiguration config;

    /** Manager for Plug-ins. */
    private final PluginManager pluginManager;

    /** MetaDataManager for handling the MetaData for this PMF/EMF. */
    private MetaDataManager metaDataManager = null;

    /** Name of the class providing the ClassLoaderResolver. */
    private final String classLoaderResolverClassName;

    /** ApiAdapter used by the context. **/
    private final ApiAdapter apiAdapter;

    /** Manager for java types and SCO wrappers. */
    private TypeManager typeManager;

    /** Manager for the datastore used by this PMF/EMF. */
    private transient StoreManager storeMgr = null;

    /** Auto-Start Mechanism for loading classes into the StoreManager (if enabled). */
    private transient AutoStartMechanism starter = null;

    /** Flag defining if this is running within the JDO JCA adaptor. */
    private boolean jca = false;

    /** Level 2 Cache, caching across ExecutionContexts. */
    private Level2Cache cache;

    /** Transaction Manager. */
    private transient TransactionManager txManager = null;

    /** JTA Transaction Manager (if using JTA). */
    private transient javax.transaction.TransactionManager jtaTxManager = null;

    /** JTA Synchronization Registry (if using JTA 1.1 and supported). */
    private transient JTASyncRegistry jtaSyncRegistry = null;

    /** Map of the ClassLoaderResolver, keyed by the clr class and the primaryLoader name. */
    private transient Map<String, ClassLoaderResolver> classLoaderResolverMap = new HashMap<String, ClassLoaderResolver>();

    /** Manager for JMX features. */
    private transient ManagementManager jmxManager = null;

    /** Statistics gathering object. */
    private transient FactoryStatistics statistics = null;

    /** Random number generator, for use when needing unique names. */
    public static final Random random = new Random();

    /** Class to use for datastore-identity. */
    private Class datastoreIdentityClass = null;

    /** Identity string translator (if any). */
    private IdentityStringTranslator idStringTranslator = null;

    /** Flag for whether we have initialised the id string translator. */
    private boolean idStringTranslatorInit = false;

    /** Identity key translator (if any). */
    private IdentityKeyTranslator idKeyTranslator = null;

    /** Flag for whether we have initialised the id key translator. */
    private boolean idKeyTranslatorInit = false;

    /** ImplementationCreator for any persistent interfaces. */
    private ImplementationCreator implCreator;

    private List<ExecutionContext.LifecycleListener> executionContextListeners = new ArrayList();

    /** Manager for dynamic fetch groups defined on the PMF/EMF. */
    private transient FetchGroupManager fetchGrpMgr;

    /** Factory for validation. */
    private transient Object validatorFactory = null;

    /** Flag for whether we have initialised the validator factory. */
    private transient boolean validatorFactoryInit = false;

    /** Pool for ExecutionContexts. */
    private ExecutionContextPool ecPool = null;

    /** Factory for ObjectProviders for managing persistable objects. */
    private ObjectProviderFactory opFactory = null;

    public static final Set<String> STARTUP_PROPERTIES = new HashSet<String>();
    static
    {
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PLUGIN_REGISTRY_CLASSNAME);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PLUGIN_REGISTRYBUNDLECHECK);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PLUGIN_ALLOW_USER_BUNDLES);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PLUGIN_VALIDATEPLUGINS);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_PERSISTENCE_XML_FILENAME);
        STARTUP_PROPERTIES.add(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY);
    };

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     */
    public NucleusContext(String apiName, Map startupProps)
    {
        this(apiName, ContextType.PERSISTENCE, startupProps);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     * @param pluginMgr Plugin Manager (or null if wanting it to be created)
     */
    public NucleusContext(String apiName, Map startupProps, PluginManager pluginMgr)
    {
        this(apiName, ContextType.PERSISTENCE, startupProps, pluginMgr);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param type The type of context required (persistence, enhancement)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     */
    public NucleusContext(String apiName, ContextType type, Map startupProps)
    {
        this(apiName, type, startupProps, null);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param type The type of context required (persistence, enhancement)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     * @param pluginMgr Plugin Manager (or null if wanting it to be created)
     */
    public NucleusContext(String apiName, ContextType type, Map startupProps, PluginManager pluginMgr)
    {
        this.type = type;
        if (pluginMgr != null)
        {
            this.pluginManager = pluginMgr;
        }
        else
        {
            this.pluginManager = PluginManager.createPluginManager(startupProps, this.getClass().getClassLoader());
        }

        // Create PersistenceConfiguration (with defaults for plugins), and impose any startup properties
        this.config = new PersistenceConfiguration(this);
        if (startupProps != null && !startupProps.isEmpty())
        {
            this.config.setPersistenceProperties(startupProps);
        }

        // Set the name of class loader resolver
        String clrName = config.getStringProperty(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME);
        classLoaderResolverClassName = pluginManager.getAttributeValueForExtension(
            "org.datanucleus.classloader_resolver", "name", clrName, "class-name");
        if (classLoaderResolverClassName == null)
        {
            // User has specified a classloader_resolver plugin that has not registered
            throw new NucleusUserException(LOCALISER.msg("001001", clrName)).setFatal();
        }

        // Initialise API, and set defaults for properties for the API
        if (apiName != null)
        {
            this.apiAdapter = ApiAdapterFactory.getInstance().getApiAdapter(apiName, pluginManager);
            this.config.setDefaultProperties(apiAdapter.getDefaultFactoryProperties());
        }
        else
        {
            this.apiAdapter = null;
        }
    }

    /**
     * Method to initialise the context for use.
     * This creates the required StoreManager(s).
     */
    public synchronized void initialise()
    {
        if (type == ContextType.PERSISTENCE)
        {
            ClassLoaderResolver clr = getClassLoaderResolver(null);
            clr.registerUserClassLoader((ClassLoader)config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY));

            boolean generateSchema = false;
            boolean generateScripts = false;
            String generateModeStr = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE);
            if (generateModeStr == null || generateModeStr.equalsIgnoreCase("none"))
            {
                // Try scripts instead of database since that wasn't set
                generateModeStr = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_MODE);
                generateScripts = true;
            }
            if (generateModeStr != null && !generateModeStr.equalsIgnoreCase("none"))
            {
                generateSchema = true;

                // Add any properties that are needed by schema generation (before we create StoreManager)
                if (!config.getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_SCHEMA))
                {
                    config.setProperty(PropertyNames.PROPERTY_AUTOCREATE_SCHEMA, "true");
                }
                if (!config.getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_TABLES))
                {
                    config.setProperty(PropertyNames.PROPERTY_AUTOCREATE_TABLES, "true");
                }
                if (!config.getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_COLUMNS))
                {
                    config.setProperty(PropertyNames.PROPERTY_AUTOCREATE_COLUMNS, "true");
                }
                if (!config.getBooleanProperty(PropertyNames.PROPERTY_AUTOCREATE_CONSTRAINTS))
                {
                    config.setProperty(PropertyNames.PROPERTY_AUTOCREATE_CONSTRAINTS, "true");
                }
                if (!config.getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_FIXED))
                {
                    config.setProperty(PropertyNames.PROPERTY_DATASTORE_FIXED, "false");
                }
                if (!config.getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY))
                {
                    config.setProperty(PropertyNames.PROPERTY_DATASTORE_READONLY, "false");
                }
            }

            // Create the StoreManager
            Set<String> propNamesWithDatastore = config.getPropertyNamesWithPrefix("datanucleus.datastore.");
            if (propNamesWithDatastore == null)
            {
                // Find the StoreManager using the persistence property if specified
                NucleusLogger.DATASTORE.debug("Creating StoreManager for datastore");
                Map<String, Object> datastoreProps = config.getDatastoreProperties();
                this.storeMgr = createStoreManagerForProperties(
                    config.getPersistenceProperties(), datastoreProps, clr, this);

                // Make sure the isolation level is valid for this StoreManager and correct if necessary
                String transactionIsolation = config.getStringProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION);
                if (transactionIsolation != null)
                {
                    String reqdIsolation = getTransactionIsolationForStoreManager(storeMgr, transactionIsolation);
                    if (!transactionIsolation.equalsIgnoreCase(reqdIsolation))
                    {
                        config.setProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION, reqdIsolation);
                    }
                }
            }
            else
            {
                NucleusLogger.DATASTORE.debug("Creating FederatedStoreManager to handle federation of primary StoreManager and " + propNamesWithDatastore.size() + " secondary datastores");
                this.storeMgr = new FederatedStoreManager(clr, this);
            }
            NucleusLogger.DATASTORE.debug("StoreManager now created");

            // ========== Initialise the StoreManager contents ==========
            // A). Load any classes specified by auto-start
            if (config.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MECHANISM) != null)
            {
                initialiseAutoStart(clr);
            }

            // B). Schema Generation
            if (generateSchema)
            {
                initialiseSchema(generateModeStr, generateScripts);
            }

            // C). Load up internal representations of all persistence-unit classes
            if (config.getStringProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME) != null &&
                config.getBooleanProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_LOAD_CLASSES))
            {
                // User is using a persistence-unit, so load up its persistence-unit classes into StoreManager
                Collection<String> loadedClasses = getMetaDataManager().getClassesWithMetaData();
                this.storeMgr.addClasses(loadedClasses.toArray(new String[loadedClasses.size()]), clr);
            }
            // ==========================================================

            if (config.getBooleanProperty(PropertyNames.PROPERTY_QUERY_COMPILE_NAMED_QUERIES_AT_STARTUP))
            {
                initialiseNamedQueries(clr);
            }

            if (ecPool == null)
            {
                ecPool = new ExecutionContextPool(this);
            }
            if (opFactory == null)
            {
                opFactory = new ObjectProviderFactoryImpl(this);
            }
        }

        logConfiguration();
    }

    /**
     * Method to initialise the auto-start mechanism, loading up the classes
     * from its store into memory so that we start from what is required to be loaded.
     * @param clr The ClassLoaderResolver
     * @throws DatastoreInitialisationException
     */
    protected void initialiseAutoStart(ClassLoaderResolver clr)
    throws DatastoreInitialisationException
    {
        String autoStartMechanism = config.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MECHANISM);
        String autoStarterClassName =
            getPluginManager().getAttributeValueForExtension("org.datanucleus.autostart", 
                "name", autoStartMechanism, "class-name");
        if (autoStarterClassName != null)
        {
            String mode = config.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MODE);
            Class[] argsClass = new Class[] {ClassConstants.STORE_MANAGER, ClassConstants.CLASS_LOADER_RESOLVER};
            Object[] args = new Object[] {storeMgr, clr};
            try
            {
                starter = (AutoStartMechanism) getPluginManager().createExecutableExtension(
                    "org.datanucleus.autostart", "name", autoStartMechanism, "class-name", argsClass, args);
                if (mode.equalsIgnoreCase("None"))
                {
                    starter.setMode(org.datanucleus.store.autostart.AutoStartMechanism.Mode.NONE);
                }
                else if (mode.equalsIgnoreCase("Checked"))
                {
                    starter.setMode(org.datanucleus.store.autostart.AutoStartMechanism.Mode.CHECKED);
                }
                else if (mode.equalsIgnoreCase("Quiet"))
                {
                    starter.setMode(org.datanucleus.store.autostart.AutoStartMechanism.Mode.QUIET);
                }
                else if (mode.equalsIgnoreCase("Ignored"))
                {
                    starter.setMode(org.datanucleus.store.autostart.AutoStartMechanism.Mode.IGNORED);
                }
            }
            catch (Exception e)
            {
                NucleusLogger.PERSISTENCE.error(StringUtils.getStringFromStackTrace(e));
            }
        }
        if (starter == null)
        {
            return;
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("034005", autoStartMechanism));
        }
        boolean illegalState = false;
        try
        {
            if (!starter.isOpen())
            {
                starter.open();
            }
            Collection existingData = starter.getAllClassData();
            if (existingData != null && existingData.size() > 0)
            {
                List classesNeedingAdding = new ArrayList();
                Iterator existingDataIter = existingData.iterator();
                while (existingDataIter.hasNext())
                {
                    StoreData data = (StoreData) existingDataIter.next();
                    if (data.isFCO())
                    {
                        // Catch classes that don't exist (e.g in use by a different app)
                        Class classFound = null;
                        try
                        {
                            classFound = clr.classForName(data.getName());
                        }
                        catch (ClassNotResolvedException cnre)
                        {
                            if (data.getInterfaceName() != null)
                            {
                                try
                                {
                                    getImplementationCreator().newInstance(clr.classForName(data.getInterfaceName()), clr);
                                    classFound = clr.classForName(data.getName());
                                }
                                catch (ClassNotResolvedException cnre2)
                                {
                                    // Do nothing
                                }
                            }
                            // Thrown if class not found
                        }

                        if (classFound != null)
                        {
                            NucleusLogger.PERSISTENCE.info(LOCALISER.msg("032003", data.getName()));
                            classesNeedingAdding.add(data.getName());
                            if (data.getMetaData() == null)
                            {
                                // StoreData doesnt have its metadata set yet so load it
                                // This ensures that the MetaDataManager always knows about these classes
                                AbstractClassMetaData acmd =
                                    getMetaDataManager().getMetaDataForClass(classFound, clr);
                                if (acmd != null)
                                {
                                    data.setMetaData(acmd);
                                }
                                else
                                {
                                    String msg = LOCALISER.msg("034004", data.getName());
                                    if (starter.getMode() == AutoStartMechanism.Mode.CHECKED)
                                    {
                                        NucleusLogger.PERSISTENCE.error(msg);
                                        throw new DatastoreInitialisationException(msg);
                                    }
                                    else if (starter.getMode() == AutoStartMechanism.Mode.IGNORED)
                                    {
                                        NucleusLogger.PERSISTENCE.warn(msg);
                                    }
                                    else if (starter.getMode() == AutoStartMechanism.Mode.QUIET)
                                    {
                                        NucleusLogger.PERSISTENCE.warn(msg);
                                        NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("034001", data.getName()));
                                        starter.deleteClass(data.getName());
                                    }
                                }
                            }
                        }
                        else
                        {
                            String msg = LOCALISER.msg("034000", data.getName());
                            if (starter.getMode() == AutoStartMechanism.Mode.CHECKED)
                            {
                                NucleusLogger.PERSISTENCE.error(msg);
                                throw new DatastoreInitialisationException(msg);
                            }
                            else if (starter.getMode() == AutoStartMechanism.Mode.IGNORED)
                            {
                                NucleusLogger.PERSISTENCE.warn(msg);
                            }
                            else if (starter.getMode() == AutoStartMechanism.Mode.QUIET)
                            {
                                NucleusLogger.PERSISTENCE.warn(msg);
                                NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("034001", data.getName()));
                                starter.deleteClass(data.getName());
                            }
                        }
                    }
                }
                String[] classesToLoad = new String[classesNeedingAdding.size()];
                Iterator classesNeedingAddingIter = classesNeedingAdding.iterator();
                int n = 0;
                while (classesNeedingAddingIter.hasNext())
                {
                    classesToLoad[n++] = (String)classesNeedingAddingIter.next();
                }

                // Load the classes into the StoreManager
                try
                {
                    storeMgr.addClasses(classesToLoad, clr);
                }
                catch (Exception e)
                {
                    // Exception while adding so some of the (referenced) classes dont exist
                    NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("034002", e));
                    //if an exception happens while loading AutoStart data, them we disable it, since
                    //it was unable to load the data from AutoStart. The memory state of AutoStart does
                    //not represent the database, and if we don't disable it, we could
                    //think that the autostart store is empty, and we would try to insert new entries in
                    //the autostart store that are already in there
                    illegalState = true;

                    // TODO Go back and add classes one-by-one to eliminate the class(es) with the problem
                }
            }
        }
        finally
        {
            if (starter.isOpen())
            {
                starter.close();
            }
            if (illegalState)
            {
                NucleusLogger.PERSISTENCE.warn(LOCALISER.msg("034003"));
                starter = null;
            }
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("034006", autoStartMechanism));
            }
        }
    }

    public AutoStartMechanism getAutoStartMechanism()
    {
        return starter;
    }

    /**
     * Method to compile all registered named queries (when the user has initialised using a persistence-unit).
     * @param clr ClassLoader resolver
     */
    protected void initialiseNamedQueries(ClassLoaderResolver clr)
    {
        MetaDataManager mmgr = getMetaDataManager();
        Set<String> queryNames = mmgr.getNamedQueryNames();
        if (queryNames != null)
        {
            // Compile all named queries of JDOQL/JPQL language using dummy ExecutionContext
            ExecutionContext ec = getExecutionContext(null, null);
            for (String queryName : queryNames)
            {
                QueryMetaData qmd = mmgr.getMetaDataForQuery(null, clr, queryName);
                if (qmd.getLanguage().equals(QueryLanguage.JPQL.toString()) ||
                    qmd.getLanguage().equals(QueryLanguage.JDOQL.toString()))
                {
                    if (NucleusLogger.QUERY.isDebugEnabled())
                    {
                        NucleusLogger.QUERY.debug(LOCALISER.msg("008017", queryName, qmd.getQuery()));
                    }
                    org.datanucleus.store.query.Query q = storeMgr.getQueryManager().newQuery(
                        qmd.getLanguage().toString(), ec, qmd.getQuery());
                    q.compile();
                    q.closeAll();
                }
            }
            ec.close();
        }
    }

    /**
     * Method to handle generation (create, drop, drop+create) of a schema at initialisation.
     * Will generate the schema for all classes that have had their metadata loaded at this point, which
     * typically means the persistence-unit.
     * @param generateModeStr Generate "mode"
     */
    protected void initialiseSchema(String generateModeStr, boolean generateScripts)
    {
        Mode mode = null;
        if (generateModeStr.equalsIgnoreCase("create"))
        {
            mode = Mode.CREATE;
        }
        else if (generateModeStr.equalsIgnoreCase("drop"))
        {
            mode = Mode.DELETE;
        }
        else if (generateModeStr.equalsIgnoreCase("drop-and-create"))
        {
            mode = Mode.DELETE_CREATE;
        }
        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            if (mode == Mode.CREATE)
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("014000"));
            }
            else if (mode == Mode.DELETE)
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("014001"));
            }
            else if (mode == Mode.DELETE_CREATE)
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("014045"));
            }
        }

        // Extract the classes that have metadata loaded (e.g persistence-unit)
        Set<String> schemaClassNames = null;
        MetaDataManager metaDataMgr = getMetaDataManager();
        FileMetaData[] filemds = metaDataMgr.getFileMetaData();
        schemaClassNames = new TreeSet<String>();
        if (filemds == null)
        {
            throw new NucleusUserException("No classes to process in generateSchema");
        }

        for (int i=0;i<filemds.length;i++)
        {
            for (int j=0;j<filemds[i].getNoOfPackages();j++)
            {
                for (int k=0;k<filemds[i].getPackage(j).getNoOfClasses();k++)
                {
                    String className = filemds[i].getPackage(j).getClass(k).getFullClassName();
                    if (!schemaClassNames.contains(className))
                    {
                        schemaClassNames.add(className);
                    }
                }
            }
        }

        StoreManager storeMgr = getStoreManager();
        if (storeMgr instanceof SchemaAwareStoreManager)
        {
            SchemaAwareStoreManager schemaStoreMgr = (SchemaAwareStoreManager) storeMgr;
            SchemaTool schemaTool = new SchemaTool();

            if (mode == Mode.CREATE)
            {
                String createScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_SOURCE);
                if (!StringUtils.isWhitespace(createScript))
                {
                    // TODO Make use of create_script ORDER
                    String scriptContent = getDatastoreScriptForResourceName(createScript);
                    NucleusLogger.DATASTORE_SCHEMA.debug(">> createScript=" + scriptContent);
                    if (storeMgr instanceof SchemaScriptAwareStoreManager && !StringUtils.isWhitespace(scriptContent))
                    {
                        ((SchemaScriptAwareStoreManager)storeMgr).executeScript(scriptContent);
                    }
                }
                if (generateScripts)
                {
                    schemaTool.setDdlFile(config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_TARGET));
                }
                schemaTool.createSchema(schemaStoreMgr, schemaClassNames);
            }
            else if (mode == Mode.DELETE)
            {
                // TODO Make use of drop_script ORDER
                String dropScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_SOURCE);
                if (!StringUtils.isWhitespace(dropScript))
                {
                    String scriptContent = getDatastoreScriptForResourceName(dropScript);
                    NucleusLogger.DATASTORE_SCHEMA.debug(">> dropScript=" + scriptContent);
                    if (storeMgr instanceof SchemaScriptAwareStoreManager && !StringUtils.isWhitespace(scriptContent))
                    {
                        ((SchemaScriptAwareStoreManager)storeMgr).executeScript(scriptContent);
                    }
                }
                if (generateScripts)
                {
                    schemaTool.setDdlFile(config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_TARGET));
                }
                schemaTool.deleteSchema(schemaStoreMgr, schemaClassNames);
            }
            else if (mode == Mode.DELETE_CREATE)
            {
                String dropScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_SOURCE);
                if (!StringUtils.isWhitespace(dropScript))
                {
                    String scriptContent = getDatastoreScriptForResourceName(dropScript);
                    NucleusLogger.DATASTORE_SCHEMA.debug(">> dropScript=" + scriptContent);
                    if (storeMgr instanceof SchemaScriptAwareStoreManager && !StringUtils.isWhitespace(scriptContent))
                    {
                        ((SchemaScriptAwareStoreManager)storeMgr).executeScript(scriptContent);
                    }
                }
                if (generateScripts)
                {
                    schemaTool.setDdlFile(config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_TARGET));
                }
                schemaTool.deleteSchema(schemaStoreMgr, schemaClassNames);

                String createScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_SOURCE);
                if (!StringUtils.isWhitespace(createScript))
                {
                    String scriptContent = getDatastoreScriptForResourceName(createScript);
                    NucleusLogger.DATASTORE_SCHEMA.debug(">> dropScript=" + scriptContent);
                    if (storeMgr instanceof SchemaScriptAwareStoreManager && !StringUtils.isWhitespace(scriptContent))
                    {
                        ((SchemaScriptAwareStoreManager)storeMgr).executeScript(scriptContent);
                    }
                }
                if (generateScripts)
                {
                    schemaTool.setDdlFile(config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_TARGET));
                }
                schemaTool.createSchema(schemaStoreMgr, schemaClassNames);
            }

            String loadScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_LOAD_SOURCE);
            if (!StringUtils.isWhitespace(loadScript))
            {
                String scriptContent = getDatastoreScriptForResourceName(loadScript);
                NucleusLogger.DATASTORE_SCHEMA.debug(">> loadScript=" + scriptContent);
                if (storeMgr instanceof SchemaScriptAwareStoreManager && !StringUtils.isWhitespace(scriptContent))
                {
                    ((SchemaScriptAwareStoreManager)storeMgr).executeScript(scriptContent);
                }
            }
        }
        else
        {
            if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("008016", StringUtils.toJVMIDString(storeMgr)));
            }
        }

        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            NucleusLogger.DATASTORE_SCHEMA.debug(LOCALISER.msg("014043"));
        }
    }

    private String getDatastoreScriptForResourceName(String scriptResourceName)
    {
        if (StringUtils.isWhitespace(scriptResourceName))
        {
            return null;
        }

        // Try as a File
        File file = new File(scriptResourceName);
        if (!file.exists())
        {
            try
            {
                URI uri = new URI(scriptResourceName);
                file = new File(uri);
            }
            catch (Exception e)
            {
            }
        }

        if (file != null && file.exists())
        {
            FileInputStream fis = null;
            try
            {
                StringBuilder str = new StringBuilder();
                fis = new FileInputStream(file);
                int content;
                while ((content = fis.read()) != -1)
                {
                    str.append((char)content);
                }
                return str.toString();
            }
            catch (Exception e)
            {
            }
            finally
            {
                if (fis != null)
                {
                    try
                    {
                        fis.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
        }

        NucleusLogger.DATASTORE_SCHEMA.info("Datastore script " + scriptResourceName + 
            " was not a valid script; has to be a filename, or a URL string");
        return null;
    }

    /**
     * Clear out resources
     */
    public synchronized void close()
    {
        if (opFactory != null)
        {
            opFactory.close();
            opFactory = null;
        }
        if (ecPool != null)
        {
            ecPool.cleanUp();
            ecPool = null;
        }
        if (fetchGrpMgr != null)
        {
            fetchGrpMgr.clearFetchGroups();
        }
        if (storeMgr != null)
        {
            storeMgr.close();
            storeMgr = null;
        }
        if (metaDataManager != null)
        {
            metaDataManager.close();
            metaDataManager = null;
        }
        if (statistics != null)
        {
            if (jmxManager != null)
            {
                jmxManager.deregisterMBean(statistics.getRegisteredName());
            }
            statistics = null;
        }
        if (jmxManager != null)
        {
            jmxManager.close();
            jmxManager = null;
        }

        if (cache != null)
        {
            // Close the L2 Cache
            cache.close();
            NucleusLogger.CACHE.debug(LOCALISER.msg("004009"));
        }

        classLoaderResolverMap.clear();
        classLoaderResolverMap = null;

        datastoreIdentityClass = null;
    }

    public ExecutionContextPool getExecutionContextPool()
    {
        if (ecPool == null)
        {
            initialise();
        }
        return ecPool;
    }

    /**
     * Accessor for the type of this context (persistence, enhancer etc).
     * @return The type
     */
    public ContextType getType()
    {
        return type;
    }

    /**
     * Accessor for the ApiAdapter
     * @return the ApiAdapter
     */
    public ApiAdapter getApiAdapter()
    {
        return apiAdapter;
    }

    /**
     * Accessor for the name of the API (JDO, JPA, etc).
     * @return the api
     */
    public String getApiName()
    {
        return (apiAdapter != null ? apiAdapter.getName() : null);
    }

    /**
     * Accessor for the persistence configuration.
     * @return Returns the persistence configuration.
     */
    public PersistenceConfiguration getPersistenceConfiguration()
    {
        return config;
    }

    /**
     * Accessor for the Plugin Manager
     * @return the PluginManager
     */
    public PluginManager getPluginManager()
    {
        return pluginManager;
    }

    /**
     * Accessor for the Meta-Data Manager.
     * @return Returns the MetaDataManager.
     */
    public synchronized MetaDataManager getMetaDataManager()
    {
        if (metaDataManager == null)
        {
            String apiName = getApiName();
            try
            {
                metaDataManager = (MetaDataManager) getPluginManager().createExecutableExtension(
                    "org.datanucleus.metadata_manager", new String[]{"name"}, new String[]{apiName}, 
                    "class", new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[]{this});
            }
            catch (Exception e)
            {
                throw new NucleusException(LOCALISER.msg("008010", apiName, e.getMessage()), e);
            }
            if (metaDataManager == null)
            {
                throw new NucleusException(LOCALISER.msg("008009", apiName));
            }
        }

        return metaDataManager;
    }

    /**
     * Accessor for the Type Manager
     * @return the TypeManager
     */
    public TypeManager getTypeManager()
    {
        if (typeManager == null)
        {
            this.typeManager = new TypeManager(this);
        }
        return typeManager;
    }

    public ObjectProviderFactory getObjectProviderFactory()
    {
        if (opFactory == null)
        {
            initialise();
        }
        return opFactory;
    }

    /**
     * Method to return an ExecutionContext for use in persistence.
     * @param owner The owner object for the context. A PM for example
     * @param options Any options affecting startup
     * @return The ExecutionContext
     */
    public ExecutionContext getExecutionContext(Object owner, Map<String, Object> options)
    {
        return ecPool.checkOut(owner, options);
    }

    /**
     * Accessor for a ClassLoaderResolver to use in resolving classes.
     * Caches the resolver for the specified primary loader, and hands it out if present.
     * @param primaryLoader Loader to use as the primary loader (or null)
     * @return The ClassLoader resolver
     */
    public ClassLoaderResolver getClassLoaderResolver(ClassLoader primaryLoader)
    {
        // Set the key we will refer to this loader by
        String resolverName = config.getStringProperty(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME);
        String key = resolverName;
        if (primaryLoader != null)
        {
            key += ":[" + StringUtils.toJVMIDString(primaryLoader) + "]"; 
        }

        if (classLoaderResolverMap == null)
        {
            classLoaderResolverMap = new HashMap<String, ClassLoaderResolver>();
        }

        ClassLoaderResolver clr = classLoaderResolverMap.get(key);
        if (clr != null)
        {
            // Return the cached loader resolver
            return clr;
        }

        // Create the ClassLoaderResolver of this type with this primary loader
        try
        {
            clr = (ClassLoaderResolver)pluginManager.createExecutableExtension(
                "org.datanucleus.classloader_resolver", "name", resolverName,
                "class-name", new Class[] {ClassLoader.class}, new Object[] {primaryLoader});
            clr.registerUserClassLoader((ClassLoader)config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY));
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new NucleusUserException(LOCALISER.msg("001002", classLoaderResolverClassName), cnfe).setFatal();
        }
        catch (Exception e)
        {
            throw new NucleusUserException(LOCALISER.msg("001003", classLoaderResolverClassName), e).setFatal();
        }
        classLoaderResolverMap.put(key, clr);

        return clr;
    }

    /**
     * Method to return the transaction isolation level that will be used for the provided StoreManager
     * bearing in mind the specified level the user requested.
     * @param storeMgr The Store Manager
     * @param transactionIsolation Requested isolation level
     * @return Isolation level to use
     * @throws TransactionIsolationNotSupportedException When no suitable level available given the requested level
     */
    public static String getTransactionIsolationForStoreManager(StoreManager storeMgr, String transactionIsolation)
    {
        if (transactionIsolation != null)
        {
            // Transaction isolation has been specified and we need to provide at least this level
            // Order of priority is :-
            // read-uncommitted (lowest), read-committed, repeatable-read, serializable (highest)
            Collection srmOptions = storeMgr.getSupportedOptions();
            if (!srmOptions.contains("TransactionIsolationLevel." + transactionIsolation))
            {
                // Requested transaction isolation isn't supported by datastore so check for higher
                if (transactionIsolation.equals("read-uncommitted"))
                {
                    if (srmOptions.contains("TransactionIsolationLevel.read-committed"))
                    {
                        return "read-committed";
                    }
                    else if (srmOptions.contains("TransactionIsolationLevel.repeatable-read"))
                    {
                        return "repeatable-read";
                    }
                    else if (srmOptions.contains("TransactionIsolationLevel.serializable"))
                    {
                        return "serializable";
                    }
                }
                else if (transactionIsolation.equals("read-committed"))
                {
                    if (srmOptions.contains("TransactionIsolationLevel.repeatable-read"))
                    {
                        return "repeatable-read";
                    }
                    else if (srmOptions.contains("TransactionIsolationLevel.serializable"))
                    {
                        return "serializable";
                    }
                }
                else if (transactionIsolation.equals("repeatable-read"))
                {
                    if (srmOptions.contains("TransactionIsolationLevel.serializable"))
                    {
                        return "serializable";
                    }
                }
                else
                {
                    throw new TransactionIsolationNotSupportedException(transactionIsolation);
                }
            }
        }
        return transactionIsolation;
    }

    /**
     * Method to create a StoreManager based on the specified properties passed in.
     * @param props The overall persistence properties
     * @param datastoreProps Persistence properties to apply to the datastore
     * @param clr ClassLoader resolver
     * @return The StoreManager
     * @throws NucleusUserException if impossible to create the StoreManager (not in CLASSPATH?, invalid definition?)
     */
    public static StoreManager createStoreManagerForProperties(Map<String, Object> props,
            Map<String, Object> datastoreProps, ClassLoaderResolver clr,
            NucleusContext nucCtx)
    {
        Extension[] exts = nucCtx.getPluginManager().getExtensionPoint("org.datanucleus.store_manager").getExtensions();
        Class[] ctrArgTypes = new Class[] {ClassConstants.CLASS_LOADER_RESOLVER, ClassConstants.NUCLEUS_CONTEXT, Map.class};
        Object[] ctrArgs = new Object[] {clr, nucCtx, datastoreProps};

        StoreManager storeMgr = null;

        String storeManagerType = (String) props.get(PropertyNames.PROPERTY_STORE_MANAGER_TYPE.toLowerCase());
        if (storeManagerType != null)
        {
            // User defined the store manager type, so find the appropriate plugin
            for (int e=0; storeMgr == null && e<exts.length; e++)
            {
                ConfigurationElement[] confElm = exts[e].getConfigurationElements();
                for (int c=0; storeMgr == null && c<confElm.length; c++)
                {
                    String key = confElm[c].getAttribute("key");
                    if (key.equalsIgnoreCase(storeManagerType))
                    {
                        try
                        {
                            storeMgr = (StoreManager)nucCtx.getPluginManager().createExecutableExtension(
                                "org.datanucleus.store_manager", "key", storeManagerType, 
                                "class-name", ctrArgTypes, ctrArgs);
                        }
                        catch (InvocationTargetException ex)
                        {
                            Throwable t = ex.getTargetException();
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
                                throw new NucleusException(t.getMessage(), t).setFatal();
                            }
                        }
                        catch (Exception ex)
                        {
                            throw new NucleusException(ex.getMessage(), ex).setFatal();
                        }
                    }
                }
            }
            if (storeMgr == null)
            {
                // No StoreManager of the specified type exists in the CLASSPATH!
                throw new NucleusUserException(LOCALISER.msg("008004", storeManagerType)).setFatal();
            }
        }

        if (storeMgr == null)
        {
            // Try using the URL of the data source
            String url = (String) props.get("datanucleus.connectionurl");
            if (url != null)
            {
                int idx = url.indexOf(':');
                if (idx > -1)
                {
                    url = url.substring(0, idx);
                }
            }

            for (int e=0; storeMgr == null && e<exts.length; e++)
            {
                ConfigurationElement[] confElm = exts[e].getConfigurationElements();
                for (int c=0; storeMgr == null && c<confElm.length; c++)
                {
                    String urlKey = confElm[c].getAttribute("url-key");
                    if (url == null || urlKey.equalsIgnoreCase(url))
                    {
                        // Either no URL, or url defined so take this StoreManager
                        try
                        {
                            storeMgr = (StoreManager)nucCtx.getPluginManager().createExecutableExtension(
                                "org.datanucleus.store_manager", "url-key", url == null ? urlKey : url, 
                                "class-name", ctrArgTypes, ctrArgs);
                        }
                        catch (InvocationTargetException ex)
                        {
                            Throwable t = ex.getTargetException();
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
                                throw new NucleusException(t.getMessage(), t).setFatal();
                            }
                        }
                        catch (Exception ex)
                        {
                            throw new NucleusException(ex.getMessage(), ex).setFatal();
                        }
                    }
                }
            }

            if (storeMgr == null)
            {
                throw new NucleusUserException(LOCALISER.msg("008004", url)).setFatal();
            }
        }

        return storeMgr;
    }

    /**
     * Method to log the configuration of this context.
     */
    protected void logConfiguration()
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug("================= NucleusContext ===============");
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("008000", pluginManager.getVersionForBundle("org.datanucleus"), 
                System.getProperty("java.version"), System.getProperty("os.name")));
            NucleusLogger.PERSISTENCE.debug("Persistence API : " + getApiName());
            if (config.hasPropertyNotNull(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME))
            {
                NucleusLogger.PERSISTENCE.debug("Persistence-Unit : " + config.getStringProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME));
            }
            NucleusLogger.PERSISTENCE.debug("Plugin Registry : " + pluginManager.getRegistryClassName());
            Object primCL = config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY);
            NucleusLogger.PERSISTENCE.debug("ClassLoading : " + config.getStringProperty(PropertyNames.PROPERTY_CLASSLOADER_RESOLVER_NAME) +
                (primCL != null ? ("primary=" + primCL) : ""));

            if (type == ContextType.PERSISTENCE)
            {
                String timeZoneID = config.getStringProperty(PropertyNames.PROPERTY_SERVER_TIMEZONE_ID);
                if (timeZoneID == null)
                {
                    timeZoneID = TimeZone.getDefault().getID();
                }
                NucleusLogger.PERSISTENCE.debug("Persistence : " + 
                        (config.getBooleanProperty(PropertyNames.PROPERTY_MULTITHREADED) ? "pm-multithreaded" : "pm-singlethreaded") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_RETAIN_VALUES) ? ", retain-values" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_RESTORE_VALUES) ? ", restore-values" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_NONTX_READ) ? ", nontransactional-read" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_NONTX_WRITE) ? ", nontransactional-write" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_PERSISTENCE_BY_REACHABILITY_AT_COMMIT) ? ", reachability-at-commit" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT) ? ", detach-all-on-commit" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK) ? ", detach-all-on-rollback" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_DETACH_ON_CLOSE) ? ", detach-on-close" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_COPY_ON_ATTACH) ? ", copy-on-attach" : "") +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS) ?
                                (config.getBooleanProperty(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS_CHECKS) ? ", managed-relations(checked)" : ", managed-relations(unchecked)") : "") +
                        ", deletion-policy=" + config.getStringProperty(PropertyNames.PROPERTY_DELETION_POLICY) +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_IGNORE_CACHE) ? ", ignoreCache" : "") +
                        ", serverTimeZone=" + timeZoneID);

                String txnType = "RESOURCE_LOCAL";
                if (TransactionType.JTA.toString().equalsIgnoreCase(config.getStringProperty(PropertyNames.PROPERTY_TRANSACTION_TYPE)))
                {
                    if (isJcaMode())
                    {
                        txnType = "JTA (via JCA adapter)";
                    }
                    else
                    {
                        txnType = "JTA";
                    }
                }

                String autoStartMechanism = config.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MECHANISM);
                if (autoStartMechanism != null)
                {
                    String autoStartClassNames = config.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_CLASSNAMES);
                    NucleusLogger.PERSISTENCE.debug("AutoStart : mechanism=" + autoStartMechanism +
                        ", mode=" + config.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MODE) +
                        ((autoStartClassNames != null) ? (", classes=" + autoStartClassNames) : ""));
                }

                NucleusLogger.PERSISTENCE.debug("Transactions : type=" + txnType +
                    ", mode=" + (config.getBooleanProperty(PropertyNames.PROPERTY_OPTIMISTIC) ? "optimistic" : "datastore") +
                    ", isolation=" + config.getStringProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION));
                NucleusLogger.PERSISTENCE.debug("ValueGeneration :" +
                        " txn-isolation=" + config.getStringProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ISOLATION) +
                        " connection=" + (config.getStringProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ATTRIBUTE).equalsIgnoreCase("New") ? "New" : "Existing"));
                NucleusLogger.PERSISTENCE.debug("Cache : Level1 (" + config.getStringProperty(PropertyNames.PROPERTY_CACHE_L1_TYPE) + ")" +
                        ", Level2 (" + config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_TYPE) + 
                        ", mode=" + config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_MODE) + ")" +
                        ", QueryResults (" + config.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_TYPE) + ")" +
                        (config.getBooleanProperty(PropertyNames.PROPERTY_CACHE_COLLECTIONS) ? ", Collections/Maps " : ""));
            }
            NucleusLogger.PERSISTENCE.debug("================================================");
        }
    }

    /**
     * Accessor for the class to use for datastore identity.
     * @return Class for datastore-identity
     */
    public synchronized Class getDatastoreIdentityClass()
    {
        if (datastoreIdentityClass == null)
        {
            String dsidName = config.getStringProperty(PropertyNames.PROPERTY_DATASTORE_IDENTITY_TYPE);
            String datastoreIdentityClassName = pluginManager.getAttributeValueForExtension(
                "org.datanucleus.store_datastoreidentity", "name", dsidName, "class-name");
            if (datastoreIdentityClassName == null)
            {
                // User has specified a datastore_identity plugin that has not registered
                throw new NucleusUserException(LOCALISER.msg("002001", dsidName)).setFatal();
            }

            // Try to load the class
            ClassLoaderResolver clr = getClassLoaderResolver(null);
            try
            {
                datastoreIdentityClass = clr.classForName(datastoreIdentityClassName, org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);
            }
            catch (ClassNotResolvedException cnre)
            {
                throw new NucleusUserException(LOCALISER.msg("002002", dsidName, 
                    datastoreIdentityClassName)).setFatal();
            }
        }
        return datastoreIdentityClass;
    }

    /**
     * Accessor for the current identity string translator to use (if any).
     * @return Identity string translator instance (or null if persistence property not set)
     */
    public synchronized IdentityStringTranslator getIdentityStringTranslator()
    {
        if (idStringTranslatorInit)
        {
            return idStringTranslator;
        }

        // Identity translation
        idStringTranslatorInit = true;
        String translatorType = config.getStringProperty(PropertyNames.PROPERTY_IDENTITY_STRING_TRANSLATOR_TYPE);
        if (translatorType != null)
        {
            try
            {
                idStringTranslator = (IdentityStringTranslator)pluginManager.createExecutableExtension(
                    "org.datanucleus.identity_string_translator", "name", translatorType, "class-name", null, null);
                return idStringTranslator;
            }
            catch (Exception e)
            {
                // User has specified a string identity translator plugin that has not registered
                throw new NucleusUserException(LOCALISER.msg("002001", translatorType)).setFatal();
            }
        }
        return null;
    }

    /**
     * Accessor for the current identity key translator to use (if any).
     * @return Identity key translator instance (or null if persistence property not set)
     */
    public synchronized IdentityKeyTranslator getIdentityKeyTranslator()
    {
        if (idKeyTranslatorInit)
        {
            return idKeyTranslator;
        }

        // Identity key translation
        idKeyTranslatorInit = true;
        String translatorType = config.getStringProperty(PropertyNames.PROPERTY_IDENTITY_KEY_TRANSLATOR_TYPE);
        if (translatorType != null)
        {
            try
            {
                idKeyTranslator = (IdentityKeyTranslator)pluginManager.createExecutableExtension(
                    "org.datanucleus.identity_key_translator", "name", translatorType, "class-name", null, null);
                return idKeyTranslator;
            }
            catch (Exception e)
            {
                // User has specified a identity key translator plugin that has not registered
                throw new NucleusUserException(LOCALISER.msg("002001", translatorType)).setFatal();
            }
        }
        return null;
    }

    /**
     * Accessor for whether statistics gathering is enabled.
     * @return Whether the user has enabled statistics or JMX management is enabled
     */
    public boolean statisticsEnabled()
    {
        return config.getBooleanProperty(PropertyNames.PROPERTY_ENABLE_STATISTICS) || getJMXManager() != null;
    }

    /**
     * Accessor for the JMX manager (if required).
     * Does nothing if the property "datanucleus.jmxType" is unset.
     * @return The JMX manager
     */
    public synchronized ManagementManager getJMXManager()
    {
        if (jmxManager == null && config.getStringProperty(PropertyNames.PROPERTY_JMX_TYPE) != null)
        {
            // User requires managed runtime, and not yet present so create manager
            jmxManager = new ManagementManager(this);
        }
        return jmxManager;
    }

    public synchronized FactoryStatistics getStatistics()
    {
        if (statistics == null && statisticsEnabled())
        {
            String name = null;
            if (getJMXManager() != null)
            {
                // Register the MBean with the active JMX manager
                name = jmxManager.getDomainName() + ":InstanceName=" + jmxManager.getInstanceName() +
                    ",Type=" + FactoryStatistics.class.getName() +
                    ",Name=Factory" + random.nextInt();
            }
            statistics = new FactoryStatistics(name);
            if (jmxManager != null)
            {
                // Register the MBean with the active JMX manager
                jmxManager.registerMBean(this.statistics, name);
            }
        }
        return statistics;
    }

    public synchronized ImplementationCreator getImplementationCreator()
    {
        if (implCreator == null)
        {
            boolean useImplCreator = config.getBooleanProperty(PropertyNames.PROPERTY_USE_IMPLEMENTATION_CREATOR);
            if (useImplCreator)
            {
                implCreator = new JDOImplementationCreator(getMetaDataManager());
            }
        }
        return implCreator;
    }

    /**
     * Accessor for the transaction manager.
     * @return The transaction manager.
     */
    public synchronized TransactionManager getTransactionManager()
    {
        if (txManager == null)
        {
            txManager = new TransactionManager();
        }
        return txManager;
    }

    /**
     * Accessor for the JTA transaction manager (if using JTA).
     * @return the JTA Transaction Manager
     */
    public synchronized javax.transaction.TransactionManager getJtaTransactionManager()
    {
        if (jtaTxManager == null)
        {
            // Find the JTA transaction manager
            // Before J2EE 5 there is no standard way to do this so use the finder process.
            // See http://www.onjava.com/pub/a/onjava/2005/07/20/transactions.html
            jtaTxManager = new TransactionManagerFinder(this).getTransactionManager(
                getClassLoaderResolver((ClassLoader)config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY)));
            if (jtaTxManager == null)
            {
                throw new NucleusTransactionException(LOCALISER.msg("015030"));
            }
        }
        return jtaTxManager;
    }

    public JTASyncRegistry getJtaSyncRegistry()
    {
        if (jtaSyncRegistry == null)
        {
            try
            {
                jtaSyncRegistry = new JTASyncRegistry();
            }
            catch (JTASyncRegistryUnavailableException jsrue)
            {
                NucleusLogger.TRANSACTION.debug("JTA TransactionSynchronizationRegistry not found at " +
                    "JNDI java:comp/TransactionSynchronizationRegistry so using Transaction to register synchronisation");
                jtaSyncRegistry = null;
            }
        }
        return jtaSyncRegistry;
    }

    public boolean isStoreManagerInitialised()
    {
        return (storeMgr != null);
    }

    /**
     * Accessor for the StoreManager
     * @return the StoreManager
     */
    public StoreManager getStoreManager()
    {
        if (storeMgr == null)
        {
            initialise();
        }
        return storeMgr;
    }

    /**
     * Method to return a handler for validation (JSR303).
     * @param ec The ExecutionContext that the handler is for.
     * @return The handler (or null if not supported on this PMF/EMF, or no validator present)
     */
    public CallbackHandler getValidationHandler(ExecutionContext ec)
    {
        if (validatorFactoryInit && validatorFactory == null)
        {
            return null;
        }

        if (config.hasPropertyNotNull(PropertyNames.PROPERTY_VALIDATION_MODE))
        {
            if (config.getStringProperty(PropertyNames.PROPERTY_VALIDATION_MODE).equalsIgnoreCase("none"))
            {
                validatorFactoryInit = true;
                return null;
            }
        }

        try
        {
            // Check on presence of validation API
            ec.getClassLoaderResolver().classForName("javax.validation.Validation");
        }
        catch (ClassNotResolvedException cnre)
        {
            validatorFactoryInit = true;
            return null;
        }

        try
        {
            if (validatorFactory == null)
            {
                validatorFactoryInit = true;

                // Factory not yet set so create it
                if (config.hasPropertyNotNull(PropertyNames.PROPERTY_VALIDATION_FACTORY))
                {
                    //create from javax.persistence.validation.factory if given
                    validatorFactory = config.getProperty(PropertyNames.PROPERTY_VALIDATION_FACTORY);
                }
                else
                {
                    validatorFactory = Validation.buildDefaultValidatorFactory();
                }
            }

            return new BeanValidatorHandler(ec, (ValidatorFactory)validatorFactory);
        }
        catch (Throwable ex) //throwable used to catch linkage errors
        {
            if (config.hasPropertyNotNull(PropertyNames.PROPERTY_VALIDATION_MODE))
            {
                if (config.getStringProperty(PropertyNames.PROPERTY_VALIDATION_MODE).equalsIgnoreCase("callback"))
                {
                    throw ec.getApiAdapter().getUserExceptionForException(ex.getMessage(), (Exception)ex);
                }
            }

            NucleusLogger.GENERAL.warn("Unable to create validator handler", ex);
        }
        return null;
    }

    /**
     * Return whether there is an L2 cache.
     * @return Whether the L2 cache is enabled
     */
    public boolean hasLevel2Cache()
    {
        getLevel2Cache();
        return !(cache instanceof NullLevel2Cache);
    }

    /**
     * Accessor for the DataStore (level 2) Cache
     * @return The datastore cache
     */
    public Level2Cache getLevel2Cache()
    {
        if (cache == null)
        {
            String level2Type = config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_TYPE);

            // Find the L2 cache class name from its plugin name
            String level2ClassName = pluginManager.getAttributeValueForExtension(
                "org.datanucleus.cache_level2", "name", level2Type, "class-name");
            if (level2ClassName == null)
            {
                // Plugin of this name not found
                throw new NucleusUserException(LOCALISER.msg("004000", level2Type)).setFatal();
            }

            try
            {
                // Create an instance of the L2 Cache
                cache = (Level2Cache)pluginManager.createExecutableExtension(
                    "org.datanucleus.cache_level2", "name", level2Type, "class-name",
                    new Class[]{ClassConstants.NUCLEUS_CONTEXT}, new Object[]{this});
                if (NucleusLogger.CACHE.isDebugEnabled())
                {
                    NucleusLogger.CACHE.debug(LOCALISER.msg("004002", level2Type));
                }
            }
            catch (Exception e)
            {
                // Class name for this L2 cache plugin is not found!
                throw new NucleusUserException(LOCALISER.msg("004001", level2Type, level2ClassName), e).setFatal();
            }
        }
        return cache;
    }

    /**
     * Object the array of registered ExecutionContext listeners.
     * @return array of {@link org.datanucleus.ExecutionContext.LifecycleListener}
     */
    public ExecutionContext.LifecycleListener[] getExecutionContextListeners()
    {
        return executionContextListeners.toArray(new ExecutionContext.LifecycleListener[executionContextListeners.size()]);
    }

    /**
     * Register a new Listener for ExecutionContext events.
     * @param listener the listener to register
     */
    public void addExecutionContextListener(ExecutionContext.LifecycleListener listener)
    {
        executionContextListeners.add(listener);
    }

    /**
     * Unregister a Listener from ExecutionContext events.
     * @param listener the listener to unregister
     */
    public void removeExecutionContextListener(ExecutionContext.LifecycleListener listener)
    {
        executionContextListeners.remove(listener);
    }

    /**
     * Mutator for whether we are in JCA mode.
     * @param jca true if using JCA connector
     */
    public synchronized void setJcaMode(boolean jca)
    {
        this.jca = jca;
    }

    /**
     * Accessor for the JCA mode.
     * @return true if using JCA connector.
     */
    public boolean isJcaMode()
    {
        return jca;
    }

    // --------------------------- Fetch Groups ---------------------------------

    /** 
     * Convenience accessor for the FetchGroupManager.
     * Creates it if not yet existing.
     * @return The FetchGroupManager
     */
    public synchronized FetchGroupManager getFetchGroupManager()
    {
        if (fetchGrpMgr == null)
        {
            fetchGrpMgr = new FetchGroupManager(this);
        }
        return fetchGrpMgr;
    }

    /**
     * Method to add a dynamic FetchGroup for use by this OMF.
     * @param grp The group
     */
    public void addInternalFetchGroup(FetchGroup grp)
    {
        getFetchGroupManager().addFetchGroup(grp);
    }

    /**
     * Method to remove a dynamic FetchGroup from use by this OMF.
     * @param grp The group
     */
    public void removeInternalFetchGroup(FetchGroup grp)
    {
        getFetchGroupManager().removeFetchGroup(grp);
    }

    /**
     * Method to create a new internal fetch group for the class+name.
     * @param cls Class that it applies to
     * @param name Name of group
     * @return The group
     */
    public FetchGroup createInternalFetchGroup(Class cls, String name)
    {
        if (!cls.isInterface() && !getApiAdapter().isPersistable(cls))
        {
            // Class but not persistable!
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        }
        else if (cls.isInterface() && !getMetaDataManager().isPersistentInterface(cls.getName()))
        {
            // Interface but not persistent
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        }
        return getFetchGroupManager().createFetchGroup(cls, name);
    }

    /**
     * Accessor for an internal fetch group for the specified class.
     * @param cls The class
     * @param name Name of the group
     * @return The FetchGroup
     * @throws NucleusUserException if the class is not persistable
     */
    public FetchGroup getInternalFetchGroup(Class cls, String name)
    {
        if (!cls.isInterface() && !getApiAdapter().isPersistable(cls))
        {
            // Class but not persistable!
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        }

        // Make sure metadata for this class is loaded
        getMetaDataManager().getMetaDataForClass(cls, getClassLoaderResolver(cls.getClassLoader()));
        if (cls.isInterface() && !getMetaDataManager().isPersistentInterface(cls.getName()))
        {
            // Interface but not persistent
            throw new NucleusUserException("Cannot create FetchGroup for " + cls + " since it is not persistable");
        }
        return getFetchGroupManager().getFetchGroup(cls, name);
    }

    /**
     * Accessor for the fetch groups for the specified name.
     * @param name Name of the group
     * @return The FetchGroup
     */
    public Set<FetchGroup> getFetchGroupsWithName(String name)
    {
        return getFetchGroupManager().getFetchGroupsWithName(name);
    }

    /**
     * Convenience method to return if the supplied id is of an object that is cacheable in the L2 cache.
     * @param id The identity
     * @return Whether the object it refers to is considered cacheable
     */
    public boolean isClassWithIdentityCacheable(Object id)
    {
        if (id == null)
        {
            return false;
        }
        AbstractClassMetaData cmd = null;
        if (id instanceof OID)
        {
            if (id instanceof DatastoreUniqueOID)
            {
                // This doesn't have the class name so can't get metadata
                return false;
            }
            cmd = getMetaDataManager().getMetaDataForClass(((OID)id).getPcClass(), getClassLoaderResolver(id.getClass().getClassLoader()));
        }
        else if (getApiAdapter().isSingleFieldIdentity(id))
        {
            cmd = getMetaDataManager().getMetaDataForClass(getApiAdapter().getTargetClassNameForSingleFieldIdentity(id), getClassLoaderResolver(id.getClass().getClassLoader()));
        }
        else
        {
            // Application identity with user PK class, so find all using this PK and take first one
            Collection<AbstractClassMetaData> cmds =
                getMetaDataManager().getClassMetaDataWithApplicationId(id.getClass().getName());
            if (cmds != null && !cmds.isEmpty())
            {
                cmd = cmds.iterator().next();
            }
        }

        return isClassCacheable(cmd);
    }

    /**
     * Convenience method to return if objects of this type are cached for this NucleusContext.
     * Uses the "cacheable" flag of the class, and the cache-mode to determine whether to cache
     */
    public boolean isClassCacheable(AbstractClassMetaData cmd)
    {
        String cacheMode = config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_MODE);
        if (cacheMode.equalsIgnoreCase("ALL"))
        {
            // Everything is cached
            return true;
        }
        else if (cacheMode.equalsIgnoreCase("NONE"))
        {
            // Nothing is cached
            return false;
        }
        else if (cacheMode.equalsIgnoreCase("ENABLE_SELECTIVE"))
        {
            // Default to not cached unless set otherwise
            if (cmd == null)
            {
                return true;
            }
            if (cmd.isCacheable() != null && cmd.isCacheable())
            {
                // Explicitly set as cached
                return true;
            }
            return false;
        }
        else if (cacheMode.equalsIgnoreCase("DISABLE_SELECTIVE"))
        {
            // Default to cached unless set otherwise
            if (cmd == null)
            {
                return true;
            }
            if (cmd.isCacheable() != null && !cmd.isCacheable())
            {
                // Explicitly set as not cached
                return false;
            }
            return true;
        }
        else
        {
            // Default to cacheable unless set otherwise
            if (cmd == null)
            {
                return true;
            }
            Boolean cacheableFlag = cmd.isCacheable();
            if (cacheableFlag == null)
            {
                return true;
            }
            else
            {
                return cacheableFlag;
            }
        }
    }
}