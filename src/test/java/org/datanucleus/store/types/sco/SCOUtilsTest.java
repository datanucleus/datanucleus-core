package org.datanucleus.store.types.sco;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.datanucleus.FetchPlanState;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.types.SCOList;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.store.types.scostore.CollectionStore;

/**
 * Tests for org.datanucleus.sco.SCOUtils methods.
 */
public class SCOUtilsTest extends TestCase
{
    /*
     * Test method for SCOUtils.toArray(CollectionStore, StateManager)
     */
    public void testToArrayCollectionStoreStateManager()
    {
        java.util.List<String> elm = new java.util.ArrayList<>();
        elm.add("TEST1");
        elm.add("TEST2");
        String[] arr = SCOUtils.toArray(new BackingStore(elm), null, new String[2]);
        assertEquals(arr[0],"TEST1");
        assertEquals(arr[1],"TEST2");
    }

    /*
     * Test method for SCOUtils.toArray(CollectionStore, StateManager, Object[])
     */
    public void testToArrayCollectionStoreStateManagerObjectArray()
    {
        java.util.List<String> elm = new java.util.ArrayList<>();
        elm.add("TEST1");
        elm.add("TEST2");
        Object[] arr = SCOUtils.toArray(new BackingStore(elm),null);
        assertEquals(arr[0],"TEST1");
        assertEquals(arr[1],"TEST2");
    }
    
    /**
     * Test that updateListWithListElements handles reordering when an element is inserted at the beginning.
     * Reproduces the scenario from GitHub issue #526.
     * @see <a href="https://github.com/datanucleus/datanucleus-core/issues/526">GitHub #526</a>
     * @see <a href="https://github.com/datanucleus/tests/pull/92">Integration test</a>
     */
    public void testUpdateListWithListElementsReorder()
    {
        TestSCOList list = new TestSCOList();
        list.add("A");
        list.add("B");
        list.add("C");

        // Desired order: insert "D" at beginning
        java.util.List<String> elements = new java.util.ArrayList<>();
        elements.add("D");
        elements.add("A");
        elements.add("B");
        elements.add("C");

        boolean updated = SCOUtils.updateListWithListElements(list, elements);

        assertTrue("List should be marked as updated", updated);
        assertEquals(4, list.size());
        assertEquals("D", list.get(0));
        assertEquals("A", list.get(1));
        assertEquals("B", list.get(2));
        assertEquals("C", list.get(3));
    }

    /**
     * Test that updateListWithListElements handles combined add, remove, and reorder.
     */
    public void testUpdateListWithListElementsAddRemoveReorder()
    {
        TestSCOList list = new TestSCOList();
        list.add("A");
        list.add("B");
        list.add("C");

        // Remove "B", add "D", reorder to [C, D, A]
        java.util.List<String> elements = new java.util.ArrayList<>();
        elements.add("C");
        elements.add("D");
        elements.add("A");

        boolean updated = SCOUtils.updateListWithListElements(list, elements);

        assertTrue("List should be marked as updated", updated);
        assertEquals(3, list.size());
        assertEquals("C", list.get(0));
        assertEquals("D", list.get(1));
        assertEquals("A", list.get(2));
    }

    /**
     * Test that updateListWithListElements returns false when nothing changes.
     */
    public void testUpdateListWithListElementsNoChange()
    {
        TestSCOList list = new TestSCOList();
        list.add("A");
        list.add("B");

        java.util.List<String> elements = new java.util.ArrayList<>();
        elements.add("A");
        elements.add("B");

        boolean updated = SCOUtils.updateListWithListElements(list, elements);
        assertFalse("List should not be marked as updated when nothing changes", updated);
    }

    /**
     * Minimal SCOList implementation backed by an ArrayList, for testing updateListWithListElements.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static class TestSCOList extends java.util.ArrayList implements SCOList
    {
        private static final long serialVersionUID = 1L;
        public Object set(int index, Object element, boolean allowDependentField)
        {
            return super.set(index, element);
        }

        public void updateEmbeddedElement(Object element, int fieldNumber, Object value, boolean makeDirty) {}
        public boolean remove(Object element, boolean allowCascadeDelete) { return super.remove(element); }
        public void load() {}
        public boolean isLoaded() { return true; }
        public void setValue(Object value) {}
        public void initialise(Object value) {}
        public void initialise() {}
        public void initialise(Object newValue, Object oldValue) {}
        public String getFieldName() { return null; }
        public Object getOwner() { return null; }
        public void unsetOwner() {}
        public Object getValue() { return this; }
        public Object detachCopy(FetchPlanState state) { return null; }
        public void attachCopy(Object value) {}
    }

    private static class BackingStore implements CollectionStore
    {
        Collection elm;

        public BackingStore(Collection elm)
        {
            this.elm = elm;
        }

        public boolean hasOrderMapping()
        {
            return false;
        }

        public boolean updateEmbeddedElement(DNStateManager sm, Object element, int fieldNumber, Object value)
        {
            return false;
        }

        public Iterator iterator(DNStateManager sm)
        {
            return elm.iterator();
        }

        public void update(DNStateManager sm, Collection coll)
        {
        }

        public int size(DNStateManager sm)
        {
            return elm.size();
        }

        public boolean contains(DNStateManager sm, Object element)
        {
            return false;
        }

        public boolean add(DNStateManager sm, Object element, int size)
        {
            return false;
        }

        public boolean addAll(DNStateManager sm, Collection elements, int size)
        {
            return false;
        }

        public boolean remove(DNStateManager sm, Object element, int size, boolean allowDependentField)
        {
            return false;
        }

        public boolean removeAll(DNStateManager sm, Collection elements, int size)
        {
            return false;
        }

        public void clear(DNStateManager sm)
        {
        }

        public StoreManager getStoreManager()
        {
            return null;
        }

        public AbstractMemberMetaData getOwnerMemberMetaData()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }
}