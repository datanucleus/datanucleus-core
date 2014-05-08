/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
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

import junit.framework.TestCase;

/**
 * Series of tests for the String "matches" function.
 */
public class RegularExpressionConverterTest extends TestCase
{
    public void testParsePattern()
    {
        //testZZteteAAyyy.dede\--
        String str = "test.*tete.yyy\\.dede\\\\--";
        RegularExpressionConverter parser = new RegularExpressionConverter('Z', 'A', '\\');
        String parsed = parser.convert(str);
        String str_correct = "testZteteAyyy.dede\\\\\\\\--";
        assertTrue("Parsing mised expression gave erroneous string : " + parsed + " but should have been " + str_correct,parsed.equals(str_correct));

        // Empty string
        RegularExpressionConverter parser2 = new RegularExpressionConverter('Z', 'A', '\\');
        assertEquals("",parser2.convert(""));

        // Any character
        str = ".output.";
        RegularExpressionConverter parser3 = new RegularExpressionConverter('Z', 'A', '\\');
        parsed = parser3.convert(str);
        str_correct = "AoutputA";
        assertEquals(str_correct, parsed);

        // "slash\city"
        str = "\"slash\\city\"";
        RegularExpressionConverter parser4 = new RegularExpressionConverter('Z', 'A', '\\');
        parsed = parser4.convert(str);
        str_correct = "\"slash\\\\city\"";
        assertEquals(str_correct, parsed);
    }
}