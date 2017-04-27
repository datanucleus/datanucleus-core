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
package org.datanucleus;

import java.util.Map;
import java.util.Set;

import org.datanucleus.cache.Level2Cache;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.IdentityManager;
import org.datanucleus.management.FactoryStatistics;
import org.datanucleus.management.ManagementManager;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.state.ObjectProviderFactory;
import org.datanucleus.store.autostart.AutoStartMechanism;
import org.datanucleus.transaction.ResourcedTransactionManager;
import org.datanucleus.transaction.jta.JTASyncRegistry;

/**
 * Context for use in the persistence process.
 * Adds on many extra services to the basic+store contexts, for transactions, executionContext, identity handling, object providers, autostart, L2 cache etc.
 */
public interface PersistenceNucleusContext extends StoreNucleusContext
{
    AutoStartMechanism getAutoStartMechanism();

    ObjectProviderFactory getObjectProviderFactory();

    ExecutionContextPool getExecutionContextPool();

    /**
     * Method to return an ExecutionContext for use in persistence.
     * @param owner The owner object for the context. PersistenceManager/EntityManager typically.
     * @param options Any options affecting startup
     * @return The ExecutionContext
     */
    ExecutionContext getExecutionContext(Object owner, Map<String, Object> options);

    /**
     * Accessor for a manager for identities.
     * @return The identity manager to use
     */
    IdentityManager getIdentityManager();

    /**
     * Accessor for whether statistics gathering is enabled.
     * @return Whether the user has enabled statistics or JMX management is enabled
     */
    boolean statisticsEnabled();

    /**
     * Accessor for the JMX manager (if required).
     * Does nothing if the property "datanucleus.jmxType" is unset.
     * @return The JMX manager
     */
    ManagementManager getJMXManager();

    FactoryStatistics getStatistics();

    ImplementationCreator getImplementationCreator();

    ResourcedTransactionManager getResourcedTransactionManager();

    /**
     * Accessor for the JTA transaction manager, when using JTA.
     * @return the JTA Transaction Manager
     */
    javax.transaction.TransactionManager getJtaTransactionManager();

    /**
     * Accessor for the JTA Synchronization registry, when using JTA.
     * @return The JTASyncRegistry (or null if not using JTA)
     */
    JTASyncRegistry getJtaSyncRegistry();

    /**
     * Method to set the BeanValidation API factory to use (when using an external JavaEE environment usage).
     * @param validationFactory The validation factory
     */
    void setValidationFactory(Object validationFactory);

    /**
     * Method to return a handler for validation (JSR303).
     * @param ec The ExecutionContext that the handler is for.
     * @return The handler (or null if not supported on this PMF/EMF, or no validator present)
     */
    BeanValidatorHandler getValidationHandler(ExecutionContext ec);

    boolean hasLevel2Cache();

    Level2Cache getLevel2Cache();

    /**
     * Object the array of registered ExecutionContext listeners.
     * @return array of {@link org.datanucleus.ExecutionContext.LifecycleListener}
     */
    ExecutionContext.LifecycleListener[] getExecutionContextListeners();

    /**
     * Register a new Listener for ExecutionContext events.
     * @param listener the listener to register
     */
    void addExecutionContextListener(ExecutionContext.LifecycleListener listener);

    /**
     * Unregister a Listener from ExecutionContext events.
     * @param listener the listener to unregister
     */
    void removeExecutionContextListener(ExecutionContext.LifecycleListener listener);

    /**
     * Mutator for whether we are in JCA mode.
     * @param jca true if using JCA connector
     */
    void setJcaMode(boolean jca);

    /**
     * Accessor for the JCA mode.
     * @return true if using JCA connector.
     */
    boolean isJcaMode();

    /** 
     * Convenience accessor for the FetchGroupManager.
     * Creates it if not yet existing.
     * @return The FetchGroupManager
     */
    FetchGroupManager getFetchGroupManager();

    /**
     * Method to add a dynamic FetchGroup for use by this OMF.
     * @param grp The group
     */
    void addInternalFetchGroup(FetchGroup grp);

    /**
     * Method to remove a dynamic FetchGroup from use by this OMF.
     * @param grp The group
     */
    void removeInternalFetchGroup(FetchGroup grp);

    /**
     * Method to create a new internal fetch group for the class+name.
     * @param cls Class that it applies to
     * @param name Name of group
     * @return The group
     */
    FetchGroup createInternalFetchGroup(Class cls, String name);

    /**
     * Accessor for an internal fetch group for the specified class.
     * @param cls The class
     * @param name Name of the group
     * @param createIfNotPresent Whether to create the fetch group if not present
     * @return The FetchGroup
     * @throws NucleusUserException if the class is not persistable
     */
    FetchGroup getInternalFetchGroup(Class cls, String name, boolean createIfNotPresent);

    /**
     * Accessor for the fetch groups for the specified name.
     * @param name Name of the group
     * @return The FetchGroup
     */
    Set<FetchGroup> getFetchGroupsWithName(String name);

    /**
     * Convenience method to return if the supplied id is of an object that is cacheable in the L2 cache.
     * @param id The identity
     * @return Whether the object it refers to is considered cacheable
     */
    boolean isClassWithIdentityCacheable(Object id);

    /**
     * Convenience method to return if objects of this type are cached for this NucleusContext.
     * Uses the "cacheable" flag of the class, and the cache-mode to determine whether to cache
     * @param cmd MetaData for the class
     * @return Whether it is cacheable
     */
    boolean isClassCacheable(AbstractClassMetaData cmd);

    /**
     * Return whether we are managing a federated context (i.e a primary StoreManager, with some secondary StoreManager(s)).
     * @return Whether this is federated
     */
    boolean isFederated();

    /**
     * Accessor for whether the supplied class is multi-tenant (i.e with a tenancy id column).
     * @param cmd The class
     * @return Whether it is multi-tenant
     */
    boolean isClassMultiTenant(AbstractClassMetaData cmd);

    /**
     * Accessor for the tenant id for the supplied class and executionContext.
     * @param ec ExecutionContext
     * @param cmd The class
     * @return The tenant id for this class and context.
     */
    String getMultiTenancyId(ExecutionContext ec, AbstractClassMetaData cmd);
}