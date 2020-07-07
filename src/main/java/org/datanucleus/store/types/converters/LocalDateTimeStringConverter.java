/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.time.LocalDateTime and a String form.
 */
public class LocalDateTimeStringConverter implements TypeConverter<LocalDateTime, String>, ColumnLengthDefiningTypeConverter
{
    private static final long serialVersionUID = 8882755544573190247L;

    public LocalDateTime toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return LocalDateTime.parse(str);
        }
        catch (DateTimeParseException pe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, LocalDateTime.class.getName()), pe);
        }
    }

    public String toDatastoreType(LocalDateTime date)
    {
        return date != null ? date.toString() : null;
    }

    public int getDefaultColumnLength(int columnPosition)
    {
        if (columnPosition != 0)
        {
            return -1;
        }
        // Persist as "yyyy-MM-ddThh:mm:ss" when stored as string
        // TODO What if toString() gives "yyyy-MM-dd'T'HH:mm:ssfnnnnnnnnn" ?
        return 19;
    }
}
