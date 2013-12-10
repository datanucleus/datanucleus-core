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
package org.datanucleus.query.compiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.VariableExpression;
import org.datanucleus.util.NucleusLogger;

/**
 * Optimiser for a query compilation.
 * Attempts to detect and correct common input problems to give a more efficiently evaluated query.
 * Currently handles the following
 * <ul>
 * <li>When the user specifies "var == this", this means nothing since the variable is the same as the candidate
 * so replaces all instances of the variable with the candidate</li>
 * </ul>
 */
public class QueryCompilerOptimiser
{
    /** The compilation that we are optimising. */
    QueryCompilation compilation;

    public QueryCompilerOptimiser(QueryCompilation compilation)
    {
        this.compilation = compilation;
    }

    /**
     * Method to perform the optimisation.
     */
    public void optimise()
    {
        if (compilation == null)
        {
            return;
        }
        if (compilation.getExprFilter() != null)
        {
            // Check for redundant variables in the filter, an expression of the form "var == this"
            Set<String> redundantVariables = new HashSet<String>();
            findRedundantFilterVariables(compilation.getExprFilter(), redundantVariables);
            if (!redundantVariables.isEmpty())
            {
                Iterator<String> redundantVarIter = redundantVariables.iterator();
                while (redundantVarIter.hasNext())
                {
                    String var = redundantVarIter.next();
                    if (NucleusLogger.QUERY.isDebugEnabled())
                    {
                        NucleusLogger.QUERY.debug("Query was defined with variable " + var + 
                        " yet this was redundant, so has been replaced by the candidate");
                    }

                    compilation.setExprFilter(replaceVariableWithCandidateInExpression(var, compilation.getExprFilter()));
                    compilation.setExprHaving(replaceVariableWithCandidateInExpression(var, compilation.getExprHaving()));
                    Expression[] exprResult = compilation.getExprResult();
                    if (exprResult != null)
                    {
                        for (int i=0;i<exprResult.length;i++)
                        {
                            exprResult[i] = replaceVariableWithCandidateInExpression(var, exprResult[i]);
                        }
                    }
                    Expression[] exprGrouping = compilation.getExprGrouping();
                    if (exprGrouping != null)
                    {
                        for (int i=0;i<exprGrouping.length;i++)
                        {
                            exprGrouping[i] = replaceVariableWithCandidateInExpression(var, exprGrouping[i]);
                        }
                    }

                    compilation.getSymbolTable().removeSymbol(compilation.getSymbolTable().getSymbol(var));
                    // TODO Remove from input variables if explicit
                }
            }
        }
    }

    /**
     * Method that replaces any occurrence of the specified variable in the provided expression with the
     * candidate primary expression. Recurses to sub-expressions.
     * @param varName Variable name
     * @param expr The expression to update
     * @return Updated expression
     */
    private Expression replaceVariableWithCandidateInExpression(String varName, Expression expr)
    {
        if (expr == null)
        {
            return null;
        }

        if (expr instanceof VariableExpression && ((VariableExpression)expr).getId().equals(varName))
        {
            List<String> tuples = new ArrayList<String>();
            tuples.add(compilation.getCandidateAlias());
            Expression replExpr = new PrimaryExpression(tuples);
            replExpr.bind(compilation.getSymbolTable());
            return replExpr;
        }
        else if (expr instanceof DyadicExpression)
        {
            DyadicExpression dyExpr = (DyadicExpression)expr;
            if (dyExpr.getLeft() != null)
            {
                dyExpr.setLeft(replaceVariableWithCandidateInExpression(varName, dyExpr.getLeft()));
            }
            if (dyExpr.getRight() != null)
            {
                dyExpr.setRight(replaceVariableWithCandidateInExpression(varName, dyExpr.getRight()));
            }
        }
        else if (expr instanceof PrimaryExpression)
        {
            if (expr.getLeft() != null)
            {
                if (expr.getLeft() instanceof VariableExpression && 
                    ((VariableExpression)expr.getLeft()).getId().equals(varName))
                {
                    // Needs to be relative to candidate so just remove the "left"
                    expr.setLeft(null);
                }
                else
                {
                    expr.setLeft(replaceVariableWithCandidateInExpression(varName, expr.getLeft()));
                }
            }
        }
        else if (expr instanceof InvokeExpression)
        {
            InvokeExpression invokeExpr = (InvokeExpression)expr;
            if (invokeExpr.getLeft() != null)
            {
                invokeExpr.setLeft(replaceVariableWithCandidateInExpression(varName, invokeExpr.getLeft()));
            }
//            List<Expression> args = invokeExpr.getArguments(); // TODO Process Invoke args
        }
        // TODO More combinations
        return expr;
    }

    /**
     * Method to process the provided filter expression and find any variables that are to all intents
     * and purposes redundant. Checks for "var == this". In this case we can just replace the variable
     * occurrences with "this".
     * @param filterExpr The filter
     * @param varNames The variable names that are redundant (updated by this method)
     */
    private void findRedundantFilterVariables(Expression filterExpr, Set<String> varNames)
    {
        if (filterExpr instanceof DyadicExpression)
        {
            DyadicExpression dyExpr = (DyadicExpression) filterExpr;
            if (dyExpr.getOperator() == Expression.OP_EQ)
            {
                if (dyExpr.getLeft() instanceof VariableExpression)
                {
                    if (dyExpr.getRight() instanceof PrimaryExpression)
                    {
                        PrimaryExpression rightExpr = (PrimaryExpression)dyExpr.getRight();
                        if (rightExpr.getId().equals(compilation.getCandidateAlias()))
                        {
                            varNames.add(((VariableExpression)dyExpr.getLeft()).getId());
                        }
                    }
                }
                else if (dyExpr.getRight() instanceof VariableExpression)
                {
                    if (dyExpr.getLeft() instanceof PrimaryExpression)
                    {
                        PrimaryExpression leftExpr = (PrimaryExpression)dyExpr.getLeft();
                        if (leftExpr.getId().equals(compilation.getCandidateAlias()))
                        {
                            varNames.add(((VariableExpression)dyExpr.getRight()).getId());
                        }
                    }
                }
            }
            else if (dyExpr.getOperator() == Expression.OP_AND)
            {
                findRedundantFilterVariables(dyExpr.getLeft(), varNames);
                findRedundantFilterVariables(dyExpr.getRight(), varNames);
            }
            else if (dyExpr.getOperator() == Expression.OP_OR)
            {
                findRedundantFilterVariables(dyExpr.getLeft(), varNames);
                findRedundantFilterVariables(dyExpr.getRight(), varNames);
            }
        }
    }
}