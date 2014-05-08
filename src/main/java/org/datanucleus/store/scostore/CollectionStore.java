/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.store.scostore;

import java.util.Collection;
import java.util.Iterator;

import org.datanucleus.state.ObjectProvider;

/**
 * Interface representation of the backing store for a Collection.
 */
public interface CollectionStore extends Store
{
    // --------------------------- Accessor Methods ----------------------------

    /**
     * Accessor for whether the store utilises an order mapping.
     * An order mapping is used to allow for ordering of elements or to allow duplicates.
     * @return Whether it uses an order mapping.
     */
    boolean hasOrderMapping();

    /**
     * Method to update en embedded element in the collection.
     * @param op ObjectProvider for the owner of the collection
     * @param element The element
     * @param fieldNumber Field to update in the element
     * @param value The new value for the field
     * @return Whether the element was modified
     */
    boolean updateEmbeddedElement(ObjectProvider op, Object element, int fieldNumber, Object value);

    // -------------------------- Collection Methods ---------------------------
 
    /**
     * Accessor for an iterator for the collection.
     * @param op ObjectProvider for the owner of the collection. 
     * @return Iterator for the collection.
     **/
    Iterator iterator(ObjectProvider op);

    /**
     * Accessor for the size of the collection.
     * @param op ObjectProvider for the owner of the collection. 
     * @return The size of the collection.
     **/
    int size(ObjectProvider op);

    /**
     * Method to check if an element exists in the collection.
     * @param op ObjectProvider for the owner of the collection. 
     * @param element Element to check
     * @return Whether the element exists in the collection.
     **/
    boolean contains(ObjectProvider op, Object element);

    /**
     * Method to add an element to the collection.
     * @param op ObjectProvider for the owner of the collection. 
     * @param element Element to add
     * @param size Current size of the collection if known. -1 if not known
     * @return Whether the element was added ok
     */
    boolean add(ObjectProvider op, Object element, int size);

    /**
     * Method to add a collection of elements to the collection.
     * @param op ObjectProvider for the owner of the collection. 
     * @param elements Elements to add
     * @param size Current size of collection (if known). -1 if not known
     * @return Whether the elements were added ok
     */
    boolean addAll(ObjectProvider op, Collection elements, int size);

    /**
     * Method to remove an element from the collection.
     * @param op ObjectProvider for the owner of the collection. 
     * @param element Element to remove
     * @param size Current size of collection if known. -1 if not known
     * @param allowDependentField Whether to allow any cascading delete actions to be fired from this removal
     * @return Whether the element was removed ok
     */
    boolean remove(ObjectProvider op, Object element, int size, boolean allowDependentField);

    /**
     * Method to remove a collection of elements from the collection.
     * @param op ObjectProvider for the owner of the collection. 
     * @param elements Element to remove
     * @param size Current size of collection if known. -1 if not known
     * @return Whether the elements were removed ok
     */
    boolean removeAll(ObjectProvider op, Collection elements, int size);

    /**
     * Method to clear the collection.
     * @param op ObjectProvider for the owner of the collection. 
     **/
    void clear(ObjectProvider op);

    /**
     * Method to update the collection to be the supplied collection of elements.
     * @param op ObjectProvider of the object
     * @param coll The collection to use
     */
    void update(ObjectProvider op, Collection coll);
}