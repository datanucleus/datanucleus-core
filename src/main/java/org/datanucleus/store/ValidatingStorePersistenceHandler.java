/**********************************************************************
 Copyright (c) 2009 Erik Bengtson and others. All rights reserved.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **********************************************************************/
package org.datanucleus.store;

import org.datanucleus.state.DNStateManager;

/**
 * Marker interface for StorePersistenceHandler implementations.
 * Implementing this interface enabled the persistence handler
 * to decide what to do if EC.findObject is requested to validate
 * the existence of a found object.
 * If the object was loaded in findObject from the persistence handler
 * then it might not be necessary to check for its existence again in DB
 * by calling sm.validate().
 * We leave this decision to the persistence handler if it supports this optimization.
 */
public interface ValidatingStorePersistenceHandler
{
    /**
     * Validates whether the supplied state manager instance exists in the datastore.
     * If the instance does not exist in the datastore, this method will fail raising a NucleusObjectNotFoundException.
     *
     * @param sm the state manager instance to check
     * @param readFromPersistenceHandler whether object was read from persistence handler in datastore.
     */
    void validate(DNStateManager sm, boolean readFromPersistenceHandler);
}
