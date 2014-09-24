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
import org.datanucleus.util.StringUtils;

/**
 * Remove operation for a collection where we have a backing store.
 */
public class CollectionRemoveOperation implements SCOOperation
{
    final ObjectProvider op;
    final CollectionStore store;

    /** The value to remove. */
    private final Object value;

    /** Whether to allow cascade-delete checks. */
    private final boolean allowCascadeDelete;

    public CollectionRemoveOperation(ObjectProvider op, CollectionStore store, Object value, boolean allowCascadeDelete)
    {
        this.op = op;
        this.store = store;
        this.value = value;
        this.allowCascadeDelete = allowCascadeDelete;
    }

    /**
     * Accessor for the value being removed.
     * @return Value being removed
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * Perform the remove(Object) operation on the specified container.
     */
    public void perform()
    {
        store.remove(op, value, -1, allowCascadeDelete);
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
        return "COLLECTION REMOVE : " + op + " field=" + store.getOwnerMemberMetaData().getName() + " value=" + StringUtils.toJVMIDString(value);
    }
}