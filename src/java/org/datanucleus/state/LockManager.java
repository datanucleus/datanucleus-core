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


/**
 * Interface defining a manager for locking of objects.
 * There are currently two modes to a LockManager.
 * <ol>
 * <li>Where the user has the object and wants it locking. In this case they provide the ObjectProvider
 * and we lock it (by a call to the datastore where appropriate).</li>
 * <li>Where the user wants to do a find of an object with a particular id. In this case we register the
 * identity as needing this lock level, and the datastore will be called to retrieve the object and will check
 * back what lock level to use.</li>
 * </ol>
 */
public interface LockManager
{
    /** Lock mode representing no locking. */
    public static final short LOCK_MODE_NONE = 0;

    /** Lock mode for optimistic reads. Same as LOCK_MODE_OPTIMISTIC_WRITE for DataNucleus. */
    public static final short LOCK_MODE_OPTIMISTIC_READ = 1;

    /** Lock mode for optimistic writes. */
    public static final short LOCK_MODE_OPTIMISTIC_WRITE = 2;

    /** Lock mode for pessimistic reads. Same as LOCK_MODE_PESSIMISTIC_WRITE for DataNucleus. */
    public static final short LOCK_MODE_PESSIMISTIC_READ = 3;

    /** Lock mode for pessimistic writes. */
    public static final short LOCK_MODE_PESSIMISTIC_WRITE = 4;

    /**
     * Method to lock the object with the provided identity (mode 2).
     * @param id Identity of the object
     * @param lockMode mode for locking
     */
    void lock(Object id, short lockMode);

    /**
     * Accessor for what locking should be applied to the object with the specified identity (mode 2).
     * @param id The identity
     * @return The lock mode to apply (NONE if nothing defined)
     */
    short getLockMode(Object id);

    /**
     * Method to clear all settings of required lock level for object ids (mode 2).
     */
    void clear();

    /**
     * Method to lock the object managed by the passed ObjectProvider (mode 1).
     * @param op ObjectProvider for the object
     * @param lockMode mode for locking
     */
    void lock(ObjectProvider op, short lockMode);

    /**
     * Method to unlock the object managed by the passed ObjectProvider (mode 1).
     * @param op ObjectProvider for the object
     */
    void unlock(ObjectProvider op);

    /**
     * Accessor for the current lock mode for the object managed by the passed ObjectProvider (mode 1).
     * @param op ObjectProvider for the object
     * @return The lock mode
     */
    short getLockMode(ObjectProvider op);

    /**
     * Method to close the manager and release resources.
     */
    void close();
}