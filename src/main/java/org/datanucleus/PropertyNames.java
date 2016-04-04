/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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

/**
 * Utility providing convenience naming of core persistence properties.
 */
public class PropertyNames
{
    public static final String PROPERTY_PLUGIN_REGISTRY_CLASSNAME = "datanucleus.plugin.pluginRegistryClassName";
    public static final String PROPERTY_PLUGIN_ALLOW_USER_BUNDLES = "datanucleus.plugin.allowUserBundles";
    public static final String PROPERTY_PLUGIN_VALIDATEPLUGINS = "datanucleus.plugin.validatePlugins";
    public static final String PROPERTY_PLUGIN_REGISTRYBUNDLECHECK = "datanucleus.plugin.pluginRegistryBundleCheck";

    public static final String PROPERTY_CLASSLOADER_RESOLVER_NAME = "datanucleus.classLoaderResolverName";
    public static final String PROPERTY_CLASSLOADER_PRIMARY = "datanucleus.primaryClassLoader";

    public static final String PROPERTY_METADATA_IGNORE_MISSING_PERSISTABLE_CLASSES = "datanucleus.metadata.ignoreMissingPersistableClasses";
    public static final String PROPERTY_METADATA_ALWAYS_DETACHABLE = "datanucleus.metadata.alwaysDetachable";
    public static final String PROPERTY_METADATA_XML_VALIDATE = "datanucleus.metadata.xml.validate";
    public static final String PROPERTY_METADATA_XML_NAMESPACE_AWARE = "datanucleus.metadata.xml.namespaceAware";
    public static final String PROPERTY_METADATA_AUTOREGISTER = "datanucleus.metadata.autoregistration";
    public static final String PROPERTY_METADATA_ALLOW_XML = "datanucleus.metadata.allowXML";
    public static final String PROPERTY_METADATA_ALLOW_ANNOTATIONS = "datanucleus.metadata.allowAnnotations";
    public static final String PROPERTY_METADATA_ALLOW_LOAD_AT_RUNTIME = "datanucleus.metadata.allowLoadAtRuntime";
    public static final String PROPERTY_METADATA_SUPPORT_ORM = "datanucleus.metadata.supportORM";
    public static final String PROPERTY_METADATA_JDO_SUFFIX = "datanucleus.metadata.jdoFileExtension";
    public static final String PROPERTY_METADATA_ORM_SUFFIX = "datanucleus.metadata.ormFileExtension";
    public static final String PROPERTY_METADATA_JDOQUERY_SUFFIX = "datanucleus.metadata.jdoqueryFileExtension";
    public static final String PROPERTY_METADATA_SCANNER = "datanucleus.metadata.scanner";
    public static final String PROPERTY_METADATA_DEFAULT_INHERITANCE_STRATEGY = "datanucleus.metadata.defaultInheritanceStrategy";
    public static final String PROPERTY_METADATA_EMBEDDED_PC_FLAT = "datanucleus.metadata.embedded.flat";
    public static final String PROPERTY_METADATA_DEFAULT_NULLABLE = "datanucleus.metadata.defaultNullable";

    public static final String PROPERTY_IGNORE_CACHE = "datanucleus.IgnoreCache";
    public static final String PROPERTY_OPTIMISTIC = "datanucleus.Optimistic";
    public static final String PROPERTY_MULTITHREADED = "datanucleus.Multithreaded";
    public static final String PROPERTY_RETAIN_VALUES = "datanucleus.RetainValues";
    public static final String PROPERTY_RESTORE_VALUES = "datanucleus.RestoreValues";
    public static final String PROPERTY_SERIALIZE_READ = "datanucleus.SerializeRead";
    public static final String PROPERTY_PMF_NAME = "datanucleus.Name";
    public static final String PROPERTY_PERSISTENCE_UNIT_NAME = "datanucleus.PersistenceUnitName";
    public static final String PROPERTY_PERSISTENCE_XML_FILENAME = "datanucleus.persistenceXmlFilename";
    public static final String PROPERTY_SERVER_TIMEZONE_ID = "datanucleus.ServerTimeZoneID";
    public static final String PROPERTY_PROPERTIES_FILE = "datanucleus.propertiesFile";
    public static final String PROPERTY_PERSISTENCE_UNIT_LOAD_CLASSES = "datanucleus.persistenceUnitLoadClasses";
    public static final String PROPERTY_DELETION_POLICY = "datanucleus.deletionPolicy";
    public static final String PROPERTY_PERSISTENCE_BY_REACHABILITY_AT_COMMIT = "datanucleus.persistenceByReachabilityAtCommit";
    public static final String PROPERTY_MANAGE_RELATIONSHIPS = "datanucleus.manageRelationships";
    public static final String PROPERTY_MANAGE_RELATIONSHIPS_CHECKS = "datanucleus.manageRelationshipsChecks";
    public static final String PROPERTY_FIND_OBJECT_TYPE_CONVERSION = "datanucleus.findObject.typeConversion";
    public static final String PROPERTY_FIND_OBJECT_VALIDATE_WHEN_CACHED = "datanucleus.findObject.validateWhenCached";
    public static final String PROPERTY_ALLOW_CALLBACKS = "datanucleus.allowCallbacks";
    public static final String PROPERTY_DATASTORE_IDENTITY_TYPE = "datanucleus.datastoreIdentityType";
    public static final String PROPERTY_IDENTITY_STRING_TRANSLATOR_TYPE = "datanucleus.identityStringTranslatorType";
    public static final String PROPERTY_IDENTITY_KEY_TRANSLATOR_TYPE = "datanucleus.identityKeyTranslatorType";
    public static final String PROPERTY_USE_IMPLEMENTATION_CREATOR = "datanucleus.useImplementationCreator";

    public static final String PROPERTY_JMX_TYPE = "datanucleus.jmxType";
    public static final String PROPERTY_ENABLE_STATISTICS = "datanucleus.enableStatistics";

    public static final String PROPERTY_EXECUTION_CONTEXT_REAPER_THREAD = "datanucleus.executionContext.reaperThread";
    public static final String PROPERTY_EXECUTION_CONTEXT_MAX_IDLE = "datanucleus.executionContext.maxIdle";
    public static final String PROPERTY_EXECUTION_CONTEXT_CLOSE_ACTIVE_TX_ACTION = "datanucleus.executionContext.closeActiveTxAction";
    public static final String PROPERTY_OBJECT_PROVIDER_REAPER_THREAD = "datanucleus.objectProvider.reaperThread";
    public static final String PROPERTY_OBJECT_PROVIDER_MAX_IDLE = "datanucleus.objectProvider.maxIdle";
    public static final String PROPERTY_OBJECT_PROVIDER_CLASS_NAME = "datanucleus.objectProvider.className";

    public static final String PROPERTY_TRANSACTION_TYPE = "datanucleus.TransactionType";
    public static final String PROPERTY_TRANSACTION_JTA_LOCATOR = "datanucleus.jtaLocator";
    public static final String PROPERTY_TRANSACTION_JTA_JNDI_LOCATION = "datanucleus.jtaJndiLocation";
    public static final String PROPERTY_TRANSACTION_ISOLATION = "datanucleus.transactionIsolation";
    public static final String PROPERTY_NONTX_READ = "datanucleus.NontransactionalRead";
    public static final String PROPERTY_NONTX_WRITE = "datanucleus.NontransactionalWrite";
    public static final String PROPERTY_NONTX_ATOMIC = "datanucleus.nontx.atomic";

    public static final String PROPERTY_FLUSH_MODE = "datanucleus.flush.mode";
    public static final String PROPERTY_FLUSH_AUTO_OBJECT_LIMIT = "datanucleus.datastoreTransactionFlushLimit";

    public static final String PROPERTY_ATTACH_SAME_DATASTORE = "datanucleus.attachSameDatastore";
    public static final String PROPERTY_DETACH_ALL_ON_COMMIT = "datanucleus.DetachAllOnCommit";
    public static final String PROPERTY_DETACH_ALL_ON_ROLLBACK = "datanucleus.DetachAllOnRollback";
    public static final String PROPERTY_DETACH_ON_CLOSE = "datanucleus.DetachOnClose";
    public static final String PROPERTY_COPY_ON_ATTACH = "datanucleus.CopyOnAttach";
    public static final String PROPERTY_DETACH_AS_WRAPPED = "datanucleus.detachAsWrapped";
    public static final String PROPERTY_DETACH_DETACHMENT_FIELDS = "datanucleus.detachmentFields";
    public static final String PROPERTY_DETACH_DETACHED_STATE = "datanucleus.detachedState";
    public static final String PROPERTY_ALLOW_ATTACH_OF_TRANSIENT = "datanucleus.allowAttachOfTransient";
    public static final String PROPERTY_MAX_FETCH_DEPTH = "datanucleus.maxFetchDepth";

    public static final String PROPERTY_CONNECTION_URL = "datanucleus.ConnectionURL";
    public static final String PROPERTY_CONNECTION_DRIVER_NAME = "datanucleus.ConnectionDriverName";
    public static final String PROPERTY_CONNECTION_USER_NAME = "datanucleus.ConnectionUserName";
    public static final String PROPERTY_CONNECTION_PASSWORD = "datanucleus.ConnectionPassword";
    public static final String PROPERTY_CONNECTION_PASSWORD_DECRYPTER = "datanucleus.ConnectionPasswordDecrypter";
    public static final String PROPERTY_CONNECTION_FACTORY_NAME = "datanucleus.ConnectionFactoryName";
    public static final String PROPERTY_CONNECTION_FACTORY2_NAME = "datanucleus.ConnectionFactory2Name";
    public static final String PROPERTY_CONNECTION_FACTORY = "datanucleus.ConnectionFactory";
    public static final String PROPERTY_CONNECTION_FACTORY2 = "datanucleus.ConnectionFactory2";
    public static final String PROPERTY_CONNECTION_RESOURCETYPE = "datanucleus.connection.resourceType";
    public static final String PROPERTY_CONNECTION_RESOURCETYPE2 = "datanucleus.connection2.resourceType";
    public static final String PROPERTY_CONNECTION_POOLINGTYPE = "datanucleus.connectionPoolingType";
    public static final String PROPERTY_CONNECTION_POOLINGTYPE2 = "datanucleus.connectionPoolingType.nontx";
    public static final String PROPERTY_CONNECTION_NONTX_RELEASE_AFTER_USE = "datanucleus.connection.nontx.releaseAfterUse";
    public static final String PROPERTY_CONNECTION_SINGLE_CONNECTION = "datanucleus.connection.singleConnectionPerExecutionContext";

    public static final String PROPERTY_CACHE_L1_TYPE = "datanucleus.cache.level1.type";
    public static final String PROPERTY_CACHE_L2_TYPE = "datanucleus.cache.level2.type";
    public static final String PROPERTY_CACHE_COLLECTIONS = "datanucleus.cache.collections";
    public static final String PROPERTY_CACHE_COLLECTIONS_LAZY = "datanucleus.cache.collections.lazy";
    public static final String PROPERTY_CACHE_L2_NAME = "datanucleus.cache.level2.cacheName";
    public static final String PROPERTY_CACHE_L2_MAXSIZE = "datanucleus.cache.level2.maxSize";
    public static final String PROPERTY_CACHE_L2_LOADFIELDS = "datanucleus.cache.level2.loadFields";
    public static final String PROPERTY_CACHE_L2_CLEARATCLOSE = "datanucleus.cache.level2.clearAtClose";
    public static final String PROPERTY_CACHE_L2_TIMEOUT = "datanucleus.cache.level2.timeout";
    public static final String PROPERTY_CACHE_L2_BATCHSIZE = "datanucleus.cache.level2.batchSize";
    public static final String PROPERTY_CACHE_L2_MODE = "datanucleus.cache.level2.mode";
    public static final String PROPERTY_CACHE_L2_CACHE_EMBEDDED = "datanucleus.cache.level2.cacheEmbedded";
    public static final String PROPERTY_CACHE_L2_READ_THROUGH = "datanucleus.cache.level2.readThrough";
    public static final String PROPERTY_CACHE_L2_WRITE_THROUGH = "datanucleus.cache.level2.writeThrough";
    public static final String PROPERTY_CACHE_L2_STORE_BY_VALUE = "datanucleus.cache.level2.storeByValue";
    public static final String PROPERTY_CACHE_L2_STATISTICS_ENABLED = "datanucleus.cache.level2.statisticsEnabled";
    public static final String PROPERTY_CACHE_L2_RETRIEVE_MODE = "datanucleus.cache.level2.retrieveMode";
    public static final String PROPERTY_CACHE_L2_STORE_MODE = "datanucleus.cache.level2.storeMode";
    public static final String PROPERTY_CACHE_L2_UPDATE_MODE = "datanucleus.cache.level2.updateMode";
    public static final String PROPERTY_CACHE_QUERYCOMPILE_TYPE = "datanucleus.cache.queryCompilation.type";
    public static final String PROPERTY_CACHE_QUERYCOMPILEDATASTORE_TYPE = "datanucleus.cache.queryCompilationDatastore.type";
    public static final String PROPERTY_CACHE_QUERYRESULTS_TYPE = "datanucleus.cache.queryResults.type";
    public static final String PROPERTY_CACHE_QUERYRESULTS_NAME = "datanucleus.cache.queryResults.cacheName";
    public static final String PROPERTY_CACHE_QUERYRESULTS_MAXSIZE = "datanucleus.cache.queryResults.maxSize";

    public static final String PROPERTY_MAPPING = "datanucleus.mapping";
    public static final String PROPERTY_MAPPING_CATALOG = "datanucleus.mapping.Catalog";
    public static final String PROPERTY_MAPPING_SCHEMA = "datanucleus.mapping.Schema";
    public static final String PROPERTY_MAPPING_TENANT_ID = "datanucleus.TenantID";

    public static final String PROPERTY_IDENTIFIER_NAMING_FACTORY = "datanucleus.identifier.namingFactory";
    public static final String PROPERTY_IDENTIFIER_CASE = "datanucleus.identifier.case";
    public static final String PROPERTY_IDENTIFIER_TABLE_PREFIX = "datanucleus.identifier.tablePrefix";
    public static final String PROPERTY_IDENTIFIER_TABLE_SUFFIX = "datanucleus.identifier.tableSuffix";
    public static final String PROPERTY_IDENTIFIER_WORD_SEPARATOR = "datanucleus.identifier.wordSeparator";
    public static final String PROPERTY_IDENTIFIER_FACTORY = "datanucleus.identifierFactory"; // TODO Drop this when RDBMS uses NamingFactory

    public static final String PROPERTY_STORE_MANAGER_TYPE = "datanucleus.storeManagerType";
    public static final String PROPERTY_DATASTORE_READ_TIMEOUT = "datanucleus.datastoreReadTimeout";
    public static final String PROPERTY_DATASTORE_WRITE_TIMEOUT = "datanucleus.datastoreWriteTimeout";
    public static final String PROPERTY_DATASTORE_READONLY = "datanucleus.readOnlyDatastore";
    public static final String PROPERTY_DATASTORE_READONLY_ACTION = "datanucleus.readOnlyDatastoreAction";

    public static final String PROPERTY_STORE_ALLOW_REFS_WITHOUT_IMPLS = "datanucleus.store.allowReferencesWithNoImplementations";

    public static final String PROPERTY_SCHEMA_GENERATE_CREATE_SCHEMAS = "datanucleus.generateSchema.create-schemas";
    public static final String PROPERTY_SCHEMA_GENERATE_DATABASE_MODE = "datanucleus.generateSchema.database.mode";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_MODE = "datanucleus.generateSchema.scripts.mode";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_TARGET = "datanucleus.generateSchema.scripts.create.target";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_TARGET = "datanucleus.generateSchema.scripts.drop.target";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_SOURCE = "datanucleus.generateSchema.scripts.create.source";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE_ORDER = "datanucleus.generateSchema.scripts.create.order";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_SOURCE = "datanucleus.generateSchema.scripts.drop.source";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP_ORDER = "datanucleus.generateSchema.scripts.drop.order";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_LOAD_SOURCE = "datanucleus.generateSchema.scripts.load";

    public static final String PROPERTY_SCHEMA_AUTOCREATE_ALL = "datanucleus.schema.autoCreateAll";
    public static final String PROPERTY_SCHEMA_AUTOCREATE_SCHEMA = "datanucleus.schema.autoCreateSchema";
    public static final String PROPERTY_SCHEMA_AUTOCREATE_TABLES = "datanucleus.schema.autoCreateTables";
    public static final String PROPERTY_SCHEMA_AUTOCREATE_COLUMNS = "datanucleus.schema.autoCreateColumns";
    public static final String PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS = "datanucleus.schema.autoCreateConstraints";
    public static final String PROPERTY_SCHEMA_AUTOCREATE_WARNONERROR = "datanucleus.schema.autoCreateWarnOnError";
    public static final String PROPERTY_SCHEMA_AUTODELETE_COLUMNS = "datanucleus.schema.autoDeleteColumns";
    public static final String PROPERTY_SCHEMA_VALIDATE_ALL = "datanucleus.schema.validateAll";
    public static final String PROPERTY_SCHEMA_VALIDATE_TABLES = "datanucleus.schema.validateTables";
    public static final String PROPERTY_SCHEMA_VALIDATE_COLUMNS = "datanucleus.schema.validateColumns";
    public static final String PROPERTY_SCHEMA_VALIDATE_CONSTRAINTS = "datanucleus.schema.validateConstraints";

    public static final String PROPERTY_VALIDATION_MODE = "datanucleus.validation.mode";
    public static final String PROPERTY_VALIDATION_GROUP_PREPERSIST = "datanucleus.validation.group.pre-persist";
    public static final String PROPERTY_VALIDATION_GROUP_PREUPDATE = "datanucleus.validation.group.pre-update";
    public static final String PROPERTY_VALIDATION_GROUP_PREREMOVE = "datanucleus.validation.group.pre-remove";
    public static final String PROPERTY_VALIDATION_FACTORY = "datanucleus.validation.factory";

    public static final String PROPERTY_AUTOSTART_MECHANISM = "datanucleus.autoStartMechanism";
    public static final String PROPERTY_AUTOSTART_MODE = "datanucleus.autoStartMechanismMode";
    public static final String PROPERTY_AUTOSTART_XMLFILE = "datanucleus.autoStartMechanismXmlFile";
    public static final String PROPERTY_AUTOSTART_CLASSNAMES = "datanucleus.autoStartClassNames";
    public static final String PROPERTY_AUTOSTART_METADATAFILES = "datanucleus.autoStartMetaDataFiles";

    public static final String PROPERTY_VALUEGEN_TXN_ISOLATION = "datanucleus.valuegeneration.transactionIsolation";
    public static final String PROPERTY_VALUEGEN_TXN_ATTRIBUTE = "datanucleus.valuegeneration.transactionAttribute";
    public static final String PROPERTY_VALUEGEN_SEQUENCE_ALLOCSIZE = "datanucleus.valuegeneration.sequence.allocationSize";
    public static final String PROPERTY_VALUEGEN_INCREMENT_ALLOCSIZE = "datanucleus.valuegeneration.increment.allocationSize";

    public static final String PROPERTY_QUERY_SQL_ALLOWALL = "datanucleus.query.sql.allowAll";
    public static final String PROPERTY_QUERY_JDOQL_ALLOWALL = "datanucleus.query.jdoql.allowAll";
    public static final String PROPERTY_QUERY_FLUSH_BEFORE_EXECUTE = "datanucleus.query.flushBeforeExecution";
    public static final String PROPERTY_QUERY_USE_FETCHPLAN = "datanucleus.query.useFetchPlan";
    public static final String PROPERTY_QUERY_CHECK_UNUSED_PARAMS = "datanucleus.query.checkUnusedParameters";
    public static final String PROPERTY_QUERY_COMPILE_OPTIMISED = "datanucleus.query.compileOptimised";
    public static final String PROPERTY_QUERY_LOAD_RESULTS_AT_COMMIT = "datanucleus.query.loadResultsAtCommit";
    public static final String PROPERTY_QUERY_COMPILATION_CACHED = "datanucleus.query.compilation.cached";
    public static final String PROPERTY_QUERY_RESULTS_CACHED = "datanucleus.query.results.cached";
    public static final String PROPERTY_QUERY_EVALUATE_IN_MEMORY = "datanucleus.query.evaluateInMemory";
    public static final String PROPERTY_QUERY_RESULTCACHE_VALIDATEOBJECTS = "datanucleus.query.resultCache.validateObjects";
    public static final String PROPERTY_QUERY_RESULT_SIZE_METHOD = "datanucleus.query.resultSizeMethod";
    public static final String PROPERTY_QUERY_COMPILE_NAMED_QUERIES_AT_STARTUP = "datanucleus.query.compileNamedQueriesAtStartup";
}
