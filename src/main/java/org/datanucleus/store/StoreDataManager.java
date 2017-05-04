/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved. 
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager for store information.
 * Keeps a record of which classes are managed by this datastore, and key information about how that class is handled.
 */
public class StoreDataManager
{
    /** Map of all managed store data, keyed by the class/field name. */
    protected Map<String, StoreData> storeDataByClass = new ConcurrentHashMap<String, StoreData>();

    /** the memory image of schema data before running it **/
    protected Map<String, StoreData> savedStoreDataByClass;

    /**
     * Clear the cache
     */
    public void clear()
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("032002"));
        }

        storeDataByClass.clear();
    }

    public void deregisterClass(String className)
    {
        storeDataByClass.remove(className);
    }

    /**
     * Method to register some data with the store.
     * @param data The StoreData to add
     */
    protected void registerStoreData(StoreData data)
    {
        if (data.isFCO())
        {
            // Index any classes by the class name
            if (storeDataByClass.containsKey(data.getName()))
            {
                return;
            }
            storeDataByClass.put(data.getName(), data); // Keyed by class name
        }
        else
        {
            // Index any fields by the name of the field
            if (storeDataByClass.containsKey(data.getName()))
            {
                return;
            }
            storeDataByClass.put(data.getName(), data); // Keyed by AbstractMemberMetaData
        }

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("032001", data));
        }
    }

    /**
     * Convenience accessor for all store data where property 1 has value1 and property 2 has value2.
     * Uses equals() on the values. Doesn't cater for null values.
     * @param key1 Property 1 name
     * @param value1 Property 1 value
     * @param key2 Property 2 name
     * @param value2 Property 2 value
     * @return Store data with the specified property values
     */
    public StoreData[] getStoreDataForProperties(String key1, Object value1, String key2, Object value2)
    {
        Collection<StoreData> results = null;

        Collection storeDatas = storeDataByClass.values();
        Iterator<StoreData> iterator = storeDatas.iterator();
        while (iterator.hasNext())
        {
            StoreData data = iterator.next();
            if (data.getProperties() != null)
            {
                Object prop1Value = data.getProperties().get(key1);
                Object prop2Value = data.getProperties().get(key2);
                if (prop1Value != null && prop1Value.equals(value1) && prop2Value != null && prop2Value.equals(value2))
                {
                    if (results == null)
                    {
                        results = new HashSet();
                    }
                    results.add(data);
                }
            }
        }

		if (results != null)
        {
            return results.toArray(new StoreData[results.size()]);
        }
        return null;
    }

    /**
     * Accessor for whether the specified class is managed currently
     * @param className The name of the class
     * @return Whether it is managed
     */
    public boolean managesClass(String className)
    {
        return storeDataByClass.containsKey(className);
    }
    
    /**
     * Accessor for the StoreData currently managed by this store.
     * @return Collection of the StoreData being managed
     */
    public Collection<StoreData> getManagedStoreData()
    {
        return Collections.unmodifiableCollection(storeDataByClass.values());
    }

    /**
     * Get the StoreData by the given className
     * @param className the fully qualified class name
     * @return the StoreData
     */
    public StoreData get(String className)
    {
        return storeDataByClass.get(className);
    }

    /**
     * Get the StoreData by the given field/property, if it has some specific store data component (join table).
     * @param mmd metadata for the the field/property
     * @return the StoreData
     */
    public StoreData get(AbstractMemberMetaData mmd)
    {
        return storeDataByClass.get(mmd.getFullFieldName());
    }

    /**
     * Accessor to the number of StoreData in cache
     * @return the number of StoreData in cache
     */
    public int size()
    {
        return storeDataByClass.size();
    }

    /**
     * Begin a transaction that changes the StoreData cache
     */
    public void begin()
    {
        savedStoreDataByClass = new ConcurrentHashMap<String, StoreData>(storeDataByClass);
    }
    
    /**
     * Rollback the transaction changes to the StoreData cache
     */
    public void rollback()
    {
        storeDataByClass = savedStoreDataByClass;
        savedStoreDataByClass = null;
    }
    
    /**
     * Commit the transaction changes to the StoreData cache
     */
    public void commit()
    {
        savedStoreDataByClass = null;
    }
}