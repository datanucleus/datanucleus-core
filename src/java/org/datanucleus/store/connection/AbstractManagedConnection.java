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
package org.datanucleus.store.connection;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.xa.XAResource;

import org.datanucleus.util.StringUtils;

/**
 * Abstract implementation of a managed connection.
 * There are three primary modes for a connection.
 * <ul>
 * <li>Transactional - the commit of connection is controlled external to this class, and when we release
 * the connection it is handed back into a pooled state, available for reuse.</li>
 * <li>Non-transactional (1) - the commit of the connection happens at close and when we release the
 * connection it closes the connection (after committing it).</li>
 * <li>Non-transactional (2) - the commit of the connection happens at release, and when we release
 * the connection it is handed back into a pooled state, available for reuse.</li>
 * </ul>
 */
public abstract class AbstractManagedConnection implements ManagedConnection
{
    /** The underlying (datastore-specific) connection. */
    protected Object conn;

    /** Whether we should close() when release() of the connection is called. */
    protected boolean closeOnRelease = true;

    /** Whether we should commit() the connection on release(). */
    protected boolean commitOnRelease = true;

    /** Whether the connection is locked for use. */
    protected boolean locked = false;

    /** Listeners for the connection. */
    protected List<ManagedConnectionResourceListener> listeners = new ArrayList();

    /** Count on the number of outstanding uses of this connection. Incremented on get. Decremented on release(). */
    protected int useCount = 0;

    protected void incrementUseCount()
    {
        useCount = useCount + 1;
    }

    /**
     * Release this connection back to us so we can pool it if required. In the case of a transactional
     * connection it is allocated and released and always pooled (not committed) during the transaction. 
     * With non-transactional connections, they can be pooled (where selected), or not (default).
     */
    public void release()
    {
        if (closeOnRelease)
        {
            useCount = useCount -1;
            if (useCount == 0)
            {
                // Close if this is the last use of the connection
                close();
            }
        }
    }

    public void transactionFlushed()
    {
        for (int i=0; i<listeners.size(); i++)
        {
            listeners.get(i).transactionFlushed();
        }
    }

    public void transactionPreClose()
    {
        for (int i=0; i<listeners.size(); i++)
        {
            listeners.get(i).transactionPreClose();
        }
    }

    public void setCloseOnRelease(boolean close)
    {
        this.closeOnRelease = close;
    }

    public void setCommitOnRelease(boolean commit)
    {
        this.commitOnRelease = commit;
    }

    public boolean closeOnRelease()
    {
        return closeOnRelease;
    }

    public boolean commitOnRelease()
    {
        return commitOnRelease;
    }

    public void addListener(ManagedConnectionResourceListener listener)
    {
        listeners.add(listener);            
    }

    public void removeListener(ManagedConnectionResourceListener listener)
    {
        listeners.remove(listener);            
    }

    public boolean isLocked()
    {
        return locked;
    }

    public synchronized void lock()
    {
        locked = true;
    }

    public synchronized void unlock()
    {
        locked = false;
    }

    /**
     * Obtain an XAResource which can be enlisted in a transaction
     * Override this if you intend on supporting this as an XA resource (default = not supported).
     * @return The XA resource
     */
    public XAResource getXAResource()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.connection.ManagedConnection#closeAfterTransactionEnd()
     */
    public boolean closeAfterTransactionEnd()
    {
        return true;
    }

    public void setSavepoint(String name)
    {
        // Override this if you support savepoints
    }

    public void releaseSavepoint(String name)
    {
        // Override this if you support savepoints
    }

    public void rollbackToSavepoint(String name)
    {
        // Override this if you support savepoints
    }

    /**
     * Method to return a string form of this object for convenience debug.
     * @return The String form
     */
    public String toString()
    {
        return StringUtils.toJVMIDString(this) + " [conn=" + StringUtils.toJVMIDString(conn) +
                ", commitOnRelease=" + commitOnRelease + 
                ", closeOnRelease=" + closeOnRelease + 
                ", closeOnTxnEnd=" + closeAfterTransactionEnd() +"]";
    }
}