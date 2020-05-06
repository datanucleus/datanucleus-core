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

import java.sql.Time;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.sql.Time and a String form.
 */
public class SqlTimeStringConverter implements TypeConverter<Time, String>
{
    private static final long serialVersionUID = -201061110824623751L;

    public Time toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return java.sql.Time.valueOf(str);
        }
        catch (IllegalArgumentException iae)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, Time.class.getName()), iae);
        }
    }

    public String toDatastoreType(Time time)
    {
        return time != null ? time.toString() : null;
    }
}