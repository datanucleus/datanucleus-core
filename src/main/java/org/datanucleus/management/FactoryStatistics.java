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

/**
 * Statistics for a factory of persistence (PMF/EMF).
 * Provides access to statistics about datastores accesses, queries, transactions as well as connections.
 */
public class FactoryStatistics extends AbstractStatistics implements FactoryStatisticsMBean
{
    int connectionActiveCurrent;
    int connectionActiveHigh;
    int connectionActiveTotal;

    public FactoryStatistics(ManagementManager mgmtManager)
    {
        super(mgmtManager, null);
    }

    public int getConnectionActiveCurrent()
    {
        return this.connectionActiveCurrent;
    }

    public int getConnectionActiveHigh()
    {
        return this.connectionActiveHigh;
    }

    public int getConnectionActiveTotal()
    {
        return this.connectionActiveTotal;
    }

    public void incrementActiveConnections()
    {
        this.connectionActiveCurrent++;
        this.connectionActiveTotal++;
        this.connectionActiveHigh = Math.max(this.connectionActiveHigh, this.connectionActiveCurrent);
    }

    public void decrementActiveConnections()
    {
        this.connectionActiveCurrent--;
    }
}