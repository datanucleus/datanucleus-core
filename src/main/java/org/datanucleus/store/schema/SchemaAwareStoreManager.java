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
package org.datanucleus.store.schema;

import java.util.Properties;
import java.util.Set;

/**
 * Interface to be implemented by all store managers that manage a "schema".
 * This interface makes the StoreManager usable with SchemaTool.
 */
public interface SchemaAwareStoreManager
{
    /**
     * Method that will create a database (catalog/schema) in the datastore (if the datastore supports it).
     * @param catalogName Name of the catalog to create
     * @param schemaName Name of the schema to create
     * @param props Any optional properties
     * @throws UnsupportedOperationException If not supported
     */
    void createDatabase(String catalogName, String schemaName, Properties props);

    /**
     * Method that will delete a database (catalog/schema) from the datastore (if the datastore supports it).
     * @param catalogName Name of the catalog to delete
     * @param schemaName Name of the schema to delete
     * @param props Any optional properties
     * @throws UnsupportedOperationException If not supported
     */
    void deleteDatabase(String catalogName, String schemaName, Properties props);

    /**
     * Create the schema (tables/constraints) for the specified classes (if supported by this datastore).
     * @param classNames Names of the classes
     * @param props Any optional properties
     * @throws UnsupportedOperationException If not supported
     */
    void createSchemaForClasses(Set<String> classNames, Properties props);

    /**
     * Delete the schema (tables/constraints) for the specified classes (if supported by this datastore).
     * @param classNames Names of the classes
     * @param props Any optional properties
     * @throws UnsupportedOperationException If not supported
     */
    void deleteSchemaForClasses(Set<String> classNames, Properties props);

    /**
     * Validate the schema (tables/constraints) for the specified classes (if supported by this datastore).
     * @param classNames Names of the classes
     * @param props Any optional properties
     * @throws UnsupportedOperationException If not supported
     */
    void validateSchemaForClasses(Set<String> classNames, Properties props);
}