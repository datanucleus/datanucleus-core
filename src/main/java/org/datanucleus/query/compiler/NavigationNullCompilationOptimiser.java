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

import java.util.Iterator;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.RelationType;
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

    ClassLoaderResolver clr;

    public NavigationNullCompilationOptimiser(QueryCompilation compilation, MetaDataManager mmgr, ClassLoaderResolver clr)
    {
        this.mmgr = mmgr;
        this.clr = clr;
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

    protected boolean isPrimaryExpressionRelationNavigation(PrimaryExpression primExpr)
    {
        List<String> tuples = primExpr.getTuples();
        Iterator<String> tupleIter = tuples.iterator();
        String component = "";
        AbstractClassMetaData cmd = mmgr.getMetaDataForClass(compilation.candidateClass, clr);
        AbstractMemberMetaData mmd = null;
        while (tupleIter.hasNext())
        {
            String name = tupleIter.next();
            if (component.length() == 0)
            {
                if (name.equals(compilation.candidateAlias))
                {
                    // Starting from this
                }
                else
                {
                    mmd = cmd.getMetaDataForMember(name);
                    RelationType relType = mmd.getRelationType(clr);
                    if (RelationType.isRelationSingleValued(relType))
                    {
                        // Should only join through 1-1/N-1 relations
                        AbstractMemberMetaData[] relMmds = mmd.getRelatedMemberMetaData(clr);
                        cmd = relMmds[0].getAbstractClassMetaData();
                        mmd = relMmds[0];
                        // TODO Is this nullable?
                    }
                }
                component = name;
            }
            else
            {
                if (cmd == null)
                {
                    return false;
                }

                if (mmd != null)
                {
                    
                }
            }

            component = component + "." + name;
        }

        // TODO Implement this
        return false;
    }
}