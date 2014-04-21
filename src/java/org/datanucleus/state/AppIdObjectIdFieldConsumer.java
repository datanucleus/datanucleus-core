/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.api.ApiAdapter;
import org.datanucleus.enhancer.Persistable;
import org.datanucleus.store.fieldmanager.FieldManager;

/**
 * Simple class to handle the copying of PK fields from an object id to an object.
 * Uses the supplied FieldManager to put the values into the object.
 * Handles PC fields that are part of the PK, cascading to (PK) fields of that object.
 */
public class AppIdObjectIdFieldConsumer implements FieldManager, Persistable.ObjectIdFieldConsumer
{
    ApiAdapter api;
    FieldManager fm;

    public AppIdObjectIdFieldConsumer(ApiAdapter api, FieldManager fm)
    {
        this.api = api;
        this.fm = fm;
    }

    public void storeBooleanField(int fieldNumber, boolean value)
    {
        fm.storeBooleanField(fieldNumber, value);
    }

    public void storeByteField(int fieldNumber, byte value)
    {
        fm.storeByteField(fieldNumber, value);
    }

    public void storeCharField(int fieldNumber, char value)
    {
        fm.storeCharField(fieldNumber, value);
    }

    public void storeDoubleField(int fieldNumber, double value)
    {
        fm.storeDoubleField(fieldNumber, value);
    }

    public void storeFloatField(int fieldNumber, float value)
    {
        fm.storeFloatField(fieldNumber, value);
    }

    public void storeIntField(int fieldNumber, int value)
    {
        fm.storeIntField(fieldNumber, value);
    }

    public void storeLongField(int fieldNumber, long value)
    {
        fm.storeLongField(fieldNumber, value);
    }

    public void storeShortField(int fieldNumber, short value)
    {
        fm.storeShortField(fieldNumber, value);
    }

    public void storeStringField(int fieldNumber, String value)
    {
        fm.storeStringField(fieldNumber, value);
    }

    public void storeObjectField(int fieldNumber, Object value)
    {
        if (api.isPersistable(value))
        {
            // Embedded PC, so cascade down its PK fields
            Persistable pc = (Persistable)value;
            pc.dnCopyKeyFieldsFromObjectId(this, pc.dnGetObjectId());
            return;
        }

        fm.storeObjectField(fieldNumber, value);
    }

    public boolean fetchBooleanField(int fieldNumber)
    {
        return fm.fetchBooleanField(fieldNumber);
    }

    public byte fetchByteField(int fieldNumber)
    {
        return fm.fetchByteField(fieldNumber);
    }

    public char fetchCharField(int fieldNumber)
    {
        return fm.fetchCharField(fieldNumber);
    }

    public double fetchDoubleField(int fieldNumber)
    {
        return fm.fetchDoubleField(fieldNumber);
    }

    public float fetchFloatField(int fieldNumber)
    {
        return fm.fetchFloatField(fieldNumber);
    }

    public int fetchIntField(int fieldNumber)
    {
        return fm.fetchIntField(fieldNumber);
    }

    public long fetchLongField(int fieldNumber)
    {
        return fm.fetchLongField(fieldNumber);
    }

    public short fetchShortField(int fieldNumber)
    {
        return fm.fetchShortField(fieldNumber);
    }

    public String fetchStringField(int fieldNumber)
    {
        return fm.fetchStringField(fieldNumber);
    }

    public Object fetchObjectField(int fieldNumber)
    {
        return fm.fetchObjectField(fieldNumber);
    }
}