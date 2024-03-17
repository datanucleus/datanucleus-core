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
package org.datanucleus.store.connection;

import javax.transaction.xa.XAResource;

/**
 * Wrapper for a connection to the datastore, allowing management.
 * A connection is handed out using <pre>getConnection</pre>, and returned using <pre>release</pre>.
 */
public interface ManagedConnection
{
    /**
     * Accessor for the datastore connection.
     * @return The underlying connection for this datastore
     */
    Object getConnection();

    /**
     * Implementations may override this method to keep track of the number of times the connection is used.
     * Method is called when a connection has been handed out to a user.
     * Implementation should then decrement the use count when the connection is released.
     */
    default void incrementUseCount() {}

    /**
     * Method to release the datastore connection back. Will have been handed out with a getConnection().
     * This may trigger a commit() of the connection depending on its operating mode at the time.
     */
    void release();

    /**
     * Flush the connection. It must invoke the operation
     * {@link ManagedConnectionResourceListener#transactionFlushed()}
     */
    void transactionFlushed();

    /**
     * Prepare the connection for end of transaction. It must invoke the operation
     * {@link ManagedConnectionResourceListener#transactionPreClose()}
     */
    void transactionPreClose();

    /**
     * Close the connection to the datastore. It most invoke the operations
     * {@link ManagedConnectionResourceListener#managedConnectionPreClose()} and
     * {@link ManagedConnectionResourceListener#managedConnectionPostClose()}.
     * The listeners are unregistered after this method is invoked.
     */
    void close();

    void setCommitOnRelease(boolean commit);

    void setCloseOnRelease(boolean close);

    boolean commitOnRelease();

    boolean closeOnRelease();

    /**
     * An XAResoure for this datastore connection.
     * Returns null if the connection is not usable in an XA sense
     * @return The XAResource
     */
    XAResource getXAResource();

    boolean isLocked();

    void lock();

    void unlock();

    /**
     * Registers a ManagedConnectionResourceListener to be notified of events.
     * @param listener The listener
     */
    void addListener(ManagedConnectionResourceListener listener);

    /**
     * Deregister a ManagedConnectionResourceListener.
     * @param listener The listener
     */
    void removeListener(ManagedConnectionResourceListener listener);

    /**
     * Convenience method for whether this connection should be closed after the end of transaction.
     * In DN 2.x, 3.0, 3.1 this was always true, and a connection lasted until txn commit, and then
     * had to get a new connection. In DN 3.2+ this is configurable per datastore connection factory. 
     * @return Whether the ConnectionManager should call close() on it when a txn ends
     */
    boolean closeAfterTransactionEnd();

    /**
     * Set this position in the txn as a savepoint with the provided name (if supported, otherwise do nothing).
     * @param name Name of savepoint
     */
    void setSavepoint(String name);

    /**
     * Release the named savepoint (or do nothing if not supported).
     * @param name Name of savepoint
     */
    void releaseSavepoint(String name);

    /**
     * Rollback the connection to the named savepoint (or do nothing if not supported).
     * @param name Name of savepoint
     */
    void rollbackToSavepoint(String name);
}