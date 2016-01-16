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

import java.time.MonthDay;

import org.datanucleus.store.types.converters.MultiColumnConverter;
import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.MonthDay and int[] (the month and the day).
 */
public class MonthDayComponentsConverter implements TypeConverter<MonthDay, int[]>, MultiColumnConverter
{
    private static final long serialVersionUID = 8265858752748293491L;

    public MonthDay toMemberType(int[] vals)
    {
        if (vals == null)
        {
            return null;
        }

        return MonthDay.of(vals[0], vals[1]);
    }

    public int[] toDatastoreType(MonthDay md)
    {
        if (md == null)
        {
            return null;
        }
        return new int[] {md.getMonthValue(), md.getDayOfMonth()};
    }

    public Class[] getDatastoreColumnTypes()
    {
        return new Class[] {int.class, int.class};
    }
}
