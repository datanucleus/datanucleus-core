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
package org.datanucleus.store.valuegenerator;

import java.util.Properties;
import java.util.UUID;

import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Value generator for a String utilising the JDK UUID class (128-bit, 36 character).
 * Results in Strings of length 36 characters, like "2cdb8cee-9134-453f-9d7a-14c0ae8184c6".
 */
public class UUIDGenerator extends AbstractGenerator<String>
{
    /**
     * Constructor.
     * @param storeMgr StoreManager
     * @param name Symbolic name for this generator
     * @param props Properties controlling its behaviour
     */
    public UUIDGenerator(StoreManager storeMgr, String name, Properties props)
    {
        super(storeMgr, name, props);
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
     * Method to reserve "size" values to the block.
     * @param size The block size
     * @return The reserved block
     */
    protected ValueGenerationBlock<String> reserveBlock(long size)
    {
        Object[] ids = new Object[(int) size];
        for (int i = 0; i < size; i++)
        {
            ids[i] = UUID.randomUUID().toString();
        }
        if (NucleusLogger.VALUEGENERATION.isDebugEnabled())
        {
            NucleusLogger.VALUEGENERATION.debug(Localiser.msg("040004", "" + size));
        }
        return new ValueGenerationBlock(ids);
    }
}