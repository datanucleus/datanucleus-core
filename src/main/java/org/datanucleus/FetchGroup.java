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
package org.datanucleus;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Group of fields for fetching, to be part of a FetchPlan.
 * Defined at runtime, via the API.
 */
public class FetchGroup implements Serializable
{
    /** Localisation utility for output messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    public static final String DEFAULT = "default";
    public static final String RELATIONSHIP = "relationship";
    public static final String MULTIVALUED = "multivalued";
    public static final String BASIC = "basic";
    public static final String ALL = "all";

    /** Context. */
    private NucleusContext nucleusCtx;

    /** Name of the group. */
    private String name;

    /** The class that this group is for. */
    private Class cls;

    /** Whether the postLoad callback is to be called when this group is loaded. */
    private boolean postLoad = false;

    /** Names of the fields/properties of the class that are part of this group. */
    private Set<String> memberNames = new HashSet();

    /** Map of recursion depth, keyed by the member name. Only has entries when not using default. */
    private Map<String, Integer> recursionDepthByMemberName = null;

    /** FetchPlans listening to this group for changes. */
    private Collection<FetchPlan> planListeners = null;

    /** Whether this group can be modified. */
    private boolean unmodifiable = false;

    /**
     * Constructor.
     * @param nucleusCtx Context
     * @param name Name of the group
     * @param cls The class
     */
    public FetchGroup(NucleusContext nucleusCtx, String name, Class cls)
    {
        this.nucleusCtx = nucleusCtx;
        this.name = name;
        this.cls = cls;
    }

    /**
     * Constructor to take a copy of the supplied group, but modifiable.
     * @param grp The existing group
     */
    public FetchGroup(FetchGroup grp)
    {
        name = grp.name;
        cls = grp.cls;
        nucleusCtx = grp.nucleusCtx;
        postLoad = grp.postLoad;

        for (String memberName : grp.memberNames)
        {
            addMember(memberName);
        }

        if (grp.recursionDepthByMemberName != null)
        {
            recursionDepthByMemberName = new HashMap(grp.recursionDepthByMemberName);
        }
    }

    /**
     * Accessor for the group name.
     * @return Name of the group
     */
    public String getName()
    {
        return name;
    }

    /**
     * Accessor for the class that this group is for.
     * @return the class
     */
    public Class getType()
    {
        return cls;
    }

    /**
     * Mutator for whether the postLoad callback should be called on loading this fetch group.
     * @param postLoad Whether the postLoad callback should be called.
     */
    public void setPostLoad(boolean postLoad)
    {
        assertUnmodifiable();

        this.postLoad  = postLoad;
    }

    /**
     * Accessor for whether to call postLoad when this group is loaded.
     * @return Whether to call postLoad
     */
    public boolean getPostLoad()
    {
        return postLoad;
    }

    /**
     * Accessor for the recursion depth for the specified field/property.
     * @param memberName Name of field/property
     * @return The recursion depth
     */
    public int getRecursionDepth(String memberName)
    {
        if (recursionDepthByMemberName != null)
        {
            Integer recursionValue = recursionDepthByMemberName.get(memberName);
            if (recursionValue != null)
            {
                return recursionValue.intValue();
            }
        }
        return 1; // Default
    }

    /**
     * Method to set the recursion depth for the specified field/property.
     * @param memberName Name of field/property
     * @param recursionDepth Recursion depth
     * @return The fetch group
     */
    public FetchGroup setRecursionDepth(String memberName, int recursionDepth)
    {
        assertUnmodifiable();
        assertNotMember(memberName);

        if (memberNames.contains(memberName))
        {
            if (recursionDepthByMemberName == null)
            {
                recursionDepthByMemberName = new HashMap();
            }
            recursionDepthByMemberName.put(memberName, Integer.valueOf(recursionDepth));
        }
        return this;
    }

    /**
     * Method to make the group unmodifiable.
     * Once unmodifiable it cannot be made modifiable again.
     * @return This group
     */
    public FetchGroup setUnmodifiable()
    {
        if (!unmodifiable)
        {
            unmodifiable = true;
        }
        return this;
    }

    /**
     * Accessor for modifiability status of this group.
     * @return Whether it is no longer modifiable
     */
    public boolean isUnmodifiable()
    {
        return unmodifiable;
    }

    /**
     * Convenience method to add the members for the specified category.
     * Supports the categories defined in the JDO2.2 spec.
     * @param categoryName Name of the category
     * @return This group
     */
    public FetchGroup addCategory(String categoryName)
    {
        assertUnmodifiable();

        String[] memberNames = getMemberNamesForCategory(categoryName);
        if (memberNames != null)
        {
            for (int i=0;i<memberNames.length;i++)
            {
                this.memberNames.add(memberNames[i]);
            }
            notifyListeners();
        }
        return this;
    }

    /**
     * Convenience method to remove the members for the specified category.
     * Supports the categories defined in the JDO2.2 spec.
     * @param categoryName Name of the category
     * @return This group
     */
    public FetchGroup removeCategory(String categoryName)
    {
        assertUnmodifiable();

        String[] memberNames = getMemberNamesForCategory(categoryName);
        if (memberNames != null)
        {
            for (int i=0;i<memberNames.length;i++)
            {
                this.memberNames.remove(memberNames[i]);
            }
            notifyListeners();
        }
        return this;
    }

    /**
     * Convenience accessor to return the member names for the specified category name.
     * @param categoryName Name of the category
     * @return The member names
     */
    private String[] getMemberNamesForCategory(String categoryName)
    {
        AbstractClassMetaData acmd = getMetaDataForClass();
        int[] memberPositions = null;
        if (categoryName.equals(DEFAULT))
        {
            memberPositions = acmd.getDFGMemberPositions();
        }
        else if (categoryName.equals(ALL))
        {
            memberPositions = acmd.getAllMemberPositions();
        }
        else if (categoryName.equals(BASIC))
        {
            memberPositions = acmd.getBasicMemberPositions(nucleusCtx.getClassLoaderResolver(null), nucleusCtx.getMetaDataManager());
        }
        else if (categoryName.equals(RELATIONSHIP))
        {
            memberPositions = acmd.getRelationMemberPositions(nucleusCtx.getClassLoaderResolver(null), nucleusCtx.getMetaDataManager());
        }
        else if (categoryName.equals(MULTIVALUED))
        {
            memberPositions = acmd.getMultivaluedMemberPositions();
        }
        else
        {
            throw nucleusCtx.getApiAdapter().getUserExceptionForException(
                "Category " + categoryName + " is invalid", null);
        }

        String[] names = new String[memberPositions.length];
        for (int i=0;i<memberPositions.length;i++)
        {
            names[i] = acmd.getMetaDataForManagedMemberAtAbsolutePosition(memberPositions[i]).getName();
        }
        return names;
    }

    /**
     * Accessor for the members that are in this fetch group.
     * @return Set of member names.
     */
    public Set<String> getMembers()
    {
        return memberNames;
    }

    /**
     * Method to add a field of the class to the fetch group.
     * @param memberName Name of the field/property
     * @return This FetchGroup
     * @throws NucleusUserException if the field/property doesn't exist for this class
     */
    public FetchGroup addMember(String memberName)
    {
        assertUnmodifiable();
        assertNotMember(memberName);

        this.memberNames.add(memberName);
        notifyListeners();
        return this;
    }

    /**
     * Method to remove a field of the class from the fetch group.
     * @param memberName Name of the field/property
     * @return This FetchGroup
     * @throws NucleusUserException if the field/property doesn't exist for this class
     */
    public FetchGroup removeMember(String memberName)
    {
        assertUnmodifiable();
        assertNotMember(memberName);

        this.memberNames.remove(memberName);
        notifyListeners();
        return this;
    }

    public FetchGroup addMembers(String[] members)
    {
        if (members == null)
        {
            return this;
        }

        for (int i=0;i<members.length;i++)
        {
            addMember(members[i]);
        }
        notifyListeners();
        return this;
    }

    public FetchGroup removeMembers(String[] members)
    {
        if (members == null)
        {
            return this;
        }

        for (int i=0;i<members.length;i++)
        {
            removeMember(members[i]);
        }
        notifyListeners();
        return this;
    }

    /**
     * Method to notify all FetchPlan listeners that this group has changed.
     */
    private void notifyListeners()
    {
        if (planListeners != null)
        {
            Iterator<FetchPlan> iter = planListeners.iterator();
            while (iter.hasNext())
            {
                iter.next().notifyFetchGroupChange(this);
            }
        }
    }

    /**
     * Method to register a listener for changes to this FetchGroup.
     * @param plan The FetchPlan that is listening
     */
    public void registerListener(FetchPlan plan)
    {
        if (planListeners == null)
        {
            planListeners = new HashSet<FetchPlan>();
        }
        planListeners.add(plan);
    }

    /**
     * Method to deregister a listener for changes to this FetchGroup.
     * @param plan The FetchPlan that is no longer listening
     */
    public void deregisterListener(FetchPlan plan)
    {
        if (planListeners != null)
        {
            planListeners.remove(plan);
        }
    }

    /**
     * Method to disconnect this fetch group from all listeners since the group is removed from use.
     */
    public void disconnectFromListeners()
    {
        if (planListeners != null)
        {
            Iterator<FetchPlan> iter = planListeners.iterator();
            while (iter.hasNext())
            {
                iter.next().notifyFetchGroupRemove(this);
            }
            planListeners.clear();
            planListeners = null;
        }
    }

    /**
     * Method to throw an exception if the fetch group is currently unmodifiable.
     * @throw NucleusUserException
     */
    private void assertUnmodifiable()
    {
        if (unmodifiable)
        {
            throw nucleusCtx.getApiAdapter().getUserExceptionForException("FetchGroup is not modifiable!", null);
        }
    }

    /**
     * Method to throw an exception if the specified member is not a member of this class.
     * @param memberName Name of the field/property
     * @throws NucleusUserException
     */
    private void assertNotMember(String memberName)
    {
        AbstractClassMetaData acmd = getMetaDataForClass();
        if (!acmd.hasMember(memberName))
        {
            throw nucleusCtx.getApiAdapter().getUserExceptionForException(
                LOCALISER.msg("006004", memberName, cls.getName()), null);
        }
    }

    private AbstractClassMetaData getMetaDataForClass()
    {
        AbstractClassMetaData acmd = null;
        if (cls.isInterface())
        {
            // Persistent interface
            acmd = nucleusCtx.getMetaDataManager().getMetaDataForInterface(cls, nucleusCtx.getClassLoaderResolver(null));
        }
        else
        {
            // Persistence class
            acmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(cls, nucleusCtx.getClassLoaderResolver(null));
        }
        return acmd;
    }

    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof FetchGroup))
        {
            return false;
        }
        FetchGroup other = (FetchGroup)obj;
        if (other.cls != cls || !other.name.equals(name))
        {
            return false;
        }
        return true;
    }

    public int hashCode()
    {
        return name.hashCode() ^ cls.hashCode();
    }

    public String toString()
    {
        return "FetchGroup : " + name + " for " + cls.getName() + 
            " members=" + StringUtils.collectionToString(memberNames) +
            ", modifiable=" + (!unmodifiable) +
            ", postLoad=" + postLoad +
            ", listeners.size=" + (planListeners != null ? planListeners.size() : 0);
    }
}