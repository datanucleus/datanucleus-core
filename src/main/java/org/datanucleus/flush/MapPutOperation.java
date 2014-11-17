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
import org.datanucleus.store.scostore.MapStore;
import org.datanucleus.store.scostore.Store;

/**
 * Put operation for a map where we have a backing store.
 */
public class MapPutOperation implements SCOOperation
{
    final ObjectProvider op;
    final int fieldNumber;
    final MapStore store;

    /** The key to add. */
    final Object key;

    /** The value to add. */
    final Object value;

    public MapPutOperation(ObjectProvider op, MapStore store, Object key, Object value)
    {
        this.op = op;
        this.fieldNumber = store.getOwnerMemberMetaData().getAbsoluteFieldNumber();
        this.store = store;
        this.key = key;
        this.value = value;
    }

    public MapPutOperation(ObjectProvider op, int fieldNum, Object key, Object value)
    {
        this.op = op;
        this.fieldNumber = fieldNum;
        this.store = null;
        this.key = key;
        this.value = value;
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
     * Accessor for the key being put.
     * @return Key being put
     */
    public Object getKey()
    {
        return key;
    }

    /**
     * Accessor for the value being put against this key.
     * @return Value being put against the key
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * Perform the put(Object, Object) operation to the backing store.
     */
    public void perform()
    {
        if (store != null)
        {
            store.put(op, key, value);
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
        return "MAP PUT : " + op + " field=" + getMemberMetaData().getName();
    }
}