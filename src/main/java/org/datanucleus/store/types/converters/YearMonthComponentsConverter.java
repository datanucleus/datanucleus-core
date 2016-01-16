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

import java.time.YearMonth;

import org.datanucleus.store.types.converters.MultiColumnConverter;
import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.YearMonth and int[] (the year and the month).
 */
public class YearMonthComponentsConverter implements TypeConverter<YearMonth, int[]>, MultiColumnConverter
{
    private static final long serialVersionUID = 375516277263118399L;

    public YearMonth toMemberType(int[] vals)
    {
        if (vals == null)
        {
            return null;
        }

        return YearMonth.of(vals[0], vals[1]);
    }

    public int[] toDatastoreType(YearMonth ym)
    {
        if (ym == null)
        {
            return null;
        }
        return new int[] {ym.getYear(), ym.getMonthValue()};
    }

    public Class[] getDatastoreColumnTypes()
    {
        return new Class[] {int.class, int.class};
    }
}
