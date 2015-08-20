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
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ArrayMetaData;
import org.datanucleus.metadata.ContainerMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.ElementContainerHandler;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.util.StringUtils;

public class ArrayHandler extends ElementContainerHandler<Object, ArrayAdapter<Object>> {
	private Class arrayClass;

	public ArrayHandler(Class arrayClass) {
		this.arrayClass = arrayClass;
	}

	@Override
	public ArrayMetaData newMetaData() {
		return new ArrayMetaData();
	}

	@Override
	public void populateMetaData(MetaDataManager mmgr, AbstractMemberMetaData mmd) {
		// Assert correct type of metaData has been defined
		ArrayMetaData arrayMetadata = assertMetadataType(mmd.getContainer());

		if (StringUtils.isEmpty(arrayMetadata.getElementType()) && mmd.getMemberRepresented() != null) {
			arrayMetadata.setElementType(getElementType(mmd));
		}
	}

	@Override
	public int getObjectType(AbstractMemberMetaData mmd) {
		// TODO This should be ARRAY_ELEMENT_PC but we haven't got that yet
		return mmd.getArray().isEmbeddedElement() || mmd.getArray().isSerializedElement()
				? ObjectProvider.EMBEDDED_COLLECTION_ELEMENT_PC : ObjectProvider.PC;
	}

	@Override
	public boolean isSerialised(AbstractMemberMetaData mmd) {
		return mmd.isSerialized() || mmd.getArray().isSerializedElement();
	}

	@Override
	public boolean isEmbedded(AbstractMemberMetaData mmd) {
		return mmd.isEmbedded() || mmd.getArray().isEmbeddedElement();
	}

	@Override
	public boolean isDefaultFetchGroup(ClassLoaderResolver clr, MetaDataManager mmgr, AbstractMemberMetaData mmd) {

		String elementTypeName = mmd.getArray().getElementType();
		
		if (StringUtils.isEmpty(elementTypeName))
        {
            throw new NucleusException("MetaData must be populated to determine default fetch group.");
        }
		
		Class elementType = clr.classForName(elementTypeName);

		// Try to find using generic type specialisation

		TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();

		return typeMgr.isDefaultFetchGroupForCollection(mmd.getType(), elementType);
	}

	protected String getElementType(AbstractMemberMetaData mmd) {
		String elementType = null;

		// Infer from generics
		Member member = mmd.getMemberRepresented();

		if (member instanceof Field) {
			elementType = ((Field) member).getType().getComponentType().getName();
		} else {
			elementType = ((Method) member).getReturnType().getComponentType().getName();
		}

		return elementType;
	}

	private ArrayMetaData assertMetadataType(ContainerMetaData existingMetaData) {
		if (existingMetaData instanceof ArrayMetaData) {
			return (ArrayMetaData) existingMetaData;
		}

		// TODO Renato Improve error handling
		throw new RuntimeException("Invalid type of metadata specified");
	}

	@Override
	public ArrayAdapter getAdapter(Object container) {
		return new ArrayAdapter<Object>(container);
	}

	@Override
	public Object newContainer(AbstractMemberMetaData mmm) {
		return Array.newInstance(arrayClass.getComponentType(), 0);
	}

	@Override
	public Object newContainer(AbstractMemberMetaData mmd, Object... objects) {
		return objects;
	}

}