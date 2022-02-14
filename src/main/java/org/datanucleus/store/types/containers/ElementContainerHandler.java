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

import org.datanucleus.PersistableObjectType;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ArrayMetaData;
import org.datanucleus.metadata.CollectionMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.ContainerMetaData;
import org.datanucleus.metadata.ElementMetaData;

public abstract class ElementContainerHandler<C, A extends ElementContainerAdapter<C>> implements ContainerHandler<C, A> 
{
	public abstract C newContainer(AbstractMemberMetaData mmd, Object... objects);

	public abstract PersistableObjectType getObjectType(AbstractMemberMetaData mmd);

    public String getElementTypeName(ContainerMetaData cmd)
    {
        if (cmd instanceof CollectionMetaData)
        {
            return ((CollectionMetaData) cmd).getElementType();
        }
        else if (cmd instanceof ArrayMetaData)
        {
            return ((ArrayMetaData) cmd).getElementType();
        }
        else
        {
            throw new NucleusException("Unable to determine element type name - container metadata not supported");
        }
    }
	
	protected void moveColumnsToElement(AbstractMemberMetaData mmd)
    {
        ColumnMetaData[] columnMetaData = mmd.getColumnMetaData();
        if (!mmd.isSerialized() && !mmd.isEmbedded() && columnMetaData != null && mmd.getTypeConverterName() == null)
        {
            // Not serialising/embedding this field, nor converting the whole field yet column info was specified. Check for specific conditions
            if (mmd.getElementMetaData() == null)
            {
                // Collection/Array with column(s) specified on field but not on element so move all column info to element
                ElementMetaData elemmd = new ElementMetaData();
                mmd.setElementMetaData(elemmd);
                for (int i=0;i<columnMetaData.length;i++)
                {
                    elemmd.addColumn(columnMetaData[i]);
                }
                
                mmd.clearColumns();
            }
        }
    }
	
	protected void copyMappedByDefinitionFromElement(AbstractMemberMetaData mmd)
    {
        ElementMetaData elementMetaData = mmd.getElementMetaData();
        if (elementMetaData != null && elementMetaData.getMappedBy() != null && mmd.getMappedBy() == null)
        {
            // User has specified "mapped-by" on element instead of field so pull it up to this level
            // <element mapped-by="..."> is the same as <field mapped-by="..."> for a collection field
            mmd.setMappedBy(elementMetaData.getMappedBy());
        }
    }
}
