/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved.
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
2007 Andy Jefferson - javadocs, formatted, copyrighted
2007 Andy Jefferson - added lock/unlock/hasConnection/hasLockedConnection and enlisting
2017 Andy Jefferson - largely rewritten to take on StoreManager getConnection methods.
    ...
**********************************************************************/
package org.datanucleus.store.connection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.XAResource;

import org.datanucleus.ClassConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.transaction.ResourcedTransaction;
import org.datanucleus.transaction.Transaction;
import org.datanucleus.transaction.TransactionEventListener;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager of connections for a datastore, allowing caching of ManagedConnections, enlistment in transaction.
 * Manages a "primary" and (optionally) a "secondary" ConnectionFactory.
 * When caching is enabled it maintains caches of the allocated ManagedConnection per ExecutionContext (an EC can have a single ManagedConnection per ConnectionFactory at any time).
 * <p>
 * The "allocateConnection" method can create connections and enlist them (like most normal persistence operations need) or create a connection and return it 
 * without enlisting it into a transaction, for example on a read-only operation, or when running non-transactional, or to get schema information.
 * </p>
 * <p>
 * Connections can be locked per ExecutionContext basis. Locking of connections is used to handle the connection over to the user application. 
 * A locked connection denies any further access to the datastore, until the user application unlock it.
 * </p>
 */
public class ConnectionManagerImpl implements ConnectionManager
{
    /** StoreManager that we are managing the connections for. */
    final StoreManager storeMgr;

    final PersistenceNucleusContext nucleusContext;

    /** Whether connection caching is enabled. */
    boolean connectionCachingEnabled = true;

    /** "Primary" ConnectionFactory, normally used for transactional operations. */
    ConnectionFactory primaryConnectionFactory = null;

    /** "Secondary" ConnectionFactory, normally used for non-transactional operations. */
    ConnectionFactory secondaryConnectionFactory = null;

    /** Cache of ManagedConnection from the "primary" ConnectionFactory, keyed by the ExecutionContext (since an EC can have max 1 per factory). */
    Map<ExecutionContext, ManagedConnection> primaryConnectionsCache;

    /** Cache of ManagedConnection from the "secondary" ConnectionFactory, keyed by the ExecutionContext (since an EC can have max 1 per factory). */
    Map<ExecutionContext, ManagedConnection> secondaryConnectionsCache;

    /**
     * Constructor.
     * This will register the "primary" and "secondary" ConnectionFactory objects.
     * @param storeMgr Store manager for whom we are managing connections
     */
    public ConnectionManagerImpl(StoreManager storeMgr)
    {
        this.storeMgr = storeMgr;
        this.nucleusContext = storeMgr.getNucleusContext();

        String datastoreName = storeMgr.getStringProperty(FederatedStoreManager.PROPERTY_DATA_FEDERATION_DATASTORE_NAME);

        // "Primary" Factory for connections - transactional
        ConfigurationElement cfElem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_connectionfactory",
            new String[] {"datastore", "transactional"}, new String[] {storeMgr.getStoreManagerKey(), "true"});
        if (cfElem != null)
        {
            try
            {
                this.primaryConnectionFactory = (ConnectionFactory)nucleusContext.getPluginManager().createExecutableExtension("org.datanucleus.store_connectionfactory",
                    new String[] {"datastore", "transactional"}, new String[] {storeMgr.getStoreManagerKey(), "true"}, "class-name",
                    new Class[] {ClassConstants.STORE_MANAGER, ClassConstants.JAVA_LANG_STRING}, new Object[] {storeMgr, AbstractConnectionFactory.RESOURCE_NAME_TX});
                this.primaryConnectionsCache = new ConcurrentHashMap<>();

                if (NucleusLogger.CONNECTION.isDebugEnabled())
                {
                    String cfName = cfElem.getAttribute("name");
                    if (datastoreName != null)
                    {
                        cfName += "-" + datastoreName;
                    }
                    NucleusLogger.CONNECTION.debug(Localiser.msg("032018", cfName));
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

        // "Secondary" Factory for connections - typically for schema/sequences etc
        cfElem = nucleusContext.getPluginManager().getConfigurationElementForExtension("org.datanucleus.store_connectionfactory",
            new String[] {"datastore", "transactional"}, new String[] {storeMgr.getStoreManagerKey(), "false"});
        if (cfElem != null)
        {
            try
            {
                this.secondaryConnectionFactory = (ConnectionFactory)nucleusContext.getPluginManager().createExecutableExtension("org.datanucleus.store_connectionfactory",
                    new String[] {"datastore", "transactional"}, new String[] {storeMgr.getStoreManagerKey(), "false"}, "class-name",
                    new Class[] {ClassConstants.STORE_MANAGER, ClassConstants.JAVA_LANG_STRING}, new Object[] {storeMgr, AbstractConnectionFactory.RESOURCE_NAME_NONTX});
                this.secondaryConnectionsCache = new ConcurrentHashMap<>();

                if (NucleusLogger.CONNECTION.isDebugEnabled())
                {
                    String cfName = cfElem.getAttribute("name");
                    if (datastoreName != null)
                    {
                        cfName += "-" + datastoreName;
                    }
                    NucleusLogger.CONNECTION.debug(Localiser.msg("032019", cfName));
                }
            }
            catch (Exception e)
            {
                throw new NucleusException("Error creating nontransactional connection factory", e).setFatal();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#close()
     */
    @Override
    public void close()
    {
        if (primaryConnectionFactory != null)
        {
            if (primaryConnectionsCache != null)
            {
                primaryConnectionsCache = null;
            }
            primaryConnectionFactory.close();
        }
        if (secondaryConnectionFactory != null)
        {
            if (secondaryConnectionsCache != null)
            {
                secondaryConnectionsCache = null;
            }
            secondaryConnectionFactory.close();
        }
    }

    /**
     * Disable binding objects to ExecutionContext references, so automatically disables the connection caching. 
     */
    public void disableConnectionCaching()
    {
        connectionCachingEnabled = false;

        primaryConnectionsCache = null;
        secondaryConnectionsCache = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#getConnection(org.datanucleus.ExecutionContext, java.util.Map)
     */
    @Override
    public ManagedConnection getConnection(ExecutionContext ec, Map options)
    {
        boolean primary = true;
        if (!ec.getTransaction().isActive())
        {
            boolean singleConnection = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_CONNECTION_SINGLE_CONNECTION);
            if (singleConnection)
            {
                primary = true;
            }
            else if (secondaryConnectionFactory != null)
            {
                primary = false;
            }
            else
            {
                // Some datastores don't define secondary handling so just fallback to the primary factory
                primary = true;
            }
        }

        ManagedConnection mconn = allocateManagedConnection(primary, ec, ec.getTransaction(), options);
        mconn.incrementUseCount(); // Will be decremented on calling mconn.release
        return mconn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#getConnection(int)
     */
    @Override
    public ManagedConnection getConnection(int isolationLevel)
    {
        // Some datastores don't define non-tx handling so just fallback to the primary factory
        boolean primary = (secondaryConnectionFactory != null) ? false : true;

        Map<String, Object> options = null;
        if (isolationLevel >= 0)
        {
            options = Collections.singletonMap(Transaction.TRANSACTION_ISOLATION_OPTION, isolationLevel);
        }

        ManagedConnection mconn = allocateManagedConnection(primary, null, null, options);
        mconn.incrementUseCount(); // Will be decremented on calling mconn.release
        return mconn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#getConnection(boolean, org.datanucleus.ExecutionContext, org.datanucleus.Transaction)
     */
    @Override
    public ManagedConnection getConnection(boolean primary, ExecutionContext ec, Transaction txn)
    {
        ManagedConnection mconn = allocateManagedConnection(primary, ec, txn, null);
        mconn.incrementUseCount(); // Will be decremented on calling mconn.release
        return mconn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#closeAllConnections(org.datanucleus.ExecutionContext)
     */
    @Override
    public void closeAllConnections(ExecutionContext ec)
    {
        if (ec != null && connectionCachingEnabled)
        {
            if (primaryConnectionFactory != null)
            {
                if (primaryConnectionsCache != null)
                {
                    ManagedConnection mc = primaryConnectionsCache.remove(ec);
                    if (mc != null)
                    {
                        mc.close();
                    }
                }
            }
            if (secondaryConnectionFactory != null)
            {
                if (secondaryConnectionsCache != null)
                {
                    ManagedConnection mc = secondaryConnectionsCache.remove(ec);
                    if (mc != null)
                    {
                        mc.close();
                    }
                }
            }
        }
    }

    /**
     * Method to remove the ManagedConnection from the cache.
     * @param primary Whether to use the primary ConnectionFactory
     * @param ec ExecutionContext
     */
    protected void removeManagedConnection(boolean primary, ExecutionContext ec)
    {
        ManagedConnection prevMC = primary ? primaryConnectionsCache.remove(ec) : secondaryConnectionsCache.remove(ec);
        if (nucleusContext.getStatistics() != null && prevMC != null)
        {
            nucleusContext.getStatistics().decrementActiveConnections();
        }
    }

    /**
     * Get a ManagedConnection from the cache.
     * @param primary Whether to use the primary ConnectionFactory
     * @param ec ExecutionContext
     * @return The managed connection
     */
    protected ManagedConnection getManagedConnection(boolean primary, ExecutionContext ec)
    {
        ManagedConnection mconn = primary ? primaryConnectionsCache.get(ec) : secondaryConnectionsCache.get(ec);
        if (mconn != null)
        {
            if (mconn.isLocked())
            {
                // Enlisted connection that is locked so throw exception
                throw new NucleusUserException(Localiser.msg("009000"));
            }

            // Already registered enlisted connection present so return it
            return mconn;
        }
        return null;
    }

    /**
     * Put a ManagedConnection into the cache.
     * @param primary Whether to use the primary ConnectionFactory
     * @param ec ExecutionContext
     * @param mconn The ManagedConnection
     */
    protected void putManagedConnection(boolean primary, ExecutionContext ec, ManagedConnection mconn)
    {
        ManagedConnection prevMC = primary ? primaryConnectionsCache.put(ec, mconn) : secondaryConnectionsCache.put(ec, mconn);
        if (nucleusContext.getStatistics() != null && prevMC == null)
        {
            nucleusContext.getStatistics().incrementActiveConnections();
        }
    }

    /**
     * Method to return a ManagedConnection for this ExecutionContext.
     * If a connection for the ExecutionContext exists in the cache will return it.
     * If no connection exists will create a new one using the ConnectionFactory.
     * This implementation provides the core fix for making the XA and non-XA commit
     * paths mutually exclusive.
     * @param primary Whether this is the primary connection pool
     * @param ec Key in the pool
     * @param transaction The transaction
     * @param options Options for the connection (e.g isolation). These will override those of the txn itself
     * @return The ManagedConnection
     */
    private ManagedConnection allocateManagedConnection(final boolean primary, final ExecutionContext ec, final org.datanucleus.transaction.Transaction transaction, Map options)
    {
        ConnectionFactory factory = primary ? primaryConnectionFactory : secondaryConnectionFactory;
        if (ec != null && connectionCachingEnabled)
        {
            ManagedConnection mconnFromPool = getManagedConnection(primary, ec);
            if (mconnFromPool != null)
            {
                // A pooled connection exists; its state must be reset for the current context.
                if (transaction != null && transaction.isActive())
                {
                    ResourcedTransaction tx = nucleusContext.getResourcedTransactionManager().getTransaction(ec);
                    XAResource res = mconnFromPool.getXAResource();

                    // Determine which commit path to use.
                    if (res != null && tx != null && !tx.isEnlisted(res))
                    {
                        // "Official" XA Path: This connection can be managed by the transaction manager.
                        // Disable the fallback commit path to prevent double-commit deadlocks.
                        mconnFromPool.setCommitOnRelease(false);
                        mconnFromPool.setCloseOnRelease(false);

                        String cfResourceType = factory.getResourceType();
                        if (!ConnectionResourceType.JTA.toString().equalsIgnoreCase(cfResourceType))
                        {
                            tx.enlistResource(res);
                        }
                    }
                    else if (res == null)
                    {
                        // "Fallback" Non-XA Path (e.g., Neo4j): No XAResource is available.
                        // The connection MUST use the commit-on-release fallback mechanism.
                        mconnFromPool.setCommitOnRelease(true);
                        mconnFromPool.setCloseOnRelease(false);
                    }
                }
                else
                {
                    // Not in a transaction: reset to default non-transactional behavior.
                    // This enables the fallback commit path for the next single operation.
                    mconnFromPool.setCommitOnRelease(true);
                    mconnFromPool.setCloseOnRelease(false); // Keep in pool
                }
                return mconnFromPool;
            }
        }

        // No cached connection, so create a new one with the required options.
        Map<String, Object> txnOptions = new HashMap<>();
        if (transaction != null && transaction.getOptions() != null && !transaction.getOptions().isEmpty())
        {
            txnOptions.putAll(transaction.getOptions());
        }
        if (options != null && !options.isEmpty())
        {
            txnOptions.putAll(options);
        }
        final ManagedConnection mconn = factory.createManagedConnection(ec, txnOptions);

        if (ec != null)
        {
            if (transaction != null && transaction.isActive())
            {
                // An active transaction exists; configure the new connection for it.
                configureTransactionEventListener(transaction, mconn);
                ResourcedTransaction tx = nucleusContext.getResourcedTransactionManager().getTransaction(ec);
                XAResource res = mconn.getXAResource();

                // Determine which commit path to use.
                if (res != null && tx != null)
                {
                    // "Official" XA Path: Enlist the resource and disable the fallback path.
                    mconn.setCommitOnRelease(false);
                    mconn.setCloseOnRelease(false);

                    String cfResourceType = factory.getResourceType();
                    if (!ConnectionResourceType.JTA.toString().equalsIgnoreCase(cfResourceType))
                    {
                        tx.enlistResource(res);
                    }
                }
                else
                {
                    // "Fallback" Non-XA Path: Enable the commit-on-release mechanism.
                    mconn.setCommitOnRelease(true);
                    mconn.setCloseOnRelease(false);
                }
            }
            else
            {
                // Not in a transaction, default to commit-on-release for non-transactional operations.
                mconn.setCommitOnRelease(true);
            }

            if (connectionCachingEnabled)
            {
                // Add listener to remove the connection from the pool when the connection closes.
                mconn.addListener(new ManagedConnectionResourceListener()
                {
                    public void transactionFlushed() {}
                    public void transactionPreClose() {}
                    public void managedConnectionPreClose() {}
                    public void managedConnectionPostClose()
                    {
                        removeManagedConnection(primary, ec); // Connection closed so remove
                        mconn.removeListener(this); // Remove this listener
                    }
                    public void resourcePostClose() {}
                });

                // Cache this connection against the ExecutionContext.
                putManagedConnection(primary, ec, mconn);
            }
        }

        return mconn;
    }

    /**
     * Configure a TransactionEventListener that closes the managed connection when a transaction commits or rolls back
     * @param transaction The transaction that we add a listener to
     * @param mconn Managed connection being used
     */
    private void configureTransactionEventListener(final org.datanucleus.transaction.Transaction transaction, final ManagedConnection mconn)
    {
        // Add handler for any enlisted connection to the transaction so we know when to close it
        if (mconn.closeAfterTransactionEnd())
        {
            transaction.addTransactionEventListener(
                new TransactionEventListener()
                {
                    public void transactionStarted() {}
                    public void transactionRolledBack()
                    {
                        try
                        {
                            mconn.close();
                        }
                        finally
                        {
                            transaction.removeTransactionEventListener(this);
                        }
                    }
                    public void transactionCommitted()
                    {
                        try
                        {
                            mconn.close();
                        }
                        finally
                        {
                            transaction.removeTransactionEventListener(this);
                        }
                    }
                    public void transactionEnded()
                    {
                        try
                        {
                            mconn.close();
                        }
                        finally
                        {
                            transaction.removeTransactionEventListener(this);
                        }
                    }
                    public void transactionPreCommit()
                    {
                        if (mconn.isLocked())
                        {
                            // Enlisted connection that is locked so throw exception
                            throw new NucleusUserException(Localiser.msg("009000"));
                        }
                        mconn.transactionPreClose();
                    }
                    public void transactionPreRollBack()
                    {
                        if (mconn.isLocked())
                        {
                            // Enlisted connection that is locked so throw exception
                            throw new NucleusUserException(Localiser.msg("009000"));
                        }
                        mconn.transactionPreClose();
                    }
                    public void transactionPreFlush() {}
                    public void transactionFlushed()
                    {
                        mconn.transactionFlushed();
                    }
                    public void transactionSetSavepoint(String name)
                    {
                        mconn.setSavepoint(name);
                    }
                    public void transactionReleaseSavepoint(String name)
                    {
                        mconn.releaseSavepoint(name);
                    }
                    public void transactionRollbackToSavepoint(String name)
                    {
                        mconn.rollbackToSavepoint(name);
                    }
                });
        }
        else
        {
            transaction.bindTransactionEventListener(
                new TransactionEventListener()
                {
                    public void transactionStarted() {}
                    public void transactionPreFlush() {}
                    public void transactionFlushed()
                    {
                        mconn.transactionFlushed();
                    }
                    public void transactionPreCommit()
                    {
                        if (mconn.isLocked())
                        {
                            // Enlisted connection that is locked so throw exception
                            throw new NucleusUserException(Localiser.msg("009000"));
                        }
                        mconn.transactionPreClose();
                    }
                    public void transactionCommitted() {}
                    public void transactionPreRollBack()
                    {
                        if (mconn.isLocked())
                        {
                            // Enlisted connection that is locked so throw exception
                            throw new NucleusUserException(Localiser.msg("009000"));
                        }
                        mconn.transactionPreClose();
                    }
                    public void transactionRolledBack() {}
                    public void transactionEnded() {}
                    public void transactionSetSavepoint(String name)
                    {
                        mconn.setSavepoint(name);
                    }
                    public void transactionReleaseSavepoint(String name)
                    {
                        mconn.releaseSavepoint(name);
                    }
                    public void transactionRollbackToSavepoint(String name)
                    {
                        mconn.rollbackToSavepoint(name);
                    }
                });
        }
    }
}