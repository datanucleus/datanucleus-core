/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.metadata;

import org.datanucleus.util.StringUtils;

/**
 * Representation of the MetaData of a named Query.
 */
public class QueryMetaData extends MetaData
{
    /** Scope of this query (if any). */
    protected String scope;

    /** Name of the query. */
    protected String name;

    /** Query language. */
    protected String language;

    /** Whether the query is unmodifiable. */
    protected boolean unmodifiable = false;

    /** The single string query */
    protected String query;

    /** The result class to use. Only applies to SQL. */
    protected String resultClass = null;

    /** Name for the MetaData defining the mapping of the result set (for JPA SQL). */
    protected String resultMetaDataName = null;

    /** Whether the query returns unique. Only applies to SQL. */
    protected boolean unique = false;

    /** Name of any fetch-plan to use. */
    protected String fetchPlanName = null;

    /**
     * Constructor for a query of the specified name. Set fields using setters, before populate().
     * @param name The Query name
     */
    public QueryMetaData(final String name)
    {
        this.name = name;
    }

    public String getScope()
    {
        return scope;
    }

    public QueryMetaData setScope(String scope)
    {
        this.scope = (StringUtils.isWhitespace(scope) ? null : scope);
        return this;
    }

    public String getName()
    {
        return name;
    }

    public String getLanguage()
    {
        if (language == null)
        {
            // Default to JDOQL
            language = QueryLanguage.JDOQL.toString();
        }
        return language;
    }

    public QueryMetaData setLanguage(String language)
    {
        if (!StringUtils.isWhitespace(language))
        {
            this.language = language;
            if (this.language.equals("javax.jdo.query.JDOQL")) // Convert to JDOQL
            {
                this.language = QueryLanguage.JDOQL.toString();
            }
            else if (this.language.equals("javax.jdo.query.SQL")) // Convert to SQL
            {
                this.language = QueryLanguage.SQL.toString();
            }
            else if (this.language.equals("javax.jdo.query.JPQL")) // Convert to JPQL
            {
                this.language = QueryLanguage.JPQL.toString();
            }
        }
        return this;
    }

    public boolean isUnmodifiable()
    {
        return unmodifiable;
    }

    public QueryMetaData setUnmodifiable(boolean unmodifiable)
    {
        this.unmodifiable = unmodifiable;
        return this;
    }

    public QueryMetaData setUnmodifiable(String unmodifiable)
    {
        if (!StringUtils.isWhitespace(unmodifiable))
        {
            this.unmodifiable = Boolean.parseBoolean(unmodifiable);
        }
        return this;
    }

    public String getQuery()
    {
        return query;
    }

    public QueryMetaData setQuery(String query)
    {
        this.query = query;
        return this;
    }

    public String getResultClass()
    {
        return resultClass;
    }

    public QueryMetaData setResultClass(String resultClass)
    {
        this.resultClass = (StringUtils.isWhitespace(resultClass) ? null : resultClass);
        return this;
    }

    public String getResultMetaDataName()
    {
        return resultMetaDataName;
    }

    public QueryMetaData setResultMetaDataName(String mdName)
    {
        this.resultMetaDataName = (StringUtils.isWhitespace(mdName) ? null : mdName);
        return this;
    }

    public boolean isUnique()
    {
        return unique;
    }

    public QueryMetaData setUnique(boolean unique)
    {
        this.unique = unique;
        return this;
    }

    public QueryMetaData setUnique(String unique)
    {
        if (!StringUtils.isWhitespace(unique))
        {
            this.unique = Boolean.parseBoolean(unique);
        }
        return this;
    }

    public String getFetchPlanName()
    {
        return fetchPlanName;
    }

    public QueryMetaData setFetchPlanName(String fpName)
    {
        this.fetchPlanName = (StringUtils.isWhitespace(fpName) ? null : fpName);
        return this;
    }

    /**
     * Returns a string representation of the object.
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<query name=\"" + name + "\"\n");
        sb.append(prefix).append("       language=\"" + language + "\"\n");
        if (unique)
        {
            sb.append(prefix).append("       unique=\"true\"\n");
        }
        if (resultClass != null)
        {
            sb.append(prefix).append("       result-class=\"" + resultClass + "\"\n");
        }
        if (fetchPlanName != null)
        {
            sb.append(prefix).append("       fetch-plan=\"" + fetchPlanName + "\"\n");
        }
        sb.append(prefix).append("       unmodifiable=\"" + unmodifiable + "\">\n");
        sb.append(prefix).append(query).append("\n");

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix + "</query>\n");
        return sb.toString();
    }
}