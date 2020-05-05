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

import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.time.MonthDay and String.
 */
public class MonthDayStringConverter implements TypeConverter<MonthDay, String>
{
    private static final long serialVersionUID = 8087124973147837116L;

    public MonthDay toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return MonthDay.parse(str);
        }
        catch (DateTimeParseException pe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, MonthDay.class.getName()), pe);
        }
    }

    public String toDatastoreType(MonthDay md)
    {
        return md != null ? md.toString() : null;
    }
}
