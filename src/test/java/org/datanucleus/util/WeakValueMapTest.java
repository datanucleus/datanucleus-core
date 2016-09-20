package org.datanucleus.util;

import java.util.Map;

import org.datanucleus.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Tests the functionality of weak-value reference map.
 */
public class WeakValueMapTest extends ReferenceValueMapTestCase
{
    /**
     * Used by the JUnit framework to construct tests.  Normally, programmers
     * would never explicitly use this constructor.
     *
     * @param name   Name of the <tt>TestCase</tt>.
     */

    public WeakValueMapTest(String name)
    {
        super(name);
    }


    protected Map newReferenceValueMap()
    {
        return new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.WEAK);
    }
}
