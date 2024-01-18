package org.datanucleus.flush;

/**
 * A marker interface for FlushProcess implementations to control
 * if org.datanucleus.ExecutionContext#flushOperationsForBackingStore
 * should do anything.
 * Some FlushProcess implementations might want to handle these
 * operations by them self.
 */
public interface ControlOperationQueueForBackingStoreFlushProcess
{
    /**
     *
     * @return whether org.datanucleus.ExecutionContext#flushOperationsForBackingStore
     * should do anything or not. Return true to have it perform all operations and return
     * false to have it do nothing.
     */
    boolean flushOperationQueueForBackingStore();
}
