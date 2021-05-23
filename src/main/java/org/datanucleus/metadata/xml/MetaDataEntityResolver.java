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

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.util.AbstractXMLEntityResolver;
import org.datanucleus.util.NucleusLogger;

/**
 * Implementation of an entity resolver for MetaData XML files.
 * Note that this applies to ALL types of MetaData XML (persistence.xml, JDO package.jdo, JDO package.orm, JDO package.jdoquery, JPA/Jakarta orm.xml).
 * We could, potentially, separate these different types of MetaData XML file, and hence split up the entities based on the handler, but not considered a priority currently.
 */
public class MetaDataEntityResolver extends AbstractXMLEntityResolver
{
    final PluginManager pluginMgr;

    public MetaDataEntityResolver(PluginManager pluginMgr)
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
                InputStream in = MetaDataParser.class.getResourceAsStream(elems[i].getAttribute("url"));
                if (in == null)
                {
                    NucleusLogger.METADATA.warn("local resource \"" + elems[i].getAttribute("url") + "\" does not exist!!!");
                }
                sources.add(new StreamSource(in));
            }
        }
        return sources.toArray(new Source[sources.size()]);
    }
}