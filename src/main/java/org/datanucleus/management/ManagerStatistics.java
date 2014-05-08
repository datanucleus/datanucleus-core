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
 * Statistics for a manager of persistence (PersistenceManager/EntityManager).
 * Provides access to statistics about datastores accesses, queries and transactions.
 * Any statistics are represented also in the factory owning this manager.
 */
public class ManagerStatistics extends AbstractStatistics implements ManagerStatisticsMBean
{
    public ManagerStatistics(String name, FactoryStatistics parent)
    {
        super(name);
        this.parent = parent;
    }
}