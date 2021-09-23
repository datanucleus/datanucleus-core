/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.store.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.scostore.CollectionStore;

/**
 * An iterator for a SCO Collection object. Works from either the delegate or a backing store, and provides iteration through the objects.
 * @param <E> Element type for the collection
 */
public class SCOCollectionIterator<E> implements Iterator<E>
{
    private final Iterator<E> iter;
    private E last = null;

    private Collection<E> ownerSCO;

    /**
     * Constructor taking the delegate or backing store.
     * @param sco The owner sco
     * @param sm StateManager of SCO Collection to iterate
     * @param theDelegate The delegate collection
     * @param backingStore The backing store (connected to the DB)
     * @param useDelegate Whether to use the delegate
     */
    public SCOCollectionIterator(Collection<E> sco, ObjectProvider sm, Collection<E> theDelegate, CollectionStore<E> backingStore, boolean useDelegate)
    {
        ownerSCO = sco;

        // Populate our entries list
        List entries = new ArrayList();

        Iterator i=null; 
        if (useDelegate)
        {
            i = theDelegate.iterator();
        }
        else
        {
            if (backingStore != null)
            {
                i = backingStore.iterator(sm);
            }
            else
            {
                i = theDelegate.iterator();
            }
        }

        while (i.hasNext())
        {
            entries.add(i.next());
        }

        iter = entries.iterator();
    }

    public boolean hasNext()
    {
        return iter.hasNext();
    }

    public E next()
    {
        last = iter.next();
        return last;
    }

    public void remove()
    {
        if (last == null)
        {
            throw new IllegalStateException();
        }

        ownerSCO.remove(last);
        last = null;
    }
}