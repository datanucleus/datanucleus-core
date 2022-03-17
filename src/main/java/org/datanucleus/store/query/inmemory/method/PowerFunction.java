/**********************************************************************
Copyright (c) 2022 Andy Jefferson and others. All rights reserved.
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
 * Evaluator for the function POWER(numExpr, numExpr2).
 */
public class PowerFunction implements InvocationEvaluator
{
    @Override
    public Object evaluate(InvokeExpression expr, Object ignored, InMemoryExpressionEvaluator eval)
    {
        List<Expression> args = expr.getArguments();
        if (args == null || args.isEmpty())
        {
            throw new NucleusException("POWER requires two arguments");
        }
        else if (args.size() == 1)
        {
            return getValueForArgExpression(args.get(0), eval);
        }

        // Number to use power on (TODO Check for double)
        Expression argExpr1 = args.get(0);
        Object argValue1 = getValueForArgExpression(argExpr1, eval);
        Double dblValue = (Double)argValue1;

        // Power to raise the number to (TODO Check for number)
        Expression argExpr2 = args.get(1);
        Object argValue2 = getValueForArgExpression(argExpr2, eval);
        Double power = ((Number)argValue2).doubleValue();

        return Math.pow(dblValue, power);
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
            throw new NucleusException("Don't support ROUND with argument of type " + argExpr.getClass().getName());
        }

        return argValue;
    }
}