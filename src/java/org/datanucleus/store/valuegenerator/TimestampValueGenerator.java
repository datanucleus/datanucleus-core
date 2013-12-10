/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import java.util.Calendar;
import java.util.Properties;

/**
 * Value generator for timestamp values (millisecs).
 * The "timestamps" are the number of milliseconds (since Jan 1 1970).
 */
public class TimestampValueGenerator extends AbstractGenerator
{
    /**
     * Constructor.
     * @param name Symbolic name of the generator
     * @param props Any properties controlling its behaviour.
     */
    public TimestampValueGenerator(String name, Properties props)
    {
        super(name, props);
    }

    /**
     * Method to reserve a block of values.
     * Only ever reserves a single timestamp, to the time at which it is created.
     * @param size Number of elements to reserve.
     * @return The block.
     */
    protected ValueGenerationBlock reserveBlock(long size)
    {
        Calendar cal = Calendar.getInstance();
        Long[] ids = new Long[1];
        ids[0] = Long.valueOf(cal.getTimeInMillis());
        ValueGenerationBlock block = new ValueGenerationBlock(ids);
        return block;
    }
}