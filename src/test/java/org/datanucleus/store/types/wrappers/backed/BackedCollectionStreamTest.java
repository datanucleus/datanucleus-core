/**********************************************************************
Copyright (c) 2026 DataNucleus contributors. All rights reserved.
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
package org.datanucleus.store.types.wrappers.backed;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassLoaderResolverImpl;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PersistenceNucleusContextImpl;
import org.datanucleus.PropertyNames;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.BackedSCOStoreManager;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.types.scostore.ListStore;
import org.datanucleus.store.types.scostore.SetStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that stream(), parallelStream() and spliterator() of the backed collection wrappers return the
 * elements of the backing store when not caching (datanucleus.cache.collections=false), the same as iterator().
 */
public class BackedCollectionStreamTest
{
    private static final java.util.List<String> ELEMENTS = Arrays.asList("First", "Second", "Third");

    private DNStateManager sm;

    private AbstractMemberMetaData mmd;

    @Before
    public void setUp()
    {
        Map<String, Object> props = new HashMap<>();
        props.put(PropertyNames.PROPERTY_CACHE_COLLECTIONS, "false");
        props.put(PropertyNames.PROPERTY_CACHE_COLLECTIONS_LAZY, "false");
        PersistenceNucleusContextImpl nucCtx = new PersistenceNucleusContextImpl(null, props)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized void initialise()
            {
            }
        };
        ClassLoaderResolver clr = new ClassLoaderResolverImpl();

        // Backing store returning ELEMENTS, as the datastore would
        Object backingStore = stub(new Class[] {ListStore.class, SetStore.class}, (name, args) ->
        {
            if (name.equals("iterator") || name.equals("listIterator"))
            {
                return ELEMENTS.listIterator();
            }
            return name.equals("size") ? ELEMENTS.size() : null;
        });
        Object storeMgr = stub(new Class[] {StoreManager.class, BackedSCOStoreManager.class},
            (name, args) -> name.equals("getBackingStoreForField") ? backingStore : null);
        Object ec = stub(new Class[] {ExecutionContext.class}, (name, args) ->
        {
            if (name.equals("getNucleusContext"))
            {
                return nucCtx;
            }
            return name.equals("getClassLoaderResolver") ? clr : null;
        });
        sm = (DNStateManager) stub(new Class[] {DNStateManager.class}, (name, args) ->
        {
            if (name.equals("getExecutionContext"))
            {
                return ec;
            }
            else if (name.equals("getStoreManager"))
            {
                return storeMgr;
            }
            return name.equals("getObjectAsPrintable") ? "owner" : null;
        });

        mmd = new FieldMetaData(null, "items");
        mmd.setPersistenceModifier(FieldPersistenceModifier.PERSISTENT);
    }

    @Test
    public void testList()
    {
        checkStreams(new List<String>(sm, mmd));
    }

    @Test
    public void testArrayList()
    {
        checkStreams(new ArrayList<String>(sm, mmd));
    }

    @Test
    public void testLinkedList()
    {
        checkStreams(new LinkedList<String>(sm, mmd));
    }

    @Test
    public void testStack()
    {
        checkStreams(new Stack<String>(sm, mmd));
    }

    @Test
    public void testVector()
    {
        checkStreams(new Vector<String>(sm, mmd));
    }

    @Test
    public void testSet()
    {
        checkStreams(new Set<String>(sm, mmd));
    }

    @Test
    public void testHashSet()
    {
        checkStreams(new HashSet<String>(sm, mmd));
    }

    @Test
    public void testLinkedHashSet()
    {
        checkStreams(new LinkedHashSet<String>(sm, mmd));
    }

    @Test
    public void testSortedSet()
    {
        checkStreams(new SortedSet<String>(sm, mmd));
    }

    @Test
    public void testTreeSet()
    {
        checkStreams(new TreeSet<String>(sm, mmd));
    }

    @Test
    public void testCollection()
    {
        checkStreams(new Collection<String>(sm, mmd));
    }

    @Test
    public void testQueue()
    {
        checkStreams(new Queue<String>(sm, mmd));
    }

    @Test
    public void testPriorityQueue()
    {
        PriorityQueue<String> coll = new PriorityQueue<>(sm, mmd);
        Assert.assertFalse("Wrapper should not be caching", coll.useCache);

        // spliterator() isn't checked since java.util.PriorityQueue makes it final and it doesn't see the backing store
        Assert.assertEquals(ELEMENTS, coll.stream().sorted().collect(Collectors.toList()));
        Assert.assertEquals(ELEMENTS, coll.parallelStream().sorted().collect(Collectors.toList()));
    }

    private static void checkStreams(java.util.Collection<String> coll)
    {
        java.util.List<String> viaIterator = new java.util.ArrayList<>();
        coll.iterator().forEachRemaining(viaIterator::add);
        Assert.assertEquals(ELEMENTS, viaIterator);

        Assert.assertEquals(ELEMENTS, coll.stream().collect(Collectors.toList()));
        Assert.assertEquals(ELEMENTS, coll.parallelStream().sorted().collect(Collectors.toList()));
        Assert.assertEquals(ELEMENTS, StreamSupport.stream(coll.spliterator(), false).collect(Collectors.toList()));
    }

    private interface StubHandler
    {
        Object invoke(String methodName, Object[] args);
    }

    /**
     * Creates a proxy implementing the given interfaces, answering the methods handled by the handler and
     * returning null (or the primitive default) for everything else.
     */
    private static Object stub(Class[] interfaces, StubHandler handler)
    {
        return Proxy.newProxyInstance(BackedCollectionStreamTest.class.getClassLoader(), interfaces, (proxy, method, args) ->
        {
            if (method.getName().equals("toString"))
            {
                return interfaces[0].getSimpleName() + " stub";
            }
            Object result = handler.invoke(method.getName(), args);
            if (result == null && method.getReturnType().isPrimitive())
            {
                if (method.getReturnType() == boolean.class)
                {
                    return false;
                }
                return method.getReturnType() == void.class ? null : 0;
            }
            return result;
        });
    }
}
