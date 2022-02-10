/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved. 
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ExecutionContext;

/**
 * TransactionManager provides a facade for creating (Open/XA) transactions.
 * A cache of transactions is held with each transaction for a user object.
 * If using with a multithreaded PM/EM then you must lock access external to TransactionManager since this is for a PMF/EMF.
 * TODO Consider merging this into org.datanucleus.transaction.TransactionImpl.
 */
public class ResourcedTransactionManager
{
    private boolean containerManagedConnections = false;

    /** Map of transaction keyed by the ExecutionContext that it is for. */
    private Map<ExecutionContext, ResourcedTransaction> txnForExecutionContext = new ConcurrentHashMap<ExecutionContext, ResourcedTransaction>();

    public void setContainerManagedConnections(boolean flag)
    {
        containerManagedConnections = flag;
    }

    public void begin(ExecutionContext ec)
    {
        if (txnForExecutionContext.get(ec) != null)
        {
            throw new NucleusTransactionException("Invalid state. Transaction has already started");
        }
        txnForExecutionContext.put(ec, new ResourcedTransaction());
    }

    public void commit(ExecutionContext ec)
    {
        ResourcedTransaction tx = txnForExecutionContext.get(ec);
        if (tx == null)
        {
            throw new NucleusTransactionException("Invalid state. Transaction does not exist");
        }

        try
        {
            if (!containerManagedConnections) 
            {
                tx.commit();
            }
        }
        finally
        {
            txnForExecutionContext.remove(ec);
        }
    }

    public void rollback(ExecutionContext ec)
    {
        ResourcedTransaction tx = txnForExecutionContext.get(ec);
        if (tx == null)
        {
            throw new NucleusTransactionException("Invalid state. Transaction does not exist");
        }

        try
        {
            if (!containerManagedConnections) 
            {
                tx.rollback();
            }
        }
        finally
        {
            txnForExecutionContext.remove(ec);
        }
    }

    public ResourcedTransaction getTransaction(ExecutionContext ec)
    {
        if (ec == null)
        {
            return null;
        }
        return txnForExecutionContext.get(ec);
    }

    public void setRollbackOnly(ExecutionContext ec)
    {
        ResourcedTransaction tx = txnForExecutionContext.get(ec);
        if (tx == null)
        {
            throw new NucleusTransactionException("Invalid state. Transaction does not exist");
        }
        tx.setRollbackOnly();
    }

    public void setTransactionTimeout(ExecutionContext ec, int millis)
    {
        throw new UnsupportedOperationException();        
    }

    public void resume(ExecutionContext ec, ResourcedTransaction tx)
    {
        throw new UnsupportedOperationException();        
    }

    public ResourcedTransaction suspend(ExecutionContext ec)
    {
        throw new UnsupportedOperationException();        
    }
}