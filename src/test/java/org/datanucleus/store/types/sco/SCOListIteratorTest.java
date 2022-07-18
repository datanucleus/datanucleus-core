/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.sco;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.datanucleus.store.types.SCOListIterator;

import junit.framework.TestCase;

/**
 * Component tests for the SCOListIterator class.
 */
public class SCOListIteratorTest extends TestCase
{
    public SCOListIteratorTest(String name)
    {
        super(name);
    }

    /**
     * Test of next/previous behaviour.
     */
    public void testIteratorNextPrevious()
    {
        LinkedList<String> list = new LinkedList<String>();
        list.add("First");
        list.add("Second");
        list.add("Third");
        List<String> sco = new ArrayList<String>();
        sco.addAll(list);

        SCOListIterator<String> iter = new SCOListIterator<>(sco, null, list, null, true, 0);

        assertTrue("hasNext returns false!", iter.hasNext());
        try
        {
            iter.set("Fifth");
            fail("set() before next()/previous() succeeded but should fail");
        }
        catch (IllegalStateException ise)
        {
            // Expected
        }

        assertTrue("hasNext returns false!", iter.hasNext());
        Object obj = iter.next();
        assertEquals("First", obj);

        // Test use of set() now that next() has been called.
        iter.set("Fifth"); // Set the first element

        assertTrue("hasNext returns false!", iter.hasNext());
        obj = iter.next();
        assertEquals("Second", obj);

        assertTrue("hasNext returns false!", iter.hasNext());
        obj = iter.next();
        assertEquals("Third", obj);

        assertFalse("hasNext returns true!", iter.hasNext());
        try
        {
            iter.next();
            fail("call to next() succeeded but is already at end of list");
        }
        catch (NoSuchElementException nsee)
        {
            // Expected
        }
    }

    /**
     * Test of next/previous behaviour with an iterator starting at -1.
     */
    public void testIteratorNextPreviousStartNegative()
    {
        LinkedList<String> list = new LinkedList<String>();
        list.add("First");
        list.add("Second");
        list.add("Third");
        List<String> sco = new ArrayList<String>();
        sco.addAll(list);

        SCOListIterator<String> iter = new SCOListIterator<>(sco, null, list, null, true, -1);

        assertTrue("hasNext returns false!", iter.hasNext());
        try
        {
            iter.set("Fifth");
            fail("set() before next()/previous() succeeded but should fail");
        }
        catch (IllegalStateException ise)
        {
            // Expected
        }

        assertTrue("hasNext returns false!", iter.hasNext());
        Object obj = iter.next();
        assertEquals("First", obj);

        // Test use of set() now that next() has been called.
        iter.set("Fifth"); // Set the first element

        assertTrue("hasNext returns false!", iter.hasNext());
        obj = iter.next();
        assertEquals("Second", obj);

        assertTrue("hasNext returns false!", iter.hasNext());
        obj = iter.next();
        assertEquals("Third", obj);

        assertFalse("hasNext returns true!", iter.hasNext());
        try
        {
            iter.next();
            fail("call to next() succeeded but is already at end of list");
        }
        catch (NoSuchElementException nsee)
        {
            // Expected
        }
    }

    /**
     * Test of add/remove/set behaviour.
     */
    public void testIteratorAddRemoveSet()
    {
        LinkedList<String> list = new LinkedList<String>();
        list.add("First");
        List<String> sco = new ArrayList<String>();
        sco.addAll(list);

        SCOListIterator<String> iter = new SCOListIterator<>(sco, null, list, null, true, 0);

        try
        {
            iter.remove();
            fail("remove() before next()/previous() succeeded but should fail");
        }
        catch (IllegalStateException ise)
        {
            // Expected
        }

        try
        {
            iter.set("BAD");
            fail("set() before next()/previous() succeeded but should fail");
        }
        catch (IllegalStateException ise)
        {
            // Expected
        }

        iter.next();
        iter.set("Second");
        assertEquals("Second", iter.previous());
        assertEquals("Second", iter.next());
        iter.remove();

        iter.add("Third");
        iter.add("Fourth");
        assertEquals("Fourth", iter.previous());
        assertEquals("Fourth", iter.next());
        iter.remove();

        try
        {
            iter.remove();
            fail("remove() after remove() succeeded but should fail");
        }
        catch (IllegalStateException ise)
        {
            // Expected
            assertEquals(1, sco.size());
        }

        try
        {
            iter.set("BAD");
            fail("set() after remove() succeeded but should fail");
        }
        catch (IllegalStateException ise)
        {
            // Expected
        }

        iter.add("Fifth");

        try
        {
            iter.remove();
            fail("remove() after add() succeeded but should fail");
        }
        catch (IllegalStateException ise)
        {
            // Expected
        }

        try
        {
            iter.set("BAD");
            fail("set() after add() succeeded but should fail");
        }
        catch (IllegalStateException ise)
        {
            // Expected
        }

        iter.previous();
        iter.remove();
        iter.add("Sixth");
        iter.previous();
        iter.set("Seventh");
        assertEquals("Seventh", iter.next());
        assertEquals("Seventh", iter.previous());
        iter.remove();
        iter.previous();
        iter.remove();

        assertTrue(sco.isEmpty());
    }
}
