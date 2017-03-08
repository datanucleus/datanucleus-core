/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved. 
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
2007 Xuan Baldauf - Contrib of notifyMainMemoryCopyIsInvalid(), findObject() (needed by DB4O plugin).
    ...
**********************************************************************/
package org.datanucleus.store;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.Transaction;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NoExtentException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.flush.FlushNonReferential;
import org.datanucleus.flush.FlushProcess;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.identity.SCOID;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.IdentityMetaData;
import org.datanucleus.metadata.IdentityStrategy;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.metadata.TableGeneratorMetaData;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.properties.PropertyStore;
import org.datanucleus.state.StateManagerImpl;
import org.datanucleus.store.autostart.AutoStartMechanism;
import org.datanucleus.store.connection.AbstractConnectionFactory;
import org.datanucleus.store.connection.ConnectionFactory;
import org.datanucleus.store.connection.ConnectionManager;
import org.datanucleus.store.connection.ConnectionManagerImpl;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.store.query.QueryManager;
import org.datanucleus.store.query.QueryManagerImpl;
import org.datanucleus.store.schema.DefaultStoreSchemaHandler;
import org.datanucleus.store.schema.StoreSchemaHandler;
import org.datanucleus.store.schema.naming.NamingCase;
import org.datanucleus.store.schema.naming.NamingFactory;
import org.datanucleus.store.valuegenerator.AbstractDatastoreGenerator;
import org.datanucleus.store.valuegenerator.ValueGenerationConnectionProvider;
import org.datanucleus.store.valuegenerator.ValueGenerationManager;
import org.datanucleus.store.valuegenerator.ValueGenerator;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;
import org.datanucleus.util.TypeConversionHelper;

/**
 * An abstract representation of a Store Manager.
 * Manages the persistence of objects to the store.
 * Will be implemented for the type of datastore (RDBMS, ODBMS, etc) in question. 
 * The store manager's responsibilities include:
 * <ul>
 * <li>Creating and/or validating datastore tables according to the persistent classes being 
 * accessed by the application.</li>
 * <li>Serving as the primary intermediary between ObjectProviders and the database.</li>
 * <li>Serving as the base Extent and Query factory.</li>
 * </ul>
 * <p>
 * A store manager's knowledge of its contents is typically not complete. It knows about
 * the classes that it has encountered in its lifetime. The PersistenceManager can make the
 * StoreManager aware of a class, and can check if the StoreManager knows about a particular class.
 */
public abstract class AbstractStoreManager extends PropertyStore implements StoreManager
{
    /** Key for this StoreManager e.g "rdbms", "neo4j" */
    protected final String storeManagerKey;

    /** Nucleus Context. */
    protected PersistenceNucleusContext nucleusContext;

    /** Manager for value generation. Lazy initialised, so use getValueGenerationManager() to access. */
    protected ValueGenerationManager valueGenerationMgr;

    /** Manager for the data definition in the datastore. */
    protected StoreDataManager storeDataMgr = new StoreDataManager();

    /** Persistence handler. */
    protected StorePersistenceHandler persistenceHandler = null;

    /** The flush process appropriate for this datastore. */
    protected FlushProcess flushProcess = null;

    /** Query Manager. Lazy initialised, so use getQueryManager() to access. */
    protected QueryManager queryMgr = null;

    /** Schema handler. */
    protected StoreSchemaHandler schemaHandler = null;

    /** Naming factory. */
    protected NamingFactory namingFactory = null;

    /** ConnectionManager **/
    protected ConnectionManager connectionMgr;

    /** Name of primary connection factory. */
    protected String primaryConnectionFactoryName;

    /** Name of secondary connection factory (null if not present). Typically for schema/sequences etc. */
    protected String secondaryConnectionFactoryName;

    /**
     * Constructor for a new StoreManager. Stores the basic information required for the datastore management.
     * @param key Key for this StoreManager
     * @param clr the ClassLoaderResolver
     * @param nucleusContext The corresponding nucleus context.
     * @param props Any properties controlling this datastore
     */
    protected AbstractStoreManager(String key, ClassLoaderResolver clr, PersistenceNucleusContext nucleusContext, Map<String, Object> props)
    {
        this.storeManagerKey = key;
        this.nucleusContext = nucleusContext;
        if (props != null)
        {
            // Store the properties for this datastore
            Iterator<Map.Entry<String, Object>> propIter = props.entrySet().iterator();
            while (propIter.hasNext())
            {
                Map.Entry<String, Object> entry = propIter.next();
                setPropertyInternal(entry.getKey(), entry.getValue());
            }
        }

        // Set up connection handling
        registerConnectionMgr();
        registerConnectionFactory();
        nucleusContext.addExecutionContextListener(new ExecutionContext.LifecycleListener()
        {
            public void preClose(ExecutionContext ec)
            {
                // Close all connections for this ExecutionContext whether tx or non-tx when ExecutionContext closes
                ConnectionFactory connFactory = connectionMgr.lookupConnectionFactory(primaryConnectionFactoryName);
                connectionMgr.closeAllConnections(connFactory, ec);
                connFactory = connectionMgr.lookupConnectionFactory(secondaryConnectionFactoryName);
                connectionMgr.closeAllConnections(connFactory, ec);
            }
        });
    }

    /**
     * Register the default ConnectionManager implementation
     */
    protected void registerConnectionMgr()
    {
        this.connectionMgr = new ConnectionManagerImpl(nucleusContext);
    }
    
    /**
     * Register the Connection Factory defined in plugins
     */
    protected void registerConnectionFactory()
    {
        String datastoreName = getStringProperty(FederatedStoreManager.PROPERTY_DATA_FEDERATION_DATASTORE_NAME);

        // Factory for connections - transactional
        ConfigurationElement cfElem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_connectionfactory",
            new String[] {"datastore", "transactional"}, new String[] {storeManagerKey, "true"});
        if (cfElem != null)
        {
            primaryConnectionFactoryName = cfElem.getAttribute("name");
            if (datastoreName != null)
            {
                primaryConnectionFactoryName += "-" + datastoreName;
            }
            try
            {
                ConnectionFactory cf = (ConnectionFactory)nucleusContext.getPluginManager().createExecutableExtension("org.datanucleus.store_connectionfactory",
                    new String[] {"datastore", "transactional"}, new String[] {storeManagerKey, "true"}, "class-name",
                    new Class[] {StoreManager.class, String.class}, new Object[] {this, AbstractConnectionFactory.RESOURCE_NAME_TX});
                connectionMgr.registerConnectionFactory(primaryConnectionFactoryName, cf);
                if (NucleusLogger.CONNECTION.isDebugEnabled())
                {
                    NucleusLogger.CONNECTION.debug(Localiser.msg("032018", primaryConnectionFactoryName));
                }
            }
            catch (Exception e)
            {
                throw new NucleusException("Error creating transactional connection factory", e).setFatal();
            }
        }
        else
        {
            throw new NucleusException("Error creating transactional connection factory. No connection factory plugin defined");
        }

        // Factory for connections - typically for schema/sequences etc
        cfElem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_connectionfactory",
            new String[] {"datastore", "transactional"}, new String[] {storeManagerKey, "false"});
        if (cfElem != null)
        {
            secondaryConnectionFactoryName = cfElem.getAttribute("name");
            if (datastoreName != null)
            {
                secondaryConnectionFactoryName += "-" + datastoreName;
            }
            try
            {
                ConnectionFactory cf = (ConnectionFactory)nucleusContext.getPluginManager().createExecutableExtension("org.datanucleus.store_connectionfactory",
                    new String[] {"datastore", "transactional"}, new String[] {storeManagerKey, "false"}, "class-name",
                    new Class[] {StoreManager.class, String.class}, new Object[] {this, AbstractConnectionFactory.RESOURCE_NAME_NONTX});
                if (NucleusLogger.CONNECTION.isDebugEnabled())
                {
                    NucleusLogger.CONNECTION.debug(Localiser.msg("032019", secondaryConnectionFactoryName));
                }
                connectionMgr.registerConnectionFactory(secondaryConnectionFactoryName, cf);
            }
            catch (Exception e)
            {
                throw new NucleusException("Error creating nontransactional connection factory", e).setFatal();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#close()
     */
    public synchronized void close()
    {
        if (primaryConnectionFactoryName != null)
        {
            ConnectionFactory cf = connectionMgr.lookupConnectionFactory(primaryConnectionFactoryName);
            if (cf != null)
            {
                cf.close();
            }
        }
        if (secondaryConnectionFactoryName != null)
        {
            ConnectionFactory cf = connectionMgr.lookupConnectionFactory(secondaryConnectionFactoryName);
            if (cf != null)
            {
                cf.close();
            }
        }
        connectionMgr = null;

        if (valueGenerationMgr != null)
        {
            valueGenerationMgr.clear();
            valueGenerationMgr = null;
        }

        storeDataMgr.clear();
        storeDataMgr = null;

        if (persistenceHandler != null)
        {
            persistenceHandler.close();
            persistenceHandler = null;
        }

        if (schemaHandler != null)
        {
            schemaHandler = null;
        }

        if (queryMgr != null)
        {
            queryMgr.close();
            queryMgr = null;
        }
        nucleusContext = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionManager()
     */
    public ConnectionManager getConnectionManager()
    {
        return connectionMgr;
    }

    /**
     * Accessor for a connection for the specified ExecutionContext.
     * If there is an active transaction, a connection from the primary DataSource will be returned. 
     * If there is no active transaction, a connection from the secondary DataSource will be returned.
     * @param ec ExecutionContext
     * @return The Connection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    public ManagedConnection getConnection(ExecutionContext ec)
    {
        return getConnection(ec, null);
    }

    /**
     * Accessor for a connection for the specified ExecutionContext.
     * If there is an active transaction, a connection from the primary DataSource will be returned. 
     * If there is no active transaction, a connection from the secondary DataSource will be returned.
     * @param ec ExecutionContext
     * @param options Any options for the connection
     * @return The Connection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    public ManagedConnection getConnection(ExecutionContext ec, Map options)
    {
        ConnectionFactory connFactory;
        if (ec.getTransaction().isActive())
        {
            connFactory = connectionMgr.lookupConnectionFactory(primaryConnectionFactoryName);
        }
        else
        {
            Boolean singleConnection = getBooleanProperty(PropertyNames.PROPERTY_CONNECTION_SINGLE_CONNECTION);
            if (singleConnection)
            {
                // Take from the primary
                connFactory = connectionMgr.lookupConnectionFactory(primaryConnectionFactoryName);
            }
            else if (secondaryConnectionFactoryName != null)
            {
                connFactory = connectionMgr.lookupConnectionFactory(secondaryConnectionFactoryName);
            }
            else
            {
                // Some datastores don't define secondary handling so just fallback to the primary factory
                connFactory = connectionMgr.lookupConnectionFactory(primaryConnectionFactoryName);
            }
        }
        return connFactory.getConnection(ec, ec.getTransaction(), options);
    }

    /**
     * Utility to return a managed connection not tied to any ExecutionContext. This would typically be used for schema operations or
     * for accessing sequences, things that we normally want to separate from any PM/EM persistence operations.
     * This method returns a connection from the secondary connection factory (e.g. datanucleus.connectionFactory2Name), if it is provided.
     * @param isolation_level The transaction isolation scheme to use See org.datanucleus.transaction.TransactionIsolation 
     *     Pass in -1 if just want the default
     * @return The Connection to the datastore
     * @throws NucleusException if an error occurs getting the connection
     */
    public ManagedConnection getConnection(int isolation_level)
    {
        ConnectionFactory connFactory = null;
        if (secondaryConnectionFactoryName != null)
        {
            connFactory = connectionMgr.lookupConnectionFactory(secondaryConnectionFactoryName);
        }
        else
        {
            // Some datastores don't define non-tx handling so just fallback to the primary factory
            connFactory = connectionMgr.lookupConnectionFactory(primaryConnectionFactoryName);
        }

        Map options = null;
        if (isolation_level >= 0)
        {
            options = new HashMap();
            options.put(Transaction.TRANSACTION_ISOLATION_OPTION, Integer.valueOf(isolation_level));
        }
        return connFactory.getConnection(null, null, options);
    }

    /**
     * Convenience accessor for the driver name to use for the connection (if applicable for this datastore).
     * @return Driver name for the connection
     */
    public String getConnectionDriverName()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME);
    }

    /**
     * Convenience accessor for the URL for the connection
     * @return Connection URL
     */
    public String getConnectionURL()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_URL);
    }

    /**
     * Convenience accessor for the username to use for the connection
     * @return Username
     */
    public String getConnectionUserName()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_USER_NAME);
    }

    /**
     * Convenience accessor for the password to use for the connection.
     * Will perform decryption if the persistence property "datanucleus.ConnectionPasswordDecrypter" has
     * also been specified.
     * @return Password
     */
    public String getConnectionPassword()
    {
        String password = getStringProperty(PropertyNames.PROPERTY_CONNECTION_PASSWORD);
        if (password != null)
        {
            String decrypterName = getStringProperty(PropertyNames.PROPERTY_CONNECTION_PASSWORD_DECRYPTER);
            if (decrypterName != null)
            {
                // Decrypt the password using the provided class
                ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(null);
                try
                {
                    Class decrypterCls = clr.classForName(decrypterName);
                    ConnectionEncryptionProvider decrypter = (ConnectionEncryptionProvider) decrypterCls.newInstance();
                    password = decrypter.decrypt(password);
                }
                catch (Exception e)
                {
                    NucleusLogger.DATASTORE.warn("Error invoking decrypter class " + decrypterName, e);
                }
            }
        }
        return password;
    }

    /**
     * Convenience accessor for the factory for the connection (transactional).
     * @return Connection Factory (transactional)
     */
    public Object getConnectionFactory()
    {
        return getProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY);
    }

    /**
     * Convenience accessor for the factory name for the connection (transactional).
     * @return Connection Factory name (transactional)
     */
    public String getConnectionFactoryName()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME);
    }

    /**
     * Convenience accessor for the factory for the connection (non-transactional).
     * @return Connection Factory (non-transactional)
     */
    public Object getConnectionFactory2()
    {
        return getProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2);
    }

    /**
     * Convenience accessor for the factory name for the connection (non-transactional).
     * @return Connection Factory name (non-transactional)
     */
    public String getConnectionFactory2Name()
    {
        return getStringProperty(PropertyNames.PROPERTY_CONNECTION_FACTORY2_NAME);
    }

	/* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#isJdbcStore()
     */
    public boolean isJdbcStore()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getPersistenceHandler()
     */
    public StorePersistenceHandler getPersistenceHandler()
    {
        return persistenceHandler;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getFlushProcess()
     */
    public FlushProcess getFlushProcess()
    {
        if (flushProcess == null)
        {
            // Default to non-referential flush
            flushProcess = new FlushNonReferential();
        }
        return flushProcess;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getQueryManager()
     */
    public QueryManager getQueryManager()
    {
        if (queryMgr == null)
        {
            // Initialise support for queries
            queryMgr = new QueryManagerImpl(nucleusContext, this);
        }
        return queryMgr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getMetaDataHandler()
     */
    public StoreSchemaHandler getSchemaHandler()
    {
        if (schemaHandler == null)
        {
            schemaHandler = new DefaultStoreSchemaHandler(this);
        }
        return schemaHandler;
    }

    public NamingFactory getNamingFactory()
    {
        if (namingFactory == null)
        {
            // Create the NamingFactory
            String namingFactoryName = getStringProperty(PropertyNames.PROPERTY_IDENTIFIER_NAMING_FACTORY);
            String namingFactoryClassName = nucleusContext.getPluginManager().getAttributeValueForExtension("org.datanucleus.identifier_namingfactory", 
                "name", namingFactoryName, "class-name");
            if (namingFactoryClassName == null)
            {
                // TODO Localise this
                throw new NucleusUserException("Error in specified NamingFactory " + namingFactoryName + " not found");
            }
            try
            {
                Class[] argTypes = new Class[] {ClassConstants.NUCLEUS_CONTEXT};
                Object[] args = new Object[] {nucleusContext};
                namingFactory = (NamingFactory)nucleusContext.getPluginManager().createExecutableExtension("org.datanucleus.identifier_namingfactory", 
                    "name", namingFactoryName, "class-name", argTypes, args);
            }
            catch (Throwable thr)
            {
                NucleusLogger.GENERAL.debug(">> Exception creating NamingFactory", thr);
            }

            // Set the case TODO Handle quoted cases (not specifiable via this property currently)
            String identifierCase = getStringProperty(PropertyNames.PROPERTY_IDENTIFIER_CASE);
            if (identifierCase != null)
            {
                if (identifierCase.equalsIgnoreCase("lowercase"))
                {
                    namingFactory.setNamingCase(NamingCase.LOWER_CASE);
                }
                else if (identifierCase.equalsIgnoreCase("UPPERCASE"))
                {
                    namingFactory.setNamingCase(NamingCase.UPPER_CASE);
                }
                else
                {
                    namingFactory.setNamingCase(NamingCase.MIXED_CASE);
                }
            }
        }

        return namingFactory;
    }

    /**
     * Method to return a datastore sequence for this datastore matching the passed sequence MetaData.
     * @param ec execution context
     * @param seqmd SequenceMetaData
     * @return The Sequence
     */
    public NucleusSequence getNucleusSequence(ExecutionContext ec, SequenceMetaData seqmd)
    {
        return new NucleusSequenceImpl(ec, this, seqmd);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getNucleusConnection(org.datanucleus.ExecutionContext)
     */
    public NucleusConnection getNucleusConnection(ExecutionContext ec)
    {
        ConnectionFactory cf = connectionMgr.lookupConnectionFactory(primaryConnectionFactoryName);

        final ManagedConnection mc;
        final boolean enlisted;
        if (!ec.getTransaction().isActive())
        {
            // no active transaction so don't enlist
            enlisted = false;
        }
        else
        {
            enlisted = true;
        }
        mc = cf.getConnection(enlisted ? ec : null, enlisted ? ec.getTransaction() : null, null); // Will throw exception if already locked

        // Lock the connection now that it is in use by the user
        mc.lock();

        Runnable closeRunnable = new Runnable()
        {
            public void run()
            {
                // Unlock the connection now that the user has finished with it
                mc.unlock();
                if (!enlisted)
                {
                    // Close the (unenlisted) connection (committing its statements)
                    mc.close();
                }
            }
        };
        return new NucleusConnectionImpl(mc.getConnection(), closeRunnable);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getValueGenerationManager()
     */
    public ValueGenerationManager getValueGenerationManager()
    {
        if (valueGenerationMgr == null)
        {
            this.valueGenerationMgr = new ValueGenerationManager();
        }
        return valueGenerationMgr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getApiAdapter()
     */
    public ApiAdapter getApiAdapter()
    {
        return nucleusContext.getApiAdapter();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getStoreManagerKey()
     */
    public String getStoreManagerKey()
    {
        return storeManagerKey;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getQueryCacheKey()
     */
    public String getQueryCacheKey()
    {
        // Fall back to the same as the key for the store manager
        return getStoreManagerKey();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getNucleusContext()
     */   
    public PersistenceNucleusContext getNucleusContext()
    {
        return nucleusContext;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getMetaDataManager()
     */   
    public MetaDataManager getMetaDataManager()
    {
        return nucleusContext.getMetaDataManager();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getDatastoreDate()
     */
    public Date getDatastoreDate()
    {
        // Just return the current date here, and let any stores override this that support it
        return new Date();
    }

    // -------------------------------- Management of Classes --------------------------------

    public StoreData getStoreDataForClass(String className)
    {
        return storeDataMgr.get(className);
    }

    /**
     * Method to register some data with the store.
     * This will also register the data with the starter process.
     * @param data The StoreData to add
     */
    protected void registerStoreData(StoreData data)
    {
        storeDataMgr.registerStoreData(data);

        // Keep the AutoStarter in step with our managed classes/fields
        if (nucleusContext.getAutoStartMechanism() != null)
        {
            nucleusContext.getAutoStartMechanism().addClass(data);
        }
    }

    /**
     * Method to deregister all existing store data so that we are managing nothing.
     */
    protected void deregisterAllStoreData()
    {
        storeDataMgr.clear();

        // Keep the AutoStarter in step with our managed classes/fields
        AutoStartMechanism starter = nucleusContext.getAutoStartMechanism();
        if (starter != null)
        {
            try
            {
                if (!starter.isOpen())
                {
                    starter.open();
                }
                starter.deleteAllClasses();
            }
            finally
            {
                if (starter.isOpen())
                {
                    starter.close();
                }
            }
        }
    }

    /**
     * Convenience method to log the configuration of this store manager.
     */
    protected void logConfiguration()
    {
        if (NucleusLogger.DATASTORE.isDebugEnabled())
        {
            NucleusLogger.DATASTORE.debug("======================= Datastore =========================");
            NucleusLogger.DATASTORE.debug("StoreManager : \"" + storeManagerKey + "\" (" + getClass().getName() + ")");

            NucleusLogger.DATASTORE.debug("Datastore : " +
                (getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY) ? "read-only" : "read-write") + 
                (getBooleanProperty(PropertyNames.PROPERTY_SERIALIZE_READ) ? ", useLocking" : ""));

            // Schema : Auto-Create/Validate
            StringBuilder autoCreateOptions = null;
            if (getSchemaHandler().isAutoCreateTables() || getSchemaHandler().isAutoCreateColumns() || getSchemaHandler().isAutoCreateConstraints())
            {
                autoCreateOptions = new StringBuilder();
                boolean first = true;
                if (getSchemaHandler().isAutoCreateTables())
                {
                    if (!first)
                    {
                        autoCreateOptions.append(",");
                    }
                    autoCreateOptions.append("Tables");
                    first = false;
                }
                if (getSchemaHandler().isAutoCreateColumns())
                {
                    if (!first)
                    {
                        autoCreateOptions.append(",");
                    }
                    autoCreateOptions.append("Columns");
                    first = false;
                }
                if (getSchemaHandler().isAutoCreateConstraints())
                {
                    if (!first)
                    {
                        autoCreateOptions.append(",");
                    }
                    autoCreateOptions.append("Constraints");
                    first = false;
                }
            }
            StringBuilder validateOptions = null;
            if (getSchemaHandler().isValidateTables() || getSchemaHandler().isValidateColumns() || getSchemaHandler().isValidateConstraints())
            {
                validateOptions = new StringBuilder();
                boolean first = true;
                if (getSchemaHandler().isValidateTables())
                {
                    validateOptions.append("Tables");
                    first = false;
                }
                if (getSchemaHandler().isValidateColumns())
                {
                    if (!first)
                    {
                        validateOptions.append(",");
                    }
                    validateOptions.append("Columns");
                    first = false;
                }
                if (getSchemaHandler().isValidateConstraints())
                {
                    if (!first)
                    {
                        validateOptions.append(",");
                    }
                    validateOptions.append("Constraints");
                    first = false;
                }
            }
            NucleusLogger.DATASTORE.debug("Schema Control : " +
                "AutoCreate(" + (autoCreateOptions != null ? autoCreateOptions.toString() : "None") + ")" +
                ", Validate(" + (validateOptions != null ? validateOptions.toString() : "None") + ")");

            String namingFactoryName = getStringProperty(PropertyNames.PROPERTY_IDENTIFIER_NAMING_FACTORY);
            String namingCase = getStringProperty(PropertyNames.PROPERTY_IDENTIFIER_CASE);
            NucleusLogger.DATASTORE.debug("Schema : NamingFactory=" + namingFactoryName + " identifierCase=" + namingCase);

            String[] queryLanguages = nucleusContext.getPluginManager().getAttributeValuesForExtension("org.datanucleus.store_query_query", "datastore", storeManagerKey, "name");
            NucleusLogger.DATASTORE.debug("Query Languages : " + (queryLanguages != null ? StringUtils.objectArrayToString(queryLanguages) : "none"));
            NucleusLogger.DATASTORE.debug("Queries : Timeout=" + getIntProperty(PropertyNames.PROPERTY_DATASTORE_READ_TIMEOUT));

            NucleusLogger.DATASTORE.debug("===========================================================");
        }
    }

    /**
     * Method to output the information about the StoreManager.
     * Supports the category "DATASTORE".
     */
    public void printInformation(String category, PrintStream ps)
    throws Exception
    {
        if (category.equalsIgnoreCase("DATASTORE"))
        {
            ps.println(Localiser.msg("032020", storeManagerKey, getConnectionURL(), getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY) ? "read-only" : "read-write"));
        }
    }

    // ------------------------------- Class Management -----------------------------------

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#managesClass(java.lang.String)
     */
    public boolean managesClass(String className)
    {
        return storeDataMgr.managesClass(className);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#addClasses(org.datanucleus.ClassLoaderResolver, java.lang.String[])
     */
    public void manageClasses(ClassLoaderResolver clr, String... classNames)
    {
        if (classNames == null)
        {
            return;
        }

        // Filter out any "simple" type classes
        String[] filteredClassNames = getNucleusContext().getTypeManager().filterOutSupportedSecondClassNames(classNames);

        // Find the ClassMetaData for these classes and all referenced by these classes
        Iterator<AbstractClassMetaData> iter = getMetaDataManager().getReferencedClasses(filteredClassNames, clr).iterator();
        while (iter.hasNext())
        {
            ClassMetaData cmd = (ClassMetaData)iter.next();
            if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                if (!storeDataMgr.managesClass(cmd.getFullClassName()))
                {
                    registerStoreData(newStoreData(cmd, clr));
                }
            }
        }
    }

    /**
     * Instantiate a StoreData instance using the provided ClassMetaData and ClassLoaderResolver. 
     * Override this method if you want to instantiate a subclass of StoreData.
     * @param cmd MetaData for the class
     * @param clr ClassLoader resolver
     * @return The StoreData
     */
    protected StoreData newStoreData(ClassMetaData cmd, ClassLoaderResolver clr) 
    {
        return new StoreData(cmd.getFullClassName(), cmd, StoreData.Type.FCO, null);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#removeAllClasses(org.datanucleus.ClassLoaderResolver)
     */
    public void unmanageAllClasses(ClassLoaderResolver clr)
    {
        // Do nothing.
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#unmanageClass(org.datanucleus.ClassLoaderResolver, java.lang.String, boolean)
     */
    @Override
    public void unmanageClass(ClassLoaderResolver clr, String className, boolean removeFromDatastore)
    {
        AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(className, clr);
        if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
        {
            if (removeFromDatastore)
            {
                // TODO Handle this
            }

            // Remove our knowledge of this class
            // TODO Remove any fields that are registered in their own right (join tables)
            storeDataMgr.deregisterClass(className);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#manageClassForIdentity(java.lang.Object, org.datanucleus.ClassLoaderResolver)
     */
    public String manageClassForIdentity(Object id, ClassLoaderResolver clr)
    {
        String className = null;
        if (IdentityUtils.isDatastoreIdentity(id))
        {
            // Check that the implied class is managed
            className = IdentityUtils.getTargetClassNameForIdentitySimple(id);
            AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(className, clr);
            if (cmd.getIdentityType() != IdentityType.DATASTORE)
            {
                throw new NucleusUserException(Localiser.msg("038001", id, cmd.getFullClassName()));
            }
        }
        else if (IdentityUtils.isSingleFieldIdentity(id))
        {
            className = IdentityUtils.getTargetClassNameForIdentitySimple(id);
            AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(className, clr);
            if (cmd.getIdentityType() != IdentityType.APPLICATION || !cmd.getObjectidClass().equals(id.getClass().getName()))
            {
                throw new NucleusUserException(Localiser.msg("038001", id, cmd.getFullClassName()));
            }
        }
        else
        {
            throw new NucleusException("StoreManager.manageClassForIdentity called for id=" + id + 
                " yet should only be called for datastore-identity/SingleFieldIdentity");
        }

        // If the class is not yet managed, manage it
        if (!managesClass(className))
        {
            manageClasses(clr, className);
        }

        return className;
    }

    // ------------------------------ PersistenceManager interface -----------------------------------

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getExtent(org.datanucleus.ExecutionContext, java.lang.Class, boolean)
     */
    public <T> Extent<T> getExtent(ExecutionContext ec, Class<T> c, boolean subclasses)
    {
        AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(c, ec.getClassLoaderResolver());
        if (!cmd.isRequiresExtent())
        {
            throw new NoExtentException(c.getName());
        }

        // Create Extent using JDOQL query
        if (!managesClass(c.getName()))
        {
            manageClasses(ec.getClassLoaderResolver(), c.getName());
        }
        return new DefaultCandidateExtent(ec, c, subclasses, cmd);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#supportsQueryLanguage(java.lang.String)
     */
    public boolean supportsQueryLanguage(String language)
    {
        if (language == null)
        {
            return false;
        }

        // Find if datastore=storeManagerKey has an extension for name="{language}"
        String name = getNucleusContext().getPluginManager().getAttributeValueForExtension("org.datanucleus.store_query_query",
            new String[] {"name", "datastore"}, new String[]{language, storeManagerKey}, "name");
        return name != null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getNativeQueryLanguage()
     */
    @Override
    public String getNativeQueryLanguage()
    {
        return null;
    }

    /**
     * Accessor for whether this value strategy is supported.
     * @param strategy The strategy
     * @return Whether it is supported.
     */
    public boolean supportsValueStrategy(String strategy)
    {
        ConfigurationElement elem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_valuegenerator",
            new String[]{"name", "unique"}, new String[] {strategy, "true"});
        if (elem != null)
        {
            // Unique strategy so supported for all datastores
            return true;
        }

        elem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_valuegenerator",
            new String[]{"name", "datastore"}, new String[] {strategy, storeManagerKey});
        if (elem != null)
        {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getClassNameForObjectID(java.lang.Object, org.datanucleus.ClassLoaderResolver, org.datanucleus.ExecutionContext)
     */
    public String getClassNameForObjectID(Object id, ClassLoaderResolver clr, ExecutionContext ec)
    {
        if (id == null)
        {
            // User stupidity
            return null;
        }
        else if (id instanceof SCOID)
        {
            // Object is a SCOID
            return ((SCOID) id).getSCOClass();
        }
        else if (IdentityUtils.isDatastoreIdentity(id) || IdentityUtils.isSingleFieldIdentity(id))
        {
            // Object is an OID or single-field id
            return IdentityUtils.getTargetClassNameForIdentitySimple(id);
        }
        else
        {
            // Application identity with user PK class, so find all using this PK
            Collection<AbstractClassMetaData> cmds = getMetaDataManager().getClassMetaDataWithApplicationId(id.getClass().getName());
            if (cmds != null)
            {
                Iterator<AbstractClassMetaData> iter = cmds.iterator();
                while (iter.hasNext())
                {
                    // Just return the class name of the first one using this id - could be any in this tree
                    AbstractClassMetaData cmd = iter.next();
                    return cmd.getFullClassName();
                }
            }
            return null;
        }
    }

    /**
     * Convenience method to return whether the strategy used by the specified class/member is generated
     * by the datastore (during a persist).
     * @param cmd Metadata for the class
     * @param absFieldNumber Absolute field number for the field (or -1 if datastore id)
     * @return Whether the strategy is generated in the datastore
     */
    public boolean isStrategyDatastoreAttributed(AbstractClassMetaData cmd, int absFieldNumber)
    {
        if (absFieldNumber < 0)
        {
            if (cmd.isEmbeddedOnly())
            {
                return false;
            }

            // Datastore-id
            IdentityMetaData idmd = cmd.getBaseIdentityMetaData();
            if (idmd == null)
            {
                // native
                String strategy = getStrategyForNative(cmd, absFieldNumber);
                if (strategy.equalsIgnoreCase("identity"))
                {
                    return true;
                }
            }
            else
            {
                IdentityStrategy idStrategy = idmd.getValueStrategy();
                if (idStrategy == IdentityStrategy.IDENTITY)
                {
                    return true;
                }
                else if (idStrategy == IdentityStrategy.NATIVE)
                {
                    String strategy = getStrategyForNative(cmd, absFieldNumber);
                    if (strategy.equalsIgnoreCase("identity"))
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
            // Value generation for a member
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absFieldNumber);
            if (mmd.getValueStrategy() == null)
            {
                return false;
            }
            else if (mmd.getValueStrategy() == IdentityStrategy.IDENTITY)
            {
                return true;
            }
            else if (mmd.getValueStrategy() == IdentityStrategy.NATIVE)
            {
                String strategy = getStrategyForNative(cmd, absFieldNumber);
                if (strategy.equalsIgnoreCase("identity"))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getStrategyValue(org.datanucleus.ExecutionContext, org.datanucleus.metadata.AbstractClassMetaData, int)
     */
    public Object getStrategyValue(ExecutionContext ec, AbstractClassMetaData cmd, int absoluteFieldNumber)
    {
        // Extract the control information for this field that needs its value
        AbstractMemberMetaData mmd = null;
        String fieldName = null;
        IdentityStrategy strategy = null;
        String sequence = null;
        String valueGeneratorName = null;
        TableGeneratorMetaData tableGeneratorMetaData = null;
        SequenceMetaData sequenceMetaData = null;
        if (absoluteFieldNumber >= 0)
        {
            // real field
            mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absoluteFieldNumber);
            fieldName = mmd.getFullFieldName();
            strategy = mmd.getValueStrategy();
            sequence = mmd.getSequence();
            valueGeneratorName = mmd.getValueGeneratorName();
        }
        else
        {
            // datastore-identity surrogate field
            fieldName = cmd.getFullClassName() + " (datastore id)";
            strategy = cmd.getIdentityMetaData().getValueStrategy();
            sequence = cmd.getIdentityMetaData().getSequence();
            valueGeneratorName = cmd.getIdentityMetaData().getValueGeneratorName();
        }

        String strategyName = strategy.toString();
        if (strategy.equals(IdentityStrategy.CUSTOM))
        {
            // Using a "custom" generator
            strategyName = strategy.getCustomName();
        }
        else if (strategy.equals(IdentityStrategy.NATIVE))
        {
            strategyName = getStrategyForNative(cmd, absoluteFieldNumber);
            strategy = IdentityStrategy.getIdentityStrategy(strategyName);
        }

        // Extract any metadata-based generation information
        if (valueGeneratorName != null)
        {
            if (strategy == IdentityStrategy.INCREMENT)
            {
                tableGeneratorMetaData = getMetaDataManager().getMetaDataForTableGenerator(ec.getClassLoaderResolver(), valueGeneratorName);
                if (tableGeneratorMetaData == null)
                {
                    throw new NucleusUserException(Localiser.msg("038005", fieldName, valueGeneratorName));
                }
            }
            else if (strategy == IdentityStrategy.SEQUENCE)
            {
                sequenceMetaData = getMetaDataManager().getMetaDataForSequence(ec.getClassLoaderResolver(), valueGeneratorName);
                if (sequenceMetaData == null)
                {
                    throw new NucleusUserException(Localiser.msg("038006", fieldName, valueGeneratorName));
                }
            }
        }
        else if (strategy == IdentityStrategy.SEQUENCE && sequence != null)
        {
            // TODO Allow for package name of this class prefix for the sequence name
            sequenceMetaData = getMetaDataManager().getMetaDataForSequence(ec.getClassLoaderResolver(), sequence);
            if (sequenceMetaData == null)
            {
                // JPOX 1.1 behaviour was just to use the sequence name in the datastore so log it and fallback to that
                NucleusLogger.VALUEGENERATION.warn("Field " + fieldName + " has been specified to use sequence " + sequence +
                    " but there is no <sequence> specified in the MetaData. Falling back to use a sequence in the datastore with this name directly.");
            }
        }

        // Value Generators are cached against a name. Some Value Generators are unique per datastore.
        // Others are per class/field. Others have a user-defined name.
        // The name that they are cached under relates to what they use.
        // Generate the name for ValueGenerationManager to use the strategy for this field/class.
        String generatorName = null;
        String generatorNameKeyInManager = null;
        ConfigurationElement elem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_valuegenerator",
            new String[]{"name", "unique"}, new String[] {strategyName, "true"});
        if (elem != null)
        {
            // Unique value generator so set the key in ValueGenerationManager to the value generator name itself
            generatorName = elem.getAttribute("name");
            generatorNameKeyInManager = generatorName;
        }
        else
        {
            // Not a unique (datastore-independent) generator so try just for this datastore
            elem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_valuegenerator", 
                new String[]{"name", "datastore"}, new String[] {strategyName, storeManagerKey});
            if (elem != null)
            {
                // Set the generator name (for use by the ValueGenerationManager)
                generatorName = elem.getAttribute("name");
            }
        }
        if (generatorNameKeyInManager == null)
        {
            // Value generator is not unique so set based on class/field
            if (absoluteFieldNumber >= 0)
            {
                // Require generation for a field so use field name for the generator symbolic name
                generatorNameKeyInManager = mmd.getFullFieldName();
            }
            else
            {
                // Require generation for a datastore id field so use the base class name for generator symbolic name
                generatorNameKeyInManager = cmd.getBaseAbstractClassMetaData().getFullClassName();
            }
        }

        // Try to find the generator from the ValueGenerationManager if we already have it managed
        ValueGenerator generator = null;
        synchronized (this)
        {
            // This block is synchronised since we don't want to let any other thread through until
            // the generator is created, in case it is the same as the next request
            generator = getValueGenerationManager().getValueGenerator(generatorNameKeyInManager);
            if (generator == null)
            {
                if (generatorName == null)
                {
                    // No available value-generator for the specified strategy for this datastore
                    throw new NucleusUserException(Localiser.msg("038004", strategy));
                }

                // Set up the default properties available for all value generators
                Properties props = getPropertiesForGenerator(cmd, absoluteFieldNumber, ec, sequenceMetaData,
                    tableGeneratorMetaData);

                Class cls = null;
                if (elem != null)
                {
                    cls = nucleusContext.getPluginManager().loadClass(elem.getExtension().getPlugin().getSymbolicName(), elem.getAttribute("class-name"));
                }
                if (cls == null)
                {
                    throw new NucleusException("Cannot create Value Generator for strategy " + generatorName);
                }

                // Create the new ValueGenerator since the first time required (registers it with the ValueGenerationManager too)
                generator = getValueGenerationManager().createValueGenerator(generatorNameKeyInManager, cls, props, this, null);
            }
        }

        // Get the next value from the generator
        Object oid = getStrategyValueForGenerator(generator, ec);

        if (mmd != null)
        {
            // replace the value of the id, but before convert the value to the field type if needed
            try
            {
                Object convertedValue = TypeConversionHelper.convertTo(oid, mmd.getType());
                if (convertedValue == null)
                {
                    throw new NucleusException(Localiser.msg("038003", mmd.getFullFieldName(), oid)).setFatal();
                }
                oid = convertedValue;
            }
            catch (NumberFormatException nfe)
            {
                throw new NucleusUserException("Value strategy created value="+oid+" type="+oid.getClass().getName() +
                        " but field is of type "+mmd.getTypeName() + ". Use a different strategy or change the type of the field " + mmd.getFullFieldName());
            }
        }

        if (NucleusLogger.VALUEGENERATION.isDebugEnabled())
        {
            NucleusLogger.VALUEGENERATION.debug(Localiser.msg("038002", fieldName, strategy, generator.getClass().getName(), oid));
        }

        return oid;
    }

    /**
     * Method defining which value-strategy to use when the user specifies "native". This will return as follows
     * <ul>
     * <li>If your field is Numeric-based (or datastore-id with numeric or no jdbc-type) then chooses the
     * first one that is supported of "identity", "sequence", "increment", otherwise exception.</li>
     * <li>Otherwise your field is String-based then chooses "uuid-hex".</li>
     * </ul>
     * If your store plugin requires something else then override this
     * @param cmd Class requiring the strategy
     * @param absFieldNumber Field of the class
     * @return The strategy used when "native" is specified
     */
    public String getStrategyForNative(AbstractClassMetaData cmd, int absFieldNumber)
    {
        if (absFieldNumber >= 0)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absFieldNumber);
            Class type = mmd.getType();
            if (String.class.isAssignableFrom(type))
            {
                return "uuid-hex";
            }
            else if (type == Long.class || type == Integer.class || type == Short.class || type == long.class || type == int.class || type == short.class || type== BigInteger.class)
            {
                if (supportsValueStrategy("identity"))
                {
                    return "identity";
                }
                else if (supportsValueStrategy("sequence") && mmd.getSequence() != null)
                {
                    return "sequence";
                }
                else if (supportsValueStrategy("increment"))
                {
                    return "increment";
                }
                throw new NucleusUserException("This datastore provider doesn't support numeric native strategy for member " + mmd.getFullFieldName());
            }
            else
            {
                throw new NucleusUserException("This datastore provider doesn't support native strategy for field of type " + type.getName());
            }
        }

        IdentityMetaData idmd = cmd.getBaseIdentityMetaData();
        if (idmd != null && idmd.getColumnMetaData() != null)
        {
            if (MetaDataUtils.isJdbcTypeString(idmd.getColumnMetaData().getJdbcType()))
            {
                return "uuid-hex";
            }
        }

        // Numeric datastore-identity
        if (supportsValueStrategy("identity"))
        {
            return "identity";
        }
        else if (supportsValueStrategy("sequence") && idmd.getSequence() != null)
        {
            return "sequence";
        }
        else if (supportsValueStrategy("increment"))
        {
            return "increment";
        }
        throw new NucleusUserException("This datastore provider doesn't support numeric native strategy for class " + cmd.getFullClassName());
    }

    /**
     * Accessor for the next value from the specified generator.
     * This implementation simply returns generator.next(). Any case where the generator requires
     * datastore connections should override this method.
     * @param generator The generator
     * @param ec execution context
     * @return The next value.
     */
    protected Object getStrategyValueForGenerator(ValueGenerator generator, final ExecutionContext ec)
    {
        Object oid = null;
        synchronized (generator)
        {
            // Get the next value for this generator for this ExecutionContext
            // Note : this is synchronised since we don't want to risk handing out this generator
            // while its connectionProvider is set to that of a different ExecutionContext
            // It maybe would be good to change ValueGenerator to have a next taking the connectionProvider
            if (generator instanceof AbstractDatastoreGenerator)
            {
                // datastore-based generator so set the connection provider, using connection for PM
                ValueGenerationConnectionProvider connProvider = new ValueGenerationConnectionProvider()
                {
                    ManagedConnection mconn;
                    public ManagedConnection retrieveConnection()
                    {
                        mconn = getConnection(ec);
                        return mconn;
                    }
                    public void releaseConnection() 
                    {
                        mconn.release();
                        mconn = null;
                    }
                };
                ((AbstractDatastoreGenerator)generator).setConnectionProvider(connProvider);
            }

            oid = generator.next();
        }
        return oid;
    }

    /**
     * Method to return the properties to pass to the generator for the specified field.
     * Will define the following properties "class-name", "root-class-name", "field-name" (if for a field),
     * "sequence-name", "key-initial-value", "key-cache-size", "sequence-table-name", "sequence-schema-name",
     * "sequence-catalog-name", "sequence-name-column-name", "sequence-nextval-column-name".
     * In addition any extension properties on the respective field or datastore-identity are also passed through as properties.
     * @param cmd MetaData for the class
     * @param absoluteFieldNumber Number of the field (-1 = datastore identity)
     * @param ec execution context
     * @param seqmd Any sequence metadata
     * @param tablegenmd Any table generator metadata
     * @return The properties to use for this field
     */
    protected Properties getPropertiesForGenerator(AbstractClassMetaData cmd, int absoluteFieldNumber, ExecutionContext ec, SequenceMetaData seqmd, TableGeneratorMetaData tablegenmd)
    {
        // Set up the default properties available for all value generators
        Properties properties = new Properties();
        AbstractMemberMetaData mmd = null;
        IdentityStrategy strategy = null;
        String sequence = null;
        Map<String, String> extensions = null;
        if (absoluteFieldNumber >= 0)
        {
            // real field
            mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(absoluteFieldNumber);
            strategy = mmd.getValueStrategy();
            sequence = mmd.getSequence();
            extensions = mmd.getExtensions();
        }
        else
        {
            // datastore-identity surrogate field
            // always use the root IdentityMetaData since the root class defines the identity
            IdentityMetaData idmd = cmd.getBaseIdentityMetaData();
            strategy = idmd.getValueStrategy();
            sequence = idmd.getSequence();
            extensions = idmd.getExtensions();
        }

        properties.setProperty(ValueGenerator.PROPERTY_CLASS_NAME, cmd.getFullClassName());
        properties.put(ValueGenerator.PROPERTY_ROOT_CLASS_NAME, cmd.getBaseAbstractClassMetaData().getFullClassName());
        if (mmd != null)
        {
            properties.setProperty(ValueGenerator.PROPERTY_FIELD_NAME, mmd.getFullFieldName());
        }
        if (sequence != null)
        {
            properties.setProperty(ValueGenerator.PROPERTY_SEQUENCE_NAME, sequence);
        }

        // Add any extension properties
        if (extensions != null)
        {
            properties.putAll(extensions);
        }

        if (strategy.equals(IdentityStrategy.NATIVE))
        {
            String realStrategyName = getStrategyForNative(cmd, absoluteFieldNumber);
            strategy = IdentityStrategy.getIdentityStrategy(realStrategyName);
        }

        if (strategy == IdentityStrategy.INCREMENT && tablegenmd != null)
        {
            // User has specified a TableGenerator (JPA)
            properties.put(ValueGenerator.PROPERTY_KEY_INITIAL_VALUE, "" + tablegenmd.getInitialValue());
            properties.put(ValueGenerator.PROPERTY_KEY_CACHE_SIZE, "" + tablegenmd.getAllocationSize());

            if (tablegenmd.getTableName() != null)
            {
                properties.put(ValueGenerator.PROPERTY_SEQUENCETABLE_TABLE, tablegenmd.getTableName());
            }
            if (tablegenmd.getCatalogName() != null)
            {
                properties.put(ValueGenerator.PROPERTY_SEQUENCETABLE_CATALOG, tablegenmd.getCatalogName());
            }
            if (tablegenmd.getSchemaName() != null)
            {
                properties.put(ValueGenerator.PROPERTY_SEQUENCETABLE_SCHEMA, tablegenmd.getSchemaName());
            }
            if (tablegenmd.getPKColumnName() != null)
            {
                properties.put(ValueGenerator.PROPERTY_SEQUENCETABLE_NAME_COLUMN, tablegenmd.getPKColumnName());
            }
            if (tablegenmd.getPKColumnName() != null)
            {
                properties.put(ValueGenerator.PROPERTY_SEQUENCETABLE_NEXTVAL_COLUMN, tablegenmd.getValueColumnName());
            }

            if (tablegenmd.getPKColumnValue() != null)
            {
                properties.put(ValueGenerator.PROPERTY_SEQUENCE_NAME, tablegenmd.getPKColumnValue());
            }
        }
        else if (strategy == IdentityStrategy.INCREMENT && tablegenmd == null)
        {
            if (!properties.containsKey(ValueGenerator.PROPERTY_KEY_CACHE_SIZE))
            {
                // Use default allocation size
                properties.put(ValueGenerator.PROPERTY_KEY_CACHE_SIZE, "" + getIntProperty(PropertyNames.PROPERTY_VALUEGEN_INCREMENT_ALLOCSIZE));
            }
        }
        else if (strategy == IdentityStrategy.SEQUENCE && seqmd != null)
        {
            // User has specified a SequenceGenerator (JDO/JPA)
            if (seqmd.getDatastoreSequence() != null)
            {
                if (seqmd.getInitialValue() >= 0)
                {
                    properties.put(ValueGenerator.PROPERTY_KEY_INITIAL_VALUE, "" + seqmd.getInitialValue());
                }
                if (seqmd.getAllocationSize() > 0)
                {
                    properties.put(ValueGenerator.PROPERTY_KEY_CACHE_SIZE, "" + seqmd.getAllocationSize());
                }
                else
                {
                    // Use default allocation size
                    properties.put(ValueGenerator.PROPERTY_KEY_CACHE_SIZE, "" + getIntProperty(PropertyNames.PROPERTY_VALUEGEN_SEQUENCE_ALLOCSIZE));
                }
                properties.put(ValueGenerator.PROPERTY_SEQUENCE_NAME, "" + seqmd.getDatastoreSequence());

                // Add on any extensions specified on the sequence
                Map<String, String> seqExtensions = seqmd.getExtensions();
                if (seqExtensions != null)
                {
                    properties.putAll(seqExtensions);
                }
            }
            else
            {
                // JDO Factory-based sequence generation
                // TODO Support this
            }
        }
        return properties;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getSubClassesForClass(java.lang.String, boolean, org.datanucleus.ClassLoaderResolver)
     */
    public Collection<String> getSubClassesForClass(String className, boolean includeDescendents, ClassLoaderResolver clr)
    {
        Collection<String> subclasses = new HashSet<>();

        String[] subclassNames = getMetaDataManager().getSubclassesForClass(className, includeDescendents);
        if (subclassNames != null)
        {
            // Load up the table for any classes that are not yet loaded
            for (int i=0;i<subclassNames.length;i++)
            {
                if (!storeDataMgr.managesClass(subclassNames[i]))
                {
                    // We have no knowledge of this class so load it now
                    manageClasses(clr, subclassNames[i]);
                }
                subclasses.add(subclassNames[i]);
            }
        }

        return subclasses;
    }

    /**
     * Accessor for the supported options in string form.
     * Typical values specified here are :-
     * <ul>
     * <li>ApplicationIdentity - if the datastore supports application identity</li>
     * <li>DatastoreIdentity - if the datastore supports datastore identity</li>
     * <li>ORM - if the datastore supports (some) ORM concepts</li>
     * <li>TransactionIsolationLevel.read-committed - if supporting this txn isolation level</li>
     * <li>TransactionIsolationLevel.read-uncommitted - if supporting this txn isolation level</li>
     * <li>TransactionIsolationLevel.repeatable-read - if supporting this txn isolation level</li>
     * <li>TransactionIsolationLevel.serializable - if supporting this txn isolation level</li>
     * <li>TransactionIsolationLevel.snapshot - if supporting this txn isolation level</li>
     * <li>Query.Cancel - if supporting cancelling of queries</li>
     * <li>Query.Timeout - if supporting timeout of queries</li>
     * </ul>
     */
    public Collection<String> getSupportedOptions()
    {
        return Collections.EMPTY_SET;
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
        return nucleusContext.getConfiguration().hasProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.properties.TypedPropertyStore#getProperty(java.lang.String)
     */
    @Override
    public Object getProperty(String name)
    {
        // Use local property value if present, otherwise relay back to context property value
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return super.getProperty(name);
        }
        return nucleusContext.getConfiguration().getProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.properties.PropertyStore#getIntProperty(java.lang.String)
     */
    @Override
    public int getIntProperty(String name)
    {
        // Use local property value if present, otherwise relay back to context property value
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return super.getIntProperty(name);
        }
        return nucleusContext.getConfiguration().getIntProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.properties.PropertyStore#getStringProperty(java.lang.String)
     */
    @Override
    public String getStringProperty(String name)
    {
        // Use local property value if present, otherwise relay back to context property value
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return super.getStringProperty(name);
        }
        return nucleusContext.getConfiguration().getStringProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.properties.PropertyStore#getBooleanProperty(java.lang.String)
     */
    @Override
    public boolean getBooleanProperty(String name)
    {
        // Use local property value if present, otherwise relay back to context property value
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return super.getBooleanProperty(name);
        }
        return nucleusContext.getConfiguration().getBooleanProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.properties.PropertyStore#getBooleanProperty(java.lang.String, boolean)
     */
    @Override
    public boolean getBooleanProperty(String name, boolean resultIfNotSet)
    {
        // Use local property value if present, otherwise relay back to context property value
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return super.getBooleanProperty(name, resultIfNotSet);
        }
        return nucleusContext.getConfiguration().getBooleanProperty(name, resultIfNotSet);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.properties.PropertyStore#getBooleanObjectProperty(java.lang.String)
     */
    @Override
    public Boolean getBooleanObjectProperty(String name)
    {
        // Use local property value if present, otherwise relay back to context property value
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return super.getBooleanObjectProperty(name);
        }
        return nucleusContext.getConfiguration().getBooleanObjectProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#transactionStarted(org.datanucleus.store.ExecutionContext)
     */
    public void transactionStarted(ExecutionContext ec)
    {
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#transactionCommitted(org.datanucleus.store.ExecutionContext)
     */
    public void transactionCommitted(ExecutionContext ec)
    {
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#transactionRolledBack(org.datanucleus.store.ExecutionContext)
     */
    public void transactionRolledBack(ExecutionContext ec)
    {
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#usesBackedSCOWrappers()
     */
    public boolean usesBackedSCOWrappers()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#useBackedSCOWrapperForMember(org.datanucleus.metadata.AbstractMemberMetaData, org.datanucleus.store.ExecutionContext)
     */
    public boolean useBackedSCOWrapperForMember(AbstractMemberMetaData mmd, ExecutionContext ec)
    {
        return usesBackedSCOWrappers();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getDefaultObjectProviderClassName()
     */
    public String getDefaultObjectProviderClassName()
    {
        return StateManagerImpl.class.getName();
    }
}