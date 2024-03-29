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
package org.datanucleus.store.query.inmemory.method;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.QueryLanguage;
import org.datanucleus.store.query.QueryUtils;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.Literal;
import org.datanucleus.store.query.expression.ParameterExpression;
import org.datanucleus.store.query.expression.PrimaryExpression;
import org.datanucleus.store.query.inmemory.InMemoryExpressionEvaluator;
import org.datanucleus.store.query.inmemory.InvocationEvaluator;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Evaluator for the method "{stringExpr}.matches(expr)".
 */
public class StringMatchesMethod implements InvocationEvaluator
{
    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        String method = expr.getOperation();

        if (invokedValue == null)
        {
            return Boolean.FALSE;
        }
        if (!(invokedValue instanceof String))
        {
            throw new NucleusException(Localiser.msg("021011", method, invokedValue.getClass().getName()));
        }

        String arg = null;
        Object argObj = null;
        Object param = expr.getArguments().get(0);
        if (expr.getArguments().size() > 1)
        {
            NucleusLogger.QUERY.info("Please note that any escape character is currently ignored");
            // TODO Cater for optional second argument that is the escape character
        }
        if (param instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param;
            argObj = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (param instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)param;
            argObj = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (param instanceof Literal)
        {
            argObj = ((Literal)param).getLiteral();
        }
        else if (param instanceof InvokeExpression)
        {
            argObj = eval.getValueForInvokeExpression((InvokeExpression) param);
        }
        else
        {
            throw new NucleusException(method + "(param) where param is instanceof " + param.getClass().getName() + " not supported");
        }

        arg = QueryUtils.getStringValue(argObj);
        if (eval.getQueryLanguage().equals(QueryLanguage.JPQL.name()))
        {
            // Convert JPQL like expression to String.matches input
            String matchesArg = arg;
            matchesArg = StringUtils.replaceAll(matchesArg, "%", ".*");
            matchesArg = matchesArg.replace('_', '.');
            arg = matchesArg;
        }

        return ((String)invokedValue).matches(arg) ? Boolean.TRUE : Boolean.FALSE;
    }
}