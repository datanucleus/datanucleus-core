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

import java.util.Properties;
import java.util.Set;

import org.datanucleus.store.StoreManager;

/**
 * Interface defining schema operation for a StoreManager.
 */
public interface StoreSchemaHandler
{
    StoreManager getStoreManager();

    boolean isAutoCreateDatabase();
    boolean isAutoCreateTables();
    boolean isAutoCreateConstraints();
    boolean isAutoCreateColumns();
    boolean isAutoCreateWarnOnError();
    boolean isAutoDeleteColumns();

    boolean isValidateTables();
    boolean isValidateColumns();
    boolean isValidateConstraints();

    /**
     * Method to clear out any cached schema information.
     */
    void clear();

    /**
     * Method to create the specified database (catalog/schema).
     * @param catalogName Name of the catalog
     * @param schemaName Name of the schema
     * @param props Any properties controlling the schema generation
     * @param connection Connection to use (null implies this will obtain its own connection)
     */
    void createDatabase(String catalogName, String schemaName, Properties props, Object connection);

    /**
     * Method to delete the specified database (catalog/schema).
     * @param catalogName Name of the catalog
     * @param schemaName Name of the schema
     * @param props Any properties controlling the schema deletion
     * @param connection Connection to use (null implies this will obtain its own connection)
     */
    void deleteDatabase(String catalogName, String schemaName, Properties props, Object connection);

    /**
     * Method to generate the required schema for the supplied classes.
     * Note that this does not generate a "schema", just the tables. Refer to createDatabase to create a "schema".
     * @param classNames Names of the classes we want the schema generating for.
     * @param props Any properties controlling the schema generation
     * @param connection Connection to use (null implies this will obtain its own connection)
     */
    void createSchemaForClasses(Set<String> classNames, Properties props, Object connection);

    /**
     * Method to delete the schema for the supplied classes.
     * Note that this does not delete a "schema", just the tables. Refer to deleteDatabase to delete a "schema".
     * @param classNames Names of the classes we want the schema deleting for.
     * @param props Any properties controlling the schema deletion
     * @param connection Connection to use (null implies this will obtain its own connection)
     */
    void deleteSchemaForClasses(Set<String> classNames, Properties props, Object connection);

    /**
     * Method to validate the schema for the supplied classes.
     * @param classNames Names of classes
     * @param props Any properties controlling schema validation
     * @param connection Connection to use (null implies this will obtain its own connection)
     */
    void validateSchema(Set<String> classNames, Properties props, Object connection);

    /**
     * Accessor for schema data store under the provided name and defined by the specified values.
     * The supported types of values is particular to the implementation.
     * @param connection Connection to the datastore
     * @param name Name of the schema component to return.
     * @param values Value(s) to use as qualifier(s) for selecting the schema component
     * @return Schema data definition for this name
     */
    StoreSchemaData getSchemaData(Object connection, String name, Object[] values);

    /**
     * Convenience method to override the specified schema generation properties and enable schema generation.
     */
    void enableSchemaGeneration();

    /**
     * Convenience method to reset the schema generation properties to their initial creation values, effectively undoing a call to <i>enableSchemaGeneration</i>.
     */
    void resetSchemaGeneration();
}