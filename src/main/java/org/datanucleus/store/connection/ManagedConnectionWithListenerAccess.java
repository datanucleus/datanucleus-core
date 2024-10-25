package org.datanucleus.store.connection;

import java.util.Collection;

public interface ManagedConnectionWithListenerAccess
{
    /**
     * Get registered listeners.
     * @return registered listeners
     */
    Collection<ManagedConnectionResourceListener> getListeners();
}
