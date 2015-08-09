package org.datanucleus.store.types.containers;

import java.util.List;

import org.datanucleus.store.types.SequenceAdapter;

public class JDKListAdapter<C extends List> extends JDKCollectionAdapter<C> implements SequenceAdapter
{
    public JDKListAdapter(C container)
    {
        super(container);
    }

    @Override
    public void update(Object newElement, int position)
    {
        container.set(position, newElement);
    }
}
