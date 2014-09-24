/**********************************************************************
Copyright (c) 2010 Peter Dettman and others. All rights reserved.
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

import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.scostore.CollectionStore;
import org.datanucleus.store.scostore.Store;

/**
 * Clear operation for a collection where we have a backing store.
 */
public class CollectionClearOperation implements SCOOperation
{
    final ObjectProvider op;
    final CollectionStore store;

    public CollectionClearOperation(ObjectProvider op, CollectionStore store)
    {
        this.op = op;
        this.store = store;
    }

    /**
     * Perform the clear() operation on the specified backing store.
     */
    public void perform()
    {
        store.clear(op);
    }

    public Store getStore()
    {
        return store;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.flush.Operation#getObjectProvider()
     */
    public ObjectProvider getObjectProvider()
    {
        return op;
    }

    public String toString()
    {
        return "COLLECTION CLEAR : " + op + " field=" + store.getOwnerMemberMetaData().getName();
    }
}