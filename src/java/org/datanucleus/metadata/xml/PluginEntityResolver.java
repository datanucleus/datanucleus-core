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

import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.util.AbstractXMLEntityResolver;

/**
 * Implementation of an entity resolver for DTD/XSD files.
 * Handles entity resolution for files configured via plugins.
 */
public class PluginEntityResolver extends AbstractXMLEntityResolver
{
    public PluginEntityResolver(PluginManager pluginMgr)
    {
        ConfigurationElement[] elems =
            pluginMgr.getConfigurationElementsForExtension("org.datanucleus.metadata_entityresolver", null, null);
        for (int i=0; i<elems.length; i++)
        {
            if (elems[i].getAttribute("type") != null )
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
}