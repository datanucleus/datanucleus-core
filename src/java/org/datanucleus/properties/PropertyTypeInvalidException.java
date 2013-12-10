/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved. 
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

import org.datanucleus.exceptions.NucleusUserException;

/**
 * Exception thrown when trying to access a property as a specific type yet it is not possible
 * to return as that type.
 *
 * @version $Revision: 1.1 $
 */
public class PropertyTypeInvalidException extends NucleusUserException
{
    /**
     * Constructs an exception for the specified class.
     * @param name Name of the property
     * @param type Required type
     */
    public PropertyTypeInvalidException(String name, String type)
    {
        // TODO Localise this message
        super("Property \"" + name + "\" is not of required type \"" + type + "\"");
    }
}