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

import java.sql.Date;

/**
 * Class to handle the conversion between java.sql.Date and a java.util.Date form.
 */
public class SqlDateDateConverter implements TypeConverter<Date, java.util.Date>
{
    private static final long serialVersionUID = 3850097783909422945L;

    public Date toMemberType(java.util.Date value)
    {
        if (value == null)
        {
            return null;
        }

        return new java.sql.Date(value.getTime());
    }

    public java.util.Date toDatastoreType(Date date)
    {
    	return date;
    }
}