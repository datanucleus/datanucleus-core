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
package org.datanucleus.query.inmemory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.store.query.Query;
import org.datanucleus.util.NucleusLogger;

/**
 * Class to evaluate a JPQL query in whole or part.
 */
public class JPQLInMemoryEvaluator extends JavaQueryInMemoryEvaluator
{
    /**
     * Constructor.
     * @param query The underlying JPQL query
     * @param candidates List of objects as input to the evaluation process
     * @param compilation Compiled query
     * @param parameterValues Input parameter values
     * @param clr ClassLoader resolver
     */
    public JPQLInMemoryEvaluator(Query query, Collection candidates, QueryCompilation compilation, Map parameterValues, ClassLoaderResolver clr)
    {
        super("JPQL", query, compilation, parameterValues, clr, candidates);

        if (this.parameterValues != null && this.parameterValues.size() > 0)
        {
            Set keys = this.parameterValues.keySet();
            boolean numericKeys = false;
            int origin = Integer.MAX_VALUE;
            for (Object key : keys)
            {
                if (numericKeys || key instanceof Integer)
                {
                    numericKeys = true;
                    if (((Integer)key).intValue() < origin)
                    {
                        origin = (Integer)key;
                    }
                }
            }

            if (numericKeys && origin != 0)
            {
                // When using JPA API the positional parameter values are origin 1, so swap to origin 0
                Map paramValues = new HashMap();
                Iterator entryIter = this.parameterValues.entrySet().iterator();
                while (entryIter.hasNext())
                {
                    Map.Entry entry = (Map.Entry)entryIter.next();
                    if (entry.getKey() instanceof Integer)
                    {
                        int pos = (Integer)entry.getKey();
                        paramValues.put(pos-1, entry.getValue());
                    }
                    else
                    {
                        paramValues.put(entry.getKey(), entry.getValue());
                    }
                }
                this.parameterValues = paramValues;
            }
        }

        if (compilation.getExprFrom() != null && compilation.getExprFrom().length > 0)
        {
            Expression[] fromExprs = compilation.getExprFrom();
            if (fromExprs.length > 1 || fromExprs[0].getRight() != null)
            {
                NucleusLogger.DATASTORE_RETRIEVE.warn("In-memory evaluation of query does not currently support JPQL FROM joins with aliases. This will be ignored so if depending on aliases defined in FROM then the query will fail");
            }
        }
    }

    /**
     * Method to evaluate a subquery of the query being evaluated.
     * @param query The subquery
     * @param compilation The subquery compilation
     * @param candidates The candidates for the subquery
     * @param outerCandidate Current candidate in the outer query (for use when linking back)
     * @return The result
     */
    protected Collection evaluateSubquery(Query query, QueryCompilation compilation, Collection candidates, Object outerCandidate)
    {
        JPQLInMemoryEvaluator eval = new JPQLInMemoryEvaluator(query, candidates, compilation, parameterValues, clr);
        // TODO Make use of outer candidate
        return eval.execute(true, true, true, true, true);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.JavaQueryEvaluator#execute(boolean, boolean, boolean, boolean, boolean)
     */
    @Override
    public Collection execute(boolean applyFilter, boolean applyOrdering, boolean applyResult, boolean applyResultClass, boolean applyRange)
    {
        Collection results = super.execute(applyFilter, applyOrdering, applyResult, applyResultClass, applyRange);
        if (results instanceof List)
        {
            return new InMemoryQueryResult((List)results, query.getExecutionContext().getApiAdapter());
        }
        return results;
    }

    /**
     * Constructs ResultClassMapper and calls its map function.
     * @param resultSet The resultSet containing the instances handled by setResult
     * @return The resultSet containing instances of the Class defined by setResultClass
     */
    Collection mapResultClass(Collection resultSet)
    {
        Expression[] result = compilation.getExprResult();
        return new JPQLResultClassMapper(query.getResultClass()).map(resultSet, result);
    }
}