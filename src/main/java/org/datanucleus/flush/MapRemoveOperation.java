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
import org.datanucleus.store.types.scostore.MapStore;
import org.datanucleus.store.types.scostore.Store;

/**
 * Remove operation for a map where we have a backing store.
 */
public class MapRemoveOperation<K, V> implements SCOOperation
{
    final ObjectProvider sm;
    final int fieldNumber;
    final MapStore<K, V> store;

    /** The key to remove. */
    final K key;

    /** The value to remove. */
    final V value;

    public MapRemoveOperation(ObjectProvider sm, MapStore store, K key, V val)
    {
        this.sm = sm;
        this.fieldNumber = store.getOwnerMemberMetaData().getAbsoluteFieldNumber();
        this.store = store;
        this.key = key;
        this.value = val;
    }

    public MapRemoveOperation(ObjectProvider sm, int fieldNum, K key, V val)
    {
        this.sm = sm;
        this.fieldNumber = fieldNum;
        this.store = null;
        this.key = key;
        this.value = val;
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
     * Accessor for the key being removed.
     * @return Key being removed
     */
    public Object getKey()
    {
        return key;
    }

    /**
     * Accessor for the value being removed for this key (if known).
     * @return Value being removed if known
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
        if (store != null)
        {
            if (value != null)
            {
                store.remove(sm, key, value);
            }
            else
            {
                store.remove(sm, key);
            }
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
        return sm;
    }

    public String toString()
    {
        return "MAP REMOVE : " + sm + " field=" + getMemberMetaData().getName();    }
}