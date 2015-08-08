package org.datanucleus.store.types.containers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HashSetHandler extends JDKCollectionHandler<Set>
{
    @Override
    public Set newContainer()
    {
        return new HashSet();
    }

    @Override
    public Set newContainer(Object... objects)
    {
        return new HashSet(Arrays.asList(objects));
    }
}
