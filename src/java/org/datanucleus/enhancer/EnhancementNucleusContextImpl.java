/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancer;

import java.util.Map;
import java.util.Set;

import org.datanucleus.AbstractNucleusContext;
import org.datanucleus.ExecutionContext;
import org.datanucleus.ExecutionContextPool;
import org.datanucleus.FetchGroup;
import org.datanucleus.FetchGroupManager;
import org.datanucleus.ImplementationCreator;
import org.datanucleus.ExecutionContext.LifecycleListener;
import org.datanucleus.cache.Level2Cache;
import org.datanucleus.identity.IdentityKeyTranslator;
import org.datanucleus.identity.IdentityStringTranslator;
import org.datanucleus.management.FactoryStatistics;
import org.datanucleus.management.jmx.ManagementManager;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.state.ObjectProviderFactory;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.autostart.AutoStartMechanism;
import org.datanucleus.transaction.TransactionManager;
import org.datanucleus.transaction.jta.JTASyncRegistry;

/**
 * NucleusContext to use when enhancing. Just provides basic plugin, type and metadata services.
 */
public class EnhancementNucleusContextImpl extends AbstractNucleusContext
{
    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     */
    public EnhancementNucleusContextImpl(String apiName, Map startupProps)
    {
        this(apiName, startupProps, null);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     * @param pluginMgr Plugin Manager (or null if wanting it to be created)
     */
    public EnhancementNucleusContextImpl(String apiName, Map startupProps, PluginManager pluginMgr)
    {
        super(apiName, startupProps, pluginMgr);
    }

    public void close()
    {
        if (metaDataManager != null)
        {
            metaDataManager.close();
            metaDataManager = null;
        }
        if (classLoaderResolverMap != null)
        {
            classLoaderResolverMap.clear();
            classLoaderResolverMap = null;
        }
        if (typeManager != null)
        {
            typeManager = null;
        }
    }

    // TODO Drop these methods when we remove them from NucleusContext
    public AutoStartMechanism getAutoStartMechanism()
    {
        return null;
    }

    public ExecutionContextPool getExecutionContextPool()
    {
        return null;
    }

    public ObjectProviderFactory getObjectProviderFactory()
    {
        return null;
    }

    public ExecutionContext getExecutionContext(Object owner, Map<String, Object> options)
    {
        return null;
    }

    public Class getDatastoreIdentityClass()
    {
        return null;
    }

    public IdentityStringTranslator getIdentityStringTranslator()
    {
        return null;
    }

    public IdentityKeyTranslator getIdentityKeyTranslator()
    {
        return null;
    }

    public boolean statisticsEnabled()
    {
        return false;
    }

    public ManagementManager getJMXManager()
    {
        return null;
    }

    public FactoryStatistics getStatistics()
    {
        return null;
    }

    public ImplementationCreator getImplementationCreator()
    {
        return null;
    }

    public TransactionManager getTransactionManager()
    {
        return null;
    }

    public javax.transaction.TransactionManager getJtaTransactionManager()
    {
        return null;
    }

    public JTASyncRegistry getJtaSyncRegistry()
    {
        return null;
    }

    public boolean isStoreManagerInitialised()
    {
        return false;
    }

    public StoreManager getStoreManager()
    {
        return null;
    }

    public CallbackHandler getValidationHandler(ExecutionContext ec)
    {
        return null;
    }

    public boolean hasLevel2Cache()
    {
        return false;
    }

    public Level2Cache getLevel2Cache()
    {
        return null;
    }

    public LifecycleListener[] getExecutionContextListeners()
    {
        return null;
    }

    public void addExecutionContextListener(LifecycleListener listener)
    {
    }

    public void removeExecutionContextListener(LifecycleListener listener)
    {
    }

    public void setJcaMode(boolean jca)
    {
    }

    public boolean isJcaMode()
    {
        return false;
    }

    public FetchGroupManager getFetchGroupManager()
    {
        return null;
    }

    public void addInternalFetchGroup(FetchGroup grp)
    {
    }

    public void removeInternalFetchGroup(FetchGroup grp)
    {
    }

    public FetchGroup createInternalFetchGroup(Class cls, String name)
    {
        return null;
    }

    public FetchGroup getInternalFetchGroup(Class cls, String name)
    {
        return null;
    }

    public Set<FetchGroup> getFetchGroupsWithName(String name)
    {
        return null;
    }

    public boolean isClassWithIdentityCacheable(Object id)
    {
        return false;
    }

    public boolean isClassCacheable(AbstractClassMetaData cmd)
    {
        return false;
    }

    protected void logConfigurationDetails()
    {
    }
}