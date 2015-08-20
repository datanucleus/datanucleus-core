package org.datanucleus.store.types.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.types.ElementContainerAdapter;

public class JDKCollectionHandler<C extends Collection> extends CollectionHandler<C>
{
    @Override
    public ElementContainerAdapter<C> getAdapter(C container)
    {
        return new JDKCollectionAdapter<C>(container);
    }

    @Override
    public C newContainer(AbstractMemberMetaData mmd)
    {
        return (C) (mmd.getOrderMetaData() == null ? new HashSet() : new ArrayList());
    }

    @Override
    public C newContainer(AbstractMemberMetaData mmd, Object... objects)
    {
        List<Object> asList = Arrays.asList(objects);
        return (C) (mmd.getOrderMetaData() == null ? new HashSet(asList) : new ArrayList(asList));
    }
}
