package org.datanucleus.store.types;

public interface SequenceAdapter
{
    public abstract void update(Object newElement, int position);
}
