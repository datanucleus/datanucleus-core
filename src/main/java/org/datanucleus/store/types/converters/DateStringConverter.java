/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.util.Date and a String form.
 * The String form follows the format "EEE MMM dd HH:mm:ss zzz yyyy". That is, milliseconds are not retained currently.
 */
public class DateStringConverter implements TypeConverter<Date, String>, ColumnLengthDefiningTypeConverter
{
    private static final long serialVersionUID = 4638239842151376340L;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.converters.ColumnLengthDefiningTypeConverter#getDefaultColumnLength(int)
     */
    @Override
    public int getDefaultColumnLength(int columnPosition)
    {
        if (columnPosition != 0)
        {
            return -1;
        }
        return 28;
    }

    public Date toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return Date.from(Instant.from(FORMATTER.parse(str)));
        }
        catch (DateTimeParseException pe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, Date.class.getName()), pe);
        }
    }

    public String toDatastoreType(Date date)
    {
        return date != null ? FORMATTER.format(date.toInstant()) : null;
    }
}