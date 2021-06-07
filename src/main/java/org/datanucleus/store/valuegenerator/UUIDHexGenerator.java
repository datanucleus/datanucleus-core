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

import org.datanucleus.store.StoreManager;

/**
 * Value generator for a UUID hexadecimal format.
 * Results in Strings of length 32 characters, containing the IP address of the local machine
 * as per the JDO2 spec section 18.6.1.
 */
public class UUIDHexGenerator extends AbstractUUIDGenerator
{
    /**
     * Constructor.
     * @param storeMgr StoreManager
     * @param name Symbolic name for this generator
     */
    public UUIDHexGenerator(StoreManager storeMgr, String name)
    {
        super(storeMgr, name);
    }

    /**
     * Accessor for the storage class for values generated with this generator.
     * @return Storage class (in this case String.class)
     */
    public static Class getStorageClass()
    {
        return String.class;
    }

    /**
     * Create an identifier with the form "IIIIIIIIJJJJJJJJHHHHLLLLLLLLCCCC".
     * Where IIIIIIII is the IP address, JJJJJJJJ is something unique across JVMs,
     * HHHH is the High Time, LLLLLLLL is the low time, and CCCC is a count.
     * @return The identifier
     */
    protected String getIdentifier()
    {
        StringBuilder str = new StringBuilder(32);

        str.append(getHexFromInt(IP_ADDRESS));
        str.append(getHexFromInt(JVM_UNIQUE));
        short timeHigh = (short) (System.currentTimeMillis() >>> 32);
        str.append(getHexFromShort(timeHigh));
        int timeLow = (int) System.currentTimeMillis();
        str.append(getHexFromInt(timeLow));
        str.append(getHexFromShort(getCount()));

        return str.toString();
    }

    /**
     * Utility to convert an int into a 8-char hex String
     * @param val The int
     * @return The hex String form of the int
     */
    private static String getHexFromInt(int val)
    {
        StringBuilder str = new StringBuilder("00000000");
        String hexstr = Integer.toHexString(val);
        str.replace(8 - hexstr.length(), 8, hexstr);
        return str.toString();
    }

    /**
     * Utility to convert a short into a 4-char hex String
     * @param val The short
     * @return The hex String form of the short
     */
    private static String getHexFromShort(short val)
    {
        StringBuilder str = new StringBuilder("0000");
        String hexstr = Integer.toHexString(val);
        str.replace(4 - hexstr.length(), 4, hexstr);
        return str.toString();
    }
}