package org.datanucleus.store.types.containers;

import java.util.Hashtable;

import org.datanucleus.metadata.AbstractMemberMetaData;

public class HashtableHandler extends JDKMapHandler<Hashtable<Object, Object>>
{
    @Override
    public Hashtable<Object, Object> newContainer(AbstractMemberMetaData mmm)
    {
        return new Hashtable();
    }
}
