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

import javax.transaction.TransactionManager;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * TransactionManager locator using a factory class.
 * All extending classes must provide the method <i>getFactoryClass()</i> returning the class of a factory
 * that has a method "getTransactionManager" returning the manager object.
 */
public abstract class FactoryBasedTransactionManagerLocator implements TransactionManagerLocator
{
    /** Localisation utility for output messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Accessor for the factory class to use for this locator.
     * @param clr ClassLoader resolver
     * @return The class
     */
    protected abstract Class getFactoryClass(ClassLoaderResolver clr);

    /**
     * Method to return the TransactionManager.
     * @return The TransactionManager
     */
    public TransactionManager getTransactionManager(ClassLoaderResolver clr)
    {
        Class factoryClass = getFactoryClass(clr);
        if (factoryClass == null)
        {
            return null;
        }
    
        try
        {
            return (TransactionManager) factoryClass.getMethod("getTransactionManager").invoke(null);
        }
        catch (Exception e)
        {
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug("Exception finding FactoryBased transaction manager " + e.getMessage());
            }
        }
        return null;
    }
}