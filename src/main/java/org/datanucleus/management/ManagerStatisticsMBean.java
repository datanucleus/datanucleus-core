package org.datanucleus.management;

/**
 * Interface defining the MBean for a persistence manager.
 */
public interface ManagerStatisticsMBean
{
    String getRegisteredName();

    int getQueryActiveTotalCount();

    int getQueryErrorTotalCount();

    int getQueryExecutionTotalCount();

    long getQueryExecutionTimeLow();

    long getQueryExecutionTimeHigh();

    long getQueryExecutionTotalTime();

    long getQueryExecutionTimeAverage();

    int getNumberOfDatastoreWrites();

    int getNumberOfDatastoreReads();

    int getNumberOfDatastoreWritesInLatestTxn();

    int getNumberOfDatastoreReadsInLatestTxn();

    int getNumberOfObjectFetches();

    int getNumberOfObjectInserts();

    int getNumberOfObjectUpdates();

    int getNumberOfObjectDeletes();

    long getTransactionExecutionTimeAverage();

    long getTransactionExecutionTimeLow();

    long getTransactionExecutionTimeHigh();

    long getTransactionExecutionTotalTime();

    int getTransactionTotalCount();

    int getTransactionActiveTotalCount();

    int getTransactionCommittedTotalCount();

    int getTransactionRolledBackTotalCount();
}