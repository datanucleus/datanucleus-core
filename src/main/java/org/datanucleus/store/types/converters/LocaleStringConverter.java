/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import java.util.Locale;

import org.datanucleus.util.I18nUtils;

/**
 * Class to handle the conversion between java.util.Locale and a String form.
 * Locale should be stored in colums from 2 to 20 characters. Normaly, we will have a string no longer than
 * 5 characters, but variants, in general, are vendor specific and can be longer than expected. 
 * The Variant codes are vendor and browser-specific. For example, use WIN for Windows, MAC for Macintosh,
 * and POSIX for POSIX. Where there are two variants, separate them with an underscore, and put the most
 * important one first. For example, a Traditional Spanish collation might construct a locale with
 * parameters for language, country and variant as: "es", "ES", "Traditional_WIN".  
 * language_country_variant
 * Examples: "en", "de_DE", "_GB", "en_US_WIN", "de__POSIX", "fr_MAC"
 * @see java.util.Locale
 */
public class LocaleStringConverter implements TypeConverter<Locale, String>, ColumnLengthDefiningTypeConverter
{
    public Locale toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        return I18nUtils.getLocaleFromString(str);
    }

    public String toDatastoreType(Locale loc)
    {
        return loc != null ? loc.toString() : null;
    }

    public int getDefaultColumnLength(int columnPosition)
    {
        if (columnPosition != 0)
        {
            return -1;
        }
        // Locales require 20 characters.
        return 20;
    }
}