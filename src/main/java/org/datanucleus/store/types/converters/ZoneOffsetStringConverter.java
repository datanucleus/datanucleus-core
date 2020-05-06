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

import java.time.DateTimeException;
import java.time.ZoneOffset;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.time.ZoneOffset and String.
 */
public class ZoneOffsetStringConverter implements TypeConverter<ZoneOffset, String>
{
    private static final long serialVersionUID = -6314756576149793428L;

    public ZoneOffset toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return ZoneOffset.of(str);
        }
        catch (DateTimeException dte)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, ZoneOffset.class.getName()), dte);
        }
    }

    public String toDatastoreType(ZoneOffset offset)
    {
        return offset != null ? offset.toString() : null;
    }
}
