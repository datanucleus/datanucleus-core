package org.datanucleus.store.types.sco;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.StoreManager;
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
        String[] arr = (String[]) SCOUtils.toArray(new BackingStore(elm),null,new String[2]);
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