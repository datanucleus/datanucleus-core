/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
import org.datanucleus.store.types.scostore.ListStore;

/**
 * Add operation at a position for a list where we have a backing store.
 */
public class ListAddAtOperation<E> extends CollectionAddOperation<E>
{
    /** Index to add the object at. */
    final int index;

    public ListAddAtOperation(DNStateManager sm, ListStore<E> store, int index, E value)
    {
        super(sm, store, value);
        this.index = index;
    }

    public ListAddAtOperation(DNStateManager sm, int fieldNum, int index, E value)
    {
        super(sm, fieldNum, value);
        this.index = index;
    }

    /**
     * Perform the add(int, Object) operation on the specified list.
     */
    public void perform()
    {
        if (store != null)
        {
            ((ListStore)store).add(sm, value, index, -1);
        }
    }

    public String toString()
    {
        return "LIST ADD-AT : " + sm + " field=" + getMemberMetaData().getName() + " index=" + index;
    }
}