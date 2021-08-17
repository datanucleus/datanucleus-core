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
import java.util.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Class to handle the conversion between java.time.LocalTime and java.sql.Time.
 */
public class LocalTimeSqlTimeConverter implements TypeConverter<LocalTime, Time>
{
    private static final long serialVersionUID = -792979141879966844L;

    public LocalTime toMemberType(Time time)
    {
        if (time == null)
        {
            return null;
        }
        // TODO return time.toLocalTime
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time.getTime()), ZoneId.systemDefault()).toLocalTime();
    }

    public Time toDatastoreType(LocalTime time)
    {
        if (time == null)
        {
            return null;
        }
        // TODO return Time.valueOf(time)
        return new Time(Date.from(time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()).getTime());
    }
}
