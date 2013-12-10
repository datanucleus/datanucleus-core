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
 * Evaluator for the function LOCATE(strExpr1, strExpr2, pos).
 */
public class LocateFunctionEvaluator implements InvocationEvaluator
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
        Object param = expr.getArguments().get(0);
        Object paramValue = null;
        if (param instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param;
            paramValue = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (param instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)param;
            paramValue = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (param instanceof Literal)
        {
            paramValue = ((Literal)param).getLiteral();
        }
        else
        {
            throw new NucleusException(method + "(str1, str2, pos) where str1 is instanceof " + param.getClass().getName() + " not supported");
        }
        if (paramValue == null)
        {
            return Integer.valueOf(-1);
        }

        Object locStr = expr.getArguments().get(1);
        String locStrValue = null;
        if (locStr instanceof Literal)
        {
            locStrValue = (String)((Literal)locStr).getLiteral();
        }
        else
        {
            throw new NucleusException(method + "(str, str2, pos) where str2 is instanceof " + locStr.getClass().getName() + " not supported");
        }

        if (expr.getArguments().size() == 3)
        {
            Object pos = expr.getArguments().get(2);
            int num2Value = -1;
            if (pos instanceof Literal)
            {
                num2Value = eval.getIntegerForLiteral((Literal)pos);
            }
            else
            {
                throw new NucleusException(method + "(str, str2, pos) where pos is instanceof " + pos.getClass().getName() + " not supported");
            }
            return Integer.valueOf(((String)paramValue).indexOf(locStrValue, num2Value));
        }
        else
        {
            return Integer.valueOf(((String)paramValue).indexOf(locStrValue));
        }
    }
}