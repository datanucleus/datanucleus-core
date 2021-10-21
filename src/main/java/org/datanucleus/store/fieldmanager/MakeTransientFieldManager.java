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

import org.datanucleus.FetchPlanForClass;
import org.datanucleus.FetchPlanState;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.containers.ContainerAdapter;

/**
 * Field Manager to handle the making transient of fields.
 */
public class MakeTransientFieldManager extends AbstractFetchDepthFieldManager
{
    /**
     * Constructor for a field manager for make transient process.
     * @param sm StateManager of the instance being detached. An instance in Persistent or Transactional state
     * @param secondClassMutableFields The second class mutable fields for the class of this object
     * @param fpClass Fetch Plan for the class of this instance
     * @param state State object to hold any pertinent controls for the fetchplan process
     */
    public MakeTransientFieldManager(DNStateManager sm, boolean[] secondClassMutableFields, FetchPlanForClass fpClass, 
            FetchPlanState state)
    {
        super(sm, secondClassMutableFields, fpClass, state);
    }

    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     */
    protected void processPersistable(Object pc)
    {
        if (sm.getExecutionContext().getApiAdapter().isPersistent(pc))
        {
            // Make transient if still persistent
            sm.getExecutionContext().getApiAdapter().getExecutionContext(pc).makeObjectTransient(pc, state);
        }
    }

    /**
     * Method to fetch an object field whether it is SCO collection, PC, or whatever for the fetchplan process.
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
            RelationType relType = mmd.getRelationType(sm.getExecutionContext().getClassLoaderResolver());
            
            if (relType != RelationType.NONE)
            {
                if (mmd.hasContainer())
                {
                    // Replace with SCO, when possible
                    value = SCOUtils.wrapSCOField(sm, fieldNumber, value, true);

                    TypeManager typeManager = sm.getExecutionContext().getTypeManager();
                    ContainerAdapter containerAdapter = typeManager.getContainerAdapter(value);
                    ApiAdapter api = sm.getExecutionContext().getApiAdapter();

                    // Process all elements of the Container that are PC
                    for (Object object : containerAdapter)
                    {
                        if (api.isPersistable(object))
                        {
                            processPersistable(object);
                        }
                    }
                }
                else
                {
                    processPersistable(value);
                }
            }
            
            if (value instanceof SCO){
                ((SCO)value).unsetOwner(); 
            }
        }

        return value;
    }

    /**
     * Method to throw and EndOfFetchPlanGraphException since we're at the end of a branch in the tree.
     * @param fieldNumber Number of the field
     * @return Object to return
     */
    protected Object endOfGraphOperation(int fieldNumber)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sm.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);

        if (value != null && secondClassMutableFields[fieldNumber])
        {
            SCO sco;
            if (value instanceof SCO)
            {
                // SCO field so unset its owner
                sco = (SCO) value;
                sco.unsetOwner();
            }
        }

        return value;
    }
}