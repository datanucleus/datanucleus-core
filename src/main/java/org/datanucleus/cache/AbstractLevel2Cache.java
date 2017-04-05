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
package org.datanucleus.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.NucleusContext;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.util.NucleusLogger;

/**
 * Abstract starting point for a third-party L2 cache plugin.
 * Override the pin/unpin methods if supportable by your plugin.
 */
public abstract class AbstractLevel2Cache implements Level2Cache
{
    private static final long serialVersionUID = 7737532122953947585L;

    protected NucleusContext nucleusCtx;

    /** Maximum size of cache (if supported by the plugin). */
    protected int maxSize = -1;

    /** Whether to clear out all objects at close(). */
    protected boolean clearAtClose = true;

    /** Timeout for cache object expiration (milliseconds). */
    protected long timeout = -1;

    /** Name of the cache to use. */
    protected String cacheName;

    public AbstractLevel2Cache(NucleusContext nucleusCtx)
    {
        this.nucleusCtx = nucleusCtx;
        Configuration conf = nucleusCtx.getConfiguration();
        maxSize = conf.getIntProperty(PropertyNames.PROPERTY_CACHE_L2_MAXSIZE);
        clearAtClose = conf.getBooleanProperty(PropertyNames.PROPERTY_CACHE_L2_CLEARATCLOSE, true);

        if (conf.hasProperty(PropertyNames.PROPERTY_CACHE_L2_TIMEOUT))
        {
            timeout = conf.getIntProperty(PropertyNames.PROPERTY_CACHE_L2_TIMEOUT);
        }

        cacheName = conf.getStringProperty(PropertyNames.PROPERTY_CACHE_L2_NAME);
        if (cacheName == null)
        {
            NucleusLogger.CACHE.warn("No 'datanucleus.cache.level2.cacheName' specified so using name of 'dataNucleus'");
            cacheName = "dataNucleus";
        }
    }

    /*
     * (non-Javadoc)
     * @see org.datanucleus.cache.Level2Cache#getAll(java.util.Collection)
     */
    public Map<Object, CachedPC> getAll(Collection oids)
    {
        if (oids == null)
        {
            return null;
        }

        // Just fallback to doing multiple gets. Overridden in the implementation if supported
        Map<Object, CachedPC> objs = new HashMap<Object, CachedPC>();
        for (Object id : oids)
        {
            CachedPC value = get(id);
            objs.put(id, value);
        }
        return objs;
    }

    /*
     * (non-Javadoc)
     * @see org.datanucleus.cache.Level2Cache#putAll(java.util.Map)
     */
    public void putAll(Map<Object, CachedPC> objs)
    {
        if (objs == null)
        {
            return;
        }

        // Just fallback to doing multiple puts. Overridden in the implementation if supported
        Iterator<Map.Entry<Object, CachedPC>> entryIter = objs.entrySet().iterator();
        while (entryIter.hasNext())
        {
            Map.Entry<Object, CachedPC> entry = entryIter.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Accessor for whether the cache is empty
     * @see org.datanucleus.cache.Level2Cache#isEmpty()
     */
    public boolean isEmpty()
    {
        return getSize() == 0;
    }

    // ============================== Unique Key Support ==============================

    /* (non-Javadoc)
     * @see org.datanucleus.cache.Level2Cache#getUnique(org.datanucleus.cache.CacheUniqueKey)
     */
    @Override
    public CachedPC getUnique(CacheUniqueKey key)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.cache.Level2Cache#putUnique(org.datanucleus.cache.CacheUniqueKey, org.datanucleus.cache.CachedPC)
     */
    @Override
    public CachedPC putUnique(CacheUniqueKey key, CachedPC pc)
    {
        return null;
    }

    /**
     * Method to remove any object cached against the provided unique key.
     * Override this in the implementation if supporting unique key caching.
     * @param key Unique key
     */
    public void removeUnique(CacheUniqueKey key)
    {
        return;
    }

    // ============================== Pinned Object Support ==============================

    public int getNumberOfPinnedObjects()
    {
        // Not supported
        return 0;
    }

    public int getNumberOfUnpinnedObjects()
    {
        // Not supported
        return 0;
    }

    public void pin(Object arg0)
    {
        // Not supported
    }

    public void pinAll(Collection arg0)
    {
        // Not supported
    }

    public void pinAll(Object[] arg0)
    {
        // Not supported
    }

    public void pinAll(Class arg0, boolean arg1)
    {
        // Not supported
    }

    public void unpin(Object arg0)
    {
        // Not supported
    }

    public void unpinAll(Collection arg0)
    {
        // Not supported
    }

    public void unpinAll(Object[] arg0)
    {
        // Not supported
    }

    public void unpinAll(Class arg0, boolean arg1)
    {
        // Not supported
    }
}