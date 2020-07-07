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

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import org.datanucleus.util.TypeConversionHelper;

/**
 * Convenience class to handle Java serialisation of a BigDecimal[] object to/from ByteBuffer.
 */
public class BigDecimalArrayByteBufferConverter implements TypeConverter<BigDecimal[], ByteBuffer>
{
    private static final long serialVersionUID = 5829673311829818607L;

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toDatastoreType(java.lang.Object)
     */
    public ByteBuffer toDatastoreType(BigDecimal[] memberValue)
    {
        if (memberValue == null)
        {
            return null;
        }
        byte[] bytes = TypeConversionHelper.getByteArrayFromBigDecimalArray(memberValue);
        return ByteBuffer.wrap(bytes);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.TypeConverter#toMemberType(java.lang.Object)
     */
    public BigDecimal[] toMemberType(ByteBuffer datastoreValue)
    {
        if (datastoreValue == null)
        {
            return null;
        }
        return TypeConversionHelper.getBigDecimalArrayFromByteArray(datastoreValue.array());
    }
}