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

import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.time.LocalDate;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.LocalDate and java.util.Date.
 */
public class LocalDateDateConverter implements TypeConverter<LocalDate, Date>
{
    private static final long serialVersionUID = -7505431105592812715L;

    public LocalDate toMemberType(Date date)
    {
        if (date == null)
        {
            return null;
        }

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        LocalDate localDate = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));
        return localDate;
    }

    public Date toDatastoreType(LocalDate localDate)
    {
        if (localDate == null)
        {
            return null;
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(localDate.getYear(), localDate.getMonth().ordinal(), localDate.getDayOfMonth());
        return new Date(cal.getTimeInMillis());
    }
}
