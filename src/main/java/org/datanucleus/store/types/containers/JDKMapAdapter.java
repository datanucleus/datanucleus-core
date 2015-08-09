package org.datanucleus.store.types.containers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.datanucleus.store.types.MapContainerAdapter;

public class JDKMapAdapter<C extends Map<Object, Object>> implements MapContainerAdapter<C>
{
    C container;
    
    public JDKMapAdapter(C container)
    {
        super();
        this.container = container;
    }

    @Override
    public C getContainer()
    {
        return container;
    }

    @Override
    public void clear()
    {
        container.clear();
    }
    
    @Override
    public Iterator<Object> iterator()
    {
        ArrayList entries = new ArrayList(container.keySet());
        entries.addAll(container.values());
        return entries.iterator();
    }
    
    @Override
    public Object put(Object key, Object value)
    {
        return container.put(key, value);
    }

    @Override
    public Object remove(Object key)
    {
        return container.remove(key);
    }
    
    @Override
    public Iterable<Entry<Object, Object>> entries()
    {
        return container.entrySet();
    }

    @Override
    public Iterable<Object> keys()
    {
        return container.keySet();
    }

    @Override
    public Iterable<Object> values()
    {
        return container.values();
    }
}
