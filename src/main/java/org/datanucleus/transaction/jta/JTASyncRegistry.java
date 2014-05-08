/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.transaction.jta;

import javax.naming.InitialContext;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * Wrapper around TransactionSynchronizationRegistry (only present JTA1.1+).
 * Grabs hold of the TransactionSynchronizationRegistry (if available), and allows registration of a Synchronization.
 */
public class JTASyncRegistry
{
    TransactionSynchronizationRegistry registry;

    public JTASyncRegistry()
    throws JTASyncRegistryUnavailableException
    {
        try
        {
            InitialContext ctx = new InitialContext();
            registry = (TransactionSynchronizationRegistry) ctx.lookup("java:comp/TransactionSynchronizationRegistry");
        }
        catch (Throwable thr)
        {
            throw new JTASyncRegistryUnavailableException();
        }
    }

    public void register(Synchronization sync)
    {
        registry.registerInterposedSynchronization(sync);
    }
}