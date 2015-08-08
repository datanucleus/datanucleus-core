package org.datanucleus.store.types.containers;

import java.util.Hashtable;


public class HashtableHandler extends JDKMapHandler<Hashtable<Object, Object>>
{
    @Override
    public Hashtable<Object, Object> newContainer()
    {
        return new Hashtable();
    }
}
