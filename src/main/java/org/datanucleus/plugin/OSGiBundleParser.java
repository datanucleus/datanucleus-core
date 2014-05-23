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
2011 Alexey Sushko - adapted PluginParser code to allow for non-Eclipse OSGi environments
    ...
 **********************************************************************/
package org.datanucleus.plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.plugin.Bundle;
import org.datanucleus.plugin.Extension;
import org.datanucleus.plugin.ExtensionPoint;
import org.datanucleus.plugin.PluginParser.Parser;
import org.datanucleus.plugin.PluginRegistry;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class OSGiBundleParser
{
    public static Bundle parseManifest(org.osgi.framework.Bundle osgiBundle)
    {
        @SuppressWarnings("unchecked")
        Dictionary<String, String> headers = osgiBundle.getHeaders();
        Bundle bundle = null;
        try
        {
            String symbolicName = getBundleSymbolicName(headers, null);
            String bundleVersion = getBundleVersion(headers, null);
            String bundleName = getBundleName(headers, null);
            String bundleVendor = getBundleVendor(headers, null);
            bundle = new Bundle(symbolicName, bundleName, bundleVendor, bundleVersion, null);
            bundle.setRequireBundle(getRequireBundle(headers));
        }
        catch (NucleusException ne)
        {
            NucleusLogger.GENERAL.warn("Plugin at bundle " + osgiBundle.getSymbolicName() + 
                " (" + osgiBundle.getBundleId() + ") failed to parse so is being ignored", ne);
            return null;
        }
        return bundle;
    }

    /**
     * Accessor for the Bundle-Name from the manifest.mf file
     * @param mf the manifest
     * @return the Set with BundleDescription
     */
    private static List<Bundle.BundleDescription> getRequireBundle(Dictionary<String, String> headers)
    {
        String str = headers.get("Require-Bundle");
        if (str == null || str.length() < 1)
        {
            return Collections.emptyList();
        }
        Parser p = new Parser(str);
        List<Bundle.BundleDescription> requiredBundle = new ArrayList<Bundle.BundleDescription>();
        String bundleSymbolicName = p.parseSymbolicName();
        while (bundleSymbolicName != null)
        {
            Bundle.BundleDescription bd = new Bundle.BundleDescription();
            bd.setBundleSymbolicName(bundleSymbolicName);
            bd.setParameters(p.parseParameters());
            bundleSymbolicName = p.parseSymbolicName();
            requiredBundle.add(bd);
        }
        return requiredBundle;
    }

    /**
     * Method to parse ExtensionPoints from plug-in file
     * @param rootElement the root element of the plugin xml
     * @param plugin the plugin bundle
     * @param clr the ClassLoaderResolver
     * @return a List of extensionPoints, if any
     * @throws NucleusException if an error occurs during parsing
     */
    private static List<ExtensionPoint> parseExtensionPoints(Element rootElement, Bundle plugin, org.osgi.framework.Bundle osgiBundle)
    {
        List<ExtensionPoint> extensionPoints = new ArrayList<ExtensionPoint>();
        try
        {
            NodeList elements = rootElement.getElementsByTagName("extension-point");
            for (int i = 0; i < elements.getLength(); i++)
            {
                Element element = (Element) elements.item(i);
                String id = element.getAttribute("id").trim();
                String name = element.getAttribute("name");
                String schema = element.getAttribute("schema");
                extensionPoints.add(new ExtensionPoint(id, name, osgiBundle.getEntry(schema), plugin));
            }
        }
        catch (NucleusException ex)
        {
            throw ex;
        }
        return extensionPoints;
    }

    /**
     * Method to parse Extensions from plug-in file
     * @param rootElement the root element of the plugin xml
     * @param plugin the plugin bundle
     * @param clr the ClassLoaderResolver
     * @return a List of extensions, if any
     * @throws NucleusException if an error occurs during parsing
     */
    private static List<Extension> parseExtensions(Element rootElement, Bundle plugin, org.osgi.framework.Bundle osgiBundle)
    {
        List<Extension> extensions = new ArrayList<Extension>();
        try
        {
            NodeList elements = rootElement.getElementsByTagName("extension");
            for (int i = 0; i < elements.getLength(); i++)
            {
                Element element = (Element) elements.item(i);
                Extension ex = new Extension(element.getAttribute("point"), plugin);
                NodeList elms = element.getChildNodes();
                extensions.add(ex);
                for (int e = 0; e < elms.getLength(); e++)
                {
                    if (elms.item(e) instanceof Element)
                    {
                        ex.addConfigurationElement(PluginParser.parseConfigurationElement(ex, (Element) elms.item(e), null));
                    }
                }
            }
        }
        catch (NucleusException ex)
        {
            throw ex;
        }
        return extensions;
    }

    private static String getHeaderValue(Dictionary<String, String> headers, String key, String defaultValue)
    {
        if (headers == null)
        {
            return defaultValue;
        }
        String name = headers.get(key);
        if (name == null)
        {
            return defaultValue;
        }
        return name;
    }

    /**
     * Accessor for the Bundle-SymbolicName from the manifest.mf file
     * @param mf the manifest
     * @param defaultValue a default value, in case no symbolic name found in manifest
     * @return the bundle symbolic name
     */
    private static String getBundleSymbolicName(Dictionary<String, String> headers, String defaultValue)
    {
        String name = getHeaderValue(headers, "Bundle-SymbolicName", defaultValue);
        StringTokenizer token = new StringTokenizer(name, ";");
        return token.nextToken().trim();
    }

    /**
     * Accessor for the Bundle-Name from the manifest.mf file
     * @param mf the manifest
     * @param defaultValue a default value, in case no name found in manifest
     * @return the bundle name
     */
    private static String getBundleName(Dictionary<String, String> headers, String defaultValue)
    {
        return getHeaderValue(headers, "Bundle-Name", defaultValue);
    }

    /**
     * Accessor for the Bundle-Vendor from the manifest.mf file
     * @param mf the manifest
     * @param defaultValue a default value, in case no vendor found in manifest
     * @return the bundle vendor
     */
    private static String getBundleVendor(Dictionary<String, String> headers, String defaultValue)
    {
        return getHeaderValue(headers, "Bundle-Vendor", defaultValue);
    }

    /**
     * Accessor for the Bundle-Version from the manifest.mf file
     * @param mf the manifest
     * @param defaultValue a default value, in case no version found in manifest
     * @return the bundle version
     */
    private static String getBundleVersion(Dictionary<String, String> headers, String defaultValue)
    {
        return getHeaderValue(headers, "Bundle-Version", defaultValue);
    }

    /**
     * Method to parse Extensions in plug-in file.
     * @param db DocumentBuilder to use for parsing
     * @param mgr the PluginManager
     * @param fileUrl URL of the plugin.xml file
     * @param plugin The Bundle
     * @param osgiBundle The OSGi Bundle
     * @return array of 2 elements. first element is a List of extensionPoints, and 2nd element is a List of
     * Extension
     * @throws NucleusException if an error occurs during parsing
     */
    public static List[] parsePluginElements(DocumentBuilder db, PluginRegistry mgr, URL fileUrl, Bundle plugin,
            org.osgi.framework.Bundle osgiBundle)
    {
        List<ExtensionPoint> extensionPoints = Collections.emptyList();
        List<Extension> extensions = Collections.emptyList();
        InputStream is = null;
        try
        {
            is = fileUrl.openStream();
            Element rootElement = db.parse(new InputSource(new InputStreamReader(is))).getDocumentElement();

            if (NucleusLogger.GENERAL.isDebugEnabled())
            {
                NucleusLogger.GENERAL.debug(Localiser.msg("024003", fileUrl.toString()));
            }
            extensionPoints = parseExtensionPoints(rootElement, plugin, osgiBundle);

            if (NucleusLogger.GENERAL.isDebugEnabled())
            {
                NucleusLogger.GENERAL.debug(Localiser.msg("024004", fileUrl.toString()));
            }
            extensions = parseExtensions(rootElement, plugin, osgiBundle);
        }
        catch (NucleusException ex)
        {
            throw ex;
        }
        catch (Exception e)
        {
            NucleusLogger.GENERAL.error(Localiser.msg("024000", fileUrl.getFile()));
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (Exception e)
                {

                }
            }
        }

        return new List[]{extensionPoints, extensions};
    }

    static DocumentBuilderFactory dbFactory = null;

    /**
     * Convenience method to create a document builder for parsing.
     * @return The document builder
     * @throws NucleusException if an error occurs creating the instance
     */
    public static DocumentBuilder getDocumentBuilder()
    {
        try
        {
            if (dbFactory == null)
            {
                dbFactory = DocumentBuilderFactory.newInstance();
            }
            return dbFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e1)
        {
            throw new NucleusException(Localiser.msg("024016", e1.getMessage()));
        }
    }
}