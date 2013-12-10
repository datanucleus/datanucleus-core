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
package org.datanucleus.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.util.StringUtils;

/**
 * FetchPlan defined in MetaData.
 */
public class FetchPlanMetaData extends MetaData
{
    /** Name of the FetchPlan. */
    String name;

    /** Max fetch depth for this FetchPlan. */
    protected int maxFetchDepth = -1;

    /** Fetch Size for use when querying using this FetchPlan. */
    protected int fetchSize = -1;

    /** Series of Fetch Groups used in this FetchPlan. Only used during construction. */
    protected List<FetchGroupMetaData> fetchGroups = new ArrayList();    

    /**
     * Constructor for a fetch plan with a name. Set fields using setters, before populate().
     * @param name Name of fetch plan
     */
    public FetchPlanMetaData(String name)
    {
        this.name = name;
    }

    /**
     * Accessor for name
     * @return Returns the name.
     */
    public final String getName()
    {
        return name;
    }

    public final int getMaxFetchDepth()
    {
        return maxFetchDepth;
    }

    public FetchPlanMetaData setMaxFetchDepth(int maxFetchDepth)
    {
        this.maxFetchDepth = maxFetchDepth;
        return this;
    }

    public FetchPlanMetaData setMaxFetchDepth(String maxFetchDepth)
    {
        if (StringUtils.isWhitespace(maxFetchDepth))
        {
            return this;
        }
        try
        {
            int value = Integer.parseInt(maxFetchDepth);
            this.maxFetchDepth = value;
        }
        catch (NumberFormatException nfe)
        {
        }
        return this;
    }

    public final int getFetchSize()
    {
        return fetchSize;
    }

    public int getNumberOfFetchGroups()
    {
        return fetchGroups.size();
    }

    public FetchPlanMetaData setFetchSize(int fetchSize)
    {
        this.fetchSize = fetchSize;
        return this;
    }

    public FetchPlanMetaData setFetchSize(String fetchSize)
    {
        if (StringUtils.isWhitespace(fetchSize))
        {
            return this;
        }
        try
        {
            int value = Integer.parseInt(fetchSize);
            this.fetchSize = value;
        }
        catch (NumberFormatException nfe)
        {
        }
        return this;
    }

    /**
     * Accessor for fetchGroupMetaData
     * @return Returns the fetchGroupMetaData.
     */
    public final FetchGroupMetaData[] getFetchGroupMetaData()
    {
        return fetchGroups.toArray(new FetchGroupMetaData[fetchGroups.size()]);
    }

    /**
     * Add a new FetchGroupMetaData
     * @param fgmd the fetch group
     */
    public void addFetchGroup(FetchGroupMetaData fgmd)
    {
        fetchGroups.add(fgmd);
        fgmd.parent = this;
    }

    /**
     * Method to create a new FetchGroup metadata, add it and return it.
     * @param name Name of the fetch group
     * @return The new fetch group metadata
     */
    public FetchGroupMetaData newFetchGroupMetaData(String name)
    {
        FetchGroupMetaData fgmd = new FetchGroupMetaData(name);
        addFetchGroup(fgmd);
        return fgmd;
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
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<fetch-plan name=\"" + name + "\"" + 
            " max-fetch-depth=\"" + maxFetchDepth + "\"" +
            " fetch-size=\"" + fetchSize + "\"\n");

        // Add fetch-groups
        Iterator iter = fetchGroups.iterator();
        while (iter.hasNext())
        {
            FetchGroupMetaData fgmd = (FetchGroupMetaData)iter.next();
            sb.append(fgmd.toString(prefix + indent, indent));
        }

        sb.append(prefix + "</fetch-plan>\n");
        return sb.toString();
    }
}