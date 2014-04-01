/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
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
2004 Andy Jefferson - changed to extend MetaData
2004 Andy Jefferson - added toString()
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.util.HashSet;
import java.util.Set;

/**
 * A fetch group defines a particular loaded state for an object graph. 
 * It specifies fields/properties to be loaded for all of the instances in the graph when
 * this fetch group is active.
 */
public class FetchGroupMetaData extends MetaData
{
    /**
     * The post-load attribute on the fetch-group element indicates whether the
     * jdoPost-Load callback will be made when the fetch group is loaded. It
     * defaults to false, for all fetch groups except the default fetch group,
     * on which it defaults to true.
     */
    boolean postLoad = false;

    /**
     * The name attribute on a field element contained within a fetch-group
     * element is the name of field in the enclosing class or a dot-separated
     * expression identifying a field reachable from the class by navigating a
     * reference, collection or map. For maps of persistencecapable classes
     * "#key" or "#value" may be appended to the name of the map field to
     * navigate the key or value respectively (e.g. to include a field of the
     * key class or value class in the fetch group).
     * 
     * For collection and arrays of persistence-capable classes, "#element" may
     * be appended to the name of the field to navigate the element. This is
     * optional; if omitted for collections and arrays, #element is assumed.
     */
    final String name;

    /**
     * A contained fetch-group element indicates that the named group is to be included in the group 
     * being defined. Nested fetch group elements are limited to only the name attribute.
     */
    protected Set<FetchGroupMetaData> fetchGroups = null;    

    /** members (fields/properties) declared to be in this fetch group. */
    protected Set<FetchGroupMemberMetaData> members = null;    

    /**
     * Constructor for a named fetch group. Set fields using setters, before populate().
     * @param name Name of fetch group
     */
    public FetchGroupMetaData(String name)
    {
        this.name = name;
    }

    public final String getName()
    {
        return name;
    }

    public final Boolean getPostLoad()
    {
        return postLoad;
    }

    public FetchGroupMetaData setPostLoad(Boolean postLoad)
    {
        this.postLoad = postLoad;
        return this;
    }

    public final Set<FetchGroupMetaData> getFetchGroups()
    {
        return fetchGroups;
    }

    /**
     * Accessor for metadata for the members of this group.
     * @return Returns the metadata for members
     */
    public final Set<FetchGroupMemberMetaData> getMembers()
    {
        return members;
    }

    public int getNumberOfMembers()
    {
        return (members != null ? members.size() : 0);
    }

    /**
     * Add a new FetchGroupMetaData
     * @param fgmd the fetch group
     */
    public void addFetchGroup(FetchGroupMetaData fgmd)
    {
        if (fetchGroups == null)
        {
            fetchGroups = new HashSet<FetchGroupMetaData>();
        }
        fetchGroups.add(fgmd);
        fgmd.parent = this;
    }

    /**
     * Add a new field/property.
     * @param fgmmd the field/property metadata
     */
    public void addMember(FetchGroupMemberMetaData fgmmd)
    {
        if (members == null)
        {
            members = new HashSet<FetchGroupMemberMetaData>();
        }
        members.add(fgmmd);
        fgmmd.parent = this;
    }

    /**
     * Method to create a new member, add it, and return it.
     * @param name The name of the fetch group
     * @return The field metadata
     */
    public FetchGroupMemberMetaData newMemberMetaData(String name)
    {
        FetchGroupMemberMetaData fgmmd = new FetchGroupMemberMetaData(this, name);
        addMember(fgmmd);
        return fgmmd;
    }

    // ----------------------------- Utilities ------------------------------------

    /**
     * Returns a string representation of the object.
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix, String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<fetch-group name=\"" + name + "\"\n");

        // Add fetch-groups
        if (fetchGroups != null)
        {
            for (FetchGroupMetaData fgmd : fetchGroups)
            {
                sb.append(fgmd.toString(prefix + indent, indent));
            }
        }

        // Add fields
        if (members != null)
        {
            for (FetchGroupMemberMetaData fgmmd : members)
            {
                sb.append(fgmmd.toString(prefix + indent, indent));
            }
        }

        sb.append(prefix + "</fetch-group>\n");
        return sb.toString();
    }
}