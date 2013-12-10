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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;

/**
 * Expression representing a case series of when expressions and their action expressions.
 */
public class CaseExpression extends Expression
{
    Map<Expression, Expression> actionByCondition = new HashMap<Expression, Expression>();
    Expression elseExpr;

    public CaseExpression(Expression elseExpr)
    {
        this.elseExpr = elseExpr;
    }

    public void addCondition(Expression whenExpr, Expression actionExpr)
    {
        actionByCondition.put(whenExpr, actionExpr);
    }

    public Map<Expression, Expression> getConditions()
    {
        return actionByCondition;
    }

    public Expression getElseExpression()
    {
        return elseExpr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.expression.Expression#bind(org.datanucleus.query.symbol.SymbolTable)
     */
    @Override
    public Symbol bind(SymbolTable symtbl)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("CaseExpression : ");
        Iterator<Expression> keyIter = actionByCondition.keySet().iterator();
        while (keyIter.hasNext())
        {
            Expression whenExpr = keyIter.next();
            Expression actionExpr = actionByCondition.get(whenExpr);
            str.append("WHEN ").append(whenExpr).append(" THEN ").append(actionExpr).append(" ");
        }
        if (elseExpr != null)
        {
            str.append("ELSE ").append(elseExpr);
        }
        return str.toString();
    }
}