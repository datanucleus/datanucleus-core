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

import java.math.BigDecimal;

import org.datanucleus.exceptions.NucleusDataStoreException;

/**
 * Class to handle the conversion between java.math.BigDecimal and a String form.
 */
public class BigDecimalStringConverter implements TypeConverter<BigDecimal, String>
{
    public BigDecimal toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return new BigDecimal(str.trim());
        }
        catch (NumberFormatException nfe)
        {
            throw new NucleusDataStoreException(LOCALISER.msg("016002", str, BigDecimal.class.getName()), nfe);
        }
    }

    public String toDatastoreType(BigDecimal bd)
    {
        return bd != null ? bd.toString() : null;
    }
}