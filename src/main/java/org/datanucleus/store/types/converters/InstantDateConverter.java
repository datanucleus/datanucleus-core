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

import java.util.Date;
import java.time.Instant;
import java.time.ZoneId;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.Instant and java.util.Date.
 */
public class InstantDateConverter implements TypeConverter<Instant, Date>
{
    private static final long serialVersionUID = 1012730202932240062L;

    public Instant toMemberType(Date date)
    {
        if (date == null)
        {
            return null;
        }

        return date.toInstant();
    }

    public Date toDatastoreType(Instant inst)
    {
        if (inst == null)
        {
            return null;
        }
        return Date.from(inst.atZone(ZoneId.systemDefault()).toInstant());
    }
}
