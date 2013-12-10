/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancer.spi;

import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.FieldConsumer;
import org.datanucleus.store.fieldmanager.FieldSupplier;

/**
 * Standard interface that any class that is persistable should implement when not using JDO.
 * Based roughly on the JDO PersistenceCapable and Detachable interfaces.
 * Note that this is not yet used by DataNucleus, but is intended as something for the future.
 */
public interface Persistable
{
    static final byte READ_WRITE_OK = 0;
    static final byte LOAD_REQUIRED = 1;
    static final byte READ_OK = -1;
    static final byte CHECK_READ = 1;
    static final byte MEDIATE_READ = 2;
    static final byte CHECK_WRITE = 4;
    static final byte MEDIATE_WRITE = 8;
    static final byte SERIALIZABLE = 16;

    ObjectProvider dnGetStateManager();

    void dnReplaceStateManager(ObjectProvider sm) throws SecurityException;

    void dnProvideField(int fieldNumber);
    void dnProvideFields(int[] fieldNumbers);
    void dnReplaceField(int fieldNumber);
    void dnReplaceFields(int[] fieldNumbers);
    void dnReplaceFlags();
    void dnCopyFields(Object other, int[] fieldNumbers);
    void dnMakeDirty(String fieldName);

    Object dnGetObjectId();
    Object dnGetTransactionalObjectId();
    Object dnGetVersion();
    boolean dnIsDirty();
    boolean dnIsTransactional();
    boolean dnIsPersistent();
    boolean dnIsNew();
    boolean dnIsDeleted();
    boolean dnIsDetached();

    Persistable dnNewInstance(ObjectProvider sm);
    Persistable dnNewInstance(ObjectProvider sm, Object oid);

    Object dnNewObjectIdInstance();
    Object dnNewObjectIdInstance(Object o);
    void dnCopyKeyFieldsToObjectId(Object oid);
    void dnCopyKeyFieldsToObjectId(FieldSupplier fm, Object oid);
    void dnCopyKeyFieldsFromObjectId(FieldConsumer fm, Object oid);

    void dnReplaceDetachedState();
}