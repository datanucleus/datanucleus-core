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
 * Value generator for a UUID String format.
 * Results in Strings of length 16 characters, containing the IP address of the local machine as per the JDO spec section 18.6.1.
 */
public class UUIDStringGenerator extends AbstractUUIDGenerator
{
    /**
     * Constructor.
     * @param storeMgr StoreManager
     * @param name Symbolic name for this generator
     */
    public UUIDStringGenerator(StoreManager storeMgr, String name)
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
     * Create an identifier with the form "IIIIJJJJHHLLLLCC".
     * Where IIII is the IP address, JJJJ is something unique across JVMs,
     * HH is the High Time, LLLL is the low time, and CC is a count.
     * @return The identifier
     */
    protected String getIdentifier()
    {
        byte[] ipAddrBytes = getBytesFromInt(IP_ADDRESS);
        byte[] jvmBytes = getBytesFromInt(JVM_UNIQUE);
        short timeHigh = (short) (System.currentTimeMillis() >>> 32);
        byte[] timeHighBytes = getBytesFromShort(timeHigh);
        int timeLow = (int) System.currentTimeMillis();
        byte[] timeLowBytes = getBytesFromInt(timeLow);
        short count = getCount();
        byte[] countBytes = getBytesFromShort(count);

        byte[] bytes = new byte[16];
        int pos = 0;
        for (int i=0;i<4;i++)
        {
            bytes[pos++] = ipAddrBytes[i];
        }
        for (int i=0;i<4;i++)
        {
            bytes[pos++] = jvmBytes[i];
        }
        for (int i=0;i<2;i++)
        {
            bytes[pos++] = timeHighBytes[i];
        }
        for (int i=0;i<4;i++)
        {
            bytes[pos++] = timeLowBytes[i];
        }
        for (int i=0;i<2;i++)
        {
            bytes[pos++] = countBytes[i];
        }

        try
        {
            return new String(bytes, "ISO-8859-1");
        }
        catch (Exception e)
        {
            return new String(bytes);
        }
    }

    /**
     * Utility to convert an int into a byte array
     * @param val The int
     * @return The bytes
     */
    private static byte[] getBytesFromInt(int val)
    {
        byte[] arr = new byte[4];
        for (int i=3;i>=0;i--)
        {
            arr[i] = (byte) ((0xFFl & val) + Byte.MIN_VALUE);
            val >>>= 8;
        }
        return arr;
    }

    /**
     * Utility to convert a short into a a byte array
     * @param val The short
     * @return The bytes
     */
    private static byte[] getBytesFromShort(short val)
    {
        byte[] arr = new byte[2];
        for (int i=1;i>=0;i--)
        {
            arr[i] = (byte) ((0xFFl & val)  + Byte.MIN_VALUE);
            val >>>= 8;
        }
        return arr;
    }
}