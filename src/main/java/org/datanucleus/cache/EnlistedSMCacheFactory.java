package org.datanucleus.cache;

import org.datanucleus.state.DNStateManager;

import java.util.Map;

/**
 * Factory interface for constructing custom enlistedSMCache in ExecutionContext.
 */
public interface EnlistedSMCacheFactory
{
    /**
     * Return new instance of enlistedSMCache used when constructing ExecutionContexts.
     * @return new instance of enlistedSMCache
     */
    Map<Object, DNStateManager> createEnlistedSMCache();
}
