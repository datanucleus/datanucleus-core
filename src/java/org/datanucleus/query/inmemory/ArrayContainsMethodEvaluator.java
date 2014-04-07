/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.lang.reflect.Array;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.VariableExpression;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the method "{arrayExpr}.contains(elemExpr)".
 */
public class ArrayContainsMethodEvaluator implements InvocationEvaluator
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

        if (invokedValue == null)
        {
            return Boolean.FALSE;
        }
        if (!invokedValue.getClass().isArray())
        {
            throw new NucleusException(LOCALISER.msg("021011", method, invokedValue.getClass().getName()));
        }

        Object param = expr.getArguments().get(0);
        Object paramValue = null;
        if (param instanceof Literal)
        {
            paramValue = ((Literal)param).getLiteral();
        }
        else if (param instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param;
            paramValue = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (param instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)param;
            paramValue = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (param instanceof VariableExpression)
        {
            VariableExpression varExpr = (VariableExpression)param;
            try
            {
                paramValue = eval.getValueForVariableExpression(varExpr);
            }
            catch (VariableNotSetException vnse)
            {
                // Throw an exception with the possible values
                throw new VariableNotSetException(varExpr, (Object[]) invokedValue);
            }
        }
        else
        {
            // TODO Implement this
            throw new NucleusException("Dont currently support use of Array.contains(" + param.getClass().getName() + ")");
        }

        for (int i=0;i<Array.getLength(invokedValue);i++)
        {
            Object elem = Array.get(invokedValue, i);
            if (elem == null && paramValue == null)
            {
                return Boolean.TRUE;
            }
            else if (elem != null && paramValue != null)
            {
                if (elem.equals(paramValue))
                {
                    return Boolean.TRUE;
                }
                else
                {
                    if (!paramValue.getClass().isAssignableFrom(elem.getClass()) &&
                        !elem.getClass().isAssignableFrom(paramValue.getClass()))
                    {
                        // Types are different, so add specific type conversion handling
                        if ((paramValue.getClass() == Long.class || 
                             paramValue.getClass() == Integer.class ||
                             paramValue.getClass() == Short.class) &&
                            (elem.getClass() == Long.class ||
                             elem.getClass() == Integer.class ||
                             elem.getClass() == Short.class))
                        {
                            long paramLong = ((Number)paramValue).longValue();
                            long elemLong = ((Number)elem).longValue();
                            if (paramLong == elemLong)
                            {
                                return Boolean.TRUE;
                            }
                        }
                    }
                }
            }
        }
        return Boolean.FALSE;
    }
}