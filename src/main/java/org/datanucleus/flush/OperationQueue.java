/**********************************************************************
Copyright (c) 2010 Peter Dettman and others. All rights reserved.
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
2013 Andy Jefferson - converted to general purpose queue for all operations
    ...
**********************************************************************/
package org.datanucleus.flush;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.scostore.CollectionStore;
import org.datanucleus.store.types.scostore.ListStore;
import org.datanucleus.store.types.scostore.MapStore;
import org.datanucleus.store.types.scostore.Store;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Queue of operations to be performed when operating in MANUAL FlushMode.
 * This queue will typically contain operations on second class objects, such as Collections or Maps.
 * They are queued in the order they are invoked by the user. 
 * The queue will contain all operations to be performed for an ExecutionContext. There are two methods for
 * processing them, one processing all (for use in the future when we support that mode in full), and one
 * that processes all for a particular SCO (backing store).
 */
public class OperationQueue
{
    protected List<Operation> queuedOperations = new ArrayList<>();

    /**
     * Method to add the specified operation to the operation queue.
     * @param oper Operation
     */
    public void enqueue(Operation oper)
    {
        DNStateManager sm = oper.getStateManager();
        if (oper instanceof SCOOperation)
        {
            if (sm.isWaitingToBeFlushedToDatastore())
            {
                // Don't enlist since not yet flushed
                NucleusLogger.PERSISTENCE.info(">> OperationQueue : not adding operation since owner not yet flushed - " + oper);
                return;
            }
        }
        queuedOperations.add(oper);
    }

    /**
     * Convenience method to log the current operation queue.
     */
    public void log()
    {
        NucleusLogger.PERSISTENCE.debug(">> OperationQueue :" + (queuedOperations.isEmpty() ? " Empty" : ("" + queuedOperations.size() + " operations")));
        for (Operation oper : queuedOperations)
        {
            NucleusLogger.PERSISTENCE.debug(">> " + oper);
        }
    }

    public void clear()
    {
        queuedOperations.clear();
    }

    /**
     * Method to provide access to inspect the queued operations. The returned list is unmodifiable.
     * @return The queued operations
     */
    public List<Operation> getOperations()
    {
        return Collections.unmodifiableList(queuedOperations);
    }

    public void removeOperations(List<Operation> removedOps)
    {
        queuedOperations.removeAll(removedOps);
    }

    /**
     * Method to perform all operations queued.
     * Those operations are then removed from the queue.
     */
    public void performAll()
    {
        for (Operation op : queuedOperations)
        {
            op.perform();
        }
        queuedOperations.clear();
    }

    /**
     * Method to perform all operations queued for the specified StateManager and backing store.
     * Those operations are then removed from the queue.
     * @param store The backing store
     * @param sm StateManager
     */
    public void performAll(Store store, DNStateManager sm)
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("023005", sm.getObjectAsPrintable(), store.getOwnerMemberMetaData().getFullFieldName()));
        }

        // Extract those operations for the specified backing store
        List<Operation> flushOperations = new ArrayList<Operation>();
        ListIterator<Operation> operIter = queuedOperations.listIterator();
        while (operIter.hasNext())
        {
            Operation oper = operIter.next();
            if (oper.getStateManager() == sm && oper instanceof SCOOperation && ((SCOOperation)oper).getStore() == store)
            {
                // Process operations for this Store and this StateManager
                flushOperations.add(oper);
                operIter.remove();
            }
        }

        // TODO Cater for Lists where cascade delete is enabled but we want to only allow cascade delete if the element isn't later added at a different place in the list.
        ListIterator<Operation> flushOperIter = flushOperations.listIterator();
        while (flushOperIter.hasNext())
        {
            Operation oper = flushOperIter.next();
            if (store instanceof CollectionStore)
            {
                if (!(store instanceof ListStore))
                {
                    if (isAddFollowedByRemoveOnSameSCO(store, sm, oper, flushOperIter))
                    {
                        // add+remove of the same element - ignore this and the next one
                        flushOperIter.next();
                    }
                    else if (isRemoveFollowedByAddOnSameSCO(store, sm, oper, flushOperIter))
                    {
                        // remove+add of the same element - ignore this and the next one
                        flushOperIter.next();
                    }
                    else
                    {
                        oper.perform();
                    }
                }
                else
                {
                    oper.perform();
                }
            }
            else if (store instanceof MapStore)
            {
                if (isPutFollowedByRemoveOnSameSCO(store, sm, oper, flushOperIter))
                {
                    // put+remove of the same key - ignore this and the next one
                    flushOperIter.next();
                }
                else
                {
                    oper.perform();
                }
            }
            else
            {
                oper.perform();
            }
        }
    }

    public void clearPersistDeleteUpdateOperations()
    {
        if (queuedOperations != null)
        {
            Iterator<Operation> opsIter = queuedOperations.iterator();
            while (opsIter.hasNext())
            {
                Operation op = opsIter.next();
                if (op instanceof PersistOperation || op instanceof DeleteOperation || op instanceof UpdateMemberOperation)
                {
                    opsIter.remove();
                }
            }
        }
    }

    /**
     * Method to process all SCO operations where the SCOs don't use a backing store.
     * This will check for add+remove/remove+add and perform cascade delete as appropriate.
     * It will then remove the SCO operations from the queue. It doesn't actually process the queued operations since when you have
     * no backing store there is nothing to do.
     * @param ec ExecutionContext
     */
    public void processOperationsForNoBackingStoreSCOs(ExecutionContext ec)
    {
        if (queuedOperations != null && !ec.getStoreManager().usesBackedSCOWrappers())
        {
            // Make use of OperationQueue for any cascade deletes that may be needed as a result of removal from collections/maps
            List<Operation> opersToIgnore = new ArrayList();
            List<Object> objectsToCascadeDelete = null;
            Iterator<Operation> opersIter = queuedOperations.iterator();
            while (opersIter.hasNext())
            {
                Operation oper = opersIter.next();
                if (oper instanceof CollectionRemoveOperation)
                {
                    // TODO Improve this test to catch things like remove from one collection, add to another and delete from the second one.
                    CollectionRemoveOperation collRemoveOper = (CollectionRemoveOperation)oper;
                    if (collRemoveOper.getStore() == null && SCOUtils.hasDependentElement(collRemoveOper.getMemberMetaData()))
                    {
                        // Check for addition of the same element, hence avoiding cascade-delete
                        boolean needsRemoving = true;
                        if (queuedOperations.size() > 1)
                        {
                            // Check for later addition that negates the cascade delete
                            Iterator<Operation> subOpersIter = queuedOperations.iterator();
                            while (subOpersIter.hasNext())
                            {
                                Operation subOper = subOpersIter.next();
                                if (subOper instanceof CollectionAddOperation)
                                {
                                    CollectionAddOperation collAddOper = (CollectionAddOperation)subOper;
                                    if (collRemoveOper.getValue().equals(collAddOper.getValue()))
                                    {
                                        needsRemoving = false;
                                        break;
                                    }
                                }
                                else if (subOper instanceof PersistOperation)
                                {
                                    // Check whether this is the persist of an object the same type as the owner of the removed element (i.e assigned to new owner)
                                    PersistOperation persOp = (PersistOperation)subOper;
                                    if (persOp.getStateManager().getObject().getClass().equals(collRemoveOper.getStateManager().getObject().getClass()))
                                    {
                                        Collection persColl = (Collection) persOp.getStateManager().provideField(collRemoveOper.getMemberMetaData().getAbsoluteFieldNumber());
                                        if (persColl != null && persColl.contains(collRemoveOper.getValue()))
                                        {
                                            needsRemoving = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (needsRemoving)
                        {
                            if (objectsToCascadeDelete == null)
                            {
                                objectsToCascadeDelete = new ArrayList();
                            }
                            NucleusLogger.PERSISTENCE.info(">> Flush collection element needs cascade delete " + collRemoveOper.getValue());
                            objectsToCascadeDelete.add(collRemoveOper.getValue());
                        }
                    }
                }
                else if (oper instanceof MapRemoveOperation)
                {
                    // TODO Improve this test to catch things like remove from one map, add to another and delete from the second one.
                    MapRemoveOperation mapRemoveOper = (MapRemoveOperation)oper;
                    if (mapRemoveOper.getStore() == null)
                    {
                        if (SCOUtils.hasDependentKey(mapRemoveOper.getMemberMetaData()))
                        {
                            // Check for later addition of the same key, hence avoiding cascade-delete
                            boolean keyNeedsRemoving = true;
                            if (queuedOperations.size() > 1)
                            {
                                // Check for later addition that negates the cascade delete
                                Iterator<Operation> subOpersIter = queuedOperations.iterator();
                                while (subOpersIter.hasNext())
                                {
                                    Operation subOper = subOpersIter.next();
                                    if (subOper instanceof MapPutOperation)
                                    {
                                        MapPutOperation mapPutOper = (MapPutOperation)subOper;
                                        if (mapRemoveOper.getKey().equals(mapPutOper.getKey()))
                                        {
                                            keyNeedsRemoving = false;
                                            break;
                                        }
                                    }
                                    else if (subOper instanceof PersistOperation)
                                    {
                                        // Check whether this is the persist of an object the same type as the owner of the removed key (i.e assigned to new owner)
                                        PersistOperation persOper = (PersistOperation)subOper;
                                        if (persOper.getStateManager().getObject().getClass().equals(mapRemoveOper.getStateManager().getObject().getClass()))
                                        {
                                            Map persMap = (Map) persOper.getStateManager().provideField(mapRemoveOper.getMemberMetaData().getAbsoluteFieldNumber());
                                            if (persMap != null && persMap.containsKey(mapRemoveOper.getKey()))
                                            {
                                                keyNeedsRemoving = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (keyNeedsRemoving)
                            {
                                if (objectsToCascadeDelete == null)
                                {
                                    objectsToCascadeDelete = new ArrayList();
                                }
                                objectsToCascadeDelete.add(mapRemoveOper.getKey());
                            }
                        }
                        else if (SCOUtils.hasDependentValue(mapRemoveOper.getMemberMetaData()))
                        {
                            // Check for later addition of the same value, hence avoiding cascade-delete
                            boolean valNeedsRemoving = true;
                            if (queuedOperations.size() > 1)
                            {
                                // Check for later addition that negates the cascade delete
                                Iterator<Operation> subOpersIter = queuedOperations.iterator();
                                while (subOpersIter.hasNext())
                                {
                                    Operation subOper = subOpersIter.next();
                                    if (subOper instanceof MapPutOperation)
                                    {
                                        MapPutOperation mapPutOper = (MapPutOperation)subOper;
                                        if (mapRemoveOper.getValue().equals(mapPutOper.getValue()))
                                        {
                                            valNeedsRemoving = false;
                                            break;
                                        }
                                    }
                                    else if (subOper instanceof PersistOperation)
                                    {
                                        // Check whether this is the persist of an object the same type as the owner of the removed value (i.e assigned to new owner)
                                        PersistOperation persOper = (PersistOperation)subOper;
                                        if (persOper.getStateManager().getObject().getClass().equals(mapRemoveOper.getStateManager().getObject().getClass()))
                                        {
                                            Map persMap = (Map) persOper.getStateManager().provideField(mapRemoveOper.getMemberMetaData().getAbsoluteFieldNumber());
                                            if (persMap != null && persMap.containsValue(mapRemoveOper.getValue()))
                                            {
                                                valNeedsRemoving = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (valNeedsRemoving)
                            {
                                if (objectsToCascadeDelete == null)
                                {
                                    objectsToCascadeDelete = new ArrayList();
                                }
                                objectsToCascadeDelete.add(mapRemoveOper.getValue());
                            }
                        }
                    }
                }

                if (oper instanceof SCOOperation)
                {
                    opersToIgnore.add(oper);
                }
            }

            // Remove all SCO operations since we don't have backing store SCOs here
            queuedOperations.removeAll(opersToIgnore);

            if (objectsToCascadeDelete != null)
            {
                for (Object deleteObj : objectsToCascadeDelete)
                {
                    NucleusLogger.PERSISTENCE.debug("Initiating cascade delete of " + deleteObj);
                    ec.deleteObjectInternal(deleteObj);
                }
            }
        }
    }

    /**
     * Convenience optimisation checker to return if the current operation is ADD of an element that is
     * immediately REMOVED. Always leaves the iterator at the same position as starting
     * @param store The backing store
     * @param sm The StateManager
     * @param currentOper The current operation
     * @param listIter The iterator of operations
     * @return Whether this is an ADD that has a REMOVE of the same element immediately after
     */
    protected static boolean isAddFollowedByRemoveOnSameSCO(Store store, DNStateManager sm, Operation currentOper, ListIterator<Operation> listIter)
    {
        if (CollectionAddOperation.class.isInstance(currentOper))
        {
            // Optimisation to check that we aren't doing add+remove of the same element consecutively
            boolean addThenRemove = false;
            if (listIter.hasNext())
            {
                // Get next element
                Operation operNext = listIter.next();
                if (CollectionRemoveOperation.class.isInstance(operNext))
                {
                    Object value = CollectionAddOperation.class.cast(currentOper).getValue();
                    if (value == CollectionRemoveOperation.class.cast(operNext).getValue())
                    {
                        addThenRemove = true;
                        NucleusLogger.PERSISTENCE.info("Member " + store.getOwnerMemberMetaData().getFullFieldName() + 
                            " of " + StringUtils.toJVMIDString(sm.getObject()) + " had an add then a remove of element " + 
                            StringUtils.toJVMIDString(value) + " - operations ignored");
                    }
                }

                // Go back
                listIter.previous();
            }
            return addThenRemove;
        }
        return false;
    }

    /**
     * Convenience optimisation checker to return if the current operation is REMOVE of an element that is
     * immediately ADDed. Always leaves the iterator at the same position as starting
     * @param store The backing store
     * @param sm The StateManager
     * @param currentOper The current operation
     * @param listIter The iterator of operations
     * @return Whether this is a REMOVE that has an ADD of the same element immediately after
     */
    protected static boolean isRemoveFollowedByAddOnSameSCO(Store store, DNStateManager sm, Operation currentOper, ListIterator<Operation> listIter)
    {
        if (CollectionRemoveOperation.class.isInstance(currentOper))
        {
            // Optimisation to check that we aren't doing add+remove of the same element consecutively
            boolean removeThenAdd = false;
            if (listIter.hasNext())
            {
                // Get next element
                Operation opNext = listIter.next();
                if (CollectionAddOperation.class.isInstance(opNext))
                {
                    Object value = CollectionRemoveOperation.class.cast(currentOper).getValue();
                    if (value == CollectionAddOperation.class.cast(opNext).getValue())
                    {
                        removeThenAdd = true;
                        NucleusLogger.PERSISTENCE.info("Member" + store.getOwnerMemberMetaData().getFullFieldName() + 
                            " of " + StringUtils.toJVMIDString(sm.getObject()) + " had a remove then add of element " + 
                            StringUtils.toJVMIDString(value) + " - operations ignored");
                    }
                }

                // Go back
                listIter.previous();
            }
            return removeThenAdd;
        }
        return false;
    }

    /**
     * Convenience optimisation checker to return if the current operation is PUT of a key that is
     * immediately REMOVED. Always leaves the iterator at the same position as starting
     * @param store The backing store
     * @param sm The StateManager
     * @param currentOper The current operation
     * @param listIter The iterator of operations
     * @return Whether this is a PUT that has a REMOVE of the same key immediately after
     */
    protected static boolean isPutFollowedByRemoveOnSameSCO(Store store, DNStateManager sm, Operation currentOper, ListIterator<Operation> listIter)
    {
        if (MapPutOperation.class.isInstance(currentOper))
        {
            // Optimisation to check that we aren't doing put+remove of the same key consecutively
            boolean putThenRemove = false;
            if (listIter.hasNext())
            {
                // Get next element
                Operation operNext = listIter.next();
                if (MapRemoveOperation.class.isInstance(operNext))
                {
                    Object key = MapPutOperation.class.cast(currentOper).getKey();
                    if (key == MapRemoveOperation.class.cast(operNext).getKey())
                    {
                        putThenRemove = true;
                        NucleusLogger.PERSISTENCE.info("Member " + store.getOwnerMemberMetaData().getFullFieldName() + 
                            " of " + StringUtils.toJVMIDString(sm.getObject()) + " had a put then a remove of key " + 
                            StringUtils.toJVMIDString(key) + " - operations ignored");
                    }
                }

                // Go back
                listIter.previous();
            }
            return putThenRemove;
        }
        return false;
    }
}