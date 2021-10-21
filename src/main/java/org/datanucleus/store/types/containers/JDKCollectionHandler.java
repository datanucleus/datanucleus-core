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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.datanucleus.metadata.AbstractMemberMetaData;

public class JDKCollectionHandler<C extends Collection> extends CollectionHandler<C>
{
    @Override
    public ElementContainerAdapter<C> getAdapter(C container)
    {
        return new JDKCollectionAdapter<C>(container);
    }

    @Override
    public C newContainer(AbstractMemberMetaData mmd)
    {
        return (C) (mmd.getOrderMetaData() == null ? new HashSet() : new ArrayList());
    }

    @Override
    public C newContainer(AbstractMemberMetaData mmd, Object... objects)
    {
        List<Object> asList = Arrays.asList(objects);
        return (C) (mmd.getOrderMetaData() == null ? new HashSet(asList) : new ArrayList(asList));
    }
}
