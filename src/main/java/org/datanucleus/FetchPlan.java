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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.util.ConcurrentReferenceHashMap;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;
import org.datanucleus.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * FetchPlan for fields for use internally.
 * A FetchPlan has a series of FetchPlanForClass objects being the fetch plan for particular classes.
 * Each FetchPlanForClass defines a series of fields of that class that are part of the fetch plan.
 * There are two types of fetch groups under consideration here.
 * <ul>
 * <li>Static fetch groups, defined in MetaData (XML/Annotations).</li>
 * <li>Dynamic fetch groups, defined via an API.</li>
 * </ul>
 */
public class FetchPlan implements Serializable
{
    private static final long serialVersionUID = 6031608568703439025L;

    /** Constant defining the fields in the default fetch group. */
    public static final String DEFAULT = "default";

    /** Constant defining all fields */
    public static final String ALL = "all";

    /** Constant defing no fields. */
    public static final String NONE = "none";

    /** Members that are loaded but not in current fetch plan should be unloaded before detachment. */
    public static final int DETACH_UNLOAD_FIELDS = 2;

    /** Members that are not loaded but are in the current fetch plan should be loaded before detachment. */
    public static final int DETACH_LOAD_FIELDS = 1;

    /** Fetch size to load all possible. */
    public static final int FETCH_SIZE_GREEDY = -1;

    /** Fetch size for the implementation to decide how many to load. */
    public static final int FETCH_SIZE_OPTIMAL = 0;

    public static final int RECURSION_DEPTH_UNLIMITED = -1;
    public static final int RECURSION_DEPTH_FK_ONLY = 0;

    /** Execution Context that this FetchPlan relates to. */
    transient final ExecutionContext ec; // Defined as transient to avoid Serializable problems

    /** ClassLoader resolver. */
    transient final ClassLoaderResolver clr; // Defined as transient to avoid Serializable problems

    /** Names of the "defined" fetch groups in the current FetchPlan. */
    final Set<String> groupNames = new HashSet<>();

    /** The "dynamic" fetch groups in the current FetchPlan. */
    transient Set<FetchGroup> dynamicGroups = null; // Defined as transient to avoid Serializable problems

    /** The Fetch size. For use when using large result sets. */
    int fetchSize = FETCH_SIZE_OPTIMAL;

    /** Options to be used during detachment. Spec 12.7 says that the default is DETACH_LOAD_FIELDS. */
    int detachmentOptions = FetchPlan.DETACH_LOAD_FIELDS;

    /** FetchPlanForClass keyed by the class name. **/
    final transient Map<String, FetchPlanForClass> fetchPlansByClassName = new HashMap<>();

    /** Maximum depth to fetch from the root object. */
    int maxFetchDepth = 1;

    /** The classes used as the roots for detachment (DetachAllOnCommit). */
    Class[] detachmentRootClasses = null;

    /** The instances used as the roots for detachment (DetachAllOnCommit). */
    Collection<Object> detachmentRoots = null;

    /**
     * Constructor. Initially has the default fetch group.
     * @param ec execution context
     * @param clr ClassLoader Resolver
     */
    public FetchPlan(ExecutionContext ec, ClassLoaderResolver clr)
    {
        this.ec = ec;
        this.clr = clr;
        groupNames.add(FetchPlan.DEFAULT);

        // Extension property to define the default detachmentOptions
        String flds = ec.getNucleusContext().getConfiguration().getStringProperty(PropertyNames.PROPERTY_DETACH_DETACHMENT_FIELDS);
        if (flds != null)
        {
            flds = flds.toLowerCase();
            if (flds.equals("load-unload-fields"))
            {
                detachmentOptions = FetchPlan.DETACH_LOAD_FIELDS | FetchPlan.DETACH_UNLOAD_FIELDS;
            }
            else if (flds.equals("unload-fields"))
            {
                detachmentOptions = FetchPlan.DETACH_UNLOAD_FIELDS;
            }
            else if (flds.equals("load-fields"))
            {
                detachmentOptions = FetchPlan.DETACH_LOAD_FIELDS;
            }
        }
    }

    /**
     * Mark all managed fetch plans to be dirty, so the active members need to be recomputed.
     */
    private void markDirty()
    {
        for (FetchPlanForClass fpCls : fetchPlansByClassName.values())
        {
            fpCls.markDirty();
        }
    }

    /**
     * Access the fetch plan for the class.
     * @param cmd metadata for the class
     * @return the FetchPlanForClass
     */
    public synchronized FetchPlanForClass getFetchPlanForClass(AbstractClassMetaData cmd)
    {
        FetchPlanForClass fpClass = fetchPlansByClassName.get(cmd.getFullClassName());
        if (fpClass == null)
        {
            fpClass = new FetchPlanForClass(cmd, this);
            fetchPlansByClassName.put(cmd.getFullClassName(), fpClass);
        }
        return fpClass;
    }

    /**
     * Method to add a group to the fetch plan.
     * @param grpName The fetch group to add
     * @return Updated Fetch Plan
     */
    public synchronized FetchPlan addGroup(String grpName)
    {
        if (grpName != null)
       {
            boolean changed = groupNames.add(grpName);
            boolean dynChanged = addDynamicGroup(grpName);
            if (changed || dynChanged)
            {
                markDirty();
            }
        }
        return this;
    }

    /**
     * Method to remove a group from the fetch plan.
     * @param grpName The fetch group to remove
     * @return Updated Fetch Plan
     */
    public synchronized FetchPlan removeGroup(String grpName)
    {
        if (grpName != null)
        {
            boolean changed = false;
            changed = groupNames.remove(grpName);
            if (dynamicGroups != null)
            {
                Iterator<FetchGroup> iter = dynamicGroups.iterator();
                while (iter.hasNext())
                {
                    FetchGroup grp = iter.next();
                    if (grp.getName().equals(grpName))
                    {
                        grp.deregisterListener(this); // Deregister us from this group
                        changed = true;
                        iter.remove();
                    }
                }
            }
            if (changed)
            {
                markDirty();
            }
        }

        return this;
    }

    /**
     * Method to clear the current groups and activate the DFG.
     * @return The FetchPlan
     */
    public synchronized FetchPlan clearGroups()
    {
        clearDynamicGroups();
        groupNames.clear();
        markDirty();
        return this;
    }

    /**
     * Accessor for the static groups for this FetchPlan.
     * Doesn't return the dynamic groups.
     * @return The fetch plan groups (unmodifiable)
     */
    public synchronized Set<String> getGroups()
    {
        return Collections.unmodifiableSet(new HashSet<String>(groupNames));
    }

    /**
     * Method to set the groups of the fetch plan.
     * @param grpNames Names of the groups
     * @return Updated Fetch Plan
     */
    public synchronized FetchPlan setGroups(Collection<String> grpNames)
    {
        clearDynamicGroups();
        groupNames.clear();

        if (grpNames != null)
        {
            groupNames.addAll(grpNames);

            for (String grpName : grpNames)
            {
                addDynamicGroup(grpName);
            }
        }

        markDirty();
        return this;
    }

    /**
     * Method to set the groups using an array.
     * @param grpNames Names of the groups
     * @return The Fetch Plan
     */
    public synchronized FetchPlan setGroups(String[] grpNames)
    {
        clearDynamicGroups();
        groupNames.clear();

        if (grpNames != null)
        {
            for (int i=0;i<grpNames.length;i++)
            {
                groupNames.add(grpNames[i]);
            }
            for (int i=0;i<grpNames.length;i++)
            {
                addDynamicGroup(grpNames[i]);
            }
        }

        markDirty();
        return this;
    }

    /**
     * Method to set the fetch group.
     * @param grpName Name of the group
     * @return The Fetch Plan
     */
    public synchronized FetchPlan setGroup(String grpName)
    {
        clearDynamicGroups();
        groupNames.clear();

        if (grpName != null)
        {
            groupNames.add(grpName);
            addDynamicGroup(grpName);
        }

        markDirty();
        return this;
    }

    /**
     * Convenience method to clear all dynamic groups.
     */
    private void clearDynamicGroups()
    {
        if (dynamicGroups != null)
        {
            for (FetchGroup grp : dynamicGroups)
            {
                grp.deregisterListener(this);
            }
            dynamicGroups.clear();
        }
    }

    /**
     * Convenience method to add dynamic fetch groups for the specified name.
     * @param grpName Name of group
     * @return Whether the groups were changed
     */
    private boolean addDynamicGroup(String grpName)
    {
        boolean changed = false;

        // Check for group registered with ExecutionContext
        Set<FetchGroup> ecGrpsWithName = ec.getFetchGroupsWithName(grpName);
        if (ecGrpsWithName != null)
        {
            if (dynamicGroups == null)
            {
                dynamicGroups = new HashSet<>();
            }
            for (FetchGroup grp : ecGrpsWithName)
            {
                dynamicGroups.add(grp);
                grp.registerListener(this); // Register us with this group
                changed = true;
            }
        }

        if (!changed)
        {
            // Check for group registered with NucleusContext
            Set<FetchGroup> grpsWithName = ec.getNucleusContext().getFetchGroupsWithName(grpName);
            if (grpsWithName != null)
            {
                if (dynamicGroups == null)
                {
                    dynamicGroups = new HashSet<>();
                }
                for (FetchGroup grp : grpsWithName)
                {
                    dynamicGroups.add(grp);
                    grp.registerListener(this); // Register us with this group
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Method to notify this FetchPlan that the specified FetchGroup has been updated.
     * <B>dynamic fetch groups extension</B>
     * @param group The dynamic FetchGroup
     */
    public void notifyFetchGroupChange(FetchGroup group)
    {
        Collection<FetchPlanForClass> fpClasses = fetchPlansByClassName.values();
        for (FetchPlanForClass fpClass : fpClasses)
        {
            Class<?> cls = clr.classForName(fpClass.cmd.getFullClassName());
            Class<?> groupType = group.getType();
            if (cls.isAssignableFrom(group.getType()) || groupType.isAssignableFrom(cls))
            {
                // Mark all potentially related fetch plans dirty so they recalculate
                fpClass.markDirty();
            }
        }
    }

    /**
     * Method to notify this FetchPlan that the specified FetchGroup has been updated.
     * <B>dynamic fetch groups extension</B>
     * @param group The dynamic FetchGroup
     */
    public void notifyFetchGroupRemove(FetchGroup group)
    {
        dynamicGroups.remove(group); // Remove the group
        notifyFetchGroupChange(group); // Recalculate all groups fields
    }

    /**
     * Set the roots for DetachAllOnCommit
     * @param roots The roots of the detachment graph.
     * @return The fetch plan with these roots
     */
    public FetchPlan setDetachmentRoots(Collection<Object> roots)
    {
        if (detachmentRootClasses != null || detachmentRoots != null)
        {
            throw new NucleusUserException(Localiser.msg("006003"));
        }

        if (roots == null)
        {
            detachmentRoots = null;
        }

        detachmentRoots = new ArrayList<>();
        detachmentRoots.addAll(roots);
        return this;
    }

    /**
     * Accessor for the roots of the detachment graph for DetachAllOnCommit.
     * @return The roots of the detachment graph.
     */
    public Collection<Object> getDetachmentRoots()
    {
        if (detachmentRoots == null)
        {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableCollection(detachmentRoots);
    }

    /**
     * Set the classes used for roots of the detachment graph for DetachAllOnCommit.
     * @param rootClasses Classes to be used as roots of the detachment graph
     * @return The fetch plan with these roots
     */
    public FetchPlan setDetachmentRootClasses(Class[] rootClasses)
    {
        if (detachmentRootClasses != null || detachmentRoots != null)
        {
            throw new NucleusUserException(Localiser.msg("006003"));
        }

        if (rootClasses == null)
        {
            detachmentRootClasses = null;
            return this;
        }

        detachmentRootClasses = new Class[rootClasses.length];
        for (int i=0;i<rootClasses.length;i++)
        {
            detachmentRootClasses[i] = rootClasses[i];
        }

        return this;
    }

    /**
     * Accessor for the root classes of the detachment graph for DetachAllOnCommit.
     * @return The classes to be used as the root of the detachment graph.
     */
    public Class[] getDetachmentRootClasses()
    {
        if (detachmentRootClasses == null)
        {
            return new Class[0];
        }

        return detachmentRootClasses;
    }

    /**
     * Method called at commit() to clear out the detachment roots.
     */
    void resetDetachmentRoots()
    {
        detachmentRootClasses = null;
        detachmentRoots = null;
    }

    /**
     * Mutator for the maximum fetch depth where -1 implies no restriction on the fetch depth.
     * Throws NucleusUserException when an illegal value (either 0 or &le; -2)
     * @param max The maximum fetch depth to fetch to
     * @return The FetchPlan
     */
    public synchronized FetchPlan setMaxFetchDepth(int max)
    {
        if (max == 0 || max < -1)
        {
            throw new NucleusUserException(Localiser.msg("006002", max));
        }
        this.maxFetchDepth = max;
        return this;
    }

    /**
     * Accessor for the maximum fetch depth.
     * @return The maximum fetch depth
     */
    public synchronized int getMaxFetchDepth()
    {
        return maxFetchDepth;
    }

    /**
     * Method to set the fetch size when using large result sets.
     * @param fetchSize the size
     * @return Updated Fetch Plan
     */
    public synchronized FetchPlan setFetchSize(int fetchSize)
    {
        if (fetchSize != FETCH_SIZE_GREEDY && fetchSize != FETCH_SIZE_OPTIMAL && fetchSize < 0)
        {
            // Invalid fetch size so just return
            return this;
        }
        this.fetchSize = fetchSize;
        return this;
    }

    /**
     * Accessor for the fetch size when using large result sets.
     * @return The size
     */
    public synchronized int getFetchSize()
    {
        return fetchSize;
    }

    /**
     * Return the options to be used at detachment.
     * @return Detachment options
     */
    public int getDetachmentOptions()
    {
        return detachmentOptions;
    }

    /**
     * Set the options to be used at detachment.
     * @param options The options
     * @return The updated fetch plan.
     */
    public FetchPlan setDetachmentOptions(int options)
    {
        detachmentOptions = options;
        return this;
    }

    /**
     * Returns a copy of this FetchPlan with all settings initialized.
     * Used when a Query has to have its own FetchPlan, so takes a copy of that of the ExecutionContext.
     * @return the FetchPlan copy
     */
    public synchronized FetchPlan getCopy()
    {
        FetchPlan fp = new FetchPlan(ec, clr); // Includes DEFAULT
        fp.maxFetchDepth = maxFetchDepth;
        fp.groupNames.remove(FetchPlan.DEFAULT);
        fp.groupNames.addAll(this.groupNames);
        if (dynamicGroups != null)
        {
            fp.dynamicGroups = new HashSet<>(dynamicGroups);
        }

        for (Map.Entry<String, FetchPlanForClass> entry : this.fetchPlansByClassName.entrySet())
        {
            String className = entry.getKey();
            FetchPlanForClass fpcls = entry.getValue();
            fp.fetchPlansByClassName.put(className, fpcls.getCopy(fp));
        }
        fp.fetchSize = this.fetchSize;
        return fp;
    }

    /**
     * Cache the result of FetchPlanImpl.isToCallPostLoadFetchPlan():
     * for a given set of loaded members of a certain class. Must be
     * invalidated with any change of fields to load, e.g. adding a fetchgroup.
     */
    private transient Map<AbstractClassMetaData, Map<BitSet, Boolean>> isToCallPostLoadFetchPlanByCmd;

    Boolean getCachedIsToCallPostLoadFetchPlan(AbstractClassMetaData cmd, BitSet loadedFields) 
    {
        if (isToCallPostLoadFetchPlanByCmd == null)
        {
            isToCallPostLoadFetchPlanByCmd = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.SOFT);
        }
        Map cachedIsToCallPostLoadFetchPlan = isToCallPostLoadFetchPlanByCmd.get(cmd);
        if (cachedIsToCallPostLoadFetchPlan==null)
        {
            return null;
        }
        return (Boolean) cachedIsToCallPostLoadFetchPlan.get(loadedFields);
    }

    void cacheIsToCallPostLoadFetchPlan(AbstractClassMetaData cmd, BitSet loadedFields, Boolean itcplfp)
    {
        if (isToCallPostLoadFetchPlanByCmd == null)
        {
            isToCallPostLoadFetchPlanByCmd = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.SOFT);
        }
        Map<BitSet, Boolean> cachedIsToCallPostLoadFetchPlan = isToCallPostLoadFetchPlanByCmd.get(cmd);
        if (cachedIsToCallPostLoadFetchPlan == null)
        {
            cachedIsToCallPostLoadFetchPlan = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.SOFT);
            isToCallPostLoadFetchPlanByCmd.put(cmd, cachedIsToCallPostLoadFetchPlan);
        }
        cachedIsToCallPostLoadFetchPlan.put(loadedFields, itcplfp);
    }

    void invalidateCachedIsToCallPostLoadFetchPlan(AbstractClassMetaData cmd)
    {
        if (isToCallPostLoadFetchPlanByCmd == null)
        {
            isToCallPostLoadFetchPlanByCmd = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.SOFT);
        }
        Map<BitSet, Boolean> cachedIsToCallPostLoadFetchPlan = isToCallPostLoadFetchPlanByCmd.get(cmd);
        if (cachedIsToCallPostLoadFetchPlan != null)
        {
            cachedIsToCallPostLoadFetchPlan.clear();
        }
    }

    public String toStringWithClasses()
    {
        return "FetchPlan " + groupNames.toString() + " classes=" + StringUtils.collectionToString(Collections.unmodifiableCollection(fetchPlansByClassName.values()));
    }

    public String toString()
    {
        return "FetchPlan " + groupNames.toString();
    }
}