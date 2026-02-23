/**********************************************************************
Copyright (c) 2026 Contributors. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.datanucleus.flush.FlushMode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for FlushMode initialization during ExecutionContext construction.
 */
public class ExecutionContextFlushModeTest
{
    @Test
    public void testFlushModeInitializedFromProperties()
    {
        Map<String, Object> props = new HashMap<>();
        props.put(PropertyNames.PROPERTY_FLUSH_MODE, "AUTO");

        PersistenceNucleusContextImpl ctx = new PersistenceNucleusContextImpl(null, props)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized void initialise()
            {
            }
        };

        ExecutionContextImpl ec = new ExecutionContextImpl(ctx, null, new HashMap<String, Object>());
        Assert.assertEquals(FlushMode.AUTO, ec.getFlushMode());
    }

    @Test
    public void testFlushModeDefaultIsNull()
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
        Assert.assertNull(ec.getFlushMode());
    }
}
