package org.datanucleus.store.types.containers;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.CollectionMetaData;
import org.datanucleus.metadata.ContainerMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.MetaDataManager;
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
    public void populateMetaData(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr, AbstractMemberMetaData mmd)
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
            mmd.getElementMetaData().populate(clr, primary, mmgr);
        }

        if (mmd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            if (mmd.isCascadeDelete())
            {
                // User has set cascade-delete (JPA) so set the element as dependent
                collectionMetadata.setDependentElement(true);
            }
            
            collectionMetadata.populate(clr, primary, mmgr);
        }
    }

    @Override
    public int getObjectType(AbstractMemberMetaData mmd) {
		
		return mmd.getCollection().isEmbeddedElement() || mmd.getCollection().isSerializedElement() ? 
				ObjectProvider.EMBEDDED_COLLECTION_ELEMENT_PC : ObjectProvider.PC;
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
    public boolean isDefaultFetchGroup(ClassLoaderResolver clr, MetaDataManager mmgr, AbstractMemberMetaData mmd)
    {
        if (!mmd.getCollection().isPopulated())
        {
            // Require mmd to be populated since it will have the element type validated and adjusted if necessary
            throw new NucleusException("MetaData must be populated in order to be able to determine default fetch group.");
        }
        
        String elementTypeName = mmd.getCollection().getElementType();

        Class elementType = clr.classForName(elementTypeName);

        // Try to find using generic type specialisation

        TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();

        return typeMgr.isDefaultFetchGroupForCollection(mmd.getType(), elementType);
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
            
            // TODO Handle declarations with type bounds? e.g. List<? extends PC> 
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