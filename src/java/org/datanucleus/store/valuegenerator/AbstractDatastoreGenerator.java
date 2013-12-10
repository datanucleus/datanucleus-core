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
 * Abstract representation of a ValueGenerator for a datastore.
 * Builds on the base AbstractValueGenerator, and providing datastore connection 
 * and StoreManager information.
 */
public abstract class AbstractDatastoreGenerator extends AbstractGenerator
{
    /** Manager for the datastore. */
    protected StoreManager storeMgr;

    /** The means of connecting to the datastore (if required by the generator). */
    protected ValueGenerationConnectionProvider connectionProvider;

    /**
     * Constructor.
     * @param name Symbolic name for the generator
     * @param props Properties controlling the behaviour of the generator
     */
    public AbstractDatastoreGenerator(String name, Properties props)
    {
        super(name, props);
        allocationSize = 1;
    }

    /**
     * Method to set the StoreManager to be used.
     * @param storeMgr The Store Manager 
     */
    public void setStoreManager(StoreManager storeMgr)
    {
        this.storeMgr = storeMgr;
    }

    /**
     * Mutator for setting the connection provider.
     * @param provider The connection provider.
     */
    public void setConnectionProvider(ValueGenerationConnectionProvider provider)
    {
        connectionProvider = provider;
    }
}