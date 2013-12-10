/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.util.NucleusLogger;

/**
 * Value generator for a UID format. 
 * To be extended by implementations giving the UID in particular forms.
 */
public abstract class AbstractUIDGenerator extends AbstractGenerator
{
    /**
     * Constructor.
     * @param name Symbolic name for this generator
     * @param props Properties controlling its behaviour
     */
    public AbstractUIDGenerator(String name, Properties props)
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
    protected ValueGenerationBlock reserveBlock(long size)
    {
        Object[] ids = new Object[(int) size];
        for (int i = 0; i < size; i++)
        {
            ids[i] = getIdentifier();
        }
        if (NucleusLogger.VALUEGENERATION.isDebugEnabled())
        {
            NucleusLogger.VALUEGENERATION.debug(LOCALISER.msg("040004", "" + size));
        }
        return new ValueGenerationBlock(ids);
    }

    /**
     * Create an identifier in the UID format required.
     * @return The identifier
     */
    protected abstract String getIdentifier();
}