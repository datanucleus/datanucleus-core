/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.datanucleus.ClassConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Abstract implementation of a ConnectionFactory for a DataNucleus supported datastore.
 */
public abstract class AbstractConnectionFactory implements ConnectionFactory
{
    /** Localisation of messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", ClassConstants.NUCLEUS_CONTEXT_LOADER);

    protected StoreManager storeMgr;

    protected Map options = null;

    protected String resourceType;

    /**
     * Constructor.
     * @param storeMgr The store manager needing the connection
     * @param resourceType Type of resource (tx, nontx)
     */
    public AbstractConnectionFactory(StoreManager storeMgr, String resourceType)
    {
        this.storeMgr = storeMgr;
        this.resourceType = resourceType;
        if (resourceType == null)
        {
            // Should never be null
        }
        else if (resourceType.equals("tx"))
        {
            // Transactional
            String configuredResourceTypeProperty = storeMgr.getStringProperty(DATANUCLEUS_CONNECTION_RESOURCE_TYPE);
            if (configuredResourceTypeProperty != null)
            {
                if (options == null)
                {
                    options = new HashMap();
                }
                options.put(ConnectionFactory.RESOURCE_TYPE_OPTION, configuredResourceTypeProperty);
            }
        }
        else
        {
            // Non-transactional
            String configuredResourceTypeProperty = storeMgr.getStringProperty(DATANUCLEUS_CONNECTION2_RESOURCE_TYPE);
            if (configuredResourceTypeProperty!=null)
            {
                if (options == null)
                {
                    options = new HashMap();
                }
                options.put(ConnectionFactory.RESOURCE_TYPE_OPTION, configuredResourceTypeProperty);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionFactory#getConnection(org.datanucleus.store.ExecutionContext, java.util.Map)
     */
    public ManagedConnection getConnection(ExecutionContext ec, org.datanucleus.Transaction txn, Map options)
    {
        Map addedOptions = new HashMap();
        if (options != null)
        {
            addedOptions.putAll(options);
        }
        if (this.options != null)
        {
            addedOptions.putAll(this.options);
        }

        ManagedConnection mconn = storeMgr.getConnectionManager().allocateConnection(this, ec, txn, addedOptions);
        ((AbstractManagedConnection)mconn).incrementUseCount();
        return mconn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionFactory#close()
     */
    public void close()
    {
    }

    /**
     * Method to return a string form of this object for convenience debug.
     * @return The String form
     */
    public String toString()
    {
        return "ConnectionFactory:" + resourceType + "[" + StringUtils.toJVMIDString(this) + "]";
    }
}