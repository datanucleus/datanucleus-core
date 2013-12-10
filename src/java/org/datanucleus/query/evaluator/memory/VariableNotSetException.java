/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.query.expression.VariableExpression;

/**
 * Exception representing a variable not having its value currently set in the query.
 */
public class VariableNotSetException extends RuntimeException
{
    protected VariableExpression varExpr = null;
    protected Object[] variableValues = null;

    /**
     * Constructor when we don't know the possible values.
     * @param varExpr The variable expression
     */
    public VariableNotSetException(VariableExpression varExpr)
    {
        this.varExpr = varExpr;
    }

    /**
     * Constructor when we know the possible variable values.
     * @param values The values
     */
    public VariableNotSetException(VariableExpression varExpr, Object[] values)
    {
        this.varExpr = varExpr;
        this.variableValues = values;
    }

    /**
     * Accessor for the variable expression that is not set.
     * @return The variable expression
     */
    public VariableExpression getVariableExpression()
    {
        return varExpr;
    }

    /**
     * Accessor for the possible variable values (if known).
     * @return The values
     */
    public Object[] getValues()
    {
        return variableValues;
    }
}