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
package org.datanucleus.query.expression;

import org.datanucleus.query.compiler.PropertySymbol;
import org.datanucleus.query.compiler.Symbol;
import org.datanucleus.query.compiler.SymbolTable;

/**
 * Expression representing a parameter.
 * The parameter can be explicit (declared as input to the query) or implicit (implied based on
 * the syntax in the query).
 */
public class ParameterExpression extends Expression
{
    private static final long serialVersionUID = -2170413163550042263L;
    String name;
    int position; // Position in the query (when name not specified in execution parameters)
    Class type;

    public ParameterExpression(String name, int position)
    {
        this.name = name;
        this.position = position;
    }

    /**
     * Constructor for when we know the name and the type (e.g via criteria queries).
     * @param name The name
     * @param type The type
     */
    public ParameterExpression(String name, Class type)
    {
        this.name = name;
        this.type = type;
        this.position = -1;
    }

    public String getId()
    {
        return name;
    }

    public int getPosition()
    {
        return position;
    }

    public Class getType()
    {
        return type;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol Table
     * @return The symbol for this expression
     */
    public Symbol bind(SymbolTable symtbl)
    {
        if (symtbl.hasSymbol(getId()))
        {
            symbol = symtbl.getSymbol(getId());
        }
        else
        {
            // No symbol for this parameter yet, so add one, but valueType is unknown still
            symbol = new PropertySymbol(getId());
            symbol.setType(Symbol.PARAMETER);
            symtbl.addSymbol(symbol);
        }
        return symbol;
    }

    public String toString()
    {
        return "ParameterExpression{" + name + "}";
    }
}