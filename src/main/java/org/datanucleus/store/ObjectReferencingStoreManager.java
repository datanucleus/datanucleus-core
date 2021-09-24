/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store;

import org.datanucleus.state.DNStateManager;

/**
 * Interface to be implemented by any StoreManager that provides objects that are actually created by the underlying datastore. 
 * This is the case with ODBMS in general where they hand out objects and those are the objects that should be managed.
 */
public interface ObjectReferencingStoreManager
{
    /**
     * Notifies this store manager that the main memory (RAM, heap) copy of the PC object  of the supplied
     * StateManager may not be regarded as valid anymore.
     * (The most common case is that the state of the PC becomes HOLLOW).
     * This is especially important for object databases employing implicit storing
     * from the main memory to the database (like DB4O).
     * These databases may stop tracking the main memory copy and linking it with its on-disk copy,
     * thus releasing memory.
     * More importantly, these databases then know that the object should be reloaded when it
     * is (maybe even implicitly) accessed again.
     *
     * To be clear: There may be multiple copies of the data of one PC object (from the user perspective),
     * namely a copy in main memory (on the Java heap) and a copy in the database (usually on disk).
     * As there may be multiple copies, some of these copies may be outdated or invalid. In case such
     * a copy is to be accessed, its contents should not be used. Rather than that, the outdated copy should
     * be overwritten by an authorative copy.
     *
     * This method marks the main memory copy of the object (on the Java heap) to be outdated in that sense.
     * @param sm StateManager managing the object
     */
    void notifyObjectIsOutdated(DNStateManager sm);
}