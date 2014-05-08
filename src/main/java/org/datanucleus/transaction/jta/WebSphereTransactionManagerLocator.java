/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.util.NucleusLogger;

/**
 * Locator for the TransactionManager for IBM WebsFear 4 and 5.
 */
public class WebSphereTransactionManagerLocator extends FactoryBasedTransactionManagerLocator
{
    Class factoryClass = null;

    /**
     * Constructor.
     * @param nucleusCtx the context this locator operates in
     */
    public WebSphereTransactionManagerLocator(NucleusContext nucleusCtx)
    {
        super();
    }

    /**
     * Method to return the factory class for this locator
     * @param clr ClassLoader resolver
     * @return The class to use (if present)
     */
    protected Class getFactoryClass(ClassLoaderResolver clr)
    {
        if (factoryClass != null)
        {
            return factoryClass;
        }

        try
        {
            // Find the TransactionManagerFactory class to use
            try
            {
                // WebSphere 5
                factoryClass = clr.classForName("com.ibm.ws.Transaction.TransactionManagerFactory");
            }
            catch (Exception e)
            {
                try
                {
                    // WebSphere 5
                    factoryClass = clr.classForName("com.ibm.ejs.jts.jta.TransactionManagerFactory");
                }
                catch (Exception e2)
                {
                    // WebSphere 4
                    factoryClass = clr.classForName("com.ibm.ejs.jts.jta.JTSXA");
                }
            }
        }
        catch (Exception e)
        {
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug("Exception finding Websphere transaction manager. " +
                    "Probably not in a Websphere environment " + e.getMessage());
            }
        }
        return factoryClass; 
    }
}