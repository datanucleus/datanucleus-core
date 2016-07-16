/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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

import java.util.List;

import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Optimiser for query compilation that searches for navigation through relations, and adds "not null" checks.
 * Applies to the FILTER only.
 */
public class NavigationNullCompilationOptimiser implements CompilationOptimiser
{
    /** The compilation that we are optimising. */
    QueryCompilation compilation;

    MetaDataManager mmgr;

    public NavigationNullCompilationOptimiser(QueryCompilation compilation, MetaDataManager mmgr)
    {
        this.mmgr = mmgr;
        this.compilation = compilation;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.CompilationOptimiser#optimise()
     */
    @Override
    public void optimise()
    {
        processPrimaryExpressionNavigationNullCheck(compilation.getExprFilter());
    }

    private void processPrimaryExpressionNavigationNullCheck(Expression expr)
    {
        if (expr == null)
        {
            return;
        }

        if (expr instanceof DyadicExpression)
        {
            processPrimaryExpressionNavigationNullCheck(expr.getLeft());
            processPrimaryExpressionNavigationNullCheck(expr.getRight());
        }
        else if (expr instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression) expr;
            if (primExpr.getLeft() != null)
            {
                Expression left = primExpr.getLeft();
                processPrimaryExpressionNavigationNullCheck(left);
            }
            else
            {
                List tuples = primExpr.getTuples();
                if (tuples.size() > 1)
                {
                    NucleusLogger.GENERAL.info(">> processPrimExpr " + primExpr + " tuples=" + StringUtils.collectionToString(tuples));
                }
                // TODO Implement this
            }
        }
        else if (expr instanceof InvokeExpression)
        {
            // TODO Implement this
        }
        // TODO Add any other types that may contain PrimaryExpression
    }
}
