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

/**
 * Validator for persistence properties used by core.
 */
public class CorePropertyValidator implements PropertyValidator
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
        else if (name.equals(PropertyNames.PROPERTY_AUTOSTART_MODE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("quiet") ||
                    strVal.equals("ignored") ||
                    strVal.equals("checked"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_FLUSH_MODE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("auto") ||
                    strVal.equals("manual") ||
                    strVal.equals("query"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("exception") ||
                    strVal.equals("log"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_DELETION_POLICY))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("jdo2") ||
                    strVal.equals("datanucleus"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_METADATA_DEFAULT_INHERITANCE_STRATEGY))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("jdo2") ||
                    strVal.equals("table_per_class"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_IDENTIFIER_CASE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("uppercase") ||
                    strVal.equals("lowercase") ||
                    strVal.equals("mixedcase"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_VALUEGEN_TXN_ATTRIBUTE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("new") ||
                    strVal.equals("existing"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_VALUEGEN_TXN_ISOLATION) ||
                (name.equals(PropertyNames.PROPERTY_TRANSACTION_ISOLATION)))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("none") ||
                    strVal.equals("read-committed") ||
                    strVal.equals("read-uncommitted") ||
                    strVal.equals("repeatable-read") ||
                    strVal.equals("serializable") ||
                    strVal.equals("snapshot"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_PLUGIN_REGISTRYBUNDLECHECK))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("exception") ||
                    strVal.equals("log") ||
                    strVal.equals("none"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_TRANSACTION_TYPE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("resource_local") ||
                    strVal.equals("jta"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_SERVER_TIMEZONE_ID))
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
        else if (name.equals(PropertyNames.PROPERTY_CONNECTION_RESOURCETYPE) 
                || name.equals(PropertyNames.PROPERTY_CONNECTION_RESOURCETYPE2))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("resource_local") ||
                    strVal.equals("jta"))
                {
                    return true;
                }
            }
        }
        else if (name.equals("datanucleus.schemaTool.mode"))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("create") ||
                    strVal.equals("delete") ||
                    strVal.equals("validate") ||
                    strVal.equals("schemainfo") ||
                    strVal.equals("dbinfo"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_DETACH_DETACHMENT_FIELDS))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("load-fields") ||
                    strVal.equals("unload-fields") ||
                    strVal.equals("load-unload-fields"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_DETACH_DETACHED_STATE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("all") ||
                    strVal.equals("fetch-groups") ||
                    strVal.equals("loaded"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_VALIDATION_MODE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("auto") ||
                    strVal.equals("none") ||
                    strVal.equals("callback"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_CACHE_L2_MODE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("enable_selective") ||
                    strVal.equals("disable_selective") ||
                    strVal.equals("all") ||
                    strVal.equals("none") ||
                    strVal.equals("unspecified"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE) ||
                name.equals(PropertyNames.PROPERTY_SCHEMA_GENERATE_SCRIPTS_MODE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("none") ||
                    strVal.equals("create") ||
                    strVal.equals("drop-and-create") ||
                    strVal.equals("drop"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_CACHE_L2_RETRIEVE_MODE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("use") ||
                    strVal.equals("bypass"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_CACHE_L2_STORE_MODE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("use") ||
                    strVal.equals("bypass") ||
                    strVal.equals("refresh"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_CACHE_L2_UPDATE_MODE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("commit-and-datastore-read") ||
                    strVal.equals("commit-only"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_EXECUTION_CONTEXT_CLOSE_ACTIVE_TX_ACTION))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("rollback") || strVal.equals("exception"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_TYPE_WRAPPER_BASIS))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("instantiated") ||
                    strVal.equals("declared"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_ORDER) ||
                name.equals(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_ORDER))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("script") ||
                    strVal.equals("metadata") || 
                    strVal.equals("metadata-then-script") ||
                    strVal.equals("script-then-metadata"))
                {
                    return true;
                }
            }
        }
        return false;
    }
}