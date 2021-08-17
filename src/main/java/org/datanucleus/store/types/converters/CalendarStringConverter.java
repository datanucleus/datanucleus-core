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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.util.Calendar and a String form.
 * Uses java.time.Instant as an intermediary.
 * Results in a String form like "2021-08-17T09:19:01.585Z".
 */
public class CalendarStringConverter implements TypeConverter<Calendar, String>, ColumnLengthDefiningTypeConverter
{
    private static final long serialVersionUID = -4905708644688677004L;

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
            Instant inst = Instant.parse(str);
            ZonedDateTime zdt = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault());
            return GregorianCalendar.from(zdt);
        }
        catch (DateTimeParseException pe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, Calendar.class.getName()), pe);
        }
    }

    public String toDatastoreType(Calendar cal)
    {
        return cal != null ? cal.toInstant().toString() : null;
    }
}