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

import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.StringUtils;

/**
 * Abstract implementation of a ConnectionFactory for a DataNucleus supported datastore.
 */
public abstract class AbstractConnectionFactory implements ConnectionFactory
{
    protected StoreManager storeMgr;

    /** Type of resource represented by this ConnectionFactory. See ConnectionResourceType. */
    protected String resourceType;

    /** Name of this resource ("tx", "non-tx" etc). */
    protected String resourceName;

    public static final String RESOURCE_NAME_TX = "tx";

    /**
     * Constructor.
     * @param storeMgr The store manager needing the connection
     * @param resourceName Name of resource (tx, nontx)
     */
    public AbstractConnectionFactory(StoreManager storeMgr, String resourceName)
    {
        this.storeMgr = storeMgr;
        this.resourceName = resourceName;
        if (resourceName == null)
        {
            // Should never be null
        }
        else if (resourceName.equals(RESOURCE_NAME_TX))
        {
            // Transactional
            resourceType = storeMgr.getStringProperty(DATANUCLEUS_CONNECTION_RESOURCE_TYPE);
        }
        else
        {
            // Non-transactional
            resourceType = storeMgr.getStringProperty(DATANUCLEUS_CONNECTION2_RESOURCE_TYPE);
        }
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public String getResourceType()
    {
        return resourceType;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ConnectionFactory#getConnection(org.datanucleus.store.ExecutionContext, java.util.Map)
     */
    public ManagedConnection getConnection(ExecutionContext ec, org.datanucleus.Transaction txn, Map options)
    {
        ManagedConnection mconn = storeMgr.getConnectionManager().allocateConnection(this, ec, txn, options);
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
        return "ConnectionFactory:" + resourceName + "[" + StringUtils.toJVMIDString(this) + "]";
    }
}