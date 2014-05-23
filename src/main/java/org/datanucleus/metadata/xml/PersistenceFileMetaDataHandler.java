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
package org.datanucleus.metadata.xml;

import java.net.URI;
import java.net.URISyntaxException;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.PersistenceFileMetaData;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Parser handler for "persistence.xml" files to convert them into a PersistenceFileMetaData.
 * Implements DefaultHandler and handles the extracting of MetaData from the
 * XML elements/attributes. This class simply constructs the MetaData representation
 * mirroring what is in the MetaData file.
 * <P>Operates the parse process using a Stack. MetaData components are added
 * to the stack as they are encountered and created. They are then popped off
 * the stack when the end element is encountered.</P>
 */
public class PersistenceFileMetaDataHandler extends AbstractMetaDataHandler
{
    URI rootURI = null;

    /**
     * Constructor. Protected to prevent instantiation.
     * @param mgr the metadata manager (not used)
     * @param filename The name of the file to parse
     * @param resolver Entity Resolver to use (null if not available)
     */
    public PersistenceFileMetaDataHandler(MetaDataManager mgr, String filename, EntityResolver resolver)
    {
        super(mgr, filename, resolver);
        metadata = new PersistenceFileMetaData(filename);
        pushStack(metadata);

        // Get the root URL
        String rootFilename = null;
        if (filename.endsWith("/META-INF/persistence.xml")) // Presumably the same on Windows?
        {
            // Default location "META-INF/persistence.xml"
            rootFilename = filename.substring(0, filename.length() - "/META-INF/persistence.xml".length());
        }
        else
        {
            // User-defined "persistence.xml" location so go up one level (omit "/persistence-filename")
            rootFilename = filename.substring(0, filename.lastIndexOf("/"));
        }

        try
        {
            // Some URLs passed to us by ClassLoader.getResources have spaces in the filename so encode
            // This was reported by Google but no test-case was forthcoming so unreproduceable
            rootFilename = rootFilename.replace(" ", "%20");
            rootURI = new URI(rootFilename);

// This is commented out since it breaks standard JPA behaviour where the rootURI.scheme is expected
// e.g orig rootURI=file:/usr/local/datanucleus/test/accessplatform/trunk/test.jpa.general/target/classes
// e.g new  rootURI=file%3A%2Fusr%2Flocal%2Fdatanucleus%2Ftest%2Faccessplatform%2Ftrunk%2Ftest.jpa.general%2Ftarget%2Fclasses
// i.e totally different
//          rootURI = new URI(URLEncoder.encode(rootFilename, "UTF-8"));
        }
        catch (URISyntaxException e)
        {
            NucleusLogger.METADATA.warn("Error deriving persistence-unit root URI from " + rootFilename, e);
        }
    }

    /**
     * Handler method called at the start of an element.
     * @param uri URI of the tag
     * @param localName Local name
     * @param qName Element name
     * @param attrs Attributes for this element 
     * @throws SAXException in parsing errors
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
    throws SAXException 
    {
        if (localName.length()<1)
        {
            localName = qName;
        }
        try
        {
            if (localName.equals("persistence"))
            {
                // New "persistence" file
                // Do nothing - created in our constructor
            }
            else if (localName.equals("persistence-unit"))
            {
                // New "persistence-unit"
                PersistenceFileMetaData filemd = (PersistenceFileMetaData)getStack();
                PersistenceUnitMetaData pumd = new PersistenceUnitMetaData(getAttr(attrs, "name"),
                    getAttr(attrs, "transaction-type"), rootURI);
                filemd.addPersistenceUnit(pumd);
                pushStack(pumd);
            }
            else if (localName.equals("properties"))
            {
                // Do nothing
            }
            else if (localName.equals("property"))
            {
                // New "property" for the current persistence unit
                PersistenceUnitMetaData pumd = (PersistenceUnitMetaData)getStack();
                pumd.addProperty(getAttr(attrs, "name"), getAttr(attrs, "value"));
            }
            else if (localName.equals("mapping-file"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("class"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("jar-file"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("jta-data-source"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("non-jta-data-source"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("description"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("provider"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("shared-cache-mode"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("validation-mode"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("exclude-unlisted-classes"))
            {
                // Processed elsewhere
            }
            else
            {
                String message = Localiser.msg("044037",qName);
                NucleusLogger.METADATA.error(message);
                throw new RuntimeException(message);
            }
        }
        catch (RuntimeException ex)
        {
            NucleusLogger.METADATA.error(Localiser.msg("044042", qName, getStack(), uri), ex);
            throw ex;
        }
    }

    /**
     * Handler method called at the end of an element.
     * @param uri URI of the tag
     * @param localName local name
     * @param qName Name of element just ending
     * @throws SAXException in parsing errors
     */
    public void endElement(String uri, String localName, String qName)
    throws SAXException
    {
        if (localName.length()<1)
        {
            localName = qName;
        }

        // Save the current string for elements that have a body value
        String currentString = getString().trim();
        if (currentString.length() > 0)
        {
            MetaData md = getStack();
            if (localName.equals("description"))
            {
                // Unit description
                ((PersistenceUnitMetaData)md).setDescription(currentString);
            }
            else if (localName.equals("provider"))
            {
                // Unit provider
                ((PersistenceUnitMetaData)md).setProvider(currentString);
            }
            else if (localName.equals("jta-data-source"))
            {
                // JTA data source
                ((PersistenceUnitMetaData)md).setJtaDataSource(currentString);
            }
            else if (localName.equals("non-jta-data-source"))
            {
                // Non-JTA data source
                ((PersistenceUnitMetaData)md).setNonJtaDataSource(currentString);
            }
            else if (localName.equals("class"))
            {
                // New persistent class
                ((PersistenceUnitMetaData)md).addClassName(currentString);
            }
            else if (localName.equals("mapping-file"))
            {
                // New mapping file
                ((PersistenceUnitMetaData)md).addMappingFile(currentString);
            }
            else if (localName.equals("jar-file"))
            {
                // New jar file
                ((PersistenceUnitMetaData)md).addJarFile(currentString);
            }
            else if (localName.equals("shared-cache-mode"))
            {
                ((PersistenceUnitMetaData)md).setCaching(currentString);
            }
            else if (localName.equals("validation-mode"))
            {
                ((PersistenceUnitMetaData)md).setValidationMode(currentString);
            }
            else if (localName.equals("exclude-unlisted-classes"))
            {
                if (StringUtils.isWhitespace(currentString))
                {
                    currentString = "true";
                }
                Boolean val = Boolean.valueOf(currentString);
                if (val != null)
                {
                    ((PersistenceUnitMetaData)md).setExcludeUnlistedClasses(val.booleanValue());
                }
            }
        }

        // Pop the tag
        // If startElement pushes an element onto the stack need a remove here for that type
        if (qName.equals("persistence-unit"))
        {
            popStack();
        }
    }
}