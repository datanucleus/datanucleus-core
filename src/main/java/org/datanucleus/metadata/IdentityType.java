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
 * Representation of the values for identity-type.
 */
public enum IdentityType
{
    APPLICATION,
    DATASTORE,
    NONDURABLE;

    /**
     * Return IdentityType from String.
     * @param value identity-type attribute value
     * @return Instance of IdentityType. If parse failed, return null.
     */
    public static IdentityType getIdentityType(final String value)
    {
        if (value == null)
        {
            return null;
        }
        else if (IdentityType.APPLICATION.toString().equalsIgnoreCase(value))
        {
            return IdentityType.APPLICATION;
        }
        else if (IdentityType.DATASTORE.toString().equalsIgnoreCase(value))
        {
            return IdentityType.DATASTORE;
        }
        else if (IdentityType.NONDURABLE.toString().equalsIgnoreCase(value))
        {
            return IdentityType.NONDURABLE;
        }
        return null;
    }
}