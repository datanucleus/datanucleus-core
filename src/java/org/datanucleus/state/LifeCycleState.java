/**********************************************************************
Copyright (c) 2002 Kelly Grizzle (TJDO) and others. All rights reserved. 
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
2003 Andy Jefferson - commented
2003 Andy Jefferson - added localiser and Logger
2005 Andy Jefferson - added Detached clean/dirty states
    ...
**********************************************************************/
package org.datanucleus.state;

import org.datanucleus.FetchPlan;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Base Class representing the life cycle state. Implemented for individual states.
 */
public abstract class LifeCycleState
{
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** transient **/
    public static final int TRANSIENT       = 0;
    /** Persistent-New **/
    public static final int P_NEW           = 1;
    /** Persistent-Clean **/
    public static final int P_CLEAN         = 2;
    /** Persistent-Dirty **/
    public static final int P_DIRTY         = 3;
    /** Hollow **/
    public static final int HOLLOW          = 4;
    /** Transaction-Clean **/
    public static final int T_CLEAN         = 5;
    /** Transaction-Dirty **/
    public static final int T_DIRTY         = 6;
    /** Persistent-New-Deleted **/
    public static final int P_NEW_DELETED   = 7;
    /** Persistent-Deleted **/
    public static final int P_DELETED       = 8;
    /** Persistent-NonTransactional **/
    public static final int P_NONTRANS      = 9;
    /** Persistent-NonTransactionalDirty **/
    public static final int P_NONTRANS_DIRTY = 10;
    /** Detached-Clean **/
    public static final int DETACHED_CLEAN   = 11;
    /** Detached-Dirty **/
    public static final int DETACHED_DIRTY   = 12;

    /** total number of states **/
    public static final int TOTAL            = 13;
    /** illegal state **/
    public static final int ILLEGAL_STATE    = 13;

    // These are state flags that are set to required valuers in specific state 
    protected boolean isDirty;
    protected boolean isNew;
    protected boolean isDeleted;
    protected boolean isTransactional;
    protected boolean isPersistent;

    protected int stateType;

    /**
     * Returns the type of the life cycle state
     * @return the type of this life cycle state
     */
    public final int stateType()
    {
        return stateType;
    }

    /**
     * Utility to change state to a new state.
     * @param op ObjectProvider
     * @param newStateType The new state
     * @return new LifeCycle state.
     */
    protected final LifeCycleState changeState(ObjectProvider op, int newStateType)
    {
        LifeCycleState newState = op.getExecutionContext().getNucleusContext().getApiAdapter().getLifeCycleState(newStateType);

        if (NucleusLogger.LIFECYCLE.isDebugEnabled())
        {
            NucleusLogger.LIFECYCLE.debug(LOCALISER.msg("027016", 
                StringUtils.toJVMIDString(op.getObject()), 
                IdentityUtils.getIdentityAsString(op.getExecutionContext().getApiAdapter(), op.getInternalObjectId()), 
                this, newState));
        }

        if (isTransactional)
        {
            if (newState == null || !newState.isTransactional)
            {
                op.evictFromTransaction();
            }
        }
        else
        {
            if (newState != null && newState.isTransactional)
            {
                op.enlistInTransaction();
            }
        }

        if (newState == null)
        {
            op.disconnect();
        }

        return newState;
    }
    
    /**
     * Utility to change state to a new state.
     * @param op ObjectProvider
     * @param newStateType The new state
     * @return new LifeCycle state.
     */
    protected final LifeCycleState changeTransientState(ObjectProvider op, int newStateType)
    {
        LifeCycleState newState = op.getExecutionContext().getNucleusContext().getApiAdapter().getLifeCycleState(newStateType);
        
        try 
        {
            op.enlistInTransaction();
        }
        catch (Exception e)
        {
            //op is already enlisted
        }

        return newState;
    }

    /**
     * Method to transition to persistent state.
     * @param op ObjectProvider.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionMakePersistent(ObjectProvider op)
    {
        return this;
    }

    /**
     * Method to transition to delete persistent state.
     * @param op ObjectProvider.
     * @return new LifeCycle state.
     */
    public LifeCycleState transitionDeletePersistent(ObjectProvider op)
    {
        return this;
    }

    /**
     * Method to transition to transactional state.
     * @param op ObjectProvider.
     * @param refreshFields Whether to refresh loaded fields
     * @return new LifeCycle state.
     */
    public LifeCycleState transitionMakeTransactional(ObjectProvider op, boolean refreshFields)
    {
        return this;
    }

    /**
     * Method to transition to nontransactional state.
     * @param op ObjectProvider.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionMakeNontransactional(ObjectProvider op)
    {
        return this;
    }

    /**
     * Method to transition to transient state.
     * @param op ObjectProvider.
     * @param useFetchPlan to make transient the fields in the fetch plan
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionMakeTransient(ObjectProvider op, boolean useFetchPlan, boolean detachAllOnCommit)
    {
        return this;
    }

    /**
     * Method to transition to transaction begin state.
     * @param op ObjectProvider.
     * @param tx Transaction.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionBegin(ObjectProvider op, org.datanucleus.Transaction tx)
    {
        return this;
    }
    
    /**
     * Method to transition to commit state.
     * @param op ObjectProvider.
     * @param tx the Transaction been committed.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionCommit(ObjectProvider op, org.datanucleus.Transaction tx)
    {
        return this;
    }

    /**
     * Method to transition to rollback state.
     * @param op ObjectProvider.
     * @param tx Transaction.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionRollback(ObjectProvider op, org.datanucleus.Transaction tx)
    {
        return this;
    }

    /**
     * Method to transition to refresh state.
     * @param op ObjectProvider.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionRefresh(ObjectProvider op)
    {
        return this;
    }

    /**
     * Method to transition to evict state.
     * @param op ObjectProvider.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionEvict(ObjectProvider op)
    {
        return this;
    }

    /**
     * Method to transition to read-field state.
     * @param op ObjectProvider.
     * @param isLoaded if the field was previously loaded 
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionReadField(ObjectProvider op, boolean isLoaded)
    {
        return this;
    }

    /**
     * Method to transition to write-field state.
     * @param op ObjectProvider.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionWriteField(ObjectProvider op)
    {
        return this;
    }

    /**
     * Method to transition to retrieve state.
     * @param op ObjectProvider.
     * @param fgOnly only retrieve the current fetch group fields
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionRetrieve(ObjectProvider op, boolean fgOnly)
    {
        return this;
    }
    
    /**
     * Method to transition to retrieve state.
     * @param op ObjectProvider.
     * @param fetchPlan the fetch plan to load fields
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionRetrieve(ObjectProvider op, FetchPlan fetchPlan)
    {
        return this;
    }

    /**
     * Method to transition to detached-clean.
     * @param op ObjectProvider.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionDetach(ObjectProvider op)
    {
        return this;
    }

    /**
     * Method to transition to persistent-clean.
     * @param op ObjectProvider.
     * @return new LifeCycle state.
     **/
    public LifeCycleState transitionAttach(ObjectProvider op)
    {
        return this;
    }

    /**
     * Method to transition when serialised.
     * @param op ObjectProvider
     * @return The new LifeCycle state
     */
    public LifeCycleState transitionSerialize(ObjectProvider op)
    {
        return this;
    }

    /**
     * Return whether the object is dirty, ie has been changed
     * (created, updated, deleted) in this Tx.
     * @return Whether the object is dirty. 
     */
    public final boolean isDirty()
    {
        return isDirty;
    }
   
    /**
     * Return whether the object was newly created.
     * @return Whether the object is new. 
     */
    public final boolean isNew()
    {
        return isNew;
    }

    /**
     * Return whether the object is deleted.
     * @return Whether the object is deleted. 
     */
    public final boolean isDeleted()
    {
        return isDeleted;
    }

    /**
     * Return whether the object is transactional.
     * @return Whether the object is transactional. 
     */
    public final boolean isTransactional()
    {
        return isTransactional;
    }
    
    /**
     * Return whether the object is persistent.
     * @return Whether the object is persistent. 
     */
    public final boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Method to return a string version of this object.
     * @return String version of the object.
     **/
    public abstract String toString();
}