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

import java.io.IOException;
import java.util.Stack;

import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Abstract handler for MetaData parsing.
 * Should be extended to handle processing of specific types of metadata.
 */
public class AbstractMetaDataHandler extends DefaultHandler
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Manager for the MetaData. */
    protected final MetaDataManager mgr;

    /** Filename for the parsed file. */
    protected final String filename;

    /** The MetaData for this file (the end result of the parse process. */
    protected MetaData metadata;

    /** Entity Resolver to use (if required) */
    protected final EntityResolver entityResolver;

    /** parser buffer */
    protected StringBuffer charactersBuffer = new StringBuffer();

    /** Whether to validate while parsing. */
    protected boolean validate = true;

    /**
     * Stack of meta-data elements. The top of the stack is always the element
     * being process at the moment. The elements are not the XML Element type
     * but are things like ClassMetaData, PackageMetaData etc.
     */
    protected Stack<MetaData> stack = new Stack();

    /**
     * Constructor.
     * @param mgr Manager for the MetaData
     * @param filename The filename
     * @param resolver Entity Resolver to use (null if not available)
     */
    public AbstractMetaDataHandler(MetaDataManager mgr, String filename, EntityResolver resolver)
    {
        super();
        this.mgr = mgr;
        this.filename = filename;
        this.entityResolver = resolver;
    }

    /**
     * Method to set whether to validate during the the handling.
     * @param validate Whether to validate
     */
    public void setValidate(boolean validate)
    {
        this.validate = validate;
    }

    /**
     * Accessor for the MetaData for this file.
     * @return The MetaData.
     */
    public MetaData getMetaData()
    {
        return metadata;
    }

    /**
     * Parser error method. If any syntactical errors are encountered on validation they will appear 
     * here and be logged as warnings. Just points the user to the line/column of their Meta-Data file 
     * for now.
     * @param e Parse Exception
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException e)
    throws SAXException
    {
        if (validate)
        {
            if (e.getColumnNumber() >= 0)
            {
                // Give the column number if it has a value!
                NucleusLogger.METADATA.warn(LOCALISER.msg("044039", filename, "" + e.getLineNumber(),
                    "" + e.getColumnNumber(), e.getMessage()));
            }
            else
            {
                NucleusLogger.METADATA.warn(LOCALISER.msg("044038", filename, "" + e.getLineNumber(),
                    e.getMessage()));
            }
        }
    }

    /**
     * Accessor for an attribute out of the attributes for an element. Allows
     * the specification of a default if no value is provided.
     * @param attrs The attributes
     * @param key Key for the attribute to return
     * @param defaultValue A default to impose if no value.
     * @return Value for the attribute with the specified key.
     */
    protected String getAttr(Attributes attrs, String key, String defaultValue)
    {
        String result = attrs.getValue(key);
        if (result == null)
        {
            return defaultValue;
        }
        else if (result.length() == 0)
        {
            return defaultValue;
        }
        return result;
    }

    /**
     * Accessor for an attribute out of the attributes for an element.
     * @param attrs The attributes
     * @param key Key for the attribute to return
     * @return Value for the attribute with the specified key.
     */
    protected String getAttr(Attributes attrs, String key)
    {
        return getAttr(attrs, key, null);
    }

    /**
     * Method to resolve XML entities. 
     * Uses the entity resolver (if provided) to check for local variants.
     * @param publicId The public id.
     * @param systemId The system id.
     * @return Input Source for the URI.
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,java.lang.String)
     */
    public InputSource resolveEntity(String publicId, String systemId)
    throws SAXException
    {
        InputSource source = null;
        if (entityResolver != null)
        {
            // Delegate to the entity resolver
            try
            {
                source = entityResolver.resolveEntity(publicId, systemId);
            }
            catch (IOException ioe)
            {
                // Do nothing
            }
        }
        if (source == null)
        {
            try
            {
                return super.resolveEntity(publicId, systemId);
            }
            catch (IOException ioe)
            {
                // Do nothing
            }
        }
        return source;
    }

    /**
     * Notification handler for the "body" data inside an element.
     * @param ch The characters
     * @param start The start position in the character array.
     * @param length The length of the string.
     * @throws SAXException in parsing errors
     */
    public void characters(char[] ch, int start, int length)
    throws SAXException
    {
        // Add to the buffer
        charactersBuffer.append(ch, start, length);
    }

    /**
     * Accessor for the "body" text metadata.
     * Resets the body text after access.
     * @return the string form of this metadata
     */
    public String getString()
    {
        String result = charactersBuffer.toString();
        charactersBuffer = new StringBuffer();
        return result;
    }

    /**
     * Accessor for the current MetaData component.
     * @return The current MetaData component.
     */
    protected MetaData getStack()
    {
        return stack.lastElement();
    }

    /**
     * Method to remove the current MetaData component from the Stack.
     * @return Latest MetaData component.
     */
    protected MetaData popStack()
    {
        return stack.pop();
    }

    /**
     * Method to add a MetaData component to the Stack.
     * @param md The component to add.
     */
    protected void pushStack(MetaData md)
    {
        stack.push(md);
    }
}