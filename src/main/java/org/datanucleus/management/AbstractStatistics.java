/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.management;

import java.util.LinkedList;

import org.datanucleus.NucleusContextHelper;

/**
 * Abstract base class for a statistics object.
 */
public abstract class AbstractStatistics
{
    /** Manager for the management (JMX) service. */
    ManagementManager manager;

    /** Name that we are known by. Will be null unless the JMX manager is not null. */
    String registeredName = null;

    /** Parent for this object. */
    AbstractStatistics parent = null;

    int numReads = 0;
    int numWrites = 0;
    int numReadsLastTxn = 0;
    int numWritesLastTxn = 0;

    int numReadsStartTxn = 0; // Work variable
    int numWritesStartTxn = 0; // Work variable

    int insertCount = 0;
    int deleteCount = 0;
    int updateCount = 0;
    int fetchCount = 0;

    int txnTotalCount;
    int txnCommittedTotalCount;
    int txnRolledBackTotalCount;
    int txnActiveTotalCount;
    int txnExecutionTotalTime = 0;
    int txnExecutionTimeHigh =-1;
    int txnExecutionTimeLow =-1;
    SMA txnExecutionTimeAverage = new SMA(50);

    int queryActiveTotalCount;
    int queryErrorTotalCount;
    int queryExecutionTotalCount;
    int queryExecutionTotalTime = 0;
    int queryExecutionTimeHigh =-1;
    int queryExecutionTimeLow =-1;
    SMA queryExecutionTimeAverage = new SMA(50);

    /**
     * Constructor defining the manager.
     * If the manager is defined then this will generate a bean name that it is registered with in the manager.
     * @param mgmtManager The Management (JMX) Manager
     */
    public AbstractStatistics(ManagementManager mgmtManager, AbstractStatistics parent)
    {
        this.manager = mgmtManager;
        this.parent = parent;

        if (mgmtManager != null)
        {
            // Register the MBean with the active JMX manager
            registeredName = manager.getDomainName() + ":InstanceName=" + manager.getInstanceName() + ",Type=" + this.getClass().getName() + ",Name=Manager" + 
                NucleusContextHelper.random.nextLong();
            mgmtManager.registerMBean(this, registeredName);
        }
    }

    public void close()
    {
        if (manager != null)
        {
            manager.deregisterMBean(registeredName);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getRegisteredName()
     */
    public String getRegisteredName()
    {
        return registeredName;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getQueryActiveTotalCount()
     */
    public int getQueryActiveTotalCount()
    {
        return queryActiveTotalCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getQueryErrorTotalCount()
     */
    public int getQueryErrorTotalCount()
    {
        return queryErrorTotalCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getQueryExecutionTotalCount()
     */
    public int getQueryExecutionTotalCount()
    {
        return queryExecutionTotalCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getQueryExecutionTimeLow()
     */
    public int getQueryExecutionTimeLow()
    {
        return queryExecutionTimeLow;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getQueryExecutionTimeHigh()
     */
    public int getQueryExecutionTimeHigh()
    {
        return queryExecutionTimeHigh;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getQueryExecutionTotalTime()
     */
    public int getQueryExecutionTotalTime()
    {
        return queryExecutionTotalTime;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getQueryExecutionTimeAverage()
     */
    public int getQueryExecutionTimeAverage()
    {
        return (int) queryExecutionTimeAverage.currentAverage();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#queryBegin()
     */
    public void queryBegin()
    {
        this.queryActiveTotalCount++;
        if (parent != null)
        {
            parent.queryBegin();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#queryExecutedWithError()
     */
    public void queryExecutedWithError()
    {
        this.queryErrorTotalCount++;
        this.queryActiveTotalCount--;
        if (parent != null)
        {
            parent.queryExecutedWithError();
        }
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#queryExecuted(long)
     */
    public void queryExecuted(long executionTime)
    {
        this.queryExecutionTotalCount++;
        this.queryActiveTotalCount--;
        queryExecutionTimeAverage.compute(executionTime);
        queryExecutionTimeLow = (int) Math.min(queryExecutionTimeLow==-1?executionTime:queryExecutionTimeLow,executionTime);
        queryExecutionTimeHigh = (int) Math.max(queryExecutionTimeHigh,executionTime);
        queryExecutionTotalTime += executionTime;
        if (parent != null)
        {
            parent.queryExecuted(executionTime);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getNumberOfDatastoreWrites()
     */
    public int getNumberOfDatastoreWrites()
    {
        return numWrites;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getNumberOfDatastoreReads()
     */
    public int getNumberOfDatastoreReads()
    {
        return numReads;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getNumberOfDatastoreWritesInLatestTxn()
     */
    public int getNumberOfDatastoreWritesInLatestTxn()
    {
        return numWritesLastTxn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getNumberOfDatastoreReadsInLatestTxn()
     */
    public int getNumberOfDatastoreReadsInLatestTxn()
    {
        return numReadsLastTxn;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#incrementNumReads()
     */
    public void incrementNumReads()
    {
        numReads++;
        if (parent != null)
        {
            parent.incrementNumReads();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#incrementNumWrites()
     */
    public void incrementNumWrites()
    {
        numWrites++;
        if (parent != null)
        {
            parent.incrementNumWrites();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getNumberOfObjectFetches()
     */
    public int getNumberOfObjectFetches()
    {
        return fetchCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getNumberOfObjectInserts()
     */
    public int getNumberOfObjectInserts()
    {
        return insertCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getNumberOfObjectUpdates()
     */
    public int getNumberOfObjectUpdates()
    {
        return updateCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getNumberOfObjectDeletes()
     */
    public int getNumberOfObjectDeletes()
    {
        return deleteCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#incrementInsertCount()
     */
    public void incrementInsertCount()
    {
        insertCount++;
        if (parent != null)
        {
            parent.incrementInsertCount();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#incrementDeleteCount()
     */
    public void incrementDeleteCount()
    {
        deleteCount++;
        if (parent != null)
        {
            parent.incrementDeleteCount();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#incrementFetchCount()
     */
    public void incrementFetchCount()
    {
        fetchCount++;
        if (parent != null)
        {
            parent.incrementFetchCount();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#incrementUpdateCount()
     */
    public void incrementUpdateCount()
    {
        updateCount++;
        if (parent != null)
        {
            parent.incrementUpdateCount();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getTransactionExecutionTimeAverage()
     */
    public int getTransactionExecutionTimeAverage()
    {
        return (int) txnExecutionTimeAverage.currentAverage();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getTransactionExecutionTimeLow()
     */
    public int getTransactionExecutionTimeLow()
    {
        return txnExecutionTimeLow;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getTransactionExecutionTimeHigh()
     */
    public int getTransactionExecutionTimeHigh()
    {
        return txnExecutionTimeHigh;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getTransactionExecutionTotalTime()
     */
    public int getTransactionExecutionTotalTime()
    {
        return txnExecutionTotalTime;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getTransactionTotalCount()
     */
    public int getTransactionTotalCount()
    {
        return txnTotalCount;
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getTransactionActiveTotalCount()
     */
    public int getTransactionActiveTotalCount()
    {
        return txnActiveTotalCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getTransactionCommittedTotalCount()
     */
    public int getTransactionCommittedTotalCount()
    {
        return txnCommittedTotalCount;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#getTransactionRolledBackTotalCount()
     */
    public int getTransactionRolledBackTotalCount()
    {
        return txnRolledBackTotalCount;
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#transactionCommitted(long)
     */
    public void transactionCommitted(long executionTime)
    {
        this.txnCommittedTotalCount++;
        this.txnActiveTotalCount--;
        txnExecutionTimeAverage.compute(executionTime);
        txnExecutionTimeLow = (int) Math.min(txnExecutionTimeLow==-1?executionTime:txnExecutionTimeLow,executionTime);
        txnExecutionTimeHigh = (int) Math.max(txnExecutionTimeHigh,executionTime);
        txnExecutionTotalTime += executionTime;

        numReadsLastTxn = numReads - numReadsStartTxn;
        numWritesLastTxn = numWrites - numWritesStartTxn;
        if (parent != null)
        {
            parent.transactionCommitted(executionTime);
        }
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#transactionRolledBack(long)
     */
    public void transactionRolledBack(long executionTime)
    {
        this.txnRolledBackTotalCount++;
        this.txnActiveTotalCount--;
        txnExecutionTimeAverage.compute(executionTime);
        txnExecutionTimeLow = (int) Math.min(txnExecutionTimeLow==-1?executionTime:txnExecutionTimeLow,executionTime);
        txnExecutionTimeHigh = (int) Math.max(txnExecutionTimeHigh,executionTime);
        txnExecutionTotalTime += executionTime;

        numReadsLastTxn = numReads - numReadsStartTxn;
        numWritesLastTxn = numWrites - numWritesStartTxn;
        if (parent != null)
        {
            parent.transactionRolledBack(executionTime);
        }
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.management.AbstractStats#transactionStarted()
     */
    public void transactionStarted()
    {
        this.txnTotalCount++;
        this.txnActiveTotalCount++;

        numReadsStartTxn = numReads;
        numWritesStartTxn = numWrites;
        if (parent != null)
        {
            parent.transactionStarted();
        }
    }

    /**
     * Simple Moving Average
     */
    public static class SMA
    {
        private LinkedList values = new LinkedList();

        private int length;

        private double sum = 0;

        private double average = 0;
        
        /**
         * 
         * @param length the maximum length
         */
        public SMA(int length)
        {
            if (length <= 0)
            {
                throw new IllegalArgumentException("length must be greater than zero");
            }
            this.length = length;
        }

        public double currentAverage()
        {
            return average;
        }

        /**
         * Compute the moving average.
         * Synchronised so that no changes in the underlying data is made during calculation.
         * @param value The value
         * @return The average
         */
        public synchronized double compute(double value)
        {
            if (values.size() == length && length > 0)
            {
                sum -= ((Double) values.getFirst()).doubleValue();
                values.removeFirst();
            }
            sum += value;
            values.addLast(new Double(value));
            average = sum / values.size();
            return average;
        }
    }
}