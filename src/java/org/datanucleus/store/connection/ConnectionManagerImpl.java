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
    ...
**********************************************************************/
package org.datanucleus.store.connection;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.TransactionEventListener;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.federation.FederatedStoreManager;
import org.datanucleus.transaction.Transaction;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager of connections for a datastore, allowing ManagedConnection pooling, enlistment in transaction.
 * The pool caches one connection per ExecutionContext.
 * The "allocateConnection" method can create connections and enlist them (like most normal persistence operations need)
 * or create a connection and return it without enlisting it into a transaction, for example the connections used to
 * generate object identity, create the database schema or obtaining the schema metadata.
 * 
 * Connections can be locked per ExecutionContext basis. Locking of connections is used to
 * handle the connection over to the user application. A locked connection denies any further
 * access to the datastore, until the user application unlock it.
 */
public class ConnectionManagerImpl implements ConnectionManager
{
    /** Localisation of messages. */
    protected static final Localiser LOCALISER=Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Context for this connection manager. */
    PersistenceNucleusContext nucleusContext;

    /** Registry of factories for connections, keyed by their symbolic name. */
    Map<String, ConnectionFactory> factories = new HashMap<String, ConnectionFactory>();

    ManagedConnectionPool connectionPool = new ManagedConnectionPool();

    /** Whether connection pooling is enabled. */
    boolean connectionPoolEnabled = true;

    /**
     * Constructor.
     * @param context Context for this manager.
     */
    public ConnectionManagerImpl(PersistenceNucleusContext context)
    {
        this.nucleusContext = context;
    }

    /**
     * Pool of managed connections keyed by poolKey objects.
     * Each "poolKey" key has its own pool of ManagedConnection's
     */
    class ManagedConnectionPool
    {
        /**
         * Connection pool keeps a reference to a connection for each ExecutionContext (and so the connection
         * used by each transaction).
         * This permits reuse of connections in the same transaction, but not at same time!!!
         * ManagedConnection must be released to return to pool.
         * On transaction commit/rollback, connection pool is cleared
         *
         * For each combination of ExecutionContext-ConnectionFactory there is 0 or 1 ManagedConnection
         */
        Map<Object, Map<ConnectionFactory, ManagedConnection>> connectionsPool = new HashMap();

        /**
         * Method to remove the managed connection from the pool.
         * @param factory The factory for connections
         * @param ec Key for the pool of managed connections
         */
        public void removeManagedConnection(ConnectionFactory factory, ExecutionContext ec)
        {
            synchronized (connectionsPool)
            {
                Object poolKey = getPoolKey(factory, ec);
                Map connectionsForPool = connectionsPool.get(poolKey);
                if (connectionsForPool != null)
                {
                    if (connectionsForPool.remove(factory) != null && nucleusContext.getStatistics() != null)
                    {
                        nucleusContext.getStatistics().decrementActiveConnections();
                    }

                    if (connectionsForPool.size() == 0)
                    {
                        // No connections remaining for this OM so remove the entry for the ExecutionContext
                        connectionsPool.remove(poolKey);
                    }
                }
            }
        }
        
        /**
         * Object a ManagedConnection from pool
         * @param factory The factory for connections
         * @param ec Key for pooling
         * @return The managed connection
         */
        public ManagedConnection getManagedConnection(ConnectionFactory factory, ExecutionContext ec)
        {
            synchronized (connectionsPool)
            {
                Object poolKey = getPoolKey(factory, ec);
                Map<ConnectionFactory, ManagedConnection> connectionsForEC = connectionsPool.get(poolKey);
                if (connectionsForEC == null)
                {
                    return null;
                }

                //obtain a ManagedConnection for an specific ConnectionFactory
                ManagedConnection mconn = connectionsForEC.get(factory);
                if (mconn != null)
                {
                    if (mconn.isLocked())
                    {
                        // Enlisted connection that is locked so throw exception
                        throw new NucleusUserException(LOCALISER.msg("009000"));
                    }                        
                    // Already registered enlisted connection present so return it
                    return mconn;
                }
            }
            return null;
        }
        
        public void putManagedConnection(ConnectionFactory factory, ExecutionContext ec, ManagedConnection mconn)
        {
            synchronized (connectionsPool)
            {
                Object poolKey = getPoolKey(factory, ec);
                Map connectionsForOM = connectionsPool.get(poolKey);
                if (connectionsForOM == null)
                {
                    connectionsForOM = new HashMap();
                    connectionsPool.put(poolKey, connectionsForOM);
                }
                if (connectionsForOM.put(factory, mconn) == null && nucleusContext.getStatistics() != null)
                {
                    nucleusContext.getStatistics().incrementActiveConnections();
                }
            }
        }

        Object getPoolKey(ConnectionFactory factory, ExecutionContext ec)
        {
            if (ec.getStoreManager() instanceof FederatedStoreManager)
            {
                // With data federation we need to key via factory+ec
                return new PoolKey(factory, ec);
            }
            return ec;
        }
    }

    class PoolKey
    {
        ConnectionFactory factory;
        ExecutionContext ec;

        public PoolKey(ConnectionFactory factory, ExecutionContext ec)
        {
            this.factory = factory;
            this.ec = ec;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {
            if (obj == null || !(obj instanceof PoolKey))
            {
                return false;
            }
            PoolKey other = (PoolKey)obj;
            return factory == other.factory && ec == other.ec;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            return factory.hashCode() ^ ec.hashCode();
        }
    }

    /**
     * Method to close all pooled connections for the specified key of the specified factory.
     * @param factory The factory
     * @param ec The key in the pool
     */
    public void closeAllConnections(final ConnectionFactory factory, final ExecutionContext ec)
    {
        if (ec != null && connectionPoolEnabled)
        {
            ManagedConnection mconnFromPool = connectionPool.getManagedConnection(factory, ec);
            if (mconnFromPool != null)
            {
                // Already registered enlisted connection present so return it
                if (NucleusLogger.CONNECTION.isDebugEnabled())
                {
                    NucleusLogger.CONNECTION.debug("Connection found in the pool : " + mconnFromPool + 
                        " for key=" + ec + " in factory=" + factory + " but owner object closing so closing connection");
                }
                mconnFromPool.close();
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
    public ManagedConnection allocateConnection(final ConnectionFactory factory, final ExecutionContext ec,
            final org.datanucleus.Transaction transaction, Map options)
    {
        if (ec != null && connectionPoolEnabled)
        {
            ManagedConnection mconnFromPool = connectionPool.getManagedConnection(factory, ec);
            if (mconnFromPool != null)
            {
                // Already registered enlisted connection present so return it
                if (NucleusLogger.CONNECTION.isDebugEnabled())
                {
                    NucleusLogger.CONNECTION.debug("Connection found in the pool : " + mconnFromPool + 
                        " for key=" + ec + " in factory=" + factory);
                }

                if (!mconnFromPool.closeAfterTransactionEnd())
                {
                    if (transaction.isActive())
                    {
                        // Transactional : make sure it is not commit-on-release
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
                        Transaction tx = nucleusContext.getTransactionManager().getTransaction(ec);
                        if (res != null && !tx.isEnlisted(res))
                        {
                            boolean enlistInLocalTM = true;
                            if (options != null && options.get(ConnectionFactory.RESOURCE_TYPE_OPTION) != null &&
                                ConnectionResourceType.JTA.toString().equalsIgnoreCase((String)options.get(ConnectionFactory.RESOURCE_TYPE_OPTION)))
                            {
                                //XAResource will be enlisted in an external JTA container,
                                //so we don't enlist it in the internal Transaction Manager
                                enlistInLocalTM = false;
                            }
                            if (enlistInLocalTM)
                            {
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

        // Create new connection
        final ManagedConnection mconn = factory.createManagedConnection(ec, 
            options == null && transaction != null ? transaction.getOptions() : options);

        // Enlist the connection in this transaction
        if (ec != null)
        {
            if (transaction.isActive())
            {
                // Connection is "managed", and enlist with txn
                configureTransactionEventListener(transaction, mconn);
                Transaction tx = nucleusContext.getTransactionManager().getTransaction(ec);
                mconn.setCommitOnRelease(false); //must be set before getting the XAResource
                mconn.setCloseOnRelease(false); //must be set before getting the XAResource

                // Enlist the connection resource if has enlistable resource
                XAResource res = mconn.getXAResource();
                if (res != null)
                {
                    boolean enlistInLocalTM = true;
                    if (options != null && options.get(ConnectionFactory.RESOURCE_TYPE_OPTION) != null &&
                        ConnectionResourceType.JTA.toString().equalsIgnoreCase((String)options.get(ConnectionFactory.RESOURCE_TYPE_OPTION)))
                    {
                        //XAResource will be enlisted in an external JTA container,
                        //so we don't enlist it in the internal Transaction Manager
                        enlistInLocalTM = false;
                    }
                    if (enlistInLocalTM)
                    {
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
                        if (NucleusLogger.CONNECTION.isDebugEnabled())
                        {
                            NucleusLogger.CONNECTION.debug("Connection removed from the pool : " + mconn +
                                " for key=" + ec + " in factory=" + factory);
                        }
                        connectionPool.removeManagedConnection(factory, ec); // Connection closed so remove

                        // Remove this listener
                        mconn.removeListener(this);
                    }
                    public void resourcePostClose() {}
                });

                // Register this connection against the ExecutionContext - connection is valid
                if (NucleusLogger.CONNECTION.isDebugEnabled())
                {
                    NucleusLogger.CONNECTION.debug("Connection added to the pool : " + mconn + 
                        " for key=" + ec + " in factory=" + factory);
                }
                connectionPool.putManagedConnection(factory, ec, mconn);
            }
        }

        return mconn;
    }

    /**
     * Configure a TransactionEventListener that closes the managed connection when a 
     * transaction commits or rolls back
     * @param transaction The transaction that we add a listener to
     * @param mconn Managed connection being used
     */
    private void configureTransactionEventListener(final org.datanucleus.Transaction transaction,
            final ManagedConnection mconn)
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
                            throw new NucleusUserException(LOCALISER.msg("009000"));
                        }
                        mconn.transactionPreClose();
                    }
                    public void transactionPreRollBack()
                    {
                        if (mconn.isLocked())
                        {
                            // Enlisted connection that is locked so throw exception
                            throw new NucleusUserException(LOCALISER.msg("009000"));
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
                            throw new NucleusUserException(LOCALISER.msg("009000"));
                        }
                        mconn.transactionPreClose();
                    }
                    public void transactionCommitted() {}
                    public void transactionPreRollBack()
                    {
                        if (mconn.isLocked())
                        {
                            // Enlisted connection that is locked so throw exception
                            throw new NucleusUserException(LOCALISER.msg("009000"));
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

    /**
     * Method to lookup a connection factory and create it if not yet existing.
     * @param name The lookup name "e.g "jdbc/tx"
     * @return The connection factory
     */
    public ConnectionFactory lookupConnectionFactory(String name)
    {
        return factories.get(name);
    }

    /**
     * Method to register a connection factory under a name.
     * @param name The lookup name "e.g "jdbc/tx"
     * @param factory The connection factory
     */
    public void registerConnectionFactory(String name, ConnectionFactory factory)
    {
        factories.put(name, factory);
    }

    /**
     * Disable binding objects to ExecutionContext references, so automatically
     * disables the connection pooling 
     */
    public void disableConnectionPool()
    {
        connectionPoolEnabled = false;
    }
}