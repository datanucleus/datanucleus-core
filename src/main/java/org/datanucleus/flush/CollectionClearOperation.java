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
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.scostore.CollectionStore;
import org.datanucleus.store.types.scostore.Store;

/**
 * Clear operation for a collection where we have a backing store.
 */
public class CollectionClearOperation implements SCOOperation
{
    final DNStateManager sm;
    final int fieldNumber;
    final CollectionStore store;

    public CollectionClearOperation(DNStateManager sm, CollectionStore store)
    {
        this.sm = sm;
        this.fieldNumber = store.getOwnerMemberMetaData().getAbsoluteFieldNumber();
        this.store = store;
    }

    public CollectionClearOperation(DNStateManager sm, int fieldNum)
    {
        this.sm = sm;
        this.fieldNumber = fieldNum;
        this.store = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.flush.SCOOperation#getMemberMetaData()
     */
    @Override
    public AbstractMemberMetaData getMemberMetaData()
    {
        return store != null ? store.getOwnerMemberMetaData() : sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
    }

    /**
     * Perform the clear() operation on the specified backing store.
     */
    public void perform()
    {
        if (store != null)
        {
            store.clear(sm);
        }
    }

    public Store getStore()
    {
        return store;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.flush.Operation#getStateManager()
     */
    public DNStateManager getStateManager()
    {
        return sm;
    }

    public String toString()
    {
        return "COLLECTION CLEAR : " + sm + " field=" + getMemberMetaData().getName();
    }
}