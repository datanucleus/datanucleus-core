/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.query;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.query.JPQLSingleStringParser;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.compiler.JPQLCompiler;
import org.datanucleus.query.compiler.JavaQueryCompiler;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of a JPQL query used by DataNucleus.
 * The query can be specified via method calls, or via a single-string form.
 * @see Query
 */
public abstract class AbstractJPQLQuery extends AbstractJavaQuery
{
    private static final long serialVersionUID = 3365033406094223177L;

    /**
     * Constructor.
     * @param storeMgr StoreManager for this query
     * @param ec ExecutionContext
     */
    public AbstractJPQLQuery(StoreManager storeMgr, ExecutionContext ec)
    {
        super(storeMgr, ec);
    }

    /**
     * Constructs a new query instance having the same criteria as the given query.
     * @param storeMgr StoreManager for this query
     * @param ec ExecutionContext
     * @param q The query from which to copy criteria.
     */
    public AbstractJPQLQuery(StoreManager storeMgr, ExecutionContext ec, AbstractJPQLQuery q)
    {
        super(storeMgr, ec);

        candidateClass = q!=null ? q.candidateClass : null;
        candidateClassName = q!=null ? q.candidateClassName : null;
        from = q!=null ? q.from : null;
        subclasses = q!=null ? q.subclasses : true;
        filter = q!=null ? q.filter : null;
        imports = q!=null ? q.imports : null;
        explicitVariables = q!=null ? q.explicitVariables : null;
        explicitParameters = q!=null ? q.explicitParameters : null;
        grouping = q!=null ? q.grouping : null;
        ordering = q!=null ? q.ordering : null;
        update = q!=null ? q.update : null;
        result = q!=null ? q.result : null;
        resultClass = q!=null ? q.resultClass : null;
        resultDistinct = q!=null ? q.resultDistinct : false;
        range = q!=null ? q.range : null;
        fromInclNo = q!=null ? q.fromInclNo : 0;
        toExclNo = q!=null ? q.toExclNo : Long.MAX_VALUE;
        fromInclParam = q!=null ? q.fromInclParam : null;
        toExclParam = q!=null ? q.toExclParam : null;
        if (q != null)
        {
            ignoreCache = q.ignoreCache;
        }

        if (q != null && q.subqueries != null && !q.subqueries.isEmpty())
        {
            Iterator<SubqueryDefinition> subqueryDefIter = q.subqueries.values().iterator();
            while (subqueryDefIter.hasNext())
            {
                SubqueryDefinition subquery = subqueryDefIter.next();
                // TODO Make copies rather than using the objects
                addSubquery(subquery.query, subquery.variableDecl, subquery.candidateExpression, subquery.parameterMap);
            }
        }
    }

    /**
     * Constructor for a JPQL query where the query is specified using the "Single-String" format.
     * @param storeMgr StoreManager for this query
     * @param ec ExecutionContext
     * @param query The query string
     */
    public AbstractJPQLQuery(StoreManager storeMgr, ExecutionContext ec, String query)
    {
        super(storeMgr, ec);

        JPQLSingleStringParser parser = new JPQLSingleStringParser(this, query);
        if (ec.getBooleanProperty(PropertyNames.PROPERTY_QUERY_JPQL_ALLOW_RANGE))
        {
            // Allow RANGE to be used in the JPQL
            parser.allowRange();
        }
        parser.parse();
    }

    /**
     * Method to return the names of the extensions supported by this query.
     * To be overridden by subclasses where they support additional extensions.
     * @return The supported extension names
     */
    public Set<String> getSupportedExtensions()
    {
        Set<String> supported = super.getSupportedExtensions();
        supported.add(EXTENSION_JPQL_STRICT);
        return supported;
    }

    /**
     * Set the result for the results.
     * @param result Optional keyword "distinct" followed by comma-separated list of 
     *     result expressions or a result class
     */
    public void setResult(String result)
    {
        discardCompiled();
        assertIsModifiable();
        if (result == null)
        {
            this.result = null;
            this.resultDistinct = false;
            return;
        }

        String str = result.trim();
        if (str.toUpperCase().startsWith("DISTINCT "))
        {
            this.resultDistinct = true;
            this.result = str.substring(8).trim();
        }
        else
        {
            this.resultDistinct = false;
            this.result = str;
        }
    }

	/**
     * Method to get key for query cache
     * @return The cache key
     */
    protected String getQueryCacheKey()
    {
        String queryCacheKey = toString();

		// JPQL single string doesn't include range so add it since (datastore) compile will depend on it if evaluated in datastore
        if (range != null)
        {
            queryCacheKey += (" RANGE " + range);
        }
        else if (fromInclNo > 0 || toExclNo != Long.MAX_VALUE)
        {
            queryCacheKey += (" RANGE " + fromInclNo + "," + toExclNo);
        }
        if (getFetchPlan() != null)
        {
            queryCacheKey += (" " + getFetchPlan().toString());
        }
        if (!subclasses)
        {
            queryCacheKey += " EXCLUDE SUBCLASSES";
        }

        String multiTenancyId = ec.getNucleusContext().getMultiTenancyId(ec, null); // TODO Fill in last arg
        if (multiTenancyId != null)
        {
            queryCacheKey += (" " + multiTenancyId);
        }

        return queryCacheKey;
    }

	/**
     * Method to take the defined parameters for the query and form a single string.
     * This is used to print out the query for logging.
     * @return The single string
     */
    public String getSingleStringQuery()
    {
        if (singleString != null)
        {
            return singleString;
        }

        StringBuilder str = new StringBuilder();
        if (type == QueryType.BULK_INSERT)
        {
            str.append("INSERT INTO ").append(from);
            str.append(" (").append(insertFields).append(") ");
            str.append(insertSelectQuery);
        }
        else if (type == QueryType.BULK_UPDATE)
        {
            str.append("UPDATE ").append(from).append(" SET ").append(update).append(' ');
            if (!StringUtils.isWhitespace(filter))
            {
                str.append("WHERE ").append(dereferenceFilter(filter));
            }
        }
        else if (type == QueryType.BULK_DELETE)
        {
            str.append("DELETE FROM ").append(from).append(' ');
            if (!StringUtils.isWhitespace(filter))
            {
                str.append("WHERE ").append(dereferenceFilter(filter));
            }
        }
        else
        {
            str.append("SELECT ");
            if (result != null)
            {
                if (resultDistinct)
                {
                    str.append("DISTINCT ");
                }
                str.append(result).append(' ');
            }
            else
            {
                if (compilation != null && compilation.getCandidateAlias() != null)
                {
                    str.append(compilation.getCandidateAlias()).append(' ');
                }
            }

            str.append("FROM " + from + " ");
            if (filter != null)
            {
                str.append("WHERE " + dereferenceFilter(filter)).append(' ');
            }
            if (grouping != null)
            {
                str.append("GROUP BY ").append(grouping).append(' ');
            }
            if (having != null)
            {
                str.append("HAVING ").append(having).append(' ');
            }
            if (ordering != null)
            {
                str.append("ORDER BY ").append(ordering).append(' ');
            }
        }

        singleString = str.toString().trim();
        return singleString;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.AbstractJavaQuery#compileGeneric(java.util.Map)
     */
    @Override
    public void compileGeneric(Map parameterValues)
    {
        if (compilation != null)
        {
            return;
        }

        QueryManager queryMgr = getQueryManager();
        String queryCacheKey = getQueryCacheKey();
        if (useCaching() && queryCacheKey != null)
        {
            QueryCompilation cachedCompilation = queryMgr.getQueryCompilationForQuery(getLanguage(), queryCacheKey);
            if (cachedCompilation != null)
            {
                compilation = cachedCompilation;
                if (compilation.getExprResult() == null)
                {
                    // If the result was "Object(e)" or "e" then this is meaningless so remove
                    result = null;
                }
                checkParameterTypesAgainstCompilation(parameterValues);
                return;
            }
        }

        long startTime = 0;
        if (NucleusLogger.QUERY.isDebugEnabled())
        {
            startTime = System.currentTimeMillis();
            NucleusLogger.QUERY.debug(Localiser.msg("021044", getLanguage(), getSingleStringQuery()));
        }
        String from = this.from;
        if (type == QueryType.BULK_INSERT)
        {
            // Append artificial alias so the compilation passes
            from += " this";
        }
        JavaQueryCompiler compiler = new JPQLCompiler(ec.getNucleusContext(), ec.getClassLoaderResolver(), from, candidateClass, candidateCollection, 
            this.filter, getParsedImports(), this.ordering, this.result, this.grouping, this.having, explicitParameters, update);
        if (getBooleanExtensionProperty(EXTENSION_JPQL_STRICT, false))
        {
            compiler.setOption(EXTENSION_JPQL_STRICT, "true");
        }
        compilation = compiler.compile(parameterValues, subqueries);
        if (QueryUtils.queryReturnsSingleRow(this))
        {
            compilation.setReturnsSingleRow();
        }
        if (resultDistinct)
        {
            compilation.setResultDistinct();
        }
        if (compilation.getExprResult() == null)
        {
            // If the result was "Object(e)" or "e" then this is meaningless so remove
            result = null;
        }
        if (NucleusLogger.QUERY.isDebugEnabled())
        {
            NucleusLogger.QUERY.debug(Localiser.msg("021045", getLanguage(), "" + (System.currentTimeMillis() - startTime)));
        }

        if (subqueries != null)
        {
            // Compile any subqueries
            compileSubqueries(subqueries, compilation, compiler, parameterValues);
        }

        if (NucleusLogger.QUERY.isDebugEnabled())
        {
            // Log the query compilation
            NucleusLogger.QUERY.debug(compilation.toString());
        }

        if (implicitParameters != null)
        {
            // Make sure any implicit parameters have their values in the compilation
            Iterator paramKeyIter = implicitParameters.keySet().iterator();
            while (paramKeyIter.hasNext())
            {
                Object paramKey = paramKeyIter.next();
                String paramName = "" + paramKey;
                applyImplicitParameterValueToCompilation(paramName, implicitParameters.get(paramName));
            }
        }

        checkParameterTypesAgainstCompilation(parameterValues);

        if (useCaching() && queryCacheKey != null)
        {
            // Cache for future reference
            queryMgr.addQueryCompilation(getLanguage(), queryCacheKey, compilation);
        }
    }

    /**
     * Method to compile the JPQL query.
     * This implementation assumes that we are using the "generic" JPQL compiler in 
     * <i>org.datanucleus.query.compiler</i>. If not then override this method.
     * Will populate the "compilation" class variable.
     * @param parameterValues Map of param values keyed by param name.
     */
    protected void compileInternal(Map parameterValues)
    {
        compileGeneric(parameterValues);
    }

    /**
     * Recursively compile the subqueries
     * @param subqueryMap The subquery definition map
     * @param parentCompilation The parent compilation
     * @param parentCompiler The parent compiler
     * @param parameterValues The parameters map
     */
    protected void compileSubqueries(Map<String, SubqueryDefinition> subqueryMap, QueryCompilation parentCompilation, JavaQueryCompiler parentCompiler, 
            Map parameterValues)
    {
    	long startTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, SubqueryDefinition>> iter = subqueryMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, SubqueryDefinition> entry = iter.next();
            SubqueryDefinition subqueryDefinition = entry.getValue();
            Query subquery = subqueryDefinition.getQuery();
            if (NucleusLogger.QUERY.isDebugEnabled())
            {
                startTime = System.currentTimeMillis();
                NucleusLogger.QUERY.debug(Localiser.msg("021044", getLanguage(), ((AbstractJPQLQuery)subquery).getSingleStringQuery()));
            }

            JavaQueryCompiler subCompiler = new JPQLCompiler(ec.getNucleusContext(), ec.getClassLoaderResolver(), subquery.from, subquery.candidateClass, null,
                subquery.filter, getParsedImports(), subquery.ordering, subquery.result, subquery.grouping, subquery.having, null, null);
            if (getBooleanExtensionProperty(EXTENSION_JPQL_STRICT, false))
            {
                subCompiler.setOption(EXTENSION_JPQL_STRICT, "true");
            }
            subCompiler.setLinkToParentQuery(parentCompiler, null);
            QueryCompilation subqueryCompilation = subCompiler.compile(parameterValues, subquery.subqueries);
            parentCompilation.addSubqueryCompilation(entry.getKey(), subqueryCompilation);
            if (NucleusLogger.QUERY.isDebugEnabled())
            {
                NucleusLogger.QUERY.debug(Localiser.msg("021045", getLanguage(), "" + (System.currentTimeMillis() - startTime)));
            }

            if (subquery.subqueries != null) 
            {
                // Recurse to nested subqueries
            	compileSubqueries(subquery.subqueries, subqueryCompilation, subCompiler, parameterValues);
            }
        }
    }
    
    /**
     * Utility to resolve the declaration to a particular class.
     * Takes the passed in name, together with the defined import declarations and returns the
     * class represented by the declaration.
     * @param classDecl The declaration
     * @return The class it resolves to (if any)
     * @throws NucleusUserException Thrown if the class cannot be resolved.
     */
    public Class resolveClassDeclaration(String classDecl)
    {
        // Try to find an entity name before relaying to the superclass method
        AbstractClassMetaData acmd = this.getStoreManager().getNucleusContext().getMetaDataManager().getMetaDataForEntityName(classDecl);
        if (acmd != null)
        {
            classDecl = acmd.getFullClassName();
        }

        return super.resolveClassDeclaration(classDecl);
    }

    /**
     * Accessor for the query language.
     * @return Query language
     */
    public String getLanguage()
    {
        return Query.LANGUAGE_JPQL;
    }
}