/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.exceptions;

import org.datanucleus.util.Localiser;

/**
 * Exception thrown when a class is required to have persistence information (metadata/annotations) yet none
 * can be found.
 */
public class NoPersistenceInformationException extends NucleusUserException
{
    private static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructs an exception for the specified class.
     * @param className Name of the class
     */
    public NoPersistenceInformationException(String className)
    {
        super(LOCALISER.msg("018001",className));
    }

    /**
     * Constructs an exception for the specified class with the supplied nested exception.
     * @param className Name of the class
     * @param nested the nested exception(s).
     */
    public NoPersistenceInformationException(String className, Exception nested)
    {
        super(LOCALISER.msg("018001",className), nested);
    }
}