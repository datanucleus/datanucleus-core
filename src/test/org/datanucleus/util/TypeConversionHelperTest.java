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
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.datanucleus.util.TypeConversionHelper;

import junit.framework.TestCase;

/**
 * Unit tests for type conversions.
 */
public class TypeConversionHelperTest extends TestCase
{
    /**
     * Test for conversion of Timestamp to String.
     */
    public void testStringToTimestamp()
    {
        // Try a standard JDBC format string
        String timestampStr = "2005-06-06 10:00:00.000000000";
        Calendar sampleCal = new GregorianCalendar();
        Timestamp ts = null;
        try
        {
            ts = TypeConversionHelper.stringToTimestamp(timestampStr, sampleCal);
        }
        catch (Exception e)
        {
            fail("Exception thrown while converting timestamp : " + e.getMessage());
        }

        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(ts.getTime());
        assertEquals("Year in Timestamp is incorrect", cal.get(Calendar.YEAR), 2005);
        assertEquals("Month in Timestamp is incorrect", cal.get(Calendar.MONTH), 5);
        assertEquals("Day in Timestamp is incorrect", cal.get(Calendar.DAY_OF_MONTH), 6);
        assertEquals("Hour in Timestamp is incorrect", cal.get(Calendar.HOUR_OF_DAY), 10);
        assertEquals("Minute in Timestamp is incorrect", cal.get(Calendar.MINUTE), 0);
        assertEquals("Second in Timestamp is incorrect", cal.get(Calendar.SECOND), 0);

        // Try an example of the crap that Oracles driver emits.
        timestampStr = "2005-6-16.9.31. 14.0";
        try
        {
            ts = TypeConversionHelper.stringToTimestamp(timestampStr, sampleCal);
        }
        catch (Exception e)
        {
            fail("Exception thrown while converting timestamp : " + e.getMessage());
        }
    }
    
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
        byte[] b = TypeConversionHelper.getByteArrayFromBigDecimalArray(a);
        BigDecimal c[] = (BigDecimal[]) TypeConversionHelper.getBigDecimalArrayFromByteArray(b);
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
        byte[] b = TypeConversionHelper.getByteArrayFromBigIntegerArray(a);
        BigInteger c[] = (BigInteger[]) TypeConversionHelper.getBigIntegerArrayFromByteArray(b);
        assertEquals(a[0],c[0]);
        assertEquals(a[1],c[1]);
    }    
}