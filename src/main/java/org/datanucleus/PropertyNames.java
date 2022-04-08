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
    public static final String PROPERTY_PLUGIN_REGISTRY_CLASSNAME = "datanucleus.plugin.pluginRegistryClassName".toLowerCase();
    public static final String PROPERTY_PLUGIN_ALLOW_USER_BUNDLES = "datanucleus.plugin.allowUserBundles".toLowerCase();
    public static final String PROPERTY_PLUGIN_VALIDATEPLUGINS = "datanucleus.plugin.validatePlugins".toLowerCase();
    public static final String PROPERTY_PLUGIN_REGISTRYBUNDLECHECK = "datanucleus.plugin.pluginRegistryBundleCheck".toLowerCase();

    public static final String PROPERTY_CLASSLOADER_RESOLVER_NAME = "datanucleus.classLoaderResolverName".toLowerCase();
    public static final String PROPERTY_CLASSLOADER_PRIMARY = "datanucleus.primaryClassLoader".toLowerCase();

    public static final String PROPERTY_METADATA_IGNORE_METADATA_FOR_MISSING_CLASSES = "datanucleus.metadata.ignoreMetaDataForMissingClasses".toLowerCase();
    public static final String PROPERTY_METADATA_ALWAYS_DETACHABLE = "datanucleus.metadata.alwaysDetachable".toLowerCase();
    public static final String PROPERTY_METADATA_XML_VALIDATE = "datanucleus.metadata.xml.validate";
    public static final String PROPERTY_METADATA_XML_NAMESPACE_AWARE = "datanucleus.metadata.xml.namespaceaware";
    public static final String PROPERTY_METADATA_AUTOREGISTER = "datanucleus.metadata.autoregistration";
    public static final String PROPERTY_METADATA_ALLOW_XML = "datanucleus.metadata.allowXML".toLowerCase();
    public static final String PROPERTY_METADATA_ALLOW_ANNOTATIONS = "datanucleus.metadata.allowAnnotations".toLowerCase();
    public static final String PROPERTY_METADATA_ALLOW_LOAD_AT_RUNTIME = "datanucleus.metadata.allowLoadAtRuntime".toLowerCase();
    public static final String PROPERTY_METADATA_SUPPORT_ORM = "datanucleus.metadata.supportORM".toLowerCase();
    public static final String PROPERTY_METADATA_JDO_SUFFIX = "datanucleus.metadata.jdoFileExtension".toLowerCase();
    public static final String PROPERTY_METADATA_ORM_SUFFIX = "datanucleus.metadata.ormFileExtension".toLowerCase();
    public static final String PROPERTY_METADATA_JDOQUERY_SUFFIX = "datanucleus.metadata.jdoqueryFileExtension".toLowerCase();
    public static final String PROPERTY_METADATA_SCANNER = "datanucleus.metadata.scanner";
    public static final String PROPERTY_METADATA_DEFAULT_INHERITANCE_STRATEGY = "datanucleus.metadata.defaultInheritanceStrategy".toLowerCase();
    public static final String PROPERTY_METADATA_EMBEDDED_PC_FLAT = "datanucleus.metadata.embedded.flat";
    public static final String PROPERTY_METADATA_DEFAULT_NULLABLE = "datanucleus.metadata.defaultNullable".toLowerCase();
    public static final String PROPERTY_METADATA_LISTENER_OBJECT = "datanucleus.metadata.listener.object";
    public static final String PROPERTY_METADATA_JAVAX_VALIDATION_SHORTCUTS = "datanucleus.metadata.javaxValidationShortcuts".toLowerCase();

    public static final String PROPERTY_METADATA_USE_DISCRIMINATOR_FOR_SINGLE_TABLE = "datanucleus.metadata.useDiscriminatorForSingleTable".toLowerCase();
    public static final String PROPERTY_METADATA_USE_DISCRIMINATOR_DEFAULT_CLASS_NAME = "datanucleus.metadata.useDiscriminatorClassNameByDefault".toLowerCase();

    public static final String PROPERTY_IGNORE_CACHE = "datanucleus.ignorecache";
    public static final String PROPERTY_OPTIMISTIC = "datanucleus.optimistic";
    public static final String PROPERTY_MULTITHREADED = "datanucleus.multithreaded";
    public static final String PROPERTY_RETAIN_VALUES = "datanucleus.retainvalues";
    public static final String PROPERTY_RESTORE_VALUES = "datanucleus.restorevalues";
    public static final String PROPERTY_SERIALIZE_READ = "datanucleus.serializeread";
    public static final String PROPERTY_PMF_NAME = "datanucleus.name";
    public static final String PROPERTY_PERSISTENCE_UNIT_NAME = "datanucleus.persistenceunitname";
    public static final String PROPERTY_PERSISTENCE_XML_FILENAME = "datanucleus.persistenceXmlFilename".toLowerCase();
    public static final String PROPERTY_SERVER_TIMEZONE_ID = "datanucleus.serverTimeZoneID".toLowerCase();
    public static final String PROPERTY_PROPERTIES_FILE = "datanucleus.propertiesFile".toLowerCase();
    public static final String PROPERTY_PERSISTENCE_UNIT_LOAD_CLASSES = "datanucleus.persistenceUnitLoadClasses".toLowerCase();
    public static final String PROPERTY_DELETION_POLICY = "datanucleus.deletionPolicy".toLowerCase();
    public static final String PROPERTY_PERSISTENCE_BY_REACHABILITY_AT_COMMIT = "datanucleus.persistenceByReachabilityAtCommit".toLowerCase();
    public static final String PROPERTY_MANAGE_RELATIONSHIPS = "datanucleus.manageRelationships".toLowerCase();
    public static final String PROPERTY_MANAGE_RELATIONSHIPS_CHECKS = "datanucleus.manageRelationshipsChecks".toLowerCase();
    public static final String PROPERTY_FIND_OBJECT_TYPE_CONVERSION = "datanucleus.findobject.typeConversion".toLowerCase();
    public static final String PROPERTY_FIND_OBJECT_VALIDATE_WHEN_CACHED = "datanucleus.findobject.validateWhenCached".toLowerCase();
    public static final String PROPERTY_ALLOW_CALLBACKS = "datanucleus.allowCallbacks".toLowerCase();
    public static final String PROPERTY_DATASTORE_IDENTITY_TYPE = "datanucleus.datastoreIdentityType".toLowerCase();
    public static final String PROPERTY_IDENTITY_STRING_TRANSLATOR_TYPE = "datanucleus.identityStringTranslatorType".toLowerCase();
    public static final String PROPERTY_IDENTITY_KEY_TRANSLATOR_TYPE = "datanucleus.identityKeyTranslatorType".toLowerCase();
    public static final String PROPERTY_USE_IMPLEMENTATION_CREATOR = "datanucleus.useImplementationCreator".toLowerCase();

    public static final String PROPERTY_RELATION_IDENTITY_STORAGE_MODE = "datanucleus.relation.identityStorageMode".toLowerCase();

    public static final String PROPERTY_TYPE_WRAPPER_BASIS = "datanucleus.type.wrapper.basis";

    public static final String PROPERTY_VERSION_NUMBER_INITIAL_VALUE = "datanucleus.version.versionnumber.initialvalue";

    public static final String PROPERTY_JMX_TYPE = "datanucleus.jmxtype";
    public static final String PROPERTY_ENABLE_STATISTICS = "datanucleus.enablestatistics";

    public static final String PROPERTY_EXECUTION_CONTEXT_REAPER_THREAD = "datanucleus.executioncontext.reaperthread";
    public static final String PROPERTY_EXECUTION_CONTEXT_MAX_IDLE = "datanucleus.executioncontext.maxidle";
    public static final String PROPERTY_EXECUTION_CONTEXT_CLOSE_ACTIVE_TX_ACTION = "datanucleus.executioncontext.closeactivetxaction";
//    public static final String PROPERTY_STATE_MANAGER_REAPER_THREAD = "datanucleus.stateManager.reaperThread".toLowerCase();
//    public static final String PROPERTY_STATE_MANAGER_MAX_IDLE = "datanucleus.stateManager.maxIdle".toLowerCase();
    public static final String PROPERTY_STATE_MANAGER_CLASS_NAME = "datanucleus.statemanager.classname";

    public static final String PROPERTY_TRANSACTION_TYPE = "datanucleus.transaction.type";
    public static final String PROPERTY_TRANSACTION_ISOLATION = "datanucleus.transaction.isolation";
    public static final String PROPERTY_TRANSACTION_JTA_LOCATOR = "datanucleus.transaction.jta.transactionmanagerlocator";
    public static final String PROPERTY_TRANSACTION_JTA_JNDI_LOCATION = "datanucleus.transaction.jta.transactionmanagerjndi";
    public static final String PROPERTY_TRANSACTION_NONTX_READ = "datanucleus.transaction.nontx.read";
    public static final String PROPERTY_TRANSACTION_NONTX_WRITE = "datanucleus.transaction.nontx.write";
    public static final String PROPERTY_TRANSACTION_NONTX_ATOMIC = "datanucleus.transaction.nontx.atomic";

    public static final String PROPERTY_FLUSH_MODE = "datanucleus.flush.mode";
    public static final String PROPERTY_FLUSH_AUTO_OBJECT_LIMIT = "datanucleus.flush.auto.objectLimit".toLowerCase();

    public static final String PROPERTY_ATTACH_SAME_DATASTORE = "datanucleus.attachSameDatastore".toLowerCase();
    public static final String PROPERTY_DETACH_ALL_ON_COMMIT = "datanucleus.detachAllOnCommit".toLowerCase();
    public static final String PROPERTY_DETACH_ALL_ON_ROLLBACK = "datanucleus.detachAllOnRollback".toLowerCase();
    public static final String PROPERTY_DETACH_ON_CLOSE = "datanucleus.detachOnClose".toLowerCase();
    public static final String PROPERTY_COPY_ON_ATTACH = "datanucleus.copyOnAttach".toLowerCase();
    public static final String PROPERTY_DETACH_AS_WRAPPED = "datanucleus.detachAsWrapped".toLowerCase();
    public static final String PROPERTY_DETACH_DETACHMENT_FIELDS = "datanucleus.detachmentFields".toLowerCase();
    public static final String PROPERTY_DETACH_DETACHED_STATE = "datanucleus.detachedState".toLowerCase();
    public static final String PROPERTY_ALLOW_ATTACH_OF_TRANSIENT = "datanucleus.allowAttachOfTransient".toLowerCase();
    public static final String PROPERTY_MAX_FETCH_DEPTH = "datanucleus.maxFetchDepth".toLowerCase();

    public static final String PROPERTY_CONNECTION_URL = "datanucleus.connectionURL".toLowerCase();
    public static final String PROPERTY_CONNECTION_DRIVER_NAME = "datanucleus.connectionDriverName".toLowerCase();
    public static final String PROPERTY_CONNECTION_USER_NAME = "datanucleus.connectionUserName".toLowerCase();
    public static final String PROPERTY_CONNECTION_PASSWORD = "datanucleus.connectionPassword".toLowerCase();
    public static final String PROPERTY_CONNECTION_PASSWORD_DECRYPTER = "datanucleus.connectionPasswordDecrypter".toLowerCase();
    public static final String PROPERTY_CONNECTION_FACTORY_NAME = "datanucleus.connectionFactoryName".toLowerCase();
    public static final String PROPERTY_CONNECTION_FACTORY2_NAME = "datanucleus.connectionFactory2Name".toLowerCase();
    public static final String PROPERTY_CONNECTION_FACTORY = "datanucleus.connectionFactory".toLowerCase();
    public static final String PROPERTY_CONNECTION_FACTORY2 = "datanucleus.connectionFactory2".toLowerCase();
    public static final String PROPERTY_CONNECTION_RESOURCETYPE = "datanucleus.connection.resourceType".toLowerCase();
    public static final String PROPERTY_CONNECTION_RESOURCETYPE2 = "datanucleus.connection2.resourceType".toLowerCase();
    public static final String PROPERTY_CONNECTION_POOLINGTYPE = "datanucleus.connectionPoolingType".toLowerCase();
    public static final String PROPERTY_CONNECTION_POOLINGTYPE2 = "datanucleus.connectionPoolingType.nontx".toLowerCase();
    public static final String PROPERTY_CONNECTION_NONTX_RELEASE_AFTER_USE = "datanucleus.connection.nontx.releaseAfterUse".toLowerCase();
    public static final String PROPERTY_CONNECTION_SINGLE_CONNECTION = "datanucleus.connection.singleConnectionPerExecutionContext".toLowerCase();

    public static final String PROPERTY_DATASTORE_READ_TIMEOUT = "datanucleus.datastorereadtimeout";
    public static final String PROPERTY_DATASTORE_WRITE_TIMEOUT = "datanucleus.datastorewritetimeout";

    public static final String PROPERTY_CACHE_L1_TYPE = "datanucleus.cache.level1.type";

    public static final String PROPERTY_CACHE_COLLECTIONS = "datanucleus.cache.collections";
    public static final String PROPERTY_CACHE_COLLECTIONS_LAZY = "datanucleus.cache.collections.lazy";

    public static final String PROPERTY_CACHE_L2_TYPE = "datanucleus.cache.level2.type";
    public static final String PROPERTY_CACHE_L2_NAME = "datanucleus.cache.level2.cachename";
    public static final String PROPERTY_CACHE_L2_MAXSIZE = "datanucleus.cache.level2.maxsize";
    public static final String PROPERTY_CACHE_L2_LOADFIELDS = "datanucleus.cache.level2.loadfields";
    public static final String PROPERTY_CACHE_L2_CLEARATCLOSE = "datanucleus.cache.level2.clearatclose";
    public static final String PROPERTY_CACHE_L2_EXPIRY_MILLIS = "datanucleus.cache.level2.expirymillis";
    public static final String PROPERTY_CACHE_L2_BATCHSIZE = "datanucleus.cache.level2.batchsize";
    public static final String PROPERTY_CACHE_L2_MODE = "datanucleus.cache.level2.mode";
    public static final String PROPERTY_CACHE_L2_CACHE_EMBEDDED = "datanucleus.cache.level2.cacheembedded";
    public static final String PROPERTY_CACHE_L2_READ_THROUGH = "datanucleus.cache.level2.readthrough";
    public static final String PROPERTY_CACHE_L2_WRITE_THROUGH = "datanucleus.cache.level2.writethrough";
    public static final String PROPERTY_CACHE_L2_STORE_BY_VALUE = "datanucleus.cache.level2.storebyvalue";
    public static final String PROPERTY_CACHE_L2_STATISTICS_ENABLED = "datanucleus.cache.level2.statisticsenabled";
    public static final String PROPERTY_CACHE_L2_RETRIEVE_MODE = "datanucleus.cache.level2.retrievemode";
    public static final String PROPERTY_CACHE_L2_STORE_MODE = "datanucleus.cache.level2.storemode";
    public static final String PROPERTY_CACHE_L2_UPDATE_MODE = "datanucleus.cache.level2.updatemode";
    public static final String PROPERTY_CACHE_L2_CONFIG_FILE = "datanucleus.cache.level2.configurationfile";

    public static final String PROPERTY_CACHE_QUERYCOMPILE_TYPE = "datanucleus.cache.queryCompilation.type".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYCOMPILE_NAME = "datanucleus.cache.queryCompilation.cacheName".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYCOMPILEDATASTORE_TYPE = "datanucleus.cache.queryCompilationDatastore.type".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYCOMPILEDATASTORE_NAME = "datanucleus.cache.queryCompilationDatastore.cacheName".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYRESULTS_TYPE = "datanucleus.cache.queryResults.type".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYRESULTS_NAME = "datanucleus.cache.queryResults.cacheName".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYRESULTS_MAXSIZE = "datanucleus.cache.queryResults.maxSize".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYRESULTS_CLEARATCLOSE = "datanucleus.cache.queryResults.clearAtClose".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYRESULTS_EXPIRY_MILLIS = "datanucleus.cache.queryResults.expiryMillis".toLowerCase();
    public static final String PROPERTY_CACHE_QUERYRESULTS_CONFIG_FILE = "datanucleus.cache.queryResults.configurationFile".toLowerCase();

    public static final String PROPERTY_MAPPING = "datanucleus.mapping";
    public static final String PROPERTY_MAPPING_CATALOG = "datanucleus.mapping.catalog";
    public static final String PROPERTY_MAPPING_SCHEMA = "datanucleus.mapping.schema";

    public static final String PROPERTY_MAPPING_TENANT_ID = "datanucleus.tenantid";
    public static final String PROPERTY_MAPPING_TENANT_READ_IDS = "datanucleus.tenantreadids";
    public static final String PROPERTY_MAPPING_TENANT_PROVIDER = "datanucleus.tenantprovider";

    public static final String PROPERTY_MAPPING_CURRENT_USER = "datanucleus.currentuser";
    public static final String PROPERTY_MAPPING_CURRENT_USER_PROVIDER = "datanucleus.currentuserprovider";

    public static final String PROPERTY_STORE_ALLOW_REFS_WITHOUT_IMPLS = "datanucleus.store.allowreferenceswithnoimplementations";

    public static final String PROPERTY_IDENTIFIER_NAMING_FACTORY = "datanucleus.identifier.namingfactory";
    public static final String PROPERTY_IDENTIFIER_CASE = "datanucleus.identifier.case";
    public static final String PROPERTY_IDENTIFIER_TABLE_PREFIX = "datanucleus.identifier.tableprefix";
    public static final String PROPERTY_IDENTIFIER_TABLE_SUFFIX = "datanucleus.identifier.tablesuffix";
    public static final String PROPERTY_IDENTIFIER_WORD_SEPARATOR = "datanucleus.identifier.wordseparator";
    public static final String PROPERTY_IDENTIFIER_FACTORY = "datanucleus.identifierfactory"; // TODO Drop this when RDBMS uses NamingFactory

    public static final String PROPERTY_DATASTORE_READONLY = "datanucleus.readonlydatastore";
    public static final String PROPERTY_DATASTORE_READONLY_ACTION = "datanucleus.readonlydatastoreaction";

    public static final String PROPERTY_SCHEMA_GENERATE_DATABASE_MODE = "datanucleus.schema.generatedatabase.mode";
    public static final String PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_SCRIPT = "datanucleus.schema.generatedatabase.createscript";
    public static final String PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_SCRIPT = "datanucleus.schema.generatedatabase.dropscript";
    public static final String PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_ORDER = "datanucleus.schema.generatedatabase.create.order";
    public static final String PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_ORDER = "datanucleus.schema.generatedatabase.drop.order";

    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_MODE = "datanucleus.schema.generatescripts.mode";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_CREATE = "datanucleus.schema.generatescripts.create";
    public static final String PROPERTY_SCHEMA_GENERATE_SCRIPTS_DROP = "datanucleus.schema.generatescripts.drop";

    public static final String PROPERTY_SCHEMA_LOAD_SCRIPT = "datanucleus.schema.loadscript";

    public static final String PROPERTY_SCHEMA_AUTOCREATE_ALL = "datanucleus.schema.autoCreateAll".toLowerCase();
    public static final String PROPERTY_SCHEMA_AUTOCREATE_DATABASE = "datanucleus.schema.autoCreateDatabase".toLowerCase();
    public static final String PROPERTY_SCHEMA_AUTOCREATE_TABLES = "datanucleus.schema.autoCreateTables".toLowerCase();
    public static final String PROPERTY_SCHEMA_AUTOCREATE_COLUMNS = "datanucleus.schema.autoCreateColumns".toLowerCase();
    public static final String PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS = "datanucleus.schema.autoCreateConstraints".toLowerCase();
    public static final String PROPERTY_SCHEMA_AUTOCREATE_WARNONERROR = "datanucleus.schema.autoCreateWarnOnError".toLowerCase();
    public static final String PROPERTY_SCHEMA_AUTODELETE_COLUMNS = "datanucleus.schema.autoDeleteColumns".toLowerCase();
    public static final String PROPERTY_SCHEMA_VALIDATE_ALL = "datanucleus.schema.validateAll".toLowerCase();
    public static final String PROPERTY_SCHEMA_VALIDATE_TABLES = "datanucleus.schema.validateTables".toLowerCase();
    public static final String PROPERTY_SCHEMA_VALIDATE_COLUMNS = "datanucleus.schema.validateColumns".toLowerCase();
    public static final String PROPERTY_SCHEMA_VALIDATE_CONSTRAINTS = "datanucleus.schema.validateConstraints".toLowerCase();
    public static final String PROPERTY_SCHEMA_TXN_ISOLATION = "datanucleus.schema.transactionIsolation".toLowerCase();

    public static final String PROPERTY_VALIDATION_MODE = "datanucleus.validation.mode";
    public static final String PROPERTY_VALIDATION_GROUP_PREPERSIST = "datanucleus.validation.group.pre-persist";
    public static final String PROPERTY_VALIDATION_GROUP_PREUPDATE = "datanucleus.validation.group.pre-update";
    public static final String PROPERTY_VALIDATION_GROUP_PREREMOVE = "datanucleus.validation.group.pre-remove";
    public static final String PROPERTY_VALIDATION_FACTORY = "datanucleus.validation.factory";

    public static final String PROPERTY_CDI_BEAN_MANAGER = "datanucleus.cdi.bean.manager";

    public static final String PROPERTY_AUTOSTART_MECHANISM = "datanucleus.autostartmechanism";
    public static final String PROPERTY_AUTOSTART_MODE = "datanucleus.autostartmechanismmode";
    public static final String PROPERTY_AUTOSTART_XMLFILE = "datanucleus.autostartmechanismxmlfile";
    public static final String PROPERTY_AUTOSTART_CLASSNAMES = "datanucleus.autostartclassnames";
    public static final String PROPERTY_AUTOSTART_METADATAFILES = "datanucleus.autostartmetadatafiles";

    public static final String PROPERTY_VALUEGEN_TXN_ISOLATION = "datanucleus.valuegeneration.transactionisolation";
    public static final String PROPERTY_VALUEGEN_TXN_ATTRIBUTE = "datanucleus.valuegeneration.transactionattribute";
    public static final String PROPERTY_VALUEGEN_SEQUENCE_ALLOCSIZE = "datanucleus.valuegeneration.sequence.allocationsize";
    public static final String PROPERTY_VALUEGEN_INCREMENT_ALLOCSIZE = "datanucleus.valuegeneration.increment.allocationsize";

    public static final String PROPERTY_QUERY_SQL_ALLOWALL = "datanucleus.query.sql.allowall";
    public static final String PROPERTY_QUERY_SQL_SYNTAXCHECKS = "datanucleus.query.sql.syntaxchecks";
    public static final String PROPERTY_QUERY_JDOQL_STRICT = "datanucleus.query.jdoql.strict";
    public static final String PROPERTY_QUERY_JDOQL_ALLOWALL = "datanucleus.query.jdoql.allowall";
    public static final String PROPERTY_QUERY_JPQL_STRICT = "datanucleus.query.jpql.strict";
    public static final String PROPERTY_QUERY_JPQL_ALLOW_RANGE = "datanucleus.query.jpql.allowrange";

    public static final String PROPERTY_QUERY_FLUSH_BEFORE_EXECUTE = "datanucleus.query.flushbeforeexecution";
    public static final String PROPERTY_QUERY_USE_FETCHPLAN = "datanucleus.query.usefetchplan";
    public static final String PROPERTY_QUERY_CHECK_UNUSED_PARAMS = "datanucleus.query.checkunusedparameters";
    public static final String PROPERTY_QUERY_COMPILE_OPTIMISE_VAR_THIS = "datanucleus.query.compileoptimisevarthis";
    public static final String PROPERTY_QUERY_LOAD_RESULTS_AT_COMMIT = "datanucleus.query.loadresultsatcommit";
    public static final String PROPERTY_QUERY_COMPILATION_CACHED = "datanucleus.query.compilation.cached";
    public static final String PROPERTY_QUERY_RESULTS_CACHED = "datanucleus.query.results.cached";
    public static final String PROPERTY_QUERY_EVALUATE_IN_MEMORY = "datanucleus.query.evaluateinmemory";
    public static final String PROPERTY_QUERY_RESULTCACHE_VALIDATEOBJECTS = "datanucleus.query.resultcache.validateobjects";
    public static final String PROPERTY_QUERY_RESULT_SIZE_METHOD = "datanucleus.query.resultsizemethod";
    public static final String PROPERTY_QUERY_COMPILE_NAMED_QUERIES_AT_STARTUP = "datanucleus.query.compilenamedqueriesatstartup";
}
