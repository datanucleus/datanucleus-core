/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.time.Duration;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Class to handle the conversion between java.time.Duration and a String form.
 */
public class DurationStringConverter implements TypeConverter<Duration, String>
{
    private static final long serialVersionUID = -7438116766732450791L;

    public Duration toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        return Duration.parse(str);
    }

    public String toDatastoreType(Duration dur)
    {
        return dur != null ? dur.toString() : null;
    }
}
