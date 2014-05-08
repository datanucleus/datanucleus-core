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
package org.datanucleus.query.compiler;

import java.lang.reflect.Field;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolResolver;
import org.datanucleus.query.symbol.SymbolTable;
import org.datanucleus.util.ClassUtils;

/**
 * Base symbol resolver, to be extended for particular query languages.
 */
public abstract class AbstractSymbolResolver implements SymbolResolver
{
    protected MetaDataManager metaDataManager;
    protected ClassLoaderResolver clr;
    protected SymbolTable symtbl;

    protected Class candidateClass;
    protected String candidateAlias;

    /**
     * Constructor for symbol resolver.
     * @param mmgr MetaData manager
     * @param clr ClassLoader resolver
     * @param symtbl Symbol table
     * @param cls Candidate class
     * @param alias Candidate alias
     */
    public AbstractSymbolResolver(MetaDataManager mmgr, ClassLoaderResolver clr, SymbolTable symtbl, 
            Class cls, String alias)
    {
        this.metaDataManager = mmgr;
        this.clr = clr;
        this.symtbl = symtbl;
        this.candidateClass = cls;
        this.candidateAlias = alias;
    }

    public Class getType(List tuples)
    {
        Class type = null;
        Symbol symbol = null;
        String firstTuple = (String)tuples.get(0);
        if (caseSensitiveSymbolNames())
        {
            symbol = symtbl.getSymbol(firstTuple);
        }
        else
        {
            symbol = symtbl.getSymbol(firstTuple);
            if (symbol == null)
            {
                symbol = symtbl.getSymbol(firstTuple.toUpperCase());
            }
            if (symbol == null)
            {
                symbol = symtbl.getSymbol(firstTuple.toLowerCase());
            }
        }
        if (symbol != null)
        {
            type = symbol.getValueType();
            if (type == null)
            {
                // Implicit variables don't have their type defined
                throw new NucleusUserException("Cannot find type of " + tuples.get(0) +
                    " since symbol has no type; implicit variable?");
            }

            for (int i=1; i<tuples.size(); i++)
            {
                type = getType(type, (String)tuples.get(i));
            }
        }
        else
        {
            symbol = symtbl.getSymbol(candidateAlias);
            type = symbol.getValueType();
            for (int i=0; i<tuples.size(); i++)
            {
                type = getType(type, (String)tuples.get(i));
            }
        }
        return type;
    }

    Class getType(Class cls, String fieldName)
    {
        AbstractClassMetaData acmd = metaDataManager.getMetaDataForClass(cls, clr);
        if (acmd != null)
        {
            AbstractMemberMetaData fmd = acmd.getMetaDataForMember(fieldName);
            if (fmd == null)
            {
                throw new NucleusUserException("Cannot access field "+fieldName+" on type "+cls.getName());
            }
            return fmd.getType();
        }
        else
        {
            Field field = ClassUtils.getFieldForClass(cls, fieldName);
            if (field == null)
            {
                throw new NucleusUserException("Cannot access field "+fieldName+" on type "+cls.getName());
            }
            return field.getType();
        }
    }

    public Class getPrimaryClass()
    {
        return candidateClass;
    }
}