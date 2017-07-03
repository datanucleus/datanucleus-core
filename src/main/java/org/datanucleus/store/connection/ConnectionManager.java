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
2007 Andy Jefferson - javadocs, lock/unlock/has methods
    ...
**********************************************************************/
package org.datanucleus.store.connection;

import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.Transaction;
import org.datanucleus.exceptions.NucleusException;

/**
 * Manager of connections for a StoreManager, allowing ManagedConnection pooling, enlistment in transaction.
 * The pool caches one connection per ExecutionContext.
 * The <i>allocateConnection</i> method can create connections and enlist them (like most normal persistence 
 * operations need) or create a connection and return it without enlisting it into a transaction, for example the 
 * connections used to generate object identity, create the database schema or obtaining the schema metadata.
 * The <i>closeAllConnections</i> method is typically called when the owning object (ExecutionContext) is being closed
 * giving us chance to close all retained connections for that ExecutionContext.
 * <p>
 * Connections can be locked per ExecutionContext basis. Locking of connections is used to
 * handle the connection over to the user application. A locked connection denies any further
 * access to the datastore, until the user application unlock it.
 * </p>
 */
public interface ConnectionManager
{
    /**
     * Method to close the connection manager. This will close all open connections.
     */
    void close();

    /**
     * Disable binding objects to "ExecutionContext" references, so automatically disables the connection pooling 
     */
    void disableConnectionPool();

    /**
     * Accessor for a connection for the specified ExecutionContext.
     * If there is an active transaction, a connection from the primary connection factory will be returned. 
     * If there is no active transaction, a connection from the secondary connection factory will be returned (unless the user has specified to just use the primary).
     * @param ec execution context
     * @return The ManagedConnection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    default ManagedConnection getConnection(ExecutionContext ec)
    {
        return getConnection(ec, null);
    }

    /**
     * Accessor for a connection for the specified ExecutionContext.
     * If there is an active transaction, a connection from the primary connection factory will be returned. 
     * If there is no active transaction, a connection from the secondary connection factory will be returned (unless the user has specified to just use the primary).
     * @param ec execution context
     * @param options connection options
     * @return The ManagedConnection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    ManagedConnection getConnection(ExecutionContext ec, Map options);

    /**
     * Accessor for a connection for the specified transaction isolation level.
     * This is used for schema and sequence access operations.
     * @param isolationLevel Isolation level (-1 implies use the default for the datastore).
     * @return The ManagedConnection
     * @throws NucleusException Thrown if an error occurs getting the connection
     */
    ManagedConnection getConnection(int isolationLevel);

    /**
     * Accessor for a connection from the specified factory, for the specified ExecutionContext dependent on whether the connection will be enlisted.
     * @param primary Whether to take use the "primary" connection factory, otherwise takes the "secondary"
     * @param ec ExecutionContext
     * @param txn The Transaction
     * @return The ManagedConnection
     */
    ManagedConnection getConnection(boolean primary, ExecutionContext ec, Transaction txn);

    /**
     * Method to close all pooled connections for the specified ExecutionContext.
     * @param ec The ExecutionContext
     */
    void closeAllConnections(final ExecutionContext ec);
}