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
package org.datanucleus.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;

import org.datanucleus.NucleusContext;
import org.datanucleus.util.SoftValueMap;

/**
 * Soft implementation of a Level 2 cache.
 * The second (unpinned) map stores soft references meaning that they may be garbage
 * collected only if necessary by the JVM.
 */
public class SoftLevel2Cache extends WeakLevel2Cache
{
    /**
     * Constructor.
     * @param nucleusCtx Context
     */
    public SoftLevel2Cache(NucleusContext nucleusCtx)
    {
        apiAdapter = nucleusCtx.getApiAdapter();
        pinnedCache = new HashMap();
        unpinnedCache = new SoftValueMap();
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException
    {
        // our "pseudo-constructor"
        in.defaultReadObject();
        unpinnedCache = new SoftValueMap();
    }
}