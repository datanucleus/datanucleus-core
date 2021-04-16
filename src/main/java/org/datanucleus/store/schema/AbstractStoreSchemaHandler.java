/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.PropertyNames;
import org.datanucleus.store.StoreManager;

/**
 * Abstract base for any StoreSchemaHandler.
 */
public abstract class AbstractStoreSchemaHandler implements StoreSchemaHandler
{
    protected StoreManager storeMgr;

    /** Whether to auto create any database (catalog/schema). */
    protected boolean autoCreateDatabase;

    /** Whether to auto create any tables. */
    protected boolean autoCreateTables;

    /** Whether to auto create any columns that are missing. */
    protected boolean autoCreateColumns;

    /** Whether to auto create any constraints */
    protected boolean autoCreateConstraints;

    /** Whether to warn only when any errors occur on auto-create. */
    protected final boolean autoCreateWarnOnError;

    /** Whether to auto delete any columns that are present but not in the metadata. */
    protected final boolean autoDeleteColumns;

    /** Whether to validate any tables */
    protected final boolean validateTables;

    /** Whether to validate any columns */
    protected final boolean validateColumns;

    /** Whether to validate any constraints */
    protected final boolean validateConstraints;

    public AbstractStoreSchemaHandler(StoreManager storeMgr)
    {
        this.storeMgr = storeMgr;

        resetSchemaGeneration();

        autoCreateWarnOnError = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_WARNONERROR);
    	autoDeleteColumns = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTODELETE_COLUMNS);

    	boolean validateAll = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_VALIDATE_ALL);
        if (validateAll)
        {
            validateTables = true;
            validateColumns = true;
            validateConstraints = true;
        }
        else
        {
            validateTables = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_VALIDATE_TABLES);
            if (!validateTables)
            {
                validateColumns = false;
            }
            else
            {
                validateColumns = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_VALIDATE_COLUMNS);
            }
            validateConstraints = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_VALIDATE_CONSTRAINTS);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.StoreSchemaHandler#getStoreManager()
     */
    @Override
    public StoreManager getStoreManager()
    {
        return storeMgr;
    }

    public boolean isAutoCreateDatabase()
    {
    	return autoCreateDatabase;
    }

    public boolean isAutoCreateTables()
    {
        return autoCreateTables;
    }

    public boolean isAutoCreateColumns()
    {
        return autoCreateColumns;
    }

    public boolean isAutoCreateConstraints()
    {
        return autoCreateConstraints;
    }

    public boolean isAutoCreateWarnOnError()
    {
        return autoCreateWarnOnError;
    }

    public boolean isAutoDeleteColumns()
    {
        return autoDeleteColumns;
    }

    public boolean isValidateTables()
    {
        return validateTables;
    }

    public boolean isValidateColumns()
    {
        return validateColumns;
    }

    public boolean isValidateConstraints()
    {
        return validateConstraints;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.StoreSchemaHandler#clear()
     */
    @Override
    public void clear()
    {
        // Do nothing. Override this if you cache data in your implementation
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.StoreSchemaHandler#createDatabase(java.lang.String, java.lang.String, java.util.Properties, java.lang.Object)
     */
    @Override
    public void createDatabase(String catalogName, String schemaName, Properties props, Object connection)
    {
        throw new UnsupportedOperationException("This datastore doesn't support creation of database");
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.StoreSchemaHandler#deleteDatabase(java.lang.String, java.lang.String, java.util.Properties, java.lang.Object)
     */
    @Override
    public void deleteDatabase(String catalogName, String schemaName, Properties props, Object connection)
    {
        throw new UnsupportedOperationException("This datastore doesn't support deletion of database");
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.StoreSchemaHandler#createSchemaForClasses(java.util.Set, java.util.Properties, java.lang.Object)
     */
    @Override
    public void createSchemaForClasses(Set<String> classNames, Properties props, Object connection)
    {
        throw new UnsupportedOperationException("This datastore doesn't support creation of schema for classes");
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.StoreSchemaHandler#deleteSchemaForClasses(java.util.Set, java.util.Properties, java.lang.Object)
     */
    @Override
    public void deleteSchemaForClasses(Set<String> classNames, Properties props, Object connection)
    {
        throw new UnsupportedOperationException("This datastore doesn't support deletion of schema for classes");
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.StoreSchemaHandler#validateSchema(java.util.Set, java.util.Properties, java.lang.Object)
     */
    @Override
    public void validateSchema(Set<String> classNames, Properties props, Object connection)
    {
        throw new UnsupportedOperationException("This datastore doesn't support validation of schema");
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.StoreSchemaHandler#getSchemaData(java.lang.Object, java.lang.String, java.lang.Object[])
     */
    @Override
    public StoreSchemaData getSchemaData(Object connection, String name, Object[] values)
    {
        // Override this if you allow access to schema information
        return null;
    }

    @Override
    public void enableSchemaGeneration()
    {
        autoCreateDatabase = true;
        autoCreateTables = true;
        autoCreateColumns = true;
        autoCreateConstraints = true;
    }

    @Override
    public void resetSchemaGeneration()
    {
        boolean readOnlyDatastore = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY);
        if (readOnlyDatastore)
        {
            autoCreateDatabase = false;
            autoCreateTables = false;
            autoCreateColumns = false;
            autoCreateConstraints = false;
        }
        else
        {
            boolean autoCreateAll = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_ALL);
            if (autoCreateAll)
            {
                autoCreateDatabase = true;
                autoCreateTables = true;
                autoCreateColumns = true;
                autoCreateConstraints = true;
            }
            else
            {
                autoCreateDatabase = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_DATABASE);
                autoCreateTables = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES);
                autoCreateColumns = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS);
                autoCreateConstraints = storeMgr.getBooleanProperty(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS);
            }
        }
    }
}