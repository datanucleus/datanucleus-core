package org.datanucleus.store.types.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.datanucleus.store.types.ElementContainerAdapter;

public class JDKCollectionHandler<C extends Collection> extends CollectionHandler<C>
{
    @Override
    public ElementContainerAdapter<C> getAdapter(C container)
    {
        return new JDKCollectionAdapter<C>(container);
    }

    @Override
    public C newContainer()
    {
        return (C) new ArrayList();
    }

    @Override
    public C newContainer(Object... objects)
    {
        return (C) new ArrayList(Arrays.asList(objects));
    }
}
