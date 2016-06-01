/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.types.IncompatibleFieldTypeException;
import org.datanucleus.store.types.scostore.Store;

/**
 * Interface implemented by a StoreManager when it supports "backed" SCO wrappers (i.e SCO fields have a
 * connection to the datastore via a "backing store" to support more efficient connectivity).
 */
public interface BackedSCOStoreManager
{
    /**
     * Accessor for the backing store for the specified field/property.
     * @param clr ClassLoader resolver
     * @param mmd MetaData for the field/property
     * @param type Type of the member
     * @return Backing store
     * @throws IncompatibleFieldTypeException raises the exception if the field is not compatible if the store
     */
    Store getBackingStoreForField(ClassLoaderResolver clr, AbstractMemberMetaData mmd, Class type);
}