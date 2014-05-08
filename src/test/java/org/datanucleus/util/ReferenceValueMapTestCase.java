package org.datanucleus.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.datanucleus.util.ReferenceValueMap;

/**
 * Abstract base class for tests that test the functionality of a
 * {@link ReferenceValueMap}.
 */
public abstract class ReferenceValueMapTestCase extends TestCase
{
    private static final int NUM_TEST_ENTRIES = 50;

    /**
     * Used by the JUnit framework to construct tests.  Normally, programmers
     * would never explicitly use this constructor.
     *
     * @param name   Name of the <tt>TestCase</tt>.
     */
    public ReferenceValueMapTestCase(String name)
    {
        super(name);
    }

    protected abstract ReferenceValueMap newReferenceValueMap();

    public void testMemoryReclamation()
    {
        ReferenceValueMap map = newReferenceValueMap();
        Runtime rt = Runtime.getRuntime();

        rt.gc();

        int added = 0;
        int size;
        long freeMem;

        /*
         * Fill the map with entries until some references get cleared.  GC
         * should cause references to be cleared well before memory fills up.
         */
        do
        {
            freeMem = rt.freeMemory();
            String key = "" + added;
            Object value = new Integer(added++);

            map.put(key, value);

            size = map.size();
        } while (size == added);

        assertTrue(size < added);

        NucleusLogger.GENERAL.info("ReferenceValueMap " + (added - size) + " entries out of " + added + " cleared when free memory was " + (int)(freeMem / 1024) + "KB");
    }


    public void testBasicFunction()
    {
        ReferenceValueMap map = newReferenceValueMap();
        String[] keyArray = new String[NUM_TEST_ENTRIES];
        Integer[] valueArray = new Integer[NUM_TEST_ENTRIES];

        for (int i = 0; i < keyArray.length; i++)
        {
            keyArray[i] = "" + i;
            valueArray[i] = new Integer(i);

            map.put(keyArray[i], valueArray[i]);
        }

        checkMapContents(map, keyArray, valueArray);

        Map map2 = (Map)map.clone();
        map.clear();

        assertEquals(0, map.size());

        map.putAll(map2);

        checkMapContents(map, keyArray, valueArray);

        assertEquals(NUM_TEST_ENTRIES, map.size());

        for (int i = 0; i < keyArray.length; ++i)
        {
            Object value = map.remove(keyArray[i]);
            assertEquals(valueArray[i], value);
        }

        assertEquals(0, map.size());
    }


    /**
     * Tests Map.get(), Map.containsKey(), Map.containsValue(), Map.entrySet(),
     * Map.keySet(), Map.values()
     */
    protected void checkMapContents(Map map, String[] keyArray, Integer[] valueArray)
    {
        assertEquals(keyArray.length, map.size());

        for (int i = 0; i < keyArray.length; i++)
            assertTrue(map.containsKey(keyArray[i]));

        assertTrue(!map.containsKey("bitbucket"));

        for (int i = 0; i < valueArray.length; i++)
            assertTrue(map.containsValue(valueArray[i]));

        assertTrue(!map.containsValue("bitbucket"));

        for (int i = 0; i < keyArray.length; i++)
            assertEquals(i, ((Integer)map.get(keyArray[i])).intValue());

        Set set = map.entrySet();
        Iterator it = set.iterator();

        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            String s = (String)entry.getKey();
            Integer i = (Integer)entry.getValue();
            assertTrue(map.containsKey(s));
            assertTrue(map.containsValue(i));
        }

        assertTrue(map.keySet().containsAll(Arrays.asList(keyArray)));
        assertTrue(map.values().containsAll(Arrays.asList(valueArray)));
    }
}