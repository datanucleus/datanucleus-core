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

import java.nio.ByteBuffer;

import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.TypeConversionHelper;

/**
 * Convenience class to handle Java serialisation of a int[] object to/from ByteBuffer.
 */
public class IntArrayByteBufferConverter implements TypeConverter<int[], ByteBuffer>
{
    private static final long serialVersionUID = -426707949213710681L;

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toDatastoreType(java.lang.Object)
     */
    public ByteBuffer toDatastoreType(int[] memberValue)
    {
        if (memberValue == null)
        {
            return null;
        }
        byte[] bytes = TypeConversionHelper.getByteArrayFromIntArray(memberValue);
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        return byteBuffer;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toMemberType(java.lang.Object)
     */
    public int[] toMemberType(ByteBuffer datastoreValue)
    {
        if (datastoreValue == null)
        {
            return null;
        }
        return TypeConversionHelper.getIntArrayFromByteArray(datastoreValue.array());
    }
}