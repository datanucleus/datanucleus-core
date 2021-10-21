/**********************************************************************
Copyright (c) 2015 Renato Garcia and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    ...
**********************************************************************/
package org.datanucleus.store.types.containers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
