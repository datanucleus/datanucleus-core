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
package org.datanucleus.query.evaluator.memory;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the function MOD(numExpr1, numExpr2).
 */
public class ModFunctionEvaluator implements InvocationEvaluator
{
    /** Localisation utility for output messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        String method = expr.getOperation();
        Object param1 = expr.getArguments().get(0);
        int param1Value = -1;
        if (param1 instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param1;
            Object val = eval.getValueForPrimaryExpression(primExpr);
            if (val instanceof Number)
            {
                param1Value = ((Number)val).intValue();
            }
            else
            {
                throw new NucleusException(method + "(num1, num2) where num1 is instanceof " + param1.getClass().getName() + " but should be integer");
            }
        }
        else if (param1 instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)param1;
            Object val = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
            if (val instanceof Number)
            {
                param1Value = ((Number)val).intValue();
            }
            else
            {
                throw new NucleusException(method + "(num1, num2) where num1 is instanceof " + param1.getClass().getName() + " but should be integer");
            }
        }
        else if (param1 instanceof Literal)
        {
            Object val = ((Literal)param1).getLiteral();
            if (val instanceof Number)
            {
                param1Value = ((Number)val).intValue();
            }
            else
            {
                throw new NucleusException(method + "(num1, num2) where num1 is instanceof " + param1.getClass().getName() + " but should be integer");
            }
        }
        else
        {
            throw new NucleusException(method + "(num1, num2) where num1 is instanceof " + param1.getClass().getName() + " not supported");
        }

        Object param2 = expr.getArguments().get(1);
        int param2Value = -1;
        if (param2 instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param2;
            Object val = eval.getValueForPrimaryExpression(primExpr);
            if (val instanceof Number)
            {
                param2Value = ((Number)val).intValue();
            }
            else
            {
                throw new NucleusException(method + "(num1, num2) where num2 is instanceof " + param2.getClass().getName() + " but should be integer");
            }
        }
        else if (param2 instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)param2;
            Object val = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
            if (val instanceof Number)
            {
                param2Value = ((Number)val).intValue();
            }
            else
            {
                throw new NucleusException(method + "(num1, num2) where num1 is instanceof " + param2.getClass().getName() + " but should be integer");
            }
        }
        else if (param2 instanceof Literal)
        {
            Object val = ((Literal)param2).getLiteral();
            if (val instanceof Number)
            {
                param2Value = ((Number)val).intValue();
            }
            else
            {
                throw new NucleusException(method + "(num1, num2) where num2 is instanceof " + param2.getClass().getName() + " but should be integer");
            }
        }
        else
        {
            throw new NucleusException(method + "(num1, num2) where num2 is instanceof " + param2.getClass().getName() + " not supported");
        }

        return Integer.valueOf(param1Value%param2Value);
    }
}