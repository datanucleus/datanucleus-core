/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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

import java.util.List;
import java.util.Optional;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.store.query.QueryUtils;
import org.datanucleus.store.query.expression.DyadicExpression;
import org.datanucleus.store.query.expression.Expression;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.Literal;
import org.datanucleus.store.query.expression.ParameterExpression;
import org.datanucleus.store.query.expression.PrimaryExpression;
import org.datanucleus.store.query.inmemory.InMemoryExpressionEvaluator;
import org.datanucleus.store.query.inmemory.InvocationEvaluator;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the method "{optionalExpr}.orElse()".
 */
public class OptionalOrElseMethod implements InvocationEvaluator
{
    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        String method = expr.getOperation();

        if (invokedValue == null)
        {
            return null;
        }
        if (!(invokedValue instanceof Optional))
        {
            throw new NucleusException(Localiser.msg("021011", method, invokedValue.getClass().getName()));
        }

        List<Expression> args = expr.getArguments();
        if (args == null || args.isEmpty() || args.size() != 1)
        {
            throw new NucleusException("orElse requires one argument");
        }

        Expression argExpr = args.get(0);
        Object argument = null;
        if (argExpr instanceof Literal)
        {
            argument = ((Literal)argExpr).getLiteral();
        }
        else if (argExpr instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)argExpr;
            argument = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (argExpr instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)argExpr;
            argument = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (argExpr instanceof InvokeExpression)
        {
            argument = eval.getValueForInvokeExpression((InvokeExpression)argExpr);
        }
        else if (argExpr instanceof DyadicExpression)
        {
            argument = ((DyadicExpression)argExpr).evaluate(eval);
        }
        else
        {
            throw new NucleusException(method + "(param1) where param is instanceof " + argExpr.getClass().getName() + " not supported");
        }
        return ((Optional)invokedValue).orElse(argument);
    }
}