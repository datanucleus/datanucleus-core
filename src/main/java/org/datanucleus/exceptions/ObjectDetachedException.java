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
 * A <tt>ObjectDetachedException</tt> is thrown if an attempt is
 * made to use the object in a process that doesn't allow detached objects.
 */
public class ObjectDetachedException extends NucleusUserException
{
    private static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructs a class-not-detachable exception with the specified
     * detail message.
     * @param class_name Name of the class
     */
    public ObjectDetachedException(String class_name)
    {
        super(LOCALISER.msg("018006", class_name));
    }

    /**
     * Constructs a class-not-detachable exception with the specified
     * detail message and nested exception.
     * @param class_name name of the class
     * @param nested the nested exception(s).
     */
    public ObjectDetachedException(String class_name, Exception nested)
    {
        super(LOCALISER.msg("018006", class_name), nested);
    }

    /**
     * Constructs a class-not-detachable exception for many objects with the specified
     * detail message and nested exceptions.
     * @param nested the nested exception(s).
     */
    public ObjectDetachedException(Throwable[] nested)
    {
        super(LOCALISER.msg("018006"), nested);
    }
}