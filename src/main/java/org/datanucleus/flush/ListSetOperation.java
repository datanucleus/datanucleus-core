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
import org.datanucleus.store.scostore.ListStore;
import org.datanucleus.store.scostore.Store;

/**
 * Set operation for a list.
 */
public class ListSetOperation implements SCOOperation
{
    final ObjectProvider op;
    final ListStore store;

    /** The position to set the value at. */
    private final int index;

    /** The value to set. */
    private final Object value;

    /** Whether to allow cascade-delete checks. */
    boolean allowCascadeDelete = true;

    public ListSetOperation(ObjectProvider op, ListStore store, int index, Object value, boolean allowCascadeDelete)
    {
        this.op = op;
        this.store = store;
        this.index = index;
        this.value = value;
        this.allowCascadeDelete = allowCascadeDelete;
    }

    /**
     * Perform the set(int, Object) operation to the backing store.
     */
    public void perform()
    {
        store.set(op, index, value, allowCascadeDelete);
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
        return "COLLECTION SET : " + op + " field=" + store.getOwnerMemberMetaData().getName() + " index=" + index;
    }
}