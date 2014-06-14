/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.federation;

import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.query.AbstractJDOQLQuery;

/**
 * JDOQL query that is federated across multiple datastores
 */
public class FederatedJDOQLQuery extends AbstractJDOQLQuery
{
    private static final long serialVersionUID = 740380628222349781L;

    /**
     * Constructs a new query instance that uses the given execution context.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     */
    public FederatedJDOQLQuery(StoreManager storeMgr, ExecutionContext ec)
    {
        super(storeMgr, ec);
    }

    /**
     * Constructs a new query instance having the same criteria as the given query.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     * @param q The query from which to copy criteria.
     */
    public FederatedJDOQLQuery(StoreManager storeMgr, ExecutionContext ec, FederatedJDOQLQuery q)
    {
        super(storeMgr, ec, q);
    }

    /**
     * Constructor for a JDOQL query where the query is specified using the "Single-String" format.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     * @param query The query string
     */
    public FederatedJDOQLQuery(StoreManager storeMgr, ExecutionContext ec, String query)
    {
        super(storeMgr, ec, query);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.Query#performExecute(java.util.Map)
     */
    @Override
    protected Object performExecute(Map parameters)
    {
        // TODO Find StoreManager(s) involved in this query and distribute it
        return null;
    }
}