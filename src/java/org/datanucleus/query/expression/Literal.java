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
2008 Andy Jefferson - javadocs, toString()
    ...
**********************************************************************/
package org.datanucleus.query.expression;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;

/**
 * Literal of some type (String, Double, Long, BigDecimal, etc).
 */
public class Literal extends Expression
{
    Object value;

    public Literal(Object value)
    {
        this.value = value;
    }

    public Object getLiteral()
    {
        return value;
    }

    public void negate()
    {
        if (value == null)
        {
            return;
        }
        else if (value instanceof BigInteger)
        {
            value = ((BigInteger)value).negate();
        }
        else if (value instanceof BigDecimal)
        {
            value = ((BigDecimal)value).negate();
        }
        else if (value instanceof Integer)
        {
            value = Integer.valueOf(-1*(Integer)value);
        }
        else if (value instanceof Long)
        {
            value = Long.valueOf(-1*(Long)value);
        }
        else if (value instanceof Double)
        {
            value = Double.valueOf(-1*(Double)value);
        }
        else if (value instanceof Float)
        {
            value = Float.valueOf(-1*(Float)value);
        }
        else if (value instanceof Short)
        {
            value = Short.valueOf((short)(-1*(Short)value));
        }
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol Table
     * @return The symbol for this expression
     */
    public Symbol bind(SymbolTable symtbl)
    {
        return null;
    }

    public String toString()
    {
        return "Literal{" + value + "}" + (alias != null ? " AS " + alias : "");
    }
}