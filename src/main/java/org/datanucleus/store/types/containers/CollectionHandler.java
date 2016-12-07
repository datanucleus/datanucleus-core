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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.CollectionMetaData;
import org.datanucleus.metadata.ContainerMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.OrderMetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.ElementContainerAdapter;
import org.datanucleus.store.types.ElementContainerHandler;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

public abstract class CollectionHandler<C extends Object> extends ElementContainerHandler<C, ElementContainerAdapter<C>>
{
    @Override
    public CollectionMetaData newMetaData()
    {
        CollectionMetaData collectionMetaData = new CollectionMetaData();
        // Reset element, it should be determined by the specified metadata or by generics
        collectionMetaData.setElementType(null);
        return collectionMetaData;
    }

    @Override
    public void populateMetaData(ClassLoaderResolver clr, ClassLoader primary, AbstractMemberMetaData mmd)
    {
        // Assert correct type of metadata has been defined
        CollectionMetaData collectionMetadata = assertValidType(mmd.getContainer());

        // Populate/update element type, if not already specified and we have the member. 
        if (StringUtils.isEmpty(collectionMetadata.getElementType()))
        {
            collectionMetadata.setElementType(getElementType(mmd));
        }
        
        if (mmd.isOrdered() && mmd.getOrderMetaData() == null)
        {
            OrderMetaData ordmd = new OrderMetaData();
            ordmd.setOrdering("#PK"); // Special value recognised by OrderMetaData
            mmd.setOrderMetaData(ordmd);
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
                collectionMetadata.setDependentElement(true);
            }
            
            collectionMetadata.populate(clr, primary);
        }
    }

    @Override
    public int getObjectType(AbstractMemberMetaData mmd) 
    {
        return mmd.getCollection().isEmbeddedElement() || mmd.getCollection().isSerializedElement() ? ObjectProvider.EMBEDDED_COLLECTION_ELEMENT_PC : ObjectProvider.PC;
    }

    @Override
    public boolean isSerialised(AbstractMemberMetaData mmd)
    {
        return mmd.isSerialized() || mmd.getCollection().isSerializedElement();
    }

    @Override
    public boolean isEmbedded(AbstractMemberMetaData mmd)
    {
        return mmd.isEmbedded() || mmd.getCollection().isEmbeddedElement();
    }

    @Override
    public boolean isDefaultFetchGroup(ClassLoaderResolver clr, TypeManager typeMgr, AbstractMemberMetaData mmd)
    {
        if (!mmd.getCollection().isPopulated())
        {
            // Require mmd to be populated since it will have the element type validated and adjusted if necessary
            throw new NucleusException("MetaData must be populated in order to be able to determine default fetch group.");
        }

        // Try to find using generic type specialisation
        return typeMgr.isDefaultFetchGroupForCollection(mmd.getType(), clr.classForName(mmd.getCollection().getElementType()));
    }

    protected String getElementType(AbstractMemberMetaData mmd)
    {
        String elementType = null;

        if (mmd.getTargetClassName() == null)
        {
            // Infer from generics
            Member member = mmd.getMemberRepresented();

            if (member instanceof Field)
            {
                elementType = ClassUtils.getCollectionElementType((Field) member);
            }
            else if (member instanceof Method)
            {
                elementType = ClassUtils.getCollectionElementType((Method) member);
            }

            if (elementType == null)
            {
                // Try to use generics to furnish any missing type info
                Type genericType = member instanceof Field ? ((Field) member).getGenericType() : ((Method) member).getGenericReturnType();

                if (genericType != null && genericType instanceof ParameterizedType)
                {
                    ParameterizedType paramGenType = (ParameterizedType) genericType;
                    Type elemGenericType = paramGenType.getActualTypeArguments()[0];
                    if (elemGenericType instanceof TypeVariable)
                    {
                        Type elemGenTypeBound = ((TypeVariable) elemGenericType).getBounds()[0];
                        if (elemGenTypeBound instanceof Class)
                        {
                            elementType = ((Class)elemGenTypeBound).getName();
                        }
                        else if (elemGenTypeBound instanceof ParameterizedType)
                        {
                            // Element type is defined as a parametrized type, e.g "Project<? extends ProjectLeader<?>>"
                            ParameterizedType paramElemGenType = (ParameterizedType)elemGenTypeBound;
                            Type paramElemGenTypeRaw = paramElemGenType.getRawType();
                            if (paramElemGenTypeRaw != null && paramElemGenTypeRaw instanceof Class)
                            {
                                elementType = ((Class)paramElemGenTypeRaw).getName();
                            }
                        }
                    }
                }
            }
        }
        else
        {
            // User has specified target class name (JPA)
            elementType = mmd.getTargetClassName();
        }

        if (elementType == null)
        {
            // Default to "Object" as element type
            elementType = Object.class.getName();
            NucleusLogger.METADATA.debug(Localiser.msg("044003", mmd.getClassName(), mmd.getName()));
        }

        return elementType;
    }

    private CollectionMetaData assertValidType(ContainerMetaData metaData)
    {
        if (metaData instanceof CollectionMetaData)
        {
            return (CollectionMetaData) metaData;
        }

        throw new NucleusException("Invalid type of metadata specified.");
    }
}