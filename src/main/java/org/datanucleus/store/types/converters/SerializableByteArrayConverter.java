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
package org.datanucleus.store.types.converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.datanucleus.exceptions.NucleusException;

/**
 * Convenience class to handle Java serialisation of a Serializable object to/from byte[].
 */
public class SerializableByteArrayConverter implements TypeConverter<Serializable, byte[]>
{

    private static final long serialVersionUID = 6620126332595722119L;

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toDatastoreType(java.lang.Object)
     */
    public byte[] toDatastoreType(Serializable memberValue)
    {
        byte[] bytes = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try
        {
            try
            {
                oos = new ObjectOutputStream(baos);
                oos.writeObject(memberValue);
                bytes = baos.toByteArray();
            }
            finally
            {
                try
                {
                    baos.close();
                }
                finally
                {
                    if (oos != null)
                    {
                        oos.close();
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            throw new NucleusException("Error serialising object of type " + memberValue.getClass().getName() + " to byte[]", ioe);
        }

        return bytes;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toMemberType(java.lang.Object)
     */
    public Serializable toMemberType(byte[] datastoreValue)
    {
        Serializable obj = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(datastoreValue);
        ObjectInputStream ois = null;
        try
        {
            try
            {
                ois = new ObjectInputStream(bais);
                obj = (Serializable)ois.readObject();
            }
            finally
            {
                try
                {
                    bais.close();
                }
                finally
                {
                    if (ois != null)
                    {
                        ois.close();
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new NucleusException("Error deserialising " + datastoreValue, e);
        }
        return obj;
    }

}
