/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.io.ObjectStreamException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.SoftValueMap;
import org.datanucleus.util.StringUtils;
import org.datanucleus.util.WeakValueMap;

/**
 * QueryResult taking in the list of identities of the objects of candidate type.
 * This is used where we cached the results of a query (the "ids") and just want to materialise them.
 * User can define the query extension "datanucleus.query.resultCache.type" to define the type of internal
 * caching of objects once they are found.
 * User can also define whether the returned objects are validated against the datastore upon retrieval
 * using the query extension "datanucleus.query.resultCache.validateObjects" (default=true)
 */
public class CandidateIdsQueryResult extends AbstractQueryResult implements java.io.Serializable
{
    /** List of identities of the candidate objects. */
    final List<Object> ids;

    Map<Integer, Object> results = null;

    /** Whether to validate the objects if getting from the cache. */
    boolean validateObjects = true;

    public CandidateIdsQueryResult(Query query, List<Object> ids)
    {
        super(query);
        this.ids = ids;
        size = (ids != null ? ids.size() : 0); // Size is known

        // Allow override of validate setting
        validateObjects = query.getBooleanExtensionProperty("datanucleus.query.resultCache.validateObjects", true);

        // Cache the results in whatever form they are required
        String ext = (String)query.getExtension("datanucleus.query.resultCache.type");
        if (ext != null)
        {
            if (ext.equalsIgnoreCase("soft"))
            {
                results = new SoftValueMap();
            }
            else if (ext.equalsIgnoreCase("strong"))
            {
                results = new HashMap();
            }
            else if (ext.equalsIgnoreCase("weak"))
            {
                results = new WeakValueMap();
            }
            else if (ext.equalsIgnoreCase("none"))
            {
                results = null;
            }
            else
            {
                results = new HashMap<Integer, Object>();
            }
        }
        else
        {
            results = new HashMap<Integer, Object>();
        }
    }

    protected void closeResults()
    {
        // No results to close
    }

    protected void closingConnection()
    {
        if (loadResultsAtCommit && isOpen())
        {
            for (int i=0;i<size;i++)
            {
                getObjectForIndex(i);
            }
        }
    }

    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof CandidateIdsQueryResult))
        {
            return false;
        }

        CandidateIdsQueryResult other = (CandidateIdsQueryResult)o;
        if (query != null)
        {
            return other.query == query;
        }
        return StringUtils.toJVMIDString(other).equals(StringUtils.toJVMIDString(this));
    }

    @Override
    public Object get(int index)
    {
        if (index < 0 || index >= size)
        {
            throw new ArrayIndexOutOfBoundsException("Index should be between 0 and " + (size-1));
        }
        return getObjectForIndex(index);
    }

    public Iterator iterator()
    {
        return new ResultIterator();
    }

    public ListIterator listIterator()
    {
        return new ResultIterator();
    }

    /**
     * Convenience method to get the object for a particular index.
     * Loads the object as required, or takes it from the internal cache (if present).
     * Stores the returned object in the internal cache (if present).
     * @param index The index
     * @return The object
     */
    protected Object getObjectForIndex(int index)
    {
        if (ids == null)
        {
            return null;
        }
        Object id = ids.get(index);

        if (results != null)
        {
            Object obj = results.get(index);
            if (obj != null)
            {
                return obj;
            }
        }

        if (query == null)
        {
            throw new NucleusUserException("Query has already been closed");
        }
        else if (query.getExecutionContext() == null || query.getExecutionContext().isClosed())
        {
            throw new NucleusUserException("ExecutionContext has already been closed");
        }
        else
        {
            ExecutionContext ec = query.getExecutionContext();
            Object obj = ec.findObject(id, validateObjects, false, null);
            if (results != null)
            {
                results.put(index, obj);
            }
            return obj;
        }
    }

    /**
     * Iterator for results for this query.
     */
    public class ResultIterator extends AbstractQueryResultIterator
    {
        int next = 0;

        public boolean hasNext()
        {
            if (!isOpen())
            {
                // Spec 14.6.7 Calling hasNext() on closed Query will return false
                return false;
            }

            return (size - next > 0);
        }

        public boolean hasPrevious()
        {
            return (next >= 1);
        }

        public Object next()
        {
            if (next == size)
            {
                throw new NoSuchElementException("Already at end of List");
            }
            Object obj = getObjectForIndex(next);
            next++;
            return obj;
        }

        public int nextIndex()
        {
            return next;
        }

        public Object previous()
        {
            if (next == 0)
            {
                throw new NoSuchElementException("Already at start of List");
            }
            Object obj = getObjectForIndex(next-1);
            next--;
            return obj;
        }

        public int previousIndex()
        {
            return (next-1);
        }
    }

    /**
     * Handle serialisation by returning a java.util.ArrayList of all of the results for this query
     * after disconnecting the query which has the consequence of enforcing the load of all objects.
     * @return The object to serialise
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        disconnect();
        List results = new java.util.ArrayList(this.results.size());
        for (int i=0;i<this.results.size();i++)
        {
            results.add(this.results.get(i));
        }
        return results;
    }
}