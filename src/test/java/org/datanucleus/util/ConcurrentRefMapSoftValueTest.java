/**********************************************************************
Copyright (c) 2017 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.util;

import java.util.Map;

import org.datanucleus.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Tests the functionality of ConcurrentReferenceHashMap with SOFT values.
 */
public class ConcurrentRefMapSoftValueTest extends ReferenceValueMapTestCase
{
    public ConcurrentRefMapSoftValueTest(String name)
    {
        super(name);
    }

    protected Map<String, Object> newReferenceValueMap()
    {
        return new ConcurrentReferenceHashMap<String, Object>(1, ReferenceType.STRONG, ReferenceType.SOFT);
    }
}
