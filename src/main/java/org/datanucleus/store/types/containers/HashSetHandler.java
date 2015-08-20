package org.datanucleus.store.types.containers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.datanucleus.metadata.AbstractMemberMetaData;

public class HashSetHandler extends JDKCollectionHandler<Set>
{
    @Override
    public Set newContainer(AbstractMemberMetaData mmm)
    {
        return new HashSet();
    }

    @Override
    public Set newContainer(AbstractMemberMetaData mmd, Object... objects)
    {
        return new HashSet(Arrays.asList(objects));
    }
}
