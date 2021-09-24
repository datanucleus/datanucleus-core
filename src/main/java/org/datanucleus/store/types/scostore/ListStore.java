/**********************************************************************
Copyright (c) 2003 David Jencks and others. All rights reserved.
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
2003 Andy Jefferson - coding standards
2003 Andy Jefferson - commented
2004 Andy Jefferson - changed to extend CollectionStore
2004 Andy Jefferson - added subList
    ...
**********************************************************************/
package org.datanucleus.store.types.scostore;

import java.util.Collection;
import java.util.ListIterator;

import org.datanucleus.state.ObjectProvider;

/**
 * Interface representation of the backing store for a List.
 * Takes the collection methods and extends them for lists.
 * @param <E> Element type for this list
 */
public interface ListStore<E> extends CollectionStore<E>
{
    /**
     * Method to add an element to the List.
     * @param sm StateManager for the owner of the List. 
     * @param element Element to add
     * @param index Position to add the element. 
     * @param size Current size of list (if known). -1 if not known
     */
    void add(ObjectProvider sm, E element, int index, int size);

    /**
     * Method to add a collection of elements to the List.
     * @param sm StateManager for the owner of the List.
     * @param c Collection of elements to add
     * @param index Position to add the elements.
     * @param size Current size of the list (if known). -1 if not known
     * @return Whether the elements were added ok
     */
	boolean addAll(ObjectProvider sm, Collection<E> c, int index, int size);

    /**
     * Method to remove an element from the List.
     * @param sm StateManager for the owner of the List.
     * @param index Position to remove the element.
     * @param size Current size of the list (if known). -1 if not known
     * @return The element that was removed.
     */
	E remove(ObjectProvider sm, int index, int size);

    /**
     * Method to retrieve an element from a position in the List.
     * @param sm StateManager for the owner of the List.
     * @param index Position of the element.
     * @return The element at that position.
     */
	E get(ObjectProvider sm, int index);

    /**
     * Method to update an element at a position in the List.
     * @param sm StateManager for the owner of the List.
     * @param index Position of the element.
     * @param element The element value
     * @param allowDependentField Whether to enable dependent field during this operation
     * @return The previous element at that position.
     */
	Object set(ObjectProvider sm, int index, Object element, boolean allowDependentField);

    /**
     * Accessor for a sublist of elements between from and to indices.
     * @param sm StateManager for the owner of the List.
     * @param from Start position (inclusive)
     * @param to End position (exclusive)
     * @return List of elements in this range.
     */
	java.util.List subList(ObjectProvider sm, int from, int to);

    /**
     * Method to return the position of an element in the List.
     * @param sm StateManager for the owner of the List.
     * @param element The element value
     * @return The position of the element.
     */
	int indexOf(ObjectProvider sm, Object element);

    /**
     * Method to return the last position of an element in the List.
     * @param sm StateManager for the owner of the List.
     * @param element The element value
     * @return The last position of the element.
     **/
	int lastIndexOf(ObjectProvider sm, Object element);

    /**
     * Accessor for a list iterator for the List.
     * @param sm StateManager for the owner of the List. 
     * @return List iterator for the List.
     */
	ListIterator<E> listIterator(ObjectProvider sm);
}