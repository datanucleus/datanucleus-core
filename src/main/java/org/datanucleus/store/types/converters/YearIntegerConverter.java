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

import java.time.Year;

/**
 * Class to handle the conversion between java.time.Year and Integer.
 */
public class YearIntegerConverter implements TypeConverter<Year, Integer>
{
    private static final long serialVersionUID = -975145761836531481L;

    public Year toMemberType(Integer val)
    {
        if (val == null)
        {
            return null;
        }

        return Year.of(val);
    }

    public Integer toDatastoreType(Year year)
    {
        return year != null ? year.getValue() : null;
    }
}
