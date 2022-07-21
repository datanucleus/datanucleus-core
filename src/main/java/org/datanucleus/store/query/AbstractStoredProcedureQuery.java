/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.QueryResultMetaData;
import org.datanucleus.metadata.StoredProcQueryParameterMode;
import org.datanucleus.store.StoreManager;

/**
 * Abstract representation of a stored procedure query.
 */
public abstract class AbstractStoredProcedureQuery extends Query
{
    private static final long serialVersionUID = 6944783614104829182L;

    protected String procedureName;

    protected Set<StoredProcedureParameter> storedProcParams = null;

    protected int resultSetNumber = 0;

    /** MetaData defining the results of the query (optional). */
    protected QueryResultMetaData[] resultMetaDatas = null;

    /** Result classes for the result sets (optional). */
    protected Class[] resultClasses = null;

    /** Repository for holding output parameter values after execution. */
    protected Map<Object, Object> outputParamValues = null;

    /**
     * Constructs a new query instance from the existing query.
     * @param storeMgr StoreManager for this query
     * @param ec ExecutionContext
     * @param query Existing query
     */
    public AbstractStoredProcedureQuery(StoreManager storeMgr, ExecutionContext ec, AbstractStoredProcedureQuery query)
    {
        this(storeMgr, ec, query.procedureName);
    }

    /**
     * Constructs a new query instance having the same criteria as the given query.
     * @param storeMgr StoreManager for this query
     * @param ec The ExecutionContext
     * @param procName Name of the stored procedure in the datastore
     */
    public AbstractStoredProcedureQuery(StoreManager storeMgr, ExecutionContext ec, String procName)
    {
        super(storeMgr, ec);
        this.procedureName = procName;
    }

    public String getLanguage()
    {
        return "STOREDPROC";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.Query#setCandidates(org.datanucleus.store.Extent)
     */
    @Override
    public void setCandidates(Extent pcs)
    {
        throw new NucleusUserException("Not supported for stored procedures");
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.query.Query#setCandidates(java.util.Collection)
     */
    @Override
    public void setCandidates(Collection pcs)
    {
        throw new NucleusUserException("Not supported for stored procedures");        
    }

    /**
     * Method to set the MetaData defining the result.
     * Setting this will unset the resultClass.
     * @param qrmds Query Result MetaData
     */
    public void setResultMetaData(QueryResultMetaData[] qrmds)
    {
        this.resultMetaDatas = qrmds;
        this.resultClasses = null;
    }

    /**
     * Set the result class for the results.
     * Setting this will unset the resultMetaData.
     * @param resultClasses The result class
     */
    public void setResultClasses(Class[] resultClasses)
    {
        this.resultClasses = resultClasses;
        this.resultMetaDatas = null;
    }

    public void registerParameter(int pos, Class type, StoredProcQueryParameterMode mode)
    {
        if (storedProcParams == null)
        {
            storedProcParams = new HashSet<AbstractStoredProcedureQuery.StoredProcedureParameter>();
        }
        StoredProcedureParameter param = new StoredProcedureParameter(mode, pos, type);
        storedProcParams.add(param);
    }

    public void registerParameter(String name, Class type, StoredProcQueryParameterMode mode)
    {
        if (storedProcParams == null)
        {
            storedProcParams = new HashSet<AbstractStoredProcedureQuery.StoredProcedureParameter>();
        }
        StoredProcedureParameter param = new StoredProcedureParameter(mode, name, type);
        storedProcParams.add(param);
    }

    /**
     * Accessor for whether there are more results after the current one.
     * @return Whether there are more results
     */
    public abstract boolean hasMoreResults();

    /**
     * Accessor for the next result set.
     * @return Next results
     */
    public abstract Object getNextResults();

    /**
     * Accessor for the update count.
     * @return Update count
     */
    public abstract int getUpdateCount();

    /**
     * Accessor for the value of the output parameter at the specified position.
     * Only to be called after execute().
     * @param pos Position
     * @return The value
     */
    public Object getOutputParameterValue(int pos)
    {
        if (outputParamValues != null)
        {
            return outputParamValues.get(pos);
        }
        return null;
    }

    /**
     * Accessor for the value of the output parameter with the specified name.
     * Only to be called after execute().
     * @param name Name of the parameter
     * @return The value
     */
    public Object getOutputParameterValue(String name)
    {
        if (outputParamValues != null)
        {
            return outputParamValues.get(name);
        }
        return null;
    }

    public static class StoredProcedureParameter
    {
        StoredProcQueryParameterMode mode;
        Integer position;
        String name;
        Class type;

        public StoredProcedureParameter(StoredProcQueryParameterMode mode, int pos, Class type)
        {
            this.mode = mode;
            this.position = pos;
            this.type = type;
        }

        public StoredProcedureParameter(StoredProcQueryParameterMode mode, String name, Class type)
        {
            this.mode = mode;
            this.name = name;
            this.type = type;
        }
        public String getName()
        {
            return name;
        }
        public Integer getPosition()
        {
            return position;
        }
        public StoredProcQueryParameterMode getMode()
        {
            return mode;
        }
        public Class getType()
        {
            return type;
        }
    }
}