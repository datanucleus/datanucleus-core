/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
import java.util.Calendar;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.util.Calendar and a String form.
 * The String form follows the format "EEE MMM dd HH:mm:ss zzz yyyy". That is, milliseconds are not retained currently.
 */
public class CalendarStringConverter implements TypeConverter<Calendar, String>, ColumnLengthDefiningTypeConverter
{
    private static final long serialVersionUID = -4905708644688677004L;

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

    public Calendar toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(Instant.from(FORMATTER.parse(str)).toEpochMilli());
            return cal;
        }
        catch (DateTimeParseException pe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, Calendar.class.getName()), pe);
        }
    }

    public String toDatastoreType(Calendar cal)
    {
        return cal != null ? FORMATTER.format(cal.getTime().toInstant()) : null;
    }
}