/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FetchGroupMemberMetaData;
import org.datanucleus.metadata.FetchGroupMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Representation of the fetch plan for a particular class, defining the members that are to be fetched.
 */
public class FetchPlanForClass
{
    /** Parent FetchPlan. */
    final FetchPlan plan;

    /** MetaData for the class that this represents. */
    final AbstractClassMetaData cmd;

    /** Absolute numbers of fields/properties in the fetch plan for this class. */
    int[] memberNumbers;

    /** Whether the record is dirty and needs the fields recalculating. */
    boolean dirty = true;

    Map<Integer, Integer> recursionDepthByMemberNumber = new HashMap<>();

    /** 
     * Cache of fetch groups by member number, as calculating them in getFetchGroupsForMemberNumber() is O(n^2) Map<Integer, Set<FetchGroupMetaData>>
     */
    private Map<Integer, Set<FetchGroupMetaData>> fetchGroupsByMemberNumber = null;

    /**
     * Constructor.
     * @param cmd MetaData for the class
     * @param fetchPlan the FetchPlan
     */
    public FetchPlanForClass(final AbstractClassMetaData cmd, FetchPlan fetchPlan)
    {
        super();
        this.cmd = cmd;
        this.plan = fetchPlan;
    }

    /**
     * Accessor for the FetchPlan that this classes plan relates to.
     * @return The FetchPlan
     */
    public final FetchPlan getFetchPlan()
    {
        return plan;
    }

    /**
     * Accessor for the MetaData for this classes plan.
     * @return MetaData for the class represented here
     */
    public final AbstractClassMetaData getAbstractClassMetaData()
    {
        return cmd;
    }

    public String toString()
    {
        return cmd.getFullClassName() + "[members=" + StringUtils.intArrayToString(getMemberNumbers()) + "]";
    }

    void markDirty()
    {
        dirty = true;
        recursionDepthByMemberNumber.clear();
        plan.invalidateCachedIsToCallPostLoadFetchPlan(cmd);
    }

    /**
     * Returns a copy of this object with all settings initialised.
     * Used when a Query has to have its own FetchPlan, so takes a copy of that of the ExecutionContext.
     * @return the FetchPlanForClass copy
     */
    FetchPlanForClass getCopy(FetchPlan fp)
    {
        FetchPlanForClass fpCopy = new FetchPlanForClass(cmd, fp);
        if (this.memberNumbers != null)
        {
            fpCopy.memberNumbers = new int[this.memberNumbers.length];
            System.arraycopy(this.memberNumbers, 0, fpCopy.memberNumbers, 0, this.memberNumbers.length);
        }
        fpCopy.dirty = this.dirty;
        return fpCopy;
    }

    /**
     * Method to return the recursion depth of this member number in the overall fetch plan.
     * @param memberNum Number of member in this class
     * @return The recursion depth
     */
    public int getRecursionDepthForMember(int memberNum)
    {
        if (dirty)
        {
            recursionDepthByMemberNumber.clear();
        }

        Integer recursionDepth = recursionDepthByMemberNumber.get(memberNum);
        if (recursionDepth != null)
        {
            return recursionDepth;
        }

        // Fallback to recursion depth for this member using its class' metadata definition, or the default for JDO/JPA (1)
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(memberNum);
        recursionDepth = (mmd.getRecursionDepth() != null) ? mmd.getRecursionDepth() : 1;

        // MetaData-based groups
        // TODO What should we do with recursionDepth if the same member is specified in multiple fetch groups? e.g 1 in groupA, and 2 in groupB
        String memberName = mmd.getName();
        Set<FetchGroupMetaData> fetchGroupsContainingField = getFetchGroupsForMemberNumber(cmd.getFetchGroupMetaData(plan.getGroups()), memberNum);
        for (FetchGroupMetaData fgmd : fetchGroupsContainingField)
        {
            Set<FetchGroupMemberMetaData> fgmmds = fgmd.getMembers();
            if (fgmmds != null)
            {
                for (FetchGroupMemberMetaData fgmmd : fgmmds)
                {
                    if (fgmmd.getName().equals(memberName))
                    {
                        recursionDepth = fgmmd.getRecursionDepth();
                        break;
                    }
                }
            }
        }

        // Dynamic groups
        if (plan.dynamicGroups != null)
        {
            // Check Dynamic Fetch groups
            for (FetchGroup group : plan.dynamicGroups)
            {
                if (group.getType().getName().equals(cmd.getFullClassName()))
                {
                    if (group.getMembers().contains(memberName))
                    {
                        recursionDepth = group.getRecursionDepth(memberName);
                        break;
                    }
                }
            }
        }

        recursionDepthByMemberNumber.put(memberNum, recursionDepth);

        return recursionDepth;
    }

    /**
     * Return whether the specified field/property is in the fetch plan
     * @param memberNumber The member number
     * @return Whether it is in the FetchPlan
     */
    public boolean hasMember(int memberNumber)
    {
        if (dirty)
        {
            BitSet fieldsNumber = getMemberNumbersByBitSet();
            return fieldsNumber.get(memberNumber);
        }
        if (memberNumbers != null)
        {
            for (int i=0;i<memberNumbers.length;i++)
            {
                if (memberNumbers[i] == memberNumber)
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get the absolute numbers of the members in the fetch plan for this class.
     * @return an array with the absolute position of the members
     */
    public int[] getMemberNumbers()
    {
        if (dirty)
        {
            dirty = false;

            // Populate memberNumbers
            BitSet fieldsNumber = getMemberNumbersByBitSet();
            int count = 0;
            for (int i = 0; i < fieldsNumber.length(); i++)
            {
                if (fieldsNumber.get(i))
                {
                    count++;
                }
            }

            memberNumbers = new int[count];
            int nextField = 0;
            for (int i = 0; i < fieldsNumber.length(); i++)
            {
                if (fieldsNumber.get(i))
                {
                    memberNumbers[nextField++] = i;
                }
            }
        }
        return memberNumbers;
    }

    /**
     * Get all members (of this class, and superclasses) in the fetch plan.
     * @return an BitSet with the bits set in the absolute position of the fields
     */
    public BitSet getMemberNumbersByBitSet()
    {
        return getMemberNumbersByBitSet(cmd);
    }

    /**
     * Get all members in the fetch plan for this class and superclasses.
     * @param cmd metadata for the class
     * @return an BitSet with the bits set in the absolute position of the members
     */
    private BitSet getMemberNumbersByBitSet(AbstractClassMetaData cmd)
    {
        FetchPlanForClass fpc = plan.getFetchPlanForClass(cmd);
        BitSet bitSet = fpc.getMemberNumbersForFetchGroups(cmd.getFetchGroupMetaData());
        if (cmd.getPersistableSuperclass() != null)
        {
            // Recurse to superclass
            AbstractClassMetaData superCmd = cmd.getSuperAbstractClassMetaData();
            FetchPlanForClass superFpc = plan.getFetchPlanForClass(superCmd);
            bitSet.or(superFpc.getMemberNumbersByBitSet(superCmd));
        }
        else
        {
            // Make sure that we always have the PK fields in the fetch plan = FetchPlanImpl.NONE
            fpc.setAsNone(bitSet);
        }

        if (plan.dynamicGroups != null)
        {
            // dynamic fetch groups
            for (FetchGroup<?> grp : plan.dynamicGroups)
            {
                if (grp.getType().getName().equals(cmd.getFullClassName()))
                {
                    // Dynamic fetch group applies
                    Set<String> members = grp.getMembers();
                    for (String memberName : members)
                    {
                        int fieldPos = cmd.getAbsolutePositionOfMember(memberName);
                        if (fieldPos >= 0)
                        {
                            bitSet.set(fieldPos);
                        }
                    }
                }
            }
        }
        return bitSet;
    }

    /**
     * Get the absolute number of the members for an array of Fetch Group.
     * @param fgmds The Fetch Groups
     * @return a BitSet with flags set to true in the member number positions
     */
    private BitSet getMemberNumbersForFetchGroups(Set<FetchGroupMetaData> fgmds)
    {
        BitSet memberNumbers = new BitSet(0);
        if (fgmds != null)
        {
            for (FetchGroupMetaData fgmd : fgmds)
            {
                if (plan.groupNames.contains(fgmd.getName()))
                {
                    memberNumbers.or(getMemberNumbersForFetchGroup(fgmd));
                }
            }
        }

        if (plan.groupNames.contains(FetchPlan.DEFAULT))
        {
            setAsDefault(memberNumbers);
        }
        if (plan.groupNames.contains(FetchPlan.ALL))
        {
            setAsAll(memberNumbers);
        }
        if (plan.groupNames.contains(FetchPlan.NONE))
        {
            setAsNone(memberNumbers);
        }
        return memberNumbers;
    }

    /**
     * Get the absolute member numbers for a particular Fetch Group.
     * @param fgmd The Fetch Group
     * @return a list of member numbers
     */
    private BitSet getMemberNumbersForFetchGroup(FetchGroupMetaData fgmd)
    {
        BitSet memberNumbers = new BitSet(0);
        Set<FetchGroupMemberMetaData> subFGmmds = fgmd.getMembers();
        if (subFGmmds != null)
        {
            for (FetchGroupMemberMetaData subFGmmd : subFGmmds)
            {
                int fieldNumber = cmd.getAbsolutePositionOfMember(subFGmmd.getName());
                if (fieldNumber == -1)
                {
                    String msg = Localiser.msg("006000", subFGmmd.getName(), fgmd.getName(), cmd.getFullClassName());
                    NucleusLogger.PERSISTENCE.error(msg);
                    throw new NucleusUserException(msg).setFatal();
                }
                memberNumbers.set(fieldNumber);
            }
        }

        // members in nested fetch-groups
        Set<FetchGroupMetaData> subFGs = fgmd.getFetchGroups();
        if (subFGs != null)
        {
            for (FetchGroupMetaData subFgmd : subFGs)
            {
                String nestedGroupName = subFgmd.getName();
                if (nestedGroupName.equals(FetchPlan.DEFAULT)) 
                {
                    setAsDefault(memberNumbers);
                }
                else if (nestedGroupName.equals(FetchPlan.ALL)) 
                {
                    setAsAll(memberNumbers);
                }
                else if (nestedGroupName.equals(FetchPlan.NONE)) 
                {
                    setAsNone(memberNumbers);
                }
                else
                {
                    FetchGroupMetaData nestedFGMD = cmd.getFetchGroupMetaData(nestedGroupName);
                    if (nestedFGMD == null)
                    {
                        throw new NucleusUserException(Localiser.msg("006001", subFgmd.getName(), fgmd.getName(), cmd.getFullClassName())).setFatal();
                    }
                    memberNumbers.or(getMemberNumbersForFetchGroup(nestedFGMD));
                }
            }
        }
        return memberNumbers;
    }

    /**
     * Sets the given BitSet of member numbers to include the DFG members.
     * @param memberNums BitSet of member numbers
     */
    private void setAsDefault(BitSet memberNums)
    {
        int numDFGMembers = cmd.getDFGMemberPositions().length;
        for (int i = 0; i < numDFGMembers; i++)
        {
            memberNums.set(cmd.getDFGMemberPositions()[i]);
        }
    }

    /**
     * Sets the given BitSet of member numbers to include all the members.
     * @param memberNums BitSet of member numbers
     */
    private void setAsAll(BitSet memberNums)
    {
        int numManagedMembers = cmd.getNoOfManagedMembers();
        for (int i = 0; i < numManagedMembers; i++)
        {
            if (cmd.getMetaDataForManagedMemberAtRelativePosition(i).getPersistenceModifier() != FieldPersistenceModifier.NONE)
            {
                memberNums.set(cmd.getNoOfInheritedManagedMembers() + i);
            }
        }
    }

    /**
     * Sets the given BitSet of member numbers to include none of the members (except the PKs).
     * @param memberNums BitSet of member numbers
     */
    private void setAsNone(BitSet memberNums)
    {
        int numManagedMembers = cmd.getNoOfManagedMembers();
        for (int i = 0; i < numManagedMembers; i++)
        {
            AbstractMemberMetaData mmd = cmd.getManagedMembers()[i];
            if (mmd.isPrimaryKey())
            {
                memberNums.set(mmd.getAbsoluteFieldNumber());
            }
        }
    }

    /**
     * Whether to call the post load or not. 
     * Checks if members in actual FetchPlan where not previouly loaded and the post-load is enabled 
     * in the metadata.
     * @param loadedMembers already loaded members
     * @return if is to call the postLoad
     */
    public boolean isToCallPostLoadFetchPlan(boolean[] loadedMembers)
    {
        BitSet cacheKey = new BitSet(loadedMembers.length);
        for (int i = 0; i < loadedMembers.length; i++)
        {
            cacheKey.set(i, loadedMembers[i]);
        }
        Boolean result = plan.getCachedIsToCallPostLoadFetchPlan(cmd, cacheKey);
        Set<String> fpGroups = plan.getGroups();

        if (result == null) 
        {
            result = Boolean.FALSE;
            int[] fieldsInActualFetchPlan = getMemberNumbers();
            for (int i = 0; i < fieldsInActualFetchPlan.length; i++)
            {
                final int fieldNumber = fieldsInActualFetchPlan[i];

                // if member in actual fetch plan was not previously loaded
                if (!loadedMembers[fieldNumber])
                {
                    AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
                    if (mmd.isDefaultFetchGroup() && fpGroups.contains(FetchPlan.DEFAULT))
                    {
                        // to call jdoPostLoad, field must be in default-fetch-group when DFG is active
                        result = Boolean.TRUE;
                    }
                    else
                    {
                        // compute only if necessary, as that's expensive to do
                        if (cmd.hasFetchGroupWithPostLoad())
                        {
                            // Statically defined fetch groups : field must be in a fetch-group which has post-load set to true
                            Integer fieldNumberInteger = Integer.valueOf(fieldNumber);
                            Set<FetchGroupMetaData> fetchGroups = null;
                            if (fetchGroupsByMemberNumber != null)
                            {
                                fetchGroups = fetchGroupsByMemberNumber.get(fieldNumberInteger);
                            }
                            if (fetchGroups == null) 
                            {
                                fetchGroups = getFetchGroupsForMemberNumber(cmd.getFetchGroupMetaData(), fieldNumber);
                                // cache those precious results from expensive invocation
                                if (fetchGroupsByMemberNumber == null)
                                {
                                    fetchGroupsByMemberNumber = new HashMap<Integer, Set<FetchGroupMetaData>>();
                                }
                                fetchGroupsByMemberNumber.put(fieldNumberInteger, fetchGroups);
                            }

                            for (FetchGroupMetaData fgmd : fetchGroups)
                            {
                                if (fgmd.getPostLoad().booleanValue())
                                {
                                    result = Boolean.TRUE;
                                    break;
                                }
                            }
                        }
                        if (!result)
                        {
                            if (plan.dynamicGroups != null)
                            {
                                // Dynamic Fetch groups
                                String fieldName = mmd.getName();
                                Class<?> cls = plan.clr.classForName(cmd.getFullClassName());
                                for (FetchGroup group : plan.dynamicGroups)
                                {
                                    Class<?> groupType = group.getType();
                                    if (groupType.isAssignableFrom(cls) && group.getMembers().contains(fieldName) && group.getPostLoad())
                                    {
                                        result = Boolean.TRUE;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (result == null)
            {
                result = Boolean.FALSE;
            }
            plan.cacheIsToCallPostLoadFetchPlan(cmd, cacheKey, result);
        }

        return result.booleanValue();
    }

    /**
     * Get all the (MetaData-based) fetch groups where this member number is included.
     * @param fgmds The Fetch Groups
     * @param memberNum the member absolute number
     * @return The Fetch Groups
     */
    private Set<FetchGroupMetaData> getFetchGroupsForMemberNumber(Set<FetchGroupMetaData> fgmds, int memberNum)
    {
        Set<FetchGroupMetaData> fetchGroups = new HashSet<>();
        if (fgmds != null)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(memberNum);
            for (FetchGroupMetaData fgmd : fgmds)
            {
                Set<FetchGroupMemberMetaData> fgmmds = fgmd.getMembers();
                if (fgmmds != null)
                {
                    for (FetchGroupMemberMetaData fgmmd : fgmmds)
                    {
                        if (fgmmd.getName().equals(mmd.getName()))
                        {
                            fetchGroups.add(fgmd);
                            break;
                        }
                    }
                }

                // Check any sub-groups
                Set<FetchGroupMetaData> subFGmds = fgmd.getFetchGroups();
                if (subFGmds != null)
                {
                    fetchGroups.addAll(getFetchGroupsForMemberNumber(subFGmds, memberNum));
                }
            }
        }
        return fetchGroups;
    }
}