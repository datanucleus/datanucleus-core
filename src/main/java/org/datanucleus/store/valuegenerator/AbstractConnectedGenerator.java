/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.store.valuegenerator;

import java.util.Properties;

import org.datanucleus.store.StoreManager;

/**
 * Abstract representation of a ValueGenerator requiring a connection to a datastore.
 * Builds on the base AbstractGenerator, providing datastore connection information.
 */
public abstract class AbstractConnectedGenerator<T> extends AbstractGenerator<T>
{
    /** The means of connecting to the datastore (if required by the generator). */
    protected ValueGenerationConnectionProvider connectionProvider;

    /**
     * Constructor.
     * @param storeMgr Store Manager
     * @param name Symbolic name for the generator
     * @param props Properties controlling the behaviour of the generator
     */
    public AbstractConnectedGenerator(StoreManager storeMgr, String name, Properties props)
    {
        super(storeMgr, name, props);
        allocationSize = 1;
    }

    /**
     * Mutator for setting the connection provider.
     * @param provider The connection provider.
     */
    public void setConnectionProvider(ValueGenerationConnectionProvider provider)
    {
        connectionProvider = provider;
    }

    public enum ConnectionPreference
    {
        NONE,
        EXISTING,
        NEW
    }

    /**
     * Accessor for any requirement for connection used by this value generator.
     * EXISTING means use the same connection as the ExecutionContext is using.
     * NEW means use a new connection, and commit it after any operation.
     * NONE means use NEW and allow override by the persistence property "datanucleus.valuegeneration.transactionAttribute".
     * @return The connection preference
     */
    public ConnectionPreference getConnectionPreference()
    {
        return ConnectionPreference.NONE;
    }
}