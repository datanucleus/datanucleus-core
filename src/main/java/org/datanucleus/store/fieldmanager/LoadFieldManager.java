/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.fieldmanager;

import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchPlanForClass;
import org.datanucleus.FetchPlanState;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.containers.ContainerHandler;

/**
 * Field Manager to handle loading all fields of all objects in the fetch plan.
 * The method in JDOStateManager only loads the fields for that object and so
 * will only load the DFG fields for objects (hence omitting any non-DFG fields
 * that are in the FetchPlan that have been omitted due to lazy-loading).
 */
public class LoadFieldManager extends AbstractFetchDepthFieldManager
{
    /**
     * Constructor for a field manager for make transient process.
     * @param sm StateManager of the instance being loaded
     * @param secondClassMutableFields The second class mutable fields for the class of this object
     * @param fpClass Fetch Plan for the class of this instance
     * @param state State object to hold any pertinent controls for the fetchplan process
     */
    public LoadFieldManager(DNStateManager sm, boolean[] secondClassMutableFields, FetchPlanForClass fpClass, FetchPlanState state)
    {
        super(sm, secondClassMutableFields, fpClass, state);
    }

    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     */
    protected void processPersistable(Object pc)
    {
        ExecutionContext ec = sm.getExecutionContext().getApiAdapter().getExecutionContext(pc);
        if (ec != null)
        {
            // Field is persisted (otherwise it may have not been persisted by reachability)
            ec.findStateManager(pc).loadFieldsInFetchPlan(state);
        }
    }

    /**
     * Method to fetch an object field whether it is SCO collection, PC, or whatever for the fetchplan
     * process.
     * @param fieldNumber Number of the field
     * @return The object
     */
    protected Object internalFetchObjectField(int fieldNumber)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sm.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);

        if (value != null)
        {
            AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            RelationType relationType = mmd.getRelationType(sm.getExecutionContext().getClassLoaderResolver());

            if (relationType != RelationType.NONE)
            {
                if (mmd.hasContainer())
                {
                    value = processContainer(mmd, value);
                }
                else
                {
                    // Process PC fields
                    processPersistable(value);
                }
            }
        }

        return value;
    }
    
    private Object processContainer(AbstractMemberMetaData mmd, Object container)
    {
        Object wrappedContainer = container;

        if (mmd.hasArray())
        {
            wrappedContainer = container;
        }
        else
        {
            if (!(container instanceof SCO))
            {
                // Replace with SCO
                wrappedContainer = SCOUtils.wrapSCOField(sm, mmd.getAbsoluteFieldNumber(), container, true);
            }
        }

        // Process all persistable objects in the container: elements,values and keys
        ExecutionContext ec = sm.getExecutionContext();
        TypeManager typeManager = ec.getTypeManager();
        ContainerHandler containerHandler = typeManager.getContainerHandler(mmd.getType());

        ApiAdapter api = ec.getApiAdapter();
        for (Object object : containerHandler.getAdapter(wrappedContainer))
        {
            if (api.isPersistable(object))
            {
                processPersistable(object);
            }
        }
        
        return wrappedContainer;
    }

    /**
     * Method called when were arrive at the end of a branch
     * @param fieldNumber Number of the field
     * @return Object to return
     */
    protected Object endOfGraphOperation(int fieldNumber)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sm.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);

        return value;
    }
}