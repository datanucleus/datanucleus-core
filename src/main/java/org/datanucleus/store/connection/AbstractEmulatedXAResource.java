/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Abstract base for any emulated XAResource implementations for the store plugins.
 */
public abstract class AbstractEmulatedXAResource implements XAResource
{
    protected ManagedConnection mconn;

    public AbstractEmulatedXAResource(ManagedConnection mconn)
    {
        this.mconn = mconn;
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid, int)
     */
    @Override
    public void start(Xid xid, int flags) throws XAException
    {
        NucleusLogger.CONNECTION.debug(Localiser.msg("009017", mconn.toString(), xid.toString(), flags));
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid)
     */
    @Override
    public int prepare(Xid xid) throws XAException
    {
        NucleusLogger.CONNECTION.debug(Localiser.msg("009018", mconn.toString(), xid.toString()));
        return 0;
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid, boolean)
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException
    {
        NucleusLogger.CONNECTION.debug(Localiser.msg("009019", mconn.toString(), xid.toString(), onePhase));
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid)
     */
    @Override
    public void rollback(Xid xid) throws XAException
    {
        NucleusLogger.CONNECTION.debug(Localiser.msg("009021", mconn.toString(), xid.toString()));
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#end(javax.transaction.xa.Xid, int)
     */
    @Override
    public void end(Xid xid, int flags) throws XAException
    {
        NucleusLogger.CONNECTION.debug(Localiser.msg("009023", mconn.toString(), xid.toString(), flags));
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#forget(javax.transaction.xa.Xid)
     */
    @Override
    public void forget(Xid xid) throws XAException
    {
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource)
     */
    @Override
    public boolean isSameRM(XAResource xares) throws XAException
    {
        return (this == xares);
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#recover(int)
     */
    @Override
    public Xid[] recover(int arg0) throws XAException
    {
        throw new XAException("Unsupported operation");
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#getTransactionTimeout()
     */
    @Override
    public int getTransactionTimeout() throws XAException
    {
        return 0;
    }

    /* (non-Javadoc)
     * @see javax.transaction.xa.XAResource#setTransactionTimeout(int)
     */
    @Override
    public boolean setTransactionTimeout(int timeout) throws XAException
    {
        return false;
    }
}