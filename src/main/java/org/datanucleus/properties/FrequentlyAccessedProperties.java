/**********************************************************************
Copyright (c) 2014 Kaarel Kann and others. All rights reserved.
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.NucleusLogger;

/**
 * Class for providing faster access to properties that are rarely set but often read.
 */
public class FrequentlyAccessedProperties
{
    private static Map<String, Field> fieldMap = new HashMap<String, Field>();

    private FrequentlyAccessedProperties defaults;

    private Boolean reachabilityAtCommit = null;

    private Boolean detachOnClose = null;

    private Boolean detachAllOnCommit = null;

    private String level2CacheStoreMode = null;

    private String level2CacheRetrieveMode = null;

    private Boolean serialiseRead = null;

    private Boolean optimisticTransaction = null;

    /**
     * Set default properties that are read when property is not defined in this instance
     * @param default Default properties
     */
    public void setDefaults(FrequentlyAccessedProperties defaults)
    {
        this.defaults = defaults;
    }

    /**
     * Tries to set the property value for any of the "frequent" properties.
     * @param property prop name
     * @param value The value of the property
     */
    public void setProperty(String property, Object value)
    {
        if (property == null)
        {
            return;
        }

        Field f = fieldMap.get(property.toLowerCase(Locale.ENGLISH));
        if (f == null)
        {
            return;
        }

        try
        {

            if (value == null)
            {
                f.set(this, value);
                return;
            }

            if (f.getType() == Boolean.class)
            {
                if (value instanceof Boolean)
                {
                    f.set(this, value);
                }
                else if (value instanceof String)
                {
                    Boolean boolVal = Boolean.valueOf((String) value);
                    f.set(this, boolVal);
                }
            }
            else
            {
                f.set(this, String.valueOf(value));
            }

        }
        catch (Exception e)
        {
            throw new NucleusUserException("Failed to set property: " + property + "=" + value + ": " + e, e);
        }

    }

    public Boolean getReachabilityAtCommit()
    {
        if (reachabilityAtCommit == null && defaults != null)
        {
            return defaults.getReachabilityAtCommit();
        }
        return reachabilityAtCommit;
    }

    public Boolean getDetachOnClose()
    {
        if (detachOnClose == null && defaults != null)
        {
            return defaults.getDetachOnClose();
        }
        return detachOnClose;
    }

    public Boolean getDetachAllOnCommit()
    {
        if (detachAllOnCommit == null && defaults != null)
        {
            return defaults.getDetachAllOnCommit();
        }
        return detachAllOnCommit;
    }

    public String getLevel2CacheStoreMode()
    {
        if (level2CacheStoreMode == null && defaults != null)
        {
            return defaults.getLevel2CacheStoreMode();
        }
        return level2CacheStoreMode;
    }

    public String getLevel2CacheRetrieveMode()
    {
        if (level2CacheRetrieveMode == null && defaults != null)
        {
            return defaults.getLevel2CacheRetrieveMode();
        }
        return level2CacheRetrieveMode;
    }

    public Boolean getSerialiseRead()
    {
        if (serialiseRead == null && defaults != null)
        {
            return defaults.getSerialiseRead();
        }
        return serialiseRead;
    }

    public Boolean getOptimisticTransaction()
    {
        if (optimisticTransaction == null && defaults != null)
        {
            return defaults.getOptimisticTransaction();
        }
        return optimisticTransaction;
    }

    private static void addField(String propertyName, String fieldName) throws NoSuchFieldException, SecurityException
    {
        Field f = FrequentlyAccessedProperties.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        fieldMap.put(propertyName.toLowerCase(Locale.ENGLISH), f);
    }

    static
    {
        try
        {
            addField(PropertyNames.PROPERTY_PERSISTENCE_BY_REACHABILITY_AT_COMMIT, "reachabilityAtCommit");
            addField(PropertyNames.PROPERTY_DETACH_ON_CLOSE, "detachOnClose");
            addField(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT, "detachAllOnCommit");
            addField(PropertyNames.PROPERTY_CACHE_L2_STORE_MODE, "level2CacheStoreMode");
            addField(PropertyNames.PROPERTY_CACHE_L2_RETRIEVE_MODE, "level2CacheRetrieveMode");
            addField(PropertyNames.PROPERTY_SERIALIZE_READ, "serialiseRead");
            addField(PropertyNames.PROPERTY_OPTIMISTIC, "optimisticTransaction");
        }
        catch (Exception e)
        {
            NucleusLogger.GENERAL.error("Failed to set up frequently accessed properties: " + e, e);
        }
    }
}