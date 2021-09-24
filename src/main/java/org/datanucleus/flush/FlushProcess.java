/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.flush;

import java.util.Collection;
import java.util.List;

import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.state.DNStateManager;

/**
 * Representation of a persistence flush process.
 * Decides in what order the "dirty" objects/fields are to be processed in.
 */
public interface FlushProcess
{
    /**
     * Execute the flush.
     * @param ec ExecutionContext
     * @param primarySMs StateManagers that were made dirty by direct API calls. Cleared during this method
     * @param secondarySMs StateManagers that were made dirty by reachability. Cleared during this method
     * @param opQueue Queue of operations
     * @return Any optimistic exceptions during the deletes/inserts/updates
     */
    List<NucleusOptimisticException> execute(ExecutionContext ec, Collection<DNStateManager> primarySMs, Collection<DNStateManager> secondarySMs, OperationQueue opQueue);
}