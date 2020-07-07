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

import java.time.Instant;

/**
 * Class to handle the conversion between java.time.Instant and Long (of the epoch millisecs).
 */
public class InstantLongConverter implements TypeConverter<Instant, Long>
{
    private static final long serialVersionUID = -5582036749563342638L;

    public Instant toMemberType(Long ms)
    {
        if (ms == null)
        {
            return null;
        }

        return Instant.ofEpochMilli(ms);
    }

    public Long toDatastoreType(Instant inst)
    {
        if (inst == null)
        {
            return null;
        }
        return inst.toEpochMilli();
    }
}
