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

import org.datanucleus.state.DNStateManager;

/**
 * Interface for an operation to be flushed.
 */
public interface Operation
{
    /**
     * Accessor for StateManager of the object that this operation is performed on.
     * @return StateManager
     */
    DNStateManager getStateManager();

    /**
     * Method to perform the operation.
     */
    void perform();
}