/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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

import java.util.BitSet;
import java.util.StringTokenizer;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the conversion between java.util.BitSet and a String form.
 */
public class BitSetStringConverter implements TypeConverter<BitSet, String>
{
    public BitSet toMemberType(String str)
    {
        if (str == null)
        {
            return null;
        }

        BitSet set = new BitSet();
        StringTokenizer tokeniser = new StringTokenizer(str.substring(1, str.length()-1), ",");
        while (tokeniser.hasMoreTokens())
        {
            String token = tokeniser.nextToken().trim();
            try
            {
                int position = Integer.valueOf(token).intValue();
                set.set(position);
            }
            catch (NumberFormatException nfe)
            {
                throw new NucleusDataStoreException(Localiser.msg("016002", str, BitSet.class.getName()), nfe);
            }
        }
        return set;
    }

    public String toDatastoreType(BitSet set)
    {
        return set != null ? set.toString() : null;
    }
}