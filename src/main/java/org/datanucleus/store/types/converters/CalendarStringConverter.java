/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.util.Calendar and a String form.
 */
public class CalendarStringConverter implements TypeConverter<Calendar, String>
{
    private static final ThreadLocal<FormatterInfo> formatterThreadInfo = new ThreadLocal<FormatterInfo>()
    {
        protected FormatterInfo initialValue()
        {
            return new FormatterInfo();
        }
    };

    static class FormatterInfo
    {
        SimpleDateFormat formatter;
    }

    private DateFormat getFormatter()
    {
        FormatterInfo formatInfo = formatterThreadInfo.get();
        if (formatInfo.formatter == null)
        {
            formatInfo.formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        }
        return formatInfo.formatter;
    }

    public Calendar toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            Date date = getFormatter().parse(str);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        }
        catch (ParseException pe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, Calendar.class.getName()), pe);
        }
    }

    public String toDatastoreType(Calendar cal)
    {
        return cal != null ? getFormatter().format(cal.getTime()) : null;
    }
}