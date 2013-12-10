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
package org.datanucleus.store.schema;

import org.datanucleus.store.StoreManager;

/**
 * Interface defining schema operation for a StoreManager.
 */
public interface StoreSchemaHandler
{
    /**
     * Method to clear out any cached schema information.
     */
    public void clear();

    /**
     * Method to create the specified schema.
     * @param conn Connection to the datastore
     * @param schemaName Name of the schema
     */
    public void createSchema(Object conn, String schemaName);

    /**
     * Method to delete the specified schema.
     * @param conn Connection to the datastore
     * @param schemaName Name of the schema
     */
    public void deleteSchema(Object conn, String schemaName);

    /**
     * Accessor for schema data store under the provided name and defined by the specified values.
     * The supported types of values is particular to the implementation.
     * @param conn Connection to the datastore
     * @param name Name of the schema component to return.
     * @param values Value(s) to use as qualifier(s) for selecting the schema component
     * @return Schema data definition for this name
     */
    public StoreSchemaData getSchemaData(Object conn, String name, Object[] values);

    /**
     * Accessor for the StoreManager.
     * @return StoreManager
     */
    public StoreManager getStoreManager();
}