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

import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.scostore.CollectionStore;
import org.datanucleus.store.scostore.Store;

/**
 * Add operation for a collection.
 */
public class CollectionAddOperation implements SCOOperation
{
    final ObjectProvider op;
    final CollectionStore store;

    /** The value to add. */
    private final Object value;

    public CollectionAddOperation(ObjectProvider op, CollectionStore store, Object value)
    {
        this.op = op;
        this.store = store;
        this.value = value;
    }

    /**
     * Accessor for the value being added.
     * @return Value being added
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * Perform the add(Object) operation to the backing store.
     */
    public void perform()
    {
        // TODO If this value is the detached object and we are using attachCopy this needs swapping for the attached object
        store.add(op, value, -1);
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
        return "COLLECTION ADD : " + op + " field=" + store.getOwnerMemberMetaData().getName();
    }
}