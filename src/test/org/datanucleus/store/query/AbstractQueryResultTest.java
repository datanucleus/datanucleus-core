package org.datanucleus.store.query;

import junit.framework.TestCase;
import junit.framework.Assert;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Test case for AbstractQueryResult
 */
public class AbstractQueryResultTest extends TestCase
{
    List list = new AbstractQueryResultImpl(null);

    Integer i10 = new Integer(10);

    Integer i20 = new Integer(20);

    Integer i30 = new Integer(30);

    public AbstractQueryResultTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        list.add(i10);
        list.add(i20);
        list.add(i30);
    }

    public void testSubList()
    {
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(1, list.subList(1, 2).size());
        Assert.assertEquals(3, list.subList(0, 3).size());

        List sList = list.subList(1, 2);
        Object o = sList.get(0);
        Assert.assertEquals(i20, o);

        try
        {
            sList.get(1);// must throw exception
            Assert.assertTrue(false);// never executed
        }
        catch (IndexOutOfBoundsException e)
        {
        }

        sList = list.subList(0, 3);
        o = sList.get(2);
        Assert.assertEquals(i30, o);
    }

    public void testToArray()
    {
        Object[] array = list.toArray();
        Assert.assertEquals(3, array.length);
        Assert.assertEquals(i10, array[0]);
        Assert.assertEquals(i20, array[1]);
        Assert.assertEquals(i30, array[2]);
    }

    public void testToArray2()
    {
        Object[] array = list.toArray(new Object[4]);
        Assert.assertEquals(4, array.length);
        Assert.assertEquals(i10, array[0]);
        Assert.assertEquals(i20, array[1]);
        Assert.assertEquals(i30, array[2]);
        Assert.assertEquals(null, array[3]);
    }

    /**
     * test implementation
     */
    public static class AbstractQueryResultImpl extends AbstractQueryResult
    {
        private ArrayList list = new ArrayList();

        public AbstractQueryResultImpl(Query query)
        {
            super(query);
        }

        protected void closeResults()
        {
        }

        protected void closingConnection()
        {
        }

        public boolean add(Object o)
        {
            return list.add(o);
        }

        public boolean equals(Object o)
        {
            return this == o;
        }

        public Object get(int index)
        {
            return list.get(index);
        }

        public Iterator iterator()
        {
            return list.iterator();
        }

        public ListIterator listIterator()
        {
            return list.listIterator();
        }

        public int size()
        {
            return list.size();
        }
    }
}