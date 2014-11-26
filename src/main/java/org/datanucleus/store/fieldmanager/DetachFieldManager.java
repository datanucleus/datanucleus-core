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
2005 Jorg von Frantzius - updates for fetch-depth
2007 Andy Jefferson - rewritten to process all fields detecting persistable objects at runtime
    ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.FetchPlanForClass;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DetachState;
import org.datanucleus.state.FetchPlanState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOUtils;

/**
 * FieldManager to handle the detachment of fields with persistable objects.
 */
public class DetachFieldManager extends AbstractFetchDepthFieldManager
{
    /** Whether we should create detached copies, or detach in situ. */
    boolean copy = true;

    /**
     * Constructor for a field manager for detachment.
     * @param op the ObjectProvider of the instance being detached. An instance in Persistent or Transactional state
     * @param secondClassMutableFields The second class mutable fields for the class of this object
     * @param fpClass Fetch Plan for the class of this instance
     * @param state State object to hold any pertinent controls for the detachment process
     * @param copy Whether to create detached COPIES or just detach in-situ
     */
    public DetachFieldManager(ObjectProvider op, boolean[] secondClassMutableFields, FetchPlanForClass fpClass, FetchPlanState state, 
            boolean copy)
    {
        super(op, secondClassMutableFields, fpClass, state);
        this.copy = copy;
    }

    /**
     * Utility method to process the passed persistable object.
     * @param pc The PC object
     * @return The processed object
     */
    protected Object processPersistable(Object pc)
    {
        if (pc == null)
        {
            return null;
        }

        ApiAdapter api = op.getExecutionContext().getApiAdapter();
        if (!api.isPersistable(pc))
        {
            return pc;
        }

        if (!api.isDetached(pc))
        {
            if (api.isPersistent(pc))
            {
                // Persistent object that is not yet detached so detach it
                if (copy)
                {
                    // Detach a copy and return the copy
                    return op.getExecutionContext().detachObjectCopy(pc, state);
                }

                // Detach the object
                op.getExecutionContext().detachObject(pc, state);
            }
        }
        return pc;
    }

    /**
     * Method to fetch an object field whether it is collection/map, PC, or whatever for the detachment 
     * process.
     * @param fieldNumber Number of the field
     * @return The object
     */
    protected Object internalFetchObjectField(int fieldNumber)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        op.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);
        if (value == null)
        {
            return null;
        }

        ClassLoaderResolver clr = op.getExecutionContext().getClassLoaderResolver();
        ApiAdapter api = op.getExecutionContext().getApiAdapter();
        AbstractMemberMetaData mmd =
            op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        RelationType relationType = mmd.getRelationType(clr);

        if (RelationType.isRelationSingleValued(relationType))
        {
            if (api.isPersistable(value))
            {
                // Process PC fields
                return processPersistable(value);
            }
        }
        else if (RelationType.isRelationMultiValued(relationType))
        {
            if (mmd.hasCollection() || mmd.hasMap())
            {
                // Process all elements of Collections/Maps that are PC
                if (copy)
                {
                    if (!(value instanceof SCO))
                    {
                        // Replace with SCO so we can work with it
                        value = SCOUtils.wrapSCOField(op, fieldNumber, value, false, false, true);
                    }
                    if (!(value instanceof SCO) && mmd.isSerialized())
                    {
                        // Collection/Map type not supported, but being serialised
                        if (mmd.hasCollection())
                        {
                            if (mmd.getCollection().elementIsPersistent())
                            {
                                throw new NucleusUserException("Unable to detach " + mmd.getFullFieldName() +
                                    " since is of an unsupported Collection type with persistent elements");
                            }
                            return value;
                        }
                        else if (mmd.hasMap())
                        {
                            if (mmd.getMap().keyIsPersistent() || mmd.getMap().valueIsPersistent())
                            {
                                throw new NucleusUserException("Unable to detach " + mmd.getFullFieldName() +
                                    " since is of an unsupported Map type with persistent keys/values");
                            }
                            return value;
                        }
                    }
                    return ((SCO)value).detachCopy(state);
                }

                if (!(value instanceof SCO))
                {
                    // Replace with SCO so we can work with it
                    value = SCOUtils.wrapSCOField(op, fieldNumber, value, false, false, true);
                }
                SCO sco = (SCO)value;

                if (sco instanceof Collection)
                {
                    // Detach all PC elements of the collection
                    SCOUtils.detachForCollection(op, ((Collection)sco).toArray(), state);
                    sco.unsetOwner();
                }
                else if (sco instanceof Map)
                {
                    // Detach all PC keys/values of the map
                    SCOUtils.detachForMap(op, ((Map)sco).entrySet(), state);
                    sco.unsetOwner();
                }

                if (SCOUtils.detachAsWrapped(op))
                {
                    return sco;
                }
                return SCOUtils.unwrapSCOField(op, fieldNumber, value, true);
            }
            else if (mmd.hasArray())
            {
                // Process object array
                if (!api.isPersistable(mmd.getType().getComponentType()))
                {
                    // Array element type is not persistable so just return
                    return value;
                }

                Object[] arrValue = (Object[])value;
                Object[] arrDetached = (Object[])Array.newInstance(mmd.getType().getComponentType(), arrValue.length);
                for (int j=0;j<arrValue.length;j++)
                {
                    // Detach elements as appropriate
                    arrDetached[j] = processPersistable(arrValue[j]);
                }
                return arrDetached;
            }
        }
        else
        {
            if (secondClassMutableFields[fieldNumber])
            {
                // Other SCO
                if (copy)
                {
                    if (!(value instanceof SCO))
                    {
                        // Replace with SCO so we can work with it
                        value = SCOUtils.wrapSCOField(op, fieldNumber, value, false, false, true);
                    }
                    return ((SCO)value).detachCopy(state);
                }

                if (!(value instanceof SCO))
                {
                    // Replace with SCO so we can work with it
                    value = SCOUtils.wrapSCOField(op, fieldNumber, value, false, false, true);
                }
                SCO sco = (SCO)value;

                if (SCOUtils.detachAsWrapped(op))
                {
                    return sco;
                }
                return SCOUtils.unwrapSCOField(op, fieldNumber, value, true);
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
        // check if the object here is PC and is in the detached cache anyway
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        op.provideFields(new int[]{fieldNumber}, sfv);
        Object value = sfv.fetchObjectField(fieldNumber);
        ApiAdapter api = op.getExecutionContext().getApiAdapter();

        if (api.isPersistable(value))
        {
            if (copy)
            {
                DetachState.Entry entry = ((DetachState)state).getDetachedCopyEntry(value);
                if (entry != null)
                {
                    // While we are at the end of a branch and this would go beyond the depth limits,
                    // the object here *is* already detached so just return it
                    return entry.getDetachedCopyObject();
                }
            }
            else if (op.getExecutionContext().getApiAdapter().isDetached(value))
            {
                return value;
            }
        }

        // we reached a leaf of the object graph to detach
        throw new EndOfFetchPlanGraphException();
    }
}