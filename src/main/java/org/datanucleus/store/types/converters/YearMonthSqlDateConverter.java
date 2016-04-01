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
import java.time.YearMonth;
import java.time.ZoneId;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.YearMonth and java.sql.Date.
 */
public class YearMonthSqlDateConverter implements TypeConverter<YearMonth, Date>
{
    private static final long serialVersionUID = 8087124973147837116L;

    public YearMonth toMemberType(Date date)
    {
        if (date == null)
        {
            return null;
        }
        return YearMonth.from(date.toLocalDate());
    }

    public Date toDatastoreType(YearMonth ym)
    {
        if (ym == null)
        {
            return null;
        }
        // Use day 2 to avoid any potential timezone rounding. Would be better using other means, but doesn't matter here
        return new Date(Date.from(ym.atDay(2).atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
    }
}
