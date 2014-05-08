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
 * Class defining the possibilities for persistence, in terms of the type of
 * persistence, and the types that are capable to be supported. 
 */
public enum FieldPersistenceModifier
{
    PERSISTENT,
    TRANSACTIONAL,
    NONE,
    DEFAULT;

    /**
     * Return FieldPersistenceModifier from String.
     * @param value persistence-modifier attribute value
     * @return Instance of FieldPersistenceModifier. 
     *         If value invalid, return null.
     */
    public static FieldPersistenceModifier getFieldPersistenceModifier(final String value)
    {
        if (value == null)
        {
            return null;
        }
        else if (FieldPersistenceModifier.PERSISTENT.toString().equalsIgnoreCase(value))
        {
            return FieldPersistenceModifier.PERSISTENT;
        }
        else if (FieldPersistenceModifier.TRANSACTIONAL.toString().equalsIgnoreCase(value))
        {
            return FieldPersistenceModifier.TRANSACTIONAL;
        }
        else if (FieldPersistenceModifier.NONE.toString().equalsIgnoreCase(value))
        {
            return FieldPersistenceModifier.NONE;
        }
        return null;
    }
}
