package org.datanucleus.store.types.containers;

import java.util.Map;

import org.datanucleus.store.types.MapContainerAdapter;

public abstract class JDKMapHandler<C extends Map<Object, Object>> extends MapHandler<C>
{
    @Override
    public MapContainerAdapter<C> getAdapter(C container)
    {
        return new JDKMapAdapter<C>(container);
    }
}
