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
    ...
**********************************************************************/
package org.datanucleus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.exceptions.TransactionActiveOnBeginException;
import org.datanucleus.exceptions.TransactionNotActiveException;
import org.datanucleus.transaction.HeuristicMixedException;
import org.datanucleus.transaction.HeuristicRollbackException;
import org.datanucleus.transaction.NucleusTransactionException;
import org.datanucleus.transaction.RollbackException;
import org.datanucleus.transaction.TransactionManager;
import org.datanucleus.transaction.TransactionUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Implementation of a transaction for a datastore. {@link org.datanucleus.Transaction}
 */
public class TransactionImpl implements Transaction
{
    /** Localisation of messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    ExecutionContext ec;

    TransactionManager txnMgr;

    /** Whether the transaction is active. */
    boolean active = false;

    /** Flag for whether we are currently committing. */
    boolean committing;

    /** Synchronisation object, for committing and rolling back. Only for JDO. */
    Synchronization sync;

    /** Whether the transaction is only for roll-back. */
    protected boolean rollbackOnly = false;

    /** Whether to serialise (lock) any read objects in this transaction. */
    protected Boolean serializeRead = null;

    /** Listeners for the lifecycle of the active transaction. **/
    private Set<TransactionEventListener> listenersPerTransaction = new HashSet();

    /** Listener to inform the ExecutionContext of transaction events. */
    private TransactionEventListener ecListener;

    /** Listeners that apply to all transactions. **/
    private List<TransactionEventListener> userListeners = new ArrayList<TransactionEventListener>();

    private Map<String, Object> options = null;

    /** start time of the transaction */
    long beginTime;

    boolean closed = false;

    /**
     * Constructor for a transaction for the specified ExecutionContext.
     * @param ec ExecutionContext
     */
    public TransactionImpl(ExecutionContext ec)
    {
        this.ec = ec;
        this.ecListener = (TransactionEventListener) ec;
        this.txnMgr = ec.getNucleusContext().getTransactionManager();

        PersistenceConfiguration config = ec.getNucleusContext().getPersistenceConfiguration();

        int isolationLevel = TransactionUtils.getTransactionIsolationLevelForName(
            config.getStringProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION));
        setOption(Transaction.TRANSACTION_ISOLATION_OPTION, isolationLevel);

        // Locking of read objects in this transaction
        Boolean serialiseReadProp = config.getBooleanObjectProperty(PropertyNames.PROPERTY_SERIALIZE_READ);
        if (ec.getProperty(PropertyNames.PROPERTY_SERIALIZE_READ) != null)
        {
            serialiseReadProp = ec.getBooleanProperty(PropertyNames.PROPERTY_SERIALIZE_READ);
        }
        if (serialiseReadProp != null)
        {
            serializeRead = serialiseReadProp;
        }
    }

    public void close()
    {
        closed = true;
    }

    /**
     * Method to begin the transaction.
     */
    public void begin()
    {
        if (ec.getMultithreaded())
        {
            synchronized (this)
            {
                txnMgr.begin(ec);
            }
        }
        else
        {
            txnMgr.begin(ec);
        }
        internalBegin();
    }

    /**
     * Method to begin the transaction.
     */
    protected void internalBegin()
    {
        if (active)
        {
            throw new TransactionActiveOnBeginException(ec);
        }

        active = true;
        beginTime = System.currentTimeMillis();
        if (ec.getStatistics() != null)
        {
            ec.getStatistics().transactionStarted();
        }
        if (NucleusLogger.TRANSACTION.isDebugEnabled())
        {
            NucleusLogger.TRANSACTION.debug(LOCALISER.msg("015000", ec, "" + ec.getBooleanProperty(PropertyNames.PROPERTY_OPTIMISTIC)));
        }

        TransactionEventListener[] ls = getListenersForEvent();
        for (TransactionEventListener tel : ls)
        {
            tel.transactionStarted();
        }
    }

    /**
     * Method to notify of preFlush.
     */
    public void preFlush()
    {
        try
        {
            TransactionEventListener[] ls = getListenersForEvent();
            for (TransactionEventListener tel : ls)
            {
                tel.transactionPreFlush();
            }
        }
        catch (Throwable ex)
        {
            if (ex instanceof NucleusException)
            {
                throw (NucleusException)ex;
            }
            // Wrap all other exceptions in a NucleusTransactionException
            throw new NucleusTransactionException(LOCALISER.msg("015005"), ex);
        }
    }

    /**
     * Method to flush the transaction.
     */
    public void flush()
    {
        try
        {
            TransactionEventListener[] ls = getListenersForEvent();
            for (TransactionEventListener tel : ls)
            {
                tel.transactionFlushed();
            }
        }
        catch (Throwable ex)
        {
            if (ex instanceof NucleusException)
            {
                throw (NucleusException)ex;
            }
            // Wrap all other exceptions in a NucleusTransactionException
            throw new NucleusTransactionException(LOCALISER.msg("015005"), ex);
        }
    }

    /**
     * Method to allow the transaction to flush any resources.
     */
    public void end()
    {
        try
        {
            flush();
        }
        finally
        {
            TransactionEventListener[] ls = getListenersForEvent();
            for (TransactionEventListener tel : ls)
            {
                tel.transactionEnded();
            }
        }
    }

    /**
     * Method to commit the transaction.
     */
    public void commit()
    {
        if (!isActive())
        {
            throw new TransactionNotActiveException();
        }

        // JDO 2.0 section 13.4.5 rollbackOnly functionality
        // It isn't clear from the spec if we are expected to do the rollback here.
        // The spec simply says that we throw an exception. This is assumed as meaning that the users code will catch
        // the exception and call rollback themselves. i.e we don't need to close the DB connection or set "active" to false.
        if (rollbackOnly)
        {
            // Throw an exception since can only exit via rollback
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug(LOCALISER.msg("015020"));
            }

            throw new NucleusDataStoreException(LOCALISER.msg("015020")).setFatal();
        }

        long startTime = System.currentTimeMillis();
        boolean success = false;
        boolean canComplete = true; //whether the transaction can be completed
        List errors = new ArrayList();
        try
        {
            flush(); // TODO Is this needed? om.preCommit will handle flush calls
            internalPreCommit();
            internalCommit();
            success = true;
        }
        catch (RollbackException e)
        {
            //catch only RollbackException because user exceptions can be raised
            //in Transaction.Synchronization and they should cascade up to user code
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug(StringUtils.getStringFromStackTrace(e));
            }            
            errors.add(e);
        }
        catch (HeuristicRollbackException e)
        {
            //catch only HeuristicRollbackException because user exceptions can be raised
            //in Transaction.Synchronization and they should cascade up to user code
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug(StringUtils.getStringFromStackTrace(e));
            }            
            errors.add(e);
        }
        catch (HeuristicMixedException e)
        {
            //catch only HeuristicMixedException because user exceptions can be raised
            //in Transaction.Synchronization and they should cascade up to user code
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug(StringUtils.getStringFromStackTrace(e));
            }            
            errors.add(e);
        }
        catch (NucleusUserException e)
        {
            //catch only NucleusUserException
            //they must be cascade up to user code and transaction is still alive
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug(StringUtils.getStringFromStackTrace(e));
            }
            canComplete = false;
            throw e;
        }
        catch (NucleusException e)
        {
            //catch only NucleusException because user exceptions can be raised
            //in Transaction.Synchronization and they should cascade up to user code
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug(StringUtils.getStringFromStackTrace(e));
            }            
            errors.add(e);
        }
        finally
        {
            if (canComplete)
            {
                try
                {
                    if (!success)
                    {
                        rollback();
                    }
                    else
                    {
                        internalPostCommit();
                    }
                }
                catch (Throwable e)
                {
                    errors.add(e);
                }
            }
        }
        if (errors.size() > 0)
        {
            throw new NucleusTransactionException(LOCALISER.msg("015007"), (Throwable[])errors.toArray(
                new Throwable[errors.size()]));
        }

        if (NucleusLogger.TRANSACTION.isDebugEnabled())
        {
            NucleusLogger.TRANSACTION.debug(LOCALISER.msg("015022", (System.currentTimeMillis() - startTime)));
        }
    }

    /**
     * Method to perform any pre-commit operations like flushing to the datastore, calling the users
     * "beforeCompletion", and general preparation for the commit.
     */
    protected void internalPreCommit()
    {
        committing = true;

        if (NucleusLogger.TRANSACTION.isDebugEnabled())
        {
            NucleusLogger.TRANSACTION.debug(LOCALISER.msg("015001", ec));
        }

        if (sync != null)
        {
            // JDO2 $13.4.3 Allow the user to perform any updates before we do loading of fields etc
            sync.beforeCompletion();
        }

        // Perform any pre-commit operations
        TransactionEventListener[] ls = getListenersForEvent();
        for (TransactionEventListener tel : ls)
        {
            tel.transactionPreCommit();
        }
    }
    
    /**
     * Internal commit, DataNucleus invokes it's own transaction manager implementation, if
     * an external transaction manager is not used.
     */
    protected void internalCommit()
    {
        // optimistic transactions that don't have dirty
        if (ec.getMultithreaded())
        {
            synchronized (this)
            {
                txnMgr.commit(ec);
            }
        }
        else
        {
            txnMgr.commit(ec);
        }
    }    

    /**
     * Method to rollback the transaction.
     */
    public void rollback()
    {
        if (!isActive())
        {
            throw new TransactionNotActiveException();
        }

        long startTime = System.currentTimeMillis();
        try
        {
            boolean canComplete = true; //whether the transaction can be completed
            committing = true;
            try
            {
                flush(); // TODO Is this really needed? om.preRollback does all necessary
            }
            finally
            {
                //even if flush fails, we ignore and go ahead cleaning up and rolling back everything ahead...
                try
                {
                    internalPreRollback();
                }
                catch (NucleusUserException e)
                {
                    //catch only NucleusUserException; they must be cascade up to user code and transaction is still alive
                    if (NucleusLogger.TRANSACTION.isDebugEnabled())
                    {
                        NucleusLogger.TRANSACTION.debug(StringUtils.getStringFromStackTrace(e));
                    }
                    canComplete = false;
                    throw e;
                }
                finally
                {
                    if (canComplete)
                    {
                        try
                        {
                            internalRollback();                          
                        }
                        finally
                        {
                            try
                            {
                                active = false;
                                if (ec.getStatistics() != null)
                                {
                                    ec.getStatistics().transactionRolledBack(System.currentTimeMillis()-beginTime);
                                }
                            }
                            finally
                            {
                                listenersPerTransaction.clear(); 
                                rollbackOnly = false; // Reset rollbackOnly flag
                                if (sync != null)
                                {
                                    sync.afterCompletion(Status.STATUS_ROLLEDBACK);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (NucleusUserException e)
        {
            throw e;
        }
        catch (NucleusException e)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("015009"), e);
        }
        finally
        {
            committing = false;
        }

        if (NucleusLogger.TRANSACTION.isDebugEnabled())
        {
            NucleusLogger.TRANSACTION.debug(LOCALISER.msg("015023", (System.currentTimeMillis() - startTime)));
        }
    }

    protected void internalPreRollback()
    {
        if (NucleusLogger.TRANSACTION.isDebugEnabled())
        {
            NucleusLogger.TRANSACTION.debug(LOCALISER.msg("015002", ec));
        }

        TransactionEventListener[] ls = getListenersForEvent();
        for (TransactionEventListener tel : ls)
        {
            tel.transactionPreRollBack();
        }
    }

    /**
     * Internal rollback, DataNucleus invokes it's own transaction manager implementation, if
     * an external transaction manager is not used.
     */
    protected void internalRollback()
    {
        org.datanucleus.transaction.Transaction tx = txnMgr.getTransaction(ec);
        if (tx != null)
        {
            if (ec.getMultithreaded())
            {
                synchronized (this)
                {
                    txnMgr.rollback(ec);
                }
            }
            else
            {
                txnMgr.rollback(ec);
            }
        }         

        TransactionEventListener[] ls = getListenersForEvent();
        for (TransactionEventListener tel : ls)
        {
            tel.transactionRolledBack();
        }
    }

    /**
     * Method to perform any post-commit operations like calling the users "afterCompletion"
     * and general clean up after the commit.
     */
    protected void internalPostCommit()
    {
        try
        {
            active = false;
            if (ec.getStatistics() != null)
            {
                ec.getStatistics().transactionCommitted(System.currentTimeMillis()-beginTime);
            }
        }
        finally
        {
            try
            {
                TransactionEventListener[] ls = getListenersForEvent();
                for (TransactionEventListener tel : ls)
                {
                    tel.transactionCommitted();
                }
            }
            finally
            {
                committing = false;
                listenersPerTransaction.clear();
                // call sync.afterCompletion() only now to support the use-case of closing the PM in afterCompletion()
                if (sync != null)
                {
                    sync.afterCompletion(Status.STATUS_COMMITTED);
                }
            }
        }
    }

    private TransactionEventListener[] getListenersForEvent()
    {
        TransactionEventListener[] ls =
            new TransactionEventListener[userListeners.size()+listenersPerTransaction.size() + 1];
        System.arraycopy(listenersPerTransaction.toArray(), 0, ls, 0, listenersPerTransaction.size());
        System.arraycopy(userListeners.toArray(), 0,  ls, listenersPerTransaction.size(), userListeners.size());
        ls[ls.length-1] = ecListener;
        return ls;
    }

    /**
     * Accessor for whether the transaction is active.
     * @return Whether the transaction is active.
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * Similar to "isActive" except that it just returns the "active" flag whereas the isActive() method can
     * also embody rejoining to underlying transactions.
     * @return The "active" flag
     */
    public boolean getIsActive()
    {
        return active;
    }

    /**
     * Accessor for whether the transaction is comitting.
     * @return Whether the transaction is committing.
     */
    public boolean isCommitting()
    {
        return committing;
    }

    // ------------------------------- Accessors/Mutators ---------------------------------------

    /**
     * Accessor for the nontransactionalRead flag for this transaction.
     * @return Whether nontransactionalRead is set.
     */
    public boolean getNontransactionalRead()
    {
        return ec.getBooleanProperty(PropertyNames.PROPERTY_NONTX_READ);
    }

    /**
     * Accessor for the nontransactionalWrite flag for this transaction.
     * @return Whether nontransactionalWrite is set.
     */
    public boolean getNontransactionalWrite()
    {
        return ec.getBooleanProperty(PropertyNames.PROPERTY_NONTX_WRITE);
    }

    /**
     * Accessor for the Optimistic setting
     * @return Whether optimistic transactions are in operation.
     */
    public boolean getOptimistic()
    {
        return ec.getBooleanProperty(PropertyNames.PROPERTY_OPTIMISTIC);
    }

    /**
     * Accessor for the restoreValues flag for this transaction.
     * @return Whether restoreValues is set.
     */
    public boolean getRestoreValues()
    {
        return ec.getBooleanProperty(PropertyNames.PROPERTY_RESTORE_VALUES);
    }

    /**
     * Accessor for the retainValues flag for this transaction.
     * @return Whether retainValues is set.
     */
    public boolean getRetainValues()
    {
        return ec.getBooleanProperty(PropertyNames.PROPERTY_RETAIN_VALUES);
    }

    /**
     * Accessor for the "rollback only" flag.
     * @return The rollback only flag
     */
    public boolean getRollbackOnly()
    {
        return rollbackOnly;
    }

    /**
     * Accessor for the synchronization object to be notified on transaction completion.
     * @return The synchronization instance to be notified on transaction completion.
     */
    public Synchronization getSynchronization()
    {
        return sync;
    }

    /**
     * Mutator for the setting of nontransactional read.
     * @param nontransactionalRead Whether to allow nontransactional read operations
     */
    public void setNontransactionalRead(boolean nontransactionalRead)
    {
        ec.setProperty(PropertyNames.PROPERTY_NONTX_READ, nontransactionalRead);
    }

    /**
     * Mutator for the setting of nontransactional write.
     * @param nontransactionalWrite Whether to allow nontransactional write operations
     */
    public void setNontransactionalWrite(boolean nontransactionalWrite)
    {
        ec.setProperty(PropertyNames.PROPERTY_NONTX_WRITE, nontransactionalWrite);
    }

    /**
     * Mutator for the optimistic transaction setting.
     * @param optimistic The optimistic transaction setting.
     */
    public void setOptimistic(boolean optimistic)
    {
        ec.setProperty(PropertyNames.PROPERTY_OPTIMISTIC, optimistic);
    }

    /**
     * Mutator for the setting of restore values.
     * @param restoreValues Whether to restore values at commit
     */
    public void setRestoreValues(boolean restoreValues)
    {
        ec.setProperty(PropertyNames.PROPERTY_RESTORE_VALUES, restoreValues);
    }

    /**
     * Mutator for the setting of retain values.
     * @param retainValues Whether to retain values at commit
     */
    public void setRetainValues(boolean retainValues)
    {
        ec.setProperty(PropertyNames.PROPERTY_RETAIN_VALUES, retainValues);
        if (retainValues)
        {
            setNontransactionalRead(true);
        }
    }

    /**
     * Mutator for the "rollback only" flag. Sets the transaction as for rollback only.
     */
    public void setRollbackOnly()
    {
        // Only apply to active transactions
        if (active)
        {
            rollbackOnly = true;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.Transaction#setSavepoint(java.lang.String)
     */
    @Override
    public void setSavepoint(String name)
    {
        throw new UnsupportedOperationException("Dont currently support savepoints");
        // TODO Implement this
    }

    /* (non-Javadoc)
     * @see org.datanucleus.Transaction#releaseSavepoint(java.lang.String)
     */
    @Override
    public void releaseSavepoint(String name)
    {
        throw new UnsupportedOperationException("Dont currently support savepoints");
        // TODO Implement this
    }

    /* (non-Javadoc)
     * @see org.datanucleus.Transaction#rollbackToSavepoint(java.lang.String)
     */
    @Override
    public void rollbackToSavepoint(String name)
    {
        throw new UnsupportedOperationException("Dont currently support savepoints");
        // TODO Implement this
    }

    /**
     * Mutator for the synchronization object to be notified on transaction completion.
     * @param sync The synchronization object to be notified on transaction completion
     */
    public void setSynchronization(Synchronization sync)
    {
        this.sync = sync;
    }

    public void addTransactionEventListener(TransactionEventListener listener)
    {
        this.listenersPerTransaction.add(listener);
    }

    public void removeTransactionEventListener(TransactionEventListener listener)
    {
        this.listenersPerTransaction.remove(listener);
        this.userListeners.remove(listener);
    }

    public void bindTransactionEventListener(TransactionEventListener listener)
    {
        this.userListeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.Transaction#getSerializeRead()
     */
    public Boolean getSerializeRead()
    {
        return serializeRead;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.Transaction#setSerializeRead(java.lang.Boolean)
     */
    public void setSerializeRead(Boolean serializeRead)
    {
        this.serializeRead = serializeRead;
    }

    public Map<String, Object> getOptions()
    {
        return options;
    }
    
    public void setOption(String option, int value)
    {
        if (options == null)
        {
            options = new HashMap<String, Object>();
        }
        options.put(option, Integer.valueOf(value));
    }
    
    public void setOption(String option, boolean value)
    {
        if (options == null)
        {
            options = new HashMap<String, Object>();
        }
        options.put(option, Boolean.valueOf(value));
    }

    public void setOption(String option, String value)
    {
        if (options == null)
        {
            options = new HashMap<String, Object>();
        }
        options.put(option, value);
    }

    public void setOption(String option, Object value)
    {
        if (options == null)
        {
            options = new HashMap<String, Object>();
        }
        options.put(option, value);
    }
}