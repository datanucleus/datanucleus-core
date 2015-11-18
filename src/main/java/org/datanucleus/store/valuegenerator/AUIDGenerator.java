/**********************************************************************
Copyright (c) 2004 Ralf Ullrich and others. All rights reserved. 
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

import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * This generator uses a Java implementation of DCE UUIDs to create unique
 * identifiers without the overhead of additional database transactions or even
 * an open database connection. The identifiers are Strings of the form
 * "LLLLLLLL-MMMM-HHHH-CCCC-NNNNNNNNNNNN" where 'L', 'M', 'H', 'C' and 'N' are
 * the DCE UUID fields named time low, time mid, time high, clock sequence and
 * node.
 * <p>
 * This generator can be used in concurrent applications. It is especially
 * useful in situations where large numbers of transactions within a certain
 * amount of time have to be made, and the additional overhead of synchronizing
 * the concurrent creation of unique identifiers through the database would
 * break performance limits.
 * </p>
 * <p>
 * There are no properties for this ValueGenerator.
 * </p>
 * <p>
 * Note: Due to limitations of the available Java API there is a chance of less
 * than 1:2^62 that two concurrently running JVMs will produce the same
 * identifiers, which is in practical terms never, because your database server
 * will have crashed a million times before this happens.
 * </p>
 */
public class AUIDGenerator extends AbstractGenerator<String>
{
    public AUIDGenerator(String name, Properties props)
    {
        super(name, props);
        allocationSize = 1;
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
            ids[i] = new AUID().toString();
        }
        if (NucleusLogger.VALUEGENERATION.isDebugEnabled())
        {
            NucleusLogger.VALUEGENERATION.debug(Localiser.msg("040004", "" + size));
        }
        return new ValueGenerationBlock(ids);
    }
}