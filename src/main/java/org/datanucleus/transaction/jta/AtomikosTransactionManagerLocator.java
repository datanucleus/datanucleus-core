/**********************************************************************
Copyright (c) 2011 Matthew Adams and others. All rights reserved.
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
2011 Andy Jefferson - don't reference atomikos directly, just use reflection
   ...
**********************************************************************/
package org.datanucleus.transaction.jta;

import javax.transaction.TransactionManager;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.util.NucleusLogger;

/**
 * Locator for the TransactionManager for Atomikos.
 */
public class AtomikosTransactionManagerLocator implements TransactionManagerLocator
{
    /**
     * Constructor.
     * @param nucleusCtx the context this locator operates in
     */
    public AtomikosTransactionManagerLocator(NucleusContext nucleusCtx)
    {
        super();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.jta.TransactionManagerLocator#getTransactionManager(org.datanucleus.ClassLoaderResolver)
     */
    public TransactionManager getTransactionManager(ClassLoaderResolver clr)
    {
        Class cls = clr.classForName("com.atomikos.icatch.jta.UserTransactionManager");
        try
        {
            return (TransactionManager) cls.newInstance();
        }
        catch (Exception e)
        {
            NucleusLogger.TRANSACTION.debug("Exception obtaining Atomikos transaction manager " + e.getMessage());
            return null;
        }
    }
}