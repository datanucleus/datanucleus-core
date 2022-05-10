/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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

import static org.datanucleus.store.types.SCOUtils.singleCollectionValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.store.types.SCOCollection;
import org.datanucleus.store.types.containers.ElementContainerHandler;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Manager for (bidirectional) relationships of an object (StateManager).
 * Designed as a stand-alone process to run just before flush.
 * Updates on bidirectional relations are registered during the persistence process. 
 * Call to checkConsistency() will check for consistency and throw exceptions as appropriate.
 * Call to process() will perform updates at the other side of the registered relations so all is consistent.
 */
public class RelationshipManagerImpl implements RelationshipManager
{
    /** StateManager for the object we are managing the relationships for. */
    final DNStateManager ownerSM;

    final ExecutionContext ec;

    /** Object being managed. */
    final Object pc;

    /** Map of bidirectional field "changes", keyed by the absolute field number of the owner object. */
    final Map<Integer, List<RelationChange>> fieldChanges;

    /**
     * Constructor.
     * @param sm StateManager for the object that we are managing relations for.
     */
    public RelationshipManagerImpl(DNStateManager sm)
    {
        this.ownerSM = sm;
        this.ec = sm.getExecutionContext();
        this.pc = sm.getObject();
        this.fieldChanges = new HashMap();
    }

    private enum ChangeType
    {
        ADD_OBJECT, // Element added to a collection
        REMOVE_OBJECT, // Element removed from a collection
        CHANGE_OBJECT // Field value set (setXXX)
        // TODO Map put, remove
    }

    /**
     * Definition of a change in a relation.
     */
    private static class RelationChange
    {
        ChangeType type;
        Object value;
        Object oldValue;
        public RelationChange(ChangeType type, Object val)
        {
            this.type = type;
            this.value = val;
        }
        public RelationChange(ChangeType type, Object newVal, Object oldVal)
        {
            this.type = type;
            this.value = newVal;
            this.oldValue = oldVal;
        }
        public String toString()
        {
            if (oldValue != null)
            {
                return "RelationChange type=" + type + " value=" + StringUtils.toJVMIDString(oldValue) + " -> " + StringUtils.toJVMIDString(value);
            }
            return "RelationChange type=" + type + " value=" + StringUtils.toJVMIDString(value);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.RelationshipManager#clearFields()
     */
    public void clearFields()
    {
        fieldChanges.clear();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.RelationshipManager#relationChange(int, java.lang.Object, java.lang.Object)
     */
    public void relationChange(int fieldNumber, Object oldValue, Object newValue)
    {
        if (ec.isManagingRelations())
        {
            return;
        }

        Integer fieldKey = Integer.valueOf(fieldNumber);
        List<RelationChange> changes = fieldChanges.get(fieldKey);
        if (changes == null)
        {
            changes = new ArrayList<RelationChange>();
            fieldChanges.put(fieldKey, changes);
        }

        AbstractMemberMetaData mmd = ownerSM.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        RelationType relationType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (relationType == RelationType.ONE_TO_ONE_BI || relationType == RelationType.MANY_TO_ONE_BI)
        {
            // TODO If this field is changed multiple times, we don't currently handle it e.g "myObj.setX(null);" followed by "myObj.setX(newX);"
            if (changes.isEmpty())
            {
                changes.add(new RelationChange(ChangeType.CHANGE_OBJECT, newValue, oldValue));
            }
            return;
        }

        if (relationType == RelationType.ONE_TO_MANY_BI || relationType == RelationType.MANY_TO_MANY_BI)
        {
            // TODO This really ought to be simply stored as RelationChange with oldColl, newColl and then move this logic to process().
            // TODO What about Map?
            if (mmd.hasCollection())
            {
                Collection oldColl = (Collection)oldValue;
                Collection newColl = (Collection)newValue;
                if (oldColl == null)
                {
                    if (newColl != null)
                    {
                        // Add all elements
                        for (Object newElem : newColl)
                        {
                            changes.add(new RelationChange(ChangeType.ADD_OBJECT, newElem));
                        }
                    }
                }
                else
                {
                    if (newColl == null)
                    {
                        AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
                        for (Object element : oldColl)
                        {
                            if (ownerSM.getLifecycleState().isDeleted)
                            {
                                // Deleting the owner, so register the element to reset its owner
                                ec.removeObjectFromLevel2Cache(ec.getApiAdapter().getIdForObject(element));
                                DNStateManager elementSM = ec.findStateManager(element);
                                if (relationType == RelationType.ONE_TO_MANY_BI)
                                {
                                    // TODO This needs marking as a secondary change. i.e we dont want follow on checks, just null out the relation during process()
                                    ec.getRelationshipManager(elementSM).relationChange(relatedMmd.getAbsoluteFieldNumber(), ownerSM.getObject(), null);
                                }
                                else if (relationType == RelationType.MANY_TO_MANY_BI)
                                {
                                    // TODO This needs marking as a secondary change. i.e we don't want follow on checks, just remove the element from the relation during process()
                                    ec.getRelationshipManager(elementSM).relationRemove(relatedMmd.getAbsoluteFieldNumber(), ownerSM.getObject());
                                }
                            }
                            else
                            {
                                // Remove the element
                                changes.add(new RelationChange(ChangeType.REMOVE_OBJECT, element));
                            }
                        }
                    }
                    else
                    {
                        // Remove some and add some
                        for (Object newElem : newColl)
                        {
                            boolean alreadyExists = false;
                            for (Object oldElem : oldColl)
                            {
                                if (newElem == oldElem)
                                {
                                    alreadyExists = true;
                                    break;
                                }
                            }
                            if (!alreadyExists)
                            {
                                DNStateManager elemSM = ec.findStateManager(newElem);
                                if (elemSM != null)
                                {
                                    AbstractMemberMetaData elemMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
                                    Object oldOwner = elemSM.provideField(elemMmd.getAbsoluteFieldNumber());
                                    if (!elemSM.isFieldLoaded(elemMmd.getAbsoluteFieldNumber()))
                                    {
                                        elemSM.loadField(elemMmd.getAbsoluteFieldNumber());
                                    }
                                    if (oldOwner != null)
                                    {
                                        // Remove from old owner collection
                                        DNStateManager oldOwnerSM = ec.findStateManager(oldOwner);
                                        if (oldOwnerSM != null)
                                        {
                                            // TODO This needs marking as a secondary change. i.e we dont want follow on checks, just remove the element from the relation during process()
                                            ec.getRelationshipManager(oldOwnerSM).relationRemove(fieldNumber, newElem);
                                        }
                                    }
                                }
                                relationAdd(fieldNumber, newElem);
                            }
                        }

                        for (Object oldElem : oldColl)
                        {
                            boolean stillExists = false;
                            for (Object newElem : newColl)
                            {
                                if (oldElem == newElem)
                                {
                                    stillExists = true;
                                    break;
                                }
                            }
                            if (!stillExists)
                            {
                                relationRemove(fieldNumber, oldElem);
                            }
                        }
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.RelationshipManager#relationAdd(int, java.lang.Object)
     */
    public void relationAdd(int fieldNumber, Object val)
    {
        if (ec.isManagingRelations())
        {
            return;
        }

        AbstractMemberMetaData mmd = ownerSM.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        RelationType relationType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (relationType != RelationType.ONE_TO_MANY_BI && relationType != RelationType.MANY_TO_MANY_BI)
        {
            return;
        }

        DNStateManager elemSM = ec.findStateManager(val);
        if (elemSM != null)
        {
            AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
            if (elemSM.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
            {
                Object currentOwnerId = ec.getApiAdapter().getIdForObject(elemSM.provideField(relatedMmd.getAbsoluteFieldNumber()));
                ec.removeObjectFromLevel2Cache(currentOwnerId);
            }
        }

        Integer fieldKey = Integer.valueOf(fieldNumber);
        List<RelationChange> changeList = fieldChanges.get(fieldKey);
        if (changeList == null)
        {
            changeList = new ArrayList();
            fieldChanges.put(fieldKey, changeList);
        }

        ec.removeObjectFromLevel2Cache(ec.getApiAdapter().getIdForObject(val));
        changeList.add(new RelationChange(ChangeType.ADD_OBJECT, val));
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.RelationshipManager#relationRemove(int, java.lang.Object)
     */
    public void relationRemove(int fieldNumber, Object val)
    {
        if (ec.isManagingRelations())
        {
            return;
        }

        AbstractMemberMetaData mmd = ownerSM.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        RelationType relationType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (relationType != RelationType.ONE_TO_MANY_BI && relationType != RelationType.MANY_TO_MANY_BI)
        {
            return;
        }

        Integer fieldKey = Integer.valueOf(fieldNumber);
        List<RelationChange> changeList = fieldChanges.get(fieldKey);
        if (changeList == null)
        {
            changeList = new ArrayList();
            fieldChanges.put(fieldKey, changeList);
        }

        ec.removeObjectFromLevel2Cache(ec.getApiAdapter().getIdForObject(val));
        changeList.add(new RelationChange(ChangeType.REMOVE_OBJECT, val));
    }

    /**
     * Accessor for whether a field is being managed.
     * @param fieldNumber Number of the field
     * @return Whether it is currently managed
     */
    public boolean managesField(int fieldNumber)
    {
        return fieldChanges.containsKey(Integer.valueOf(fieldNumber));
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.RelationshipManager#checkConsistency()
     */
    public void checkConsistency()
    {
        Iterator iter = fieldChanges.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<Integer, List<RelationChange>> entry = (Map.Entry)iter.next();
            int fieldNumber = entry.getKey().intValue();
            List<RelationChange> changes = entry.getValue();

            AbstractMemberMetaData mmd = ownerSM.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            ClassLoaderResolver clr = ec.getClassLoaderResolver();
            RelationType relationType = mmd.getRelationType(clr);
            if (relationType == RelationType.ONE_TO_ONE_BI)
            {
                // 1-1 bidirectional
                checkOneToOneBidirectionalRelation(mmd, clr, ec, changes);
            }
            else if (relationType == RelationType.MANY_TO_ONE_BI)
            {
                // N-1 bidirectional
                checkManyToOneBidirectionalRelation(mmd, clr, ec, changes);
            }
            else if (relationType == RelationType.ONE_TO_MANY_BI)
            {
                // 1-N bidirectional
                checkOneToManyBidirectionalRelation(mmd, clr, ec, changes);
            }
            else if (relationType == RelationType.MANY_TO_MANY_BI)
            {
                // M-N bidirectional
                checkManyToManyBidirectionalRelation(mmd, clr, ec, changes);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.RelationshipManager#process()
     */
    public void process()
    {
        Iterator iter = fieldChanges.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<Integer, List<RelationChange>> entry = (Map.Entry)iter.next();
            int fieldNumber = entry.getKey().intValue();
            List<RelationChange> changes = entry.getValue();

            AbstractMemberMetaData mmd = ownerSM.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            ClassLoaderResolver clr = ec.getClassLoaderResolver();
            RelationType relationType = mmd.getRelationType(clr);
            if (relationType == RelationType.ONE_TO_ONE_BI)
            {
                // 1-1 bidirectional
                processOneToOneBidirectionalRelation(mmd, clr, ec, changes);
            }
            else if (relationType == RelationType.MANY_TO_ONE_BI)
            {
                // N-1 bidirectional
                processManyToOneBidirectionalRelation(mmd, clr, ec, changes);
            }
            else if (relationType == RelationType.ONE_TO_MANY_BI)
            {
                // 1-N bidirectional
                processOneToManyBidirectionalRelation(mmd, clr, ec, changes);
            }
            else if (relationType == RelationType.MANY_TO_MANY_BI)
            {
                // M-N bidirectional
                processManyToManyBidirectionalRelation(mmd, clr, ec, changes);
            }
        }
    }

    /**
     * Method to check the consistency of the passed field as 1-1.
     * Processes the case where we had a 1-1 field set at this side previously to some value and now to
     * some other value. We need to make sure that all of the affected objects are now related consistently.
     * Taking an example <pre>a.b = b1; a.b = b2;</pre> so A's b field is changed from b1 to b2.
     * The following changes are likely to be necessary
     * <ul>
     * <li>b1.a = null - so we null out the old related objects link back to this object</li>
     * <li>b2.oldA = null - if b2 was previously related to a different A, null out that objects link to b2</li>
     * <li>b2.a = a - set the link from b2 back to a so it is bidirectional</li>
     * </ul>
     * @param mmd MetaData for the field
     * @param clr ClassLoader resolver
     * @param ec ExecutionContext
     * @param changes List of changes to the field
     */
    protected void checkOneToOneBidirectionalRelation(AbstractMemberMetaData mmd, ClassLoaderResolver clr, ExecutionContext ec, List<RelationChange> changes)
    {
        for (RelationChange change : changes)
        {
            if (change.type == ChangeType.CHANGE_OBJECT)
            {
                Object newValue = ownerSM.provideField(mmd.getAbsoluteFieldNumber()); // TODO Use change.value (JDO TCK test pm.conf can fail due to being hollow instead of transient)
                if (newValue != null)
                {
                    // Previously had "a.b = b1"; Now have "a.b = b2"
                    // Check that the new value hasnt been assigned to something other than this object
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, newValue);
                    DNStateManager newSM = ec.findStateManager(newValue);
                    if (newSM != null && relatedMmd != null)
                    {
                        if (!newSM.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            // Load the field in case we need to set the old value
                            newSM.loadField(relatedMmd.getAbsoluteFieldNumber());
                        }
                        Object newValueFieldValue = newSM.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (newValueFieldValue != pc)
                        {
                            RelationshipManager newRelMgr = ec.getRelationshipManager(newSM);
                            if (newRelMgr != null && newRelMgr.managesField(relatedMmd.getAbsoluteFieldNumber()))
                            {
                                // New value has had its side of the relation changed to a different value altogether!
                                if (newValueFieldValue == null)
                                {
                                    String msg = Localiser.msg("013003", StringUtils.toJVMIDString(pc), mmd.getName(), StringUtils.toJVMIDString(newValue), relatedMmd.getName());
                                    NucleusLogger.PERSISTENCE.error(msg);
                                    throw new NucleusUserException(msg);
                                }

                                String msg = Localiser.msg("013002", StringUtils.toJVMIDString(pc), mmd.getName(), StringUtils.toJVMIDString(newValue), relatedMmd.getName(),
                                    StringUtils.toJVMIDString(newValueFieldValue));
                                NucleusLogger.PERSISTENCE.error(msg);
                                throw new NucleusUserException(msg);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Method to check the consistency of the passed field as 1-N.
     * @param mmd MetaData for the field
     * @param clr ClassLoader resolver
     * @param ec ExecutionContext
     * @param changes List of changes to the collection
     */
    protected void checkOneToManyBidirectionalRelation(AbstractMemberMetaData mmd, ClassLoaderResolver clr, ExecutionContext ec, List<RelationChange> changes)
    {
        for (RelationChange change : changes)
        {
            if (change.type == ChangeType.ADD_OBJECT)
            {
                if (ec.getApiAdapter().isDeleted(change.value))
                {
                    // The element was added but was then the element object was deleted!
                    throw new NucleusUserException(Localiser.msg("013008", StringUtils.toJVMIDString(pc), mmd.getName(), StringUtils.toJVMIDString(change.value)));
                }

                AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                DNStateManager newElementSM = ec.findStateManager(change.value);
                if (newElementSM != null)
                {
                    if (newElementSM.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                    {
                        RelationshipManager newElementRelMgr = ec.getRelationshipManager(newElementSM);
                        if (newElementRelMgr != null && newElementRelMgr.managesField(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            // Element has had the owner set, so make sure it is set to this object
                            Object newValueFieldValue = newElementSM.provideField(relatedMmd.getAbsoluteFieldNumber());
                            if (newValueFieldValue != pc && newValueFieldValue != null)
                            {
                                ApiAdapter api = ec.getApiAdapter();
                                Object id1 = api.getIdForObject(pc);
                                Object id2 = api.getIdForObject(newValueFieldValue);
                                if (id1 != null && id2 != null && id1.equals(id2))
                                {
                                    // Do nothing, just the difference between attached and detached form of the same object
                                    // Note could add check on ExecutionContext of the two objects to be safe (detached will likely be null)
                                }
                                else
                                {
                                    // The element has a different owner than the PC with this collection
                                    // This catches cases where the user has set the wrong owner, and also
                                    // will catch cases where the user has added it to two collections
                                    throw new NucleusUserException(Localiser.msg("013009", StringUtils.toJVMIDString(pc), mmd.getName(), 
                                        StringUtils.toJVMIDString(change.value), StringUtils.toJVMIDString(newValueFieldValue)));
                                }
                            }
                        }
                    }
                }
            }
            else if (change.type == ChangeType.REMOVE_OBJECT)
            {
                if (ec.getApiAdapter().isDeleted(change.value))
                {
                    // The element was removed and was then the element object was deleted so do nothing
                }
                else
                {
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    DNStateManager newElementSM = ec.findStateManager(change.value);
                    if (newElementSM != null)
                    {
                        if (newElementSM.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            RelationshipManager newElementRelMgr = ec.getRelationshipManager(newElementSM);
                            if (newElementRelMgr != null && newElementRelMgr.managesField(relatedMmd.getAbsoluteFieldNumber()))
                            {
                                // Element has had the owner set, so make sure it is not set to this object
                                Object newValueFieldValue = newElementSM.provideField(relatedMmd.getAbsoluteFieldNumber());
                                if (newValueFieldValue == pc)
                                {
                                    // The element was removed from the collection, but was updated to have its owner set to the collection owner!
                                    throw new NucleusUserException(Localiser.msg("013010", StringUtils.toJVMIDString(pc), mmd.getName(), StringUtils.toJVMIDString(change.value)));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Method to check the consistency of the passed field as N-1.
     * Processes the case where we had an N-1 field set at this side previously to some value and now to
     * some other value.That is, this object was in some collection/map originally, and now should be in some
     * other collection/map. So in terms of an example this object "a" was in collection "b1.as" before and is 
     * now in "b2.as". The following changes are likely to be necessary
     * <ul>
     * <li>b1.getAs().remove(a) - remove it from b1.as if still present</li>
     * <li>b2.getAs().add(a) - add it to b1.as if not present</li>
     * </ul>
     * @param mmd MetaData for the field
     * @param clr ClassLoader resolver
     * @param ec ExecutionContext
     * @param changes List of changes to the field
     */
    protected void checkManyToOneBidirectionalRelation(AbstractMemberMetaData mmd, ClassLoaderResolver clr, ExecutionContext ec, List<RelationChange> changes)
    {
        // TODO Implement N-1
    }

    /**
     * Method to check consistency of the passed field as M-N.
     * @param mmd MetaData for the field
     * @param clr ClassLoader resolver
     * @param ec ExecutionContext
     * @param changes List of changes to the collection
     */
    protected void checkManyToManyBidirectionalRelation(AbstractMemberMetaData mmd, ClassLoaderResolver clr, ExecutionContext ec, List<RelationChange> changes)
    {
        // TODO Implement M-N
    }

    /**
     * Method to process all 1-1 bidir fields.
     * Processes the case where we had a 1-1 field set at this side previously to some value and now to
     * some other value. We need to make sure that all of the affected objects are now related consistently.
     * Taking an example <pre>a.b = b1; a.b = b2;</pre> so A's b field is changed from b1 to b2.
     * The following changes are likely to be necessary
     * <ul>
     * <li>b1.a = null - so we null out the old related objects link back to this object</li>
     * <li>b2.oldA = null - if b2 was previously related to a different A, null out that objects link to b2</li>
     * <li>b2.a = a - set the link from b2 back to a so it is bidirectional</li>
     * </ul>
     * @param mmd MetaData for the field
     * @param clr ClassLoader resolver
     * @param ec ExecutionContext
     * @param changes List of changes to the field
     */
    protected void processOneToOneBidirectionalRelation(AbstractMemberMetaData mmd, ClassLoaderResolver clr, ExecutionContext ec, List<RelationChange> changes)
    {
        for (RelationChange change : changes)
        {
            if (change.type == ChangeType.CHANGE_OBJECT)
            {
                Object oldValue = change.oldValue;
                Object newValue = ownerSM.provideField(mmd.getAbsoluteFieldNumber()); // TODO Use change.value (JDO TCK test pm.conf can fail due to being hollow instead of transient)
                oldValue = mmd.isSingleCollection() ? singleCollectionValue(ec.getTypeManager(), oldValue) : oldValue;
                if (oldValue != null)
                {
                    // Previously had "a.b = b1"; "a.b" has been changed
                    // Need to remove from the other side if still set
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, oldValue);
                    DNStateManager oldSM = ec.findStateManager(oldValue);
                    if (oldSM != null)
                    {
                        boolean oldIsDeleted = ec.getApiAdapter().isDeleted(oldSM.getObject());
                        if (!oldIsDeleted)
                        {
                            // Old still exists, so make sure its relation is correct
                            if (!oldSM.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                            {
                                // Load the field in case we need to set the old value
                                oldSM.loadField(relatedMmd.getAbsoluteFieldNumber());
                            }
                            Object oldValueFieldValue = oldSM.provideField(relatedMmd.getAbsoluteFieldNumber());
                            oldValueFieldValue = mmd.isSingleCollection() ? singleCollectionValue(ec.getTypeManager(), oldValueFieldValue) : oldValueFieldValue;
                            if (oldValueFieldValue == null)
                            {
                                // Set to null so nothing to do
                            }
                            else if (oldValueFieldValue == pc)
                            {
                                // Still set to this object, so null out the other objects relation
                                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                                {
                                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("013004", StringUtils.toJVMIDString(oldValue), relatedMmd.getFullFieldName(),
                                        StringUtils.toJVMIDString(pc), StringUtils.toJVMIDString(newValue)));
                                }
                                Object replaceValue = null;
                                if (relatedMmd.isSingleCollection())
                                {
                                    replaceValue = ec.getTypeManager().getContainerHandler(relatedMmd.getType()).newContainer(mmd);
                                }
                                oldSM.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), replaceValue);
                            }
                        }
                        else
                        {
                            // Old value is already deleted so don't need to set its relation field
                        }
                    }
                }
                newValue = mmd.isSingleCollection() ? singleCollectionValue(ec.getTypeManager(), newValue) : newValue;
                if (newValue != null)
                {
                    // Previously had "a.b = b1"; Now have "a.b = b2"
                    // Need to set the other side if not yet set, and unset any related old value on the other side
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, newValue);
                    // Force persistence because it might not be persisted yet when using delayed operations
                    DNStateManager newSM = ec.findStateManager(newValue,true);
                    if (newSM != null && relatedMmd != null)
                    {
                        if (!newSM.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            // Load the field in case we need to set the link from the old value
                            newSM.loadField(relatedMmd.getAbsoluteFieldNumber());
                        }
                        Object newValueFieldValue = newSM.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (relatedMmd.isSingleCollection())
                        {
                            newValueFieldValue = singleCollectionValue(ec.getTypeManager(), newValueFieldValue);
                        }
                        
                        if (newValueFieldValue == null)
                        {
                            // Was set to null so set to our object
                            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                            {
                                NucleusLogger.PERSISTENCE.debug(Localiser.msg("013005", StringUtils.toJVMIDString(newValue), relatedMmd.getFullFieldName(), StringUtils.toJVMIDString(pc)));
                            }
                            Object replaceValue = pc;
                            if (relatedMmd.isSingleCollection())
                            {
                            	ElementContainerHandler containerHandler = ec.getTypeManager().getContainerHandler(relatedMmd.getType());
								replaceValue = containerHandler.newContainer(null, pc);
                            }
                            newSM.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), replaceValue);
                        }
                        else if (newValueFieldValue != pc)
                        {
                            // Was set to different object, so null out the other objects relation
                            DNStateManager newValueFieldSM = ec.findStateManager(newValueFieldValue);
                            if (newValueFieldSM != null)
                            {
                                // Null out the field of the related object of the new value
                                if (!newValueFieldSM.isFieldLoaded(mmd.getAbsoluteFieldNumber()))
                                {
                                    // Load the field in case we need to set the link from the old value
                                    newValueFieldSM.loadField(mmd.getAbsoluteFieldNumber());
                                }
                                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                                {
                                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("013004", StringUtils.toJVMIDString(newValueFieldValue), mmd.getFullFieldName(),
                                        StringUtils.toJVMIDString(newValue), StringUtils.toJVMIDString(pc)));
                                }
                                newValueFieldSM.replaceFieldValue(mmd.getAbsoluteFieldNumber(), null);
                            }
                            // Update the field of the new value to our object
                            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                            {
                                NucleusLogger.PERSISTENCE.debug(Localiser.msg("013005", StringUtils.toJVMIDString(newValue), relatedMmd.getFullFieldName(), StringUtils.toJVMIDString(pc)));
                            }
                            
                            Object replaceValue = pc;
                            if (relatedMmd.isSingleCollection())
                            {
                            	ElementContainerHandler containerHandler = ec.getTypeManager().getContainerHandler(relatedMmd.getType());
                                replaceValue = containerHandler.newContainer(null, pc);
                            }
                            newSM.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), replaceValue);
                        }
                    }
                }
            }
        }
    }

    /**
     * Method to process all 1-N bidirectional fields.
     * @param mmd MetaData for the field
     * @param clr ClassLoader resolver
     * @param ec ExecutionContext
     * @param changes List of changes to the collection
     */
    protected void processOneToManyBidirectionalRelation(AbstractMemberMetaData mmd, ClassLoaderResolver clr, ExecutionContext ec, List<RelationChange> changes)
    {
        // TODO A better way here would be to consider a particular element and if removed+added
        // then we should allow for that in updates. Also, what if an element is reassigned to a different
        // collection; this is not really allowed for here since we only consider one collection
        for (RelationChange change : changes)
        {
            if (change.type != ChangeType.ADD_OBJECT && change.type != ChangeType.REMOVE_OBJECT)
            {
                continue;
            }

            DNStateManager sm = ec.findStateManager(change.value);
            if (sm == null && ec.getApiAdapter().isDetached(change.value))
            {
                // Provided value was detached, so get its attached equivalent
                Object attached = ec.getAttachedObjectForId(ec.getApiAdapter().getIdForObject(change.value));
                if (attached != null)
                {
                    sm = ec.findStateManager(attached);
                }
            }
            if (sm != null)
            {
                if (change.type == ChangeType.ADD_OBJECT)
                {
                    // make sure the link to the owner is now set
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    if (sm.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                    {
                        Object currentVal = sm.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (currentVal != ownerSM.getObject())
                        {
                            sm.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), ownerSM.getObject());
                        }
                    }
                    else
                    {
                        ec.removeObjectFromLevel2Cache(sm.getInternalObjectId());
                    }
                }
                else if (change.type == ChangeType.REMOVE_OBJECT)
                {
                    // make sure the link back to the owner is not still set
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    if (sm.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                    {
                        Object currentVal = sm.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (currentVal == ownerSM.getObject())
                        {
                            sm.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), null);
                        }
                    }
                    else
                    {
                        ec.removeObjectFromLevel2Cache(sm.getInternalObjectId());
                    }
                }
            }
        }
    }

    /**
     * Method to process all N-1 bidirectional fields.
     * Processes the case where we had an N-1 field set at this side previously to some value and now to
     * some other value.That is, this object was in some collection/map originally, and now should be in some
     * other collection/map. So in terms of an example this object "a" was in collection "b1.as" before and is 
     * now in "b2.as". The following changes are likely to be necessary
     * <ul>
     * <li>b1.getAs().remove(a) - remove it from b1.as if still present</li>
     * <li>b2.getAs().add(a) - add it to b1.as if not present</li>
     * </ul>
     * @param mmd MetaData for the field
     * @param clr ClassLoader resolver
     * @param ec ExecutionContext
     * @param changes List of changes to the collection
     */
    protected void processManyToOneBidirectionalRelation(AbstractMemberMetaData mmd, ClassLoaderResolver clr, ExecutionContext ec, List<RelationChange> changes)
    {
        for (RelationChange change : changes)
        {
            if (change.type == ChangeType.CHANGE_OBJECT)
            {
                Object oldValue = change.oldValue;
                Object newValue = ownerSM.provideField(mmd.getAbsoluteFieldNumber()); // TODO Use change.value
                if (oldValue != null)
                {
                    // Has been removed from a Collection/Map
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, oldValue);
                    DNStateManager oldSM = ec.findStateManager(oldValue);
                    if (oldSM != null && relatedMmd != null && oldSM.getLoadedFields()[relatedMmd.getAbsoluteFieldNumber()])
                    {
                        if (oldSM.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            Object oldContainerValue = oldSM.provideField(relatedMmd.getAbsoluteFieldNumber());
                            if (oldContainerValue instanceof Collection)
                            {
                                Collection oldColl = (Collection)oldContainerValue;
                                if (oldColl.contains(pc))
                                {
                                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                                    {
                                        NucleusLogger.PERSISTENCE.debug(Localiser.msg("013006", StringUtils.toJVMIDString(pc), mmd.getFullFieldName(),
                                            relatedMmd.getFullFieldName(), StringUtils.toJVMIDString(oldValue)));
                                    }

                                    if (oldColl instanceof SCOCollection)
                                    {
                                        // Avoid any cascade deletes that could have been fired by this action
                                        ((SCOCollection)oldColl).remove(pc, false);
                                    }
                                    else
                                    {
                                        oldColl.remove(pc);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        if (oldSM != null)
                        {
                            ec.removeObjectFromLevel2Cache(oldSM.getInternalObjectId());
                        }
                    }
                }

                if (newValue != null)
                {
                    // Add new value to the Collection
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, newValue);
                    DNStateManager newSM = ec.findStateManager(newValue);
                    if (newSM != null && relatedMmd != null && newSM.getLoadedFields()[relatedMmd.getAbsoluteFieldNumber()])
                    {
                        Object newContainerValue = newSM.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (newContainerValue instanceof Collection)
                        {
                            Collection newColl = (Collection)newContainerValue;
                            if (!newColl.contains(pc))
                            {
                                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                                {
                                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("013007", StringUtils.toJVMIDString(pc), mmd.getFullFieldName(),
                                        relatedMmd.getFullFieldName(), StringUtils.toJVMIDString(newValue)));
                                }
                                newColl.add(pc);
                            }
                        }
                    }
                    else
                    {
                        // Relation field not loaded so evict it from the L2 cache to avoid loading old field values
                        ec.removeObjectFromLevel2Cache(ec.getApiAdapter().getIdForObject(newValue));
                    }
                }
            }
        }
    }

    /**
     * Method to process all M-N bidirectional fields.
     * @param mmd MetaData for the field
     * @param clr ClassLoader resolver
     * @param ec ExecutionContext
     * @param changes List of changes to the collection
     */
    protected void processManyToManyBidirectionalRelation(AbstractMemberMetaData mmd, ClassLoaderResolver clr, ExecutionContext ec, List<RelationChange> changes)
    {
        for (RelationChange change : changes)
        {
            if (change.type != ChangeType.ADD_OBJECT && change.type != ChangeType.REMOVE_OBJECT)
            {
                continue;
            }

            DNStateManager sm = ec.findStateManager(change.value);
            if (sm == null && ec.getApiAdapter().isDetached(change.value))
            {
                // Provided value was detached, so get its attached equivalent
                Object attached = ec.getAttachedObjectForId(ec.getApiAdapter().getIdForObject(change.value));
                if (attached != null)
                {
                    sm = ec.findStateManager(attached);
                }
            }
            if (sm != null)
            {
                if (change.type == ChangeType.ADD_OBJECT)
                {
                    // make sure the element has the owner in its collection
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    ec.removeObjectFromLevel2Cache(sm.getInternalObjectId());
                    ec.removeObjectFromLevel2Cache(ownerSM.getInternalObjectId());
                    if (ownerSM.isFieldLoaded(mmd.getAbsoluteFieldNumber()) && !ownerSM.getLifecycleState().isDeleted)
                    {
                        Collection currentVal = (Collection)ownerSM.provideField(mmd.getAbsoluteFieldNumber());
                        if (currentVal != null && !currentVal.contains(sm.getObject()))
                        {
                            currentVal.add(sm.getObject());
                        }
                    }
                    if (sm.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                    {
                        Collection currentVal = (Collection)sm.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (currentVal != null && !currentVal.contains(ownerSM.getObject()))
                        {
                            currentVal.add(ownerSM.getObject());
                        }
                    }
                }
                else if (change.type == ChangeType.REMOVE_OBJECT)
                {
                    // make sure the element removes the owner from its collection
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    ec.removeObjectFromLevel2Cache(sm.getInternalObjectId());
                    ec.removeObjectFromLevel2Cache(ownerSM.getInternalObjectId());
                    if (ownerSM.isFieldLoaded(mmd.getAbsoluteFieldNumber()) && !ownerSM.getLifecycleState().isDeleted)
                    {
                        Collection currentVal = (Collection)ownerSM.provideField(mmd.getAbsoluteFieldNumber());
                        if (!sm.getLifecycleState().isDeleted && currentVal != null && currentVal.contains(sm.getObject()))
                        {
                            currentVal.remove(sm.getObject());
                        }
                        else
                        {
                            // element is deleted so can't call remove since it may try to read fields from it
                            // so just unload the collection in the owner forcing it to be reloaded from the DB
                            ownerSM.unloadField(mmd.getAbsoluteFieldNumber());
                        }
                    }
                    if (sm.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()) && !sm.getLifecycleState().isDeleted)
                    {
                        Collection currentVal = (Collection)sm.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (currentVal != null && currentVal.contains(ownerSM.getObject()))
                        {
                            currentVal.remove(ownerSM.getObject());
                        }
                    }
                }
            }
        }
    }
}