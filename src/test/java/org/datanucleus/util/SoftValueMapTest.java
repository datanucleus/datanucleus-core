package org.datanucleus.util;

import java.util.Map;

import org.datanucleus.util.SoftValueMap;

/**
 * Tests the functionality of soft referenced value map.
 */
public class SoftValueMapTest extends ReferenceValueMapTestCase
{
    public SoftValueMapTest(String name)
    {
        super(name);
    }

    protected Map newReferenceValueMap()
    {
        // TODO Use ConcurrentReferenceHashMap?
        return new SoftValueMap();
    }
}
