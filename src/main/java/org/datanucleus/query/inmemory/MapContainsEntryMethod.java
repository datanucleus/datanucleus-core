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

import java.util.Map;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.VariableExpression;

/**
 * Evaluator for the method "{mapExpr}.containsEntry(keyExpr,valueExpr)".
 */
public class MapContainsEntryMethod implements InvocationEvaluator
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
        if (!(invokedValue instanceof Map))
        {
            throw new NucleusException(eval.getLocaliser().msg("021011", method, invokedValue.getClass().getName()));
        }

        Object keyParam = expr.getArguments().get(0);
        Object valParam = expr.getArguments().get(1);

        Object keyValue = null;
        if (keyParam instanceof Literal)
        {
            keyValue = ((Literal)keyParam).getLiteral();
        }
        else if (keyParam instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)keyParam;
            keyValue = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (keyParam instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)keyParam;
            keyValue = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (keyParam instanceof VariableExpression)
        {
            VariableExpression varExpr = (VariableExpression)keyParam;
            try
            {
                keyValue = eval.getValueForVariableExpression(varExpr);
            }
            catch (VariableNotSetException vnse)
            {
                // Throw an exception with the possible values of values
                throw new VariableNotSetException(varExpr, ((Map)invokedValue).values().toArray());
            }
        }
        else
        {
            throw new NucleusException("Dont currently support use of containsEntry(" + 
                keyParam.getClass().getName() + ",?)");
        }

        Object valValue = null;
        if (valParam instanceof Literal)
        {
            valValue = ((Literal)valParam).getLiteral();
        }
        else if (keyParam instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)valParam;
            valValue = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (keyParam instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)valParam;
            valValue = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (keyParam instanceof VariableExpression)
        {
            VariableExpression varExpr = (VariableExpression)valParam;
            try
            {
                valValue = eval.getValueForVariableExpression(varExpr);
            }
            catch (VariableNotSetException vnse)
            {
                // Throw an exception with the possible values of values
                throw new VariableNotSetException(varExpr, ((Map)invokedValue).values().toArray());
            }
        }
        else
        {
            throw new NucleusException("Dont currently support use of containsEntry(?," + 
                    valParam.getClass().getName() + ")");
        }

        Map invokedMap = (Map)invokedValue;
        if (invokedMap.containsKey(keyValue))
        {
            Object currentValForKey = invokedMap.get(keyValue);
            if (currentValForKey.equals(valValue))
            {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}