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
 */
public class LocaleStringConverter implements TypeConverter<Locale, String>
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
}