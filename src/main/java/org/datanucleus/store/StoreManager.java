/**********************************************************************
Copyright (c) 2008 Erik Bengtson and others. All rights reserved. 
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
package org.datanucleus.store;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.flush.FlushProcess;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.store.connection.ConnectionManager;
import org.datanucleus.store.query.Extent;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.query.QueryManager;
import org.datanucleus.store.schema.StoreSchemaHandler;
import org.datanucleus.store.schema.naming.NamingFactory;
import org.datanucleus.store.valuegenerator.ValueGenerationManager;

/**
 * Interface defining management of a datastore.
 * To be implemented by all new datastore support. Please use AbstractStoreManager and extend it.
 * All StoreManagers should have a single constructor with signature
 * <pre>
 * public MyStoreManager(ClassLoaderResolver clr, NucleusContext ctx, Map&lt;String, Object&gt; props)
 * {
 * }
 * </pre>
 * The constructor arguments are
 * <ul>
 * <li>ClassLoader Resolver, for assistance in any class loading needed.</li>
 * <li>The context where the StoreManager is being used, providing services like metadata, etc</li>
 * <li>Any persistence properties that are defined for the store, to configure schema generation etc</li>
 * </ul>
 * See also AbstractStoreManager which provides something that can be extended, simplifying the process of
 * implementing a StoreManager; note that the first argument of the constructor to AbstractStoreManager is
 * the "key" for your StoreManager (e.g ODF has "odf" as its key, RDBMS has "rdbms").
 */
public interface StoreManager
{
    /** Whether the store supports persistence of "application" identity. */
    public static final String OPTION_APPLICATION_ID = "ApplicationId";
    /** Whether the store supports persistence of composite "application" identity. */
    public static final String OPTION_APPLICATION_COMPOSITE_ID = "ApplicationCompositeId";
    /** Whether the store supports persistence of "datastore" identity. */
    public static final String OPTION_DATASTORE_ID = "DatastoreId";
    /** Whether the store supports persistence of "non-durable" identity. */
    public static final String OPTION_NONDURABLE_ID = "NonDurableId";

    /** Whether the store supports "ORM" concepts in general. */
    public static final String OPTION_ORM = "ORM";

    /** Whether the store supports embedding PC objects (into the owning objects datastore representation). */
    public static final String OPTION_ORM_EMBEDDED_PC = "ORM.EmbeddedPC";
    /** Whether the store supports embedding collections (into the owning objects datastore representation). */
    public static final String OPTION_ORM_EMBEDDED_COLLECTION = "ORM.EmbeddedCollection";
    /** Whether the store supports embedding maps (into the owning objects datastore representation). */
    public static final String OPTION_ORM_EMBEDDED_MAP = "ORM.EmbeddedMap";
    /** Whether the store supports embedding arrays (into the owning objects datastore representation). */
    public static final String OPTION_ORM_EMBEDDED_ARRAY = "ORM.EmbeddedArray";

    /** Whether the embedded object is storable nested like in JSON (default is flat embedding). */
    public static final String OPTION_ORM_EMBEDDED_PC_NESTED = "ORM.EmbeddedPC.Nested";

    /** Whether the embedded element is storable nested like in JSON (default is in separate table). */
    public static final String OPTION_ORM_EMBEDDED_COLLECTION_NESTED = "ORM.EmbeddedCollection.Nested";
    /** Whether the embedded element is storable nested like in JSON (default is in separate table). */
    public static final String OPTION_ORM_EMBEDDED_MAP_NESTED = "ORM.EmbeddedMap.Nested";
    /** Whether the embedded key/value is storable nested like in JSON (default is in separate table). */
    public static final String OPTION_ORM_EMBEDDED_ARRAY_NESTED = "ORM.EmbeddedArray.Nested";

    /** Whether the store supports serialisation of a PC into a "column". */
    public static final String OPTION_ORM_SERIALISED_PC = "ORM.SerialisedPC";
    /** Whether the store supports serialisation of a collection element into a "column". */
    public static final String OPTION_ORM_SERIALISED_COLLECTION_ELEMENT = "ORM.SerialisedCollectionElement";
    /** Whether the store supports serialisation of a map key into a "column". */
    public static final String OPTION_ORM_SERIALISED_MAP_KEY = "ORM.SerialisedMapKey";
    /** Whether the store supports serialisation of a map value into a "column". */
    public static final String OPTION_ORM_SERIALISED_MAP_VALUE = "ORM.SerialisedMapValue";
    /** Whether the store supports serialisation of a array element into a "column". */
    public static final String OPTION_ORM_SERIALISED_ARRAY_ELEMENT = "ORM.SerialisedArrayElement";

    /** Whether the store supports "secondary tables". */
    public static final String OPTION_ORM_SECONDARY_TABLE = "ORM.SecondaryTable";
    /** Whether the store supports "foreign keys". */
    public static final String OPTION_ORM_FOREIGN_KEYS = "ORM.ForeignKeys";

    /** Whether the store supports "complete table" inheritance. */
    public static final String OPTION_ORM_INHERITANCE_COMPLETE_TABLE = "ORM.Inheritance.CompleteTable";
    /** Whether the store supports "single table" inheritance. */
    public static final String OPTION_ORM_INHERITANCE_SINGLE_TABLE = "ORM.Inheritance.SingleTable";
    /** Whether the store supports "joined table" inheritance. */
    public static final String OPTION_ORM_INHERITANCE_JOINED_TABLE = "ORM.Inheritance.JoinedTable";

    /** Whether the store supports ACID transactions. */
    public static final String OPTION_TRANSACTION_ACID = "Transaction.ACID";

    /** Whether the store supports "read-committed" transaction isolation. */
    public static final String OPTION_TXN_ISOLATION_READ_COMMITTED = "TransactionIsolationLevel.read-committed";
    /** Whether the store supports "read-uncommitted" transaction isolation. */
    public static final String OPTION_TXN_ISOLATION_READ_UNCOMMITTED = "TransactionIsolationLevel.read-uncommitted";
    /** Whether the store supports "repeatable-read" transaction isolation. */
    public static final String OPTION_TXN_ISOLATION_REPEATABLE_READ = "TransactionIsolationLevel.repeatable-read";
    /** Whether the store supports "serializable" transaction isolation. */
    public static final String OPTION_TXN_ISOLATION_SERIALIZABLE = "TransactionIsolationLevel.serializable";

    /** Whether the store supports cancellation of queries once started. */
    public static final String OPTION_QUERY_CANCEL = "Query.Cancel";
    /** Whether the store supports JDOQL queries performing "bulk" INSERT of data. */
    public static final String OPTION_QUERY_JDOQL_BULK_INSERT = "Query.JDOQL.BulkInsert";
    /** Whether the store supports JDOQL queries performing "bulk" UPDATE of data. */
    public static final String OPTION_QUERY_JDOQL_BULK_UPDATE = "Query.JDOQL.BulkUpdate";
    /** Whether the store supports JDOQL queries performing "bulk" DELETE of data. */
    public static final String OPTION_QUERY_JDOQL_BULK_DELETE = "Query.JDOQL.BulkDelete";
    /** Whether the store supports use of "bitwise" operations in JDOQL queries. */
    public static final String OPTION_QUERY_JDOQL_BITWISE_OPS = "Query.JDOQL.BitwiseOperations";
    /** Whether the store supports JPQL queries performing "bulk" INSERT of data. */
    public static final String OPTION_QUERY_JPQL_BULK_INSERT = "Query.JPQL.BulkInsert";
    /** Whether the store supports JPQL queries performing "bulk" UPDATE of data. */
    public static final String OPTION_QUERY_JPQL_BULK_UPDATE = "Query.JPQL.BulkUpdate";
    /** Whether the store supports JPQL queries performing "bulk" DELETE of data. */
    public static final String OPTION_QUERY_JPQL_BULK_DELETE = "Query.JPQL.BulkDelete";

    /** Whether the store supports specification of a timeout on datastore operations. */
    public static final String OPTION_DATASTORE_TIMEOUT = "Datastore.Timeout";
    /** Whether the store will persist milliseconds when persisting a "time" type. */
    public static final String OPTION_DATASTORE_TIME_STORES_MILLISECS = "Datastore.Time.Millisecs";
    /** Whether the store will persist nanoseconds when persisting a "time" type. */
    public static final String OPTION_DATASTORE_TIME_STORES_NANOSECS = "Datastore.Time.Nanosecs";

    public static final String RELATION_IDENTITY_STORAGE_PERSISTABLE_IDENTITY = "PersistableIdentity";

    /**
     * Strings representing features that are supported by this datastore.
     * Refer to the Strings in org.datanucleus.store.StoreManager with prefix "OPTION_".
     * @return The supported options
     */
    Collection<String> getSupportedOptions();

    void close();

    boolean isClosed();

    MetaDataManager getMetaDataManager();

    StorePersistenceHandler getPersistenceHandler();

    FlushProcess getFlushProcess();

    NamingFactory getNamingFactory();

    QueryManager getQueryManager();

    StoreSchemaHandler getSchemaHandler();

    StoreData getStoreDataForClass(String className);

    /**
     * Method to return a datastore sequence for this datastore matching the passed sequence MetaData.
     * @param ec execution context
     * @param seqmd SequenceMetaData
     * @return The Sequence
     */
    default NucleusSequence getNucleusSequence(ExecutionContext ec, SequenceMetaData seqmd)
    {
        return new NucleusSequenceImpl(ec, this, seqmd);
    }

    /**
     * Method to return a connection to the user for the ExecutionContext.
     * Typically provides a wrapper to the currently in-use ManagedConnection.
     * @param ec execution context
     * @return The datastore Connection
     */
    NucleusConnection getNucleusConnection(ExecutionContext ec);

    /**
     * Accessor for the connection manager for this store manager.
     * @return connection manager
     */
    ConnectionManager getConnectionManager();

    /**
     * Convenience accessor for the URL for the connections.
     * @return connection URL
     */
    default String getConnectionURL()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_URL);
    }

    /**
     * Convenience accessor for the user name to use for the connections (where required).
     * @return user name
     */
    default String getConnectionUserName()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_USER_NAME);
    }

    /**
     * Convenience accessor for the password to use for the connections (where required).
     * @return Password
     */
    String getConnectionPassword();

    /**
     * Convenience accessor for the driver name to use for the connections (where supported).
     * @return driver name
     */
    default String getConnectionDriverName()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME);
    }

    /**
     * Convenience accessor for the primary connection factory (when a factory was provided by the user).
     * @return Connection Factory (primary)
     */
    default Object getConnectionFactory()
    {
        return getProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY);
    }

    /**
     * Convenience accessor for the factory (JNDI) name for the primary connection factory (when provided by the user).
     * @return Connection Factory name (primary)
     */
    default String getConnectionFactoryName()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME);
    }

    /**
     * Convenience accessor for the secondary connection factory (when a factory was provided by the user).
     * @return Connection Factory (secondary)
     */
    default Object getConnectionFactory2()
    {
        return getProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2);
    }

    /**
     * Convenience accessor for the factory (JNDI) name for the secondary connection factory (when provided by the user).
     * @return Connection Factory name (secondary)
     */
    default String getConnectionFactory2Name()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2_NAME);
    }

    /**
     * Accessor for the ValueGenerationManager for generating field values.
     * @return The ValueGenerationManager for this datastore
     */
    ValueGenerationManager getValueGenerationManager();

    /**
     * Accessor for whether this value generation strategy is supported.
     * @param strategy The value generation strategy
     * @return Whether it is supported.
     */
    boolean supportsValueGenerationStrategy(String strategy);

    /**
     * Convenience method to return whether the value generation strategy used by the specified class/member is generated in the datastore during a persist.
     * @param cmd Metadata for the class
     * @param absFieldNumber number of the field (or -1 if for datastore-id)
     * @return if the object for the value generation strategy is attributed by the database ("identity", etc)
     */
    boolean isValueGenerationStrategyDatastoreAttributed(AbstractClassMetaData cmd, int absFieldNumber);

    /**
     * Method to retrieve the value for a value generation strategy for a particular field.
     * @param ec execution context
     * @param cmd AbstractClassMetaData for the class
     * @param absoluteFieldNumber The field number
     * @return The generated value
     */
    Object getValueGenerationStrategyValue(ExecutionContext ec, AbstractClassMetaData cmd, int absoluteFieldNumber);

    /**
     * Method defining which value-strategy to use when the user specifies "native"/"auto".
     * @param cmd Class requiring the strategy
     * @param absFieldNumber Field of the class
     * @return The value generation strategy used when "native"/"auto" is specified
     */
    String getValueGenerationStrategyForNative(AbstractClassMetaData cmd, int absFieldNumber);

    /**
     * Accessor for the API adapter.
     * @return API adapter
     */
    ApiAdapter getApiAdapter();

    /**
     * Accessor for the key for this store manager.
     * @return StoreManager key
     */
    String getStoreManagerKey();

    /**
     * Accessor for the key used for representing this store manager in the query cache.
     * @return Key for the query cache
     */
    default String getQueryCacheKey()
    {
        // Fall back to the same as the key for the store manager
        return getStoreManagerKey();
    }

    /**
     * Accessor for the context in which this StoreManager is running.
     * @return Returns the context.
     */
    PersistenceNucleusContext getNucleusContext();

    /**
     * Get the date/time of the datastore.
     * @return Date/time of the datastore
     */
    default Date getDatastoreDate()
    {
        // Just return the current date here, and let any stores override this that support it
        return new Date();
    }

    /**
     * Returns whether the datastore is a "JDBC datastore". If it is then the JDO spec needs to 
     * return a connection that implements java.sql.Connection. RDBMS is the only one that will
     * return true from here (or certainly as we can foresee now).
     * @return Whether this is a JDBC datastore
     */
    boolean isJdbcStore();

    /**
     * Method to output particular information owned by this datastore.
     * Each StoreManager can support whichever categories it likes.
     * @param cat Category of information 
     * @param ps PrintStream
     * @throws Exception Thrown if an error occurs in the output process
     */
    void printInformation(String cat, PrintStream ps) throws Exception;

    /**
     * Method to return whether the specified member should use a backed SCO wrapper.
     * @param mmd Metadata for the member
     * @param ec ExecutionContext
     * @return Whether to use a back SCO wrapper (false means use a simple SCO wrapper).
     */
    boolean useBackedSCOWrapperForMember(AbstractMemberMetaData mmd, ExecutionContext ec);

    /**
     * Accessor for whether the specified class is managed currently
     * @param className The name of the class
     * @return Whether it is managed
     */
    boolean managesClass(String className);

    /**
     * Manage the specified classes.
     * This method is primarily useful for applications that wish to perform all
     * of their datastore initialization up front, rather than wait for the runtime to do it on-demand.
     * @param clr The ClassLoaderResolver
     * @param classNames The class(es) to be managed
     * @exception org.datanucleus.exceptions.DatastoreValidationException
     *      If there is some mismatch between the current datastore contents and
     *      those necessary to enable persistence of the given classes.
     */
    void manageClasses(ClassLoaderResolver clr, String... classNames);

    /**
     * Method to remove knowledge of the specified class from this StoreManager.
     * This can optionally also remove it from the datastore.
     * @param clr ClassLoader resolver
     * @param className Name of the class
     * @param removeFromDatastore Whether to also remove it from the datastore (otherwise just from the StoreManager)
     */
    void unmanageClass(ClassLoaderResolver clr, String className, boolean removeFromDatastore);

    /**
     * Remove all classes from the persistence model for the datastore.
     * This empties the datastore of all datastore objects managed by us.
     * All objects of types not managed are left untouched.
     * In the case of RDBMS this means drop all tables for types managed by us.
     * @param clr The ClassLoaderResolver
     */
    void unmanageAllClasses(ClassLoaderResolver clr);

    /**
     * Convenience method to ensure that the class defined by the passed DatastoreId/SingleFieldId is managed by the store.
     * @param id identity
     * @param clr ClassLoader resolver
     * @return The class name of the class associated to this identity
     * @throws NucleusUserException if the identity is assigned to the wrong class
     */
    String manageClassForIdentity(Object id, ClassLoaderResolver clr);

    /**
     * Interface to getting an Extent for a class.
     * @param ec execution context
     * @param c  The class requiring the Extent
     * @param subclasses Whether to include subclasses of 'c'
     * @return The Extent.
     * @param <T> Type of the extent
     */
    <T> Extent<T> getExtent(ExecutionContext ec, Class<T> c, boolean subclasses);

    /**
     * Accessor for the supported query languages.
     * @return The supported query languages
     */
    Collection<String> getSupportedQueryLanguages();

    /**
     * Accessor for whether this query language is supported.
     * @param language The language
     * @return Whether it is supported.
     */
    boolean supportsQueryLanguage(String language);

    /**
     * Method to return a new query, for the specified language and ExecutionContext.
     * @param language The query language
     * @param ec ExecutionContext
     * @return The query
     */
    Query newQuery(String language, ExecutionContext ec);

    /**
     * Method to return a new query, for the specified language and ExecutionContext, using the specified query string.
     * @param language The query language
     * @param ec ExecutionContext
     * @param queryString The query string
     * @return The query
     */
    Query newQuery(String language, ExecutionContext ec, String queryString);

    /**
     * Method to return a new query, for the specified language and ExecutionContext, using the specified existing query to copy from.
     * @param language The query language
     * @param ec ExecutionContext
     * @param q Existing query
     * @return The query
     */
    Query newQuery(String language, ExecutionContext ec, Query q);

    /**
     * Accessor for the native query language of this store.
     * @return The native query language (e.g "SQL")
     */
    default String getNativeQueryLanguage()
    {
        return null;
    }

    /**
     * Returns the class corresponding to the given object identity. 
     * If the object is datastore-identity, return the PC class specified in the identity.
     * If the object is single-field identity, return the PC class specified in the identity
     * If the object is an AppID PK, return the PC class that uses it.
     * If the object is a SCOID, return the SCO class. 
     * If the object is a persistable class, return the class. 
     * @param id The identity of some object.
     * @param clr ClassLoader resolver
     * @param ec execution context
     * @return For datastore identity, return the class of the corresponding
     * object. For application identity, return the class of the corresponding
     * object or null if object does not exist.
     * @exception ClassCastException If the type of ID is not recognized
     */
    String getClassNameForObjectID(Object id, ClassLoaderResolver clr, ExecutionContext ec);

    /**
     * Utility to return the names of the classes that are known subclasses of the provided
     * class. Actually uses the MetaDataManager for determining what is a subclass
     * since the MetaData is often registered before being needed by the Store. 
     * <b>The important difference is that this method will then register the subclass as required</b>
     * @param className Class for which we search for subclasses.
     * @param includeDescendents Whether to include subclasses of subclasses etc
     * @param clr The ClassLoaderResolver
     * @return Set of classes that are subclasses of the input class.
     */
    Collection<String> getSubClassesForClass(String className, boolean includeDescendents, ClassLoaderResolver clr);

    Object getProperty(String name);
    boolean hasProperty(String name);
    int getIntProperty(String name);
    boolean getBooleanProperty(String name);
    boolean getBooleanProperty(String name, boolean resultIfNotSet);
    Boolean getBooleanObjectProperty(String name);
    String getStringProperty(String name);

    /**
     * Convenience method to override the specified schema generation properties and enable schema generation.
     */
    void enableSchemaGeneration();

    /**
     * Convenience method to reset the schema generation properties to their initial creation values, effectively undoing a call to <i>enableSchemaGeneration</i>.
     */
    void resetSchemaGeneration();

    /**
     * Method to inform the StoreManager that a transaction has started for the specified execution context.
     * This allows the StoreManager to initialise any objects as required.
     * @param ec ExecutionContext
     */
    default void transactionStarted(ExecutionContext ec)
    {
    }

    /**
     * Method to inform the StoreManager that a transaction has committed for the specified execution context.
     * This allows the StoreManager to close any objects as required.
     * @param ec ExecutionContext
     */
    default void transactionCommitted(ExecutionContext ec)
    {
    }

    /**
     * Method to inform the StoreManager that a transaction has rolled back for the specified execution context.
     * This allows the StoreManager to close any objects as required.
     * @param ec ExecutionContext
     */
    default void transactionRolledBack(ExecutionContext ec)
    {
    }

    String getDefaultStateManagerClassName();

    /**
     * Whether this store manager uses backing-store based SCO wrappers.
     * @return Whether this store provides backing stores for SCO wrappers.
     */
    default boolean usesBackedSCOWrappers()
    {
        return false;
    }
}