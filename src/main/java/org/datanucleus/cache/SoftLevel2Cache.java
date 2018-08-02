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

import org.datanucleus.NucleusContext;
import org.datanucleus.util.ConcurrentReferenceHashMap;
import org.datanucleus.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Soft implementation of a Level 2 cache.
 * The second (unpinned) map stores soft references meaning that they may be garbage collected only if necessary by the JVM.
 */
public class SoftLevel2Cache extends AbstractReferencedLevel2Cache
{
    public static final String NAME = "soft";

    private static final long serialVersionUID = -96782958845067038L;

    /**
     * Constructor.
     * @param nucleusCtx Context
     */
    public SoftLevel2Cache(NucleusContext nucleusCtx)
    {
        super(nucleusCtx);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.cache.AbstractReferencedLevel2Cache#initialiseCaches()
     */
    @Override
    protected void initialiseCaches()
    {
        unpinnedCache = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.SOFT);
        uniqueKeyCache = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.SOFT);
    }
}