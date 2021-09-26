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
package org.datanucleus.state;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * Pool of StateManager objects.
 * By default pool a maximum of 100 StateManager objects for reuse.
 * Has an optional reaper thread that cleans out the unused pooled objects every 60 secs.
 */
public class StateManagerPool
{
    private long maxIdle = 100;
    private long expirationTime;

    private Map<DNStateManager, Long> recyclableSMs;

    private CleanUpThread cleaner;

    private Class opClass;

    public StateManagerPool(int maxIdle, boolean reaperThread, Class opClass)
    {
        this.maxIdle = maxIdle;
        this.expirationTime = 30000; // 30 seconds
        this.recyclableSMs = new ConcurrentHashMap<>();
        this.opClass = opClass;

        if (reaperThread)
        {
            // Start cleanup thread to run every 60 secs
            cleaner = new CleanUpThread(this, expirationTime*2);
            cleaner.start();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug("Started pool of StateManagers (maxPool=" + maxIdle + ", reaperThread=" + reaperThread + ")");
        }
    }

    public void close()
    {
        if (cleaner != null)
        {
            cleaner.interrupt();
        }
    }

    protected DNStateManager create(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        return (DNStateManager) ClassUtils.newInstance(opClass, StateManagerFactoryImpl.STATE_MANAGER_CTR_ARG_CLASSES, new Object[] {ec, cmd});
    }

    public boolean validate(DNStateManager sm)
    {
        // TODO Any situations where we don't want to reuse it?
        return true;
    }

    public void expire(DNStateManager sm)
    {
    }

    public synchronized DNStateManager checkOut(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        long now = System.currentTimeMillis();
        DNStateManager sm;
        if (!recyclableSMs.isEmpty())
        {
            Set<DNStateManager> sms = recyclableSMs.keySet();
            Iterator<DNStateManager> smIter = sms.iterator();
            while (smIter.hasNext())
            {
                sm = smIter.next();
                if ((now - recyclableSMs.get(sm)) > expirationTime)
                {
                    // object has expired
                    recyclableSMs.remove(sm);
                    expire(sm);
                    sm = null;
                }
                else
                {
                    if (validate(sm))
                    {
                        recyclableSMs.remove(sm);
                        sm.connect(ec, cmd);
                        return sm;
                    }

                    // object failed validation
                    recyclableSMs.remove(sm);
                    expire(sm);
                    sm = null;
                }
            }
        }

        // no objects available, create a new one
        sm = create(ec, cmd);
        return sm;
    }

    public synchronized void cleanUp()
    {
       long now = System.currentTimeMillis();    
       Set<DNStateManager> sms = recyclableSMs.keySet();
       for (DNStateManager sm : sms)
       {
           if ((now - (recyclableSMs.get(sm)).longValue()) > expirationTime)
           {
               recyclableSMs.remove(sm);
               expire(sm);
               sm = null;
           }
       }
       System.gc();
    }

    public synchronized void checkIn(DNStateManager sm)
    {
        if (recyclableSMs.size() < maxIdle)
        {
            recyclableSMs.put(sm, System.currentTimeMillis());
        }
    }

    class CleanUpThread extends Thread
    {
        private StateManagerPool pool;
        private long sleepTime;

        CleanUpThread(StateManagerPool pool, long sleepTime)
        {
            this.pool = pool;
            this.sleepTime = sleepTime;
        }
        
        public void run()
        {
            boolean needsStopping = false;
            while (!needsStopping)
            {
                try
                {
                    sleep(sleepTime);
                }
                catch (InterruptedException e)
                {
                    needsStopping = true;
                }
                pool.cleanUp();
            }
        }
    }
}