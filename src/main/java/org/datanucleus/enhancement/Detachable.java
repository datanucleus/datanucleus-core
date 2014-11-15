/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancement;

/**
 * This interface is implemented by classes that can be detached from the persistence context and later
 * attached. The interface includes the contract by which the StateManager can set the object id, version,
 * BitSet of loaded fields, and BitSet of modified fields so they are preserved while outside the persistence
 * environment.
 * <P>
 * The detached state is stored as a field in each instance of Detachable. The field is serialized so as to
 * maintain the state of the instance while detached. While detached, only the BitSet of modified fields will
 * be modified. The structure of the Object[] dnDetachedState is as follows:
 * <ul>
 * <li>dnDetachedState[0]: the Object Id of the instance</li>
 * <li>dnDetachedState[1]: the Version of the instance</li>
 * <li>dnDetachedState[2]: a BitSet of loaded fields</li>
 * <li>dnDetachedState[3]: a BitSet of modified fields</li>
 * </ul>
 */
public interface Detachable
{
    /**
     * This method calls the StateManager with the current detached state instance as a parameter and replaces
     * the current detached state instance with the value provided by the StateManager.
     */
    public void dnReplaceDetachedState();
}
