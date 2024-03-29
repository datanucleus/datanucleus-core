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
package org.datanucleus.store.query.expression;

import org.datanucleus.store.query.compiler.PropertySymbol;
import org.datanucleus.store.query.compiler.Symbol;
import org.datanucleus.store.query.compiler.SymbolTable;

/**
 * Expression representing a parameter.
 * The parameter can be explicit (declared as input to the query) or implicit (implied based on the syntax in the query).
 */
public class ParameterExpression<T> extends Expression
{
    private static final long serialVersionUID = -2170413163550042263L;

    private String name;

    private int position; // Position in the query (when name not specified in execution parameters)

    private Class<T> type;

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
    public ParameterExpression(String name, Class<T> type)
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

    public void setType(Class<T> type)
    {
        this.type = type;
    }
    public Class<T> getType()
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
            // No symbol for this parameter yet, so add one
            symbol = new PropertySymbol(getId());
            symbol.setType(Symbol.PARAMETER);
            if (type != null)
            {
                // Add valueType if known
                symbol.setValueType(type);
            }
            symtbl.addSymbol(symbol);
        }
        return symbol;
    }

    public String toString()
    {
        return "ParameterExpression{" + name + "}";
    }
}