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

import java.sql.Time;
import java.util.Calendar;
import java.time.OffsetTime;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.OffsetTime and java.sql.Time.
 */
public class OffsetTimeSqlTimeConverter implements TypeConverter<OffsetTime, Time>
{
    private static final long serialVersionUID = 3340435954686655963L;

    public OffsetTime toMemberType(Time time)
    {
        if (time == null)
        {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        return OffsetTime.of(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND)*1000000, null);
    }

    public Time toDatastoreType(OffsetTime time)
    {
        if (time == null)
        {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(0, 0, 0, time.getHour(), time.getMinute(), time.getSecond());
        cal.set(Calendar.MILLISECOND, time.getNano()/1000000);
        return new Time(cal.getTimeInMillis());
    }
}
