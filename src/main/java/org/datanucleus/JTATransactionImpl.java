/**********************************************************************
Copyright (c) 2006 Jorg von Frantzius and others. All rights reserved.
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
2006 Andy Jefferson - localised, adapted to latest CVS
2007 GUido Anzuoni - move TX Manager lookup to Context
2008 Jorg von Frantzius - Fix bugs and test with JBOSS 4.0.3
2009 Guido Anzuoni - changes to allow PM close in afterCompletion
2013 Andy Jefferson - added autoJoin, isJoined, and getIsActive for JPA requirements.
    ...
**********************************************************************/
package org.datanucleus;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.properties.PropertyStore;
import org.datanucleus.store.connection.ConnectionFactory;
import org.datanucleus.store.connection.ConnectionResourceType;
import org.datanucleus.transaction.NucleusTransactionException;
import org.datanucleus.transaction.jta.JTASyncRegistry;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Transaction that is synchronized with a Java Transaction Service (JTA) transaction.
 * Works only in environments where a TransactionManager is present. This transaction joins to the 
 * transaction via the TransactionManager, and the TransactionManager notifies this class of completion 
 * via the beforeCompletion/afterCompletion callback hooks. This transaction can be configured as "autoJoin"
 * whereby it will try to join when it is created and otherwise when isActive() is called. When it is not
 * set to "autoJoin" the developer has to call <pre>joinTransaction</pre> which will check the current
 * join status and join as necessary.
 * <p>
 * When this transaction is being used the transactions must be controlled using javax.transaction.UserTransaction,
 * and not using local transactions (e.g PM.currentTransaction().begin()). 
 * Should also work for SessionBeans, as per spec UserTransaction reflects SessionBean-based tx demarcation.
 * See also {@link org.datanucleus.Transaction}
 */
public class JTATransactionImpl extends TransactionImpl implements Synchronization 
{
    private static boolean JBOSS_SERVER = System.getProperty("jboss.server.name") != null;

    private enum JoinStatus
    {
        NO_TXN, IMPOSSIBLE, JOINED
    }

    private TransactionManager jtaTM;

    /** Underlying JTA txn we are currently synced with. Null when no JTA transaction active or not yet joined. */
    private javax.transaction.Transaction jtaTx;

    private JTASyncRegistry jtaSyncRegistry;

    protected JoinStatus joinStatus = JoinStatus.NO_TXN;

    private UserTransaction userTransaction;

    protected boolean autoJoin = true;

    /**
     * Constructor.
     * Will attempt to join with the transaction manager to get the underlying transaction.
     * @param ec ExecutionContext
     * @param autoJoin Whether to auto-join to the underlying UserTransaction on isActive and at creation?
     * @param properties Properties to use with the transaction
     */
    JTATransactionImpl(ExecutionContext ec, boolean autoJoin, PropertyStore properties)
    {
        super(ec, properties);
        this.autoJoin = autoJoin;

        // we only make sense in combination with ResourceType.JTA. Verify this has been set.
        Configuration conf = ec.getNucleusContext().getConfiguration();
        if (!(ConnectionResourceType.JTA.toString().equalsIgnoreCase(conf.getStringProperty(
                ConnectionFactory.DATANUCLEUS_CONNECTION_RESOURCE_TYPE)) && 
            ConnectionResourceType.JTA.toString().equalsIgnoreCase(conf.getStringProperty(
                ConnectionFactory.DATANUCLEUS_CONNECTION2_RESOURCE_TYPE))))
        {
            throw new NucleusException("Internal error: either " +
                ConnectionFactory.DATANUCLEUS_CONNECTION_RESOURCE_TYPE + " or " +
                ConnectionFactory.DATANUCLEUS_CONNECTION2_RESOURCE_TYPE + 
                " have not been set to JTA; this should have happened automatically.");
        }

        // Inform DN TransactionManager to not do anything with the actual datastore connection as managed by JTA
        txnMgr.setContainerManagedConnections(true);

        jtaTM = ec.getNucleusContext().getJtaTransactionManager();
        if (jtaTM == null)
        {
            throw new NucleusTransactionException(Localiser.msg("015030"));
        }
        jtaSyncRegistry = ec.getNucleusContext().getJtaSyncRegistry();

        if (autoJoin)
        {
            // Join to any UserTransaction if present
            joinTransaction();
        }
    }

    public boolean isJoined()
    {
        return (joinStatus == JoinStatus.JOINED);
    }

    private int getTransactionStatus()
    {
        try
        {
            return jtaTM.getStatus();
        }
        catch (SystemException se)
        {
            throw new NucleusTransactionException(Localiser.msg("015026"), se);
        }
    }
    /**
     * Method to call if you want to join to the underlying UserTransaction.
     * Will be called by isActive() and constructor if "autoJoin" is set, otherwise has to be called by user code.
     */
    public void joinTransaction()
    {
        if (joinStatus != JoinStatus.JOINED)
        {
            try
            {
                javax.transaction.Transaction txn = jtaTM.getTransaction();
                int txnstat = jtaTM.getStatus();
                if (jtaTx != null && !jtaTx.equals(txn))
                {
                    // changed transaction, clear saved jtaTxn, reset join status and reprocess
                    // it should happen only if joinStatus == JOIN_STATUS_IMPOSSIBLE
                    if (! (joinStatus == JoinStatus.IMPOSSIBLE))
                    {
                        throw new InternalError("JTA Transaction changed without being notified");
                    }
                    jtaTx = null;
                    joinStatus = JoinStatus.NO_TXN;
                    joinTransaction();
                }
                else
                {
                    if (jtaTx == null)
                    {
                        jtaTx = txn;
                        boolean allow_join = (txnstat == Status.STATUS_ACTIVE);
                        if (allow_join)
                        {
                            joinStatus = JoinStatus.IMPOSSIBLE;
                            try
                            {
                                if (jtaSyncRegistry != null)
                                {
                                    // Register via TransactionSynchronizationRegistry
                                    jtaSyncRegistry.register(this);
                                }
                                else
                                {
                                    // Register via Transaction
                                    jtaTx.registerSynchronization(this);
                                }
                                boolean was_active = super.isActive();
                                if (!was_active)
                                {
                                    // the transaction is active here
                                    internalBegin();
                                }
                            }
                            catch (Exception e)
                            {
                                throw new NucleusTransactionException("Cannot register Synchronization to a valid JTA Transaction", e);
                            }
                            joinStatus = JoinStatus.JOINED;
                        }
                        else
                        {
                            if (jtaTx != null)
                            {
                                joinStatus = JoinStatus.IMPOSSIBLE;
                            }
                        }
                    }
                }
            }
            catch (SystemException e)
            {
                throw new NucleusTransactionException(Localiser.msg("015026"), e);
            }
        }
    }

    public boolean getIsActive()
    {
        if (closed)
        {
            return false;
        }
        int txnStatus = getTransactionStatus();
        if (txnStatus == Status.STATUS_COMMITTED || txnStatus == Status.STATUS_ROLLEDBACK)
        {
            return false;
        }
        return super.getIsActive();
    }

    /**
     * Accessor for whether the transaction is active. The UserTransaction is considered active if its status is
     * anything other than {@link Status#STATUS_NO_TRANSACTION}, i.e. when the current thread is associated with a
     * JTA transaction. <b>Note that this will attempt to join if not yet joined</b>
     * @return Whether the transaction is active.
     */
    public boolean isActive()
    {
        if (autoJoin)
        {
            if (joinStatus == JoinStatus.JOINED)
            {
                // already joined so already in sync, so return "active" flag to avoid JNDI lookups
                return super.isActive();
            }

            // join as required
            joinTransaction();
            return super.isActive() || joinStatus == JoinStatus.IMPOSSIBLE;
        }

        // Just return the "active" flag
        return super.isActive();
    }

    /**
     * JDO spec "16.1.3 Stateless Session Bean with Bean Managed Transactions": "acquiring a PM without beginning a UserTransaction results 
     * in the PM being able to manage transaction boundaries via begin, commit, and rollback methods on JDO Transaction.
     * The PM will automatically begin the User-Transaction during Transaction.begin and automatically commit the UserTransaction during Transaction.commit"
     */
    public void begin()
    {
        joinTransaction();
        if (joinStatus != JoinStatus.NO_TXN)
        {
            throw new NucleusTransactionException("JTA Transaction is already active");
        }

        UserTransaction utx;
        try
        {
            Context ctx = new InitialContext();
            if (JBOSS_SERVER)
            {
                // JBoss unfortunately doesn't always provide UserTransaction at the JavaEE standard location
                // see e.g. http://docs.jboss.org/admin-devel/Chap4.html
                utx = (UserTransaction) ctx.lookup("UserTransaction");
            }
            else
            {
                utx = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
            }
        }
        catch (NamingException e)
        {
            throw ec.getApiAdapter().getUserExceptionForException("Failed to obtain UserTransaction", e);
        }

        try
        {
            utx.begin();
        }
        catch (NotSupportedException e)
        {
            throw ec.getApiAdapter().getUserExceptionForException("Failed to begin UserTransaction", e);
        }
        catch (SystemException e)
        {
            throw ec.getApiAdapter().getUserExceptionForException("Failed to begin UserTransaction", e);
        }

        joinTransaction();
        if (joinStatus != JoinStatus.JOINED)
        {
            throw new NucleusTransactionException("Cannot join an auto started UserTransaction");
        }
        userTransaction = utx;
    }

    /**
     * Allow UserTransaction demarcation
     */
    public void commit()
    {
        if (userTransaction == null)
        {
            throw new NucleusTransactionException("No internal UserTransaction");
        }

        try
        {
            userTransaction.commit();
        }
        catch (Exception e)
        {
            throw ec.getApiAdapter().getUserExceptionForException("Failed to commit UserTransaction", e);
        }
        finally
        {
            userTransaction = null;
        }
    }

    /**
     * Allow UserTransaction demarcation
     */
    public void rollback()
    {
        if (userTransaction == null)
        {
            throw new NucleusTransactionException("No internal UserTransaction");
        }

        try
        {
            userTransaction.rollback();
        }
        catch (Exception e)
        {
            throw ec.getApiAdapter().getUserExceptionForException("Failed to rollback UserTransaction", e);
        }
        finally
        {
            userTransaction = null;
        }

    }

    /**
     * Allow UserTransaction demarcation
     */
    public void setRollbackOnly()
    {
        if (userTransaction == null)
        {
            throw new NucleusTransactionException("No internal UserTransaction");
        }

        try
        {
            userTransaction.setRollbackOnly();
        }
        catch (Exception e)
        {
            throw ec.getApiAdapter().getUserExceptionForException("Failed to rollback-only UserTransaction", e);
        }
    }

    // ----------------- Implementation of javax.transaction.Synchronization -------------------

    /**
     * The beforeCompletion method is called by the transaction manager prior to the start of the two-phase 
     * transaction commit process.
     */
    public void beforeCompletion()
    {
        RuntimeException thr = null;
        boolean success = false;
        try
        {
            flush();
            // internalPreCommit() can lead to new updates performed by usercode  
            // in the Synchronization.beforeCompletion() callback
            internalPreCommit();
            flush();
            success = true;
        }
        catch (RuntimeException e)
        {
            thr = e;
            throw e;
        }
        finally
        {
            if (!success) 
            {
                NucleusLogger.TRANSACTION.error(Localiser.msg("015044"), thr);
                try
                {
                    jtaTx.setRollbackOnly();
                }
                catch (Exception e)
                {
                    NucleusLogger.TRANSACTION.fatal(Localiser.msg("015045"), e);
                }
            }
        }
    }

    /**
     * This method is called by the transaction manager after the transaction is committed or rolled back.
     * Must be synchronised because some callers expect to be owner of this object's monitor 
     * (internalPostCommit() calls closeSQLConnection() which calls notifyAll()).
     * @param status The status
     */
    public synchronized void afterCompletion(int status)
    {
        if (closed)
        {
            // JavaEE container has closed the ExecutionContext (EntityManager) before calling afterCompletion on the
            // Synchronisation object we registered with it. No point cleaning anything up here. Take the consequences.
            NucleusLogger.TRANSACTION.warn(Localiser.msg("015048", this));
            return;
        }

        RuntimeException thr = null;
        boolean success = false;
        try
        {
            if (status == Status.STATUS_ROLLEDBACK)
            {
                super.rollback();
            }
            else if (status == Status.STATUS_COMMITTED)
            {
                super.internalPostCommit();
            }
            else
            {
                // this method is called after completion, so we can reasonably expect final status codes
                NucleusLogger.TRANSACTION.fatal(Localiser.msg("015047", status));
            }
            success = true;
        }
        catch (RuntimeException re)
        {
            thr = re;
            NucleusLogger.TRANSACTION.error("Exception in afterCompletion : " + re.getMessage(), re);
            throw re;
        }
        finally
        {
            jtaTx = null;
            joinStatus = JoinStatus.NO_TXN;
            if (!success)
            {
                NucleusLogger.TRANSACTION.error(Localiser.msg("015046"), thr);
            }
        }

        if (active)
        {
            throw new NucleusTransactionException("internal error, must not be active after afterCompletion()!");
        }
    }
}