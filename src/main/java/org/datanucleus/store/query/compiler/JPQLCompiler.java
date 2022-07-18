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
package org.datanucleus.store.query.compiler;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.QueryLanguage;
import org.datanucleus.store.query.JPQLQueryHelper;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.query.expression.Expression;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.PrimaryExpression;
import org.datanucleus.util.Imports;

/**
 * Implementation of a compiler for JPQL (JSR0220, JSR0317).
 */
public class JPQLCompiler extends JavaQueryCompiler
{
    public JPQLCompiler(PersistenceNucleusContext nucCtx, ClassLoaderResolver clr, String from, Class candidateClass, Collection candidates,
            String filter, Imports imports, String ordering, String result, String grouping, String having, String params, String update)
    {
        super(nucCtx, clr, from, candidateClass, candidates, filter, imports, ordering, result, grouping, having, params, null, update);
        this.from = from;
        this.caseSensitiveAliases = false;
    }

    /**
     * Method to compile the query, and return the compiled results.
     * @param parameters the parameter map of values keyed by param name
     * @param subqueryMap Map of subquery variables, keyed by the subquery name
     * @return The compiled query
     */
    public QueryCompilation compile(Map parameters, Map<String, Object> subqueryMap)
    {
        parser = new JPQLParser();
        if (options != null && options.containsKey(Query.EXTENSION_JPQL_STRICT))
        {
            parser.setStrict(Boolean.parseBoolean((String)options.get(Query.EXTENSION_JPQL_STRICT)));
        }

        symtbl = new SymbolTable();
        symtbl.setSymbolResolver(this);
        if (parentCompiler != null)
        {
            symtbl.setParentSymbolTable(parentCompiler.symtbl);
        }

        if (subqueryMap != null && !subqueryMap.isEmpty())
        {
            // Load subqueries into symbol table so the compilation knows about them
            Iterator<String> subqueryIter = subqueryMap.keySet().iterator();
            while (subqueryIter.hasNext())
            {
                String subqueryName = subqueryIter.next();
                Symbol sym = new PropertySymbol(subqueryName);
                sym.setType(Symbol.VARIABLE);
                symtbl.addSymbol(sym);
            }
        }

        Expression[] exprFrom = compileFrom();
        compileCandidatesParametersVariables(parameters);
        Expression exprFilter = compileFilter();
        Expression[] exprOrdering = compileOrdering();
        Expression[] exprResult = compileResult();
        Expression[] exprGrouping = compileGrouping();
        Expression exprHaving = compileHaving();
        Expression[] exprUpdate = compileUpdate();

        if (exprResult != null && exprResult.length == 1 && exprResult[0] instanceof PrimaryExpression)
        {
            // Check for special case of "Object(p)" in result, which means no special result
            String resultExprId = ((PrimaryExpression)exprResult[0]).getId();
            if (resultExprId.equalsIgnoreCase(candidateAlias))
            {
                exprResult = null;
            }
        }
        if (exprResult != null)
        {
            for (int i=0;i<exprResult.length;i++)
            {
                if (exprResult[i] instanceof InvokeExpression)
                {
                    InvokeExpression invokeExpr = (InvokeExpression) exprResult[i];
                    if (isMethodNameAggregate(invokeExpr.getOperation()))
                    {
                        // Make sure these have 1 argument
                        List<Expression> args = invokeExpr.getArguments();
                        if (args == null || args.size() != 1)
                        {
                            throw new NucleusUserException("JPQL query has result clause using aggregate (" + invokeExpr.getOperation() + ") but this needs 1 argument");
                        }
                    }
                }
            }
        }

        QueryCompilation compilation = new QueryCompilation(candidateClass, candidateAlias, symtbl,
            exprResult, exprFrom, exprFilter, exprGrouping, exprHaving, exprOrdering, exprUpdate);
        compilation.setQueryLanguage(getLanguage());

        return compilation;
    }

    /**
     * Method to perform a lookup of the class name from the input name.
     * Makes use of the query "imports" and the lookup to "entity name".
     * @param className Name of the class
     * @return The class corresponding to this name
     * @throws ClassNotResolvedException thrown if not resolvable using imports or entity name
     */
    public Class resolveClass(String className)
    {
        AbstractClassMetaData acmd = metaDataManager.getMetaDataForEntityName(className);
        if (acmd != null)
        {
            // Resolved as an entityName, so return this class
            String fullClassName = acmd.getFullClassName();
            if (fullClassName != null)
            {
                return clr.classForName(fullClassName);
            }
        }

        return super.resolveClass(className);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.symbol.SymbolResolver#supportsVariables()
     */
    public boolean supportsImplicitVariables()
    {
        // We use variables for subqueries but not as a general feature so mark as false for now
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.symbol.SymbolResolver#caseSensitiveSymbolNames()
     */
    public boolean caseSensitiveSymbolNames()
    {
        return false;
    }

    /**
     * Accessor for the query language name.
     * @return Name of the query language.
     */
    public String getLanguage()
    {
        return QueryLanguage.JPQL.name();
    }

    /**
     * Method to return if the supplied name is a keyword.
     * Keywords can only appear at particular places in a query so we need to detect for valid queries.
     * @param name The name
     * @return Whether it is a keyword
     */
    protected boolean isKeyword(String name)
    {
        return JPQLQueryHelper.isKeyword(name);
    }

    private static boolean isMethodNameAggregate(String methodName)
    {
        if (methodName.equalsIgnoreCase("avg") || methodName.equalsIgnoreCase("count") || methodName.equalsIgnoreCase("sum") || 
            methodName.equalsIgnoreCase("min") || methodName.equalsIgnoreCase("max"))
        {
            return true;
        }
        return false;
    }
}