package org.datanucleus.store.types;

import org.datanucleus.metadata.AbstractMemberMetaData;

public abstract class ElementContainerHandler<C, A extends ElementContainerAdapter<C>>
		implements ContainerHandler<C, A> {

	public abstract C newContainer(Object... objects);

	public abstract int getObjectType(AbstractMemberMetaData mmd);
}
