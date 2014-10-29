/**********************************************************************
Copyright (c) 2011 Alexey Sushko and others. All rights reserved.
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
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.plugin.Bundle;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.Extension;
import org.datanucleus.plugin.ExtensionPoint;
import org.datanucleus.plugin.PluginRegistry;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class OSGiPluginRegistry implements PluginRegistry 
{
    /** DataNucleus package to define whether to check for deps, etc. */
    private static final String DATANUCLEUS_PKG = "org.datanucleus";

    /** extension points keyed by Unique Id (plugin.id +"."+ id) */
    Map<String, ExtensionPoint> extensionPointsByUniqueId = new HashMap<String, ExtensionPoint>();

    /** registered bundles files keyed by bundle symbolic name */
    Map<String, Bundle> registeredPluginByPluginId = new HashMap<String, Bundle>();

    /** extension points */
    ExtensionPoint[] extensionPoints;

    /**
     * Constructor
     * @param clr the ClassLoaderResolver
     */
    public OSGiPluginRegistry(ClassLoaderResolver clr)
    {
        extensionPoints = new ExtensionPoint[0];
    }

    /**
     * Accessor for the ExtensionPoint with the specified id.
     * @param id the unique id of the extension point
     * @return null if the ExtensionPoint is not registered
     */
    public ExtensionPoint getExtensionPoint(String id)
    {
        return extensionPointsByUniqueId.get(id);
    }

    public ExtensionPoint[] getExtensionPoints()
    {
       return extensionPoints;
    }

    /**
     * Look for Bundles/Plugins and register them. 
     * Register also ExtensionPoints and Extensions declared in "/plugin.xml" files
     */
    public void registerExtensionPoints()
    {
        registerExtensions();
    }

    public void registerExtensions()
    {
        if (extensionPoints.length > 0)
        {
            return;
        }
        List registeringExtensions = new ArrayList();

        org.osgi.framework.Bundle bdl = FrameworkUtil.getBundle(this.getClass());
        BundleContext ctx = bdl.getBundleContext();
        if (ctx == null)
        {
            // TODO Any way we can handle this better? e.g force OSGi to start it?
            NucleusLogger.GENERAL.error("Bundle " + bdl.getSymbolicName() + " is in state " + bdl.getState() + " and has NULL context, so cannot register it properly!");
        }

        // parse the plugin files
        DocumentBuilder docBuilder = OSGiBundleParser.getDocumentBuilder();
        org.osgi.framework.Bundle[] osgiBundles = ctx.getBundles();
        for (org.osgi.framework.Bundle osgiBundle : osgiBundles)
        {
            URL pluginURL = osgiBundle.getEntry("plugin.xml");

            if (pluginURL == null)
            {
                continue;
            }

            Bundle bundle = registerBundle(osgiBundle);
            if (bundle == null)
            {
                // No MANIFEST.MF for this plugin.xml so ignore it
                continue;
            }

            List[] elements = OSGiBundleParser.parsePluginElements(docBuilder, this, pluginURL, bundle, osgiBundle);
            registerExtensionPointsForPluginInternal(elements[0], false);
            registeringExtensions.addAll(elements[1]);
        }
        extensionPoints = extensionPointsByUniqueId.values().toArray(new ExtensionPoint[extensionPointsByUniqueId.values().size()]);

        // Register the extensions now that we have the extension-points all loaded
        for (int i = 0; i < registeringExtensions.size(); i++)
        {
            Extension extension = (Extension) registeringExtensions.get(i);
            ExtensionPoint exPoint = getExtensionPoint(extension.getExtensionPointId());
            if (exPoint == null)
            {
                if (extension.getPlugin() != null && extension.getPlugin().getSymbolicName() != null && 
                    extension.getPlugin().getSymbolicName().startsWith(DATANUCLEUS_PKG))
                {
                    NucleusLogger.GENERAL.warn(Localiser.msg("024002", extension.getExtensionPointId(), 
                        extension.getPlugin().getSymbolicName(), extension.getPlugin().getManifestLocation()));
                }
            }
            else
            {
                extension.setExtensionPoint(exPoint);
                exPoint.addExtension(extension);
            }
        }
    }

    /**
     * Sorter for extensions that puts DataNucleus extensions first, then any vendor extension.
     */
    protected static class ExtensionSorter implements Comparator<Extension>, Serializable
    {
        private static final long serialVersionUID = -264321551131696022L;

        public int compare(Extension o1, Extension o2)
        {
            String name1 = o1.getPlugin().getSymbolicName();
            String name2 = o2.getPlugin().getSymbolicName();
            if (name1.startsWith("org.datanucleus") && !name2.startsWith("org.datanucleus"))
            {
                return -1;
            }
            else if (!name1.startsWith("org.datanucleus") && name2.startsWith("org.datanucleus"))
            {
                return 1;
            }
            else
            {
                return name1.compareTo(name2);
            }
        }
    }

    /**
     * Register extension-points for the specified plugin.
     * @param extPoints ExtensionPoints for this plugin
     * @param updateExtensionPointsArray Whether to update "extensionPoints" array
     */
    protected void registerExtensionPointsForPluginInternal(List extPoints, boolean updateExtensionPointsArray)
    {
        // Register extension-points
        Iterator<ExtensionPoint> pluginExtPointIter = extPoints.iterator();
        while (pluginExtPointIter.hasNext())
        {
            ExtensionPoint exPoint = pluginExtPointIter.next();
            extensionPointsByUniqueId.put(exPoint.getUniqueId(), exPoint);
        }
        if (updateExtensionPointsArray)
        {
            extensionPoints = extensionPointsByUniqueId.values().toArray(new ExtensionPoint[extensionPointsByUniqueId.values().size()]);
        }
    }

    /**
     * Register the plugin bundle.
     * @param osgiBundle the OSGi bundle
     * @return the Plugin
     */
    private Bundle registerBundle(org.osgi.framework.Bundle osgiBundle)
    {
        Bundle bundle = OSGiBundleParser.parseManifest(osgiBundle);
        if (bundle == null)
        {
            // Didn't parse correctly, so ignore it
            return null;
        }

        if (registeredPluginByPluginId.get(bundle.getSymbolicName()) == null)
        {
            if (NucleusLogger.GENERAL.isDebugEnabled())
            {
                NucleusLogger.GENERAL.debug("Registering bundle " + bundle.getSymbolicName() + 
                    " version " + bundle.getVersion() + " at URL " + bundle.getManifestLocation() + ".");
            }
            registeredPluginByPluginId.put(bundle.getSymbolicName(), bundle);
        }
        return bundle;
    }

    /**
     * Loads a class (do not initialize) from an attribute of {@link ConfigurationElement}
     * @param confElm the configuration element
     * @param name the attribute name
     * @return the Class
     * @throws NoSuchMethodException if an error occurs
     * @throws SecurityException if an error occurs
     * @throws InvocationTargetException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws InstantiationException if an error occurs
     * @throws IllegalArgumentException if an error occurs
     */
    public Object createExecutableExtension(ConfigurationElement confElm, String name, Class[] argsClass, Object[] args)
        throws ClassNotFoundException,
        SecurityException,
        NoSuchMethodException,
        IllegalArgumentException,
        InstantiationException,
        IllegalAccessException,
        InvocationTargetException
    {
        String symbolicName = confElm.getExtension().getPlugin().getSymbolicName();
        String attribute = confElm.getAttribute(name);
        org.osgi.framework.Bundle osgiBundle = getOsgiBundle(symbolicName);
        Class cls = osgiBundle.loadClass(attribute);
        Constructor constructor = cls.getConstructor(argsClass);

        try
        {
            return constructor.newInstance(args);
        }
        catch (InstantiationException e1)
        {
            NucleusLogger.GENERAL.error(e1.getMessage(), e1);
            throw e1;
        }
        catch (IllegalAccessException e2)
        {
            NucleusLogger.GENERAL.error(e2.getMessage(), e2);
            throw e2;
        }
        catch (IllegalArgumentException e3)
        {
            NucleusLogger.GENERAL.error(e3.getMessage(), e3);
            throw e3;
        }
        catch (InvocationTargetException e4)
        {
            NucleusLogger.GENERAL.error(e4.getMessage(), e4);
            throw e4;
        }
    }

    /**
     * Loads a class (do not initialize)
     * @param pluginId the plugin id
     * @param className the class name
     * @return the Class
     * @throws ClassNotFoundException if there is a problem in loading
     */
    public Class loadClass(String pluginId, String className) throws ClassNotFoundException
    {
        return getOsgiBundle(pluginId).loadClass(className);
    }

    public URL resolveURLAsFileURL(URL url) throws IOException
    {
        return null;
    }

    /**
     * Resolve constraints declared in bundle manifest.mf files. This must be invoked after registering all
     * bundles. Should log errors if bundles are not resolvable, or raise runtime exceptions.
     */
    public void resolveConstraints()
    {
        // ignored. OSGi Framework will handle this
    }

    /**
     * Accessor for all registered bundles
     * @return the bundles
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    public Bundle[] getBundles()
    {
        return registeredPluginByPluginId.values().toArray(new Bundle[registeredPluginByPluginId.values().size()]);
    }

    private org.osgi.framework.Bundle getOsgiBundle(String symbolicName)
    {
        BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        org.osgi.framework.Bundle[] osgiBundles = ctx.getBundles();

        for (org.osgi.framework.Bundle osgiBundle : osgiBundles)
        {
            if (symbolicName.equals(osgiBundle.getSymbolicName()))
                return osgiBundle;
        }
        return null;
    }
}
