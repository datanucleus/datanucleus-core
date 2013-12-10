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

import org.datanucleus.query.expression.InvokeExpression;

/**
 * Interface representing an evaluator for an invocation of a method/function.
 */
public interface InvocationEvaluator
{
    /**
     * Method to evaluate the InvokeExpression, as part of the overall evaluation
     * defined by the InMemoryExpressionEvaluator.
     * @param expr The expression for invocation
     * @param invokedValue Value on which we are invoking
     * @param eval The overall evaluator for in-memory
     * @return The result
     */
    Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval);
}