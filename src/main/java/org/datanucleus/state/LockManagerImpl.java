/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a lock manager for objects.
 */
public class LockManagerImpl implements LockManager
{
    Map<Object, Short> requiredLockModesById = null;

    public LockManagerImpl()
    {
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#close()
     */
    public void close()
    {
        clear();
        requiredLockModesById = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#lock(java.lang.Object, short)
     */
    public void lock(Object id, short lockMode)
    {
        if (requiredLockModesById == null)
        {
            requiredLockModesById = new HashMap<>();
        }
        requiredLockModesById.put(id, lockMode);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#getLockMode(java.lang.Object)
     */
    public short getLockMode(Object id)
    {
        if (requiredLockModesById != null)
        {
            Short lockMode = requiredLockModesById.get(id);
            if (lockMode != null)
            {
                return lockMode.shortValue();
            }
        }

        return LOCK_MODE_NONE;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#clear()
     */
    public void clear()
    {
        if (requiredLockModesById != null)
        {
            requiredLockModesById.clear();
            requiredLockModesById = null;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#lock(org.datanucleus.ObjectProvider, short)
     */
    public void lock(ObjectProvider op, short lockMode)
    {
        op.lock(lockMode);

        if (lockMode == LOCK_MODE_PESSIMISTIC_READ || lockMode == LOCK_MODE_PESSIMISTIC_WRITE)
        {
            // Do a SELECT ... FOR UPDATE (for RDBMS) or alternative to lock the object in the datastore
            op.locate();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#unlock(org.datanucleus.ObjectProvider)
     */
    public void unlock(ObjectProvider op)
    {
        // TODO Need to remove any row lock from the datastore
        op.unlock();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#getLockMode(org.datanucleus.ObjectProvider)
     */
    public short getLockMode(ObjectProvider op)
    {
        return op.getLockMode();
    }
}