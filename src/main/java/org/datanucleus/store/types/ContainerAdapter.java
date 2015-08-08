package org.datanucleus.store.types;

/**
 * An adapter for container classes allowing DN to operate on them in a generic form instead of
 * depending directly on the JDK containers.
 * @param <C> The container class
 */
public interface ContainerAdapter<C> extends Iterable<Object>
{
    C getContainer();

    void clear();
}