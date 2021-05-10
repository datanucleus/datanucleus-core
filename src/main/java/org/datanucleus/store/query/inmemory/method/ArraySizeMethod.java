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
package org.datanucleus.store.query.inmemory.method;

import java.lang.reflect.Array;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.inmemory.InMemoryExpressionEvaluator;
import org.datanucleus.store.query.inmemory.InvocationEvaluator;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the method "{arrayExpr}.size()".
 * NOTE : obviously this "size()" doesn't exist in Java (should be "length"), but added for convenience.
 */
public class ArraySizeMethod implements InvocationEvaluator
{
    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        String method = expr.getOperation();

        Integer result = null;
        if (invokedValue == null)
        {
            result = Integer.valueOf(0);
        }
        else if (invokedValue.getClass().isArray())
        {
            result = Array.getLength(invokedValue);
        }
        else
        {
            throw new NucleusException(Localiser.msg("021011", method, invokedValue.getClass().getName()));
        }
        return result;
    }
}