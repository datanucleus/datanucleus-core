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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.connection.ManagedConnectionResourceListener;
import org.datanucleus.util.Localiser;

/**
 * Abstract representation of a QueryResult.
 * Provides default implementations of the majority of list methods that we aren't likely to
 * be providing in a concrete query result.
 * This class is used where your query implementation needs to return a wrapper to a List so that
 * you can intercept calls and convert a row of the results into object(s), to avoid full instantiation
 * at creation.
 * Supports the following query extensions :-
 * <ul>
 * <li><b>datanucleus.query.resultSizeMethod</b> The method used to find the size of the result set.</li>
 * <li><b>datanucleus.query.loadResultsAtCommit</b> Whether to load all results when the connection is closing.
 * Has no effect if caching is not used.</li>
 * </ul>
 */
public abstract class AbstractQueryResult<E> extends AbstractList<E> implements QueryResult<E>, Serializable
{
    private static final long serialVersionUID = -4600803916251436835L;

    /** Whether the results are close. */
    protected boolean closed = false;

    /** The Query object. */
    protected Query query;

    /** List of listeners to notify when the query results are closed. */
    protected List<ManagedConnectionResourceListener> connectionListeners = null;

    ApiAdapter api;

    /** size of the query results. Is -1 until known. */
    protected int size = -1;

    /** Method for getting the size of the results. */
    protected String resultSizeMethod = "last"; // Default to moving to last row

    /** Whether to load any unread results at commit (when connection is closed). */
    protected boolean loadResultsAtCommit = true; // Default to load

    /**
     * Constructor of the result from a Query.
     * @param query The Query
     */
    public AbstractQueryResult(Query query)
    {
        this.query = query;
        if (query != null) // In tests we pass in null
        {
            this.api = query.getExecutionContext().getApiAdapter();

            // Process any supported extensions
            resultSizeMethod = query.getStringExtensionProperty(Query.EXTENSION_RESULT_SIZE_METHOD, "last");
            loadResultsAtCommit = query.getBooleanExtensionProperty(Query.EXTENSION_LOAD_RESULTS_AT_COMMIT, true);
        }
    }

    /**
     * Method to disconnect the results from the ExecutionContext, meaning that thereafter it just behaves
     * like a List. All remaining results are read in at this point (unless selected not to be).
     */
    public void disconnect()
    {
        if (query == null)
        {
            // Already disconnected
            return;
        }

        try
        {
            // Inform that we are closing the connection, so all results are read as necessary
            closingConnection();

            // Close the result set
            closeResults();
        }
        finally
        {
            // We put this in finally in case the store plugin QueryResult throws an exception during disconnect
            // so this marks the QueryResult as already disconnected
            query = null;
        }
    }

    /**
     * Inform the query result that the connection is being closed so perform
     * any operations now, or rest in peace.
     */
    protected abstract void closingConnection();

    /**
     * Inform the query result that we are closing the results now.
     */
    protected abstract void closeResults();

    /**
     * Method to close the results, meaning that they are inaccessible after this point.
     */
    public synchronized void close()
    {
        if (closed)
        {
            return;
        }

        // Close the result set
        closeResults();

        // Release all resources
        query = null;
        closed = true;

        if (connectionListeners != null)
        {
            // Call all listeners that we are closing
            Iterator<ManagedConnectionResourceListener> iter = connectionListeners.iterator();
            while (iter.hasNext())
            {
                iter.next().resourcePostClose();
            }
            connectionListeners.clear();
            connectionListeners = null;
        }
    }

    /**
     * Method to register a listener to be notified when the query result is closing.
     * @param listener The listener
     */
    public void addConnectionListener(ManagedConnectionResourceListener listener)
    {
        if (connectionListeners == null)
        {
            connectionListeners = new ArrayList();
        }
        connectionListeners.add(listener);
    }

    /**
     * Accessor whether the results are open.
     * @return Whether it is open.
     */
    protected boolean isOpen()
    {
        return closed == false;
    }

    /**
     * Internal method to throw an Exception if the ResultSet is open.
     */
    protected void assertIsOpen()
    {
        if (!isOpen())
        {
            throw api.getUserExceptionForException(Localiser.msg("052600"), null);
        }
    }

    // ------------------------- Implementation of the List -------------------------

    /**
     * Method to add a result. Unsupported.
     * @param index The position to add
     * @param element The results to add
     */
    public void add(int index, E element)
    {
        throw new UnsupportedOperationException(Localiser.msg("052603"));
    }

    /**
     * Method to add results. Unsupported.
     * @param o The result to add
     * @return true if added successfully
     */
    public boolean add(E o)
    {
        throw new UnsupportedOperationException(Localiser.msg("052603"));
    }

    /**
     * Method to add results. Unsupported.
     * @param index The position to add
     * @param c The results to add
     * @return true if added successfully
     */
    public boolean addAll(int index, Collection c)
    {
        throw new UnsupportedOperationException(Localiser.msg("052603"));
    }

    /**
     * Method to clear the results.
     */
    public void clear()
    {
        throw new UnsupportedOperationException(Localiser.msg("052603"));
    }

    /**
     * Method to check if the specified object is contained in this result.
     * @param o The object
     * @return Whether it is contained here.
     */
    public boolean contains(Object o)
    {
        throw new UnsupportedOperationException(Localiser.msg("052604"));
    }

    /**
     * Method to check if all of the specified objects are contained here.
     * @param c The collection of objects
     * @return Whether they are all contained here.
     */
    public boolean containsAll(Collection c)
    {
        throw new UnsupportedOperationException(Localiser.msg("052604"));
    }

    /**
     * Equality operator for QueryResults.
     * Overrides the AbstractList implementation since that uses 
     * size() and iterator() and that would cause problems when closed.
     * @param o The object to compare against
     * @return Whether they are equal
     */
    public abstract boolean equals(Object o);

    /**
     * Method to retrieve a particular element from the list.
     * @param index The index of the element
     * @return The element at index
     */
    public abstract E get(int index);

    /**
     * Accessor for the hashcode of this object
     * @return The hash code
     */
    public int hashCode()
    {
        if (query != null)
        {
            return query.hashCode();
        }
        else
        {
            // Disconnected
            return super.hashCode();
        }
    }

    /**
     * Method to check the index of a result. Not supported.
     * @param o The result
     * @return The position
     */
    public int indexOf(Object o)
    {
        throw new UnsupportedOperationException(Localiser.msg("052604"));
    }

    /**
     * Returns <tt>true</tt> if this collection contains no elements.<p>
     * @return <tt>true</tt> if this collection contains no elements.
     */
    public boolean isEmpty()
    {
        return size() < 1;
    }

    /**
     * Accessor for an iterator for the results.
     * @return The iterator
     */
    public abstract Iterator<E> iterator();

    /**
     * Method to check the last index of a result. Not supported.
     * @param o The result
     * @return The last index
     */
    public int lastIndexOf(Object o)
    {
        throw new UnsupportedOperationException(Localiser.msg("052604"));
    }

    /**
     * Accessor for a list iterator for the results.
     * @return a ListIterator with the query results
     */
    public abstract ListIterator<E> listIterator();

    /**
     * Method to remove a result. Not supported.
     * @param index The position of the result.
     * @return The removed object.
     */
    public E remove(int index)
    {
        throw new UnsupportedOperationException(Localiser.msg("052603"));
    }

    /**
     * Method to set the position of a result. Not supported.
     * @param index Position of the result
     * @param element The result
     * @return The element
     */
    public E set(int index, E element)
    {
        throw new UnsupportedOperationException(Localiser.msg("052603"));
    }

    /**
     * Method to return the size of the result.
     * Hands off the calculation of the size to getSizeUsingMethod() which should be overridden
     * if you want to support other methods.
     * @return The size of the result.
     */
    public int size()
    {
        assertIsOpen();
        if (size < 0)
        {
            // Size not calculated so get the value
            size = getSizeUsingMethod();
        }
        return size;
    }

    /**
     * Method return a sub list of results.
     * Method create new ArrayList, iterate and call get() in subclass for optimum performance.
     * @param fromIndex start position
     * @param toIndex end position (exclusive)
     * @return The list of results
     */
    public List<E> subList(int fromIndex, int toIndex)
    {
        int subListLength = toIndex - fromIndex;
        ArrayList subList = new ArrayList(subListLength);
        for (int i = fromIndex; i < toIndex; i++)
        {
            subList.add(get(i));
        }
        return subList;
    }

    /**
     * Method to return the results as an array.
     * @return The array.
     */
    public Object[] toArray()
    {
        Object[] array = new Object[size()];
        for (int i = 0; i < array.length; i++)
        {
            array[i] = get(i);
        }
        return array;
    }

    /**
     * Method to return the results as an array.
     * @param a The array to copy into. 
     * @return The array.
     */
    public Object[] toArray(Object[] a)
    {
        int theSize = size();
        if (a.length >= theSize)
        {
            for (int i = 0; i < a.length; i++)
            {
                if (i < theSize)
                {
                    a[i] = get(i);
                }
                else
                {
                    a[i] = null;
                }
            }
            return a;
        }

        // Collection doesn't fit in the supplied array so allocate new as per Collection.toArray javadoc
        return toArray();
    }

    /**
     * Method to get the size using the "resultSizeMethod".
     * This implementation supports "COUNT" method. 
     * Override this in subclasses to implement other methods.
     * @return The size
     */
    protected int getSizeUsingMethod()
    {
        if (resultSizeMethod.equalsIgnoreCase("COUNT"))
        {
            if (query != null && query.getCompilation() != null)
            {
                ExecutionContext ec = query.getExecutionContext();
                if (query.getCompilation().getQueryLanguage().equalsIgnoreCase("JDOQL"))
                {
                    // JDOQL : "count([DISTINCT ]this)" query
                    Query countQuery = query.getStoreManager().getQueryManager().newQuery("JDOQL", ec, query);
                    if (query.getResultDistinct())
                    {
                        countQuery.setResult("COUNT(DISTINCT this)");
                    }
                    else
                    {
                        countQuery.setResult("count(this)");
                    }
                    countQuery.setOrdering(null); // Ordering not relevant to a count
                    countQuery.setRange(null); // Don't want range to interfere with the query

                    Map queryParams = query.getInputParameters();
                    long count;
                    if (queryParams != null)
                    {
                        count = ((Long)countQuery.executeWithMap(queryParams)).longValue();
                    }
                    else
                    {
                        count = ((Long)countQuery.execute()).longValue();
                    }

                    if (query.getRange() != null)
                    {
                        // Query had a range, so update the returned count() to allow for the required range
                        long rangeStart = query.getRangeFromIncl();
                        long rangeEnd = query.getRangeToExcl();
                        count -= rangeStart;
                        if (count > (rangeEnd - rangeStart))
                        {
                            count = rangeEnd - rangeStart;
                        }
                    }

                    countQuery.closeAll();
                    return (int)count;
                }
                else if (query.getCompilation().getQueryLanguage().equalsIgnoreCase("JPQL"))
                {
                    // JPQL : "count()" query
                    Query countQuery = query.getStoreManager().getQueryManager().newQuery("JPQL", ec, query);
                    countQuery.setResult("count(" + query.getCompilation().getCandidateAlias() + ")");
                    countQuery.setOrdering(null);
                    countQuery.setRange(null); // Don't want range to interfere with the query
                    Map queryParams = query.getInputParameters();
                    long count;
                    if (queryParams != null)
                    {
                        count = ((Long)countQuery.executeWithMap(queryParams)).longValue();
                    }
                    else
                    {
                        count = ((Long)countQuery.execute()).longValue();
                    }

                    if (query.getRange() != null)
                    {
                        // Query had a range, so update the returned count() to allow for the required range
                        long rangeStart = query.getRangeFromIncl();
                        long rangeEnd = query.getRangeToExcl();
                        count -= rangeStart;
                        if (count > (rangeEnd - rangeStart))
                        {
                            count = rangeEnd - rangeStart;
                        }
                    }

                    countQuery.closeAll();
                    return (int)count;
                }
            }

            throw new NucleusUserException("datanucleus.query.resultSizeMethod of \"COUNT\" is only valid" +
            " for use with JDOQL or JPQL currently");
        }
        throw new NucleusUserException("DataNucleus doesnt currently support any method \"" + 
                    resultSizeMethod + "\" for determining the size of the query results");
    }
}