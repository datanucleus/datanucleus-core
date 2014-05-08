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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.datanucleus.util.MultiMap;

/**
 * Manager for dynamic fetch groups.
 * Manages a set of fetch groups with each FetchGroup for a particular class with a name.
 * 
 * This class is thread safe.
 */
public class FetchGroupManager
{
    /** Map of dynamic fetch groups, keyed by the group name. */
    private MultiMap fetchGroupByName;

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
            fetchGroupByName = new MultiMap();
        }

        // Check for existing group with this name for this type
        Collection coll = (Collection)fetchGroupByName.get(grp.getName());
        if (coll != null)
        {
            // Check for existing entry for this name and class
            Iterator iter = coll.iterator();
            while (iter.hasNext())
            {
                FetchGroup existingGrp = (FetchGroup)iter.next();
                if (existingGrp.getName().equals(grp.getName()) && 
                    existingGrp.getType().getName().equals(grp.getType().getName()))
                {
                    // Already have a group for this name+class so replace it
                    existingGrp.disconnectFromListeners(); // Remove the old group from use
                    iter.remove(); // Remove the old group
                }
            }
        }

        fetchGroupByName.put(grp.getName(), grp);
    }

    /**
     * Method to remove a dynamic FetchGroup from use.
     * @param grp The group
     */
    public synchronized void removeFetchGroup(FetchGroup grp)
    {
        if (fetchGroupByName != null)
        {
            Collection coll = (Collection) fetchGroupByName.get(grp.getName());
            if (coll != null)
            {
                Iterator iter = coll.iterator();
                while (iter.hasNext())
                {
                    Object obj = iter.next();
                    FetchGroup existingGrp = (FetchGroup)obj;
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
     * If the fetch group of this name for this class doesn't exist then will create one.
     * @param cls The class
     * @param name Name of the group
     * @return The FetchGroup
     */
    public synchronized FetchGroup getFetchGroup(Class cls, String name)
    {
        if (fetchGroupByName != null)
        {
            Collection coll = (Collection) fetchGroupByName.get(name);
            if (coll != null)
            {
                Iterator iter = coll.iterator();
                while (iter.hasNext())
                {
                    FetchGroup grp = (FetchGroup)iter.next();
                    if (grp.getType() == cls)
                    {
                        return grp;
                    }
                }
            }
        }

        // Create a new group and add to our managed list
        FetchGroup grp = createFetchGroup(cls, name);
        addFetchGroup(grp);
        return grp;
    }

    /**
     * Method to create a new FetchGroup for the class and name.
     * Doesn't add it to the internally managed groups.
     * @param cls The class
     * @param name Name of the group
     * @return The FetchGroup
     */
    public FetchGroup createFetchGroup(Class cls, String name)
    {
        // Not present so create a new FetchGroup and add it
        return new FetchGroup(nucleusCtx, name, cls);
    }

    /**
     * Accessor for the fetch groups for the specified name.
     * @param name Name of the group
     * @return The FetchGroup
     */
    public synchronized Set<FetchGroup> getFetchGroupsWithName(String name)
    {
        if (fetchGroupByName != null)
        {
            Collection coll = (Collection) fetchGroupByName.get(name);
            if (coll != null)
            {
                return new HashSet(coll);
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
            Collection fetchGroups = fetchGroupByName.values();
            Iterator iter = fetchGroups.iterator();
            while (iter.hasNext())
            {
                FetchGroup grp = (FetchGroup)iter.next();
                grp.disconnectFromListeners(); // Remove the group from use
            }
            fetchGroupByName.clear();
        }
    }
}