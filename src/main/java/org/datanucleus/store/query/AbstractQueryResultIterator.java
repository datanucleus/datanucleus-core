/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import java.util.ListIterator;

import org.datanucleus.util.Localiser;

/**
 * Abstract implementation of an iterator for query results.
 * Can be used as the base class for an iterator for the implementation of AbstractQueryResult.
 */
public abstract class AbstractQueryResultIterator implements ListIterator
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Constructor.
     */
    public AbstractQueryResultIterator()
    {
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#add(java.lang.Object)
     */
    public void add(Object arg0)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#hasNext()
     */
    public abstract boolean hasNext();

    /* (non-Javadoc)
     * @see java.util.ListIterator#hasPrevious()
     */
    public abstract boolean hasPrevious();

    /* (non-Javadoc)
     * @see java.util.ListIterator#next()
     */
    public abstract Object next();

    /* (non-Javadoc)
     * @see java.util.ListIterator#nextIndex()
     */
    public abstract int nextIndex();

    /* (non-Javadoc)
     * @see java.util.ListIterator#previous()
     */
    public abstract Object previous();

    /* (non-Javadoc)
     * @see java.util.ListIterator#previousIndex()
     */
    public abstract int previousIndex();

    /* (non-Javadoc)
     * @see java.util.ListIterator#remove()
     */
    public void remove()
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#set(java.lang.Object)
     */
    public void set(Object arg0)
    {
        throw new UnsupportedOperationException(LOCALISER.msg("052603"));
    }
}