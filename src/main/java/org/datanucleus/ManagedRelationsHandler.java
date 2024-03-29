/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.state.LifeCycleState;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.state.RelationshipManager;
import org.datanucleus.state.RelationshipManagerImpl;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Handler to process "managed relations".
 */
public class ManagedRelationsHandler
{
    /** Whether to perform consistency checks. */
    private boolean performChecks = false;

    /** Flag for whether we are running "managed relations" execute() at this point in time. */
    private boolean executing = false;

    /** Map of RelationshipManager keyed by StateManager that it is for. */
    private Map<DNStateManager, RelationshipManager> managedRelationDetails = null;

    /**
     * Constructor for a "managed relations" handler.
     * @param performChecks Whether to perform consistency checks as part of the execute process.
     */
    public ManagedRelationsHandler(boolean performChecks)
    {
        this.performChecks = performChecks;
        this.managedRelationDetails = new ConcurrentHashMap();
    }

    public void setPerformChecks(boolean checks)
    {
        this.performChecks = checks;
    }

    /**
     * Method to return the RelationshipManager for the specified StateManager.
     * If none is currently present will create one
     * @param sm StateManager
     * @return The RelationshipManager for this object
     */
    public RelationshipManager getRelationshipManagerForStateManager(DNStateManager sm)
    {
        RelationshipManager relMgr = managedRelationDetails.get(sm);
        if (relMgr == null)
        {
            relMgr = new RelationshipManagerImpl(sm);
            managedRelationDetails.put(sm, relMgr);
        }
        return relMgr;
    }

    public void clear()
    {
        managedRelationDetails.clear();
    }

    public boolean isExecuting()
    {
        return executing;
    }

    public void addRelationshipManagerForStateManager(DNStateManager sm, RelationshipManager relMgr)
    {
        managedRelationDetails.put(sm, relMgr);
    }

    public void execute()
    {
        if (managedRelationDetails.isEmpty())
        {
            return;
        }

        try
        {
            executing = true;
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("013000"));
            }

            if (performChecks)
            {
                // Tests for negative situations where inconsistently assigned
                Iterator<Map.Entry<DNStateManager, RelationshipManager>> managedRelEntryIter = managedRelationDetails.entrySet().iterator();
                while (managedRelEntryIter.hasNext())
                {
                    Map.Entry<DNStateManager, RelationshipManager> managedRelEntry = managedRelEntryIter.next();
                    DNStateManager sm = managedRelEntry.getKey();
                    LifeCycleState lc = sm.getLifecycleState();
                    if (lc == null || lc.isDeleted())
                    {
                        // Has been deleted so ignore all relationship changes
                        continue;
                    }

                    managedRelEntry.getValue().checkConsistency();
                }
            }

            // Process updates to manage the other side of the relations
            Iterator<Map.Entry<DNStateManager, RelationshipManager>> managedRelEntryIter = managedRelationDetails.entrySet().iterator();
            while (managedRelEntryIter.hasNext())
            {
                Map.Entry<DNStateManager, RelationshipManager> managedRelEntry = managedRelEntryIter.next();
                DNStateManager sm = managedRelEntry.getKey();
                LifeCycleState lc = sm.getLifecycleState();
                if (lc == null || lc.isDeleted())
                {
                    // Has been deleted so ignore all relationship changes
                    continue;
                }
                RelationshipManager relMgr = managedRelEntry.getValue();
                relMgr.process();
                relMgr.clearFields();
            }
            managedRelationDetails.clear();

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("013001"));
            }
        }
        finally
        {
            executing = false;
        }
    }
}