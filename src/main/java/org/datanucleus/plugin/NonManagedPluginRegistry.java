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
2007 Andr� F�genschuh - Support for protocol "jar:http:"
2008 Alexi Polenur - Support for Oracle AS "jndi:", "code-source:" protocols
2008 Georg Jansen - contrib for JBoss5 vfsXXX protocols
2011 Renato Garcia - contrib for JBoss6 vfs protocol
2011 Daniel Baldes - contrib for "jar:https://"
    ...
 **********************************************************************/
package org.datanucleus.plugin;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.plugin.Bundle.BundleDescription;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Manages the registry of Extensions and Extension Points outside any OSGI container.
 * This implementation cannot handle multiple versions of the same plugin, so it either raises an exception or logs the issue as a warning. 
 * This is different to that mandated by the OSGi specification 3.0 $3.5.2.
 */
public class NonManagedPluginRegistry implements PluginRegistry
{
    /** DataNucleus package to define whether to check for deps, etc. */
    private static final String DATANUCLEUS_PKG = "org.datanucleus";

    /** directories that are searched for plugin files */
    private static final String PLUGIN_DIR = "/";

    /** filters all accepted manifest file names */
    private static final FilenameFilter MANIFEST_FILE_FILTER = new FilenameFilter()
    {
        public boolean accept(File dir, String name)
        {
            // accept a directory named "meta-inf"
            if (name.equalsIgnoreCase("meta-inf"))
            {
                return true;
            }
            // or accept /meta-inf/manifest.mf
            if (!dir.getName().equalsIgnoreCase("meta-inf"))
            {
                return false;
            }
            return name.equalsIgnoreCase("manifest.mf");
        }
    };

    /**
     * Character that is used in URLs of jars to separate the file name from the path of a resource inside
     * the jar.<br/> example: jar:file:foo.jar!/META-INF/manifest.mf
     */
    private static final char JAR_SEPARATOR = '!';

    /** ClassLoaderResolver corresponding to the persistence context. */
    private final ClassLoaderResolver clr;

    /** extension points keyed by Unique Id (plugin.id +"."+ id) */
    Map<String, ExtensionPoint> extensionPointsByUniqueId = new HashMap<>();

    /** registered bundles files keyed by bundle symbolic name */
    Map<String, Bundle> registeredPluginByPluginId = new HashMap<>();

    /** extension points */
    ExtensionPoint[] extensionPoints;

    /** Type of check on bundles (EXCEPTION, LOG, NONE). */
    private String bundleCheckType = "EXCEPTION";

    /** Whether to load up any user (third-party) bundles. */
    private boolean allowUserBundles = false;

    /**
     * Constructor.
     * @param clr the ClassLoaderResolver
     * @param bundleCheckType Type of check on bundles (EXCEPTION, LOG, NONE)
     * @param allowUserBundles Whether to only load DataNucleus bundles (org.datanucleus).
     */
    public NonManagedPluginRegistry(ClassLoaderResolver clr, String bundleCheckType, boolean allowUserBundles)
    {
        this.clr = clr;
        extensionPoints = new ExtensionPoint[0];

        this.bundleCheckType = bundleCheckType != null ? bundleCheckType.toUpperCase() : "EXCEPTION";
        this.allowUserBundles = allowUserBundles;
    }

    @Override
    public ExtensionPoint getExtensionPoint(String id)
    {
        return extensionPointsByUniqueId.get(id);
    }

    @Override
    public ExtensionPoint[] getExtensionPoints()
    {
        return extensionPoints;
    }

    @Override
    public void registerExtensionPoints()
    {
        registerExtensions();
    }

    @Override
    public void registerExtensions()
    {
        if (extensionPoints.length > 0)
        {
            return;
        }

        List<Extension> registeringExtensions = new ArrayList<>();

        // parse the plugin files
        DocumentBuilder docBuilder = PluginParser.getDocumentBuilder();
        try
        {
            // Search and retrieve the URL for the "/plugin.xml" files located in the classpath.
            Enumeration<URL> paths = clr.getResources(PLUGIN_DIR + "plugin.xml", ClassConstants.NUCLEUS_CONTEXT_LOADER);
            while (paths.hasMoreElements())
            {
                URL pluginURL = paths.nextElement();
                URL manifest = getManifestURL(pluginURL);
                if (manifest == null)
                {
                    // No MANIFEST.MF for this plugin.xml so ignore it
                    continue;
                }

                Bundle bundle = registerBundle(manifest);
                if (bundle == null)
                {
                    // No MANIFEST.MF for this plugin.xml so ignore it
                    continue;
                }

                List[] elements = PluginParser.parsePluginElements(docBuilder, this, pluginURL, bundle, clr);
                registerExtensionPointsForPluginInternal(elements[0], false);
                registeringExtensions.addAll(elements[1]);
            }
        }
        catch (IOException e)
        {
            throw new NucleusException("Error loading resource", e).setFatal();
        }

        extensionPoints = extensionPointsByUniqueId.values().toArray(new ExtensionPoint[extensionPointsByUniqueId.values().size()]);

        // Register the extensions now that we have the extension-points all loaded
        for (Extension extension : registeringExtensions)
        {
            ExtensionPoint exPoint = getExtensionPoint(extension.getExtensionPointId());
            if (exPoint == null)
            {
                if (extension.getPlugin() != null && extension.getPlugin().getSymbolicName() != null &&
                    extension.getPlugin().getSymbolicName().startsWith(DATANUCLEUS_PKG))
                {
                    NucleusLogger.GENERAL.warn(Localiser.msg("024002", extension.getExtensionPointId(), 
                        extension.getPlugin().getSymbolicName(), extension.getPlugin().getManifestLocation().toString()));
                }
            }
            else
            {
                extension.setExtensionPoint(exPoint);
                exPoint.addExtension(extension);
            }
        }

        if (allowUserBundles)
        {
            ExtensionSorter sorter = new ExtensionSorter();
            for (int i=0;i<extensionPoints.length;i++)
            {
                ExtensionPoint pt = extensionPoints[i];
                pt.sortExtensions(sorter);
            }
        }
    }

    /**
     * Sorter for extensions that puts DataNucleus extensions first, then any vendor extension.
     */
    protected static class ExtensionSorter implements Comparator<Extension>, Serializable
    {
        private static final long serialVersionUID = 2606866392881023620L;

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
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
    protected void registerExtensionPointsForPluginInternal(List<ExtensionPoint> extPoints, boolean updateExtensionPointsArray)
    {
        // Register extension-points
        for (ExtensionPoint exPoint : extPoints)
        {
            extensionPointsByUniqueId.put(exPoint.getUniqueId(), exPoint);
        }
        if (updateExtensionPointsArray)
        {
            extensionPoints = extensionPointsByUniqueId.values().toArray(new ExtensionPoint[extensionPointsByUniqueId.values().size()]);
        }
    }

    /**
     * Register the plugin bundle.
     * @param manifest the url to the "meta-inf/manifest.mf" file or a jar file
     * @return the Plugin
     */
    protected Bundle registerBundle(URL manifest)
    {
        if (manifest == null)
        {
            throw new IllegalArgumentException(Localiser.msg("024007"));
        }

        InputStream is = null;

        try
        {
            Manifest mf = null;
            if (manifest.getProtocol().equals("jar") || manifest.getProtocol().equals("zip") || manifest.getProtocol().equals("wsjar"))
            {
                if (manifest.getPath().startsWith("http://") || manifest.getPath().startsWith("https://"))
                {
                    // protocol formats:
                    // jar:http:<path>!<manifest-file>, zip:http:<path>!<manifest-file>
                    // e.g jar:http://<host>[:port]/[app-path]/datanucleus-java5.jar!/plugin.xml
                    JarURLConnection jarConnection = (JarURLConnection) manifest.openConnection();
                    URL url = jarConnection.getJarFileURL();
                    mf = jarConnection.getManifest();
                    if (mf == null)
                    {
                        return null;
                    }
                    return registerBundle(mf, url);
                }

                int begin = 4;
                if (manifest.getProtocol().equals("wsjar"))
                {
                    begin = 6;
                }

                // protocol formats: 
                //     jar:<path>!<manifest-file>, zip:<path>!<manifest-file>
                //     jar:file:<path>!<manifest-file>, zip:file:<path>!<manifest-file>
                String path = StringUtils.getDecodedStringFromURLString(manifest.toExternalForm());
                int index = path.indexOf(JAR_SEPARATOR);
                String jarPath = path.substring(begin, index);
                if (jarPath.startsWith("file:"))
                {
                    // remove "file:" from path, so we can use in File constructor
                    jarPath = jarPath.substring(5);
                }

                if (path.substring(index + 1).contains("!"))
                {
                    // Embedded jar within app jar, such as "jar:file:<app_jar_path>!<path_inside_app_jar>!/plugin.xml"
                    File file = new File(jarPath);
                    URL rarUrl = file.toURI().toURL();

                    String embeddedJar = path.substring(index+1, path.indexOf(JAR_SEPARATOR,index+1));
                    if(embeddedJar.startsWith("/")) 
                    {
                        embeddedJar = embeddedJar.substring(1);
                    }

                    JarFile rarFile = new JarFile(file);
                    JarInputStream jis = new JarInputStream(rarFile.getInputStream(rarFile.getEntry(embeddedJar)));
                    try
                    {
                        mf = jis.getManifest();
                        if (mf == null)
                        {
                            return null;
                        }
                    }
                    finally
                    {
                        jis.close();
                        rarFile.close();
                    }
                    return registerBundle(mf, rarUrl);
                }

                File jarFile = new File(jarPath);
                JarFile jar = new JarFile(jarFile);
                try
                {
                    mf = jar.getManifest();
                    if (mf == null)
                    {
                        return null;
                    }
                    return registerBundle(mf, jarFile.toURI().toURL());
                }
                finally
                {
                    jar.close();
                }
            }
            else if (manifest.getProtocol().equals("rar") || manifest.getProtocol().equals("war"))
            {
                // protocol formats:
                //     rar:<rar-path>!<jar-path>!<manifest-file>, war:<war-path>!<jar-path>!<manifest-file>
                String path = StringUtils.getDecodedStringFromURLString(manifest.toExternalForm());
                int index = path.indexOf(JAR_SEPARATOR);
                String rarPath = path.substring(4, index);
                File file = new File(rarPath);
                URL rarUrl = file.toURI().toURL();
                
                String jarPath = path.substring(index+1, path.indexOf(JAR_SEPARATOR,index+1));
                JarFile rarFile = new JarFile(file);
                try
                {
                    JarInputStream jis = new JarInputStream(rarFile.getInputStream(rarFile.getEntry(jarPath)));
                    try
                    {
                        mf = jis.getManifest();
                        if (mf == null)
                        {
                            return null;
                        }
                    }
                    finally
                    {
                        jis.close();
                    }
                    return registerBundle(mf, rarUrl);
                }
                finally
                {
                    rarFile.close();
                }
            }
            else
            {
                is = manifest.openStream();
                mf = new Manifest(is);
                return registerBundle(mf,manifest);
            }
        }
        catch (IOException e)
        {
            throw new NucleusException(Localiser.msg("024008", manifest), e).setFatal();
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    // ignored
                }
            }
        }
    }

    /**
     * Register the plugin bundle.
     * @param mf the Manifest
     * @param manifest the url to the "meta-inf/manifest.mf" file or a jar file
     * @return the Plugin
     */
    protected Bundle registerBundle(Manifest mf, URL manifest)
    {
        Bundle bundle = PluginParser.parseManifest(mf, manifest);
        if (bundle == null || bundle.getSymbolicName() == null)
        {
            // Didn't parse correctly or the bundle is basically junk, so ignore it
            return null;
        }

        if (!allowUserBundles && !bundle.getSymbolicName().startsWith(DATANUCLEUS_PKG))
        {
            NucleusLogger.GENERAL.debug("Ignoring bundle " + bundle.getSymbolicName() + " since not DataNucleus, and only loading DataNucleus bundles");
            return null;
        }

        if (registeredPluginByPluginId.get(bundle.getSymbolicName()) == null)
        {
            if (NucleusLogger.GENERAL.isDebugEnabled())
            {
                NucleusLogger.GENERAL.debug("Registering bundle " + bundle.getSymbolicName() + " version " + bundle.getVersion() + " at URL " + bundle.getManifestLocation() + ".");
            }
            registeredPluginByPluginId.put(bundle.getSymbolicName(), bundle);
        }
        else
        {
            Bundle previousBundle = registeredPluginByPluginId.get(bundle.getSymbolicName());
            if (bundle.getSymbolicName().startsWith(DATANUCLEUS_PKG) &&
                !bundle.getManifestLocation().toExternalForm().equals(previousBundle.getManifestLocation().toExternalForm()))
            {
                String msg = Localiser.msg("024009", bundle.getSymbolicName(), bundle.getManifestLocation(), previousBundle.getManifestLocation());
                if (bundleCheckType.equals("EXCEPTION"))
                {
                    throw new NucleusException(msg);
                }
                else if (bundleCheckType.equals("LOG"))
                {
                    NucleusLogger.GENERAL.warn(msg);
                }
                else
                {
                    // Nothing
                }
            }
        }
        return bundle;
    }

    /**
     * Get the URL to the "manifest.mf" file relative to the plugin URL ($pluginurl/meta-inf/manifest.mf)
     * @param pluginURL the url to the "plugin.xml" file
     * @return a URL to the "manifest.mf" file or a URL for a jar file
     */
    private URL getManifestURL(URL pluginURL)
    {
        if (pluginURL == null)
        {
            return null;
        }
        if (pluginURL.toString().startsWith("jar") || pluginURL.toString().startsWith("zip") || 
            pluginURL.toString().startsWith("rar") || pluginURL.toString().startsWith("war") ||
            pluginURL.toString().startsWith("wsjar"))
        {
            // URL for file containing the manifest
            return pluginURL;
        }
        else if (pluginURL.toString().startsWith("vfs") || pluginURL.toString().startsWith("bundle"))
        {
            // JBoss (5+) proprietary protocols input:
            // vfsfile:C:/appserver/jboss-5.0.0.Beta4/server/default/deploy/datanucleus-jca-1.0.0.rar/datanucleus-core-1.0-SNAPSHOT.jar/plugin.xml
            // output:
            // vfsfile:C:/appserver/jboss-5.0.0.Beta4/server/default/deploy/datanucleus-jca-1.0.0.rar/datanucleus-core-1.0-SNAPSHOT.jar!/plugin.xml
            String urlStr = pluginURL.toString().replace("plugin.xml", "META-INF/MANIFEST.MF");
            try
            {
                return new URL(urlStr);
            }
            catch (MalformedURLException e)
            {
                NucleusLogger.GENERAL.warn(Localiser.msg("024010", urlStr), e);
                return null;
            }
        }
        else if (pluginURL.toString().startsWith("jndi"))
        {
            // "Oracle AS" uses JNDI protocol. For example
            // input:  jndi:/opt/oracle/product/10.1.3.0.3_portal/j2ee/OC4J_Portal/applications/presto/presto/WEB-INF/lib/jpox-rdbms-1.2-SNAPSHOT.jar/plugin.xml
            // output: jar:file:/opt/oracle/product/10.1.3.0.3_portal/j2ee/OC4J_Portal/applications/presto/presto/WEB-INF/lib/jpox-rdbms-1.2-SNAPSHOT.jar!/plugin.xml 
            String urlStr = pluginURL.toString().substring(5);
            urlStr = urlStr.replaceAll("\\.jar/", ".jar!/");
            urlStr = "jar:file:" + urlStr;
            try
            {
                // URL for file containing the manifest
                return new URL(urlStr);
            }
            catch (MalformedURLException e)
            {
                NucleusLogger.GENERAL.warn(Localiser.msg("024010", urlStr), e);
                return null;
            }
        }
        else if (pluginURL.toString().startsWith("code-source"))
        {
            // "Oracle AS" also uses code-source protocol. For example
            // input:  code-source:/opt/oc4j/j2ee/home/applications/presto/presto/WEB-INF/lib/jpox-rdmbs-1.2-SNAPSHOT.jar!/plugin.xml
            // output: jar:file:/opt/oc4j/j2ee/home/applications/presto/presto/WEB-INF/lib/jpox-rdmbs-1.2-SNAPSHOT.jar!/plugin.xml
            String urlStr = pluginURL.toString().substring(12); //strip "code-source:"
            urlStr = "jar:file:" + urlStr;
            try
            {
                // URL for file containing the manifest
                return new URL(urlStr);
            }
            catch (MalformedURLException e)
            {
                NucleusLogger.GENERAL.warn(Localiser.msg("024010", urlStr), e);
                return null;
            }
        }

        try
        {
            File file = new File(new URI(pluginURL.toString()).getPath());
            File[] dirs = new File(file.getParent()).listFiles(MANIFEST_FILE_FILTER);
            if (dirs != null && dirs.length > 0)
            {
                File[] files = dirs[0].listFiles(MANIFEST_FILE_FILTER);
                if (files != null && files.length > 0)
                {
                    try
                    {
                        return files[0].toURI().toURL();
                    }
                    catch (MalformedURLException e)
                    {
                        NucleusLogger.GENERAL.warn(Localiser.msg("024011", pluginURL), e);
                        return null;
                    }
                }
            }
        }
        catch (URISyntaxException use)
        {
            NucleusLogger.GENERAL.warn(Localiser.msg("024011", pluginURL), use);
            return null;
        }

        NucleusLogger.GENERAL.warn(Localiser.msg("024012", pluginURL));
        return null;
    }

    @Override
    public Object createExecutableExtension(ConfigurationElement confElm, String name, Class[] argTypes, Object[] args)
        throws ClassNotFoundException,
        SecurityException
    {
        return ClassUtils.newInstance(clr.classForName(confElm.getAttribute(name),org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER), argTypes, args);
    }

    @Override
    public Class loadClass(String pluginId, String className) throws ClassNotFoundException
    {
        return clr.classForName(className, org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);
    }

    @Override
    public URL resolveURLAsFileURL(URL url) throws IOException
    {
        return url;
    }

    @Override
    public void resolveConstraints()
    {
        for (Bundle bundle : registeredPluginByPluginId.values())
        {
            List<BundleDescription> set = bundle.getRequireBundle();
            for (BundleDescription bd : set)
            {
                String symbolicName = bd.getBundleSymbolicName();

                Bundle requiredBundle = registeredPluginByPluginId.get(symbolicName);
                if (requiredBundle == null) // TODO Add option of only logging problems in DataNucleus deps
                {
                    String resolution = bd.getParameter("resolution");
                    if (resolution != null && resolution.toLowerCase().equals("optional"))
                    {
                        NucleusLogger.GENERAL.debug(Localiser.msg("024013", bundle.getSymbolicName(), symbolicName));
                    }
                    else
                    {
                        NucleusLogger.GENERAL.error(Localiser.msg("024014", bundle.getSymbolicName(), symbolicName));
                    }
                }

                if (bd.getParameter("bundle-version") != null)
                {
                    if (requiredBundle != null && 
                        !isVersionInInterval(requiredBundle.getVersion(), bd.getParameter("bundle-version")))
                    {
                        NucleusLogger.GENERAL.error(Localiser.msg("024015", bundle.getSymbolicName(), symbolicName, bd.getParameter("bundle-version"), bundle.getVersion()));
                    }
                }
            }
        }
    }
    
    /**
     * Check if the version is in interval
     * @param version The version
     * @param interval The interval definition
     * @return Whether the version is in this interval
     */
    private boolean isVersionInInterval(String version, String interval)
    {
        //versionRange has only floor
        Bundle.BundleVersionRange versionRange = PluginParser.parseVersionRange(version);
        Bundle.BundleVersionRange intervalRange = PluginParser.parseVersionRange(interval);
        int compare_floor = versionRange.floor.compareTo(intervalRange.floor);
        boolean result = true;
        if (intervalRange.floor_inclusive)
        {
            result = compare_floor >= 0;
        }
        else
        {
            result = compare_floor > 0;
        }
        if (intervalRange.ceiling != null)
        {
            int compare_ceiling = versionRange.floor.compareTo(intervalRange.ceiling);
            if (intervalRange.ceiling_inclusive)
            {
                result = compare_ceiling <= 0;
            }
            else
            {
                result = compare_ceiling < 0;
            }
        }
        return result;
    }

    @Override
    public Bundle[] getBundles()
    {
        return registeredPluginByPluginId.values().toArray(new Bundle[registeredPluginByPluginId.values().size()]);
    }

    /**
     * Register extension and extension points for the specified plugin.
     * <B>Note that this is only present for testing purposes, hence also why it is public.</B>
     * @param pluginURL the URL to the plugin
     * @param bundle the bundle
     */
    public void registerExtensionsForPlugin(URL pluginURL, Bundle bundle)
    {
        DocumentBuilder docBuilder = PluginParser.getDocumentBuilder();
        List[] elements = PluginParser.parsePluginElements(docBuilder, this, pluginURL, bundle, clr);
        registerExtensionPointsForPluginInternal(elements[0], true);

        // Register extensions
        Iterator<Extension> pluginExtensionIter = elements[1].iterator();
        while (pluginExtensionIter.hasNext())
        {
            Extension extension = pluginExtensionIter.next();
            ExtensionPoint exPoint = extensionPointsByUniqueId.get(extension.getExtensionPointId());
            if (exPoint == null)
            {
                NucleusLogger.GENERAL.warn(Localiser.msg("024002", extension.getExtensionPointId(), 
                    extension.getPlugin().getSymbolicName(), extension.getPlugin().getManifestLocation().toString()));
            }
            else
            {
                extension.setExtensionPoint(exPoint);
                exPoint.addExtension(extension);
            }
        }
    }
}