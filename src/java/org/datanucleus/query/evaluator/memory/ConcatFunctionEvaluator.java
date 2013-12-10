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
 * Evaluator for the function CONCAT(strExpr1, strExpr2).
 */
public class ConcatFunctionEvaluator implements InvocationEvaluator
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
            throw new NucleusException(method + "(param) where param is instanceof " + param.getClass().getName() + " not supported");
        }
        Object param2 = expr.getArguments().get(1);
        Object param2Value = null;
        if (param2 instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param;
            param2Value = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (param2 instanceof ParameterExpression)
        {
            ParameterExpression param2Expr = (ParameterExpression)param2;
            param2Value = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), param2Expr);
        }
        else if (param2 instanceof Literal)
        {
            param2Value = ((Literal)param).getLiteral();
        }
        else
        {
            throw new NucleusException(method + "(param, param2) where param2 is instanceof " + param2.getClass().getName() + " not supported");
        }

        if (paramValue == null)
        {
            return null;
        }
        return ((String)paramValue).concat((String)param2Value);
    }
}