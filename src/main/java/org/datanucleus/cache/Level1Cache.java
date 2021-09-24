/**********************************************************************
Copyright (c) 2003 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.cache;

import java.util.Map;

import org.datanucleus.state.ObjectProvider;

/**
 * Provides an interface for Level 1 caches.
 * Currently we just require a Map, but interfacing this provides the flexibility to being able to add requirements in the future.
 */
public interface Level1Cache extends Map<Object, ObjectProvider>
{
    public static final String NONE_NAME = "none";

    /**
     * Method to retrieve StateManager for the specified unique key.
     * @param key Unique key
     * @return StateManager if one is cached for this unique key
     */
    ObjectProvider getUnique(CacheUniqueKey key);

    /**
     * Method to store a StateManager for this unique key.
     * @param key The unique key
     * @param sm StateManager
     * @return The previous StateManager for this unique key if one was present, otherwise null
     */
    Object putUnique(CacheUniqueKey key, ObjectProvider sm);
}