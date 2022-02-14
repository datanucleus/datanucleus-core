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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PersistableObjectType;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ArrayMetaData;
import org.datanucleus.metadata.ContainerMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.util.StringUtils;

public class ArrayHandler extends ElementContainerHandler<Object, ArrayAdapter<Object>>
{
    private Class arrayClass;

    public ArrayHandler(Class arrayClass)
    {
        this.arrayClass = arrayClass;
    }

    @Override
    public ArrayMetaData newMetaData()
    {
        return new ArrayMetaData();
    }

    @Override
    public void populateMetaData(ClassLoaderResolver clr, ClassLoader primary, AbstractMemberMetaData mmd)
    {
        // Assert correct type of metaData has been defined
        ArrayMetaData arrayMetadata = assertMetadataType(mmd.getContainer());

        // Populate/update element type, if not already specified
        if (StringUtils.isEmpty(arrayMetadata.getElementType()))
        {
            arrayMetadata.setElementType(getElementType(mmd));
        }
        
        moveColumnsToElement(mmd);
        copyMappedByDefinitionFromElement(mmd);
        
        if (mmd.getElementMetaData() != null)
        {
            mmd.getElementMetaData().populate(clr, primary);
        }
        
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            if (mmd.isCascadeDelete())
            {
                // User has set cascade-delete (JPA) so set the element as dependent
                arrayMetadata.setDependentElement(true);
            }
            
            arrayMetadata.populate(clr, primary);
        }
    }

    @Override
    public PersistableObjectType getObjectType(AbstractMemberMetaData mmd)
    {
        return mmd.getArray().isEmbeddedElement() || mmd.getArray().isSerializedElement() ? PersistableObjectType.EMBEDDED_ARRAY_ELEMENT_PC : PersistableObjectType.PC;
    }

    @Override
    public boolean isSerialised(AbstractMemberMetaData mmd)
    {
        return mmd.isSerialized() || mmd.getArray().isSerializedElement();
    }

    @Override
    public boolean isEmbedded(AbstractMemberMetaData mmd)
    {
        return mmd.isEmbedded() || mmd.getArray().isEmbeddedElement();
    }

    @Override
    public boolean isDefaultFetchGroup(ClassLoaderResolver clr, TypeManager typeMgr, AbstractMemberMetaData mmd)
    {
        String elementTypeName = getElementType(mmd);
        if (StringUtils.isEmpty(elementTypeName))
        {
            throw new NucleusException("MetaData must be populated to determine default fetch group.");
        }

        // Try to find using generic type specialisation
        return typeMgr.isDefaultFetchGroupForCollection(mmd.getType(), clr.classForName(elementTypeName));
    }

    protected String getElementType(AbstractMemberMetaData mmd)
    {
        String elementType = null;

        // Infer from generics
        Member member = mmd.getMemberRepresented();

        if (member instanceof Field)
        {
            elementType = ((Field) member).getType().getComponentType().getName();
        }
        else if (member instanceof Method)
        {
            elementType = ((Method) member).getReturnType().getComponentType().getName();
        }

        return elementType;
    }

    private ArrayMetaData assertMetadataType(ContainerMetaData existingMetaData)
    {
        if (existingMetaData instanceof ArrayMetaData)
        {
            return (ArrayMetaData) existingMetaData;
        }

        throw new NucleusException("Invalid type of metadata specified.");
    }

    @Override
    public ArrayAdapter getAdapter(Object container)
    {
        return new ArrayAdapter<Object>(container);
    }

    @Override
    public Object newContainer(AbstractMemberMetaData mmm)
    {
        return Array.newInstance(arrayClass.getComponentType(), 0);
    }

    @Override
    public Object newContainer(AbstractMemberMetaData mmd, Object... objects)
    {
        return objects;
    }
}