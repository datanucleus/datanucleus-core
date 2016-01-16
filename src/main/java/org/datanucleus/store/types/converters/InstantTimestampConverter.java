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

import java.sql.Timestamp;
import java.time.Instant;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.Instant and java.sql.Timestamp
 */
public class InstantTimestampConverter implements TypeConverter<Instant, Timestamp>
{
    private static final long serialVersionUID = 1012730202932240062L;

    public Instant toMemberType(Timestamp ts)
    {
        if (ts == null)
        {
            return null;
        }

        return ts.toInstant();
    }

    public Timestamp toDatastoreType(Instant inst)
    {
        if (inst == null)
        {
            return null;
        }
        return Timestamp.from(inst);
    }
}
