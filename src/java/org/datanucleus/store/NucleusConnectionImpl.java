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
package org.datanucleus.store;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;

/**
 * Representation of a datastore connection.
 * Provides access to the native connection for the datastore.
 */
public class NucleusConnectionImpl implements NucleusConnection
{
    /** Localisation utility for output messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Native connection for this datastore. **/
    private final Object nativeConnection;

    /** run "onClose" on close call */
    private final Runnable onClose;

    /** whether this connection is available to the developer */
    private boolean isAvailable = true;

    /**
     * Constructor for a datastore connection holder.
     * @param conn The native connection
     * @param onClose What to perform on closure
     */
    public NucleusConnectionImpl(Object conn, Runnable onClose)
    {
        this.nativeConnection = conn;
        this.onClose = onClose;
    }

    /**
     * Method to close the connection. 
     * Performs whatever action was specified at creation.
     * @throws NucleusUserException Thrown if the connection is no longer available.
     */
    public void close()
    {
        if (!isAvailable)
        {
            throw new NucleusUserException(LOCALISER.msg("046001"));
        }
        isAvailable = false;
        onClose.run();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.NucleusConnection#isAvailable()
     */
    public boolean isAvailable()
    {
        return isAvailable;
    }

    /**
     * Accessor for the native connection for this datastore.
     * For RDBMS this would be a java.sql.Connection, or for db4o an ObjectContainer etc.
     * @return The native connection
     */
    public Object getNativeConnection()
    {
        return nativeConnection;
    }
}