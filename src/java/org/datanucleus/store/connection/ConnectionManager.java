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
     * Method to lookup a connection factory and create it if not yet existing.
     * @param name The lookup name "e.g "jdbc/tx"
     * @return The connection factory
     */
    public ConnectionFactory lookupConnectionFactory(String name);

    /**
     * Method to register a connection factory
     * @param name The lookup name "e.g "jdbc/tx"
     * @param factory The connection factory
     */
    public void registerConnectionFactory(String name, ConnectionFactory factory);

    /**
     * Method to close all pooled connections for the specified key of the specified factory.
     * @param factory The factory
     * @param ec The key in the pool
     */
    public void closeAllConnections(final ConnectionFactory factory, final ExecutionContext ec);

    /**
     * Allocate a connection using the specified factory (unless we already have one cached for the ExecutionContext).
     * @param factory The ConnectionFactory to create any new connection with
     * @param ec ExecutionContext that binds the connection during its lifetime (key in the pool)
     * @param options Any options for allocating the connection (e.g isolation)
     * @return The ManagedConnection
     */
    public ManagedConnection allocateConnection(ConnectionFactory factory, final ExecutionContext ec, 
            org.datanucleus.Transaction tx, Map options);
    
    /**
     * Disable binding objects to "ExecutionContext" references, so automatically
     * disables the connection pooling 
     */
    void disableConnectionPool();
}