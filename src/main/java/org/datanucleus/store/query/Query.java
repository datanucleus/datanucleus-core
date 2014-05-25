/**********************************************************************
Copyright (c) 2002 Kelly Grizzle (TJDO) and others. All rights reserved.
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
2003 Andy Jefferson - commented and localised
2003 Andy Jefferson - coding standards
2004 Andy Jefferson - addition of setUnique() and setRange()
2005 Andy Jefferson - added timeout support.
2009 Andy Jefferson - added ranges. added cancel/timeout API
    ...
**********************************************************************/
package org.datanucleus.store.query;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.ExecutionContextListener;
import org.datanucleus.FetchPlan;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUnsupportedOptionException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.exceptions.TransactionNotActiveException;
import org.datanucleus.exceptions.TransactionNotReadableException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.QueryResultMetaData;
import org.datanucleus.query.JDOQLQueryHelper;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;
import org.datanucleus.store.Extent;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Imports;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Abstract implementation for all queries in DataNucleus.
 * Implementations of JDOQL, SQL, JPQL, etc should extend this.
 * Parameters can be implicit (defined in the query via syntaxes such as ":name", "?1")
 * or explicit (defined via declareParameters). They can also be named or numbered.
 * When passing a map of parameters with values, they are keyed by String (named parameters)
 * or Integer (numbered parameters).
 */
public abstract class Query implements Serializable, ExecutionContextListener
{
    public static final String EXTENSION_FLUSH_BEFORE_EXECUTION = PropertyNames.PROPERTY_QUERY_FLUSH_BEFORE_EXECUTE;
    public static final String EXTENSION_USE_FETCH_PLAN = PropertyNames.PROPERTY_QUERY_USE_FETCHPLAN;
    public static final String EXTENSION_RESULT_SIZE_METHOD = PropertyNames.PROPERTY_QUERY_RESULT_SIZE_METHOD;
    public static final String EXTENSION_LOAD_RESULTS_AT_COMMIT = PropertyNames.PROPERTY_QUERY_LOAD_RESULTS_AT_COMMIT;
    public static final String EXTENSION_COMPILATION_CACHED = PropertyNames.PROPERTY_QUERY_COMPILATION_CACHED;
    public static final String EXTENSION_RESULTS_CACHED = PropertyNames.PROPERTY_QUERY_RESULTS_CACHED;
    public static final String EXTENSION_EVALUATE_IN_MEMORY = PropertyNames.PROPERTY_QUERY_EVALUATE_IN_MEMORY;
    public static final String EXTENSION_CHECK_UNUSED_PARAMETERS = PropertyNames.PROPERTY_QUERY_CHECK_UNUSED_PARAMS;

    public static final String EXTENSION_MULTITHREAD = "datanucleus.query.multithread";
    public static final String EXTENSION_RESULT_CACHE_TYPE = "datanucleus.query.resultCacheType";
    public static final String EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE = "datanucleus.query.closeResultsAtManagerClose";

    protected final transient StoreManager storeMgr;

    protected transient ExecutionContext ec;

    protected final transient ClassLoaderResolver clr;

    public static final short SELECT = 0;
    public static final short BULK_UPDATE = 1;
    public static final short BULK_DELETE = 2;
    public static final short OTHER = 3;

    /** Type of query. */
    protected short type = SELECT;

    /** The candidate class for this query. */
    protected Class candidateClass;

    /** Name of the candidate class (used when specified via Single-String). */
    protected String candidateClassName;

    /** Whether to allow subclasses of the candidate class be returned. */
    protected boolean subclasses = true;

    /** Whether to return single value, or collection from the query. */
    protected boolean unique = false;

    /** From clause of the query (optional). */
    protected transient String from = null;

    /** UPDATE clause of a query. */
    protected transient String update = null;

    /** Specification of the result of the query e.g aggregates etc. Doesn't include any "distinct". */
    protected String result = null;

    /** Whether the results are marked as distinct. This is extracted out of the result for clarity. */
    protected boolean resultDistinct = false;

    /** User-defined class that best represents the results of a query. Populated if specified via setResultClass(). */
    protected Class resultClass = null;

    /** Temporary variable for the name of the result class (may need resolving using imports). */
    protected String resultClassName = null;

    /** The filter for the query. */
    protected String filter;

    /** Any import declarations for the types used in the query, semicolon separated. */
    protected String imports;

    /** Any explicit variables defined for this query, semicolon separated. */
    protected String explicitVariables;

    /** Any explicit parameters defined for this query, comma separated. */
    protected String explicitParameters;

    /** Ordering clause for the query, governing the order objects are returned. */
    protected String ordering;

    /** Grouping clause for the query, for use with aggregate expressions. */
    protected String grouping;

    /** Having clause for the query */
    protected String having;

    /** String form of the query result range. For convenience only. */
    protected String range;

    /** Query result range start position (inclusive). */
    protected long fromInclNo = 0;

    /** Query result range end position (exclusive). */
    protected long toExclNo = Long.MAX_VALUE;

    /** Query result range lower limit (inclusive) as a parameter name. */
    protected String fromInclParam = null;

    /** Query result range upper limit (exclusive) as a parameter name. */
    protected String toExclParam = null;

    /** Whether the query can be modified */
    protected boolean unmodifiable = false;

    /** Whether to ignore dirty instances in the query. */
    protected boolean ignoreCache = false;

    /** Fetch Plan to use for the query. */
    private FetchPlan fetchPlan;

    /** Whether to serialise (lock) any read objects from this query. */
    private Boolean serializeRead = null;

    /** Read timeout (milliseconds), if any. */
    private Integer readTimeoutMillis = null;

    /** Write timeout (milliseconds), if any. */
    private Integer writeTimeoutMillis = null;

    /** Any extensions */
    protected Map<String, Object> extensions = null;

    /** Any subqueries, keyed by the variable name that they represent. */
    protected Map<String, SubqueryDefinition> subqueries = null;

    /**
     * Map of implicit parameters, keyed by the name/number. 
     * Named parameters are keyed by String form of the name.
     * Numbered parameters are keyed by the position (Integer).
     */
    protected transient HashMap implicitParameters = null;

    /** The imports definition. */
    protected transient Imports parsedImports = null;

    /** Array of (explicit) parameter names. */
    protected transient String[] parameterNames = null;

    /** Query compilation (when using the generic query compiler). */
    protected transient QueryCompilation compilation = null;

    /**
     * All query results obtained from this query.
     * This is required because the query can be executed multiple times changing
     * the input slightly each time for example.
     */
    protected transient HashSet<QueryResult> queryResults = new HashSet(1);

    /** Currently executing object for this query, keyed by the thread, to allow for cancellation. */
    protected transient Map<Thread, Object> tasks = new ConcurrentHashMap(1);

    /**
     * Constructs a new query instance that uses the given object manager.
     * @param storeMgr Store Manager used for this query
     * @param ec execution context
     */
    public Query(StoreManager storeMgr, ExecutionContext ec)
    {
        this.storeMgr = storeMgr;
        this.ec = ec;
        if (ec == null)
        {
            // EX should always be provided so throw exception if null
            throw new NucleusUserException(Localiser.msg("021012"));
        }
        this.clr = ec.getClassLoaderResolver();

        this.ignoreCache = ec.getBooleanProperty(PropertyNames.PROPERTY_IGNORE_CACHE);
        this.readTimeoutMillis = ec.getIntProperty(PropertyNames.PROPERTY_DATASTORE_READ_TIMEOUT);
        this.writeTimeoutMillis = ec.getIntProperty(PropertyNames.PROPERTY_DATASTORE_WRITE_TIMEOUT);

        boolean closeAtEcClose = getBooleanExtensionProperty(EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE, false);
        if (closeAtEcClose)
        {
            // Register this query as a listener for ExecutionContext closure, so we can read in all results as required.
            ec.registerExecutionContextListener(this);
        }
    }

    /**
     * Whether this query should cache the results from the times it is ran. With JDO we would do this
     * since it has a close() method to clear them out. With JPA we typically would not do this since
     * there is no close() capability.
     * @param cache Whether to cache the query results obtained by an execute() call.
     */
    public void setCacheResults(boolean cache)
    {
        if (cache && queryResults == null)
        {
            queryResults = new HashSet<QueryResult>();
        }
        else if (!cache)
        {
            queryResults = null;
        }
    }

    /**
     * Accessor for the query language.
     * @return Query language
     */
    public String getLanguage()
    {
        throw new UnsupportedOperationException("Query Language accessor not supported in this query");
    }

    /**
     * Utility to remove any previous compilation of this Query.
     */
    protected void discardCompiled()
    {
        parsedImports = null;
        parameterNames = null;
        compilation = null;
    }

    /**
     * Method to set the generic compilation for this query. This is used where we are generating
     * the query via a criteria API, and so have the single-string form and the compilation ready
     * when we create the query itself.
     * @param compilation The compilation
     */
    public void setCompilation(QueryCompilation compilation)
    {
        this.compilation = compilation;
        if (compilation != null && NucleusLogger.QUERY.isDebugEnabled())
        {
            // Log the query compilation
            NucleusLogger.QUERY.debug(compilation.toString());
        }
    }

    /**
     * Equality operator.
     * @param obj Object to compare against
     * @return Whether this and the other object are equal.
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (!(obj instanceof Query))
        {
            return false;
        }

        Query q = (Query)obj;

        if (candidateClass == null) 
        {
            if (q.candidateClass != null)
            {
                return false;
            }
        }
        else if (!candidateClass.equals(q.candidateClass))
        {
            return false;
        }

        if (filter == null) 
        {
            if (q.filter != null)
            {
                return false;
            }
        }
        else if (!filter.equals(q.filter))
        {
            return false;
        }

        if (imports == null)
        {
            if (q.imports != null)
            {
                return false;
            }
        }
        else if (!imports.equals(q.imports)) 
        {
            return false;
        }

        if (explicitParameters == null)
        {
            if (q.explicitParameters != null)
            {
                return false;
            }
        }
        else if (!explicitParameters.equals(q.explicitParameters))
        {
            return false;
        }

        if (explicitVariables == null)
        {
            if (q.explicitVariables != null)
            {
                return false;
            }
        }
        else if (!explicitVariables.equals(q.explicitVariables))
        {
            return false;
        }

        if (unique != q.unique)
        {
            return false;
        }

        if (serializeRead != q.serializeRead)
        {
            return false;
        }

        if (unmodifiable != q.unmodifiable)
        {
            return false;
        }

        if (resultClass != q.resultClass)
        {
            return false;
        }

        if (grouping == null)
        {
            if (q.grouping != null)
            {
                return false;
            }
        }
        else if (!grouping.equals(q.grouping))
        {
            return false;
        }

        if (ordering == null)
        {
            if (q.ordering != null)
            {
                return false;
            }
        }
        else if (!ordering.equals(q.ordering))
        {
            return false;
        }

        return true;
    }

    /**
     * Hashcode generator.
     * @return The Hashcode for this object.
     */
    public int hashCode()
    {
        return (candidateClass == null ? 0 : candidateClass.hashCode()) ^
            (result == null ? 0 : result.hashCode()) ^
        	(filter == null ? 0 : filter.hashCode()) ^
        	(imports == null ? 0 : imports.hashCode()) ^
        	(explicitParameters == null ? 0 : explicitParameters.hashCode()) ^
        	(explicitVariables == null ? 0 : explicitVariables.hashCode()) ^
        	(resultClass == null ? 0 : resultClass.hashCode()) ^
        	(grouping == null ? 0 : grouping.hashCode()) ^
            (having == null ? 0 : having.hashCode()) ^
        	(ordering == null ? 0 : ordering.hashCode()) ^
            (range == null ? 0 : range.hashCode());
    }

    /**
     * Accessor for the query type.
     * @return The query type
     */
    public short getType()
    {
        return type;
    }

    /**
     * Mutator to set the query type.
     * @param type The query type
     */
    public void setType(short type)
    {
        if (type == SELECT || type == BULK_UPDATE || type == BULK_DELETE)
        {
            this.type = type;
        }
        else
        {
            throw new NucleusUserException(
                "Query only supports types of SELECT, BULK_UPDATE, BULK_DELETE : unknown value " + type);
        }
    }

    /**
     * Accessor for the StoreManager associated with this Query.
     * @return the StoreManager associated with this Query.
     */
    public StoreManager getStoreManager()
    {
        return storeMgr;
    }

    /**
     * Accessor for the Execution Context associated with this Query.
     * @return Execution Context for the query
     */
    public ExecutionContext getExecutionContext()
    {
        return ec;
    }

    public void executionContextClosing(ExecutionContext ec)
    {
        closeAll();
        this.ec = null;
    }

    /**
     * Add a vendor-specific extension this query. The key and value are not standard.
     * An implementation must ignore keys that are not recognized.
     * @param key the extension key
     * @param value the extension value
     */
    public void addExtension(String key, Object value)
    {
        if (extensions == null)
        {
            extensions = new HashMap();
        }
        extensions.put(key, value);
        if (key.equals(EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE))
        {
            boolean closeAtEcClose = getBooleanExtensionProperty(EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE, false);
            if (closeAtEcClose)
            {
                ec.registerExecutionContextListener(this);
            }
            else
            {
                ec.deregisterExecutionContextListener(this);
            }
        }
    }

    /**
     * Set multiple extensions, or use null to clear extensions.
     * Map keys and values are not standard.
     * An implementation must ignore entries that are not recognized.
     * @param extensions Any extensions
     * @see #addExtension
     */
    public void setExtensions(Map extensions)
    {
        this.extensions = (extensions != null ? new HashMap(extensions) : null);
        if (extensions.containsKey(EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE))
        {
            boolean closeAtEcClose = getBooleanExtensionProperty(EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE, false);
            if (closeAtEcClose)
            {
                ec.registerExecutionContextListener(this);
            }
            else
            {
                ec.deregisterExecutionContextListener(this);
            }
        }
    }

    /**
     * Accessor for the value of an extension for this query.
     * @param key The key
     * @return The value (if this extension is specified)
     */
    public Object getExtension(String key)
    {
        return (extensions != null ? extensions.get(key) : null);
    }

    /**
     * Accessor for the extensions defined for this query.
     * @return Extensions
     */
    public Map<String, Object> getExtensions()
    {
        return extensions;
    }

    /**
     * Convenience accessor to return whether an extension is set (or whether the persistence property
     * of the same name is set), and what is its boolean value. Returns "resultIfNotSet" if not set.
     * @param name The extension/property name
     * @param resultIfNotSet The value to return if there is neither an extension nor a persistence
     *                       property of the same name
     * @return The boolean value
     */
    public boolean getBooleanExtensionProperty(String name, boolean resultIfNotSet)
    {
        if (extensions != null && extensions.containsKey(name))
        {
            Object value = extensions.get(name);
            if (value instanceof Boolean)
            {
                return (Boolean)value;
            }
            else
            {
                return Boolean.valueOf((String)value);
            }
        }
        else
        {
            return ec.getNucleusContext().getConfiguration().getBooleanProperty(name, resultIfNotSet);
        }
    }

    /**
     * Convenience accessor to return whether an extension is set (or whether the persistence property
     * of the same name is set), and what is its String value. Returns "resultIfNotSet" if not set.
     * @param name The extension/property name
     * @param resultIfNotSet The value to return if there is neither an extension nor a persistence
     *                       property of the same name
     * @return The String value
     */
    public String getStringExtensionProperty(String name, String resultIfNotSet)
    {
        if (extensions != null && extensions.containsKey(name))
        {
            return (String)extensions.get(name);
        }
        else
        {
            String value = ec.getNucleusContext().getConfiguration().getStringProperty(name);
            return (value != null ? value : resultIfNotSet);
        }
    }

    /**
     * Method to return the names of the extensions supported by this query.
     * To be overridden by subclasses where they support additional extensions.
     * @return The supported extension names
     */
    public Set<String> getSupportedExtensions()
    {
        Set<String> extensions = new HashSet<String>();
        extensions.add(EXTENSION_FLUSH_BEFORE_EXECUTION);
        extensions.add(EXTENSION_USE_FETCH_PLAN);
        extensions.add(EXTENSION_RESULT_SIZE_METHOD);
        extensions.add(EXTENSION_LOAD_RESULTS_AT_COMMIT);
        extensions.add(EXTENSION_RESULT_CACHE_TYPE);
        extensions.add(EXTENSION_RESULTS_CACHED);
        extensions.add(EXTENSION_COMPILATION_CACHED);
        extensions.add(EXTENSION_MULTITHREAD);
        extensions.add(EXTENSION_EVALUATE_IN_MEMORY);
        extensions.add(EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE);
        return extensions;
    }

    /**
     * This method retrieves the fetch plan associated with the Query. It always returns
     * the identical instance for the same Query instance. Any change made to the fetch plan 
     * affects subsequent query execution. Fetch plan is described in JDO2 $12.7
     * @return the FetchPlan
     */
    public FetchPlan getFetchPlan()
    {
        if (fetchPlan == null)
        {
            // Copy the FetchPlan of the ExecutionContext
            this.fetchPlan = ec.getFetchPlan().getCopy();
        }

        return fetchPlan;
    }

    /**
     * Mutator for the FetchPlan of the query.
     * This is called when applying a named FetchPlan.
     * @param fp The FetchPlan
     */
    public void setFetchPlan(FetchPlan fp)
    {
        // Update the FetchPlan
        this.fetchPlan = fp;
    }

    /**
     * Set the UPDATE clause of the query.
     * @param update the update clause
     */
    public void setUpdate(String update)
    {
        discardCompiled();
        assertIsModifiable();
    
        this.update = update;
    }

    /**
     * Accessor for the UPDATE clause of the JPQL query.
     * @return Update clause
     */
    public String getUpdate()
    {
        return update;
    }

    /**
     * Accessor for the class of the candidate instances of the query.
     * @return the Class of the candidate instances.
     */
    public Class getCandidateClass()
    {
        return candidateClass;
    }

    /**
     * Mutator for the class of the candidate instances of the query.
     * @param candidateClass the Class of the candidate instances.
     */
    public void setCandidateClass(Class candidateClass)
    {
        discardCompiled();
        assertIsModifiable();

        this.candidateClassName = (candidateClass != null ? candidateClass.getName() : null);
        this.candidateClass = candidateClass;
    }

    /**
     * Convenience method to set the name of the candidate class.
     * @param candidateClassName Name of the candidate class
     */
    public void setCandidateClassName(String candidateClassName)
    {
        this.candidateClassName = (candidateClassName != null ? candidateClassName.trim() : null);
    }

    /**
     * Accessor for the candidate class name.
     * @return Name of the candidate class (if any)
     */
    public String getCandidateClassName()
    {
        return candidateClassName;
    }

    protected AbstractClassMetaData getCandidateClassMetaData()
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(candidateClass, clr);
        if (candidateClass.isInterface())
        {
            // Query of interface
            String[] impls = ec.getMetaDataManager().getClassesImplementingInterface(candidateClass.getName(), clr);
            if (impls.length == 1 && cmd.isImplementationOfPersistentDefinition())
            {
                // Only the generated implementation, so just use its metadata
            }
            else
            {
                // Use metadata for the persistent interface
                cmd = ec.getMetaDataManager().getMetaDataForInterface(candidateClass, clr);
                if (cmd == null)
                {
                    throw new NucleusUserException("Attempting to query an interface yet it is not declared 'persistent'." +
                        " Define the interface in metadata as being persistent to perform this operation, and make sure" +
                        " any implementations use the same identity and identity member(s)");
                }
            }
        }

        return cmd;
    }

    /**
     * Set the candidates to the query.
     * @param from the candidates
     */
    public void setFrom(String from)
    {
        discardCompiled();
        assertIsModifiable();
    
        this.from = from;
    }

    /**
     * Accessor for the FROM clause of the query.
     * @return From clause
     */
    public String getFrom()
    {
        return from;
    }

    /**
     * Set the candidate Extent to query. To be implemented by extensions.
     * @param pcs the Candidate Extent.
     */
    public abstract void setCandidates(Extent pcs);

    /**
     * Set the candidate Collection to query. To be implemented by extensions.
     * @param pcs the Candidate collection.
     */
    public abstract void setCandidates(Collection pcs);

    /**
     * Set the filter for the query.
     * @param filter the query filter.
     */
    public void setFilter(String filter)
    {
        discardCompiled();
        assertIsModifiable();

        this.filter =
            (StringUtils.isWhitespace(filter) ? null :
                StringUtils.removeSpecialTagsFromString(filter).trim());
    }

    /**
     * Accessor for the filter specification.
     * @return Filter specification
     */
    public String getFilter()
    {
        return filter;
    }

    /**
     * Set the import statements to be used to identify the fully qualified name
     * of variables or parameters.
     * @param imports import statements separated by semicolons.
     */
    public void declareImports(String imports)
    {
        discardCompiled();
        assertIsModifiable();

        this.imports =
            (StringUtils.isWhitespace(imports) ? null :
                StringUtils.removeSpecialTagsFromString(imports).trim());
    }

    /**
     * Accessor for the imports specification.
     * @return Imports specification
     */
    public String getImports()
    {
        return imports;
    }

    /**
     * Method to define the explicit parameters.
     * @param parameters the list of parameters separated by commas
     */
    public void declareExplicitParameters(String parameters)
    {
        discardCompiled();
        assertIsModifiable();

        this.explicitParameters = 
            (StringUtils.isWhitespace(parameters) ? null : 
                StringUtils.removeSpecialTagsFromString(parameters).trim());
    }

    /**
     * Accessor for the explicit parameters specification.
     * @return Explicit parameters specification
     */
    public String getExplicitParameters()
    {
        return explicitParameters;
    }

    /**
     * Method to set the value of a named implicit parameter where known before execution.
     * @param name Name of the parameter
     * @param value Value of the parameter
     * @throws QueryInvalidParametersException if the parameter is invalid
     */
    public void setImplicitParameter(String name, Object value)
    {
        if (implicitParameters == null)
        {
            implicitParameters = new HashMap();
        }
        implicitParameters.put(name, value);

        if (compilation == null)
        {
            // Compile since we need to know here if the parameter is invalid
            discardCompiled();
            compileInternal(implicitParameters);
        }

        // Perform any checks on parameter validity, and apply its value
        applyImplicitParameterValueToCompilation(name, value);
    }

    /**
     * Method to set the value of a numbered implicit parameter where known before execution.
     * @param position Position of the parameter
     * @param value Value of the parameter
     * @throws QueryInvalidParametersException if the parameter is invalid
     */
    public void setImplicitParameter(int position, Object value)
    {
        if (implicitParameters == null)
        {
            implicitParameters = new HashMap();
        }
        implicitParameters.put(Integer.valueOf(position), value);

        if (compilation == null)
        {
            // Compile since we need to know here if the parameter is invalid
            discardCompiled();
            compileInternal(implicitParameters);
        }

        // Perform any checks on parameter validity, and apply its value
        applyImplicitParameterValueToCompilation("" + position, value);
    }

    /**
     * Convenience method to apply an implicit parameter value to the compilation symbol table.
     * If the (generic) compilation doesn't exist then does nothing.
     * If the parameter doesn't exist in the symbol table then an exception is thrown (since no point
     * providing a parameter if not in the query).
     * @param name Name of the parameter
     * @param value Value of the parameter
     * @throws QueryInvalidParametersException if the parameter doesn't exist in the query or if the type
     *     of the parameter provided is inconsistent with the query
     */
    protected void applyImplicitParameterValueToCompilation(String name, Object value)
    {
        if (compilation == null)
        {
            return;
        }

        // Apply to the main query
        boolean symbolFound = false;
        Symbol sym = compilation.getSymbolTable().getSymbol(name);
        if (sym != null)
        {
            symbolFound = true;
            if (sym.getValueType() == null && value != null)
            {
                // Update the compilation providing the type of this parameter
                sym.setValueType(value.getClass());
            }
            else if (sym.getValueType() != null && value != null)
            {
                if (!QueryUtils.queryParameterTypesAreCompatible(sym.getValueType(), value.getClass()))
                {
                    // Parameter value supplied is not consistent with what the query compilation expects
                    throw new QueryInvalidParametersException("Parameter " + name +
                        " needs to be assignable from " + sym.getValueType().getName() +
                        " yet the value is of type " + value.getClass().getName());
                }
            }
        }

        // Apply to any subqueries
        boolean subSymbolFound = applyImplicitParameterValueToSubqueries(name, value, compilation);
        if (subSymbolFound) 
        {
        	symbolFound = true;
        }
        
        if (!symbolFound)
        {
            // No reference to this parameter was found in the compilation, so throw exception
            throw new QueryInvalidParametersException(Localiser.msg("021116", name));
        }
    }
    
    protected boolean applyImplicitParameterValueToSubqueries(String name, Object value, QueryCompilation comp)
    {
    	boolean symbolFound = false;
    	Symbol sym = null;
        // Apply to any subqueries
        String[] subqueryNames = comp.getSubqueryAliases();
        if (subqueryNames != null)
        {
            for (int i=0;i<subqueryNames.length;i++)
            {
                QueryCompilation subCompilation = comp.getCompilationForSubquery(subqueryNames[i]);
                sym = subCompilation.getSymbolTable().getSymbol(name);
                if (sym != null)
                {
                    symbolFound = true;
                    if (sym.getValueType() == null && value != null)
                    {
                        // Update the compilation providing the type of this parameter
                        sym.setValueType(value.getClass());
                    }
                    else if (sym.getValueType() != null && value != null)
                    {
                        if (!QueryUtils.queryParameterTypesAreCompatible(sym.getValueType(), value.getClass()))
                        {
                            // Parameter value supplied is not consistent with what the query compilation expects
                            throw new QueryInvalidParametersException("Parameter " + name +
                                " needs to be assignable from " + sym.getValueType().getName() +
                                " yet the value is of type " + value.getClass().getName());
                        }
                    }
                }
                boolean subSymbolFound = applyImplicitParameterValueToSubqueries(name, value, subCompilation);
                if (subSymbolFound) 
                {
                	symbolFound = true;
                }
            }
        }
    	return symbolFound;
    }

    /**
     * Accessor for the implicit parameters.
     * Named params are keyed by the name. Positional params are keyed by the Integer(position).
     * @return Implicit params
     */
    public Map getImplicitParameters()
    {
        return implicitParameters;
    }

    /**
     * Method to define the explicit variables for the query.
     * @param variables the variables separated by semicolons.
     */
    public void declareExplicitVariables(String variables)
    {
        discardCompiled();
        assertIsModifiable();

        this.explicitVariables =
            (StringUtils.isWhitespace(variables) ? null :
                StringUtils.removeSpecialTagsFromString(variables).trim());
    }

    /**
     * Accessor for the explicit variables specification.
     * @return Explicit variables specification
     */
    public String getExplicitVariables()
    {
        return explicitVariables;
    }

    /**
     * Set the ordering specification for the result Collection.
     * @param ordering the ordering specification.
     */
    public void setOrdering(String ordering)
    {
        discardCompiled();
        assertIsModifiable();

        this.ordering = (ordering != null ? ordering.trim() : null);
    }

    /**
     * Accessor for the ordering string for the query.
     * @return Ordering specification
     */
    public String getOrdering()
    {
        return ordering;
    }

    /**
     * Set the grouping specification for the result Collection.
     * @param grouping the grouping specification.
     */
    public void setGrouping(String grouping)
    {
        discardCompiled();
        assertIsModifiable();

        this.grouping = (grouping != null ? grouping.trim() : null);
    }

    /**
     * Accessor for the grouping string for the query.
     * @return Grouping specification
     */
    public String getGrouping()
    {
        return grouping;
    }

    /**
     * Set the having specification for the result Collection.
     * @param having the having specification.
     */
    public void setHaving(String having)
    {
        discardCompiled();
        assertIsModifiable();

        this.having = (having != null ? having.trim() : null);
    }

    /**
     * Accessor for the having string for the query.
     * @return Having specification
     */
    public String getHaving()
    {
        return having;
    }

    /**
     * Set the uniqueness of the results. A value of true will return a single
     * value (or null) where the application knows that there are 0 or 1
     * objects to be returned. See JDO 2.0 specification $14.6
     * @param unique whether the result is unique
     */
    public void setUnique(boolean unique)
    {
        discardCompiled();
        assertIsModifiable();

        this.unique = unique;
    }

    /**
     * Accessor for whether the query results are unique.
     * @return Whether it is unique
     */
    public boolean isUnique()
    {
        return unique;
    }

    /**
     * Set the range of the results. By default all results are returned but
     * this allows specification of a range of elements required. See JDO 2.0
     * specification section 14.6.8
     * @param fromIncl From element no (inclusive) to return
     * @param toExcl To element no (exclusive) to return
     */
    public void setRange(long fromIncl, long toExcl)
    {
        discardCompiled();

        // JDO2 spec 14.6 setRange is valid when unmodifiable so don't check it
        if (fromIncl >= 0 && fromIncl < Long.MAX_VALUE)
        {
            this.fromInclNo = fromIncl;
        }
        if (toExcl >= 0)
        {
            this.toExclNo = toExcl;
        }
        this.fromInclParam = null;
        this.toExclParam = null;
        this.range = "" + fromInclNo + "," + toExclNo;
    }

    /**
     * Set the range of the results. By default all results are returned but
     * this allows specification of a range of elements required. See JDO 2.0
     * specification section 14.6.8
     * @param range Range string
     */
    public void setRange(String range)
    {
        discardCompiled();

        this.range = range;
        if (range == null)
        {
            // Cater for unsetting
            fromInclNo = 0;
            fromInclParam = null;
            toExclNo = Long.MAX_VALUE;
            toExclParam = null;
            return;
        }

        StringTokenizer tok = new StringTokenizer(range, ",");

        // Get lower limit
        if (!tok.hasMoreTokens())
        {
            throw new NucleusUserException("Invalid range. Expected 'lower, upper'");
        }
        String first = tok.nextToken().trim();
        try
        {
            fromInclNo = Long.valueOf(first);
        }
        catch (NumberFormatException nfe)
        {
            fromInclNo = 0;
            fromInclParam = first.trim();
            if (fromInclParam.startsWith(":"))
            {
                fromInclParam = fromInclParam.substring(1);
            }
        }

        // Get upper limit
        if (!tok.hasMoreTokens())
        {
            throw new NucleusUserException("Invalid range. Expected 'lower, upper'");
        }
        String second = tok.nextToken().trim();
        try
        {
            toExclNo = Long.valueOf(second);
        }
        catch (NumberFormatException nfe)
        {
            toExclNo = Long.MAX_VALUE;
            toExclParam = second.trim();
            if (toExclParam.startsWith(":"))
            {
                toExclParam = toExclParam.substring(1);
            }
        }
    }

    /**
     * Accessor for the range specification string.
     * @return Range specification
     */
    public String getRange()
    {
        return range;
    }

    /**
     * Accessor for the range lower limit (inclusive).
     * @return Range lower limit
     */
    public long getRangeFromIncl()
    {
        return fromInclNo;
    }

    /**
     * Accessor for the range upper limit (exclusive).
     * @return Range upper limit
     */
    public long getRangeToExcl()
    {
        return toExclNo;
    }

    /**
     * Accessor for the range lower limit parameter (inclusive).
     * @return Range lower limit
     */
    public String getRangeFromInclParam()
    {
        return fromInclParam;
    }

    /**
     * Accessor for the range upper limit parameter (exclusive).
     * @return Range upper limit
     */
    public String getRangeToExclParam()
    {
        return toExclParam;
    }

    /**
     * Set the result for the results.
     * @param result Comma-separated result expressions
     */
    public void setResult(String result)
    {
        discardCompiled();
        assertIsModifiable();

        // Should be overridden if wanting specific handling of result "distinct"
        this.result = (result != null ? result.trim() : null);
    }

    /**
     * Accessor for the result specification string.
     * @return Result specification
     */
    public String getResult()
    {
        return result;
    }

    /**
     * Mark the result as distinct (or not).
     * This is not part of JDOQL/JPQL but provided for convenience.
     * @param distinct Whether to treat as distinct
     */
    public void setResultDistinct(boolean distinct)
    {
        this.resultDistinct = distinct;
    }

    /**
     * Accessor for whether the results are distinct.
     * By default this is extracted from the "result" clause.
     * @return Whether distinct
     */
    public boolean getResultDistinct()
    {
        return resultDistinct;
    }

    public String getResultClassName()
    {
        return resultClassName;
    }

    /**
     * Method to set the result class name, direct from a single-string query.
     * The name could be a shortened form, allowing for imports to resolve it.
     * @param resultClassName Name of the result class
     */
    public void setResultClassName(String resultClassName)
    {
        discardCompiled();

        try
        {
            this.resultClass = clr.classForName(resultClassName);
            this.resultClassName = null;
        }
        catch (ClassNotResolvedException cnre)
        {
            // Not class name, so explore other possibilities
            // We may not have Imports set yet so leave until compile process
            this.resultClassName = resultClassName;
            this.resultClass = null;
        }
    }

    /**
     * Set the result class for the results.
     * The result class must obey various things as per the JDO 2.0 spec 14.6.12.
     * @param result_cls The result class
     */
    public void setResultClass(Class result_cls)
    {
        discardCompiled();

        // JDO2 spec 14.6 setResultClass is valid when unmodifiable so don't check it
        this.resultClass = result_cls;
        this.resultClassName = null;
    }

    /**
     * Accessor for the result class.
     * @return Result class
     */
    public Class getResultClass()
    {
        return resultClass;
    }

    /**
     * Method to set the MetaData defining the result.
     * If the query doesn't support such a setting will throw a NucleusException.
     * @param qrmd QueryResultMetaData
     */
    public void setResultMetaData(QueryResultMetaData qrmd)
    {
        throw new NucleusException("This query doesn't support the use of setResultMetaData()");
    }

    /**
     * Set the ignoreCache option. Currently this simply stores the ignoreCache value, and doesn't 
     * necessarily use it. The parameter is a "hint" to the query engine.
     * @param ignoreCache the setting of the ignoreCache option.
     */
    public void setIgnoreCache(boolean ignoreCache)
    {
        discardCompiled();

        // JDO2 spec 14.6 setIgnoreCache is valid when unmodifiable so dont check it
        this.ignoreCache = ignoreCache;
    }

    /**
     * Accessor for the ignoreCache option setting.
     * @return the ignoreCache option setting
     * @see #setIgnoreCache
     */
    public boolean getIgnoreCache()
    {
        return ignoreCache;
    }

    /**
     * Accessor for whether this query includes subclasses
     * @return Returns whether the query includes subclasses.
     */
    public boolean isSubclasses()
    {
        return subclasses;
    }

    /**
     * Mutator for whether this query includes subclasses
     * @param subclasses Where subclasses of the candidate class are to be included.
     */
    public void setSubclasses(boolean subclasses)
    {
        discardCompiled();
        assertIsModifiable();

        this.subclasses = subclasses;
    }

    /**
     * Accessor for whether to serialise (lock) any read objects retrieved from this query.
     * True means that we will lock them, False means don't lock them, and null implies it is left to
     * the implementation.
     * @return Whether to lock
     */
    public Boolean getSerializeRead()
    {
        return serializeRead;
    }

    /**
     * Mutator for whether to serialise (lock) any read objects in this query.
     * @param serialize Whether to serialise (lock) the query objects
     */
    public void setSerializeRead(Boolean serialize)
    {
        discardCompiled();
        assertIsModifiable();

        this.serializeRead = serialize;
    }

    /**
     * Accessor for unmodifiable.
     * @return Returns the unmodifiable.
     */
    public boolean isUnmodifiable()
    {
        return unmodifiable;
    }

    /**
     * Method to throw an exception if the query is currently not modifiable.
     * @throws NucleusUserException Thrown when it is unmodifiable
     */
    protected void assertIsModifiable()
    {
        if (unmodifiable)
        {
            throw new NucleusUserException(Localiser.msg("021014"));
        }
    }

    /**
     * Mutator for unmodifiable.
     */
    public void setUnmodifiable()
    {
        this.unmodifiable = true;
    }

    /**
     * Mutator to set the datastore read timeout for this query.
     * @param timeout The timeout
     */
    public void setDatastoreReadTimeoutMillis(Integer timeout)
    {
        if (!supportsTimeout())
        {
            throw new NucleusUnsupportedOptionException("Timeout not supported on this query");
        }
        readTimeoutMillis = timeout;
    }

    /**
     * Convenience accessor for the datastore read timeout (milliseconds).
     * Returns null if not defined.
     * @return the timeout
     */
    public Integer getDatastoreReadTimeoutMillis()
    {
        return readTimeoutMillis;
    }

    /**
     * Mutator to set the datastore write timeout for this query.
     * @param timeout The timeout
     */
    public void setDatastoreWriteTimeoutMillis(Integer timeout)
    {
        if (!supportsTimeout())
        {
            throw new NucleusUnsupportedOptionException("Timeout not supported on this query");
        }
        writeTimeoutMillis = timeout;
    }

    /**
     * Convenience accessor for the datastore write timeout (milliseconds).
     * Returns null if not defined.
     * @return the timeout
     */
    public Integer getDatastoreWriteTimeoutMillis()
    {
        return writeTimeoutMillis;
    }

    public QueryManager getQueryManager()
    {
        if (storeMgr != null)
        {
            return storeMgr.getQueryManager();
        }
        return null;
    }

    /**
     * Method to add a subquery to this query.
     * @param sub The subquery
     * @param variableDecl Declaration of variables
     * @param candidateExpr Candidate expression
     * @param paramMap Map of parameters for this subquery
     */
    public void addSubquery(Query sub, String variableDecl, String candidateExpr, Map paramMap)
    {
        if (StringUtils.isWhitespace(variableDecl))
        {
            throw new NucleusUserException(Localiser.msg("021115"));
        }

        if (sub == null)
        {
            // No subquery so the variable is unset effectively meaning that it is an explicit variable
            if (explicitVariables == null)
            {
                explicitVariables = variableDecl;
            }
            else
            {
                explicitVariables += ";" + variableDecl;
            }
        }
        else
        {
            // Register the subquery against its variable name for later use
            if (subqueries == null)
            {
                subqueries = new HashMap();
            }
            String subqueryVariableName = variableDecl.trim();
            int sepPos = subqueryVariableName.indexOf(' ');
            subqueryVariableName = subqueryVariableName.substring(sepPos+1);
            if (!StringUtils.isWhitespace(candidateExpr))
            {
                // Set candidate expression for subquery
                sub.setFrom(candidateExpr);
            }
            subqueries.put(subqueryVariableName,
                new SubqueryDefinition(sub, StringUtils.isWhitespace(candidateExpr) ? null : candidateExpr, 
                        variableDecl, paramMap));
        }
    }

    /**
     * Simple representation of a subquery, its candidate, params and variables.
     */
    public static class SubqueryDefinition
    {
        Query query;
        String candidateExpression;
        String variableDecl;
        Map parameterMap;

        public SubqueryDefinition(Query q, String candidates, String variables, Map params)
        {
            this.query = q;
            this.candidateExpression = candidates;
            this.variableDecl = variables;
            this.parameterMap = params;
        }

        public Query getQuery()
        {
            return this.query;
        }

        public String getCandidateExpression()
        {
            return this.candidateExpression;
        }

        public String getVariableDeclaration()
        {
            return this.variableDecl;
        }

        public Map getParameterMap()
        {
            return this.parameterMap;
        }
    }

    /**
     * Accessor for the subquery for the supplied variable.
     * @param variableName Name of the variable
     * @return Subquery for the variable (if a subquery exists for this variable)
     */
    public SubqueryDefinition getSubqueryForVariable(String variableName)
    {
        if (subqueries == null)
        {
            return null;
        }
        return subqueries.get(variableName);
    }

    /**
     * Accessor for whether there is a subquery for the specified variable name.
     * @param variableName Name of the variable
     * @return Whether there is a subquery defined
     */
    public boolean hasSubqueryForVariable(String variableName)
    {
        return (subqueries == null ? false : subqueries.containsKey(variableName));
    }

    /**
     * Convenience method that will flush any outstanding updates to the datastore.
     * This is intended to be used before execution so that the datastore has all relevant data present
     * for what the query needs.
     */
    protected void prepareDatastore()
    {
        boolean flush = false;
        if (!ignoreCache && !ec.isDelayDatastoreOperationsEnabled())
        {
            flush = true;
        }
        else
        {
            flush = getBooleanExtensionProperty(EXTENSION_FLUSH_BEFORE_EXECUTION, false);
        }

        if (flush)
        {
            // Make sure all changes are in the datastore before proceeding
            ec.flushInternal(false);
        }
    }

    /**
     * Accessor for the query compilation.
     * Will be null if the query doesn't use the "generic" query mechanism.
     * @return The query compilation
     */
    public QueryCompilation getCompilation()
    {
        return compilation;
    }

    /**
     * Method to return if the query is compiled.
     * @return Whether it is compiled
     */
    protected boolean isCompiled()
    {
        return compilation != null;
    }

    /**
     * Verify the elements of the query and provide a hint to the query to prepare and optimize an execution plan.
     */
    public void compile()
    {
        try
        {
            if (candidateClass != null)
            {
                clr.setPrimary(candidateClass.getClassLoader());
            }
            compileInternal(null);
        }
        finally
        {
            clr.setPrimary(null);
        }
    }

    /**
     * Method to compile the query. To be implemented by the query implementation.
     * @param parameterValues Parameter values keyed by name (when compiling for execution)
     */
    protected abstract void compileInternal(Map parameterValues);

    /**
     * Accessor for the parsed imports.
     * If no imports are set then adds candidate class and user imports.
     * @return Parsed imports
     */
    public Imports getParsedImports()
    {
        if (parsedImports == null)
        {
            parsedImports = new Imports();
            if (candidateClassName != null)
            {
                parsedImports.importPackage(candidateClassName);
            }
            if (imports != null)
            {
                parsedImports.parseImports(imports);
            }
        }
        return parsedImports;
    }

    // ----------------------------- Execute -----------------------------------

    /**
     * Execute the query and return the filtered results.
     * @return the filtered results (List, or Object).
     * @see #executeWithArray(Object[] parameters)
     */
    public Object execute()
    {
        return executeWithArray(new Object[0]);
    }

    /**
     * Execute the query and return the filtered results using the array of parameters.
     * @param parameterValues the Object array with all of the parameters.
     * @return the filtered results (List, or Object).
     * @see #executeQuery(Map parameters)
     * @throws NoQueryResultsException Thrown if no results were returned from the query.
     */
    public Object executeWithArray(Object[] parameterValues)
    {
        assertIsOpen();
        if (!ec.getTransaction().isActive() && !ec.getTransaction().getNontransactionalRead())
        {
            throw new TransactionNotReadableException();
        }

        return executeQuery(getParameterMapForValues(parameterValues));
    }

    /**
     * Execute the query and return the filtered results using the map of parameters.
     * @param parameters the Map containing all of the parameters.
     * @return the filtered results (List, or Object)
     * @see #executeQuery(Map parameters)
     * @throws NoQueryResultsException Thrown if no results were returned from the query.
     */
    public Object executeWithMap(Map parameters)
    {
        assertIsOpen();
        if (!ec.getTransaction().isActive() && !ec.getTransaction().getNontransactionalRead())
        {
            throw new TransactionNotReadableException();
        }

        return executeQuery(parameters);
    }

    protected Map inputParameters;

    /**
     * Accessor for the input parameters for this query.
     * @return The input parameters map, with param values keyed by param name
     */
    public Map getInputParameters()
    {
        return inputParameters;
    }

    /**
     * Convenience method for whether this query supports timeouts.
     * Defaults to false, so override if supporting a timeout in the concrete implementation
     * @return Whether timeouts are supported.
     */
    protected boolean supportsTimeout()
    {
        return false;
    }

    /**
     * Method to execute the actual query. Calls performExecute() allowing individual implementations
     * to do what they require for execution (in-memory, in-datastore, etc). Applies result checking.
     * @param parameters Map of parameter values keyed by parameter name
     * @return Result. Will be List for SELECT queries, and Long for BULK_UPDATE/BULK_DELETE
     * @throws NoQueryResultsException Thrown if no results were returned from the query.
     * @throws QueryNotUniqueException Thrown if multiple results, yet expected one
     */
    protected Object executeQuery(final Map parameters)
    {
        try
        {
            if (candidateClass != null)
            {
                clr.setPrimary(candidateClass.getClassLoader());
            }

            this.inputParameters = new HashMap();
            if (implicitParameters != null)
            {
                inputParameters.putAll(implicitParameters);
            }
            if (parameters != null)
            {
                inputParameters.putAll(parameters);
            }

            // Ensure we have a compiled query
            try
            {
                compileInternal(inputParameters);
                checkForMissingParameters(inputParameters);

                if (compilation != null)
                {
                    candidateClass = compilation.getCandidateClass();
                }
            }
            catch (RuntimeException re)
            {
                // If an exception occurs during compilation make sure we clean up
                discardCompiled();
                throw re;
            }

            // Make sure the datastore is prepared (new objects flushed as required)
            prepareDatastore();

            if (toExclNo - fromInclNo <= 0)
            {
                // User range excludes results, so follow JDO spec 14.6.8
                if (shouldReturnSingleRow())
                {
                    return null;
                }
                else
                {
                    return Collections.EMPTY_LIST;
                }
            }

            boolean failed = true; // flag to use for checking the state of the execution results
            long start = 0;
            if (ec.getStatistics() != null)
            {
                start = System.currentTimeMillis();
                ec.getStatistics().queryBegin();
            }

            try
            {
                // Execute the query
                Object result = performExecute(inputParameters);

                // Process the results
                if (type == BULK_DELETE || type == BULK_UPDATE)
                {
                    // Bulk update/delete return a Long
                    return result;
                }
                else if (type == SELECT)
                {
                    // Select, so return the range of objects
                    Collection qr = (Collection)result;

                    failed = false;
                    if (shouldReturnSingleRow())
                    {
                        // Single row only needed (unique specified, or using aggregates etc), so just take first row
                        try
                        {
                            if (qr == null || qr.size() == 0)
                            {
                                throw new NoQueryResultsException("No query results were returned");
                            }
                            else
                            {
                                if (!processesRangeInDatastoreQuery() && (toExclNo - fromInclNo <= 0))
                                {
                                    // JDO2 spec 14.6.8 (range excludes instances, so return null)
                                    throw new NoQueryResultsException("No query results were returned in the required range");
                                }

                                Iterator qrIter = qr.iterator();
                                Object firstRow = qrIter.next();
                                if (qrIter.hasNext())
                                {
                                    failed = true;
                                    throw new QueryNotUniqueException();
                                }
                                return firstRow;
                            }
                        }
                        finally
                        {
                            // can close results right now because we don't return it,
                            // also user cannot close it otherwise except for calling closeAll()
                            if (qr != null)
                            {
                                close(qr);
                            }
                        }
                    }
                    else
                    {
                        if (qr instanceof QueryResult && queryResults != null)
                        {
                            // Result handler, so register the results so we can close later
                            queryResults.add((QueryResult)qr);
                        }

                        return qr;
                    }
                }
                else
                {
                    // 'Other' statement, so just return the result
                    return result;
                }
            }
            finally
            {
                if (ec.getStatistics() != null)
                {
                    if (failed)
                    {
                        ec.getStatistics().queryExecutedWithError();
                    }
                    else
                    {
                        ec.getStatistics().queryExecuted(System.currentTimeMillis()-start);
                    }
                }
            }
        }
        finally
        {
            clr.setPrimary(null);
        }
    }

    /**
     * Method that will throw an {@link UnsupportedOperationException} if the query implementation doesn't
     * support cancelling queries. Implementations that support the cancel operation should override this.
     */
    protected void assertSupportsCancel()
    {
        throw new UnsupportedOperationException("This query implementation doesn't support the cancel of executing queries");
    }

    /**
     * Method to cancel any currently running queries.
     * Operates if the implementation supports cancelling of queries via the method
     * <pre>assertSupportsCancel()</pre>
     */
    public void cancel()
    {
        assertSupportsCancel();
        Iterator taskIterator = tasks.entrySet().iterator();
        while (taskIterator.hasNext())
        {
            Map.Entry entry = (Map.Entry)taskIterator.next();
            boolean success = cancelTaskObject(entry.getValue());
            NucleusLogger.QUERY.debug("Query cancelled for thread=" + ((Thread)entry.getKey()).getId() + 
                " with success=" + success);
        }
        tasks.clear();
    }

    /**
     * Method to cancel a running query in the specified Thread.
     * Operates if the implementation supports cancelling of queries via the method
     * <pre>assertSupportsCancel()</pre>
     * @param thread The thread
     */
    public void cancel(Thread thread)
    {
        assertSupportsCancel();
        synchronized (tasks)
        {
            Object threadObject = tasks.get(thread);
            if (threadObject != null)
            {
                boolean success = cancelTaskObject(threadObject);
                NucleusLogger.QUERY.debug("Query (in thread=" + thread.getId() + ") cancelled with success=" + success);
            }
            tasks.remove(thread);
        }
    }

    protected void registerTask(Object taskObject)
    {
        synchronized (tasks)
        {
            tasks.put(Thread.currentThread(), taskObject);
        }
    }

    protected void deregisterTask()
    {
        synchronized (tasks)
        {
            tasks.remove(Thread.currentThread());
        }
    }

    /**
     * Method to perform the cancellation of a query task.
     * This implementation does nothing. Override if you
     * @param obj The task
     * @return Whether the task was cancelled
     */
    protected boolean cancelTaskObject(Object obj)
    {
        return true;
    }

    /**
     * Method to actually execute the query. To be implemented by extending
     * classes for the particular query language.
     * @param parameters Map containing the parameters.
     * @return Query result - QueryResult if SELECT, or Long if BULK_UPDATE, BULK_DELETE
     */
    protected abstract Object performExecute(Map parameters);

    /**
     * Method to return if the datastore query will check any range constraints of this query.
     * If this returns false and a range is specified then the range has to be managed using post-processing.
     * This implementation assumes false and should be overridden if the datastore query can handle range processing.
     * @return Whether the query processes range in the datastore
     */
    public boolean processesRangeInDatastoreQuery()
    {
        return false;
    }

    // ------------------------- Delete Persistent -----------------------------

    /**
     * Method to delete all objects found by this query, catering for cascade changes and updates
     * to in-memory objects.
     * @return The number of deleted objects.
     */
    public long deletePersistentAll()
    {
        return deletePersistentAll(new Object[0]);
    }

    /**
     * Method to delete all objects found by this query, catering for cascade changes and updates
     * to in-memory objects.
     * @param parameterValues the Object array with values of the parameters.
     * @return the filtered Collection.
     */
    public long deletePersistentAll(Object[] parameterValues)
    {
        return deletePersistentAll(getParameterMapForValues(parameterValues));
    }

    /**
     * Method to delete all objects found by this query, catering for cascade changes and updates
     * to in-memory objects.
     * @param parameters Map of parameters for the query
     * @return the number of deleted objects
     */
    public long deletePersistentAll(Map parameters)
    {
        assertIsOpen();
        if (!ec.getTransaction().isActive() && !ec.getTransaction().getNontransactionalWrite())
        {
            // tx not active and not allowing non-tx write
            throw new TransactionNotActiveException();
        }

        // Check for specification of any illegal attributes
        if (result != null)
        {
            throw new NucleusUserException(Localiser.msg("021029"));
        }
        if (resultClass != null)
        {
            throw new NucleusUserException(Localiser.msg("021030"));
        }
        if (ordering != null)
        {
            throw new NucleusUserException(Localiser.msg("021027"));
        }
        if (grouping != null)
        {
            throw new NucleusUserException(Localiser.msg("021028"));
        }
        if (range != null)
        {
            throw new NucleusUserException(Localiser.msg("021031"));
        }
        if (fromInclNo >= 0 && toExclNo >= 0 && (fromInclNo != 0 || toExclNo != Long.MAX_VALUE))
        {
            throw new NucleusUserException(Localiser.msg("021031"));
        }

        return performDeletePersistentAll(parameters);
    }

    /**
     * Execute the query to delete persistent objects.
     * Provides a default implementation that executes the query to find the objects, and then
     * calls ExecutionContext.deleteObjects() on the returned objects.
     * @param parameters the Map containing all of the parameters.
     * @return the number of deleted objects.
     */
    protected long performDeletePersistentAll(Map parameters)
    {
        boolean requiresUnique = unique;
        try
        {
            // Make sure we have a compiled query without any "unique" setting
            if (unique)
            {
                unique = false;
                discardCompiled();
            }
            compileInternal(parameters);

            // Execute the query to get the instances affected
            Collection results = (Collection)performExecute(parameters);
            if (results == null)
            {
                return 0;
            }
            else
            {
                // TODO : To make this most efficient we shouldn't instantiate things into memory
                // but instead check if the object to be deleted implements DeleteCallback, or there are
                // any lifecycle listeners for this object type, or if there are any dependent fields
                // and only then do we instantiate it.
                int number = results.size();
                if (requiresUnique && number > 1)
                {
                    throw new NucleusUserException(Localiser.msg("021032"));
                }

                // Instances to be deleted are flushed first (JDO2 [14.8-4])
                Iterator resultsIter = results.iterator();
                while (resultsIter.hasNext())
                {
                    ec.findObjectProvider(resultsIter.next()).flush();
                }

                // Perform the deletion - deletes any dependent objects
                ec.deleteObjects(results.toArray());

                if (results instanceof QueryResult)
                {
                    // Close any results that are of type QueryResult (which manage the access to the results)
                    ((QueryResult)results).close();
                }

                return number;
            }
        }
        finally
        {
            if (requiresUnique != unique)
            {
                // Reinstate unique setting
                unique = requiresUnique;
                discardCompiled();
            }
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Close a query result and release any resources associated with it.
     * @param queryResult the result of execute(...) on this Query instance.
     */
    public void close(Object queryResult)
    {
        if (queryResult != null && queryResult instanceof QueryResult)
        {
            // important to remove result first before closing it, as its hashCode() may
            // change after closing so the queryResults HashSet won't find it anymore and 
            // consequently won't remove it (see AbstractQueryResult.hashCode())
            if (queryResults != null)
            {
                queryResults.remove(queryResult);
            }
            ((QueryResult)queryResult).close();
        }
    }

    /**
     * Close all query results associated with this Query instance,
     * and release all resources associated with them.
     */
    public void closeAll()
    {
        if (ec != null)
        {
            boolean closeAtEcClose = getBooleanExtensionProperty(EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE, false);
            if (closeAtEcClose)
            {
                // No longer need notifying of ExecutionContext closure
                ec.deregisterExecutionContextListener(this);
            }
        }
        if (queryResults != null)
        {
            QueryResult[] qrs = queryResults.toArray(new QueryResult[queryResults.size()]);
            for (int i = 0; i < qrs.length; ++i)
            {
                close(qrs[i]);
            }
        }

        if (fetchPlan != null)
        {
            fetchPlan.clearGroups().addGroup(FetchPlan.DEFAULT);
        }
    }

    /**
     * Convenience method to return whether the query should return a single row.
     * @return Whether it represents a unique row
     */
    protected boolean shouldReturnSingleRow()
    {
        return QueryUtils.queryReturnsSingleRow(this);
    }

    /**
     * Convenience method to convert the input parameters into a parameter map keyed by the parameter
     * name. If the parameters for this query are explicit then they are keyed by the names defined
     * as input via "declareParameters()".
     * @param parameterValues Parameter values
     * @return The parameter map.
     */
    protected Map getParameterMapForValues(Object[] parameterValues)
    {
        // Generate a parameter map from the parameter names to these input values
        HashMap parameterMap = new HashMap();
        int position = 0;
        if (explicitParameters != null)
        {
            // Explicit parameters
            StringTokenizer t1 = new StringTokenizer(explicitParameters, ",");
            while (t1.hasMoreTokens())
            {
                StringTokenizer t2 = new StringTokenizer(t1.nextToken(), " ");
                if (t2.countTokens() != 2)
                {
                    // Invalid spec; should be "{type_decl} {param_name}"
                    throw new NucleusUserException(Localiser.msg("021101", explicitParameters));
                }
                t2.nextToken(); // Parameter type declaration
                String parameterName = t2.nextToken();
                if (!JDOQLQueryHelper.isValidJavaIdentifierForJDOQL(parameterName))
                {
                    // Invalid parameter name for Java
                    throw new NucleusUserException(Localiser.msg("021102",parameterName));
                }

                if (parameterMap.containsKey(parameterName))
                {
                    // Duplicate definition of a parameter
                    throw new NucleusUserException(Localiser.msg("021103", parameterName));
                }
                if (parameterValues.length < position+1)
                {
                    // Too many parameters defined and not enough values
                    throw new NucleusUserException(Localiser.msg("021108", "" + (position+1), "" + parameterValues.length));
                }

                parameterMap.put(parameterName, parameterValues[position++]);
            }
            if (parameterMap.size() != parameterValues.length)
            {
                // Too many values and not enough parameters declared
                throw new NucleusUserException(Localiser.msg("021108", "" + parameterMap.size(), "" + parameterValues.length));
            }
        }
        else
        {
            // Positional implicit parameters (JDO input)
            for (int i = 0; i < parameterValues.length; ++i)
            {
                // Dummy parameter name for DataNucleus, utilised by the implementation
                parameterMap.put(Integer.valueOf(i), parameterValues[i]);
            }
        }
        return parameterMap;
    }

    /**
     * Convenience accessor for whether to use the fetch plan with this query.
     * Defaults to true but can be turned off by the user for performance reasons.
     * @return Whether to use the fetch plan
     */
    protected boolean useFetchPlan()
    {
        boolean useFetchPlan = getBooleanExtensionProperty(EXTENSION_USE_FETCH_PLAN, true);
        if (type == BULK_UPDATE || type == BULK_DELETE)
        {
            // Don't want anything selecting apart from the PK fields when doing updates/deletes
            useFetchPlan = false;
        }

        return useFetchPlan;
    }

    /**
     * Whether the query compilation(s) should be cached.
     * @return Should we cache the compilation of the query
     */
    public boolean useCaching()
    {
        return getBooleanExtensionProperty(EXTENSION_COMPILATION_CACHED, true);
    }

    /**
     * Whether the results of the query should be cached.
     * @return Should we cache the results of the query
     */
    public boolean useResultsCaching()
    {
        if (!useCaching())
        {
            return false;
        }
        return getBooleanExtensionProperty(EXTENSION_RESULTS_CACHED, false);
    }

    /**
     * Whether the query compilation(s) should check for unused parameters.
     * @return Should we check for unused parameters and throw an exception if found
     */
    public boolean checkUnusedParameters()
    {
        return getBooleanExtensionProperty(EXTENSION_CHECK_UNUSED_PARAMETERS, true);
    }

    /**
     * Method to do checks of the input parameters with respect to their types being consistent
     * with the types of the parameters in the compilation.
     * Checks for unused input parameters. Doesn't check for missing parameters.
     * @param parameterValues The input parameter values keyed by their name (or position)
     */
    protected void checkParameterTypesAgainstCompilation(Map parameterValues)
    {
        if (compilation == null)
        {
            return;
        }
        else if (parameterValues == null || parameterValues.isEmpty())
        {
            return;
        }

        // Check for unused parameters
        boolean checkUnusedParams = checkUnusedParameters();
        Iterator it = parameterValues.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            Object paramKey = entry.getKey();
            Symbol sym = null;

            // Find the symbol for the parameter in the compilation (or subquery compilations)
            sym = deepFindSymbolForParameterInCompilation(compilation, paramKey);

            if (sym != null)
            {
                Class expectedValueType = sym.getValueType();
                if (entry.getValue() != null && expectedValueType != null &&
                     !QueryUtils.queryParameterTypesAreCompatible(expectedValueType, entry.getValue().getClass()))
                {
                    // Supplied parameter value is of inconsistent type
                    throw new NucleusUserException("Parameter \"" + paramKey + "\" was specified as " +
                        entry.getValue().getClass().getName() + " but should have been " + 
                        expectedValueType.getName());
                }
            }
            // TODO Also do this for positional params if not found ?
            else if (paramKey instanceof String)
            {
                if ((fromInclParam == null && toExclParam == null) ||
                    (!paramKey.equals(fromInclParam) && !paramKey.equals(toExclParam)))
                {
                    if (checkUnusedParams)
                    {
                        throw new QueryInvalidParametersException(Localiser.msg("021116", paramKey));
                    }
                }
            }
        }
    }

    /**
     * Method to check for any missing parameters that the query compilation is expecting but which aren't
     * supplied to execute().
     * @param parameterValues The input parameter values keyed by their name (or position)
     */
    protected void checkForMissingParameters(Map parameterValues)
    {
        if (compilation == null)
        {
            return;
        }
        else if (parameterValues == null)
        {
            parameterValues = new HashMap();
        }

        boolean namedParametersSupplied = true;
        if (parameterValues.size() > 0)
        {
            Object key = parameterValues.keySet().iterator().next();
            if (!(key instanceof String))
            {
                namedParametersSupplied = false;
            }
        }

        if (namedParametersSupplied)
        {
            // Check for missing named parameters
            SymbolTable symtbl = compilation.getSymbolTable();
            Collection<String> symNames = symtbl.getSymbolNames();
            if (symNames != null && !symNames.isEmpty())
            {
                for (String symName : symNames)
                {
                    Symbol sym = symtbl.getSymbol(symName);
                    if (sym.getType() == Symbol.PARAMETER)
                    {
                        if (!parameterValues.containsKey(symName))
                        {
                            throw new QueryInvalidParametersException(Localiser.msg("021119", symName));
                        }
                    }
                }
            }
        }
    }

    protected Symbol deepFindSymbolForParameterInCompilation(QueryCompilation compilation, Object paramKey)
    {
        Symbol sym = null;
        sym = getSymbolForParameterInCompilation(compilation, paramKey);
        if (sym == null)
        {
            String[] subqueryNames = compilation.getSubqueryAliases();
            if (subqueryNames != null)
            {
                for (int i=0;i<subqueryNames.length;i++)
                {
                    sym = deepFindSymbolForParameterInCompilation(
                        compilation.getCompilationForSubquery(subqueryNames[i]), paramKey);
                    if (sym != null)
                    {
                        break;
                    }
                }
            }
        }
        return sym;
    }
    
    /**
     * Convenience method to find a symbol for the specified parameter in the provided compilation.
     * @param compilation The compilation
     * @param paramKey The parameter name/position
     * @return The symbol (if present)
     */
    private Symbol getSymbolForParameterInCompilation(QueryCompilation compilation, Object paramKey)
    {
        Symbol sym = null;
        if (paramKey instanceof Integer)
        {
            ParameterExpression expr = compilation.getParameterExpressionForPosition((Integer)paramKey);
            if (expr != null)
            {
                sym = expr.getSymbol();
            }
        }
        else
        {
            String paramName = (String)paramKey;
            sym = compilation.getSymbolTable().getSymbol(paramName);
        }
        return sym;
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
        try
        {
            return getParsedImports().resolveClassDeclaration(classDecl, ec.getClassLoaderResolver(), 
                (candidateClass == null ? null : candidateClass.getClassLoader()));
        }
        catch (ClassNotResolvedException e)
        {
            throw new NucleusUserException(Localiser.msg("021015", classDecl));
        }
    }

    protected void assertIsOpen()
    {
        if (ec == null || ec.isClosed())
        {
            // Throw exception if query is closed (e.g JDO2 [14.6.1])
            throw new NucleusUserException(Localiser.msg("021013")).setFatal();
        }
    }

    /**
     * Method returning the native query performed by this query (if the query has been compiled, and
     * if the datastore plugin supports this).
     * @return The native query (e.g for RDBMS this is the SQL).
     */
    public Object getNativeQuery()
    {
        return null;
    }
}
