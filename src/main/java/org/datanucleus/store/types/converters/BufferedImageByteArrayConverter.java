/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.datanucleus.exceptions.NucleusDataStoreException;

/**
 * Convenience class to handle Java serialisation of a BufferedImage object to/from byte[].
 */
public class BufferedImageByteArrayConverter implements TypeConverter<BufferedImage, byte[]>
{

    private static final long serialVersionUID = 585211414298721468L;

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toDatastoreType(java.lang.Object)
     */
    public byte[] toDatastoreType(BufferedImage memberValue)
    {
        if (memberValue == null)
        {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(8192))
        {
            ImageIO.write(memberValue, "jpg", baos);
            return baos.toByteArray();
        }
        catch (IOException ioe)
        {
            throw new NucleusDataStoreException("Error serialising object of type BufferedImage to byte array", ioe);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toMemberType(java.lang.Object)
     */
    public BufferedImage toMemberType(byte[] datastoreValue)
    {
        if (datastoreValue == null || datastoreValue.length == 0)
        {
            return null;
        }

        try
        {
            return ImageIO.read(new ByteArrayInputStream(datastoreValue));
        }
        catch (IOException ioe)
        {
            throw new NucleusDataStoreException("Error deserialising image datastoreValue", ioe);
        }
    }
}
