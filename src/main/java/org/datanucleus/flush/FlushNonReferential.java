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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.state.DNStateManager;
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
     * @see org.datanucleus.FlushProcess#execute(org.datanucleus.ExecutionContext, java.util.Collection, java.util.Collection, org.datanucleus.flush.OperationQueue)
     */
    public List<NucleusOptimisticException> execute(ExecutionContext ec, Collection<DNStateManager> primarySMs, Collection<DNStateManager> secondarySMs, OperationQueue opQueue)
    {
        // Make copy of StateManagers so we don't have ConcurrentModification issues
        Set<DNStateManager> smsToFlush = new HashSet<>();
        if (primarySMs != null)
        {
            smsToFlush.addAll(primarySMs);
            primarySMs.clear();
        }
        if (secondarySMs != null)
        {
            smsToFlush.addAll(secondarySMs);
            secondarySMs.clear();
        }

        // Process all delete, insert and update of objects
        List<NucleusOptimisticException> excptns = flushDeleteInsertUpdateGrouped(smsToFlush, ec);

        if (opQueue != null)
        {
            if (!ec.getStoreManager().usesBackedSCOWrappers())
            {
                // This ExecutionContext is not using backing store SCO wrappers, so process SCO Operations for cascade delete etc.
                opQueue.processOperationsForNoBackingStoreSCOs(ec);
            }
            opQueue.clearPersistDeleteUpdateOperations();
        }

        return excptns;
    }

    /**
     * Method that does the flushing of the passed StateManagers, grouping them into all DELETEs, then all INSERTs,
     * finally all UPDATEs. The StorePersistenceHandler will get calls to <i>deleteObjects</i>, <i>insertObjects</i>
     * and <i>updateObject</i> (for each other one). Note that this is in a separate method to allow calls by
     * other FlushProcesses that want to take advantage of the basic flush method without 
     * @param smsToFlush StateManagers to process
     * @param ec ExecutionContext
     * @return Any optimistic verification exceptions thrown during flush
     */
    public List<NucleusOptimisticException> flushDeleteInsertUpdateGrouped(Set<DNStateManager> smsToFlush, ExecutionContext ec)
    {
        List<NucleusOptimisticException> optimisticFailures = null;

        Set<Class> classesToFlush = null;
        if (ec.getNucleusContext().getStoreManager().getQueryManager().getQueryResultsCache() != null)
        {
            classesToFlush = new HashSet<>();
        }

        Set<DNStateManager> smsToDelete = new HashSet<>();
        Set<DNStateManager> smsToInsert = new HashSet<>();
        Iterator<DNStateManager> smIter = smsToFlush.iterator();
        while (smIter.hasNext())
        {
            DNStateManager sm = smIter.next();
            if (sm.isEmbedded())
            {
                sm.markAsFlushed(); // Embedded have nothing to flush since the owner manages it
                smIter.remove();
            }
            else
            {
                if (classesToFlush != null && sm.getObject() != null)
                {
                    classesToFlush.add(sm.getObject().getClass());
                }
                if (sm.getLifecycleState().isNew() && !sm.isFlushedToDatastore() && !sm.isFlushedNew())
                {
                    // P_NEW and not yet flushed to datastore
                    smsToInsert.add(sm);
                    smIter.remove();
                }
                else if (sm.getLifecycleState().isDeleted() && !sm.isFlushedToDatastore())
                {
                    if (!sm.getLifecycleState().isNew())
                    {
                        // P_DELETED
                        smsToDelete.add(sm);
                        smIter.remove();
                    }
                    else if (sm.getLifecycleState().isNew() && sm.isFlushedNew())
                    {
                        // P_NEW_DELETED already persisted
                        smsToDelete.add(sm);
                        smIter.remove();
                    }
                }
            }
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("010046", smsToDelete.size(), smsToInsert.size(), smsToFlush.size()));
        }

        StorePersistenceHandler persistenceHandler = ec.getStoreManager().getPersistenceHandler();
        if (!smsToDelete.isEmpty())
        {
            // Perform preDelete - deleteAll - postDelete, and mark all StateManagers as flushed
            // TODO This omits some parts of sm.internalDeletePersistent
            for (DNStateManager sm : smsToDelete)
            {
                sm.setFlushing(true);
                ec.getCallbackHandler().preDelete(sm.getObject());
            }
            try
            {
                persistenceHandler.deleteObjects(smsToDelete.toArray(new DNStateManager[smsToDelete.size()]));
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
            for (DNStateManager sm : smsToDelete)
            {
                ec.getCallbackHandler().postDelete(sm.getObject());
                sm.setFlushedNew(false);
                sm.markAsFlushed();
                sm.setFlushing(false);
            }
        }

        if (!smsToInsert.isEmpty())
        {
            // Perform preStore - insertAll - postStore, and mark all StateManagers as flushed
            // TODO This omits some parts of sm.internalMakePersistent
            for (DNStateManager sm : smsToInsert)
            {
                sm.setFlushing(true);
                ec.getCallbackHandler().preStore(sm.getObject());
                // TODO Make sure identity is set since user could have updated fields in preStore
            }
            persistenceHandler.insertObjects(smsToInsert.toArray(new DNStateManager[smsToInsert.size()]));
            for (DNStateManager sm : smsToInsert)
            {
                ec.getCallbackHandler().postStore(sm.getObject());
                sm.setFlushedNew(true);
                sm.markAsFlushed();
                sm.setFlushing(false);
                ec.putObjectIntoLevel1Cache(sm); // Update the object in the cache(s) now that version/id are set
            }
        }

        if (!smsToFlush.isEmpty())
        {
            // Objects to update
            for (DNStateManager sm : smsToFlush)
            {
                try
                {
                    sm.flush();
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
            for (Class cls : classesToFlush)
            {
                ec.getNucleusContext().getStoreManager().getQueryManager().evictQueryResultsForType(cls);
            }
        }
        
        return optimisticFailures;
    }
}