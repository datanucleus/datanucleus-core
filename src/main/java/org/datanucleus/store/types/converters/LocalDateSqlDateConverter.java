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

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Class to handle the conversion between java.time.LocalDate and java.sql.Date.
 */
public class LocalDateSqlDateConverter implements TypeConverter<LocalDate, Date>
{
    private static final long serialVersionUID = -4923966747560026044L;

    public LocalDate toMemberType(Date date)
    {
        if (date == null)
        {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault()).toLocalDate();
    }

    public Date toDatastoreType(LocalDate localDate)
    {
        if (localDate == null)
        {
            return null;
        }
        return new Date(java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
    }
}
