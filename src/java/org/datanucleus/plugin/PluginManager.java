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
2006 Thomas Marti - Added support for configurable plugin file names
    ...
**********************************************************************/
package org.datanucleus.plugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassLoaderResolverImpl;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.ClassNotResolvedException;

/**
 * Manages the registry of Extensions and Extension Points for the plugin mechanism.
 */
public class PluginManager
{
    private PluginRegistry registry;

    /**
     * Constructor.
     * @param registryClassName Name of the registry
     * @param clr the ClassLoaderResolver
     * @param props Configuration properties for the plugin system
     */
    public PluginManager(String registryClassName, ClassLoaderResolver clr, Properties props)
    {
        String bundleCheckAction = "EXCEPTION";
        if (props.containsKey("bundle-check-action"))
        {
            bundleCheckAction = props.getProperty("bundle-check-action");
        }
        String allowUserBundles = props.getProperty("allow-user-bundles");
        boolean userBundles = (allowUserBundles != null ? Boolean.valueOf(allowUserBundles) : true);

        registry = PluginRegistryFactory.newPluginRegistry(registryClassName, bundleCheckAction, userBundles, clr);

        // Register extension points declared in "/plugin.xml", and then register the extensions of these
        registry.registerExtensionPoints();
        registry.registerExtensions();

        String validateStr = (props.containsKey("validate-plugins") ? props.getProperty("validate-plugins") : "false");
        if (validateStr.equalsIgnoreCase("true"))
        {
            registry.resolveConstraints();
        }
    }

    /**
     * Accessor for the PluginRegistry class name.
     * @return Name of the plugin registry
     */
    public String getRegistryClassName()
    {
        return registry.getClass().getName();
    }

    /**
     * Acessor for the ExtensionPoint
     * @param id the unique id of the extension point
     * @return null if the ExtensionPoint is not registered
     */
    public ExtensionPoint getExtensionPoint(String id)
    {
        return registry.getExtensionPoint(id);
    }

    /**
     * Convenience accessor for getting the (first) ConfigurationElement for an extension (of an extension point).
     * @param extensionPointName The extension point
     * @param discrimAttrName Attribute on the extension to use as discriminator
     * @param discrimAttrValue Value for discriminator attribute
     * @return The configuration element
     */
    public ConfigurationElement getConfigurationElementForExtension(String extensionPointName,
            String discrimAttrName, String discrimAttrValue)
    {
      return getConfigurationElementForExtension(extensionPointName,
          discrimAttrName != null ? new String[] {discrimAttrName} : new String[0],
          discrimAttrValue != null ? new String[] {discrimAttrValue} : new String[0]);
    }

    /**
     * Convenience accessor for getting the ConfigurationElement(s) for an extension (of an extension point).
     * @param extensionPointName The extension point
     * @param discrimAttrName Attribute on the extension to use as discriminator
     * @param discrimAttrValue Value for discriminator attribute
     * @return Configuration elements
     */
    public ConfigurationElement[] getConfigurationElementsForExtension(String extensionPointName,
            String discrimAttrName, String discrimAttrValue)
    {
        List<ConfigurationElement> elems = getConfigurationElementsForExtension(extensionPointName,
            discrimAttrName != null ? new String[] {discrimAttrName} : new String[0],
            discrimAttrValue != null ? new String[] {discrimAttrValue} : new String[0]);
        if (!elems.isEmpty())
        {
            return elems.toArray(new ConfigurationElement[elems.size()]);
        }
        return null;
    }

    /**
     * Convenience accessor for getting the ConfigurationElement for an extension (of an extension point).
     * @param extensionPointName The extension point
     * @param discrimAttrName Attribute on the extension to use as discriminator1
     * @param discrimAttrValue Value for discriminator1 attribute
     * @return Configuration Element
     */
    public ConfigurationElement getConfigurationElementForExtension(String extensionPointName,
            String[] discrimAttrName, String[] discrimAttrValue)
    {
        List matchingConfigElements = getConfigurationElementsForExtension(extensionPointName, discrimAttrName, discrimAttrValue);
        if (!matchingConfigElements.isEmpty())
        {
            return (ConfigurationElement) matchingConfigElements.get(0);
        }
        return null;
    }

    /**
     * Internal accessor for getting getting the ConfigurationElement(s) for an extension (of an extension
     * point), sorted by their priority attribute (if defined).
     * @param extensionPointName The extension point
     * @param discrimAttrName Attributes on the exension to use as discriminator
     * @param discrimAttrValue Values for discriminator attributes
     * @return Configuration elements
     */
    private List getConfigurationElementsForExtension(String extensionPointName, String[] discrimAttrName, String[] discrimAttrValue)
    {
        List matchingConfigElements = new LinkedList();

        ExtensionPoint extensionPoint = getExtensionPoint(extensionPointName);
        if (extensionPoint!=null)
        {
            Extension[] ex = extensionPoint.getExtensions();
            for (int i=0; i<ex.length; i++)
            {
                ConfigurationElement[] confElm = ex[i].getConfigurationElements();
                for (int j=0; j<confElm.length; j++)
                {
                    // Find an extension with this discriminator value
                    boolean equals = true;
                    for (int k=0; k<discrimAttrName.length; k++)
                    {
                        if (discrimAttrValue[k] == null)
                        {
                            if (confElm[j].getAttribute(discrimAttrName[k]) != null)
                            {
                                equals = false;
                                break;
                            }
                        }
                        else
                        {
                            if (confElm[j].getAttribute(discrimAttrName[k]) == null)
                            {
                                equals = false;
                                break;
                            }
                            else if (!confElm[j].getAttribute(discrimAttrName[k]).equalsIgnoreCase(discrimAttrValue[k]))
                            {
                                equals = false;
                                break;
                            }
                        }
                    }
                    if (equals)
                    {
                        matchingConfigElements.add(confElm[j]);
                    }
                }
            }
        }
        Collections.sort(matchingConfigElements, new ConfigurationElementPriorityComparator());
        return matchingConfigElements;
    }

    /**
     * Comparator for comparing ConfigurationElements by their priority attribute.
     * Elements without priority attribute are considered equal, having priority 0.
     * Higher priority means lesser comparison result.
     */
    private static final class ConfigurationElementPriorityComparator implements Comparator<ConfigurationElement>
    {
        public int compare(ConfigurationElement elm1, ConfigurationElement elm2)
        {
            String pri1 = elm1.getAttribute("priority");
            String pri2 = elm2.getAttribute("priority");
            return (pri2 == null ? 0 : Integer.parseInt(pri2)) - (pri1 == null ? 0 : Integer.parseInt(pri1));
        }
    }

    /**
     * Convenience accessor for getting the value of an attribute for an extension (of an extension point).
     * @param extensionPoint The extension point
     * @param discrimAttrName Attribute on the extension to use as discriminator
     * @param discrimAttrValue Value for discriminator attribute
     * @param attributeName Name of the attribute whose value we want
     * @return The value of the attribute
     */
    public String getAttributeValueForExtension(String extensionPoint, String discrimAttrName, String discrimAttrValue, String attributeName)
    {
        ConfigurationElement elem = getConfigurationElementForExtension(extensionPoint, discrimAttrName, discrimAttrValue);
        if (elem != null)
        {
            return elem.getAttribute(attributeName);
        }
        return null;
    }

    /**
     * Convenience accessor for getting the value of an attribute for an extension (of an extension point).
     * @param extensionPoint The extension point
     * @param discrimAttrName Attribute on the extension to use as discriminator
     * @param discrimAttrValue Value for discriminator attribute
     * @param attributeName Name of the attribute whose value we want
     * @return The value(s) of the attribute
     */
    public String[] getAttributeValuesForExtension(String extensionPoint, String discrimAttrName, String discrimAttrValue, String attributeName)
    {
        ConfigurationElement[] elems = getConfigurationElementsForExtension(extensionPoint, discrimAttrName, discrimAttrValue);
        if (elems != null)
        {
            String[] attrValues = new String[elems.length];
            for (int i=0;i<elems.length;i++)
            {
                attrValues[i] = elems[i].getAttribute(attributeName);
            }
            return attrValues;
        }
        return null;
    }

    /**
     * Convenience accessor for getting the value of an attribute for an extension (of an extension point).
     * @param extensionPoint The extension point
     * @param discrimAttrName Attribute on the extension to use as discriminator1
     * @param discrimAttrValue Value for discriminator1 attribute
     * @param attributeName Name of the attribute whose value we want
     * @return The value of the attribute
     */
    public String getAttributeValueForExtension(String extensionPoint, String[] discrimAttrName, String[] discrimAttrValue, String attributeName)
    {
        ConfigurationElement elem = getConfigurationElementForExtension(extensionPoint, 
            discrimAttrName, discrimAttrValue);
        if (elem != null)
        {
            return elem.getAttribute(attributeName);
        }
        return null;
    }
    
    /**
     * Convenience accessor for getting the Class of an attribute for an extension (of an extension point).
     * @param extensionPoint The extension point
     * @param discrimAttrName Attribute on the extension to use as discriminator
     * @param discrimAttrValue Value for discriminator attribute
     * @param attributeName Name of the attribute whose value we want
     * @param argsClass Classes of the arguments
     * @param args The arguments
     * @return The value of the attribute
     * @throws ClassNotFoundException if an error occurs
     * @throws SecurityException if an error occurs
     * @throws NoSuchMethodException if an error occurs
     * @throws IllegalArgumentException if an error occurs
     * @throws InstantiationException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws InvocationTargetException if an error occurs
     */
    public Object createExecutableExtension(String extensionPoint, String discrimAttrName, String discrimAttrValue, String attributeName, Class[] argsClass, Object[] args) 
    throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        ConfigurationElement elem = getConfigurationElementForExtension(extensionPoint, discrimAttrName, discrimAttrValue);
        if (elem != null)
        {
            return registry.createExecutableExtension(elem, attributeName, argsClass, args);
        }
        return null;
    }

    /**
     * Convenience accessor for getting the Class of an attribute for an extension (of an extension point).
     * @param extensionPoint The extension point
     * @param discrimAttrName First attribute on the extension to use as discriminator
     * @param discrimAttrValue Value for first discriminator attribute
     * @param attributeName Name of the attribute whose value we want
     * @param argsClass Classes of the arguments
     * @param args The arguments
     * @return The value of the attribute
     * @throws ClassNotFoundException if an error occurs
     * @throws SecurityException if an error occurs
     * @throws NoSuchMethodException if an error occurs
     * @throws IllegalArgumentException if an error occurs
     * @throws InstantiationException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws InvocationTargetException if an error occurs
     */
    public Object createExecutableExtension(String extensionPoint, String[] discrimAttrName, String[] discrimAttrValue, String attributeName, Class[] argsClass, Object[] args) 
    throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        ConfigurationElement elem = getConfigurationElementForExtension(extensionPoint, 
            discrimAttrName, discrimAttrValue);
        if (elem != null)
        {
            return registry.createExecutableExtension(elem, attributeName, argsClass, args);
        }
        return null;
    }

    /**
     * Loads a class (do not initialize)
     * @param pluginId the plugin id
     * @param className the class name
     * @return the Class
     * @throws ClassNotResolvedException if an error occurs
     */
    public Class loadClass(String pluginId, String className) 
    throws ClassNotResolvedException
    {
        try
        {
            return registry.loadClass(pluginId, className);
        }
        catch(ClassNotFoundException ex)
        {
            throw new ClassNotResolvedException(ex.getMessage(),ex);
        }
    }

    /**
     * Converts a URL that uses a user-defined protocol into a URL that uses the file protocol.
     * @param url the url to be converted
     * @return the converted URL
     * @throws IOException if an error occurs
     */
    public URL resolveURLAsFileURL(URL url) throws IOException
    {
        return registry.resolveURLAsFileURL(url);
    }    

    /**
     * Accessor for the version of a particular bundle (or null if not registered).
     * @param bundleName Name of the bundle
     * @return The version
     */
    public String getVersionForBundle(String bundleName)
    {
        Bundle[] bundles = registry.getBundles();
        if (bundles != null)
        {
            for (int i=0;i<bundles.length;i++)
            {
                Bundle bundle = bundles[i];
                if (bundle.getSymbolicName().equals(bundleName))
                {
                    return bundle.getVersion();
                }
            }
        }

        return null;
    }

    /**
     * Convenience method that will create and return a PluginManager using any passed in properties.
     * Supports the following properties
     * <ul>
     * <li>datanucleus.primaryClassLoader</li>
     * <li>datanucleus.plugin.pluginRegistryClassName</li>
     * <li>datanucleus.plugin.pluginRegistryBundleCheck</li>
     * <li>datanucleus.plugin.allowUserBundles</li>
     * <li>datanucleus.plugin.validatePlugins</li>
     * </ul>
     * @param props Any properties defining the plugin manager capabilities
     * @param loader Any class loader to make use of when loading
     * @return The PluginManager
     */
    public static PluginManager createPluginManager(Map props, ClassLoader loader)
    {
        ClassLoaderResolver clr = (loader != null ? new ClassLoaderResolverImpl(loader) : new ClassLoaderResolverImpl());
        if (props != null)
        {
            clr.registerUserClassLoader((ClassLoader)props.get(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY));
        }

        Properties pluginProps = new Properties();
        String registryClassName = null;
        if (props != null)
        {
            registryClassName = (String)props.get(PropertyNames.PROPERTY_PLUGIN_REGISTRY_CLASSNAME);

            if (props.containsKey(PropertyNames.PROPERTY_PLUGIN_REGISTRYBUNDLECHECK))
            {
                pluginProps.setProperty("bundle-check-action", (String)props.get(PropertyNames.PROPERTY_PLUGIN_REGISTRYBUNDLECHECK));
            }
            if (props.containsKey(PropertyNames.PROPERTY_PLUGIN_ALLOW_USER_BUNDLES))
            {
                pluginProps.setProperty("allow-user-bundles", (String)props.get(PropertyNames.PROPERTY_PLUGIN_ALLOW_USER_BUNDLES));
            }
            if (props.containsKey(PropertyNames.PROPERTY_PLUGIN_VALIDATEPLUGINS))
            {
                pluginProps.setProperty("validate-plugins", (String)props.get(PropertyNames.PROPERTY_PLUGIN_VALIDATEPLUGINS));
            }
        }
        return new PluginManager(registryClassName, clr, pluginProps);
    }
}