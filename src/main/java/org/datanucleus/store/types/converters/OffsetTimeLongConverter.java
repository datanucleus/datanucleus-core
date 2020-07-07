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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Class to handle the conversion between java.time.OffsetTime and a long form (nanos of day).
 */
public class OffsetTimeLongConverter implements TypeConverter<OffsetTime, Long>
{
    private static final long serialVersionUID = 7502678558541569308L;

    public OffsetTime toMemberType(Long val)
    {
        if (val == null)
        {
            return null;
        }

        ZonedDateTime time_zoned = LocalDateTime.of(LocalDate.now(), LocalTime.ofNanoOfDay(val)).atZone(ZoneId.systemDefault());
        return OffsetTime.from(time_zoned);
    }

    public Long toDatastoreType(OffsetTime time)
    {
        return time != null ? time.toLocalTime().toNanoOfDay() : null;
    }
}
