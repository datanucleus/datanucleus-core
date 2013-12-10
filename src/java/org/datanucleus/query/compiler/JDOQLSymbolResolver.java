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

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.query.symbol.SymbolTable;

/**
 * Symbol resolver for JDOQL.
 */
public class JDOQLSymbolResolver extends AbstractSymbolResolver
{
    /**
     * Constructor for symbol resolver.
     * @param mmgr MetaData manager
     * @param clr ClassLoader resolver
     * @param symtbl Symbol table
     * @param cls Candidate class
     * @param alias Candidate alias
     */
    public JDOQLSymbolResolver(MetaDataManager mmgr, ClassLoaderResolver clr, SymbolTable symtbl, 
            Class cls, String alias)
    {
        super(mmgr, clr, symtbl, cls, alias);
    }

    /**
     * Method to perform a lookup of the class name from the input name.
     * Makes use of the query "imports" and the lookup to "entity name".
     * @param className Name of the class
     * @return The class corresponding to this name
     * @throws ClassNotResolvedException thrown if not resolvable using imports or entity name
     */
    public Class resolveClass(String className)
    {
        // Try via "entity name"
        AbstractClassMetaData acmd = metaDataManager.getMetaDataForEntityName(className);
        if (acmd != null)
        {
            String fullClassName = acmd.getFullClassName();
            if (fullClassName != null)
            {
                return clr.classForName(fullClassName);
            }
        }

        throw new ClassNotResolvedException("Class " + className + " for query has not been resolved. Check the query and any imports specification");
    }

    public boolean caseSensitiveSymbolNames()
    {
        return true;
    }

    public boolean supportsImplicitVariables()
    {
        return true;
    }
}
