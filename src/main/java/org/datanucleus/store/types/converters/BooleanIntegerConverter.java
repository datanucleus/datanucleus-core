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

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.lang.Boolean and an Integer (0, 1) form.
 */
public class BooleanIntegerConverter implements TypeConverter<Boolean, Integer>
{
    private static final long serialVersionUID = -6180650436706210421L;

    public Boolean toMemberType(Integer val)
    {
        if (val == null)
        {
            return null;
        }

        try
        {
            return val == 1 ? true : false;
        }
        catch (NumberFormatException nfe)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", val, Boolean.class.getName()), nfe);
        }
    }

    public Integer toDatastoreType(Boolean bool)
    {
        return bool != null ? (bool ? 1 : 0) : null;
    }
}