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
package org.datanucleus.store.types.converters;

import java.util.UUID;

/**
 * Class to handle the conversion between java.util.UUID and a String form.
 */
public class UUIDStringConverter implements TypeConverter<UUID, String>, ColumnLengthDefiningTypeConverter
{
    public UUID toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        return UUID.fromString(str);
    }

    public String toDatastoreType(UUID uuid)
    {
        return uuid != null ? uuid.toString() : null;
    }

    public int getDefaultColumnLength(int columnPosition)
    {
        if (columnPosition != 0)
        {
            return -1;
        }
        // java.util.UUID requires 36 chars.
        return 36;
    }
}