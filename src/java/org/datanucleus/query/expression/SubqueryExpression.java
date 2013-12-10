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
package org.datanucleus.query.expression;

import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;

/**
 * Expression containing a subquery.
 * A subquery is formed from a keyword and a variable expression representing the subquery, so
 * something like
 * <pre>KEYWORD(subquery)</pre>
 * e.g
 * <pre>EXISTS (SELECT 1 FROM MYTABLE WHERE MYID = 4)</pre>
 */
public class SubqueryExpression extends Expression
{
    String keyword;

    /**
     * @param keyword The keyword on the subquery
     * @param operand The variable expression representing the subquery
     */
    public SubqueryExpression(String keyword, VariableExpression operand)
    {
        this.keyword = keyword;
        this.right = operand;
    }

    public Symbol bind(SymbolTable symtbl)
    {
        right.bind(symtbl);

        return null;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public String toString()
    {
        return "SubqueryExpression{" + keyword + "(" + right + ")}";
    }
}