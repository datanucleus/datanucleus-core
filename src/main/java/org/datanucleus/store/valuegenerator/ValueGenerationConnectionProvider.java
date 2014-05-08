/**********************************************************************
Copyright (c) 2003 Erik Bengtson and others. All rights reserved.
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
2006 Andy Jefferson - changed to be generic connection provider
    ...
**********************************************************************/
package org.datanucleus.store.valuegenerator;

import org.datanucleus.store.connection.ManagedConnection;

/**
 * Connection provider for a ValueGenerator that requires connections to their datastore.
 */
public interface ValueGenerationConnectionProvider
{
    /**
     * Provides a Connection for a ValueGenerator. The returned connection should
     * be cast to the appropriate type for the datastore.
     * @return The connection
     */
    ManagedConnection retrieveConnection();

    /**
     * Releases the Connection. Inform to the provider that connection is no longer in use.
     */
    void releaseConnection();
}