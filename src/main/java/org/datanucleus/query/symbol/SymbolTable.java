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
package org.datanucleus.query.symbol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.StringUtils;

/**
 * Table of symbols in a query.
 */
public class SymbolTable implements Serializable
{
    private static final long serialVersionUID = -4839286733223290900L;

    /** SymbolTable for the parent query when this is a subquery, otherwise null. */
    SymbolTable parentSymbolTable = null;

    Map<String, Symbol> symbols = new HashMap();
    List<Symbol> symbolsTable = new ArrayList();

    /**
     * Resolver for symbols.
     * Note that this is not serialisable, but is set when compile() is called.
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

    public void setParentSymbolTable(SymbolTable tbl)
    {
        this.parentSymbolTable = tbl;
    }

    public SymbolTable getParentSymbolTable()
    {
        return parentSymbolTable;
    }

    Symbol getSymbol(int index)
    {
        synchronized (symbolsTable)
        {
            return symbolsTable.get(index);
        }
    }

    /**
     * Accessor for the names of the symbols in this table.
     * @return Names of the symbols
     */
    public Collection<String> getSymbolNames()
    {
        return new HashSet<String>(symbols.keySet());
    }

    public Symbol getSymbol(String name)
    {
        synchronized (symbolsTable)
        {
            return symbols.get(name);
        }
    }    

    public Symbol getSymbolIgnoreCase(String name)
    {
        synchronized (symbolsTable)
        {
            Iterator<String> iter = symbols.keySet().iterator();
            while (iter.hasNext())
            {
                String key = iter.next();
                if (key.equalsIgnoreCase(name))
                {
                    return symbols.get(key);
                }
            }
            return null;
        }
    }    

    public boolean hasSymbol(String name)
    {
        synchronized (symbolsTable)
        {
            return symbols.containsKey(name);
        }
    }    
    
    public int addSymbol(Symbol symbol)
    {
        synchronized (symbolsTable)
        {
            if (symbols.containsKey(symbol.getQualifiedName()))
            {
                throw new NucleusException("Symbol " + symbol.getQualifiedName() + " already exists.");
            }
            symbols.put(symbol.getQualifiedName(), symbol);
            symbolsTable.add(symbol);
            return symbolsTable.size();
        }
    }

    public void removeSymbol(Symbol symbol)
    {
        synchronized (symbolsTable)
        {
            if (!symbols.containsKey(symbol.getQualifiedName()))
            {
                throw new NucleusException("Symbol " + symbol.getQualifiedName() + " doesnt exist.");
            }
            symbols.remove(symbol.getQualifiedName());
            symbolsTable.remove(symbol);
        }
    }

    public String toString()
    {
        return "SymbolTable : " + StringUtils.mapToString(symbols);
    }
}