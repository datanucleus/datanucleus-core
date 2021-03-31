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
    long txnExecutionTotalTime = 0;
    long txnExecutionTimeHigh =-1;
    long txnExecutionTimeLow =-1;
    SMA txnExecutionTimeAverage = new SMA(50);

    int queryActiveTotalCount;
    int queryErrorTotalCount;
    int queryExecutionTotalCount;
    long queryExecutionTotalTime = 0;
    long queryExecutionTimeHigh =-1;
    long queryExecutionTimeLow =-1;
    SMA queryExecutionTimeAverage = new SMA(50);

    /**
     * Constructor defining the manager.
     * If the manager is defined then this will generate a bean name that it is registered with in the manager.
     * @param mgmtManager The Management (JMX) Manager
     * @param parent Parent statistics object (optional)
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

    public String getRegisteredName()
    {
        return registeredName;
    }

    public int getQueryActiveTotalCount()
    {
        return queryActiveTotalCount;
    }

    public int getQueryErrorTotalCount()
    {
        return queryErrorTotalCount;
    }

    public int getQueryExecutionTotalCount()
    {
        return queryExecutionTotalCount;
    }

    public long getQueryExecutionTimeLow()
    {
        return queryExecutionTimeLow;
    }

    public long getQueryExecutionTimeHigh()
    {
        return queryExecutionTimeHigh;
    }

    public long getQueryExecutionTotalTime()
    {
        return queryExecutionTotalTime;
    }

    public long getQueryExecutionTimeAverage()
    {
        return (int) queryExecutionTimeAverage.currentAverage();
    }

    public void queryBegin()
    {
        this.queryActiveTotalCount++;
        if (parent != null)
        {
            parent.queryBegin();
        }
    }

    public void queryExecutedWithError()
    {
        this.queryErrorTotalCount++;
        this.queryActiveTotalCount--;
        if (parent != null)
        {
            parent.queryExecutedWithError();
        }
    }

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

    public int getNumberOfDatastoreWrites()
    {
        return numWrites;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.FactoryStatisticsMBean#getNumberOfDatastoreReads()
     */
    public int getNumberOfDatastoreReads()
    {
        return numReads;
    }

    public int getNumberOfDatastoreWritesInLatestTxn()
    {
        return numWritesLastTxn;
    }

    public int getNumberOfDatastoreReadsInLatestTxn()
    {
        return numReadsLastTxn;
    }

    public void incrementNumReads()
    {
        numReads++;
        if (parent != null)
        {
            parent.incrementNumReads();
        }
    }

    public void incrementNumWrites()
    {
        numWrites++;
        if (parent != null)
        {
            parent.incrementNumWrites();
        }
    }

    public int getNumberOfObjectFetches()
    {
        return fetchCount;
    }

    public int getNumberOfObjectInserts()
    {
        return insertCount;
    }

    public int getNumberOfObjectUpdates()
    {
        return updateCount;
    }

    public int getNumberOfObjectDeletes()
    {
        return deleteCount;
    }

    public void incrementInsertCount()
    {
        insertCount++;
        if (parent != null)
        {
            parent.incrementInsertCount();
        }
    }

    public void incrementDeleteCount()
    {
        deleteCount++;
        if (parent != null)
        {
            parent.incrementDeleteCount();
        }
    }

    public void incrementFetchCount()
    {
        fetchCount++;
        if (parent != null)
        {
            parent.incrementFetchCount();
        }
    }

    public void incrementUpdateCount()
    {
        updateCount++;
        if (parent != null)
        {
            parent.incrementUpdateCount();
        }
    }

    public long getTransactionExecutionTimeAverage()
    {
        return (int) txnExecutionTimeAverage.currentAverage();
    }

    public long getTransactionExecutionTimeLow()
    {
        return txnExecutionTimeLow;
    }

    public long getTransactionExecutionTimeHigh()
    {
        return txnExecutionTimeHigh;
    }

    public long getTransactionExecutionTotalTime()
    {
        return txnExecutionTotalTime;
    }

    public int getTransactionTotalCount()
    {
        return txnTotalCount;
    }

    public int getTransactionActiveTotalCount()
    {
        return txnActiveTotalCount;
    }

    public int getTransactionCommittedTotalCount()
    {
        return txnCommittedTotalCount;
    }

    public int getTransactionRolledBackTotalCount()
    {
        return txnRolledBackTotalCount;
    }

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
            values.addLast(Double.valueOf(value));
            average = sum / values.size();
            return average;
        }
    }
}