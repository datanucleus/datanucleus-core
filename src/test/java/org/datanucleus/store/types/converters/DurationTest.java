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

import java.time.Duration;

import junit.framework.TestCase;

/**
 * Tests for java.time.Duration conversion.
 */
public class DurationTest extends TestCase
{
    public void testConversionToDouble()
    {
        DurationDoubleConverter conv = new DurationDoubleConverter();

        // Conversion from Duration to Double
        Duration d1 = Duration.ofNanos(5000005000l);
        Double d1Double = conv.toDatastoreType(d1);
        assertEquals(5, d1Double.longValue());
        assertEquals(5000l, (long) ((d1Double.doubleValue()*1E9) - (d1Double.longValue()*1E9)));

        Duration d2 = Duration.ofNanos(7999999999l);
        Double d2Double = conv.toDatastoreType(d2);
        assertEquals(7, d2Double.longValue());
        assertEquals(999999999l, (long) ((d2Double.doubleValue()*1E9) - (d2Double.longValue()*1E9)));

        // Conversion of Double to Duration
        Double dbl1 = Double.valueOf(8.000600010);
        Duration dbl1Dur = conv.toMemberType(dbl1);
        assertEquals(8, dbl1Dur.getSeconds());
        assertEquals(600010, dbl1Dur.getNano(), 1);

        Double dbl2 = Double.valueOf(5.070000099);
        Duration dbl2Dur = conv.toMemberType(dbl2);
        assertEquals(5, dbl2Dur.getSeconds());
        assertEquals(70000099, dbl2Dur.getNano(), 1);
    }
}