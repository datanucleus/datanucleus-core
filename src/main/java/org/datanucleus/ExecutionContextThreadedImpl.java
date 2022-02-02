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
 * Locks various methods in an attempt to prevent conflicting thread updates.
 * Note we could have just put this code in ExecutionContextImpl but better to split it out since the majority use-case is to have a non-thread-safe PM/EM.
 * Note also that having thread-safe ExecutionContext usage depends on much more than having this class, since SCO wrappers would need to coordinate
 * with such locks, as would the Transaction for the ExecutionContext.
 * TODO Evaluate all of the places we currently lock (when multithreaded) to find corner cases not caught.
 * <p>
 * This class *tries to be* thread-safe, but there is no guarantee. You are better advised to design your application to use ExecutionContextImpl for a single thread.
 * </p>
 */
public class ExecutionContextThreadedImpl extends ExecutionContextImpl
{
    /** Lock object for use during commit/rollback/evict, to prevent any further field accesses. */
    protected Lock lock;

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

    /**
     * Accessor for the context lock object. 
     * @return The lock object
     */
    protected final Lock getLock()
    {
        if (lock == null)
        {
            lock = new ReentrantLock();
        }
        return lock;
    }

    @Override
    public synchronized void threadLock()
    {
        if (lockCounter == 0)
        {
            getLock().lock();
        }
        lockCounter++;
    }

    @Override
    public synchronized void threadUnlock()
    {
        lockCounter--;
        if (lockCounter == 0)
        {
            getLock().unlock();
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
            getLock().lock();

            super.processNontransactionalUpdate();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void enlistInTransaction(DNStateManager sm)
    {
        try
        {
            getLock().lock();

            super.enlistInTransaction(sm);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void evictFromTransaction(DNStateManager sm)
    {
        try
        {
            getLock().lock();

            super.evictFromTransaction(sm);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void addStateManagerToCache(DNStateManager sm)
    {
        try
        {
            getLock().lock();

            super.addStateManagerToCache(sm);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void removeStateManagerFromCache(DNStateManager sm)
    {
        try
        {
            getLock().lock();

            super.removeStateManagerFromCache(sm);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public DNStateManager findStateManager(Object pc)
    {
        try
        {
            getLock().lock();

            return super.findStateManager(pc);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void close()
    {
        try
        {
            getLock().lock();

            super.close();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void evictObject(Object obj)
    {
        try
        {
            getLock().lock();

            super.evictObject(obj);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void refreshObject(Object obj)
    {
        try
        {
            getLock().lock();

            super.refreshObject(obj);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void retrieveObjects(boolean useFetchPlan, Object... pcs)
    {
        try
        {
            getLock().lock();

            super.retrieveObjects(useFetchPlan, pcs);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public Object persistObject(Object obj, boolean merging)
    {
        try
        {
            getLock().lock();

            return super.persistObject(obj, merging);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public Object[] persistObjects(Object... objs)
    {
        try
        {
            getLock().lock();

            return super.persistObjects(objs);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void deleteObject(Object obj)
    {
        try
        {
            getLock().lock();

            super.deleteObject(obj);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void deleteObjects(Object... objs)
    {
        try
        {
            getLock().lock();

            super.deleteObjects(objs);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void makeObjectTransient(Object obj, FetchPlanState state)
    {
        try
        {
            getLock().lock();

            super.makeObjectTransient(obj, state);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void makeObjectTransactional(Object obj)
    {
        try
        {
            getLock().lock();

            super.makeObjectTransactional(obj);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void attachObject(DNStateManager ownerSM, Object pc, boolean sco)
    {
        try
        {
            getLock().lock();

            super.attachObject(ownerSM, pc, sco);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public Object attachObjectCopy(DNStateManager ownerSM, Object pc, boolean sco)
    {
        try
        {
            getLock().lock();

            return super.attachObjectCopy(ownerSM, pc, sco);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void detachObject(FetchPlanState state, Object obj)
    {
        try
        {
            getLock().lock();

            super.detachObject(state, obj);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void detachObjects(FetchPlanState state, Object... objs)
    {
        try
        {
            getLock().lock();

            super.detachObjects(state, objs);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public Object detachObjectCopy(Object pc, FetchPlanState state)
    {
        try
        {
            getLock().lock();

            return super.detachObjectCopy(state, pc);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void clearDirty(DNStateManager sm)
    {
        try
        {
            getLock().lock();

            super.clearDirty(sm);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void clearDirty()
    {
        try
        {
            getLock().lock();

            super.clearDirty();
        }
        finally
        {
            getLock().unlock();
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
            getLock().lock();

            super.evictAllObjects();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void markDirty(DNStateManager sm, boolean directUpdate)
    {
        try
        {
            getLock().lock();

            super.markDirty(sm, directUpdate);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void flush()
    {
        try
        {
            getLock().lock();

            super.flush();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void flushInternal(boolean flushToDatastore)
    {
        try
        {
            getLock().lock();

            super.flushInternal(flushToDatastore);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void replaceObjectId(Persistable pc, Object oldID, Object newID)
    {
        try
        {
            getLock().lock();

            super.replaceObjectId(pc, oldID, newID);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public Extent getExtent(Class pcClass, boolean subclasses)
    {
        try
        {
            getLock().lock();

            return super.getExtent(pcClass, subclasses);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void evictObjects(Class cls, boolean subclasses)
    {
        try
        {
            getLock().lock();

            super.evictObjects(cls, subclasses);
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void refreshAllObjects()
    {
        try
        {
            getLock().lock();

            super.refreshAllObjects();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public List<DNStateManager> getObjectsToBeFlushed()
    {
        try
        {
            getLock().lock();

            return super.getObjectsToBeFlushed();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void postBegin()
    {
        try
        {
            getLock().lock();

            super.postBegin();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void preCommit()
    {
        try
        {
            getLock().lock();

            super.preCommit();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void postCommit()
    {
        try
        {
            getLock().lock();

            super.postCommit();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void preRollback()
    {
        try
        {
            getLock().lock();

            super.preRollback();
        }
        finally
        {
            getLock().unlock();
        }
    }

    public void postRollback()
    {
        try
        {
            getLock().lock();

            super.postRollback();
        }
        finally
        {
            getLock().unlock();
        }
    }
}