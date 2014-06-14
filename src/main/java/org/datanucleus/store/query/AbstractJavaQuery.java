/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.store.Extent;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of a Java-based query.
 * To be extended by Java-based query languages.
 */
public abstract class AbstractJavaQuery extends Query
{
    private static final long serialVersionUID = 7429197167814283812L;

    /** Collection of candidates for this query. */
    protected transient Collection candidateCollection = null;

    /** Cached form of the single string form of the query. */
    protected String singleString = null;

    /**
     * Constructor for a Java-based query.
     * @param storeMgr StoreManager for this query
     * @param ec ExecutionContext
     */
    public AbstractJavaQuery(StoreManager storeMgr, ExecutionContext ec)
    {
        super(storeMgr, ec);
    }

    /**
     * Set the candidate Extent to query.
     * Passing in null clears off the current candidate Extent.
     * @param pcs the Candidate Extent.
     */
    public void setCandidates(Extent pcs)
    {
        discardCompiled();
        assertIsModifiable();

        if (pcs != null)
        {
            // An Extent is equivalent to a candidate and a subclass flag
            setCandidateClass(pcs.getCandidateClass());
            setSubclasses(pcs.hasSubclasses());
        }
        candidateCollection = null;
    }

    /**
     * Set the candidate collection to query.
     * Passing in null clears off the current candidate collection.
     * @param pcs the Candidate collection.
     */
    public void setCandidates(Collection pcs)
    {
        discardCompiled();
        assertIsModifiable();

        candidateCollection = pcs;
    }

    /**
     * Method to discard our current compiled query due to changes.
     * @see org.datanucleus.store.query.Query#discardCompiled()
     */
    protected void discardCompiled()
    {
        super.discardCompiled();
        singleString = null;
    }

    /**
     * Method to generate the generic compilation of this query.
     * @param parameterValues Values for any parameters
     */
    public abstract void compileGeneric(Map parameterValues);

    /**
     * Execute the query to delete persistent objects.
     * @param parameters the Map containing all of the parameters.
     * @return the number of deleted objects.
     */
    protected long performDeletePersistentAll(Map parameters)
    {
        if (candidateCollection != null && candidateCollection.isEmpty())
        {
            // Candidates specified, but was empty so nothing to delete
            return 0;
        }

        return super.performDeletePersistentAll(parameters);
    }

    /**
     * Accessor for a single string form of the query.
     * @return Single string form of the query.
     */
    public abstract String getSingleStringQuery();

    /**
     * Stringifier method
     * @return Single-string form of this query.
     */
    public String toString()
    {
        return getSingleStringQuery();
    }

    /**
     * Convenience method to return whether the query should be evaluated in-memory.
     * @return Use in-memory evaluation?
     */
    protected boolean evaluateInMemory()
    {
        return getBooleanExtensionProperty(EXTENSION_EVALUATE_IN_MEMORY, false);
    }

	/**
     * Method to expand the subqueries defined in a filter.
     * @param input the input string
     * @return The filter expanded
     */
    protected String dereferenceFilter(String input)
    {
        if (subqueries == null)
        {
            return input;
        }

        String output = input;
        Iterator subqueryIter = subqueries.entrySet().iterator();
        while (subqueryIter.hasNext())
        {
            Map.Entry<String, SubqueryDefinition> entry = (Map.Entry) subqueryIter.next();
            SubqueryDefinition subqueryDefinition = entry.getValue();
            AbstractJavaQuery subquery = (AbstractJavaQuery) subqueryDefinition.getQuery();
            output = StringUtils.replaceAll(output, entry.getKey(), "("+subquery.getSingleStringQuery()+")");
        }
        return output;
    }
}