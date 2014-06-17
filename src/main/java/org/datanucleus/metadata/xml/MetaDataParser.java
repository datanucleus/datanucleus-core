/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
2004 Andy Jefferson - added class and field custom object creation facility
2004 Andy Jefferson - updated resolve entity to use Ralf Ullrich suggestion
2004 Marco Schulze (NightLabs.de) - added safety checks for missing local dtd files
2004 Marco Schulze (NightLabs.de) - added special handling of SAXException
    ...
**********************************************************************/
package org.datanucleus.metadata.xml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.datanucleus.ClassConstants;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.InvalidMetaDataException;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to provide the parsing framework for parsing metadata files.
 * This will support parsing of any metadata files where the resultant object is
 * derived from org.datanucleus.metadata.MetaData, so can be used on JDO files, ORM files,
 * JDOQUERY files, JPA files, or "persistence.xml" files. Can be used for any future
 * metadata files too.
 * <P>
 * Provides 3 different entry points depending on whether the caller has a URL,
 * a file, or an InputStream.
 * </P>
 */
public class MetaDataParser extends DefaultHandler
{
    /** MetaData manager. */
    protected final MetaDataManager mgr;

    /** Plugin Manager. */
    protected final PluginManager pluginMgr;

    /** Whether to validate while parsing. */
    protected final boolean validate;

    /** Whether to support namespaces. */
    protected boolean namespaceAware = true;

    /** SAXParser being used. */
    SAXParser parser = null;

    /**
     * Constructor.
     * @param mgr MetaDataManager
     * @param pluginMgr Manager for plugins
     * @param validate Whether to validate while parsing
     */
    public MetaDataParser(MetaDataManager mgr, PluginManager pluginMgr, boolean validate)
    {
        this.mgr = mgr;
        this.pluginMgr = pluginMgr;
        this.validate = validate;
    }

    public void setNamespaceAware(boolean aware)
    {
        if (namespaceAware != aware)
        {
            parser = null;
        }
        this.namespaceAware = aware;
    }

    /**
     * Method to parse a MetaData file given the URL of the file.
     * @param url Url of the metadata file
     * @param handlerName Name of the handler plugin to use when parsing
     * @return The MetaData for this file
     * @throws NucleusException thrown if error occurred
     */
    public MetaData parseMetaDataURL(URL url, String handlerName)
    {
        if (url == null)
        {
            String msg = Localiser.msg("044031");
            NucleusLogger.METADATA.error(msg);
            throw new NucleusException(msg);
        }

        InputStream in = null;
        try
        {
            in = url.openStream();
        }
        catch (Exception ignore)
        {
        }
        if (in == null)
        {
            try
            {
                in = new FileInputStream(StringUtils.getFileForFilename(url.getFile()));
            }
            catch (Exception ignore)
            {
            }
        }
        if (in == null)
        {
            NucleusLogger.METADATA.error(Localiser.msg("044032", url.toString()));
            throw new NucleusException(Localiser.msg("044032", url.toString()));
        }

        // Parse the file
        return parseMetaDataStream(in, url.toString(), handlerName);
    }

    /**
     * Method to parse a MetaData file given the filename.
     * @param fileName Name of the file
     * @param handlerName Name of the handler plugin to use when parsing
     * @return The MetaData for this file
     * @throws NucleusException if error occurred
     */
    public MetaData parseMetaDataFile(String fileName, String handlerName)
    {
        InputStream in = null;
        try
        {
            in = new URL(fileName).openStream();
        }
        catch (Exception ignore)
        {
            //do nothing
        }
        if (in == null)
        {
            try
            {
                in = new FileInputStream(StringUtils.getFileForFilename(fileName));
            }
            catch (Exception ignore)
            {
                //do nothing
            }
        }
        if (in == null)
        {
            NucleusLogger.METADATA.error(Localiser.msg("044032", fileName));
            throw new NucleusException(Localiser.msg("044032", fileName));
        }

        // Parse the file
        return parseMetaDataStream(in, fileName, handlerName);
    }

    /**
     * Method to parse a MetaData file given an InputStream.
     * Closes the input stream when finished.
     * @param in input stream
     * @param filename Name of the file (if applicable)
     * @param handlerName Name of the handler plugin to use when parsing
     * @return The MetaData for this file
     * @throws NucleusException thrown if error occurred
     */
    public synchronized MetaData parseMetaDataStream(InputStream in, String filename, String handlerName)
    {
        if (in == null)
        {
            throw new NullPointerException("input stream is null");
        }

        if (NucleusLogger.METADATA.isDebugEnabled())
        {
            NucleusLogger.METADATA.debug(Localiser.msg("044030", filename, handlerName, validate ? "true" : "false"));
        }
        try
        {
            if (parser == null)
            {
                // Create a SAXParser (use JDK parser for now)
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setValidating(validate);
                factory.setNamespaceAware(namespaceAware);
                if (validate)
                {
                    try
                    {
                        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                            getRegisteredSchemas(pluginMgr));
                        if (schema != null)
                        {
                            try
                            {
                                factory.setSchema(schema);
                            }
                            catch (UnsupportedOperationException e)
                            {
                                // may happen in conflict of JDK 1.5 and older xml libraries (xerces)
                                NucleusLogger.METADATA.info(e.getMessage());
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        // Cannot validate since no SchemaFactory could be loaded?
                        NucleusLogger.METADATA.info(e.getMessage());
                    }

                    // Xerces (if in CLASSPATH) needs this for validation (of XSD)
                    try
                    {
                        factory.setFeature("http://apache.org/xml/features/validation/schema", true);
                    }
                    catch (Exception e)
                    {
                        NucleusLogger.METADATA.info(e.getMessage());
                    }
                }
                parser = factory.newSAXParser();
            }

            // Generate the default handler to process the metadata
            DefaultHandler handler = null;
            EntityResolver entityResolver = null;
            try
            {
                entityResolver = EntityResolverFactory.getInstance(pluginMgr, handlerName);
                if (entityResolver != null)
                {
                    parser.getXMLReader().setEntityResolver(entityResolver);
                }
                Class[] argTypes = new Class[] {ClassConstants.METADATA_MANAGER, String.class, EntityResolver.class};
                Object[] argValues = new Object[] {mgr, filename, entityResolver};

                handler = (DefaultHandler)pluginMgr.createExecutableExtension("org.datanucleus.metadata_handler", 
                    "name", handlerName, "class-name", argTypes, argValues);
                if (handler == null)
                {
                    // Plugin of this name not found
                    throw new NucleusUserException(Localiser.msg("044028", handlerName)).setFatal();
                }
            }
            catch (Exception e)
            {
                String msg = Localiser.msg("044029", handlerName, e.getMessage());
                throw new NucleusException(msg, e);
            }

            // Set whether to validate
            ((AbstractMetaDataHandler)handler).setValidate(validate);

            // Parse the metadata
            parser.parse(in, handler);

            // Return the FileMetaData that has been parsed
            return ((AbstractMetaDataHandler)handler).getMetaData();
        }
        catch (NucleusException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Throwable cause = e;
            if (e instanceof SAXException)
            {
                SAXException se = (SAXException)e;
                cause = se.getException();
            }
            cause = e.getCause() == null ? cause : e.getCause();

            NucleusLogger.METADATA.error(Localiser.msg("044040", filename, cause));
            if (cause instanceof InvalidMetaDataException)
            {
                throw (InvalidMetaDataException)cause;
            }

            throw new NucleusException(Localiser.msg("044033", e), cause);
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (Exception ignore)
            {
                // Do nothing
            }
        }
    }

    /**
     * The list of schemas registered in the plugin metadata_entityresolver
     * @param pm the PluginManager
     * @return the Sources pointing to the .xsd files
     */
    private Source[] getRegisteredSchemas(PluginManager pm)
    {
        ConfigurationElement[] elems = pm.getConfigurationElementsForExtension(
            "org.datanucleus.metadata_entityresolver", null, null);
        Set<Source> sources = new HashSet<Source>();
        for (int i=0; i<elems.length; i++)
        {
            if (elems[i].getAttribute("type") == null)
            {
                InputStream in = MetaDataParser.class.getResourceAsStream(elems[i].getAttribute("url"));
                if (in == null)
                {
                    NucleusLogger.METADATA.warn("local resource \"" + elems[i].getAttribute("url") + 
                        "\" does not exist!!!");
                }
                sources.add(new StreamSource(in));
            }
        }
        return sources.toArray(new Source[sources.size()]);
    }
}