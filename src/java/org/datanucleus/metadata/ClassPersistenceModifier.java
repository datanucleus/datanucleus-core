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
 * Definition of the options for persistence-modifier of a class.
 */
public enum ClassPersistenceModifier
{
    PERSISTENCE_CAPABLE("persistence-capable"),
    PERSISTENCE_AWARE("persistence-aware"),
    NON_PERSISTENT("non-persistent");

    String name;

    private ClassPersistenceModifier(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    /**
     * Return ClassPersistenceModifier from String.
     * @param value persistence-modifier attribute value
     * @return Instance of ClassPersistenceModifier. 
     *         If value invalid, return null.
     */
    public static ClassPersistenceModifier getClassPersistenceModifier(final String value)
    {
        if (value == null)
        {
            // Default to persistable since old files won't have this.
            return ClassPersistenceModifier.PERSISTENCE_CAPABLE;
        }
        else if (ClassPersistenceModifier.PERSISTENCE_CAPABLE.toString().equalsIgnoreCase(value))
        {
            return ClassPersistenceModifier.PERSISTENCE_CAPABLE;
        }
        else if (ClassPersistenceModifier.PERSISTENCE_AWARE.toString().equalsIgnoreCase(value))
        {
            return ClassPersistenceModifier.PERSISTENCE_AWARE;
        }
        else if (ClassPersistenceModifier.NON_PERSISTENT.toString().equalsIgnoreCase(value))
        {
            return ClassPersistenceModifier.NON_PERSISTENT;
        }
        return null;
    }
}