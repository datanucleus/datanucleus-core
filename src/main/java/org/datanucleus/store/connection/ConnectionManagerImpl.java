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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.XAResource;

import org.datanucleus.ClassConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.Transaction;
import org.datanucleus.TransactionEventListener;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.transaction.ResourcedTransaction;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager of connections for a datastore, allowing caching of ManagedConnections, enlistment in transaction.
 * Manages a "primary" and (optionally) a "secondary" ConnectionFactory.
 * Each ExecutionContext can have at most 1 ManagedConnection per ConnectionFactory at any time.
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
    StoreManager storeMgr;

    /** Context for this connection manager. */
    PersistenceNucleusContext nucleusContext;

    /** Whether connection pooling is enabled. */
    boolean connectionPoolEnabled = true;

    /** "Primary" ConnectionFactory, normally used for transactional operations. */
    ConnectionFactory primaryConnectionFactory = null;

    /** "Secondary" ConnectionFactory, normally used for non-transactional operations. */
    ConnectionFactory secondaryConnectionFactory = null;

    /**
     * Constructor.
     * This will register the "primary" and "secondary" ConnectionFactory objects.
     * @param context Context for this manager.
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
            primaryConnectionFactory.close();
        }
        if (secondaryConnectionFactory != null)
        {
            secondaryConnectionFactory.close();
        }
    }

    /**
     * Disable binding objects to ExecutionContext references, so automatically
     * disables the connection pooling 
     */
    public void disableConnectionPool()
    {
        connectionPoolEnabled = false;
    }

    /**
     * Connection pool keeps a reference to a connection for each ExecutionContext (and so the connection used by each transaction).
     * This permits reuse of connections in the same transaction, but not at same time!!!
     * ManagedConnection must be released to return to pool.
     * On transaction commit/rollback, connection pool is cleared
     *
     * For each combination of ExecutionContext-ConnectionFactory there is 0 or 1 ManagedConnection
     * TODO Change this to have a primary and secondary since we only have 2 ConnectionFactory. Need to update allocateConnection too.
     */
    Map<ExecutionContext, Map<ConnectionFactory, ManagedConnection>> connectionsPool = new ConcurrentHashMap<>();

    /**
     * Method to remove the managed connection from the pool.
     * @param factory The factory for connections
     * @param ec Key for the pool of managed connections
     */
    protected void removeManagedConnection(ConnectionFactory factory, ExecutionContext ec)
    {
        Map<ConnectionFactory, ManagedConnection> connectionsForEC = connectionsPool.get(ec);
        if (connectionsForEC != null)
        {
            ManagedConnection prevConn = connectionsForEC.remove(factory);

            if (nucleusContext.getStatistics() != null && prevConn != null)
            {
                nucleusContext.getStatistics().decrementActiveConnections();
            }

            if (connectionsForEC.isEmpty())
            {
                // No connections remaining for this ExecutionContext
                connectionsPool.remove(ec);
            }
        }
    }

    /**
     * Obtain a ManagedConnection from the pool.
     * @param factory The factory for connections
     * @param ec Key for pooling
     * @return The managed connection
     */
    protected ManagedConnection getManagedConnection(ConnectionFactory factory, ExecutionContext ec)
    {
        Map<ConnectionFactory, ManagedConnection> connectionsForEC = connectionsPool.get(ec);
        if (connectionsForEC == null)
        {
            return null;
        }

        // obtain a ManagedConnection for an specific ConnectionFactory
        ManagedConnection mconn = connectionsForEC.get(factory);
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

    protected void putManagedConnection(ConnectionFactory factory, ExecutionContext ec, ManagedConnection mconn)
    {
        Map<ConnectionFactory, ManagedConnection> connectionsForEC = connectionsPool.get(ec);
        if (connectionsForEC == null)
        {
            connectionsForEC = new HashMap();
            connectionsPool.put(ec, connectionsForEC);
        }

        ManagedConnection prevConn = connectionsForEC.put(factory, mconn);
        if (nucleusContext.getStatistics() != null && prevConn == null)
        {
            nucleusContext.getStatistics().incrementActiveConnections();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#getConnection(org.datanucleus.ExecutionContext, java.util.Map)
     */
    @Override
    public ManagedConnection getConnection(ExecutionContext ec, Map options)
    {
        ConnectionFactory connFactory;
        if (ec.getTransaction().isActive())
        {
            connFactory = primaryConnectionFactory;
        }
        else
        {
            boolean singleConnection = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_CONNECTION_SINGLE_CONNECTION);
            if (singleConnection)
            {
                connFactory = primaryConnectionFactory;
            }
            else if (secondaryConnectionFactory != null)
            {
                connFactory = secondaryConnectionFactory;
            }
            else
            {
                // Some datastores don't define secondary handling so just fallback to the primary factory
                connFactory = primaryConnectionFactory;
            }
        }

        ManagedConnection mconn = allocateConnection(connFactory, ec, ec.getTransaction(), options);
        ((AbstractManagedConnection)mconn).incrementUseCount(); // Will be decremented on calling mconn.release
        return mconn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#getConnection(int)
     */
    @Override
    public ManagedConnection getConnection(int isolationLevel)
    {
        // Some datastores don't define non-tx handling so just fallback to the primary factory
        ConnectionFactory connFactory = (secondaryConnectionFactory != null) ? secondaryConnectionFactory : primaryConnectionFactory;

        Map<String, Object> options = null;
        if (isolationLevel >= 0)
        {
            options = new HashMap<>();
            options.put(Transaction.TRANSACTION_ISOLATION_OPTION, Integer.valueOf(isolationLevel));
        }

        ManagedConnection mconn = allocateConnection(connFactory, null, null, options);
        ((AbstractManagedConnection)mconn).incrementUseCount(); // Will be decremented on calling mconn.release
        return mconn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#getConnection(boolean, org.datanucleus.ExecutionContext, org.datanucleus.Transaction)
     */
    @Override
    public ManagedConnection getConnection(boolean primary, ExecutionContext ec, Transaction txn)
    {
        ConnectionFactory cf = primary ? primaryConnectionFactory : secondaryConnectionFactory;

        ManagedConnection mconn = allocateConnection(cf, ec, txn, null);
        ((AbstractManagedConnection)mconn).incrementUseCount(); // Will be decremented on calling mconn.release
        return mconn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionManager#closeAllConnections(org.datanucleus.ExecutionContext)
     */
    @Override
    public void closeAllConnections(ExecutionContext ec)
    {
        if (ec != null && connectionPoolEnabled)
        {
            if (primaryConnectionFactory != null)
            {
                ManagedConnection mconnFromPool = getManagedConnection(primaryConnectionFactory, ec);
                if (mconnFromPool != null)
                {
                    // Already registered enlisted connection present so return it
                    mconnFromPool.close();
                }
            }
            if (secondaryConnectionFactory != null)
            {
                ManagedConnection mconnFromPool = getManagedConnection(secondaryConnectionFactory, ec);
                if (mconnFromPool != null)
                {
                    // Already registered enlisted connection present so return it
                    mconnFromPool.close();
                }
            }
        }
    }

    /**
     * Method to return a connection for this ExecutionContext.
     * If a connection for the ExecutionContext exists in the cache will return it.
     * If no connection exists will create a new one using the ConnectionFactory.
     * @param factory ConnectionFactory it relates to
     * @param ec Key in the pool
     * @param options Options for the connection (e.g isolation). These will override those of the txn itself
     * @return The ManagedConnection
     */
    private ManagedConnection allocateConnection(final ConnectionFactory factory, final ExecutionContext ec, final org.datanucleus.Transaction transaction, Map options)
    {
        if (ec != null && connectionPoolEnabled)
        {
            ManagedConnection mconnFromPool = getManagedConnection(factory, ec);
            if (mconnFromPool != null)
            {
                // Factory already has a ManagedConnection
                if (!mconnFromPool.closeAfterTransactionEnd())
                {
                    if (transaction.isActive())
                    {
                        // ManagedConnection that is not closed after commit, so make sure it is enlisted
                        if (mconnFromPool.commitOnRelease())
                        {
                            mconnFromPool.setCommitOnRelease(false);
                        }
                        if (mconnFromPool.closeOnRelease())
                        {
                            mconnFromPool.setCloseOnRelease(false);
                        }

                        // Enlist the connection resource if is not enlisted and has enlistable resource
                        XAResource res = mconnFromPool.getXAResource();
                        ResourcedTransaction tx = nucleusContext.getResourcedTransactionManager().getTransaction(ec);
                        if (res != null && tx != null && !tx.isEnlisted(res))
                        {
                            String cfResourceType = factory.getResourceType();
                            if (!ConnectionResourceType.JTA.toString().equalsIgnoreCase(cfResourceType))
                            {
                                // Enlist the resource with this transaction EXCEPT where using external JTA container
                                tx.enlistResource(res);
                            }
                        }
                    }
                    else
                    {
                        // Nontransactional : Reset to commit-on-release
                        if (!mconnFromPool.commitOnRelease())
                        {
                            mconnFromPool.setCommitOnRelease(true);
                        }
                        if (mconnFromPool.closeOnRelease())
                        {
                            mconnFromPool.setCloseOnRelease(false);
                        }
                    }
                }

                return mconnFromPool;
            }
        }

        // No cached connection so create new connection with required options
        Map txnOptions = new HashMap();
        if (transaction != null && transaction.getOptions() != null && !transaction.getOptions().isEmpty())
        {
            txnOptions.putAll(transaction.getOptions());
        }
        if (options != null && !options.isEmpty())
        {
            txnOptions.putAll(options);
        }
        final ManagedConnection mconn = factory.createManagedConnection(ec, txnOptions);

        // Enlist the connection in this transaction
        if (ec != null)
        {
            if (transaction.isActive())
            {
                // Connection is "managed", and enlist with txn
                configureTransactionEventListener(transaction, mconn);
                ResourcedTransaction tx = nucleusContext.getResourcedTransactionManager().getTransaction(ec);
                mconn.setCommitOnRelease(false); //must be set before getting the XAResource
                mconn.setCloseOnRelease(false); //must be set before getting the XAResource

                // Enlist the connection resource if has enlistable resource
                XAResource res = mconn.getXAResource();
                if (res != null && tx != null && !tx.isEnlisted(res))
                {
                    String cfResourceType = factory.getResourceType();
                    if (!ConnectionResourceType.JTA.toString().equalsIgnoreCase(cfResourceType))
                    {
                        // Enlist the resource with this transaction EXCEPT where using external JTA container
                        tx.enlistResource(res);
                    }
                }
            }

            if (connectionPoolEnabled)
            {
                // Add listener to remove the connection from the pool when the connection closes
                mconn.addListener(new ManagedConnectionResourceListener()
                {
                    public void transactionFlushed() {}
                    public void transactionPreClose() {}
                    public void managedConnectionPreClose() {}
                    public void managedConnectionPostClose()
                    {
                        removeManagedConnection(factory, ec); // Connection closed so remove

                        // Remove this listener
                        mconn.removeListener(this);
                    }
                    public void resourcePostClose() {}
                });

                // Register this connection against the ExecutionContext - connection is valid
                putManagedConnection(factory, ec, mconn);
            }
        }

        return mconn;
    }

    /**
     * Configure a TransactionEventListener that closes the managed connection when a transaction commits or rolls back
     * @param transaction The transaction that we add a listener to
     * @param mconn Managed connection being used
     */
    private void configureTransactionEventListener(final org.datanucleus.Transaction transaction, final ManagedConnection mconn)
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