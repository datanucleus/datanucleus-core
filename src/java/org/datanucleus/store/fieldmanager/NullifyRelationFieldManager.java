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

import java.util.Collection;
import java.util.Map;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.ObjectProvider;

/**
 * Manager that nullifies any Collection/Map/PC fields of the object.
 */
public class NullifyRelationFieldManager extends AbstractFieldManager
{
    /** ObjectProvider for the object. */
    private final ObjectProvider op;

    /**
     * Constructor.
     * @param op the ObjectProvider
     */
    public NullifyRelationFieldManager(ObjectProvider op)
    {
        this.op = op; 
    }

    /**
     * Accessor for object field.
     * @param fieldNumber Number of field 
     * @return Object value
     */
    public Object fetchObjectField(int fieldNumber)
    {
        Object value = op.provideField(fieldNumber);
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        RelationType relType = mmd.getRelationType(op.getExecutionContext().getClassLoaderResolver());
        if (value == null)
        {
            return null;
        }
        else if (RelationType.isRelationSingleValued(relType))
        {
            // Process PC fields
            op.makeDirty(fieldNumber);
            return null;
        }
        else if (RelationType.isRelationMultiValued(relType))
        {
            if (value instanceof Collection)
            {
                // Process Collection fields
                op.makeDirty(fieldNumber);
                ((Collection)value).clear();
                return value;
            }
            else if (value instanceof Map)
            {
                // Process Map fields
                op.makeDirty(fieldNumber);
                ((Map)value).clear();
                return value;
            }
            else if (value.getClass().isArray() && Object.class.isAssignableFrom(value.getClass().getComponentType()))
            {
                // Process object array fields
                // TODO Check if the array element is PC and nullify
            }
        }

        //do not need to nullify fields that are not references and resides embedded in this object
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