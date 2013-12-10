/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.util;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.datanucleus.plugin.PluginManager;
import org.xml.sax.EntityResolver;

/**
 * Factory for Entity Resolvers.
 * This factory holds a cache of resolvers, so we only ever have one of each type.
 */
public class EntityResolverFactory
{
    /**
     * Contains a cache of entity resolvers
     */
    private static Map resolvers = new HashMap();

    private EntityResolverFactory()
    {
        // Private constructor to prevent instantiation
    }

    /**
     * Factory method for EntityResolver instances
     * @param pluginManager the pluginManager
     * @param handlerName the handler name
     * @return The EntityResolver instance for this handler name
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws NoSuchMethodException 
     * @throws ClassNotFoundException 
     * @throws IllegalArgumentException 
     * @throws SecurityException 
     */
    public static EntityResolver getInstance(PluginManager pluginManager, String handlerName) 
    throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, 
        InstantiationException, IllegalAccessException, InvocationTargetException
    {
        EntityResolver resolver = (EntityResolver) resolvers.get(handlerName);
        if (resolver == null)
        {
            resolver = (EntityResolver) pluginManager.createExecutableExtension("org.datanucleus.metadata_handler", "name", 
                handlerName, "entity-resolver", new Class[] {PluginManager.class}, new Object[]{pluginManager});
            resolvers.put(handlerName, resolver);
        }
        return resolver;
    }
}