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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.Localiser;

/**
 * Locator for a JTA TransactionManager using JNDI context namings.
 * All extending classes must provide the method <i>getJNDIName()</i> returning a name that is then looked up
 * via JNDI to return the manager object.
 */
public abstract class JNDIBasedTransactionManagerLocator implements TransactionManagerLocator
{
    /** Localisation utility for output messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Accessor for the JNDI name to use.
     * @return The JNDI name where the txn manager is stored.
     */
    public abstract String getJNDIName();

    /**
     * Method to return the TransactionManager looking it up using JNDI.
     * @param clr ClassLoader Resolver
     * @return The TransactionManager
     */
    public TransactionManager getTransactionManager(ClassLoaderResolver clr)
    {
        try
        {
            InitialContext ctx = new InitialContext();
            try
            {
                return (TransactionManager) ctx.lookup(getJNDIName());
            }
            catch (Exception e)
            {
                return null;
            }
        }
        catch (NamingException ne)
        {
            // probably NoInitialContextException, other NamingExceptions due to bad names are silently caught above
            throw new NucleusException(LOCALISER.msg("015029"), ne);
        }
    }
}