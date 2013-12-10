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
public class AUIDGenerator extends AbstractUIDGenerator
{
    /**
     * Constructor.
     */
    public AUIDGenerator(String name, Properties props)
    {
        super(name, props);
        allocationSize = 1;
    }

    /**
     * Accessor for a new identifier.
     * @return The identifier
     */
    protected String getIdentifier()
    {
        return new AUID().toString();
    }
}