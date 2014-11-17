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

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.scostore.ListStore;
import org.datanucleus.store.scostore.Store;

/**
 * Remove operation for a list at a particular index where we have a backing store.
 */
public class ListRemoveAtOperation implements SCOOperation
{
    final ObjectProvider op;
    final int fieldNumber;
    final ListStore store;

    /** The index to remove. */
    final int index;

    public ListRemoveAtOperation(ObjectProvider op, ListStore store, int index)
    {
        this.op = op;
        this.fieldNumber = store.getOwnerMemberMetaData().getAbsoluteFieldNumber();
        this.store = store;
        this.index = index;
    }

    public ListRemoveAtOperation(ObjectProvider op, int fieldNum, int index)
    {
        this.op = op;
        this.fieldNumber = fieldNum;
        this.store = null;
        this.index = index;
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
     * Perform the remove(int) operation on the specified container.
     */
    public void perform()
    {
        if (store != null)
        {
            store.remove(op, index, -1);
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
        return "COLLECTION REMOVE-AT : " + op + " field=" + 
            (store!=null?store.getOwnerMemberMetaData().getName() : op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber).getName()) + " index=" + index;
    }
}