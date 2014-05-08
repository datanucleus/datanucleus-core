/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved. 
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
2002 Kelly Grizzle (TJDO)
2003 Andy Jefferson - commented and localised.
    ...
**********************************************************************/
package org.datanucleus.exceptions;

import org.datanucleus.util.Localiser;

/**
 * Exception thrown if a named connection factory cannot be found using its JNDI name.
 */
public class ConnectionFactoryNotFoundException extends NucleusUserException
{
    private static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructs a connection factory not found exception.
     * @param name The JNDI name that was not found.
     * @param nested The nested exception from the naming service.
     */
    public ConnectionFactoryNotFoundException(String name,Exception nested)
    {
        super(LOCALISER.msg("009002", name), nested);
    }
}