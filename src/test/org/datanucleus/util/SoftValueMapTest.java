package org.datanucleus.util;

import org.datanucleus.util.ReferenceValueMap;
import org.datanucleus.util.SoftValueMap;

/**
 * Tests the functionality of {@link SoftValueMap}.
 */
public class SoftValueMapTest extends ReferenceValueMapTestCase
{
    /**
     * Used by the JUnit framework to construct tests.  Normally, programmers
     * would never explicitly use this constructor.
     *
     * @param name   Name of the <tt>TestCase</tt>.
     */

    public SoftValueMapTest(String name)
    {
        super(name);
    }


    protected ReferenceValueMap newReferenceValueMap()
    {
        return new SoftValueMap();
    }
}
