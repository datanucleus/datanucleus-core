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
package org.datanucleus.store.federation;

import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.StorePersistenceHandler;

/**
 * Persistence handler for federated datastores.
 * Distributes the inserts/updates/deletes/fetches to the appropriate datastore.
 */
public class FederatedPersistenceHandler implements StorePersistenceHandler
{
    /** Manager for the store. */
    FederatedStoreManager storeMgr;

    /**
     * Constructor.
     * @param storeMgr StoreManager
     */
    public FederatedPersistenceHandler(StoreManager storeMgr)
    {
        this.storeMgr = (FederatedStoreManager)storeMgr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#close()
     */
    public void close()
    {
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#useReferentialIntegrity()
     */
    public boolean useReferentialIntegrity()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#insertObjects(org.datanucleus.store.ObjectProvider[])
     */
    public void insertObjects(ObjectProvider... sms)
    {
        // TODO Support splitting the array into the respective datastore
        for (int i=0;i<sms.length;i++)
        {
            insertObject(sms[i]);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#deleteObjects(org.datanucleus.store.ObjectProvider[])
     */
    public void deleteObjects(ObjectProvider... sms)
    {
        // TODO Support splitting the array into the respective datastore
        for (int i=0;i<sms.length;i++)
        {
            deleteObject(sms[i]);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#batchStart(org.datanucleus.store.ExecutionContext)
     */
    public void batchStart(ExecutionContext ec, PersistenceBatchType batchType)
    {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#batchEnd(org.datanucleus.store.ExecutionContext)
     */
    public void batchEnd(ExecutionContext ec, PersistenceBatchType type)
    {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#insertObject(org.datanucleus.store.ObjectProvider)
     */
    public void insertObject(ObjectProvider sm)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(sm.getClassMetaData());
        classStoreMgr.getPersistenceHandler().insertObject(sm);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#updateObject(org.datanucleus.store.ObjectProvider, int[])
     */
    public void updateObject(ObjectProvider sm, int[] fieldNumbers)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(sm.getClassMetaData());
        classStoreMgr.getPersistenceHandler().updateObject(sm, fieldNumbers);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#deleteObject(org.datanucleus.store.ObjectProvider)
     */
    public void deleteObject(ObjectProvider sm)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(sm.getClassMetaData());
        classStoreMgr.getPersistenceHandler().deleteObject(sm);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#fetchObject(org.datanucleus.store.ObjectProvider, int[])
     */
    public void fetchObject(ObjectProvider sm, int[] fieldNumbers)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(sm.getClassMetaData());
        classStoreMgr.getPersistenceHandler().fetchObject(sm, fieldNumbers);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#fetchObjects(int[], org.datanucleus.state.ObjectProvider[])
     */
    @Override
    public void fetchObjects(int[] fieldNumbers, ObjectProvider... sms)
    {
        // Override this to provide bulk fetching of the same fields from multiple objects
        for (ObjectProvider sm : sms)
        {
            fetchObject(sm, fieldNumbers);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#locateObject(org.datanucleus.store.ObjectProvider)
     */
    public void locateObject(ObjectProvider sm)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(sm.getClassMetaData());
        classStoreMgr.getPersistenceHandler().locateObject(sm);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#locateObjects(org.datanucleus.store.ObjectProvider[])
     */
    public void locateObjects(ObjectProvider[] sms)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(sms[0].getClassMetaData());
        classStoreMgr.getPersistenceHandler().locateObjects(sms);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#findObject(org.datanucleus.store.ExecutionContext, java.lang.Object)
     */
    public Object findObject(ExecutionContext ec, Object id)
    {
        // TODO Find the class of the object and then hand to the appropriate datastore
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#findObjects(org.datanucleus.store.ExecutionContext, java.lang.Object[])
     */
    public Object[] findObjects(ExecutionContext ec, Object[] ids)
    {
        // TODO Find the class of the object(s) and then hand to the appropriate datastore
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#findObjectForKeys(org.datanucleus.ExecutionContext, org.datanucleus.metadata.AbstractClassMetaData, java.lang.String[], java.lang.Object[])
     */
    @Override
    public Object findObjectForUnique(ExecutionContext ec, AbstractClassMetaData cmd, String[] memberNames, Object[] values)
    {
        // TODO Find the appropriate datastore and process there
        return null;
    }
}