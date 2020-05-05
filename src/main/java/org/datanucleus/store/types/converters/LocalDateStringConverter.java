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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.time.LocalDate and a String form.
 */
public class LocalDateStringConverter implements TypeConverter<LocalDate, String>, ColumnLengthDefiningTypeConverter
{
    private static final long serialVersionUID = 3884056834450290793L;

    public LocalDate toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return LocalDate.parse(str);
        }
        catch (DateTimeParseException pe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, LocalDate.class.getName()), pe);
        }
    }

    public String toDatastoreType(LocalDate date)
    {
        return date != null ? date.toString() : null;
    }

    @Override
    public int getDefaultColumnLength(int columnPosition)
    {
        if (columnPosition != 0)
        {
            return -1;
        }
        // Persist as "yyyy-MM-dd" when stored as string
        return 10;
    }
}
