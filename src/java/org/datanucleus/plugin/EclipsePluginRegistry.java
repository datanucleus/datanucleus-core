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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.VersionRange;

/**
 * Manages the registry of Extensions and Extension Points.
 */
public class EclipsePluginRegistry implements PluginRegistry
{
    /**
     * Constructor
     * @param clr the ClassLoaderResolver
     */
    public EclipsePluginRegistry(ClassLoaderResolver clr)
    {
        //check if org.datanucleus.store_manager is a registered extension point
        if (RegistryFactory.getRegistry() == null || 
            RegistryFactory.getRegistry().getExtensionPoint("org.datanucleus.store_manager") == null)
        {
            throw new RuntimeException("This plug-in registry class can only be used if datanucleus-core is managed by Eclipse");
        }
    }

    /**
     * Acessor for the ExtensionPoint
     * @param id the unique id of the extension point
     * @return null if the ExtensionPoint is not registered
     */
    public ExtensionPoint getExtensionPoint(String id)
    {
        IExtensionPoint eclipseExPoint = RegistryFactory.getRegistry().getExtensionPoint(id);
        Bundle plugin = new Bundle(eclipseExPoint.getContributor().getName(), "", "", "", null);

        org.osgi.framework.Bundle bundle = Platform.getBundle(eclipseExPoint.getContributor().getName());
        try
        {
            ExtensionPoint exPoint = new ExtensionPoint(eclipseExPoint.getSimpleIdentifier(), eclipseExPoint.getLabel(),
                bundle.getResource(eclipseExPoint.getSchemaReference()),plugin);
            for (int e=0; e<eclipseExPoint.getExtensions().length; e++)
            {
                Bundle pluginEx = new Bundle(eclipseExPoint.getExtensions()[e].getContributor().getName(), "", "", "", null);
                Extension ex = new Extension(exPoint, pluginEx);
                configurationElement(ex,eclipseExPoint.getExtensions()[e].getConfigurationElements(),null);
                exPoint.addExtension(ex);
            }
            return exPoint;
        }
        catch (InvalidRegistryObjectException e)
        {
            //LOG
        }
        return null;
    }

    /**
     * process configuration elements
     * @param ex the Extension 
     * @param elms the ConfigurationElements to process
     * @param parent the parent of this ConfigurationElement. null if none 
     */
    private void configurationElement(Extension ex, IConfigurationElement[] elms, ConfigurationElement parent)
    {
        for (int c=0; c<elms.length; c++)
        {
            IConfigurationElement iconfElm = elms[c];
            ConfigurationElement confElm = new ConfigurationElement(ex,iconfElm.getName(),null);
            for( int a=0; a<iconfElm.getAttributeNames().length; a++)
            {
                confElm.putAttribute(iconfElm.getAttributeNames()[a],
                    iconfElm.getAttribute(iconfElm.getAttributeNames()[a]));
            }
            confElm.setText(iconfElm.getValue());
            if( parent == null )
            {
                ex.addConfigurationElement(confElm);
            }
            else
            {
                parent.addConfigurationElement(confElm);
            }
            configurationElement(ex,iconfElm.getChildren(),confElm);
        }
    }
    
    /**
     * Acessor for the currently registed ExtensionPoints
     * @return array of ExtensionPoints
     */
    public ExtensionPoint[] getExtensionPoints()
    {
        IExtensionPoint[] eclipseExPoint = RegistryFactory.getRegistry().getExtensionPoints();
        List elms = new ArrayList();
        for (int i=0; i<eclipseExPoint.length; i++)
        {
            Bundle plugin = new Bundle(eclipseExPoint[i].getContributor().getName(), "", "", "", null);

            try
            {
            	org.osgi.framework.Bundle bundle = Platform.getBundle(eclipseExPoint[i].getContributor().getName());
                ExtensionPoint exPoint = new ExtensionPoint(eclipseExPoint[i].getSimpleIdentifier(), 
                    eclipseExPoint[i].getLabel(), bundle.getResource(eclipseExPoint[i].getSchemaReference()), plugin);
                for (int e=0; e<eclipseExPoint[i].getExtensions().length; e++)
                {
                    Extension ex = new Extension(exPoint, plugin);
                    configurationElement(ex,eclipseExPoint[i].getExtensions()[e].getConfigurationElements(), null);
                    exPoint.addExtension(ex);
                }
                elms.add(exPoint);
            }
            catch (InvalidRegistryObjectException e)
            {
                //LOG
            }
        }
        return (ExtensionPoint[]) elms.toArray(new ExtensionPoint[elms.size()]);
    }

    /**
     * Register Extension Points
     */
    public void registerExtensionPoints()
    {
        //ignore. done by Eclipse
    }

    /**
     * Register ExtensionPoints and Extensions declared in plugin files
     */
    public void registerExtensions()
    {
        //ignore. done by Eclipse
    }

    /**
     * Converts a URL that uses a user-defined protocol into a URL that uses the file protocol.
     * @param url the url to be converted
     * @return the converted URL
     * @throws IOException
     */
    public URL resolveURLAsFileURL(URL url) throws IOException
    {
        return FileLocator.toFileURL(url);
    }
    
    /**
     * Loads a class (do not initialize) from an attribute of {@link ConfigurationElement}
     * @param confElm the configuration element
     * @param name the attribute name
     * @return the Class
     * @throws NoSuchMethodException 
     * @throws SecurityException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws IllegalArgumentException 
     */
    public Object createExecutableExtension(ConfigurationElement confElm, String name, Class[] argsClass, Object[] args) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Class cls = Platform.getBundle(confElm.getExtension().getPlugin().getSymbolicName()).loadClass(confElm.getAttribute(name));
        Constructor constructor = cls.getConstructor(argsClass);
        return constructor.newInstance(args);
    }
    
    /**
     * Loads a class (do not initialize)
     * @param pluginId the plugin id
     * @param className the class name
     * @return the Class
     * @throws ClassNotFoundException 
     */
    public Class loadClass(String pluginId, String className) throws ClassNotFoundException
    {
        return Platform.getBundle(pluginId).loadClass(className);
    }

    /**
     * Resolve constraints declared in bundle manifest.mf files. 
     * This must be invoked after registering all bundles. 
     * Should log errors if bundles are not resolvable, or raise runtime exceptions.
     */    
    public void resolveConstraints()
    {
        //ignored. Eclipse will handle this
    }

    /**
     * Converts {@link BundleSpecification} into {@link BundleDescription}
     * @param bs the {@link BundleSpecification}
     * @return the {@link BundleDescription}
     */
    private Bundle.BundleDescription getBundleDescription(BundleSpecification bs)
    {
        Bundle.BundleDescription bd = new Bundle.BundleDescription();
        bd.setBundleSymbolicName(bs.getBundle().getSymbolicName());
        Map parameters = new HashMap();
        if (bs.isOptional())
        {
            parameters.put("resolution", "optional");
        }
        if (VersionRange.emptyRange != bs.getVersionRange())
        {
            parameters.put("bundle-version", bs.getVersionRange().toString());
        }
        bd.setParameters(parameters);
        return bd;
    }
    /**
     * Accessor for all registered bundles
     * @return the bundles
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    public Bundle[] getBundles()
    {
        int size = Platform.getPlatformAdmin().getState().getBundles().length;
        Bundle[] bundles = new Bundle[size];
        for (int i=0; i<size; i++)
        {
            BundleDescription bd = Platform.getPlatformAdmin().getState().getBundles()[i];
            bundles[i] = new Bundle(bd.getSymbolicName(),bd.getSymbolicName(), bd.getSupplier().getName(), bd.getVersion().toString(), null);
            BundleSpecification[] bs = bd.getRequiredBundles();
            List requiredBundles = new ArrayList();
            for (int j=0; j<bs.length; j++)
            {
                requiredBundles.add(getBundleDescription(bs[j]));
            }
            bundles[i].setRequireBundle(requiredBundles);
        }
        return bundles;
    }
}