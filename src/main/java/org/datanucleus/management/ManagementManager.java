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

import org.datanucleus.NucleusContext;
import org.datanucleus.NucleusContextHelper;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Management interface for DataNucleus.
 * Management operations and attributes are exposed through this interface that holds statistics linked to a NucleusContext instance.
 * <p>
 * The mechanics for starting and stopping JMX servers are not defined here, and must be done by plug-ins, by providing the implementation of {@link ManagementServer}.
 * This Manager controls the lifecycle of management servers.
 * A management server is started when an instance of this class is created, and its shutdown when the close operation is invoked
 * The management server startup is triggered when the Manager gets enabled.
 * </p>
 */
public class ManagementManager
{
    /** NucleusContext that we are managing. **/
    final private NucleusContext nucleusContext;
    
    /** Whether this is closed. **/
    private boolean closed = false;

    /** The Management Server. **/
    private ManagementServer mgmtServer;

    /** Domain name for this configuration. **/
    private String domainName;

    /** Instance name for this configuration. **/
    private String instanceName;

    /**
     * Constructor for Management.
     * @param ctxt the NucleusContext that we are operating for
     */
    public ManagementManager(NucleusContext ctxt)
    {
        this.nucleusContext = ctxt;

        this.domainName = ctxt.getConfiguration().getStringProperty("datanucleus.PersistenceUnitName");
        if (this.domainName == null)
        {
            this.domainName = "datanucleus";
        }
        this.instanceName = "datanucleus-" + NucleusContextHelper.random.nextInt();

        startManagementServer();
    }

    /**
     * Instance name for this manager instance.
     * @return Instance name
     */
    public String getInstanceName()
    {
        return instanceName;
    }

    /**
     * Domain name for this manager instance.
     * @return Domain name
     */
    public String getDomainName()
    {
        return domainName;
    }

    /**
     * Register an MBean into the MBeanServer
     * @param mbean the MBean instance
     * @param name the mbean name
     */
    public void registerMBean(Object mbean, String name)
    {
        mgmtServer.registerMBean(mbean, name);
    }

    /**
     * Deregister an MBean from the MBeanServer
     * @param name the mbean name
     */
    public void deregisterMBean(String name)
    {
        mgmtServer.unregisterMBean(name);
    }

    /**
     * Whether this Manager is not closed
     * @return true if this Manager is open
     */
    public boolean isOpen()
    {
        return !closed;
    }

    /**
     * Close a instance.
     * @throws NucleusException if the manager is closed
     */
    public synchronized void close()
    {
        assertNotClosed();
        stopManagementServer();
        this.closed = true;
    }

    /**
     * Assert that this instance is open
     */
    private void assertNotClosed()
    {
        if (this.closed)
        {
            throw new NucleusException("Management instance is closed and cannot be used. You must acquire a new context").setFatal();
        }
    }

    /**
     * Start Management Server
     */
    private void startManagementServer()
    {
        if (mgmtServer == null)
        {
            String jmxType = nucleusContext.getConfiguration().getStringProperty(PropertyNames.PROPERTY_JMX_TYPE);
            try
            {
                if (jmxType != null)
                {
                    // TODO Remove "default"
                    if (jmxType.equals("platform") || jmxType.equals("default"))
                    {
                        mgmtServer = new PlatformManagementServer();
                    }
                    else if (jmxType.equals("mx4j"))
                    {
                        mgmtServer = new Mx4jManagementServer();
                    }
                }
                if (mgmtServer == null)
                {
                    NucleusLogger.GENERAL.error("Could not start management server of type " + jmxType + " since not found");
                }
                else
                {
                    NucleusLogger.GENERAL.info("Starting Management Server");
                    mgmtServer.start();
                }
            }
            catch (Exception e)
            {
                mgmtServer = null;
                NucleusLogger.GENERAL.error("Error instantiating or connecting to Management Server : " + StringUtils.getStringFromStackTrace(e));
            }
        }
    }

    /**
     * Shutdown Management Server
     */
    private void stopManagementServer()
    {
        if (mgmtServer != null)
        {
            NucleusLogger.GENERAL.info("Stopping Management Server");
            mgmtServer.stop();
        }
    }
}