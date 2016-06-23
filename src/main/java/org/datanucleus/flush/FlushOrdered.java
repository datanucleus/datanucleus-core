/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.flush.FlushProcess;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Flush process that processes the objects in the order that they became dirty.
 * If a datastore uses referential integrity this is typically the best way of maintaining a valid update process.
 */
public class FlushOrdered implements FlushProcess
{
    /* (non-Javadoc)
     * @see org.datanucleus.FlushProcess#execute(org.datanucleus.ExecutionContext, java.util.List, java.util.List, org.datanucleus.flush.OperationQueue)
     */
    public List<NucleusOptimisticException> execute(ExecutionContext ec, List<ObjectProvider> primaryOPs, List<ObjectProvider> secondaryOPs, OperationQueue opQueue)
    {
        // Note that opQueue is not processed directly here, but instead will be processed via callbacks from the persistence of other objects
        // TODO The opQueue needs to be processed from here instead of via the callbacks, see NUCCORE-904

        List<NucleusOptimisticException> optimisticFailures = null;

        // Make copy of ObjectProviders so we don't have ConcurrentModification issues
        Object[] toFlushPrimary = null;
        Object[] toFlushSecondary = null;
        try
        {
            if (ec.getMultithreaded()) // Why lock here? should be on overall flush
            {
                ec.getLock().lock();
            }

            if (primaryOPs != null)
            {
                toFlushPrimary = primaryOPs.toArray();
                primaryOPs.clear();
            }
            if (secondaryOPs != null)
            {
                toFlushSecondary = secondaryOPs.toArray();
                secondaryOPs.clear();
            }
        }
        finally
        {
            if (ec.getMultithreaded())
            {
                ec.getLock().unlock();
            }
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            int total = 0;
            if (toFlushPrimary != null)
            {
                total += toFlushPrimary.length;
            }
            if (toFlushSecondary != null)
            {
                total += toFlushSecondary.length;
            }
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("010003", total));
        }

        Set<Class> classesToFlush = null;
        if (ec.getNucleusContext().getStoreManager().getQueryManager().getQueryResultsCache() != null)
        {
            classesToFlush = new HashSet();
        }

        // a). primary dirty objects
        if (toFlushPrimary != null)
        {
            for (int i = 0; i < toFlushPrimary.length; i++)
            {
                ObjectProvider op = (ObjectProvider) toFlushPrimary[i];
                try
                {
                    op.flush();
                    if (classesToFlush != null && op.getObject() != null)
                    {
                        classesToFlush.add(op.getObject().getClass());
                    }
                }
                catch (NucleusOptimisticException oe)
                {
                    if (optimisticFailures == null)
                    {
                        optimisticFailures = new ArrayList();
                    }
                    optimisticFailures.add(oe);
                }
            }
        }

        // b). secondary dirty objects
        if (toFlushSecondary != null)
        {
            for (int i = 0; i < toFlushSecondary.length; i++)
            {
                ObjectProvider op = (ObjectProvider) toFlushSecondary[i];
                try
                {
                    op.flush();
                    if (classesToFlush != null && op.getObject() != null)
                    {
                        classesToFlush.add(op.getObject().getClass());
                    }
                }
                catch (NucleusOptimisticException oe)
                {
                    if (optimisticFailures == null)
                    {
                        optimisticFailures = new ArrayList();
                    }
                    optimisticFailures.add(oe);
                }
            }
        }

        if (opQueue != null)
        {
            if (!ec.getStoreManager().usesBackedSCOWrappers())
            {
                // This ExecutionContext is not using backing store SCO wrappers, so process SCO Operations for cascade delete etc.
                opQueue.processOperationsForNoBackingStoreSCOs(ec);
            }
            opQueue.clearPersistDeleteUpdateOperations();
        }

        if (classesToFlush != null)
        {
            // Flush any query results from cache for these types
            Iterator<Class> queryClsIter = classesToFlush.iterator();
            while (queryClsIter.hasNext())
            {
                Class cls = queryClsIter.next();
                ec.getNucleusContext().getStoreManager().getQueryManager().evictQueryResultsForType(cls);
            }
        }

        return optimisticFailures;
    }
}