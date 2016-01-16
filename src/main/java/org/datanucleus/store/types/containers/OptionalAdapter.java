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

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import org.datanucleus.store.types.ElementContainerAdapter;

public class OptionalAdapter extends ElementContainerAdapter<Optional>
{
    public OptionalAdapter(Optional optional)
    {
        super(optional);
    }

    @Override
    public void clear()
    {
        setContainer(Optional.empty());
    }

    @Override
    public Iterator<Object> iterator()
    {
        return container.isPresent() ? new OptionalIterator(container.get()) : Collections.emptyIterator();
    }

    @Override
    public void add(Object newElement)
    {
        setContainer(Optional.ofNullable(newElement));
    }

    @Override
    public void remove(Object element)
    {
        setContainer(Optional.empty());
    }

    class OptionalIterator implements Iterator<Object>
    {
        private Object value;

        private boolean hasNext = true;

        public OptionalIterator(Object value)
        {
            super();
            this.value = value;
        }

        @Override
        public boolean hasNext()
        {
            return hasNext;
        }

        @Override
        public Object next()
        {
            if (hasNext)
            {
                hasNext = false;
                return value;
            }
            throw new java.util.NoSuchElementException();
        }
    }
}
