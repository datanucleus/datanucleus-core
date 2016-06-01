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
package org.datanucleus.query.compiler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.ParameterExpression;

/**
 * Representation of the components of a compiled java "string-based" query.
 * Assumes that the query has the following forms
 * <pre>
 * SELECT {result} FROM {from} WHERE {filter} GROUP BY {grouping} HAVING {having} ORDER BY {order}
 * UPDATE {from} SET {update} WHERE {filter}
 * DELETE FROM {from} WHERE {filter}
 * </pre>
 */
public class QueryCompilation implements Serializable
{
    private static final long serialVersionUID = 2976142726587145777L;

    /** Query language that this is a compilation for. */
    protected String queryLanguage;

    /** Primary candidate class. */
    protected Class candidateClass;

    /** Alias for the (primary) candidate. Defaults to "this". */
    protected String candidateAlias = "this";

    /** Whether the query will return a single row. */
    protected boolean returnsSingleRow = false;

    /** Compiled Symbol Table. */
    protected SymbolTable symtbl;

    /** Whether the result is distinct. */
    protected boolean resultDistinct = false;

    /** Compiled result expression. */
    protected Expression[] exprResult;

    /** Compiled from expression. */
    protected Expression[] exprFrom;

    /** Compiled update expression. */
    protected Expression[] exprUpdate;

    /** Compiled filter expression */
    protected Expression exprFilter = null;

    /** Compiled grouping expression. */
    protected Expression[] exprGrouping;

    /** Compiled having expression. */
    protected Expression exprHaving;

    /** Compiled ordering expression. */
    protected Expression[] exprOrdering;

    /** Compilations of any subqueries, keyed by the subquery variable name. */
    protected Map<String, QueryCompilation> subqueryCompilations = null;

    public QueryCompilation(Class candidateCls, String candidateAlias, SymbolTable symtbl, 
            Expression[] results, Expression[] froms, Expression filter, Expression[] groupings, 
            Expression having, Expression[] orderings, Expression[] updates)
    {
        this.candidateClass = candidateCls;
        this.candidateAlias = candidateAlias;
        this.symtbl = symtbl;
        this.exprResult = results;
        this.exprFrom = froms;
        this.exprFilter = filter;
        this.exprGrouping = groupings;
        this.exprHaving = having;
        this.exprOrdering = orderings;
        this.exprUpdate = updates;
    }

    public void setQueryLanguage(String lang)
    {
        this.queryLanguage = lang;
    }

    public String getQueryLanguage()
    {
        return this.queryLanguage;
    }

    public void setResultDistinct()
    {
        this.resultDistinct = true;
    }

    public boolean getResultDistinct()
    {
        return resultDistinct;
    }

    public void setReturnsSingleRow()
    {
        this.returnsSingleRow = true;
    }

    /**
     * Method to add the compilation for a subquery of this query.
     * @param alias Alias for the subquery (variable name)
     * @param compilation The compilation
     */
    public void addSubqueryCompilation(String alias, QueryCompilation compilation)
    {
        if (subqueryCompilations == null)
        {
            subqueryCompilations = new HashMap();
        }
        subqueryCompilations.put(alias, compilation);
    }

    /**
     * Accessor for the compilation for a subquery with the specified alias.
     * @param alias Alias of subquery
     * @return The compilation
     */
    public QueryCompilation getCompilationForSubquery(String alias)
    {
        return (subqueryCompilations != null ? subqueryCompilations.get(alias) : null);
    }

    /**
     * Accessor for the aliases for any subqueries in this compilation.
     * @return The subquery aliases (if any)
     */
    public String[] getSubqueryAliases()
    {
        if (subqueryCompilations == null || subqueryCompilations.isEmpty())
        {
            return null;
        }
        String[] aliases = new String[subqueryCompilations.size()];
        Iterator<String> iter = subqueryCompilations.keySet().iterator();
        int i = 0;
        while (iter.hasNext())
        {
            aliases[i++] = iter.next();
        }
        return aliases;
    }

    /**
     * Accessor for whether this query will return a single row.
     * This is true if all result selects are aggregates, or the user has set "unique" on the
     * query.
     * @return Whether this query will return a single row
     */
    public boolean returnsSingleRow()
    {
        return returnsSingleRow;
    }

    /**
     * Accessor for the types of the result row components.
     * If no result is defined then will be an array of size 1 with element type "candidate".
     * @return The result type(s)
     */
    public Class[] getResultTypes()
    {
        if (exprResult != null && exprResult.length > 0)
        {
            Class[] results = new Class[exprResult.length];
            for (int i=0;i<exprResult.length;i++)
            {
                Symbol colSym = exprResult[i].getSymbol();
                results[i] = colSym.getValueType();
            }
            return results;
        }

        // Each row must be an instance of the candidate
        return new Class[] {candidateClass};
    }

    /**
     * Accessor for the candidate class.
     * @return Candidate class
     */
    public Class getCandidateClass()
    {
        return candidateClass;
    }

    /**
     * Accessor for the candidate alias.
     * @return Candidate alias
     */
    public String getCandidateAlias()
    {
        return candidateAlias;
    }

    /**
     * Accessor for the symbol table for the query.
     * @return Symbol table, for parameter, variable lookup.
     */
    public SymbolTable getSymbolTable()
    {
        return symtbl;
    }

    /**
     * Accessor for any result expression(s).
     * @return The results
     */
    public Expression[] getExprResult()
    {
        return exprResult;
    }

    /**
     * Accessor for any from expression(s).
     * @return The from clauses
     */
    public Expression[] getExprFrom()
    {
        return exprFrom;
    }

    /**
     * Accessor for any update expression(s).
     * @return The updates
     */
    public Expression[] getExprUpdate()
    {
        return exprUpdate;
    }

    /**
     * Accessor for the filter expression.
     * @return The filter
     */
    public Expression getExprFilter()
    {
        return exprFilter;
    }

    public void setExprFilter(Expression filter)
    {
        exprFilter = filter;
    }

    /**
     * Accessor for any grouping expression(s).
     * @return The grouping
     */
    public Expression[] getExprGrouping()
    {
        return exprGrouping;
    }

    /**
     * Accessor for any having expression.
     * @return The having clause
     */
    public Expression getExprHaving()
    {
        return exprHaving;
    }

    public void setExprHaving(Expression having)
    {
        exprHaving = having;
    }

    /**
     * Accessor for any ordering expression(s).
     * @return The ordering
     */
    public Expression[] getExprOrdering()
    {
        return exprOrdering;
    }

    public ParameterExpression getParameterExpressionForPosition(int pos)
    {
        ParameterExpression paramExpr = null;
        if (exprResult != null)
        {
            for (int i=0;i<exprResult.length;i++)
            {
                paramExpr = QueryUtils.getParameterExpressionForPosition(exprResult[i], pos);
                if (paramExpr != null)
                {
                    return paramExpr;
                }
            }
        }
        if (exprFilter != null)
        {
            paramExpr = QueryUtils.getParameterExpressionForPosition(exprFilter, pos);
            if (paramExpr != null)
            {
                return paramExpr;
            }
        }
        if (exprGrouping != null)
        {
            for (int i=0;i<exprGrouping.length;i++)
            {
                paramExpr = QueryUtils.getParameterExpressionForPosition(exprGrouping[i], pos);
                if (paramExpr != null)
                {
                    return paramExpr;
                }
            }
        }
        if (exprHaving != null)
        {
            paramExpr = QueryUtils.getParameterExpressionForPosition(exprHaving, pos);
            if (paramExpr != null)
            {
                return paramExpr;
            }
        }
        if (exprOrdering != null)
        {
            for (int i=0;i<exprOrdering.length;i++)
            {
                paramExpr = QueryUtils.getParameterExpressionForPosition(exprOrdering[i], pos);
                if (paramExpr != null)
                {
                    return paramExpr;
                }
            }
        }

        return null;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("QueryCompilation:\n");
        str.append(debugString("  "));
        return str.toString();
    }

    public String debugString(String indent)
    {
        StringBuilder str = new StringBuilder();
        if (exprResult != null)
        {
            str.append(indent).append("[result:");
            if (resultDistinct)
            {
                str.append(" DISTINCT ");
            }
            for (int i=0;i<exprResult.length;i++)
            {
                if (i > 0)
                {
                    str.append(",");
                }
                str.append(exprResult[i]);
            }
            str.append("]\n");
        }
        if (exprFrom != null)
        {
            str.append(indent).append("[from:");
            for (int i=0;i<exprFrom.length;i++)
            {
                if (i > 0)
                {
                    str.append(",");
                }
                str.append(exprFrom[i]);
            }
            str.append("]\n");
        }
        if (exprUpdate != null)
        {
            str.append(indent).append("[update:");
            for (int i=0;i<exprUpdate.length;i++)
            {
                if (i > 0)
                {
                    str.append(",");
                }
                str.append(exprUpdate[i]);
            }
            str.append("]\n");
        }
        if (exprFilter != null)
        {
            str.append(indent).append("[filter:").append(exprFilter).append("]\n");
        }
        if (exprGrouping != null)
        {
            str.append(indent).append("[grouping:");
            for (int i=0;i<exprGrouping.length;i++)
            {
                if (i > 0)
                {
                    str.append(",");
                }
                str.append(exprGrouping[i]);
            }
            str.append("]\n");
        }
        if (exprHaving != null)
        {
            str.append(indent).append("[having:").append(exprHaving).append("]\n");
        }
        if (exprOrdering != null)
        {
            str.append(indent).append("[ordering:");
            for (int i=0;i<exprOrdering.length;i++)
            {
                if (i > 0)
                {
                    str.append(",");
                }
                str.append(exprOrdering[i]);
            }
            str.append("]\n");
        }

        str.append(indent).append("[symbols: ");
        Iterator<String> symIter = symtbl.getSymbolNames().iterator();
        while (symIter.hasNext())
        {
            String symName = symIter.next();
            Symbol sym = symtbl.getSymbol(symName);
            if (sym.getValueType() != null)
            {
                str.append(symName + " type=" + sym.getValueType().getName());
            }
            else
            {
                str.append(symName + " type=unknown");
            }
            if (symIter.hasNext())
            {
                str.append(", ");
            }
        }
        str.append("]");

        if (subqueryCompilations != null)
        {
            str.append("\n");
            Iterator subqIter = subqueryCompilations.entrySet().iterator();
            while (subqIter.hasNext())
            {
                Map.Entry<String, QueryCompilation> entry = (Map.Entry)subqIter.next();
                str.append(indent).append("[subquery: " + entry.getKey() + "\n");
                str.append(entry.getValue().debugString(indent + "  ")).append("]");
                if (subqIter.hasNext())
                {
                    str.append("\n");
                }
            }
        }

        return str.toString();
    }
}