/**********************************************************************
Copyright (c) 2008 Erik Bengtson and others. All rights reserved.
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
2008 Andy Jefferson - restructured to take in applyXXX flags and evaluate different parts if required
    ...
**********************************************************************/
package org.datanucleus.query.inmemory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.store.query.Query;

/**
 * Class to evaluate a JDOQL query in whole or part.
 */
public class JDOQLInMemoryEvaluator extends JavaQueryInMemoryEvaluator
{
    /**
     * Constructor.
     * @param query The underlying JDOQL query
     * @param candidates List of objects as input to the evaluation process
     * @param compilation Query compilation
     * @param parameterValues Input parameter values keyed by the param name
     * @param clr ClassLoader resolver
     */
    public JDOQLInMemoryEvaluator(Query query, Collection candidates, QueryCompilation compilation, 
            Map parameterValues, ClassLoaderResolver clr)
    {
        super("JDOQL", query, compilation, parameterValues, clr, candidates);
    }

    /**
     * Method to evaluate a subquery of the query being evaluated.
     * @param query The subquery
     * @param compilation The subquery compilation
     * @param candidates The candidates for the subquery
     * @param outerCandidate Current candidate in the outer query (for use when linking back)
     * @return The result
     */
    protected Collection evaluateSubquery(Query query, QueryCompilation compilation, Collection candidates,
            Object outerCandidate)
    {
        JDOQLInMemoryEvaluator eval = new JDOQLInMemoryEvaluator(query, candidates, compilation, parameterValues, clr);
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
     * Constructs ResultClassMapper and calls its map function
     * @param resultSet The resultSet containing the instances handled by setResult
     * @return The resultSet containing instances of the Class defined by setResultClass
     */
    Collection mapResultClass(Collection resultSet)
    {
        Expression[] result = compilation.getExprResult();
        return new JDOQLResultClassMapper(query.getResultClass()).map(resultSet, result);
    }
}