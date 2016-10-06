/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.NullifyRelationFieldManager;
import org.datanucleus.store.fieldmanager.ReachabilityFieldManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Handler to process "persistence-by-reachability" at commit.
 * This is a feature of the JDO spec that is enabled by default for that API.
 * It runs a cursory check for objects that have been pulled in to be persisted by "persistence-by-reachability" (cascading) but that are no longer needing to be persisted
 * maybe due to the cascading origin object being deleted.
 */
public class ReachabilityAtCommitHandler
{
    private ExecutionContext ec;

    /** Flag for whether we are running "persistence-by-reachability" at commit execute() at this point in time. */
    private boolean executing = false;

    /** Reachability : Set of ids of objects persisted using persistObject, or known as already persistent in the current txn. */
    private Set persistedIds = null;

    /** Reachability : Set of ids of objects deleted using deleteObject. */
    private Set deletedIds = null;

    /** Reachability : Set of ids of objects newly persistent in the current transaction */
    private Set flushedNewIds = null;

    /** Reachability : Set of ids for all objects enlisted in this transaction. */
    private Set enlistedIds = null;

    /**
     * Constructor for a reachability-at-commit handler.
     * @param ec ExecutionContext that it is for
     */
    public ReachabilityAtCommitHandler(ExecutionContext ec)
    {
        this.ec = ec;
        this.persistedIds = ec.getMultithreaded() ? ConcurrentHashMap.newKeySet() : new HashSet();
        this.deletedIds = ec.getMultithreaded() ? ConcurrentHashMap.newKeySet() : new HashSet();
        this.flushedNewIds = ec.getMultithreaded() ? ConcurrentHashMap.newKeySet() : new HashSet();
        this.enlistedIds = ec.getMultithreaded() ? ConcurrentHashMap.newKeySet() : new HashSet();
    }

    /**
     * Method to clear the stored ids of objects involved in the reachability process.
     */
    public void clear()
    {
        persistedIds.clear();
        deletedIds.clear();
        flushedNewIds.clear();
        enlistedIds.clear();
    }

    public boolean isExecuting()
    {
        return executing;
    }

    public void addEnlistedObject(Object id)
    {
        enlistedIds.add(id);
    }
    public boolean isObjectEnlisted(Object id)
    {
        return enlistedIds.contains(id);
    }

    public void addPersistedObject(Object id)
    {
        persistedIds.add(id);
    }
    public boolean isObjectPersisted(Object id)
    {
        return persistedIds.contains(id);
    }

    public void addDeletedObject(Object id)
    {
        deletedIds.add(id);
    }
    public boolean isObjectDeleted(Object id)
    {
        return deletedIds.contains(id);
    }

    public void addFlushedNewObject(Object id)
    {
        flushedNewIds.add(id);
    }
    public boolean isObjectFlushedNew(Object id)
    {
        return flushedNewIds.contains(id);
    }

    /**
     * Method that will allow swapping of an "id", for example when an object has recently been assigned its true "id".
     * @param oldID The old id that it is registered with. If this is null then we do nothing
     * @param newID The new id to use in place
     */
    public void swapObjectId(Object oldID, Object newID)
    {
        if (oldID != null)
        {
            if (enlistedIds.remove(oldID))
            {
                enlistedIds.add(newID);
            }
            if (flushedNewIds.remove(oldID))
            {
                flushedNewIds.add(newID);
            }
            if (persistedIds.remove(oldID))
            {
                persistedIds.add(newID);
            }
            if (deletedIds.remove(oldID))
            {
                deletedIds.add(newID);
            }
        }
    }

    /**
     * Method to perform the "persistence-by-reachability" at commit.
     */
    public void execute()
    {
        try
        {
            this.executing = true;
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("010032"));
            }

            // If we have some new objects in this transaction, and we have some known persisted objects (either
            // from makePersistent in this txn, or enlisted existing objects) then run reachability checks
            if (!persistedIds.isEmpty() && !flushedNewIds.isEmpty())
            {
                Set currentReachables = new HashSet();

                // Run "reachability" on all known persistent objects for this txn
                Object ids[] = persistedIds.toArray();
                Set objectNotFound = new HashSet();
                for (int i=0; i<ids.length; i++)
                {
                    if (!deletedIds.contains(ids[i]))
                    {
                        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                        {
                            NucleusLogger.PERSISTENCE.debug("Performing reachability algorithm on object with id \""+ids[i]+"\"");
                        }
                        try
                        {
                            ObjectProvider op = ec.findObjectProvider(ec.findObject(ids[i], true, true, null));

                            if (!op.isDeleted() && !currentReachables.contains(ids[i]))
                            {
                                // Make sure all of its relation fields are loaded before continuing. Is this necessary, since its enlisted?
                                op.loadUnloadedRelationFields();

                                // Add this object id since not yet reached
                                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                                {
                                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("007000", StringUtils.toJVMIDString(op.getObject()), ids[i], op.getLifecycleState()));
                                }
                                currentReachables.add(ids[i]);

                                // Go through all relation fields using ReachabilityFieldManager
                                ReachabilityFieldManager pcFM = new ReachabilityFieldManager(op, currentReachables);
                                int[] relationFieldNums = op.getClassMetaData().getRelationMemberPositions(ec.getClassLoaderResolver());
                                if (relationFieldNums != null && relationFieldNums.length > 0)
                                {
                                    op.provideFields(relationFieldNums, pcFM);
                                }
                            }
                        }
                        catch (NucleusObjectNotFoundException ex)
                        {
                            objectNotFound.add(ids[i]);
                        }
                    }
                    else
                    {
                        // Was deleted earlier so ignore
                    }
                }

                // Remove any of the "reachable" instances that are no longer "reachable"
                flushedNewIds.removeAll(currentReachables);

                Object nonReachableIds[] = flushedNewIds.toArray();
                if (nonReachableIds != null && nonReachableIds.length > 0)
                {
                    // For all of instances no longer reachable we need to delete them from the datastore
                    // A). Nullify all of their fields.
                    // TODO See CORE-3276 for a possible change to this
                    for (int i=0; i<nonReachableIds.length; i++)
                    {
                        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                        {
                            NucleusLogger.PERSISTENCE.debug(Localiser.msg("010033", nonReachableIds[i]));
                        }
                        try
                        {
                            if (!objectNotFound.contains(nonReachableIds[i]))
                            {
                                ObjectProvider op = ec.findObjectProvider(ec.findObject(nonReachableIds[i], true, true, null));

                                if (!op.getLifecycleState().isDeleted() && !ec.getApiAdapter().isDetached(op.getObject()))
                                {
                                    // Null any relationships for relation fields of this object
                                    op.replaceFields(op.getClassMetaData().getNonPKMemberPositions(), new NullifyRelationFieldManager(op));
                                    ec.flush();
                                }
                            }
                        }
                        catch (NucleusObjectNotFoundException ex)
                        {
                            // just ignore if the object does not exist anymore  
                        }
                    }

                    // B). Remove the objects
                    for (int i=0; i<nonReachableIds.length; i++)
                    {
                        try
                        {
                            if (!objectNotFound.contains(nonReachableIds[i]))
                            {
                                ObjectProvider op = ec.findObjectProvider(ec.findObject(nonReachableIds[i], true, true, null));
                                op.deletePersistent();
                            }
                        }
                        catch (NucleusObjectNotFoundException ex)
                        {
                            //just ignore if the file does not exist anymore  
                        }
                    }
                }

                // Make sure any updates are flushed
                ec.flushInternal(true);
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("010034"));
            }
        }
        finally
        {
            this.executing = false;
        }
    }
}
