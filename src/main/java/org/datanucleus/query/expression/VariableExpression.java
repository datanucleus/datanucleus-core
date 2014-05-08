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
    ...
**********************************************************************/
package org.datanucleus.query.expression;

import org.datanucleus.query.symbol.PropertySymbol;
import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;

/**
 * Expression representing a variable.
 * The variable can be explicit (declared as input to the query) or implicit (implied based on
 * the query).
 */
public class VariableExpression extends Expression
{
    String name;
    Class type;

    public VariableExpression(String name)
    {
        this.name = name;
    }

    public VariableExpression(String name, Class type)
    {
        this.name = name;
        this.type = type;
    }

    public String getId()
    {
        return name;
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
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
            // No symbol for this variable yet, so add one
            if (type != null)
            {
                symbol = new PropertySymbol(getId(), type);
            }
            else
            {
                symbol = new PropertySymbol(getId());
            }
            symbol.setType(Symbol.VARIABLE);
            symtbl.addSymbol(symbol);
        }
        return symbol;
    }

    public String toString()
    {
        return "VariableExpression{" + name + "}" + (alias != null ? " AS " + alias : "");
    }
}