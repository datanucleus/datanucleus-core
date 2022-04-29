/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.datanucleus.enhancement.Persistable;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.query.Extent;

/**
 * ExecutionContext to attempt to handle multi-threaded PM/EM cases.
 * Intercepts various methods in an attempt to prevent conflicting thread updates by locking the current thread.
 * Note we could have just put this code in ExecutionContextImpl but better to split it out since the majority use-case is to have a non-thread-safe PM/EM.
 * Note also that having thread-safe ExecutionContext usage depends on much more than having this class, since SCO wrappers would need to coordinate
 * with such locks, as would the Transaction for the ExecutionContext.
 * TODO Evaluate all of the places we currently lock (when multithreaded) to find corner cases not caught.
 * <p>
 * This class *tries to be* thread-safe, but there is no guarantee. You are better advised to design your application to use PM/EM for a single thread.
 * </p>
 */
public class ExecutionContextThreadedImpl extends ExecutionContextImpl
{
    /** Lock object to lock to the current thread, and then release when the operation is complete. */
    protected Lock lock;

    /** Counter in case a user operation causes the call of another method that will lock, so only unlock when this is 0. */
    int lockCounter = 0;

    /**
     * @param ctx NucleusContext
     * @param owner Owner object (PM, EM)
     * @param options Any options affecting startup
     */
    public ExecutionContextThreadedImpl(PersistenceNucleusContext ctx, Object owner, Map<String, Object> options)
    {
        super(ctx, owner, options);
    }

    @Override
    public synchronized void threadLock()
    {
        if (lockCounter == 0 && lock == null)
        {
            lock = new ReentrantLock();
        }
        lockCounter++;
    }

    @Override
    public synchronized void threadUnlock()
    {
        lockCounter--;
        if (lockCounter == 0 && lock != null)
        {
            lock.unlock();
        }
    }

    /**
     * Accessor for whether the usage is multi-threaded.
     * @return True
     */
    public boolean getMultithreaded()
    {
        return true;
    }

    public void processNontransactionalUpdate()
    {
        try
        {
            threadLock();

            super.processNontransactionalUpdate();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void enlistInTransaction(DNStateManager sm)
    {
        try
        {
            threadLock();

            super.enlistInTransaction(sm);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void evictFromTransaction(DNStateManager sm)
    {
        try
        {
            threadLock();

            super.evictFromTransaction(sm);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void addStateManagerToCache(DNStateManager sm)
    {
        try
        {
            threadLock();

            super.addStateManagerToCache(sm);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void removeStateManagerFromCache(DNStateManager sm)
    {
        try
        {
            threadLock();

            super.removeStateManagerFromCache(sm);
        }
        finally
        {
            threadUnlock();
        }
    }

    public DNStateManager findStateManager(Object pc)
    {
        try
        {
            threadLock();

            return super.findStateManager(pc);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void close()
    {
        try
        {
            threadLock();

            super.close();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void evictObject(Object obj)
    {
        try
        {
            threadLock();

            super.evictObject(obj);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void refreshObject(Object obj)
    {
        try
        {
            threadLock();

            super.refreshObject(obj);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void retrieveObjects(boolean useFetchPlan, Object... pcs)
    {
        try
        {
            threadLock();

            super.retrieveObjects(useFetchPlan, pcs);
        }
        finally
        {
            threadUnlock();
        }
    }

    public Object persistObject(Object obj, boolean merging)
    {
        try
        {
            threadLock();

            return super.persistObject(obj, merging);
        }
        finally
        {
            threadUnlock();
        }
    }

    public Object[] persistObjects(Object... objs)
    {
        try
        {
            threadLock();

            return super.persistObjects(objs);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void deleteObject(Object obj)
    {
        try
        {
            threadLock();

            super.deleteObject(obj);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void deleteObjects(Object... objs)
    {
        try
        {
            threadLock();

            super.deleteObjects(objs);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void makeObjectTransient(Object obj, FetchPlanState state)
    {
        try
        {
            threadLock();

            super.makeObjectTransient(obj, state);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void makeObjectTransactional(Object obj)
    {
        try
        {
            threadLock();

            super.makeObjectTransactional(obj);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void attachObject(DNStateManager ownerSM, Object pc, boolean sco)
    {
        try
        {
            threadLock();

            super.attachObject(ownerSM, pc, sco);
        }
        finally
        {
            threadUnlock();
        }
    }

    public Object attachObjectCopy(DNStateManager ownerSM, Object pc, boolean sco)
    {
        try
        {
            threadLock();

            return super.attachObjectCopy(ownerSM, pc, sco);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void detachObject(FetchPlanState state, Object obj)
    {
        try
        {
            threadLock();

            super.detachObject(state, obj);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void detachObjects(FetchPlanState state, Object... objs)
    {
        try
        {
            threadLock();

            super.detachObjects(state, objs);
        }
        finally
        {
            threadUnlock();
        }
    }

    public Object detachObjectCopy(Object pc, FetchPlanState state)
    {
        try
        {
            threadLock();

            return super.detachObjectCopy(state, pc);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void clearDirty(DNStateManager sm)
    {
        try
        {
            threadLock();

            super.clearDirty(sm);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void clearDirty()
    {
        try
        {
            threadLock();

            super.clearDirty();
        }
        finally
        {
            threadUnlock();
        }
    }

    /**
     * Method to evict all current objects from L1 cache.
     */
    public void evictAllObjects()
    {
        assertIsOpen();

        try
        {
            threadLock();

            super.evictAllObjects();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void markDirty(DNStateManager sm, boolean directUpdate)
    {
        try
        {
            threadLock();

            super.markDirty(sm, directUpdate);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void flush()
    {
        try
        {
            threadLock();

            super.flush();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void flushInternal(boolean flushToDatastore)
    {
        try
        {
            threadLock();

            super.flushInternal(flushToDatastore);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void replaceObjectId(Persistable pc, Object oldID, Object newID)
    {
        try
        {
            threadLock();

            super.replaceObjectId(pc, oldID, newID);
        }
        finally
        {
            threadUnlock();
        }
    }

    public Extent getExtent(Class pcClass, boolean subclasses)
    {
        try
        {
            threadLock();

            return super.getExtent(pcClass, subclasses);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void evictObjects(Class cls, boolean subclasses)
    {
        try
        {
            threadLock();

            super.evictObjects(cls, subclasses);
        }
        finally
        {
            threadUnlock();
        }
    }

    public void refreshAllObjects()
    {
        try
        {
            threadLock();

            super.refreshAllObjects();
        }
        finally
        {
            threadUnlock();
        }
    }

    public List<DNStateManager> getObjectsToBeFlushed()
    {
        try
        {
            threadLock();

            return super.getObjectsToBeFlushed();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void postBegin()
    {
        try
        {
            threadLock();

            super.postBegin();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void preCommit()
    {
        try
        {
            threadLock();

            super.preCommit();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void postCommit()
    {
        try
        {
            threadLock();

            super.postCommit();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void preRollback()
    {
        try
        {
            threadLock();

            super.preRollback();
        }
        finally
        {
            threadUnlock();
        }
    }

    public void postRollback()
    {
        try
        {
            threadLock();

            super.postRollback();
        }
        finally
        {
            threadUnlock();
        }
    }
}