/******************************************************************
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
*****************************************************************/
package org.datanucleus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Abstract implementation of an entity resolver for XML files.
 * Supports a series of internally supported public or system identities, allowing
 * implementers to support particular identities and direct them to local copies
 * of the DTD for example.
 */
public abstract class AbstractXMLEntityResolver implements EntityResolver
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Map of public identity entities supported. The key will be the identity, and the value is the local input to use. */
    protected HashMap publicIdEntities = new HashMap();

    /** Map of system identity entities supported. The key will be the identity, and the value is the local input to use. */
    protected HashMap systemIdEntities = new HashMap();

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
                String internalEntity = (String)publicIdEntities.get(publicId);
                if (internalEntity != null)
                {
                    return getLocalInputSource(publicId, systemId, internalEntity);
                }
            }

            if (systemId != null)
            {
                // Use systemId if possible
                String internalEntity = (String)systemIdEntities.get(systemId);
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
                            NucleusLogger.METADATA.debug(LOCALISER.msg("028001", publicId, systemId));
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
                            NucleusLogger.METADATA.debug(LOCALISER.msg("028001", publicId, systemId));
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

            NucleusLogger.METADATA.error(LOCALISER.msg("028002", publicId, systemId));
            return null;
        }
        catch (Exception e)
        {
        	NucleusLogger.METADATA.error(LOCALISER.msg("028003", publicId, systemId), e);
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
            NucleusLogger.METADATA.debug(LOCALISER.msg("028000", publicId, systemId, localPath));
        }

    	InputStream in = AbstractXMLEntityResolver.class.getResourceAsStream(localPath);
    	if (in == null)
    	{
    		NucleusLogger.METADATA.fatal("local resource \"" + localPath + "\" does not exist!!!");
    		throw new FileNotFoundException("Unable to load resource: " + localPath);
    	}
    	return new InputSource(new InputStreamReader(in));
    }
}