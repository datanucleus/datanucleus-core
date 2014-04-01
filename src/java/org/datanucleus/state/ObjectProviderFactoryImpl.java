/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.state;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.jdo.spi.JDOImplHelper;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.Configuration;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.cache.CachedPC;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.store.FieldValues;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Factory for ObjectProviders.
 * Makes use of a pool to allow reuse to save recreation of objects.
 */
public class ObjectProviderFactoryImpl implements ObjectProviderFactory
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    Class opClass = null;

    public static Class[] OBJECT_PROVIDER_CTR_ARG_CLASSES = new Class[] {ExecutionContext.class, AbstractClassMetaData.class};

    // Single pool of all ObjectProvider objects. TODO Consider having one pool per object type.
//    ObjectProviderPool opPool = null;

    public ObjectProviderFactoryImpl(PersistenceNucleusContext nucCtx)
    {
        Configuration conf = nucCtx.getConfiguration();
        String opClassName = conf.getStringProperty(PropertyNames.PROPERTY_OBJECT_PROVIDER_CLASS_NAME);
        if (StringUtils.isWhitespace(opClassName))
        {
            // Use default ObjectProvider for the StoreManager
            opClassName = nucCtx.getStoreManager().getDefaultObjectProviderClassName();
        }
        opClass = nucCtx.getClassLoaderResolver(null).classForName(opClassName);

        // Register the StateManager class with JDOImplHelper for security
        AccessController.doPrivileged(new PrivilegedAction() 
        {
            public Object run()
            {
                JDOImplHelper.registerAuthorizedStateManagerClass(opClass);
                return null;
            }
        });

//        opPool = new ObjectProviderPool(conf.getIntProperty(PropertyNames.PROPERTY_OBJECT_PROVIDER_MAX_IDLE),
//            conf.getBooleanProperty(PropertyNames.PROPERTY_OBJECT_PROVIDER_REAPER_THREAD),
//            opClass);
    }

    public void close()
    {
//        opPool.close();
    }

    /**
     * Constructs an ObjectProvider to manage a hollow instance having the given object ID.
     * This constructor is used for creating new instances of existing persistent objects.
     * @param ec the ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param id the identity of the object.
     */
    public ObjectProvider newForHollow(ExecutionContext ec, Class pcClass, Object id)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd =
            ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForHollow(id, null, cls);
        return op;
    }

    /**
     * Constructs an ObjectProvider to manage a recently populated hollow instance having the
     * given object ID and the given field values. This constructor is used for
     * creating new instances of persistent objects obtained e.g. via a Query or backed by a view.
     * @param ec ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param id the identity of the object.
     * @param fv the initial field values of the object.
     */
    public ObjectProvider newForHollow(ExecutionContext ec, Class pcClass, Object id, FieldValues fv)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd =
            ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForHollow(id, fv, cls);
        return op;
    }

    /**
     * Constructs an ObjectProvider to manage a hollow instance having the given object ID.
     * The instance is already supplied.
     * @param ec ExecutionContext
     * @param id the identity of the object.
     * @param pc The object that is hollow that we are going to manage
     */
    public ObjectProvider newForHollowPreConstructed(ExecutionContext ec, Object id, Object pc)
    {
        AbstractClassMetaData cmd = 
            ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForHollowPreConstructed(id, pc);
        return op;
    }

    /**
     * Constructs an ObjectProvider to manage a hollow (or P-clean) instance having the given FieldValues.
     * This constructor is used for creating new instances of existing persistent objects using application identity.
     * @param ec ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param fv the initial field values of the object.
     * @deprecated Use newForHollowPopulated instead
     */
    public ObjectProvider newForHollowPopulatedAppId(ExecutionContext ec, Class pcClass, final FieldValues fv)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd =
            ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForHollowAppId(fv, cls);
        return op;
    }

    /**
     * Constructs an ObjectProvider to manage the specified persistent instance having the given object ID.
     * @param ec ExecutionContext
     * @param id the identity of the object.
     * @param pc The object that is persistent that we are going to manage
     */
    public ObjectProvider newForPersistentClean(ExecutionContext ec, Object id, Object pc)
    {
        AbstractClassMetaData cmd = 
            ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForPersistentClean(id, pc);
        return op;
    }

    /**
     * Constructs an ObjectProvider to manage a persistable instance that will
     * be EMBEDDED/SERIALISED into another persistable object. The instance will not be
     * assigned an identity in the process since it is a SCO.
     * @param ec ExecutionContext
     * @param pc The persistable to manage (see copyPc also)
     * @param copyPc Whether the SM should manage a copy of the passed PC or that one
     * @param ownerOP Owner ObjectProvider
     * @param ownerFieldNumber Field number in owner object where this is stored
     */
    public ObjectProvider newForEmbedded(ExecutionContext ec, Object pc, boolean copyPc, 
            ObjectProvider ownerOP, int ownerFieldNumber)
    {
        AbstractClassMetaData cmd = 
            ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForEmbedded(pc, copyPc);
        if (ownerOP != null)
        {
            ec.registerEmbeddedRelation(ownerOP, ownerFieldNumber, op);
        }
        return op;
    }

    /**
     * Constructs an ObjectProvider for an object of the specified type, creating the PC object to hold the values
     * where this object will be EMBEDDED/SERIALISED into another persistable object. The instance will not be
     * assigned an identity in the process since it is a SCO.
     * @param ec ExecutionContext
     * @param cmd Meta-data for the class that this is an instance of.
     * @param ownerOP Owner ObjectProvider
     * @param ownerFieldNumber Field number in owner object where this is stored
     */
    public ObjectProvider newForEmbedded(ExecutionContext ec, AbstractClassMetaData cmd, 
            ObjectProvider ownerOP, int ownerFieldNumber)
    {
        Class pcClass = ec.getClassLoaderResolver().classForName(cmd.getFullClassName());
        ObjectProvider op = newForHollow(ec, pcClass, null);
        op.initialiseForEmbedded(op.getObject(), false);
        if (ownerOP != null)
        {
            ec.registerEmbeddedRelation(ownerOP, ownerFieldNumber, op);
        }
        return op;
    }

    /**
     * Constructs an ObjectProvider to manage a transient instance that is becoming newly persistent.
     * A new object ID for the instance is obtained from the store manager and the object is inserted
     * in the data store. This constructor is used for assigning ObjectProviders to existing
     * instances that are transitioning to a persistent state.
     * @param ec ExecutionContext
     * @param pc the instance being make persistent.
     * @param preInsertChanges Any changes to make before inserting
     */
    public ObjectProvider newForPersistentNew(ExecutionContext ec, Object pc, FieldValues preInsertChanges)
    {
        AbstractClassMetaData cmd = 
            ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForPersistentNew(pc, preInsertChanges);
        return op;
    }

    /**
     * Constructs an ObjectProvider to manage a transactional-transient instance.
     * A new object ID for the instance is obtained from the store manager and the object is inserted in 
     * the data store. This constructor is used for assigning an ObjectProvider to a transient instance
     * that is transitioning to a transient clean state.
     * @param ec ExecutionContext
     * @param pc the instance being make persistent.
     */
    public ObjectProvider newForTransactionalTransient(ExecutionContext ec, Object pc)
    {
        AbstractClassMetaData cmd = 
            ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForTransactionalTransient(pc);
        return op;
    }

    /**
     * Constructor for creating an ObjectProvider to manage a persistable object in detached state.
     * @param ec ExecutionContext
     * @param pc the detached object
     * @param id the identity of the object.
     * @param version the detached version
     */
    public ObjectProvider newForDetached(ExecutionContext ec, Object pc, Object id, Object version)
    {
        AbstractClassMetaData cmd = 
            ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForDetached(pc, id, version);
        return op;
    }

    /**
     * Constructor for creating an ObjectProvider to manage a persistable object that is not persistent yet
     * is about to be deleted. Consequently the initial lifecycle state will be P_NEW, but will soon
     * move to P_NEW_DELETED.
     * @param ec Execution Context
     * @param pc the object being deleted from persistence
     */
    public ObjectProvider newForPNewToBeDeleted(ExecutionContext ec, Object pc)
    {
        AbstractClassMetaData cmd = 
            ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForPNewToBeDeleted(pc);
        return op;
    }

    /**
     * Constructor to create an ObjectProvider for an object taken from the L2 cache with the specified id.
     * Creates an object that the cached object represents, assigns a ObjectProvider to it, and copies across 
     * the fields that were cached, and its version (if available).
     * @param ec ExecutionContext
     * @param id Id to assign to the persistable object
     * @param cachedPC CachedPC object from the L2 cache
     */
    public ObjectProvider newForCachedPC(ExecutionContext ec, Object id, CachedPC cachedPC)
    {
        AbstractClassMetaData cmd = 
            ec.getMetaDataManager().getMetaDataForClass(cachedPC.getObjectClass(), ec.getClassLoaderResolver());
        ObjectProvider op = getObjectProvider(ec, cmd);
        op.initialiseForCachedPC(cachedPC, id);
        return op;
    }

    /**
     * Hook to allow an ObjectProvider to mark itself as disconnected so that it is returned to the pool.
     * @param op The ObjectProvider to re-pool
     */
    public void disconnectObjectProvider(ObjectProvider op)
    {
        // TODO Enable this when the pool is working in multithreaded mode [NUCCORE-1007]
//        opPool.checkIn(op);
    }

    protected ObjectProvider getObjectProvider(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        return (ObjectProvider) ClassUtils.newInstance(opClass, OBJECT_PROVIDER_CTR_ARG_CLASSES, new Object[] {ec, cmd});
        // TODO Enable this when the pool is working in multithreaded mode [NUCCORE-1007]
//        return opPool.checkOut(ec, cmd);
    }

    private Class getInitialisedClassForClass(Class pcCls, ClassLoaderResolver clr)
    {
        try
        {
            // calling the CLR will make sure the class is initialized
            return clr.classForName(pcCls.getName(), pcCls.getClassLoader(), true);
        }
        catch (ClassNotResolvedException e)
        {
            throw new NucleusUserException(LOCALISER.msg("026015", pcCls.getName())).setFatal();
        }
    }
}