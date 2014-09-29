/**********************************************************************
Copyright (c) 2008 Erik Bengtson and others. All rights reserved. 
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
2011 Andy Jefferson - completely rewritten to be usable
    ...
**********************************************************************/
package org.datanucleus.store.federation;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.NucleusContext;
import org.datanucleus.NucleusContextHelper;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.flush.FlushProcess;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.state.ReferentialStateManagerImpl;
import org.datanucleus.store.Extent;
import org.datanucleus.store.NucleusConnection;
import org.datanucleus.store.NucleusSequence;
import org.datanucleus.store.StoreData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.StorePersistenceHandler;
import org.datanucleus.store.connection.ConnectionManager;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.query.QueryManager;
import org.datanucleus.store.schema.StoreSchemaHandler;
import org.datanucleus.store.schema.naming.NamingFactory;
import org.datanucleus.store.valuegenerator.ValueGenerationManager;
import org.datanucleus.util.NucleusLogger;

/**
 * A federated StoreManager orchestrates the persistence/retrieval for multiple datastores.
 * It is responsible for creating the individual StoreManager instances for the datastore(s)
 * that are being federated. Has a "primary" StoreManager where data is stored by default when no metadata
 * is specified for a class, and then has a map of "secondary" StoreManagers keyed by name that can be used
 * for persistence as defined in metadata. When a request comes in to persist some data, this class is
 * responsible for selecting the appropriate StoreManager for persistence. When a request comes in to query
 * some data, this class is responsible for selecting the appropriate StoreManager to query.
 * <p>
 * Assumes that there are persistence properties of the form
 * <pre>
 * datanucleus.datastore.SecondStore=secondstore.properties
 * datanucleus.datastore.ThirdStore=thirdstore.properties
 * </pre>
 * where these properties files have the properties for the secondary stores with names "SecondStore", "ThirdStore".
 */
public class FederatedStoreManager implements StoreManager
{
    public static final String PROPERTY_DATA_FEDERATION_DATASTORE_NAME = "DATA_FEDERATION_DATASTORE_NAME";

    /** Primary StoreManager. */
    StoreManager primaryStoreMgr;

    /** Map of secondary StoreManager keyed by their symbolic name. */
    Map<String, StoreManager> secondaryStoreMgrMap = null;

    final NucleusContext nucleusContext;

    /** Persistence handler. */
    protected StorePersistenceHandler persistenceHandler = null;

    /** Query Manager. Lazy initialised, so use getQueryManager() to access. */
    private QueryManager queryMgr = null;

    public FederatedStoreManager(ClassLoaderResolver clr, NucleusContext nucleusContext)
    {
        this.nucleusContext = nucleusContext;

        // Primary StoreManager
        Map<String, Object> datastoreProps = nucleusContext.getConfiguration().getDatastoreProperties();
        this.primaryStoreMgr = NucleusContextHelper.createStoreManagerForProperties(
            nucleusContext.getConfiguration().getPersistenceProperties(), 
            datastoreProps, clr, nucleusContext);

        // Correct transaction isolation level to match the datastore capabilities
        String transactionIsolation = nucleusContext.getConfiguration().getStringProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION);
        if (transactionIsolation != null)
        {
            String reqdIsolation = NucleusContextHelper.getTransactionIsolationForStoreManager(primaryStoreMgr, transactionIsolation);
            if (!transactionIsolation.equalsIgnoreCase(reqdIsolation))
            {
                nucleusContext.getConfiguration().setProperty(PropertyNames.PROPERTY_TRANSACTION_ISOLATION, reqdIsolation);
            }
        }

        Set<String> propNamesWithDatastore = nucleusContext.getConfiguration().getPropertyNamesWithPrefix("datanucleus.datastore.");
        if (propNamesWithDatastore != null)
        {
            secondaryStoreMgrMap = new HashMap<String, StoreManager>();

            Iterator<String> nameIter = propNamesWithDatastore.iterator();
            while (nameIter.hasNext())
            {
                String datastorePropName = nameIter.next();
                String datastoreName = datastorePropName.substring("datanucleus.datastore.".length());
                String filename = nucleusContext.getConfiguration().getStringProperty(datastorePropName);

                Configuration datastoreConf = new Configuration(nucleusContext);
                datastoreConf.setPropertiesUsingFile(filename);
                datastoreConf.setProperty(PROPERTY_DATA_FEDERATION_DATASTORE_NAME, datastoreName);
                StoreManager storeMgr = NucleusContextHelper.createStoreManagerForProperties(
                    datastoreConf.getPersistenceProperties(), datastoreConf.getDatastoreProperties(), clr, 
                    nucleusContext);
                secondaryStoreMgrMap.put(datastoreName, storeMgr);
            }
        }

        this.persistenceHandler = new FederatedPersistenceHandler(this);
    }

    public NucleusContext getNucleusContext()
    {
        return nucleusContext;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getMetaDataManager()
     */
    @Override
    public MetaDataManager getMetaDataManager()
    {
        return nucleusContext.getMetaDataManager();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getFlushProcess()
     */
    public FlushProcess getFlushProcess()
    {
        // TODO Ideally we want to use an ordered flush if one of the stores uses that, otherwise use FlushNonReferential
        return primaryStoreMgr.getFlushProcess();
    }

    public void close()
    {
        primaryStoreMgr.close();
        primaryStoreMgr = null;

        if (secondaryStoreMgrMap != null)
        {
            Iterator<String> secondaryNameIter = secondaryStoreMgrMap.keySet().iterator();
            while (secondaryNameIter.hasNext())
            {
                String name = secondaryNameIter.next();
                StoreManager secStoreMgr = secondaryStoreMgrMap.get(name);
                secStoreMgr.close();
            }
            secondaryStoreMgrMap.clear();
            secondaryStoreMgrMap = null;
        }

        persistenceHandler.close();

        if (queryMgr != null)
        {
            queryMgr.close();
            queryMgr = null;
        }
    }

    /**
     * Accessor for the StoreManager to use for persisting the specified class.
     * TODO Extend this so that we can persist some objects of one type into one datastore, and other
     * objects of that type into a different datastore.
     * @param cmd Metadata for the class
     * @return The StoreManager to use
     */
    public StoreManager getStoreManagerForClass(AbstractClassMetaData cmd)
    {
        if (cmd.hasExtension("datastore"))
        {
            String datastoreName = cmd.getValueForExtension("datastore");
            if (secondaryStoreMgrMap == null || !secondaryStoreMgrMap.containsKey(datastoreName))
            {
                throw new NucleusUserException("Class " + cmd.getFullClassName() + " specified to persist to datastore " +
                    datastoreName + " yet not defined");
            }
            return secondaryStoreMgrMap.get(datastoreName);
        }
        return primaryStoreMgr;
    }

    /**
     * Accessor for the StoreManager to use for the specified class.
     * @param className Name of the class
     * @param clr ClassLoader resolver
     * @return The StoreManager to use
     */
    public StoreManager getStoreManagerForClass(String className, ClassLoaderResolver clr)
    {
        // TODO Cater for class being persisted to multiple datastores
        AbstractClassMetaData cmd = nucleusContext.getMetaDataManager().getMetaDataForClass(className, clr);
        return getStoreManagerForClass(cmd);
    }

    public void manageClasses(ClassLoaderResolver clr, String... classNames)
    {
        if (classNames != null)
        {
            for (int i=0;i<classNames.length;i++)
            {
                getStoreManagerForClass(classNames[i], clr).manageClasses(clr, classNames[i]);
            }
        }
    }

    public NamingFactory getNamingFactory()
    {
        return primaryStoreMgr.getNamingFactory();
    }

    public ApiAdapter getApiAdapter()
    {
        return nucleusContext.getApiAdapter();
    }

    public String getClassNameForObjectID(Object id, ClassLoaderResolver clr, ExecutionContext ec)
    {
        return primaryStoreMgr.getClassNameForObjectID(id, clr, ec);
    }

    public Date getDatastoreDate()
    {
        return primaryStoreMgr.getDatastoreDate();
    }

    public Extent getExtent(ExecutionContext ec, Class c, boolean subclasses)
    {
        return getStoreManagerForClass(c.getName(), ec.getClassLoaderResolver()).getExtent(ec, c, subclasses);
    }

    public boolean isJdbcStore()
    {
        return primaryStoreMgr.isJdbcStore();
    }

    public NucleusConnection getNucleusConnection(ExecutionContext ec)
    {
        return primaryStoreMgr.getNucleusConnection(ec);
    }

    public NucleusSequence getNucleusSequence(ExecutionContext ec, SequenceMetaData seqmd)
    {
        return primaryStoreMgr.getNucleusSequence(ec, seqmd);
    }

    public StoreSchemaHandler getSchemaHandler()
    {
        return primaryStoreMgr.getSchemaHandler();
    }

    public StoreData getStoreDataForClass(String className)
    {
        // TODO If not present in primary store, try secondary stores
        return primaryStoreMgr.getStoreDataForClass(className);
    }

    public StorePersistenceHandler getPersistenceHandler()
    {
        return persistenceHandler;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getQueryManager()
     */
    public QueryManager getQueryManager()
    {
        if (queryMgr == null)
        {
            queryMgr = new FederatedQueryManagerImpl(nucleusContext, this);
        }
        return queryMgr;
    }

    public ValueGenerationManager getValueGenerationManager()
    {
        return primaryStoreMgr.getValueGenerationManager();
    }

    public String getStoreManagerKey()
    {
        return primaryStoreMgr.getStoreManagerKey();
    }

    public String getQueryCacheKey()
    {
        return primaryStoreMgr.getQueryCacheKey();
    }

    public Object getStrategyValue(ExecutionContext ec, AbstractClassMetaData cmd, int absoluteFieldNumber)
    {
        return getStoreManagerForClass(cmd).getStrategyValue(ec, cmd, absoluteFieldNumber);
    }

    public Collection<String> getSubClassesForClass(String className, boolean includeDescendents, ClassLoaderResolver clr)
    {
        return getStoreManagerForClass(className, clr).getSubClassesForClass(className, includeDescendents, clr);
    }

    public boolean isStrategyDatastoreAttributed(AbstractClassMetaData cmd, int absFieldNumber)
    {
        return getStoreManagerForClass(cmd).isStrategyDatastoreAttributed(cmd, absFieldNumber);
    }

    public String manageClassForIdentity(Object id, ClassLoaderResolver clr)
    {
        NucleusLogger.PERSISTENCE.debug(">> TODO Need to allocate manageClassForIdentity(" + id + ") to correct store manager");
        // TODO Work out if this class is in this store manager
        return primaryStoreMgr.manageClassForIdentity(id, clr);
    }

    public boolean managesClass(String className)
    {
        return primaryStoreMgr.managesClass(className);
    }

    public void printInformation(String category, PrintStream ps) throws Exception
    {
        primaryStoreMgr.printInformation(category, ps);        
    }

    public void unmanageAllClasses(ClassLoaderResolver clr)
    {
        primaryStoreMgr.unmanageAllClasses(clr);
        if (secondaryStoreMgrMap != null)
        {
            Collection<StoreManager> secStoreMgrs = secondaryStoreMgrMap.values();
            for (StoreManager storeMgr : secStoreMgrs)
            {
                storeMgr.unmanageAllClasses(clr);
            }
        }
    }

    public void unmanageClass(ClassLoaderResolver clr, String className, boolean removeFromDatastore)
    {
        primaryStoreMgr.unmanageClass(clr, className, removeFromDatastore);
        if (secondaryStoreMgrMap != null)
        {
            Collection<StoreManager> secStoreMgrs = secondaryStoreMgrMap.values();
            for (StoreManager storeMgr : secStoreMgrs)
            {
                storeMgr.unmanageClass(clr, className, removeFromDatastore);
            }
        }
    }

    public boolean supportsQueryLanguage(String language)
    {
        return primaryStoreMgr.supportsQueryLanguage(language);
    }

    public String getNativeQueryLanguage()
    {
        return primaryStoreMgr.getNativeQueryLanguage();
    }

    public boolean supportsValueStrategy(String language)
    {
        return primaryStoreMgr.supportsValueStrategy(language);
    }

    public Collection getSupportedOptions()
    {
        return primaryStoreMgr.getSupportedOptions();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionManager()
     */
    public ConnectionManager getConnectionManager()
    {
        return primaryStoreMgr.getConnectionManager();
    }

    public ManagedConnection getConnection(ExecutionContext ec)
    {
        return primaryStoreMgr.getConnection(ec);
    }
    
    public ManagedConnection getConnection(ExecutionContext ec, Map options)
    {
        return primaryStoreMgr.getConnection(ec, options);
    }

    public ManagedConnection getConnection(int isolation_level)
    {
        return primaryStoreMgr.getConnection(isolation_level);
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionDriverName()
     */
    public String getConnectionDriverName()
    {
        return primaryStoreMgr.getConnectionDriverName();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionURL()
     */
    public String getConnectionURL()
    {
        return primaryStoreMgr.getConnectionURL();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionUserName()
     */
    public String getConnectionUserName()
    {
        return primaryStoreMgr.getConnectionUserName();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionPassword()
     */
    public String getConnectionPassword()
    {
        return primaryStoreMgr.getConnectionPassword();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionFactory()
     */
    public Object getConnectionFactory()
    {
        return primaryStoreMgr.getConnectionFactory();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionFactory2()
     */
    public Object getConnectionFactory2()
    {
        return primaryStoreMgr.getConnectionFactory2();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionFactory2Name()
     */
    public String getConnectionFactory2Name()
    {
        return primaryStoreMgr.getConnectionFactory2Name();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getConnectionFactoryName()
     */
    public String getConnectionFactoryName()
    {
        return primaryStoreMgr.getConnectionFactoryName();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getProperty(java.lang.String)
     */
    public Object getProperty(String name)
    {
        return primaryStoreMgr.getProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#hasProperty(java.lang.String)
     */
    public boolean hasProperty(String name)
    {
        return primaryStoreMgr.hasProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getIntProperty(java.lang.String)
     */
    public int getIntProperty(String name)
    {
        return primaryStoreMgr.getIntProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getBooleanProperty(java.lang.String)
     */
    public boolean getBooleanProperty(String name)
    {
        return primaryStoreMgr.getBooleanProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getBooleanProperty(java.lang.String, boolean)
     */
    public boolean getBooleanProperty(String name, boolean resultIfNotSet)
    {
        return primaryStoreMgr.getBooleanProperty(name, resultIfNotSet);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getBooleanObjectProperty(java.lang.String)
     */
    public Boolean getBooleanObjectProperty(String name)
    {
        return primaryStoreMgr.getBooleanObjectProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getStringProperty(java.lang.String)
     */
    public String getStringProperty(String name)
    {
        return primaryStoreMgr.getStringProperty(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#transactionStarted(org.datanucleus.store.ExecutionContext)
     */
    public void transactionStarted(ExecutionContext ec)
    {
        primaryStoreMgr.transactionStarted(ec);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#transactionCommitted(org.datanucleus.store.ExecutionContext)
     */
    public void transactionCommitted(ExecutionContext ec)
    {
        primaryStoreMgr.transactionCommitted(ec);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#transactionRolledBack(org.datanucleus.store.ExecutionContext)
     */
    public void transactionRolledBack(ExecutionContext ec)
    {
        primaryStoreMgr.transactionRolledBack(ec);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#useBackedSCOWrapperForMember(org.datanucleus.metadata.AbstractMemberMetaData, org.datanucleus.store.ExecutionContext)
     */
    public boolean useBackedSCOWrapperForMember(AbstractMemberMetaData mmd, ExecutionContext ec)
    {
        return primaryStoreMgr.useBackedSCOWrapperForMember(mmd, ec);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StoreManager#getDefaultObjectProviderClassName()
     */
    public String getDefaultObjectProviderClassName()
    {
        return ReferentialStateManagerImpl.class.getName();
    }
}