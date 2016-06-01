/**********************************************************************
Copyright (c) 2008 Erik Bengtson and others. All rights reserved.
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
2008 Andy Jefferson - add type
    ...
**********************************************************************/
package org.datanucleus.query.compiler;

import java.io.Serializable;

/**
 * Symbol representing a property/identifier in a query. 
 * This can be an identifier, or a parameter for example.
 */
public class PropertySymbol implements Symbol, Serializable
{
	private static final long serialVersionUID = -7781522317458406758L;

    /** Type of symbol. Variable, parameter, etc. */
    int type;

    /** Qualified name of the symbol. */
    final String qualifiedName;

    /** Type of the value. Useful where we don't know the value yet, but know the type. */
    Class valueType;

    public PropertySymbol(String qualifiedName)
    {
        this.qualifiedName = qualifiedName;
    }

    public PropertySymbol(String qualifiedName, Class type)
    {
        this.qualifiedName = qualifiedName;
        this.valueType = type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public int getType()
    {
        return type;
    }

    public String getQualifiedName()
    {
        return qualifiedName;
    }

    public Class getValueType()
    {
        return valueType;
    }

    public void setValueType(Class type)
    {
        this.valueType = type;
    }

    public String toString()
    {
        String typeName = null;
        if (type == IDENTIFIER)
        {
            typeName = "IDENTIFIER";
        }
        else if (type == PARAMETER)
        {
            typeName = "PARAMETER";
        }
        else if (type == VARIABLE)
        {
            typeName = "VARIABLE";
        }
        return "Symbol: " + qualifiedName + " [valueType=" + valueType + ", " + typeName + "]";
    }
}