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
package org.datanucleus.query.expression;

import org.datanucleus.exceptions.NucleusException;

/**
 * Exception thrown when compiling a PrimaryExpression and we find that it really represents
 * an invocation of a method, and so should be swapped in the expression tree.
 */
public class PrimaryExpressionIsInvokeException extends NucleusException
{
    /** The InvokeExpression that the PrimaryExpression should be swapped with. */
    InvokeExpression invokeExpr;

    public PrimaryExpressionIsInvokeException(InvokeExpression expr)
    {
        super("PrimaryExpression should be a InvokeExpression " + expr);
        invokeExpr = expr;
    }

    /**
     * Accessor for the InvokeExpression that this primary expression should be swapped for.
     * @return The InvokeExpression
     */
    public InvokeExpression getInvokeExpression()
    {
        return invokeExpr;
    }
}