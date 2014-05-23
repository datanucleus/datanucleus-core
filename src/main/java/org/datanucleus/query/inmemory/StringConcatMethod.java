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

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the method "{stringExpr}.concat(extraStr)".
 */
public class StringConcatMethod implements InvocationEvaluator
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
        if (!(invokedValue instanceof String))
        {
            throw new NucleusException(Localiser.msg("021011", method, invokedValue.getClass().getName()));
        }

        Expression arg0Expr = expr.getArguments().get(0);
        Object arg0Val = null;
        if (arg0Expr instanceof PrimaryExpression)
        {
            arg0Val = eval.getValueForPrimaryExpression((PrimaryExpression)arg0Expr);
        }
        else if (arg0Expr instanceof ParameterExpression)
        {
            arg0Val = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), (ParameterExpression)arg0Expr);
        }
        else if (arg0Expr instanceof Literal)
        {
            arg0Val = ((Literal)arg0Expr).getLiteral();
        }
        else if (arg0Expr instanceof DyadicExpression)
        {
            arg0Val = ((DyadicExpression)arg0Expr).evaluate(eval);
        }

        if (!(arg0Val instanceof String))
        {
            throw new NucleusException(method + "(param1) : param1 must be String");
        }

        return ((String)invokedValue).concat((String)arg0Val);
    }
}