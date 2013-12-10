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

/**
 * Validator for a property that represents a boolean.
 */
public class BooleanPropertyValidator implements PersistencePropertyValidator
{
    /**
     * Method to validate the property, allowing only Boolean or String(with boolean values).
     * @param name Name of property
     * @param value Value of property
     * @return Whether it is valid
     */
    public boolean validate(String name, Object value)
    {
        return validateValueIsBoolean(value);
    }

    /**
     * Convenience method that checks that the passed value is usable as a boolean.
     * @param value The value
     * @return Whether it is boolean
     */
    public static boolean validateValueIsBoolean(Object value)
    {
        if (value == null)
        {
            return false;
        }
        if (value instanceof Boolean)
        {
            return true;
        }
        else if (value instanceof String)
        {
            String val = ((String)value).trim();
            if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false"))
            {
                return true;
            }
        }
        return false;
    }
}