/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.FetchGroupMemberMetaData;
import org.datanucleus.metadata.FetchGroupMetaData;

/**
 * Manager for dynamic fetch groups.
 * Manages a set of fetch groups with each FetchGroup for a particular class with a name.
 * 
 * This class is thread safe.
 */
public class FetchGroupManager
{
    /** Map of dynamic fetch groups, keyed by the group name. */
    private Map<String, Set<FetchGroup>> fetchGroupByName;

    /** Context that we are operating in. */
    private NucleusContext nucleusCtx;

    /**
     * Constructor for a FetchGroupManager for a particular Context.
     * @param ctx The Context
     */
    public FetchGroupManager(NucleusContext ctx)
    {
        this.nucleusCtx = ctx;
    }

    /**
     * Method to add a dynamic fetch group.
     * @param grp The fetch group
     */
    public synchronized void addFetchGroup(FetchGroup grp)
    {
        if (fetchGroupByName == null)
        {
            fetchGroupByName = new HashMap<>();
            Set<FetchGroup> coll = new HashSet<>();
            coll.add(grp);
            fetchGroupByName.put(grp.getName(), coll);
        }
        else
        {
            // Check for existing group with this name for this type
            Set<FetchGroup> coll = fetchGroupByName.get(grp.getName());
            if (coll != null)
            {
                // Check for existing entry for this name and class
                Iterator<FetchGroup> iter = coll.iterator();
                while (iter.hasNext())
                {
                    FetchGroup existingGrp = iter.next();
                    if (existingGrp.getName().equals(grp.getName()) && existingGrp.getType().getName().equals(grp.getType().getName()))
                    {
                        // Already have a group for this name+class so replace it
                        existingGrp.disconnectFromListeners(); // Remove the old group from use
                        iter.remove(); // Remove the old group
                    }
                }
                coll.add(grp);
            }
            else
            {
                coll = new HashSet<>();
                coll.add(grp);
                fetchGroupByName.put(grp.getName(), coll);
            }
        }
    }

    /**
     * Method to remove a dynamic FetchGroup from use.
     * @param grp The group
     */
    public synchronized void removeFetchGroup(FetchGroup grp)
    {
        if (fetchGroupByName != null)
        {
            Collection<FetchGroup> coll = fetchGroupByName.get(grp.getName());
            if (coll != null)
            {
                Iterator<FetchGroup> iter = coll.iterator();
                while (iter.hasNext())
                {
                    FetchGroup existingGrp = iter.next();
                    if (existingGrp.getType() == grp.getType())
                    {
                        existingGrp.disconnectFromListeners(); // Remove the group from use
                        iter.remove();
                    }
                }
            }
        }
    }

    /**
     * Accessor for a fetch group for the specified class.
     * If the fetch group of this name for this class doesn't exist then will create one if the flag is set.
     * @param cls The class
     * @param name Name of the group
     * @param createIfNotPresent Whether this method should create+add a FetchGroup if on with this name isn't found.
     * @return The FetchGroup
     * @param <T> Type that the FetchGroup is for
     */
    public synchronized <T> FetchGroup<T> getFetchGroup(Class<T> cls, String name, boolean createIfNotPresent)
    {
        if (fetchGroupByName != null)
        {
            Collection<FetchGroup> coll = fetchGroupByName.get(name);
            if (coll != null)
            {
                for (FetchGroup grp : coll)
                {
                    if (grp.getType() == cls)
                    {
                        return grp;
                    }
                }
            }
        }

        if (createIfNotPresent)
        {
            // Create a new group and add to our managed list
            FetchGroup<T> grp = createFetchGroup(cls, name);
            addFetchGroup(grp);
            return grp;
        }
        return null;
    }

    /**
     * Method to create a new FetchGroup for the class and name.
     * <b>Doesn't add it to the internally managed groups.</b>
     * @param cls The class
     * @param name Name of the group
     * @return The FetchGroup
     * @param <T> Type that the FetchGroup is for
     */
    public <T> FetchGroup<T> createFetchGroup(Class<T> cls, String name)
    {
        // Not present so create a new FetchGroup and add it
        FetchGroup<T> fg = new FetchGroup<T>(nucleusCtx, name, cls);
        if (name.equals(FetchGroup.DEFAULT))
        {
            // Special case of wanting to create a group to override the DFG
            fg.addCategory(FetchGroup.DEFAULT);
        }
        else
        {
            // Check if this class has a named FetchGroup of this name, so we start from the same members
            ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(cls.getClassLoader());
            AbstractClassMetaData cmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(cls, clr);
            if (cmd != null)
            {
                FetchGroupMetaData fgmd = cmd.getFetchGroupMetaData(name);
                if (fgmd != null)
                {
                    Set<FetchGroupMemberMetaData> fgmmds = fgmd.getMembers();
                    for (FetchGroupMemberMetaData fgmmd : fgmmds)
                    {
                        fg.addMember(fgmmd.getName());
                        if (fgmmd.getRecursionDepth() != 1)
                        {
                            fg.setRecursionDepth(fgmmd.getName(), fgmmd.getRecursionDepth());
                        }
                    }
                }
            }
        }
        return fg;
    }

    /**
     * Accessor for the fetch groups for the specified name. Used by FetchPlan to find FetchGroups for a specific name.
     * @param name Name of the group
     * @return The FetchGroup
     */
    public synchronized Set<FetchGroup> getFetchGroupsWithName(String name)
    {
        if (fetchGroupByName != null)
        {
            Collection<FetchGroup> coll = fetchGroupByName.get(name);
            if (coll != null)
            {
                return new HashSet<>(coll);
            }
        }
        return null;
    }

    /**
     * Clear out all fetch groups from use by this manager.
     */
    public synchronized void clearFetchGroups()
    {
        if (fetchGroupByName != null)
        {
            for (Set<FetchGroup> fgrps : fetchGroupByName.values())
            {
                for (FetchGroup grp : fgrps)
                {
                    grp.disconnectFromListeners(); // Remove the group from use
                }
            }

            fetchGroupByName.clear();
        }
    }
}