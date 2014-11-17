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
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.StorePersistenceHandler;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Flush method for cases where the datastore doesn't use referential integrity so we can send batches
 * of deletes, then batches of inserts, then any updates to optimise the persistence.
 * This also makes use of the OperationQueue to do more intelligent handling of cascade delete when elements are removed
 * from Collections, checking if it is later added to a different collection.
 */
public class FlushNonReferential implements FlushProcess
{
    /* (non-Javadoc)
     * @see org.datanucleus.FlushProcess#execute(org.datanucleus.ExecutionContext, java.util.List, java.util.List, org.datanucleus.flush.OperationQueue)
     */
    public List<NucleusOptimisticException> execute(ExecutionContext ec, List<ObjectProvider> primaryOPs, List<ObjectProvider> secondaryOPs, OperationQueue opQueue)
    {
        // Make copy of ObjectProviders so we don't have ConcurrentModification issues
        Set<ObjectProvider> opsToFlush = new HashSet<ObjectProvider>();
        if (primaryOPs != null)
        {
            opsToFlush.addAll(primaryOPs);
            primaryOPs.clear();
        }
        if (secondaryOPs != null)
        {
            opsToFlush.addAll(secondaryOPs);
            secondaryOPs.clear();
        }

        // Process all delete, insert and update of objects
        List<NucleusOptimisticException> excptns = flushDeleteInsertUpdateGrouped(opsToFlush, ec);

        if (opQueue != null && !ec.getStoreManager().usesBackedSCOWrappers())
        {
            opQueue.processOperationsForNoBackingStoreSCOs(ec);
        }

        return excptns;
    }

    /**
     * Method that does the flushing of the passed ObjectProviders, grouping them into all DELETEs, then all INSERTs,
     * finally all UPDATEs. The StorePersistenceHandler will get calls to <i>deleteObjects</i>, <i>insertObjects</i>
     * and <i>updateObject</i> (for each other one). Note that this is in a separate method to allow calls by
     * other FlushProcesses that want to take advantage of the basic flush method without 
     * @param opsToFlush The ObjectProviders to process
     * @param ec ExecutionContext
     * @return Any optimistic verification exceptions thrown during flush
     */
    public List<NucleusOptimisticException> flushDeleteInsertUpdateGrouped(Set<ObjectProvider> opsToFlush, ExecutionContext ec)
    {
        List<NucleusOptimisticException> optimisticFailures = null;

        Set<Class> classesToFlush = null;
        if (ec.getNucleusContext().getStoreManager().getQueryManager().getQueryResultsCache() != null)
        {
            classesToFlush = new HashSet();
        }

        Set<ObjectProvider> opsToDelete = new HashSet<ObjectProvider>();
        Set<ObjectProvider> opsToInsert = new HashSet<ObjectProvider>();
        Iterator<ObjectProvider> opIter = opsToFlush.iterator();
        while (opIter.hasNext())
        {
            ObjectProvider op = opIter.next();
            if (op.isEmbedded())
            {
                op.markAsFlushed(); // Embedded have nothing to flush since the owner manages it
                opIter.remove();
            }
            else
            {
                if (classesToFlush != null)
                {
                    classesToFlush.add(op.getObject().getClass());
                }
                if (op.getLifecycleState().isNew() && !op.isFlushedToDatastore() && !op.isFlushedNew())
                {
                    // P_NEW and not yet flushed to datastore
                    opsToInsert.add(op);
                    opIter.remove();
                }
                else if (op.getLifecycleState().isDeleted() && !op.isFlushedToDatastore())
                {
                    if (!op.getLifecycleState().isNew())
                    {
                        // P_DELETED
                        opsToDelete.add(op);
                        opIter.remove();
                    }
                    else if (op.getLifecycleState().isNew() && op.isFlushedNew())
                    {
                        // P_NEW_DELETED already persisted
                        opsToDelete.add(op);
                        opIter.remove();
                    }
                }
            }
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("010046", opsToDelete.size(), opsToInsert.size(), opsToFlush.size()));
        }

        StorePersistenceHandler persistenceHandler = ec.getStoreManager().getPersistenceHandler();
        if (!opsToDelete.isEmpty())
        {
            // Perform preDelete - deleteAll - postDelete, and mark all ObjectProviders as flushed
            // TODO This omits some parts of sm.internalDeletePersistent
            for (ObjectProvider op : opsToDelete)
            {
                op.setFlushing(true);
                ec.getCallbackHandler().preDelete(op.getObject());
            }
            try
            {
                persistenceHandler.deleteObjects(opsToDelete.toArray(new ObjectProvider[opsToDelete.size()]));
            }
            catch (NucleusOptimisticException noe)
            {
                optimisticFailures = new ArrayList();
                Throwable[] nestedExcs = noe.getNestedExceptions();
                if (nestedExcs != null && nestedExcs.length > 1)
                {
                    NucleusOptimisticException[] noes = (NucleusOptimisticException[])nestedExcs;
                    for (int i=0;i<nestedExcs.length;i++)
                    {
                        optimisticFailures.add(noes[i]);
                    }
                }
                else
                {
                    optimisticFailures.add(noe);
                }
            }
            for (ObjectProvider op : opsToDelete)
            {
                ec.getCallbackHandler().postDelete(op.getObject());
                op.setFlushedNew(false);
                op.markAsFlushed();
                op.setFlushing(false);
            }
        }

        if (!opsToInsert.isEmpty())
        {
            // Perform preStore - insertAll - postStore, and mark all ObjectProviders as flushed
            // TODO This omits some parts of sm.internalMakePersistent
            for (ObjectProvider op : opsToInsert)
            {
                op.setFlushing(true);
                ec.getCallbackHandler().preStore(op.getObject());
                // TODO Make sure identity is set since user could have updated fields in preStore
            }
            persistenceHandler.insertObjects(opsToInsert.toArray(new ObjectProvider[opsToInsert.size()]));
            for (ObjectProvider op : opsToInsert)
            {
                ec.getCallbackHandler().postStore(op.getObject());
                op.setFlushedNew(true);
                op.markAsFlushed();
                op.setFlushing(false);
                ec.putObjectIntoLevel1Cache(op); // Update the object in the cache(s) now that version/id are set
            }
        }

        if (!opsToFlush.isEmpty())
        {
            // Objects to update
            for (ObjectProvider op : opsToFlush)
            {
                try
                {
                    op.flush();
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