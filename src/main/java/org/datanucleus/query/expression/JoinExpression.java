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
package org.datanucleus.query.expression;

import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;

/**
 * Expression representing a join between a candidate class, and the class of a field of the first class.
 * An example is in JPQL where we have a "from" clause like
 * <pre>SELECT ... FROM Product p JOIN p.reviews r [ON {cond_expr}]</pre>
 * so the join between "Product p" and "Review r" using "p.reviews".
 * Can have a JoinExpression to its right.
 */
public class JoinExpression extends Expression
{
    private static final long serialVersionUID = -3758088504354624725L;

    public enum JoinType
    {
        JOIN_INNER, JOIN_LEFT_OUTER, JOIN_RIGHT_OUTER, JOIN_INNER_FETCH, JOIN_LEFT_OUTER_FETCH, JOIN_RIGHT_OUTER_FETCH
    }

    JoinType type;
    PrimaryExpression primExpr; // Expression for the field we are joining to
    DyadicExpression onExpr; // Optional ON expression to add to the join clause

    public JoinExpression(PrimaryExpression expr, String alias, JoinType type)
    {
        this.primExpr = expr;
        this.alias = alias;
        this.type = type;
    }

    public void setJoinExpression(JoinExpression expr)
    {
        this.right = expr;
    }

    public void setOnExpression(DyadicExpression expr)
    {
        this.onExpr = expr;
    }

    public PrimaryExpression getPrimaryExpression()
    {
        return primExpr;
    }

    public DyadicExpression getOnExpression()
    {
        return onExpr;
    }

    public String getAlias()
    {
        return alias;
    }

    public JoinType getType()
    {
        return type;
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol Table
     * @return The symbol for this expression
     */
    public Symbol bind(SymbolTable symtbl)
    {
        // TODO Implement this
        return null;
    }

    public String toString()
    {
        if (right != null)
        {
            return "JoinExpression{" + type + " " + primExpr + " alias=" + alias + " join=" + right + 
                    (onExpr != null ? (" on=" + onExpr) : "") + "}";
        }
        else
        {
            return "JoinExpression{" + type + " " + primExpr + " alias=" + alias + 
                    (onExpr != null ? (" on=" + onExpr) : "") + "}";
        }
    }
}