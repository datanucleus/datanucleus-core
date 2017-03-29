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
    public void insertObjects(ObjectProvider... ops)
    {
        // TODO Support splitting the array into the respective datastore
        for (int i=0;i<ops.length;i++)
        {
            insertObject(ops[i]);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#deleteObjects(org.datanucleus.store.ObjectProvider[])
     */
    public void deleteObjects(ObjectProvider... ops)
    {
        // TODO Support splitting the array into the respective datastore
        for (int i=0;i<ops.length;i++)
        {
            deleteObject(ops[i]);
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
    public void insertObject(ObjectProvider op)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(op.getClassMetaData());
        classStoreMgr.getPersistenceHandler().insertObject(op);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#updateObject(org.datanucleus.store.ObjectProvider, int[])
     */
    public void updateObject(ObjectProvider op, int[] fieldNumbers)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(op.getClassMetaData());
        classStoreMgr.getPersistenceHandler().updateObject(op, fieldNumbers);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#deleteObject(org.datanucleus.store.ObjectProvider)
     */
    public void deleteObject(ObjectProvider op)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(op.getClassMetaData());
        classStoreMgr.getPersistenceHandler().deleteObject(op);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#fetchObject(org.datanucleus.store.ObjectProvider, int[])
     */
    public void fetchObject(ObjectProvider op, int[] fieldNumbers)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(op.getClassMetaData());
        classStoreMgr.getPersistenceHandler().fetchObject(op, fieldNumbers);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#fetchObjects(int[], org.datanucleus.state.ObjectProvider[])
     */
    @Override
    public void fetchObjects(int[] fieldNumbers, ObjectProvider... ops)
    {
        // Override this to provide bulk fetching of the same fields from multiple objects
        for (ObjectProvider op : ops)
        {
            fetchObject(op, fieldNumbers);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#locateObject(org.datanucleus.store.ObjectProvider)
     */
    public void locateObject(ObjectProvider op)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(op.getClassMetaData());
        classStoreMgr.getPersistenceHandler().locateObject(op);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#locateObjects(org.datanucleus.store.ObjectProvider[])
     */
    public void locateObjects(ObjectProvider[] ops)
    {
        StoreManager classStoreMgr = storeMgr.getStoreManagerForClass(ops[0].getClassMetaData());
        classStoreMgr.getPersistenceHandler().locateObjects(ops);
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