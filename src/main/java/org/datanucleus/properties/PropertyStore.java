/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Representation of a store of properties.
 * The properties can be for persistence, or for the datastore, or whatever.
 * This class provides convenience type accessors to the properties.
 */
public abstract class PropertyStore
{
    /** Map of properties. */
    protected Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Method to set a property in the store
     * @param name Name of the property
     * @param value Its value
     */
    protected void setPropertyInternal(String name, Object value)
    {
        this.properties.put(name.toLowerCase(Locale.ENGLISH), value);
    }

    /**
     * Method to get the value of a property from the store.
     * @param name Name of the property
     * @return Its value (or null)
     */
    public Object getProperty(String name)
    {
        if (properties.containsKey(name.toLowerCase(Locale.ENGLISH)))
        {
            return properties.get(name.toLowerCase(Locale.ENGLISH));
        }
        return null;
    }

    /**
     * Accessor for whether a particular property is defined (but may be null).
     * @param name Property name
     * @return Whether the property is defined
     */
    public boolean hasProperty(String name)
    {
        return properties.containsKey(name.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Accessor for whether a particular property is defined and has a non-null value.
     * @param name Property name
     * @return Whether the property is defined
     */
    public boolean hasPropertyNotNull(String name)
    {
        return getProperty(name) != null;
    }

    /**
     * Accessor for the specified property as an int.
     * If the specified property isn't found returns 0.
     * @param name Name of the property
     * @return Int value for the property
     * @throws PropertyTypeInvalidException thrown when the property is not available as this type
     */
    public int getIntProperty(String name)
    {
        Object obj = getProperty(name);
        if (obj != null)
        {
            if (obj instanceof Number)
            {
                return ((Number)obj).intValue();
            }
            else if (obj instanceof String)
            {
                Integer intVal = Integer.valueOf((String)obj);
                setPropertyInternal(name, intVal); // Replace String value with Integer
                return intVal.intValue();
            }
        }
        else
        {
            return 0;
        }
        throw new PropertyTypeInvalidException(name, "int");
    }

    /**
     * Accessor for the specified property as a boolean.
     * If the specified property isn't found returns false.
     * @param name Name of the property
     * @return Boolean value for the property
     * @throws PropertyTypeInvalidException thrown when the property is not available as this type
     */
    public boolean getBooleanProperty(String name)
    {
        return getBooleanProperty(name, false);
    }

    /**
     * Accessor for the specified property as a boolean.
     * @param name Name of the property
     * @param resultIfNotSet The value to return if no value for the specified property is found.
     * @return Boolean value for the property
     * @throws PropertyTypeInvalidException thrown when the property is not available as this type
     */
    public boolean getBooleanProperty(String name, boolean resultIfNotSet)
    {
        Object obj = getProperty(name);
        if (obj != null)
        {
            if (obj instanceof Boolean)
            {
                return ((Boolean)obj).booleanValue();
            }
            else if (obj instanceof String)
            {
                Boolean boolVal = Boolean.valueOf((String)obj);
                setPropertyInternal(name, boolVal); // Replace String value with Boolean
                return boolVal.booleanValue();
            }
        }
        else
        {
            return resultIfNotSet;
        }
        throw new PropertyTypeInvalidException(name, "boolean");
    }

    /**
     * Accessor for the specified property as a Boolean.
     * If the specified property isn't found returns false.
     * @param name Name of the property
     * @return Boolean value for the property (or null if not present)
     * @throws PropertyTypeInvalidException thrown when the property is not available as this type
     */
    public Boolean getBooleanObjectProperty(String name)
    {
        Object obj = getProperty(name);
        if (obj != null)
        {
            if (obj instanceof Boolean)
            {
                return ((Boolean)obj);
            }
            else if (obj instanceof String)
            {
                Boolean boolVal = Boolean.valueOf((String)obj);
                setPropertyInternal(name, boolVal); // Replace String value with Boolean
                return boolVal;
            }
        }
        else
        {
            return null;
        }
        throw new PropertyTypeInvalidException(name, "Boolean");
    }

    /**
     * Accessor for the specified property as a String.
     * If the specified property isn't found returns null.
     * @param name Name of the property
     * @return String value for the property
     * @throws PropertyTypeInvalidException thrown when the property is not available as this type
     */
    public String getStringProperty(String name)
    {
        Object obj = getProperty(name);
        if (obj != null)
        {
            if (obj instanceof String)
            {
                return ((String)obj);
            }
        }
        else
        {
            return null;
        }
        throw new PropertyTypeInvalidException(name, "String");
    }
}