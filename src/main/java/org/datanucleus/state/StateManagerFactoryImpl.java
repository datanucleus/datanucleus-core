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
import org.datanucleus.PersistableObjectType;
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

        // Load class for default StateManager
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

    @Override
    public void close()
    {
//        smPool.close();
    }

    @Override
    public <T> DNStateManager<T> newForHollow(ExecutionContext ec, Class<T> pcClass, Object id)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForHollow(id, null, cls);
        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForHollow(ExecutionContext ec, Class<T> pcClass, Object id, FieldValues fv)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForHollow(id, fv, cls);
        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForHollowPreConstructed(ExecutionContext ec, Object id, T pc)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForHollowPreConstructed(id, pc);
        return sm;
    }

    @Deprecated
    @Override
    public <T> DNStateManager<T> newForHollowPopulatedAppId(ExecutionContext ec, Class<T> pcClass, final FieldValues fv)
    {
        Class cls = getInitialisedClassForClass(pcClass, ec.getClassLoaderResolver());
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pcClass, ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForHollowAppId(fv, cls);
        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForPersistentClean(ExecutionContext ec, Object id, T pc)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForPersistentClean(id, pc);
        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForEmbedded(ExecutionContext ec, T pc, boolean copyPc, DNStateManager ownerSM, int ownerMemberNumber, PersistableObjectType objectType)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForEmbedded(pc, copyPc);
        if (ownerSM != null)
        {
            ec.registerEmbeddedRelation(ownerSM, ownerMemberNumber, objectType, sm);
        }

        return sm;
    }

    @Override
    public DNStateManager newForEmbedded(ExecutionContext ec, AbstractClassMetaData cmd, DNStateManager ownerSM, int ownerMemberNumber, PersistableObjectType objectType)
    {
        // TODO Collection : If the member metadata has "fieldTypes" specified then we should use that data here rather than just the "cmd" of the element.
//      Class embeddedType = clr.classForName(embCmd.getFullClassName());
//      if (mmd.getFieldTypes() != null && mmd.getFieldTypes().length > 0)
//      {
//          // Embedded type has field-type defined so use that as our embedded type
//          embeddedType = ec.getClassLoaderResolver().classForName(mmd.getFieldTypes()[0]);
//      }

        Class pcClass = ec.getClassLoaderResolver().classForName(cmd.getFullClassName());
        DNStateManager sm = newForHollow(ec, pcClass, null);
        sm.initialiseForEmbedded(sm.getObject(), false);
        if (ownerSM != null)
        {
            ec.registerEmbeddedRelation(ownerSM, ownerMemberNumber, objectType, sm);
        }

        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForPersistentNew(ExecutionContext ec, T pc, FieldValues preInsertChanges)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForPersistentNew(pc, preInsertChanges);
        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForTransactionalTransient(ExecutionContext ec, T pc)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForTransactionalTransient(pc);
        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForDetached(ExecutionContext ec, T pc, Object id, Object version)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForDetached(pc, id, version);
        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForPNewToBeDeleted(ExecutionContext ec, T pc)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForPNewToBeDeleted(pc);
        return sm;
    }

    @Override
    public <T> DNStateManager<T> newForCachedPC(ExecutionContext ec, Object id, CachedPC cachedPC)
    {
        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(cachedPC.getObjectClass(), ec.getClassLoaderResolver());
        DNStateManager sm = getStateManager(ec, cmd);
        sm.initialiseForCachedPC(cachedPC, id);
        return sm;
    }

    @Override
    public void disconnectStateManager(DNStateManager sm)
    {
        // TODO Enable this when the pool is working in multithreaded mode [NUCCORE-1007]
//        smPool.checkIn(sm);
    }

    protected DNStateManager getStateManager(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        // TODO Potentially could allow SM class to use to be defined in the cmd
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