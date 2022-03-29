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
package org.datanucleus.enhancer;

import java.util.Map;

import org.datanucleus.AbstractNucleusContext;
import org.datanucleus.plugin.PluginManager;

/**
 * NucleusContext to use when enhancing. Just provides basic plugin, type and metadata services.
 */
public class EnhancementNucleusContextImpl extends AbstractNucleusContext
{
    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     */
    public EnhancementNucleusContextImpl(String apiName, Map startupProps)
    {
        this(apiName, startupProps, null);
    }

    /**
     * Constructor for the context.
     * @param apiName Name of the API that we need a context for (JDO, JPA, etc)
     * @param startupProps Any properties that could define behaviour of this context (plugin registry, class loading etc)
     * @param pluginMgr Plugin Manager (or null if wanting it to be created)
     */
    public EnhancementNucleusContextImpl(String apiName, Map startupProps, PluginManager pluginMgr)
    {
        super(apiName, startupProps, pluginMgr);
    }

    @Override
    public void close()
    {
        if (metaDataManager != null)
        {
            metaDataManager.close();
            metaDataManager = null;
        }
        if (classLoaderResolverMap != null)
        {
            classLoaderResolverMap.clear();
            classLoaderResolverMap = null;
        }
        if (typeManager != null)
        {
            typeManager = null;
        }

        super.close();
    }

    @Override
    protected void logConfigurationDetails()
    {
    }
}