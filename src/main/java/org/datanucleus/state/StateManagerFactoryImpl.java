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

import org.datanucleus.ClassConstants;
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
 * Factory for StateManagers.
 */
public class StateManagerFactoryImpl implements StateManagerFactory
{
    /** Class for the default StateManager type. */
    Class<? extends DNStateManager> smClass = null;

    public static final Class[] STATE_MANAGER_CTR_ARG_CLASSES = new Class[] {ClassConstants.EXECUTION_CONTEXT, AbstractClassMetaData.class};

    // Single pool of all StateManager objects. TODO Consider having one pool per object type.
//    StateManagerPool smPool = null;

    public StateManagerFactoryImpl(PersistenceNucleusContext nucCtx)
    {
        Configuration conf = nucCtx.getConfiguration();
        String smClassName = conf.getStringProperty(PropertyNames.PROPERTY_STATE_MANAGER_CLASS_NAME);
        if (StringUtils.isWhitespace(smClassName))
        {
            // Use default StateManager for the StoreManager
            smClassName = nucCtx.getStoreManager().getDefaultStateManagerClassName();
        }
        smClass = nucCtx.getClassLoaderResolver(null).classForName(smClassName);

//        smPool = new StateManagerPool(conf.getIntProperty(PropertyNames.PROPERTY_OBJECT_PROVIDER_MAX_IDLE),
//            conf.getBooleanProperty(PropertyNames.PROPERTY_OBJECT_PROVIDER_REAPER_THREAD),
//            smClass);
    }

    public void close()
    {
//        smPool.close();
    }

    /**
     * Constructs a StateManager to manage a hollow instance having the given object ID.
     * This constructor is used for creating new instances of existing persistent objects.
     * @param ec the ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param id the identity of the object.
     */
    public <T> DNStateManager<T> newForHollow(ExecutionContext ec, Class<T> pcClass, Object id)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForHollow(id, null, cls);
        return sm;
    }

    /**
     * Constructs a StateManager to manage a recently populated hollow instance having the
     * given object ID and the given field values. This constructor is used for
     * creating new instances of persistent objects obtained e.g. via a Query or backed by a view.
     * @param ec ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param id the identity of the object.
     * @param fv the initial field values of the object.
     */
    public <T> DNStateManager<T> newForHollow(ExecutionContext ec, Class<T> pcClass, Object id, FieldValues fv)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForHollow(id, fv, cls);
        return sm;
    }

    /**
     * Constructs a StateManager to manage a hollow instance having the given object ID.
     * The instance is already supplied.
     * @param ec ExecutionContext
     * @param id the identity of the object.
     * @param pc The object that is hollow that we are going to manage
     */
    public <T> DNStateManager<T> newForHollowPreConstructed(ExecutionContext ec, Object id, T pc)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForHollowPreConstructed(id, pc);
        return sm;
    }

    /**
     * Constructs a StateManager to manage a hollow (or P-clean) instance having the given FieldValues.
     * This constructor is used for creating new instances of existing persistent objects using application identity.
     * @param ec ExecutionContext
     * @param pcClass the class of the new instance to be created.
     * @param fv the initial field values of the object.
     * @deprecated Use newForHollow instead
     */
    public <T> DNStateManager<T> newForHollowPopulatedAppId(ExecutionContext ec, Class<T> pcClass, final FieldValues fv)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForHollowAppId(fv, cls);
        return sm;
    }

    /**
     * Constructs a StateManager to manage the specified persistent instance having the given object ID.
     * @param ec ExecutionContext
     * @param id the identity of the object.
     * @param pc The object that is persistent that we are going to manage
     */
    public <T> DNStateManager<T> newForPersistentClean(ExecutionContext ec, Object id, T pc)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForPersistentClean(id, pc);
        return sm;
    }

    /**
     * Constructs a StateManager to manage a persistable instance that will
     * be EMBEDDED/SERIALISED into another persistable object. The instance will not be
     * assigned an identity in the process since it is a SCO.
     * @param ec ExecutionContext
     * @param pc The persistable to manage (see copyPc also)
     * @param copyPc Whether the SM should manage a copy of the passed PC or that one
     * @param ownerSM Owner StateManager
     * @param ownerFieldNumber Field number in owner object where this is stored
     */
    public <T> DNStateManager<T> newForEmbedded(ExecutionContext ec, T pc, boolean copyPc, DNStateManager ownerSM, int ownerFieldNumber)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForEmbedded(pc, copyPc);
        if (ownerSM != null)
        {
            ec.registerEmbeddedRelation(ownerSM, ownerFieldNumber, sm);
        }
        return sm;
    }

    /**
     * Constructs a StateManager for an object of the specified type, creating the PC object to hold the values
     * where this object will be EMBEDDED/SERIALISED into another persistable object. The instance will not be
     * assigned an identity in the process since it is a SCO.
     * @param ec ExecutionContext
     * @param cmd Meta-data for the class that this is an instance of.
     * @param ownerSM Owner StateManager
     * @param ownerFieldNumber Field number in owner object where this is stored
     */
    public DNStateManager newForEmbedded(ExecutionContext ec, AbstractClassMetaData cmd, DNStateManager ownerSM, int ownerFieldNumber)
    {
        Class pcClass = ec.getClassLoaderResolver().classForName(cmd.getFullClassName());
        DNStateManager sm = newForHollow(ec, pcClass, null);
        sm.initialiseForEmbedded(sm.getObject(), false);
        if (ownerSM != null)
        {
            ec.registerEmbeddedRelation(ownerSM, ownerFieldNumber, sm);
        }
        return sm;
    }

    /**
     * Constructs a StateManager to manage a transient instance that is becoming newly persistent.
     * A new object ID for the instance is obtained from the store manager and the object is inserted
     * in the data store. This constructor is used for assigning StateManagers to existing
     * instances that are transitioning to a persistent state.
     * @param ec ExecutionContext
     * @param pc the instance being make persistent.
     * @param preInsertChanges Any changes to make before inserting
     */
    public <T> DNStateManager<T> newForPersistentNew(ExecutionContext ec, T pc, FieldValues preInsertChanges)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForPersistentNew(pc, preInsertChanges);
        return sm;
    }

    /**
     * Constructs a StateManager to manage a transactional-transient instance.
     * A new object ID for the instance is obtained from the store manager and the object is inserted in 
     * the data store. This constructor is used for assigning a StateManager to a transient instance
     * that is transitioning to a transient clean state.
     * @param ec ExecutionContext
     * @param pc the instance being make persistent.
     */
    public <T> DNStateManager<T> newForTransactionalTransient(ExecutionContext ec, T pc)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForTransactionalTransient(pc);
        return sm;
    }

    /**
     * Constructor for creating a StateManager to manage a persistable object in detached state.
     * @param ec ExecutionContext
     * @param pc the detached object
     * @param id the identity of the object.
     * @param version the detached version
     */
    public <T> DNStateManager<T> newForDetached(ExecutionContext ec, T pc, Object id, Object version)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForDetached(pc, id, version);
        return sm;
    }

    /**
     * Constructor for creating a StateManager to manage a persistable object that is not persistent yet
     * is about to be deleted. Consequently the initial lifecycle state will be P_NEW, but will soon
     * move to P_NEW_DELETED.
     * @param ec Execution Context
     * @param pc the object being deleted from persistence
     */
    public <T> DNStateManager<T> newForPNewToBeDeleted(ExecutionContext ec, T pc)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForPNewToBeDeleted(pc);
        return sm;
    }

    /**
     * Constructor to create a StateManager for an object taken from the L2 cache with the specified id.
     * Creates an object that the cached object represents, assigns a StateManager to it, and copies across 
     * the fields that were cached, and its version (if available).
     * @param ec ExecutionContext
     * @param id Id to assign to the persistable object
     * @param cachedPC CachedPC object from the L2 cache
     */
    public <T> DNStateManager<T> newForCachedPC(ExecutionContext ec, Object id, CachedPC cachedPC)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(cachedPC.getObjectClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForCachedPC(cachedPC, id);
        return sm;
    }

    /**
     * Hook to allow a StateManager to mark itself as disconnected so that it is returned to the pool.
     * @param sm StateManager to re-pool
     */
    public void disconnectStateManager(DNStateManager sm)
    {
        // TODO Enable this when the pool is working in multithreaded mode [NUCCORE-1007]
//        smPool.checkIn(sm);
    }

    protected DNStateManager getStateManager(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        return ClassUtils.newInstance(smClass, STATE_MANAGER_CTR_ARG_CLASSES, new Object[] {ec, cmd});
        // TODO Enable this when the pool is working in multithreaded mode [NUCCORE-1007]
//        return smPool.checkOut(ec, cmd);
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
            throw new NucleusUserException(Localiser.msg("026015", pcCls.getName())).setFatal();
        }
    }
}