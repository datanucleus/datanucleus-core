/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.datanucleus.store.types.converters.ArrayConversionHelper;

import junit.framework.TestCase;

/**
 * Unit tests for type conversions.
 */
public class TypeConversionHelperTest extends TestCase
{
    /**
     * Test BigDecimal conversions
     */
    public void testBigDecimalConversion()
    {
        BigDecimal a[] = new BigDecimal[6];
        a[0] = new BigDecimal(10.32);
        a[1] = new BigDecimal(102.32);
        a[2] = new BigDecimal(-2322232323102.3323232);
        a[3] = new BigDecimal(232323232322232323102.3323232);
        a[4] = new BigDecimal("2007908.54548");
        a[5] = new BigDecimal("64564645656.78657");        
        byte[] b = ArrayConversionHelper.getByteArrayFromBigDecimalArray(a);
        BigDecimal c[] = ArrayConversionHelper.getBigDecimalArrayFromByteArray(b);
        assertEquals(a[0],c[0]);
        assertEquals(a[1],c[1]);
        assertEquals(a[2],c[2]);
        assertEquals(a[3],c[3]);
        assertEquals(a[4],c[4]);
        assertEquals(a[5],c[5]);
    }
    
    /**
     * Test BigInteger conversions
     */
    public void testBigIntegerConversion()
    {
        BigInteger a[] = new BigInteger[2];
        a[0] = new BigInteger(""+Long.MAX_VALUE);
        a[1] = new BigInteger(""+Long.MIN_VALUE);
        byte[] b = ArrayConversionHelper.getByteArrayFromBigIntegerArray(a);
        BigInteger c[] = ArrayConversionHelper.getBigIntegerArrayFromByteArray(b);
        assertEquals(a[0],c[0]);
        assertEquals(a[1],c[1]);
    }    
}