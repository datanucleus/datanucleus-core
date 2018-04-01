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
2009 Andy Jefferson - accessor for symbol names
    ...
 **********************************************************************/
package org.datanucleus.query.compiler;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.StringUtils;

/**
 * Table of symbols in a query.
 */
public class SymbolTable implements Serializable
{
    private static final long serialVersionUID = -4839286733223290900L;

    /** SymbolTable for the parent query (when this is a subquery), otherwise null. */
    SymbolTable parentSymbolTable = null;

    Map<String, Symbol> symbols = new HashMap<>();

    /**
     * Resolver for symbols. Note that this is not serialisable, but is set when compile() is called.
     */
    transient SymbolResolver resolver;

    public void setSymbolResolver(SymbolResolver resolver)
    {
        this.resolver = resolver;
    }

    public SymbolResolver getSymbolResolver()
    {
        return resolver;
    }

    /**
     * Set the symbol table for any parent query, so that if this query refers to an expression from the parent query then it is resolvable.
     * @param tbl The parent symbol table
     */
    public void setParentSymbolTable(SymbolTable tbl)
    {
        this.parentSymbolTable = tbl;
    }

    /**
     * Accessor for the parent symbol table (if any).
     * @return The parent symbol table
     */
    public SymbolTable getParentSymbolTable()
    {
        return parentSymbolTable;
    }

    /**
     * Accessor for the names of the symbols in this table.
     * @return Names of the symbols
     */
    public Collection<String> getSymbolNames()
    {
        return Collections.unmodifiableCollection(symbols.keySet());
    }

    /**
     * Return the Symbol for the specified name if known.
     * If there is a parent symbol table then looks up in that after if not found here (unless the name is "this").
     * @param name The name to look up
     * @return The symbol for this name
     */
    public Symbol getSymbol(String name)
    {
        if (symbols.containsKey(name))
        {
            return symbols.get(name);
        }
        if (parentSymbolTable != null && !name.equals("this"))
        {
            return parentSymbolTable.getSymbol(name);
        }
        return null;
    }

    /**
     * Return the Symbol for the specified name if known, treating the name as case-insensitive.
     * If there is a parent symbol table then looks up in that after if not found here (unless the name is "this").
     * @param name The name to look up
     * @return The symbol for this name
     */
    public Symbol getSymbolIgnoreCase(String name)
    {
        Iterator<Map.Entry<String, Symbol>> iter = symbols.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, Symbol> symbolEntry = iter.next();
            String key = symbolEntry.getKey();
            if (key.equalsIgnoreCase(name))
            {
                return symbolEntry.getValue();
            }
        }
        if (parentSymbolTable != null && !name.equals("this"))
        {
            return parentSymbolTable.getSymbolIgnoreCase(name);
        }
        return null;
    }

    /**
     * Accessor for whether this symbol table has a particular symbol name. Does not make any use of parent symbol table(s).
     * @param name The name of the symbol we require
     * @return Whether it is present here
     */
    public boolean hasSymbol(String name)
    {
        return symbols.containsKey(name);
    }
    // TODO Consider providing a method that checks the parentSymbolTable also

    public int addSymbol(Symbol symbol)
    {
        if (symbols.containsKey(symbol.getQualifiedName()))
        {
            throw new NucleusException("Symbol " + symbol.getQualifiedName() + " already exists.");
        }
        symbols.put(symbol.getQualifiedName(), symbol);
        return symbols.size();
    }

    public void removeSymbol(Symbol symbol)
    {
        if (!symbols.containsKey(symbol.getQualifiedName()))
        {
            throw new NucleusException("Symbol " + symbol.getQualifiedName() + " doesnt exist.");
        }
        symbols.remove(symbol.getQualifiedName());
    }

    public String toString()
    {
        return "SymbolTable : " + StringUtils.mapToString(symbols);
    }
}