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

/**
 * Extension declared in a plug-in. Represents the XML element
 */
public class Extension
{
    /** reference to the extension point this extension implements * */
    private ExtensionPoint point;

    /** declared plugin * */
    final private Bundle plugin;
    
    /** point id **/
    final private String pointId;

    /** Configuration elements * */
    ConfigurationElement[] configurationElement;

    public Extension(ExtensionPoint point, Bundle plugin)
    {
        this.point = point;
        this.pointId = point.getUniqueId();
        this.plugin = plugin;
        this.configurationElement = new ConfigurationElement[0];
    }

    public Extension(String pointId, Bundle plugin)
    {
        this.pointId = pointId;
        this.plugin = plugin;
        this.configurationElement = new ConfigurationElement[0];
    }
    
    /**
     * Assign the ExtensionPoint to this Extension
     * @param point
     */
    public void setExtensionPoint(ExtensionPoint point)
    {
        this.point = point;
    }
    
    /**
     * Acessor to the extension point id
     * @return extension point id
     */
    public String getExtensionPointId()
    {
        return this.pointId;
    }
    
    /**
     * Accessor for the Plug-in that declared this Extension
     * @return the Plug-in
     */
    public Bundle getPlugin()
    {
        return plugin;
    }

    /**
     * Acessor for the ExtensionPoint that this Extension implements
     * @return the ExtensionPoint
     */
    public ExtensionPoint getPoint()
    {
        return point;
    }

    /**
     * Add a new child ConfigurationElement (declared nested in the extension XML element)
     * @param element the ConfigurationElement
     */
    public void addConfigurationElement(ConfigurationElement element)
    {
        ConfigurationElement[] elms = new ConfigurationElement[configurationElement.length + 1];
        System.arraycopy(configurationElement, 0, elms, 0, configurationElement.length);
        elms[configurationElement.length] = element;
        configurationElement = elms;
    }

    /**
     * Acessor for all ConfigurationElements declared in the Extension
     * @return array of ConfigurationElement
     */
    public ConfigurationElement[] getConfigurationElements()
    {
        return configurationElement;
    }
}