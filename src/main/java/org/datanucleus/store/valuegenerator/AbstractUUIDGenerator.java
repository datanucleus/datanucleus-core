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

import java.net.InetAddress;
import java.util.Properties;

import org.datanucleus.util.TypeConversionHelper;

/**
 * Value generator for a UUID format. To be extended by implementations
 * giving the UUID in particular forms.
 */
public abstract class AbstractUUIDGenerator extends AbstractUIDGenerator
{
    /** IP Address of local machine. */
    static final int IP_ADDRESS;

    static 
    {
        // Calculate the IP address of this machine for use in the UUID
        int ipAddr = 0;
        try
        {
            ipAddr = TypeConversionHelper.getIntFromByteArray(InetAddress.getLocalHost().getAddress());
        }
        catch (Exception e)
        {
            ipAddr = 0;
        }

        IP_ADDRESS = ipAddr;
    }

    /** Unique value across JVMs on this machine. */
    static final int JVM_UNIQUE = (int) (System.currentTimeMillis() >>> 8);

    static short counter = (short) 0;

    /**
     * Constructor.
     * @param name Symbolic name for this generator
     * @param props Properties controlling its behaviour
     */
    public AbstractUUIDGenerator(String name, Properties props)
    {
        super(name, props);
    }

    /**
     * Simple counter for identities.
     * @return The next count value
     */
    protected short getCount() 
    {
        // We could move this to the individual implementing classes
        // so they have their own count
        synchronized(AbstractUUIDGenerator.class) 
        {
            if (counter < 0)
            {
                counter = 0;
            }
            return counter++;
        }
    }
}