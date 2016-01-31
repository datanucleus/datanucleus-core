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
import java.time.LocalTime;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.LocalTime and java.util.Date.
 */
public class LocalTimeDateConverter implements TypeConverter<LocalTime, Date>
{
    private static final long serialVersionUID = -2093237215554953399L;

    public LocalTime toMemberType(Date date)
    {
        if (date == null)
        {
            return null;
        }

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        return LocalTime.of(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND)*1000000);
    }

    public Date toDatastoreType(LocalTime time)
    {
        if (time == null)
        {
            return null;
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(0, 0, 0, time.getHour(), time.getMinute(), time.getSecond());
        cal.set(Calendar.MILLISECOND, time.getNano()/1000000);
        return new Date(cal.getTimeInMillis());
    }
}
