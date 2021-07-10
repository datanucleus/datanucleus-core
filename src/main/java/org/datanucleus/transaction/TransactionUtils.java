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
package org.datanucleus.transaction;

import java.util.Collection;

import org.datanucleus.exceptions.TransactionIsolationNotSupportedException;
import org.datanucleus.store.StoreManager;

/**
 * Utility methods relating to transactions.
 */
public class TransactionUtils
{
    /**
     * Method to return the transaction isolation level that will be used for the provided StoreManager
     * bearing in mind the specified level the user requested.
     * @param storeMgr The Store Manager
     * @param transactionIsolation Requested isolation level
     * @return Isolation level to use
     * @throws TransactionIsolationNotSupportedException When no suitable level available given the requested level
     */
    public static String getTransactionIsolationForStoreManager(StoreManager storeMgr, String transactionIsolation)
    {
        if (transactionIsolation != null)
        {
            // Transaction isolation has been specified and we need to provide at least this level
            // Order of priority is :-
            // read-uncommitted (lowest), read-committed, repeatable-read, serializable (highest)
            Collection<String> supportedOptions = storeMgr.getSupportedOptions();
            if (!supportedOptions.contains("TransactionIsolationLevel." + transactionIsolation))
            {
                // Requested transaction isolation isn't supported by datastore so check for higher
                if (transactionIsolation.equals("read-uncommitted"))
                {
                    if (supportedOptions.contains(StoreManager.OPTION_TXN_ISOLATION_READ_COMMITTED))
                    {
                        return "read-committed";
                    }
                    else if (supportedOptions.contains(StoreManager.OPTION_TXN_ISOLATION_REPEATABLE_READ))
                    {
                        return "repeatable-read";
                    }
                    else if (supportedOptions.contains(StoreManager.OPTION_TXN_ISOLATION_SERIALIZABLE))
                    {
                        return "serializable";
                    }
                }
                else if (transactionIsolation.equals("read-committed"))
                {
                    if (supportedOptions.contains(StoreManager.OPTION_TXN_ISOLATION_REPEATABLE_READ))
                    {
                        return "repeatable-read";
                    }
                    else if (supportedOptions.contains(StoreManager.OPTION_TXN_ISOLATION_SERIALIZABLE))
                    {
                        return "serializable";
                    }
                }
                else if (transactionIsolation.equals("repeatable-read"))
                {
                    if (supportedOptions.contains(StoreManager.OPTION_TXN_ISOLATION_SERIALIZABLE))
                    {
                        return "serializable";
                    }
                }
                else
                {
                    throw new TransactionIsolationNotSupportedException(transactionIsolation);
                }
            }
        }
        return transactionIsolation;
    }

    /**
     * Accessor for a string name of a transaction isolation level.
     * @param isolation The isolation level (as defined by UserTransaction).
     * @return The name
     */
    public static String getNameForTransactionIsolationLevel(int isolation)
    {
        if (isolation == TransactionIsolation.NONE)
        {
            return "none";
        }
        else if (isolation == TransactionIsolation.READ_COMMITTED)
        {
            return "read-committed";
        }
        else if (isolation == TransactionIsolation.READ_UNCOMMITTED)
        {
            return "read-uncommitted";
        }
        else if (isolation == TransactionIsolation.REPEATABLE_READ)
        {
            return "repeatable-read";
        }
        else if (isolation == TransactionIsolation.SERIALIZABLE)
        {
            return "serializable";
        }
        else
        {
            return "UNKNOWN";
        }
    }

    /**
     * Convenience method to convert the supplied isolation level name into the
     * associated UserTransaction type number.
     * @param isolationName The name of the isolation level
     * @return Isolation level type
     */
    public static int getTransactionIsolationLevelForName(String isolationName)
    {
        if (isolationName.equalsIgnoreCase("none"))
        {
            return TransactionIsolation.NONE;
        }
        else if (isolationName.equalsIgnoreCase("read-committed"))
        {
            return TransactionIsolation.READ_COMMITTED;
        }
        else if (isolationName.equalsIgnoreCase("read-uncommitted"))
        {
            return TransactionIsolation.READ_UNCOMMITTED;
        }
        else if (isolationName.equalsIgnoreCase("repeatable-read"))
        {
            return TransactionIsolation.REPEATABLE_READ;
        }
        else if (isolationName.equalsIgnoreCase("serializable"))
        {
            return TransactionIsolation.SERIALIZABLE;
        }
        else
        {
            return -1;
        }
    }
}