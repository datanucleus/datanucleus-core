/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.store.types;

/**
 * Representation of a wrapper for a mutable List SCO type supported.
 **/
public interface SCOList<T> extends SCOCollection<T>
{
    /**
     * Overload the basic List set() method to allow turning off of the dependent-field
     * deletion process.
     * @param index The index to set the element at
     * @param element The element
     * @param allowDependentField Whether to allow dependent-field deletes
     * @return The previous object at this position
     */
    public Object set(int index, Object element, boolean allowDependentField);
}