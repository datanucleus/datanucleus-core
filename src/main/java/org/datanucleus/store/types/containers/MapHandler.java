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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.ContainerMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.MapMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.ValueMetaData;
import org.datanucleus.store.types.ContainerHandler;
import org.datanucleus.store.types.MapContainerAdapter;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

public abstract class MapHandler<C> implements ContainerHandler<C, MapContainerAdapter<C>>
{
    @Override
    public MapMetaData newMetaData()
    {
        return new MapMetaData();
    }

    @Override
    public void populateMetaData(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr, AbstractMemberMetaData mmd)
    {
        MapMetaData mapMd = assertValidType(mmd.getContainer());

        if (mapMd.getKeyType() == null)
        {
            mapMd.setKeyType(getKeyType(mmd));
        }

        if (mapMd.getValueType() == null)
        {
            mapMd.setValueType(getValueType(mmd));
        }
        
        moveColumnsToValue(mmd);
        
        if (mmd.getKeyMetaData() != null)
        {
            // Populate any key object
            mmd.getKeyMetaData().populate(clr, primary, mmgr);
        }
        
        if (mmd.getValueMetaData() != null)
        {
            // Populate any value object
            mmd.getValueMetaData().populate(clr, primary, mmgr);
        }
        
        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            String keyCascadeVal = mmd.getValueForExtension("cascade-delete-key");
            
            if (mmd.isCascadeDelete())
            {
                // User has set cascade-delete (JPA) so set the value as dependent
                mapMd.setDependentKey(false); // JPA spec doesn't define what this should be
                mapMd.setDependentValue(true);
            }
            
            if (keyCascadeVal != null)
            {
                if (keyCascadeVal.equalsIgnoreCase("true"))
                {
                    mapMd.setDependentKey(true);
                }
                else
                {
                    mapMd.setDependentKey(false);
                }
            }
            
            mapMd.populate(clr, primary, mmgr);
        }
    }

    private String getKeyType(AbstractMemberMetaData mmd)
    {
        String keyType = null;

        // Infer from generics
        Member member = mmd.getMemberRepresented();

        if (member instanceof Field)
        {
            keyType = ClassUtils.getMapKeyType((Field) member);
        }
        else if (member instanceof Method)
        {
            keyType = ClassUtils.getMapKeyType((Method) member);
        }

        // TODO Bounded typed declarations: Shouldn't this be part of getMapKeyType?
        if (keyType == null)
        {
            // Try to use generics to furnish any missing type info
            Type genericType = member instanceof Field ? ((Field) member).getGenericType() : ((Method) member).getGenericReturnType();

            if (genericType != null && genericType instanceof ParameterizedType)
            {
                ParameterizedType paramGenType = (ParameterizedType) genericType;
                Type keyGenericType = paramGenType.getActualTypeArguments()[0];
                if (keyGenericType instanceof TypeVariable)
                {
                    Type keyGenTypeBound = ((TypeVariable) keyGenericType).getBounds()[0];
                    if (keyGenTypeBound instanceof Class)
                    {
                        keyType = ((Class) keyGenTypeBound).getName();
                    }
                }
            }
        }

        if (keyType == null)
        {
            // Default to "Object" as element type
            keyType = Object.class.getName();
            NucleusLogger.METADATA.debug(Localiser.msg("044004", mmd.getClassName(), mmd.getName()));
        }

        return keyType;
    }

    private String getValueType(AbstractMemberMetaData mmd)
    {
        String valueType = null;

        if (mmd.getTargetClassName() == null)
        {
            // Infer from generics
            Member member = mmd.getMemberRepresented();

            if (member instanceof Field)
            {
                valueType = ClassUtils.getMapValueType((Field) member);
            }
            else
            {
                valueType = ClassUtils.getMapValueType((Method) member);
            }

            // TODO Bounded typed declarations: Shouldn't this be part of getMapvalueType?
            if (valueType == null)
            {
                // Try to use generics to furnish any missing type info
                Type genericType = member instanceof Field ? ((Field) member).getGenericType() : ((Method) member).getGenericReturnType();

                if (genericType != null && genericType instanceof ParameterizedType)
                {
                    ParameterizedType paramGenType = (ParameterizedType) genericType;
                    Type valueGenericType = paramGenType.getActualTypeArguments()[1];
                    if (valueGenericType instanceof TypeVariable)
                    {
                        Type valueGenTypeBound = ((TypeVariable) valueGenericType).getBounds()[0];
                        if (valueGenTypeBound instanceof Class)
                        {
                            valueType = ((Class) valueGenTypeBound).getName();
                        }
                    }
                }
            }
        }
        else
        {
            // User has specified target class name (JPA)
            valueType = mmd.getTargetClassName();
        }

        if (valueType == null)
        {
            // Default to "Object" as element type
            valueType = Object.class.getName();
            NucleusLogger.METADATA.debug(Localiser.msg("044004", mmd.getClassName(), mmd.getName()));
        }

        return valueType;
    }

    private MapMetaData assertValidType(ContainerMetaData metaData)
    {
        if (metaData instanceof MapMetaData)
        {
            return (MapMetaData) metaData;
        }

        throw new NucleusException("Invalid type of metadata specified.");
    }

    private void moveColumnsToValue(AbstractMemberMetaData mmd)
    {
        ColumnMetaData[] columnMetaData = mmd.getColumnMetaData();
        if (!mmd.isSerialized() && !mmd.isEmbedded() && columnMetaData != null && mmd.getTypeConverterName() == null)
        {
            // Not serialising/embedding this field, nor converting the whole field yet column info was specified. Check for specific conditions
            if (mmd.getValueMetaData() == null)
            {
                // Map with column(s) specified on field but not on value so move all column info to value
                ValueMetaData valmd = new ValueMetaData();
                mmd.setValueMetaData(valmd);
                for (int i=0;i<columnMetaData.length;i++)
                {
                    valmd.addColumn(columnMetaData[i]);
                }

                mmd.clearColumns();
            }
        }
    }

    @Override
    public boolean isSerialised(AbstractMemberMetaData mmd)
    {
        return mmd.isSerialized() || mmd.getMap().isSerializedKey() || mmd.getMap().isSerializedValue();
    }

    @Override
    public boolean isEmbedded(AbstractMemberMetaData mmd)
    {
        return mmd.isEmbedded() || mmd.getMap().isEmbeddedKey() || mmd.getMap().isEmbeddedValue();
    }

    @Override
    public boolean isDefaultFetchGroup(ClassLoaderResolver clr, MetaDataManager mmgr, AbstractMemberMetaData mmd)
    {
        return false;
    }
}
