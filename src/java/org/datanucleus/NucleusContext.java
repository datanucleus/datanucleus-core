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
package org.datanucleus;

import org.datanucleus.api.ApiAdapter;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.store.types.TypeManager;

/**
 * Representation of the context being run within DataNucleus. Provides basic services that can be used by all DataNucleus contexts.
 * <ul>
 * <li>properties defining configuration for persistence.</li>
 * <li>manages the plugins involved in this DataNucleus context</li>
 * <li>provides access to the API in use</li>
 * <li>manages metadata for persistable classes</li>
 * <li>provides access to the TypeManager, defining the behaviour for java types.</li>
 * </ul>
 */
public interface NucleusContext
{
    /**
     * Method to initialise the context for use.
     * If any services are considered essential for operation then they will be enabled here, otherwise left for lazy initialisation.
     */
    void initialise();

    /**
     * Clear out resources for the supported services.
     */
    void close();

    ApiAdapter getApiAdapter();

    /**
     * Accessor for the name of the API (JDO, JPA, etc).
     * @return the api
     */
    String getApiName();

    Configuration getConfiguration();

    PluginManager getPluginManager();

    MetaDataManager getMetaDataManager();

    TypeManager getTypeManager();

    /**
     * Accessor for a ClassLoaderResolver to use in resolving classes.
     * Caches the resolver for the specified primary loader, and hands it out if present.
     * @param primaryLoader Loader to use as the primary loader (or null)
     * @return The ClassLoader resolver
     */
    ClassLoaderResolver getClassLoaderResolver(ClassLoader primaryLoader);

    boolean supportsORMMetaData();
}