/**********************************************************************
Copyright (c) 2017 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.query.cache;

import org.datanucleus.Configuration;
import org.datanucleus.NucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.util.NucleusLogger;

/**
 * Root implementation of a query results cache, providing provessing of common persistence properties.
 */
public abstract class AbstractQueryResultsCache implements QueryResultsCache
{
    private static final long serialVersionUID = 5815194081248492071L;

    protected NucleusContext nucleusCtx;

    /** Maximum size of cache (if supported by the plugin). */
    protected int maxSize = -1;

    /** Whether to clear out all objects at close(). */
    protected boolean clearAtClose = true;

    /** Timeout for cache object expiration (milliseconds). */
    protected long expiryMillis = -1;

    /** Name of the cache to use. */
    protected String cacheName;

    public AbstractQueryResultsCache(NucleusContext nucleusCtx)
    {
        this.nucleusCtx = nucleusCtx;
        Configuration conf = nucleusCtx.getConfiguration();
        maxSize = conf.getIntProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_MAXSIZE);
        clearAtClose = conf.getBooleanProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_CLEARATCLOSE, true);

        if (conf.hasProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_EXPIRY_MILLIS))
        {
            expiryMillis = conf.getIntProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_EXPIRY_MILLIS);
        }

        cacheName = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_QUERYRESULTS_NAME);
        if (cacheName == null)
        {
            NucleusLogger.CACHE.warn("No property " + PropertyNames.PROPERTY_CACHE_QUERYRESULTS_NAME + " specified so using name of 'DataNucleus-Query'");
            cacheName = "DataNucleus-Query";
        }
    }
}
