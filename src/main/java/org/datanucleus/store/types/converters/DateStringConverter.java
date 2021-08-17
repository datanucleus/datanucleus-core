/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
import java.util.Date;

/**
 * Class to handle the conversion between java.util.Date and a String form.
 * Uses java.time.Instant as an intermediary.
 * Results in a String form like "2021-08-17T09:19:01.585Z".
 */
public class DateStringConverter implements TypeConverter<Date, String>, ColumnLengthDefiningTypeConverter
{
    private static final long serialVersionUID = 4638239842151376340L;

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

    public Date toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        Instant inst = Instant.parse(str);
        return Date.from(inst.atZone(ZoneId.systemDefault()).toInstant());
    }

    public String toDatastoreType(Date date)
    {
        if (date == null)
        {
            return null;
        }

        return date.toInstant().toString();
    }
}