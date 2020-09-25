/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.schema;

import org.datanucleus.ExecutionContext;

/**
 * Interface to be implemented where the user wants to allocate a "tenantId" for each datastore access.
 * One example of such a usage is where the web system "session" has a 'username' and so can access it using this method.
 */
public interface MultiTenancyProvider
{
    /**
     * Return the tenant id for the current tenant (for use in all WRITE operations).
     * @param ec ExecutionContext
     * @return The tenant id
     */
    String getTenantId(ExecutionContext ec);

    /**
     * Return the tenant id(s) that the current tenant can view (for use in all READ operations).
     * @param ec ExecutionContext
     * @return The read tenant ids
     */
    String[] getTenantReadIds(ExecutionContext ec);
}