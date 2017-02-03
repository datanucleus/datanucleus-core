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

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.plugin.PluginManager;

/**
 * Entry point for locating a JTA TransactionManager.
 */
public class TransactionManagerFinder
{
    /** The NucleusContext. */
    NucleusContext nucleusContext;

    /**
     * Constructor.
     * @param ctx Context for persistence
     */
    public TransactionManagerFinder(NucleusContext ctx)
    {
        nucleusContext = ctx;
    }

    /**
     * Accessor for the accessible JTA transaction manager.
     * @param clr ClassLoader resolver
     * @return The JTA manager found (if any)
     */
    public TransactionManager getTransactionManager(ClassLoaderResolver clr)
    {
        String jtaLocatorName = nucleusContext.getConfiguration().getStringProperty(PropertyNames.PROPERTY_TRANSACTION_JTA_LOCATOR);
        PluginManager pluginMgr = nucleusContext.getPluginManager();
        if ("autodetect".equalsIgnoreCase(jtaLocatorName) || jtaLocatorName == null)
        {
            // Cycle through all available locators and find one that returns a TransactionManager
            String[] locatorNames = pluginMgr.getAttributeValuesForExtension("org.datanucleus.jta_locator", null, null, "name");
            if (locatorNames != null)
            {
                for (int i=0;i<locatorNames.length;i++)
                {
                    try
                    {
                        TransactionManagerLocator locator = (TransactionManagerLocator)pluginMgr.createExecutableExtension(
                            "org.datanucleus.jta_locator", "name", locatorNames[i], 
                            "class-name", new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[] {nucleusContext});
                        if (locator != null)
                        {
                            TransactionManager tm = locator.getTransactionManager(clr);
                            if (tm != null)
                            {
                                return tm;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        // Ignore any errors
                    }
                }
            }
        }
        else
        {
            // User has specified which locator to use
            try
            {
                TransactionManagerLocator locator = (TransactionManagerLocator)pluginMgr.createExecutableExtension(
                        "org.datanucleus.jta_locator", "name", jtaLocatorName, 
                        "class-name", new Class[] {ClassConstants.NUCLEUS_CONTEXT}, new Object[] {nucleusContext});
                return locator.getTransactionManager(clr);
            }
            catch (Exception e)
            {
                // Ignore any errors
            }
        }
        return null;
    }
}