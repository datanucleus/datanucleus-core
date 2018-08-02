/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
 * Weak referenced implementation of a Level 2 cache.
 * <p>
 * Operates with 3 maps internally. One stores all pinned objects that have been selected to be retained by user's application. 
 * The second stores all other objects, and is the default location where objects are placed when being added here, using weak references meaning that they can 
 * get garbage collected as necessary by the JVM.
 * The third stores objects keyed by the unique key that they relate to.
 * </P>
 * <P>
 * Maintains collections of the classes and the identities that are to be pinned if they ever are put into the cache. These are defined by the pinAll(), pin() methods.
 * </P>
 * <P>
 * All mutating methods, and the get method have been synchronized to prevent conflicts.
 * </P>
 */
public class WeakLevel2Cache extends AbstractReferencedLevel2Cache
{
    public static final String NAME = "weak";

    private static final long serialVersionUID = 1328458846223231905L;

    /**
     * Constructor.
     * @param nucleusCtx Context
     */
    public WeakLevel2Cache(NucleusContext nucleusCtx)
    {
        super(nucleusCtx);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.cache.AbstractReferencedLevel2Cache#initialiseCaches()
     */
    @Override
    protected void initialiseCaches()
    {
        unpinnedCache = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.WEAK);
        uniqueKeyCache = new ConcurrentReferenceHashMap<>(1, ReferenceType.STRONG, ReferenceType.WEAK);
    }
}