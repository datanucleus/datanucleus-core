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
 **********************************************************************/
package org.datanucleus.cache;

import org.datanucleus.state.DNStateManager;

/**
 * A marker interface for Level1Cache for optimizing commit.
 * During commit we are not interested in hollow PC objects in Level1
 * cache - Level1 caches implementing this interface could optimize
 * the filtering of finding non-hollow PC objects.
 */
public interface TieredLevel1Cache extends Level1Cache {
    /**
     * Return non-hollow state managers that could be considered as dirty during commit phase.
     * @return non-hollow state managers to consider during commit phase
     */
    Iterable<? extends DNStateManager> hotValues();
}
