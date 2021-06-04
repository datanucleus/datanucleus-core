/**********************************************************************
Copyright (c) 2002 Kelly Grizzle and others. All rights reserved.
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
2003 Andy Jefferson - commented
2006 Andy Jefferson - rewritten to be independent of JDO
    ...
**********************************************************************/
package org.datanucleus.transaction;

import java.util.Map;

import javax.transaction.Synchronization;

import org.datanucleus.exceptions.NucleusUserException;

/**
 * Representation of an ExecutionContext transaction within DataNucleus.
 * 
 * Handling of transactions in DataNucleus is split into 4 layers:
 * <ul>
 * <li>API - The User Visible Transaction API</li>
 * <li>ExecutionContext Transaction - The Transaction assigned to an ExecutionContext</li>
 * <li>X/Open/JTA - The ResourcedTransactionManager managing ResourcedTransaction associated to the underlying datastore transaction</li>
 * <li>Resource - The Transaction handled by the datastore</li>
 * </ul>
 *
 * In the the API layer, there are interfaces provided to the user application, as such:
 * <ul>
 * <li>JDO Transaction - the JDO API interface</li>
 * <li>JPA EntityTransaction - the JPA API interface</li>
 * <li>{@link javax.transaction.UserTransaction} - the JTA API interface</li>
 * </ul>
 *
 * In the ExecutionContext layer, the {@link org.datanucleus.transaction.Transaction} interface defines the contract for handling transactions for the ExecutionContext.
 * <p>
 * In the X/Open/JTA layer the handling of XA resources is done. 
 * It means, XAResources are obtained and enlisted to a TransactionManager. 
 * The TransactionManager will commit or rollback the resources at the end of the transactions. 
 * There are two kinds of TransactionManager: DataNucleus and JTA. 
 * A JTA TransactionManager is external to DataNucleus, while the DataNucleus TransactionManager is implemented by DataNucleus as {@link org.datanucleus.transaction}.
 * The DataNucleus TransactionManager is used when the DataSource used to obtain connections to the underlying database is not enlisted in an external JTA TransactionManager.
 * The JTA TransactionManager is usually found when running in JavaEE application servers, however nowadays there are many JTA containers that can be used in JavaSE.
 * <p>
 * The scenarios where a JTA TransactionManager is used is:
 * When an JTA TransactionManager exists, and the connections to the underlying databases are acquired via transactional DataSources. 
 * That means, when you ask a connection to the DataSource, it will automatically enlist it in a JTA TransactionManager.
 * <p>
 * The Resource layer is handled by the datastore. For example, with RDBMS databases, the javax.sql.Connection is the API used to demarcate the database transactions. 
 * In an RDBMS database, the resource layer is handling the database transaction.
 * 
 * For a treatment of isolation levels, refer to http://www.cs.umb.edu/~poneil/iso.pdf
 */ 
public interface Transaction
{
    /** Option to use when wanting to set the transaction isolation level. */
    public static final String TRANSACTION_ISOLATION_OPTION = "transaction.isolation";

    /**
     * Method to inform the transaction that it is closed.
     * This is only necessary for JBoss usage of JPA where it doesn't bother calling afterCompletion on the JTA txn
     * until after it closes the EntityManager so we need a hook to inform the transaction that it is closed so ignore
     * any subsequent afterCompletion.
     */
    void close();

    /**
     * Begin a transaction.
     * The type of transaction (datastore/optimistic) is determined by the setting of the Optimistic flag.
     * @throws NucleusUserException if transactions are managed by a container in the managed environment, or if the transaction is already active.
     */
    void begin();

    /**
     * Commit the current transaction. The commit will trigger flushing the transaction, will
     * invoke the preCommit, commit the resources and invoke postCommit listeners.
     * If during flush or preCommit phases a NucleusUserException is raised, then the transaction will not
     * complete and the transaction remains active. The NucleusUserException is cascaded to the caller.
     * @throws NucleusUserException if transactions are managed by a container in the managed environment, or if the transaction is not active.
     */
    void commit();

    /**
     * Rollback the current transaction. The commit will trigger flushing the transaction, will
     * invoke the preRollback, rollback the resources and invoke postRollback listeners.
     * If during flush or preRollback phases a NucleusUserException is raised, then the transaction will not
     * complete and the transaction remains active. The NucleusUserException is cascaded to the caller.
     * @throws NucleusUserException if transactions are managed by a container in the managed environment, or if the transaction is not active.
     */
    void rollback();

    /**
     * Returns whether there is a transaction currently active. This can also attempt to join to
     * any underlying transaction if the implementation requires it. Use <pre>getIsActive</pre> if you just want the active flag.
     * @return Whether the transaction is active.
     */
    boolean isActive();

    /**
     * Return whether the transaction is active and return immediately.
     * @return Whether it is active
     */
    boolean getIsActive();

    /**
     * Accessor for the time (millisecs) from System.currentTimeMillis when the transaction started.
     * @return Time at which the transaction started. -1 implies not yet started.
     */
    long getBeginTime();

    /**
     * Method to notify that flush is started.
     */
    void preFlush();

    /**
     * Method to allow the transaction to flush any resources.
     */
    void flush();

    /**
     * Method to notify that the transaction is ended.
     */
    void end();
    
    /**
     * Returns the rollback-only status of the transaction.
     * When begun, the rollback-only status is false. Either the application or the JDO implementation may set this flag using setRollbackOnly.
     * @return Whether the transaction has been marked for rollback.
     */
    boolean getRollbackOnly();

    /**
     * Sets the rollback-only status of the transaction to <code>true</code>.
     * After this flag is set to <code>true</code>, the transaction can no longer be committed.
     * @throws NucleusUserException if the flag is true and an attempt is made to commit the txn
     */
    void setRollbackOnly();

    /**
     * If <code>true</code>, allow persistent instances to be read without a transaction active.
     * If an implementation does not support this option, a NucleusUserException is thrown.
     * @param nontransactionalRead Whether to have non-tx reads
     * @throws NucleusUserException if not supported
     */
    void setNontransactionalRead(boolean nontransactionalRead);

    /**
     * If <code>true</code>, allows persistent instances to be read without a transaction active.
     * @return Whether we are allowing non-tx reads
     */
    boolean getNontransactionalRead();

    /**
     * If <code>true</code>, allow persistent instances to be written without a transaction active.
     * @param nontransactionalWrite Whether requiring non-tx writes
     * @throws NucleusUserException if not supported
     */
    void setNontransactionalWrite(boolean nontransactionalWrite);

    /**
     * If <code>true</code>, allows persistent instances to be written without a transaction active.
     * @return Whether we are allowing non-tx writes
     */
    boolean getNontransactionalWrite();

    /**
     * Set whether to auto-commit any non-tx writes.
     * @param autoCommit Whether to auto-commit any non-tx writes
     * @throws NucleusUserException if not supported
     */
    void setNontransactionalWriteAutoCommit(boolean autoCommit);

    /**
     * Whether to auto-commit any non-tx writes.
     * @return Whether to auto-commit any non-tx writes.
     */
    boolean getNontransactionalWriteAutoCommit();

    /**
     * If <code>true</code>, at commit instances retain their values and the instances transition to persistent-nontransactional.
     * @param retainValues the value of the retainValues property
     * @throws NucleusUserException if not supported
     */
    void setRetainValues(boolean retainValues);

    /**
     * If <code>true</code>, at commit time instances retain their field values.
     * @return the value of the retainValues property
     */
    boolean getRetainValues();

    /**
     * If <code>true</code>, at rollback, fields of newly persistent instances are restored to their values as of the beginning of 
     * the transaction, and the instances revert to transient. Additionally, fields of modified instances of primitive types and 
     * immutable reference types are restored to their values as of the beginning of the transaction.
     * <P>If <code>false</code>, at rollback, the values of fields of newly persistent instances are unchanged and the instances revert 
     * to transient. Additionally, dirty instances transition to hollow.
     * @param restoreValues the value of the restoreValues property
     * @throws NucleusUserException if not supported
     */
    void setRestoreValues(boolean restoreValues);

    /**
     * Return the current value of the restoreValues property.
     * @return the value of the restoreValues property
     */
    boolean getRestoreValues();

    /**
     * Optimistic transactions do not hold data store locks until commit time.
     * @param optimistic the value of the Optimistic flag.
     * @throws NucleusUserException if not supported
     */
    void setOptimistic(boolean optimistic);

    /**
     * Optimistic transactions do not hold data store locks until commit time.
     * @return the value of the Optimistic property.
     */
    boolean getOptimistic();

    /**
     * Mutator for whether to serialize (lock) any read objects in this transaction.
     * @param serializeRead Whether to serialise (lock) any read objects
     */
    void setSerializeRead(Boolean serializeRead);

    /**
     * Accessor for the setting for whether to serialize read objects (lock them).
     * @return the value of the serializeRead property
     */
    Boolean getSerializeRead();

    /**
     * The user can specify a <code>Synchronization</code> instance to be notified on transaction completions.
     * The <code>beforeCompletion</code> method is called prior to flushing instances to the data store.
     * <P>
     * The <code>afterCompletion</code> method is called after performing state transitions of persistent and transactional instances, 
     * following the data store commit or rollback operation.
     * <P>
     * Only one <code>Synchronization</code> instance can be registered with the <code>Transaction</code>. If the application requires 
     * more than one instance to receive synchronization callbacks, then the single application instance is responsible for managing 
     * them, and forwarding callbacks to them.
     * @param sync the <code>Synchronization</code> instance to be notified; <code>null</code> for none
     */
    void setSynchronization(Synchronization sync);

    /**
     * The user-specified <code>Synchronization</code> instance for this <code>Transaction</code> instance.
     * @return the user-specified <code>Synchronization</code> instance.
     */
    Synchronization getSynchronization();

    /**
     * Checks whether a transaction is committing.
     * @return Whether the transaction is committing
     */
    boolean isCommitting();

    /**
     * Method to register the current position as a savepoint with the provided name (assuming the datastore supports it).
     * @param name Savepoint name
     */
    void setSavepoint(String name);

    /**
     * Method to deregister the current position as a savepoint with the provided name (assuming the datastore supports it).
     * @param name Savepoint name
     */
    void releaseSavepoint(String name);

    /**
     * Method to rollback the transaction to the specified savepoint (assuming the datastore supports it).
     * @param name Savepoint name
     */
    void rollbackToSavepoint(String name);

    /**
     * Adds a transaction listener. After commit or rollback, listeners are cleared
     * @param listener The listener to add
     */
    void addTransactionEventListener(TransactionEventListener listener);

    /**
     * Removes the specified listener.
     * @param listener Listener to remove
     */
    void removeTransactionEventListener(TransactionEventListener listener);

    /**
     * Listeners that are never cleared, and invoked for all transactions
     * @param listener listener to bind
     */
    void bindTransactionEventListener(TransactionEventListener listener);

    /**
     * Obtain all settings for this Transaction
     * @return a map with settings
     */
    Map<String, Object> getOptions();

    void setOption(String option, int value);
    
    void setOption(String option, boolean value);

    void setOption(String option, String value);

    void setOption(String option, Object value);
}
