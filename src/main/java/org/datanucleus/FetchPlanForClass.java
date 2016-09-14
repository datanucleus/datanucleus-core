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
import java.util.Iterator;
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

    /** 
     * Cache of fetch groups by member number, as calculating them in getFetchGroupsForMemberNumber() 
     * is O(n^2) Map<Integer, Set<FetchGroupMetaData>>
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
        plan.invalidateCachedIsToCallPostLoadFetchPlan(cmd);
    }

    FetchPlanForClass getCopy(FetchPlan fp)
    {
        FetchPlanForClass fpCopy = new FetchPlanForClass(cmd, fp);
        if (this.memberNumbers != null)
        {
            fpCopy.memberNumbers = new int[this.memberNumbers.length];
            for (int i = 0; i < fpCopy.memberNumbers.length; i++)
            {
                fpCopy.memberNumbers[i] = this.memberNumbers[i];
            }
        }
        fpCopy.dirty = this.dirty;
        return fpCopy;
    }

    /**
     * Method to return the effective depth of this member number in the overall fetch plan.
     * @param memberNum Number of member in this class
     * @return The (max) recursion depth
     */
    public int getMaxRecursionDepthForMember(int memberNum)
    {
        // prepare array of FetchGroupMetaData from current fetch plan
        Set<String> currentGroupNames = new HashSet(plan.getGroups());

        // find FetchGroupMetaDatas that contain the field in question
        Set<FetchGroupMetaData> fetchGroupsContainingField = getFetchGroupsForMemberNumber(cmd.getFetchGroupMetaData(currentGroupNames), memberNum);

        // find recursion depth for field in its class <field> definition
        int recursionDepth = cmd.getMetaDataForManagedMemberAtAbsolutePosition(memberNum).getRecursionDepth();
        if (recursionDepth == AbstractMemberMetaData.UNDEFINED_RECURSION_DEPTH)
        {
            recursionDepth = AbstractMemberMetaData.DEFAULT_RECURSION_DEPTH;
        }

        // find if it has been overridden in a <fetch-group> definition
        String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(memberNum).getName();
        for (Iterator<FetchGroupMetaData> iter = fetchGroupsContainingField.iterator(); iter.hasNext();)
        {
            FetchGroupMetaData fgmd = iter.next();
            Set<FetchGroupMemberMetaData> fgmmds = fgmd.getMembers();
            if (fgmmds != null)
            {
                for (FetchGroupMemberMetaData fgmmd : fgmmds)
                {
                    if (fgmmd.getName().equals(fieldName))
                    {
                        if (fgmmd.getRecursionDepth() != AbstractMemberMetaData.UNDEFINED_RECURSION_DEPTH)
                        {
                            recursionDepth = fgmmd.getRecursionDepth();
                        }
                    }
                }
            }
        }
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
            Iterator<FetchGroup> iter = plan.dynamicGroups.iterator();
            while (iter.hasNext())
            {
                FetchGroup grp = iter.next();
                if (grp.getType().getName().equals(cmd.getFullClassName()))
                {
                    // Dynamic fetch group applies
                    Set<String> members = grp.getMembers();
                    Iterator<String> membersIter = members.iterator();
                    while (membersIter.hasNext())
                    {
                        String memberName = membersIter.next();
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
                if (plan.groups.contains(fgmd.getName()))
                {
                    memberNumbers.or(getMemberNumbersForFetchGroup(fgmd));
                }
            }
        }

        if (plan.groups.contains(FetchPlan.DEFAULT))
        {
            setAsDefault(memberNumbers);
        }
        if (plan.groups.contains(FetchPlan.ALL))
        {
            setAsAll(memberNumbers);
        }
        if (plan.groups.contains(FetchPlan.NONE))
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
        for (int i = 0; i < cmd.getDFGMemberPositions().length; i++)
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
        for (int i = 0; i < cmd.getNoOfManagedMembers(); i++)
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
        for (int i = 0; i < cmd.getNoOfManagedMembers(); i++)
        {
            AbstractMemberMetaData fmd = cmd.getManagedMembers()[i];
            if (fmd.isPrimaryKey())
            {
                memberNums.set(fmd.getAbsoluteFieldNumber());
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
        
        if (result == null) 
        {
            result = Boolean.FALSE;
            int[] fieldsInActualFetchPlan = getMemberNumbers();
            for (int i = 0; i < fieldsInActualFetchPlan.length; i++)
            {
                final int fieldNumber = fieldsInActualFetchPlan[i];
                String fieldName = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber).getFullFieldName();
                // if field in actual fetch plan was not previously loaded
                if (!loadedMembers[fieldNumber])
                {
                    if (cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber).isDefaultFetchGroup() && plan.getGroups().contains(FetchPlan.DEFAULT))
                    {
                        // to call jdoPostLoad, field must be in default-fetch-group when DFG is active
                        result = Boolean.TRUE;
                    }
                    else
                    {
                        // compute only if necessary, as that's expensive to do
                        if (cmd.hasFetchGroupWithPostLoad())
                        {
                            // field must be in a fetch-group which has post-load set to true
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
                            for (Iterator it = fetchGroups.iterator(); it.hasNext();)
                            {
                                FetchGroupMetaData fgmd = (FetchGroupMetaData) it.next();
                                if (fgmd.getPostLoad().booleanValue())
                                {
                                    result = Boolean.TRUE;
                                }
                            }
                        }
                        
                        if (plan.dynamicGroups != null)
                        {
                            Class cls = plan.clr.classForName(cmd.getFullClassName());
                            for (Iterator<FetchGroup> it = plan.dynamicGroups.iterator(); it.hasNext();)
                            {
                                FetchGroup group = it.next();
                                Set groupMembers = group.getMembers();
                                if (group.getType().isAssignableFrom(cls) && groupMembers.contains(fieldName) && group.getPostLoad())
                                {
                                    result = Boolean.TRUE;
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
     * Get all the fetch groups where this member number is included.
     * @param fgmds The Fetch Groups
     * @param memberNum the member absolute number
     * @return The Fetch Groups
     */
    private Set<FetchGroupMetaData> getFetchGroupsForMemberNumber(Set<FetchGroupMetaData> fgmds, int memberNum)
    {
        Set<FetchGroupMetaData> fetchGroups = new HashSet();
        if (fgmds != null)
        {
            for (FetchGroupMetaData fgmd : fgmds)
            {
                Set<FetchGroupMemberMetaData> subFGmmds = fgmd.getMembers();
                if (subFGmmds != null)
                {
                    for (FetchGroupMemberMetaData subFGmmd : subFGmmds)
                    {
                        if (subFGmmd.getName().equals(cmd.getMetaDataForManagedMemberAtAbsolutePosition(memberNum).getName()))
                        {
                            fetchGroups.add(fgmd);
                        }
                    }
                }
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