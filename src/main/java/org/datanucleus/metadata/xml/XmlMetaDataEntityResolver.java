/******************************************************************
Copyright (c) 2008 Erik Bengtson and others. All rights reserved.
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
*****************************************************************/
package org.datanucleus.metadata.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation of an entity resolver for XML MetaData files.
 * Supports a series of internally supported public or system identities.
 * Note that this applies to ALL types of XML MetaData (persistence.xml, JDO package.jdo, JDO package.orm, JDO package.jdoquery, JPA/Jakarta orm.xml).
 * We could, potentially, separate these different types of MetaData XML file, and hence split up the entities based on the handler, but not considered a priority currently.
 */
public class XmlMetaDataEntityResolver implements EntityResolver
{
    /** Map of public identity entities supported. The key will be the identity, and the value is the local input to use. */
    protected Map<String, String> publicIdEntities = new HashMap<>();

    /** Map of system identity entities supported. The key will be the identity, and the value is the local input to use. */
    protected Map<String, String> systemIdEntities = new HashMap<>();

    final PluginManager pluginMgr;

    public XmlMetaDataEntityResolver(PluginManager pluginMgr)
    {
        this.pluginMgr = pluginMgr;

        ConfigurationElement[] elems = pluginMgr.getConfigurationElementsForExtension("org.datanucleus.metadata_entityresolver", null, null);
        for (int i=0; i<elems.length; i++)
        {
            if (elems[i].getAttribute("type") != null)
            {
                if (elems[i].getAttribute("type").equals("PUBLIC"))
                {
                    publicIdEntities.put(elems[i].getAttribute("identity"), elems[i].getAttribute("url"));
                }
                else if (elems[i].getAttribute("type").equals("SYSTEM"))
                {
                    systemIdEntities.put(elems[i].getAttribute("identity"), elems[i].getAttribute("url"));
                }
            }
        }
    }

    /**
     * The list of schemas registered in the plugin "metadata_entityresolver".
     * @return the Sources pointing to the .xsd files
     */
    public Source[] getRegisteredSchemas()
    {
        ConfigurationElement[] elems = pluginMgr.getConfigurationElementsForExtension("org.datanucleus.metadata_entityresolver", null, null);
        Set<Source> sources = new HashSet<>();
        for (int i=0; i<elems.length; i++)
        {
            if (elems[i].getAttribute("type") == null)
            {
                InputStream in = XmlMetaDataParser.class.getResourceAsStream(elems[i].getAttribute("url"));
                if (in == null)
                {
                    NucleusLogger.METADATA.warn("local resource \"" + elems[i].getAttribute("url") + "\" does not exist!!!");
                }
                sources.add(new StreamSource(in));
            }
        }
        return sources.toArray(new Source[sources.size()]);
    }

    /**
     * Method to resolve XML entities.
     * Allows for the internally supported public and system identity entities.
     * @param publicId The public id.
     * @param systemId The system id.
     * @return Input Source for the URI.
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,java.lang.String)
     */
    public InputSource resolveEntity(String publicId, String systemId)
    throws SAXException
    {
        try
        {
            if (publicId != null)
            {
                // Use publicId if possible
                String internalEntity = publicIdEntities.get(publicId);
                if (internalEntity != null)
                {
                    return getLocalInputSource(publicId, systemId, internalEntity);
                }
            }

            if (systemId != null)
            {
                // Use systemId if possible
                String internalEntity = systemIdEntities.get(systemId);
                if (internalEntity != null)
                {
                    return getLocalInputSource(publicId, systemId, internalEntity);
                }
                else if (systemId.startsWith("file://"))
                {
                    // This is used to load from a file on the system, not on the CLASSPATH
                    String localPath = systemId.substring(7);
                    File file = new File(localPath);
                    if (file.exists())
                    {
                        if (NucleusLogger.METADATA.isDebugEnabled())
                        {
                            NucleusLogger.METADATA.debug(Localiser.msg("028001", publicId, systemId));
                        }
                        FileInputStream in = new FileInputStream(file);
                        return new InputSource(in);
                    }
                    return null;
                }
                else if (systemId.startsWith("file:"))
                {
                    // Try to get the local file using CLASSPATH
                    return getLocalInputSource(publicId, systemId, systemId.substring(5));
                }
                else if (systemId.startsWith("http:"))
                {
                    // Try to get the URL and open its stream
                    try
                    {
                        if (NucleusLogger.METADATA.isDebugEnabled())
                        {
                            NucleusLogger.METADATA.debug(Localiser.msg("028001", publicId, systemId));
                        }
                        URL url = new URL(systemId);
                        InputStream url_stream = url.openStream();
                        return new InputSource(url_stream);
                    }
                    catch (Exception e)
                    {
                        NucleusLogger.METADATA.error(e);
                    }
                }
            }

            NucleusLogger.METADATA.error(Localiser.msg("028002", publicId, systemId));
            return null;
        }
        catch (Exception e)
        {
            NucleusLogger.METADATA.error(Localiser.msg("028003", publicId, systemId), e);
            throw new SAXException(e.getMessage(), e);
        }
    }

    /**
     * Accessor for the input source for a path.
     * @param publicId Public identity
     * @param systemId System identity
     * @param localPath The local path
     * @return The input source
     * @throws FileNotFoundException if the local file is not accessible
     */
    protected InputSource getLocalInputSource(String publicId, String systemId, String localPath)
    throws FileNotFoundException
    {
        if (NucleusLogger.METADATA.isDebugEnabled())
        {
            NucleusLogger.METADATA.debug(Localiser.msg("028000", publicId, systemId, localPath));
        }

        InputStream in = XmlMetaDataEntityResolver.class.getResourceAsStream(localPath);
        if (in == null)
        {
            NucleusLogger.METADATA.fatal("local resource \"" + localPath + "\" does not exist!!!");
            throw new FileNotFoundException("Unable to load resource: " + localPath);
        }
        return new InputSource(new InputStreamReader(in));
    }
}