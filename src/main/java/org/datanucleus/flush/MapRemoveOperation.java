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
import org.datanucleus.store.scostore.MapStore;
import org.datanucleus.store.scostore.Store;

/**
 * Remove operation for a map where we have a backing store.
 */
public class MapRemoveOperation implements SCOOperation
{
    final ObjectProvider op;
    final int fieldNumber;
    final MapStore store;

    /** The key to remove. */
    final Object key;

    /** The value to remove. */
    final Object value;

    public MapRemoveOperation(ObjectProvider op, MapStore store, Object key, Object val)
    {
        this.op = op;
        this.fieldNumber = store.getOwnerMemberMetaData().getAbsoluteFieldNumber();
        this.store = store;
        this.key = key;
        this.value = val;
    }

    public MapRemoveOperation(ObjectProvider op, int fieldNum, Object key, Object val)
    {
        this.op = op;
        this.fieldNumber = fieldNum;
        this.store = null;
        this.key = key;
        this.value = val;
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
                store.remove(op, key, value);
            }
            else
            {
                store.remove(op, key);
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
        return op;
    }

    public String toString()
    {
        return "MAP REMOVE : " + op + " field=" + 
            (store!=null?store.getOwnerMemberMetaData().getName() : op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber).getName());
    }
}