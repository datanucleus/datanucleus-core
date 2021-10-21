/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved. 
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

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.containers.ContainerAdapter;

/**
 * Manager that nullifies any Collection/Map/PC fields of the object.
 * NOTE : If called on a non-relation field, this will reset the field to 0!
 * Used by "persistence-by-reachability at commit" functionality.
 */
public class NullifyRelationFieldManager extends AbstractFieldManager
{
    /** StateManager for the object. */
    private final DNStateManager sm;

    /**
     * Constructor.
     * @param sm StateManager
     */
    public NullifyRelationFieldManager(DNStateManager sm)
    {
        this.sm = sm; 
    }

    /**
     * Accessor for object field.
     * @param fieldNumber Number of field 
     * @return Object value
     */
    public Object fetchObjectField(int fieldNumber)
    {
        Object value = sm.provideField(fieldNumber);
        
        if (value != null)
        {
            AbstractMemberMetaData mmd = sm.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
            RelationType relType = mmd.getRelationType(sm.getExecutionContext().getClassLoaderResolver());
            
            // do not need to nullify fields that are not references and resides embedded in this object
            if (relType != RelationType.NONE)
            {
                if (mmd.hasContainer())
                {
                    TypeManager typeManager = sm.getExecutionContext().getTypeManager();
                    ContainerAdapter containerAdapter = typeManager.getContainerAdapter(value);
                    containerAdapter.clear();

                    return containerAdapter.getContainer();
                }
                
                // Process PC fields
                sm.makeDirty(fieldNumber);
                return null;
            }
        }

        return value;
    }

    public boolean fetchBooleanField(int fieldNumber)
    {
        return true;
    }

    public char fetchCharField(int fieldNumber)
    {
        return '0';
    }

    public byte fetchByteField(int fieldNumber)
    {
        return (byte)0;
    }

    public double fetchDoubleField(int fieldNumber)
    {
        return 0;
    }

    public float fetchFloatField(int fieldNumber)
    {
        return 0;
    }

    public int fetchIntField(int fieldNumber)
    {
        return 0;
    }

    public long fetchLongField(int fieldNumber)
    {
        return 0;
    }

    public short fetchShortField(int fieldNumber)
    {
        return 0;
    }

    public String fetchStringField(int fieldNumber)
    {
        return "";
    }
}