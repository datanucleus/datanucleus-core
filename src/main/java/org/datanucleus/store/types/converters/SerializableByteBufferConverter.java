/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.datanucleus.exceptions.NucleusDataStoreException;

/**
 * Convenience class to handle Java serialisation of a Serializable object to/from ByteBuffer.
 */
public class SerializableByteBufferConverter implements TypeConverter<Serializable, ByteBuffer>
{

    private static final long serialVersionUID = 585211414298721468L;

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toDatastoreType(java.lang.Object)
     */
    public ByteBuffer toDatastoreType(Serializable memberValue)
    {
        if (memberValue == null)
        {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos))
            {
                oos.writeObject(memberValue);
            }
            return ByteBuffer.wrap(baos.toByteArray());
        }
        catch (IOException ioe)
        {
            throw new NucleusDataStoreException("Error serialising object of type " + memberValue.getClass().getName() + " to ByteBuffer", ioe);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toMemberType(java.lang.Object)
     */
    public Serializable toMemberType(ByteBuffer datastoreValue)
    {
        if (datastoreValue == null || datastoreValue.limit() == 0)
        {
            return null;
        }

        byte[] dataStoreValueInBytes = new byte[datastoreValue.remaining()];
        datastoreValue.get(dataStoreValueInBytes);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(dataStoreValueInBytes))
        {
            try (ObjectInputStream ois = new ObjectInputStream(bais))
            {
                return (Serializable) ois.readObject();
            }
        }
        catch (Exception e)
        {
            throw new NucleusDataStoreException("Error deserialising " + datastoreValue, e);
        }
    }
}
