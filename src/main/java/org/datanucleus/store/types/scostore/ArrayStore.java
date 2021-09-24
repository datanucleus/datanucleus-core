/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.scostore;

import java.util.Iterator;
import java.util.List;

import org.datanucleus.state.DNStateManager;

/**
 * Interface representation of the backing store for an array.
 * @param <E> Element type for this array
 **/
public interface ArrayStore<E> extends Store
{
    /**
     * Accessor for an iterator for the array.
     * @param sm StateManager for the owner of the array. 
     * @return Iterator for the array.
     **/
    Iterator<E> iterator(DNStateManager sm);

    /**
     * Method to retrieve the elements of the array.
     * @param sm StateManager for the owner of the array
     * @return The List of elements in the array (in the same order)
     */
    List<E> getArray(DNStateManager sm);

    /**
     * Accessor for the size of the array.
     * @param sm StateManager for the owner of the array. 
     * @return The size of the array.
     */
    int size(DNStateManager sm);

    /**
     * Method to clear the array.
     * @param sm StateManager for the owner of the array. 
     */
    void clear(DNStateManager sm);

    /**
     * Method to set the elements in the array.
     * @param sm StateManager for the owner of the array.
     * @param array The array
     * @return Whether the elements were added ok
     */
	boolean set(DNStateManager sm, Object array);
}