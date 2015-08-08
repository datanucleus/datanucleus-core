package org.datanucleus.store.types;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ContainerMetaData;
import org.datanucleus.metadata.MetaDataManager;

/**
 * Provides support for SCO Containers types in DN. SCO Containers contain other FCOs or SCOs objects e.g.:
 * collections, maps and arrays. ContainerHandlers work as factories being responsible for instantiating new
 * container instances. They also provide the necessary metadata and related operations as well as the access
 * to the ContainerAdapter. One instance of the ContainerHandler can be across the types, whereas the
 * ContainerAdapter requires one instance their respective container instances.
 * @param <C> The class of the container
 * @param <A> ContainerAdater that is returned by use
 */
public interface ContainerHandler<C, A extends ContainerAdapter<C>>
{
    C newContainer();

    A getAdapter(C container);

    <M extends ContainerMetaData> M newMetaData();

    void populateMetaData(MetaDataManager mmgr, AbstractMemberMetaData mmd);

    boolean isDefaultFetchGroup(ClassLoaderResolver clr, MetaDataManager mmgr, AbstractMemberMetaData mmd);

    boolean isSerialised(AbstractMemberMetaData mmd);

    boolean isEmbedded(AbstractMemberMetaData mmd);
}
