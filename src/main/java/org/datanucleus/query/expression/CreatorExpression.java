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

import java.util.Iterator;
import java.util.List;

import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.query.symbol.PropertySymbol;
import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;
import org.datanucleus.util.StringUtils;

/**
 * Expression representing something like "new X.Y.Z([param1[,param2[,param3]]])".
 */
public class CreatorExpression extends Expression
{
    /** Components of the class name being created e.g ["org", "datanucleus", "MyClass"]. */
    List tuples;

    /** Arguments for the creation call. */
    List<Expression> arguments;

    public CreatorExpression(List tuples, List args)
    {
        this.tuples = tuples;
        this.arguments = args;
        if (args != null && !args.isEmpty())
        {
            Iterator<Expression> argIter = args.iterator();
            while (argIter.hasNext())
            {
                argIter.next().parent = this;
            }
        }
    }

    /**
     * Accessor for the class name of the object being created.
     * @return Name of the class
     */
    public String getId()
    {
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < tuples.size(); i++)
        {
            if (id.length() > 0)
            {
                id.append('.');
            }
            id.append((String) tuples.get(i));
        }
        return id.toString();
    }

    /**
     * Accessor for the arguments to use in the creation call.
     * @return Argument list
     */
    public List<Expression> getArguments()
    {
        return arguments;
    }

    public List getTuples()
    {
        return tuples;
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol table
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
            try
            {
                // Try to find this as a complete class name (e.g as used in "instanceof")
                String className = getId();
                Class cls = symtbl.getSymbolResolver().resolveClass(className);
                symbol = new PropertySymbol(getId(), cls);
            }
            catch (ClassNotResolvedException cnre)
            {
                throw new NucleusUserException("CreatorExpression defined with class of " + getId() + " yet this class is not found");
            }
        }
        return symbol;
    }

    public String toString()
    {
        return "CreatorExpression{" + getId() + "(" + StringUtils.collectionToString(arguments) + ")}" +
            (alias != null ? " AS " + alias : "");
    }
}