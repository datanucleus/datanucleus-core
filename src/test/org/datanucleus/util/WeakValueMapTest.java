package org.datanucleus.util;

import org.datanucleus.util.ReferenceValueMap;
import org.datanucleus.util.WeakValueMap;

/**
 * Tests the functionality of {@link WeakValueMap}.
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


    protected ReferenceValueMap newReferenceValueMap()
    {
        return new WeakValueMap();
    }
}
