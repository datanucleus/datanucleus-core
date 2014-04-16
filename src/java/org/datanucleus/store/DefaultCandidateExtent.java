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
package org.datanucleus.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchPlan;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.query.QueryResult;

/**
 * Extent that does a simple JDOQL query for the candidate with/without subclasses.
 */
public class DefaultCandidateExtent<T> extends AbstractExtent<T>
{
    /** FetchPlan for use with this Extent. */
    private FetchPlan fetchPlan = null;

    /** Underlying query for getting the Extent. */
    private Query query;

    /** Map of the iterators of the Extents accessed. */
    protected Map<Iterator, QueryResult> queryResultsByIterator = new HashMap();

    /**
     * Constructor.
     * @param ec execution context
     * @param cls candidate class
     * @param subclasses Whether to include subclasses
     * @param cmd MetaData for the candidate class
     */
    public DefaultCandidateExtent(ExecutionContext ec, Class<T> cls, boolean subclasses, AbstractClassMetaData cmd)
    {
        super(ec, cls, subclasses, cmd);

        query = ec.newQuery();
        fetchPlan = query.getFetchPlan();
        query.setCandidateClass(cls);
        query.setSubclasses(subclasses);
    }

    public Iterator<T> iterator()
    {
        Object results = query.execute();
        Iterator iter = null;
        if (results instanceof QueryResult)
        {
            QueryResult qr = (QueryResult)results;
            iter = qr.iterator();
            queryResultsByIterator.put(iter, qr);
        }
        else
        {
            iter = ((Collection)results).iterator();
        }

        return iter;
    }

    public boolean hasSubclasses()
    {
        return subclasses;
    }

    public ExecutionContext getExecutionContext()
    {
        return ec;
    }

    public FetchPlan getFetchPlan()
    {
        return fetchPlan;
    }

    public void closeAll()
    {
        queryResultsByIterator.clear();
        query.closeAll();

        // Clear out the fetch groups since no longer needed
        fetchPlan.clearGroups().addGroup(FetchPlan.DEFAULT);
    }

    public void close(Iterator<T> iterator)
    {
        QueryResult qr = queryResultsByIterator.remove(iterator);
        if (qr != null)
        {
            query.close(qr);
        }
    }
}