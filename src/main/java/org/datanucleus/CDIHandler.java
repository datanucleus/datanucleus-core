/**********************************************************************
Copyright (c) 2017 Andy Jefferson and others. All rights reserved. 
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * Handles the integration of "javax.enterprise.inject" CDI API.
 * Note that this is the only class referring to CDI classes so that it is usable in environments without CDI present.
 */
public class CDIHandler
{
    BeanManager beanManager;

    Set<CreationalContext> creationalContexts = new HashSet<>();

    /** Cache of InjectionTarget keyed by the object that they created. */
    Map<Object, InjectionTarget> injectionTargets = new HashMap<>();

    public CDIHandler(Object beanMgr)
    {
        this.beanManager = (BeanManager) beanMgr;

    }

    /**
     * Method to instantiate an object of the specified type with injected dependencies.
     * @param cls The type to instantiate
     * @return The instance
     * @param <T> Type of the object
     */
    public <T> T createObjectWithInjectedDependencies(Class<T> cls)
    {
        AnnotatedType<T> type = beanManager.createAnnotatedType(cls);

        // Create the CreationalContext, and cache them for later closure
        CreationalContext<T> creationalCtx = beanManager.createCreationalContext(null);
        creationalContexts.add(creationalCtx);

        // Create an InjectionTarget, and the bean object with injected dependencies, and cache the InjectionTarget per object so we can dispose of them at closure
        InjectionTarget<T> injectionTarget = beanManager.createInjectionTarget(type);
        T obj = injectionTarget.produce(creationalCtx);
        injectionTarget.inject(obj, creationalCtx);
        injectionTarget.postConstruct(obj);
        injectionTargets.put(obj, injectionTarget);

        return obj;
    }

    public void close()
    {
        if (!injectionTargets.isEmpty())
        {
            Set keys = new HashSet<>();
            synchronized(injectionTargets)
            {
                keys.addAll(injectionTargets.keySet());
                for (Object key : keys)
                {
                    try
                    {
                        InjectionTarget target = injectionTargets.get(key);
                        target.preDestroy(key);
                        target.dispose(key);
                        injectionTargets.remove(key);
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
        }
        if (!creationalContexts.isEmpty())
        {
            Iterator<CreationalContext> ctxIter = creationalContexts.iterator();
            while (ctxIter.hasNext())
            {
                CreationalContext ctx = ctxIter.next();
                ctx.release();
                ctxIter.remove();
            }
        }
    }
}