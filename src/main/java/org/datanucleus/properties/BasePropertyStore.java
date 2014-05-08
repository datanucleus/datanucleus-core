/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.properties;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Basic property store implementation, allowing setting of properties
 */
public class BasePropertyStore extends PropertyStore
{
    /**
     * Method to set a property in the store
     * @param name Name of the property
     * @param value Its value
     */
    public void setProperty(String name, Object value)
    {
        setPropertyInternal(name, value);
    }

    public void dump(NucleusLogger logger)
    {
        logger.debug(">> BasePropertyStore : "+ StringUtils.mapToString(properties));
    }

    public Set<String> getPropertyNames()
    {
        return Collections.unmodifiableSet(properties.keySet());
    }

    public Map<String, Object> getProperties()
    {
        return Collections.unmodifiableMap(properties);
    }
}