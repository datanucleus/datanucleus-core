/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import java.util.TimeZone;

import org.datanucleus.PropertyNames;
import org.datanucleus.store.connection.ConnectionFactory;

/**
 * Validator for persistence properties used by core.
 */
public class CorePropertyValidator implements PersistencePropertyValidator
{
    /**
     * Validate the specified property.
     * @param name Name of the property
     * @param value Value
     * @return Whether it is valid
     */
    public boolean validate(String name, Object value)
    {
        if (name == null)
        {
            return false;
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_AUTOSTART_MODE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("Quiet") ||
                    strVal.equalsIgnoreCase("Ignored") ||
                    strVal.equalsIgnoreCase("Checked"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_FLUSH_MODE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("Auto") ||
                    strVal.equalsIgnoreCase("Manual"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("EXCEPTION") ||
                    strVal.equalsIgnoreCase("LOG"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_DELETION_POLICY))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("JDO2") ||
                    strVal.equalsIgnoreCase("DataNucleus"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_DEFAULT_INHERITANCE_STRATEGY))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("JDO2") ||
                    strVal.equalsIgnoreCase("TABLE_PER_CLASS"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_IDENTIFIER_CASE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("UPPERCASE") ||
                    strVal.equalsIgnoreCase("lowercase") ||
                    strVal.equalsIgnoreCase("PreserveCase"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_VALUEGEN_TXN_ATTRIBUTE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("New") ||
                    strVal.equalsIgnoreCase("UsePM"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_VALUEGEN_TXN_ISOLATION) ||
                (name.equalsIgnoreCase(PropertyNames.PROPERTY_TRANSACTION_ISOLATION)))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("none") ||
                    strVal.equalsIgnoreCase("read-committed") ||
                    strVal.equalsIgnoreCase("read-uncommitted") ||
                    strVal.equalsIgnoreCase("repeatable-read") ||
                    strVal.equalsIgnoreCase("serializable") ||
                    strVal.equalsIgnoreCase("snapshot"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_PLUGIN_REGISTRYBUNDLECHECK))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("EXCEPTION") ||
                    strVal.equalsIgnoreCase("LOG") ||
                    strVal.equalsIgnoreCase("NONE"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_TRANSACTION_TYPE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("RESOURCE_LOCAL") ||
                    strVal.equalsIgnoreCase("JTA"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_SERVER_TIMEZONE_ID))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;

                String[] availableIDs = TimeZone.getAvailableIDs();
                boolean validZone = false;
                for (int i=0; i<availableIDs.length;i++)
                {
                    if (availableIDs[i].equals(strVal))
                    {
                        return true;
                    }
                }
                if (!validZone)
                {
                    return false;
                }
            }
        }
        else if (name.equalsIgnoreCase(ConnectionFactory.DATANUCLEUS_CONNECTION_RESOURCE_TYPE) 
                || name.equalsIgnoreCase(ConnectionFactory.DATANUCLEUS_CONNECTION2_RESOURCE_TYPE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("RESOURCE_LOCAL") ||
                    strVal.equalsIgnoreCase("JTA"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase("datanucleus.schemaTool.mode"))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("create") ||
                    strVal.equalsIgnoreCase("delete") ||
                    strVal.equalsIgnoreCase("validate") ||
                    strVal.equalsIgnoreCase("schemainfo") ||
                    strVal.equalsIgnoreCase("dbinfo"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_DETACH_DETACHMENT_FIELDS))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("load-fields") ||
                    strVal.equalsIgnoreCase("unload-fields") ||
                    strVal.equalsIgnoreCase("load-unload-fields"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_DETACH_DETACHED_STATE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("all") ||
                    strVal.equalsIgnoreCase("fetch-groups") ||
                    strVal.equalsIgnoreCase("loaded"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_VALIDATION_MODE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("auto") ||
                    strVal.equalsIgnoreCase("none") ||
                    strVal.equalsIgnoreCase("callback"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_CACHE_L2_MODE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("ENABLE_SELECTIVE") ||
                    strVal.equalsIgnoreCase("DISABLE_SELECTIVE") ||
                    strVal.equalsIgnoreCase("ALL") ||
                    strVal.equalsIgnoreCase("NONE") ||
                    strVal.equalsIgnoreCase("UNSPECIFIED"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE) ||
                name.equalsIgnoreCase(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_MODE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("none") ||
                    strVal.equalsIgnoreCase("create") ||
                    strVal.equalsIgnoreCase("drop-and-create") ||
                    strVal.equalsIgnoreCase("drop"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_CACHE_L2_RETRIEVE_MODE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("use") ||
                    strVal.equalsIgnoreCase("bypass"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_CACHE_L2_STORE_MODE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("use") ||
                    strVal.equalsIgnoreCase("bypass") ||
                    strVal.equalsIgnoreCase("refresh"))
                {
                    return true;
                }
            }
        }
        else if (name.equalsIgnoreCase(PropertyNames.PROPERTY_CACHE_L2_UPDATE_MODE))
        {
            if (value instanceof String)
            {
                String strVal = (String)value;
                if (strVal.equalsIgnoreCase("commit-and-datastore-read") ||
                    strVal.equalsIgnoreCase("commit-only"))
                {
                    return true;
                }
            }
        }
        return false;
    }
}