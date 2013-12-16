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

import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.query.JDOQLSingleStringParser;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.compiler.JDOQLCompiler;
import org.datanucleus.query.compiler.JavaQueryCompiler;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.NucleusLogger;

/**
 * Abstract representation of a JDOQL query.
 * The query can be specified via method calls, or via a single-string form.
 * @see Query
 */
public abstract class AbstractJDOQLQuery extends AbstractJavaQuery
{
    /**
     * Constructor.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     */
    public AbstractJDOQLQuery(StoreManager storeMgr, ExecutionContext ec)
    {
        super(storeMgr, ec);
    }

    /**
     * Constructs a new query instance having the same criteria as the given query.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     * @param q The query from which to copy criteria.
     */
    public AbstractJDOQLQuery(StoreManager storeMgr, ExecutionContext ec, AbstractJDOQLQuery q)
    {
        this(storeMgr, ec);

        candidateClass = (q!=null ? q.candidateClass : null);
        candidateClassName = (q!=null ? q.candidateClassName : null);
        subclasses = (q!=null ? q.subclasses : true);
        filter = (q!=null ? q.filter : null);
        imports = (q!=null ? q.imports : null);
        explicitVariables = (q!=null ? q.explicitVariables : null);
        explicitParameters = (q!=null ? q.explicitParameters : null);
        grouping = (q!=null ? q.grouping : null);
        ordering = (q!=null ? q.ordering : null);
        update = (q!=null ? q.update : null);
        result = (q!=null ? q.result : null);
        resultClass = (q!=null ? q.resultClass : null);
        resultDistinct = (q!=null ? q.resultDistinct : false);
        range = (q!=null ? q.range : null);
        fromInclNo = (q!=null ? q.fromInclNo : 0);
        toExclNo = (q!=null ? q.toExclNo : Long.MAX_VALUE);
        fromInclParam = (q!=null ? q.fromInclParam : null);
        toExclParam = (q!=null ? q.toExclParam : null);
        if (q != null)
        {
            ignoreCache = q.ignoreCache;
        }

        if (q != null && q.subqueries != null && !q.subqueries.isEmpty())
        {
            Iterator<String> subqueryKeyIter = q.subqueries.keySet().iterator();
            while (subqueryKeyIter.hasNext())
            {
                String key = subqueryKeyIter.next();
                SubqueryDefinition subquery = q.subqueries.get(key);
                // TODO Make copies rather than using the objects
                addSubquery(subquery.query, subquery.variableDecl, subquery.candidateExpression, subquery.parameterMap);
            }
        }
    }

    /**
     * Constructor for a JDOQL query where the query is specified using the "Single-String" format.
     * @param storeMgr StoreManager for this query
     * @param ec execution context
     * @param query The query string
     */
    public AbstractJDOQLQuery(StoreManager storeMgr, ExecutionContext ec, String query)
    {
        this(storeMgr, ec);

        // Parse the single-string query for errors
        JDOQLSingleStringParser parser = new JDOQLSingleStringParser(this, query);
        boolean allowAllSyntax = ec.getNucleusContext().getPersistenceConfiguration().getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL);
        if (ec.getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL) != null)
        {
            allowAllSyntax = ec.getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL);
        }
        if (allowAllSyntax)
        {
            parser.setAllowDelete(true);
            parser.setAllowUpdate(true);
        }
        parser.parse();

        if (candidateClassName != null)
        {
            try
            {
                // Set the candidateClass since the single-string parse only sets the candidateClassName
                // Note that the candidateClassName at this point could be unqualified
                candidateClass = getParsedImports().resolveClassDeclaration(candidateClassName, clr, null);
                candidateClassName = candidateClass.getName();
            }
            catch (ClassNotResolvedException e)
            {
                // TODO Localise this
                NucleusLogger.QUERY.warn("Candidate class for JDOQL single-string query (" + candidateClassName + 
                    ") could not be resolved", e);
            }
        }
    }

    /**
     * Set the grouping specification for the result Collection.
     * @param grouping the grouping specification.
     */
    public void setGrouping(String grouping)
    {
        discardCompiled();
        assertIsModifiable();
        
        //discard previous values
        this.grouping = null;
        setHaving(null);

        if (grouping != null && grouping.length() > 0)
        {
            // The "grouping" string will be of the form "...., ...., ... HAVING ...."
            // so we parse it into the former part as a grouping clause, and the latter part as a having clause
            if (grouping.indexOf("HAVING") >= 0)
            {
                setHaving(grouping.substring(grouping.indexOf("HAVING") + 7));
                this.grouping = grouping.substring(0, grouping.indexOf("HAVING")-1);
            }
            else if (grouping.indexOf("having") >= 0)
            {
                setHaving(grouping.substring(grouping.indexOf("having") + 7));
                this.grouping = grouping.substring(0, grouping.indexOf("having")-1);
            }
            else
            {
                this.grouping = grouping.trim();
            }
        }
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
        if (str.startsWith("distinct ") || str.startsWith("DISTINCT "))
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

        if (getFetchPlan() != null)
        {
            queryCacheKey += getFetchPlan().toString();
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
        if (type == BULK_UPDATE)
        {
            str.append("UPDATE " + from + " SET " + update + " ");
        }
        else if (type == BULK_DELETE)
        {
            str.append("DELETE ");
        }
        else
        {
            str.append("SELECT ");
        }

        if (unique)
        {
            str.append("UNIQUE ");
        }
        if (result != null)
        {
            if (resultDistinct)
            {
                str.append("DISTINCT ");
            }
            str.append(result + " ");
        }
        if (resultClass != null)
        {
            str.append("INTO " + resultClass.getName() + " ");
        }
        if (from != null)
        {
            // Subquery is of the form "<candidate-expression> alias"
            str.append("FROM " + from + " ");
        }
        else if (candidateClassName != null)
        {
            // Query is of the form "<candidate-class-name> [EXCLUDE-SUBCLASSES]"
            str.append("FROM " + candidateClassName + " ");
            if (!subclasses)
            {
                str.append("EXCLUDE SUBCLASSES ");
            }
        }
        if (filter != null)
        {
            str.append("WHERE " + dereferenceFilter(filter) + " ");
        }
        if (explicitVariables != null)
        {
            str.append("VARIABLES " + explicitVariables + " ");
        }
        if (explicitParameters != null)
        {
            str.append("PARAMETERS " + explicitParameters + " ");
        }
        if (imports != null)
        {
            str.append(imports + " ");
        }
        if (grouping != null)
        {
            str.append("GROUP BY " + grouping + " ");
        }
        if (having != null)
        {
            str.append("HAVING " + having + " ");
        }
        if (ordering != null)
        {
            str.append("ORDER BY " + ordering + " ");
        }

        if (range != null)
        {
            str.append("RANGE " + range);
        }
        else if (fromInclNo > 0 || toExclNo != Long.MAX_VALUE)
        {
            str.append("RANGE " + fromInclNo + "," + toExclNo);
        }

        singleString = str.toString().trim();
        return singleString;
    }

    /**
     * Method to compile the JDOQL query.
     * This implementation assumes that we are using the "generic" JDOQL compiler in 
     * <i>org.datanucleus.query.compiler</i>. If not then override this method.
     * Will populate the "compilation" class variable.
     * @param parameterValues Map of parameter values keyed by parameter name.
     */
    protected void compileInternal(Map parameterValues)
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
                checkParameterTypesAgainstCompilation(parameterValues);
                return;
            }
        }

        // Resolve resultClass name if defined
        if (resultClassName != null)
        {
            // Throws NucleusUserException if not resolvable
            resultClass = resolveClassDeclaration(resultClassName);
            resultClassName = null;
        }

        long startTime = 0;
        if (NucleusLogger.QUERY.isDebugEnabled())
        {
            startTime = System.currentTimeMillis();
            NucleusLogger.QUERY.debug(LOCALISER.msg("021044", getLanguage(), getSingleStringQuery()));
        }
        JDOQLCompiler compiler = new JDOQLCompiler(ec.getMetaDataManager(), ec.getClassLoaderResolver(), 
            from, candidateClass, candidateCollection, 
            this.filter, getParsedImports(), this.ordering, this.result, this.grouping, this.having, 
            explicitParameters, explicitVariables, this.update);
        boolean allowAllSyntax = ec.getNucleusContext().getPersistenceConfiguration().getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL);
        if (ec.getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL) != null)
        {
            allowAllSyntax = ec.getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL);
        }
        compiler.setAllowAll(allowAllSyntax);
        compilation = compiler.compile(parameterValues, subqueries);
        if (QueryUtils.queryReturnsSingleRow(this))
        {
            compilation.setReturnsSingleRow();
        }
        if (resultDistinct)
        {
            compilation.setResultDistinct();
        }
        if (NucleusLogger.QUERY.isDebugEnabled())
        {
            NucleusLogger.QUERY.debug(LOCALISER.msg("021045", getLanguage(), 
                "" + (System.currentTimeMillis() - startTime)));
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

        checkParameterTypesAgainstCompilation(parameterValues);

        if (useCaching() && queryCacheKey != null)
        {
            // Cache for future reference
            queryMgr.addQueryCompilation(getLanguage(), queryCacheKey, compilation);
        }
    }

    /**
     * Recursively compile the subqueries
     * @param subqueryMap The subquery definition map
     * @param parentCompilation 
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
                NucleusLogger.QUERY.debug(LOCALISER.msg("021044", getLanguage(), 
                    ((AbstractJDOQLQuery)subquery).getSingleStringQuery()));
            }

            JDOQLCompiler subCompiler = new JDOQLCompiler(ec.getMetaDataManager(), ec.getClassLoaderResolver(),
                subquery.from, subquery.candidateClass, null,
                subquery.filter, getParsedImports(), subquery.ordering, subquery.result, 
                subquery.grouping, subquery.having, subquery.explicitParameters, null, null);
            boolean allowAllSyntax = ec.getNucleusContext().getPersistenceConfiguration().getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL);
            if (ec.getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL) != null)
            {
                allowAllSyntax = ec.getBooleanProperty(PropertyNames.PROPERTY_QUERY_JDOQL_ALLOWALL);
            }
            subCompiler.setAllowAll(allowAllSyntax);
            subCompiler.setLinkToParentQuery(parentCompiler, subqueryDefinition.getParameterMap());
            QueryCompilation subqueryCompilation = subCompiler.compile(parameterValues, null);
            if (QueryUtils.queryReturnsSingleRow(subquery))
            {
                subqueryCompilation.setReturnsSingleRow();
            }
            parentCompilation.addSubqueryCompilation(entry.getKey(), subqueryCompilation);
            if (NucleusLogger.QUERY.isDebugEnabled())
            {
                NucleusLogger.QUERY.debug(LOCALISER.msg("021045", getLanguage(), 
                    "" + (System.currentTimeMillis() - startTime)));
            }

            if (subquery.subqueries != null) 
            {
                // Recurse to nested subqueries
            	compileSubqueries(subquery.subqueries, subqueryCompilation, subCompiler, parameterValues);
            }
        }
    }

	/**
     * Accessor for the query language.
     * @return Query language
     */
    public String getLanguage()
    {
        return "JDOQL";
    }
}