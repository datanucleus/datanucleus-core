/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.exceptions;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;

/**
 * An exception that is thrown when we have a relation to another persistable object that is not yet persistent
 * and where the relation is not marked as cascade-persist.
 *
 * @see org.datanucleus.store.StoreManager
 */
public class ReachableObjectNotCascadedException extends NucleusUserException
{
    protected static final Localiser LOCALISER=Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructs the exception.
     * @param fieldName Name of the field where the non-persisted object is stored.
     * @param pc The object that is not persisted and not cascadable
     */
    public ReachableObjectNotCascadedException(String fieldName, Object pc)
    {
        super(LOCALISER.msg("018008", fieldName, pc));
    }
}