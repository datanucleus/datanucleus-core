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
 */
public enum DiscriminatorStrategy
{
    NONE("none"),
    VALUE_MAP("value-map"),
    CLASS_NAME("class-name"),
    ENTITY_NAME("entity-name");

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
        else if (DiscriminatorStrategy.ENTITY_NAME.toString().equals(value))
        {
            return DiscriminatorStrategy.ENTITY_NAME;
        }
        return null;
    }
}