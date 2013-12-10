/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import org.datanucleus.util.StringUtils;

import junit.framework.TestCase;

/**
 * Series of tests for string manipulation utilities.
 */
public class StringUtilsTest extends TestCase
{
    /**
     * Start of the test, so log it and initialise.
     * @param name Name of the test case (not used)
     */
    public StringUtilsTest(String name)
    {
        super(name);
    }
    
    /**
     * Test for isWhitespace method.
     */
    public void testIsWhitespace()
    {
        assertTrue("null String should have been confirmed as whitespace but wasn't",
            StringUtils.isWhitespace(null));
        assertTrue("empty String should have been confirmed as whitespace but wasn't",
            StringUtils.isWhitespace("   "));
        assertTrue("Non empty String should have been defined as not whitespace but was",
            !StringUtils.isWhitespace("JPOX"));
        assertTrue("Partly empty String should have been defined as not whitespace but was",
            !StringUtils.isWhitespace("   JPOXWorld "));
    }
    
    /**
     * Test for areStringsEqual method
     */
    public void testAreStringsEqual()
    {
        assertTrue("Identical Strings should have been decreed equal but weren't",
            StringUtils.areStringsEqual("JPOX","JPOX"));
        assertTrue("Non-identical Strings should have been decreed as non-equal but were",
            !StringUtils.areStringsEqual("JPOX","Kodo"));
        assertTrue("Null strings should have been decreed equal but weren't",
            StringUtils.areStringsEqual(null,null));
        assertTrue("Null and non-null Strings should have been decreed as non-equal, but were",
            !StringUtils.areStringsEqual("null",null));
    }
    
    /**
     * Test for left-aligned padded strings.
     */
    public void testLeftAlignedPaddedString()
    {
        assertTrue("String left aligned (extending the string) was incorrect",
            StringUtils.leftAlignedPaddedString("JPOX",10).equals("JPOX      "));
        assertTrue("String left aligned (same length) was incorrect",
            StringUtils.leftAlignedPaddedString("JPOX",4).equals("JPOX"));
    }
    
    /**
     * Test for right-aligned padded strings.
     */
    public void testRightAlignedPaddedString()
    {
        assertTrue("String right aligned (extending the string) was incorrect",
            StringUtils.rightAlignedPaddedString("JPOX",10).equals("      JPOX"));
        assertTrue("String right aligned (same length) was incorrect",
            StringUtils.rightAlignedPaddedString("JPOX",4).equals("JPOX"));
    }
    
    /**
     * Test for splitting strings at identifiers.
     */
    public void testSplit()
    {
        String[] tokens=StringUtils.split("JPOX Kodo JDOGenie"," ");
        assertTrue("First token of split string is incorrect", tokens[0].equals("JPOX"));
        assertTrue("Second token of split string is incorrect", tokens[1].equals("Kodo"));
        assertTrue("Third token of split string is incorrect", tokens[2].equals("JDOGenie"));
        
        String[] tokens2=StringUtils.split("Andy Jefferson::Erik Bengtson::David Ezzio::Eagle","::");
        assertTrue("First token of split string is incorrect", tokens2[0].equals("Andy Jefferson"));
        assertTrue("Second token of split string is incorrect", tokens2[1].equals("Erik Bengtson"));
        assertTrue("Third token of split string is incorrect", tokens2[2].equals("David Ezzio"));
        assertTrue("Fourth token of split string is incorrect", tokens2[3].equals("Eagle"));
    }
    
    /**
     * Test for conversion of a boolean array to a String.
     */
    public void testBooleanArrayToString()
    {
        boolean[] values=new boolean[] {true, false, false, true, true};
        assertTrue("String version of boolean array [5] is incorrect", StringUtils.booleanArrayToString(values).equals("[YNNYY]"));
        
        assertTrue("String version of null boolean array is incorrect", StringUtils.booleanArrayToString(null).equals("null"));
        
        boolean[] values2 = new boolean[] {false};
        assertTrue("String version of boolean array [1] is incorrect", StringUtils.booleanArrayToString(values2).equals("[N]"));
    }
    
    /**
     * Test for conversion of a int array to a String.
     */
    public void testIntArrayToString()
    {
        int[] values=new int[] {9876, 5432, 1, -6, 5};
        assertTrue("String version of int array [5] is incorrect", StringUtils.intArrayToString(values).equals("[9876, 5432, 1, -6, 5]"));
        
        assertTrue("String version of null int array is incorrect", StringUtils.intArrayToString(null).equals("null"));
        
        int[] values2 = new int[] {1234567};
        assertTrue("String version of int array [1] is incorrect", StringUtils.intArrayToString(values2).equals("[1234567]"));
    }
    
    /**
     * Test for conversion of a collection to a String.
     */
    public void testCollectionToString()
    {
        Collection coll=new ArrayList();
        coll.add("JPOX version 1.0");
        coll.add("JPOX version 1.1.0-alpha-3");
        coll.add("BCEL version 5.1");
        assertTrue("String version of Collection is incorrect", 
            StringUtils.collectionToString(coll).equals("JPOX version 1.0, JPOX version 1.1.0-alpha-3, BCEL version 5.1"));

        assertTrue("String version of empty Collection is incorrect", StringUtils.collectionToString(new HashSet()).equals("<none>"));
    }

    private static final int NUM_RANDOM_CASES = 5000;
    private static final String[][] CUSTOM_CASES =
    {
        { "0",          "0" },
        { "12.34",      "12.34" },
        { "1.234",      "1234e-3" },
        { ".1234E12",   "123400000000" },
        { "-.1234",     "-1234e-4" },
        { "1234",       ".1234e+4" },
        { ".75E10",     ".75e+10" },
        { "-.75E10",    "-7.5e+9" },
        { ".5E-9",      "5e-10" }
    };

    public void testExponentialFormatCustomValues() throws Throwable
    {
        for (int i = 0; i < CUSTOM_CASES.length; ++i)
            assertEquals(CUSTOM_CASES[i][0], 
                StringUtils.exponentialFormatBigDecimal(new BigDecimal(CUSTOM_CASES[i][1])));
    }

    public void testExponentialFormatRandomValues() throws Throwable
    {
        Random rnd = new Random(0L);

        for (int i = 0; i < NUM_RANDOM_CASES; ++i)
        {
            BigDecimal bd1 = new BigDecimal(new BigInteger(128, rnd), rnd.nextInt(100));
            String s = StringUtils.exponentialFormatBigDecimal(bd1);
            BigDecimal bd2 = new BigDecimal(StringUtils.exponentialFormatBigDecimal(bd1));

            assertEquals("Formatting " + bd1 + " yielded " + s + " which doesn't equal", 0, bd1.compareTo(bd2));
        }
    }
}