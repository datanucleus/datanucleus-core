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

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.scostore.CollectionStore;
import org.datanucleus.store.types.scostore.Store;
import org.datanucleus.util.StringUtils;

/**
 * Remove operation for a collection.
 * This is usually for the situation where we have a backing store, but also can be used where we are removing an object from a collection and
 * the field is marked as cascade delete but we don't want to delete immediately.
 */
public class CollectionRemoveOperation<E> implements SCOOperation
{
    final ObjectProvider op;
    final int fieldNumber;
    final CollectionStore<E> store;

    /** The value to remove. */
    final E value;

    /** Whether to allow cascade-delete checks. */
    final boolean allowCascadeDelete;

    public CollectionRemoveOperation(ObjectProvider op, CollectionStore<E> store, E value, boolean allowCascadeDelete)
    {
        this.op = op;
        this.fieldNumber = store.getOwnerMemberMetaData().getAbsoluteFieldNumber();
        this.store = store;
        this.value = value;
        this.allowCascadeDelete = allowCascadeDelete;
    }

    public CollectionRemoveOperation(ObjectProvider op, int fieldNum, E value, boolean allowCascadeDelete)
    {
        this.op = op;
        this.fieldNumber = fieldNum;
        this.store = null;
        this.value = value;
        this.allowCascadeDelete = allowCascadeDelete;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.flush.SCOOperation#getMemberMetaData()
     */
    @Override
    public AbstractMemberMetaData getMemberMetaData()
    {
        return store != null ? store.getOwnerMemberMetaData() : op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
    }

    /**
     * Accessor for the value being removed.
     * @return Value being removed
     */
    public E getValue()
    {
        return value;
    }

    /**
     * Perform the remove(Object) operation on the specified container.
     */
    public void perform()
    {
        if (store != null)
        {
            store.remove(op, value, -1, allowCascadeDelete);
        }
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
        return "COLLECTION REMOVE : " + op + " field=" + getMemberMetaData().getName() + " value=" + StringUtils.toJVMIDString(value);
    }
}