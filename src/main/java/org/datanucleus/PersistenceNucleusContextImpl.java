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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.validation.Validation;

import org.datanucleus.cache.JavaxCacheLevel2Cache;
import org.datanucleus.cache.Level2Cache;
import org.datanucleus.cache.NullLevel2Cache;
import org.datanucleus.cache.SoftLevel2Cache;
import org.datanucleus.cache.WeakLevel2Cache;
import org.datanucleus.enhancer.ImplementationCreator;
import org.datanucleus.enhancer.ImplementationCreatorImpl;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.DatastoreUniqueLongId;
import org.datanucleus.identity.IdentityManager;
import org.datanucleus.identity.IdentityManagerImpl;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.identity.SCOID;
import org.datanucleus.management.FactoryStatistics;
import org.datanucleus.management.ManagementManager;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.FileMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataListener;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.QueryLanguage;
import org.datanucleus.metadata.QueryMetaData;
import org.datanucleus.metadata.TransactionType;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.properties.CorePropertyValidator;
import org.datanucleus.properties.StringPropertyValidator;
import org.datanucleus.state.StateManagerFactory;
import org.datanucleus.state.StateManagerFactoryImpl;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.StoreManagerHelper;
import org.datanucleus.store.autostart.AutoStartMechanism;
import org.datanucleus.store.autostart.AutoStartMechanismUtils;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.store.schema.CurrentUserProvider;
import org.datanucleus.store.schema.MultiTenancyProvider;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
import org.datanucleus.store.schema.SchemaScriptAwareStoreManager;
import org.datanucleus.store.schema.SchemaTool;
import org.datanucleus.store.schema.SchemaTool.Mode;
import org.datanucleus.transaction.NucleusTransactionException;
import org.datanucleus.transaction.ResourcedTransactionManager;
import org.datanucleus.transaction.TransactionUtils;
import org.datanucleus.transaction.jta.JTASyncRegistry;
import org.datanucleus.transaction.jta.JTASyncRegistryUnavailableException;
import org.datanucleus.transaction.jta.TransactionManagerFinder;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Extends the basic DataNucleus context, adding on services for
 * <ul>
 * <li>creating <i>ExecutionContext</i> objects to handle persistence. Uses a pool of <i>ExecutionContext</i> objects, reusing them as required.</li>
 * <li>providing a cache across <i>ExecutionContext</i> objects (the "Level 2" cache).</li>
 * <li>provides a factory for creating <i>StateManagers</i>. This factory makes use of pooling, allowing reuse.</li>
 * <li>provides access to the datastore via a <i>StoreManager</i></li>
 * </ul>
 */
public class PersistenceNucleusContextImpl extends AbstractNucleusContext implements Serializable, PersistenceNucleusContext
{
    private static final long serialVersionUID = 7166558862250068749L;

    /** Manager for the datastore used by this PMF/EMF. */
    private transient StoreManager storeMgr = null;

    private boolean federated = false;

    /** Auto-Start Mechanism for loading classes into the StoreManager (if enabled). */
    private transient AutoStartMechanism starter = null;

    /** Flag defining if this is running within the JDO JCA adaptor. */
    private boolean jca = false;

    /** Level 2 Cache, caching across ExecutionContexts. */
    private Level2Cache cache;

    /** ResourcedTransaction Manager. */
    private transient ResourcedTransactionManager txManager = null;

    /** JTA Transaction Manager (if using JTA). */
    private transient javax.transaction.TransactionManager jtaTxManager = null;

    /** JTA Synchronization Registry (if using JTA 1.1 and supported). */
    private transient JTASyncRegistry jtaSyncRegistry = null;

    /** Manager for JMX features. */
    private transient ManagementManager jmxManager = null;

    /** Statistics gathering object. */
    private transient FactoryStatistics statistics = null;

    /** Manager for object identities. */
    private IdentityManager identityManager;

    /** ImplementationCreator for any persistent interfaces. */
    private ImplementationCreator implCreator;

    private List<ExecutionContext.LifecycleListener> executionContextListeners = new ArrayList();

    /** Manager for dynamic fetch groups defined on the PMF/EMF. */
    private transient FetchGroupManager fetchGrpMgr;

    /** Factory for validation. */
    private transient Object validatorFactory = null;

    /** Flag for whether we have initialised the ValidatorFactory. */
    private transient boolean validatorFactoryInit = false;

    private transient CDIHandler cdiHandler = null;

    /** Flag for whether we have initialised the CDIHandler. */
    private transient boolean cdiHandlerInit = false;

    /** Pool for ExecutionContexts. */
    private ExecutionContextPool ecPool = null;

    /** Factory for StateManagers for managing persistable objects. */
    private StateManagerFactory smFactory = null;

    private MultiTenancyProvider multiTenancyProvider = null;

    private CurrentUserProvider currentUserProvider = null;

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     */
    public PersistenceNucleusContextImpl(String apiName, Map startupProps)
    {
        this(apiName, startupProps, null);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     * @param pluginMgr Plugin Manager (or null if wanting it to be created)
     */
    public PersistenceNucleusContextImpl(String apiName, Map startupProps, PluginManager pluginMgr)
    {
        super(apiName, startupProps, pluginMgr);
    }

    @Override
    public void applyDefaultProperties(Configuration conf)
    {
        super.applyDefaultProperties(conf);

        // PersistenceContext level features
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_IGNORE_CACHE, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_OPTIMISTIC, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_MULTITHREADED, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_RETAIN_VALUES, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_RESTORE_VALUES, null, false, false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_JMX_TYPE, null, null, null, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_ENABLE_STATISTICS, null, false, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_PMF_NAME, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_PERSISTENCE_XML_FILENAME, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SERVER_TIMEZONE_ID, null, null, CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_PROPERTIES_FILE, null, null, null, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_PERSISTENCE_UNIT_LOAD_CLASSES, null, false, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_EXECUTION_CONTEXT_REAPER_THREAD, null, false, false, false);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_EXECUTION_CONTEXT_MAX_IDLE, null, 20, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_EXECUTION_CONTEXT_CLOSE_ACTIVE_TX_ACTION, null, "exception", CorePropertyValidator.class.getName(), false, false);

//        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_STATE_MANAGER_REAPER_THREAD, null, false, false, false);
//        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_STATE_MANAGER_MAX_IDLE, null, 0, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_STATE_MANAGER_CLASS_NAME, null, null, null, false, false);

        conf.addDefaultProperty(PropertyNames.PROPERTY_DATASTORE_IDENTITY_TYPE, null, "datanucleus", null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_IDENTITY_STRING_TRANSLATOR_TYPE, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_IDENTITY_KEY_TRANSLATOR_TYPE, null, null, null, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_USE_IMPLEMENTATION_CREATOR, null, true, false, false);

        // Transactions
        conf.addDefaultProperty(PropertyNames.PROPERTY_TRANSACTION_TYPE, null, null, CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION, null, "read-committed", CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_TRANSACTION_JTA_LOCATOR, null, "autodetect", null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_TRANSACTION_JTA_JNDI_LOCATION, null, null, null, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_TRANSACTION_NONTX_READ, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_TRANSACTION_NONTX_WRITE, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_TRANSACTION_NONTX_ATOMIC, null, true, false, true);

        // Flush process
        conf.addDefaultProperty(PropertyNames.PROPERTY_FLUSH_MODE, null, null, CorePropertyValidator.class.getName(), false, true);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_FLUSH_AUTO_OBJECT_LIMIT, null, 1, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_MARK_ROLLBACKONLY_ON_ERROR_IN_FLUSH, null, null, false, true);

        // Value Generation
        conf.addDefaultProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ISOLATION, null, "read-committed", CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_VALUEGEN_TXN_ATTRIBUTE, null, "NEW", CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_VALUEGEN_SEQUENCE_ALLOCSIZE, null, 10, false, false);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_VALUEGEN_INCREMENT_ALLOCSIZE, null, 10, false, false);

        // Bean Validation
        conf.addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_MODE, null, "auto", CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREPERSIST, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREUPDATE, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREREMOVE, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_VALIDATION_FACTORY, null, null, null, false, false);

        // Store Definition
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY, null, false, false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION, null, "EXCEPTION", CorePropertyValidator.class.getName(), false, true);

        // Schema Generation
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE, null, "none", CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_MODE, null, "none", CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_ORDER, null, null, CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE, null, "datanucleus-schema-create.ddl", null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_SCRIPT, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_ORDER, null, null, CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP, null, "datanucleus-schema-drop.ddl", null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_SCRIPT, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_SCHEMA_LOAD_SCRIPT, null, null, null, false, false);

        // Cache
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_COLLECTIONS, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_COLLECTIONS_LAZY, null, null, false, false);

        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_L1_TYPE, null, "soft", null, false, false);

        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_TYPE, null, "soft", null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_MODE, null, "UNSPECIFIED", CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_NAME, null, "datanucleus", null, false, false);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_L2_MAXSIZE, null, -1, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_LOADFIELDS, null, true, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CLEARATCLOSE, null, true, false, false);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_L2_EXPIRY_MILLIS, null, -1, false, false);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_L2_BATCHSIZE, null, 100, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CACHE_EMBEDDED, null, true, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_READ_THROUGH, null, true, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_WRITE_THROUGH, null, true, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_STATISTICS_ENABLED, null, false, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_STORE_BY_VALUE, null, true, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_RETRIEVE_MODE, null, "use", CorePropertyValidator.class.getName(), false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_STORE_MODE, null, "use", CorePropertyValidator.class.getName(), false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_L2_UPDATE_MODE, null, "commit-and-datastore-read", CorePropertyValidator.class.getName(), false, true);

        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_QUERYCOMPILE_TYPE, null, "soft", null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_QUERYCOMPILEDATASTORE_TYPE, null, "soft", null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_TYPE, null, "soft", null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_NAME, null, "datanucleus-query", null, false, false);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_MAXSIZE, null, -1, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_CLEARATCLOSE, null, true, false, false);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_EXPIRY_MILLIS, null, -1, false, false);

        // Queries
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_SQL_ALLOWALL, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_JPQL_ALLOW_RANGE, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_SQL_SYNTAXCHECKS, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_FLUSH_BEFORE_EXECUTE, null, false, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_USE_FETCHPLAN, null, true, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_CHECK_UNUSED_PARAMS, null, true, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_COMPILE_OPTIMISE_VAR_THIS, null, false, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_LOAD_RESULTS_AT_COMMIT, null, true, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_COMPILATION_CACHED, null, true, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_RESULTS_CACHED, null, false, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_EVALUATE_IN_MEMORY, null, false, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_RESULTCACHE_VALIDATEOBJECTS, null, true, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_QUERY_RESULT_SIZE_METHOD, null, "last", null, false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_QUERY_COMPILE_NAMED_QUERIES_AT_STARTUP, null, false, false, false);

        // Other properties
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_PERSISTENCE_BY_REACHABILITY_AT_COMMIT, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS_CHECKS, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SERIALIZE_READ, null, false, false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_DELETION_POLICY, null, "JDO2", CorePropertyValidator.class.getName(), false, true);
        // TODO Would be nice to set the default here to "false" but JDO TCK "instanceCallbacks" fails
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_FIND_OBJECT_VALIDATE_WHEN_CACHED, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_FIND_OBJECT_TYPE_CONVERSION, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_ALLOW_CALLBACKS, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_DETACH_ON_CLOSE, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_COPY_ON_ATTACH, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_ATTACH_SAME_DATASTORE, null, true, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_ALLOW_ATTACH_OF_TRANSIENT, null, false, false, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_DETACH_AS_WRAPPED, null, false, false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_DETACH_DETACHMENT_FIELDS, null, "load-fields", CorePropertyValidator.class.getName(), false, false); // TODO Change last arg to true
        conf.addDefaultProperty(PropertyNames.PROPERTY_DETACH_DETACHED_STATE, null, "fetch-groups", CorePropertyValidator.class.getName(), false, false); // TODO Change last arg to true
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_MAX_FETCH_DEPTH, null, 1, false, true);

        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_VERSION_NUMBER_INITIAL_VALUE, null, 1, false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_RELATION_IDENTITY_STORAGE_MODE, null, StoreManager.RELATION_IDENTITY_STORAGE_PERSISTABLE_IDENTITY, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_TYPE_WRAPPER_BASIS, null, "instantiated", CorePropertyValidator.class.getName(), false, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_TYPE_TREAT_JAVA_UTIL_DATE_AS_MUTABLE, null, true, false, false);

        // ========================= Generally all properties below here are specified at the StoreManager level =============================

        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_ALL, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_DATABASE, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_WARNONERROR, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_VALIDATE_ALL, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_VALIDATE_TABLES, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_VALIDATE_COLUMNS, null, false, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_SCHEMA_VALIDATE_CONSTRAINTS, null, false, true, false);

        // Schema identifier naming
        conf.addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_NAMING_FACTORY, null, "datanucleus2", null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_CASE, null, null, CorePropertyValidator.class.getName(), true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_TABLE_PREFIX, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_TABLE_SUFFIX, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_WORD_SEPARATOR, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_IDENTIFIER_FACTORY, null, "datanucleus2", null, true, false); // TODO Migrate RDBMS to use NamingFactory and drop this

        // Datastore
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_DATASTORE_READ_TIMEOUT, null, null, true, true);
        conf.addDefaultIntegerProperty(PropertyNames.PROPERTY_DATASTORE_WRITE_TIMEOUT, null, null, true, true);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_STORE_ALLOW_REFS_WITHOUT_IMPLS, null, false, false, true);

        conf.addDefaultProperty(PropertyNames.PROPERTY_MAPPING, null, null, StringPropertyValidator.class.getName(), true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_MAPPING_CATALOG, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_MAPPING_SCHEMA, null, null, null, true, false);

        // Multitenancy
        conf.addDefaultProperty(PropertyNames.PROPERTY_MAPPING_TENANT_ID, null, null, null, false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_MAPPING_TENANT_READ_IDS, null, null, null, false, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_MAPPING_TENANT_PROVIDER, null, null, null, false, false);

        // Current user
        conf.addDefaultProperty(PropertyNames.PROPERTY_MAPPING_CURRENT_USER, null, null, null, false, true);
        conf.addDefaultProperty(PropertyNames.PROPERTY_MAPPING_CURRENT_USER_PROVIDER, null, null, null, false, false);

        // Auto-Start Mechanism
        conf.addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_MECHANISM, null, "None", null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_MODE, null, "Quiet", CorePropertyValidator.class.getName(), true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_CLASSNAMES, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_AUTOSTART_METADATAFILES, null, null, null, true, false);

        // Connection
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_URL, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_USER_NAME, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_PASSWORD, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_PASSWORD_DECRYPTER, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2_NAME, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_RESOURCETYPE, null, null, CorePropertyValidator.class.getName(), true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_RESOURCETYPE2, null, null, CorePropertyValidator.class.getName(), true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_POOLINGTYPE, null, null, null, true, false);
        conf.addDefaultProperty(PropertyNames.PROPERTY_CONNECTION_POOLINGTYPE2, null, null, null, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CONNECTION_NONTX_RELEASE_AFTER_USE, null, true, true, false);
        conf.addDefaultBooleanProperty(PropertyNames.PROPERTY_CONNECTION_SINGLE_CONNECTION, null, false, true, false);
    }

    @Override
    public synchronized void initialise()
    {
        final ClassLoaderResolver clr = getClassLoaderResolver(null);
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
        }

        // Create the StoreManager
        try
        {
            Set<String> propNamesWithDatastore = config.getPropertyNamesWithPrefix("datanucleus.datastore.");
            if (propNamesWithDatastore == null)
            {
                // Find the StoreManager using the persistence property if specified
                NucleusLogger.DATASTORE.debug("Creating StoreManager for datastore");
                Map<String, Object> datastoreProps = config.getDatastoreProperties();
                this.storeMgr = StoreManagerHelper.createStoreManagerForProperties(config.getPersistenceProperties(), datastoreProps, clr, this);

                // Make sure the isolation level is valid for this StoreManager and correct if necessary
                String transactionIsolation = config.getStringProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION);
                if (transactionIsolation != null)
                {
                    String reqdIsolation = TransactionUtils.getTransactionIsolationForStoreManager(storeMgr, transactionIsolation);
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
                this.federated = true;
            }
        }
        catch (NucleusException ne)
        {
            NucleusLogger.DATASTORE.error("Exception thrown creating StoreManager : " + StringUtils.getMessageFromRootCauseOfThrowable(ne));
            throw ne;
        }
        NucleusLogger.DATASTORE.debug("StoreManager now created");

        // Make sure MetaDataManager is initialised
        MetaDataManager mmgr = getMetaDataManager();
        final Level2Cache cache = getLevel2Cache();
        if (cache != null)
        {
            // Add listener for metadata loading so we can pin any classes in the L2 cache that are marked for that
            mmgr.registerListener(new MetaDataListener()
            {
                @Override
                public void loaded(AbstractClassMetaData cmd)
                {
                    if (cmd.hasExtension("cache-pin") && cmd.getValueForExtension("cache-pin").equalsIgnoreCase("true"))
                    {
                        // Register as auto-pinned in the L2 cache
                        Class cls = clr.classForName(cmd.getFullClassName());
                        cache.pinAll(cls, false);
                    }
                }
            });
        }

        // ========== Initialise the StoreManager contents ==========
        // A). Load any classes specified by auto-start
        String autoStartMechanism = config.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MECHANISM);
        if (autoStartMechanism != null && !autoStartMechanism.equals("None"))
        {
            String mode = config.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_MODE);
            starter = AutoStartMechanismUtils.createAutoStartMechanism(this, clr, autoStartMechanism, mode);
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
            this.storeMgr.manageClasses(clr, loadedClasses.toArray(new String[loadedClasses.size()]));
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
        if (smFactory == null)
        {
            smFactory = new StateManagerFactoryImpl(this);
        }

        if (config.hasProperty(PropertyNames.PROPERTY_MAPPING_TENANT_PROVIDER))
        {
            try
            {
                multiTenancyProvider = (MultiTenancyProvider)config.getProperty(PropertyNames.PROPERTY_MAPPING_TENANT_PROVIDER); 
            }
            catch (Throwable thr)
            {
                NucleusLogger.PERSISTENCE.warn("Error accessing property " + PropertyNames.PROPERTY_MAPPING_TENANT_PROVIDER + "; should be an instance of MultiTenancyProvider but isnt! Ignored");
            }
        }

        if (config.hasProperty(PropertyNames.PROPERTY_MAPPING_CURRENT_USER_PROVIDER))
        {
            try
            {
                currentUserProvider = (CurrentUserProvider)config.getProperty(PropertyNames.PROPERTY_MAPPING_CURRENT_USER_PROVIDER); 
            }
            catch (Throwable thr)
            {
                NucleusLogger.PERSISTENCE.warn("Error accessing property " + PropertyNames.PROPERTY_MAPPING_CURRENT_USER_PROVIDER + "; should be an instance of CurrentUserProvider but isnt! Ignored");
            }
        }

        super.initialise();
    }

    @Override
    public synchronized void close()
    {
        if (cdiHandler != null)
        {
            cdiHandler.close();
        }
        if (smFactory != null)
        {
            smFactory.close();
            smFactory = null;
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
            statistics.close();
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
            cache = null;
            NucleusLogger.CACHE.debug(Localiser.msg("004009"));
        }
        if (classLoaderResolverMap != null)
        {
            classLoaderResolverMap.clear();
            classLoaderResolverMap = null;
        }
        if (typeManager != null)
        {
            typeManager = null;
        }

        identityManager = null;

        super.close();
    }

    @Override
    protected void logConfigurationDetails()
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
                (config.getBooleanProperty(PropertyNames.PROPERTY_TRANSACTION_NONTX_READ) ? ", nontransactional-read" : "") +
                (config.getBooleanProperty(PropertyNames.PROPERTY_TRANSACTION_NONTX_WRITE) ? ", nontransactional-write" : "") +
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
        if (autoStartMechanism != null && !autoStartMechanism.equals("None"))
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

    @Override
    public boolean isFederated()
    {
        return federated;
    }

    @Override
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
                if (qmd.getLanguage().equals(QueryLanguage.JPQL.name()) || qmd.getLanguage().equals(QueryLanguage.JDOQL.name()))
                {
                    if (NucleusLogger.QUERY.isDebugEnabled())
                    {
                        NucleusLogger.QUERY.debug(Localiser.msg("008017", queryName, qmd.getQuery()));
                    }
                    org.datanucleus.store.query.Query q = storeMgr.newQuery(qmd.getLanguage().toString(), ec, qmd.getQuery());
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
     * @param generateScripts Whether to generate
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
                NucleusLogger.DATASTORE_SCHEMA.debug(Localiser.msg("014000"));
            }
            else if (mode == Mode.DELETE)
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(Localiser.msg("014001"));
            }
            else if (mode == Mode.DELETE_CREATE)
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(Localiser.msg("014045"));
            }
        }

        // Extract the classes that have metadata loaded (e.g persistence-unit)
        Set<String> schemaClassNames = new TreeSet<String>();
        FileMetaData[] filemds = getMetaDataManager().getFileMetaData();
        if (filemds == null)
        {
            throw new NucleusUserException("No classes to process in generateSchema");
        }

        for (int i=0;i<filemds.length;i++)
        {
            int numPackages = filemds[i].getNoOfPackages();
            for (int j=0;j<numPackages;j++)
            {
                int numClasses = filemds[i].getPackage(j).getNoOfClasses();
                for (int k=0;k<numClasses;k++)
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

            // Set schema properties to allow us to do schema changes
            storeMgr.enableSchemaGeneration();

            if (mode == Mode.CREATE)
            {
                if (generateScripts)
                {
                    // Generate the required script
                    schemaTool.setDdlFile(config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE));
                    schemaTool.createSchemaForClasses(schemaStoreMgr, schemaClassNames);
                }
                else
                {
                    // Process the required metadata/script
                    String createOrder = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_ORDER);
                    String createScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_SCRIPT);
                    if (StringUtils.isWhitespace(createScript))
                    {
                        createScript = null;
                    }
                    if (StringUtils.isWhitespace(createOrder))
                    {
                        createOrder = (createScript != null) ? "script" : "metadata";
                    }
                    else
                    {
                        if (createOrder.equals("script") || createOrder.equals("metadata-then-script") || createOrder.equals("script-the-metadata"))
                        {
                            if (createScript == null)
                            {
                                NucleusLogger.DATASTORE_SCHEMA.warn("create order set to " + createOrder + " but no script defined, so using metadata instead");
                                createOrder = "metadata";
                            }
                        }
                    }

                    if (createOrder.equals("script"))
                    {
                        processDatastoreScript(createScript);
                    }
                    else if (createOrder.equals("script-then-metadata"))
                    {
                        processDatastoreScript(createScript);
                        schemaTool.createSchemaForClasses(schemaStoreMgr, schemaClassNames);
                    }
                    else if (createOrder.equals("metadata-then-script"))
                    {
                        schemaTool.createSchemaForClasses(schemaStoreMgr, schemaClassNames);
                        processDatastoreScript(createScript);
                    }
                    else
                    {
                        schemaTool.createSchemaForClasses(schemaStoreMgr, schemaClassNames);
                    }
                }
            }
            else if (mode == Mode.DELETE)
            {
                if (generateScripts)
                {
                    // Generate the required script
                    schemaTool.setDdlFile(config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP));
                    schemaTool.deleteSchemaForClasses(schemaStoreMgr, schemaClassNames);
                }
                else
                {
                    // Process the required metadata/script
                    String dropOrder = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_ORDER);
                    String dropScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_SCRIPT);
                    if (StringUtils.isWhitespace(dropScript))
                    {
                        dropScript = null;
                    }
                    if (StringUtils.isWhitespace(dropOrder))
                    {
                        dropOrder = (dropScript != null) ? "script" : "metadata";
                    }
                    else
                    {
                        if (dropOrder.equals("script") || dropOrder.equals("metadata-then-script") || dropOrder.equals("script-the-metadata"))
                        {
                            if (dropScript == null)
                            {
                                NucleusLogger.DATASTORE_SCHEMA.warn("drop order set to " + dropOrder + " but no script defined, so using metadata instead");
                                dropOrder = "metadata";
                            }
                        }
                    }

                    if (dropOrder.equals("script"))
                    {
                        processDatastoreScript(dropScript);
                    }
                    else if (dropOrder.equals("script-then-metadata"))
                    {
                        processDatastoreScript(dropScript);
                        schemaTool.deleteSchemaForClasses(schemaStoreMgr, schemaClassNames);
                    }
                    else if (dropOrder.equals("metadata-then-script"))
                    {
                        schemaTool.deleteSchemaForClasses(schemaStoreMgr, schemaClassNames);
                        processDatastoreScript(dropScript);
                    }
                    else
                    {
                        schemaTool.deleteSchemaForClasses(schemaStoreMgr, schemaClassNames);
                    }
                }
            }
            else if (mode == Mode.DELETE_CREATE)
            {
                if (generateScripts)
                {
                    // Generate the required scripts
                    schemaTool.setDdlFile(config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP));
                    schemaTool.deleteSchemaForClasses(schemaStoreMgr, schemaClassNames);

                    schemaTool.setDdlFile(config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE));
                    schemaTool.createSchemaForClasses(schemaStoreMgr, schemaClassNames);
                }
                else
                {
                    // Process the required metadata/scripts
                    String dropOrder = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_ORDER);
                    String dropScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_SCRIPT);
                    if (StringUtils.isWhitespace(dropScript))
                    {
                        dropScript = null;
                    }
                    if (StringUtils.isWhitespace(dropOrder))
                    {
                        dropOrder = (dropScript != null) ? "script" : "metadata";
                    }
                    else
                    {
                        if (dropOrder.equals("script") || dropOrder.equals("metadata-then-script") || dropOrder.equals("script-the-metadata"))
                        {
                            if (dropScript == null)
                            {
                                NucleusLogger.DATASTORE_SCHEMA.warn("drop order set to " + dropOrder + " but no script defined, so using metadata instead");
                                dropOrder = "metadata";
                            }
                        }
                    }

                    if (dropOrder.equals("script"))
                    {
                        processDatastoreScript(dropScript);
                    }
                    else if (dropOrder.equals("script-then-metadata"))
                    {
                        processDatastoreScript(dropScript);
                        schemaTool.deleteSchemaForClasses(schemaStoreMgr, schemaClassNames);
                    }
                    else if (dropOrder.equals("metadata-then-script"))
                    {
                        schemaTool.deleteSchemaForClasses(schemaStoreMgr, schemaClassNames);
                        processDatastoreScript(dropScript);
                    }
                    else
                    {
                        schemaTool.deleteSchemaForClasses(schemaStoreMgr, schemaClassNames);
                    }

                    String createOrder = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_ORDER);
                    String createScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_SCRIPT);
                    if (StringUtils.isWhitespace(createScript))
                    {
                        createScript = null;
                    }
                    if (StringUtils.isWhitespace(createOrder))
                    {
                        createOrder = (createScript != null) ? "script" : "metadata";
                    }
                    else
                    {
                        if (createOrder.equals("script") || createOrder.equals("metadata-then-script") || createOrder.equals("script-the-metadata"))
                        {
                            if (createScript == null)
                            {
                                NucleusLogger.DATASTORE_SCHEMA.warn("create order set to " + createOrder + " but no script defined, so using metadata instead");
                                createOrder = "metadata";
                            }
                        }
                    }

                    if (createOrder.equals("script"))
                    {
                        processDatastoreScript(createScript);
                    }
                    else if (createOrder.equals("script-then-metadata"))
                    {
                        processDatastoreScript(createScript);
                        schemaTool.createSchemaForClasses(schemaStoreMgr, schemaClassNames);
                    }
                    else if (createOrder.equals("metadata-then-script"))
                    {
                        schemaTool.createSchemaForClasses(schemaStoreMgr, schemaClassNames);
                        processDatastoreScript(createScript);
                    }
                    else
                    {
                        schemaTool.createSchemaForClasses(schemaStoreMgr, schemaClassNames);
                    }
                }
            }

            String loadScript = config.getStringProperty(PropertyNames.PROPERTY_SCHEMA_LOAD_SCRIPT);
            if (!StringUtils.isWhitespace(loadScript))
            {
                String scriptContent = getDatastoreScriptForResourceName(loadScript);
                if (storeMgr instanceof SchemaScriptAwareStoreManager && !StringUtils.isWhitespace(scriptContent))
                {
                    ((SchemaScriptAwareStoreManager)storeMgr).executeScript(scriptContent);
                }
            }

            // Reset schema properties on StoreManager
            storeMgr.resetSchemaGeneration();
        }
        else
        {
            if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
            {
                NucleusLogger.DATASTORE_SCHEMA.debug(Localiser.msg("008016", StringUtils.toJVMIDString(storeMgr)));
            }
        }

        if (NucleusLogger.DATASTORE_SCHEMA.isDebugEnabled())
        {
            NucleusLogger.DATASTORE_SCHEMA.debug(Localiser.msg("014043"));
        }
    }

    private void processDatastoreScript(String scriptResourceName)
    {
        String scriptContent = getDatastoreScriptForResourceName(scriptResourceName);
        if (storeMgr instanceof SchemaScriptAwareStoreManager && !StringUtils.isWhitespace(scriptContent))
        {
            ((SchemaScriptAwareStoreManager)storeMgr).executeScript(scriptContent);
        }
    }

    /**
     * Method to load the contents of a file given a resource name.
     * @param scriptResourceName The resource name (filename?)
     * @return The contents
     */
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
                file = new File(new URI(scriptResourceName));
            }
            catch (Exception e)
            {
                // Try via the ClassLoaderResolver
                URL url = getClassLoaderResolver(getClass().getClassLoader()).getResource(scriptResourceName, null);
                if (url != null)
                {
                    file = new File(url.getFile());
                }
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

        NucleusLogger.DATASTORE_SCHEMA.warn(Localiser.msg("014046", scriptResourceName));
        return null;
    }

    @Override
    public ExecutionContextPool getExecutionContextPool()
    {
        if (ecPool == null)
        {
            initialise();
        }
        return ecPool;
    }

    @Override
    public StateManagerFactory getStateManagerFactory()
    {
        if (smFactory == null)
        {
            initialise();
        }
        return smFactory;
    }

    @Override
    public ExecutionContext getExecutionContext(Object owner, Map<String, Object> options)
    {
        return getExecutionContextPool().checkOut(owner, options);
    }

    @Override
    public IdentityManager getIdentityManager()
    {
        if (identityManager == null)
        {
            identityManager = new IdentityManagerImpl(this);
        }
        return identityManager;
    }

    @Override
    public boolean statisticsEnabled()
    {
        return config.getBooleanProperty(PropertyNames.PROPERTY_ENABLE_STATISTICS) || getJMXManager() != null;
    }

    @Override
    public synchronized ManagementManager getJMXManager()
    {
        if (jmxManager == null && config.getStringProperty(PropertyNames.PROPERTY_JMX_TYPE) != null)
        {
            // User requires managed runtime, and not yet present so create manager
            jmxManager = new ManagementManager(this);
        }
        return jmxManager;
    }

    @Override
    public synchronized FactoryStatistics getStatistics()
    {
        if (statistics == null && statisticsEnabled())
        {
            statistics = new FactoryStatistics(jmxManager);
        }
        return statistics;
    }

    @Override
    public synchronized ImplementationCreator getImplementationCreator()
    {
        if (implCreator == null)
        {
            boolean useImplCreator = config.getBooleanProperty(PropertyNames.PROPERTY_USE_IMPLEMENTATION_CREATOR);
            if (useImplCreator)
            {
                implCreator = new ImplementationCreatorImpl(getMetaDataManager());
            }
        }
        return implCreator;
    }

    @Override
    public synchronized ResourcedTransactionManager getResourcedTransactionManager()
    {
        if (txManager == null)
        {
            txManager = new ResourcedTransactionManager();
        }
        return txManager;
    }

    @Override
    public synchronized javax.transaction.TransactionManager getJtaTransactionManager()
    {
        if (jtaTxManager == null)
        {
            // Find the JTA transaction manager - there is no standard way to do this so use the finder process.
            jtaTxManager = new TransactionManagerFinder(this).getTransactionManager(getClassLoaderResolver((ClassLoader)config.getProperty(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY)));
            if (jtaTxManager == null)
            {
                throw new NucleusTransactionException(Localiser.msg("015030"));
            }
        }
        return jtaTxManager;
    }

    @Override
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

    @Override
    public StoreManager getStoreManager()
    {
        if (storeMgr == null)
        {
            initialise();
        }
        return storeMgr;
    }

    @Override
    public boolean supportsORMMetaData()
    {
        if (storeMgr != null)
        {
            return storeMgr.getSupportedOptions().contains(StoreManager.OPTION_ORM);
        }
        return true;
    }

    @Override
    public BeanValidationHandler getBeanValidationHandler(ExecutionContext ec)
    {
        String validationMode = config.getStringProperty(PropertyNames.PROPERTY_VALIDATION_MODE);
        if ("none".equalsIgnoreCase(validationMode))
        {
            validatorFactoryInit = true;
            return null;
        }

        try
        {
            if (validatorFactory == null && !validatorFactoryInit)
            {
                validatorFactoryInit = true;

                // Factory not yet set so create it
                if (config.hasPropertyNotNull(PropertyNames.PROPERTY_VALIDATION_FACTORY))
                {
                    // Create from javax.persistence.validation.factory if given
                    validatorFactory = config.getProperty(PropertyNames.PROPERTY_VALIDATION_FACTORY);
                }
                else
                {
                    // Create via BeanValidation API bootstrap if present
                    try
                    {
                        ec.getClassLoaderResolver().classForName("javax.validation.Validation");
                        validatorFactory = Validation.buildDefaultValidatorFactory();
                    }
                    catch (ClassNotResolvedException cnre)
                    {
                        NucleusLogger.PERSISTENCE.debug("No BeanValidation API (javax.validation) present so cannot utilise BeanValidation hooks");
                    }
                }
            }

            return (validatorFactory != null) ? new BeanValidationHandler(ec, validatorFactory) : null;
        }
        catch (Throwable ex) //throwable used to catch linkage errors
        {
            if ("callback".equalsIgnoreCase(validationMode))
            {
                throw ec.getApiAdapter().getUserExceptionForException(ex.getMessage(), (Exception)ex);
            }

            NucleusLogger.GENERAL.warn("Unable to create validator handler", ex);
        }
        return null;
    }

    @Override
    public CDIHandler getCDIHandler()
    {
        if (cdiHandlerInit)
        {
            return cdiHandler;
        }

        cdiHandlerInit = true;
        if (config.hasPropertyNotNull(PropertyNames.PROPERTY_CDI_BEAN_MANAGER))
        {
            Object cdiBeanManager = config.getProperty(PropertyNames.PROPERTY_CDI_BEAN_MANAGER);
            cdiHandler = new CDIHandler(cdiBeanManager);
        }
        else
        {
            // Try to find a BeanManager using JNDI lookup (in case we have standalone JNDI and CDI)
            try 
            {
                Object cdiBeanManager = new InitialContext().lookup("java:comp/BeanManager");
                if (cdiBeanManager != null)
                {
                    cdiHandler = new CDIHandler(cdiBeanManager);
                }

            }
            catch (NamingException e) 
            {
                return null;
            }
        }

        return cdiHandler;
    }

    @Override
    public boolean hasLevel2Cache()
    {
        getLevel2Cache();
        return !(cache instanceof NullLevel2Cache);
    }

    @Override
    public Level2Cache getLevel2Cache()
    {
        if (cache == null)
        {
            String level2Type = config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_TYPE);
            if (NullLevel2Cache.NAME.equals(level2Type))
            {
                cache = new NullLevel2Cache(this);
            }
            else if (SoftLevel2Cache.NAME.equals(level2Type))
            {
                cache = new SoftLevel2Cache(this);
            }
            else if (WeakLevel2Cache.NAME.equals(level2Type))
            {
                cache = new WeakLevel2Cache(this);
            }
            else if (JavaxCacheLevel2Cache.NAME.equals(level2Type))
            {
                cache = new JavaxCacheLevel2Cache(this);
            }
            else
            {
                // Find the L2 cache class name from its plugin name
                String level2ClassName = pluginManager.getAttributeValueForExtension("org.datanucleus.cache_level2", "name", level2Type, "class-name");
                if (level2ClassName == null)
                {
                    // Plugin of this name not found
                    throw new NucleusUserException(Localiser.msg("004000", level2Type)).setFatal();
                }

                try
                {
                    // Create an instance of the L2 Cache
                    cache = (Level2Cache)pluginManager.createExecutableExtension("org.datanucleus.cache_level2", "name", level2Type, "class-name",
                        new Class[]{ClassConstants.NUCLEUS_CONTEXT}, new Object[]{this});
                    if (NucleusLogger.CACHE.isDebugEnabled())
                    {
                        NucleusLogger.CACHE.debug(Localiser.msg("004002", level2Type));
                    }
                }
                catch (Exception e)
                {
                    // Class name for this L2 cache plugin is not found!
                    throw new NucleusUserException(Localiser.msg("004001", level2Type, level2ClassName), e).setFatal();
                }
            }
        }
        return cache;
    }

    @Override
    public ExecutionContext.LifecycleListener[] getExecutionContextListeners()
    {
        return executionContextListeners.toArray(new ExecutionContext.LifecycleListener[executionContextListeners.size()]);
    }

    @Override
    public void addExecutionContextListener(ExecutionContext.LifecycleListener listener)
    {
        executionContextListeners.add(listener);
    }

    @Override
    public void removeExecutionContextListener(ExecutionContext.LifecycleListener listener)
    {
        executionContextListeners.remove(listener);
    }

    @Override
    public synchronized void setJcaMode(boolean jca)
    {
        this.jca = jca;
    }

    @Override
    public boolean isJcaMode()
    {
        return jca;
    }

    // --------------------------- Fetch Groups ---------------------------------

    @Override
    public synchronized FetchGroupManager getFetchGroupManager()
    {
        if (fetchGrpMgr == null)
        {
            fetchGrpMgr = new FetchGroupManager(this);
        }
        return fetchGrpMgr;
    }

    @Override
    public void addInternalFetchGroup(FetchGroup grp)
    {
        getFetchGroupManager().addFetchGroup(grp);
    }

    @Override
    public void removeInternalFetchGroup(FetchGroup grp)
    {
        getFetchGroupManager().removeFetchGroup(grp);
    }

    @Override
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

    @Override
    public FetchGroup getInternalFetchGroup(Class cls, String name, boolean createIfNotPresent)
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
        return getFetchGroupManager().getFetchGroup(cls, name, createIfNotPresent);
    }

    @Override
    public Set<FetchGroup> getFetchGroupsWithName(String name)
    {
        return getFetchGroupManager().getFetchGroupsWithName(name);
    }

    @Override
    public boolean isClassWithIdentityCacheable(Object id)
    {
        if (id == null)
        {
            return false;
        }
        if (id instanceof SCOID)
        {
            return false;
        }
        else if (id instanceof DatastoreUniqueLongId)
        {
            // This doesn't have the class name so can't get metadata
            return false;
        }

        AbstractClassMetaData cmd = null;
        String className = IdentityUtils.getTargetClassNameForIdentity(id);
        if (className != null)
        {
            // "Identity" defines the class name
            cmd = getMetaDataManager().getMetaDataForClass(className, getClassLoaderResolver(id.getClass().getClassLoader()));
        }
        else
        {
            // Application identity with user PK class, so find all using this PK and take first one
            Collection<AbstractClassMetaData> cmds = getMetaDataManager().getClassMetaDataWithApplicationId(id.getClass().getName());
            if (cmds != null && !cmds.isEmpty())
            {
                cmd = cmds.iterator().next();
            }
        }

        return isClassCacheable(cmd);
    }

    @Override
    public boolean isClassCacheable(AbstractClassMetaData cmd)
    {
        if (cmd != null && cmd.getIdentityType() == IdentityType.NONDURABLE)
        {
            return false;
        }

        String cacheMode = config.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_MODE).toUpperCase();
        if (cacheMode.equals("ALL"))
        {
            // Everything is cached
            return true;
        }
        else if (cacheMode.equals("NONE"))
        {
            // Nothing is cached
            return false;
        }
        else if (cacheMode.equals("ENABLE_SELECTIVE"))
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
        else if (cacheMode.equals("DISABLE_SELECTIVE"))
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
            return cacheableFlag;
        }
    }

    @Override
    public String getTenantId(ExecutionContext ec)
    {
        if (multiTenancyProvider != null)
        {
            return multiTenancyProvider.getTenantId(ec);
        }
        return ec.getStringProperty(PropertyNames.PROPERTY_MAPPING_TENANT_ID);
    }

    @Override
    public String[] getTenantReadIds(ExecutionContext ec)
    {
        if (multiTenancyProvider != null)
        {
            String[] tenantReadIds = multiTenancyProvider.getTenantReadIds(ec);
            return (tenantReadIds != null) ? tenantReadIds : new String[] {multiTenancyProvider.getTenantId(ec)};
        }

        // Check for tenantReadIds property (either overridden in EC, or from config)
        String readIds = null;
        if (ec != null)
        {
            readIds = ec.getStringProperty(PropertyNames.PROPERTY_MAPPING_TENANT_READ_IDS);
        }
        if (readIds == null)
        {
            readIds = config.getStringProperty(PropertyNames.PROPERTY_MAPPING_TENANT_READ_IDS);
        }
        if (readIds != null)
        {
            return readIds.split(",");
        }

        // Fallback to current tenant id (either overridden in EC, or from config)
        String tenantId = null;
        if (ec != null)
        {
            // Overridden in EC
            tenantId = ec.getStringProperty(PropertyNames.PROPERTY_MAPPING_TENANT_ID);
        }
        if (tenantId == null)
        {
            tenantId = config.getStringProperty(PropertyNames.PROPERTY_MAPPING_TENANT_ID);
        }
        return new String[] {tenantId};
    }

    @Override
    public String getCurrentUser(ExecutionContext ec)
    {
        if (currentUserProvider != null)
        {
            return currentUserProvider.currentUser();
        }
        return ec.getStringProperty(PropertyNames.PROPERTY_MAPPING_CURRENT_USER);
    }
}