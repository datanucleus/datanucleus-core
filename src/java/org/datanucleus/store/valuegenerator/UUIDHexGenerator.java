/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.valuegenerator;

import java.util.Properties;

import org.datanucleus.util.TypeConversionHelper;

/**
 * Value generator for a UUID hexadecimal format.
 * Results in Strings of length 32 characters, containing the IP address of the local machine
 * as per the JDO2 spec section 18.6.1.
 */
public class UUIDHexGenerator extends AbstractUUIDGenerator
{
    /**
     * Constructor.
     * @param name Symbolic name for this generator
     * @param props Properties controlling its behaviour
     */
    public UUIDHexGenerator(String name, Properties props)
    {
        super(name, props);
    }

    /**
     * Create an identifier with the form "IIIIIIIIJJJJJJJJHHHHLLLLLLLLCCCC".
     * Where IIIIIIII is the IP address, JJJJJJJJ is something unique across JVMs,
     * HHHH is the High Time, LLLLLLLL is the low time, and CCCC is a count.
     * @return The identifier
     */
    protected String getIdentifier()
    {
        StringBuffer str = new StringBuffer(32);

        str.append(TypeConversionHelper.getHexFromInt(IP_ADDRESS));
        str.append(TypeConversionHelper.getHexFromInt(JVM_UNIQUE));
        short timeHigh = (short) (System.currentTimeMillis() >>> 32);
        str.append(TypeConversionHelper.getHexFromShort(timeHigh));
        int timeLow = (int) System.currentTimeMillis();
        str.append(TypeConversionHelper.getHexFromInt(timeLow));
        str.append(TypeConversionHelper.getHexFromShort(getCount()));

        return str.toString();
    }
}