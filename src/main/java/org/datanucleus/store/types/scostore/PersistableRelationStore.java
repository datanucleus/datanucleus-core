/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.state.ObjectProvider;

/**
 * Representation of the relation between two persistable objects.
 * Provides the connectivity to the datastore allowing the relation to be managed separately.
 * In an RDBMS sense, this is for an N-1 unidirectional join table relation, and represents the join table entry.
 */
public interface PersistableRelationStore extends Store
{
    /**
     * Method to add the relation between the provided objects.
     * @param op1 Object 1 provider
     * @param op2 Object 2 provider
     * @return Whether the relation was added
     */
    boolean add(ObjectProvider op1, ObjectProvider op2);

    /**
     * Method to remove the relation from the provided object.
     * @param op1 Object 1 provider
     * @return Whether the relation was removed
     */
    boolean remove(ObjectProvider op1);

    /**
     * Method to update the relation for the first object to relate to the second object.
     * This removes any previous relation from this object and replaces it with the new relation.
     * @param op1 Object 1 provider
     * @param op2 Object 2 provider
     * @return Whether the relation was replaced
     */
    boolean update(ObjectProvider op1, ObjectProvider op2);
}