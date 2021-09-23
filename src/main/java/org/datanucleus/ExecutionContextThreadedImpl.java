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
import org.datanucleus.state.ObjectProvider;
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

    /**
     * @param ctx NucleusContext
     * @param owner Owner object (PM, EM)
     * @param options Any options affecting startup
     */
    public ExecutionContextThreadedImpl(PersistenceNucleusContext ctx, Object owner, Map<String, Object> options)
    {
        super(ctx, owner, options);
        this.lock = new ReentrantLock();
    }

    /**
     * Accessor for the context lock object. 
     * @return The lock object
     */
    public Lock getLock()
    {
        return lock;
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
            lock.lock();

            super.processNontransactionalUpdate();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void enlistInTransaction(ObjectProvider sm)
    {
        try
        {
            lock.lock();

            super.enlistInTransaction(sm);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void evictFromTransaction(ObjectProvider sm)
    {
        try
        {
            lock.lock();

            super.evictFromTransaction(sm);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void addObjectProviderToCache(ObjectProvider sm)
    {
        try
        {
            lock.lock();

            super.addObjectProviderToCache(sm);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void removeObjectProviderFromCache(ObjectProvider sm)
    {
        try
        {
            lock.lock();

            super.removeObjectProviderFromCache(sm);
        }
        finally
        {
            lock.unlock();
        }
    }

    public ObjectProvider findObjectProvider(Object pc)
    {
        try
        {
            lock.lock();

            return super.findObjectProvider(pc);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void close()
    {
        try
        {
            lock.lock();

            super.close();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void evictObject(Object obj)
    {
        try
        {
            lock.lock();

            super.evictObject(obj);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void refreshObject(Object obj)
    {
        try
        {
            lock.lock();

            super.refreshObject(obj);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void retrieveObjects(boolean useFetchPlan, Object... pcs)
    {
        try
        {
            lock.lock();

            super.retrieveObjects(useFetchPlan, pcs);
        }
        finally
        {
            lock.unlock();
        }
    }

    public Object persistObject(Object obj, boolean merging)
    {
        try
        {
            lock.lock();

            return super.persistObject(obj, merging);
        }
        finally
        {
            lock.unlock();
        }
    }

    public Object[] persistObjects(Object... objs)
    {
        try
        {
            lock.lock();

            return super.persistObjects(objs);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void deleteObject(Object obj)
    {
        try
        {
            lock.lock();

            super.deleteObject(obj);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void deleteObjects(Object... objs)
    {
        try
        {
            lock.lock();

            super.deleteObjects(objs);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void makeObjectTransient(Object obj, FetchPlanState state)
    {
        try
        {
            lock.lock();

            super.makeObjectTransient(obj, state);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void makeObjectTransactional(Object obj)
    {
        try
        {
            lock.lock();

            super.makeObjectTransactional(obj);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void attachObject(ObjectProvider ownerSM, Object pc, boolean sco)
    {
        try
        {
            lock.lock();

            super.attachObject(ownerSM, pc, sco);
        }
        finally
        {
            lock.unlock();
        }
    }

    public Object attachObjectCopy(ObjectProvider ownerSM, Object pc, boolean sco)
    {
        try
        {
            lock.lock();

            return super.attachObjectCopy(ownerSM, pc, sco);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void detachObject(FetchPlanState state, Object obj)
    {
        try
        {
            lock.lock();

            super.detachObject(state, obj);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void detachObjects(FetchPlanState state, Object... objs)
    {
        try
        {
            lock.lock();

            super.detachObjects(state, objs);
        }
        finally
        {
            lock.unlock();
        }
    }

    public Object detachObjectCopy(Object pc, FetchPlanState state)
    {
        try
        {
            lock.lock();

            return super.detachObjectCopy(state, pc);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void clearDirty(ObjectProvider sm)
    {
        try
        {
            lock.lock();

            super.clearDirty(sm);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void clearDirty()
    {
        try
        {
            lock.lock();

            super.clearDirty();
        }
        finally
        {
            lock.unlock();
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
            lock.lock();

            super.evictAllObjects();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void markDirty(ObjectProvider sm, boolean directUpdate)
    {
        try
        {
            lock.lock();

            super.markDirty(sm, directUpdate);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void flush()
    {
        try
        {
            lock.lock();

            super.flush();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void flushInternal(boolean flushToDatastore)
    {
        try
        {
            lock.lock();

            super.flushInternal(flushToDatastore);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void replaceObjectId(Persistable pc, Object oldID, Object newID)
    {
        try
        {
            lock.lock();

            super.replaceObjectId(pc, oldID, newID);
        }
        finally
        {
            lock.unlock();
        }
    }

    public Extent getExtent(Class pcClass, boolean subclasses)
    {
        try
        {
            lock.lock();

            return super.getExtent(pcClass, subclasses);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void evictObjects(Class cls, boolean subclasses)
    {
        try
        {
            lock.lock();

            super.evictObjects(cls, subclasses);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void refreshAllObjects()
    {
        try
        {
            lock.lock();

            super.refreshAllObjects();
        }
        finally
        {
            lock.unlock();
        }
    }

    public List<ObjectProvider> getObjectsToBeFlushed()
    {
        try
        {
            lock.lock();

            return super.getObjectsToBeFlushed();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void postBegin()
    {
        try
        {
            lock.lock();

            super.postBegin();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void preCommit()
    {
        try
        {
            lock.lock();

            super.preCommit();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void postCommit()
    {
        try
        {
            lock.lock();

            super.postCommit();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void preRollback()
    {
        try
        {
            lock.lock();

            super.preRollback();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void postRollback()
    {
        try
        {
            lock.lock();

            super.postRollback();
        }
        finally
        {
            lock.unlock();
        }
    }
}