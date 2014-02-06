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
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.NucleusContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.flush.FlushProcess;
import org.datanucleus.identity.OID;
import org.datanucleus.identity.SCOID;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.store.connection.ConnectionManager;
import org.datanucleus.store.connection.ManagedConnection;
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
    /**
     * Accessor for the supported options in string form
     */
    Collection getSupportedOptions();

    /**
     * Release of resources.
     */
    void close();

    /**
     * Accessor for the store persistence handler.
     * @return Store persistence handler.
     */
    StorePersistenceHandler getPersistenceHandler();

    /**
     * Accessor for the flush process to use with this datastore.
     * @return The flush process
     */
    FlushProcess getFlushProcess();

    /**
     * Accessor for the schema naming factory.
     * @return Naming factory.
     */
    NamingFactory getNamingFactory();

    /**
     * Accessor for the query manager for this datastore.
     * @return Query Manager for this store
     */
    QueryManager getQueryManager();

    /**
     * Accessor for the store schema handler (if this datastore supports the concept of a schema).
     * @return Store schema handler.
     */
    StoreSchemaHandler getSchemaHandler();

    /**
     * Method to return a datastore sequence for this datastore matching the passed sequence MetaData.
     * @param ec execution context
     * @param seqmd SequenceMetaData
     * @return The Sequence
     */
    NucleusSequence getNucleusSequence(ExecutionContext ec, SequenceMetaData seqmd);

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
     * Accessor for a connection for the specified ExecutionContext.<p>
     * If there is an active transaction, a connection from the transactional
     * connection factory will be returned. If there is no active transaction,
     * a connection from the nontransactional connection factory will be returned.
     * @param ec execution context
     * @return The Connection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    ManagedConnection getConnection(ExecutionContext ec);

    /**
     * Accessor for a connection for the specified ExecutionContext.<p>
     * If there is an active transaction, a connection from the transactional
     * connection factory will be returned. If there is no active transaction,
     * a connection from the nontransactional connection factory will be returned.
     * @param ec execution context
     * @param options connetion options
     * @return The Connection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    ManagedConnection getConnection(ExecutionContext ec, Map options);

    /**
     * Convenience accessor for the URL for the connection.
     * @return connection URL
     */
    String getConnectionURL();

    /**
     * Convenience accessor for the driver name to use for the connection (where supported).
     * @return driver name
     */
    String getConnectionDriverName();

    /**
     * Convenience accessor for the user name to use for the connection (where required).
     * @return user name
     */
    String getConnectionUserName();

    /**
     * Convenience accessor for the password to use for the connection (where required).
     * @return Password
     */
    String getConnectionPassword();

    /**
     * Convenience accessor for the primary connection factory.
     * @return Connection Factory (primary)
     */
    Object getConnectionFactory();

    /**
     * Convenience accessor for the factory name for the primary connection factory.
     * @return Connection Factory name (primary)
     */
    String getConnectionFactoryName();

    /**
     * Convenience accessor for the secondary connection factory.
     * @return Connection Factory (secondary)
     */
    Object getConnectionFactory2();

    /**
     * Convenience accessor for the factory name for the secondary connection factory.
     * @return Connection Factory name (secondary)
     */
    String getConnectionFactory2Name();

    /**
     * Accessor for the ValueGenerationManager for obtaining sequences.
     * @return The ValueGenerationManagerr for this datastore
     */
    ValueGenerationManager getValueGenerationManager();

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
    String getQueryCacheKey();

    /**
     * Accessor for the context in which this StoreManager is running
     * @return Returns the context.
     */
    NucleusContext getNucleusContext();

    /**
     * Get the date/time of the datastore.
     * @return Date/time of the datastore
     */
    Date getDatastoreDate();

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
     * @exception org.datanucleus.store.exceptions.DatastoreValidationException
     *      If there is some mismatch between the current datastore contents and
     *      those necessary to enable persistence of the given classes.
     */
    void manageClasses(ClassLoaderResolver clr, String... classNames);

    /**
     * Remove all classes from the persistence model for the datastore.
     * This empties the datastore of all datastore objects managed by us.
     * All objects of types not managed are left untouched.
     * In the case of RDBMS this means drop all tables for types managed by us.
     * @param clr The ClassLoaderResolver
     */
    void unmanageAllClasses(ClassLoaderResolver clr);

    /**
     * Convenience method to ensure that the class defined by the passed OID/SingleFIeldIdentity is 
     * managed by the store.
     * @param id OID
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
     */
    Extent getExtent(ExecutionContext ec, Class c, boolean subclasses);

    /**
     * Accessor for whether this query language is supported.
     * @param language The language
     * @return Whether it is supported.
     */
    boolean supportsQueryLanguage(String language);

    /**
     * Accessor for whether this value strategy is supported.
     * @param strategy The strategy
     * @return Whether it is supported.
     */
    boolean supportsValueStrategy(String strategy);

    /**
     * Returns the class corresponding to the given object identity. 
     * If the object is an OID (datastore-identity), return the PC class specified in the identity.
     * If the object is SingleFieldIdentity, return the PC class specified in the identity
     * If the object is an AppID PK, return the PC class that uses it.
     * If the object is a SCOID, return the SCO class. 
     * If the object is a persistable class, return the class. 
     * @param id The identity of some object.
     * @param clr ClassLoader resolver
     * @param ec execution context
     * @return For datastore identity, return the class of the corresponding
     * object. For application identity, return the class of the corresponding
     * object or null if object does not exist.
     * @exception ClassCastException If the type of ID is not recognized ({@link OID}
     * or {@link SCOID}).
     */
    String getClassNameForObjectID(Object id, ClassLoaderResolver clr, ExecutionContext ec);

    /**
     * Convenience method to return whether the strategy used by the specified class/member is
     * generated in the datastore during a persist.
     * @param cmd Metadata for the class
     * @param absFieldNumber number of the field (or -1 if for datastore-id)
     * @return if the object for the strategy is attributed by the database
     */
    boolean isStrategyDatastoreAttributed(AbstractClassMetaData cmd, int absFieldNumber);

    /**
     * Method to retrieve the value for a strategy for a particular field.
     * @param ec execution context
     * @param cmd AbstractClassMetaData for the class
     * @param absoluteFieldNumber The field number
     * @return The value
     */
    Object getStrategyValue(ExecutionContext ec, AbstractClassMetaData cmd, int absoluteFieldNumber);

    /**
     * Utility to return the names of the classes that are known subclasses of the provided
     * class. Actually uses the MetaDataManager for determining what is a subclass
     * since the MetaData is often registered before being needed by the Store.
     * @param className Class for which we search for subclasses.
     * @param includeDescendents Whether to include subclasses of subclasses etc
     * @param clr The ClassLoaderResolver
     * @return Set of classes that are subclasses of the input class.
     * TODO Use method in MetaDataManager and remove this
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
     * Method to inform the StoreManager that a transaction has started for the specified execution context.
     * This allows the StoreManager to initialise any objects as required.
     * @param ec ExecutionContext
     */
    void transactionStarted(ExecutionContext ec);

    /**
     * Method to inform the StoreManager that a transaction has committed for the specified execution context.
     * This allows the StoreManager to close any objects as required.
     * @param ec ExecutionContext
     */
    void transactionCommitted(ExecutionContext ec);

    /**
     * Method to inform the StoreManager that a transaction has rolled back for the specified execution context.
     * This allows the StoreManager to close any objects as required.
     * @param ec ExecutionContext
     */
    void transactionRolledBack(ExecutionContext ec);

    boolean isAutoCreateTables();
    boolean isAutoCreateConstraints();
    boolean isAutoCreateColumns();

    String getDefaultObjectProviderClassName();
}