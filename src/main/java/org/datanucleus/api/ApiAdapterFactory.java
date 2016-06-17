/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved. 
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
2008 Andy Jefferson - rewritten to initialise on demand
    ...
**********************************************************************/
package org.datanucleus.api;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;

/**
 * Factory of API adapters.
 */
public class ApiAdapterFactory
{
    /** The adapter instances, mapped by naming string. */
    Map<String, ApiAdapter> adapters = new HashMap();

    /** Singleton instance */
    static ApiAdapterFactory adapterFactory = new ApiAdapterFactory();

    /**
     * Accessor for the ApiAdapterFactory (singleton).
     * @return The manager of type information
     */
    public static ApiAdapterFactory getInstance()
    {
        return adapterFactory;
    }

    /**
     * Protected constructor to prevent outside instantiation
     */
    protected ApiAdapterFactory()
    {
    }

    /**
     * Method to add support for an API via an adapter.
     * @param name name of the API
     * @param apiAdapter instance of adapter
     */
    private void addAdapter(String name, ApiAdapter apiAdapter)
    {
        if (name == null || apiAdapter == null)
        {
            return;
        }

        adapters.put(name, apiAdapter);
    }

    /**
     * Accessor for an adapter, given the api name.
     * If the API adapter doesn't yet exist will try to initialise it from the plugin information
     * defined under extension-point "org.datanucleus.api_adapter".
     * @param name the adapter name
     * @param pluginMgr Plugin Manager
     * @return The ApiAdapter
     * @throws NucleusUserException when requested API not found in CLASSPATH
     */
    public ApiAdapter getApiAdapter(String name, PluginManager pluginMgr)
    {
        ApiAdapter api = adapters.get(name);
        if (api == null)
        {
            try
            {
                api = (ApiAdapter) pluginMgr.createExecutableExtension("org.datanucleus.api_adapter", "name", name, "class-name", null, null);
                if (api == null)
                {
                    String msg = Localiser.msg("022001", name);
                    NucleusLogger.PERSISTENCE.error(msg);
                    throw new NucleusUserException(msg);
                }
                adapterFactory.addAdapter(name, api);
            }
            catch (Error err) // NoClassDefFoundError for some dependent class?
            {
                String className = pluginMgr.getAttributeValueForExtension("org.datanucleus.api_adapter", "name", name, "class-name");
                String msg = Localiser.msg("022000", className, err.getMessage());
                NucleusLogger.PERSISTENCE.error(msg, err);
                throw new NucleusUserException(msg);
            }
            catch (InvocationTargetException e)
            {
                String className = pluginMgr.getAttributeValueForExtension("org.datanucleus.api_adapter", "name", name, "class-name");
                String msg = Localiser.msg("022000", className, e.getTargetException());
                NucleusLogger.PERSISTENCE.error(msg, e);
                throw new NucleusUserException(msg);
            }
            catch (NucleusUserException nue)
            {
                throw nue;
            }
            catch (Exception e)
            {
                String className = pluginMgr.getAttributeValueForExtension("org.datanucleus.api_adapter", "name", name, "class-name");
                String msg = Localiser.msg("022000", className, e.getMessage());
                NucleusLogger.PERSISTENCE.error(msg, e);
                throw new NucleusUserException(msg);
            }
        }
        return api;
    }
}