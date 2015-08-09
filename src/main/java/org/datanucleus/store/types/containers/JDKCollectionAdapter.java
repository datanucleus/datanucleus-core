/**********************************************************************
Copyright (c) 2015 Renato and others. All rights reserved.
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

import java.util.Collection;
import java.util.Iterator;

import org.datanucleus.store.types.ElementContainerAdapter;

public class JDKCollectionAdapter<C extends Collection> extends ElementContainerAdapter<C>
{
    public JDKCollectionAdapter(C container)
    {
        super(container);
    }

    public Iterator<Object> iterator()
    {
        return container.iterator();
    }

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
    public void add(Object newElement)
    {
        container.add(newElement);
    }
    
    @Override
    public void remove(Object element)
    {
        container.remove(element);
    }
}
