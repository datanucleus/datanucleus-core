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

import java.util.Map;

import org.datanucleus.ExecutionContext;

/**
 * Factory for connections to the datastore.
 * To be implemented by all StoreManagers.
 */
public interface ConnectionFactory
{
    /** User-visible configuration property name */
    public static final String DATANUCLEUS_CONNECTION_RESOURCE_TYPE = "datanucleus.connection.resourceType";
    /** User-visible configuration property name */
    public static final String DATANUCLEUS_CONNECTION2_RESOURCE_TYPE = "datanucleus.connection2.resourceType";

    /**
     * Obtain a connection from the Factory. The connection will be enlisted within the transaction
     * associated to the ExecutionContext if enlist set to true.
     * @param ec ExecutionContext that the connection is bound to during its lifecycle (or null)
     * @param transaction The transaction to enlist with
     * @param options Any options for then creating the connection
     * @return the {@link org.datanucleus.store.connection.ManagedConnection}
     */
    ManagedConnection getConnection(ExecutionContext ec, org.datanucleus.Transaction transaction, Map<String, Object> options);

    /**
     * Create the ManagedConnection. 
     * Only used by ConnectionManager so do not call this.
     * @param ec ExecutionContext that the connection is bound to during its lifecycle (if any)
     * @param transactionOptions the Transaction options this connection will be enlisted to, null if non existent
     * @return The ManagedConnection.
     */
    ManagedConnection createManagedConnection(ExecutionContext ec, Map<String, Object> transactionOptions);

    /**
     * Release any resources that have been allocated.
     */
    void close();

    /**
     * Type of resource that this ConnectionFactory represents. See ConnectionResourceType.
     * @return Resource type ("JTA", "RESOURCE_LOCAL")
     */
    String getResourceType();
}