/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.exceptions.DatastoreReadOnlyException;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of a persistence handler, to be extended by datastores own variant.
 */
public abstract class AbstractPersistenceHandler implements StorePersistenceHandler
{
    protected StoreManager storeMgr;

    public AbstractPersistenceHandler(StoreManager storeMgr)
    {
        this.storeMgr = storeMgr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#batchStart(org.datanucleus.ExecutionContext, org.datanucleus.store.PersistenceBatchType)
     */
    public void batchStart(ExecutionContext ec, PersistenceBatchType batchType)
    {
        // Override in subclasses if supporting batching using this mechanism
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#batchEnd(org.datanucleus.ExecutionContext, org.datanucleus.store.PersistenceBatchType)
     */
    public void batchEnd(ExecutionContext ec, PersistenceBatchType type)
    {
        // Override in subclasses if supporting batching using this mechanism
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#insertObjects(org.datanucleus.store.ObjectProvider[])
     */
    public void insertObjects(ObjectProvider... ops)
    {
        if (ops.length == 1)
        {
            insertObject(ops[0]);
            return;
        }
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
        if (ops.length == 1)
        {
            deleteObject(ops[0]);
            return;
        }

        for (int i=0;i<ops.length;i++)
        {
            deleteObject(ops[i]);
        }
    }

    /**
     * Simple implementation of location of multiple objects, relaying the locate check for each object
     * to <pre>locateObject</pre>.
     * Should be overridden by the datastore implementation if it is possible to do bulk locates.
     * @param ops ObjectProviders for the objects to locate
     * @throws NucleusObjectNotFoundException if an object doesn't exist
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    public void locateObjects(ObjectProvider[] ops)
    {
        if (ops.length == 1)
        {
            locateObject(ops[0]);
            return;
        }

        for (int i=0;i<ops.length;i++)
        {
            locateObject(ops[i]);
        }
    }

    /**
     * Simple implementation of find of multiple objects, relaying the find for each object
     * to <pre>findObject</pre>.
     * Should be overridden by the datastore implementation if it is possible to do bulk retrieval.
     * @param ec execution context
     * @param ids identities of the object(s) to retrieve
     * @return The persistable objects with these identities (in the same order as <pre>ids</pre>)
     * @throws NucleusObjectNotFoundException if an object doesn't exist
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    public Object[] findObjects(ExecutionContext ec, Object[] ids)
    {
        Object[] objects = new Object[ids.length];
        for (int i=0;i<ids.length;i++)
        {
            objects[i] = findObject(ec, ids[i]);
        }
        return objects;
    }

    /**
     * Convenience method to assert when this StoreManager is read-only and the specified object
     * is attempting to be updated.
     * @param op ObjectProvider for the object
     */
    public void assertReadOnlyForUpdateOfObject(ObjectProvider op)
    {
        if (op.getExecutionContext().getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY))
        {
            if (op.getExecutionContext().getStringProperty(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION).equalsIgnoreCase("EXCEPTION"))
            {
                throw new DatastoreReadOnlyException(Localiser.msg("032004",
                    op.getObjectAsPrintable()), op.getExecutionContext().getClassLoaderResolver());
            }
            else
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("032005", op.getObjectAsPrintable()));
                }
                return;
            }
        }
        else
        {
            AbstractClassMetaData cmd = op.getClassMetaData();
            if (cmd.hasExtension("read-only"))
            {
                String value = cmd.getValueForExtension("read-only");
                if (!StringUtils.isWhitespace(value))
                {
                    boolean readonly = Boolean.valueOf(value).booleanValue();
                    if (readonly)
                    {
                        if (op.getExecutionContext().getStringProperty(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION).equalsIgnoreCase("EXCEPTION"))
                        {
                            throw new DatastoreReadOnlyException(Localiser.msg("032006",
                                op.getObjectAsPrintable()), op.getExecutionContext().getClassLoaderResolver());
                        }
                        else
                        {
                            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                            {
                                NucleusLogger.PERSISTENCE.debug(Localiser.msg("032007", op.getObjectAsPrintable()));
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
}