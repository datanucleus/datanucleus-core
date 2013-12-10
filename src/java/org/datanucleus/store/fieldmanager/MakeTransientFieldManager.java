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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.FetchPlanForClass;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.FetchPlanState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.SCO;

/**
 * Field Manager to handle the making transient of fields.
 */
public class MakeTransientFieldManager extends AbstractFetchDepthFieldManager
{
    /**
     * Constructor for a field manager for make transient process.
     * @param sm the ObjectProvider of the instance being detached. An instance in Persistent or Transactional state
     * @param secondClassMutableFields The second class mutable fields for the class of this object
     * @param fpClass Fetch Plan for the class of this instance
     * @param state State object to hold any pertinent controls for the fetchplan process
     */
    public MakeTransientFieldManager(ObjectProvider sm, boolean[] secondClassMutableFields, FetchPlanForClass fpClass, 
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
        if (op.getExecutionContext().getApiAdapter().isPersistent(pc))
        {
            // Make transient if still persistent
            op.getExecutionContext().getApiAdapter().getExecutionContext(pc).makeObjectTransient(pc, state);
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
        op.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);

        if (value != null)
        {
            AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            RelationType relType = mmd.getRelationType(op.getExecutionContext().getClassLoaderResolver());
            if (RelationType.isRelationSingleValued(relType))
            {
                // Process PC fields
                processPersistable(value);
            }
            else if (RelationType.isRelationMultiValued(relType))
            {
                ApiAdapter api = op.getExecutionContext().getApiAdapter();
                if (value instanceof Collection)
                {
                    // Process all elements of the Collection that are PC
                    if (!(value instanceof SCO))
                    {
                        // Replace with SCO
                        value = op.wrapSCOField(fieldNumber, value, false, false, true);
                    }
                    SCO sco = (SCO)value;

                    Collection coll = (Collection)value;
                    Iterator iter = coll.iterator();
                    while (iter.hasNext())
                    {
                        Object element = iter.next();
                        if (api.isPersistable(element))
                        {
                            processPersistable(element);
                        }
                    }
                    sco.unsetOwner();
                }
                else if (value instanceof Map)
                {
                    // Process all keys, values of the Map that are PC
                    if (!(value instanceof SCO))
                    {
                        // Replace with SCO
                        value = op.wrapSCOField(fieldNumber, value, false, false, true);
                    }
                    SCO sco = (SCO)value;

                    Map map = (Map)value;

                    // Process any keys that are persistable
                    Set keys = map.keySet();
                    Iterator iter = keys.iterator();
                    while (iter.hasNext())
                    {
                        Object mapKey = iter.next();
                        if (api.isPersistable(mapKey))
                        {
                            processPersistable(mapKey);
                        }
                    }

                    // Process any values that are persistable
                    Collection values = map.values();
                    iter = values.iterator();
                    while (iter.hasNext())
                    {
                        Object mapValue = iter.next();
                        if (api.isPersistable(mapValue))
                        {
                            processPersistable(mapValue);
                        }
                    }

                    sco.unsetOwner();
                }
                else if (value instanceof Object[])
                {
                    Object[] array = (Object[]) value;
                    for (int i=0;i<array.length;i++)
                    {
                        Object element = array[i];
                        if (api.isPersistable(element))
                        {
                            processPersistable(element);
                        }
                    }
                }
            }
            else if (value instanceof SCO)
            {
                // Other SCO field, so unset its owner
                SCO sco = (SCO) value;
                sco.unsetOwner();
            }
            else
            {
                // Primitive, or primitive array, or some unsupported container type
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
        op.provideFields(new int[]{fieldNumber}, sfv);
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