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

import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Extension Point declared in a plug-in. Represents the XML declaration.
 */
public class ExtensionPoint
{
    /** unique id * */
    final private String id;

    /** user friendly name * */
    final private String name;

    /** path to schema (xsd) file * */
    final private URL schema;

    /** declared plugin * */
    final private Bundle plugin;

    /** Extensions * */
    private Extension[] extensions;

    /**
     * Constructor
     * @param id the unique id
     * @param name the friendly name
     * @param schema the path to the schema file
     * @param plugin the declared plugin
     */
    public ExtensionPoint(String id, String name, URL schema, Bundle plugin)
    {
        this.id = id;
        this.name = name;
        this.schema = schema;
        this.plugin = plugin;
        extensions = new Extension[0];
    }

    public Extension[] getExtensions()
    {
        return extensions;
    }

    public void sortExtensions(Comparator<Extension> comp)
    {
        Arrays.sort(extensions, comp);
    }

    public void addExtension(Extension extension)
    {
        Extension[] exs = new Extension[extensions.length + 1];
        System.arraycopy(extensions, 0, exs, 0, extensions.length);
        exs[extensions.length] = extension;
        extensions = exs;
    }

    /**
     * Accessor for the id of this ExtensionPoint
     * @return the id (relative id)
     */
    public String getId()
    {
        return id;
    }

    /**
     * Accessor for the pluginId + DOT + id.
     * @return the absolute id (unique id)
     */
    public String getUniqueId()
    {
        return plugin.getSymbolicName() + "." + id;
    }

    /**
     * Accessor for a user friendly name
     * @return the ExtentionPoint name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Accessor to the URL that points to the schema (.xsd) file
     * @return the schema URL
     */
    public URL getSchema()
    {
        return schema;
    }

    /**
     * Accessor for the Plug-in that declared this ExtensionPoint
     * @return the Plug-in
     */
    public Bundle getBundle()
    {
        return plugin;
    }
}
