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
package org.datanucleus;

/**
 * Listener of events raised on transaction begin, commit, rollback and flush.
 */
public interface TransactionEventListener
{
    /**
     * Method invoked when the transaction is started.
     */
    void transactionStarted();

    /**
     * Method invoked when the transaction is ended (Using XA).
     */
    void transactionEnded();

    /**
     * Method invoked just before a flush.
     */
    void transactionPreFlush();

    /**
     * Method invoked when the transaction is flushed (happens before commit, rollback).
     */
    void transactionFlushed();

    /**
     * Method invoked before the transaction commit.
     */
    void transactionPreCommit();

    /**
     * Method invoked when the transaction is committed.
     */
    void transactionCommitted();

    /**
     * Method invoked before the transaction is rolledback.
     */
    void transactionPreRollBack();

    /**
     * Method invoked when the transaction is rolled back.
     */
    void transactionRolledBack();
}