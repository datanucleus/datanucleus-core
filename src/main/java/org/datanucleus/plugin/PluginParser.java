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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * Parser for "manifest.mf" and "plugin.xml" files.
 */
class PluginParser
{
    public static Bundle parseManifest(Manifest mf, URL fileUrl)
    {
        Bundle bundle = null;
        try
        {
            String symbolicName = getBundleSymbolicName(mf, null);
            String bundleVersion = getBundleVersion(mf, null);
            String bundleName = getBundleName(mf, null);
            String bundleVendor = getBundleVendor(mf, null);
            bundle = new Bundle(symbolicName, bundleName, bundleVendor, bundleVersion, fileUrl);
            bundle.setRequireBundle(getRequireBundle(mf));
        }
        catch (NucleusException ne)
        {
            NucleusLogger.GENERAL.warn("Plugin at URL=" + fileUrl + " failed to parse so is being ignored", ne);
            return null;
        }
        return bundle;
    }
    
    /**
     * Accessor for the Bundle-Name from the manifest.mf file
     * @param mf the manifest
     * @return the Set with BundleDescription
     */
    private static List<Bundle.BundleDescription> getRequireBundle(Manifest mf)
    {
        String str = mf.getMainAttributes().getValue("Require-Bundle");
        if( str == null || str.length() < 1 )
        {
            return Collections.EMPTY_LIST;
        }
        Parser p = new Parser(str);
        List<Bundle.BundleDescription> requiredBundle = new ArrayList<Bundle.BundleDescription>();
        String bundleSymbolicName = p.parseSymbolicName();
        while(bundleSymbolicName != null)
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
    private static List<ExtensionPoint> parseExtensionPoints(Element rootElement, Bundle plugin, ClassLoaderResolver clr)
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
                extensionPoints.add(new ExtensionPoint(id, name, clr.getResource(schema, null), plugin));
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
    private static List<Extension> parseExtensions(Element rootElement, Bundle plugin, ClassLoaderResolver clr)
    {
        List<Extension> extensions = new ArrayList<Extension>();
        try
        {
            NodeList elements = rootElement.getElementsByTagName("extension");
            for (int i = 0; i < elements.getLength(); i++)
            {
                Element element = (Element) elements.item(i);
                Extension ex = new Extension(element.getAttribute("point"),plugin);
                NodeList elms = element.getChildNodes();
                extensions.add(ex);
                for (int e = 0; e < elms.getLength(); e++)
                {
                    if (elms.item(e) instanceof Element)
                    {
                        ex.addConfigurationElement(parseConfigurationElement(ex, (Element) elms.item(e), null));
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
    
    /**
     * Accessor for the Bundle-SymbolicName from the manifest.mf file
     * @param mf the manifest
     * @param defaultValue a default value, in case no symbolic name found in manifest
     * @return the bundle symbolic name
     */
    private static String getBundleSymbolicName(Manifest mf, String defaultValue)
    {
        if (mf == null)
        {
            return defaultValue;
        }
        String name = mf.getMainAttributes().getValue("Bundle-SymbolicName");
        if (name == null)
        {
            return defaultValue;
        }
        StringTokenizer token = new StringTokenizer(name, ";");
        return token.nextToken().trim();
    }

    /**
     * Accessor for the Bundle-Name from the manifest.mf file
     * @param mf the manifest
     * @param defaultValue a default value, in case no name found in manifest
     * @return the bundle name
     */
    private static String getBundleName(Manifest mf, String defaultValue)
    {
        if (mf == null)
        {
            return defaultValue;
        }
        String name = mf.getMainAttributes().getValue("Bundle-Name");
        if (name == null)
        {
            return defaultValue;
        }
        return name;
    }

    /**
     * Accessor for the Bundle-Vendor from the manifest.mf file
     * @param mf the manifest
     * @param defaultValue a default value, in case no vendor found in manifest
     * @return the bundle vendor
     */
    private static String getBundleVendor(Manifest mf, String defaultValue)
    {
        if (mf == null)
        {
            return defaultValue;
        }
        String vendor = mf.getMainAttributes().getValue("Bundle-Vendor");
        if (vendor == null)
        {
            return defaultValue;
        }
        return vendor;
    }

    /**
     * Accessor for the Bundle-Version from the manifest.mf file
     * @param mf the manifest
     * @param defaultValue a default value, in case no version found in manifest
     * @return the bundle version
     */
    private static String getBundleVersion(Manifest mf, String defaultValue)
    {
        if (mf == null)
        {
            return defaultValue;
        }
        String version = mf.getMainAttributes().getValue("Bundle-Version");
        if (version == null)
        {
            return defaultValue;
        }
        return version;
    }

    /**
     * Method to parse Extensions in plug-in file.
     * @param db DocumentBuilder to use for parsing
     * @param mgr the PluginManager
     * @param fileUrl URL of the plugin.xml file
     * @param clr the ClassLoaderResolver
     * @return array of 2 elements. first element is a List of extensionPoints, and 2nd element is a List of Extension
     * @throws NucleusException if an error occurs during parsing
     */
    public static List[] parsePluginElements(DocumentBuilder db, PluginRegistry mgr, URL fileUrl, Bundle plugin,
            ClassLoaderResolver clr)
    {
        List extensionPoints = Collections.EMPTY_LIST;
        List extensions = Collections.EMPTY_LIST;
        InputStream is = null;
        InputStreamReader isr = null;
        try
        {
            is = fileUrl.openStream();
            isr = new InputStreamReader(is);
            Element rootElement = db.parse(new InputSource(isr)).getDocumentElement();

            if (NucleusLogger.GENERAL.isDebugEnabled())
            {
                NucleusLogger.GENERAL.debug(Localiser.msg("024003", fileUrl.toString()));
            }
            extensionPoints = parseExtensionPoints(rootElement, plugin, clr);

            if (NucleusLogger.GENERAL.isDebugEnabled())
            {
                NucleusLogger.GENERAL.debug(Localiser.msg("024004", fileUrl.toString()));
            }
            extensions = parseExtensions(rootElement, plugin, clr);
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
            if (isr != null)
            {
                try
                {
                    isr.close();
                }
                catch (IOException e)
                {
                }
            }
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
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

    /**
     * Parses the current element and children, creating a ConfigurationElement object
     * @param ex the {@link Extension}
     * @param element the current element
     * @param parent the parent. null if the parent is Extension
     * @return the ConfigurationElement for the element
     */
    public static ConfigurationElement parseConfigurationElement(Extension ex, Element element, ConfigurationElement parent)
    {
        ConfigurationElement confElm = new ConfigurationElement(ex, element.getNodeName(), parent);
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Node attribute = attributes.item(i);
            confElm.putAttribute(attribute.getNodeName(), attribute.getNodeValue());
        }
        NodeList elements = element.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++)
        {
            if (elements.item(i) instanceof Element)
            {
                Element elm = (Element) elements.item(i);
                ConfigurationElement child = parseConfigurationElement(ex, elm, confElm);
                confElm.addConfigurationElement(child);
            }
            else if (elements.item(i) instanceof Text)
            {
                confElm.setText(elements.item(i).getNodeValue());
            }
        }
        return confElm;
    }

    /**
     * Parse a Version Range as per OSGi spec 3.0 $3.2.5
     * @param interval the interval string
     * @return
     */
    public static Bundle.BundleVersionRange parseVersionRange(String interval)
    {
        Parser p = new Parser(interval);
        Bundle.BundleVersionRange versionRange = new Bundle.BundleVersionRange();
        
        if( p.parseChar('['))
        {
            //inclusive
            versionRange.floor_inclusive = true;
        }
        else if( p.parseChar('('))
        {
            //exclusive
            versionRange.floor_inclusive = false;
        }
        versionRange.floor = new Bundle.BundleVersion();
        versionRange.floor.major = p.parseIntegerLiteral().intValue();
        if( p.parseChar('.'))
        {
            versionRange.floor.minor = p.parseIntegerLiteral().intValue();
        }
        if( p.parseChar('.'))
        {
            versionRange.floor.micro = p.parseIntegerLiteral().intValue();
        }
        if( p.parseChar('.'))
        {
            versionRange.floor.qualifier = p.parseIdentifier(); 
        }
        if( p.parseChar(','))
        {
            versionRange.ceiling = new Bundle.BundleVersion();
            versionRange.ceiling.major = p.parseIntegerLiteral().intValue();
            if( p.parseChar('.'))
            {
                versionRange.ceiling.minor = p.parseIntegerLiteral().intValue();
            }
            if( p.parseChar('.'))
            {
                versionRange.ceiling.micro = p.parseIntegerLiteral().intValue();
            }
            if( p.parseChar('.'))
            {
                versionRange.ceiling.qualifier = p.parseIdentifier(); 
            }
            if( p.parseChar(']'))
            {
                //inclusive
                versionRange.ceiling_inclusive = true;
            }
            else if( p.parseChar(')'))
            {
                //exclusive
                versionRange.ceiling_inclusive = false;
            }
        }
        return versionRange;
    }

    /**
     * Parser for a list of Bundle-Description
     */
    public static class Parser
    {
        private final String input;

        protected final CharacterIterator ci;

        /**
         * Constructor
         * @param input The input string
         **/
        public Parser(String input)
        {
            this.input = input;

            ci = new StringCharacterIterator(input);
        }

        /**
         * Accessor for the input string.
         * @return The input string.
         */
        public String getInput()
        {
            return input;
        }

        /**
         * Accessor for the current index in the input string.
         * @return The current index.
         */
        public int getIndex()
        {
            return ci.getIndex();
        }

        /**
         * Skip over any whitespace from the current position.
         * @return The new position
         */
        public int skipWS()
        {
            int startIdx = ci.getIndex();
            char c = ci.current();

            while (Character.isWhitespace(c) || c == '\t' || c == '\f' || c == '\n' || c == '\r' || c == '\u0009' || c == '\u000c' || c == '\u0020' || c == '\11' || c == '\12' || c == '\14' || c == '\15' || c == '\40')
            {
                c = ci.next();
            }

            return startIdx;
        }

        /**
         * Check if END OF TEXT is reach
         * @return true if END OF TEXT is reach
         */
        public boolean parseEOS()
        {
            skipWS();

            return ci.current() == CharacterIterator.DONE;
        }

        /**
         * Check if char <code>c</code> is found
         * @param c the Character to find
         * @return true if <code>c</code> is found
         */
        public boolean parseChar(char c)
        {
            skipWS();

            if (ci.current() == c)
            {
                ci.next();
                return true;
            }
            return false;
        }

        /**
         * Check if char <code>c</code> is found
         * @param c the Character to find
         * @param unlessFollowedBy the character to validate it does not follow <code>c</code>
         * @return true if <code>c</code> is found and not followed by <code>unlessFollowedBy</code>
         */
        public boolean parseChar(char c, char unlessFollowedBy)
        {
            int savedIdx = skipWS();

            if (ci.current() == c && ci.next() != unlessFollowedBy)
            {
                return true;
            }

            ci.setIndex(savedIdx);
            return false;
        }

        /**
         * Parse an integer number from the current position.
         * @return The integer number parsed (null if not valid).
         */
        public BigInteger parseIntegerLiteral()
        {
            int savedIdx = skipWS();

            StringBuilder digits = new StringBuilder();
            int radix;
            char c = ci.current();

            if (c == '0')
            {
                c = ci.next();

                if (c == 'x' || c == 'X')
                {
                    radix = 16;
                    c = ci.next();

                    while (isHexDigit(c))
                    {
                        digits.append(c);
                        c = ci.next();
                    }
                }
                else if (isOctDigit(c))
                {
                    radix = 8;

                    do
                    {
                        digits.append(c);
                        c = ci.next();
                    } while (isOctDigit(c));
                }
                else
                {
                    radix = 10;
                    digits.append('0');
                }
            }
            else
            {
                radix = 10;

                while (isDecDigit(c))
                {
                    digits.append(c);
                    c = ci.next();
                }
            }

            if (digits.length() == 0)
            {
                ci.setIndex(savedIdx);
                return null;
            }

            if (c == 'l' || c == 'L')
            {
                ci.next();
            }

            return new BigInteger(digits.toString(), radix);
        }
        
        /**
         * Check if String <code>s</code> is found
         * @param s the String to find
         * @return true if <code>s</code> is found
         */
        public boolean parseString(String s)
        {
            int savedIdx = skipWS();

            int len = s.length();
            char c = ci.current(); 

            for (int i = 0; i < len; ++i)
            {
                if (c != s.charAt(i))
                {
                    ci.setIndex(savedIdx);
                    return false;
                }

                c = ci.next();
            }

            return true;
        }

        /**
         * Check if String <code>s</code> is found ignoring the case
         * @param s the String to find
         * @return true if <code>s</code> is found
         */
        public boolean parseStringIgnoreCase(String s)
        {
            String lowerCasedString = s.toLowerCase();
            
            int savedIdx = skipWS();

            int len = lowerCasedString.length();
            char c = ci.current(); 

            for (int i = 0; i < len; ++i)
            {
                if (Character.toLowerCase(c) != lowerCasedString.charAt(i))
                {
                    ci.setIndex(savedIdx);
                    return false;
                }

                c = ci.next();
            }

            return true;
        }

        /**
         * Parse a java identifier from the current position.
         * @return The identifier
         */
        public String parseIdentifier()
        {
            skipWS();
            char c = ci.current();

            if (!Character.isJavaIdentifierStart(c))
            {
                return null;
            }

            StringBuilder id = new StringBuilder();
            id.append(c);
            // hyphen symbol is valid according OSGi specification
            while (Character.isJavaIdentifierPart(c = ci.next()) || c=='-')
            {
                id.append(c);
            }

            return id.toString();
        }

        /**
         * Parse an OSGi interval from the current position.
         * @return The interval
         */
        public String parseInterval()
        {
            skipWS();
            char c = ci.current();

            StringBuilder id = new StringBuilder();

            while ((c>='A' && c<='Z') || (c>='a' && c<='z') || (c>='0' && c<='9' ) || c=='.' || c=='_' || c=='-' || c=='[' || c==']' || c=='(' || c==')')
            {
                id.append(c);
                c = ci.next();
            }

            return id.toString();
        }
        
        /**
         * Parses the text string (up to the next space) and
         * returns it. The name includes '.' characters.
         * This can be used, for example, when parsing a class name wanting to
         * read in the full name (including package) so that it can then be
         * checked for existence in the CLASSPATH.
         * @return The name
         */
        public String parseName()
        {
            int savedIdx = skipWS();
            String id;

            if ((id = parseIdentifier()) == null)
            {
                return null;
            }

            StringBuilder qn = new StringBuilder(id);

            while (parseChar('.'))
            {
                if ((id = parseIdentifier()) == null)
                {
                    ci.setIndex(savedIdx);
                    return null;
                }

                qn.append('.').append(id);
            }

            return qn.toString();
        }

        /**
         * Utility to return if a character is a decimal digit.
         * @param c The character
         * @return Whether it is a decimal digit
         */
        private final boolean isDecDigit(char c)
        {
            return c >= '0' && c <= '9';
        }

        /**
         * Utility to return if a character is a octal digit.
         * @param c The character
         * @return Whether it is a octal digit
         */
        private final boolean isOctDigit(char c)
        {
            return c >= '0' && c <= '7';
        }

        /**
         * Utility to return if a character is a hexadecimal digit.
         * @param c The character
         * @return Whether it is a hexadecimal digit
         */
        private final boolean isHexDigit(char c)
        {
            return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
        }

        /**
         * Utility to return if the next non-whitespace character is a single quote.
         * @return Whether it is a single quote at the current point (ignoring whitespace)
         */
        public boolean nextIsSingleQuote()
        {
            skipWS();
            return ci.current() == '\'';
        }

        /**
         * Utility to return if the next character is a dot.
         * @return Whether it is a dot at the current point
         */
        public boolean nextIsDot()
        {
            return ci.current() == '.';
        }

        /**
         * Utility to return if the next character is a comma.
         * @return Whether it is a dot at the current point
         */
        public boolean nextIsComma()
        {
            return ci.current() == ',';
        }
        
        /**
         * Utility to return if the next character is a semi-colon.
         * @return Whether it is a semi-colon at the current point
         */
        public boolean nextIsSemiColon()
        {
            return ci.current() == ';';
        }

        /**
         * Parse a String literal
         * @return the String parsed. null if single quotes or double quotes is found
         * @throws NucleusUserException if an invalid character is found or the CharacterIterator is finished
         */
        public String parseStringLiteral()
        {
            skipWS();

            // Strings can be surrounded by single or double quotes
            char quote = ci.current();
            if (quote != '"' && quote != '\'')
            {
                return null;
            }

            StringBuilder lit = new StringBuilder();
            char c;

            while ((c = ci.next()) != quote)
            {
                if (c == CharacterIterator.DONE)
                {
                    throw new NucleusUserException("Invalid string literal: " + input);
                }

                if (c == '\\')
                {
                    c = parseEscapedCharacter();
                }

                lit.append(c);
            }

            ci.next();

            return lit.toString();
        }

        /**
         * Parse a escaped character
         * @return the escaped char
         * @throws NucleusUserException if a escaped character is not valid
         */
        private char parseEscapedCharacter()
        {
            char c;

            if (isOctDigit(c = ci.next()))
            {
                int i = c - '0';

                if (isOctDigit(c = ci.next()))
                {
                    i = i * 8 + (c - '0');

                    if (isOctDigit(c = ci.next()))
                    {
                        i = i * 8 + (c - '0');
                    }
                    else
                    {
                        ci.previous();
                    }
                }
                else
                {
                    ci.previous();
                }

                if (i > 0xff)
                {
                    throw new NucleusUserException("Invalid character escape: '\\" + Integer.toOctalString(i) + "'");
                }

                return (char)i;
            }

            switch (c)
            {
                case 'b' :
                    return '\b';
                case 't' :
                    return '\t';
                case 'n' :
                    return '\n';
                case 'f' :
                    return '\f';
                case 'r' :
                    return '\r';
                case '"' :
                    return '"';
                case '\'' :
                    return '\'';
                case '\\' :
                    return '\\';
                default :
                    throw new NucleusUserException("Invalid character escape: '\\" + c + "'");
            }
        }

        public String remaining()
        {
            StringBuilder sb = new StringBuilder();
            char c = ci.current();
            while (c != CharacterIterator.DONE)
            {
                sb.append(c);
                c = ci.next();
            }
            return sb.toString();
        }
        
        public String toString()
        {
            return input;
        }
     
        public Map parseParameters()
        {
            skipWS();
            Map paramaters = new HashMap();
            while(nextIsSemiColon())
            {
                parseChar(';');
                skipWS();
                String name = parseName();
                skipWS();
                if( !parseString(":=") && !parseString("=") )
                {
                    throw new NucleusUserException("Expected := or = symbols but found \""+remaining()+"\" at position "+this.getIndex()+" of text \""+input+"\"");
                }
                String argument = parseStringLiteral();
                if( argument == null )
                {
                    argument = parseIdentifier();
                }
                if( argument == null )
                {
                    argument = parseInterval();
                }                
                paramaters.put(name,argument);
                skipWS();
            }
            return paramaters;
        }
        
        public String parseSymbolicName()
        {
            if( nextIsComma() )
            {
                parseChar(',');
            }
            String name = parseName();
            if( name == null && !parseEOS())
            {
                throw new NucleusUserException("Invalid characters found \""+remaining()+"\" at position "+this.getIndex()+" of text \""+input+"\"");
            }
            return name;
        }
    }
}