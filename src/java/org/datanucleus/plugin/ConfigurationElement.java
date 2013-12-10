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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents XML elements declared nested in the extension element
 */
public class ConfigurationElement
{
    /** parent element **/
    final private ConfigurationElement parent;
    /** child elements **/
    private ConfigurationElement[] children;
    /** attributes **/
    final private Map attributes = new HashMap();
    /** attributes **/
    private String[] attributeNames;
    /** element name **/
    private String name;
    /** text of element **/
    private String text;
    /** the Extension **/
    private Extension extension;
    /**
     * Constructor
     * @param name the element's name
     * @param parent the parent. null if there is no parent
     */
    public ConfigurationElement(Extension extension, String name, ConfigurationElement parent)
    {
        this.extension = extension;
        this.name = name;
        this.parent = parent;
        this.attributeNames = new String[0]; 
        this.children = new ConfigurationElement[0];
    }

    /**
     * Acessor for the name of this element
     * @return the name of this element
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Acessor for the parent of this ConfigurationElement
     * @return can return null if there is no parent, or the parent is the Extension
     */
    public ConfigurationElement getParent()
    {
        return parent;
    }
    
    /**
     * Acessor for all children of this ConfigurationElement
     * @return the ConfigurationElement declared nested in this element
     */
    public ConfigurationElement[] getChildren()
    {
        return children;
    }

    /**
     * Acessor for the attribute value by a given name
     * @param name the attribute name
     * @return null if the attribute cannot be found
     */
    public String getAttribute(String name)
    {
        return (String) attributes.get(name);
    }
    
    /**
     * Put a new attribute to this element
     * @param name the attribute's name
     * @param value the attribute's value
     */
    public void putAttribute(String name, String value)
    {
        String[] names = new String[attributeNames.length+1];
        System.arraycopy(attributeNames, 0, names, 0, attributeNames.length);
        names[attributeNames.length] = name;
        attributeNames = names;        
        attributes.put(name,value);
    }   

    /**
     * Add a new children ConfigurationElement to this element
     * @param confElm the ConfigurationElement
     */
    public void addConfigurationElement(ConfigurationElement confElm)
    {
        ConfigurationElement[] elm = new ConfigurationElement[children.length+1];
        System.arraycopy(children, 0, elm, 0, children.length);
        elm[children.length] = confElm;
        children = elm;        
    }   
    
    /**
     * Acessor for all attribute names declared in this element
     * @return the attribute names
     */
    public String[] getAttributeNames()
    {
        return attributeNames;
    }
    
    /**
     * Setter to the text
     * @param text the text
     */
    public void setText(String text)
    {
        this.text = text;
    }
    
    /**
     * Accessor to the text
     * @return the text
     */
    public String getText()
    {
        return text;
    }
    
    /**
     * Accesstor to the {@link Extension}
     * @return the {@link Extension}
     */
    public Extension getExtension()
    {
        return extension;
    }
    
    public String toString() 
    {
        return name + " " + attributes;
    }
}
