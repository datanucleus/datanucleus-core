/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.store.query.QueryUtils;
import org.datanucleus.store.query.expression.Expression;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.Literal;
import org.datanucleus.store.query.expression.ParameterExpression;
import org.datanucleus.store.query.expression.PrimaryExpression;
import org.datanucleus.store.query.inmemory.InMemoryExpressionEvaluator;
import org.datanucleus.store.query.inmemory.InvocationEvaluator;

/**
 * Evaluator for the function NULLIF(numExpr, numExpr2).
 * Returns null if the args are equal, otherwise returns the first arg.
 */
public class NullIfFunction implements InvocationEvaluator
{
    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object ignored, InMemoryExpressionEvaluator eval)
    {
        List<Expression> args = expr.getArguments();
        if (args == null || args.isEmpty())
        {
            throw new NucleusException("NULLIF requires two arguments");
        }
        else if (args.size() == 1)
        {
            return getValueForArgExpression(args.get(0), eval);
        }

        Expression argExpr1 = args.get(0);
        Expression argExpr2 = args.get(1);
        Object argValue1 = getValueForArgExpression(argExpr1, eval);
        Object argValue2 = getValueForArgExpression(argExpr2, eval);
        if (argValue1 == argValue2)
        {
            return null;
        }
        return argValue1;
    }

    protected Object getValueForArgExpression(Expression argExpr, InMemoryExpressionEvaluator eval)
    {
        Object argValue = null;
        if (argExpr instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)argExpr;
            argValue = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (argExpr instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)argExpr;
            argValue = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (argExpr instanceof Literal)
        {
            argValue = ((Literal)argExpr).getLiteral();
        }
        else
        {
            throw new NucleusException("Don't support NULLIF with argument of type " + argExpr.getClass().getName());
        }

        return argValue;
    }
}