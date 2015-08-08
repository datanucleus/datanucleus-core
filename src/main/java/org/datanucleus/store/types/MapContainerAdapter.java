package org.datanucleus.store.types;

import java.util.Map.Entry;

public interface MapContainerAdapter<C extends Object> extends ContainerAdapter<C>
{
    Object put(Object key, Object value);

    Object remove(Object key);
    
    Iterable<Entry<Object, Object>> entries();

    Iterable<Object> keys();

    Iterable<Object> values();
}
