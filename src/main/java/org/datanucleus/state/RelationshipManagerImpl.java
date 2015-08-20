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
import org.datanucleus.store.types.ElementContainerHandler;
import org.datanucleus.store.types.SCOCollection;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Manager for (bidirectional) relationships of a class.
 * Designed as a stand-alone process to run just before flush.
 * Updates on bidirectional relations are registered during the persistence process. 
 * Call to checkConsistency() will check for consistency and throw exceptions as appropriate.
 * Call to process() will perform updates at the other side of the registered relations so all is consistent.
 */
public class RelationshipManagerImpl implements RelationshipManager
{
    /** ObjectProvider for the object we are managing the relationships for. */
    final ObjectProvider ownerOP;

    final ExecutionContext ec;

    /** Object being managed. */
    final Object pc;

    /** Map of bidirectional field "changes", keyed by the absolute field number of the owner object. */
    final Map<Integer, List<RelationChange>> fieldChanges;

    /**
     * Constructor.
     * @param op ObjectProvider for the object that we are managing relations for.
     */
    public RelationshipManagerImpl(ObjectProvider op)
    {
        this.ownerOP = op;
        this.ec = op.getExecutionContext();
        this.pc = op.getObject();
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

        AbstractMemberMetaData mmd = ownerOP.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
                if (oldValue == null)
                {
                    if (newValue != null)
                    {
                        // Add all elements
                        Iterator iter = ((Collection)newValue).iterator();
                        while (iter.hasNext())
                        {
                            changes.add(new RelationChange(ChangeType.ADD_OBJECT, iter.next()));
                        }
                    }
                }
                else
                {
                    if (newValue == null)
                    {
                        AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
                        Iterator iter = ((Collection)oldValue).iterator();
                        while (iter.hasNext())
                        {
                            Object element = iter.next();
                            if (ownerOP.getLifecycleState().isDeleted)
                            {
                                // Deleting the owner, so register the element to reset its owner
                                ec.removeObjectFromLevel2Cache(ec.getApiAdapter().getIdForObject(element));
                                ObjectProvider elementOP = ec.findObjectProvider(element);
                                if (relationType == RelationType.ONE_TO_MANY_BI)
                                {
                                    // TODO This needs marking as a secondary change. i.e we dont want follow on checks, just null out the relation during process()
                                    ec.getRelationshipManager(elementOP).relationChange(relatedMmd.getAbsoluteFieldNumber(), ownerOP.getObject(), null);
                                }
                                else if (relationType == RelationType.MANY_TO_MANY_BI)
                                {
                                    // TODO This needs marking as a secondary change. i.e we don't want follow on checks, just remove the element from the relation during process()
                                    ec.getRelationshipManager(elementOP).relationRemove(relatedMmd.getAbsoluteFieldNumber(), ownerOP.getObject());
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
                        Iterator newIter = ((Collection)newValue).iterator();
                        while (newIter.hasNext())
                        {
                            Object newElem = newIter.next();
                            Iterator oldIter = ((Collection)oldValue).iterator();
                            boolean alreadyExists = false;
                            while (oldIter.hasNext())
                            {
                                Object oldElem = oldIter.next();
                                if (newElem == oldElem)
                                {
                                    alreadyExists = true;
                                    break;
                                }
                            }
                            if (!alreadyExists)
                            {
                                ObjectProvider elemOP = ec.findObjectProvider(newElem);
                                if (elemOP != null)
                                {
                                    AbstractMemberMetaData elemMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
                                    Object oldOwner = elemOP.provideField(elemMmd.getAbsoluteFieldNumber());
                                    if (!elemOP.isFieldLoaded(elemMmd.getAbsoluteFieldNumber()))
                                    {
                                        elemOP.loadField(elemMmd.getAbsoluteFieldNumber());
                                    }
                                    if (oldOwner != null)
                                    {
                                        // Remove from old owner collection
                                        ObjectProvider oldOwnerOP = ec.findObjectProvider(oldOwner);
                                        if (oldOwnerOP != null)
                                        {
                                            // TODO This needs marking as a secondary change. i.e we dont want follow on checks, just remove the element from the relation during process()
                                            ec.getRelationshipManager(oldOwnerOP).relationRemove(fieldNumber, newElem);
                                        }
                                    }
                                }
                                relationAdd(fieldNumber, newElem);
                            }
                        }
                        Iterator oldIter = ((Collection)oldValue).iterator();
                        while (oldIter.hasNext())
                        {
                            Object oldElem = oldIter.next();
                            newIter = ((Collection)newValue).iterator();
                            boolean stillExists = false;
                            while (newIter.hasNext())
                            {
                                Object newElem = newIter.next();
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

        AbstractMemberMetaData mmd = ownerOP.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        RelationType relationType = mmd.getRelationType(ec.getClassLoaderResolver());
        if (relationType != RelationType.ONE_TO_MANY_BI && relationType != RelationType.MANY_TO_MANY_BI)
        {
            return;
        }

        ObjectProvider elemOP = ec.findObjectProvider(val);
        if (elemOP != null)
        {
            AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(ec.getClassLoaderResolver())[0];
            if (elemOP.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
            {
                Object currentOwnerId = ec.getApiAdapter().getIdForObject(elemOP.provideField(relatedMmd.getAbsoluteFieldNumber()));
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

        AbstractMemberMetaData mmd = ownerOP.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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

            AbstractMemberMetaData mmd = ownerOP.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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

            AbstractMemberMetaData mmd = ownerOP.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
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
        Iterator iter = changes.iterator();
        while (iter.hasNext())
        {
            RelationChange change = (RelationChange)iter.next();
            if (change.type == ChangeType.CHANGE_OBJECT)
            {
                Object newValue = ownerOP.provideField(mmd.getAbsoluteFieldNumber()); // TODO Use change.value (JDO TCK test pm.conf can fail due to being hollow instead of transient)
                if (newValue != null)
                {
                    // Previously had "a.b = b1"; Now have "a.b = b2"
                    // Check that the new value hasnt been assigned to something other than this object
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, newValue);
                    ObjectProvider newOP = ec.findObjectProvider(newValue);
                    if (newOP != null && relatedMmd != null)
                    {
                        if (!newOP.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            // Load the field in case we need to set the old value
                            newOP.loadField(relatedMmd.getAbsoluteFieldNumber());
                        }
                        Object newValueFieldValue = newOP.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (newValueFieldValue != pc)
                        {
                            RelationshipManager newRelMgr = ec.getRelationshipManager(newOP);
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
        Iterator iter = changes.iterator();
        while (iter.hasNext())
        {
            RelationChange change = (RelationChange)iter.next();
            if (change.type == ChangeType.ADD_OBJECT)
            {
                if (ec.getApiAdapter().isDeleted(change.value))
                {
                    // The element was added but was then the element object was deleted!
                    throw new NucleusUserException(Localiser.msg("013008", StringUtils.toJVMIDString(pc), mmd.getName(), StringUtils.toJVMIDString(change.value)));
                }

                AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                ObjectProvider newElementOP = ec.findObjectProvider(change.value);
                if (newElementOP != null)
                {
                    if (newElementOP.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                    {
                        RelationshipManager newElementRelMgr = ec.getRelationshipManager(newElementOP);
                        if (newElementRelMgr != null && newElementRelMgr.managesField(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            // Element has had the owner set, so make sure it is set to this object
                            Object newValueFieldValue = newElementOP.provideField(relatedMmd.getAbsoluteFieldNumber());
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
                    ObjectProvider newElementOP = ec.findObjectProvider(change.value);
                    if (newElementOP != null)
                    {
                        if (newElementOP.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            RelationshipManager newElementRelMgr = ec.getRelationshipManager(newElementOP);
                            if (newElementRelMgr != null && newElementRelMgr.managesField(relatedMmd.getAbsoluteFieldNumber()))
                            {
                                // Element has had the owner set, so make sure it is not set to this object
                                Object newValueFieldValue = newElementOP.provideField(relatedMmd.getAbsoluteFieldNumber());
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
        Iterator iter = changes.iterator();
        while (iter.hasNext())
        {
            RelationChange change = (RelationChange)iter.next();
            if (change.type == ChangeType.CHANGE_OBJECT)
            {
                Object oldValue = change.oldValue;
                Object newValue = ownerOP.provideField(mmd.getAbsoluteFieldNumber()); // TODO Use change.value (JDO TCK test pm.conf can fail due to being hollow instead of transient)
                oldValue = mmd.isSingleCollection() ? singleCollectionValue(ec.getTypeManager(), oldValue) : oldValue;
                if (oldValue != null)
                {
                    // Previously had "a.b = b1"; "a.b" has been changed
                    // Need to remove from the other side if still set
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, oldValue);
                    ObjectProvider oldOP = ec.findObjectProvider(oldValue);
                    if (oldOP != null)
                    {
                        boolean oldIsDeleted = ec.getApiAdapter().isDeleted(oldOP.getObject());
                        if (!oldIsDeleted)
                        {
                            // Old still exists, so make sure its relation is correct
                            if (!oldOP.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                            {
                                // Load the field in case we need to set the old value
                                oldOP.loadField(relatedMmd.getAbsoluteFieldNumber());
                            }
                            Object oldValueFieldValue = oldOP.provideField(relatedMmd.getAbsoluteFieldNumber());
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
                                oldOP.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), replaceValue);
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
                    ObjectProvider newOP = ec.findObjectProvider(newValue,true);
                    if (newOP != null && relatedMmd != null)
                    {
                        if (!newOP.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            // Load the field in case we need to set the link from the old value
                            newOP.loadField(relatedMmd.getAbsoluteFieldNumber());
                        }
                        Object newValueFieldValue = newOP.provideField(relatedMmd.getAbsoluteFieldNumber());
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
                            newOP.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), replaceValue);
                        }
                        else if (newValueFieldValue != pc)
                        {
                            // Was set to different object, so null out the other objects relation
                            ObjectProvider newValueFieldOP = ec.findObjectProvider(newValueFieldValue);
                            if (newValueFieldOP != null)
                            {
                                // Null out the field of the related object of the new value
                                if (!newValueFieldOP.isFieldLoaded(mmd.getAbsoluteFieldNumber()))
                                {
                                    // Load the field in case we need to set the link from the old value
                                    newValueFieldOP.loadField(mmd.getAbsoluteFieldNumber());
                                }
                                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                                {
                                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("013004", StringUtils.toJVMIDString(newValueFieldValue), mmd.getFullFieldName(),
                                        StringUtils.toJVMIDString(newValue), StringUtils.toJVMIDString(pc)));
                                }
                                newValueFieldOP.replaceFieldValue(mmd.getAbsoluteFieldNumber(), null);
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
                            newOP.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), replaceValue);
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
        Iterator iter = changes.iterator();
        while (iter.hasNext())
        {
            RelationChange change = (RelationChange)iter.next();
            if (change.type != ChangeType.ADD_OBJECT && change.type != ChangeType.REMOVE_OBJECT)
            {
                continue;
            }

            ObjectProvider op = ec.findObjectProvider(change.value);
            if (op == null && ec.getApiAdapter().isDetached(change.value))
            {
                // Provided value was detached, so get its attached equivalent
                Object attached = ec.getAttachedObjectForId(ec.getApiAdapter().getIdForObject(change.value));
                if (attached != null)
                {
                    op = ec.findObjectProvider(attached);
                }
            }
            if (op != null)
            {
                if (change.type == ChangeType.ADD_OBJECT)
                {
                    // make sure the link to the owner is now set
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    if (op.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                    {
                        Object currentVal = op.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (currentVal != ownerOP.getObject())
                        {
                            op.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), ownerOP.getObject());
                        }
                    }
                    else
                    {
                        ec.removeObjectFromLevel2Cache(op.getInternalObjectId());
                    }
                }
                else if (change.type == ChangeType.REMOVE_OBJECT)
                {
                    // make sure the link back to the owner is not still set
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    if (op.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                    {
                        Object currentVal = op.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (currentVal == ownerOP.getObject())
                        {
                            op.replaceFieldValue(relatedMmd.getAbsoluteFieldNumber(), null);
                        }
                    }
                    else
                    {
                        ec.removeObjectFromLevel2Cache(op.getInternalObjectId());
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
        Iterator iter = changes.iterator();
        while (iter.hasNext())
        {
            RelationChange change = (RelationChange)iter.next();
            if (change.type == ChangeType.CHANGE_OBJECT)
            {
                Object oldValue = change.oldValue;
                Object newValue = ownerOP.provideField(mmd.getAbsoluteFieldNumber()); // TODO Use change.value
                if (oldValue != null)
                {
                    // Has been removed from a Collection/Map
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, oldValue);
                    ObjectProvider oldOP = ec.findObjectProvider(oldValue);
                    if (oldOP != null && relatedMmd != null && oldOP.getLoadedFields()[relatedMmd.getAbsoluteFieldNumber()])
                    {
                        if (oldOP.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                        {
                            Object oldContainerValue = oldOP.provideField(relatedMmd.getAbsoluteFieldNumber());
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
                        if (oldOP != null)
                        {
                            ec.removeObjectFromLevel2Cache(oldOP.getInternalObjectId());
                        }
                    }
                }

                if (newValue != null)
                {
                    // Add new value to the Collection
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaDataForObject(clr, pc, newValue);
                    ObjectProvider newOP = ec.findObjectProvider(newValue);
                    if (newOP != null && relatedMmd != null && newOP.getLoadedFields()[relatedMmd.getAbsoluteFieldNumber()])
                    {
                        Object newContainerValue = newOP.provideField(relatedMmd.getAbsoluteFieldNumber());
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
        Iterator iter = changes.iterator();
        while (iter.hasNext())
        {
            RelationChange change = (RelationChange)iter.next();
            if (change.type != ChangeType.ADD_OBJECT && change.type != ChangeType.REMOVE_OBJECT)
            {
                continue;
            }

            ObjectProvider op = ec.findObjectProvider(change.value);
            if (op == null && ec.getApiAdapter().isDetached(change.value))
            {
                // Provided value was detached, so get its attached equivalent
                Object attached = ec.getAttachedObjectForId(ec.getApiAdapter().getIdForObject(change.value));
                if (attached != null)
                {
                    op = ec.findObjectProvider(attached);
                }
            }
            if (op != null)
            {
                if (change.type == ChangeType.ADD_OBJECT)
                {
                    // make sure the element has the owner in its collection
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    ec.removeObjectFromLevel2Cache(op.getInternalObjectId());
                    ec.removeObjectFromLevel2Cache(ownerOP.getInternalObjectId());
                    if (ownerOP.isFieldLoaded(mmd.getAbsoluteFieldNumber()) && !ownerOP.getLifecycleState().isDeleted)
                    {
                        Collection currentVal = (Collection)ownerOP.provideField(mmd.getAbsoluteFieldNumber());
                        if (currentVal != null && !currentVal.contains(op.getObject()))
                        {
                            currentVal.add(op.getObject());
                        }
                    }
                    if (op.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()))
                    {
                        Collection currentVal = (Collection)op.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (currentVal != null && !currentVal.contains(ownerOP.getObject()))
                        {
                            currentVal.add(ownerOP.getObject());
                        }
                    }
                }
                else if (change.type == ChangeType.REMOVE_OBJECT)
                {
                    // make sure the element removes the owner from its collection
                    AbstractMemberMetaData relatedMmd = mmd.getRelatedMemberMetaData(clr)[0];
                    ec.removeObjectFromLevel2Cache(op.getInternalObjectId());
                    ec.removeObjectFromLevel2Cache(ownerOP.getInternalObjectId());
                    if (ownerOP.isFieldLoaded(mmd.getAbsoluteFieldNumber()) && !ownerOP.getLifecycleState().isDeleted)
                    {
                        Collection currentVal = (Collection)ownerOP.provideField(mmd.getAbsoluteFieldNumber());
                        if (!op.getLifecycleState().isDeleted && currentVal != null && currentVal.contains(op.getObject()))
                        {
                            currentVal.remove(op.getObject());
                        }
                        else
                        {
                            // element is deleted so can't call remove since it may try to read fields from it
                            // so just unload the collection in the owner forcing it to be reloaded from the DB
                            ownerOP.unloadField(mmd.getName());
                        }
                    }
                    if (op.isFieldLoaded(relatedMmd.getAbsoluteFieldNumber()) && !op.getLifecycleState().isDeleted)
                    {
                        Collection currentVal = (Collection)op.provideField(relatedMmd.getAbsoluteFieldNumber());
                        if (currentVal != null && currentVal.contains(ownerOP.getObject()))
                        {
                            currentVal.remove(ownerOP.getObject());
                        }
                    }
                }
            }
        }
    }
}