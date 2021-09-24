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
package org.datanucleus.state;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.metadata.VersionStrategy;

/**
 * Interface defining a manager for locking of objects.
 * There are currently two modes to a LockManager.
 * <ol>
 * <li>Where the user has the object and wants it locking. In this case they provide StateManager and we lock it (by a call to the datastore where appropriate).</li>
 * <li>Where the user wants to do a find of an object with a particular id. In this case we register the identity as needing this lock level, 
 * and the datastore will be called to retrieve the object and will check back what lock level to use.</li>
 * </ol>
 * The LockManager controls all aspects of what objects need any form of locking, the updates to versions, and version checking (optimistic locking).
 */
public interface LockManager
{
    /**
     * Method to lock the object managed by the passed StateManager (mode 1).
     * @param sm StateManager for the object
     * @param lockMode mode for locking
     */
    void lock(DNStateManager sm, LockMode lockMode);

    /**
     * Method to unlock the object managed by the passed StateManager (mode 1).
     * @param sm StateManager for the object
     */
    void unlock(DNStateManager sm);

    /**
     * Accessor for the current lock mode for the object managed by the passed StateManager (mode 1).
     * @param sm StateManager for the object
     * @return The lock mode
     */
    LockMode getLockMode(DNStateManager sm);

    /**
     * Method to lock the object with the provided identity (mode 2).
     * @param id Identity of the object
     * @param lockMode mode for locking
     */
    void lock(Object id, LockMode lockMode);

    /**
     * Accessor for what locking should be applied to the object with the specified identity (mode 2).
     * @param id The identity
     * @return The lock mode to apply (NONE if nothing defined)
     */
    LockMode getLockMode(Object id);

    /**
     * Method to clear all settings of required lock level.
     */
    void clear();

    /**
     * Method to close the manager and release resources.
     */
    void close();

    /**
     * Method to perform an optimistic version check on the specified StateManager.
     * @param sm StateManager
     * @param versionStrategy The version strategy in use
     * @param versionDatastore Version of the object in the datastore
     */
    void performOptimisticVersionCheck(DNStateManager sm, VersionStrategy versionStrategy, Object versionDatastore);

    /**
     * Convenience method to provide the next version to use given the VersionMetaData and the current version.
     * @param vermd Version metadata
     * @param currentVersion The current version
     * @return The next version
     * @throws NucleusUserException Thrown if the strategy is not supported.
     */
    Object getNextVersion(VersionMetaData vermd, Object currentVersion);
}