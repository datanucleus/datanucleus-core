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
 * Validator for a property that represents an integer.
 */
public class IntegerPropertyValidator implements PersistencePropertyValidator
{
    /**
     * Method to validate the property, allowing only Integer or String(with Integer values).
     * @param name Name of property
     * @param value Value of property
     * @return Whether it is valid
     */
    public boolean validate(String name, Object value)
    {
        if (value == null)
        {
            return false;
        }
        if (value instanceof Integer)
        {
            return true;
        }
        else if (value instanceof String)
        {
            String val = ((String)value).trim();
            try
            {
                Integer.valueOf(val);
                return true;
            }
            catch (NumberFormatException nfe)
            {
                return false;
            }
        }
        return false;
    }
}