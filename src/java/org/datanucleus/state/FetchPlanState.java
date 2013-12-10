/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Holder for the state control for FetchPlan processing.
 * Maintains a list of the member names being fetched. The first item in the List will be the root.
 * When a new branch of the graph is processed the member name is added, and it is removed once it
 * has been processed. This provides a means of always knowing the depth in the current graph, and also of
 * allowing detection of recursion of member names.
 */
public class FetchPlanState
{
    /**
     * List of member names in the graph. The first is the root of the tree, and members are added as 
     * they are encountered and removed when they are finished with.
     */
    protected List<String> memberNames = new ArrayList<String>();

    /**
     * Method to add a member name to the list since it is being processed
     * @param memberName The member to add
     */
    public void addMemberName(String memberName)
    {
        memberNames.add(memberName);
    }

    /**
     * Method to remove the latest member name from the list since it is now processed
     */
    public void removeLatestMemberName()
    {
        memberNames.remove(memberNames.size()-1);
    }

    /**
     * Accessor for the object graph depth currently
     * @return The graph depth
     */
    public int getCurrentFetchDepth()
    {
        return memberNames.size();
    }

    /**
     * Accessor for the current depth for the specified member name.
     * @param memberName The name of the field/property
     * @return The depth for this member
     */
    public int getObjectDepthForType(String memberName)
    {
        return calculateObjectDepthForMember(memberNames, memberName);
    }

    protected static int calculateObjectDepthForMember(List<String> memberNames, String memberName)
    {
        ListIterator iter = memberNames.listIterator(memberNames.size()); // Start at the end
        int number = 0;
        while (iter.hasPrevious())
        {
            String field = (String)iter.previous();
            if (field.equals(memberName))
            {
                number++;
            }
            else
            {
                break;
            }
        }
        return number;
    }
}