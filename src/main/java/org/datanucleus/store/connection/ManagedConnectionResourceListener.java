/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

/**
 * Listener for the relation between a ManagedConnection and a resource using that ManagedConnection.
 * The resource often needs to know when the ManagedConnection is going to be closed. Similarly the
 * ManagedConnection may need to know when the resource is being closed (so it can free up resources).
 */
public interface ManagedConnectionResourceListener
{
    /**
     * Transaction being flushed. Can be invoked multiple times during the lifecycle of the {@link ManagedConnection}
     */
    void transactionFlushed();

    /**
     * Transaction about to be committed/rolled-back. Opportunity to make final use of the connection.
     */
    void transactionPreClose();

    /**
     * Method invoked when the managed connection is about to be closed.
     * Allows the resource to finish its use of the managed connection.
     */
    void managedConnectionPreClose();

    /**
     * Method invoked when the managed connection has just been closed.
     */
    void managedConnectionPostClose();

    /**
     * Method invoked when the resource has been closed.
     * Allows deregistering of this listener from the managed connection.
     */
    void resourcePostClose();
}