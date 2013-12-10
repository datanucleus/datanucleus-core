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
    ...
 **********************************************************************/
package org.datanucleus.plugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

/**
 * Loader and registry of Extensions and Extension Points. The implementation of this interface must have a public
 * constructor taking the ClassLoaderResolver interface as argument. 
 * The plugin registry metadata/bundle resolution is ruled by OSGi specification. 
 * The following sections of the OSGi 3.0 specification must be fully supported: 3.5.2, 3.5.3, 3.2.4, 3.2.5. 
 * The section 3.6.3 is optional. All other OSGi parts not mentioned above are not likely to be supported.
 */
public interface PluginRegistry
{
    /**
     * Acessor for the ExtensionPoint
     * @param id the unique id of the extension point
     * @return null if the ExtensionPoint is not registered
     */
    ExtensionPoint getExtensionPoint(String id);

    /**
     * Acessor for the currently registed ExtensionPoints
     * @return array of ExtensionPoints
     */
    ExtensionPoint[] getExtensionPoints();

    /**
     * Look for Bundles/Plugins and register them. Register also ExtensionPoints and Extensions declared in /plugin.xml
     * files
     */
    void registerExtensionPoints();

    /**
     * Look for Bundles/Plugins and register them. Register also ExtensionPoints and Extensions declared in /plugin.xml
     * files
     */
    void registerExtensions();

    /**
     * Loads a class (do not initialize) from an attribute of {@link ConfigurationElement}
     * @param confElm the configuration element
     * @param name the attribute name
     * @return the Class
     */
    Object createExecutableExtension(ConfigurationElement confElm, String name, Class[] argsClass, Object[] args)
        throws ClassNotFoundException,
        SecurityException,
        NoSuchMethodException,
        IllegalArgumentException,
        InstantiationException,
        IllegalAccessException,
        InvocationTargetException;

    /**
     * Loads a class (do not initialize)
     * @param pluginId the plugin id
     * @param className the class name
     * @return the Class
     * @throws ClassNotFoundException
     */
    Class loadClass(String pluginId, String className) throws ClassNotFoundException;

    /**
     * Converts a URL that uses a user-defined protocol into a URL that uses the file protocol.
     * @param url the url to be converted
     * @return the converted URL
     * @throws IOException
     */
    URL resolveURLAsFileURL(URL url) throws IOException;
    
    /**
     * Resolve constraints declared in bundle manifest.mf files. 
     * This must be invoked after registering all bundles.
     * Should log errors if bundles are not resolvable, or raise runtime exceptions.
     */
    void resolveConstraints();
    
    /**
     * Accessor for all registered bundles
     * @return the bundles
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    Bundle[] getBundles();
}