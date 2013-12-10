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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.datanucleus.api.ApiAdapter;
import org.datanucleus.util.StringUtils;

/**
 * Holder for the detachment state control for the detachment process.
 */
public class DetachState extends FetchPlanState
{
    /** a map for the current execution of detachCopy with detached objects keyed by the object id **/
    private Map<Object, Entry> detachedObjectById = new HashMap<Object, Entry>();

    /** Adapter for the API being used. */
    private ApiAdapter api;

    /**
     * Constructor.
     * @param api The API adapter
     */
    public DetachState(ApiAdapter api)
    {
        this.api = api;
    }

    /**
     * Set to the current state a detached copy object
     * @param pc The persistable object
     * @param detachedPC the Detached persistable object
     */
    public void setDetachedCopyEntry(Object pc, Object detachedPC)
    {
        detachedObjectById.put(getKey(pc), new Entry(detachedPC));
    }

    /**
     * Get any existing detached copy object for the passed in PersistenceCapable
     * @param pc the PersistenceCapable of the object searched
     * @return the Detached PC
     */
    public Entry getDetachedCopyEntry(Object pc)
    {
        return detachedObjectById.get(getKey(pc));
    }

    private Object getKey(Object pc)
    {
        Object id = api.getIdForObject(pc);
        if (id == null)
        {
            // embedded element (NO ids)
            return StringUtils.toJVMIDString(pc);
        }
        else
        {
            return id;
        }
    }

    public class Entry
    {
        private Object detachedPC;
        private List<List<String>> detachStates = new LinkedList<List<String>>();

        Entry(Object detachedPC)
        {
            this.detachedPC = detachedPC;
            this.detachStates.add(getCurrentState());
        }

        public Object getDetachedCopyObject()
        {
            return detachedPC;
        }

        /**
         * Determine whether the current state is "dominated" by any previous
         * detach state for this entry, in which case we know that all the required
         * fields will already be in the detached copy.
         * (Dominance is transitive, so we can remove redundant entries)
         * @return true if we can prove the current state is fully detached already
         */
        public boolean checkCurrentState()
        {
            List<String> currentState = getCurrentState();

            Iterator<List<String>> iter = detachStates.iterator();
            while (iter.hasNext())
            {
                List<String> detachState = iter.next();
                if (dominates(detachState, currentState))
                {
                    return true;
                }
                if (dominates(currentState, detachState))
                {
                    iter.remove();
                }
            }
            detachStates.add(currentState);
            return false;
        }

        private List<String> getCurrentState()
        {
            return new ArrayList<String>(memberNames);
        }

        private boolean dominates(List<String> candidate, List<String> target)
        {
            if (candidate.size() == 0)
                return true;
            if (candidate.size() > target.size())
                return false;
            String fieldName = target.get(target.size() - 1);
            // TODO If fieldName is not a recursive field, then return true;
            return calculateObjectDepthForMember(candidate, fieldName) <= calculateObjectDepthForMember(target, fieldName);
       }
    }
}