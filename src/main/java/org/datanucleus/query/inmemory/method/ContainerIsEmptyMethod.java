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
package org.datanucleus.query.inmemory.method;

import java.util.Collection;
import java.util.Map;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.inmemory.InMemoryExpressionEvaluator;
import org.datanucleus.query.inmemory.InvocationEvaluator;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the method "{contExpr}.isEmpty()".
 */
public class ContainerIsEmptyMethod implements InvocationEvaluator
{
    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        String method = expr.getOperation();

        Boolean result = null;
        if (invokedValue == null)
        {
            result = Boolean.TRUE;
        }
        else if (invokedValue instanceof Collection)
        {
            result = ((Collection)invokedValue).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (invokedValue instanceof Map)
        {
            result = ((Map)invokedValue).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
        }
        else
        {
            throw new NucleusException(Localiser.msg("021011", method, invokedValue.getClass().getName()));
        }
        return result;
    }
}