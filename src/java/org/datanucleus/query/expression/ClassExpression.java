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
 * Expression representing a candidate in a FROM clause.
 * This is used in JPQL where we have a "from" clause like
 * <pre> SELECT ... FROM Product p JOIN p.reviews r</pre>
 * so the ClassExpression is for alias "p" of type Product. 
 * The class name is stored in the Symbol, keyed by this alias.
 * Can have a JoinExpression to its right.
 */
public class ClassExpression extends Expression
{
    /** Optional candidate expression when in subquery and the class is a relation to the outer query. */
    String candidateExpression;

    public ClassExpression(String alias)
    {
        this.alias = alias;
    }

    public void setCandidateExpression(String expr)
    {
        this.candidateExpression = expr;
    }

    public String getCandidateExpression()
    {
        return candidateExpression;
    }

    /**
     * Set the right expression to the provided join.
     * @param expr Join information
     */
    public void setJoinExpression(JoinExpression expr)
    {
        this.right = expr;
    }

    public String getAlias()
    {
        return alias;
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol table
     * @return The symbol for this expression
     */
    public Symbol bind(SymbolTable symtbl)
    {
        symbol = symtbl.getSymbol(alias);
        return symbol;
    }

    public String toString()
    {
        if (right != null)
        {
            return "ClassExpression(" + 
                (candidateExpression != null ? ("candidate=" + candidateExpression + " ") : "") + 
                "alias=" + alias + " join=" + right + ")";
        }
        else
        {
            return "ClassExpression(" + 
                (candidateExpression != null ? ("candidate=" + candidateExpression + " ") : "") + 
                "alias=" + alias + ")";
        }
    }
}