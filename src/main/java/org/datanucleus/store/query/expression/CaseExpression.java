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
package org.datanucleus.store.query.expression;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.store.query.compiler.Symbol;
import org.datanucleus.store.query.compiler.SymbolTable;

/**
 * Expression representing a case series of when expressions and their action expressions.
 */
public class CaseExpression extends Expression
{
    private static final long serialVersionUID = -7123407498309440027L;

    List<ExpressionPair> actionConditions = new ArrayList<ExpressionPair>();
    Expression elseExpr;

    public CaseExpression()
    {
    }

    public void addCondition(Expression whenExpr, Expression actionExpr)
    {
        actionConditions.add(new ExpressionPair(whenExpr, actionExpr));
    }

    public void setElseExpression(Expression elseExpr)
    {
        this.elseExpr = elseExpr;
    }

    public List<ExpressionPair> getConditions()
    {
        return actionConditions;
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
        Iterator<ExpressionPair> actionCondIter = actionConditions.iterator();
        while (actionCondIter.hasNext())
        {
            ExpressionPair pair = actionCondIter.next();
            pair.getWhenExpression().bind(symtbl);
            pair.getActionExpression().bind(symtbl);
        }
        if (elseExpr != null)
        {
            elseExpr.bind(symtbl);
        }
        return null;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("CaseExpression : ");
        Iterator<ExpressionPair> actionCondIter = actionConditions.iterator();
        while (actionCondIter.hasNext())
        {
            ExpressionPair pair = actionCondIter.next();
            str.append("WHEN ").append(pair.getWhenExpression()).append(" THEN ").append(pair.getActionExpression()).append(" ");
        }
        if (elseExpr != null)
        {
            str.append("ELSE ").append(elseExpr);
        }
        if (alias != null)
        {
            str.append(" AS ").append(alias);
        }
        return str.toString();
    }

    public class ExpressionPair
    {
        Expression whenExpr;
        Expression actionExpr;
        public ExpressionPair(Expression when, Expression action)
        {
            this.whenExpr = when;
            this.actionExpr = action;
        }
        public Expression getWhenExpression()
        {
            return whenExpr;
        }
        public Expression getActionExpression()
        {
            return actionExpr;
        }
    }
}