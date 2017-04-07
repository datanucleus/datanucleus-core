/**********************************************************************
Copyright (c) 2017 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.query.compiler.Symbol;
import org.datanucleus.query.compiler.SymbolTable;

/**
 * Expression representing a the type of the contained expression.
 */
public class TypeExpression extends Expression
{
    private static final long serialVersionUID = -7123407498309440027L;

    Expression containedExpression;

    public TypeExpression(Expression containedExpr)
    {
        this.containedExpression = containedExpr;
    }

    public Expression getContainedExpression()
    {
        return containedExpression;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.expression.Expression#bind(org.datanucleus.query.symbol.SymbolTable)
     */
    @Override
    public Symbol bind(SymbolTable symtbl)
    {
        containedExpression.bind(symtbl);
        return null;
    }

    public String toString()
    {
        return new StringBuilder("TypeExpression {").append(containedExpression).append("}").toString();
    }
}