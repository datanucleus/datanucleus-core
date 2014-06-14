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

/**
 * Metadata representation of a parameter for a named stored proc query.
 */
public class StoredProcQueryParameterMetaData extends MetaData
{
    private static final long serialVersionUID = 7363911357565223250L;

    String name;

    String type;

    StoredProcQueryParameterMode mode;

    public String getName()
    {
        return name;
    }

    public StoredProcQueryParameterMetaData setName(String name)
    {
        this.name = name;
        return this;
    }

    public String getType()
    {
        return type;
    }

    public StoredProcQueryParameterMetaData setType(String type)
    {
        this.type = type;
        return this;
    }

    public StoredProcQueryParameterMode getMode()
    {
        return mode;
    }

    public StoredProcQueryParameterMetaData setMode(StoredProcQueryParameterMode mode)
    {
        this.mode = mode;
        return this;
    }
}