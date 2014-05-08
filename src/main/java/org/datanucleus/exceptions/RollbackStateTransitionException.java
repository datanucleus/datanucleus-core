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
2007 Andy Jefferson - changed to extend NucleusException
    ...
**********************************************************************/
package org.datanucleus.exceptions;

import org.datanucleus.util.Localiser;

/**
 * Exception thrown when an error occurs in the rollback process of a state change.
 */
public class RollbackStateTransitionException extends NucleusException
{
    private static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructor.
     * @param nested The nested exceptions
     */
    public RollbackStateTransitionException(java.lang.Exception[] nested)
    {
        super(LOCALISER.msg("015031"), nested);
        setFatal();
    }
}