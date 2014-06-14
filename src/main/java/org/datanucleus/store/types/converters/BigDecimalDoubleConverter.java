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

import java.math.BigDecimal;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.math.BigDecimal and a Double form.
 */
public class BigDecimalDoubleConverter implements TypeConverter<BigDecimal, Double>
{
    private static final long serialVersionUID = 9192173072810027540L;

    public BigDecimal toMemberType(Double val)
    {
        if (val == null)
        {
            return null;
        }

        try
        {
            return BigDecimal.valueOf(val);
        }
        catch (NumberFormatException nfe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", val, BigDecimal.class.getName()), nfe);
        }
    }

    public Double toDatastoreType(BigDecimal bd)
    {
        return bd.doubleValue();
    }
}