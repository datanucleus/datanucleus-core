/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.valuegenerator;

import org.datanucleus.store.valuegenerator.UUIDHexGenerator;

import junit.framework.TestCase;

/**
 * Unit tests for the "uuid-hex" identity generation.
 */
public class UUIDHexGeneratorTest extends TestCase
{
    /**
     * Test for the length of the generated "uuid-hex" string.
     * Should be 32 characters as per the JDO2 spec.
     */
    public void testStringLength()
    {
        UUIDHexGenerator gen = new UUIDHexGenerator("Test", null);
        for (int i=0; i<10; i++)
        {
            Object id = gen.next();
            assertEquals(32, id.toString().length());
        }
    }

    /**
     * Test for equality of any identifiers generated. Should be unique.
     */
    public void testEquality()
    {
        // Create 1000 identifiers
        String[] ids = new String[1000];
        UUIDHexGenerator gen = new UUIDHexGenerator("Test", null);
        for (int i=0;i<ids.length;i++)
        {
            ids[i] = (String)gen.next();
        }

        // Check for equality of any of them
        for (int i=0;i<ids.length;i++)
        {
            for (int j=0;j<ids.length;j++)
            {
                if (i != j)
                {
                    assertFalse("Two uuid-hex identifiers are equal yet should be unique! : " + ids[i] + " and " + ids[j], ids[i].equals(ids[j]));
                }
            }
        }
    }
}