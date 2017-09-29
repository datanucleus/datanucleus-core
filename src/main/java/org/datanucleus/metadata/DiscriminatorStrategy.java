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

/**
 * Representation of the values for discriminator "strategy".
 * <ul>
 * <li><b>VALUE_MAP</b> where you define a value for each class, with no fallback default.</li>
 * <li><b>CLASS_NAME</b> where the class name is used as the discriminator value</li>
 * <li><b>VALUE_MAP_ENTITY_NAME</b> where you define a value for each class, but the fallback is the entity name when no value is provided.</li>
 * </ul>
 */
public enum DiscriminatorStrategy
{
    NONE("none"),
    VALUE_MAP("value-map"),
    CLASS_NAME("class-name"),
    VALUE_MAP_ENTITY_NAME("value-map-entity-name");

    String name;

    private DiscriminatorStrategy(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    /**
     * Accessor for the strategy
     * @param value The string form
     * @return The strategy
     */
    public static DiscriminatorStrategy getDiscriminatorStrategy(final String value)
    {
        if (value == null)
        {
            return null;
        }
        else if (DiscriminatorStrategy.NONE.toString().equals(value))
        {
            return DiscriminatorStrategy.NONE;
        }
        else if (DiscriminatorStrategy.VALUE_MAP.toString().equals(value))
        {
            return DiscriminatorStrategy.VALUE_MAP;
        }
        else if (DiscriminatorStrategy.CLASS_NAME.toString().equals(value))
        {
            return DiscriminatorStrategy.CLASS_NAME;
        }
        else if (DiscriminatorStrategy.VALUE_MAP_ENTITY_NAME.toString().equals(value))
        {
            return DiscriminatorStrategy.VALUE_MAP_ENTITY_NAME;
        }
        return null;
    }
}