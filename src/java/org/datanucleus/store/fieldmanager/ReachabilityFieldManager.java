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
package org.datanucleus.store.fieldmanager;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Field manager that runs reachability on all PC objects referenced from the source object.
 * Whenever a PC object is encountered "runReachability" is performed on the ObjectProvider of the object.
 */
public class ReachabilityFieldManager extends AbstractFieldManager
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** ObjectProvider for the owning object. */
    private final ObjectProvider op;

    /** Set of reachables up to this point. */
    private Set reachables = null;

    /**
     * Constructor.
     * @param op The ObjectProvider for the object.
     * @param reachables Reachables up to this point
     */
    public ReachabilityFieldManager(ObjectProvider op, Set reachables)
    {
        this.op = op;
        this.reachables = reachables;
    }

    /**
     * Utility method to process the passed persistable object.
     * @param obj The persistable object
     * @param mmd MetaData for the member storing this object
     */
    protected void processPersistable(Object obj, AbstractMemberMetaData mmd)
    {
        ObjectProvider objOP = this.op.getExecutionContext().findObjectProvider(obj);
        if (objOP != null)
        {
            objOP.runReachability(reachables);
        }
        else
        {
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("007005", 
                    op.getExecutionContext().getApiAdapter().getIdForObject(obj), mmd.getFullFieldName()));
            }
        }
    }

    /**
     * Method to store an object field.
     * @param fieldNumber Number of the field (absolute)
     * @param value Value of the field
     */
    public void storeObjectField(int fieldNumber, Object value)
    {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        if (value != null)
        {
            boolean persistCascade = mmd.isCascadePersist();
            RelationType relType = mmd.getRelationType(op.getExecutionContext().getClassLoaderResolver());
            ApiAdapter api = op.getExecutionContext().getApiAdapter();
            if (persistCascade)
            {
                if (RelationType.isRelationSingleValued(relType))
                {
                    // Process PC fields
                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("007004", mmd.getFullFieldName()));
                    }
                    processPersistable(value, mmd);
                }
                else if (RelationType.isRelationMultiValued(relType))
                {
                    if (value instanceof Collection)
                    {
                        // Process all elements of the Collection that are PC
                        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                        {
                            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("007002", mmd.getFullFieldName()));
                        }
                        Collection coll = (Collection)value;
                        Iterator iter = coll.iterator();
                        while (iter.hasNext())
                        {
                            Object element = iter.next();
                            if (api.isPersistable(element))
                            {
                                processPersistable(element, mmd);
                            }
                        }
                    }
                    else if (value instanceof Map)
                    {
                        // Process all keys, values of the Map that are PC
                        Map map = (Map)value;

                        // Process any keys that are persistable
                        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                        {
                            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("007002", mmd.getFullFieldName()));
                        }
                        Set keys = map.keySet();
                        Iterator iter = keys.iterator();
                        while (iter.hasNext())
                        {
                            Object mapKey = iter.next();
                            if (api.isPersistable(mapKey))
                            {
                                processPersistable(mapKey, mmd);
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
                                processPersistable(mapValue, mmd);
                            }
                        }
                    }
                    else if (value instanceof Object[])
                    {
                        // Process all array elements that are PC
                        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                        {
                            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("007003", mmd.getFullFieldName()));
                        }
                        Object[] array = (Object[]) value;
                        for (int i=0;i<array.length;i++)
                        {
                            Object element = array[i];
                            if (api.isPersistable(element))
                            {
                                processPersistable(element, mmd);
                            }
                        }
                    }
                }
                else
                {
                    // Primitive, or primitive array, or some unsupported container type
                }
            }
        }
    }

    public void storeBooleanField(int fieldNumber, boolean value)
    {
        // Do nothing
    }

    public void storeByteField(int fieldNumber, byte value)
    {
        // Do nothing
    }

    public void storeCharField(int fieldNumber, char value)
    {
        // Do nothing
    }

    public void storeDoubleField(int fieldNumber, double value)
    {
        // Do nothing
    }

    public void storeFloatField(int fieldNumber, float value)
    {
        // Do nothing
    }

    public void storeIntField(int fieldNumber, int value)
    {
        // Do nothing
    }

    public void storeLongField(int fieldNumber, long value)
    {
        // Do nothing
    }

    public void storeShortField(int fieldNumber, short value)
    {
        // Do nothing
    }

    public void storeStringField(int fieldNumber, String value)
    {
        // Do nothing
    }
}