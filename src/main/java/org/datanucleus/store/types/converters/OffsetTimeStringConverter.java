/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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

import java.time.OffsetTime;

import org.datanucleus.store.types.converters.ColumnLengthDefiningTypeConverter;
import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.OffsetTime and a String form.
 */
public class OffsetTimeStringConverter implements TypeConverter<OffsetTime, String>, ColumnLengthDefiningTypeConverter
{
    private static final long serialVersionUID = 7774900007678148768L;

    public OffsetTime toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        return OffsetTime.parse(str);
    }

    public String toDatastoreType(OffsetTime time)
    {
        return time != null ? time.toString() : null;
    }

    public int getDefaultColumnLength(int columnPosition)
    {
        if (columnPosition != 0)
        {
            return -1;
        }
        // Persist as "hh:mm:ss.SSS" when stored as string
        return 12;
    }
}
