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

import java.util.List;
import java.util.Optional;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.store.types.ElementContainerAdapter;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.containers.CollectionHandler;

public class OptionalHandler extends CollectionHandler<Optional>
{
    @Override
    public Optional newContainer(AbstractMemberMetaData mmm)
    {
        return Optional.empty();
    }

    @Override
    public ElementContainerAdapter<Optional> getAdapter(Optional container)
    {
        return new OptionalAdapter(container);
    }

    @Override
    public Optional newContainer(AbstractMemberMetaData mmd, Object... values)
    {
        return Optional.ofNullable(values[0]);
    }

    @Override
    public void populateMetaData(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr, AbstractMemberMetaData mmd)
    {
        mmd.getCollection().setSingleElement(true);

        // Get columns defined metadata - not visible
        List<ColumnMetaData> columns = new AbstractMemberMetaData(mmd.getParent(), mmd)
        {
            private static final long serialVersionUID = 1L;

            public List<ColumnMetaData> getColumns()
            {
                return columns;
            }
        }.getColumns();

        if (columns == null || columns.size() == 0)
        {
            // Optional should allow nullable by default
            ColumnMetaData colmd = new ColumnMetaData();
            colmd.setAllowsNull(Boolean.TRUE);
            mmd.addColumn(colmd);
        }
        
        super.populateMetaData(clr, primary, mmgr, mmd);
    }

    /**
     * Default fetch group is defined by the type of the element.
     */
    @Override
    public boolean isDefaultFetchGroup(ClassLoaderResolver clr, MetaDataManager mmgr, AbstractMemberMetaData mmd)
    {
        String elementTypeName = mmd.getCollection().getElementType();
        Class elementClass = clr.classForName(elementTypeName);
        TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();

        return typeMgr.isDefaultFetchGroup(elementClass);
    }
}
