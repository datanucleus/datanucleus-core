/**********************************************************************
Copyright (c) 2005 Erik Bengtson and others. All rights reserved. 
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
package org.datanucleus.store.query;

import java.util.Collection;

/**
 * Lazy collection results from a Query. The actual result elements are only loaded when accessed.
 * <p>
 * The lifecycle of a QueryResult is as follows
 * <ul>
 * <li><b>Open, Connected, With Connection</b> - the query has been run and the results returned.</li>
 * <li><b>Open, Disconnected</b> - the query has been run, and the txn committed, and the PM closed so has 
 *     its results available internally</li>
 * <li><b>Closed, Disconnected</b> - the query has been run, txn committed, query results closed.</li>
 * </ul>
 */
public interface QueryResult extends Collection
{
    /**
     * Method to close the results, making them unusable thereafter.
     */
    void close();

    /**
     * Method to disconnect the results from the ExecutionContext, meaning that thereafter it just behaves
     * like a List.
     */
    void disconnect();
}