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
    ...
*****************************************************************/
package org.datanucleus.store.autostart;

import org.datanucleus.util.AbstractXMLEntityResolver;

/**
 * Implementation of an entity resolver for Auto Starter files.
 */
public class XMLAutoStarterEntityResolver extends AbstractXMLEntityResolver
{
    /** Public Key for DataNucleus auto starter. */
    public static final String PUBLIC_ID_KEY="-//DataNucleus//DTD DataNucleus AutoStarter Metadata 1.0//EN";

    public XMLAutoStarterEntityResolver()
    {
        // Add definitions for internally supported URLs
        publicIdEntities.put(PUBLIC_ID_KEY, "/org/datanucleus/datanucleus_autostart_1_0.dtd");
        systemIdEntities.put(PUBLIC_ID_KEY, "/org/datanucleus/datanucleus_autostart_1_0.dtd");
        systemIdEntities.put("file:/org/datanucleus/datanucleus_autostart_1_0.dtd", 
            "/org/datanucleus/datanucleus_autostart_1_0.dtd");
    }
}