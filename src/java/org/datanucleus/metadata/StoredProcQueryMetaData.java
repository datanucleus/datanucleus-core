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
package org.datanucleus.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata representation of a named stored proc query.
 */
public class StoredProcQueryMetaData extends MetaData
{
    String name;

    String procedureName;

    List<StoredProcQueryParameterMetaData> parameters;

    List<String> resultClasses;

    List<String> resultSetMappings;

    /**
     * Constructor for a stored proc query of the specified name. Set fields using setters, before populate().
     * @param name The name
     */
    public StoredProcQueryMetaData(final String name)
    {
        this.name = name;
    }

    public StoredProcQueryMetaData setName(String name)
    {
        this.name = name;
        return this;
    }

    public StoredProcQueryMetaData setProcedureName(String name)
    {
        this.procedureName = name;
        return this;
    }

    public StoredProcQueryMetaData addParameter(StoredProcQueryParameterMetaData param)
    {
        if (parameters == null)
        {
            parameters = new ArrayList<StoredProcQueryParameterMetaData>(1);
        }
        this.parameters.add(param);
        return this;
    }

    public StoredProcQueryMetaData addResultClass(String resultClass)
    {
        if (resultClasses == null)
        {
            resultClasses = new ArrayList<String>(1);
        }
        this.resultClasses.add(resultClass);
        return this;
    }

    public StoredProcQueryMetaData addResultSetMapping(String mapping)
    {
        if (resultSetMappings == null)
        {
            resultSetMappings = new ArrayList<String>(1);
        }
        this.resultSetMappings.add(mapping);
        return this;
    }

    public String getName()
    {
        return name;
    }

    public String getProcedureName()
    {
        return procedureName;
    }

    public List<StoredProcQueryParameterMetaData> getParameters()
    {
        return parameters;
    }

    public List<String> getResultClasses()
    {
        return resultClasses;
    }

    public List<String> getResultSetMappings()
    {
        return resultSetMappings;
    }
}