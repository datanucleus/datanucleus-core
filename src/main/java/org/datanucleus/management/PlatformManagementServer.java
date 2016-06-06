/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved.
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
***********************************************************************/
package org.datanucleus.management;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.management.ManagementServer;
import org.datanucleus.util.NucleusLogger;

/**
 * Wrapper for the JRE "Platform" JMX server.
 */
public class PlatformManagementServer implements ManagementServer
{
    MBeanServer mbeanServer;

    public void start()
    {
        if (NucleusLogger.GENERAL.isDebugEnabled())
        {
            NucleusLogger.GENERAL.debug("Starting ManagementServer");
        }

        mbeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    public void stop()
    {
        if (NucleusLogger.GENERAL.isDebugEnabled())
        {
            NucleusLogger.GENERAL.debug("Stopping ManagementServer");
        }
        mbeanServer = null;
    }

    /**
     * Register a MBean into the MBeanServer
     * @param mbean the MBean instance
     * @param name the mbean name
     */
    public void registerMBean(Object mbean, String name)
    {
        try
        {
            ObjectName objName = new ObjectName(name);
            mbeanServer.registerMBean(mbean, objName);
        }
        catch (Exception e)
        {
            throw new NucleusException(e.getMessage(),e);
        }
    }
    
    /**
     * Unregister a MBean from the MBeanServer
     * @param name the mbean name
     */
    public void unregisterMBean(String name)
    {
        try
        {
            ObjectName objName = new ObjectName(name);
            mbeanServer.unregisterMBean(objName);
        }
        catch (Exception e)
        {
            throw new NucleusException(e.getMessage(),e);
        }
    }
}