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
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Class to handle the conversion between java.time.OffsetDateTime and java.util.Date.
 */
public class OffsetDateTimeDateConverter implements TypeConverter<OffsetDateTime, Date>
{
    private static final long serialVersionUID = 800484212767523129L;

    public OffsetDateTime toMemberType(Date date)
    {
        if (date == null)
        {
            return null;
        }
        return OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public Date toDatastoreType(OffsetDateTime datetime)
    {
        if (datetime == null)
        {
            return null;
        }
        return Date.from(datetime.atZoneSameInstant(ZoneId.systemDefault()).toInstant());
    }
}
