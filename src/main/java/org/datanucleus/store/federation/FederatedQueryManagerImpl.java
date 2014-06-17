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

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.NucleusContext;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.query.QueryManagerImpl;

/**
 * Manager for queries for federated datastores.
 * Responsible for distributing queries across multiple datastores and federating the results.
 */
public class FederatedQueryManagerImpl extends QueryManagerImpl
{
    public FederatedQueryManagerImpl(NucleusContext nucleusContext, StoreManager storeMgr)
    {
        super(nucleusContext, storeMgr);
    }

    /**
     * Method to find and initialise the query cache, for caching query compilations.
     */
    protected void initialiseQueryCaches()
    {
        // Do nothing since not caching here
    }

    /**
     * Method to generate a new query using the passed query as basis.
     * @param language The query language
     * @param ec ExecutionContext
     * @param query The query filter (String) or a previous Query
     * @return The Query
     */
    public Query newQuery(String language, ExecutionContext ec, Object query)
    {
        if (language == null)
        {
            return null;
        }

        String languageImpl = language;

        // Find the query support for this language and this datastore
        if (query == null)
        {
            // TODO We don't have candidate so don't know the StoreManager to use
            throw new NucleusException("Not yet supported for queries with unknown candidate");
        }

        if (query instanceof String)
        {
            // Single-string query
            String queryString = (String)query;
            String candidateName = null;
            if (languageImpl.equalsIgnoreCase("JDOQL"))
            {
                int candidateStart = queryString.toUpperCase().indexOf(" FROM ") + 6;
                int candidateEnd = queryString.indexOf(" ", candidateStart+1);
                candidateName = queryString.substring(candidateStart, candidateEnd);
            }

            if (candidateName != null)
            {
                ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(null);
                AbstractClassMetaData cmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(candidateName, clr);
                StoreManager classStoreMgr = ((FederatedStoreManager)storeMgr).getStoreManagerForClass(cmd);
                return classStoreMgr.getQueryManager().newQuery(languageImpl, ec, query);
            }
            // TODO Extract the candidate for this query
            // TODO Find StoreManager for the candidate
            throw new NucleusException("Not yet supported for single-string queries");
        }
        else if (query instanceof Query)
        {
            // Based on previous query
            StoreManager storeMgr = ((Query)query).getStoreManager();
            return storeMgr.getQueryManager().newQuery(languageImpl, ec, query);
        }
        else
        {
            if (query instanceof Class)
            {
                // Find StoreManager for the candidate
                Class cls = (Class)query;
                ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(cls.getClassLoader());
                AbstractClassMetaData cmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(cls, clr);
                StoreManager classStoreMgr = ((FederatedStoreManager)storeMgr).getStoreManagerForClass(cmd);
                return classStoreMgr.getQueryManager().newQuery(languageImpl, ec, query);
            }
            throw new NucleusException("Not yet supported for queries taking in object of type " + query.getClass());
        }
    }
}