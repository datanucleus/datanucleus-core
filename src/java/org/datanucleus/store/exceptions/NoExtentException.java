/**********************************************************************
Copyright (c) 2002 Mike Martin and others. All rights reserved.
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
    Andy Jefferson - coding standards
    ...
**********************************************************************/
package org.datanucleus.store.exceptions;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;

/**
 * A <tt>NoExtentException</tt> is thrown if an attempt is made to perform an
 * operation using a class that is not backed by an extent (ie table or view)
 * in the database and the operation is not supported on such classes.
 *
 * @see org.datanucleus.store.StoreManager
 */
public class NoExtentException extends NucleusUserException
{
    protected static final Localiser LOCALISER=Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructs a no extent exception.
     * @param className Name of the class on which the operation requiring an
     *                  extent was attempted.
     */
    public NoExtentException(String className)
    {
        super(LOCALISER.msg("018007", className));
    }
}