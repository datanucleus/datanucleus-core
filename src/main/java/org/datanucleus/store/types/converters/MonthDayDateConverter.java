/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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

import java.sql.Date;
import java.time.MonthDay;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.MonthDay and String.
 */
public class MonthDayDateConverter implements TypeConverter<MonthDay, Date>
{
    private static final long serialVersionUID = 8087124973147837116L;

    @SuppressWarnings("deprecation")
    public MonthDay toMemberType(Date date)
    {
        if (date == null)
        {
            return null;
        }

        return MonthDay.of(date.getMonth()+1, date.getDate()+1);
    }

    @SuppressWarnings("deprecation")
    public Date toDatastoreType(MonthDay md)
    {
        return new Date(0, md.getMonthValue()-1, md.getDayOfMonth()-1);
    }
}
