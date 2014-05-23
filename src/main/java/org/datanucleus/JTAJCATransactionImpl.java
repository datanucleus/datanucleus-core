/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved.
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
    ...
**********************************************************************/
package org.datanucleus;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.datanucleus.properties.PropertyStore;
import org.datanucleus.transaction.NucleusTransactionException;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * A transaction that is synchronised with a Java Transaction Service (JTA) transaction with JCA.
 * It is only used when using the JDO API within a JavaEE environment using the JCA adapter (i.e not for JPA).
 */
public class JTAJCATransactionImpl extends TransactionImpl implements Synchronization 
{
    private TransactionManager jtaTM;

    /** JTA transaction we currently are synchronized with. Null when there is no JTA transaction active or not yet detected. */
    private javax.transaction.Transaction jtaTx;

    private boolean markedForRollback = false; // TODO This doesn't seem to be ever true, why?

    /**
     * Constructor.
     * @param ec ExecutionContext
     */
    JTAJCATransactionImpl(ExecutionContext ec, PropertyStore properties)
    {
        super(ec, properties);
        joinTransaction();
    }
    
    JTAJCATransactionImpl(ExecutionContext ec)
    {
        this(ec, null);
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.TransactionImpl#getIsActive()
     */
    @Override
    public boolean getIsActive()
    {
        // Just use isActive() since we haven't been able to test whether we should allow rejoining on JavaEE with JCA.
        return isActive();
    }

    /**
     * Accessor for whether the transaction is active.
     * @return Whether the transaction is active.
     **/
    public boolean isActive()
    {
        boolean isActive = super.isActive();
        if (isActive)
        {
            //do not join transaction if org.datanucleus.Transaction already started
            return true;
        }
        joinTransaction();
        return active;
    }

    // ------------------- Methods to get the JTA transaction for synchronising --------------------------

    /**
     * Synchronise our active state with that of the JTA transaction, if it exists.
     * Look for an active JTA transaction. if there is one, begin() ourselves
     * and register synchronisation. We must poll because there is no
     * way of getting notified of a newly begun transaction.<p>
     */
    private synchronized void joinTransaction()
    {       
        if (active)
        {
            return;
        }

        // try to registerSynchronization()
        try
        {
            if (jtaTM == null)
            {
                // Retrieve the JTA TransactionManager. Unfortunately, before JavaEE 5 there is no specified way to do it, 
                // only app-server-specific ways. In JavaEE 5, we can use TransactionSynchronizationRegistry
                jtaTM = ec.getNucleusContext().getJtaTransactionManager();
                if (jtaTM == null)
                {
                    throw new NucleusTransactionException(Localiser.msg("015030"));
                }
            }
            jtaTx = jtaTM.getTransaction();
            if (jtaTx != null && jtaTx.getStatus() == Status.STATUS_ACTIVE)
            {
                if (!ec.getNucleusContext().isJcaMode()) // TODO Aren't we always in JCA mode with this class?
                {
                    //in JCA mode, we do not register Synchronization
                    // TODO Use JtaSyncRegistry if available?
                    jtaTx.registerSynchronization(this);
                }                

                //the transaction is active here
                begin();
            }
            else
            {
                // jtaTx can be null when there is no active transaction.
                // There is no app-server agnostic way of getting notified
                // when a global transaction has started. Instead, we
                // poll for jtaTx' status in getConnection() and isActive()

                // If a transaction was marked for rollback before we could
                // register synchronisation, we won't be called back when it
                // is rolled back
                if (markedForRollback)
                {
                    // as jtaTx is null there is no active transaction, meaning
                    // that the jtaTx was actually rolled back after it had
                    // been marked for rollback: catch up
                    rollback();
                    markedForRollback = false;
                }
            }
        }
        catch (SystemException se)
        {
            throw new NucleusTransactionException(Localiser.msg("015026"), se);
        }
        catch (RollbackException e)
        {
            NucleusLogger.TRANSACTION.error("Exception while joining transaction: " + StringUtils.getStringFromStackTrace(e));
            // tx is marked for rollback: leave registeredSynchronizationOnJtaTx==false
            // so that we try to register again next time we're called
        }
    }

    /**
     * Called by the transaction manager prior to the start of the two-phase transaction commit process.
     */
    public void beforeCompletion()
    {
        try
        {
            internalPreCommit();
        }
        catch (Throwable th)
        {
            // TODO Localise these messages
            NucleusLogger.TRANSACTION.error("Exception flushing work in JTA transaction. Mark for rollback", th);
            try
            {
                jtaTx.setRollbackOnly();
            }
            catch (Exception e)
            {
                NucleusLogger.TRANSACTION.fatal("Cannot mark transaction for rollback after exception in beforeCompletion. PersistenceManager might be in inconsistent state", e);
            }
        }
    }

    /**
     * Called by the transaction manager after the transaction is committed or rolled back.
     * Must be synchronised because some callers expect to be owner of this object's monitor (internalPostCommit() 
     * calls closeSQLConnection() which calls notifyAll()).
     * @param status The status
     */
    public synchronized void afterCompletion(int status)
    {
        try
        {
            if (status == Status.STATUS_ROLLEDBACK)
            {
                rollback();
            }
            else if (status == Status.STATUS_COMMITTED)
            {
                internalPostCommit();
            }
            else
            {
                // TODO Localise this
                NucleusLogger.TRANSACTION.fatal("Received unexpected transaction status + " + status);
            }
        }
        catch (Throwable th)
        {
            // TODO Localise this
            NucleusLogger.TRANSACTION.error("Exception during afterCompletion in JTA transaction. PersistenceManager might be in inconsistent state");
        }
        finally
        {
            // done with this jtaTx. Make us synchronise with JTA again, as there there is no callback for a new tx
            jtaTx = null;
        }
    }   
}