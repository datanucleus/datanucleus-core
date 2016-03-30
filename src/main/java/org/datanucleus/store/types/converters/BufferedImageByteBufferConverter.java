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
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Convenience class to handle Java serialisation of a BufferedImage object to/from ByteBuffer.
 */
public class BufferedImageByteBufferConverter implements TypeConverter<BufferedImage, ByteBuffer>
{

    private static final long serialVersionUID = 585211414298721468L;

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toDatastoreType(java.lang.Object)
     */
    public ByteBuffer toDatastoreType(BufferedImage memberValue)
    {
        if (memberValue == null)
        {
            return null;
        }

        byte[] bytes = null;
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
            ImageIO.write(memberValue, "jpg", baos);
            bytes = baos.toByteArray();
            baos.close();
        }
        catch (IOException e)
        {
        }
        return ByteBuffer.wrap(bytes);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toMemberType(java.lang.Object)
     */
    public BufferedImage toMemberType(ByteBuffer datastoreValue)
    {
        if (datastoreValue == null || datastoreValue.limit() == 0)
        {
            return null;
        }

        BufferedImage obj = null;
        byte [] dataStoreValueInBytes = new byte[datastoreValue.remaining()];
        datastoreValue.get(dataStoreValueInBytes);
        try
        {
            obj = ImageIO.read(new ByteArrayInputStream(dataStoreValueInBytes));
        }
        catch (IOException e)
        {
        }
        return obj;
    }
}
