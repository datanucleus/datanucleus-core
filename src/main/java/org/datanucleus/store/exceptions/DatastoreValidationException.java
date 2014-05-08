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

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * A <tt>DatastoreValidationException</tt> is thrown if a mismatch is discovered
 * between what the JDO runtime thinks the datastore should look like and what it
 * actually looks like.
 */
public class DatastoreValidationException extends NucleusDataStoreException
{
    protected static final Localiser LOCALISER=Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructs a datastore validation exception with the specified detail
     * message.
     * @param msg the detail message
     */
    public DatastoreValidationException(String msg)
    {
        super(msg);
    }

    /**
     * Constructs a schema validation exception with the specified detail
     * message and nested exception.
     *
     * @param msg the detail message
     * @param nested the nested exception(s).
     */
    public DatastoreValidationException(String msg, Exception nested)
    {
        super(msg, nested);
    }
}