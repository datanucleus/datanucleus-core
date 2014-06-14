/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved. 
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.datanucleus.ExecutionContext;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.QueryResultMetaData;
import org.datanucleus.store.Extent;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;

/**
 * Base definition of a query using SQL.
 * Based around the JDO definition of an SQL query where you typically set the SQL query filter
 * and have parameters set-able but not much else.
 */
public abstract class AbstractSQLQuery extends Query
{
    private static final long serialVersionUID = 3079774959293575353L;

    /** The statement that the user specified to the Query. */
    protected transient final String inputSQL;

    /** The actual SQL issued at execution time. */
    protected transient String compiledSQL = null;

    /** MetaData defining the results of the query. */
    protected QueryResultMetaData resultMetaData = null;

    /**
     * Constructs a new query instance from the existing query.
     * @param storeMgr StoreManager for this query
     * @param ec ExecutionContext
     * @param query Existing query
     */
    public AbstractSQLQuery(StoreManager storeMgr, ExecutionContext ec, AbstractSQLQuery query)
    {
        this(storeMgr, ec, query.inputSQL);
    }

    /**
     * Constructs a new query instance having the same criteria as the given query.
     * @param storeMgr StoreManager for this query
     * @param ec The ExecutionContext
     * @param sqlText The SQL query string
     */
    public AbstractSQLQuery(StoreManager storeMgr, ExecutionContext ec, String sqlText)
    {
        super(storeMgr, ec);

        candidateClass = null;
        filter = null;
        imports = null;
        explicitVariables = null;
        explicitParameters = null;
        ordering = null;

        if (sqlText == null)
        {
            throw new NucleusUserException(Localiser.msg("059001"));
        }

        // Remove any end-of-line chars for when user dumped the query in a text file with one word per line!
        this.inputSQL = sqlText.replace('\n', ' ').trim();

        // Detect type of SQL statement
        String firstToken = new StringTokenizer(inputSQL, " ").nextToken();
        if (firstToken.equalsIgnoreCase("SELECT"))
        {
            type = SELECT;
        }
        else if (firstToken.equalsIgnoreCase("DELETE"))
        {
            type = BULK_DELETE;
            unique = true;
        }
        else if (firstToken.equalsIgnoreCase("UPDATE") || firstToken.equalsIgnoreCase("INSERT") ||
                firstToken.equalsIgnoreCase("MERGE"))
        {
            type = BULK_UPDATE;
            unique = true;
        }
        else
        {
            // Stored procedures, others
            type = OTHER;
            unique = true;
        }

        Configuration conf = ec.getNucleusContext().getConfiguration();
        if (ec.getApiAdapter().getName().equalsIgnoreCase("JDO"))
        {
            // Check for strict SQL if required
            boolean allowAllSyntax = conf.getBooleanProperty(PropertyNames.PROPERTY_QUERY_SQL_ALLOWALL);
            if (ec.getProperty(PropertyNames.PROPERTY_QUERY_SQL_ALLOWALL) != null)
            {
                allowAllSyntax = ec.getBooleanProperty(PropertyNames.PROPERTY_QUERY_SQL_ALLOWALL);
            }
            if (!allowAllSyntax)
            {
                // JDO spec [14.7] : SQL queries must start with SELECT/select
                if (!firstToken.equals("SELECT") && !firstToken.startsWith("select"))
                {
                    throw new NucleusUserException(Localiser.msg("059002", inputSQL));
                }
            }
        }
    }

    public String getLanguage()
    {
        return "SQL";
    }

    /**
     * Utility to discard any compiled query.
     * @see org.datanucleus.store.query.Query#discardCompiled()
     */
    protected void discardCompiled()
    {
        super.discardCompiled();

        compiledSQL = null;
    }

    /**
     * Accessor for the user-input SQL query.
     * @return User-input SQL
     */
    public String getInputSQL()
    {
        return inputSQL;
    }

    /**
     * Set the candidate Extent to query.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param pcs the Candidate Extent.
     * @throws NucleusUserException Always thrown since method not applicable
     */
    public void setCandidates(Extent pcs)
    {
        throw new NucleusUserException(Localiser.msg("059004"));
    }

    /**
     * Set the candidate Collection to query.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param pcs the Candidate collection.
     * @throws NucleusUserException Always thrown since method not applicable
     */
    public void setCandidates(Collection pcs)
    {
        throw new NucleusUserException(Localiser.msg("059005"));
    }

    /**
     * Set the result for the results. The application might want to get results
     * from a query that are not instances of the candidate class. The results
     * might be fields of persistent instances, instances of classes other than
     * the candidate class, or aggregates of fields.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param result The result parameter consists of the optional keyword
     * distinct followed by a commaseparated list of named result expressions or
     * a result class specification.
     * @throws NucleusUserException Always thrown.
     */
    public void setResult(String result)
    {
        throw new NucleusUserException(Localiser.msg("059006"));
    }

    /**
     * Method to set the MetaData defining the result.
     * Setting this will unset the resultClass.
     * @param qrmd Query Result MetaData
     */
    public void setResultMetaData(QueryResultMetaData qrmd)
    {
        this.resultMetaData = qrmd;
        super.setResultClass(null);
    }

    /**
     * Set the result class for the results.
     * Setting this will unset the resultMetaData.
     * @param result_cls The result class
     */
    public void setResultClass(Class result_cls)
    {
        super.setResultClass(result_cls);
        this.resultMetaData = null;
    }

    /**
     * Set the range of the results. Not applicable for SQL.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param fromIncl From element no (inclusive) to return
     * @param toExcl To element no (exclusive) to return
     * @throws NucleusUserException Always thrown.
     */
    public void setRange(int fromIncl, int toExcl)
    {
        throw new NucleusUserException(Localiser.msg("059007"));
    }

    /**
     * Method to set whether to use subclasses. Not applicable for SQL.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param subclasses Whether to use subclasses
     * @throws NucleusUserException Always thrown.
     */
    public void setSubclasses(boolean subclasses)
    {
        throw new NucleusUserException(Localiser.msg("059004"));
    }

    /**
     * Set the filter for the query. Not applicable for SQL.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param filter the query filter.
     * @throws NucleusUserException Always thrown since method not applicable
     */
    public void setFilter(String filter)
    {
        throw new NucleusUserException(Localiser.msg("059008"));
    }

    /**
     * Declare the unbound variables to be used in the query.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param variables the variables separated by semicolons.
     * @throws NucleusUserException Always thrown since method not applicable
     */
    public void declareExplicitVariables(String variables)
    {
        throw new NucleusUserException(Localiser.msg("059009"));
    }

    /**
     * Declare the explicit parameters to be used in the query.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param parameters the parameters separated by semicolons.
     * @exception NucleusUserException Always thrown.
     */
    public void declareExplicitParameters(String parameters)
    {
        throw new NucleusUserException(Localiser.msg("059016"));
    }

    /**
     * Set the import statements to be used to identify the fully qualified name of variables or parameters.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param imports import statements separated by semicolons.
     * @exception NucleusUserException Always thrown.
     */
    public void declareImports(String imports)
    {
        throw new NucleusUserException(Localiser.msg("059026"));
    }

    /**
     * Set the grouping specification for the result Collection.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param grouping the grouping specification.
     * @throws NucleusUserException  Always thrown.
     */
    public void setGrouping(String grouping)
    {
        throw new NucleusUserException(Localiser.msg("059010"));
    }

    /**
     * Set the ordering specification for the result Collection.
     * This implementation always throws a NucleusUserException since this concept doesn't apply to SQL queries.
     * @param ordering  the ordering specification.
     * @throws NucleusUserException  Always thrown.
     */
    public void setOrdering(String ordering)
    {
        throw new NucleusUserException(Localiser.msg("059011"));
    }

    /**
     * Execute the query to delete persistent objects.
     * @param parameters the Map containing all of the parameters.
     * @return the filtered QueryResult of the deleted objects.
     */
    protected long performDeletePersistentAll(Map parameters)
    {
        throw new NucleusUserException(Localiser.msg("059000"));
    }

    /**
     * Execute the query and return the filtered List.
     * Override the method in Query since we want the parameter names to be Integer based here.
     * @param parameters the Object array with all of the parameters.
     * @return The query results
     */
    public Object executeWithArray(Object[] parameters)
    {
        // Convert the input array into a Map with Integer keys 1, 2, etc
        HashMap parameterMap = new HashMap();
        if (parameters != null)
        {
            for (int i = 0; i < parameters.length; ++i)
            {
                parameterMap.put(Integer.valueOf(i+1), parameters[i]);
            }
        }

        // Prepare for execution
        Map executionMap = prepareForExecution(parameterMap);

        // Execute using superclass method
        return super.executeQuery(executionMap);
    }

    /**
     * Execute the query using the input Map of parameters.
     * @param executeParameters the Map of the parameters passed in to execute().
     * @return The query results
     */
    public Object executeWithMap(Map executeParameters)
    {
        // Prepare for execution
        Map executionMap = prepareForExecution(executeParameters);

        // Execute using superclass method
        return super.executeQuery(executionMap);
    }

    /**
     * Method to process the input parameters preparing the statement and parameters for execution.
     * The parameters returned are ready for execution. Compiles the query, and updates the 
     * "compiledSQL" and "parameterNames".
     * Supports positional parameters, numbered parameters (?1, ?2), and named parameters (:p1, :p3).
     * If using named parameters then the keys of the Map must align to the names in the SQL.
     * If using numbered/positional parameters then the keys of the Map must be Integer and align with the
     * parameter numbers/positions.
     * @param executeParameters The input parameters map
     * @return Map of parameters for execution
     */
    protected Map prepareForExecution(Map executeParameters)
    {
        Map params = new HashMap();
        if (implicitParameters != null)
        {
            // Add any implicit parameters defined via the API
            params.putAll(implicitParameters);
        }
        if (executeParameters != null)
        {
            // Add any parameters defined at execute()
            params.putAll(executeParameters);
        }

        compileInternal(executeParameters);

        // Clear the parameterNames that are set in compile since we assign ours using the parameterMap passed in
        List paramNames = new ArrayList();

        // Build up list of expected parameters (in the order the query needs them)
        // Allow for positional parameters ('?'), numbered parameters ("?1") or named parameters (":myParam")
        Collection expectedParams = new ArrayList();
        boolean complete = false;
        int charPos = 0;
        char[] statement = compiledSQL.toCharArray();
        StringBuilder paramName = null;
        int paramPos = 0;
        boolean colonParam = true;
        StringBuilder runtimeJdbcText = new StringBuilder();
        while (!complete)
        {
            char c = statement[charPos];
            boolean endOfParam = false;
            if (c == '?')
            {
                // New positional/numbered parameter
                colonParam = false;
                paramPos++;
                paramName = new StringBuilder();
            }
            else if (c == ':')
            {
                // New named parameter
                if (charPos > 0)
                {
                    char prev = statement[charPos-1];
                    if (Character.isLetterOrDigit(prev))
                    {
                        // Some valid SQL can include colon, so ignore if the part just before is alphanumeric
                    }
                    else
                    {
                        colonParam = true;
                        paramPos++;
                        paramName = new StringBuilder();
                    }
                }
                else
                {
                    colonParam = true;
                    paramPos++;
                    paramName = new StringBuilder();
                }
            }
            else
            {
                if (paramName != null)
                {
                    if (Character.isLetterOrDigit(c))
                    {
                        // Allow param names to include alphnumeric
                        paramName.append(c);
                    }
                    else
                    {
                        endOfParam = true;
                    }
                }
            }
            if (paramName != null)
            {
                if (endOfParam)
                {
                    // Replace the param by "?" in the runtime SQL
                    runtimeJdbcText.append('?');
                    runtimeJdbcText.append(c);
                }
            }
            else
            {
                runtimeJdbcText.append(c);
            }

            charPos++;

            complete = (charPos == compiledSQL.length());
            if (complete && paramName != null && !endOfParam)
            {
                runtimeJdbcText.append('?');
            }

            if (paramName != null && (complete || endOfParam))
            {
                // Process the parameter
                if (paramName.length() > 0)
                {
                    // Named/Numbered parameter
                    if (colonParam)
                    {
                        expectedParams.add(paramName.toString());
                    }
                    else
                    {
                        try
                        {
                            Integer num = Integer.valueOf(paramName.toString());
                            expectedParams.add(num);
                        }
                        catch (NumberFormatException nfe)
                        {
                            throw new NucleusUserException("SQL query " + inputSQL + 
                                " contains an invalid parameter specification " + paramName.toString());
                        }
                    }
                }
                else
                {
                    if (!colonParam)
                    {
                        // Positional parameter
                        expectedParams.add(Integer.valueOf(paramPos));
                    }
                    else
                    {
                        // Just a colon so ignore it
                    }
                }
                paramName = null;
            }
        }
        compiledSQL = runtimeJdbcText.toString(); // Update the SQL that JDBC will receive to just have ? for a parameter

        if (expectedParams.size() > 0 && params.isEmpty())
        {
            // We expect some parameters yet the user gives us none!
            throw new NucleusUserException(Localiser.msg("059028", inputSQL, "" + expectedParams.size()));
        }

        // Build a Map of params with keys 1, 2, 3, etc representing the position in the runtime JDBC SQL
        Map executeMap = new HashMap();

        // Cycle through the expected params
        paramPos = 1;
        Iterator expectedParamIter = expectedParams.iterator();
        while (expectedParamIter.hasNext())
        {
            Object key = expectedParamIter.next();
            if (!params.containsKey(key))
            {
                // Expected parameter is not provided
                throw new NucleusUserException(Localiser.msg("059030", inputSQL, "" + key));
            }

            executeMap.put(Integer.valueOf(paramPos), params.get(key));
            paramNames.add("" + paramPos);
            paramPos++;
        }
        parameterNames = (String[])paramNames.toArray(new String[paramNames.size()]);

        return executeMap;
    }

    /**
     * Convenience method to return whether the query should return a single row.
     * @return Whether it represents a unique row
     */
    protected boolean shouldReturnSingleRow()
    {
        if (unique)
        {
            return true;
        }
        // An SQL query returns what it returns
        return false;
    }
}