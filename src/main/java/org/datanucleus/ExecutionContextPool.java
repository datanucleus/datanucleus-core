/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ExecutionContext;
import org.datanucleus.util.NucleusLogger;

/**
 * Pool of ExecutionContext objects.
 * By default will pool a maximum of 20 ExecutionContext objects for reuse.
 * Has an optional reaper thread that cleans out the unused pooled objects every 60 secs.
 */
public class ExecutionContextPool
{
    private PersistenceNucleusContext nucCtx;

    private long maxIdle = 20;
    private long expirationTime;

    private Map<ExecutionContext, Long> recyclableECs;

    private CleanUpThread cleaner;

    public ExecutionContextPool(PersistenceNucleusContext nucCtx)
    {
        this.maxIdle = nucCtx.getConfiguration().getIntProperty(PropertyNames.PROPERTY_EXECUTION_CONTEXT_MAX_IDLE);
        this.nucCtx = nucCtx;
        this.expirationTime = 30000; // 30 seconds
        this.recyclableECs = new ConcurrentHashMap<ExecutionContext, Long>();

        // Start cleanup thread to run every 60 secs
        if (nucCtx.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_EXECUTION_CONTEXT_REAPER_THREAD))
        {
            cleaner = new CleanUpThread(this, expirationTime*2);
            cleaner.start();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug("Started pool of ExecutionContext (maxPool=" + maxIdle + 
                ", reaperThread=" + (cleaner != null) + ")");
        }
    }

    protected ExecutionContext create(Object owner, Map<String, Object> options)
    {
        if (nucCtx.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_MULTITHREADED))
        {
            return new ExecutionContextThreadedImpl(nucCtx, owner, options);
        }
        else
        {
            return new ExecutionContextImpl(nucCtx, owner, options);
        }
    }

    public boolean validate(ExecutionContext ec)
    {
        // TODO Any situations where we don't want to reuse it?
        return true;
    }

    public void expire(ExecutionContext ec)
    {
    }

    public synchronized ExecutionContext checkOut(Object owner, Map<String, Object> options)
    {
        long now = System.currentTimeMillis();
        ExecutionContext ec;
        if (recyclableECs.size() > 0)
        {
            Set<ExecutionContext> e = recyclableECs.keySet();
            Iterator<ExecutionContext> eIter = e.iterator();
            while (eIter.hasNext())
            {
                ec = eIter.next();
                if ((now - recyclableECs.get(ec)) > expirationTime)
                {
                    // object has expired
                    recyclableECs.remove(ec);
                    expire(ec);
                    ec = null;
                }
                else
                {
                    if (validate(ec))
                    {
                        recyclableECs.remove(ec);
                        ec.initialise(owner, options);
                        return (ec);
                    }
                    else
                    {
                        // object failed validation
                        recyclableECs.remove(ec);
                        expire(ec);
                        ec = null;
                    }
                }
            }
        }

        // no objects available, create a new one
        ec = create(owner, options);
        return (ec);
    }

    public synchronized void cleanUp()
    {
        ExecutionContext ec;
        long now = System.currentTimeMillis();        
        Set<ExecutionContext> e = recyclableECs.keySet();
        Iterator<ExecutionContext> eIter = e.iterator();
        while (eIter.hasNext())
        {
            ec = eIter.next();
            if ((now - (recyclableECs.get(ec)).longValue()) > expirationTime)
            {
                recyclableECs.remove(ec);
                expire(ec);
                ec = null;
            }
        }
        System.gc();
    }

    public synchronized void checkIn(ExecutionContext ec)
    {
        if (recyclableECs.size() < maxIdle)
        {
            recyclableECs.put(ec, System.currentTimeMillis());
        }
    }

    class CleanUpThread extends Thread
    {
        private ExecutionContextPool pool;
        private long sleepTime;

        CleanUpThread(ExecutionContextPool pool, long sleepTime)
        {
            this.pool = pool;
            this.sleepTime = sleepTime;
        }
        
        public void run()
        {
            while (true)
            {
                try
                {
                    sleep(sleepTime);
                }
                catch (InterruptedException e)
                {
                }         
                pool.cleanUp();
            }
        }
    }
}