/******************************************************************
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
2004 Erik Bengtson - changed to use org.w3c.dom
2004 Andy Jefferson - added table-owner. Changed table to be optional
   ...
*****************************************************************/
package org.datanucleus.store.autostart;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.DatastoreInitialisationException;
import org.datanucleus.store.StoreData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * An auto-starter mechanism storing its definition in an XML file.
 * Is independent of the datastore since it is stored as a file and not in the actual datastore.
 *
 * TODO Add a DataNucleusAutoStart DTD to validate the file automatically.
 * TODO If we have one per PMF, need to guarantee unique naming of file.
 */
public class XMLAutoStarter extends AbstractAutoStartMechanism
{
    protected final URL fileUrl;
    protected Document doc;
    protected Element rootElement;
    String version = null;

    Set<String> autoStartClasses = new HashSet<String>();

    /**
     * Constructor, taking the XML file URL.
     * @param storeMgr The StoreManager managing the store that we are auto-starting.
     * @param clr The ClassLoaderResolver
     * @throws MalformedURLException if an error occurs processing the URL
     */
    public XMLAutoStarter(StoreManager storeMgr, ClassLoaderResolver clr) throws MalformedURLException
    {
        super();

        this.fileUrl = new URL("file:" + storeMgr.getStringProperty(PropertyNames.PROPERTY_AUTOSTART_XMLFILE));

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db;

        try
        {
            InputStreamReader isr = null;
            db = factory.newDocumentBuilder();
            try
            {
                db.setEntityResolver(new XMLAutoStarterEntityResolver());
                isr = new InputStreamReader(fileUrl.openStream());
                rootElement = db.parse(new InputSource(isr)).getDocumentElement();
                doc = rootElement.getOwnerDocument();
            }
            catch (Exception e)
            {
                NucleusLogger.PERSISTENCE.info(Localiser.msg("034201", fileUrl.getFile()));

                // File doesn't exist, so create it
                doc = db.newDocument();
                rootElement = doc.createElement("datanucleus_autostart");
                doc.appendChild(rootElement);

                writeToFile();
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
            }
        }
        catch (ParserConfigurationException e1)
        {
            NucleusLogger.PERSISTENCE.error(Localiser.msg("034202", fileUrl.getFile(), e1.getMessage()));
        }

        version = storeMgr.getNucleusContext().getPluginManager().getVersionForBundle("org.datanucleus");
    }

    /**
     * Accessor for all auto start data for this starter.
     * @return The class auto start data. Collection of StoreData elements
     * @throws DatastoreInitialisationException If an error occurs in datastore init
     */
    public Collection<StoreData> getAllClassData()
    throws DatastoreInitialisationException
    {
        Collection<StoreData> classes = new HashSet<>();

        NodeList classElements = rootElement.getElementsByTagName("class");
        for (int i=0; i<classElements.getLength(); i++)
        {
            Element element = (Element) classElements.item(i);

            // TODO Likely should just use "FCO" here since these are all classes, not fields
            StoreData data = new StoreData(element.getAttribute("name"), null, element.getAttribute("type").equals("FCO") ? StoreData.Type.FCO : StoreData.Type.SCO, null);
            autoStartClasses.add(data.getName());

            NamedNodeMap attributeMap = element.getAttributes();
            for (int j=0;j<attributeMap.getLength();j++)
            {
                Node attr = attributeMap.item(j);
                String attrName = attr.getNodeName();
                String attrValue = attr.getNodeValue();
                if (!attrName.equals("name") && !attrName.equals("type"))
                {
                    data.addProperty(attrName, attrValue);
                }
            }
            classes.add(data);
        }

        return classes;
    }

    /**
     * Whether it's open for writing (add/delete) classes to the auto start mechanism.
     * This autostarter is always open
     * @return whether this is open for writing 
     */
    public boolean isOpen()
    {
        return true;
    }

    /**
     * Performs the write to the XML file.
     */
    public void close()
    {
        writeToFile();
        super.close();
    }

    /**
     * Method to add a class to the starter.
     * Adds attributes for all defined properties.
     * @param data The store data to add
     */
    public void addClass(StoreData data)
    {
        if (autoStartClasses.contains(data.getName()))
        {
            return;
        }

        Element classElement = doc.createElement("class");
        classElement.setAttribute("name", data.getName());
        classElement.setAttribute("type", data.isFCO() ? "FCO" : "SCO");
        classElement.setAttribute("version", version);

        Map dataProps = data.getProperties();
        Iterator propsIter = dataProps.entrySet().iterator();
        while (propsIter.hasNext())
        {
            Map.Entry entry = (Map.Entry)propsIter.next();
            String key = (String)entry.getKey();
            Object val = entry.getValue();
            if (val instanceof String)
            {
                classElement.setAttribute(key, (String)val);
            }
        }

        rootElement.appendChild(classElement);
    }

    /**
     * Method to remove a class from the starter
     * @param className The name of the class to remove.
     */
    public void deleteClass(String className)
    {
        autoStartClasses.remove(className);

        NodeList classElements = rootElement.getElementsByTagName("class");
        for (int i=0; i<classElements.getLength(); i++)
        {
            Element element = (Element) classElements.item(i);
            String attr = element.getAttribute("name");
            if (attr != null && attr.equals(className))
            {
                rootElement.removeChild(element);
            }
        }
    }

    /**
     * Remove all classes from the starter.
     */
    public void deleteAllClasses()
    {
        autoStartClasses.clear();

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db;

        try
        {
        	// Dont do any deletion, just reconstruct the DOM with an empty root element
            db = factory.newDocumentBuilder();
            doc = db.newDocument();
            rootElement = doc.createElement("datanucleus_autostart");
            doc.appendChild(rootElement);
        }
        catch (ParserConfigurationException e)
        {
            NucleusLogger.PERSISTENCE.error(Localiser.msg("034203", fileUrl.getFile(), e.getMessage()));
        }
    }

    /**
     * Method to give a descriptive name for the starter process.
     * @return Description of the starter process.
     */
    public String getStorageDescription()
    {
        return Localiser.msg("034200");
    }

    /**
     * Method to write the DOM to its file.
     */
    private synchronized void writeToFile()
    {
        // Write the DOM back to file
        FileOutputStream os = null;
        try
        {
            os = new FileOutputStream(fileUrl.getFile());
            StreamResult result = new StreamResult(os);

            Transformer m = TransformerFactory.newInstance().newTransformer();
            m.setOutputProperty(OutputKeys.INDENT, "yes");
            m.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, XMLAutoStarterEntityResolver.PUBLIC_ID_KEY);
            m.transform(new DOMSource(doc), result);
            os.close();
        }
        catch (Exception e)
        {
            NucleusLogger.PERSISTENCE.error(Localiser.msg("034203", fileUrl.getFile(), e.getMessage()));
        }
        finally
        {
            if (os != null)
            {
                try
                {
                    os.close();
                    os = null;
                }
                catch (IOException ioe)
                {}
            }
        }
    }
}