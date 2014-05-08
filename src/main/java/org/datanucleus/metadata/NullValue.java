/**********************************************************************
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
**********************************************************************/
package org.datanucleus.metadata;

import org.datanucleus.util.StringUtils;

/**
 * Representation of how to handle a null value (in a field).
 */
public enum NullValue
{
    EXCEPTION, // Throw an exception when persisting and finding a null
    DEFAULT, // Use the default value when persisting and finding a null
    NONE; // Just persist null when finding a null

    /**
     * Obtain a NullValue for the given name by <code>value</code>
     * @param value the name
     * @return the NullValue found or NullValue.NONE if not found. If <code>value</code> is null, returns NullValue.NONE.
     */    
    public static NullValue getNullValue(final String value)
    {
        if (StringUtils.isWhitespace(value))
        {
            return NullValue.NONE;
        }
        else if (NullValue.DEFAULT.toString().equalsIgnoreCase(value))
        {
            return NullValue.DEFAULT;
        }
        else if (NullValue.EXCEPTION.toString().equalsIgnoreCase(value))
        {
            return NullValue.EXCEPTION;
        }
        else if (NullValue.NONE.toString().equalsIgnoreCase(value))
        {
            return NullValue.NONE;
        }
        return NullValue.NONE;
    }
}