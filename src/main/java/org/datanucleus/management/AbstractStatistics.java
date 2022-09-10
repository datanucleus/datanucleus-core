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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    final AtomicInteger numReads = new AtomicInteger();
    final AtomicInteger numWrites = new AtomicInteger();
    final AtomicInteger numReadsLastTxn = new AtomicInteger();
    final AtomicInteger numWritesLastTxn = new AtomicInteger();

    final AtomicInteger numReadsStartTxn = new AtomicInteger(); // Work variable
    final AtomicInteger numWritesStartTxn = new AtomicInteger(); // Work variable

    final AtomicInteger insertCount = new AtomicInteger();
    final AtomicInteger deleteCount = new AtomicInteger();
    final AtomicInteger updateCount = new AtomicInteger();
    final AtomicInteger fetchCount = new AtomicInteger();

    final AtomicInteger txnTotalCount = new AtomicInteger();
    final AtomicInteger txnCommittedTotalCount = new AtomicInteger();
    final AtomicInteger txnRolledBackTotalCount = new AtomicInteger();
    final AtomicInteger txnActiveTotalCount = new AtomicInteger();
    final AtomicLong txnExecutionTotalTime = new AtomicLong();
    final AtomicLong txnExecutionTimeHigh = new AtomicLong(-1);
    final AtomicLong txnExecutionTimeLow = new AtomicLong(-1);
    SMA txnExecutionTimeAverage = new SMA(50);

    final AtomicInteger queryActiveTotalCount = new AtomicInteger();
    final AtomicInteger queryErrorTotalCount = new AtomicInteger();
    final AtomicInteger queryExecutionTotalCount = new AtomicInteger();
    final AtomicLong queryExecutionTotalTime = new AtomicLong();
    final AtomicLong queryExecutionTimeHigh = new AtomicLong(-1);
    final AtomicLong queryExecutionTimeLow = new AtomicLong(-1);
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
                ManagementManager.random.nextLong();
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
        return queryActiveTotalCount.intValue();
    }

    public int getQueryErrorTotalCount()
    {
        return queryErrorTotalCount.intValue();
    }

    public int getQueryExecutionTotalCount()
    {
        return queryExecutionTotalCount.intValue();
    }

    public long getQueryExecutionTimeLow()
    {
        return queryExecutionTimeLow.longValue();
    }

    public long getQueryExecutionTimeHigh()
    {
        return queryExecutionTimeHigh.longValue();
    }

    public long getQueryExecutionTotalTime()
    {
        return queryExecutionTotalTime.longValue();
    }

    public long getQueryExecutionTimeAverage()
    {
        return (int) queryExecutionTimeAverage.currentAverage();
    }

    public void queryBegin()
    {
        this.queryActiveTotalCount.incrementAndGet();
        if (parent != null)
        {
            parent.queryBegin();
        }
    }

    public void queryExecutedWithError()
    {
        this.queryErrorTotalCount.incrementAndGet();
        this.queryActiveTotalCount.decrementAndGet();
        if (parent != null)
        {
            parent.queryExecutedWithError();
        }
    }

    public void queryExecuted(long executionTime)
    {
        this.queryExecutionTotalCount.incrementAndGet();
        this.queryActiveTotalCount.decrementAndGet();
        queryExecutionTimeAverage.compute(executionTime);
        queryExecutionTimeLow.accumulateAndGet(executionTime, (prev, x) -> {
            if (prev == -1) {
                return x;
            }
            return Math.min(prev, x);
        });
        queryExecutionTimeHigh.accumulateAndGet(executionTime, Math::max);
        queryExecutionTotalTime.addAndGet(executionTime);
        if (parent != null)
        {
            parent.queryExecuted(executionTime);
        }
    }

    public int getNumberOfDatastoreWrites()
    {
        return numWrites.intValue();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.management.FactoryStatisticsMBean#getNumberOfDatastoreReads()
     */
    public int getNumberOfDatastoreReads()
    {
        return numReads.intValue();
    }

    public int getNumberOfDatastoreWritesInLatestTxn()
    {
        return numWritesLastTxn.intValue();
    }

    public int getNumberOfDatastoreReadsInLatestTxn()
    {
        return numReadsLastTxn.intValue();
    }

    public void incrementNumReads()
    {
        numReads.incrementAndGet();
        if (parent != null)
        {
            parent.incrementNumReads();
        }
    }

    public void incrementNumWrites()
    {
        numWrites.incrementAndGet();
        if (parent != null)
        {
            parent.incrementNumWrites();
        }
    }

    public int getNumberOfObjectFetches()
    {
        return fetchCount.intValue();
    }

    public int getNumberOfObjectInserts()
    {
        return insertCount.intValue();
    }

    public int getNumberOfObjectUpdates()
    {
        return updateCount.intValue();
    }

    public int getNumberOfObjectDeletes()
    {
        return deleteCount.intValue();
    }

    public void incrementInsertCount()
    {
        insertCount.incrementAndGet();
        if (parent != null)
        {
            parent.incrementInsertCount();
        }
    }

    public void incrementDeleteCount()
    {
        deleteCount.incrementAndGet();
        if (parent != null)
        {
            parent.incrementDeleteCount();
        }
    }

    public void incrementFetchCount()
    {
        fetchCount.incrementAndGet();
        if (parent != null)
        {
            parent.incrementFetchCount();
        }
    }

    public void incrementUpdateCount()
    {
        updateCount.incrementAndGet();
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
        return txnExecutionTimeLow.longValue();
    }

    public long getTransactionExecutionTimeHigh()
    {
        return txnExecutionTimeHigh.longValue();
    }

    public long getTransactionExecutionTotalTime()
    {
        return txnExecutionTotalTime.longValue();
    }

    public int getTransactionTotalCount()
    {
        return txnTotalCount.intValue();
    }

    public int getTransactionActiveTotalCount()
    {
        return txnActiveTotalCount.intValue();
    }

    public int getTransactionCommittedTotalCount()
    {
        return txnCommittedTotalCount.intValue();
    }

    public int getTransactionRolledBackTotalCount()
    {
        return txnRolledBackTotalCount.intValue();
    }

    public void transactionCommitted(long executionTime)
    {
        this.txnCommittedTotalCount.incrementAndGet();
        this.txnActiveTotalCount.decrementAndGet();
        txnExecutionTimeAverage.compute(executionTime);
        txnExecutionTimeLow.accumulateAndGet(executionTime, (prev, x) -> {
            if (prev == -1) {
                return x;
            }
            return Math.min(prev, x);
        });
        txnExecutionTimeHigh.accumulateAndGet(executionTime, Math::max);
        txnExecutionTotalTime.addAndGet(executionTime);

        numReadsLastTxn.accumulateAndGet(numReads.intValue(), (prev, x) -> x - prev);
        numWritesLastTxn.accumulateAndGet(numWrites.intValue(), (prev, x) -> x - prev);
        if (parent != null)
        {
            parent.transactionCommitted(executionTime);
        }
    }

    public void transactionRolledBack(long executionTime)
    {
        this.txnRolledBackTotalCount.incrementAndGet();
        this.txnActiveTotalCount.decrementAndGet();
        txnExecutionTimeAverage.compute(executionTime);
        txnExecutionTimeLow.accumulateAndGet(executionTime, (prev, x) -> {
            if (prev == -1) {
                return x;
            }
            return Math.min(prev, x);
        });
        txnExecutionTimeHigh.accumulateAndGet(executionTime, Math::max);
        txnExecutionTotalTime.addAndGet(executionTime);

        numReadsLastTxn.accumulateAndGet(numReads.intValue(), (prev, x) -> x - prev);
        numWritesLastTxn.accumulateAndGet(numWrites.intValue(), (prev, x) -> x - prev);
        if (parent != null)
        {
            parent.transactionRolledBack(executionTime);
        }
    }

    public void transactionStarted()
    {
        this.txnTotalCount.incrementAndGet();
        this.txnActiveTotalCount.incrementAndGet();

        numReadsStartTxn.set(numReads.intValue());
        numWritesStartTxn.set(numWrites.intValue());
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
        private LinkedList<Double> values = new LinkedList<>();

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
                sum -= values.getFirst().doubleValue();
                values.removeFirst();
            }
            sum += value;
            values.addLast(Double.valueOf(value));
            average = sum / values.size();
            return average;
        }
    }
}