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
package org.datanucleus.management.jmx;

/**
 * Management Server for MBeans.
 * Plugin Extension Point: org.datanucleus.management_server
 * The implementation of this class must have a default public constructor
 */
public interface ManagementServer
{
    /**
     * Start the Management Server. If this operation is invoked while the server is started, 
     * this operation is ignored. This operation can also connect to a remote MBeanServer,
     * instead of creating a new MBeanServer instance. This depends on the configuration.
     */
    void start();

    /**
     * Stop the Management Server. If this operation is invoked while the server is stop, 
     * this operation is ignored. This operation can also disconnect from a remote MBeanServer,
     * instead of destroying a MBeanServer instance. This depends on the configuration.
     */
    void stop();

    /**
     * Register a MBean into the MBeanServer
     * @param mbean the MBean instance
     * @param name the mbean name
     */
    void registerMBean(Object mbean, String name);

    /**
     * Unregister a MBean from the MBeanServer
     * @param name the mbean name
     */
    void unregisterMBean(String name);
}