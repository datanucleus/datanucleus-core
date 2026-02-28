/**********************************************************************
Copyright (c) 2025 DataNucleus contributors. All rights reserved.
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.datanucleus.state.DNStateManager;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that preRollback() handles null entries in the enlisted SM cache
 * without throwing NullPointerException. Null entries can appear when the
 * cache uses WEAK reference values and GC clears them during iteration.
 */
public class ExecutionContextNullSMGuardTest
{
    /**
     * Verifies that preRollback() safely skips null StateManager entries
     * that may appear in enlistedSMCache due to weak reference collection.
     */
    @Test
    public void testPreRollbackSkipsNullSMEntries() throws Exception
    {
        Map<String, Object> props = new HashMap<>();
        PersistenceNucleusContextImpl ctx = new PersistenceNucleusContextImpl(null, props)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized void initialise()
            {
            }
        };

        ExecutionContextImpl ec = new ExecutionContextImpl(ctx, null, new HashMap<String, Object>());

        // Use reflection to inject a null value into enlistedSMCache,
        // simulating a weak reference cleared by GC
        Field cacheField = ExecutionContextImpl.class.getDeclaredField("enlistedSMCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, DNStateManager> cache = (Map<Object, DNStateManager>) cacheField.get(ec);
        cache.put("gc-cleared-key", null);

        // Call preRollback() directly — it iterates enlistedSMCache without
        // requiring an active transaction. Must complete without NPE.
        try
        {
            ec.preRollback();
        }
        catch (NullPointerException e)
        {
            Assert.fail("preRollback() threw NPE on null SM entry: " + e.getMessage());
        }
    }
}
