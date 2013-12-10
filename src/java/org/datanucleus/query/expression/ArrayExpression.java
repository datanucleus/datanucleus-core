/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;
import org.datanucleus.util.StringUtils;

/**
 * Expression representing an input array of expressions (or at least some of the array is of expressions).
 */
public class ArrayExpression extends Expression
{
    /** Elements of the array. */
    List<Expression> elements;

    public ArrayExpression(Expression[] elements)
    {
        this.elements = new ArrayList<Expression>();
        if (elements != null)
        {
            for (int i=0;i<elements.length;i++)
            {
                this.elements.add(elements[i]);
                elements[i].parent = this;
            }
        }
    }

    /**
     * Accessor for an element of the array.
     * @return Element
     */
    public Expression getElement(int index)
    {
        if (index < 0 || index >= elements.size())
        {
            throw new IndexOutOfBoundsException();
        }
        return elements.get(index);
    }

    public int getArraySize()
    {
        return elements.size();
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol table
     * @return The symbol for this expression
     */
    public Symbol bind(SymbolTable symtbl)
    {
        for (int i=0;i<elements.size();i++)
        {
            Expression expr = elements.get(i);
            expr.bind(symtbl);
        }
        // ArrayExpression has no symbol as such since just a container
        return symbol;
    }

    public String toString()
    {
        return "ArrayExpression{" + StringUtils.collectionToString(elements) + "}";
    }
}