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

import java.util.Arrays;
import java.util.LinkedList;

import org.datanucleus.metadata.AbstractMemberMetaData;

public class LinkedListHandler extends JDKCollectionHandler<LinkedList>
{
    @Override
    public LinkedList newContainer(AbstractMemberMetaData mmm)
    {
        return new LinkedList();
    }

    @Override
    public LinkedList newContainer(AbstractMemberMetaData mmd, Object... objects)
    {
        return new LinkedList(Arrays.asList(objects));
    }
    
    @Override
    public ElementContainerAdapter<LinkedList> getAdapter(LinkedList container)
    {
        return new JDKListAdapter<LinkedList>(container);
    }
}
