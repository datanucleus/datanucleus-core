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
 * Pool of ObjectProvider objects.
 * By default pool a maximum of 100 ObjectProvider objects for reuse.
 * Has an optional reaper thread that cleans out the unused pooled objects every 60 secs.
 */
public class ObjectProviderPool
{
    private long maxIdle = 100;
    private long expirationTime;

    private Map<ObjectProvider, Long> recyclableOps;

    private CleanUpThread cleaner;

    private Class opClass;

    public ObjectProviderPool(int maxIdle, boolean reaperThread, Class opClass)
    {
        this.maxIdle = maxIdle;
        this.expirationTime = 30000; // 30 seconds
        this.recyclableOps = new ConcurrentHashMap<>();
        this.opClass = opClass;

        if (reaperThread)
        {
            // Start cleanup thread to run every 60 secs
            cleaner = new CleanUpThread(this, expirationTime*2);
            cleaner.start();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug("Started pool of ObjectProviders (maxPool=" + maxIdle + 
                ", reaperThread=" + reaperThread + ")");
        }
    }

    public void close()
    {
        if (cleaner != null)
        {
            cleaner.interrupt();
        }
    }

    protected ObjectProvider create(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        return (ObjectProvider) ClassUtils.newInstance(opClass, ObjectProviderFactoryImpl.OBJECT_PROVIDER_CTR_ARG_CLASSES, 
            new Object[] {ec, cmd});
    }

    public boolean validate(ObjectProvider op)
    {
        // TODO Any situations where we don't want to reuse it?
        return true;
    }

    public void expire(ObjectProvider op)
    {
    }

    public synchronized ObjectProvider checkOut(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        long now = System.currentTimeMillis();
        ObjectProvider op;
        if (!recyclableOps.isEmpty())
        {
            Set<ObjectProvider> ops = recyclableOps.keySet();
            Iterator<ObjectProvider> opIter = ops.iterator();
            while (opIter.hasNext())
            {
                op = opIter.next();
                if ((now - recyclableOps.get(op)) > expirationTime)
                {
                    // object has expired
                    recyclableOps.remove(op);
                    expire(op);
                    op = null;
                }
                else
                {
                    if (validate(op))
                    {
                        recyclableOps.remove(op);
                        op.connect(ec, cmd);
                        return op;
                    }

                    // object failed validation
                    recyclableOps.remove(op);
                    expire(op);
                    op = null;
                }
            }
        }

        // no objects available, create a new one
        op = create(ec, cmd);
        return op;
    }

    public synchronized void cleanUp()
    {
       ObjectProvider op;
       long now = System.currentTimeMillis();    
       Set<ObjectProvider> ops = recyclableOps.keySet();
       Iterator<ObjectProvider> opIter = ops.iterator();
       while (opIter.hasNext())
       {
           op = opIter.next();
           if ((now - (recyclableOps.get(op)).longValue()) > expirationTime)
           {
               recyclableOps.remove(op);
               expire(op);
               op = null;
           }
       }
       System.gc();
    }

    public synchronized void checkIn(ObjectProvider op)
    {
        if (recyclableOps.size() < maxIdle)
        {
            recyclableOps.put(op, System.currentTimeMillis());
        }
    }

    class CleanUpThread extends Thread
    {
        private ObjectProviderPool pool;
        private long sleepTime;

        CleanUpThread(ObjectProviderPool pool, long sleepTime)
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