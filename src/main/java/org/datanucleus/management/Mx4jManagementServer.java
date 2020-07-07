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

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NoSuchObjectException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import mx4j.tools.naming.NamingService;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.NucleusLogger;

/**
 * Wrapper for the MX4J JMX server.
 */
public class Mx4jManagementServer implements ManagementServer
{
    MBeanServer server;
    JMXConnectorServer jmxServer;
    NamingService naming;
   
    /**
     * Start the Management Server. If this operation is invoked
     * while the server is started, this operation is ignored.
     * This operation can also connect to a remote MBeanServer,
     * instead of creating a new MBeanServer instance. This depends
     * of the configuration.
     */
    public void start()
    {
        if (NucleusLogger.GENERAL.isDebugEnabled())
        {
            NucleusLogger.GENERAL.debug("Starting ManagementServer");
        }

        //set this as property
        int port = 1199;
         
        try
        {
            //create a naming (JNDI) service acessible via RMI
            naming = new NamingService(port);
            naming.start();

            // create a MBeanServer
            server = MBeanServerFactory.createMBeanServer();

            //bind the MBeanServer to JNDI tree
            String hostName = InetAddress.getLocalHost().getHostName();
            Map<String, String> env = new HashMap<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
            env.put(Context.PROVIDER_URL, "rmi://"+hostName+":"+port);

            // Start JMX server
            JMXServiceURL address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+hostName+":"+port+"/datanucleus");
            jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(address, env, server);
            jmxServer.start();
            if (NucleusLogger.GENERAL.isDebugEnabled())
            {
                NucleusLogger.GENERAL.debug("MBeanServer listening at " + jmxServer.getAddress().toString());
            }
        }
        catch (Exception e)
        {
            throw new NucleusException(e.getMessage(),e);
        }
    }
    
    /**
     * Stop the Management Server. If this operation is invoked
     * while the server is stop, this operation is ignored 
     * This operation can also disconnect from a remote MBeanServer,
     * instead of destroying a MBeanServer instance. This depends
     * of the configuration.
     */
    public void stop()
    {
        if (NucleusLogger.GENERAL.isDebugEnabled())
        {
            NucleusLogger.GENERAL.debug("Stopping ManagementServer");
        }
        if (jmxServer != null)
        {
            try
            {
                jmxServer.stop();
            }
            catch (IOException e)
            {
                NucleusLogger.GENERAL.error(e);
            }
        }
        if (naming != null)
        {
            try
            {
                naming.stop();
            }
            catch (NoSuchObjectException e)
            {
                NucleusLogger.GENERAL.error(e);
            }
        }
        jmxServer = null;
        naming = null;
        server = null;
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
            server.registerMBean(mbean, objName);
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
            server.unregisterMBean(objName);
        }
        catch (Exception e)
        {
            throw new NucleusException(e.getMessage(),e);
        }
    }

    public Object getMBeanServer()
    {
        return server;
    }
}