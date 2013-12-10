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
package org.datanucleus.query.symbol;

import java.util.List;

import org.datanucleus.exceptions.ClassNotResolvedException;

/**
 * Interface for use in the resolution of symbols during query compilation.
 */
public interface SymbolResolver
{
    Class getType(List tuples);

    /**
     * Accessor for the candidate class of the query.
     * @return The candidate class
     */
    Class getPrimaryClass();

    /**
     * Method to resolve the provided name to a class (if possible).
     * Some query languages allow definition of imports of packages to check (e.g JDOQL)
     * and so use this as a hook for that capability.
     * @param className Name of the prospective "class"
     * @return The resolved class
     * @throws ClassNotResolvedException if not found
     */
    Class resolveClass(String className);

    /**
     * Whether we should accept implicit variables in the query.
     * JDOQL supports variables, yet JPQL doesn't. Also in JDOQL if the user supplies some
     * explicit variables then it doesn't allow implicit variables.
     * @return Whether to support implicit variables
     */
    boolean supportsImplicitVariables();

    /**
     * Whether names of symbols are case-sensitive (e.g JDOQL returns true, but JPQL returns false).
     * @return Whether case sensitive
     */
    boolean caseSensitiveSymbolNames();
}